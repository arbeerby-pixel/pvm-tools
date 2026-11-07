package com.arber.pvmtools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/** Reminds the player to refresh an expired Saturated Heart boost when the heart is ready. */
final class SaturatedHeartInventoryOverlay extends WidgetItemOverlay
{
	private static final Color HIGHLIGHT = new Color(255, 180, 0, 70);
	private static final Color OUTLINE = new Color(255, 210, 70, 230);

	private final Client client;
	private final PvmToolsConfig config;

	@Inject
	SaturatedHeartInventoryOverlay(Client client, PvmToolsConfig config)
	{
		this.client = client;
		this.config = config;
		showOnInterfaces(InterfaceID.INVENTORY);
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!shouldHighlight(
			itemId,
			config.showSaturatedHeartReminder(),
			client.getGameState(),
			client.getVarbitValue(VarbitID.SATURATED_HEART_TIME),
			client.getVarbitValue(VarbitID.IMBUED_HEART_TIMER)))
		{
			return;
		}

		Rectangle bounds = widgetItem.getCanvasBounds();
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return;
		}

		graphics.setColor(HIGHLIGHT);
		graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
		Stroke previousStroke = graphics.getStroke();
		try
		{
			graphics.setColor(OUTLINE);
			graphics.setStroke(new BasicStroke(2f));
			graphics.drawRect(bounds.x + 1, bounds.y + 1, bounds.width - 3, bounds.height - 3);
		}
		finally
		{
			graphics.setStroke(previousStroke);
		}
	}

	static boolean shouldHighlight(
		int itemId,
		boolean enabled,
		GameState gameState,
		int activeEffectTicks,
		int cooldownTicks)
	{
		return enabled
			&& itemId == ItemID.SATURATED_HEART
			&& gameState == GameState.LOGGED_IN
			&& activeEffectTicks <= 0
			&& cooldownTicks <= 0;
	}
}
