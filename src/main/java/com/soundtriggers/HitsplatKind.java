package com.soundtriggers;

import net.runelite.api.HitsplatID;

/**
 * A user-facing grouping of RuneLite's many raw {@link HitsplatID} type codes
 * into the handful of categories a player actually cares about.
 *
 * <p>The "self vs. others" distinction is handled separately by
 * {@link HitsplatTarget}, so the {@code _ME}/{@code _OTHER} variants of an id
 * collapse into a single kind here, as do the various damage colour variants.
 */
public enum HitsplatKind
{
	ANY("Any"),
	DAMAGE("Damage",
		HitsplatID.DAMAGE_ME, HitsplatID.DAMAGE_OTHER,
		HitsplatID.DAMAGE_ME_CYAN, HitsplatID.DAMAGE_OTHER_CYAN,
		HitsplatID.DAMAGE_ME_ORANGE, HitsplatID.DAMAGE_OTHER_ORANGE,
		HitsplatID.DAMAGE_ME_YELLOW, HitsplatID.DAMAGE_OTHER_YELLOW,
		HitsplatID.DAMAGE_ME_WHITE, HitsplatID.DAMAGE_OTHER_WHITE,
		HitsplatID.DAMAGE_ME_POISE, HitsplatID.DAMAGE_OTHER_POISE,
		HitsplatID.DAMAGE_MAX_ME, HitsplatID.DAMAGE_MAX_ME_CYAN,
		HitsplatID.DAMAGE_MAX_ME_ORANGE, HitsplatID.DAMAGE_MAX_ME_YELLOW,
		HitsplatID.DAMAGE_MAX_ME_WHITE, HitsplatID.DAMAGE_MAX_ME_POISE),
	BLOCK("Block", HitsplatID.BLOCK_ME, HitsplatID.BLOCK_OTHER),
	HEAL("Heal", HitsplatID.HEAL),
	POISON("Poison", HitsplatID.POISON),
	VENOM("Venom", HitsplatID.VENOM),
	DISEASE("Disease", HitsplatID.DISEASE, HitsplatID.DISEASE_BLOCKED),
	BURN("Burn", HitsplatID.BURN),
	BLEED("Bleed", HitsplatID.BLEED),
	PRAYER_DRAIN("Prayer drain", HitsplatID.PRAYER_DRAIN);

	private final String displayName;
	private final int[] ids;

	HitsplatKind(String displayName, int... ids)
	{
		this.displayName = displayName;
		this.ids = ids;
	}

	/** Whether a raw {@link net.runelite.api.Hitsplat#getHitsplatType()} code falls under this kind. */
	boolean matches(int hitsplatType)
	{
		if (this == ANY)
		{
			return true;
		}
		for (int id : ids)
		{
			if (id == hitsplatType)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
