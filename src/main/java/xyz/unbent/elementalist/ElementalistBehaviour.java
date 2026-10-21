package xyz.unbent.elementalist;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;
import xyz.unbent.garrison.api.behaviour.GuardBehaviour;
import xyz.unbent.garrison.api.guard.Guard;
import xyz.unbent.garrison.api.menu.MenuButton;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code behaviour: elementalist}. Garrison picks the target and moves the guard as for any fighter; once a second
 * this decides whether to cast: the element's close-range ability if the enemy is near enough, otherwise its ranged
 * one, each on its own cooldown. The mob's own melee attack still works when it gets close.
 *
 * The element is saved on the guard ({@link #ELEMENT_KEY}), so it survives restarts and a fallen guard returning.
 */
public final class ElementalistBehaviour implements GuardBehaviour {

	public static final String ELEMENT_KEY = "elementalist:element";

	private final Casts casts;
	private final MenuButton elementButton;
	/** Guard id -> ability -> epoch millis when it is ready again. */
	private final Map<UUID, Map<Ability, Long>> readyAt = new HashMap<>();

	ElementalistBehaviour(Casts casts) {
		this.casts = casts;
		this.elementButton = new ElementButton(this);
	}

	/* Element */

	/** The guard's element. One that has none yet gets the configured default (or a random one), saved. */
	public Element elementOf(Guard guard) {
		Element element = Element.parse(guard.getData(ELEMENT_KEY));
		if (element == null) {
			Element configured = casts.settings().getDefaultElement();
			element = configured != null ? configured : Element.random();
			guard.setData(ELEMENT_KEY, element.name());
		}
		return element;
	}

	void setElement(Guard guard, Element element) {
		guard.setData(ELEMENT_KEY, element.name());
		// A new element starts fresh: no cooldowns carried over from the old one.
		readyAt.remove(guard.getId());
		Mob mob = guard.getEntity();
		if (mob != null)
			ambient(mob, element, 25);
	}

	/* GuardBehaviour */

	@Override
	public void onLoad(@NotNull Guard guard, @NotNull Mob mob) {
		elementOf(guard);
	}

	@Override
	public void tick(@NotNull Guard guard, @NotNull Mob mob) {
		Element element = elementOf(guard);
		if (casts.settings().isAmbientParticles())
			ambient(mob, element, 3);

		if (!(mob.getTarget() instanceof LivingEntity target) || !target.isValid() || target.isDead()
			|| !target.getWorld().equals(mob.getWorld()) || !casts.garrison().isEnemy(guard, target))
			return;
		double distance = mob.getEyeLocation().distance(target.getEyeLocation());
		if (tryCast(element.close(), guard, mob, target, distance))
			return;
		tryCast(element.ranged(), guard, mob, target, distance);
	}

	@Override
	public void onRemove(@NotNull Guard guard) {
		readyAt.remove(guard.getId());
	}

	@Override
	public @NotNull List<MenuButton> getMenuButtons(@NotNull Guard guard) {
		return List.of(elementButton);
	}

	/* Casting */

	private boolean tryCast(Ability ability, Guard guard, Mob mob, LivingEntity target, double distance) {
		if (distance > reach(ability))
			return false;
		long now = System.currentTimeMillis();
		Map<Ability, Long> cooldowns = readyAt.computeIfAbsent(guard.getId(), id -> new EnumMap<>(Ability.class));
		if (cooldowns.getOrDefault(ability, 0L) > now)
			return false;
		// Area abilities around the guard don't need sight of the target; aimed ones do.
		boolean aimed = ability != Ability.TREMOR && ability != Ability.FROST;
		if (aimed && !mob.hasLineOfSight(target))
			return false;
		if (!Abilities.cast(ability, casts, guard, mob, target))
			return false;
		cooldowns.put(ability, now + casts.settings().cooldownMillis(ability));
		return true;
	}

	/** How close the target must be to use this ability. */
	private double reach(Ability ability) {
		ElementalistSettings s = casts.settings();
		return switch (ability) {
			case TREMOR, FROST -> s.get(ability, "radius");
			default -> s.get(ability, "range");
		};
	}

	private static void ambient(Mob mob, Element element, int count) {
		World world = mob.getWorld();
		Location at = mob.getLocation().add(0, mob.getHeight() * 0.6, 0);
		switch (element) {
			case FIRE -> world.spawnParticle(Particle.SMALL_FLAME, at, count, 0.3, 0.4, 0.3, 0.01);
			case WATER -> world.spawnParticle(Particle.DRIPPING_WATER, at, count, 0.3, 0.4, 0.3, 0.0);
			case EARTH -> world.spawnParticle(Particle.FALLING_DUST, at, count, 0.3, 0.4, 0.3, 0.0, Material.DIRT.createBlockData());
			case AIR -> world.spawnParticle(Particle.CLOUD, at, Math.max(1, count / 2), 0.3, 0.4, 0.3, 0.0);
		}
	}
}
