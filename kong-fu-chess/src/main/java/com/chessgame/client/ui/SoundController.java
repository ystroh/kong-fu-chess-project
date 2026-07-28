package com.chessgame.client.ui;

import com.chessgame.common.protocol.response.ActionOccurredMessage;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

public final class SoundController {

    public SoundController(GameStateCoordinator coordinator) {
        coordinator.onActionOccurred(this::handleAction);
    }

    private void handleAction(ActionOccurredMessage msg) {
        if (msg.actionType().equals("JUMP")) {
            play("/sounds/jump.wav");
        } else if (msg.capture()) {
            play("/sounds/capture.wav");
        } else {
            play("/sounds/move.wav");
        }

        if (msg.gameOver()) {
            play("/sounds/game_over.wav");
        }
    }

    private void play(String resourcePath) {
        try {
            URL url = getClass().getResource(resourcePath);
            if (url == null) {
                System.out.println("Sound file not found: " + resourcePath);
                return;
            }
            AudioInputStream stream = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            clip.start();
        } catch (Exception e) {
            System.out.println("Failed to play sound: " + e);
        }
    }
}
