package com.arber.pvmtools;

import java.awt.Color;
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
		description = "Configure tracker persistence, supplies, and data used by Automatic chat tabs.",
		position = 4
	)
	String trackerSection = "trackerSection";

	@ConfigSection(
		name = "Chat tabs",
		description = "Choose what each chat tab shows, or keep its normal RuneScape appearance.",
		position = 5
	)
	String chatTabSection = "chatTabSection";

	@ConfigSection(
		name = "Interface",
		description = "Configure small interface helpers.",
		position = 6
	)
	String interfaceSection = "interfaceSection";

	@ConfigSection(
		name = "Updates",
		description = "Configure PvM Toolkit update notes.",
		position = 7
	)
	String updateSection = "updateSection";

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
		description = "Simple hides the Supply Details and XP Details cards. Advanced shows them. Everything else is the same.",
		section = generalSection,
		position = 3
	)
	default ToolkitPanelMode panelMode()
	{
		return ToolkitPanelMode.ADVANCED;
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
		description = "Show the red game viewport overlay. Audio cues and the optional popup are configured separately.",
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
		name = "Warning popup",
		description = "Show a small popup with a short message, such as Re-dose Prayer, when a warning starts. Works even if Screen warning is off.",
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
		description = "Show a red screen warning when a potion timer is about to run out. Prayer timer warnings are always on.",
		section = warningSection,
		position = 5
	)
	default boolean flashPotionWarnings()
	{
		return true;
	}

	@Range(
		min = 0,
		max = 99
	)
	@ConfigItem(
		keyName = "hitpointsWarningThresholdPercent",
		name = "Hitpoints threshold (%)",
		description = "Warn when boosted Hitpoints reaches this percentage of your real maximum. Set 0 to disable.",
		section = warningSection,
		position = 6
	)
	default int hitpointsWarningThresholdPercent()
	{
		return 25;
	}

	@ConfigItem(
		keyName = "hitpointsWarningSound",
		name = "Hitpoints sound",
		description = "Sound played once when Hitpoints reach the threshold. Picking a sound plays it so you can hear it.",
		section = warningSection,
		position = 7
	)
	default ResourceWarningSound hitpointsWarningSound()
	{
		return ResourceWarningSound.BELL;
	}

	@Range(
		min = 0,
		max = 99
	)
	@ConfigItem(
		keyName = "prayerWarningThreshold",
		name = "Prayer threshold (points)",
		description = "Warn when prayer points reach this value. Set 0 to disable.",
		section = warningSection,
		position = 8
	)
	default int prayerWarningThreshold()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "prayerWarningSound",
		name = "Prayer sound",
		description = "Sound played once when prayer points reach the threshold. Picking a sound plays it so you can hear it.",
		section = warningSection,
		position = 9
	)
	default ResourceWarningSound prayerWarningSound()
	{
		return ResourceWarningSound.CHIME;
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
		position = 10
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
		position = 11
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
		position = 12
	)
	default boolean valuableDropSound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flashInventoryFull",
		name = "Inventory full warning",
		description = "Show a screen warning once when inventory becomes full. Needs Inventory spaces to be on.",
		section = warningSection,
		position = 13
	)
	default boolean flashInventoryFull()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundPotionExpired",
		name = "Timer empty sound",
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
		keyName = "showPvmFreezeTimer",
		name = "PvM freeze timer",
		description = "Show the remaining freeze time when your ice or bind spell hits the NPC you are fighting. PvP targets are ignored.",
		section = timerSection,
		position = 2
	)
	default boolean showPvmFreezeTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "soundCannonEmpty",
		name = "Cannon empty sound",
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
		name = "Automatic Trade clock",
		description = "Show the local 24-hour clock when the Trade chat tab is set to Automatic. Explicit tab choices take priority.",
		section = chatTabSection,
		position = 7
	)
	default boolean tradeButtonClock()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chatTabOverridesEnabled",
		name = "Enable chat tab displays",
		description = "Show custom data in chat tabs. Turn off to restore every chat tab, including Trade, to its normal appearance without changing your choices or recorded statistics.",
		section = chatTabSection,
		position = 0
	)
	default boolean chatTabOverridesEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clanChatTabDisplay",
		name = "Clan tab",
		description = "Choose data for the Clan tab. Vanilla keeps the normal tab. Automatic gives this tab the next active tracker.",
		section = chatTabSection,
		position = 1
	)
	default ChatTabDisplay clanChatTabDisplay()
	{
		return ChatTabDisplay.AUTOMATIC;
	}

	@ConfigItem(
		keyName = "channelChatTabDisplay",
		name = "Channel tab",
		description = "Choose data for the Channel tab. Vanilla keeps the normal tab. Automatic gives this tab the next active tracker.",
		section = chatTabSection,
		position = 2
	)
	default ChatTabDisplay channelChatTabDisplay()
	{
		return ChatTabDisplay.AUTOMATIC;
	}

	@ConfigItem(
		keyName = "privateChatTabDisplay",
		name = "Private tab",
		description = "Choose data for the Private tab. Vanilla keeps the normal tab. Automatic gives this tab the next active tracker.",
		section = chatTabSection,
		position = 3
	)
	default ChatTabDisplay privateChatTabDisplay()
	{
		return ChatTabDisplay.AUTOMATIC;
	}

	@ConfigItem(
		keyName = "publicChatTabDisplay",
		name = "Public tab",
		description = "Choose data for the Public tab. Vanilla keeps the normal tab. Automatic gives this tab the next active tracker.",
		section = chatTabSection,
		position = 4
	)
	default ChatTabDisplay publicChatTabDisplay()
	{
		return ChatTabDisplay.AUTOMATIC;
	}

	@ConfigItem(
		keyName = "gameChatTabDisplay",
		name = "Game tab",
		description = "Choose data for the Game tab. Vanilla keeps the normal tab. Automatic gives this tab the next active tracker.",
		section = chatTabSection,
		position = 5
	)
	default ChatTabDisplay gameChatTabDisplay()
	{
		return ChatTabDisplay.AUTOMATIC;
	}

	@ConfigItem(
		keyName = "tradeChatTabDisplay",
		name = "Trade tab",
		description = "Choose data for the Trade tab. Vanilla keeps its normal name and status. Automatic uses the Automatic Trade clock setting.",
		section = chatTabSection,
		position = 6
	)
	default ChatTabDisplay tradeChatTabDisplay()
	{
		return ChatTabDisplay.AUTOMATIC;
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
		name = "Loot value source",
		description = "Choose how new loot, inventory, drop alerts, and profit are valued. Saved history keeps its recorded value.",
		section = interfaceSection,
		position = 2
	)
	default ToolkitPriceSource priceSource()
	{
		return ToolkitPriceSource.GE_GUIDE;
	}

	@ConfigItem(
		keyName = "supplyPriceSource",
		name = "Supply cost source",
		description = "Choose the market prices used for new potion, food, cannon, rune, ammunition, and charge costs. High Alch is never used for costs.",
		section = interfaceSection,
		position = 3
	)
	default ToolkitMarketPriceSource supplyPriceSource()
	{
		return ToolkitMarketPriceSource.GE_GUIDE;
	}

	@ConfigItem(
		keyName = "highlightGroundItems",
		name = "Loot glow",
		description = "Always outline ground item drops that are visible through RuneLite's Ground Items settings.",
		section = interfaceSection,
		position = 4
	)
	default boolean highlightGroundItems()
	{
		return true;
	}

	@Range(
		min = 0,
		max = Integer.MAX_VALUE
	)
	@ConfigItem(
		keyName = "groundItemHighlightMinimum",
		name = "Loot glow minimum",
		description = "Only outline drops worth at least this much GP. The whole stack is counted. Set 0 to outline every visible drop.",
		section = interfaceSection,
		position = 5
	)
	default int groundItemHighlightMinimum()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "groundItemHighlightColor",
		name = "Loot glow color",
		description = "Choose a non-blue color so RuneLite's blue hover highlight remains visible.",
		section = interfaceSection,
		position = 6
	)
	default Color groundItemHighlightColor()
	{
		return new Color(255, 170, 0);
	}

	@Range(
		min = 1,
		max = 6
	)
	@ConfigItem(
		keyName = "groundItemHighlightWidth",
		name = "Loot glow width",
		description = "Set the thickness of the always-on ground item outline.",
		section = interfaceSection,
		position = 7
	)
	default int groundItemHighlightWidth()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "groundItemLifetimeText",
		name = "Drop lifetime text",
		description = "Turn visible ground item text into a green-to-red bar that shrinks toward despawn.",
		section = interfaceSection,
		position = 8
	)
	default boolean groundItemLifetimeText()
	{
		return true;
	}

	@ConfigItem(
		keyName = "groundItemLifetimeMode",
		name = "Drop timer mode",
		description = "Show the shrinking drop timer on every visible drop or only drops above a value threshold.",
		section = interfaceSection,
		position = 9
	)
	default GroundItemLifetimeMode groundItemLifetimeMode()
	{
		return GroundItemLifetimeMode.ALL_VISIBLE;
	}

	@Range(
		min = 0,
		max = Integer.MAX_VALUE
	)
	@ConfigItem(
		keyName = "groundItemLifetimeThreshold",
		name = "Drop timer minimum",
		description = "Minimum total stack value for the drop timer when Value threshold mode is selected.",
		section = interfaceSection,
		position = 10
	)
	default int groundItemLifetimeThreshold()
	{
		return 10_000;
	}

	@Range(
		min = 0,
		max = 100
	)
	@ConfigItem(
		keyName = "groundItemLifetimeBackground",
		name = "Drop timer faded text",
		description = "How dark the expired part of drop lifetime text becomes. Set 0 to keep it bright.",
		section = interfaceSection,
		position = 11
	)
	default int groundItemLifetimeBackground()
	{
		return 35;
	}

	@ConfigItem(
		keyName = "lootClickThrough",
		name = "Loot click-through",
		description = "Prioritize Take over NPC actions when visible ground item loot is under an NPC.",
		section = interfaceSection,
		position = 12
	)
	default boolean lootClickThrough()
	{
		return true;
	}

	@ConfigItem(
		keyName = "wildernessSafety",
		name = "Wilderness safety",
		description = "Disable menu-changing helpers such as loot click-through in Wilderness and PvP worlds.",
		section = interfaceSection,
		position = 13
	)
	default boolean wildernessSafety()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dontShowUpdateScroll",
		name = "Don't show updates",
		description = "Do not show automatic PvM Toolkit update notes for future versions.",
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
		description = "How long chat tab totals are kept. Session starts from zero when RuneLite starts. Forever keeps them until you reset. Side panel periods are not affected.",
		section = trackerSection,
		position = 0
	)
	default TrackerPersistenceMode trackerMode()
	{
		return TrackerPersistenceMode.FOREVER;
	}

	@ConfigItem(
		keyName = "clanLootTracker",
		name = "Automatic loot tracker",
		description = "Include picked-up NPC loot in Automatic chat tabs. Explicit tab choices and recorded loot statistics are unaffected.",
		section = trackerSection,
		position = 1
	)
	default boolean clanLootTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "publicSupplyCostTracker",
		name = "Automatic supply cost",
		description = "Include consumed supply costs in Automatic chat tabs. Explicit tab choices and recorded supply statistics are unaffected.",
		section = trackerSection,
		position = 2
	)
	default boolean publicSupplyCostTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ignoredSupplyItems",
		name = "Ignored supplies",
		description = "Items that should count as used but cost 0 gp. Separate exact item names with commas. Potion dose suffixes are optional. Changes apply to future usage only.",
		section = trackerSection,
		position = 3
	)
	default String ignoredSupplyItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "channelCombatXpTracker",
		name = "Automatic combat XP",
		description = "Include combat XP in Automatic chat tabs. Explicit tab choices and recorded XP statistics are unaffected.",
		section = trackerSection,
		position = 4
	)
	default boolean channelCombatXpTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "privateSlayerXpTracker",
		name = "Automatic Slayer XP",
		description = "Include Slayer XP in Automatic chat tabs. Explicit tab choices and recorded XP statistics are unaffected.",
		section = trackerSection,
		position = 5
	)
	default boolean privateSlayerXpTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "topXpSkillTracker",
		name = "Automatic top skill XP",
		description = "Include the skill with the most tracked PvM XP in Automatic chat tabs. Explicit tab choices and recorded XP statistics are unaffected.",
		section = trackerSection,
		position = 6
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

	@ConfigItem(
		keyName = "combatLootExclusionsV1",
		name = "",
		description = "",
		hidden = true
	)
	default String combatLootExclusions()
	{
		return "";
	}

}
