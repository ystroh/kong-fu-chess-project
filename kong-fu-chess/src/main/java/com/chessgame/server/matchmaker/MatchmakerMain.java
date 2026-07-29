
package com.chessgame.server.matchmaker;

import com.chessgame.server.common.bus.NatsEventBus;

public final class MatchmakerMain {

    public static void main(String[] args) {
        NatsEventBus bus = new NatsEventBus();
        PlayMatchmaker playMatchmaker = new PlayMatchmaker();
        RoomManager roomManager = new RoomManager();

        MatchmakerRoomsController controller = new MatchmakerRoomsController(playMatchmaker, roomManager, bus);
        controller.start();
    }
}