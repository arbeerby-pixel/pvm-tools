package com.arber.pvmtools;

import net.runelite.api.gameval.ItemID;

enum CombatPotionTimerType
{
	PRAYER("Prayer", net.runelite.api.ItemID.PRAYER_POTION4),
	OVERLOAD("Overload", ItemID.NZONE4DOSEOVERLOADPOTION),
	RAID_OVERLOAD("Overload", ItemID.RAIDS_VIAL_OVERLOAD_STRONG_4),
	BLIGHTED_OVERLOAD("Blighted overload", ItemID.DEADMAN4DOSEOVERLOAD),
	ANTIFIRE("Antifire", ItemID._4DOSE1ANTIDRAGON),
	EXTENDED_ANTIFIRE("Extended antifire", ItemID._4DOSE2ANTIDRAGON),
	SUPER_ANTIFIRE("Super antifire", ItemID._4DOSE3ANTIDRAGON),
	EXTENDED_SUPER_ANTIFIRE("Extended super antifire", ItemID._4DOSE4ANTIDRAGON),
	ANTI_VENOM_PLUS("Anti-venom+", ItemID.ANTIVENOM_4),
	DIVINE_SUPER_ATTACK("Divine super attack", ItemID._4DOSEDIVINEATTACK),
	DIVINE_SUPER_STRENGTH("Divine super strength", ItemID._4DOSEDIVINESTRENGTH),
	DIVINE_SUPER_DEFENCE("Divine super defence", ItemID._4DOSEDIVINEDEFENCE),
	DIVINE_SUPER_COMBAT("Divine super combat", ItemID._4DOSEDIVINECOMBAT),
	DIVINE_RANGING("Divine ranging", ItemID._4DOSEDIVINERANGE),
	DIVINE_MAGIC("Divine magic", ItemID._4DOSEDIVINEMAGIC),
	DIVINE_BASTION("Divine bastion", ItemID._4DOSEDIVINEBASTION),
	DIVINE_BATTLEMAGE("Divine battlemage", ItemID._4DOSEDIVINEBATTLEMAGE),
	;

	private final String displayName;
	private final int imageItemId;

	CombatPotionTimerType(String displayName, int imageItemId)
	{
		this.displayName = displayName;
		this.imageItemId = imageItemId;
	}

	String getDisplayName()
	{
		return displayName;
	}

	int getImageItemId()
	{
		return imageItemId;
	}
}
