package com.arber.pvmtools;

import java.awt.Color;
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
		assertTrue(config.tradeButtonClock());
		assertTrue(config.clanLootTracker());
		assertTrue(config.publicSupplyCostTracker());
		assertTrue(config.channelCombatXpTracker());
		assertTrue(config.privateSlayerXpTracker());
		assertTrue(config.topXpSkillTracker());
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
	}

	@Test
	public void lootGlowUsesVisibleOrangeOutlineByDefault()
	{
		assertTrue(config.highlightGroundItems());
		assertEquals(0, config.groundItemHighlightMinimum());
		assertEquals(new Color(255, 170, 0), config.groundItemHighlightColor());
		assertEquals(2, config.groundItemHighlightWidth());
	}

	@Test
	public void groundLootHelpersUseSafeDefaults()
	{
		assertTrue(config.groundItemLifetimeText());
		assertEquals(GroundItemLifetimeMode.ALL_VISIBLE, config.groundItemLifetimeMode());
		assertEquals(10_000, config.groundItemLifetimeThreshold());
		assertEquals(35, config.groundItemLifetimeBackground());
		assertTrue(config.lootClickThrough());
		assertTrue(config.wildernessSafety());
	}

	@Test
	public void sharedWarningsUseTheSafeThreeFlashDefaults()
	{
		assertTrue(config.flashScreenWarning());
		assertTrue(config.warningPopup());
		assertEquals(55, config.warningIntensity());
		assertEquals(3, config.warningFlashSeconds());
		assertEquals(10, config.warningSeconds());
	}
}
