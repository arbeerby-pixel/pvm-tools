package com.arber.pvmtools;

import com.google.common.collect.Table;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemID;
import net.runelite.api.ItemLayer;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.grounditems.GroundItemsPlugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.QuantityFormatter;

class GroundItemLifetimeTextOverlay extends Overlay
{
	private static final String GROUND_ITEMS_GROUP = "grounditems";
	private static final int MAX_DISTANCE = 2500;
	private static final int OFFSET_Z = 20;
	private static final int STRING_GAP = 15;

	private final Client client;
	private final PvmToolsPlugin plugin;
	private final ConfigManager configManager;
	private final ItemManager itemManager;
	private final PluginManager pluginManager;
	private final TextComponent textComponent = new TextComponent();
	private final Map<WorldPoint, Integer> offsetMap = new HashMap<>();
	private final Map<ItemKey, ItemLifetime> itemLifetimes = new HashMap<>();
	private GroundItemsPlugin groundItemsPlugin;

	@Inject
	private GroundItemLifetimeTextOverlay(
		Client client,
		PvmToolsPlugin plugin,
		ConfigManager configManager,
		ItemManager itemManager,
		PluginManager pluginManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.configManager = configManager;
		this.itemManager = itemManager;
		this.pluginManager = pluginManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_HIGHEST);
	}

	void onItemSpawned(Tile tile, TileItem item)
	{
		if (tile == null || item == null)
		{
			return;
		}

		int tick = client.getTickCount();
		int despawnTick = item.getDespawnTime();
		if (despawnTick <= tick)
		{
			return;
		}

		ItemKey key = new ItemKey(tile.getWorldLocation(), item.getId());
		itemLifetimes.computeIfAbsent(key, ignored -> new ItemLifetime(tick, despawnTick));
	}

	void onItemDespawned(Tile tile, TileItem item)
	{
		if (tile != null && item != null)
		{
			itemLifetimes.remove(new ItemKey(tile.getWorldLocation(), item.getId()));
		}
	}

	void reset()
	{
		itemLifetimes.clear();
		offsetMap.clear();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.shouldShowGroundItemLifetimeText() || !isGroundItemTextVisible())
		{
			return null;
		}

		Player player = client.getLocalPlayer();
		Scene scene = client.getScene();
		if (player == null || scene == null || scene.getTiles() == null)
		{
			return null;
		}

		Tile[][][] tiles = scene.getTiles();
		int plane = client.getPlane();
		if (plane < 0 || plane >= tiles.length || tiles[plane] == null)
		{
			return null;
		}

		GroundItemVisibilityFilter visibilityFilter = GroundItemVisibilityFilter.load(configManager, itemManager);
		Table groundItemsTable = getGroundItemsTable();
		offsetMap.clear();
		for (Tile[] row : tiles[plane])
		{
			if (row == null)
			{
				continue;
			}

			for (Tile tile : row)
			{
				renderTile(graphics, tile, player, visibilityFilter, groundItemsTable);
				if (tile != null)
				{
					renderTile(graphics, tile.getBridge(), player, visibilityFilter, groundItemsTable);
				}
			}
		}

		int tick = client.getTickCount();
		itemLifetimes.entrySet().removeIf(entry -> entry.getValue().despawnTick <= tick);
		return null;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void renderTile(
		Graphics2D graphics,
		Tile tile,
		Player player,
		GroundItemVisibilityFilter visibilityFilter,
		Table groundItemsTable)
	{
		if (tile == null || tile.getGroundItems() == null || tile.getGroundItems().isEmpty())
		{
			return;
		}

		ItemLayer itemLayer = tile.getItemLayer();
		if (itemLayer == null)
		{
			return;
		}

		LocalPoint groundPoint = LocalPoint.fromWorld(itemLayer.getWorldView(), itemLayer.getWorldLocation());
		if (groundPoint == null || groundPoint.getWorldView() == net.runelite.api.WorldView.TOPLEVEL
			&& player.getLocalLocation().distanceTo(groundPoint) > MAX_DISTANCE)
		{
			return;
		}

		Map<Integer, TileItem> itemsById = new LinkedHashMap<>();
		for (TileItem item : tile.getGroundItems())
		{
			itemsById.put(item.getId(), item);
		}

		List<TileItem> orderedItems = new ArrayList<>(itemsById.size());
		if (groundItemsTable != null)
		{
			Map row = groundItemsTable.row(tile.getWorldLocation());
			for (Object itemId : row.keySet())
			{
				TileItem item = itemsById.remove(itemId);
				if (item != null)
				{
					orderedItems.add(item);
				}
			}
		}
		orderedItems.addAll(itemsById.values());

		for (TileItem item : orderedItems)
		{
			if (!visibilityFilter.isVisible(item)
				|| !shouldDisplayOwnership(item)
				|| !plugin.shouldShowGroundItemLifetime(item.getId(), item.getQuantity()))
			{
				continue;
			}

			ItemLifetime lifetime = getOrCreateLifetime(tile, item);
			if (lifetime == null)
			{
				continue;
			}

			String text = buildItemText(item);
			Point textPoint = Perspective.getCanvasTextLocation(
				client,
				graphics,
				groundPoint,
				text,
				itemLayer.getHeight() + OFFSET_Z);
			if (textPoint == null)
			{
				continue;
			}

			int offset = offsetMap.compute(tile.getWorldLocation(), (key, value) -> value == null ? 0 : value + 1);
			renderLifetimeText(graphics, text, textPoint.getX(), textPoint.getY() - STRING_GAP * offset, lifetime);
		}
	}

	@SuppressWarnings("rawtypes")
	private Table getGroundItemsTable()
	{
		if (groundItemsPlugin == null)
		{
			for (Plugin candidate : pluginManager.getPlugins())
			{
				if (candidate instanceof GroundItemsPlugin)
				{
					groundItemsPlugin = (GroundItemsPlugin) candidate;
					break;
				}
			}
		}

		return groundItemsPlugin == null ? null : groundItemsPlugin.getCollectedGroundItems();
	}

	private ItemLifetime getOrCreateLifetime(Tile tile, TileItem item)
	{
		int tick = client.getTickCount();
		int despawnTick = item.getDespawnTime();
		if (despawnTick <= tick)
		{
			return null;
		}

		ItemKey key = new ItemKey(tile.getWorldLocation(), item.getId());
		return itemLifetimes.computeIfAbsent(key, ignored -> new ItemLifetime(tick, despawnTick));
	}

	private void renderLifetimeText(Graphics2D graphics, String text, int x, int y, ItemLifetime lifetime)
	{
		int tick = client.getTickCount();
		int totalTicks = Math.max(1, lifetime.despawnTick - lifetime.spawnTick);
		float remaining = Math.max(0f, Math.min(1f, (lifetime.despawnTick - tick) / (float) totalTicks));
		FontMetrics metrics = graphics.getFontMetrics();
		int fullWidth = metrics.stringWidth(text);
		int visibleWidth = Math.min(fullWidth, Math.max(1, Math.round(fullWidth * remaining)));
		boolean outline = getBooleanGroundItemConfig("textOutline", false);

		renderFadedText(graphics, text, x, y, outline);

		Shape oldClip = graphics.getClip();
		graphics.clipRect(x - 2, y - metrics.getAscent() - 2, visibleWidth + 2, metrics.getHeight() + 4);
		renderText(graphics, text, x, y, lifetimeColor(remaining), outline);
		graphics.setClip(oldClip);
	}

	private void renderFadedText(Graphics2D graphics, String text, int x, int y, boolean outline)
	{
		int darkness = plugin.getGroundItemLifetimeFadedTextDarkness();
		int channel = 255 - darkness * 255 / 100;
		renderText(graphics, text, x, y, new Color(channel, channel, channel), outline);
	}

	private void renderText(Graphics2D graphics, String text, int x, int y, Color color, boolean outline)
	{
		textComponent.setText(text);
		textComponent.setColor(color);
		textComponent.setOutline(outline);
		textComponent.setPosition(new java.awt.Point(x, y));
		textComponent.render(graphics);
	}

	private Color lifetimeColor(float remaining)
	{
		return Color.getHSBColor(remaining / 3f, 0.95f, 1f);
	}

	private String buildItemText(TileItem item)
	{
		ItemComposition composition = itemManager.getItemComposition(item.getId());
		if (composition == null)
		{
			return "Item";
		}

		StringBuilder text = new StringBuilder(composition.getName());
		int quantity = item.getQuantity();
		if (quantity > 1)
		{
			text.append(" (").append(QuantityFormatter.quantityToStackSize(quantity)).append(')');
		}

		if (item.getId() != ItemID.COINS_995)
		{
			appendPrice(text, composition, quantity);
		}

		return text.toString();
	}

	private void appendPrice(StringBuilder text, ItemComposition composition, int quantity)
	{
		String mode = getGroundItemConfig("priceDisplayMode", "BOTH");
		if ("OFF".equalsIgnoreCase(mode))
		{
			return;
		}

		int realItemId = composition.getNote() != -1 ? composition.getLinkedNoteId() : composition.getId();
		long gePrice = (long) itemManager.getItemPrice(realItemId) * quantity;
		long haPrice = (long) composition.getHaPrice() * quantity;
		if ("BOTH".equalsIgnoreCase(mode))
		{
			appendPriceValue(text, "GE", gePrice);
			appendPriceValue(text, "HA", haPrice);
			return;
		}

		appendPriceValue(text, null, "HA".equalsIgnoreCase(mode) ? haPrice : gePrice);
	}

	private void appendPriceValue(StringBuilder text, String prefix, long price)
	{
		if (price <= 0)
		{
			return;
		}

		text.append(" (");
		if (prefix != null)
		{
			text.append(prefix).append(": ");
		}
		text.append(QuantityFormatter.quantityToStackSize(price)).append(" gp)");
	}

	private boolean isGroundItemTextVisible()
	{
		String mode = getGroundItemConfig("itemHighlightMode", "BOTH");
		return "BOTH".equalsIgnoreCase(mode) || "OVERLAY".equalsIgnoreCase(mode);
	}

	private boolean shouldDisplayOwnership(TileItem item)
	{
		String mode = getGroundItemConfig("ownershipFilterMode", "ALL");
		if ("DROPS".equalsIgnoreCase(mode))
		{
			return item.getOwnership() == TileItem.OWNERSHIP_SELF || item.getOwnership() == TileItem.OWNERSHIP_GROUP;
		}
		if ("TAKEABLE".equalsIgnoreCase(mode))
		{
			return item.getOwnership() != TileItem.OWNERSHIP_OTHER || client.getVarbitValue(VarbitID.IRONMAN) == 0;
		}
		return true;
	}

	private String getGroundItemConfig(String key, String defaultValue)
	{
		String value = configManager.getConfiguration(GROUND_ITEMS_GROUP, key);
		return value == null || value.isBlank() ? defaultValue : value;
	}

	private boolean getBooleanGroundItemConfig(String key, boolean defaultValue)
	{
		String value = configManager.getConfiguration(GROUND_ITEMS_GROUP, key);
		return value == null ? defaultValue : Boolean.parseBoolean(value);
	}

	private static final class ItemLifetime
	{
		private final int spawnTick;
		private final int despawnTick;

		private ItemLifetime(int spawnTick, int despawnTick)
		{
			this.spawnTick = spawnTick;
			this.despawnTick = despawnTick;
		}
	}

	private static final class ItemKey
	{
		private final WorldPoint worldPoint;
		private final int itemId;

		private ItemKey(WorldPoint worldPoint, int itemId)
		{
			this.worldPoint = worldPoint;
			this.itemId = itemId;
		}

		@Override
		public boolean equals(Object other)
		{
			if (this == other)
			{
				return true;
			}
			if (!(other instanceof ItemKey))
			{
				return false;
			}

			ItemKey that = (ItemKey) other;
			return itemId == that.itemId && worldPoint.equals(that.worldPoint);
		}

		@Override
		public int hashCode()
		{
			return 31 * worldPoint.hashCode() + itemId;
		}
	}
}

