package com.arber.pvmtools;

import java.util.Collections;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmSlayerTaskAttributionTest
{
	@Test
	public void taskNameMatchesItsOwnMonsterOnly()
	{
		Set<String> noTargets = Collections.emptySet();
		assertTrue(PvmToolsPlugin.isSlayerTaskTarget("Greater demon", "Greater Demons", noTargets));
		assertFalse(PvmToolsPlugin.isSlayerTaskTarget("Zulrah", "Greater Demons", noTargets));
		assertFalse(PvmToolsPlugin.isSlayerTaskTarget("Black demon", "Greater Demons", noTargets));
	}

	@Test
	public void runeliteTargetsCoverMonstersNotNamedAfterTheTask()
	{
		Set<String> targets = Set.of("cerberus", "skeleton hellhound");
		assertTrue(PvmToolsPlugin.isSlayerTaskTarget("Cerberus", "Hellhounds", targets));
		assertTrue(PvmToolsPlugin.isSlayerTaskTarget("Skeleton Hellhound", "Hellhounds", targets));
		assertFalse(PvmToolsPlugin.isSlayerTaskTarget("Vorkath", "Hellhounds", targets));
	}

	@Test
	public void missingSourceOrTaskCountsForNothing()
	{
		Set<String> targets = Set.of("greater demon");
		assertFalse(PvmToolsPlugin.isSlayerTaskTarget(null, "Greater Demons", targets));
		assertFalse(PvmToolsPlugin.isSlayerTaskTarget("", "Greater Demons", targets));
		assertFalse(PvmToolsPlugin.isSlayerTaskTarget("Greater demon", "", targets));
	}

	@Test
	public void completionMessagesAreRecognised()
	{
		assertTrue(PvmToolsPlugin.isSlayerTaskCompletionMessage(
			"You have completed your task! You killed 195 Greater demons. You gained 16,380 xp."));
		assertTrue(PvmToolsPlugin.isSlayerTaskCompletionMessage(
			"You've completed 120 tasks and received 15 points, giving you a total of 300; return to a Slayer master."));
		assertFalse(PvmToolsPlugin.isSlayerTaskCompletionMessage("You're assigned to kill Greater demons; only 11 more to go."));
		assertFalse(PvmToolsPlugin.isSlayerTaskCompletionMessage("You've completed the Dragon Slayer quest."));
	}
}
