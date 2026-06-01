package com.soundtriggers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link TriggerMatcher}, the plugin's functional core. No live
 * {@code Client} is involved — every rule is exercised on plain
 * {@link SoundTrigger} models and primitive event data.
 */
public class TriggerMatcherTest
{
	private static SoundTrigger trigger(TriggerType type)
	{
		SoundTrigger t = new SoundTrigger();
		t.setType(type);
		t.setEnabled(true);
		return t;
	}

	// ---- shared guard behaviour -------------------------------------------------

	@Test
	public void disabledTriggerNeverMatches()
	{
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setEnabled(false);
		assertFalse(TriggerMatcher.matchesHitsplat(t, 10, true));
	}

	@Test
	public void wrongTypeNeverMatches()
	{
		SoundTrigger t = trigger(TriggerType.ITEM_DROP);
		// An ITEM_DROP trigger must not respond to a hitsplat event.
		assertFalse(TriggerMatcher.matchesHitsplat(t, 10, true));
	}

	// ---- hitsplat ---------------------------------------------------------------

	@Test
	public void hitsplatNullValueMatchesAnyAmount()
	{
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatValue(null);
		t.setHitsplatTarget(HitsplatTarget.ANY);
		assertTrue(TriggerMatcher.matchesHitsplat(t, 0, true));
		assertTrue(TriggerMatcher.matchesHitsplat(t, 999, false));
	}

	@Test
	public void hitsplatExactValueMatchesOnlyThatAmount()
	{
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatValue(50);
		assertTrue(TriggerMatcher.matchesHitsplat(t, 50, true));
		assertFalse(TriggerMatcher.matchesHitsplat(t, 49, true));
	}

