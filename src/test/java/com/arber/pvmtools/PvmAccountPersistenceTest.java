package com.arber.pvmtools;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigProfile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PvmAccountPersistenceTest
{
	@Test
	public void sharedHistoryIsBackedUpOnceWithoutGuessingWhichAccountOwnsIt()
	{
		Store store = new Store();
		Map<String, String> originals = new HashMap<>();
		for (String key : PvmAccountPersistence.DATA_KEYS)
		{
			String value = key + "\nshared=history \u00e5";
			store.settings().put(key, value);
			originals.put(key, value);
		}
		PvmAccountPersistence persistence = new PvmAccountPersistence(store.config);
		store.account = "rs-main";
		persistence.selectCurrentProfile();
		String backup = store.settings().get(PvmAccountPersistence.LEGACY_BACKUP_KEY);
		for (String key : PvmAccountPersistence.DATA_KEYS)
		{
			assertNull(persistence.read(key));
			assertEquals(originals.get(key), store.settings().get(key));
			assertTrue(backup.contains("\n" + key + "=" + Base64.getEncoder().encodeToString(
				originals.get(key).getBytes(StandardCharsets.UTF_8))));
		}

		store.settings().put("savedLootTrackerValue", "changed later");
		store.account = "rs-gim";
		persistence.selectCurrentProfile();
		assertEquals(backup, store.settings().get(PvmAccountPersistence.LEGACY_BACKUP_KEY));
		for (String key : PvmAccountPersistence.DATA_KEYS)
		{
			assertNull(persistence.read(key));
		}
	}

	@Test
	public void everySavedCategoryAndExclusionRoundTripsIndependentlyForMainAndGim()
	{
		Store store = new Store();
		PvmAccountPersistence persistence = new PvmAccountPersistence(store.config);
		store.account = "rs-main";
		persistence.selectCurrentProfile();
		for (String key : PvmAccountPersistence.DATA_KEYS)
		{
			persistence.write(key, "main:" + key);
		}
		store.account = "rs-gim";
		assertTrue(persistence.needsReload());
		persistence.selectCurrentProfile();
		for (String key : PvmAccountPersistence.DATA_KEYS)
		{
			assertNull(persistence.read(key));
			persistence.write(key, "gim:" + key);
		}
		store.account = "rs-main";
		persistence.selectCurrentProfile();
		for (String key : PvmAccountPersistence.DATA_KEYS)
		{
			assertEquals("main:" + key, persistence.read(key));
			assertEquals("gim:" + key, store.accountData("rs-gim").get(key));
		}
	}

	@Test
	public void flushUsesCapturedAccountEvenAfterRuneLiteSelectsTheNextAccount()
	{
		Store store = new Store();
		store.account = "rs-main";
		PvmAccountPersistence persistence = new PvmAccountPersistence(store.config);
		persistence.selectCurrentProfile();
		store.account = "rs-gim";
		persistence.write("savedLootTrackerValue", "123");
		assertEquals("123", store.accountData("rs-main").get("savedLootTrackerValue"));
		assertNull(store.accountData("rs-gim").get("savedLootTrackerValue"));
	}

	@Test
	public void unknownAccountCannotReadOrWriteSharedActivity()
	{
		Store store = new Store();
		store.settings().put("savedLootTrackerValue", "999");
		PvmAccountPersistence persistence = new PvmAccountPersistence(store.config);
		persistence.selectCurrentProfile();
		assertFalse(persistence.hasProfile());
		assertNull(persistence.read("savedLootTrackerValue"));
		persistence.write("savedLootTrackerValue", "0");
		assertEquals("999", store.settings().get("savedLootTrackerValue"));
		assertTrue(store.accounts.isEmpty());
	}

	@Test
	public void changingNormalConfigProfileReloadsPreferencesWithoutChangingAccountData()
	{
		Store store = new Store();
		store.account = "rs-main";
		PvmAccountPersistence persistence = new PvmAccountPersistence(store.config);
		persistence.selectCurrentProfile();
		persistence.write("savedLootTrackerValue", "123");
		store.configProfile = 2L;
		store.settings().put("savedLootTrackerValue", "unrelated legacy profile");
		assertTrue(persistence.needsReload());
		persistence.selectCurrentProfile();
		assertFalse(persistence.needsReload());
		assertEquals("123", persistence.read("savedLootTrackerValue"));
		assertTrue(store.settings().containsKey(PvmAccountPersistence.LEGACY_BACKUP_KEY));
	}

	static final class Store
	{
		final ConfigManager config = mock(ConfigManager.class);
		final Map<String, Map<String, String>> accounts = new HashMap<>();
		final Map<Long, Map<String, String>> normalProfiles = new HashMap<>();
		String account;
		long configProfile = 1L;

		Store()
		{
			when(config.getRSProfileKey()).thenAnswer(call -> account);
			when(config.getProfile()).thenAnswer(call ->
			{
				ConfigProfile profile = mock(ConfigProfile.class);
				when(profile.getId()).thenReturn(configProfile);
				return profile;
			});
			when(config.getConfiguration(anyString(), anyString())).thenAnswer(call ->
				settings().get(call.getArgument(1)));
			when(config.getConfiguration(anyString(), anyString(), anyString())).thenAnswer(call ->
				accountData(call.getArgument(1)).get(call.getArgument(2)));
			doAnswer(call ->
			{
				settings().put(call.getArgument(1), call.getArgument(2));
				return null;
			}).when(config).setConfiguration(anyString(), anyString(), anyString());
			doAnswer(call ->
			{
				accountData(call.getArgument(1)).put(call.getArgument(2), call.getArgument(3));
				return null;
			}).when(config).setConfiguration(anyString(), anyString(), anyString(), anyString());
		}

		Map<String, String> settings()
		{
			return normalProfiles.computeIfAbsent(configProfile, ignored -> new HashMap<>());
		}

		Map<String, String> accountData(String profile)
		{
			return accounts.computeIfAbsent(profile, ignored -> new HashMap<>());
		}
	}
}
