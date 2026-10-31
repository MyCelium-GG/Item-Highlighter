# ItemHighlighter

[![License](https://img.shields.io/github/license/MyCelium-GG/MyCel-Lib)](LICENSE)

Give any item a color and spot it instantly. Hover an item in any inventory
and press **H**, pick a color, done: it gets a soft glow in the hotbar, offhand,
inventories and containers, a glowing outline in the world (dropped items and
item frames) and a colored `★ Highlighted` tooltip line. Client side.

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
  glowing outline, using vanilla's own glow rendering. The outline uses the
  nearest of the 16 team colors, and the menu shows which one; your exact color
  still shows in slots and tooltips.
- Hotbar, offhand, inventory and container slots get a soft glow in your exact
  color that fades in from the slot's edge. The item and its count stay on top,
  fully readable, for blocks and items alike.
- Hovering a highlighted item anywhere (inventory, chests, recipe book,
  creative menu) shows a colored `★ Highlighted` tooltip line. Rarity colors
  and other tooltip lines are untouched.
- Highlights persist in `config/itemhighlighter_data.json` (up to 256
  items, oldest dropped first).

## The menu

Press **H** (rebindable under Controls, Inventory) to open it: over an item in
any inventory for that item, or in the world for the held item. `/hl` opens it
too. Your highlighted items are on the left, and typing in the search box finds
any item, so you can mark things you don't have yet. Picking a color from the
palette, the picker or a hex code highlights the item right away.

## Commands (`/hl`)

Hold the item and type (client command, no permissions needed):

- `/hl #ff55aa` — highlights the held item type in that color
  (also `&#ff55aa`; legacy codes like `/hl &a` or `/hl §a` work too;
  anything after the color, e.g. `/hl &a&lMy Axe`, is ignored).
- `/hl` — opens the menu.
- `/hl clear` — removes the held item type's highlight.
- `/hl list` — lists every highlighted item with a color swatch.

## Configuration (`config/itemhighlighter.json`)

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch (also toggleable live via `Mycel.setEnabled`). |
| `defaultColor` | `0xFFA657` | Default highlight color, packed hex RGB (`0x000000`-`0xFFFFFF`). |
| `showTooltipLine` | `true` | Append the colored `★ Highlighted` tooltip line. |
| `hotbarHighlight` | `true` | Show the glow behind highlighted items in the hotbar, inventory and containers. |
| `worldGlow` | `true` | Give highlighted dropped items and item frames a glowing outline. |
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
