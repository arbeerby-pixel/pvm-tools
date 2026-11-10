package com.arber.pvmtools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.ItemLayer;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

class GroundItemHighlightOverlay extends Overlay
{
	private static final int OUTLINE_WIDTH = 2;
	private static final int OUTLINE_FEATHER = 4;

	private final PvmToolsPlugin plugin;
	private final ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	private GroundItemHighlightOverlay(PvmToolsPlugin plugin, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.plugin = plugin;
		this.modelOutlineRenderer = modelOutlineRenderer;
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

		Color color = plugin.getGroundItemHighlightColor();
		GroundItemVisibilityFilter visibilityFilter = plugin.getGroundItemVisibilityFilter();
		List<Tile> groundItemTiles = plugin.getGroundItemTiles();
		for (Tile tile : groundItemTiles)
		{
			renderTile(tile, color, visibilityFilter);
		}

		return null;
	}

	private void renderTile(Tile tile, Color color, GroundItemVisibilityFilter visibilityFilter)
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
			if (visibilityFilter.isVisible(item) && plugin.meetsGroundItemHighlightMinimum(item))
			{
				modelOutlineRenderer.drawOutline(itemLayer, item, OUTLINE_WIDTH, color, OUTLINE_FEATHER);
			}
		}
	}
}
