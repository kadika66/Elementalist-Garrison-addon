package xyz.unbent.elementalist;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import xyz.unbent.garrison.api.guard.Guard;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Something an Elementalist sends flying: moves a fixed distance each tick (in half-block steps, so it can't skip
 * through a target or a wall), stops at the first solid block, and hits the first of the guard's enemies it touches.
 * It passes through friends. It stops if the guard dies or unloads.
 */
final class Shot {

	private static final double STEP = 0.5;
	private static final double HIT_RADIUS = 0.8;

	private Shot() {}

	/**
	 * @param trail   drawn at the shot's position each tick
	 * @param visual  an entity (a rock) moved along with the shot and removed when it ends, or null
	 * @param onHit   the enemy it hit, and where
	 */
	static void fire(Casts casts, Guard guard, Mob caster, Location start, Vector direction, double speed, double range,
	                 Consumer<Location> trail, @Nullable Entity visual, BiConsumer<LivingEntity, Location> onHit) {
		Location position = start.clone();
		Vector heading = direction.clone().normalize();
		int steps = Math.max(1, (int) Math.ceil(speed / STEP));
		Vector step = heading.clone().multiply(speed / steps);
		double[] travelled = {0};

		casts.repeat(task -> {
			if (!caster.isValid() || position.getWorld() == null || !position.isChunkLoaded()) {
				end(casts, task, visual);
				return;
			}
			for (int i = 0; i < steps; i++) {
				position.add(step);
				travelled[0] += step.length();
				if (travelled[0] > range || !position.getBlock().isPassable()) {
					end(casts, task, visual);
					return;
				}
				for (LivingEntity entity : position.getWorld().getNearbyLivingEntities(position, HIT_RADIUS)) {
					if (entity.equals(caster) || entity.isDead() || !casts.garrison().isEnemy(guard, entity))
						continue;
					end(casts, task, visual);
					onHit.accept(entity, position.clone());
					return;
				}
			}
			trail.accept(position);
			if (visual != null && visual.isValid())
				visual.teleport(position);
		});
	}

	private static void end(Casts casts, org.bukkit.scheduler.BukkitTask task, @Nullable Entity visual) {
		task.cancel();
		if (visual != null)
			casts.remove(visual);
	}
}
