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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import xyz.unbent.garrison.api.guard.Guard;

/**
 * The eight abilities, written for mobs (a guard aiming at its current target), not ported from any other plugin.
 * None of them place, break or ignite blocks.
 */
final class Abilities {

	private static final Particle.DustOptions WATER_DUST = new Particle.DustOptions(Color.fromRGB(40, 110, 255), 1.3f);
	private static final Particle.DustOptions FROST_DUST = new Particle.DustOptions(Color.fromRGB(200, 235, 255), 1.0f);

	private Abilities() {}

	/** Casts an ability at the guard's target. Returns false if it couldn't (e.g. tremor while airborne). */
	static boolean cast(Ability ability, Casts casts, Guard guard, Mob caster, LivingEntity target) {
		return switch (ability) {
			case FIRE_BLAST -> fireBlast(casts, guard, caster, target);
			case HEAT_WAVE -> heatWave(casts, guard, caster, target);
			case EARTH_BLAST -> earthBlast(casts, guard, caster, target);
			case TREMOR -> tremor(casts, guard, caster);
			case AIR_BLAST -> airBlast(casts, guard, caster, target);
			case AIR_SWIPE -> airSwipe(casts, guard, caster, target);
			case WATER_WHIP -> waterWhip(casts, guard, caster, target);
			case FROST -> frost(casts, guard, caster);
		};
	}

	/* Fire */

	private static boolean fireBlast(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.FIRE_BLAST;
		Location start = launchPoint(caster, target);
		World world = caster.getWorld();
		world.playSound(start, Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.2f);
		Shot.fire(casts, guard, caster, start, aim(start, target), s.get(a, "speed"), s.get(a, "range"),
			at -> {
				world.spawnParticle(Particle.FLAME, at, 4, 0.08, 0.08, 0.08, 0.01);
				world.spawnParticle(Particle.SMOKE, at, 1, 0.05, 0.05, 0.05, 0.0);
			},
			null,
			(hit, at) -> {
				if (casts.hurt(guard, hit, s.get(a, "damage")))
					hit.setFireTicks(Math.max(hit.getFireTicks(), s.ticks(a, "burn-seconds")));
				world.spawnParticle(Particle.FLAME, at, 20, 0.3, 0.3, 0.3, 0.05);
				world.playSound(at, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 1.4f);
			});
		return true;
	}

	private static boolean heatWave(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.HEAT_WAVE;
		double range = s.get(a, "range");
		double halfAngle = Math.toRadians(s.get(a, "angle") / 2);
		Location eye = caster.getEyeLocation();
		Vector forward = aim(eye, target);
		World world = caster.getWorld();
		world.playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);

