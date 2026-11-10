package com.arber.pvmtools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.ItemComposition;
import net.runelite.api.TileItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

final class GroundItemVisibilityFilter
{
	private static final String GROUND_ITEMS_GROUP = "grounditems";
	private static final String HIDDEN_ITEMS_KEY = "hiddenItems";
	private static final String HIGHLIGHTED_ITEMS_KEY = "highlightedItems";
	private static final String SHOW_HIGHLIGHTED_ONLY_KEY = "showHighlightedOnly";
	private static final String HIDE_UNDER_VALUE_KEY = "hideUnderValue";
	private static final String DONT_HIDE_UNTRADEABLES_KEY = "dontHideUntradeables";
	private static final String DEFAULT_HIDDEN_ITEMS = "Vial, Ashes, Coins, Bones, Bucket, Jug, Seaweed";
	private static final int MATCH_NONE = 0;
	private static final int MATCH_WILDCARD = 1;
	private static final int MATCH_EXACT = 2;

	private final ItemManager itemManager;
	private final List<ItemThreshold> hiddenItems;
	private final List<ItemThreshold> highlightedItems;
	private final boolean showHighlightedOnly;
	private final int hideUnderValue;
	private final boolean dontHideUntradeables;
	// Overlays ask about the same stacks every frame for as long as this instance lives.
	private final Map<Long, Boolean> visibilityByItemAndQuantity = new HashMap<>();

	static GroundItemVisibilityFilter load(ConfigManager configManager, ItemManager itemManager)
	{
		return new GroundItemVisibilityFilter(
			itemManager,
			getCsvConfig(configManager, HIDDEN_ITEMS_KEY, DEFAULT_HIDDEN_ITEMS),
			getCsvConfig(configManager, HIGHLIGHTED_ITEMS_KEY, ""),
			getBooleanConfig(configManager, SHOW_HIGHLIGHTED_ONLY_KEY, false),
			getIntConfig(configManager, HIDE_UNDER_VALUE_KEY, 0),
			getBooleanConfig(configManager, DONT_HIDE_UNTRADEABLES_KEY, true));
	}

	private GroundItemVisibilityFilter(
		ItemManager itemManager,
		List<String> hiddenItems,
		List<String> highlightedItems,
		boolean showHighlightedOnly,
		int hideUnderValue,
		boolean dontHideUntradeables)
	{
		this.itemManager = itemManager;
		this.hiddenItems = parseThresholds(hiddenItems);
		this.highlightedItems = parseThresholds(highlightedItems);
		this.showHighlightedOnly = showHighlightedOnly;
		this.hideUnderValue = hideUnderValue;
		this.dontHideUntradeables = dontHideUntradeables;
	}

	boolean isVisible(TileItem item)
	{
		int itemId = item.getId();
		int quantity = item.getQuantity();
		long key = (long) itemId << 32 | quantity & 0xFFFFFFFFL;
		Boolean visible = visibilityByItemAndQuantity.get(key);
		if (visible != null)
		{
			return visible;
		}

		ItemComposition composition;
		try
		{
			composition = itemManager.getItemComposition(itemId);
		}
		catch (RuntimeException ignored)
		{
			// Keep RuneLite's own visible loot visible while item definitions are reloading.
			return true;
		}
		if (composition == null)
		{
			return true;
		}

		visible = isVisible(itemId, quantity, composition);
		visibilityByItemAndQuantity.put(key, visible);
		return visible;
	}

	private boolean isVisible(int itemId, int quantity, ItemComposition composition)
	{
		String name = composition.getName();
		int highlighted = matches(highlightedItems, name, quantity);
		int hidden = matches(hiddenItems, name, quantity);
		boolean isHighlighted = highlighted == MATCH_EXACT
			|| highlighted == MATCH_WILDCARD && hidden != MATCH_EXACT;
		boolean isHidden = hidden == MATCH_EXACT && highlighted != MATCH_EXACT
			|| hidden == MATCH_WILDCARD && highlighted == MATCH_NONE;

		if (isHidden || showHighlightedOnly && !isHighlighted)
		{
			return false;
		}

		return isHighlighted || !isHiddenByValue(itemId, composition);
	}

