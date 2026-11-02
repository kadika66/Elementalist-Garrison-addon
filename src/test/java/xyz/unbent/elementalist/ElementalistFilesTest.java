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
			if (!config.contains(section + ability.reachKey()))
				missing.add(ability.key() + "." + ability.reachKey());
		}
		// The mastery numbers, read as settings.mastery("key") anywhere in the code.
		Pattern masteryRead = Pattern.compile("mastery\\(\"([a-z\\-]+)\"\\)");
		try (Stream<Path> files = Files.walk(SOURCES)) {
			for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList())
				for (Matcher m = masteryRead.matcher(Files.readString(file)); m.find(); )
					if (!config.contains("mastery." + m.group(1)))
						missing.add("mastery." + m.group(1));
		}
		assertEquals(List.of(), missing, "config.yml is missing");
	}

	@Test
	void trainingTreeUnlocksEveryPathAndNothingElse() throws Exception {
		YamlConfiguration tree = yaml("training-tree.yml");
		java.util.Set<String> unlocks = new java.util.TreeSet<>();
		for (String node : tree.getKeys(false))
			for (java.util.Map<?, ?> effect : tree.getMapList(node + ".effects"))
				if (effect.get("unlock") != null)
					unlocks.add(String.valueOf(effect.get("unlock")));
		java.util.Set<String> expected = new java.util.TreeSet<>();
		for (Element element : Element.values()) {
			expected.add("elementalist:" + element.masteryUnlock());
			for (String path : element.paths().keySet())
				expected.add("elementalist:" + path);
		}
		assertEquals(expected, unlocks, "training-tree.yml unlocks");
	}

	@Test
	void eachElementsThreePathsExcludeEachOther() throws Exception {
		YamlConfiguration tree = yaml("training-tree.yml");
		// node -> its unlock; then for each element, its three path nodes must all exclude the other two.
		java.util.Map<String, String> nodeOfUnlock = new java.util.HashMap<>();
		for (String node : tree.getKeys(false))
			for (java.util.Map<?, ?> effect : tree.getMapList(node + ".effects"))
				if (effect.get("unlock") != null)
					nodeOfUnlock.put(String.valueOf(effect.get("unlock")), node);
		List<String> problems = new ArrayList<>();
		for (Element element : Element.values()) {
			List<String> group = new ArrayList<>();
			group.add(nodeOfUnlock.get("elementalist:" + element.masteryUnlock()));
			for (String path : element.paths().keySet())
				group.add(nodeOfUnlock.get("elementalist:" + path));
			for (String node : group)
				for (String other : group)
					if (!node.equals(other) && !tree.getStringList(node + ".excludes").contains(other))
						problems.add(node + " doesn't exclude " + other);
		}
		assertEquals(List.of(), problems);
	}

	@Test
	void wholeTreeFitsInGarrisonsDefaultFortyPoints() throws Exception {
		// Counting one path per element (the rest are excluded), as Garrison's validator does.
		YamlConfiguration tree = yaml("training-tree.yml");
		int total = 0;
		java.util.Set<String> counted = new java.util.HashSet<>();
		for (String node : tree.getKeys(false)) {
			if (counted.contains(node))
				continue;
			int cost = tree.getInt(node + ".cost", 1) * tree.getInt(node + ".ranks", 1);
			for (String other : tree.getStringList(node + ".excludes")) {
				counted.add(other);
				cost = Math.max(cost, tree.getInt(other + ".cost", 1) * tree.getInt(other + ".ranks", 1));
			}
			counted.add(node);
			total += cost;
		}
		assertTrue(total <= 40, "the tree costs " + total + " points to finish, more than Garrison's default 40");
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
