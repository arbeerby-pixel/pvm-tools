package com.arber.pvmtools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class CombatPotionWarningOverlay extends Overlay
{
	private final Client client;
	private final PvmToolsPlugin plugin;

	@Inject
	private CombatPotionWarningOverlay(Client client, PvmToolsPlugin plugin)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		this.client = client;
		this.plugin = plugin;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Color color = plugin.getScreenFlashColor();
		if (color == null || color.getAlpha() <= 0)
		{
			return null;
		}

		Rectangle bounds = client.getCanvas().getBounds();
		if (bounds.width <= 0 || bounds.height <= 0)
		{
			return null;
		}

		graphics.setColor(color);
		graphics.fillRect(0, 0, bounds.width, bounds.height);
		return null;
	}
}