	private boolean isHiddenByValue(int itemId, ItemComposition composition)
	{
		if (hideUnderValue <= 0)
		{
			return false;
		}

		int linkedItemId = composition.getNote() != -1 ? composition.getLinkedNoteId() : itemId;
		long gePrice = linkedItemId == 995 ? 1L : itemManager.getItemPrice(linkedItemId);
		int haPrice = composition.getHaPrice();
		boolean canHideByValue = gePrice > 0 || composition.isGeTradeable() || !dontHideUntradeables;
		return canHideByValue && gePrice < hideUnderValue && haPrice < hideUnderValue;
	}

	private static List<String> getCsvConfig(ConfigManager configManager, String key, String defaultValue)
	{
		String value = configManager.getConfiguration(GROUND_ITEMS_GROUP, key);
		if (value == null)
		{
			value = defaultValue;
		}

		return value == null || value.trim().isEmpty() ? Collections.emptyList() : Text.fromCSV(value);
	}

	private static boolean getBooleanConfig(ConfigManager configManager, String key, boolean defaultValue)
	{
		String value = configManager.getConfiguration(GROUND_ITEMS_GROUP, key);
		return value == null ? defaultValue : Boolean.parseBoolean(value);
	}

	private static int getIntConfig(ConfigManager configManager, String key, int defaultValue)
	{
		String value = configManager.getConfiguration(GROUND_ITEMS_GROUP, key);
		if (value == null)
		{
			return defaultValue;
		}

		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException ignored)
		{
			return defaultValue;
		}
	}

	private static List<ItemThreshold> parseThresholds(List<String> configuredItems)
	{
		List<ItemThreshold> thresholds = new ArrayList<>(configuredItems.size());
		for (String configuredItem : configuredItems)
		{
			ItemThreshold threshold = ItemThreshold.fromName(configuredItem);
			if (threshold != null)
			{
				thresholds.add(threshold);
			}
		}
		return thresholds;
	}

	private static int matches(List<ItemThreshold> configuredItems, String itemName, int quantity)
	{
		int wildcardMatch = MATCH_NONE;
		for (ItemThreshold threshold : configuredItems)
		{
			if (!threshold.quantityHolds(quantity))
			{
				continue;
			}

			if (threshold.wildcard)
			{
				if (WildcardMatcher.matches(threshold.name, itemName))
				{
					wildcardMatch = MATCH_WILDCARD;
				}
				continue;
			}

			if (threshold.name.equalsIgnoreCase(itemName))
			{
				return MATCH_EXACT;
			}
		}

		return wildcardMatch;
	}

	private static final class ItemThreshold
	{
		private final String name;
		private final int quantity;
		private final boolean lessThan;
		private final boolean wildcard;

		private ItemThreshold(String name, int quantity, boolean lessThan, boolean wildcard)
		{
			this.name = name;
			this.quantity = quantity;
			this.lessThan = lessThan;
			this.wildcard = wildcard;
		}

		private static ItemThreshold fromName(String name)
		{
			if (name == null || name.trim().isEmpty())
			{
				return null;
			}

			boolean wildcard = name.contains("*");
			boolean lessThan = false;
			int quantity = 0;
			int split = findQuantitySplit(name);
			if (split >= 0)
			{
				lessThan = name.charAt(split) == '<';
				try
				{
					quantity = Integer.parseInt(name.substring(split + 1).trim());
					name = name.substring(0, split);
				}
				catch (NumberFormatException ignored)
				{
					quantity = 0;
					lessThan = false;
				}
			}

			return new ItemThreshold(name.trim(), quantity, lessThan, wildcard);
		}

		private static int findQuantitySplit(String name)
		{
			for (int i = name.length() - 1; i >= 0; i--)
			{
				char c = name.charAt(i);
				if (Character.isDigit(c) || Character.isWhitespace(c))
				{
					continue;
				}

				return c == '<' || c == '>' ? i : -1;
			}

			return -1;
		}

		private boolean quantityHolds(int itemQuantity)
		{
			return lessThan ? itemQuantity < quantity : itemQuantity > quantity;
		}
	}
}
