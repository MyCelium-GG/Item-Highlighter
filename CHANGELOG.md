# Changelog

All notable changes to ItemHighlighter are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
ItemHighlighter is versioned semantically (`MAJOR.MINOR.PATCH`).
It runs on Minecraft 26.3 (Fabric + Forge + NeoForge, Java 25) and requires
 [Mycel](https://github.com/MyCelium-GG/MyCel-Lib) (`my.celium.org`).

## [1.0.3] - Unreleased

### Changed

- Requires Mycel 1.3.1.
- New default highlight color: wiki orange `#FFA657` instead of legacy
  yellow `#FFFF55`. Existing configs keep the color they have.

## [1.0.2] - 2026-10-19

### Changed

- Updated Mycel lib to 1.3.0: Mycel's chat messages now use the MyCelium wiki's
  red style.
- Checks for new versions through the MyCelium wiki (one log line when an
  update is out; turn off with `enableUpdateChecker` in `config/mycel.json`).

### Fixed

- Highlighted entities now actually glow on the client. The glow flag is set
  directly instead of through `setGlowingTag`, which could never turn it on.

## [1.0.1] - 2026-10-10

### Changed

- Updated Mycel lib to 1.2.1.

## [1.0.0] - 2026-10-10

### Added

- Initial release.
- `/hl` client command (no permissions needed): `/hl #RRGGBB` highlights
  the held item type (legacy `&`/`§` codes work too), `/hl` uses the
  configured `defaultColor`, `/hl clear` removes the held item's highlight,
  `/hl list` shows every highlighted item with a color swatch.
- Per-item-type highlights with custom colors, persisted across restarts in
  `config/itemhighlighter_data.json` (up to 256 items, oldest dropped first).
- `matchComponents` config: when true, only stacks identical to the
  highlighted one match (same enchantments, custom name, damage and other
  components); when false, every item of the same type highlights.
- Glowing world outline for dropped items and item frames holding
  highlighted items, using vanilla's own glow rendering (nearest of the 16
  palette colors; the exact color still shows in the hotbar, tooltip and
  `/hl list`).
- Texture-hugging tint over highlighted item sprites in the hotbar, offhand,
  inventories and containers: opaque texels tinted, crisp outline plus soft
  halo on adjacent transparent texels, empty sprite corners untouched.
- Colored `★ Highlighted` tooltip line on highlighted items everywhere
  (inventory, chests, recipe book, creative menu); rarity colors and other
  tooltip lines untouched.
- Config (`config/itemhighlighter.json`): `enabled`, `defaultColor`
  (`0xFFFF55`), `showTooltipLine`, `hotbarHighlight`, `matchComponents`.
  Reload live with `/mycel reload itemhighlighter`; config screen available
  from the mod list on Forge and NeoForge.
- Client-side only, no keybinds. Built on Mycel 1.2.1
  (`com.github.MyCelium-GG:MyCel-Lib:1.2.1` via JitPack).
