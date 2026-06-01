---
# RuneLite Side-Panel UI Best Practices

A reference guide for building Swing-based side-panel plugins in RuneLite.
Covers layout, threading, styling, component patterns, and interaction.

---

## Threading Model

RuneLite plugins operate across three distinct threads. Crossing thread boundaries
incorrectly causes subtle bugs or crashes.

### The three threads

| Thread | What runs there | How to schedule work |
|--------|----------------|----------------------|
| **Client thread** | Game logic, `@Subscribe` event handlers, `Client` API calls | `clientThread.invokeLater()` or `invokeAtTickEnd()` |
| **Swing EDT** | All Swing component creation and mutation | `SwingUtilities.invokeLater()` |
| **Async threads** | Asset loading (sprites, images), background I/O | Deliver results back via `SwingUtilities.invokeLater()` |

### The canonical cross-thread flow

```
Game event (@Subscribe)           ← fires on client thread
  ↓ read raw data from client
  SwingUtilities.invokeLater()    ← marshal to EDT
    ↓ mutate panel components
    revalidate() + repaint()
```

Never touch `Client` APIs from the EDT; never mutate Swing components from the
client thread.

### Checking which thread you're on

```java
if (client.isClientThread()) {
    doWork();
} else {
    clientThread.invokeLater(this::doWork);
}
```

Use `invokeAtTickEnd()` (not `invokeLater`) when you need the entire game tick
to finish before sampling client state — e.g. reading varbit-derived values
that may still be accumulating mid-tick.

### Async asset delivery to the EDT

Whenever you load a sprite or image asynchronously, wrap the delivery in
`SwingUtilities.invokeLater`:

```java
spriteManager.getSpriteAsync(spriteId, 0, sprite ->
    SwingUtilities.invokeLater(() -> button.setIcon(new ImageIcon(sprite))));
```

### Removing item listeners before programmatic updates

When you need to programmatically update a combo box or list model (e.g. to
rebuild its contents), remove all existing listeners first, make your changes,
then re-add them. Otherwise each programmatic item change fires listener
callbacks you don't want:

```java
ItemListener[] listeners = combo.getItemListeners();
for (ItemListener l : listeners) combo.removeItemListener(l);
combo.removeAllItems();
// ... rebuild items ...
for (ItemListener l : listeners) combo.addItemListener(l);
```

---

## Panel Registration & Lifecycle

### Registering the panel

Create the `PluginPanel` in `startUp()` and destroy it in `shutDown()`. Use
a `NavigationButton` to put it in the client toolbar:

```java
@Override
protected void startUp() {
    panel = new MyPanel(this);
    navButton = NavigationButton.builder()
        .tooltip("My Plugin")
        .icon(MY_ICON)
        .priority(5)
        .panel(panel)
        .build();
    clientToolbar.addNavigation(navButton);
}

@Override
protected void shutDown() {
    clientToolbar.removeNavigation(navButton);
}
```

### Panel is long-lived, not recreated

The panel instance lives for the duration of the plugin session. Use view
switching (CardLayout) or visibility toggling instead of destroying and
recreating the panel object.

---

## Layout Managers

### Which layout to use when

| Situation | Layout manager |
|-----------|---------------|
| General container with header + content + footer | `BorderLayout` |
| Vertical stack of sections | `BoxLayout(Y_AXIS)` |
| Multi-column icon grid (skill buttons, etc.) | `GridLayout(0, N, hgap, vgap)` |
| Switching between distinct views | `CardLayout` |
| Single-column list with visibility-aware padding | Custom `DynamicPaddedGridLayout` pattern (see below) |

### FixedWidthPanel pattern

Wrap scroll-pane content in a panel that forces its width to `PluginPanel.PANEL_WIDTH`
to prevent horizontal overflow:

```java
public class FixedWidthPanel extends JPanel {
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PluginPanel.PANEL_WIDTH, super.getPreferredSize().height);
    }
}
```

### CardLayout for multi-view panels

When the panel has distinct states (e.g. list view vs. detail view), use
`CardLayout` inside a single `JScrollPane`. Only the active card is visible;
switching is instant with no flicker:

```java
private static final String VIEW_LIST = "list";
private static final String VIEW_DETAIL = "detail";

CardLayout viewportLayout = new CardLayout();
JPanel viewport = new JPanel(viewportLayout);
viewport.add(listWrapper, VIEW_LIST);
viewport.add(detailWrapper, VIEW_DETAIL);

// Switch:
viewportLayout.show(viewport, VIEW_LIST);
```