	@Test
	public void hitsplatTargetSelfRequiresLocalPlayer()
	{
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatTarget(HitsplatTarget.SELF);
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, true));
		assertFalse(TriggerMatcher.matchesHitsplat(t, 10, false));
	}

	@Test
	public void hitsplatTargetOthersExcludesLocalPlayer()
	{
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatTarget(HitsplatTarget.OTHERS);
		assertFalse(TriggerMatcher.matchesHitsplat(t, 10, true));
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, false));
	}

	// ---- substring matchers (item / chat / player / npc) ------------------------

	@Test
	public void itemBlankFilterNeverFires()
	{
		// A blank item name means the trigger is unconfigured, not "any drop".
		SoundTrigger t = trigger(TriggerType.ITEM_DROP);
		t.setItemName(null);
		assertFalse(TriggerMatcher.matchesItem(t, "Dragon bones"));

		t.setItemName("");
		assertFalse(TriggerMatcher.matchesItem(t, "Dragon bones"));
	}

	@Test
	public void itemFilterIsCaseInsensitiveSubstring()
	{
		SoundTrigger t = trigger(TriggerType.ITEM_DROP);
		t.setItemName("bones");
		assertTrue(TriggerMatcher.matchesItem(t, "Dragon BONES"));
		assertFalse(TriggerMatcher.matchesItem(t, "Dragon dagger"));
	}

	@Test
	public void chatFilterIsCaseInsensitiveSubstring()
	{
		SoundTrigger t = trigger(TriggerType.CHAT_MESSAGE);
		t.setChatPattern("level up");
		assertTrue(TriggerMatcher.matchesChat(t, "Congratulations, you just advanced a LEVEL UP!"));
		assertFalse(TriggerMatcher.matchesChat(t, "You feel something weird."));
	}

	@Test
	public void playerSpawnFilterMatchesSubstring()
	{
		SoundTrigger t = trigger(TriggerType.PLAYER_SEEN);
		t.setPlayerName("zezima");
		assertTrue(TriggerMatcher.matchesPlayerSpawn(t, "Zezima"));
		assertFalse(TriggerMatcher.matchesPlayerSpawn(t, "Durial321"));
	}

	@Test
	public void containsModeMatchesPartialName()
	{
		SoundTrigger t = trigger(TriggerType.NPC_SEEN);
		t.setNpcName("goblin");
		t.setNpcNameMatchMode(MatchMode.CONTAINS);
		assertTrue(TriggerMatcher.matchesNpcSpawn(t, "Cave goblin"));
		assertTrue(TriggerMatcher.matchesNpcSpawn(t, "Goblin"));
	}

	@Test
	public void exactModeRequiresWholeName()
	{
		SoundTrigger t = trigger(TriggerType.NPC_SEEN);
		t.setNpcName("goblin");
		t.setNpcNameMatchMode(MatchMode.EXACT);
		// Case-insensitive but the whole name must match.
		assertTrue(TriggerMatcher.matchesNpcSpawn(t, "Goblin"));
		assertFalse(TriggerMatcher.matchesNpcSpawn(t, "Cave goblin"));
	}

	@Test
	public void exactModePlayerNameRequiresWholeName()
	{
		SoundTrigger t = trigger(TriggerType.PLAYER_SEEN);
		t.setPlayerName("zezima");
		t.setPlayerNameMatchMode(MatchMode.EXACT);
		assertTrue(TriggerMatcher.matchesPlayerSpawn(t, "Zezima"));
		assertFalse(TriggerMatcher.matchesPlayerSpawn(t, "Zezima123"));
	}

	@Test
	public void exactModeWithBlankFilterNeverFires()
	{
		// Exact + blank is unconfigured: it must not fire on every spawn...
		SoundTrigger t = trigger(TriggerType.NPC_SEEN);
		t.setNpcName(null);
		t.setNpcNameMatchMode(MatchMode.EXACT);
		assertFalse(TriggerMatcher.matchesNpcSpawn(t, "Goblin"));

		t.setNpcName("");
		assertFalse(TriggerMatcher.matchesNpcSpawn(t, "Goblin"));
	}

	@Test
	public void containsModeWithBlankFilterFiresOnAny()
	{
		// ...but Contains + blank is the deliberate "anyone in view" alert.
		SoundTrigger t = trigger(TriggerType.NPC_SEEN);
		t.setNpcName(null);
		t.setNpcNameMatchMode(MatchMode.CONTAINS);
		assertTrue(TriggerMatcher.matchesNpcSpawn(t, "Goblin"));

		t.setNpcName("");
		assertTrue(TriggerMatcher.matchesNpcSpawn(t, "Goblin"));
	}

	@Test
	public void spawnFilterWithNullNameDoesNotMatch()
	{
		// A null name can't satisfy a non-null filter, but matches when filter is null.
		SoundTrigger filtered = trigger(TriggerType.NPC_SEEN);
		filtered.setNpcName("goblin");
		assertFalse(TriggerMatcher.matchesNpcSpawn(filtered, null));

		SoundTrigger anyNpc = trigger(TriggerType.NPC_SEEN);
		anyNpc.setNpcName(null);
		assertTrue(TriggerMatcher.matchesNpcSpawn(anyNpc, null));
	}

	// ---- status effect ----------------------------------------------------------

	@Test
	public void statusEffectGainedRequiresGained()
	{
		SoundTrigger t = trigger(TriggerType.STATUS_EFFECT);
		t.setStatusEffectCondition(StatusEffectCondition.GAINED);
		t.setStatusEffectType(StatusEffectType.ANY);
		assertTrue(TriggerMatcher.matchesStatusEffect(t, StatusEffectType.POISON, true, false));
		assertFalse(TriggerMatcher.matchesStatusEffect(t, StatusEffectType.POISON, false, true));
	}

	@Test
	public void statusEffectLostRequiresLost()
	{
		SoundTrigger t = trigger(TriggerType.STATUS_EFFECT);
		t.setStatusEffectCondition(StatusEffectCondition.LOST);
		t.setStatusEffectType(StatusEffectType.ANY);
		assertTrue(TriggerMatcher.matchesStatusEffect(t, StatusEffectType.POISON, false, true));
		assertFalse(TriggerMatcher.matchesStatusEffect(t, StatusEffectType.POISON, true, false));
	}

	@Test
	public void statusEffectPoisonDoesNotFireOnVenom()
	{
		SoundTrigger t = trigger(TriggerType.STATUS_EFFECT);
		t.setStatusEffectCondition(StatusEffectCondition.GAINED);
		t.setStatusEffectType(StatusEffectType.POISON);
		assertTrue(TriggerMatcher.matchesStatusEffect(t, StatusEffectType.POISON, true, false));
		assertFalse(TriggerMatcher.matchesStatusEffect(t, StatusEffectType.VENOM, true, false));
	}

	@Test
	public void statusEffectVenomRequiresVenom()
	{
		SoundTrigger t = trigger(TriggerType.STATUS_EFFECT);
		t.setStatusEffectCondition(StatusEffectCondition.GAINED);
		t.setStatusEffectType(StatusEffectType.VENOM);
		assertTrue(TriggerMatcher.matchesStatusEffect(t, StatusEffectType.VENOM, true, false));
		assertFalse(TriggerMatcher.matchesStatusEffect(t, StatusEffectType.POISON, true, false));
	}

	@Test
	public void statusEffectDiseaseRequiresDisease()
	{
		SoundTrigger t = trigger(TriggerType.STATUS_EFFECT);
		t.setStatusEffectCondition(StatusEffectCondition.GAINED);
		t.setStatusEffectType(StatusEffectType.DISEASE);
		assertTrue(TriggerMatcher.matchesStatusEffect(t, StatusEffectType.DISEASE, true, false));
		assertFalse(TriggerMatcher.matchesStatusEffect(t, StatusEffectType.POISON, true, false));
		assertFalse(TriggerMatcher.matchesStatusEffect(t, StatusEffectType.VENOM, true, false));
	}

	@Test
	public void statusEffectAnyMatchesEveryEffect()
	{
		SoundTrigger t = trigger(TriggerType.STATUS_EFFECT);
		t.setStatusEffectCondition(StatusEffectCondition.GAINED);
		t.setStatusEffectType(StatusEffectType.ANY);
		assertTrue(TriggerMatcher.matchesStatusEffect(t, StatusEffectType.POISON, true, false));
		assertTrue(TriggerMatcher.matchesStatusEffect(t, StatusEffectType.VENOM, true, false));
		assertTrue(TriggerMatcher.matchesStatusEffect(t, StatusEffectType.DISEASE, true, false));
	}

	// ---- stat comparison --------------------------------------------------------

	@Test
	public void statBelowIsStrict()
	{
		assertTrue(TriggerMatcher.matchesStatCondition(19, StatComparison.BELOW, 20));
		assertFalse(TriggerMatcher.matchesStatCondition(20, StatComparison.BELOW, 20));
		assertFalse(TriggerMatcher.matchesStatCondition(21, StatComparison.BELOW, 20));
	}

	@Test
	public void statAboveIsStrict()
	{
		assertTrue(TriggerMatcher.matchesStatCondition(21, StatComparison.ABOVE, 20));
		assertFalse(TriggerMatcher.matchesStatCondition(20, StatComparison.ABOVE, 20));
		assertFalse(TriggerMatcher.matchesStatCondition(19, StatComparison.ABOVE, 20));
	}

	// ---- PLAYER_STAT state machine ---------------------------------------------

	@Test
	public void stepStatPrimeRecordsBaselineWithoutFiring()
	{
		// Condition already true at login: prime must not fire.
		TriggerMatcher.StatStep step = TriggerMatcher.stepStat(
			StatTriggerState.INITIAL, true, true, null, 1_000L, true);
		assertFalse(step.fire);
		assertTrue(step.next.conditionMet);
		assertEquals(0L, step.next.lastFiredMs);
	}

	@Test
	public void stepStatFiresOnRisingEdge()
	{
		// Baseline: condition not met.
		StatTriggerState primed = TriggerMatcher.stepStat(
			StatTriggerState.INITIAL, false, true, null, 1_000L, true).next;

		// Next tick the condition becomes true → edge fire.
		TriggerMatcher.StatStep step = TriggerMatcher.stepStat(
			primed, true, true, null, 2_000L, false);
		assertTrue(step.fire);
		assertEquals(2_000L, step.next.lastFiredMs);
	}

	@Test
	public void stepStatDoesNotRefireWhileConditionHoldsWithoutRepeat()
	{
		StatTriggerState afterEdge = new StatTriggerState(true, 2_000L);
		TriggerMatcher.StatStep step = TriggerMatcher.stepStat(
			afterEdge, true, true, null, 3_000L, false);
		assertFalse(step.fire);
		assertEquals(2_000L, step.next.lastFiredMs);
	}

	@Test
	public void stepStatRepeatsOnlyAfterIntervalElapses()
	{
		StatTriggerState afterEdge = new StatTriggerState(true, 2_000L);

		// 1s later, with a 5s repeat configured: too soon.
		TriggerMatcher.StatStep tooSoon = TriggerMatcher.stepStat(
			afterEdge, true, true, 5, 3_000L, false);
		assertFalse(tooSoon.fire);

		// 5s later: repeat fires and the timestamp advances.
		TriggerMatcher.StatStep due = TriggerMatcher.stepStat(
			afterEdge, true, true, 5, 7_000L, false);
		assertTrue(due.fire);
		assertEquals(7_000L, due.next.lastFiredMs);
	}

	@Test
	public void stepStatDisabledTriggerDoesNotFireButStillTracksState()
	{
		StatTriggerState primed = new StatTriggerState(false, 0L);
		TriggerMatcher.StatStep step = TriggerMatcher.stepStat(
			primed, true, false, null, 2_000L, false);
		assertFalse(step.fire);
		// State still advances so a later re-enable edge-detects correctly.
		assertTrue(step.next.conditionMet);
	}

	@Test
	public void stepStatNoLongerMetClearsConditionForNextEdge()
	{
		StatTriggerState afterEdge = new StatTriggerState(true, 2_000L);
		TriggerMatcher.StatStep dropped = TriggerMatcher.stepStat(
			afterEdge, false, true, null, 3_000L, false);
		assertFalse(dropped.fire);
		assertFalse(dropped.next.conditionMet);

		// Crossing again is a fresh rising edge.
		TriggerMatcher.StatStep reArmed = TriggerMatcher.stepStat(
			dropped.next, true, true, null, 4_000L, false);
		assertTrue(reArmed.fire);
	}
}
