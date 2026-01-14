package com.autoregisterloginpremium;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class AutoRegisterLoginPremiumClient implements ClientModInitializer {

    private String currentPassword;
    private boolean authSent;

    @Override
    public void onInitializeClient() {

        // Lors de la connexion à un serveur
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (handler.getServerData() == null) return;

            String ip = handler.getServerData().ip
                    .replace(":", "_")
                    .replace("/", "_");

            currentPassword = getOrCreatePassword(ip);
            authSent = false;
        });

        // Lecture des messages serveur
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || authSent) return;

            String text = message.getString().toLowerCase();

            // Serveur demande /premium
            if (text.contains("/premium")
                    && mc.getUser().getType() == User.Type.MSA) {

                sendCommand(mc, "premium",
                        "[AutoAuth] Passage en premium",
                        ChatFormatting.GOLD);

                new Thread(() -> {
                try {
                        Thread.sleep(2000); // délai de 2 secondes
                        mc.execute(() -> {
                            if (mc.player != null) {
                                mc.player.connection.sendCommand("premium");
                                mc.player.displayClientMessage(
                                    Component.literal("[AutoAuth] Passage en premium (2/2)").withStyle(ChatFormatting.GOLD),
                                    true
                                );
                            }
                        });
                    } catch (InterruptedException ignored) {}
                }).start();
            }

            // Serveur demande /login
            else if (text.contains("/login")) {
                sendCommand(mc,
                        "login " + currentPassword,
                        "[AutoAuth] Connexion automatique",
                        ChatFormatting.GREEN);
            }

            // Serveur demande /register
            else if (text.contains("/register")) {
                sendCommand(mc,
                        "register " + currentPassword + " " + currentPassword,
                        "[AutoAuth] Enregistrement automatique",
                        ChatFormatting.AQUA);
            }
        });
    }

    private void sendCommand(Minecraft mc, String command, String feedback, ChatFormatting color) {
        authSent = true;

        new Thread(() -> {
            try {
                Thread.sleep(1500); // anti-spam
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.player.connection.sendCommand(command);
                        mc.player.displayClientMessage(
							Component.literal(feedback).withStyle(color),
							true
						);
                    }
                });
            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    private String getOrCreatePassword(String ip) {
        Path file = Paths.get("config", "autoauth", ip + ".json");

        try {
            if (Files.exists(file)) {
                String json = Files.readString(file);
                return JsonParser.parseString(json)
                        .getAsJsonObject()
                        .get("password")
                        .getAsString();
            }

            Files.createDirectories(file.getParent());

            String password = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 10);

            JsonObject obj = new JsonObject();
            obj.addProperty("password", password);

            Files.writeString(file, obj.toString());
            return password;

        } catch (IOException e) {
            return "Pass12345";
        }
    }
}