		// Flames fanned across the cone.
		for (int ray = -3; ray <= 3; ray++) {
			Vector direction = forward.clone().rotateAroundY(halfAngle * ray / 3.0);
			for (double d = 0.8; d <= range; d += 0.6)
				world.spawnParticle(Particle.FLAME, eye.clone().add(direction.clone().multiply(d)), 1, 0.1, 0.1, 0.1, 0.02);
		}
		for (LivingEntity entity : world.getNearbyLivingEntities(eye, range)) {
			if (entity.equals(caster) || entity.isDead())
				continue;
			Vector to = centre(entity).toVector().subtract(eye.toVector());
			if (to.lengthSquared() > range * range || to.lengthSquared() < 1e-6 || forward.angle(to) > halfAngle)
				continue;
			if (!caster.hasLineOfSight(entity))
				continue;
			if (casts.hurt(guard, entity, s.get(a, "damage")))
				entity.setFireTicks(Math.max(entity.getFireTicks(), s.ticks(a, "burn-seconds")));
		}
		return true;
	}

	/* Earth */

	private static boolean earthBlast(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.EARTH_BLAST;
		Location start = launchPoint(caster, target);
		World world = caster.getWorld();
		Material material = groundMaterial(caster);
		BlockData dust = material.createBlockData();
		BlockDisplay rock = casts.spawnRock(start, material);
		world.playSound(start, Sound.BLOCK_STONE_BREAK, 1.0f, 0.7f);
		Shot.fire(casts, guard, caster, start, aim(start, target), s.get(a, "speed"), s.get(a, "range"),
			at -> world.spawnParticle(Particle.BLOCK, at, 2, 0.1, 0.1, 0.1, 0.0, dust),
			rock,
			(hit, at) -> {
				if (casts.hurt(guard, hit, s.get(a, "damage")))
					push(hit, caster, s.get(a, "knockback"), 0.35);
				world.spawnParticle(Particle.BLOCK, at, 25, 0.3, 0.3, 0.3, 0.0, dust);
				world.playSound(at, Sound.BLOCK_STONE_BREAK, 1.0f, 0.9f);
			});
		return true;
	}

	private static boolean tremor(Casts casts, Guard guard, Mob caster) {
		if (!caster.isOnGround())
			return false;
		ElementalistSettings s = casts.settings();
		Ability a = Ability.TREMOR;
		double radius = s.get(a, "radius");
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
			if (!casts.hurt(guard, entity, s.get(a, "damage")))
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

	/* Air */

	private static boolean airBlast(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.AIR_BLAST;
		Location start = launchPoint(caster, target);
		World world = caster.getWorld();
		world.playSound(start, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.6f);
		Shot.fire(casts, guard, caster, start, aim(start, target), s.get(a, "speed"), s.get(a, "range"),
			at -> world.spawnParticle(Particle.CLOUD, at, 3, 0.15, 0.15, 0.15, 0.01),
			null,
			(hit, at) -> {
				if (casts.hurt(guard, hit, s.get(a, "damage")))
					push(hit, caster, s.get(a, "knockback"), 0.5);
				world.spawnParticle(Particle.CLOUD, at, 15, 0.3, 0.3, 0.3, 0.08);
				world.playSound(at, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.2f);
			});
		return true;
	}

	private static boolean airSwipe(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.AIR_SWIPE;
		Location start = launchPoint(caster, target);
		Vector forward = aim(start, target);
		World world = caster.getWorld();
		int streams = Math.max(1, (int) s.get(a, "streams"));
		double spread = Math.toRadians(s.get(a, "spread"));
		world.playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
		for (int i = 0; i < streams; i++) {
			// Evenly across [-spread, +spread]; a single stream goes straight.
			double offset = streams == 1 ? 0 : -spread + 2 * spread * i / (streams - 1);
			Shot.fire(casts, guard, caster, start, forward.clone().rotateAroundY(offset), s.get(a, "speed"), s.get(a, "range"),
				at -> world.spawnParticle(Particle.CLOUD, at, 1, 0.02, 0.02, 0.02, 0.0),
				null,
				(hit, at) -> {
					casts.hurt(guard, hit, s.get(a, "damage"));
					world.spawnParticle(Particle.SWEEP_ATTACK, at, 1, 0, 0, 0, 0);
				});
		}
		return true;
	}

	/* Water */

	private static boolean waterWhip(Casts casts, Guard guard, Mob caster, LivingEntity target) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.WATER_WHIP;
		Location start = launchPoint(caster, target);
		World world = caster.getWorld();
		world.playSound(start, Sound.ENTITY_GENERIC_SPLASH, 0.8f, 1.3f);
		Shot.fire(casts, guard, caster, start, aim(start, target), s.get(a, "speed"), s.get(a, "range"),
			at -> {
				world.spawnParticle(Particle.DUST, at, 3, 0.08, 0.08, 0.08, 0.0, WATER_DUST);
				world.spawnParticle(Particle.SPLASH, at, 2, 0.1, 0.1, 0.1, 0.0);
			},
			null,
			(hit, at) -> {
				if (casts.hurt(guard, hit, s.get(a, "damage")))
					// Towards the guard: a negative push.
					push(hit, caster, -s.get(a, "pull"), 0.3);
				world.spawnParticle(Particle.SPLASH, at, 25, 0.3, 0.3, 0.3, 0.1);
				world.playSound(at, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.7f, 1.2f);
			});
		return true;
	}

	private static boolean frost(Casts casts, Guard guard, Mob caster) {
		ElementalistSettings s = casts.settings();
		Ability a = Ability.FROST;
		double radius = s.get(a, "radius");
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
			if (!casts.hurt(guard, entity, s.get(a, "damage")))
				continue;
			if (slow > 0) {
				entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slow, 1, false, true, true));
				entity.setFreezeTicks(Math.max(entity.getFreezeTicks(), Math.min(140, slow)));
			}
		}
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
		AttributeInstance max = entity.getAttribute(Attribute.MAX_HEALTH);
		double cap = max == null ? entity.getHealth() : max.getValue();
		if (entity.getHealth() >= cap)
			return;
		entity.setHealth(Math.min(cap, entity.getHealth() + amount));
		entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation().add(0, entity.getHeight(), 0), 4, 0.3, 0.2, 0.3, 0.0);
	}
}
