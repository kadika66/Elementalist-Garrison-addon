package xyz.unbent.elementalist;

/** Every ability, with its section name under {@code abilities:} in config.yml and a line for the menu. */
public enum Ability {

	FIRE_BLAST("fire-blast", "Fire Blast", "ranged burst that sets its target alight"),
	HEAT_WAVE("heat-wave", "Heat Wave", "cone of flame at enemies close in front"),
	EARTH_BLAST("earth-blast", "Earth Blast", "thrown rock that knocks its target back"),
	TREMOR("tremor", "Tremor", "ground slam: stuns, then slows, enemies nearby"),
	AIR_BLAST("air-blast", "Air Blast", "strong push that shoves an enemy away"),
	AIR_SWIPE("air-swipe", "Air Swipe", "fast fan of light cuts at range"),
	WATER_WHIP("water-whip", "Water Whip", "ranged lash that pulls its target in"),
	FROST("frost", "Frost", "slows enemies nearby, heals itself and guards around it");

	private final String key;
	private final String display;
	private final String description;

	Ability(String key, String display, String description) {
		this.key = key;
		this.display = display;
		this.description = description;
	}

	public String key() { return key; }
	public String display() { return display; }
	public String description() { return description; }
}
