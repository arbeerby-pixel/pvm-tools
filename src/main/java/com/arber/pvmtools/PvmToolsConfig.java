package com.arber.pvmtools;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(PvmToolsConfig.GROUP)
public interface PvmToolsConfig extends Config
{
	String GROUP = "pvmTools";

	@ConfigSection(
		name = "General",
		description = "Configure the main PvM helpers.",
		position = 0
	)
	String generalSection = "generalSection";

	@ConfigSection(
		name = "Timers",
		description = "Configure prayer and potion timers.",
		position = 1
	)
	String timerSection = "timerSection";

	@ConfigSection(
		name = "Warnings",
		description = "Configure screen warning behavior.",
		position = 2
	)
	String warningSection = "warningSection";

	@ConfigSection(
		name = "Cannon",
		description = "Configure cannon combat warnings.",
		position = 3
	)
	String cannonSection = "cannonSection";

	@ConfigSection(
		name = "Trackers",
		description = "Configure chat tab trackers.",
		position = 4
	)
	String trackerSection = "trackerSection";

	@ConfigSection(
		name = "Interface",
		description = "Configure small interface helpers.",
		position = 5
	)
	String interfaceSection = "interfaceSection";

	@ConfigSection(
		name = "Updates",
		description = "Configure PvM Tools update notes.",
		position = 6
	)
	String updateSection = "updateSection";

