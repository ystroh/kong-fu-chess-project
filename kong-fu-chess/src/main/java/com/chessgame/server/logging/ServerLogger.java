package com.chessgame.server.logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

public final class ServerLogger {

    private static final PrintWriter writer = createWriter();

    private ServerLogger() {
    }

    private static PrintWriter createWriter() {
        try {
            return new PrintWriter(new FileWriter("logs/server.log", true), true);
        } catch (IOException e) {
            return new PrintWriter(System.out, true);
        }
    }

    public static synchronized void log(String message) {
        writer.println(Instant.now() + " " + message);
        writer.flush();
    }
}
