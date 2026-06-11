package com.soundtriggers;

// Regions where sound triggers are disabled to prevent audio-based advantages.
// Mirrors the list maintained by RuneLite's Watchdog plugin; update this list
// when the development team announces changes in #development on their Discord.
enum RestrictedRegion
{
	ALCHEMICAL_HYDRA(5536),
	VARDORVIS(4405),
	LEVIATHAN(8291),
	WHISPERER(10595),
	SUCELLUS(12132),
	VORKATH(9023),
	INFERNO(9043),
	FIGHT_CAVE(9551),
	COLOSSEUM(7216),
	KALPHITE_QUEEN(13972),
	COX(13136, 13137, 13393, 13138, 13394, 13139, 13395, 13140, 13396, 13141, 13397, 13145, 13401, 12889),
	TOB(12613, 13125, 13122, 13123, 13379, 12612, 12611),
	TOA(14160, 15698, 15700, 14162, 14164, 15186, 15188, 14674, 14676, 15184, 15696),
	YAMA(6045),
	DOOM_OF_MOKHAIOTL(5269, 13668, 14180),
	NIGHTMARE(15515);

	private final int[] regionIds;

	RestrictedRegion(int... regionIds)
	{
		this.regionIds = regionIds;
	}

	static boolean isRestricted(int regionId)
	{
		for (RestrictedRegion r : values())
		{
			for (int id : r.regionIds)
			{
				if (id == regionId)
				{
					return true;
				}
			}
		}
		return false;
	}
}
