package com.arber.pvmtools;

import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.game.ItemManager;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PvmSerpentineSupplyTest
{
	private static final int HEAD_SLOT = EquipmentInventorySlot.HEAD.getSlotIdx();

	@Test
	public void chargedSerpentineHelmScaleDropIsRecordedExactly()
	{
		Fixture fixture = new Fixture(ItemID.SERPENTINE_HELM, 11_000);
		fixture.equipmentChanged(ItemID.SERPENTINE_HELM, 10_990);

		verify(fixture.consumer).record(
			ItemID.ZULRAHS_SCALES, 10, PvmToolsPlugin.SupplyCostType.ZULRAH_SCALE);
		verifyNoMoreInteractions(fixture.consumer);
	}

	@Test
	public void recolouredSerpentineHelmsUseTheSameScaleAccounting()
	{
		for (int helmId : new int[]{ItemID.TANZANITE_HELM, ItemID.MAGMA_HELM})
		{
			Fixture fixture = new Fixture(helmId, 500);
			fixture.equipmentChanged(helmId, 490);

			verify(fixture.consumer).record(
				ItemID.ZULRAHS_SCALES, 10, PvmToolsPlugin.SupplyCostType.ZULRAH_SCALE);
			verifyNoMoreInteractions(fixture.consumer);
		}
	}

	@Test
	public void removingOrEquippingAHelmDoesNotLookLikeScaleConsumption()
	{
		Fixture fixture = new Fixture(ItemID.SERPENTINE_HELM, 11_000);
		fixture.equipmentChanged(-1, 0);
		verifyNoMoreInteractions(fixture.consumer);

		fixture.equipmentChanged(ItemID.SERPENTINE_HELM, 11_000);
		verifyNoMoreInteractions(fixture.consumer);
	}

	@Test
	public void finalDepletionToUnchargedHelmCountsTheRemainingScales()
	{
		Fixture fixture = new Fixture(ItemID.SERPENTINE_HELM, 10);
		fixture.equipmentChanged(ItemID.SERPENTINE_HELM_UNCHARGED, 1);

		verify(fixture.consumer).record(
			ItemID.ZULRAHS_SCALES, 10, PvmToolsPlugin.SupplyCostType.ZULRAH_SCALE);
		verifyNoMoreInteractions(fixture.consumer);
	}

	@Test
	public void reinitializingOnLoginEstablishesTheCurrentHelmBaseline()
	{
		Fixture fixture = new Fixture(ItemID.SERPENTINE_HELM, 10_990);
		fixture.tracker.initialize();
		fixture.equipmentChanged(ItemID.SERPENTINE_HELM, 10_980);

		verify(fixture.consumer).record(
			ItemID.ZULRAHS_SCALES, 10, PvmToolsPlugin.SupplyCostType.ZULRAH_SCALE);
		verifyNoMoreInteractions(fixture.consumer);
	}

	private static final class Fixture
	{
		private final Client client = mock(Client.class);
		private final PvmSupplyUsageTracker.SupplyUsageConsumer consumer = mock(PvmSupplyUsageTracker.SupplyUsageConsumer.class);
		private final PvmSupplyUsageTracker tracker = new PvmSupplyUsageTracker(
			client, mock(ItemManager.class), consumer);

		private Fixture(int helmId, int quantity)
		{
			when(client.getTickCount()).thenReturn(100);
			when(client.getSkillExperience(any(Skill.class))).thenReturn(1_000);
			ItemContainer inventory = emptyContainer();
			ItemContainer equipment = equipment(helmId, quantity);
			when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inventory);
			when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(equipment);
			tracker.initialize();
		}

		private void equipmentChanged(int helmId, int quantity)
		{
			ItemContainer equipment = equipment(helmId, quantity);
			when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(equipment);
			tracker.onItemContainerChanged(new ItemContainerChanged(
				InventoryID.EQUIPMENT.getId(), equipment));
		}

		private ItemContainer emptyContainer()
		{
			ItemContainer container = mock(ItemContainer.class);
			when(container.getItems()).thenReturn(new Item[0]);
			return container;
		}

		private ItemContainer equipment(int helmId, int quantity)
		{
			Item[] items = new Item[Math.max(EquipmentInventorySlot.AMMO.getSlotIdx() + 1, HEAD_SLOT + 1)];
			for (int index = 0; index < items.length; index++)
			{
				items[index] = new Item(-1, 0);
			}
			if (helmId > 0)
			{
				items[HEAD_SLOT] = new Item(helmId, quantity);
			}

			ItemContainer container = mock(ItemContainer.class);
			when(container.getItems()).thenReturn(items);
			when(container.getItem(anyInt())).thenAnswer(call -> items[call.getArgument(0)]);
			return container;
		}
	}
}
