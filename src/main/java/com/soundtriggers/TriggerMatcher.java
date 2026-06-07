package com.soundtriggers;

/**
 * The plugin's functional core: pure decisions about whether a given
 * {@link SoundTrigger} should fire for a particular in-game event.
 *
 * <p>Nothing here touches a live {@code Client}, the event bus, or any other
 * RuneLite service. The {@link SoundTriggersPlugin} handlers (the imperative
 * shell) read the volatile bits out of each event — a damage amount, an item
 * name, the local player flag — and pass those primitives in. That split keeps
 * every matching rule unit-testable without booting a client.
 *
 * <p>Each {@code matches*} method also checks {@link SoundTrigger#isEnabled()}
 * and {@link SoundTrigger#getType()}, so callers can simply iterate all
 * triggers and fire on a {@code true} result.
 */
final class TriggerMatcher
{
	private TriggerMatcher()
	{
	}

	static boolean matchesHitsplat(SoundTrigger trigger, int amount, int hitsplatType,
		boolean isLocalPlayer, boolean isMine)
	{
		if (!trigger.isEnabled() || trigger.getType() != TriggerType.HITSPLAT)
		{
			return false;
		}

		if (trigger.getHitsplatValue() != null && trigger.getHitsplatValue() != amount)
		{
			return false;
		}

		HitsplatKind kind = trigger.getHitsplatKind();
		if (kind != null && !kind.matches(hitsplatType))
		{
			return false;
		}

		// Recipient: whom the hitsplat lands on (HitsplatApplied.getActor()).
		HitsplatTarget target = trigger.getHitsplatTarget();
		if (target == HitsplatTarget.SELF && !isLocalPlayer)
		{
			return false;
		}
		if (target == HitsplatTarget.OTHERS && isLocalPlayer)
		{
			return false;
		}

		// Source: who dealt the hitsplat (Hitsplat.isMine()).
		HitsplatSource source = trigger.getHitsplatSource();
		if (source == HitsplatSource.ME && !isMine)
		{
			return false;
		}
		if (source == HitsplatSource.OTHERS && isMine)
		{
			return false;
		}

		return true;
	}

	static boolean matchesItem(SoundTrigger trigger, String itemName)
	{
		if (!trigger.isEnabled() || trigger.getType() != TriggerType.ITEM_DROP)
		{
			return false;
		}

		return matches(itemName, trigger.getItemName(), trigger.getItemNameMatchMode());
	}

	static boolean matchesChat(SoundTrigger trigger, String message)
	{
		if (!trigger.isEnabled() || trigger.getType() != TriggerType.CHAT_MESSAGE)
		{
			return false;
		}

		return matches(message, trigger.getChatPattern(), trigger.getChatPatternMatchMode());
	}

	static boolean matchesPlayerSpawn(SoundTrigger trigger, String playerName)
	{
		if (!trigger.isEnabled() || trigger.getType() != TriggerType.PLAYER_SEEN)
		{
			return false;
		}

		return matches(playerName, trigger.getPlayerName(), trigger.getPlayerNameMatchMode());
	}

	static boolean matchesNpcSpawn(SoundTrigger trigger, String npcName)
	{
		if (!trigger.isEnabled() || trigger.getType() != TriggerType.NPC_SEEN)
		{
			return false;
		}

		return matches(npcName, trigger.getNpcName(), trigger.getNpcNameMatchMode());
	}

	/**
	 * @param changed the concrete effect whose state just flipped — one of
	 *                {@link StatusEffectType#POISON}, {@link StatusEffectType#VENOM},
	 *                or {@link StatusEffectType#DISEASE} (never {@code ANY})
	 */
	static boolean matchesStatusEffect(SoundTrigger trigger, StatusEffectType changed, boolean gained, boolean lost)
	{
		if (!trigger.isEnabled() || trigger.getType() != TriggerType.STATUS_EFFECT)
		{
			return false;
		}

		StatusEffectCondition condition = trigger.getStatusEffectCondition();
		if (condition == null)
		{
			condition = StatusEffectCondition.GAINED;
		}
		if (condition == StatusEffectCondition.GAINED && !gained)
		{
			return false;
		}
		if (condition == StatusEffectCondition.LOST && !lost)
		{
			return false;
		}

		StatusEffectType effectType = trigger.getStatusEffectType();
		if (effectType == null)
		{
			effectType = StatusEffectType.ANY;
		}
		return effectType == StatusEffectType.ANY || effectType == changed;
	}

