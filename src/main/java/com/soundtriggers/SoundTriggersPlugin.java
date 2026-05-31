package com.soundtriggers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

import javax.inject.Inject;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@PluginDescriptor(
	name = "Sound Triggers",
	description = "Play custom sound files when configurable in-game events occur",
	tags = {"sound", "audio", "triggers", "hitsplat", "notifications"}
)
public class SoundTriggersPlugin extends Plugin
{
	static final String CONFIG_GROUP = "soundtriggers";
	static final String CONFIG_KEY_TRIGGERS = "triggers";

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private AudioPlayer audioPlayer;

	@Inject
	private Gson gson;

	private static final int VENOM_VALUE_CUTOFF = 1_000_000;

	private SoundTriggersPanel panel;
	private NavigationButton navButton;
	private List<SoundTrigger> triggers = new ArrayList<>();
	private int lastPoisonVarbit = 0;

	@Override
	protected void startUp()
	{
		lastPoisonVarbit = client.getVarpValue(VarPlayerID.POISON);
		loadTriggers();

		panel = new SoundTriggersPanel(this);

		navButton = NavigationButton.builder()
			.tooltip("Sound Triggers")
			.icon(buildIcon())
			.priority(5)
			.panel(panel)
			.build();

		SwingUtilities.invokeLater(() -> clientToolbar.addNavigation(navButton));
	}

	@Override
	protected void shutDown()
	{
		saveTriggers();
		SwingUtilities.invokeLater(() -> clientToolbar.removeNavigation(navButton));
		panel = null;
		navButton = null;
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		int amount = event.getHitsplat().getAmount();
		Actor actor = event.getActor();
		boolean isLocalPlayer = (actor instanceof Player)
			&& actor == client.getLocalPlayer();

		for (SoundTrigger trigger : new ArrayList<>(triggers))
		{
			if (!trigger.isEnabled() || trigger.getType() != TriggerType.HITSPLAT)
			{
				continue;
			}

			if (trigger.getHitsplatValue() != null && trigger.getHitsplatValue() != amount)
			{
				continue;
			}

			HitsplatTarget target = trigger.getHitsplatTarget();
			if (target == HitsplatTarget.SELF && !isLocalPlayer)
			{
				continue;
			}
			if (target == HitsplatTarget.OTHERS && isLocalPlayer)
			{
				continue;
			}

			playTrigger(trigger);
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		String itemName = itemManager.getItemComposition(event.getItem().getId()).getName();

		for (SoundTrigger trigger : new ArrayList<>(triggers))
		{
			if (!trigger.isEnabled() || trigger.getType() != TriggerType.ITEM_DROP)
			{
				continue;
			}

			if (trigger.getItemName() != null
				&& !itemName.toLowerCase().contains(trigger.getItemName().toLowerCase()))
			{
				continue;
			}

			playTrigger(trigger);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		String message = event.getMessage();

		for (SoundTrigger trigger : new ArrayList<>(triggers))
		{
			if (!trigger.isEnabled() || trigger.getType() != TriggerType.CHAT_MESSAGE)
			{
				continue;
			}

			if (trigger.getChatPattern() != null
				&& !message.toLowerCase().contains(trigger.getChatPattern().toLowerCase()))
			{
				continue;
			}

			playTrigger(trigger);
		}
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned event)
	{
		Player player = event.getPlayer();
		if (player == client.getLocalPlayer())
		{
			return;
		}

		String name = player.getName();

		for (SoundTrigger trigger : new ArrayList<>(triggers))
		{
			if (!trigger.isEnabled() || trigger.getType() != TriggerType.PLAYER_SPAWN)
			{
				continue;
			}

			if (trigger.getPlayerName() != null
				&& (name == null || !name.toLowerCase().contains(trigger.getPlayerName().toLowerCase())))
			{
				continue;
			}

			playTrigger(trigger);
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		String name = npc.getName();

		for (SoundTrigger trigger : new ArrayList<>(triggers))
		{
			if (!trigger.isEnabled() || trigger.getType() != TriggerType.NPC_SPAWN)
			{
				continue;
			}

			if (trigger.getNpcName() != null
				&& (name == null || !name.toLowerCase().contains(trigger.getNpcName().toLowerCase())))
			{
				continue;
			}

			playTrigger(trigger);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() != VarPlayerID.POISON)
		{
			return;
		}

		int newValue = event.getValue();
		int oldValue = lastPoisonVarbit;
		lastPoisonVarbit = newValue;

		if (oldValue != 0 || newValue == 0)
		{
			return;
		}

		boolean isVenom = newValue >= VENOM_VALUE_CUTOFF;

		for (SoundTrigger trigger : new ArrayList<>(triggers))
		{
			if (!trigger.isEnabled() || trigger.getType() != TriggerType.STATUS_EFFECT)
			{
				continue;
			}

			StatusEffectType effectType = trigger.getStatusEffectType();
			if (effectType == StatusEffectType.POISON && isVenom)
			{
				continue;
			}
			if (effectType == StatusEffectType.VENOM && !isVenom)
			{
				continue;
			}

			playTrigger(trigger);
		}
	}

	public List<SoundTrigger> getTriggers()
	{
		return triggers;
	}

	public void saveTriggers()
	{
		String json = gson.toJson(triggers);
		configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_TRIGGERS, json);
	}

	private void loadTriggers()
	{
		String json = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY_TRIGGERS);
		if (json != null && !json.isEmpty())
		{
			Type listType = new TypeToken<List<SoundTrigger>>()
			{
			}.getType();
			List<SoundTrigger> loaded = gson.fromJson(json, listType);
			if (loaded != null)
			{
				triggers = loaded;
				return;
			}
		}
		triggers = new ArrayList<>();
	}

	private void playTrigger(SoundTrigger trigger)
	{
		String path = trigger.getSoundPath();
		if (path == null || path.isEmpty())
		{
			return;
		}

		File file = new File(path);
		if (!file.exists())
		{
			log.warn("Sound Triggers: sound file not found: {}", path);
			return;
		}

		try
		{
			audioPlayer.play(file, trigger.getVolume() / 100.0f);
		}
		catch (UnsupportedAudioFileException | IOException | LineUnavailableException e)
		{
			log.warn("Sound Triggers: failed to play {}", path, e);
		}
	}

	/** Draws a simple speaker icon programmatically — no external resource needed. */
	private static BufferedImage buildIcon()
	{
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(200, 200, 200));

		// Speaker body
		g.fillRect(1, 5, 4, 6);

		// Speaker cone
		int[] xPoints = {5, 11, 11, 5};
		int[] yPoints = {5, 1, 15, 11};
		g.fillPolygon(xPoints, yPoints, 4);

		// Sound waves
		g.setColor(new Color(150, 220, 150));
		g.drawArc(11, 4, 3, 8, -60, 120);

		g.dispose();
		return img;
	}

	@Provides
	SoundTriggersConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SoundTriggersConfig.class);
	}
}
