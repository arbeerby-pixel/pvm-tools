package com.arber.pvmtools;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.VarbitID;

enum PrayerDrainEntry
{
	THICK_SKIN(Prayer.THICK_SKIN, 1),
	BURST_OF_STRENGTH(Prayer.BURST_OF_STRENGTH, 1),
	CLARITY_OF_THOUGHT(Prayer.CLARITY_OF_THOUGHT, 1),
	SHARP_EYE(Prayer.SHARP_EYE, 1),
	MYSTIC_WILL(Prayer.MYSTIC_WILL, 1),
	ROCK_SKIN(Prayer.ROCK_SKIN, 6),
	SUPERHUMAN_STRENGTH(Prayer.SUPERHUMAN_STRENGTH, 6),
	IMPROVED_REFLEXES(Prayer.IMPROVED_REFLEXES, 6),
	RAPID_RESTORE(Prayer.RAPID_RESTORE, 1),
	RAPID_HEAL(Prayer.RAPID_HEAL, 2),
	PROTECT_ITEM(Prayer.PROTECT_ITEM, 2),
	HAWK_EYE(Prayer.HAWK_EYE, 6),
	MYSTIC_LORE(Prayer.MYSTIC_LORE, 6),
	STEEL_SKIN(Prayer.STEEL_SKIN, 12),
	ULTIMATE_STRENGTH(Prayer.ULTIMATE_STRENGTH, 12),
	INCREDIBLE_REFLEXES(Prayer.INCREDIBLE_REFLEXES, 12),
	PROTECT_FROM_MAGIC(Prayer.PROTECT_FROM_MAGIC, 12),
	PROTECT_FROM_MISSILES(Prayer.PROTECT_FROM_MISSILES, 12),
	PROTECT_FROM_MELEE(Prayer.PROTECT_FROM_MELEE, 12),
	EAGLE_EYE(Prayer.EAGLE_EYE, 12)
	{
		@Override
		boolean isEnabled(Client client)
		{
			return !DEADEYE.isEnabled(client);
		}
	},
	MYSTIC_MIGHT(Prayer.MYSTIC_MIGHT, 12)
	{
		@Override
		boolean isEnabled(Client client)
		{
			return !MYSTIC_VIGOUR.isEnabled(client);
		}
	},
	RETRIBUTION(Prayer.RETRIBUTION, 3),
	REDEMPTION(Prayer.REDEMPTION, 6),
	SMITE(Prayer.SMITE, 18),
	PRESERVE(Prayer.PRESERVE, 2),
	CHIVALRY(Prayer.CHIVALRY, 24),
	DEADEYE(Prayer.DEADEYE, 12)
	{
		@Override
		boolean isEnabled(Client client)
		{
			boolean inLms = client.getVarbitValue(VarbitID.BR_INGAME) != 0;
			boolean unlocked = client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) != 0;
			return unlocked && !inLms;
		}
	},
	MYSTIC_VIGOUR(Prayer.MYSTIC_VIGOUR, 12)
	{
		@Override
		boolean isEnabled(Client client)
		{
			boolean inLms = client.getVarbitValue(VarbitID.BR_INGAME) != 0;
			boolean unlocked = client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) != 0;
			return unlocked && !inLms;
		}
	},
	PIETY(Prayer.PIETY, 24),
	RIGOUR(Prayer.RIGOUR, 24),
	AUGURY(Prayer.AUGURY, 24),
	RP_REJUVENATION(Prayer.RP_REJUVENATION, 4),
	RP_ANCIENT_STRENGTH(Prayer.RP_ANCIENT_STRENGTH, 18),
	RP_ANCIENT_SIGHT(Prayer.RP_ANCIENT_SIGHT, 18),
	RP_ANCIENT_WILL(Prayer.RP_ANCIENT_WILL, 18),
	RP_PROTECT_ITEM(Prayer.RP_PROTECT_ITEM, 18),
	RP_RUINOUS_GRACE(Prayer.RP_RUINOUS_GRACE, 1),
	RP_DAMPEN_MAGIC(Prayer.RP_DAMPEN_MAGIC, 14),
	RP_DAMPEN_RANGED(Prayer.RP_DAMPEN_RANGED, 14),
	RP_DAMPEN_MELEE(Prayer.RP_DAMPEN_MELEE, 14),
	RP_TRINITAS(Prayer.RP_TRINITAS, 22),
	RP_BERSERKER(Prayer.RP_BERSERKER, 2),
	RP_PURGE(Prayer.RP_PURGE, 18),
	RP_METABOLISE(Prayer.RP_METABOLISE, 12),
	RP_REBUKE(Prayer.RP_REBUKE, 12),
	RP_VINDICATION(Prayer.RP_VINDICATION, 9),
	RP_DECIMATE(Prayer.RP_DECIMATE, 28),
	RP_ANNIHILATE(Prayer.RP_ANNIHILATE, 28),
	RP_VAPORISE(Prayer.RP_VAPORISE, 28),
	RP_FUMUS_VOW(Prayer.RP_FUMUS_VOW, 14),
	RP_UMBRA_VOW(Prayer.RP_UMBRA_VOW, 14),
	RP_CRUORS_VOW(Prayer.RP_CRUORS_VOW, 14),
	RP_GLACIES_VOW(Prayer.RP_GLACIES_VOW, 14),
	RP_WRATH(Prayer.RP_WRATH, 3),
	RP_INTENSIFY(Prayer.RP_INTENSIFY, 28),
	;

	private final Prayer prayer;
	private final int drainEffect;

	PrayerDrainEntry(Prayer prayer, int drainEffect)
	{
		this.prayer = prayer;
		this.drainEffect = drainEffect;
	}

	boolean isEnabled(Client client)
	{
		return true;
	}

	boolean isActive(Client client)
	{
		return client.isPrayerActive(prayer) && isEnabled(client);
	}

	int getDrainEffect()
	{
		return drainEffect;
	}
}
