package com.soundtriggers;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;

public class SoundTriggersPanel extends PluginPanel
{
	private final SoundTriggersPlugin plugin;
	private final ScrollablePanel triggersContainer;
	private final JScrollPane scrollPane;
	private final JPanel restrictedBanner;

	/** When set, the rebuilt card for this trigger opens straight into a name edit. */
	private SoundTrigger triggerToRename;

	public SoundTriggersPanel(SoundTriggersPlugin plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// ---- Header ----
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel("Sound Triggers");
		title.setForeground(Color.WHITE);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
		title.setAlignmentX(0.0f);

		JButton addButton = new JButton("New");
		SwingUtil.removeButtonDecorations(addButton);
		addButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
		addButton.setForeground(Color.WHITE);
		addButton.setOpaque(true);
		addButton.setContentAreaFilled(true);
		addButton.setToolTipText("Add a new trigger");
		addButton.addActionListener(e -> addTrigger());

		JPanel buttonsRow = new JPanel(new GridLayout(1, 1, 4, 0));
		buttonsRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		buttonsRow.setAlignmentX(0.0f);
		buttonsRow.add(addButton);

		restrictedBanner = new JPanel(new BorderLayout());
		restrictedBanner.setBackground(new Color(140, 80, 0));
		restrictedBanner.setBorder(new EmptyBorder(5, 8, 5, 8));
		JLabel bannerLabel = new JLabel("Sound triggers are disabled in this region.");
		bannerLabel.setForeground(Color.WHITE);
		bannerLabel.setFont(bannerLabel.getFont().deriveFont(Font.PLAIN, 11f));
		restrictedBanner.add(bannerLabel, BorderLayout.CENTER);
		restrictedBanner.setVisible(false);

		header.add(title);
		header.add(Box.createVerticalStrut(8));
		header.add(buttonsRow);
		header.add(Box.createVerticalStrut(8));
		header.add(restrictedBanner);

		// ---- Trigger list ----
		triggersContainer = new ScrollablePanel();
		triggersContainer.setLayout(new BoxLayout(triggersContainer, BoxLayout.Y_AXIS));
		triggersContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		triggersContainer.setBorder(new EmptyBorder(4, 4, 4, 4));

		scrollPane = new JScrollPane(triggersContainer);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(null);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));

		add(header, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);

		rebuild();
	}

	/** Clears and repopulates the trigger list from the plugin's current trigger state. */
	public void rebuild()
	{
		int savedScroll = scrollPane.getVerticalScrollBar().getValue();

		triggersContainer.removeAll();

		for (SoundTrigger trigger : plugin.getTriggers())
		{
			TriggerPanel panel = new TriggerPanel(trigger, plugin, this);
			triggersContainer.add(panel);
			triggersContainer.add(Box.createVerticalStrut(4));

			if (trigger == triggerToRename)
			{
				panel.expand();
				panel.beginRename();
			}
		}
		triggerToRename = null;

		refreshLayout();
		SwingUtilities.invokeLater(() ->
			scrollPane.getVerticalScrollBar().setValue(savedScroll));
	}

	public void setRegionRestricted(boolean restricted)
	{
		restrictedBanner.setVisible(restricted);
		refreshLayout();
	}

	void refreshLayout()
	{
		revalidate();
		repaint();
	}

	private void importTrigger()
	{
		String json;
		try
		{
			json = (String) Toolkit.getDefaultToolkit()
				.getSystemClipboard().getData(DataFlavor.stringFlavor);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this,
				"Could not read clipboard.", "Import Failed",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		SoundTrigger trigger = plugin.importTriggerFromJson(json);
		if (trigger == null)
		{
			JOptionPane.showMessageDialog(this,
				"Clipboard does not contain a valid trigger.", "Import Failed",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		boolean needsSound = trigger.getSoundSource() == SoundSource.FILE;
		plugin.getTriggers().add(trigger);
		plugin.saveTriggers();
		rebuild();

		if (needsSound)
		{
			JOptionPane.showMessageDialog(this,
				"The imported trigger uses a custom sound file. You'll need to select your own sound file for it before it will trigger.",
				"Sound File Required",
				JOptionPane.WARNING_MESSAGE);
		}
	}

	private void addTrigger()
	{
		SoundTrigger trigger = new SoundTrigger();
		plugin.getTriggers().add(trigger);
		plugin.saveTriggers();
		// Drop the new trigger's card straight into a name edit on rebuild.
		triggerToRename = trigger;
		rebuild();
	}

	private static class ScrollablePanel extends JPanel implements Scrollable
	{
		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return visibleRect.height;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}
