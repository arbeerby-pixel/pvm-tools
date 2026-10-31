package com.arber.pvmtools;

import com.arber.pvmtools.PvmToolsPlugin.SupplyCostType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PvmAccountSwitchTest
{
	@Test
	public void mainAndGimRestoreTheirOwnCountersPeriodsTasksAndHistory() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.select("rs-main");
		fixture.activity(100, 25, 50, 10);
		invoke(fixture.plugin, "startSlayerTask", "Nechryael", "Slayer Tower", 20, 30);
		invoke(fixture.plugin, "addTaskHistoryEntry", fixture.plugin.getCurrentSlayerTaskSnapshot(), 1000L);
		long now = System.currentTimeMillis();
		set(fixture.plugin, "currentSlayerTaskElapsedMillis", 10_000L);
		set(fixture.plugin, "currentSlayerTaskActiveSinceMillis", now - 5_000L);
		set(fixture.plugin, "currentSlayerTaskLastActivityMillis", now);
		fixture.plugin.toggleCombatLootItemExcluded("Nechryael", 995);
		fixture.runQueued();

		// RuneLite changes its current key before delivering the profile event.
		fixture.select("rs-gim");
		fixture.assertTotals(0, 0, 0, 0);
		assertFalse(fixture.plugin.getCurrentSlayerTaskSnapshot().isActive());
		assertTrue(fixture.plugin.getTaskHistorySnapshot().isEmpty());
		assertFalse(fixture.plugin.isCombatLootItemExcluded("Nechryael", 995));
		fixture.activity(7, 3, 8, 2);
		invoke(fixture.plugin, "startSlayerTask", "Bloodveld", "Catacombs", 40, 40);

		fixture.select("rs-main");
		fixture.assertTotals(100, 25, 50, 10);
		assertEquals("Nechryael", fixture.plugin.getCurrentSlayerTaskSnapshot().getName());
		assertTrue(fixture.plugin.getCurrentSlayerTaskSnapshot().getElapsedMillis() >= 15_000L);
		assertEquals(0L, get(fixture.plugin, "currentSlayerTaskActiveSinceMillis"));
		assertEquals(1, fixture.plugin.getTaskHistorySnapshot().size());
		assertTrue(fixture.plugin.isCombatLootItemExcluded("Nechryael", 995));
		for (PvmToolsStatsPeriod period : PvmToolsStatsPeriod.values())
		{
			assertEquals(100L, fixture.plugin.getStatsSnapshot(period).getLootValue());
		}

		fixture.select("rs-gim");
		fixture.assertTotals(7, 3, 8, 2);
		assertEquals("Bloodveld", fixture.plugin.getCurrentSlayerTaskSnapshot().getName());
		assertTrue(fixture.plugin.getTaskHistorySnapshot().isEmpty());
		assertEquals(0L, get(fixture.plugin, "currentSlayerTaskActiveSinceMillis"));
	}

	@Test
	public void delayedResetFromMainCannotEraseTheGimAccount() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.select("rs-gim");
		fixture.activity(7, 3, 8, 2);
		fixture.select("rs-main");
		fixture.activity(100, 25, 50, 10);
		fixture.plugin.resetTrackers(true, true, true, true);
		fixture.store.account = "rs-gim";
		invoke(fixture.plugin, "synchronizeAccountPersistence", false);
		fixture.runQueued();
		fixture.assertTotals(7, 3, 8, 2);
	}

	@Test
	public void normalConfigProfileSwitchRetainsTheAccountAndItsUnflushedActivity() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.select("rs-main");
		fixture.activity(100, 25, 50, 10);
		fixture.store.configProfile = 2L;
		fixture.store.settings().put("savedLootTrackerValue", "999999");
		fixture.plugin.onProfileChanged(new ProfileChanged());
		fixture.runQueued();
		fixture.assertTotals(100, 25, 50, 10);
		assertEquals("999999", fixture.store.settings().get("savedLootTrackerValue"));
		assertTrue(fixture.store.settings().containsKey(PvmAccountPersistence.LEGACY_BACKUP_KEY));
	}

	@Test
	public void missingAccountKeepsLegacyHistoryUntouchedAndCannotCollectIntoIt() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.store.settings().put("savedLootTrackerValue", "999999");
		fixture.select(null);
		invoke(fixture.plugin, "addSupplyCost", 25L, SupplyCostType.FOOD, 1L);
		invoke(fixture.plugin, "checkpointTrackerPersistence", System.currentTimeMillis(), true);
		assertEquals("999999", fixture.store.settings().get("savedLootTrackerValue"));
		assertTrue(fixture.store.accounts.isEmpty());
		fixture.select("rs-gim");
		fixture.assertTotals(0, 0, 0, 0);
	}

	private static final class Fixture
	{
		private final PvmAccountPersistenceTest.Store store = new PvmAccountPersistenceTest.Store();
		private final PvmToolsPlugin plugin = new PvmToolsPlugin();
		private final Client client = mock(Client.class);
		private final Deque<Runnable> queued = new ArrayDeque<>();
		private int magicXp = 10_000;
		private int slayerXp = 10_000;

		private Fixture() throws Exception
		{
			PvmToolsConfig config = mock(PvmToolsConfig.class, call ->
				Modifier.isAbstract(call.getMethod().getModifiers())
					? RETURNS_DEFAULTS.answer(call) : CALLS_REAL_METHODS.answer(call));
			when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
			when(client.isClientThread()).thenReturn(true);
			when(client.getLocalPlayer()).thenReturn(mock(Player.class));
			when(client.getRealSkillLevel(any(Skill.class))).thenReturn(1);
			when(client.getSkillExperience(any(Skill.class))).thenAnswer(call ->
				call.getArgument(0) == Skill.MAGIC ? magicXp : call.getArgument(0) == Skill.SLAYER ? slayerXp : 10_000);
			ClientThread clientThread = mock(ClientThread.class);
			doAnswer(call ->
			{
				queued.add(call.getArgument(0));
				return null;
			}).when(clientThread).invokeLater(any(Runnable.class));
			PluginManager pluginManager = mock(PluginManager.class);
			when(pluginManager.getPlugins()).thenReturn(List.of(plugin));
			when(pluginManager.isPluginActive(plugin)).thenReturn(true);
			set(plugin, "started", true);
			set(plugin, "client", client);
			set(plugin, "clientThread", clientThread);
			set(plugin, "configManager", store.config);
			set(plugin, "config", config);
			set(plugin, "pluginManager", pluginManager);
			set(plugin, "itemManager", mock(ItemManager.class));
		}

		private void select(String profile) throws Exception
		{
			String previous = store.account;
			store.account = profile;
			plugin.onRuneScapeProfileChanged(new RuneScapeProfileChanged(previous, profile));
			runQueued();
			set(plugin, "xpTrackerInitializationTicks", 0);
			invoke(plugin, "syncTrackerSkillBaselines");
		}

		private void activity(long loot, long supply, int combat, int slayer) throws Exception
		{
			set(plugin, "npcLootValue", loot);
			invoke(plugin, "addLootStats", 995, 1L, loot, null);
			invoke(plugin, "persistLootTrackerValue");
			invoke(plugin, "addSupplyCost", supply, SupplyCostType.FOOD, 1L);
			magicXp += combat;
			slayerXp += slayer;
			plugin.onStatChanged(new StatChanged(Skill.MAGIC, magicXp, 99, 99));
			plugin.onStatChanged(new StatChanged(Skill.SLAYER, slayerXp, 99, 99));
		}

		private void assertTotals(long loot, long supply, long combat, long slayer) throws Exception
		{
			PvmToolsStats stats = plugin.getStatsSnapshot(PvmToolsStatsPeriod.ALL_TIME);
			assertEquals(loot, stats.getLootValue());
			assertEquals(supply, stats.getSupplyCostValue());
			assertEquals(combat, stats.getCombatXp());
			assertEquals(slayer, stats.getSlayerXp());
			assertEquals(loot, get(plugin, "npcLootValue"));
			assertEquals(supply, get(plugin, "supplyCostValue"));
			assertEquals(combat, invoke(plugin, "getCombatXpGained"));
			assertEquals(slayer, get(plugin, "slayerXpGained"));
		}

		private void runQueued()
		{
			while (!queued.isEmpty())
			{
				queued.removeFirst().run();
			}
		}
	}

	private static Object invoke(Object target, String name, Object... arguments) throws Exception
	{
		for (Method method : target.getClass().getDeclaredMethods())
		{
			if (method.getName().equals(name) && method.getParameterCount() == arguments.length)
			{
				method.setAccessible(true);
				return method.invoke(target, arguments);
			}
		}
		throw new NoSuchMethodException(name);
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
