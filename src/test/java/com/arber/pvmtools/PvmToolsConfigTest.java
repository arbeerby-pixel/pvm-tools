package com.arber.pvmtools;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmToolsConfigTest
{
	private final PvmToolsConfig config = new PvmToolsConfig()
	{
	};

	@Test
	public void allChatTabsAreEnabledByDefault()
	{
		assertEquals(TrackerPersistenceMode.FOREVER, config.trackerMode());
		assertTrue(config.chatTabOverridesEnabled());
		assertEquals("", config.ignoredSupplyItems());
	}

	@Test
	public void automaticTabDefaultsPreserveExistingLayoutsAndClockPreference()
	{
		assertEquals(ChatTabDisplay.AUTOMATIC, config.clanChatTabDisplay());
		assertEquals(ChatTabDisplay.AUTOMATIC, config.channelChatTabDisplay());
		assertEquals(ChatTabDisplay.AUTOMATIC, config.privateChatTabDisplay());
		assertEquals(ChatTabDisplay.AUTOMATIC, config.publicChatTabDisplay());
		assertEquals(ChatTabDisplay.AUTOMATIC, config.gameChatTabDisplay());
		assertEquals(ChatTabDisplay.AUTOMATIC, config.tradeChatTabDisplay());
	}

	@Test
	public void eachTabAssignmentHasItsOwnSavedKeyAndConfigRow() throws Exception
	{
		String[] tabKeys = {
			"clanChatTabDisplay", "channelChatTabDisplay", "privateChatTabDisplay",
			"publicChatTabDisplay", "gameChatTabDisplay", "tradeChatTabDisplay"
		};
		Set<Integer> positions = new HashSet<>();
		for (String key : tabKeys)
		{
			Method method = PvmToolsConfig.class.getMethod(key);
			ConfigItem item = method.getAnnotation(ConfigItem.class);
			assertEquals(ChatTabDisplay.class, method.getReturnType());
			assertEquals(key, item.keyName());
			assertEquals(PvmToolsConfig.chatTabSection, item.section());
			assertTrue(positions.add(item.position()));
			assertTrue(item.position() > 0);
		}
		ConfigItem master = PvmToolsConfig.class.getMethod("chatTabOverridesEnabled")
			.getAnnotation(ConfigItem.class);
		assertEquals(PvmToolsConfig.chatTabSection, master.section());
		assertEquals(0, master.position());
		assertEquals("chatTabOverridesEnabled", master.keyName());
	}

	@Test
	public void removedSettingsNoLongerHaveAConfigRow()
	{
		Set<String> keys = new HashSet<>();
		for (Method method : PvmToolsConfig.class.getMethods())
		{
			ConfigItem item = method.getAnnotation(ConfigItem.class);
			if (item != null)
			{
				keys.add(item.keyName());
			}
		}
		for (String removed : new String[]{"tradeButtonClock", "clanLootTracker", "publicSupplyCostTracker",
			"channelCombatXpTracker", "privateSlayerXpTracker", "topXpSkillTracker", "warningFlashSeconds",
			"flashPrayerWarnings", "soundPrayerExpired", "groundItemHighlightWidth", "groundItemLifetimeMode",
			"groundItemLifetimeBackground", "wildernessSafety", "previewUpdateScroll"})
		{
			assertFalse(removed, keys.contains(removed));
		}
	}

	@Test
	public void tabDisplayOptionsHaveClearNamesForNormalTabsAndEachCombatSkill()
	{
		assertEquals("Vanilla (normal tab)", ChatTabDisplay.VANILLA.toString());
		assertEquals("Net profit", ChatTabDisplay.NET_PROFIT.toString());
		assertEquals("Attack XP", ChatTabDisplay.ATTACK_XP.toString());
		assertEquals("Strength XP", ChatTabDisplay.STRENGTH_XP.toString());
		assertEquals("Defence XP", ChatTabDisplay.DEFENCE_XP.toString());
		assertEquals("Ranged XP", ChatTabDisplay.RANGED_XP.toString());
		assertEquals("Magic XP", ChatTabDisplay.MAGIC_XP.toString());
		assertEquals("Hitpoints XP", ChatTabDisplay.HITPOINTS_XP.toString());
		assertEquals("Clock", ChatTabDisplay.CLOCK.toString());
	}

	@Test
	public void cannonWarningsAndPingAreEnabledByDefault()
	{
		assertTrue(config.warnCannonEmpty());
		assertTrue(config.warnCannonRepair());
		assertTrue(config.soundCannonEmpty());
		assertFalse(config.notifyCannonWarnings());
	}

	@Test
	public void slayerAndInventoryHelpersAreEnabledByDefault()
	{
		assertTrue(config.hideDeathSpawns());
		assertTrue(config.flashSuperiorSpawns());
		assertTrue(config.superiorExamineHints());
		assertTrue(config.showInventorySpaces());
		assertTrue(config.showSaturatedHeartReminder());
		assertTrue(config.slayerTrackingEnabled());
	}

	@Test
	public void slayerTrackingUsesItsOwnConfigKey() throws Exception
	{
		ConfigItem item = PvmToolsConfig.class.getMethod("slayerTrackingEnabled")
			.getAnnotation(ConfigItem.class);
		assertEquals("slayerTrackingEnabled", item.keyName());
		assertEquals(PvmToolsConfig.generalSection, item.section());
	}

	@Test
	public void saturatedHeartReminderUsesItsOwnConfigKey() throws Exception
	{
		ConfigItem item = PvmToolsConfig.class.getMethod("showSaturatedHeartReminder")
			.getAnnotation(ConfigItem.class);
		assertEquals("showSaturatedHeartReminder", item.keyName());
		assertEquals(PvmToolsConfig.timerSection, item.section());
	}

	@Test
	public void lootGlowUsesVisibleOrangeOutlineByDefault()
	{
		assertTrue(config.highlightGroundItems());
		assertEquals(0, config.groundItemHighlightMinimum());
		assertEquals(new Color(255, 170, 0), config.groundItemHighlightColor());
	}

	@Test
	public void groundLootHelpersUseSafeDefaults()
	{
		assertEquals(ToolkitPriceSource.GE_GUIDE, config.priceSource());
		assertEquals(ToolkitMarketPriceSource.GE_GUIDE, config.supplyPriceSource());
		assertTrue(config.groundItemLifetimeText());
		// 0 shows the drop timer on every visible drop.
		assertEquals(0, config.groundItemLifetimeThreshold());
		assertTrue(config.lootClickThrough());
	}

	@Test
	public void highAlchCanOnlyBeSelectedForLootValue()
	{
		assertEquals("High Alch values", ToolkitPriceSource.HIGH_ALCH.toString());
		assertEquals(2, ToolkitMarketPriceSource.values().length);
	}

	@Test
	public void sharedWarningsUseTheSafeThreeFlashDefaults()
	{
		assertTrue(config.flashScreenWarning());
		assertTrue(config.warningPopup());
		assertEquals(55, config.warningIntensity());
		assertEquals(10, config.warningSeconds());
		assertEquals(25, config.hitpointsWarningThresholdPercent());
		assertEquals(10, config.prayerWarningThreshold());
		assertEquals(ResourceWarningSound.BELL, config.hitpointsWarningSound());
		assertEquals(ResourceWarningSound.CHIME, config.prayerWarningSound());
	}
}
