# Server Design — Distributed Chess Server

## רקע

השרת הקיים (single-JVM) עובד לפי המודל הבא: `ChessWebSocketServer` מחזיק את כל
חיבורי הלקוחות, `CommandHandler` מטפל בכל סוגי ההודעות, ו-`GameMatch` (thread
ייעודי + `BlockingQueue<Command>` per game) מריץ את מנוע המשחק. זה עובד מצוין
ל-container בודד, אבל לא scale-י ל-10 מיליון משתמשים בו-זמנית.

המסמך הזה מתאר את המעבר לארכיטקטורה מבוזרת, שרצה על כמה containers, תוך
שינוי מינימלי לקוד הקיים ול**פרוטוקול מול הלקוח**.

## עיקרון-על: אין שינוי בפרוטוקול מול הלקוח

**הכל נשאר WebSocket. חיבור אחד בלבד, לכל אורך חיי הלקוח** — login, יצירת/הצטרפות
לחדר, matchmaking, מהלכים. **לא** נוסף REST/HTTP, **לא** נדרש redirect או חיבור
כפול. השיקול: `LOGIN` קורה פעם אחת בחיי החיבור בדיוק כמו כל הודעה אחרת — אין
יתרון אמיתי בפרוטוקול נפרד, ויש חיסרון ברור (שני clients בלקוח, לא אחד).

בעקבות זה — **אין הפרדה בין "API Gateway" ל-"WS Gateway"**. יש סוג container
אחד: `Gateway`, שהוא בעצם הקוד הקיים (`ChessWebSocketServer` + `CommandHandler`
+ `ConnectionSession`) כמעט ללא שינוי חיצוני.

## חלוקת האחריות — 5 סוגי Containers

### 1. Gateway
- מריץ בדיוק את `ChessWebSocketServer` + `CommandHandler` הקיימים.
- מחזיק את חיבורי ה-WebSocket הפיזיים ואת `ConnectionSession` לכל לקוח.
- **שינוי יחיד מהותי**: `ConnectionSession` לא מחזיק יותר הפניה ישירה ל-
  `GameMatch` (אובייקט), אלא רק `gameId` (String) — כי ה-`GameMatch` עצמו
  **אף פעם** לא רץ בתוך ה-Gateway, הוא רץ אך ורק על Game Server Shard.
- כש-`MOVE`/`JUMP`/`RESIGN` מגיעים: בונה `Command` (כמו היום, דרך
  `CommandParser`), ומפרסם אותו ל-NATS subject `game.<gameId>.commands` —
  **תמיד**, בלי מקרה-קצה של "match מקומי". ה-Gateway לא יודע ולא צריך לדעת
  איזה Shard מריץ את המשחק — NATS מנתב את זה.
- `ClientGateway` (במקום `Map<String, ServerSocketConnection>` בלבד) גם
  מאזין ל-NATS subject `client.<username>.out` כדי לקבל עדכוני `GAME_STATE`/
  `ACTION_OCCURRED` משדרים ולדחוף אותם לסוקט הנכון.

### 2. Matchmaker & Rooms
- מכיל את `PlayMatchmaker` (ללא שינוי — כבר עובד מול Redis, `zrangeByScore`)
  ואת `RoomManager`.
- **שינוי נדרש ב-`RoomManager`**: היום מחזיק `Map<String, Room>` בזיכרון
  מקומי. כדי לתמוך בכמה instances של השירות הזה, ה-state עובר ל-Redis,
  באותה שיטה שכבר קיימת ב-`PlayMatchmaker`.
- כשנמצא זיווג (`Paired`) — מפרסם ל-NATS `match.found` עם שני שמות המשתמשים,
  ולא בונה `GameMatch` בעצמו (זה תפקיד ה-Allocator+Shard).

### 3. Game Allocator
- חדש. מאזין ל-`match.found`.
- מחזיק registry של Shards פעילים (heartbeat דרך Redis).
- בוחר Shard פנוי, מייצר `gameId`, מפרסם `match.assigned` עם
  `{gameId, shardId, whiteUsername, blackUsername}`.

