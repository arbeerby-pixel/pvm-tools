package com.arber.pvmtools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.PluginManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PvmLootAccountingTest
{
	@Test
	public void coinsAndGroundItemFromOneDeathCountOneKillInEitherEventOrder() throws Exception
	{
		for (boolean serverFirst : new boolean[]{false, true})
		{
			Fixture fixture = new Fixture();
			if (serverFirst)
			{
				fixture.serverLoot(100, true);
				fixture.death();
			}
			else
			{
				fixture.death();
				fixture.serverLoot(100, true);
			}
			assertEquals(1, fixture.sources().size());
			Object deathSource = fixture.sources().get(0);
			fixture.inventoryCoins(100);
			Object groundSource = invoke(fixture.plugin, "claimPendingNpcLootSource",
				new Class<?>[]{int.class, int.class, WorldPoint.class, String.class},
				554, 1, Fixture.POINT, "Nechryael");
			assertSame(deathSource, groundSource);
			fixture.recordGroundLoot(groundSource);

			fixture.assertLoot(110L, 1L);
		}
	}

	@Test
	public void twoSameTypeKillsRemainDistinctWhenOneInventoryUpdateBatchesCoins() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.death();
		fixture.serverLoot(100, false);
		fixture.death();
		fixture.serverLoot(50, false);
		assertEquals(2, fixture.sources().size());
		assertNotSame(fixture.sources().get(0), fixture.sources().get(1));
		fixture.inventoryCoins(150);

		fixture.assertLoot(150L, 2L);
		assertEquals(0, fixture.pendingGains().size());
	}

	@Test
	public void laterServerLootCanClaimTheRemainderOfABatchedInventoryGain() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.inventoryCoins(150);
		fixture.serverLoot(100, false);
		fixture.assertLoot(100L, 1L);
		assertEquals(1, fixture.pendingGains().size());
		assertEquals(50, ((Integer) get(fixture.pendingGains().get(0), "quantity")).intValue());
		fixture.tick++;
		fixture.serverLoot(50, false);

		fixture.assertLoot(150L, 2L);
		assertEquals(0, fixture.pendingGains().size());
	}

	@Test
	public void unconfirmedCurrencyRemainderExpiresWithoutBecomingLoot() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.inventoryCoins(150);
		fixture.serverLoot(100, false);
		fixture.tick += 4;
		invoke(fixture.plugin, "reconcilePendingDirectCurrencyLoot", new Class<?>[0]);
		assertEquals(0, fixture.pendingGains().size());
		fixture.serverLoot(50, false);

		fixture.assertLoot(100L, 1L);
	}

	@Test
	public void returningRemainingCannonballsDoesNotCountAsConsumption() throws Exception
	{
		Fixture fixture = new Fixture();
		set(fixture.plugin, "cannonPlaced", true);
		when(fixture.client.getVarpValue(VarPlayerID.DROPCANNON)).thenReturn(0);
		fixture.cannonChange(50, 0);
		when(fixture.client.getVarpValue(VarPlayerID.DROPCANNON)).thenReturn(4);
		invoke(fixture.plugin, "suppressCannonPickupWarnings", new Class<?>[0]);
		fixture.cannonChange(50, 0);

		assertEquals(0L, fixture.stats().getCannonballCount());
		assertEquals(0L, fixture.stats().getSupplyCostValue());
		assertEquals(0L, ((Long) get(fixture.plugin, "cannonballSupplyCount")).longValue());
	}

	@Test
	public void firingOneCannonballCountsOneSupply() throws Exception
	{
		Fixture fixture = new Fixture();
		set(fixture.plugin, "cannonPlaced", true);
		when(fixture.client.getVarpValue(VarPlayerID.DROPCANNON)).thenReturn(4);
		fixture.cannonChange(50, 49);

		assertEquals(1L, fixture.stats().getCannonballCount());
		assertEquals(100L, fixture.stats().getCannonballSupplyCostValue());
		assertEquals(1L, ((Long) get(fixture.plugin, "cannonballSupplyCount")).longValue());
		assertEquals(100L, ((Long) get(fixture.plugin, "supplyCostValue")).longValue());
	}

	@Test
	public void firingWhileWalkingToPickUpCannonStillCountsSupplies() throws Exception
	{
		Fixture fixture = new Fixture();
		set(fixture.plugin, "cannonPlaced", true);
		when(fixture.client.getVarpValue(VarPlayerID.DROPCANNON)).thenReturn(4);
		invoke(fixture.plugin, "suppressCannonPickupWarnings", new Class<?>[0]);
		fixture.cannonChange(50, 49);
		assertEquals(1L, fixture.stats().getCannonballCount());
		assertEquals(100L, fixture.stats().getCannonballSupplyCostValue());
	}

	@Test
	public void hiddenLootTrackerStillRecordsAndPersistsLoot() throws Exception
	{
		Fixture fixture = new Fixture();
		doAnswer(call -> false).when(fixture.config).clanLootTracker();
		fixture.death();
		fixture.serverLoot(100, false);
		fixture.inventoryCoins(100);
		fixture.assertLoot(100L, 1L);
		invoke(fixture.plugin, "checkpointTrackerPersistence", new Class<?>[]{long.class, boolean.class},
			System.currentTimeMillis(), true);
		invoke(fixture.plugin, "checkpointStatsPersistence", new Class<?>[]{long.class, boolean.class},
			System.currentTimeMillis(), true);

		assertEquals("100", fixture.saved.get("savedLootTrackerValue"));
		assertEquals(100L, PvmToolsStats.deserialize(fixture.saved.get("savedStatsAllTimeV1"), "all").getLootValue());
	}

	private static final class Fixture
	{
		private static final WorldPoint POINT = new WorldPoint(3200, 3200, 0);
		private final PvmToolsPlugin plugin = new PvmToolsPlugin();
		private final Client client = mock(Client.class);
		private final ItemContainer inventory = mock(ItemContainer.class);
		private final NPCComposition composition = mock(NPCComposition.class);
		private final PvmToolsConfig config = mock(PvmToolsConfig.class, call ->
			Modifier.isAbstract(call.getMethod().getModifiers())
				? RETURNS_DEFAULTS.answer(call) : CALLS_REAL_METHODS.answer(call));
		private final Map<String, String> saved = new HashMap<>();
		private final Deque<Runnable> queued = new ArrayDeque<>();
		private int tick = 100;

		private Fixture() throws Exception
		{
			doAnswer(call -> saved.get("savedStatsDayV1")).when(config).savedStatsDay();
			doAnswer(call -> saved.get("savedStatsWeekV1")).when(config).savedStatsWeek();
			doAnswer(call -> saved.get("savedStatsMonthV1")).when(config).savedStatsMonth();
			doAnswer(call -> saved.get("savedStatsYearV1")).when(config).savedStatsYear();
			doAnswer(call -> saved.get("savedStatsAllTimeV1")).when(config).savedStatsAllTime();
			Player player = mock(Player.class);
			when(player.getWorldLocation()).thenReturn(POINT);
			when(client.getLocalPlayer()).thenReturn(player);
			when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
			when(client.isClientThread()).thenReturn(true);
			when(client.getTickCount()).thenAnswer(call -> tick);
			when(composition.getName()).thenReturn("Nechryael");
			when(composition.getCombatLevel()).thenReturn(115);
			ItemManager itemManager = mock(ItemManager.class);
			ItemComposition item = mock(ItemComposition.class);
			when(item.getName()).thenReturn("Test item");
			when(item.getPrice()).thenReturn(100);
			when(itemManager.getItemComposition(anyInt())).thenReturn(item);
			when(itemManager.getItemPriceWithSource(anyInt(), eq(false))).thenReturn(100L);
			ClientThread clientThread = mock(ClientThread.class);
			doAnswer(call ->
			{
				queued.add(call.getArgument(0));
				return null;
			}).when(clientThread).invokeLater(any(Runnable.class));
			ConfigManager configManager = mock(ConfigManager.class);
			when(configManager.getConfiguration(anyString(), anyString()))
				.thenAnswer(call -> saved.get(call.getArgument(1)));
			doAnswer(call ->
			{
				saved.put(call.getArgument(1), call.getArgument(2));
				return null;
			}).when(configManager).setConfiguration(anyString(), anyString(), anyString());
			PluginManager pluginManager = mock(PluginManager.class);
			when(pluginManager.getPlugins()).thenReturn(List.of(plugin));
			when(pluginManager.isPluginActive(plugin)).thenReturn(true);
			saved.put("activeOwner", "PVM");
			saved.put("ownerSource", "activity");
			saved.put("ownerLeaseUntilMillis", Long.toString(Long.MAX_VALUE));
			set(plugin, "started", true);
			set(plugin, "client", client);
			set(plugin, "clientThread", clientThread);
			set(plugin, "itemManager", itemManager);
			set(plugin, "configManager", configManager);
			set(plugin, "config", config);
			set(plugin, "pluginManager", pluginManager);
			inventoryCoins(0);
		}

		private void death()
		{
			NPC npc = mock(NPC.class);
			when(npc.getWorldLocation()).thenReturn(POINT);
			when(npc.getName()).thenReturn("Nechryael");
			when(npc.getCombatLevel()).thenReturn(115);
			plugin.onActorDeath(new ActorDeath(npc));
		}

		private void serverLoot(int coins, boolean groundItem)
		{
			List<ItemStack> items = groundItem
				? List.of(new ItemStack(ItemID.COINS, coins), new ItemStack(554, 1))
				: List.of(new ItemStack(ItemID.COINS, coins));
			plugin.onServerNpcLoot(new ServerNpcLoot(composition, items));
		}

		private void inventoryCoins(int coins) throws Exception
		{
			when(inventory.getItems()).thenReturn(new Item[]{new Item(ItemID.COINS, coins)});
			invoke(plugin, "trackDirectNpcCurrencyLoot", new Class<?>[]{ItemContainer.class}, inventory);
		}

		private void recordGroundLoot(Object source) throws Exception
		{
			invoke(plugin, "recordPickedUpNpcLoot",
				new Class<?>[]{int.class, int.class, long.class, source.getClass()}, 554, 1, 10L, source);
		}

		private void cannonChange(int before, int after) throws Exception
		{
			invoke(plugin, "trackCannonballSupplyCost", new Class<?>[]{int.class, int.class}, before, after);
		}

		private PvmToolsStats stats()
		{
			return plugin.getStatsSnapshot(PvmToolsStatsPeriod.ALL_TIME);
		}

		private void assertLoot(long value, long kills) throws Exception
		{
			assertEquals(value, stats().getLootValue());
			assertEquals(value, ((Long) get(plugin, "npcLootValue")).longValue());
			assertEquals(1, stats().getCombatLootSources().size());
			assertEquals(kills, stats().getCombatLootSources().get(0).getKills());
			assertEquals(value, stats().getCombatLootSources().get(0).getTotalValue());
		}

		private List<?> sources() throws Exception
		{
			return (List<?>) get(plugin, "recentNpcDeaths");
		}

		private List<?> pendingGains() throws Exception
		{
			return (List<?>) get(plugin, "pendingDirectCurrencyGains");
		}
	}

	private static Object invoke(Object target, String name, Class<?>[] types, Object... values) throws Exception
	{
		Method method = target.getClass().getDeclaredMethod(name, types);
		method.setAccessible(true);
		return method.invoke(target, values);
	}

	private static Object get(Object target, String name) throws Exception
	{
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static void set(Object target, String name, Object value) throws Exception
	{
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
