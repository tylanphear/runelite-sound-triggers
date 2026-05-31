package com.soundtriggers;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * A collapsible card representing one {@link SoundTrigger}.
 *
 * <p>Click the header row to expand/collapse the configuration fields.
 */
public class TriggerPanel extends JPanel
{
	private final SoundTrigger trigger;
	private final SoundTriggersPlugin plugin;
	private final SoundTriggersPanel parentPanel;

	private static final String PLACEHOLDER = "(unnamed)";

	private final JTextField headerNameField;
	private final JPanel detailsPanel;
	private boolean expanded = false;

	public TriggerPanel(SoundTrigger trigger, SoundTriggersPlugin plugin, SoundTriggersPanel parentPanel)
	{
		this.trigger = trigger;
		this.plugin = plugin;
		this.parentPanel = parentPanel;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(
			new MatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(0, 0, 0, 0)
		));

		headerNameField = new JTextField()
		{
			@Override
			public Dimension getPreferredSize()
			{
				Dimension d = super.getPreferredSize();
				java.awt.Font f = getFont();
				if (f == null) return d;
				java.awt.FontMetrics fm = getFontMetrics(f);
				if (fm == null) return d;
				String display = getText().isEmpty() ? PLACEHOLDER : getText();
				int w = fm.stringWidth(display) + getInsets().left + getInsets().right + 6;
				return new Dimension(Math.max(w, 60), d.height);
			}
		};
		add(buildHeader(), BorderLayout.NORTH);

