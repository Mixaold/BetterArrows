# Better Arrows

A Fabric mod for **Minecraft 26.2**. Arrows stick where they actually hit, and blocks answer an
arrow like the material they are instead of like one generic wall.

Every feature below can be turned off on its own.

## Arrows stick where they actually hit

Vanilla only remembers *how many* arrows are stuck in an entity. Every frame it re-rolls a random
spot on a random body part, seeded by the entity's id — shoot a zombie in the head and the arrow can
show up in its leg. Mobs do not even get that much: vanilla never draws stuck arrows on them at all.

- Captures the exact 3D hit point vanilla itself computes on impact and then throws away, and
  anchors the arrow there.
- Attaches to the **bone** that was hit, so the arrow swings with the arm and bends with the leg
  instead of being glued to the entity's overall position.
- Orientation comes from the arrow's real flight direction, so a shot from below points up.
- Every living entity, vanilla or modded, with no allowlist — and no giant arrows on a giant slime,
  no invisible ones on a baby mob.
- Survives a rejoin: the hit is stored relative to the entity and persisted, so a player who joins
  later still sees the arrows already stuck, in the same places.
- Fades out like vanilla, oldest first, as the arrow count decays.
- Built for model replacements such as Fresh Animations (via Entity Model Features): an arrow
  attaches to the bone, never to the inflated shell a pack layers over every limb — that shell is
  the nearest surface to an incoming arrow and the wrong answer, since it need not move with the
  bone at all. Past a certain gap between mesh and hitbox the mod draws nothing rather than guess.

## Blocks answer back

- **Ricochet.** A glancing shot skips off a surface instead of planting itself. How shallow it has
  to be, and how much speed survives, both depend on the material: stone takes a hit well off the
  perpendicular, wood about half that, loose ground like sand or gravel only a sliver near flat, and
  wool, mud, snow or slime never bounce at all. A steeper hit loses more of its speed to the block,
  by the same rule everywhere in the mod.
- **Ice.** A shallow shot skates *along* the ice, slowing as it goes, instead of pinging off it like
  a window — and an arrow can never bury itself in ice, so a steep shot is thrown off rather than
  embedded. Which blocks count is the `betterarrows:arrows_slide_on` block tag, `#minecraft:ice` by
  default, so a datapack can add more.
- **Material impact sounds.** Wood thuds, stone clacks, metal rings. Blocks that already make their
  own noise (bells, amethyst) are left alone, listed in the `betterarrows:no_impact_sound` tag.
- **Block debris.** Crumble particles carrying the real texture of the block that was hit.

No damage tuning anywhere: vanilla already scales arrow damage by speed, so an arrow that has shed
energy on a bounce hits softer on its own.

## Requirements

- Minecraft 26.2, Fabric Loader 0.19.3+, Java 25
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Cloth Config](https://modrinth.com/mod/cloth-config) for the settings screen;
  [Mod Menu](https://modrinth.com/mod/modmenu) optional, for the button that opens it
- Install on **both client and server** (or use singleplayer): the client draws the arrows, the
  server decides where they landed and whether they bounced

## Configuration

`config/betterarrows.json`, read once at startup, with an in-game screen in English and Russian via
Mod Menu. On a multiplayer server the server's file decides gameplay — ricochet, ice, sounds,
particles — and each client's file decides rendering.

`debugLogging` is off by default and prints which body part every arrow resolved onto. It is worth
knowing about: an arrow the mod cannot place is simply invisible, so "stuck in the wrong place" and
"gave up on this one" look identical in game without it.

## Building

```
./gradlew build
```

The mod is `build/libs/betterarrows-<version>.jar` — not the `-sources` jar next to it.

## License

MIT, see [LICENSE](LICENSE). Author: Mixaold.

## Credits

Positioning research drew on the publicly documented behaviour of vanilla's own stuck-arrow
rendering and on [Arrow In The Knee](https://modrinth.com/mod/aitk) by RazorPlay01, which first
extended stuck-arrow *visibility* to mobs while keeping vanilla's random placement. This mod goes
further and tracks the real hit point instead.
