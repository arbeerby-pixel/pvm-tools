package com.arber.pvmtools;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmSupplyIgnoreListTest
{
	@Test
	public void acceptsCommaSemicolonAndLineSeparatedItemNames()
	{
		PvmSupplyIgnoreList ignored = PvmSupplyIgnoreList.fromConfig(
			"Prayer potion, Shark; Dragon arrow\nZulrah's scales");

		assertTrue(ignored.matches("Prayer potion(4)"));
		assertTrue(ignored.matches("Prayer potion(1)"));
		assertTrue(ignored.matches("shark"));
		assertTrue(ignored.matches("Dragon arrow"));
		assertTrue(ignored.matches("Zulrah's scales"));
	}

	@Test
	public void normalizesDoseSuffixesAndWhitespaceWithoutPartialMatches()
	{
		PvmSupplyIgnoreList ignored = PvmSupplyIgnoreList.fromConfig("  Super   restore(2)  ");

		assertTrue(ignored.matches("Super restore(4)"));
		assertTrue(ignored.matches("SUPER RESTORE(1)"));
		assertTrue(ignored.matches("Super restore(6)"));
		assertFalse(ignored.matches("Blighted super restore(4)"));
	}

	@Test
	public void emptyConfigurationDoesNotIgnoreAnything()
	{
		PvmSupplyIgnoreList ignored = PvmSupplyIgnoreList.fromConfig(" , ; \n ");

		assertTrue(ignored.isEmpty());
		assertFalse(ignored.matches("Prayer potion(4)"));
	}
}
