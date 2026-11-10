package com.arber.pvmtools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PvmSettingsMigrationTest
{
	@Test
	public void tradeClockThatWasSwitchedOffBecomesAVanillaTradeTabOnce() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.saved.put("tradeButtonClock", "false");
		fixture.migrate("migrateTradeClockSetting");
		assertEquals("VANILLA", fixture.saved.get("tradeChatTabDisplay"));
		assertFalse(fixture.saved.containsKey("tradeButtonClock"));

		// Choosing Automatic afterwards is the user's own choice and must stick.
		fixture.saved.put("tradeChatTabDisplay", "AUTOMATIC");
		fixture.migrate("migrateTradeClockSetting");
		assertEquals("AUTOMATIC", fixture.saved.get("tradeChatTabDisplay"));
	}

	@Test
	public void tradeClockMigrationKeepsAnExplicitTabChoiceAndAnEnabledClock() throws Exception
	{
		Fixture explicit = new Fixture();
		explicit.saved.put("tradeButtonClock", "false");
		explicit.saved.put("tradeChatTabDisplay", "LOOT");
		explicit.migrate("migrateTradeClockSetting");
		assertEquals("LOOT", explicit.saved.get("tradeChatTabDisplay"));
		assertFalse(explicit.saved.containsKey("tradeButtonClock"));

		Fixture enabled = new Fixture();
		enabled.saved.put("tradeButtonClock", "true");
		enabled.migrate("migrateTradeClockSetting");
		assertFalse(enabled.saved.containsKey("tradeChatTabDisplay"));
		assertFalse(enabled.saved.containsKey("tradeButtonClock"));

		Fixture untouched = new Fixture();
		untouched.migrate("migrateTradeClockSetting");
		assertTrue(untouched.saved.isEmpty());
	}

	@Test
	public void dropTimerThatShowedEveryDropGetsAMinimumOfZeroOnce() throws Exception
	{
		for (String mode : new String[]{null, "ALL_VISIBLE"})
		{
			Fixture fixture = new Fixture();
			fixture.saved.put("groundItemLifetimeThreshold", "50000");
			if (mode != null)
			{
				fixture.saved.put("groundItemLifetimeMode", mode);
			}
			fixture.migrate("migrateDropTimerMinimum");
			assertEquals("0", fixture.saved.get("groundItemLifetimeThreshold"));
			assertFalse(fixture.saved.containsKey("groundItemLifetimeMode"));

			// A minimum chosen after the migration must not be reset by the next start.
			fixture.saved.put("groundItemLifetimeThreshold", "20000");
			fixture.migrate("migrateDropTimerMinimum");
			assertEquals("20000", fixture.saved.get("groundItemLifetimeThreshold"));
		}
	}

	@Test
	public void dropTimerValueThresholdKeepsItsMinimumIncludingTheOldDefault() throws Exception
	{
		Fixture oldDefault = new Fixture();
		oldDefault.saved.put("groundItemLifetimeMode", "VALUE_THRESHOLD");
		oldDefault.migrate("migrateDropTimerMinimum");
		assertEquals("10000", oldDefault.saved.get("groundItemLifetimeThreshold"));
		assertFalse(oldDefault.saved.containsKey("groundItemLifetimeMode"));

		Fixture chosen = new Fixture();
		chosen.saved.put("groundItemLifetimeMode", "VALUE_THRESHOLD");
		chosen.saved.put("groundItemLifetimeThreshold", "25000");
		chosen.migrate("migrateDropTimerMinimum");
		assertEquals("25000", chosen.saved.get("groundItemLifetimeThreshold"));
		assertFalse(chosen.saved.containsKey("groundItemLifetimeMode"));
	}

	private static final class Fixture
	{
		private final PvmToolsPlugin plugin = new PvmToolsPlugin();
		private final Map<String, String> saved = new HashMap<>();

		private Fixture() throws Exception
		{
			ConfigManager manager = mock(ConfigManager.class);
			when(manager.getConfiguration(eq(PvmToolsConfig.GROUP), anyString()))
				.thenAnswer(call -> saved.get(call.<String>getArgument(1)));
			doAnswer(call ->
			{
				Object value = call.getArgument(2);
				saved.put(call.getArgument(1), value instanceof Enum ? ((Enum<?>) value).name() : String.valueOf(value));
				return null;
			}).when(manager).setConfiguration(eq(PvmToolsConfig.GROUP), anyString(), any(Object.class));
			doAnswer(call ->
			{
				saved.remove(call.<String>getArgument(1));
				return null;
			}).when(manager).unsetConfiguration(eq(PvmToolsConfig.GROUP), anyString());
			PvmToolsConfig config = mock(PvmToolsConfig.class);
			when(config.tradeChatTabDisplay()).thenAnswer(call ->
				ChatTabDisplay.valueOf(saved.getOrDefault("tradeChatTabDisplay", "AUTOMATIC")));
			set("configManager", manager);
			set("config", config);
		}

		private void migrate(String name) throws Exception
		{
			Method method = PvmToolsPlugin.class.getDeclaredMethod(name);
			method.setAccessible(true);
			method.invoke(plugin);
		}

		private void set(String name, Object value) throws Exception
		{
			Field field = PvmToolsPlugin.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(plugin, value);
		}
	}
}
