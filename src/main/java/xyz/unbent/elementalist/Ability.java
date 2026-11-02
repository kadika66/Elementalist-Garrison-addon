package xyz.unbent.elementalist;

import org.jetbrains.annotations.Nullable;

/**
 * Every ability, with its section name under {@code abilities:} in config.yml and a line for the menu.
 * The eight base abilities were renamed in 1.1.0; their old section names ({@link #legacyKey}) are still read from a
 * server's config.yml that has them, so nobody's tuning is lost.
 */
public enum Ability {

	/* Base abilities: every Elementalist of the element has these. */
	CINDER_SHOT("cinder-shot", "fire-blast", "Cinder Shot", "ranged burst that sets its target alight"),
	SEARING_ARC("searing-arc", "heat-wave", "Searing Arc", "cone of flame at enemies close in front"),
	STONESHOT("stoneshot", "earth-blast", "Stoneshot", "thrown rock that knocks its target back"),
	QUAKE_STOMP("quake-stomp", "tremor", "Quake Stomp", "ground slam: stuns, then slows, enemies nearby"),
	GALE_SHOVE("gale-shove", "air-blast", "Gale Shove", "strong push that shoves an enemy away"),
	RAZOR_WIND("razor-wind", "air-swipe", "Razor Wind", "fast fan of light cuts at range"),
	TIDE_LASH("tide-lash", "water-whip", "Tide Lash", "ranged lash that pulls its target in"),
	FROSTBLOOM("frostbloom", "frost", "Frostbloom", "slows enemies nearby, heals itself and guards around it"),

	/* Path abilities: unlocked by a town's training, one path per element. */
	RETURNING_BLADE("returning-blade", null, "Returning Blade", "an iron blade thrown out and back, cutting enemies both ways"),
	MOLTEN_RING("molten-ring", null, "Molten Ring", "a ring of molten drips around an enemy that burns everyone inside"),
	CHAIN_SPARK("chain-spark", null, "Chain Spark", "a spark that jumps from enemy to enemy"),
	ASH_LANCE("ash-lance", null, "Ash Lance", "a smoke beam that bursts on impact, hurting enemies around it"),
	ICE_LANCE("ice-lance", null, "Ice Lance", "a block of ice that hits hard and slows"),
	ROSE_RAZOR("rose-razor", null, "Rose Razor", "a spinning rose flung like a blade"),
	CYCLONE("cyclone", null, "Cyclone", "lifts an enemy high and drops it"),
	VACUUM_SNARE("vacuum-snare", null, "Vacuum Snare", "drags the enemies around its target together and slows them");

	private final String key;
	@Nullable private final String legacyKey;
	private final String display;
	private final String description;

	Ability(String key, @Nullable String legacyKey, String display, String description) {
		this.key = key;
		this.legacyKey = legacyKey;
		this.display = display;
		this.description = description;
	}

	public String key() { return key; }
	/** The section name before 1.1.0, or null for abilities that are new since. */
	@Nullable public String legacyKey() { return legacyKey; }
	public String display() { return display; }
	public String description() { return description; }

	/** Area abilities around the guard: cast when an enemy is within "radius", and need no line of sight. */
	public boolean isArea() {
		return this == QUAKE_STOMP || this == FROSTBLOOM;
	}

	/** The config number that says how close the target must be: "radius" for area abilities, "range" for the rest. */
	public String reachKey() {
		return isArea() ? "radius" : "range";
	}

	/** One of the eight abilities every Elementalist has (not a path ability). */
	public boolean isBase() {
		return legacyKey != null;
	}
}
