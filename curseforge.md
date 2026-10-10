# ItemHighlighter

Hold an item, give it a color, spot it instantly. Highlighted items get a
glowing outline in the world (dropped items and item frames), a tinted
item texture in the hotbar, inventory and containers, plus a colored
`★ Highlighted` tooltip line -
in your inventory, in chests, in your hand, everywhere you look at items.
Client side, no keybinds.

- `/hl #RRGGBB` highlights the held item type (legacy `&`/`§` codes work too), `/hl clear` removes it, `/hl list` shows all
- Per-item-type highlights with custom colors, persisted across restarts (`matchComponents` narrows it to the exact held item)
- Config: `enabled`, `defaultColor`, `showTooltipLine`, `hotbarHighlight`, `matchComponents`
- Requires **Mycel**: install it alongside this mod

Reload live with `/mycel reload itemhighlighter`.
