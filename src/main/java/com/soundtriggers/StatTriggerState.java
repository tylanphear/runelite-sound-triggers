package com.soundtriggers;

/**
 * Immutable per-tick snapshot of a {@link TriggerType#PLAYER_STAT} trigger's
 * runtime state.
 *
 * <p>Not persisted; lives only for the duration of a login session. Advanced
 * one game tick at a time by {@link TriggerMatcher#stepStat}, which returns a
 * fresh instance rather than mutating this one — the plugin (the imperative
 * shell) is responsible for storing the returned state.
 */
final class StatTriggerState
{
	/** State for a freshly seen trigger, before its first tick. */
	static final StatTriggerState INITIAL = new StatTriggerState(false, 0L);

	/** Whether the trigger's condition was satisfied on the previous tick. */
	final boolean conditionMet;
	/** Wall-clock time the trigger last fired, used to pace repeats. */
	final long lastFiredMs;

	StatTriggerState(boolean conditionMet, long lastFiredMs)
	{
		this.conditionMet = conditionMet;
		this.lastFiredMs = lastFiredMs;
	}
}
