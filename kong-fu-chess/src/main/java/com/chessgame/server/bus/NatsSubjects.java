package com.chessgame.server.bus;

public final class NatsSubjects {

    private NatsSubjects() {
    }

    public static String commands(String gameId) {
        return "game." + gameId + ".commands";
    }

    public static String playerDisconnected(String gameId) {
        return "game." + gameId + ".player-disconnected";
    }

    public static String playerReconnected(String gameId) {
        return "game." + gameId + ".player-reconnected";
    }

    public static String clientOutbox(String username) {
        return "client." + username + ".out";
    }

    public static String matchmakingRequest() {
        return "matchmaking.request";
    }

    public static String matchmakingCancel() {
        return "matchmaking.cancel";
    }

    public static String matchFound() {
        return "match.found";
    }

    public static String matchAssigned() {
        return "match.assigned";
    }

    public static String roomsCreate() {
        return "rooms.create";
    }

    public static String roomsJoin() {
        return "rooms.join";
    }

    public static String roomsCancel() {
        return "rooms.cancel";
    }

    public static String reconnectKey(String username) {
        return "reconnect:" + username;
    }

    public static String sessionAssigned(String username) {
        return "session." + username + ".assigned";
    }

    public static String shardAssign(String shardId) {
        return "shard." + shardId + ".assign";
    }
}