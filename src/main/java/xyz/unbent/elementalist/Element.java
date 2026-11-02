package xyz.unbent.elementalist;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The four elements, in the order the menu button cycles through them. Each has two base abilities and, through the
 * town's training (training-tree.yml), one of three paths: a mastery of the base abilities, or one of two path
 * abilities. Unlock ids are {@code elementalist:<id>}.
 */
public enum Element {

	FIRE("Fire", NamedTextColor.RED, Material.BLAZE_POWDER, Ability.CINDER_SHOT, Ability.SEARING_ARC,
		"fire-blue", "fire-storm", Ability.CHAIN_SPARK, "fire-ash", Ability.ASH_LANCE),
	WATER("Water", NamedTextColor.AQUA, Material.HEART_OF_THE_SEA, Ability.TIDE_LASH, Ability.FROSTBLOOM,
		"water-tide", "water-ice", Ability.ICE_LANCE, "water-bloom", Ability.ROSE_RAZOR),
	EARTH("Earth", NamedTextColor.GREEN, Material.MOSSY_COBBLESTONE, Ability.STONESHOT, Ability.QUAKE_STOMP,
		"earth-stone", "earth-iron", Ability.RETURNING_BLADE, "earth-magma", Ability.MOLTEN_RING),
	AIR("Air", NamedTextColor.GRAY, Material.FEATHER, Ability.RAZOR_WIND, Ability.GALE_SHOVE,
		"air-tempest", "air-cyclone", Ability.CYCLONE, "air-vacuum", Ability.VACUUM_SNARE);

	private final String display;
	private final TextColor colour;
	private final Material icon;
	private final Ability ranged;
	private final Ability close;
	private final String masteryUnlock;
	private final Map<String, Ability> paths = new LinkedHashMap<>();

	Element(String display, TextColor colour, Material icon, Ability ranged, Ability close,
	        String masteryUnlock, String pathA, Ability abilityA, String pathB, Ability abilityB) {
		this.display = display;
		this.colour = colour;
		this.icon = icon;
		this.ranged = ranged;
		this.close = close;
		this.masteryUnlock = masteryUnlock;
		paths.put(pathA, abilityA);
		paths.put(pathB, abilityB);
	}

	public String display() { return display; }
	/** Lower case, as used in training settings: {@code elementalist:<key>-damage}. */
	public String key() { return name().toLowerCase(Locale.ROOT); }
	public TextColor colour() { return colour; }
	public Material icon() { return icon; }
	/** The ability it uses at range. */
	public Ability ranged() { return ranged; }
	/** The ability it prefers when the enemy is close. */
	public Ability close() { return close; }
	/** The path that strengthens the two base abilities instead of adding one (unlock id without the prefix). */
	public String masteryUnlock() { return masteryUnlock; }
	/** The two path abilities, by unlock id (without the prefix). */
	public Map<String, Ability> paths() { return paths; }

	/** The element an ability belongs to. */
	public static Element of(Ability ability) {
		for (Element element : values())
			if (element.ranged == ability || element.close == ability || element.paths.containsValue(ability))
				return element;
		throw new IllegalArgumentException("No element has " + ability);
	}

	public Element next() {
		return values()[(ordinal() + 1) % values().length];
	}

	public Element previous() {
		return values()[(ordinal() + values().length - 1) % values().length];
	}

	@Nullable
	public static Element parse(@Nullable String text) {
		if (text == null)
			return null;
		try {
			return valueOf(text.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static Element random() {
		return values()[ThreadLocalRandom.current().nextInt(values().length)];
	}
}