Override `getPreferredSize()` on the viewport panel to return only the visible
card's preferred size — otherwise the scroll pane will allocate space for all
cards simultaneously:

```java
@Override
public Dimension getPreferredSize() {
    for (Component c : getComponents()) {
        if (c.isVisible()) return c.getPreferredSize();
    }
    return super.getPreferredSize();
}
```

---

## Scrolling

### JScrollPane configuration

Always disable horizontal scrolling — RuneLite side panels have a fixed width:

```java
JScrollPane scrollPane = new JScrollPane(content);
scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
```

### Preserving scroll position across rebuilds

Save the scroll offset before rebuilding, then restore it via `invokeLater`
(after the layout pass completes):

```java
int savedScroll = scrollPane.getVerticalScrollBar().getValue();
// ... rebuild content ...
SwingUtilities.invokeLater(() ->
    scrollPane.getVerticalScrollBar().setValue(savedScroll));
```

---

## Color & Styling

### Always use ColorScheme constants

Never hardcode UI colors — use `net.runelite.client.ui.ColorScheme`:

| Constant | Typical use |
|----------|-------------|
| `ColorScheme.DARK_GRAY_COLOR` | Panel backgrounds, default button state |
| `ColorScheme.DARKER_GRAY_COLOR` | Sub-panel / card backgrounds |
| `ColorScheme.DARK_GRAY_HOVER_COLOR` | Button hover state |
| `ColorScheme.BRAND_ORANGE` | Active/selected section headers, highlights |
| `ColorScheme.LIGHT_GRAY_COLOR` | Selected dropdown items, alternate foreground |

Only use `Color.WHITE`, `Color.BLACK`, `Color.GRAY` directly for semantic text
contrast (e.g. white text on dark, black text on orange backgrounds).

### Hover states

Implement hover via `color.brighter()` / `color.darker()`, not by hardcoding a
separate hover color:

```java
button.addMouseListener(new MouseAdapter() {
    @Override public void mouseEntered(MouseEvent e) { button.setBackground(base.brighter()); }
    @Override public void mouseExited(MouseEvent e) { button.setBackground(base); }
});
```

### Section highlighting (active vs. inactive)

Active/current sections: `ColorScheme.BRAND_ORANGE` background + `Color.BLACK` text.
Inactive sections: `ColorScheme.DARKER_GRAY_COLOR.darker()` background + `Color.WHITE` text.

---

## Fonts

### Use FontManager for Runescape fonts

```java
import net.runelite.client.ui.FontManager;

label.setFont(FontManager.getRunescapeBoldFont());        // headers
label.setFont(FontManager.getRunescapeFont());             // body text
label.setFont(FontManager.getRunescapeFont().deriveFont(Font.BOLD, 16f));  // custom size/style
```

### Underline for clickable text

Use `TextAttribute.UNDERLINE` to make a label look like a link:

```java
Map<TextAttribute, Object> attrs = new HashMap<>(label.getFont().getAttributes());
attrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
label.setFont(label.getFont().deriveFont(attrs));
```

Swap fonts on hover (mouseEntered/mouseExited) rather than re-styling in place.

---

## Icon & Image Loading

### Load icons once as static fields

Load and scale icons once in a `static` initializer. Never reload icons inside
`paintComponent` or on every panel rebuild:

```java
private static final ImageIcon MY_ICON;
static {
    MY_ICON = new ImageIcon(
        ImageUtil.resizeImage(
            ImageUtil.loadImageResource(MyPlugin.class, "/my-icon.png"), 16, 16));
}
```

### Transform images before wrapping in ImageIcon

`ImageUtil` provides compositing helpers:

```java
ImageUtil.loadImageResource(MyPlugin.class, "/icon.png")  // load
ImageUtil.resizeImage(img, w, h)                          // scale
ImageUtil.alphaOffset(img, -180)                          // dim (deselected state)
ImageUtil.fillImage(img, color)                           // tint
```

### Async sprite loading

When loading sprites from `SpriteManager`, always deliver to the EDT:

```java
spriteManager.getSpriteAsync(spriteId, 0, sprite ->
    SwingUtilities.invokeLater(() -> {
        button.setIcon(new ImageIcon(sprite));
        button.revalidate();
    }));
```

