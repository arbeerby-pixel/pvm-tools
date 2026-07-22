package com.arber.pvmtools;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

class InventorySpacesInfoBox extends InfoBox
{
	private static final int IMAGE_SIZE = 32;
	private static final int IMAGE_PADDING = 2;
	private static final int LOW_SPACE_THRESHOLD = 3;
	private static final String FONT_NAME = "Arial";
	private static final Color WARNING_YELLOW = new Color(255, 218, 69);
	private static final Color WARNING_RED = new Color(255, 45, 45);

	private int freeSpaces;

	InventorySpacesInfoBox(int freeSpaces, Plugin plugin)
	{
		super(createSpacesImage(freeSpaces), plugin);
		this.freeSpaces = freeSpaces;
		setPriority(InfoBoxPriority.MED);
	}

	boolean setFreeSpaces(int freeSpaces)
	{
		if (this.freeSpaces == freeSpaces)
		{
			return false;
		}

		this.freeSpaces = freeSpaces;
		setImage(createSpacesImage(freeSpaces));
		return true;
	}

	int getFreeSpaces()
	{
		return freeSpaces;
	}

	@Override
	public String getText()
	{
		return "";
	}

	@Override
	public Color getTextColor()
	{
		return getSpacesColor(freeSpaces);
	}

	@Override
	public String getName()
	{
		return "INVENTORY_SPACES";
	}

	private static BufferedImage createSpacesImage(int freeSpaces)
	{
		BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			String text = Integer.toString(freeSpaces);
			Font font = fitFont(graphics, text);
			graphics.setFont(font);

			FontMetrics metrics = graphics.getFontMetrics();
			int x = (IMAGE_SIZE - metrics.stringWidth(text)) / 2;
			int y = ((IMAGE_SIZE - metrics.getHeight()) / 2) + metrics.getAscent();

			graphics.setColor(Color.BLACK);
			for (int offsetX = -1; offsetX <= 1; offsetX++)
			{
				for (int offsetY = -1; offsetY <= 1; offsetY++)
				{
					if (offsetX != 0 || offsetY != 0)
					{
						graphics.drawString(text, x + offsetX, y + offsetY);
					}
				}
			}

			graphics.setColor(getSpacesColor(freeSpaces));
			graphics.drawString(text, x, y);
		}
		finally
		{
			graphics.dispose();
		}

		return image;
	}

	private static Font fitFont(Graphics2D graphics, String text)
	{
		for (int size = 30; size >= 14; size--)
		{
			Font font = new Font(FONT_NAME, Font.BOLD, size);
			FontMetrics metrics = graphics.getFontMetrics(font);
			if (metrics.stringWidth(text) <= IMAGE_SIZE - IMAGE_PADDING * 2
				&& metrics.getHeight() <= IMAGE_SIZE - IMAGE_PADDING * 2)
			{
				return font;
			}
		}

		return new Font(FONT_NAME, Font.BOLD, 14);
	}

	private static Color getSpacesColor(int freeSpaces)
	{
		if (freeSpaces <= 0)
		{
			return WARNING_RED;
		}

		if (freeSpaces <= LOW_SPACE_THRESHOLD)
		{
			return WARNING_YELLOW;
		}

		return Color.WHITE;
	}
}
