package com.soundtriggers;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.SwingUtil;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.FontMetrics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
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

	private static final ListCellRenderer<Object> COMBO_RENDERER = new DefaultListCellRenderer()
	{
		@Override
		public Component getListCellRendererComponent(
			JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus)
		{
			super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
			setBorder(new EmptyBorder(2, 4, 2, 4));
			setBackground(isSelected ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARK_GRAY_COLOR);
			setForeground(Color.WHITE);
			return this;
		}
	};

	private final JTextField headerNameField;
	private final JPanel detailsPanel;

	private String originalName;

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
		headerNameField.setToolTipText("Double-click to rename this trigger");
		headerNameField.setEditable(false);
		headerNameField.setFocusable(false);
		headerNameField.setHighlighter(null);
		headerNameField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				commitRename();
			}
		});
		headerNameField.addActionListener(e -> commitRename());
		headerNameField.getInputMap(WHEN_FOCUSED).put(
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelRename");
		headerNameField.getActionMap().put("cancelRename", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) { cancelRename(); }
		});
		headerNameField.getDocument().addDocumentListener(simpleListener(headerNameField::revalidate));

		JButton deleteButton = new JButton("✕");
		SwingUtil.removeButtonDecorations(deleteButton);
		deleteButton.setFont(deleteButton.getFont().deriveFont(9f));
		deleteButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		deleteButton.setBorder(new EmptyBorder(2, 6, 2, 6));
		deleteButton.setToolTipText("Delete this trigger");
		deleteButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e) { deleteButton.setForeground(Color.WHITE); }
			@Override
			public void mouseExited(MouseEvent e)  { deleteButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR); }
		});
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
			public void mousePressed(MouseEvent e)
			{
				// Clicking a non-focusable area doesn't move keyboard focus, so
				// focusLost won't fire on the name field. Commit explicitly here.
				commitRename();
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				toggleExpand();
			}
		};
		header.addMouseListener(toggleExpand);

		// Single click on the name field toggles expand/collapse; double click starts a rename.
		// A timer separates the two: the single-click expand is deferred so a double click
		// can cancel it before it fires.
		Timer nameClickTimer = new Timer(200, e2 -> toggleExpand());
		nameClickTimer.setRepeats(false);
		headerNameField.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (!SwingUtilities.isLeftMouseButton(e))
				{
					return;
				}
				if (e.getClickCount() == 2)
				{
					nameClickTimer.stop();
					if (!headerNameField.isEditable())
					{
						beginRename();
					}
				}
				else if (e.getClickCount() == 1 && !headerNameField.isEditable())
				{
					nameClickTimer.restart();
				}
			}
		});

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

	private void toggleExpand()
	{
		detailsPanel.setVisible(!detailsPanel.isVisible());
		parentPanel.refreshLayout();
	}

	/** Expands the card to reveal its configuration fields. */
	void expand()
	{
		detailsPanel.setVisible(true);
		parentPanel.refreshLayout();
	}

	/**
	 * Switches the name field into editable, focused mode. Used by the double-click
	 * handler and to drop a freshly created trigger straight into a rename.
	 */
	void beginRename()
	{
		originalName = trigger.getName();
		boolean hasName = originalName != null && !originalName.isEmpty();
		if (!hasName)
		{
			headerNameField.setText("");
			headerNameField.setForeground(Color.WHITE);
		}
		headerNameField.setHighlighter(new javax.swing.text.DefaultHighlighter());
		headerNameField.setFocusable(true);
		headerNameField.setEditable(true);
		SwingUtilities.invokeLater(() ->
		{
			headerNameField.requestFocusInWindow();
			headerNameField.selectAll();
		});
	}

	private void commitRename()
	{
		if (!headerNameField.isEditable())
		{
			return;
		}
		String text = headerNameField.getText().trim();
		if (text.isEmpty())
		{
			trigger.setName(null);
			headerNameField.setText(PLACEHOLDER);
			headerNameField.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		}
		else
		{
			trigger.setName(text);
			headerNameField.setForeground(Color.WHITE);
		}
		plugin.saveTriggers();
		headerNameField.setEditable(false);
		headerNameField.setFocusable(false);
		headerNameField.setHighlighter(null);
	}

	private void cancelRename()
	{
		if (!headerNameField.isEditable())
		{
			return;
		}
		trigger.setName(originalName);
		plugin.saveTriggers();
		boolean hasName = originalName != null && !originalName.isEmpty();
		headerNameField.setText(hasName ? originalName : PLACEHOLDER);
		headerNameField.setForeground(hasName ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
		headerNameField.setEditable(false);
		headerNameField.setFocusable(false);
		headerNameField.setHighlighter(null);
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

		details.add(buildSoundSection());
		details.add(Box.createVerticalStrut(8));
		details.add(makeDivider());
		details.add(Box.createVerticalStrut(8));

		// Type selector
		JComboBox<TriggerType> typeBox = new JComboBox<>(TriggerType.values());
		typeBox.setSelectedItem(trigger.getType());
		typeBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		typeBox.setForeground(Color.WHITE);
		typeBox.setFont(FontManager.getRunescapeSmallFont());
		typeBox.setRenderer(COMBO_RENDERER);
		typeBox.setToolTipText("What kind of in-game event this trigger fires on");
		details.add(makeRow("Type", typeBox));
		details.add(Box.createVerticalStrut(4));

		// Type-specific sections
		JPanel hitsplatSection = buildHitsplatSection();
		JPanel itemSection = buildItemSection();
		JPanel chatSection = buildChatSection();
		JPanel npcSeenSection = buildNpcSeenSection();
		JPanel statusEffectSection = buildStatusEffectSection();
		JPanel playerStatSection = buildPlayerStatSection();

		updateSectionVisibility(hitsplatSection, itemSection, chatSection,
			npcSeenSection, statusEffectSection, playerStatSection,
			trigger.getType());

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
				npcSeenSection, statusEffectSection, playerStatSection,
				selected);
			parentPanel.refreshLayout();
		});

		details.add(hitsplatSection);
		details.add(itemSection);
		details.add(chatSection);
		details.add(npcSeenSection);
		details.add(statusEffectSection);
		details.add(playerStatSection);

		return details;
	}

	private void updateSectionVisibility(JPanel hitsplat, JPanel item, JPanel chat,
		JPanel npcSeen, JPanel statusEffect, JPanel playerStat, TriggerType type)
	{
		hitsplat.setVisible(type == TriggerType.HITSPLAT);
		item.setVisible(type == TriggerType.ITEM_DROP);
		chat.setVisible(type == TriggerType.CHAT_MESSAGE);
		npcSeen.setVisible(type == TriggerType.NPC_SEEN);
		statusEffect.setVisible(type == TriggerType.STATUS_EFFECT);
		playerStat.setVisible(type == TriggerType.PLAYER_STAT);
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
		bindNullableInt(valueField, trigger::setHitsplatValue);

		JComboBox<HitsplatTarget> targetBox = makeEnumCombo(
			HitsplatTarget.values(), trigger.getHitsplatTarget(), trigger::setHitsplatTarget);
		targetBox.setToolTipText("Whom the hitsplat lands on");

		JComboBox<HitsplatSource> sourceBox = makeEnumCombo(
			HitsplatSource.values(), trigger.getHitsplatSource(), trigger::setHitsplatSource);
		sourceBox.setToolTipText("Who dealt the hitsplat");

		JComboBox<HitsplatKind> kindBox = makeEnumCombo(
			HitsplatKind.values(), trigger.getHitsplatKind(), trigger::setHitsplatKind);
		kindBox.setToolTipText("Which kind of hitsplat to match (damage, poison, heal, …)");

		section.add(makeRow("Kind", kindBox));
		section.add(Box.createVerticalStrut(4));
		section.add(makeRow("Recipient", targetBox));
		section.add(Box.createVerticalStrut(4));
		section.add(makeRow("Source", sourceBox));
		section.add(Box.createVerticalStrut(4));
		section.add(makeRow("Value", valueField));
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
		field.setToolTipText("Leave blank to match any item drop");
		bindNullableText(field, trigger::setItemName);

		JComboBox<MatchMode> matchBox = makeMatchModeBox(trigger.getItemNameMatchMode(), trigger::setItemNameMatchMode);

		JPanel nameRow = makeRow("Name", field);
		nameRow.setVisible(trigger.getItemNameMatchMode() != MatchMode.ANY);
		matchBox.addActionListener(e ->
		{
			MatchMode selected = (MatchMode) matchBox.getSelectedItem();
			if (selected != null)
			{
				nameRow.setVisible(selected != MatchMode.ANY);
				parentPanel.refreshLayout();
			}
		});

		section.add(makeRow("Match", matchBox));
		section.add(Box.createVerticalStrut(4));
		section.add(nameRow);
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
		bindNullableText(field, trigger::setChatPattern);

		JComboBox<MatchMode> matchBox = makeMatchModeBox(trigger.getChatPatternMatchMode(), trigger::setChatPatternMatchMode);

		JPanel patternRow = makeRow("Pattern", field);
		patternRow.setVisible(trigger.getChatPatternMatchMode() != MatchMode.ANY);
		matchBox.addActionListener(e ->
		{
			MatchMode selected = (MatchMode) matchBox.getSelectedItem();
			if (selected != null)
			{
				patternRow.setVisible(selected != MatchMode.ANY);
				parentPanel.refreshLayout();
			}
		});

		section.add(makeRow("Match", matchBox));
		section.add(Box.createVerticalStrut(4));
		section.add(patternRow);
		section.add(Box.createVerticalStrut(4));
		return section;
	}

	private JPanel buildNpcSeenSection()
	{
		JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JTextField field = new JTextField(trigger.getNpcName() != null ? trigger.getNpcName() : "");
		styleTextField(field);
		field.setToolTipText("Leave blank to match any NPC");
		bindNullableText(field, trigger::setNpcName);

		JComboBox<MatchMode> matchBox = makeMatchModeBox(trigger.getNpcNameMatchMode(), trigger::setNpcNameMatchMode);

		JPanel nameRow = makeRow("Name", field);
		nameRow.setVisible(trigger.getNpcNameMatchMode() != MatchMode.ANY);
		matchBox.addActionListener(e ->
		{
			MatchMode selected = (MatchMode) matchBox.getSelectedItem();
			if (selected != null)
			{
				nameRow.setVisible(selected != MatchMode.ANY);
				parentPanel.refreshLayout();
			}
		});

		section.add(makeRow("Match", matchBox));
		section.add(Box.createVerticalStrut(4));
		section.add(nameRow);
		section.add(Box.createVerticalStrut(4));
		return section;
	}

	private JPanel buildStatusEffectSection()
	{
		JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JComboBox<StatusEffectType> effectBox = makeEnumCombo(
			StatusEffectType.values(), trigger.getStatusEffectType(), trigger::setStatusEffectType);

		JComboBox<StatusEffectCondition> conditionBox = makeEnumCombo(
			StatusEffectCondition.values(), trigger.getStatusEffectCondition(), trigger::setStatusEffectCondition);

		section.add(makeRow("Effect", effectBox));
		section.add(Box.createVerticalStrut(4));
		section.add(makeRow("Condition", conditionBox));
		section.add(Box.createVerticalStrut(4));
		return section;
	}

	private JPanel buildPlayerStatSection()
	{
		JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JComboBox<PlayerStat> statBox = makeEnumCombo(
			PlayerStat.values(), trigger.getPlayerStat(), trigger::setPlayerStat);

		JComboBox<StatComparison> comparisonBox = makeEnumCombo(
			StatComparison.values(), trigger.getStatComparison(), trigger::setStatComparison);

		JTextField thresholdField = new JTextField(
			trigger.getStatThreshold() != null ? trigger.getStatThreshold().toString() : "");
		styleTextField(thresholdField);
		thresholdField.setToolTipText("The (absolute) value to compare against (e.g. 50). Leave blank to disable.");
		bindNullableInt(thresholdField, trigger::setStatThreshold);

		JTextField repeatField = new JTextField(
			trigger.getStatRepeatSeconds() != null ? trigger.getStatRepeatSeconds().toString() : "");
		styleTextField(repeatField);
		repeatField.setToolTipText(
			"Seconds between repeats while the condition holds. Leave blank to play once when crossing.");
		bindPositiveInt(repeatField, trigger::setStatRepeatSeconds);

		section.add(makeRow("Stat", statBox));
		section.add(Box.createVerticalStrut(4));
		section.add(makeRow("When", comparisonBox));
		section.add(Box.createVerticalStrut(4));
		section.add(makeRow("Value", thresholdField));
		section.add(Box.createVerticalStrut(4));
		section.add(makeRow("Repeat", repeatField));
		section.add(Box.createVerticalStrut(4));
		return section;
	}

	// -------------------------------------------------------------------------
	// Sound section (source toggle + conditional sub-sections)
	// -------------------------------------------------------------------------

	private JPanel buildSoundSection()
	{
		JPanel fileSection = new JPanel();
		fileSection.setLayout(new BoxLayout(fileSection, BoxLayout.Y_AXIS));
		fileSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
		fileSection.add(buildSoundFileRow());
		fileSection.add(Box.createVerticalStrut(4));

		JPanel builtinSection = new JPanel();
		builtinSection.setLayout(new BoxLayout(builtinSection, BoxLayout.Y_AXIS));
		builtinSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
		builtinSection.add(buildBuiltinSoundRow());
		builtinSection.add(Box.createVerticalStrut(4));

		JPanel customSection = new JPanel();
		customSection.setLayout(new BoxLayout(customSection, BoxLayout.Y_AXIS));
		customSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
		customSection.add(buildCustomSoundRow());
		customSection.add(Box.createVerticalStrut(4));

		SoundSource currentSource = trigger.getSoundSource();
		fileSection.setVisible(currentSource == SoundSource.FILE);
		builtinSection.setVisible(currentSource == SoundSource.BUILTIN);
		customSection.setVisible(currentSource == SoundSource.CUSTOM);

		JPanel volumeRow = buildVolumeRow();
		boolean initiallyFile = trigger.getSoundSource() == SoundSource.FILE;
		volumeRow.setEnabled(initiallyFile);
		setChildrenEnabled(volumeRow, initiallyFile);
		setChildrenToolTipText(volumeRow, initiallyFile ? null : "Built-ins use the game's sound effect volume");

		JComboBox<SoundSource> sourceBox = makeEnumCombo(
			SoundSource.values(), trigger.getSoundSource(), src ->
			{
				trigger.setSoundSource(src);
				fileSection.setVisible(src == SoundSource.FILE);
				builtinSection.setVisible(src == SoundSource.BUILTIN);
				customSection.setVisible(src == SoundSource.CUSTOM);
				boolean fileSource = src == SoundSource.FILE;
				volumeRow.setEnabled(fileSource);
				setChildrenEnabled(volumeRow, fileSource);
				setChildrenToolTipText(volumeRow, fileSource ? null : "Built-ins use the game's sound effect volume");
				parentPanel.refreshLayout();
			});
		sourceBox.setToolTipText("Where to get the sound from");

		JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);
		section.add(makeRow("Source", sourceBox));
		section.add(Box.createVerticalStrut(4));
		section.add(fileSection);
		section.add(builtinSection);
		section.add(customSection);
		section.add(volumeRow);

		return section;
	}

	private JPanel buildCustomSoundRow()
	{
		JTextField field = new JTextField(
			trigger.getCustomSoundId() != null ? trigger.getCustomSoundId().toString() : "");
		styleTextField(field);
		field.setToolTipText("Sound effect ID (e.g. 3813 for level-up jingle)");
		bindNullableInt(field, trigger::setCustomSoundId);
		return makeRow("Sound ID", field);
	}

	private JPanel buildBuiltinSoundRow()
	{
		JComboBox<BuiltinSound> soundBox = makeEnumCombo(
			BuiltinSound.values(), trigger.getBuiltinSound(), trigger::setBuiltinSound);
		return makeRow("Sound", soundBox);
	}

	// -------------------------------------------------------------------------
	// Sound file row
	// -------------------------------------------------------------------------

	private JPanel buildSoundFileRow()
	{
		JButton fileButton = new JButton();
		SwingUtil.removeButtonDecorations(fileButton);
		fileButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		fileButton.setForeground(Color.WHITE);
		fileButton.setOpaque(true);
		fileButton.setContentAreaFilled(true);
		fileButton.setFont(FontManager.getRunescapeSmallFont());
		fileButton.setHorizontalAlignment(SwingConstants.LEFT);

		Runnable refreshLabel = () ->
		{
			String path = trigger.getSoundPath();
			if (path == null || path.isEmpty())
			{
				fileButton.setText("Choose a file to play");
				fileButton.setToolTipText("No file selected");
				return;
			}
			fileButton.setToolTipText(path);
			int w = fileButton.getWidth();
			if (w > 0)
			{
				FontMetrics fm = fileButton.getFontMetrics(fileButton.getFont());
				int available = w - fileButton.getInsets().left - fileButton.getInsets().right - 2;
				fileButton.setText(truncatePathLeft(path, fm, available));
			}
			else
			{
				fileButton.setText(path);
			}
		};

		refreshLabel.run();

		fileButton.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				refreshLabel.run();
			}
		});

		fileButton.addActionListener(e ->
		{
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Select WAV Sound File");
			chooser.setFileFilter(new FileNameExtensionFilter("WAV Audio (*.wav)", "wav"));
			String currentPath = trigger.getSoundPath();
			if (currentPath != null && !currentPath.isEmpty())
			{
				File current = new File(currentPath);
				if (current.getParentFile() != null)
				{
					chooser.setCurrentDirectory(current.getParentFile());
				}
			}
			if (chooser.showOpenDialog(TriggerPanel.this) == JFileChooser.APPROVE_OPTION)
			{
				trigger.setSoundPath(chooser.getSelectedFile().getAbsolutePath());
				refreshLabel.run();
				plugin.saveTriggers();
			}
		});

		return makeRow("Sound", fileButton);
	}

	private static String truncatePathLeft(String path, FontMetrics fm, int maxWidth)
	{
		if (fm.stringWidth(path) <= maxWidth)
		{
			return path;
		}
                for (int i = 3; i < path.length(); ++i)
                {
			String candidate = "..." + path.substring(i);
			if (fm.stringWidth(candidate) <= maxWidth)
			{
				return candidate;
			}
		}
		return path.substring(path.length() - 3);
	}

	// -------------------------------------------------------------------------
	// Volume row
	// -------------------------------------------------------------------------

	private static final String[] VOLUME_LABELS = {"Off", "Low", "Mid", "High", "Full"};

	private JPanel buildVolumeRow()
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel label = makeFieldLabel("Volume");
		label.setPreferredSize(new Dimension(42, 20));
		label.setVerticalAlignment(SwingConstants.TOP);

		int level = Math.min(4, Math.max(0, trigger.getVolume()));
		JSlider slider = new JSlider(0, 4, level);
		slider.setBackground(ColorScheme.LIGHT_GRAY_COLOR);
		slider.setSnapToTicks(true);
		slider.setMajorTickSpacing(1);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);

		java.util.Hashtable<Integer, JLabel> labelTable = new java.util.Hashtable<>();
		for (int i = 0; i <= 4; i++)
		{
			JLabel l = new JLabel(VOLUME_LABELS[i]);
			l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			l.setFont(FontManager.getRunescapeSmallFont());
			labelTable.put(i, l);
		}
		slider.setLabelTable(labelTable);

		slider.addChangeListener(e ->
		{
			trigger.setVolume(slider.getValue());
			if (!slider.getValueIsAdjusting())
			{
				plugin.saveTriggers();
			}
		});

		row.add(label, BorderLayout.WEST);
		row.add(slider, BorderLayout.CENTER);

		return row;
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static void setChildrenEnabled(java.awt.Container container, boolean enabled)
	{
		for (java.awt.Component c : container.getComponents())
		{
			c.setEnabled(enabled);
		}
	}

	private static void setChildrenToolTipText(java.awt.Container container, String tooltip)
	{
		for (java.awt.Component c : container.getComponents())
		{
			if (c instanceof javax.swing.JComponent)
			{
				((javax.swing.JComponent) c).setToolTipText(tooltip);
			}
		}
	}

	/**
	 * Creates a two-column row: a right-aligned label on the left and a
	 * component filling the remaining width on the right.
	 */
	private JPanel makeRow(String labelText, java.awt.Component component)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setBorder(new EmptyBorder(1, 0, 1, 0));

		JLabel label = makeFieldLabel(labelText);
		label.setPreferredSize(new Dimension(60, 20));

		row.add(label, BorderLayout.WEST);
		row.add(component, BorderLayout.CENTER);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		return row;
	}

	private static JPanel makeDivider()
	{
		JPanel line = new JPanel();
		line.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
		line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		line.setPreferredSize(new Dimension(0, 1));
		return line;
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
		field.setFont(FontManager.getRunescapeSmallFont());
	}

	/**
	 * Wraps {@code onChange} in a {@link DocumentListener} that fires it on any
	 * insert, remove, or content change — the three callbacks Swing splits an
	 * edit into but which we always treat identically.
	 */
	private static DocumentListener simpleListener(Runnable onChange)
	{
		return new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent e) { onChange.run(); }
			@Override public void removeUpdate(DocumentEvent e) { onChange.run(); }
			@Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
		};
	}

	/**
	 * Binds a text field to a string setter: the trimmed text is passed through,
	 * with blank mapped to {@code null} ("match any"), and saved on every edit.
	 */
	private void bindNullableText(JTextField field, java.util.function.Consumer<String> setter)
	{
		field.getDocument().addDocumentListener(simpleListener(() ->
		{
			String text = field.getText().trim();
			setter.accept(text.isEmpty() ? null : text);
			plugin.saveTriggers();
		}));
	}

	/**
	 * Binds a text field to an integer setter: blank maps to {@code null}, a valid
	 * number is parsed and saved, and unparseable input is ignored so the user can
	 * finish typing without the partial value being persisted.
	 */
	private void bindNullableInt(JTextField field, java.util.function.Consumer<Integer> setter)
	{
		field.getDocument().addDocumentListener(simpleListener(() ->
		{
			String text = field.getText().trim();
			Integer value;
			if (text.isEmpty())
			{
				value = null;
			}
			else
			{
				try
				{
					value = Integer.parseInt(text);
				}
				catch (NumberFormatException ignored)
				{
					return;
				}
			}
			setter.accept(value);
			plugin.saveTriggers();
		}));
	}

	/**
	 * Like {@link #bindNullableInt} but also rejects zero and negative values,
	 * treating them as unparseable (the setter is not called and nothing is saved).
	 */
	private void bindPositiveInt(JTextField field, java.util.function.Consumer<Integer> setter)
	{
		field.getDocument().addDocumentListener(simpleListener(() ->
		{
			String text = field.getText().trim();
			Integer value;
			if (text.isEmpty())
			{
				value = null;
			}
			else
			{
				try
				{
					value = Integer.parseInt(text);
					if (value <= 0)
					{
						return;
					}
				}
				catch (NumberFormatException ignored)
				{
					return;
				}
			}
			setter.accept(value);
			plugin.saveTriggers();
		}));
	}

	private JComboBox<MatchMode> makeMatchModeBox(MatchMode current, java.util.function.Consumer<MatchMode> setter)
	{
		JComboBox<MatchMode> box = makeEnumCombo(MatchMode.values(), current, setter);
		box.setToolTipText("Any: match everything. Contains: match part of the name. Exact: match the whole name.");
		return box;
	}

	/**
	 * Builds a combo box over an enum's values, pre-selects {@code current}, and
	 * saves the chosen value through {@code setter} on every change.
	 */
	private <E> JComboBox<E> makeEnumCombo(E[] values, E current, java.util.function.Consumer<E> setter)
	{
		JComboBox<E> box = new JComboBox<>(values);
		box.setSelectedItem(current != null ? current : values[0]);
		box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		box.setForeground(Color.WHITE);
		box.setFont(FontManager.getRunescapeSmallFont());
		box.setRenderer(COMBO_RENDERER);
		box.addActionListener(e ->
		{
			int index = box.getSelectedIndex();
			if (index >= 0)
			{
				setter.accept(box.getItemAt(index));
				plugin.saveTriggers();
			}
		});
		return box;
	}
}
