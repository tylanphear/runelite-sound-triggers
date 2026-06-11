package com.soundtriggers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.runelite.api.HitsplatID;
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
		assertFalse(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, true, true));
	}

	@Test
	public void wrongTypeNeverMatches()
	{
		SoundTrigger t = trigger(TriggerType.ITEM_DROP);
		// An ITEM_DROP trigger must not respond to a hitsplat event.
		assertFalse(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, true, true));
	}

	// ---- hitsplat ---------------------------------------------------------------

	@Test
	public void hitsplatNullValueMatchesAnyAmount()
	{
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatValue(null);
		t.setHitsplatTarget(HitsplatTarget.ANY);
		assertTrue(TriggerMatcher.matchesHitsplat(t, 0, HitsplatID.DAMAGE_ME, true, true));
		assertTrue(TriggerMatcher.matchesHitsplat(t, 999, HitsplatID.DAMAGE_OTHER, false, false));
	}

	@Test
	public void hitsplatExactValueMatchesOnlyThatAmount()
	{
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatValue(50);
		assertTrue(TriggerMatcher.matchesHitsplat(t, 50, HitsplatID.DAMAGE_ME, true, true));
		assertFalse(TriggerMatcher.matchesHitsplat(t, 49, HitsplatID.DAMAGE_ME, true, true));
	}

	@Test
	public void hitsplatTargetSelfRequiresLocalPlayer()
	{
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatTarget(HitsplatTarget.SELF);
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, true, true));
		assertFalse(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, false, false));
	}

	@Test
	public void hitsplatTargetOthersExcludesLocalPlayer()
	{
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatTarget(HitsplatTarget.OTHERS);
		assertFalse(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, true, true));
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, false, false));
	}

	@Test
	public void hitsplatSourceMeRequiresMine()
	{
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatSource(HitsplatSource.ME);
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, true, true));
		assertFalse(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, true, false));
	}

	@Test
	public void hitsplatSourceOthersExcludesMine()
	{
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatSource(HitsplatSource.OTHERS);
		assertFalse(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, true, true));
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, true, false));
	}

	@Test
	public void hitsplatSourceAndRecipientAreIndependent()
	{
		// "I deal damage to another player": source ME, recipient OTHERS.
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatSource(HitsplatSource.ME);
		t.setHitsplatTarget(HitsplatTarget.OTHERS);
		// dealt by me, lands on someone else -> matches
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, false, true));
		// dealt by me, but lands on me -> recipient excludes it
		assertFalse(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, true, true));
		// lands on someone else, but not dealt by me -> source excludes it
		assertFalse(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, false, false));
	}

	@Test
	public void hitsplatNullSourceMatchesAnySource()
	{
		// Triggers persisted before the source filter existed deserialize with a null source.
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatTarget(HitsplatTarget.ANY);
		t.setHitsplatSource(null);
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, true, true));
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_OTHER, false, false));
	}

	@Test
	public void hitsplatKindAnyMatchesEveryType()
	{
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatKind(HitsplatKind.ANY);
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, true, true));
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.POISON, true, true));
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.HEAL, true, true));
	}

	@Test
	public void hitsplatKindMatchesOnlyThatKind()
	{
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatKind(HitsplatKind.POISON);
		assertTrue(TriggerMatcher.matchesHitsplat(t, 4, HitsplatID.POISON, true, true));
		assertFalse(TriggerMatcher.matchesHitsplat(t, 4, HitsplatID.DAMAGE_ME, true, true));
		assertFalse(TriggerMatcher.matchesHitsplat(t, 4, HitsplatID.VENOM, true, true));
	}

	@Test
	public void hitsplatKindCollapsesMeAndOtherAndColourVariants()
	{
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatTarget(HitsplatTarget.ANY);
		t.setHitsplatKind(HitsplatKind.DAMAGE);
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME, true, true));
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_OTHER, false, false));
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_MAX_ME, true, true));
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.DAMAGE_ME_ORANGE, true, true));
		assertFalse(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.HEAL, true, true));
	}

	@Test
	public void hitsplatNullKindMatchesAnyType()
	{
		// Triggers persisted before the kind filter existed deserialize with a null kind.
		SoundTrigger t = trigger(TriggerType.HITSPLAT);
		t.setHitsplatKind(null);
		assertTrue(TriggerMatcher.matchesHitsplat(t, 10, HitsplatID.POISON, true, true));
	}

	// ---- substring matchers (item / chat / player / npc) ------------------------

	@Test
	public void itemBlankFilterDoesNotFireByDefault()
	{
		// Default EXACT mode: blank filter = unconfigured, no fire.
		SoundTrigger t = trigger(TriggerType.ITEM_DROP);
		t.setItemName(null);
		assertFalse(TriggerMatcher.matchesItem(t, "Dragon bones"));

		t.setItemName("");
		assertFalse(TriggerMatcher.matchesItem(t, "Dragon bones"));
	}

	@Test
	public void itemAnyModeMatchesAnyDrop()
	{
		// ANY mode fires regardless of item name or filter value.
		SoundTrigger t = trigger(TriggerType.ITEM_DROP);
		t.setItemNameMatchMode(MatchMode.ANY);
		t.setItemName(null);
		assertTrue(TriggerMatcher.matchesItem(t, "Dragon bones"));

		t.setItemName("something");
		assertTrue(TriggerMatcher.matchesItem(t, "Dragon bones"));
	}

	@Test
	public void itemBlankFilterWithContainsModeDoesNotFire()
	{
		// CONTAINS + blank is unconfigured, not "fire on any drop" — use ANY for that.
		SoundTrigger t = trigger(TriggerType.ITEM_DROP);
		t.setItemNameMatchMode(MatchMode.CONTAINS);
		t.setItemName(null);
		assertFalse(TriggerMatcher.matchesItem(t, "Dragon bones"));

		t.setItemName("");
		assertFalse(TriggerMatcher.matchesItem(t, "Dragon bones"));
	}

	@Test
	public void itemFilterIsCaseInsensitiveSubstring()
	{
		SoundTrigger t = trigger(TriggerType.ITEM_DROP);
		t.setItemNameMatchMode(MatchMode.CONTAINS);
		t.setItemName("bones");
		assertTrue(TriggerMatcher.matchesItem(t, "Dragon BONES"));
		assertFalse(TriggerMatcher.matchesItem(t, "Dragon dagger"));
	}

	@Test
	public void itemFilterExactMatchMode()
	{
		SoundTrigger t = trigger(TriggerType.ITEM_DROP);
		t.setItemName("Dragon bones");
		t.setItemNameMatchMode(MatchMode.EXACT);
		assertTrue(TriggerMatcher.matchesItem(t, "Dragon bones"));
		assertTrue(TriggerMatcher.matchesItem(t, "DRAGON BONES"));
		assertFalse(TriggerMatcher.matchesItem(t, "Dragon bones (noted)"));
	}

	@Test
	public void chatFilterIsCaseInsensitiveSubstring()
	{
		SoundTrigger t = trigger(TriggerType.CHAT_MESSAGE);
		t.setChatPatternMatchMode(MatchMode.CONTAINS);
		t.setChatPattern("level up");
		assertTrue(TriggerMatcher.matchesChat(t, "Congratulations, you just advanced a LEVEL UP!"));
		assertFalse(TriggerMatcher.matchesChat(t, "You feel something weird."));
	}

	@Test
	public void chatBlankFilterDoesNotFireByDefault()
	{
		// Default EXACT mode: blank pattern = unconfigured, no fire.
		SoundTrigger t = trigger(TriggerType.CHAT_MESSAGE);
		t.setChatPattern(null);
		assertFalse(TriggerMatcher.matchesChat(t, "Anything at all."));

		t.setChatPattern("");
		assertFalse(TriggerMatcher.matchesChat(t, "Anything at all."));
	}

	@Test
	public void chatAnyModeMatchesAnyMessage()
	{
		// ANY mode fires regardless of message content or pattern value.
		SoundTrigger t = trigger(TriggerType.CHAT_MESSAGE);
		t.setChatPatternMatchMode(MatchMode.ANY);
		t.setChatPattern(null);
		assertTrue(TriggerMatcher.matchesChat(t, "Anything at all."));

		t.setChatPattern("something");
		assertTrue(TriggerMatcher.matchesChat(t, "Anything at all."));
	}

	@Test
	public void chatBlankFilterWithContainsModeDoesNotFire()
	{
		// CONTAINS + blank is unconfigured, not "fire on any message" — use ANY for that.
		SoundTrigger t = trigger(TriggerType.CHAT_MESSAGE);
		t.setChatPatternMatchMode(MatchMode.CONTAINS);
		t.setChatPattern(null);
		assertFalse(TriggerMatcher.matchesChat(t, "Anything at all."));

		t.setChatPattern("");
		assertFalse(TriggerMatcher.matchesChat(t, "Anything at all."));
	}

	@Test
	public void chatFilterExactMatchMode()
	{
		SoundTrigger t = trigger(TriggerType.CHAT_MESSAGE);
		t.setChatPattern("You have been poisoned!");
		t.setChatPatternMatchMode(MatchMode.EXACT);
		assertTrue(TriggerMatcher.matchesChat(t, "You have been poisoned!"));
		assertTrue(TriggerMatcher.matchesChat(t, "YOU HAVE BEEN POISONED!"));
		assertFalse(TriggerMatcher.matchesChat(t, "You have been poisoned! Ouch."));
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
	public void anyModeFiresOnAnySpawn()
	{
		// ANY mode is the explicit "anyone in view" alert — blank CONTAINS is no longer enough.
		SoundTrigger t = trigger(TriggerType.NPC_SEEN);
		t.setNpcNameMatchMode(MatchMode.ANY);
		assertTrue(TriggerMatcher.matchesNpcSpawn(t, "Goblin"));
		assertTrue(TriggerMatcher.matchesNpcSpawn(t, "Cave goblin"));
	}

	@Test
	public void containsModeWithBlankFilterDoesNotFire()
	{
		// CONTAINS + blank is unconfigured, not "fire on any spawn" — use ANY for that.
		SoundTrigger t = trigger(TriggerType.NPC_SEEN);
		t.setNpcName(null);
		t.setNpcNameMatchMode(MatchMode.CONTAINS);
		assertFalse(TriggerMatcher.matchesNpcSpawn(t, "Goblin"));

		t.setNpcName("");
		assertFalse(TriggerMatcher.matchesNpcSpawn(t, "Goblin"));
	}

	@Test
	public void spawnFilterWithNullNameDoesNotMatch()
	{
		// A null actor name can't satisfy a non-null filter.
		SoundTrigger filtered = trigger(TriggerType.NPC_SEEN);
		filtered.setNpcName("goblin");
		assertFalse(TriggerMatcher.matchesNpcSpawn(filtered, null));

		// ANY mode fires even when the actor name is null.
		SoundTrigger anyNpc = trigger(TriggerType.NPC_SEEN);
		anyNpc.setNpcNameMatchMode(MatchMode.ANY);
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
