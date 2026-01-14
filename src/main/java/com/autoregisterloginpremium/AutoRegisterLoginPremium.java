package com.autoregisterloginpremium;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoRegisterLoginPremium implements ModInitializer {
	public static final String MOD_ID = "auto-register-login-premium";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        LOGGER.info("AutoRegisterLoginPremium Mod initialisé avec succès !");
	}
}