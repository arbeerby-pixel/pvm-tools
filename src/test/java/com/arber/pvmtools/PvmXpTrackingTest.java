package com.arber.pvmtools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PvmXpTrackingTest
{
	@Test
	public void restoredTotalsAddEveryNewXpDropToChatAndStats() throws Exception
	{
		Fixture fixture = new Fixture(535_000L, 458_000L);
		fixture.gain(Skill.MAGIC, 108);
		fixture.gain(Skill.SLAYER, 43);
		fixture.gain(Skill.MAGIC, 72);
		fixture.gain(Skill.SLAYER, 51);

		fixture.assertTotals(535_180L, 458_094L);
		fixture.flush();
		assertEquals("458094", fixture.saved.get("savedSlayerXpTrackerValueV2"));
		assertEquals(535_180L, fixture.savedStats().getCombatXp(Skill.MAGIC));
		assertEquals(458_094L, fixture.savedStats().getSlayerXp());
		assertEquals(535_180L, fixture.savedCombatXp(Skill.MAGIC));
	}

	@Test
	public void combatTotalIncludesEachSkillOnce() throws Exception
	{
		Fixture fixture = new Fixture(535_000L, 0L);
		fixture.gain(Skill.MAGIC, 108);
		fixture.gain(Skill.HITPOINTS, 37);
		fixture.gain(Skill.RANGED, 43);

		fixture.assertTotals(535_188L, 0L);
		assertEquals(535_108L, fixture.chatSkillXp(Skill.MAGIC));
		assertEquals(37L, fixture.chatSkillXp(Skill.HITPOINTS));
		assertEquals(43L, fixture.chatSkillXp(Skill.RANGED));
		assertEquals(37L, fixture.stats().getCombatXp(Skill.HITPOINTS));
		assertEquals(43L, fixture.stats().getCombatXp(Skill.RANGED));
	}

	@Test
	public void zeroXpBaselineCountsTheFirstGain() throws Exception
	{
		Fixture fixture = new Fixture(0L, 0L);
		fixture.experience.put(Skill.MAGIC, 0);
		fixture.experience.put(Skill.SLAYER, 0);
		fixture.startPlugin();
		fixture.gain(Skill.MAGIC, 108);
		fixture.gain(Skill.SLAYER, 43);

		fixture.assertTotals(108L, 43L);
	}

	@Test
	public void duplicateAndStaleEventsCannotCountTheSameXpAgain() throws Exception
	{
		Fixture fixture = new Fixture(1_000L, 2_000L);
		fixture.gain(Skill.MAGIC, 100);
		fixture.gain(Skill.SLAYER, 50);
		fixture.event(Skill.MAGIC, 10_100);
		fixture.event(Skill.MAGIC, 10_060);
		fixture.event(Skill.SLAYER, 10_050);
		fixture.event(Skill.SLAYER, 10_025);
		fixture.gain(Skill.MAGIC, 25);
		fixture.gain(Skill.SLAYER, 15);

		fixture.assertTotals(1_125L, 2_065L);
	}

	@Test
	public void restartAndLoginRebaseXpWithoutLosingSavedTotals() throws Exception
	{
		Fixture fixture = new Fixture(1_000L, 2_000L);
		fixture.gain(Skill.MAGIC, 100);
		fixture.gain(Skill.SLAYER, 50);
		fixture.flush();
		fixture.startPlugin();
		fixture.gain(Skill.MAGIC, 25);
		fixture.gain(Skill.SLAYER, 15);
		fixture.assertTotals(1_125L, 2_065L);

		// A fresh login establishes a baseline even when account XP is lower.
		when(fixture.client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		invoke(fixture.plugin, "resetFamilyState");
		fixture.experience.put(Skill.MAGIC, 500);
		fixture.experience.put(Skill.SLAYER, 0);
		when(fixture.client.getGameState()).thenReturn(GameState.LOGGED_IN);
		invoke(fixture.plugin, "syncTrackerSkillBaselines");
		fixture.gain(Skill.MAGIC, 20);
		fixture.gain(Skill.SLAYER, 10);

		fixture.assertTotals(1_145L, 2_075L);
	}

	@Test
	public void resetIgnoresQueuedXpFromBeforeTheReset() throws Exception
	{
		Fixture fixture = new Fixture(1_000L, 2_000L);
		fixture.gain(Skill.MAGIC, 100);
		fixture.gain(Skill.SLAYER, 50);
		// The client can be ahead of the deferred StatChanged events at reset.
		fixture.experience.put(Skill.MAGIC, 10_200);
		fixture.experience.put(Skill.SLAYER, 10_100);
		fixture.plugin.resetTrackers(false, false, true, true);
		fixture.runQueued();
		fixture.event(Skill.MAGIC, 10_150);
		fixture.event(Skill.MAGIC, 10_200);
		fixture.event(Skill.SLAYER, 10_075);
		fixture.event(Skill.SLAYER, 10_100);
		fixture.assertTotals(0L, 0L);
		fixture.gain(Skill.MAGIC, 25);
		fixture.gain(Skill.SLAYER, 15);

		fixture.assertTotals(25L, 15L);
	}

	@Test
	public void firstFullSkillPacketAfterLoginIsABaselineNotEarnedXp() throws Exception
	{
		Fixture fixture = new Fixture(0L, 0L);
		fixture.experience.put(Skill.MAGIC, 0);
		fixture.experience.put(Skill.SLAYER, 0);
		fixture.state(GameState.LOGGING_IN);
		fixture.state(GameState.LOGGED_IN);

		// Login can deliver the account's complete XP after the initial zero/partial array.
		fixture.experience.put(Skill.MAGIC, 37_600_000);
		fixture.experience.put(Skill.SLAYER, 6_780_000);
		fixture.event(Skill.MAGIC, 37_600_000);
		fixture.event(Skill.SLAYER, 6_780_000);
		fixture.tick();
		fixture.tick();
		fixture.assertTotals(0L, 0L);

		fixture.gain(Skill.MAGIC, 108);
		fixture.gain(Skill.SLAYER, 43);
		fixture.assertTotals(108L, 43L);
	}

	@Test
	public void reconnectAfterMobileOfflineGainsDoesNotCountTheFullAccountSnapshot() throws Exception
	{
		Fixture fixture = new Fixture(37_600_000L, 6_780_000L);
		fixture.experience.put(Skill.MAGIC, 0);
		fixture.experience.put(Skill.SLAYER, 0);
		fixture.state(GameState.CONNECTION_LOST);
		fixture.state(GameState.LOGGING_IN);
		fixture.state(GameState.LOGGED_IN);

		// While offline, XP is earned elsewhere; reconnect sends the full account total.
		fixture.experience.put(Skill.MAGIC, 37_600_000);
		fixture.experience.put(Skill.SLAYER, 6_780_000);
		fixture.event(Skill.MAGIC, 37_600_000);
		fixture.event(Skill.SLAYER, 6_780_000);
		fixture.tick();
		fixture.tick();
		fixture.assertTotals(37_600_000L, 6_780_000L);

		fixture.gain(Skill.MAGIC, 72);
		fixture.gain(Skill.SLAYER, 51);
		fixture.assertTotals(37_600_072L, 6_780_051L);
}

	@Test
	public void partiallyLoadedLoginSkillPacketIsAlsoABaselineNotEarnedXp() throws Exception
	{
		Fixture fixture = new Fixture(0L, 0L);
		fixture.experience.put(Skill.MAGIC, 12_000);
		fixture.experience.put(Skill.SLAYER, 3_000);
		fixture.state(GameState.LOGGING_IN);
		fixture.state(GameState.LOGGED_IN);

		fixture.experience.put(Skill.MAGIC, 37_600_000);
		fixture.experience.put(Skill.SLAYER, 6_780_000);
		fixture.event(Skill.MAGIC, 37_600_000);
		fixture.event(Skill.SLAYER, 6_780_000);
		fixture.tick();
		fixture.tick();
		fixture.assertTotals(0L, 0L);
	}

	@Test
	public void worldHopReplayDoesNotCountAlreadyTrackedAccountXp() throws Exception
	{
		Fixture fixture = new Fixture(0L, 0L);
		fixture.gain(Skill.MAGIC, 100);
		fixture.state(GameState.HOPPING);
		fixture.experience.put(Skill.MAGIC, 10_100);
		fixture.state(GameState.LOGGED_IN);
		fixture.event(Skill.MAGIC, 10_100);
		fixture.tick();
		fixture.tick();
		fixture.assertTotals(100L, 0L);

		fixture.gain(Skill.MAGIC, 25);
		fixture.assertTotals(125L, 0L);
	}

	@Test
	public void startupCallbackRecoversChatTotalsFromLifetimeStats() throws Exception
	{
		Fixture fixture = new Fixture(535_000L, 458_000L);
		PvmToolsStats lifetime = new PvmToolsStats("all");
		lifetime.addCombatXp(Skill.MAGIC, 722_000L);
		lifetime.addSlayerXp(594_000L);
		lifetime.addSupplyCost(5_460_000L, 0L, PvmToolsPlugin.SupplyCostType.CANNONBALL);
		fixture.saved.put("savedSupplyCostTrackerValue", "6050000");
		fixture.saved.put("savedStatsAllTimeV1", lifetime.serialize());
		fixture.startPlugin();
		assertEquals(535_000L, fixture.chatSkillXp(Skill.MAGIC));
		assertEquals(722_000L, fixture.stats().getCombatXp(Skill.MAGIC));
		assertEquals(6_050_000L, ((Long) get(fixture.plugin, "supplyCostValue")).longValue());
		assertEquals(5_460_000L, fixture.stats().getSupplyCostValue());
		invoke(fixture.plugin, "initializeClientStateLater");
		fixture.runQueued();
		fixture.assertTotals(722_000L, 594_000L);
		assertEquals(6_050_000L, fixture.stats().getSupplyCostValue());
		fixture.gain(Skill.MAGIC, 108);
		fixture.gain(Skill.SLAYER, 43);
		fixture.assertTotals(722_108L, 594_043L);
		fixture.flush();
		assertEquals(722_108L, fixture.savedCombatXp(Skill.MAGIC));
		assertEquals("594043", fixture.saved.get("savedSlayerXpTrackerValueV2"));
		assertEquals("6050000", fixture.saved.get("savedSupplyCostTrackerValue"));
		fixture.startPlugin();
		invoke(fixture.plugin, "initializeClientStateLater");
		fixture.runQueued();
		fixture.assertTotals(722_108L, 594_043L);
		assertEquals(6_050_000L, ((Long) get(fixture.plugin, "supplyCostValue")).longValue());
		assertEquals(6_050_000L, fixture.stats().getSupplyCostValue());
	}

	@Test
	public void currentTaskIgnoresActivityFromOtherMonsters() throws Exception
	{
		Fixture fixture = new Fixture(0L, 0L);
		PvmToolsPlugin plugin = fixture.plugin;
		set(plugin, "currentSlayerTaskName", "Greater Demons");
		set(plugin, "activeCombatLootSourceTick", 100);

		// The Discord report: 15.2M from elsewhere landed on an 11-kill task.
		set(plugin, "activeCombatLootSourceName", "Zulrah");
		fixture.loot(15_200_000L, "Zulrah");
		fixture.supply(2_000L);
		fixture.gain(Skill.MAGIC, 100);
		assertEquals(0L, get(plugin, "currentSlayerTaskLootValue"));
		assertEquals(0L, get(plugin, "currentSlayerTaskSupplyCostValue"));
		assertEquals(0L, get(plugin, "currentSlayerTaskCombatXp"));

		set(plugin, "activeCombatLootSourceName", "Greater demon");
		fixture.loot(5_000L, "Greater demon");
		fixture.supply(300L);
		fixture.gain(Skill.MAGIC, 40);
		assertEquals(5_000L, get(plugin, "currentSlayerTaskLootValue"));
		assertEquals(300L, get(plugin, "currentSlayerTaskSupplyCostValue"));
		assertEquals(40L, get(plugin, "currentSlayerTaskCombatXp"));

		// Everything still counts in the period totals.
		assertEquals(15_205_000L, fixture.stats().getLootValue());
		assertEquals(2_300L, fixture.stats().getSupplyCostValue());
	}

	@Test
	public void lootPickedUpLaterStaysWithTheMonsterThatDroppedIt() throws Exception
	{
		Fixture fixture = new Fixture(0L, 0L);
		PvmToolsPlugin plugin = fixture.plugin;
		set(plugin, "currentSlayerTaskName", "Greater Demons");
		set(plugin, "activeCombatLootSourceTick", 100);
		set(plugin, "activeCombatLootSourceName", "Zulrah");

		fixture.loot(5_000L, "Greater demon");

		assertEquals(5_000L, get(plugin, "currentSlayerTaskLootValue"));
	}

	@Test
	public void completionMessageMarksAMultiKillFinishAsCompleted() throws Exception
	{
		Fixture fixture = new Fixture(0L, 0L);
		set(fixture.plugin, "currentSlayerTaskAmount", 3);
		assertEquals(false, invoke(fixture.plugin, "isCompletedSlayerTask"));

		set(fixture.plugin, "slayerTaskCompletionMessageSeen", true);
		assertEquals(true, invoke(fixture.plugin, "isCompletedSlayerTask"));
	}

	private static final class Fixture
	{
		private final Map<String, String> saved = new HashMap<>();
		private final Deque<Runnable> queued = new ArrayDeque<>();
		private final EnumMap<Skill, Integer> experience = new EnumMap<>(Skill.class);
		private final Client client = mock(Client.class);
		private final ClientThread clientThread = mock(ClientThread.class);
		private final ConfigManager configManager = mock(ConfigManager.class);
		private final PvmToolsConfig config = mock(PvmToolsConfig.class, call ->
			Modifier.isAbstract(call.getMethod().getModifiers())
				? RETURNS_DEFAULTS.answer(call) : CALLS_REAL_METHODS.answer(call));
		private PvmToolsPlugin plugin;

		private Fixture(long magicXp, long slayerXp) throws Exception
		{
			for (Skill skill : Skill.values())
			{
				experience.put(skill, 10_000);
			}
			when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
			when(client.getLocalPlayer()).thenReturn(mock(Player.class));
			when(client.getRealSkillLevel(any(Skill.class))).thenReturn(1);
			when(client.isClientThread()).thenReturn(true);
			when(client.getTickCount()).thenReturn(100);
			when(configManager.getRSProfileKey()).thenReturn("rs-main");
			when(client.getSkillExperience(any(Skill.class))).thenAnswer(call -> experience.get(call.getArgument(0)));
			doAnswer(call ->
			{
				queued.add(call.getArgument(0));
				return null;
			}).when(clientThread).invokeLater(any(Runnable.class));
			when(configManager.getConfiguration(anyString(), anyString()))
				.thenAnswer(call -> saved.get(call.getArgument(1)));
			when(configManager.getConfiguration(anyString(), org.mockito.ArgumentMatchers.eq("rs-main"), anyString()))
				.thenAnswer(call -> saved.get(call.getArgument(2)));
			doAnswer(call ->
			{
				saved.put(call.getArgument(1), call.getArgument(2));
				return null;
			}).when(configManager).setConfiguration(anyString(), anyString(), anyString());
			doAnswer(call ->
			{
				saved.put(call.getArgument(2), call.getArgument(3));
				return null;
			}).when(configManager).setConfiguration(anyString(), org.mockito.ArgumentMatchers.eq("rs-main"), anyString(), anyString());
			saved.put("savedCombatXpTrackerValuesV2", "MAGIC=" + magicXp);
			saved.put("savedSlayerXpTrackerValueV2", Long.toString(slayerXp));
			PvmToolsStats allTime = new PvmToolsStats("all");
			allTime.addCombatXp(Skill.MAGIC, magicXp);
			allTime.addSlayerXp(slayerXp);
			saved.put("savedStatsAllTimeV1", allTime.serialize());
			startPlugin();
		}

		private void startPlugin() throws Exception
		{
			queued.clear();
			plugin = new PvmToolsPlugin();
			set(plugin, "started", true);
			set(plugin, "client", client);
			set(plugin, "clientThread", clientThread);
			set(plugin, "configManager", configManager);
			set(plugin, "config", config);
			PvmAccountPersistence persistence = new PvmAccountPersistence(configManager);
			persistence.selectCurrentProfile();
			set(plugin, "accountPersistence", persistence);
			set(plugin, "itemManager", mock(ItemManager.class));
			PluginManager pluginManager = mock(PluginManager.class);
			when(pluginManager.getPlugins()).thenReturn(List.of(plugin));
			when(pluginManager.isPluginActive(plugin)).thenReturn(true);
			set(plugin, "pluginManager", pluginManager);
			saved.put("activeOwner", "PVM");
			saved.put("ownerSource", "activity");
			saved.put("ownerLeaseUntilMillis", Long.toString(Long.MAX_VALUE));
			saved.put("activeSession", (String) invoke(plugin, "getToolkitUiSessionId"));
			invoke(plugin, "loadTrackerValues");
			invoke(plugin, "loadStatsValues");
		}

		private void runQueued()
		{
			while (!queued.isEmpty())
			{
				queued.removeFirst().run();
			}
		}

		private void gain(Skill skill, int delta)
		{
			int currentXp = experience.get(skill) + delta;
			// RuneLite updates the client XP before delivering StatChanged.
			experience.put(skill, currentXp);
			event(skill, currentXp);
		}

		private void event(Skill skill, int xp)
		{
			plugin.onStatChanged(new StatChanged(skill, xp, 99, 99));
		}

		private void loot(long value, String source) throws Exception
		{
			Method method = PvmToolsPlugin.class.getDeclaredMethod("addLootStats", int.class, long.class, long.class, String.class);
			method.setAccessible(true);
			method.invoke(plugin, 995, value, value, source);
		}

		private void supply(long value) throws Exception
		{
			Method method = PvmToolsPlugin.class.getDeclaredMethod(
				"addSupplyCostStats", long.class, long.class, PvmToolsPlugin.SupplyCostType.class);
			method.setAccessible(true);
			method.invoke(plugin, value, 1L, PvmToolsPlugin.SupplyCostType.FOOD);
		}

		private void tick()
		{
			plugin.onGameTick(new GameTick());
		}

		private void state(GameState state)
		{
			when(client.getGameState()).thenReturn(state);
			GameStateChanged event = new GameStateChanged();
			event.setGameState(state);
			plugin.onGameStateChanged(event);
		}

		private PvmToolsStats stats()
		{
			return plugin.getStatsSnapshot(PvmToolsStatsPeriod.ALL_TIME);
		}

		private PvmToolsStats savedStats()
		{
			return PvmToolsStats.deserialize(saved.get("savedStatsAllTimeV1"), "all");
		}

		@SuppressWarnings("unchecked")
		private long chatSkillXp(Skill skill) throws Exception
		{
			Map<Skill, Long> combat = (Map<Skill, Long>) get(plugin, "combatXpGainedBySkill");
			return combat.getOrDefault(skill, 0L);
		}

		private long savedCombatXp(Skill skill)
		{
			for (String value : saved.get("savedCombatXpTrackerValuesV2").split(";"))
			{
				String[] entry = value.split("=", 2);
				if (entry[0].equals(skill.name()))
				{
					return Long.parseLong(entry[1]);
				}
			}
			throw new AssertionError("Missing saved XP for " + skill);
		}

		private void assertTotals(long combatXp, long slayerXp) throws Exception
		{
			assertEquals(combatXp, ((Long) invoke(plugin, "getCombatXpGained")).longValue());
			assertEquals(slayerXp, ((Long) get(plugin, "slayerXpGained")).longValue());
			assertEquals(combatXp, stats().getCombatXp());
			assertEquals(slayerXp, stats().getSlayerXp());
		}

		private void flush() throws Exception
		{
			for (String methodName : new String[]{"checkpointTrackerPersistence", "checkpointStatsPersistence"})
			{
				Method method = PvmToolsPlugin.class.getDeclaredMethod(methodName, long.class, boolean.class);
				method.setAccessible(true);
				method.invoke(plugin, System.currentTimeMillis(), true);
			}
		}
	}

	private static Object invoke(PvmToolsPlugin plugin, String name) throws Exception
	{
		Method method = PvmToolsPlugin.class.getDeclaredMethod(name);
		method.setAccessible(true);
		return method.invoke(plugin);
	}

	private static Object get(PvmToolsPlugin plugin, String name) throws Exception
	{
		Field field = PvmToolsPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(plugin);
	}

	private static void set(PvmToolsPlugin plugin, String name, Object value) throws Exception
	{
		Field field = PvmToolsPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(plugin, value);
	}
}
