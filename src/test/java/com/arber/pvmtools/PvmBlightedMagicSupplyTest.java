package com.arber.pvmtools;

import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PvmBlightedMagicSupplyTest
{
	@Test
	public void confirmedSpellConsumptionTracksEachBlightedSackByItsOwnItemId()
	{
		int[] sackIds = {
			ItemID.BLIGHTED_ANCIENT_ICE_SACK,
			ItemID.BLIGHTED_ENTANGLE_SACK,
			ItemID.BLIGHTED_TELEPORT_SPELL_SACK,
			ItemID.BLIGHTED_VENGEANCE_SACK,
			ItemID.BLIGHTED_SURGE_SACK
		};
		for (int itemId : sackIds)
		{
			Fixture fixture = new Fixture(itemId, 10);
			fixture.inventoryChanged(itemId, 9);
			fixture.magicXpChanged();
			fixture.tracker.onGameTick();

			verify(fixture.consumer).record(itemId, 1, PvmToolsPlugin.SupplyCostType.RUNE);
			verifyNoMoreInteractions(fixture.consumer);
		}
	}

	@Test
	public void lastSackConsumedBySpellIsCounted()
	{
		Fixture fixture = new Fixture(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 1);
		fixture.magicXpChanged();
		fixture.inventoryChanged(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 0);
		fixture.tracker.onGameTick();

		verify(fixture.consumer).record(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 1, PvmToolsPlugin.SupplyCostType.RUNE);
		verifyNoMoreInteractions(fixture.consumer);
	}

	@Test
	public void inventoryRemovalWithoutSpellDoesNotCountAsSackConsumption()
	{
		Fixture fixture = new Fixture(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 10);
		fixture.inventoryChanged(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 0);
		fixture.tracker.onGameTick();
		fixture.magicXpChanged();
		fixture.tracker.onGameTick();

		verifyNoMoreInteractions(fixture.consumer);
	}

	@Test
	public void spellWithoutInventoryDecreaseDoesNotCountSackConsumption()
	{
		Fixture fixture = new Fixture(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 10);
		fixture.inventoryChanged(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 10);
		fixture.magicXpChanged();
		fixture.tracker.onGameTick();

		verifyNoMoreInteractions(fixture.consumer);
	}

	@Test
	public void existingRuneConsumptionStillTracksActualRunes()
	{
		Fixture fixture = new Fixture(ItemID.WATER_RUNE, 10);
		fixture.inventoryChanged(ItemID.WATER_RUNE, 8);
		fixture.magicXpChanged();
		fixture.tracker.onGameTick();

		verify(fixture.consumer).record(ItemID.WATER_RUNE, 2, PvmToolsPlugin.SupplyCostType.RUNE);
		verifyNoMoreInteractions(fixture.consumer);
	}

	@Test
	public void magicLevelChangeWithoutXpDoesNotChargeBankedSacks()
	{
		Fixture fixture = new Fixture(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 10);
		fixture.inventoryChanged(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 0);
		fixture.tracker.onStatChanged(new StatChanged(Skill.MAGIC, 1_000, 10, 11));
		fixture.tracker.onGameTick();

		verifyNoMoreInteractions(fixture.consumer);
	}

	@Test
	public void duplicateAndLowerXpAfterCastCannotChargeLaterBankRemoval()
	{
		Fixture fixture = new Fixture(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 10);
		fixture.inventoryChanged(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 9);
		fixture.magicXpChanged();
		fixture.tracker.onGameTick();
		fixture.inventoryChanged(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 0);
		fixture.tracker.onStatChanged(new StatChanged(Skill.MAGIC, 1_005, 10, 10));
		fixture.tracker.onStatChanged(new StatChanged(Skill.MAGIC, 1_010, 10, 10));
		fixture.tracker.onGameTick();

		verify(fixture.consumer).record(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 1, PvmToolsPlugin.SupplyCostType.RUNE);
		verifyNoMoreInteractions(fixture.consumer);
	}

	@Test
	public void zeroXpLoadIsNotCastButFirstRealGainStillConfirmsConsumption()
	{
		Fixture fixture = new Fixture(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 10, 0);
		fixture.inventoryChanged(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 9);
		fixture.tracker.onStatChanged(new StatChanged(Skill.MAGIC, 0, 1, 2));
		fixture.tracker.onGameTick();
		verifyNoMoreInteractions(fixture.consumer);

		fixture.inventoryChanged(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 8);
		fixture.magicXpChanged();
		fixture.tracker.onGameTick();
		verify(fixture.consumer).record(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 1, PvmToolsPlugin.SupplyCostType.RUNE);
		verifyNoMoreInteractions(fixture.consumer);
	}

	@Test
	public void readyLoginBaselineDoesNotTreatFullMagicXpAsCast()
	{
		Fixture fixture = new Fixture(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 10);
		fixture.tracker.reset();
		when(fixture.client.getSkillExperience(Skill.MAGIC)).thenReturn(20_000_000);
		fixture.tracker.initialize();
		fixture.inventoryChanged(ItemID.BLIGHTED_ANCIENT_ICE_SACK, 0);
		fixture.tracker.onStatChanged(new StatChanged(Skill.MAGIC, 20_000_000, 99, 99));
		fixture.tracker.onGameTick();

		verifyNoMoreInteractions(fixture.consumer);
	}

	private static final class Fixture
	{
		private final Client client = mock(Client.class);
		private final PvmSupplyUsageTracker.SupplyUsageConsumer consumer = mock(PvmSupplyUsageTracker.SupplyUsageConsumer.class);
		private final PvmSupplyUsageTracker tracker = new PvmSupplyUsageTracker(client, null, consumer);
		private int magicExperience;

		private Fixture(int itemId, int quantity)
		{
			this(itemId, quantity, 1_000);
		}

		private Fixture(int itemId, int quantity, int magicExperience)
		{
			this.magicExperience = magicExperience;
			when(client.getTickCount()).thenReturn(100);
			when(client.getSkillExperience(Skill.MAGIC)).thenAnswer(call -> this.magicExperience);
			ItemContainer initialInventory = inventory(itemId, quantity);
			when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(initialInventory);
			tracker.initialize();
		}

		private void inventoryChanged(int itemId, int quantity)
		{
			ItemContainer changedInventory = inventory(itemId, quantity);
			when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(changedInventory);
			tracker.onItemContainerChanged(new ItemContainerChanged(
				InventoryID.INVENTORY.getId(), changedInventory));
		}

		private void magicXpChanged()
		{
			magicExperience += 10;
			tracker.onStatChanged(new StatChanged(Skill.MAGIC, magicExperience, 10, 10));
		}

		private ItemContainer inventory(int itemId, int quantity)
		{
			ItemContainer inventory = mock(ItemContainer.class);
			when(inventory.getItems()).thenReturn(quantity > 0
				? new Item[]{new Item(itemId, quantity)}
				: new Item[0]);
			return inventory;
		}
	}
}
