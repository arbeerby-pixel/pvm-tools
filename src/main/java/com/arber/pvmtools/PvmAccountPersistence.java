package com.arber.pvmtools;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigProfile;

/** Keeps saved activity tied to a captured RuneScape save profile, including during a switch. */
final class PvmAccountPersistence
{
	static final String LEGACY_BACKUP_KEY = "legacySharedStatsBackupV1";
	static final List<String> DATA_KEYS = List.of(
		"savedLootTrackerValue",
		"savedSupplyCostTrackerValue",
		"savedSupplyCostTrackerBreakdown",
		"savedCombatXpTrackerValuesV2",
		"savedSlayerXpTrackerValueV2",
		"savedStatsDayV1",
		"savedStatsWeekV1",
		"savedStatsMonthV1",
		"savedStatsYearV1",
		"savedStatsAllTimeV1",
		"savedCurrentSlayerTaskV1",
		"savedSlayerTaskHistoryV1",
		"combatLootExclusionsV1");

	private final ConfigManager configManager;
	private String profileKey;
	private Long configProfileId;
	private boolean selected;

	PvmAccountPersistence(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	boolean needsReload()
	{
		return !selected || !Objects.equals(profileKey, currentProfileKey())
			|| !Objects.equals(configProfileId, currentConfigProfileId());
	}

	void selectCurrentProfile()
	{
		preserveLegacySharedHistory();
		profileKey = currentProfileKey();
		configProfileId = currentConfigProfileId();
		selected = true;
	}

	boolean hasProfile()
	{
		return profileKey != null;
	}

	String read(String key)
	{
		// A null profile passed to ConfigManager is the shared config profile.
		// Never use that fallback for account activity, including before the first login.
		return profileKey == null ? null
			: configManager.getConfiguration(PvmToolsConfig.GROUP, profileKey, key);
	}

	void write(String key, String value)
	{
		if (profileKey != null)
		{
			configManager.setConfiguration(PvmToolsConfig.GROUP, profileKey, key, value);
		}
	}

	private String currentProfileKey()
	{
		String current = configManager.getRSProfileKey();
		return current == null || current.isBlank() ? null : current;
	}

	private Long currentConfigProfileId()
	{
		ConfigProfile profile = configManager.getProfile();
		return profile == null ? null : profile.getId();
	}

	private void preserveLegacySharedHistory()
	{
		if (configManager.getConfiguration(PvmToolsConfig.GROUP, LEGACY_BACKUP_KEY) != null)
		{
			return;
		}

		StringBuilder backup = new StringBuilder("1");
		for (String key : DATA_KEYS)
		{
			String value = configManager.getConfiguration(PvmToolsConfig.GROUP, key);
			if (value != null)
			{
				backup.append('\n').append(key).append('=')
					.append(Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));
			}
		}
		// Old totals can already contain more than one account. Retain them intact,
		// without guessing which account owns them or copying them into every login.
		configManager.setConfiguration(PvmToolsConfig.GROUP, LEGACY_BACKUP_KEY, backup.toString());
	}

	static boolean isPersistenceKey(String key)
	{
		return DATA_KEYS.contains(key) || LEGACY_BACKUP_KEY.equals(key);
	}
}
