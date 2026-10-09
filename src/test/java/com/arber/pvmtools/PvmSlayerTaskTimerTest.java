package com.arber.pvmtools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PvmSlayerTaskTimerTest
{
	@Test
	public void activeTimerUsesCurrentTimeBeforeTimeout()
	{
		assertEquals(90_000L, PvmToolsPlugin.calculateSlayerTaskSegmentEndMillis(
			10_000L, 80_000L, 90_000L, 120_000L));
	}

	@Test
	public void inactiveTimerStopsAtTimeoutDeadline()
	{
		assertEquals(200_000L, PvmToolsPlugin.calculateSlayerTaskSegmentEndMillis(
			10_000L, 80_000L, 900_000L, 120_000L));
	}

	@Test
	public void timerNeverEndsBeforeCurrentActiveSegment()
	{
		assertEquals(300_000L, PvmToolsPlugin.calculateSlayerTaskSegmentEndMillis(
			300_000L, 100_000L, 900_000L, 120_000L));
	}

	@Test
	public void missingActivityKeepsLegacyActiveBehavior()
	{
		assertEquals(900_000L, PvmToolsPlugin.calculateSlayerTaskSegmentEndMillis(
			10_000L, 0L, 900_000L, 120_000L));
	}
}
