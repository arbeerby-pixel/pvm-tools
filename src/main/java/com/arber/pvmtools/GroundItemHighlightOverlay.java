package com.arber.pvmtools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemLayer;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

class GroundItemHighlightOverlay extends Overlay
{
	private static final int OUTLINE_FEATHER = 4;

	private final Client client;
	private final PvmToolsPlugin plugin;
	private final ModelOutlineRenderer modelOutlineRenderer;
	private final ConfigManager configManager;
	private final ItemManager itemManager;

	@Inject
	private GroundItemHighlightOverlay(
		Client client,
		PvmToolsPlugin plugin,
		ModelOutlineRenderer modelOutlineRenderer,
		ConfigManager configManager,
		ItemManager itemManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.modelOutlineRenderer = modelOutlineRenderer;
		this.configManager = configManager;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.shouldHighlightGroundItems())
		{
			return null;
		}

		Scene scene = client.getScene();
		if (scene == null || scene.getTiles() == null)
		{
			return null;
		}

		Tile[][][] tiles = scene.getTiles();
		int plane = client.getPlane();
		if (plane < 0 || plane >= tiles.length || tiles[plane] == null)
		{
			return null;
		}

		Color color = plugin.getGroundItemHighlightColor();
		int width = plugin.getGroundItemHighlightWidth();
		GroundItemVisibilityFilter visibilityFilter = GroundItemVisibilityFilter.load(configManager, itemManager);
		for (Tile[] row : tiles[plane])
		{
			if (row == null)
			{
				continue;
			}

			for (Tile tile : row)
			{
				renderTile(tile, color, width, visibilityFilter);
				if (tile != null)
				{
					renderTile(tile.getBridge(), color, width, visibilityFilter);
				}
			}
		}

		return null;
	}

	private void renderTile(Tile tile, Color color, int width, GroundItemVisibilityFilter visibilityFilter)
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

		for (TileItem item : tile.getGroundItems())
		{
			if (visibilityFilter.isVisible(item))
			{
				modelOutlineRenderer.drawOutline(itemLayer, item, width, color, OUTLINE_FEATHER);
			}
		}
	}
}
