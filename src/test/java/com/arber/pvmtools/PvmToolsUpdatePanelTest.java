package com.arber.pvmtools;

import java.awt.Canvas;
import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PvmToolsUpdatePanelTest
{
	@Test
	public void closeButtonDismissesAndRemovesPanel() throws Exception
	{
		AtomicBoolean dismissed = new AtomicBoolean();
		AtomicReference<PvmToolsUpdatePanel> panelReference = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() ->
		{
			TestHierarchy hierarchy = createHierarchy();
			PvmToolsUpdatePanel panel = new PvmToolsUpdatePanel();
			assertTrue(panel.showPanel(hierarchy.canvas, "1.0.0", new String[]{"Test note"}, () -> dismissed.set(true), null));
			assertTrue(panel.isPanelVisible());

			JButton closeButton = (JButton) panel.getComponent(0);
			closeButton.doClick();
			panelReference.set(panel);
		});

		assertTrue(dismissed.get());
		assertFalse(panelReference.get().isPanelVisible());
		assertNull(panelReference.get().getParent());
	}

	@Test
	public void panelOnlyOccupiesTheCenteredScrollBounds() throws Exception
	{
		AtomicReference<Rectangle> boundsReference = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() ->
		{
			TestHierarchy hierarchy = createHierarchy();
			PvmToolsUpdatePanel panel = new PvmToolsUpdatePanel();
			assertTrue(panel.showPanel(hierarchy.canvas, "1.0.0", new String[]{"Test note"}, null, null));
			boundsReference.set(panel.getBounds());
			panel.hidePanel();
		});

		Rectangle bounds = boundsReference.get();
		assertEquals(496, bounds.width);
		assertEquals(312, bounds.height);
		assertTrue(bounds.x > 0);
		assertTrue(bounds.y > 0);
	}

	@Test
	public void dontShowButtonDisablesAndRemovesPanel() throws Exception
	{
		AtomicBoolean dismissed = new AtomicBoolean();
		AtomicBoolean disabled = new AtomicBoolean();
		AtomicReference<PvmToolsUpdatePanel> panelReference = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() ->
		{
			TestHierarchy hierarchy = createHierarchy();
			PvmToolsUpdatePanel panel = new PvmToolsUpdatePanel();
			assertTrue(panel.showPanel(
				hierarchy.canvas,
				"1.0.0",
				new String[]{"Test note"},
				() -> dismissed.set(true),
				() -> disabled.set(true)));

			JButton dontShowButton = (JButton) panel.getComponent(1);
			dontShowButton.doClick();
			panelReference.set(panel);
		});

		assertTrue(disabled.get());
		assertFalse(dismissed.get());
		assertFalse(panelReference.get().isPanelVisible());
		assertNull(panelReference.get().getParent());
	}

	private static TestHierarchy createHierarchy()
	{
		JRootPane rootPane = new JRootPane();
		rootPane.setSize(900, 700);
		JPanel content = new JPanel(null);
		rootPane.setContentPane(content);
		rootPane.doLayout();
		content.setSize(900, 700);

		Canvas canvas = new Canvas();
		canvas.setBounds(20, 20, 800, 600);
		content.add(canvas);
		return new TestHierarchy(canvas);
	}

	private static final class TestHierarchy
	{
		private final Canvas canvas;

		private TestHierarchy(Canvas canvas)
		{
			this.canvas = canvas;
		}
	}
}
