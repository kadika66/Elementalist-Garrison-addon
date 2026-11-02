# Elementalist patch log

## 1.1.0
**Skill tree, three paths per element, new ability names.** Needs Garrison API 1.5.0 (Garrison 1.0.0) to run; the
skill tree needs API 1.6.0 (Garrison 1.0.1). Builds against `com.github.kadika66:garrison-api:1.0.1`.
Not in game yet.

- **Renamed** (Kad's picks): Fire Blast -> Cinder Shot, Heat Wave -> Searing Arc, Earth Blast -> Stoneshot, Tremor ->
  Quake Stomp, Water Whip -> Tide Lash, Frost -> Frostbloom, Air Swipe -> Razor Wind, Air Blast -> Gale Shove.
  config.yml sections renamed to match; a server config with the old section names still works (old numbers are
  used until the new section sets them).
- **Skill tree** (`training-tree.yml`, registered with Garrison's new `registerTrainingTree`; a tree the owner writes
  in training.yml wins). Tier 1-2 for every element: Hardened (+2 health/rank), Focus (-8% cooldowns/rank), Reach
  (+10% range/rank), Composure (+0.1 knockback resistance/rank). Tier 3: an Attunement per element (+12% damage/rank).
  Tiers 4-6: three paths per element, choose one (rows = elements in the menu). 29 points to finish everything.
- **Paths** (unlock `elementalist:<id>`):
  - Fire: Blue Flame (mastery: +25%, blue flames) / Storm (Chain Spark, up to 3 enemies) / Ash (Ash Lance: smoke beam
    that bursts; particles and Garrison damage only, never a real explosion).
  - Water: Tide (mastery: +25%, Tide Lash hits a second enemy) / Ice (Ice Lance: ice block display, Slowness II) /
    Bloom (Rose Razor: spinning poppy display).
  - Earth: Stone (mastery: +25%, Stoneshot throws a second stone) / Iron (Returning Blade: iron sword display out and
    back, hits each enemy once each way; written fresh, not from Sword Dance) / Magma (Molten Ring: lava-drip particle
    ring, burns enemies inside once a second; no lava placed).
  - Air: Tempest (mastery: +25%, Razor Wind +2 blades) / Cyclone (whirl, then thrown straight up; fall damage works on
    undead) / Vacuum (Vacuum Snare: drags enemies near the target together, slows them).
  Path abilities are tried first each second when ready and in range, then close, then ranged.
- **Fix:** Frostbloom's heal used `Attribute.MAX_HEALTH`, which only exists from 1.21.3 (NoSuchFieldError on 1.21.1-2).
  Now looked up by name with the old name as fallback.
- Element button lore shows the trained path ability (or the mastery).
- Tests: every ability's numbers (incl. reach key) and every mastery number exist in config.yml; the tree unlocks exactly
  the 12 path ids; each element's three paths exclude each other; the tree fits in 40 points.

## 1.0.0 (release)
First public release, same code as UF.5. Builds against the public Garrison API from JitPack
(`com.github.kadika66:garrison-api:1.0.0`) instead of a local Garrison build. README rewritten for the public.

## 1.0.0-UF.5 (2026-10-20)
Licence: MIT (new `LICENSE`), Kad's choice, for free release on GitHub, Modrinth and Spigot. No code change.

## 1.0.0-UF.4 (2026-10-20)
Kad's build: all 3 tests pass.
Tests (JUnit 5): `ElementalistFilesTest` checks every number each ability reads exists in config.yml (including its
cooldown and reach), garrison-addon.yml points at `ElementalistAddon`, and no "bend" wording in shipped files.
Builds against Garrison 1.0.0-UF.17 (install Garrison first). No gameplay change.

## 1.0.0-UF.3 (2026-10-19)
Wording only (Kad's call, to keep clear of "bending" trademarks): the element message now reads "This Elementalist
now wields <element>." Same change in config.yml, README and a code comment. Kad tested UF.2: all four elements work
and look right.

## 1.0.0-UF.2 (2026-10-19)
UF.1 failed Kad's build: `Casts.repeat` used `BukkitScheduler#runTaskTimer(Plugin, Consumer<BukkitTask>, long, long)`,
which returns void, as if it returned the task. Now uses the Runnable overload (returns `BukkitTask`) and passes the
task to the body through a one-element holder. No other change.

## 1.0.0-UF.1 (2026-10-19)
First build. Needs Garrison 1.0.0-UF.12 (API 1.4.0); refuses to enable on older APIs.

Design agreed with Kad: no block placement (particles, sounds, non-persistent block displays); written from scratch
(no PK code, licence free to choose); element picked with a free menu button; all four elements fight.

- `behaviour: elementalist`. Element saved as guard data `elementalist:element`; default from `default-element`
  (`random`). Changing element clears its cooldowns.
- Abilities (config.yml `abilities:`): Fire Blast / Heat Wave, Earth Blast / Tremor, Air Swipe / Air Blast,
  Water Whip / Frost. Projectiles move in half-block steps, stop on non-passable blocks, hit the first enemy
  (`Garrison#isEnemy`), damage via `Garrison#damage`. Fire only sets entities alight. Tremor stun = Slowness VII for
  `stun-seconds`, then Slowness II; skipped while the guard is airborne. Frost heals the caster and same-town guards.
- All tasks and rock displays are tracked and cleared on disable.
- Not compiled against real libraries in the sandbox (repos blocked); checked brace balance and a library-less javac
  pass with only missing-library errors.
