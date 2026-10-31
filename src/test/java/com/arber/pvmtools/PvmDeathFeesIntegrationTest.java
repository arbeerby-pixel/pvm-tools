package com.arber.pvmtools;

import com.arber.pvmtools.PvmToolsPlugin.SupplyCostType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PvmDeathFeesIntegrationTest
{
	@Test
	public void onlyCompletedSystemPaymentMessageReachesPersistedFeeTotals() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.select("rs-main");
		fixture.chat(ChatMessageType.PUBLICCHAT, "Death charges you 500,000 x Coins.");
		fixture.chat(ChatMessageType.GAMEMESSAGE, "Death will charge you 500,000 x Coins.");
		fixture.assertFees(0L, 0L);
		fixture.chat(ChatMessageType.GAMEMESSAGE, "<col=ff0000>Death charges you 500,000 x Coins.</col>");
		fixture.assertFees(500_000L, 1L);
		assertEquals(500_000L, fixture.saved(PvmToolsStatsPeriod.ALL_TIME).getDeathFeesValue());
	}

	@Test
	public void paymentImmediatelyPersistsEveryPeriodAndReducesProfitSeparately() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.select("rs-main");
		fixture.seedOtherActivity();
		fixture.pay(150_000L);
		fixture.pay(50_000L);

		fixture.assertFees(200_000L, 2L);
		assertEquals(700_000L, invoke(fixture.plugin, "getChatNetProfit"));
		for (PvmToolsStatsPeriod period : PvmToolsStatsPeriod.values())
		{
			PvmToolsStats live = fixture.plugin.getStatsSnapshot(period);
			PvmToolsStats saved = fixture.saved(period);
			assertEquals(200_000L, live.getDeathFeesValue());
			assertEquals(2L, live.getDeathFeeCount());
			assertEquals(200_000L, saved.getDeathFeesValue());
			assertEquals(2L, saved.getDeathFeeCount());
			assertEquals(1_000_000L, saved.getLootValue());
			assertEquals(100_000L, saved.getSupplyCostValue());
			assertEquals(200L, saved.getCombatXp());
			assertEquals(50L, saved.getSlayerXp());
			assertEquals(700_000L, saved.getNetProfit());
		}
	}

	@Test
	public void mainAndGimRoundTripTheirOwnDeathFeeTotals() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.select("rs-main");
		fixture.pay(500_000L);
		fixture.select("rs-gim");
		fixture.assertFees(0L, 0L);
		fixture.pay(100_000L);
		fixture.select("rs-main");
		fixture.assertFees(500_000L, 1L);
		fixture.select("rs-gim");
		fixture.assertFees(100_000L, 1L);
		assertEquals(500_000L, PvmToolsStats.deserialize(
			fixture.store.accountData("rs-main").get("savedStatsAllTimeV1"), "all").getDeathFeesValue());
		assertEquals(100_000L, PvmToolsStats.deserialize(
			fixture.store.accountData("rs-gim").get("savedStatsAllTimeV1"), "all").getDeathFeesValue());
	}

	@Test
	public void feeResetAndExistingFourCategoryResetDoNotEraseEachOther() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.select("rs-main");
		fixture.seedOtherActivity();
		fixture.pay(50_000L);
		fixture.plugin.resetTrackers(false, false, false, false, true);
		fixture.runQueued();
		fixture.assertFees(0L, 0L);
		PvmToolsStats stats = fixture.plugin.getStatsSnapshot(PvmToolsStatsPeriod.ALL_TIME);
		assertEquals(1_000_000L, stats.getLootValue());
		assertEquals(100_000L, stats.getSupplyCostValue());
		assertEquals(200L, stats.getCombatXp());
		assertEquals(50L, stats.getSlayerXp());

		fixture.pay(75_000L);
		fixture.plugin.resetTrackers(true, true, true, true);
		fixture.runQueued();
		fixture.assertFees(75_000L, 1L);
		for (PvmToolsStatsPeriod period : PvmToolsStatsPeriod.values())
		{
			PvmToolsStats saved = fixture.saved(period);
			assertEquals(75_000L, saved.getDeathFeesValue());
			assertEquals(1L, saved.getDeathFeeCount());
			assertEquals(0L, saved.getLootValue());
			assertEquals(0L, saved.getSupplyCostValue());
			assertEquals(0L, saved.getCombatXp());
			assertEquals(0L, saved.getSlayerXp());
		}
	}

	@Test
	public void sessionCounterRestartsWhileSavedPeriodsKeepTheirFees() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.select("rs-main");
		fixture.pay(100_000L);
		fixture.mode = TrackerPersistenceMode.SESSION;
		invoke(fixture.plugin, "loadTrackerValues");
		assertEquals(0L, get(fixture.plugin, "deathFeesValue"));
		assertEquals(100_000L, fixture.saved(PvmToolsStatsPeriod.ALL_TIME).getDeathFeesValue());
		fixture.pay(50_000L);
		assertEquals(50_000L, get(fixture.plugin, "deathFeesValue"));
		assertEquals(150_000L, fixture.saved(PvmToolsStatsPeriod.ALL_TIME).getDeathFeesValue());
		assertEquals(2L, fixture.saved(PvmToolsStatsPeriod.ALL_TIME).getDeathFeeCount());
		fixture.mode = TrackerPersistenceMode.FOREVER;
		invoke(fixture.plugin, "loadTrackerValues");
		fixture.assertFees(150_000L, 2L);
	}

	@Test
	public void queuedFeeResetForMainCannotEraseTheNewlySelectedGimAccount() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.select("rs-gim");
		fixture.pay(100_000L);
		fixture.select("rs-main");
		fixture.pay(500_000L);
		fixture.plugin.resetTrackers(false, false, false, false, true);
		fixture.store.account = "rs-gim";
		invoke(fixture.plugin, "synchronizeAccountPersistence", false);
		fixture.runQueued();
		fixture.assertFees(100_000L, 1L);
		fixture.select("rs-main");
		fixture.assertFees(500_000L, 1L);
	}

	@Test
	public void accountSwitchFlushesConfirmedBankPaymentIntoCapturedOldAccountOnly() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.select("rs-main");
		fixture.pendingBankPayment(50_000L, true);
		fixture.assertFees(0L, 0L);
		fixture.select("rs-gim");
		fixture.assertFees(0L, 0L);
		PvmToolsStats mainSaved = PvmToolsStats.deserialize(
			fixture.store.accountData("rs-main").get("savedStatsAllTimeV1"), "all");
		assertEquals(50_000L, mainSaved.getDeathFeesValue());
		assertEquals(1L, mainSaved.getDeathFeeCount());

		// A generic bank message without a reclaim UI or success confirmation is not a fee.
		fixture.pendingBankPayment(25_000L, false);
		fixture.select("rs-main");
		fixture.assertFees(50_000L, 1L);
		fixture.select("rs-gim");
		fixture.assertFees(0L, 0L);
	}

	@Test
	public void logoutFlushesConfirmedBankPaymentBeforeClearingItsPendingState() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.select("rs-main");
		fixture.pendingBankPayment(50_000L, true);
		fixture.assertFees(0L, 0L);
		fixture.state(GameState.LOGGING_IN);
		fixture.assertFees(50_000L, 1L);
		assertEquals(50_000L, fixture.saved(PvmToolsStatsPeriod.ALL_TIME).getDeathFeesValue());
		assertEquals(1L, fixture.saved(PvmToolsStatsPeriod.ALL_TIME).getDeathFeeCount());
		((PvmDeathFeeTracker) get(fixture.plugin, "deathFeeTracker")).onGameTick(102);
		fixture.assertFees(50_000L, 1L);
	}

	private static final class Fixture
	{
		private final PvmAccountPersistenceTest.Store store = new PvmAccountPersistenceTest.Store();
		private final PvmToolsPlugin plugin = new PvmToolsPlugin();
		private final Client client = mock(Client.class);
		private final Deque<Runnable> queued = new ArrayDeque<>();
		private TrackerPersistenceMode mode = TrackerPersistenceMode.FOREVER;

		private Fixture() throws Exception
		{
			when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
			when(client.isClientThread()).thenReturn(true);
			when(client.getLocalPlayer()).thenReturn(mock(Player.class));
			when(client.getRealSkillLevel(any(Skill.class))).thenReturn(1);
			when(client.getSkillExperience(any(Skill.class))).thenReturn(10_000);
			when(client.getTickCount()).thenReturn(100);
			PvmToolsConfig config = mock(PvmToolsConfig.class, call ->
				Modifier.isAbstract(call.getMethod().getModifiers())
					? RETURNS_DEFAULTS.answer(call) : CALLS_REAL_METHODS.answer(call));
			doAnswer(call -> mode).when(config).trackerMode();
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

		private void select(String profile)
		{
			String previous = store.account;
			store.account = profile;
			plugin.onRuneScapeProfileChanged(new RuneScapeProfileChanged(previous, profile));
			runQueued();
		}

		private void seedOtherActivity() throws Exception
		{
			set(plugin, "npcLootValue", 1_000_000L);
			invoke(plugin, "addLootStats", 995, 1_000_000L, 1_000_000L, null);
			invoke(plugin, "persistLootTrackerValue");
			invoke(plugin, "addSupplyCost", 100_000L, SupplyCostType.FOOD, 1L);
			invoke(plugin, "addCombatXpStats", Skill.MAGIC, 200L);
			invoke(plugin, "addSlayerXpStats", 50L);
		}

		private void pay(long amount) throws Exception
		{
			invoke(plugin, "recordDeathFee", amount);
		}

		private void pendingBankPayment(long amount, boolean reclaimInterfaceOpen) throws Exception
		{
			((PvmDeathFeeTracker) get(plugin, "deathFeeTracker")).onChatMessage(
				ChatMessageType.GAMEMESSAGE, "Payment has been taken from your bank: "
					+ String.format(java.util.Locale.ENGLISH, "%,d", amount) + " x Coins",
				100, reclaimInterfaceOpen);
		}

		private void state(GameState state)
		{
			when(client.getGameState()).thenReturn(state);
			GameStateChanged event = new GameStateChanged();
			event.setGameState(state);
			plugin.onGameStateChanged(event);
		}

		private void chat(ChatMessageType type, String text)
		{
			ChatMessage event = new ChatMessage();
			event.setType(type);
			event.setMessage(text);
			plugin.onChatMessage(event);
		}

		private PvmToolsStats saved(PvmToolsStatsPeriod period)
		{
			String key = "savedStats" + (period == PvmToolsStatsPeriod.ALL_TIME ? "AllTime"
				: period.name().charAt(0) + period.name().substring(1).toLowerCase(java.util.Locale.ENGLISH)) + "V1";
			return PvmToolsStats.deserialize(store.accountData(store.account).get(key),
				plugin.getStatsSnapshot(period).getPeriodId());
		}

		private void assertFees(long amount, long count) throws Exception
		{
			assertEquals(amount, get(plugin, "deathFeesValue"));
			PvmToolsStats stats = plugin.getStatsSnapshot(PvmToolsStatsPeriod.ALL_TIME);
			assertEquals(amount, stats.getDeathFeesValue());
			assertEquals(count, stats.getDeathFeeCount());
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
