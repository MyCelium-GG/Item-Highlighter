# ItemHighlighter

[![License](https://img.shields.io/github/license/MyCelium-GG/MyCel-Lib)](LICENSE)

Hold an item, give it a color, spot it instantly. Highlighted items get a
glowing outline in the world (dropped items and item frames), a tinted
item texture in the hotbar, offhand, inventory and containers, plus a
colored `★ Highlighted` tooltip line - in your inventory, in chests, in
your hand, everywhere you look at items.
Client side, no keybinds.

Built on [Mycel](https://github.com/MyCelium-GG/MyCel-Lib) (`my.celium.org`), the shared
core for the MyCelium ecosystem: configuration, platform abstraction and
diagnostics are reused.

| | |
|---|---|
| Mod id | `itemhighlighter` |
| Minecraft | **26.3**, **26.2**, **26.1** (26.1-26.1.2) · Java **25** |
| Loaders | Fabric, Forge, NeoForge |

## Behavior

- Highlights apply per item type: highlighting a diamond sword highlights
  every diamond sword (by registry id, e.g. `minecraft:diamond_sword`).
  Enable `matchComponents` to highlight only the exact held item instead
  (same enchantments, custom name, damage and other components).
- Dropped items and item frames holding highlighted items get a real
  glowing outline, using vanilla's own glow rendering. The outline uses
  the nearest of the 16 palette colors; your exact color still shows in
  the hotbar, the tooltip, and `/hl list`.
- Hotbar, offhand, inventory and container items are tinted in your exact
  color with a soft halo hugging the item texture itself - never the slot
  around it. Empty sprite corners stay untouched, so the glow follows
  swords, tools, bottles and other shapes instead of drawing a box.
- Hovering a highlighted item anywhere (inventory, chests, recipe book,
  creative menu) shows a colored `★ Highlighted` tooltip line. Rarity colors
  and other tooltip lines are untouched.
- Highlights persist in `config/itemhighlighter_data.json` (up to 256
  items, oldest dropped first).

## Highlighting (`/hl`)

Hold the item and type (client command, no permissions needed):

- `/hl #ff55aa` — highlights the held item type in that color
  (also `&#ff55aa`; legacy codes like `/hl &a` or `/hl §a` work too;
  anything after the color, e.g. `/hl &a&lMy Axe`, is ignored).
- `/hl` — same, but with the configured `defaultColor`.
- `/hl clear` — removes the held item type's highlight.
- `/hl list` — lists every highlighted item with a color swatch.

## Configuration (`config/itemhighlighter.json`)

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch (also toggleable live via `Mycel.setEnabled`). |
| `defaultColor` | `0xFFA657` | Default highlight color, packed hex RGB (`0x000000`-`0xFFFFFF`). |
| `showTooltipLine` | `true` | Append the colored `★ Highlighted` tooltip line. |
| `hotbarHighlight` | `true` | Tint highlighted item textures in the hotbar, inventory and containers. |
| `matchComponents` | `false` | Only highlight stacks identical to the held one (enchantments, names, damage, …) instead of the whole item type. |

Reload live with `/mycel reload itemhighlighter`. A config screen is
available from the mod list on Forge and NeoForge.

## Building

JDK 25, then:

```sh
./gradlew build            # all loaders + tests
./gradlew :common:test     # unit tests only
```

Mycel itself comes from [JitPack](https://jitpack.io/#MyCelium-GG/MyCel-Lib)
as one universal jar for all loaders
(`com.github.MyCelium-GG:MyCel-Lib:1.4.0`). No local setup needed.
