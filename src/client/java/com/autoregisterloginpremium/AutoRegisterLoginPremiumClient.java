package com.autoregisterloginpremium;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.network.chat.Component;

public class AutoRegisterLoginPremiumClient implements ClientModInitializer {

    // Initialisation du Logger
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoAuthPremium");

    private String currentPassword;
    private boolean authSent;

    @Override
    public void onInitializeClient() {
        LOGGER.info("AutoRegisterLoginPremium initialisé !");

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (handler.getServerData() == null) return;

            String rawIp = handler.getServerData().ip;
            String ip = rawIp.replace(":", "_").replace("/", "_");

            LOGGER.info("Connexion au serveur : {}", rawIp);
            currentPassword = getOrCreatePassword(ip);
            authSent = false;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || authSent) return;

            String text = message.getString().toLowerCase();

            // Détection du register & je suis premium
            if (text.contains("/register")) {
                LOGGER.info("Détection d'une demande d'enregistrement.");
                sendCommand(mc, "register " + currentPassword + " " + currentPassword, "[AutoAuth] Enregistrement automatique", ChatFormatting.AQUA, 1500);
                sendCommand(mc, "login " + currentPassword, "[AutoAuth] Connexion automatique", ChatFormatting.GREEN, 3000);

                if(mc.getUser().getType() == User.Type.MSA) 
                {
                    LOGGER.info("Passage en premium pour la prochaine connexion.");
                    sendCommand(mc, "premium", "[AutoAuth] Passage en premium", ChatFormatting.GOLD, 4500);
                    sendCommand(mc, "premium", "[AutoAuth] Passage en premium (2/2)", ChatFormatting.GOLD, 6000);
                }
            } 
            else if (text.contains("/login")) {
                LOGGER.info("Détection d'une demande de login. Envoi du mot de passe stocké.");
                sendCommand(mc, "login " + currentPassword, "[AutoAuth] Connexion automatique", ChatFormatting.GREEN, 1500);

            } 
            
        });
    }

    private void sendCommand(Minecraft mc, String command, String feedback, ChatFormatting color, int delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.player.connection.sendCommand(command);
                        mc.player.displayClientMessage(Component.literal("§7[AutoRegisterLoginPremium]" + command), false);
                        mc.player.displayClientMessage(Component.literal(feedback).withStyle(color), true);
                    }
                });
            } catch (InterruptedException e) {
                LOGGER.error("Le délai de la commande a été interrompu", e);
            }
        }).start();
    }

    private String getOrCreatePassword(String ip) {
        Path file = Paths.get("config", "autoauth", ip + ".json");

        try {
            if (Files.exists(file)) {
                String json = Files.readString(file);
                String pass = JsonParser.parseString(json).getAsJsonObject().get("password").getAsString();
                LOGGER.info("Mot de passe chargé depuis le fichier pour l'IP : {}", ip);
                return pass;
            }

            Files.createDirectories(file.getParent());
            String password = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            
            JsonObject obj = new JsonObject();
            obj.addProperty("password", password);
            Files.writeString(file, obj.toString());

            LOGGER.info("Nouveau mot de passe généré et sauvegardé pour : {}", ip);
            return password;

        } catch (IOException e) {
            LOGGER.error("Erreur lors de la lecture/écriture du fichier de configuration pour {}", ip, e);
            return "Pass12345";
        }
    }
}