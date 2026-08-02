package com.arber.pvmtools;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmToolsUpdatePanelTest
{
	@Test
	public void closeButtonDismissesAndUnregistersOverlay()
	{
		TestHost host = new TestHost();
		AtomicBoolean dismissed = new AtomicBoolean();
		PvmToolsUpdatePanel panel = host.createPanel();
		assertTrue(panel.showPanel(host.canvas, "1.0.0", new String[]{"Test note"}, () -> dismissed.set(true), null));

		Dimension size = renderAndPosition(panel);
		Point closePoint = absolutePoint(panel, size.width / 2 + 132, size.height - 46);
		click(panel, host.canvas, closePoint);

		assertTrue(dismissed.get());
		assertFalse(panel.isPanelVisible());
		assertTrue(host.overlayRemoves.get() == 1);
		assertTrue(host.mouseRemoves.get() == 1);
	}

	@Test
	public void dontShowControlDisablesAndUnregistersOverlay()
	{
		TestHost host = new TestHost();
		AtomicBoolean disabled = new AtomicBoolean();
		PvmToolsUpdatePanel panel = host.createPanel();
		assertTrue(panel.showPanel(host.canvas, "1.0.0", new String[]{"Test note"}, null, () -> disabled.set(true)));

		Dimension size = renderAndPosition(panel);
		Point dontShowPoint = absolutePoint(panel, size.width / 2 - 54, size.height - 46);
		click(panel, host.canvas, dontShowPoint);

		assertTrue(disabled.get());
		assertFalse(panel.isPanelVisible());
	}

	@Test
	public void gameplayClicksOutsideControlsAreNeverConsumed()
	{
		TestHost host = new TestHost();
		PvmToolsUpdatePanel panel = host.createPanel();
		assertTrue(panel.showPanel(host.canvas, "1.0.0", new String[]{"Test note"}, null, null));

		Dimension size = renderAndPosition(panel);
		Point gameplayPoint = absolutePoint(panel, size.width / 2, size.height / 2);
		MouseEvent press = mouseEvent(host.canvas, MouseEvent.MOUSE_PRESSED, gameplayPoint, MouseEvent.BUTTON1);
		MouseEvent release = mouseEvent(host.canvas, MouseEvent.MOUSE_RELEASED, gameplayPoint, MouseEvent.BUTTON1);
		MouseEvent click = mouseEvent(host.canvas, MouseEvent.MOUSE_CLICKED, gameplayPoint, MouseEvent.BUTTON1);

		panel.mousePressed(press);
		panel.mouseReleased(release);
		panel.mouseClicked(click);

		assertFalse(press.isConsumed());
		assertFalse(release.isConsumed());
		assertFalse(click.isConsumed());
		assertTrue(panel.isPanelVisible());
		panel.hidePanel();
	}

	@Test
	public void controlClicksAreConsumedBeforeReachingTheGame()
	{
		TestHost host = new TestHost();
		PvmToolsUpdatePanel panel = host.createPanel();
		assertTrue(panel.showPanel(host.canvas, "1.0.0", new String[]{"Test note"}, null, null));

		Dimension size = renderAndPosition(panel);
		Point closePoint = absolutePoint(panel, size.width / 2 + 132, size.height - 46);
		MouseEvent press = mouseEvent(host.canvas, MouseEvent.MOUSE_PRESSED, closePoint, MouseEvent.BUTTON1);
		panel.mousePressed(press);

		assertTrue(press.isConsumed());
		panel.hidePanel();
	}

	private static Dimension renderAndPosition(PvmToolsUpdatePanel panel)
	{
		BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		Dimension size;
		try
		{
			size = panel.render(graphics);
		}
		finally
		{
			graphics.dispose();
		}

		Point location = panel.getPreferredLocation();
		panel.setBounds(new java.awt.Rectangle(location, size));
		return size;
	}

	private static Point absolutePoint(PvmToolsUpdatePanel panel, int localX, int localY)
	{
		return new Point(panel.getBounds().x + localX, panel.getBounds().y + localY);
	}

	private static void click(PvmToolsUpdatePanel panel, Canvas canvas, Point point)
	{
		MouseEvent press = mouseEvent(canvas, MouseEvent.MOUSE_PRESSED, point, MouseEvent.BUTTON1);
		MouseEvent release = mouseEvent(canvas, MouseEvent.MOUSE_RELEASED, point, MouseEvent.BUTTON1);
		MouseEvent click = mouseEvent(canvas, MouseEvent.MOUSE_CLICKED, point, MouseEvent.BUTTON1);
		panel.mousePressed(press);
		panel.mouseReleased(release);
		panel.mouseClicked(click);
		assertTrue(press.isConsumed());
		assertTrue(release.isConsumed());
		assertTrue(click.isConsumed());
	}

	private static MouseEvent mouseEvent(Canvas canvas, int id, Point point, int button)
	{
		return new MouseEvent(canvas, id, System.currentTimeMillis(), 0, point.x, point.y, 1, false, button);
	}

	private static final class TestHost
	{
		private final Canvas canvas = new Canvas();
		private final AtomicInteger overlayRemoves = new AtomicInteger();
		private final AtomicInteger mouseRemoves = new AtomicInteger();

		private PvmToolsUpdatePanel createPanel()
		{
			return new PvmToolsUpdatePanel(
				() -> 800,
				() -> 600,
				overlay -> { },
				overlay -> overlayRemoves.incrementAndGet(),
				listener -> { },
				listener -> mouseRemoves.incrementAndGet());
		}
	}
}
