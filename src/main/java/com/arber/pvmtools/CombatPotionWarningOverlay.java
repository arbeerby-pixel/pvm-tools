package com.arber.pvmtools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class CombatPotionWarningOverlay extends Overlay
{
	private static final int FRAME_BANDS = 8;
	private static final int MAX_FRAME_THICKNESS = 48;

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

		paintEdgeFrame(graphics, color, client.getCanvasWidth(), client.getCanvasHeight());
		return null;
	}

	// A full-screen fill hid the game exactly when the player needed to see it, at low Hitpoints.
	static void paintEdgeFrame(Graphics2D graphics, Color color, int width, int height)
	{
		if (width <= 0 || height <= 0)
		{
			return;
		}

		int step = Math.max(1, Math.min(MAX_FRAME_THICKNESS, Math.min(width, height) / 10) / FRAME_BANDS);
		for (int band = 0; band < FRAME_BANDS; band++)
		{
			int inset = band * step;
			int innerWidth = width - 2 * inset;
			int sideHeight = height - 2 * inset - 2 * step;
			if (innerWidth <= 2 * step || sideHeight <= 0)
			{
				return;
			}

			int alpha = color.getAlpha() * (FRAME_BANDS - band) / FRAME_BANDS;
			graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
			graphics.fillRect(inset, inset, innerWidth, step);
			graphics.fillRect(inset, height - inset - step, innerWidth, step);
			graphics.fillRect(inset, inset + step, step, sideHeight);
			graphics.fillRect(width - inset - step, inset + step, step, sideHeight);
		}
	}
}