Store both selected/deselected icon variants as client properties on the button
so you can swap without re-fetching:

```java
button.putClientProperty("selectedIcon", selectedIcon);
button.putClientProperty("deselectedIcon", deselectedIcon);
```

---

## Component Creation Patterns

### Disable HTML in JLabels

By default, Swing interprets label text as HTML if it starts with `<html>`.
This can cause rendering surprises. Disable it explicitly:

```java
JLabel label = new JLabel("text");
label.putClientProperty("html.disable", Boolean.TRUE);
```

### Factory pattern for text components

Centralize text-component creation so you don't repeat the same configuration
flags (non-editable, non-focusable, transparent, line-wrap) across every panel:

```java
// JTextArea factory
JTextArea area = new JTextArea();
area.setLineWrap(true);
area.setWrapStyleWord(true);
area.setEditable(false);
area.setFocusable(false);
area.setOpaque(false);
area.setBackground(UIManager.getColor("Label.background"));
area.setBorder(new EmptyBorder(0, 0, 0, 0));

// JTextPane factory
JTextPane pane = new JTextPane();
pane.setEditable(false);
pane.setFocusable(false);
pane.setOpaque(false);
```

### Button decoration removal

Strip default Swing button borders/bevels before applying plugin styling:

```java
SwingUtil.removeButtonDecorations(button);
button.setUI(new BasicButtonUI());
button.setBackground(ColorScheme.DARK_GRAY_COLOR);
button.setFocusPainted(false);
```

### Icon button construction

Standard pattern for a flat icon button with hover and tooltip:

```java
JButton btn = new JButton();
SwingUtil.removeButtonDecorations(btn);
btn.setUI(new BasicButtonUI());
btn.setIcon(MY_ICON);
btn.setToolTipText("What this does");
btn.setBackground(ColorScheme.DARK_GRAY_COLOR);
btn.addActionListener(e -> handleClick());
btn.addMouseListener(new MouseAdapter() {
    @Override public void mouseEntered(MouseEvent e) { btn.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR); }
    @Override public void mouseExited(MouseEvent e)  { btn.setBackground(ColorScheme.DARK_GRAY_COLOR); }
});
```

### Tooltips on all interactive elements

Set `setToolTipText()` on every interactive component — buttons, checkboxes,
combo boxes, icon labels. This is a minimum UX expectation for RuneLite plugins.

---

## Borders & Spacing

### Spacing hierarchy

Use `EmptyBorder` at each nesting level. A typical hierarchy:

```
Outer section wrapper  →  EmptyBorder(10, 10, 10, 10)
Section header         →  EmptyBorder(7, 7, 3, 7)
Section body container →  EmptyBorder(10, 5, 10, 5)
Individual row/item    →  EmptyBorder(5, 5, 5, 5)
Text components        →  EmptyBorder(0, 0, 0, 0)
```

### Visual dividers between sections

Use a compound border: a thin `MatteBorder` as the dividing line, combined with
an `EmptyBorder` for internal padding:

```java
panel.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createMatteBorder(5, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR),
    BorderFactory.createEmptyBorder(5, 5, 5, 5)
));
```

---

## Collapsible Sections

### Implementation pattern

Track collapsed state via the visibility of the body panel — avoid a separate
boolean field:

```java
protected void collapse() {
    if (!isCollapsed()) {
        bodyPanel.setVisible(false);
        applyDimmer(false, headerPanel);
        expandIcon.setIcon(COLLAPSED_ICON);
    }
}

protected void expand() {
    if (isCollapsed()) {
        bodyPanel.setVisible(true);
        applyDimmer(true, headerPanel);
        expandIcon.setIcon(EXPANDED_ICON);
    }
}

public boolean isCollapsed() {
    return !bodyPanel.isVisible();
}
```

`applyDimmer` calls `setBackground(bg.darker())` or `setBackground(bg.brighter())`
on the header to visually communicate the collapsed state.

### Attaching toggle listeners

Attach the same `MouseListener` to both the collapse icon and the header label
so users can click anywhere on the header row:

```java
MouseAdapter toggleListener = new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (isCollapsed()) expand(); else collapse();
        }
    }
};
headerLabel.addMouseListener(toggleListener);
expandIcon.addMouseListener(toggleListener);
```

