package com.arber.pvmtools;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
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

	@Test
	public void everyReleaseNoteRendersOnStandardCanvasWithoutPaging() throws Exception
	{
		TestHost host = new TestHost();
		PvmToolsUpdatePanel panel = host.createPanel();
		String[] notes = releaseNotes();
		assertTrue(panel.showPanel(host.canvas, "1.4.7", notes, null, null));
		Dimension size = renderAndPosition(panel);

		assertEquals(1, panel.getNotePageCount());
		assertEquals(String.join(" ", notes), String.join(" ", panel.getRenderedNoteLines()));
		assertTrue(size.height > 330);
		assertTrue(size.height <= 600 - 48);
		assertTrue(controlBounds(panel, "nextNotesBounds").isEmpty());
		assertTrue(controlBounds(panel, "previousNotesBounds").isEmpty());
		panel.hidePanel();
	}

	@Test
	public void smallSupportedCanvasCanPageThroughEveryLineOfReleaseNotes() throws Exception
	{
		TestHost host = new TestHost(508, 368);
		PvmToolsUpdatePanel panel = host.createPanel();
		String[] notes = releaseNotes();
		assertTrue(panel.showPanel(host.canvas, "1.4.7", notes, null, null));
		Dimension size = renderAndPosition(panel);
		assertEquals(320, size.height);
		assertTrue(panel.getNotePageCount() > 1);
		List<String> rendered = new ArrayList<>(panel.getRenderedNoteLines());
		while (panel.getNotePage() + 1 < panel.getNotePageCount())
		{
			int previousPage = panel.getNotePage();
			clickControl(panel, host.canvas, "nextNotesBounds");
			renderAndPosition(panel);
			assertEquals(previousPage + 1, panel.getNotePage());
			assertFalse(panel.getRenderedNoteLines().isEmpty());
			rendered.addAll(panel.getRenderedNoteLines());
		}
		assertEquals(String.join(" ", notes), String.join(" ", rendered));
		int lastPage = panel.getNotePage();
		clickControl(panel, host.canvas, "previousNotesBounds");
		renderAndPosition(panel);
		assertEquals(lastPage - 1, panel.getNotePage());
		panel.hidePanel();
		Rectangle next = controlBounds(panel, "nextNotesBounds");
		assertTrue(next.isEmpty());
		MouseEvent press = mouseEvent(host.canvas, MouseEvent.MOUSE_PRESSED, new Point(0, 0), MouseEvent.BUTTON1);
		panel.mousePressed(press);
		assertFalse(press.isConsumed());
	}

	@Test
	public void aMultilineNoteCanContinueOntoFollowingPagesWithoutDroppingWords() throws Exception
	{
		TestHost host = new TestHost(508, 368);
		PvmToolsUpdatePanel panel = host.createPanel();
		String note = String.join(" ", java.util.Collections.nCopies(60, "multiline"));
		assertTrue(panel.showPanel(host.canvas, "1.4.7", new String[]{note}, null, null));
		renderAndPosition(panel);
		List<String> rendered = new ArrayList<>(panel.getRenderedNoteLines());
		assertTrue(panel.getNotePageCount() > 1);
		while (panel.getNotePage() + 1 < panel.getNotePageCount())
		{
			clickControl(panel, host.canvas, "nextNotesBounds");
			renderAndPosition(panel);
			rendered.addAll(panel.getRenderedNoteLines());
		}
		assertEquals(note, String.join(" ", rendered));
		panel.hidePanel();
	}

	private static String[] releaseNotes() throws Exception
	{
		Field field = PvmToolsPlugin.class.getDeclaredField("UPDATE_SCROLL_NOTES");
		field.setAccessible(true);
		return (String[]) field.get(null);
	}

	private static Rectangle controlBounds(PvmToolsUpdatePanel panel, String fieldName) throws Exception
	{
		Field field = PvmToolsUpdatePanel.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (Rectangle) field.get(panel);
	}

	private static void clickControl(PvmToolsUpdatePanel panel, Canvas canvas, String fieldName) throws Exception
	{
		Rectangle bounds = controlBounds(panel, fieldName);
		assertFalse(bounds.isEmpty());
		click(panel, canvas, absolutePoint(panel, bounds.x + bounds.width / 2, bounds.y + bounds.height / 2));
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
		private final int width;
		private final int height;
		private final AtomicInteger overlayRemoves = new AtomicInteger();
		private final AtomicInteger mouseRemoves = new AtomicInteger();

		private TestHost()
		{
			this(800, 600);
		}

		private TestHost(int width, int height)
		{
			this.width = width;
			this.height = height;
		}

		private PvmToolsUpdatePanel createPanel()
		{
			return new PvmToolsUpdatePanel(
				() -> width,
				() -> height,
				overlay -> { },
				overlay -> overlayRemoves.incrementAndGet(),
				listener -> { },
				listener -> mouseRemoves.incrementAndGet());
		}
	}
}
