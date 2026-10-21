# Elementalist

A free addon for **Garrison**, the town-guard plugin for Towny <!-- Kad: store link -->: a town guard that wields fire,
water, earth or air, with two abilities per element. Written from scratch for mobs. MIT licensed.

**Requires Garrison 1.0.0 or newer** (Garrison API 1.4.0+). Garrison is sold separately; this addon does nothing on its
own.

## Install

1. Download `Elementalist.jar` <!-- Kad: releases / Modrinth / Spigot link --> and put it in
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
| Fire | Fire Blast: burst, sets target alight | Heat Wave: cone of flame (5 blocks) |
| Water | Water Whip: lash that pulls in | Frost: slows enemies, heals itself + town guards nearby (6 blocks) |
| Earth | Earth Blast: thrown rock, knockback | Tremor: ground slam, stun then slow (4 blocks) |
| Air | Air Swipe: fan of three light cuts | Air Blast: strong push away (8 blocks) |

Once a second, with an enemy targeted: the close ability if the enemy is in its reach and it's off cooldown, otherwise
the ranged one. Aimed abilities need line of sight. The husk still swings when it gets close.

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

## Licence

MIT, see [LICENSE](LICENSE).
