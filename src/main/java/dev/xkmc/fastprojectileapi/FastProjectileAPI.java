package dev.xkmc.fastprojectileapi;

import dev.xkmc.fastprojectileapi.collision.FastMapInit;
import dev.xkmc.fastprojectileapi.spellcircle.SpellCircleConfig;
import dev.xkmc.l2core.serial.config.ConfigTypeEntry;
import dev.xkmc.l2core.serial.config.PacketHandlerWithConfig;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static dev.xkmc.fastprojectileapi.FastProjectileAPI.MODID;

@Mod(MODID)
public class FastProjectileAPI {

	public static final String MODID = "fast_projectile_api";
	public static final Logger LOGGER = LogManager.getLogger();
	public static final PacketHandlerWithConfig HANDLER = new PacketHandlerWithConfig(MODID, 1);

	public static final ConfigTypeEntry<SpellCircleConfig> SPELL =
			new ConfigTypeEntry<>(HANDLER, "spell", SpellCircleConfig.class);

	public FastProjectileAPI() {
		FastMapInit.init();
	}

	public static Identifier loc(String id) {
		return Identifier.fromNamespaceAndPath(MODID, id);
	}

}
