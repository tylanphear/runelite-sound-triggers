package com.soundtriggers;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;

public class SoundTriggersPanel extends PluginPanel
{
	private final SoundTriggersPlugin plugin;
	private final ScrollablePanel triggersContainer;

	public SoundTriggersPanel(SoundTriggersPlugin plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// ---- Header ----
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel("Sound Triggers");
		title.setForeground(Color.WHITE);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));

		JButton addButton = new JButton("+ Add Trigger");
		addButton.setFocusPainted(false);
		addButton.addActionListener(e -> addTrigger());

		header.add(title, BorderLayout.WEST);
		header.add(addButton, BorderLayout.EAST);

		// ---- Trigger list ----
		triggersContainer = new ScrollablePanel();
		triggersContainer.setLayout(new BoxLayout(triggersContainer, BoxLayout.Y_AXIS));
		triggersContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		triggersContainer.setBorder(new EmptyBorder(4, 4, 4, 4));

		JScrollPane scrollPane = new JScrollPane(triggersContainer);
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
		triggersContainer.removeAll();

		for (SoundTrigger trigger : plugin.getTriggers())
		{
			triggersContainer.add(new TriggerPanel(trigger, plugin, this));
			triggersContainer.add(Box.createVerticalStrut(4));
		}

		refreshLayout();
	}

	void refreshLayout()
	{
		revalidate();
		repaint();
	}

	private void addTrigger()
	{
		plugin.getTriggers().add(new SoundTrigger());
		plugin.saveTriggers();
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
			return 16;
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
