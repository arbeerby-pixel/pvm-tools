package com.arber.pvmtools;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class PvmSupplyIgnoreList
{
	private static final Pattern SEPARATOR = Pattern.compile("[,;\\r\\n]+");
	private static final Pattern DOSE_SUFFIX = Pattern.compile("\\s*\\([1-4]\\)\\s*$");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final PvmSupplyIgnoreList EMPTY = new PvmSupplyIgnoreList(Collections.emptySet());

	private final Set<String> itemNames;

	private PvmSupplyIgnoreList(Set<String> itemNames)
	{
		this.itemNames = itemNames;
	}

	static PvmSupplyIgnoreList empty()
	{
		return EMPTY;
	}

	static PvmSupplyIgnoreList fromConfig(String value)
	{
		if (value == null || value.isBlank())
		{
			return empty();
		}

		Set<String> names = new HashSet<>();
		for (String entry : SEPARATOR.split(value))
		{
			String normalized = normalize(entry);
			if (!normalized.isEmpty())
			{
				names.add(normalized);
			}
		}
		return names.isEmpty()
			? empty()
			: new PvmSupplyIgnoreList(Collections.unmodifiableSet(names));
	}

	boolean isEmpty()
	{
		return itemNames.isEmpty();
	}

	boolean matches(String itemName)
	{
		return itemNames.contains(normalize(itemName));
	}

	static String normalize(String itemName)
	{
		if (itemName == null)
		{
			return "";
		}

		String withoutDose = DOSE_SUFFIX.matcher(itemName).replaceFirst("");
		return WHITESPACE.matcher(withoutDose.trim())
			.replaceAll(" ")
			.toLowerCase(Locale.ENGLISH);
	}
}
