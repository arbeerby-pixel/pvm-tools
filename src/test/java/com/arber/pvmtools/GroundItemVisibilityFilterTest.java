package com.arber.pvmtools;

import net.runelite.api.ItemComposition;
import net.runelite.api.TileItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GroundItemVisibilityFilterTest
{
	private final ConfigManager configManager = mock(ConfigManager.class);
	private final ItemManager itemManager = mock(ItemManager.class);

	@Test
	public void hiddenHighlightedAndQuantityRulesStillApply()
	{
		when(configManager.getConfiguration("grounditems", "hiddenItems")).thenReturn("Bones, Coins<1000, Rune*");
		when(configManager.getConfiguration("grounditems", "highlightedItems")).thenReturn("Rune platebody");
		item(526, "Bones");
		item(995, "Coins");
		item(1127, "Rune platebody");
		item(1113, "Rune chainbody");
		item(4151, "Abyssal whip");
		GroundItemVisibilityFilter filter = GroundItemVisibilityFilter.load(configManager, itemManager);

		assertFalse(filter.isVisible(drop(526, 1)));
		assertFalse(filter.isVisible(drop(995, 999)));
		assertTrue(filter.isVisible(drop(995, 1000)));
		assertTrue(filter.isVisible(drop(1127, 1)));
		assertFalse(filter.isVisible(drop(1113, 1)));
		assertTrue(filter.isVisible(drop(4151, 1)));
	}

	@Test
	public void sameItemAndQuantityIsOnlyWorkedOutOnce()
	{
		when(configManager.getConfiguration("grounditems", "hiddenItems")).thenReturn("Coins<1000");
		item(995, "Coins");
		GroundItemVisibilityFilter filter = GroundItemVisibilityFilter.load(configManager, itemManager);

		for (int frame = 0; frame < 50; frame++)
		{
			assertFalse(filter.isVisible(drop(995, 10)));
			assertTrue(filter.isVisible(drop(995, 5_000)));
		}

		verify(itemManager, times(2)).getItemComposition(995);
	}

	private void item(int itemId, String name)
	{
		ItemComposition composition = mock(ItemComposition.class);
		when(composition.getName()).thenReturn(name);
		when(itemManager.getItemComposition(itemId)).thenReturn(composition);
	}

	private static TileItem drop(int itemId, int quantity)
	{
		TileItem item = mock(TileItem.class);
		when(item.getId()).thenReturn(itemId);
		when(item.getQuantity()).thenReturn(quantity);
		return item;
	}
}
