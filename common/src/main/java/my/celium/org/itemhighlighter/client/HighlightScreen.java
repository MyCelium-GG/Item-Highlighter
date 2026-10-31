package my.celium.org.itemhighlighter.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.joml.Matrix3x2fStack;

import com.mojang.blaze3d.platform.InputConstants;

import my.celium.org.client.MycelScreens;
import my.celium.org.client.MycelTheme;
import my.celium.org.itemhighlighter.HighlightColor;
import my.celium.org.itemhighlighter.HighlightKeys;
import my.celium.org.itemhighlighter.HighlightStore;
import my.celium.org.itemhighlighter.ItemHighlighterMod;
import my.celium.org.config.ConfigValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The highlight menu: your highlighted items on the left (typing searches
 * every item instead), and on the right the selected item with its live
 * preview, the color palette and picker. Picking a color highlights the item
 * right away. Client only.
 */
public final class HighlightScreen extends Screen {
    private static final int TITLE_BAR = 20;
    private static final int PAD = 8;
    private static final int LIST_W = 128;
    private static final int ROW_H = 20;
    private static final int CELL = 13;
    private static final int PALETTE_COLS = 8;
    private static final int PALETTE_ROWS = 3;
    private static final int PREVIEW = 36;
    private static final int HUE_H = 6;
    private static final int DRAG_SQUARE = 1;
    private static final int DRAG_HUE = 2;