---

## Dropdown (JComboBox) Patterns

### Always use a custom renderer

The default `DefaultListCellRenderer` does not respect `ColorScheme`. Extend it:

```java
public class ThemedDropdownRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(
        JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus) {

        super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        setBackground(isSelected ? list.getBackground() : ColorScheme.DARK_GRAY_COLOR);
        setForeground(isSelected ? ColorScheme.LIGHT_GRAY_COLOR : Color.WHITE);

        if (value instanceof Enum) {
            setText(Text.titleCase((Enum<?>) value));  // "SHOW_ALL" → "Show All"
        }
        return this;
    }
}
```

### ItemListener guard

Only act on `SELECTED` events to avoid double-firing when the selection changes:

```java
combo.addItemListener(e -> {
    if (e.getStateChange() == ItemEvent.SELECTED) {
        handleSelection((MyEnum) e.getItem());
    }
});
```

---

## Search / Dynamic Filtering

### Use DocumentListener for text search

`DocumentListener` fires on every keystroke — all three methods should call the
same handler:

```java
searchField.getDocument().addDocumentListener(new DocumentListener() {
    @Override public void insertUpdate(DocumentEvent e) { onSearchChanged(); }
    @Override public void removeUpdate(DocumentEvent e) { onSearchChanged(); }
    @Override public void changedUpdate(DocumentEvent e) { onSearchChanged(); }
});
```

### Efficient partial-refresh (search vs. full rebuild)

For search/filter changes that don't alter the full data set, prefer removing
and re-adding existing panel instances rather than rebuilding them from scratch:

```java
private void showMatchingPanels(String query) {
    allPanels.forEach(listContainer::remove);

    if (query == null || query.isEmpty()) {
        allPanels.forEach(listContainer::add);
    } else {
        String[] terms = query.toLowerCase().split(" ");
        allPanels.stream()
            .filter(p -> Text.matchesSearchTerms(Arrays.asList(terms), p.getKeywords()))
            .forEach(listContainer::add);
    }

    listContainer.revalidate();
    listContainer.repaint();
}
```

Store searchable keywords in the panel itself (set at construction time from
the underlying data object) so filtering does not need to re-query the model.

### Full rebuild after filter/sort changes

When the filter predicate or sort order changes at the data level, do a full
rebuild of the panel list — clear, recreate, add section headers if needed,
then re-apply the current search string:

```java
public void refresh(List<MyItem> items) {
    allPanels.forEach(listContainer::remove);
    allPanels.clear();

    for (MyItem item : items) {
        allPanels.add(new MyItemPanel(item));
    }

    allPanels.forEach(listContainer::add);
    listContainer.revalidate();
    listContainer.repaint();

    // Re-apply current search so it survives the rebuild:
    showMatchingPanels(searchField.getText());
}
```

### Stream-based filter composition

Compose multiple independent filters as Java stream predicates for clean,
testable filtering logic:

```java
List<MyItem> filtered = allItems.stream()
    .filter(config.typeFilter())        // type predicate
    .filter(config.stateFilter())       // state predicate
    .sorted(config.ordering())          // sort comparator
    .collect(Collectors.toList());
```

Define each predicate/comparator as a method on the config enum so the filter
pipeline remains readable.

---

## Revalidate & Repaint

After any structural change (adding, removing, or resizing a component), call:

```java
container.revalidate();   // recompute layout
container.repaint();      // redraw
```

Call them on the smallest container that contains all changed children —
calling on a parent works but is wasteful. Always do this pair together; a
single `repaint()` without `revalidate()` can leave stale layout artifacts.

---

## Empty-State Messaging

When a filtered list is empty, show a descriptive label in place of the list.
Toggle it via `setVisible()`:

```java
emptyMessage.setText("No triggers match your current filters.");
emptyMessage.setVisible(filteredList.isEmpty());
listContainer.setVisible(!filteredList.isEmpty());
```

Do not hide the panel itself — only the inner content.

---

## ConcurrentModification Safety

If panel lists can be iterated while another thread (e.g., the EDT via an
event) also modifies them, use `CopyOnWriteArrayList`:

```java
private final List<MyItemPanel> panels = new CopyOnWriteArrayList<>();
```

This is especially relevant for any collection iterated during a game-tick
update while the EDT might be removing entries in response to a shutdown.
