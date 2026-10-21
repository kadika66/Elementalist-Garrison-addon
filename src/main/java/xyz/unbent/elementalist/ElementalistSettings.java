package xyz.unbent.elementalist;

import org.bukkit.configuration.file.YamlConfiguration;
import xyz.unbent.garrison.api.addon.AddonContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

/**
 * config.yml in {@code plugins/Garrison/addons/Elementalist/}. Written from the jar's copy the first time; any key
 * missing from the server's copy falls back to the jar's value.
 */
public final class ElementalistSettings {

	private final YamlConfiguration config;

	private ElementalistSettings(YamlConfiguration config) {
		this.config = config;
	}

	public static ElementalistSettings load(AddonContext context) {
		Logger logger = context.getLogger();
		File file = new File(context.getDataFolder(), "config.yml");
		YamlConfiguration defaults = new YamlConfiguration();
		try (InputStream in = context.getResource("config.yml")) {
			if (in != null) {
				byte[] bytes = in.readAllBytes();
				defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(new java.io.ByteArrayInputStream(bytes), StandardCharsets.UTF_8));
				if (!file.isFile())
					Files.copy(new java.io.ByteArrayInputStream(bytes), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			logger.severe("Could not write the default config.yml: " + e.getMessage());
		}
		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		config.setDefaults(defaults);
		return new ElementalistSettings(config);
	}

	/** The element new Elementalists start with; null means a random one each. */
	public Element getDefaultElement() {
		// No default argument on these getters: with one, Bukkit skips the jar's defaults.
		return Element.parse(config.getString("default-element"));
	}

	public boolean isAmbientParticles() {
		return config.getBoolean("ambient-particles");
	}

	/** A number from an ability's section, never negative. */
	public double get(Ability ability, String key) {
		return Math.max(0, config.getDouble("abilities." + ability.key() + "." + key));
	}

	public long cooldownMillis(Ability ability) {
		return (long) (get(ability, "cooldown-seconds") * 1000);
	}

	/** Seconds to ticks. */
	public int ticks(Ability ability, String key) {
		return (int) Math.round(get(ability, key) * 20);
	}
}
