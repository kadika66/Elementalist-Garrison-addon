package xyz.unbent.elementalist;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shipped config.yml has every number each ability reads, and garrison-addon.yml points at the addon class.
 * (What the abilities look like and do is tested in game.)
 */
class ElementalistFilesTest {

	private static final Path RESOURCES = Path.of("src/main/resources");
	private static final Path SOURCES = Path.of("src/main/java/xyz/unbent/elementalist");

	@Test
	void everyAbilityHasEveryNumberItReads() throws Exception {
		YamlConfiguration config = yaml("config.yml");
		List<String> missing = new ArrayList<>();
		// Abilities.java: each ability's method starts "Ability a = Ability.NAME;" and then reads s.get(a, "key").
		String abilities = Files.readString(SOURCES.resolve("Abilities.java"));
		Pattern read = Pattern.compile("s\\.(?:get|ticks)\\(a, \"([a-z\\-]+)\"\\)");
		String[] parts = abilities.split("Ability a = Ability\\.");
		for (int i = 1; i < parts.length; i++) {
			Ability ability = Ability.valueOf(parts[i].substring(0, parts[i].indexOf(';')));
			for (Matcher m = read.matcher(parts[i]); m.find(); )
				if (!config.contains("abilities." + ability.key() + "." + m.group(1)))
					missing.add(ability.key() + "." + m.group(1));
		}
		// Every ability also needs a cooldown, and the reach the behaviour checks before casting.
		for (Ability ability : Ability.values()) {
			String section = "abilities." + ability.key() + ".";
			if (!config.contains(section + "cooldown-seconds"))
				missing.add(ability.key() + ".cooldown-seconds");
			String reach = ability == Ability.TREMOR || ability == Ability.FROST ? "radius" : "range";
			if (!config.contains(section + reach))
				missing.add(ability.key() + "." + reach);
		}
		assertEquals(List.of(), missing, "config.yml is missing");
	}

	@Test
	void descriptorPointsAtTheAddonClass() throws Exception {
		YamlConfiguration descriptor = yaml("garrison-addon.yml");
		assertEquals("Elementalist", descriptor.getString("name"));
		assertEquals(ElementalistAddon.class.getName(), descriptor.getString("main"));
	}

	@Test
	void noBendingWordingInShippedFiles() throws IOException {
		List<String> hits = new ArrayList<>();
		try (Stream<Path> files = Files.walk(RESOURCES)) {
			for (Path file : files.filter(Files::isRegularFile).toList())
				if (Files.readString(file).toLowerCase(Locale.ROOT).contains("bend"))
					hits.add(file.getFileName().toString());
		}
		assertTrue(hits.isEmpty(), "\"bend\" wording in " + hits);
	}

	private static YamlConfiguration yaml(String name) throws IOException, InvalidConfigurationException {
		YamlConfiguration config = new YamlConfiguration();
		config.loadFromString(Files.readString(RESOURCES.resolve(name)));
		return config;
	}
}
