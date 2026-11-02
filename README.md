# Elementalist

A free addon for **Garrison**, the town-guard plugin for Towny ([BuiltByBit](https://builtbybit.com/resources/garrison-guards-for-towny.133397/)): a town guard that wields fire,
water, earth or air, with two abilities per element and three trainable paths each. Written from scratch for mobs. MIT licensed.

**Requires Garrison 1.0.0 or newer** (Garrison API 1.5.0+); the skill tree needs Garrison 1.0.1. Garrison is sold separately; this addon does nothing on its
own.

## Install

1. Download `Elementalist.jar` from [Releases](https://github.com/kadika66/Elementalist-Garrison-addon/releases) and put it in
   `plugins/Garrison/addons/` (not `plugins/`).
2. Add a guard type to `plugins/Garrison/garrison.yml`. Garrison's default file has this one commented out at the bottom
   of `types:`:

```yaml
  elementalist:
    display: 'Elementalist'
    entity: HUSK
    behaviour: elementalist
    cost: 900
    upkeep: 16
    health: 26
    damage: 2
    speed-multiplier: 1.05
    dye-armor: false
    gear-slots: [HEAD, CHEST, LEGS, FEET]
    issued:
      HEAD: CHAINMAIL_HELMET
      CHEST: CHAINMAIL_CHESTPLATE
      LEGS: CHAINMAIL_LEGGINGS
      FEET: CHAINMAIL_BOOTS
```
3. Restart the server (addons load once, at startup). `/garrison addons` should list Elementalist. Towns can now
   `/garrison buy elementalist`.

Settings: `plugins/Garrison/addons/Elementalist/config.yml` (damage, cooldowns and reach per ability; the starting
element).

## In game

Sneak + right-click the guard (or `/garrison gear`): armour slots on the middle row, the element button on the bottom
row. Left-click for the next element, right-click for the previous. Free, any time, for whoever manages the garrison.
New Elementalists start with `default-element` (random by default). The element is saved on the guard.

| Element | Ranged | Close |
|---|---|---|
| Fire | Cinder Shot: burst, sets target alight | Searing Arc: cone of flame (5 blocks) |
| Water | Tide Lash: lash that pulls in | Frostbloom: slows enemies, heals itself + town guards nearby (6 blocks) |
| Earth | Stoneshot: thrown rock, knockback | Quake Stomp: ground slam, stun then slow (4 blocks) |
| Air | Razor Wind: fan of three light cuts | Gale Shove: strong push away (8 blocks) |

Once a second, with an enemy targeted: a trained path ability if it's ready and in range, otherwise the close ability
if the enemy is in its reach and it's off cooldown, otherwise the ranged one. Aimed abilities need line of sight. The
husk still swings when it gets close.

## Training (Garrison 1.0.1+)

Elementalists bring their own skill tree (`training-tree.yml` in the jar), used for any guard type with
`behaviour: elementalist` unless you write one for that type in Garrison's `training.yml`. Open it with
`/garrison training`. Each guard only uses the shared nodes and its own element's.

| Tier | Nodes |
|---|---|
| 1 | Hardened (+2 health per rank), Focus (abilities back 8% sooner per rank) |
| 2 | Reach (+10% range per rank), Composure (knockback resistance) |
| 3 | Fire / Water / Earth / Air Attunement (+12% damage per rank for that element) |
| 4-6 | **Three paths per element, choose one** |

| Element | Mastery path | Path | Path |
|---|---|---|---|
| Fire | **Blue Flame**: +25%, flames burn blue | **Storm** (Chain Spark): a spark that jumps between up to 3 enemies | **Ash** (Ash Lance): a smoke beam that bursts on impact, hurting enemies around it |
| Water | **Tide**: +25%, Tide Lash hits a second enemy | **Ice** (Ice Lance): a block of ice that hits hard and slows | **Bloom** (Rose Razor): a spinning rose flung like a blade |
| Earth | **Stone**: +25%, Stoneshot throws a second stone | **Iron** (Returning Blade): a blade thrown out and back, cutting both ways | **Magma** (Molten Ring): a ring of molten drips that burns enemies inside |
| Air | **Tempest**: +25%, Razor Wind gets two more blades | **Cyclone**: lifts an enemy high and drops it | **Vacuum** (Vacuum Snare): drags enemies together and slows them |

Finishing the whole tree costs 29 points. On Garrison 1.0.0 the abilities all work, but there's nothing to train.

Numbers for every ability, path and mastery are in `config.yml`. Sections were renamed in 1.1.0; old section names
(fire-blast, tremor...) in an existing config are still read.

## Safety

- No block is ever placed, broken or set on fire. Rocks are block displays: not saved with the chunk, removed when they
  land and when the addon disables.
- Only the guard's enemies are hit (shots pass through friends). Damage comes from the guard through Garrison, so
  friendly-fire protection, kill credit, outlawing and bounties work as they do for melee.
- Knockback and pull use `setVelocity`, like any plugin knockback. Tested with GrimAC.
- Not Folia-ready (neither is Garrison).

## Building

```
mvn clean package
```
`target/Elementalist.jar`. The Garrison API comes from JitPack
([garrison-api](https://github.com/kadika66/garrison-api)); this is also a working example of a Garrison addon.

## Support

Questions and bug reports: the [plugin support channel on Discord](https://discord.gg/ACMTFTz7Sq), or open an issue here.

## Licence

MIT, see [LICENSE](LICENSE).
