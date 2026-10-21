package xyz.unbent.elementalist;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import xyz.unbent.garrison.api.Garrison;
import xyz.unbent.garrison.api.guard.Guard;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Everything abilities share: Garrison, the settings, and the running tasks and block displays, so that all of them
 * stop and vanish when the addon is disabled. Abilities never place or break blocks; rocks are block displays, which
 * are never saved with the world, so a crash can't leave one behind either.
 */
public final class Casts {

	public static final String ROCK_TAG = "elementalist_rock";

	private final Plugin plugin;
	private final Garrison garrison;
	private final ElementalistSettings settings;
	private final Set<BukkitTask> tasks = new HashSet<>();
	private final Set<Entity> displays = new HashSet<>();

	Casts(Plugin plugin, Garrison garrison, ElementalistSettings settings) {
		this.plugin = plugin;
		this.garrison = garrison;
		this.settings = settings;
	}

	public Garrison garrison() { return garrison; }
	public ElementalistSettings settings() { return settings; }

	/** Runs every tick until the task cancels itself. */
	public void repeat(Consumer<BukkitTask> body) {
		tasks.removeIf(BukkitTask::isCancelled);
		// runTaskTimer's Consumer<BukkitTask> overload returns void, so use the Runnable one and hand the task
		// over through a holder. The first run is a tick later, by when the holder is filled.
		BukkitTask[] holder = new BukkitTask[1];
		holder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> body.accept(holder[0]), 0L, 1L);
		tasks.add(holder[0]);
	}

	/** Runs once, later. */
	public void later(Runnable body, long ticks) {
		tasks.removeIf(BukkitTask::isCancelled);
		tasks.add(Bukkit.getScheduler().runTaskLater(plugin, body, Math.max(1L, ticks)));
	}

	/** A floating rock. Not saved with the chunk; removed by {@link #remove} or on disable. */
	public BlockDisplay spawnRock(Location at, Material material) {
		BlockDisplay rock = at.getWorld().spawn(at, BlockDisplay.class, display -> {
			display.setPersistent(false);
			display.addScoreboardTag(ROCK_TAG);
			display.setBlock(material.createBlockData());
			display.setTeleportDuration(1);
			display.setTransformation(new Transformation(new Vector3f(-0.3f, -0.3f, -0.3f), new AxisAngle4f(),
				new Vector3f(0.6f, 0.6f, 0.6f), new AxisAngle4f()));
		});
		displays.add(rock);
		return rock;
	}

	public void remove(Entity display) {
		displays.remove(display);
		display.remove();
	}

	/**
	 * Hurts an enemy on the guard's behalf. With 0 damage configured nothing is dealt, but it still answers whether
	 * the target is an enemy, so the ability's other effects (knockback, slowness) still apply.
	 */
	public boolean hurt(Guard guard, LivingEntity target, double damage) {
		if (damage <= 0)
			return garrison.isEnemy(guard, target);
		return garrison.damage(guard, target, damage);
	}

	/** Addon disabled: stop every ability mid-flight and take every rock away. */
	void shutdown() {
		for (BukkitTask task : tasks)
			task.cancel();
		tasks.clear();
		for (Entity display : displays)
			display.remove();
		displays.clear();
	}
}
