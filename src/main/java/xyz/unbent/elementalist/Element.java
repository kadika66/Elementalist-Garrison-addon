package xyz.unbent.elementalist;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** The four elements, in the order the menu button cycles through them. */
public enum Element {

	FIRE("Fire", NamedTextColor.RED, Material.BLAZE_POWDER, Ability.FIRE_BLAST, Ability.HEAT_WAVE),
	WATER("Water", NamedTextColor.AQUA, Material.HEART_OF_THE_SEA, Ability.WATER_WHIP, Ability.FROST),
	EARTH("Earth", NamedTextColor.GREEN, Material.MOSSY_COBBLESTONE, Ability.EARTH_BLAST, Ability.TREMOR),
	AIR("Air", NamedTextColor.GRAY, Material.FEATHER, Ability.AIR_SWIPE, Ability.AIR_BLAST);

	private final String display;
	private final TextColor colour;
	private final Material icon;
	private final Ability ranged;
	private final Ability close;

	Element(String display, TextColor colour, Material icon, Ability ranged, Ability close) {
		this.display = display;
		this.colour = colour;
		this.icon = icon;
		this.ranged = ranged;
		this.close = close;
	}

	public String display() { return display; }
	public TextColor colour() { return colour; }
	public Material icon() { return icon; }
	/** The ability it uses at range. */
	public Ability ranged() { return ranged; }
	/** The ability it prefers when the enemy is close. */
	public Ability close() { return close; }

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
