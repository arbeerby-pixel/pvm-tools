package com.arber.pvmtools;

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
	public void sharedWarningsUseTheSafeThreeFlashDefaults()
	{
		assertTrue(config.flashScreenWarning());
		assertTrue(config.warningPopup());
		assertEquals(55, config.warningIntensity());
		assertEquals(3, config.warningFlashSeconds());
		assertEquals(10, config.warningSeconds());
	}
}