### 4. Game Server Shards
- מריץ את `GameMatch` + `GameEngine` + חלק מ-`MatchLauncher` הקיימים —
  **כמעט ללא שינוי לוגי**. ה-`GameEngine` נשאר single source of truth
  לחוקי המשחק, בדיוק כמו היום.
- מאזין ל-`match.assigned` בשבילו → קורא ל-`MatchLauncher.launch()` בדיוק
  כמו היום, ואז נרשם ל-`game.<gameId>.commands`.
- `ClientGateway` בתוך ה-Shard משתנה: במקום לשלוח ישירות לסוקט, מפרסם
  ל-NATS `client.<username>.out` — ה-Gateway הרלוונטי מאזין ומעביר ללקוח.
- אין לו גישה לסוקטים בכלל.

### 5. Observability
- הרחבה של `LogHandler`/`ServerLogger` הקיימים: logs, metrics, health
  checks. לא דחוף לגרסה הראשונה ("קטן ועובד").

## טכנולוגיות

| טכנולוגיה | תפקיד |
|---|---|
| **NATS** | ערוץ events פנימי בין כל 4 סוגי השירותים (fire-and-forget) |
| **Redis** | state זמני: matchmaking queue (קיים), rooms (חדש), reconnect, shard registry |
| **PostgreSQL** | state קבוע: users, ratings, (בעתיד: games/move history) |
| **Docker Compose** | גרסה ראשונה, קטנה, לבדיקה מקומית |
| **Kubernetes/K3s** | שלב מאוחר יותר, ל-scale אמיתי |

## זרימת אירועים מלאה — משחק דרך Matchmaking

```
1. לקוח → Gateway: PLAY (WebSocket, כמו היום)
2. Gateway → NATS "matchmaking.request" {username, rating}
3. Matchmaker (מאזין) → PlayMatchmaker.tryPair() הקיים, מול Redis
4. אם Paired: Matchmaker → NATS "match.found" {white, black}
5. Game Allocator (מאזין) → בוחר Shard פנוי → יוצר gameId
   → NATS "match.assigned" {gameId, shardId, white, black}
6. Game Shard הנבחר (מאזין) → MatchLauncher.launch() מקומית, כמו היום
   → GameMatch רץ, נרשם ל-"game.<gameId>.commands"
7. Gateway של כל שחקן (מאזין ל-"match.assigned") →
   מעדכן ConnectionSession(state=IN_GAME, gameId)
   → נרשם ל-"client.<username>.out" לקבלת עדכוני GAME_STATE
8. לקוח → Gateway: MOVE → NATS "game.<gameId>.commands" → Shard מבצע
   → GameMatch משדר SnapshotUpdatedEvent → ClientGateway (בShard) →
   NATS "client.<username>.out" → Gateway → סוקט → לקוח
```

## מה לא משתנה

- `GameEngine`, `RuleEngine`, כל חוקי המשחק — **ללא שינוי כלל**.
- `GameMatch` (thread + BlockingQueue per game, tick 33ms, EventBus) —
  **ללא שינוי לוגי**.
- הפרוטוקול מול הלקוח (`MessageType`, `ServerMessageType`) — **ללא שינוי**.
- `EloCalculator`, `ScoreHandler`, `UserRepository` — **ללא שינוי**.

## סדר מימוש (Docker Compose, גרסה ראשונה)

1. `NatsEventBus` — עטיפה משותפת לכל השירותים.
2. `Game Server Shard` — הכי קרוב לקוד הקיים, מתחילים כאן.
3. `Matchmaker & Rooms`.
4. `Game Allocator`.
5. `Gateway`.
6. `docker-compose.yml`: gateway, matchmaker-rooms, allocator, game-shard
   (×1 להתחלה), nats, redis, postgres.

עדיף גרסה קטנה שעובדת מאשר ניסיון לבנות הכל בבת אחת.
