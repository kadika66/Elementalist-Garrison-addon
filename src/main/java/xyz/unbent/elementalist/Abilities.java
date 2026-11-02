package xyz.unbent.elementalist;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import xyz.unbent.garrison.api.guard.Guard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Every ability, written for mobs (a guard aiming at its current target), not ported from any other plugin.
 * None of them place, break or ignite blocks, and every hit goes through Garrison ({@link Casts#hurt}), so only the
 * guard's enemies are hurt and friendly-fire, kill credit, outlaw and bounty rules apply.
 *
 * Damage and range follow the town's training: {@link Casts#power} (attunement, mastery) and {@link Casts#reach}.
 */
final class Abilities {

	private static final Particle.DustOptions WATER_DUST = new Particle.DustOptions(Color.fromRGB(40, 110, 255), 1.3f);
	private static final Particle.DustOptions FROST_DUST = new Particle.DustOptions(Color.fromRGB(200, 235, 255), 1.0f);
	private static final Particle.DustOptions SPARK_DUST = new Particle.DustOptions(Color.fromRGB(120, 190, 255), 0.9f);
	private static final Particle.DustOptions PETAL_DUST = new Particle.DustOptions(Color.fromRGB(200, 20, 40), 1.0f);

	private Abilities() {}

	/** Casts an ability at the guard's target. Returns false if it couldn't (e.g. quake stomp while airborne). */
	static boolean cast(Ability ability, Casts casts, Guard guard, Mob caster, LivingEntity target) {
		return switch (ability) {
			case CINDER_SHOT -> cinderShot(casts, guard, caster, target);
			case SEARING_ARC -> searingArc(casts, guard, caster, target);
			case STONESHOT -> stoneshot(casts, guard, caster, target);
			case QUAKE_STOMP -> quakeStomp(casts, guard, caster);
			case GALE_SHOVE -> galeShove(casts, guard, caster, target);
			case RAZOR_WIND -> razorWind(casts, guard, caster, target);
			case TIDE_LASH -> tideLash(casts, guard, caster, target);
			case FROSTBLOOM -> frostbloom(casts, guard, caster);
			case RETURNING_BLADE -> returningBlade(casts, guard, caster, target);
			case MOLTEN_RING -> moltenRing(casts, guard, caster, target);
			case CHAIN_SPARK -> chainSpark(casts, guard, caster, target);
			case ASH_LANCE -> ashLance(casts, guard, caster, target);
			case ICE_LANCE -> iceLance(casts, guard, caster, target);
			case ROSE_RAZOR -> roseRazor(casts, guard, caster, target);
			case CYCLONE -> cyclone(casts, guard, caster, target);
			case VACUUM_SNARE -> vacuumSnare(casts, guard, caster, target);
		};
	}

	/* Fire */

	private static boolean cinderShot(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.CINDER_SHOT;
		// Fire's mastery path burns blue.
		Particle flame = casts.has(guard, Element.FIRE.masteryUnlock()) ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
		double damage = s.get(a, "damage") * casts.power(guard, a);
		Location start = launchPoint(caster, target);
		World world = caster.getWorld();
		world.playSound(start, Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.2f);
		Shot.fire(casts, guard, caster, start, aim(start, target), s.get(a, "speed"), casts.reach(guard, s.get(a, "range")),
			at -> {
				world.spawnParticle(flame, at, 4, 0.08, 0.08, 0.08, 0.01);
				world.spawnParticle(Particle.SMOKE, at, 1, 0.05, 0.05, 0.05, 0.0);
			},
			null,
			(hit, at) -> {
				if (casts.hurt(guard, hit, damage))
					hit.setFireTicks(Math.max(hit.getFireTicks(), s.ticks(a, "burn-seconds")));
				world.spawnParticle(flame, at, 20, 0.3, 0.3, 0.3, 0.05);
				world.playSound(at, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 1.4f);
			});
		return true;
	}

	private static boolean searingArc(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.SEARING_ARC;
		Particle flame = casts.has(guard, Element.FIRE.masteryUnlock()) ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
		double range = casts.reach(guard, s.get(a, "range"));
		double halfAngle = Math.toRadians(s.get(a, "angle") / 2);
		double damage = s.get(a, "damage") * casts.power(guard, a);
		Location eye = caster.getEyeLocation();
		Vector forward = aim(eye, target);
		World world = caster.getWorld();
		world.playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);

		// Flames fanned across the cone.
		for (int ray = -3; ray <= 3; ray++) {
			Vector direction = forward.clone().rotateAroundY(halfAngle * ray / 3.0);
			for (double d = 0.8; d <= range; d += 0.6)
				world.spawnParticle(flame, eye.clone().add(direction.clone().multiply(d)), 1, 0.1, 0.1, 0.1, 0.02);
		}
		for (LivingEntity entity : world.getNearbyLivingEntities(eye, range)) {
			if (entity.equals(caster) || entity.isDead())
				continue;
			Vector to = centre(entity).toVector().subtract(eye.toVector());
			if (to.lengthSquared() > range * range || to.lengthSquared() < 1e-6 || forward.angle(to) > halfAngle)
				continue;
			if (!caster.hasLineOfSight(entity))
				continue;
			if (casts.hurt(guard, entity, damage))
				entity.setFireTicks(Math.max(entity.getFireTicks(), s.ticks(a, "burn-seconds")));
		}
		return true;
	}

	/** Storm path: a jagged blue spark that jumps from enemy to enemy, weaker with each jump. Never a real lightning bolt. */
	private static boolean chainSpark(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.CHAIN_SPARK;
		double damage = s.get(a, "damage") * casts.power(guard, a);
		int targets = Math.max(1, (int) s.get(a, "targets"));
		double jumpRange = s.get(a, "jump-range");
		double falloff = s.get(a, "falloff");
		World world = caster.getWorld();
		world.playSound(caster.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.8f);

		Set<UUID> struck = new HashSet<>();
		Location from = caster.getEyeLocation();
		LivingEntity current = target;
		for (int i = 0; i < targets && current != null; i++) {
			Location to = centre(current);
			zigzag(world, from, to);
			casts.hurt(guard, current, damage * Math.pow(falloff, i));
			world.spawnParticle(Particle.ELECTRIC_SPARK, to, 12, 0.3, 0.4, 0.3, 0.05);
			struck.add(current.getUniqueId());
			from = to;
			current = nearestEnemy(casts, guard, caster, current, jumpRange, struck);
		}
		return true;
	}

	/** Ash path: a tight smoke beam; where it stops (an enemy or a wall) it bursts, hurting enemies around. Breaks nothing. */
	private static boolean ashLance(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.ASH_LANCE;
		double power = casts.power(guard, a);
		double range = casts.reach(guard, s.get(a, "range"));
		double burstRadius = s.get(a, "burst-radius");
		World world = caster.getWorld();
		Location point = caster.getEyeLocation();
		Vector step = aim(point, target).multiply(0.4);
		LivingEntity hit = null;
		world.playSound(point, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.5f);

		// Instant beam: walk it out until it meets an enemy or a solid block.
		for (double travelled = 0; travelled < range; travelled += 0.4) {
			point.add(step);
			if (!point.getBlock().isPassable())
				break;
			world.spawnParticle(Particle.SMOKE, point, 2, 0.04, 0.04, 0.04, 0.0);
			hit = firstEnemyAt(casts, guard, caster, point, 0.8);
			if (hit != null)
				break;
		}
		if (hit != null)
			casts.hurt(guard, hit, s.get(a, "damage") * power);
		// The burst: only particles, a sound and damage to the guard's enemies. Never a real explosion.
		world.spawnParticle(Particle.EXPLOSION, point, 3, 0.4, 0.4, 0.4, 0.0);
		world.spawnParticle(Particle.LARGE_SMOKE, point, 25, burstRadius / 3, 0.5, burstRadius / 3, 0.02);
		world.playSound(point, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.3f);
		double burst = s.get(a, "burst-damage") * power;
		for (LivingEntity entity : world.getNearbyLivingEntities(point, burstRadius)) {
			if (entity.equals(caster) || entity.equals(hit) || entity.isDead())
				continue;
			casts.hurt(guard, entity, burst);
		}
		return true;
	}

	/* Earth */

	private static boolean stoneshot(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		throwStone(casts, guard, caster, target);
		// Stone path (mastery): a second stone at a second enemy.
		if (casts.has(guard, Element.EARTH.masteryUnlock())) {
			LivingEntity second = nearestEnemy(casts, guard, caster, caster, casts.reach(guard, casts.settings().mastery("second-target-range")), Set.of(target.getUniqueId()));
			if (second != null)
				throwStone(casts, guard, caster, second);
		}
		return true;
	}

	private static void throwStone(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.STONESHOT;
		double damage = s.get(a, "damage") * casts.power(guard, a);
		Location start = launchPoint(caster, target);
		World world = caster.getWorld();
		Material material = groundMaterial(caster);
		BlockData dust = material.createBlockData();
		BlockDisplay rock = casts.spawnRock(start, material);
		world.playSound(start, Sound.BLOCK_STONE_BREAK, 1.0f, 0.7f);
		Shot.fire(casts, guard, caster, start, aim(start, target), s.get(a, "speed"), casts.reach(guard, s.get(a, "range")),
			at -> world.spawnParticle(Particle.BLOCK, at, 2, 0.1, 0.1, 0.1, 0.0, dust),
			rock,
			(hit, at) -> {
				if (casts.hurt(guard, hit, damage))
					push(hit, caster, s.get(a, "knockback"), 0.35);
				world.spawnParticle(Particle.BLOCK, at, 25, 0.3, 0.3, 0.3, 0.0, dust);
				world.playSound(at, Sound.BLOCK_STONE_BREAK, 1.0f, 0.9f);
			});
	}

	private static boolean quakeStomp(Casts casts, Guard guard, Mob caster) {
		if (!caster.isOnGround())
			return false;
		ElementalistSettings s = casts.settings();
		Ability a = Ability.QUAKE_STOMP;
		double radius = s.get(a, "radius");
		double damage = s.get(a, "damage") * casts.power(guard, a);
		Location centre = caster.getLocation();
		World world = caster.getWorld();
		BlockData dust = groundMaterial(caster).createBlockData();
		world.playSound(centre, Sound.BLOCK_STONE_BREAK, 1.2f, 0.5f);
		world.playSound(centre, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.6f);
		// A ring of dust running outward over three ticks.
		for (int wave = 1; wave <= 3; wave++) {
			double r = radius * wave / 3.0;
			int points = 10 * wave;
			casts.later(() -> {
				for (int i = 0; i < points; i++) {
					double angle = 2 * Math.PI * i / points;
					Location at = centre.clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
					world.spawnParticle(Particle.BLOCK, at, 2, 0.1, 0.05, 0.1, 0.0, dust);
				}
			}, wave);
		}
		int stun = s.ticks(a, "stun-seconds");
		int slow = s.ticks(a, "slow-seconds");
		for (LivingEntity entity : world.getNearbyLivingEntities(centre, radius, 2.5)) {
			if (entity.equals(caster) || entity.isDead() || entity.getLocation().distanceSquared(centre) > radius * radius)
				continue;
			if (!casts.hurt(guard, entity, damage))
				continue;
			// Stunned (barely able to move), then slowed.
			if (stun > 0)
				entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, stun, 6, false, true, true));
			if (slow > 0)
				casts.later(() -> {
					if (entity.isValid() && !entity.isDead())
						entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slow, 1, false, true, true));
				}, Math.max(1, stun));
		}
		return true;
	}

	/** Iron path: an iron sword thrown out to the target and back, cutting each enemy it passes once each way. */
	private static boolean returningBlade(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.RETURNING_BLADE;
		double damage = s.get(a, "damage") * casts.power(guard, a);
		double speed = Math.max(0.2, s.get(a, "speed"));
		Location start = launchPoint(caster, target);
		// Out to just past where the target stands now, never further than the ability's range.
		double outward = Math.min(casts.reach(guard, s.get(a, "range")), start.distance(centre(target)) + 1.5);
		Vector heading = aim(start, target);
		World world = caster.getWorld();
		ItemDisplay blade = casts.spawnItem(start, Material.IRON_SWORD);
		world.playSound(start, Sound.ITEM_TRIDENT_THROW, 0.9f, 1.3f);

		Location position = start.clone();
		Set<UUID> hitOut = new HashSet<>();
		Set<UUID> hitBack = new HashSet<>();
		double[] travelled = {0};
		boolean[] returning = {false};
		int[] age = {0};
		casts.repeat(task -> {
			age[0]++;
			// Gone, unloaded, or flying too long (stuck behind something on the way back): it just vanishes.
			if (!caster.isValid() || !blade.isValid() || !position.isChunkLoaded() || age[0] > 100) {
				task.cancel();
				casts.remove(blade);
				return;
			}
			Vector step;
			if (!returning[0]) {
				step = heading.clone().multiply(speed);
				travelled[0] += speed;
				if (travelled[0] >= outward || !position.clone().add(step).getBlock().isPassable())
					returning[0] = true;
			} else {
				Location home = caster.getEyeLocation();
				Vector back = home.toVector().subtract(position.toVector());
				if (back.lengthSquared() <= speed * speed) {
					task.cancel();
					casts.remove(blade);
					world.playSound(home, Sound.ITEM_ARMOR_EQUIP_IRON, 0.6f, 1.4f);
					return;
				}
				step = back.normalize().multiply(speed);
			}
			position.add(step);
			blade.teleport(position);
			Casts.spin(blade, 0.9f);
			world.spawnParticle(Particle.CRIT, position, 1, 0.05, 0.05, 0.05, 0.0);
			Set<UUID> already = returning[0] ? hitBack : hitOut;
			for (LivingEntity entity : world.getNearbyLivingEntities(position, 0.9)) {
				if (entity.equals(caster) || entity.isDead() || already.contains(entity.getUniqueId()))
					continue;
				if (casts.hurt(guard, entity, damage)) {
					already.add(entity.getUniqueId());
					world.playSound(position, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.6f);
				}
			}
		});
		return true;
	}

	/** Magma path: a ring of molten drips around the target that burns every enemy inside for a few seconds. No blocks. */
	private static boolean moltenRing(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.MOLTEN_RING;
		double radius = s.get(a, "radius");
		double damage = s.get(a, "damage") * casts.power(guard, a);
		int lifetime = s.ticks(a, "seconds");
		int burn = s.ticks(a, "burn-seconds");
		Location centre = target.getLocation().clone();
		World world = caster.getWorld();
		world.playSound(centre, Sound.BLOCK_LAVA_POP, 1.0f, 0.8f);
		world.playSound(centre, Sound.ITEM_BUCKET_EMPTY_LAVA, 0.8f, 1.0f);
		int[] age = {0};
		casts.repeat(task -> {
			if (!caster.isValid() || age[0] > lifetime || !centre.isChunkLoaded()) {
				task.cancel();
				return;
			}
			if (age[0] % 3 == 0) {
				int points = (int) Math.max(12, radius * 8);
				for (int i = 0; i < points; i++) {
					double angle = 2 * Math.PI * i / points;
					Location at = centre.clone().add(Math.cos(angle) * radius, 1.6, Math.sin(angle) * radius);
					world.spawnParticle(Particle.DRIPPING_LAVA, at, 1, 0.05, 0.1, 0.05, 0.0);
				}
				world.spawnParticle(Particle.LAVA, centre.clone().add(0, 0.2, 0), 2, radius / 2, 0.05, radius / 2, 0.0);
				world.spawnParticle(Particle.LANDING_LAVA, centre.clone().add(0, 0.1, 0), 4, radius / 2, 0.0, radius / 2, 0.0);
			}
			// Once a second, everyone inside burns.
			if (age[0] % 20 == 0)
				for (LivingEntity entity : world.getNearbyLivingEntities(centre, radius, 2.5)) {
					if (entity.equals(caster) || entity.isDead())
						continue;
					Location at = entity.getLocation();
					double dx = at.getX() - centre.getX(), dz = at.getZ() - centre.getZ();
					if (dx * dx + dz * dz > radius * radius)
						continue;
					if (casts.hurt(guard, entity, damage) && burn > 0)
						entity.setFireTicks(Math.max(entity.getFireTicks(), burn));
				}
			age[0]++;
		});
		return true;
	}

	/* Air */

	private static boolean galeShove(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.GALE_SHOVE;
		double damage = s.get(a, "damage") * casts.power(guard, a);
		Location start = launchPoint(caster, target);
		World world = caster.getWorld();
		world.playSound(start, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.6f);
		Shot.fire(casts, guard, caster, start, aim(start, target), s.get(a, "speed"), casts.reach(guard, s.get(a, "range")),
			at -> world.spawnParticle(Particle.CLOUD, at, 3, 0.15, 0.15, 0.15, 0.01),
			null,
			(hit, at) -> {
				if (casts.hurt(guard, hit, damage))
					push(hit, caster, s.get(a, "knockback"), 0.5);
				world.spawnParticle(Particle.CLOUD, at, 15, 0.3, 0.3, 0.3, 0.08);
				world.playSound(at, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.2f);
			});
		return true;
	}

	private static boolean razorWind(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.RAZOR_WIND;
		double damage = s.get(a, "damage") * casts.power(guard, a);
		Location start = launchPoint(caster, target);
		Vector forward = aim(start, target);
		World world = caster.getWorld();
		int streams = Math.max(1, (int) s.get(a, "streams"));
		// Tempest path (mastery): more blades in the fan.
		if (casts.has(guard, Element.AIR.masteryUnlock()))
			streams += (int) casts.settings().mastery("extra-blades");
		double spread = Math.toRadians(s.get(a, "spread"));
		world.playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
		for (int i = 0; i < streams; i++) {
			// Evenly across [-spread, +spread]; a single stream goes straight.
			double offset = streams == 1 ? 0 : -spread + 2 * spread * i / (streams - 1);
			Shot.fire(casts, guard, caster, start, forward.clone().rotateAroundY(offset), s.get(a, "speed"), casts.reach(guard, s.get(a, "range")),
				at -> world.spawnParticle(Particle.CLOUD, at, 1, 0.02, 0.02, 0.02, 0.0),
				null,
				(hit, at) -> {
					casts.hurt(guard, hit, damage);
					world.spawnParticle(Particle.SWEEP_ATTACK, at, 1, 0, 0, 0, 0);
				});
		}
		return true;
	}

	/** Cyclone path: a whirl of wind around the target, then it's thrown straight up; the fall does the rest. */
	private static boolean cyclone(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.CYCLONE;
		double damage = s.get(a, "damage") * casts.power(guard, a);
		double lift = s.get(a, "lift");
		int windup = Math.max(1, s.ticks(a, "windup-seconds"));
		World world = caster.getWorld();
		world.playSound(target.getLocation(), Sound.ENTITY_BREEZE_IDLE_GROUND, 1.0f, 0.8f);
		int[] age = {0};
		casts.repeat(task -> {
			if (!target.isValid() || target.isDead() || !caster.isValid()) {
				task.cancel();
				return;
			}
			Location base = target.getLocation();
			double turn = age[0] * 0.6;
			for (int i = 0; i < 3; i++) {
				double angle = turn + i * 2 * Math.PI / 3;
				double height = (age[0] % 10) * 0.25;
				world.spawnParticle(Particle.CLOUD, base.clone().add(Math.cos(angle) * 0.9, height, Math.sin(angle) * 0.9), 1, 0, 0, 0, 0);
			}
			if (age[0]++ < windup)
				return;
			task.cancel();
			// Still an enemy after the windup (it may have become a friend, or left the fight): up it goes.
			if (casts.hurt(guard, target, damage)) {
				Vector velocity = target.getVelocity().setX(0).setZ(0).setY(lift);
				target.setVelocity(velocity);
				world.spawnParticle(Particle.GUST, base, 2, 0.2, 0.2, 0.2, 0.0);
				world.playSound(base, Sound.ENTITY_BREEZE_JUMP, 1.0f, 0.9f);
			}
		});
		return true;
	}

	/** Vacuum path: every enemy near the target is dragged together onto it and slowed, ready for the rest of the garrison. */
	private static boolean vacuumSnare(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.VACUUM_SNARE;
		double radius = s.get(a, "radius");
		double pull = s.get(a, "pull");
		double damage = s.get(a, "damage") * casts.power(guard, a);
		int slow = s.ticks(a, "slow-seconds");
		Location centre = target.getLocation().clone();
		World world = caster.getWorld();
		world.playSound(centre, Sound.ENTITY_BREEZE_INHALE, 1.0f, 0.7f);
		// Air rushing in: particles given a velocity towards the centre (count 0 makes the offset a direction).
		for (int i = 0; i < 40; i++) {
			double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
			double distance = radius * (0.5 + ThreadLocalRandom.current().nextDouble() * 0.5);
			Location at = centre.clone().add(Math.cos(angle) * distance, 0.5 + ThreadLocalRandom.current().nextDouble(), Math.sin(angle) * distance);
			Vector in = centre.toVector().subtract(at.toVector()).multiply(0.15);
			world.spawnParticle(Particle.CLOUD, at, 0, in.getX(), in.getY(), in.getZ(), 1.0);
		}
		for (LivingEntity entity : world.getNearbyLivingEntities(centre, radius)) {
			if (entity.equals(caster) || entity.isDead())
				continue;
			if (!casts.hurt(guard, entity, damage))
				continue;
			Vector in = centre.toVector().subtract(entity.getLocation().toVector()).setY(0);
			if (in.lengthSquared() > 0.25)
				entity.setVelocity(in.normalize().multiply(pull * Math.min(1.0, Math.sqrt(in.lengthSquared()) / radius + 0.3)).setY(0.15));
			if (slow > 0)
				entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slow, 1, false, true, true));
		}
		return true;
	}

	/* Water */

	private static boolean tideLash(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		lash(casts, guard, caster, target);
		// Tide path (mastery): a second lash at a second enemy.
		if (casts.has(guard, Element.WATER.masteryUnlock())) {
			LivingEntity second = nearestEnemy(casts, guard, caster, caster, casts.reach(guard, casts.settings().mastery("second-target-range")), Set.of(target.getUniqueId()));
			if (second != null)
				lash(casts, guard, caster, second);
		}
		return true;
	}

	private static void lash(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.TIDE_LASH;
		double damage = s.get(a, "damage") * casts.power(guard, a);
		Location start = launchPoint(caster, target);
		World world = caster.getWorld();
		world.playSound(start, Sound.ENTITY_GENERIC_SPLASH, 0.8f, 1.3f);
		Shot.fire(casts, guard, caster, start, aim(start, target), s.get(a, "speed"), casts.reach(guard, s.get(a, "range")),
			at -> {
				world.spawnParticle(Particle.DUST, at, 3, 0.08, 0.08, 0.08, 0.0, WATER_DUST);
				world.spawnParticle(Particle.SPLASH, at, 2, 0.1, 0.1, 0.1, 0.0);
			},
			null,
			(hit, at) -> {
				if (casts.hurt(guard, hit, damage))
					// Towards the guard: a negative push.
					push(hit, caster, -s.get(a, "pull"), 0.3);
				world.spawnParticle(Particle.SPLASH, at, 25, 0.3, 0.3, 0.3, 0.1);
				world.playSound(at, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.7f, 1.2f);
			});
	}

	private static boolean frostbloom(Casts casts, Guard guard, Mob caster) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.FROSTBLOOM;
		double radius = s.get(a, "radius");
		double damage = s.get(a, "damage") * casts.power(guard, a);
		Location centre = caster.getLocation().add(0, 1, 0);
		World world = caster.getWorld();
		world.playSound(centre, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.6f);
		world.playSound(centre, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.0f);
		int points = 32;
		for (int i = 0; i < points; i++) {
			double angle = 2 * Math.PI * i / points;
			Location at = centre.clone().add(Math.cos(angle) * radius, -0.5, Math.sin(angle) * radius);
			world.spawnParticle(Particle.SNOWFLAKE, at, 2, 0.1, 0.2, 0.1, 0.01);
		}
		world.spawnParticle(Particle.DUST, centre, 30, radius / 3, 0.5, radius / 3, 0.0, FROST_DUST);

		int slow = s.ticks(a, "slow-seconds");
		double heal = s.get(a, "heal");
		for (LivingEntity entity : world.getNearbyLivingEntities(centre, radius)) {
			if (entity.isDead() || entity.getLocation().distanceSquared(caster.getLocation()) > radius * radius)
				continue;
			if (entity.equals(caster) || isSameTownGuard(casts, guard, entity)) {
				heal(entity, heal);
				continue;
			}
			if (!casts.hurt(guard, entity, damage))
				continue;
			if (slow > 0) {
				entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slow, 1, false, true, true));
				entity.setFreezeTicks(Math.max(entity.getFreezeTicks(), Math.min(140, slow)));
			}
		}
		return true;
	}

	/** Ice path: a block of ice that hits hard and leaves its target slowed and frosted. */
	private static boolean iceLance(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.ICE_LANCE;
		double damage = s.get(a, "damage") * casts.power(guard, a);
		int slow = s.ticks(a, "slow-seconds");
		int level = Math.max(1, (int) s.get(a, "slow-level"));
		Location start = launchPoint(caster, target);
		World world = caster.getWorld();
		BlockDisplay ice = casts.spawnRock(start, Material.ICE);
		BlockData shards = Material.ICE.createBlockData();
		world.playSound(start, Sound.BLOCK_GLASS_PLACE, 1.0f, 1.4f);
		Shot.fire(casts, guard, caster, start, aim(start, target), s.get(a, "speed"), casts.reach(guard, s.get(a, "range")),
			at -> world.spawnParticle(Particle.SNOWFLAKE, at, 2, 0.05, 0.05, 0.05, 0.0),
			ice,
			(hit, at) -> {
				if (casts.hurt(guard, hit, damage) && slow > 0) {
					hit.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slow, level - 1, false, true, true));
					hit.setFreezeTicks(Math.max(hit.getFreezeTicks(), Math.min(140, slow)));
				}
				world.spawnParticle(Particle.BLOCK, at, 25, 0.3, 0.3, 0.3, 0.0, shards);
				world.playSound(at, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.2f);
			});
		return true;
	}

	/** Bloom path: the water drawn out of a rose, the rose flung spinning like a blade, petals in its wake. */
	private static boolean roseRazor(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.ROSE_RAZOR;
		double damage = s.get(a, "damage") * casts.power(guard, a);
		Location start = launchPoint(caster, target);
		World world = caster.getWorld();
		ItemDisplay rose = casts.spawnItem(start, Material.POPPY);
		world.playSound(start, Sound.BLOCK_AZALEA_LEAVES_BREAK, 1.0f, 1.5f);
		Shot.fire(casts, guard, caster, start, aim(start, target), s.get(a, "speed"), casts.reach(guard, s.get(a, "range")),
			at -> {
				world.spawnParticle(Particle.DUST, at, 2, 0.08, 0.08, 0.08, 0.0, PETAL_DUST);
				Casts.spin(rose, 1.2f);
			},
			rose,
			(hit, at) -> {
				casts.hurt(guard, hit, damage);
				world.spawnParticle(Particle.DUST, at, 20, 0.3, 0.3, 0.3, 0.0, PETAL_DUST);
				world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.8f);
			});
		return true;
	}

	/* Helpers */

	/** Just in front of the guard's eyes, towards the target. */
	private static Location launchPoint(Mob caster, LivingEntity target) {
		Location eye = caster.getEyeLocation();
		return eye.add(aim(eye, target).multiply(0.6));
	}

	/** From a point to the middle of the target. */
	private static Vector aim(Location from, LivingEntity target) {
		Vector direction = centre(target).toVector().subtract(from.toVector());
		return direction.lengthSquared() < 1e-6 ? from.getDirection() : direction.normalize();
	}

	private static Location centre(LivingEntity entity) {
		return entity.getLocation().add(0, entity.getHeight() / 2, 0);
	}

	/** The nearest of the guard's enemies within range of {@code around}, in its sight, not one of {@code skip}. */
	@Nullable
	private static LivingEntity nearestEnemy(Casts casts, Guard guard, Mob caster, LivingEntity around, double range, Set<UUID> skip) {
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (LivingEntity entity : around.getWorld().getNearbyLivingEntities(around.getLocation(), range)) {
			if (entity.equals(caster) || entity.equals(around) || entity.isDead() || skip.contains(entity.getUniqueId()))
				continue;
			double distance = entity.getLocation().distanceSquared(around.getLocation());
			if (distance >= bestDistance || !casts.garrison().isEnemy(guard, entity) || !around.hasLineOfSight(entity))
				continue;
			best = entity;
			bestDistance = distance;
		}
		return best;
	}

	@Nullable
	private static LivingEntity firstEnemyAt(Casts casts, Guard guard, Mob caster, Location point, double radius) {
		for (LivingEntity entity : point.getWorld().getNearbyLivingEntities(point, radius))
			if (!entity.equals(caster) && !entity.isDead() && casts.garrison().isEnemy(guard, entity))
				return entity;
		return null;
	}

	/** A jagged line of spark particles between two points. */
	private static void zigzag(World world, Location from, Location to) {
		Vector line = to.toVector().subtract(from.toVector());
		double length = line.length();
		if (length < 0.1)
			return;
		Vector direction = line.clone().normalize();
		// Any vector across the line, for the jags.
		Vector across = direction.clone().crossProduct(Math.abs(direction.getY()) < 0.9 ? new Vector(0, 1, 0) : new Vector(1, 0, 0)).normalize();
		List<Location> joints = new ArrayList<>();
		int segments = Math.max(2, (int) (length / 1.2));
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0; i <= segments; i++) {
			Location joint = from.clone().add(direction.clone().multiply(length * i / segments));
			if (i > 0 && i < segments)
				joint.add(across.clone().rotateAroundAxis(direction, random.nextDouble(Math.PI * 2)).multiply(random.nextDouble(0.2, 0.5)));
			joints.add(joint);
		}
		for (int i = 0; i < joints.size() - 1; i++) {
			Location a = joints.get(i), b = joints.get(i + 1);
			Vector step = b.toVector().subtract(a.toVector());
			int points = Math.max(2, (int) (step.length() * 5));
			step.multiply(1.0 / points);
			Location point = a.clone();
			for (int p = 0; p < points; p++) {
				world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, SPARK_DUST);
				point.add(step);
			}
		}
	}

	/** Sets the target moving away from the guard (towards it if strength is negative), with a little lift. */
	private static void push(LivingEntity target, Mob caster, double strength, double lift) {
		if (strength == 0)
			return;
		Vector away = target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0);
		if (away.lengthSquared() < 1e-4)
			away = caster.getLocation().getDirection().setY(0);
		if (away.lengthSquared() < 1e-4)
			return;
		away.normalize().multiply(strength).setY(lift);
		target.setVelocity(away);
	}

	/** The block the guard stands on, if it makes a decent rock; cobblestone otherwise. */
	private static Material groundMaterial(Mob caster) {
		Block below = caster.getLocation().subtract(0, 0.1, 0).getBlock();
		Material type = below.getType();
		return type.isBlock() && type.isSolid() && type.isOccluding() ? type : Material.COBBLESTONE;
	}

	private static boolean isSameTownGuard(Casts casts, Guard guard, LivingEntity entity) {
		Guard other = casts.garrison().getGuard(entity);
		return other != null && other.getTownId().equals(guard.getTownId());
	}

	private static void heal(LivingEntity entity, double amount) {
		if (amount <= 0)
			return;
		// Looked up by name: the Attribute.MAX_HEALTH constant only exists from 1.21.3 (it was GENERIC_MAX_HEALTH).
		Attribute maxHealth = org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft("max_health"));
		if (maxHealth == null)
			maxHealth = org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft("generic.max_health"));
		AttributeInstance max = maxHealth == null ? null : entity.getAttribute(maxHealth);
		double cap = max == null ? entity.getHealth() : max.getValue();
		if (entity.getHealth() >= cap)
			return;
		entity.setHealth(Math.min(cap, entity.getHealth() + amount));
		entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation().add(0, entity.getHeight(), 0), 4, 0.3, 0.2, 0.3, 0.0);
	}
}