	static boolean matchesStatCondition(int value, StatComparison comparison, int threshold)
	{
		if (comparison == StatComparison.ABOVE)
		{
			return value > threshold;
		}
		return value < threshold;
	}

	/**
	 * Advances one {@link TriggerType#PLAYER_STAT} trigger by a single game tick.
	 *
	 * @param prev          the trigger's state from the previous tick (use
	 *                      {@link StatTriggerState#INITIAL} the first time)
	 * @param conditionMet  whether the stat currently satisfies the threshold
	 * @param enabled       whether the trigger is enabled
	 * @param repeatSeconds seconds between repeats while the condition holds;
	 *                      {@code null} or {@code 0} means fire once on the edge
	 * @param nowMs         current wall-clock time, in milliseconds
	 * @param prime         {@code true} on the first tick after (re)login: record
	 *                      the baseline without firing, so a condition already
	 *                      true on arrival does not fire spuriously
	 * @return the fire decision plus the trigger's next state
	 */
	static StatStep stepStat(StatTriggerState prev, boolean conditionMet, boolean enabled,
		Integer repeatSeconds, long nowMs, boolean prime)
	{
		// On the first tick after login, only record the baseline.
		if (prime)
		{
			return new StatStep(false, new StatTriggerState(conditionMet, 0L));
		}

		boolean fire = false;
		long lastFiredMs = prev.lastFiredMs;

		if (conditionMet && enabled)
		{
			if (!prev.conditionMet)
			{
				// Edge: the value just crossed the threshold.
				fire = true;
				lastFiredMs = nowMs;
			}
			else if (repeatSeconds != null && repeatSeconds > 0
				&& nowMs - prev.lastFiredMs >= repeatSeconds * 1000L)
			{
				// Condition still holds and an interval has elapsed: repeat.
				fire = true;
				lastFiredMs = nowMs;
			}
		}

		return new StatStep(fire, new StatTriggerState(conditionMet, lastFiredMs));
	}

	/**
	 * {@code true} when {@code haystack} contains {@code filter} ignoring case.
	 */
	private static boolean containsIgnoreCase(String haystack, String filter)
	{
		if (filter == null || filter.isEmpty())
		{
			return false;
		}
		return haystack != null
			&& haystack.toLowerCase().contains(filter.toLowerCase());
	}

	/**
	 * Matches an in-game {@code name} against a {@code filter} per {@code mode}:
	 * {@link MatchMode#ANY} matches any string, {@link MatchMode#CONTAINS}
	 * (the default for a {@code null} mode) does a case-insensitive
	 * substring match, and {@link MatchMode#EXACT} requires
	 * case-insensitive equality.
	 */
	private static boolean matches(String name, String filter, MatchMode mode)
	{
		if (mode == MatchMode.ANY)
		{
			return true;
		}
		if (mode == MatchMode.CONTAINS)
		{
			return containsIgnoreCase(name, filter);
		}
		if (name == null || name.isEmpty() || filter == null || filter.isEmpty())
		{
			return false;
		}
		return name.equalsIgnoreCase(filter);
	}

	/** Result of advancing a {@link TriggerType#PLAYER_STAT} trigger one tick. */
	static final class StatStep
	{
		/** Whether the trigger should fire this tick. */
		final boolean fire;
		/** The trigger's state to carry into the next tick. */
		final StatTriggerState next;

		StatStep(boolean fire, StatTriggerState next)
		{
			this.fire = fire;
			this.next = next;
		}
	}
}
