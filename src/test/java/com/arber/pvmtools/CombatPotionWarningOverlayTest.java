package com.arber.pvmtools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CombatPotionWarningOverlayTest
{
	@Test
	public void warningPaintsAFadingFrameAndLeavesTheGameVisible()
	{
		BufferedImage image = paint(765, 503, new Color(255, 0, 0, 200));

		// min(48, 503 / 10) / 8 = 6 px per band, 8 bands, so the frame is 48 px thick.
		assertEquals(200, alpha(image, 0, 0));
		assertEquals(200, alpha(image, 764, 502));
		assertEquals(200, alpha(image, 382, 5));
		assertEquals(175, alpha(image, 382, 6));
		assertEquals(25, alpha(image, 382, 47));
		assertEquals(0, alpha(image, 382, 48));
		assertEquals(25, alpha(image, 47, 251));
		assertEquals(0, alpha(image, 48, 251));
		assertEquals(25, alpha(image, 765 - 48, 251));
		assertEquals(0, alpha(image, 765 - 49, 251));
		assertEquals(25, alpha(image, 382, 503 - 48));
		assertEquals(0, alpha(image, 382, 503 - 49));
		for (int y = 48; y < 503 - 48; y++)
		{
			for (int x = 48; x < 765 - 48; x++)
			{
				assertEquals(0, alpha(image, x, y));
			}
		}
	}

	@Test
	public void frameUsesTheFlashColourAndNeverCoversATinyCanvas()
	{
		BufferedImage image = paint(765, 503, new Color(0, 255, 80, 90));
		Color corner = new Color(image.getRGB(0, 0));
		// Painting with alpha may round a channel by one.
		assertEquals(0, corner.getRed(), 2);
		assertEquals(255, corner.getGreen(), 2);
		assertEquals(80, corner.getBlue(), 2);

		BufferedImage tiny = paint(10, 10, new Color(255, 0, 0, 255));
		assertEquals(0, alpha(tiny, 5, 5));
		assertTrue(alpha(tiny, 0, 0) > 0);
	}

	private static BufferedImage paint(int width, int height, Color color)
	{
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			CombatPotionWarningOverlay.paintEdgeFrame(graphics, color, width, height);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}

	private static int alpha(BufferedImage image, int x, int y)
	{
		return image.getRGB(x, y) >>> 24;
	}
}