    private record Row(String key, ItemStack stack) {
    }

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double px, double py) {
            return px >= x && py >= y && px < x + w && py < y + h;
        }

        int right() {
            return x + w;
        }

        int bottom() {
            return y + h;
        }
    }

    private final AbstractContainerScreen<?> parent;
    private final List<Row> rows = new ArrayList<>();
    private List<Row> allItems;
    private EditBox search;
    private EditBox hex;
    private FlatButton removeButton;
    private String selectedKey = "";
    private ItemStack selectedStack = ItemStack.EMPTY;
    /** Selected item's color, or the default while it has none yet. */
    private int color;
    private float hue;
    private float saturation;
    private float value;
    private int scroll;
    private int dragging;
    private boolean updatingHex;

    HighlightScreen(AbstractContainerScreen<?> parent, ItemStack item) {
        super(Component.literal("Item Highlighter"));
        this.parent = parent;
        this.color = ItemHighlighterMod.effectiveDefaultColor();
        if (item != null && !item.isEmpty()) {
            select(HighlightKeys.keyFor(item), item);
        } else {
            for (String key : HighlightStore.entries().keySet()) {
                select(key, stackForKey(key));
                break;
            }
        }
    }

    private static ItemStack stackForKey(String key) {
        Identifier id = Identifier.tryParse(HighlightKeys.displayId(key));
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void select(String key, ItemStack stack) {
        selectedKey = key;
        selectedStack = stack.copy();
        selectedStack.setCount(1);
        Integer stored = HighlightStore.getColor(key);
        setColor(stored != null ? stored : ItemHighlighterMod.effectiveDefaultColor());
        syncHsv();
        updateRemoveButton();
    }

    private boolean selected() {
        return !selectedStack.isEmpty();
    }

    private boolean highlighted() {
        return selected() && HighlightStore.getColor(selectedKey) != null;
    }

    private void setColor(int rgb) {
        color = rgb & 0xFFFFFF;
        if (hex != null && !updatingHex) {
            updatingHex = true;
            hex.setValue(HighlightColor.toHex(color));
            updatingHex = false;
        }
    }

    private void syncHsv() {
        float[] hsv = HighlightColor.toHsv(color);
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
    }

    private void commitColor() {
        if (selected()) {
            HighlightStore.setColor(selectedKey, color);
            updateRemoveButton();
            refreshRows();
        }
    }

    // ---------------------------------------------------------------- layout

    private int panelW() {
        return Math.min(width - 12, 346);
    }

    private int panelH() {
        return Math.min(height - 12, 208);
    }

    private int px() {
        return (width - panelW()) / 2;
    }

    private int py() {
        return (height - panelH()) / 2;
    }

    private Rect list() {
        int top = py() + TITLE_BAR + PAD + 18;
        return new Rect(px() + PAD, top, LIST_W, py() + panelH() - PAD - top);
    }

    private int rightX() {
        return px() + PAD + LIST_W + PAD;
    }

    private int rightW() {
        return px() + panelW() - PAD - rightX();
    }

    private Rect preview() {
        return new Rect(rightX(), py() + TITLE_BAR + PAD, PREVIEW, PREVIEW);
    }

    private Rect palette() {
        return new Rect(rightX(), preview().bottom() + PAD, PALETTE_COLS * CELL, PALETTE_ROWS * CELL);
    }

    private Rect square() {
        Rect p = palette();
        int x = p.right() + PAD;
        return new Rect(x, p.y(), Math.max(10, px() + panelW() - PAD - x), p.h() - 1);
    }

    private Rect hueBar() {
        Rect s = square();
        return new Rect(s.x(), s.bottom() + 6, s.w(), HUE_H);
    }

    private int hexRowY() {
        return palette().bottom() + 3;
    }

    private int toggleY() {
        return py() + panelH() - PAD - FlatButton.HEIGHT * 2 - 4;
    }

    // ---------------------------------------------------------------- widgets

    @Override
    protected void init() {
        search = new EditBox(font, px() + PAD + 4, py() + TITLE_BAR + PAD + 3, LIST_W - 8, 9,
                Component.literal("Search items"));
        search.setBordered(false);
        search.setMaxLength(64);
        search.setHint(MycelTheme.colored("Search every item", MycelTheme.FAINT));
        search.setResponder(text -> {
            scroll = 0;
            refreshRows();
        });
        addRenderableWidget(search);

        hex = new EditBox(font, rightX() + font.width("Hex") + 10, hexRowY() + 3, 50, 9, Component.literal("Hex color"));
        hex.setBordered(false);
        hex.setMaxLength(7);
        hex.setValue(HighlightColor.toHex(color));
        hex.setResponder(this::onHexTyped);
        addRenderableWidget(hex);

        int toggleW = (rightW() - 4) / 2;
        addRenderableWidget(toggle(rightX(), toggleY(), toggleW, "Slot glow", ItemHighlighterMod.hotbarHighlight));
        addRenderableWidget(toggle(rightX() + toggleW + 4, toggleY(), toggleW, "World glow", ItemHighlighterMod.worldGlow));

        int bottom = py() + panelH() - PAD - FlatButton.HEIGHT;
        int buttonW = (rightW() - 4) / 2;
        removeButton = new FlatButton(rightX(), bottom, buttonW, Component.literal("Remove"), false, this::removeSelected);
        addRenderableWidget(removeButton);
        String done = parent != null ? "Back" : "Done";
        addRenderableWidget(new FlatButton(rightX() + buttonW + 4, bottom, buttonW, Component.literal(done), true, this::onClose));
        updateRemoveButton();
        refreshRows();
    }

    private FlatButton toggle(int x, int y, int w, String label, ConfigValue<Boolean> option) {
        FlatButton[] button = new FlatButton[1];
        button[0] = new FlatButton(x, y, w, toggleLabel(label, option), false, () -> {
            option.set(!option.get());
            ItemHighlighterMod.config.save();
            button[0].setMessage(toggleLabel(label, option));
        });
        return button[0];
    }

    private static Component toggleLabel(String label, ConfigValue<Boolean> option) {
        return Component.literal(label + (option.get() ? ": On" : ": Off"));
    }

    private void updateRemoveButton() {
        if (removeButton != null) {
            removeButton.active = highlighted();
        }
    }

    private void onHexTyped(String text) {
        if (updatingHex) {
            return;
        }
        String raw = text.trim();
        Integer parsed = HighlightColor.parseLenient(raw.startsWith("#") ? raw : "#" + raw);
        if (parsed != null && raw.length() >= 6) {
            updatingHex = true;
            setColor(parsed);
            updatingHex = false;
            syncHsv();
            commitColor();
        }
    }

    private void removeSelected() {
        if (selected() && HighlightStore.remove(selectedKey)) {
            updateRemoveButton();
            refreshRows();
        }
    }

    private void refreshRows() {
        rows.clear();
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            Map<String, Integer> entries = HighlightStore.entries();
            if (selected() && !entries.containsKey(selectedKey)) {
                rows.add(new Row(selectedKey, selectedStack));
            }
            for (String key : entries.keySet()) {
                ItemStack stack = key.equals(selectedKey) ? selectedStack : stackForKey(key);
                if (!stack.isEmpty()) {
                    rows.add(new Row(key, stack));
                }
            }
        } else {
            for (Row row : allItems()) {
                if (row.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)
                        || row.key().contains(query)) {
                    rows.add(row);
                }
            }
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private List<Row> allItems() {
        if (allItems == null) {
            allItems = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item != Items.AIR) {
                    ItemStack stack = new ItemStack(item);
                    allItems.add(new Row(HighlightKeys.typeId(stack), stack));
                }
            }
        }
        return allItems;
    }

    private int visibleRows() {
        return list().h() / ROW_H;
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - visibleRows());
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float a) {
        int x0 = px();
        int y0 = py();
        int x1 = x0 + panelW();
        int y1 = y0 + panelH();
        MycelTheme.panel(gfx, x0, y0, x1, y1);
        gfx.fill(x0 + 2, y0 + 2, x1 - 2, y0 + TITLE_BAR, MycelTheme.ACCENT_DARK);
        gfx.fill(x0 + 2, y0 + TITLE_BAR, x1 - 2, y0 + TITLE_BAR + 1, MycelTheme.HEADER_EDGE);
        gfx.text(font, "Item Highlighter", x0 + PAD, y0 + 7, MycelTheme.HEADING);
        String count = HighlightStore.entries().size() + " highlighted";
        gfx.text(font, count, x1 - PAD - font.width(count), y0 + 7, MycelTheme.HEADING);

        drawField(gfx, px() + PAD, py() + TITLE_BAR + PAD, LIST_W, 14, search.isFocused());
        drawList(gfx, mx, my);
        if (selected()) {
            drawSelected(gfx);
            drawPalette(gfx, mx, my);
            drawPicker(gfx);
            gfx.text(font, "Hex", rightX(), hexRowY() + 3, MycelTheme.DIM);
            drawField(gfx, hex.getX() - 4, hexRowY(), 58, 14, hex.isFocused());
            drawWorldColor(gfx);
        } else {
            drawNothingSelected(gfx);
        }
        hex.visible = selected();
        super.extractRenderState(gfx, mx, my, a);
    }

    private void drawField(GuiGraphicsExtractor gfx, int x, int y, int w, int h, boolean focused) {
        gfx.fill(x, y, x + w, y + h, focused ? MycelTheme.ACCENT : MycelTheme.BORDER);
        gfx.fill(x + 1, y + 1, x + w - 1, y + h - 1, MycelTheme.SURFACE);
    }

    private void drawList(GuiGraphicsExtractor gfx, int mx, int my) {
        Rect r = list();
        gfx.fill(r.x(), r.y(), r.right(), r.bottom(), MycelTheme.BORDER);
        gfx.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, MycelTheme.SURFACE);
        if (rows.isEmpty()) {
            String[] lines = search.getValue().isBlank()
                    ? new String[] {"Nothing highlighted.", "Search for an item,", "or press H over one", "in any inventory."}
                    : new String[] {"No item called that."};
            for (int i = 0; i < lines.length; i++) {
                gfx.text(font, lines[i], r.x() + 6, r.y() + 6 + i * 11, MycelTheme.DIM);
            }
            return;
        }
        int end = Math.min(rows.size(), scroll + visibleRows());
        for (int i = scroll; i < end; i++) {
            Row row = rows.get(i);
            int y = r.y() + (i - scroll) * ROW_H;
            boolean isSelected = row.key().equals(selectedKey);
            boolean hover = r.contains(mx, my) && my >= y && my < y + ROW_H;
            if (isSelected || hover) {
                gfx.fill(r.x() + 1, y + 1, r.right() - 1, y + ROW_H - 1, isSelected ? MycelTheme.BORDER : MycelTheme.NOTE);
            }
            if (isSelected) {
                gfx.fill(r.x() + 1, y + 1, r.x() + 3, y + ROW_H - 1, MycelTheme.ACCENT);
            }
            gfx.item(row.stack(), r.x() + 4, y + 2);
            Integer stored = HighlightStore.getColor(row.key());
            int textRight = r.right() - 6 - (stored != null ? 10 : 0);
            String name = MycelTheme.fit(font, row.stack().getHoverName().getString(), textRight - r.x() - 24);
            gfx.text(font, name, r.x() + 23, y + 6, stored != null ? MycelTheme.TEXT : MycelTheme.DIM);
            if (stored != null) {
                gfx.fill(r.right() - 12, y + 6, r.right() - 4, y + 14, 0xFF0E0E0E);
                gfx.fill(r.right() - 11, y + 7, r.right() - 5, y + 13, 0xFF000000 | stored);
            }
        }
        if (maxScroll() > 0) {
            int trackH = r.h() - 2;
            int thumb = Math.max(10, trackH * visibleRows() / rows.size());
            int y = r.y() + 1 + (trackH - thumb) * scroll / maxScroll();
            gfx.fill(r.right() - 3, y, r.right() - 1, y + thumb, MycelTheme.DIM);
        }
    }

    private void drawSelected(GuiGraphicsExtractor gfx) {
        Rect p = preview();
        gfx.fill(p.x(), p.y(), p.right(), p.bottom(), 0xFF8B8B8B);
        Matrix3x2fStack pose = gfx.pose();
        pose.pushMatrix();
        pose.translate(p.x() + 2, p.y() + 2);
        pose.scale(2f, 2f);
        gfx.fill(0, 0, 16, 16, 0xFF8B8B8B);
        HotbarHighlight.glow(gfx, 0, 0, color);
        gfx.item(selectedStack, 0, 0);
        pose.popMatrix();
        int x = p.right() + PAD;
        int room = px() + panelW() - PAD - x;
        gfx.text(font, MycelTheme.fit(font, selectedStack.getHoverName().getString(), room), x, p.y() + 3, MycelTheme.HEADING);
        boolean exact = HighlightKeys.isExactKey(selectedKey);
        gfx.text(font, MycelTheme.fit(font, HighlightKeys.displayId(selectedKey) + (exact ? " (exact)" : ""), room),
                x, p.y() + 14, MycelTheme.FAINT);
        gfx.text(font, highlighted() ? "Highlighted" : "Pick a color to highlight it", x, p.y() + 25,
                highlighted() ? MycelTheme.GOOD : MycelTheme.DIM);
    }

    /** World outlines only come in vanilla's 16 team colors, so this shows the closest one. */
    private void drawWorldColor(GuiGraphicsExtractor gfx) {
        Integer outline = HighlightColor.parse("&" + HighlightColor.nearestLegacyCode(color));
        if (outline == null) {
            return;
        }
        int y = hexRowY() + 19;
        String label = "World outline";
        gfx.text(font, label, rightX(), y, MycelTheme.DIM);
        int x = rightX() + font.width(label) + 5;
        gfx.fill(x, y - 1, x + 9, y + 8, 0xFF0E0E0E);
        gfx.fill(x + 1, y, x + 8, y + 7, 0xFF000000 | outline);
    }

    private void drawNothingSelected(GuiGraphicsExtractor gfx) {
        Rect p = preview();
        gfx.text(font, "Pick an item on the left to", p.x(), p.y() + 3, MycelTheme.TEXT);
        gfx.text(font, "color it. In any inventory,", p.x(), p.y() + 14, MycelTheme.TEXT);
        gfx.text(font, "hover an item and press H.", p.x(), p.y() + 25, MycelTheme.TEXT);
    }

    private void drawPalette(GuiGraphicsExtractor gfx, int mx, int my) {
        Rect r = palette();
        int hovered = paletteAt(mx, my);
        for (int i = 0; i < HighlightColor.PRESETS.length; i++) {
            int x = r.x() + (i % PALETTE_COLS) * CELL;
            int y = r.y() + (i / PALETTE_COLS) * CELL;
            int rgb = HighlightColor.PRESETS[i].rgb();
            gfx.fill(x, y, x + CELL - 1, y + CELL - 1,
                    rgb == color ? MycelTheme.HEADING : i == hovered ? 0xFF9A9A9A : 0xFF0E0E0E);
            gfx.fill(x + 1, y + 1, x + CELL - 2, y + CELL - 2, 0xFF000000 | rgb);
        }
        if (hovered >= 0) {
            HighlightColor.Preset preset = HighlightColor.PRESETS[hovered];
            gfx.setTooltipForNextFrame(font, Component.literal(preset.name() + "  " + HighlightColor.toHex(preset.rgb())), mx, my);
        }
    }

    private int paletteAt(double mx, double my) {
        Rect r = palette();
        if (!r.contains(mx, my)) {
            return -1;
        }
        int index = (int) ((my - r.y()) / CELL) * PALETTE_COLS + (int) ((mx - r.x()) / CELL);
        return index < HighlightColor.PRESETS.length ? index : -1;
    }

    private void drawPicker(GuiGraphicsExtractor gfx) {
        Rect sq = square();
        int cellW = 3;
        int cellH = 2;
        for (int x = 0; x < sq.w(); x += cellW) {
            float s = (float) x / (sq.w() - 1);
            for (int y = 0; y < sq.h(); y += cellH) {
                float v = 1 - (float) y / (sq.h() - 1);
                gfx.fill(sq.x() + x, sq.y() + y, Math.min(sq.right(), sq.x() + x + cellW),
                        Math.min(sq.bottom(), sq.y() + y + cellH), 0xFF000000 | HighlightColor.hsv(hue, s, v));
            }
        }
        gfx.outline(sq.x() - 1, sq.y() - 1, sq.w() + 2, sq.h() + 2, MycelTheme.BORDER);
        int markX = sq.x() + Math.round(saturation * (sq.w() - 1));
        int markY = sq.y() + Math.round((1 - value) * (sq.h() - 1));
        gfx.outline(markX - 3, markY - 3, 7, 7, 0xFF000000);
        gfx.outline(markX - 2, markY - 2, 5, 5, 0xFFFFFFFF);
        Rect hb = hueBar();
        for (int x = 0; x < hb.w(); x++) {
            gfx.fill(hb.x() + x, hb.y(), hb.x() + x + 1, hb.bottom(), 0xFF000000 | HighlightColor.hsv((float) x / hb.w(), 1, 1));
        }
        gfx.outline(hb.x() - 1, hb.y() - 1, hb.w() + 2, hb.h() + 2, MycelTheme.BORDER);
        int markHue = hb.x() + Math.round(hue * (hb.w() - 1));
        gfx.fill(markHue - 2, hb.y() - 2, markHue + 3, hb.bottom() + 2, 0xFF000000);
        gfx.fill(markHue - 1, hb.y() - 1, markHue + 2, hb.bottom() + 1, 0xFFFFFFFF);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        double mx = event.x();
        double my = event.y();
        setFocused(null);
        Rect r = list();
        if (r.contains(mx, my)) {
            int index = scroll + (int) ((my - r.y()) / ROW_H);
            if (index >= 0 && index < rows.size()) {
                Row row = rows.get(index);
                select(row.key(), row.stack());
            }
            return true;
        }
        if (!selected()) {
            return false;
        }
        int preset = paletteAt(mx, my);
        if (preset >= 0) {
            setColor(HighlightColor.PRESETS[preset].rgb());
            syncHsv();
            commitColor();
            return true;
        }
        Rect sq = square();
        if (new Rect(sq.x() - 2, sq.y() - 2, sq.w() + 4, sq.h() + 4).contains(mx, my)) {
            dragging = DRAG_SQUARE;
            pickFromSquare(mx, my);
            return true;
        }
        Rect hb = hueBar();
        if (new Rect(hb.x() - 2, hb.y() - 3, hb.w() + 4, hb.h() + 6).contains(mx, my)) {
            dragging = DRAG_HUE;
            pickFromHue(mx);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging == DRAG_SQUARE) {
            pickFromSquare(event.x(), event.y());
            return true;
        }
        if (dragging == DRAG_HUE) {
            pickFromHue(event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != 0) {
            // Saving on release keeps a drag from writing the file every frame.
            dragging = 0;
            commitColor();
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (list().contains(x, y)) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll + (scrollY > 0 ? -1 : scrollY < 0 ? 1 : 0)));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private void pickFromSquare(double mx, double my) {
        Rect r = square();
        saturation = (float) Math.max(0, Math.min(1, (mx - r.x()) / (r.w() - 1)));
        value = (float) Math.max(0, Math.min(1, 1 - (my - r.y()) / (r.h() - 1)));
        setColor(HighlightColor.hsv(hue, saturation, value));
    }

    private void pickFromHue(double mx) {
        Rect r = hueBar();
        hue = (float) Math.max(0, Math.min(0.999, (mx - r.x()) / r.w()));
        setColor(HighlightColor.hsv(hue, saturation, value));
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean stillOpen = parent != null && minecraft.player != null
                && (minecraft.player.containerMenu == parent.getMenu() || parent instanceof CreativeModeInventoryScreen);
        MycelScreens.open(stillOpen ? parent : null);
    }
}
