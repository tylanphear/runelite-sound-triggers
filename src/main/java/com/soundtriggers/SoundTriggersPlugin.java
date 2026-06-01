package com.soundtriggers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private int lastDiseaseVarbit = 0;

	/** Per-trigger runtime state for PLAYER_STAT triggers, keyed by trigger id. */
	private final Map<String, StatTriggerState> statStates = new HashMap<>();
	/**
	 * Becomes {@code true} once stat baselines have been sampled after login.
	 * Until then we prime each trigger's state without firing, so a condition
	 * that was already true at login does not fire spuriously.
	 */
	private boolean statsPrimed = false;

	/**
	 * Tick of the most recent {@code LOGGED_IN} transition. On login (and on
	 * world-hop / region load) the client spawns every actor already in the
	 * scene on this same tick; we skip those so "Player Seen" / "NPC Seen"
	 * only fire for actors that come into view while actually playing.
	 */
	private int loginTick = -1;

	@Override
	protected void startUp()
	{
		lastPoisonVarbit = client.getVarpValue(VarPlayerID.POISON);
		lastDiseaseVarbit = client.getVarpValue(VarPlayerID.DISEASE);
		statsPrimed = false;
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
		statStates.clear();
		statsPrimed = false;
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
			if (TriggerMatcher.matchesHitsplat(trigger, amount, isLocalPlayer))
			{
				playTrigger(trigger);
			}
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		String itemName = itemManager.getItemComposition(event.getItem().getId()).getName();

		for (SoundTrigger trigger : new ArrayList<>(triggers))
		{
			if (TriggerMatcher.matchesItem(trigger, itemName))
			{
				playTrigger(trigger);
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		String message = event.getMessage();

		for (SoundTrigger trigger : new ArrayList<>(triggers))
		{
			if (TriggerMatcher.matchesChat(trigger, message))
			{
				playTrigger(trigger);
			}
		}
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned event)
	{
		if (isLoginSpawn())
		{
			return;
		}

		Player player = event.getPlayer();
		if (player == client.getLocalPlayer())
		{
			return;
		}

		String name = player.getName();

		for (SoundTrigger trigger : new ArrayList<>(triggers))
		{
			if (TriggerMatcher.matchesPlayerSpawn(trigger, name))
			{
				playTrigger(trigger);
			}
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (isLoginSpawn())
		{
			return;
		}

		NPC npc = event.getNpc();
		String name = npc.getName();

		for (SoundTrigger trigger : new ArrayList<>(triggers))
		{
			if (TriggerMatcher.matchesNpcSpawn(trigger, name))
			{
				playTrigger(trigger);
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() == VarPlayerID.POISON)
		{
			int oldValue = lastPoisonVarbit;
			int newValue = event.getValue();
			lastPoisonVarbit = newValue;

			// Above the cutoff the affliction is venom rather than ordinary poison;
			// read it from whichever side of the transition is non-zero.
			boolean wasVenom = oldValue >= VENOM_VALUE_CUTOFF;
			boolean isVenom = newValue >= VENOM_VALUE_CUTOFF;
			fireStatusEffect(isVenom || wasVenom ? StatusEffectType.VENOM : StatusEffectType.POISON,
				oldValue, newValue);
		}
		else if (event.getVarpId() == VarPlayerID.DISEASE)
		{
			int oldValue = lastDiseaseVarbit;
			int newValue = event.getValue();
			lastDiseaseVarbit = newValue;

			fireStatusEffect(StatusEffectType.DISEASE, oldValue, newValue);
		}
	}

	/**
	 * Fires any matching STATUS_EFFECT triggers for a 0&harr;non-zero transition of
	 * the given affliction. A change that stays non-zero (e.g. poison ticking down,
	 * or poison escalating into venom) is neither a gain nor a loss and is ignored.
	 */
	private void fireStatusEffect(StatusEffectType changed, int oldValue, int newValue)
	{
		boolean gained = oldValue == 0 && newValue != 0;
		boolean lost = oldValue != 0 && newValue == 0;
		if (!gained && !lost)
		{
			return;
		}

		for (SoundTrigger trigger : new ArrayList<>(triggers))
		{
			if (TriggerMatcher.matchesStatusEffect(trigger, changed, gained, lost))
			{
				playTrigger(trigger);
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// Re-prime baselines whenever we (re)enter the world, so leaving and
		// returning (logout, hop, disconnect) does not fire on a condition that
		// was already true on arrival.
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			loginTick = client.getTickCount();
		}
		else
		{
			statsPrimed = false;
		}
	}

	/**
	 * True while we're still on the tick the client logged in / loaded a new
	 * region on. The actor spawns delivered on this tick are the bulk
	 * scene-load batch (everyone already standing around), not actors that
	 * just came into view, so spawn triggers should ignore them.
	 */
	private boolean isLoginSpawn()
	{
		return client.getTickCount() == loginTick;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		boolean prime = !statsPrimed;
		long now = System.currentTimeMillis();

		for (SoundTrigger trigger : new ArrayList<>(triggers))
		{
			if (trigger.getType() != TriggerType.PLAYER_STAT)
			{
				continue;
			}

			Integer threshold = trigger.getStatThreshold();
			boolean conditionMet = threshold != null
				&& TriggerMatcher.matchesStatCondition(sampleStat(trigger.getPlayerStat()),
					trigger.getStatComparison(), threshold);

			StatTriggerState prev = statStates.getOrDefault(
				trigger.getId(), StatTriggerState.INITIAL);

			TriggerMatcher.StatStep step = TriggerMatcher.stepStat(prev, conditionMet,
				trigger.isEnabled(), trigger.getStatRepeatSeconds(), now, prime);

			statStates.put(trigger.getId(), step.next);

			if (step.fire)
			{
				playTrigger(trigger);
			}
		}

		// Drop runtime state for triggers that no longer exist.
		if (statStates.size() > triggers.size())
		{
			statStates.keySet().removeIf(id -> triggers.stream()
				.noneMatch(t -> t.getId().equals(id)));
		}

		statsPrimed = true;
	}

	/** Returns the stat's current value on the same 0&ndash;100 / points scale the player sees. */
	private int sampleStat(PlayerStat stat)
	{
		switch (stat)
		{
			case HEALTH:
				return client.getBoostedSkillLevel(Skill.HITPOINTS);
			case PRAYER:
				return client.getBoostedSkillLevel(Skill.PRAYER);
			case RUN_ENERGY:
				// getEnergy() is 0–10000 (one decimal of precision).
				return client.getEnergy() / 100;
			case SPECIAL_ATTACK:
				// SA_ENERGY varp is 0–1000.
				return client.getVarpValue(VarPlayerID.SA_ENERGY) / 10;
			default:
				return 0;
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
			audioPlayer.play(file, volumePercentToGainDb(trigger.getVolume()));
		}
		catch (UnsupportedAudioFileException | IOException | LineUnavailableException e)
		{
			log.warn("Sound Triggers: failed to play {}", path, e);
		}
	}

	/**
	 * Converts a 0-100 volume percentage into the decibel gain expected by
	 * {@link AudioPlayer#play}. The control is logarithmic: 100% maps to 0 dB
	 * (unchanged), 50% to about -6 dB, and 0% is clamped to a large negative
	 * value to effectively mute playback.
	 */
	private static float volumePercentToGainDb(int volumePercent)
	{
		float fraction = volumePercent / 100.0f;
		if (fraction <= 0.0f)
		{
			return -80.0f;
		}
		return (float) (20.0 * Math.log10(fraction));
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