		detailsPanel = buildDetails();
		detailsPanel.setVisible(false);
		add(detailsPanel, BorderLayout.CENTER);
	}

	// -------------------------------------------------------------------------
	// Header
	// -------------------------------------------------------------------------

	private JPanel buildHeader()
	{
		JPanel header = new JPanel(new BorderLayout(5, 0));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(new EmptyBorder(6, 8, 6, 8));
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JCheckBox toggle = new JCheckBox();
		toggle.setSelected(trigger.isEnabled());
		toggle.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		toggle.setToolTipText("Enable / disable this trigger");
		toggle.addActionListener(e ->
		{
			trigger.setEnabled(toggle.isSelected());
			plugin.saveTriggers();
		});

		boolean hasName = trigger.getName() != null && !trigger.getName().isEmpty();
		headerNameField.setText(hasName ? trigger.getName() : PLACEHOLDER);
		headerNameField.setForeground(hasName ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
		headerNameField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerNameField.setCaretColor(Color.WHITE);
		headerNameField.setBorder(BorderFactory.createEmptyBorder());
		headerNameField.setFont(FontManager.getRunescapeSmallFont());
		headerNameField.setToolTipText("Click to rename this trigger");
		headerNameField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusGained(FocusEvent e)
			{
				if (PLACEHOLDER.equals(headerNameField.getText()))
				{
					headerNameField.setText("");
					headerNameField.setForeground(Color.WHITE);
				}
			}

			@Override
			public void focusLost(FocusEvent e)
			{
				if (headerNameField.getText().isEmpty())
				{
					headerNameField.setText(PLACEHOLDER);
					headerNameField.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				}
			}
		});
		headerNameField.addActionListener(e -> headerNameField.transferFocus());
		headerNameField.getDocument().addDocumentListener(new DocumentListener()
		{
			private void update()
			{
				String text = headerNameField.getText();
				trigger.setName(PLACEHOLDER.equals(text) ? "" : text);
				plugin.saveTriggers();
				headerNameField.revalidate();
			}

			@Override public void insertUpdate(DocumentEvent e) { update(); }
			@Override public void removeUpdate(DocumentEvent e) { update(); }
			@Override public void changedUpdate(DocumentEvent e) { update(); }
		});

		JButton deleteButton = new JButton("✕");
		deleteButton.setFont(deleteButton.getFont().deriveFont(9f));
		deleteButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		deleteButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		deleteButton.setBorder(new EmptyBorder(2, 6, 2, 6));
		deleteButton.setFocusPainted(false);
		deleteButton.setToolTipText("Delete this trigger");
		deleteButton.addActionListener(e ->
		{
			plugin.getTriggers().remove(trigger);
			plugin.saveTriggers();
			parentPanel.rebuild();
		});

		// Expand/collapse on header click (but not on the name field, checkbox, or delete button)
		MouseAdapter toggleExpand = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				expanded = !expanded;
				detailsPanel.setVisible(expanded);
				parentPanel.refreshLayout();
			}
		};
		header.addMouseListener(toggleExpand);

		JPanel left = new JPanel(new BorderLayout(4, 0));
		left.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		left.addMouseListener(toggleExpand);
		JPanel nameContainer = new JPanel(new GridBagLayout());
		nameContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameContainer.addMouseListener(toggleExpand);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		nameContainer.add(headerNameField, gbc);

		left.add(toggle, BorderLayout.WEST);
		left.add(nameContainer, BorderLayout.CENTER);

		header.add(left, BorderLayout.CENTER);
		header.add(deleteButton, BorderLayout.EAST);

		return header;
	}

	// -------------------------------------------------------------------------
	// Details panel
	// -------------------------------------------------------------------------

	private JPanel buildDetails()
	{
		JPanel details = new JPanel();
		details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
		details.setBackground(ColorScheme.DARK_GRAY_COLOR);
		details.setBorder(new EmptyBorder(6, 10, 10, 10));

		// Sound file
		details.add(buildSoundFileRow());
		details.add(Box.createVerticalStrut(4));

		// Volume
		details.add(buildVolumeRow());
		details.add(Box.createVerticalStrut(4));

		// Type selector
		JComboBox<TriggerType> typeBox = new JComboBox<>(TriggerType.values());
		typeBox.setSelectedItem(trigger.getType());
		details.add(makeRow("Type", typeBox));
		details.add(Box.createVerticalStrut(4));

		// Type-specific sections
		JPanel hitsplatSection = buildHitsplatSection();
		JPanel itemSection = buildItemSection();
		JPanel chatSection = buildChatSection();
		JPanel playerSpawnSection = buildPlayerSpawnSection();
		JPanel npcSpawnSection = buildNpcSpawnSection();
		JPanel statusEffectSection = buildStatusEffectSection();

		updateSectionVisibility(hitsplatSection, itemSection, chatSection,
			playerSpawnSection, npcSpawnSection, statusEffectSection, trigger.getType());

		typeBox.addActionListener(e ->
		{
			TriggerType selected = (TriggerType) typeBox.getSelectedItem();
			if (selected == null)
			{
				return;
			}
			trigger.setType(selected);
			plugin.saveTriggers();
			updateSectionVisibility(hitsplatSection, itemSection, chatSection,
				playerSpawnSection, npcSpawnSection, statusEffectSection, selected);
			parentPanel.refreshLayout();
		});

		details.add(hitsplatSection);
		details.add(itemSection);
		details.add(chatSection);
		details.add(playerSpawnSection);
		details.add(npcSpawnSection);
		details.add(statusEffectSection);

		return details;
	}

	private void updateSectionVisibility(JPanel hitsplat, JPanel item, JPanel chat,
		JPanel playerSpawn, JPanel npcSpawn, JPanel statusEffect, TriggerType type)
	{
		hitsplat.setVisible(type == TriggerType.HITSPLAT);
		item.setVisible(type == TriggerType.ITEM_DROP);
		chat.setVisible(type == TriggerType.CHAT_MESSAGE);
		playerSpawn.setVisible(type == TriggerType.PLAYER_SPAWN);
		npcSpawn.setVisible(type == TriggerType.NPC_SPAWN);
		statusEffect.setVisible(type == TriggerType.STATUS_EFFECT);
	}

	// -------------------------------------------------------------------------
	// Type-specific sections
	// -------------------------------------------------------------------------

	private JPanel buildHitsplatSection()
	{
		JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JTextField valueField = new JTextField(
			trigger.getHitsplatValue() != null ? trigger.getHitsplatValue().toString() : "");
		styleTextField(valueField);
		valueField.setToolTipText("Leave blank to match any hitsplat value");
		valueField.getDocument().addDocumentListener(new DocumentListener()
		{
			private void update()
			{
				String text = valueField.getText().trim();
				if (text.isEmpty())
				{
					trigger.setHitsplatValue(null);
				}
				else
				{
					try
					{
						trigger.setHitsplatValue(Integer.parseInt(text));
					}
					catch (NumberFormatException ignored)
					{
						return;
					}
				}
				plugin.saveTriggers();
			}

			@Override public void insertUpdate(DocumentEvent e) { update(); }
			@Override public void removeUpdate(DocumentEvent e) { update(); }
			@Override public void changedUpdate(DocumentEvent e) { update(); }
		});

		JComboBox<HitsplatTarget> targetBox = new JComboBox<>(HitsplatTarget.values());
		targetBox.setSelectedItem(trigger.getHitsplatTarget());
		targetBox.addActionListener(e ->
		{
			trigger.setHitsplatTarget((HitsplatTarget) targetBox.getSelectedItem());
			plugin.saveTriggers();
		});

		section.add(makeRow("Value", valueField));
		section.add(Box.createVerticalStrut(4));
		section.add(makeRow("Target", targetBox));
		section.add(Box.createVerticalStrut(4));

		return section;
	}

	private JPanel buildItemSection()
	{
		JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JTextField field = new JTextField(trigger.getItemName() != null ? trigger.getItemName() : "");
		styleTextField(field);
		field.setToolTipText("Leave blank to match any item name");
		field.getDocument().addDocumentListener(new DocumentListener()
		{
			private void update()
			{
				String text = field.getText().trim();
				trigger.setItemName(text.isEmpty() ? null : text);
				plugin.saveTriggers();
			}

			@Override public void insertUpdate(DocumentEvent e) { update(); }
			@Override public void removeUpdate(DocumentEvent e) { update(); }
			@Override public void changedUpdate(DocumentEvent e) { update(); }
		});

		section.add(makeRow("Item", field));
		section.add(Box.createVerticalStrut(4));
		return section;
	}

	private JPanel buildChatSection()
	{
		JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JTextField field = new JTextField(trigger.getChatPattern() != null ? trigger.getChatPattern() : "");
		styleTextField(field);
		field.setToolTipText("Leave blank to match any chat message");
		field.getDocument().addDocumentListener(new DocumentListener()
		{
			private void update()
			{
				String text = field.getText().trim();
				trigger.setChatPattern(text.isEmpty() ? null : text);
				plugin.saveTriggers();
			}

			@Override public void insertUpdate(DocumentEvent e) { update(); }
			@Override public void removeUpdate(DocumentEvent e) { update(); }
			@Override public void changedUpdate(DocumentEvent e) { update(); }
		});

		section.add(makeRow("Pattern", field));
		section.add(Box.createVerticalStrut(4));
		return section;
	}

	private JPanel buildPlayerSpawnSection()
	{
		JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JTextField field = new JTextField(trigger.getPlayerName() != null ? trigger.getPlayerName() : "");
		styleTextField(field);
		field.setToolTipText("Leave blank to match any player");
		field.getDocument().addDocumentListener(new DocumentListener()
		{
			private void update()
			{
				String text = field.getText().trim();
				trigger.setPlayerName(text.isEmpty() ? null : text);
				plugin.saveTriggers();
			}

			@Override public void insertUpdate(DocumentEvent e) { update(); }
			@Override public void removeUpdate(DocumentEvent e) { update(); }
			@Override public void changedUpdate(DocumentEvent e) { update(); }
		});

		section.add(makeRow("Player", field));
		section.add(Box.createVerticalStrut(4));
		return section;
	}

	private JPanel buildNpcSpawnSection()
	{
		JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JTextField field = new JTextField(trigger.getNpcName() != null ? trigger.getNpcName() : "");
		styleTextField(field);
		field.setToolTipText("Leave blank to match any NPC");
		field.getDocument().addDocumentListener(new DocumentListener()
		{
			private void update()
			{
				String text = field.getText().trim();
				trigger.setNpcName(text.isEmpty() ? null : text);
				plugin.saveTriggers();
			}

			@Override public void insertUpdate(DocumentEvent e) { update(); }
			@Override public void removeUpdate(DocumentEvent e) { update(); }
			@Override public void changedUpdate(DocumentEvent e) { update(); }
		});

		section.add(makeRow("NPC", field));
		section.add(Box.createVerticalStrut(4));
		return section;
	}

	private JPanel buildStatusEffectSection()
	{
		JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JComboBox<StatusEffectType> effectBox = new JComboBox<>(StatusEffectType.values());
		effectBox.setSelectedItem(trigger.getStatusEffectType());
		effectBox.addActionListener(e ->
		{
			StatusEffectType selected = (StatusEffectType) effectBox.getSelectedItem();
			if (selected != null)
			{
				trigger.setStatusEffectType(selected);
				plugin.saveTriggers();
			}
		});

		section.add(makeRow("Effect", effectBox));
		section.add(Box.createVerticalStrut(4));
		return section;
	}

	// -------------------------------------------------------------------------
	// Sound file row
	// -------------------------------------------------------------------------

	private JPanel buildSoundFileRow()
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

		JLabel label = makeFieldLabel("Sound");
		label.setPreferredSize(new Dimension(42, 20));

		String savedPath = trigger.getSoundPath() != null ? trigger.getSoundPath() : "";
		JTextField pathField = new JTextField(savedPath);
		styleTextField(pathField);
		pathField.setEditable(false);
		pathField.setToolTipText(savedPath.isEmpty() ? "No file selected" : savedPath);

		JButton browseButton = new JButton("…");
		browseButton.setFont(browseButton.getFont().deriveFont(10f));
		browseButton.setPreferredSize(new Dimension(28, 20));
		browseButton.setFocusPainted(false);
		browseButton.setToolTipText("Choose a WAV file");
		browseButton.addActionListener(e ->
		{
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Select WAV Sound File");
			chooser.setFileFilter(new FileNameExtensionFilter("WAV Audio (*.wav)", "wav"));
			if (!savedPath.isEmpty())
			{
				File current = new File(savedPath);
				if (current.getParentFile() != null)
				{
					chooser.setCurrentDirectory(current.getParentFile());
				}
			}
			if (chooser.showOpenDialog(TriggerPanel.this) == JFileChooser.APPROVE_OPTION)
			{
				String path = chooser.getSelectedFile().getAbsolutePath();
				trigger.setSoundPath(path);
				pathField.setText(path);
				pathField.setToolTipText(path);
				plugin.saveTriggers();
			}
		});

		row.add(label, BorderLayout.WEST);
		row.add(pathField, BorderLayout.CENTER);
		row.add(browseButton, BorderLayout.EAST);

		return row;
	}

	// -------------------------------------------------------------------------
	// Volume row
	// -------------------------------------------------------------------------

	private JPanel buildVolumeRow()
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

		JLabel label = makeFieldLabel("Volume");
		label.setPreferredSize(new Dimension(42, 20));

		JSlider slider = new JSlider(0, 100, trigger.getVolume());
		slider.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel valueLabel = new JLabel(trigger.getVolume() + "%");
		valueLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		valueLabel.setFont(FontManager.getRunescapeSmallFont());
		valueLabel.setPreferredSize(new Dimension(34, 20));

		slider.addChangeListener(e ->
		{
			int vol = slider.getValue();
			valueLabel.setText(vol + "%");
			trigger.setVolume(vol);
			if (!slider.getValueIsAdjusting())
			{
				plugin.saveTriggers();
			}
		});

		row.add(label, BorderLayout.WEST);
		row.add(slider, BorderLayout.CENTER);
		row.add(valueLabel, BorderLayout.EAST);

		return row;
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * Creates a two-column row: a right-aligned label on the left and a
	 * component filling the remaining width on the right.
	 */
	private JPanel makeRow(String labelText, java.awt.Component component)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		row.setBorder(new EmptyBorder(1, 0, 1, 0));

		JLabel label = makeFieldLabel(labelText);
		label.setPreferredSize(new Dimension(42, 20));

		row.add(label, BorderLayout.WEST);
		row.add(component, BorderLayout.CENTER);
		return row;
	}

	private static JLabel makeFieldLabel(String text)
	{
		JLabel label = new JLabel(text + ":");
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setFont(FontManager.getRunescapeSmallFont());
		return label;
	}

	private static void styleTextField(JTextField field)
	{
		field.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		field.setForeground(Color.WHITE);
		field.setCaretColor(Color.WHITE);
		field.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
	}
}