	@ConfigItem(
		keyName = "showPrayerTimer",
		name = "Prayer timer",
		description = "Show a prayer time remaining infobox while prayers are active.",
		section = timerSection,
		position = 100,
		hidden = true
	)
	default boolean showPrayerTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPotionTimers",
		name = "Potion timers",
		description = "Show prayer and all supported potion timer infoboxes.",
		section = timerSection,
		position = 0
	)
	default boolean showPotionTimers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideDeathSpawns",
		name = "Hide Death Spawns",
		description = "Hide Death Spawn and Chaotic Death Spawn NPCs from Nechryaels.",
		section = generalSection,
		position = 0
	)
	default boolean hideDeathSpawns()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flashSuperiorSpawns",
		name = "Flash superior spawn",
		description = "Flash the screen when a superior Slayer monster spawns.",
		section = generalSection,
		position = 1
	)
	default boolean flashSuperiorSpawns()
	{
		return true;
	}

	@ConfigItem(
		keyName = "superiorExamineHints",
		name = "Superior examine hints",
		description = "Add a short prayer and kill tip when examining superior Slayer monsters.",
		section = generalSection,
		position = 2
	)
	default boolean superiorExamineHints()
	{
		return true;
	}

	@ConfigItem(
		keyName = "panelMode",
		name = "Panel mode",
		description = "Simple keeps the side panel compact. Advanced shows task history and detailed supply and XP stats.",
		section = generalSection,
		position = 3
	)
	default ToolkitPanelMode panelMode()
	{
		return ToolkitPanelMode.ADVANCED;
	}

	@ConfigItem(
		keyName = "showOverloads",
		name = "Overloads",
		description = "Show timers for overload, raid overload, and blighted overload.",
		section = timerSection,
		position = 100,
		hidden = true
	)
	default boolean showOverloads()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showAntifire",
		name = "Antifire",
		description = "Show a timer for standard antifire potions.",
		section = timerSection,
		position = 101,
		hidden = true
	)
	default boolean showAntifire()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showExtendedAntifire",
		name = "Extended antifire",
		description = "Show a timer for extended antifire potions.",
		section = timerSection,
		position = 102,
		hidden = true
	)
	default boolean showExtendedAntifire()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSuperAntifire",
		name = "Super antifire",
		description = "Show a timer for super antifire potions.",
		section = timerSection,
		position = 103,
		hidden = true
	)
	default boolean showSuperAntifire()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showExtendedSuperAntifire",
		name = "Extended super antifire",
		description = "Show a timer for extended super antifire potions.",
		section = timerSection,
		position = 104,
		hidden = true
	)
	default boolean showExtendedSuperAntifire()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showAntiVenomPlus",
		name = "Anti-venom+",
		description = "Show a timer for Anti-venom+ while venom immunity is active.",
		section = timerSection,
		position = 105,
		hidden = true
	)
	default boolean showAntiVenomPlus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDivineSuperCombat",
		name = "Divine super combat",
		description = "Show a timer for divine super combat potion.",
		section = timerSection,
		position = 106,
		hidden = true
	)
	default boolean showDivineSuperCombat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDivineBastion",
		name = "Divine bastion",
		description = "Show a timer for divine bastion potion.",
		section = timerSection,
		position = 107,
		hidden = true
	)
	default boolean showDivineBastion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDivineBattlemage",
		name = "Divine battlemage",
		description = "Show a timer for divine battlemage potion.",
		section = timerSection,
		position = 108,
		hidden = true
	)
	default boolean showDivineBattlemage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDivineRanging",
		name = "Divine ranging",
		description = "Show a timer for divine ranging potion.",
		section = timerSection,
		position = 109,
		hidden = true
	)
	default boolean showDivineRanging()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDivineMagic",
		name = "Divine magic",
		description = "Show a timer for divine magic potion.",
		section = timerSection,
		position = 110,
		hidden = true
	)
	default boolean showDivineMagic()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDivineSuperAttack",
		name = "Divine super attack",
		description = "Show a timer for divine super attack potion.",
		section = timerSection,
		position = 111,
		hidden = true
	)
	default boolean showDivineSuperAttack()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDivineSuperStrength",
		name = "Divine super strength",
		description = "Show a timer for divine super strength potion.",
		section = timerSection,
		position = 112,
		hidden = true
	)
	default boolean showDivineSuperStrength()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDivineSuperDefence",
		name = "Divine super defence",
		description = "Show a timer for divine super defence potion.",
		section = timerSection,
		position = 113,
		hidden = true
	)
	default boolean showDivineSuperDefence()
	{
		return true;
	}

	@ConfigItem(
		keyName = "warnCannonEmpty",
		name = "Cannon empty",
		description = "Show a screen warning when your cannon reaches 0 cannonballs.",
		section = cannonSection,
		position = 0
	)
	default boolean warnCannonEmpty()
	{
		return true;
	}

	@ConfigItem(
		keyName = "warnCannonRepair",
		name = "Cannon repair",
		description = "Show a screen warning when your cannon enters repair mode.",
		section = cannonSection,
		position = 1
	)
	default boolean warnCannonRepair()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyCannonWarnings",
		name = "Cannon notifications",
		description = "Send a RuneLite notification when your cannon is empty or needs repair.",
		section = cannonSection,
		position = 2
	)
	default boolean notifyCannonWarnings()
	{
		return false;
	}

	@ConfigItem(
		keyName = "flashScreenWarning",
		name = "Screen warning",
		description = "Show the red game viewport warning for enabled warning types.",
		section = warningSection,
		position = 0
	)
	default boolean flashScreenWarning()
	{
		return true;
	}

	@Range(
		min = 0,
		max = 100
	)
	@ConfigItem(
		keyName = "warningIntensity",
		name = "Warning redness",
		description = "Red overlay strength. 0 keeps warnings enabled but makes the screen overlay invisible.",
		section = warningSection,
		position = 1
	)
	default int warningIntensity()
	{
		return 55;
	}

	@Range(
		min = 1,
		max = 5
	)
	@ConfigItem(
		keyName = "warningFlashSeconds",
		name = "Blink seconds",
		description = "How many seconds each red flash stays visible before the next blink.",
		section = warningSection,
		position = 2
	)
	default int warningFlashSeconds()
	{
		return 3;
	}

	@Range(
		min = 3,
		max = 60
	)
	@ConfigItem(
		keyName = "warningSeconds",
		name = "Warning seconds",
		description = "Start red timer text and flash warnings when this many seconds remain.",
		section = warningSection,
		position = 3
	)
	default int warningSeconds()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "warningPopup",
		name = "Collection log popup",
		description = "Show a collection-log popup with a short action message when the red screen warning flashes.",
		section = warningSection,
		position = 4
	)
	default boolean warningPopup()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flashPotionWarnings",
		name = "Potion warnings",
		description = "Show a screen warning when visible prayer or potion timers enter the warning window.",
		section = warningSection,
		position = 5
	)
	default boolean flashPotionWarnings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flashPrayerWarnings",
		name = "Prayer warnings",
		description = "Show a screen warning when the prayer timer enters the warning window.",
		section = warningSection,
		position = 100,
		hidden = true
	)
	default boolean flashPrayerWarnings()
	{
		return true;
	}

	@Range(
		min = 0,
		max = Integer.MAX_VALUE
	)
	@ConfigItem(
		keyName = "valuableDropThreshold",
		name = "Drop alert threshold",
		description = "Alert when an NPC drop you pick up is worth at least this much GP. Set 0 to alert on every NPC drop.",
		section = warningSection,
		position = 8
	)
	default int valuableDropThreshold()
	{
		return 1_000_000;
	}

	@ConfigItem(
		keyName = "valuableDropFlash",
		name = "Drop alert flash",
		description = "Flash the screen green when you pick up an NPC drop above the threshold.",
		section = warningSection,
		position = 9
	)
	default boolean valuableDropFlash()
	{
		return true;
	}

	@ConfigItem(
		keyName = "valuableDropSound",
		name = "Drop alert sound",
		description = "Play a different ping when you pick up an NPC drop above the threshold.",
		section = warningSection,
		position = 10
	)
	default boolean valuableDropSound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flashInventoryFull",
		name = "Inventory full warning",
		description = "Show a screen warning once when inventory becomes full.",
		section = warningSection,
		position = 7
	)
	default boolean flashInventoryFull()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundPotionExpired",
		name = "Timer empty ping",
		description = "Play a short ping when prayer reaches 0 or a visible potion timer reaches 0.",
		section = timerSection,
		position = 1
	)
	default boolean soundPotionExpired()
	{
		return true;
	}

	@ConfigItem(
		keyName = "soundPrayerExpired",
		name = "Prayer empty ping",
		description = "Play a short ping when your prayer points reach 0.",
		section = timerSection,
		position = 101,
		hidden = true
	)
	default boolean soundPrayerExpired()
	{
		return true;
	}

	@ConfigItem(
		keyName = "soundCannonEmpty",
		name = "Cannon empty ping",
		description = "Play a short ping when your cannon reaches 0 cannonballs.",
		section = cannonSection,
		position = 3
	)
	default boolean soundCannonEmpty()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tradeButtonClock",
		name = "Trade button clock",
		description = "Replace the Trade chat button text with the local time in 24-hour format.",
		section = interfaceSection,
		position = 0
	)
	default boolean tradeButtonClock()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showInventorySpaces",
		name = "Inventory spaces",
		description = "Show an infobox with the number of free inventory spaces. Hover it to see inventory value.",
		section = interfaceSection,
		position = 1
	)
	default boolean showInventorySpaces()
	{
		return true;
	}

	@ConfigItem(
		keyName = "priceSource",
		name = "Value price source",
		description = "Choose the prices used for inventory value and value trackers.",
		section = interfaceSection,
		position = 2
	)
	default ToolkitPriceSource priceSource()
	{
		return ToolkitPriceSource.GE_GUIDE;
	}

	@ConfigItem(
		keyName = "dontShowUpdateScroll",
		name = "Don't show updates",
		description = "Do not show automatic PvM Tools update notes for future versions.",
		section = updateSection,
		position = 0
	)
	default boolean dontShowUpdateScroll()
	{
		return false;
	}

	@ConfigItem(
		keyName = "previewUpdateScroll",
		name = "Preview update scroll",
		description = "Show the update scroll again for testing.",
		section = updateSection,
		position = 1
	)
	default boolean previewUpdateScroll()
	{
		return false;
	}

	@ConfigItem(
		keyName = "trackerMode",
		name = "Tracker mode",
		description = "Session resets all trackers when RuneLite starts. Forever continues all trackers from the saved values.",
		section = trackerSection,
		position = 0
	)
	default TrackerPersistenceMode trackerMode()
	{
		return TrackerPersistenceMode.FOREVER;
	}

	@ConfigItem(
		keyName = "clanLootTracker",
		name = "Loot tracker",
		description = "Show the total value of NPC drops you pick up in the dynamic chat tab trackers.",
		section = trackerSection,
		position = 1
	)
	default boolean clanLootTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "publicSupplyCostTracker",
		name = "Supply cost tracker",
		description = "Show the value of consumed potions, food, and cannonballs in the dynamic chat tab trackers.",
		section = trackerSection,
		position = 2
	)
	default boolean publicSupplyCostTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "channelCombatXpTracker",
		name = "Combat XP tracker",
		description = "Show combat XP gained in the dynamic chat tab trackers.",
		section = trackerSection,
		position = 3
	)
	default boolean channelCombatXpTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "privateSlayerXpTracker",
		name = "Slayer XP tracker",
		description = "Show Slayer XP gained in the dynamic chat tab trackers.",
		section = trackerSection,
		position = 4
	)
	default boolean privateSlayerXpTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "topXpSkillTracker",
		name = "Top XP skill tracker",
		description = "Show the skill with the most tracked PvM XP in the dynamic chat tab trackers.",
		section = trackerSection,
		position = 5
	)
	default boolean topXpSkillTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackerDefaultsInitializedV1",
		name = "",
		description = "",
		hidden = true
	)
	default boolean trackerDefaultsInitialized()
	{
		return false;
	}

	@ConfigItem(
		keyName = "resetTrackerTarget",
		name = "Reset tracker",
		description = "Choose which tracker should be reset.",
		section = trackerSection,
		position = 6,
		hidden = true
	)
	default TrackerResetTarget resetTrackerTarget()
	{
		return TrackerResetTarget.LOOT;
	}

	@ConfigItem(
		keyName = "resetSelectedTracker",
		name = "Reset selected tracker",
		description = "Reset the tracker selected above.",
		section = trackerSection,
		position = 7,
		hidden = true
	)
	default boolean resetSelectedTracker()
	{
		return false;
	}

	@ConfigItem(
		keyName = "savedLootTrackerValue",
		name = "",
		description = "",
		hidden = true
	)
	default String savedLootTrackerValue()
	{
		return "0";
	}

	@ConfigItem(
		keyName = "savedSupplyCostTrackerValue",
		name = "",
		description = "",
		hidden = true
	)
	default String savedSupplyCostTrackerValue()
	{
		return "0";
	}

	@ConfigItem(
		keyName = "savedSupplyCostTrackerBreakdown",
		name = "",
		description = "",
		hidden = true
	)
	default String savedSupplyCostTrackerBreakdown()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedCombatXpTrackerValuesV2",
		name = "",
		description = "",
		hidden = true
	)
	default String savedCombatXpTrackerValues()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedSlayerXpTrackerValueV2",
		name = "",
		description = "",
		hidden = true
	)
	default String savedSlayerXpTrackerValue()
	{
		return "0";
	}

	@ConfigItem(
		keyName = "savedStatsDayV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedStatsDay()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedStatsWeekV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedStatsWeek()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedStatsMonthV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedStatsMonth()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedStatsYearV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedStatsYear()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedStatsAllTimeV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedStatsAllTime()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedCurrentSlayerTaskV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedCurrentSlayerTask()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedSlayerTaskHistoryV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedSlayerTaskHistory()
	{
		return "";
	}

}
