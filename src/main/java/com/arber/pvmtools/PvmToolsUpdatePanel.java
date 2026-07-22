package com.arber.pvmtools;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.ui.FontManager;

@Singleton
final class PvmToolsUpdatePanel extends JPanel
{
	private static final Color PARCHMENT = new Color(191, 174, 132);
	private static final Color PARCHMENT_LIGHT = new Color(230, 216, 169);
	private static final Color PARCHMENT_DARK = new Color(116, 91, 50);
	private static final Color TEXT_COLOR = new Color(35, 25, 15);
	private static final Color TITLE_COLOR = new Color(125, 0, 0);
	private static final Color GOLD_COLOR = new Color(190, 112, 0);
	private static final int MIN_WIDTH = 430;
	private static final int MAX_WIDTH = 780;
	private static final int MIN_HEIGHT = 300;
	private static final int MAX_HEIGHT = 420;
	private static final double CANVAS_WIDTH_RATIO = 0.62d;
	private static final double CANVAS_HEIGHT_RATIO = 0.52d;
	private static final int EDGE_GAP = 24;
	private static final int ROLL_HEIGHT = 28;
	private static final int CONTENT_INSET = 34;
	private static final int CLOSE_SIZE = 34;
	private static final int DONT_SHOW_WIDTH = 240;
	private static final int DONT_SHOW_HEIGHT = 32;

	private final JButton closeButton = new JButton();
	private final JButton dontShowButton = new JButton("Don't show update notes again");
	private final Timer repositionTimer;
	private Canvas canvas;
	private JLayeredPane layeredPane;
	private String version = "dev";
	private List<String> notes = List.of();
	private Runnable dismissAction = () -> { };
	private Runnable disableAction = () -> { };

	@Inject
	PvmToolsUpdatePanel()
	{
		setLayout(null);
		setOpaque(true);
		setBackground(PARCHMENT);
		setFocusable(false);
		setName("PvM Tools update scroll");

		closeButton.setToolTipText("Close update notes");
		closeButton.setFocusPainted(false);
		closeButton.setContentAreaFilled(false);
		closeButton.setBorderPainted(false);
		closeButton.setFocusable(false);
		closeButton.addActionListener(event -> dismissFromButton());
		add(closeButton);

		dontShowButton.setFont(FontManager.getRunescapeFont().deriveFont(15f));
		dontShowButton.setForeground(PARCHMENT_LIGHT);
		dontShowButton.setBackground(PARCHMENT_DARK);
		dontShowButton.setToolTipText("Disable future PvM Tools update notes");
		dontShowButton.setFocusPainted(false);
		dontShowButton.setFocusable(false);
		dontShowButton.setMargin(new java.awt.Insets(0, 4, 0, 4));
		dontShowButton.addActionListener(event -> disableFromButton());
		add(dontShowButton);

		repositionTimer = new Timer(250, event -> updateBounds());
		repositionTimer.setRepeats(true);
	}

	boolean showPanel(
		Canvas targetCanvas,
		String updateVersion,
		String[] updateNotes,
		Runnable onDismiss,
		Runnable onDisable)
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			throw new IllegalStateException("Update panel must be shown on the Swing event thread");
		}

		JRootPane rootPane = SwingUtilities.getRootPane(targetCanvas);
		if (rootPane == null || targetCanvas.getWidth() < MIN_WIDTH + EDGE_GAP * 2
			|| targetCanvas.getHeight() < MIN_HEIGHT + EDGE_GAP * 2)
		{
			return false;
		}

		hidePanel();
		canvas = targetCanvas;
		layeredPane = rootPane.getLayeredPane();
		version = updateVersion == null || updateVersion.isBlank() ? "dev" : updateVersion;
		notes = updateNotes == null ? List.of() : new ArrayList<>(Arrays.asList(updateNotes));
		dismissAction = onDismiss == null ? () -> { } : onDismiss;
		disableAction = onDisable == null ? () -> { } : onDisable;

		layeredPane.add(this, JLayeredPane.POPUP_LAYER);
		updateBounds();
		setVisible(true);
		layeredPane.revalidate();
		layeredPane.repaint();
		repositionTimer.start();
		return true;
	}

	void hidePanel()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(this::hidePanel);
			return;
		}

		repositionTimer.stop();
		Container parent = getParent();
		if (parent != null)
		{
			parent.remove(this);
			parent.revalidate();
			parent.repaint();
		}

		canvas = null;
		layeredPane = null;
		dismissAction = () -> { };
		disableAction = () -> { };
		setVisible(false);
	}

	boolean isPanelVisible()
	{
		return getParent() != null && isVisible();
	}

	@Override
	public void doLayout()
	{
		closeButton.setBounds(getWidth() - CONTENT_INSET - CLOSE_SIZE + 6, ROLL_HEIGHT + 6, CLOSE_SIZE, CLOSE_SIZE);
		dontShowButton.setBounds(
			(getWidth() - DONT_SHOW_WIDTH) / 2,
			getHeight() - ROLL_HEIGHT - DONT_SHOW_HEIGHT - 11,
			DONT_SHOW_WIDTH,
			DONT_SHOW_HEIGHT);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			drawScroll(g);
			drawContent(g);
			drawCloseButton(g);
		}
		finally
		{
			g.dispose();
		}
	}

	private void updateBounds()
	{
		if (canvas == null || layeredPane == null || canvas.getParent() == null)
		{
			return;
		}

		Dimension canvasSize = canvas.getSize();
		int availableWidth = canvasSize.width - EDGE_GAP * 2;
		int availableHeight = canvasSize.height - EDGE_GAP * 2;
		int width = clamp((int) Math.round(canvasSize.width * CANVAS_WIDTH_RATIO), MIN_WIDTH, Math.min(MAX_WIDTH, availableWidth));
		int height = clamp((int) Math.round(canvasSize.height * CANVAS_HEIGHT_RATIO), MIN_HEIGHT, Math.min(MAX_HEIGHT, availableHeight));
		if (width < MIN_WIDTH || height < MIN_HEIGHT)
		{
			setVisible(false);
			return;
		}

		Point canvasOrigin = SwingUtilities.convertPoint(canvas, 0, 0, layeredPane);
		int x = canvasOrigin.x + (canvasSize.width - width) / 2;
		int y = canvasOrigin.y + (canvasSize.height - height) / 2;
		setBounds(x, y, width, height);
		setVisible(true);
		revalidate();
		repaint();
	}

	private void drawScroll(Graphics2D g)
	{
		int bodyY = ROLL_HEIGHT / 2;
		int bodyHeight = getHeight() - ROLL_HEIGHT;
		g.setPaint(new GradientPaint(0, bodyY, PARCHMENT_LIGHT, getWidth(), bodyY + bodyHeight, PARCHMENT));
		g.fillRect(0, bodyY, getWidth(), bodyHeight);
		g.setColor(new Color(91, 69, 38, 130));
		g.drawRect(0, bodyY, getWidth() - 1, bodyHeight - 1);

		drawRoll(g, 0);
		drawRoll(g, getHeight() - ROLL_HEIGHT);
	}

	private void drawRoll(Graphics2D g, int y)
	{
		g.setPaint(new GradientPaint(0, y, PARCHMENT_LIGHT, 0, y + ROLL_HEIGHT, PARCHMENT_DARK));
		g.fillRoundRect(0, y, getWidth(), ROLL_HEIGHT, ROLL_HEIGHT, ROLL_HEIGHT);
		g.setColor(new Color(78, 56, 29, 150));
		g.drawRoundRect(0, y, getWidth() - 1, ROLL_HEIGHT - 1, ROLL_HEIGHT, ROLL_HEIGHT);
	}

	private void drawContent(Graphics2D g)
	{
		int contentX = CONTENT_INSET + 10;
		int contentWidth = getWidth() - (CONTENT_INSET + 10) * 2;
		int y = ROLL_HEIGHT + 42;

		Font titleFont = FontManager.getRunescapeBoldFont().deriveFont(27f);
		Font subtitleFont = FontManager.getRunescapeFont().deriveFont(18f);
		Font bodyFont = FontManager.getRunescapeFont().deriveFont(19f);

		g.setFont(titleFont);
		drawCenteredText(g, "PvM Tools Update", y, TITLE_COLOR);
		y += 31;

		g.setFont(subtitleFont);
		drawCenteredText(g, "Version " + version, y, GOLD_COLOR);
		y += 39;

		g.setFont(bodyFont);
		g.setColor(TEXT_COLOR);
		g.drawString("What's new", contentX, y);
		y += g.getFontMetrics().getHeight() + 2;

		for (String note : notes)
		{
			for (String line : wrapText(g, "- " + note, contentWidth))
			{
				if (y > getHeight() - ROLL_HEIGHT - DONT_SHOW_HEIGHT - 24)
				{
					break;
				}
				g.drawString(line, contentX, y);
				y += g.getFontMetrics().getHeight() + 1;
			}
			y += 2;
		}
	}

	private void drawCloseButton(Graphics2D g)
	{
		Composite oldComposite = g.getComposite();
		g.setComposite(AlphaComposite.SrcOver.derive(0.88f));
		g.setColor(new Color(239, 222, 172));
		g.fillRoundRect(closeButton.getX(), closeButton.getY(), closeButton.getWidth(), closeButton.getHeight(), 8, 8);
		g.setColor(PARCHMENT_DARK);
		g.setStroke(new BasicStroke(2));
		g.drawRoundRect(closeButton.getX(), closeButton.getY(), closeButton.getWidth(), closeButton.getHeight(), 8, 8);
		int inset = 10;
		g.drawLine(
			closeButton.getX() + inset,
			closeButton.getY() + inset,
			closeButton.getX() + closeButton.getWidth() - inset,
			closeButton.getY() + closeButton.getHeight() - inset);
		g.drawLine(
			closeButton.getX() + closeButton.getWidth() - inset,
			closeButton.getY() + inset,
			closeButton.getX() + inset,
			closeButton.getY() + closeButton.getHeight() - inset);
		g.setStroke(new BasicStroke(1));
		g.setComposite(oldComposite);
	}

	private void drawCenteredText(Graphics2D g, String text, int y, Color color)
	{
		FontMetrics metrics = g.getFontMetrics();
		g.setColor(color);
		g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, y);
	}

	private List<String> wrapText(Graphics2D g, String text, int maxWidth)
	{
		FontMetrics metrics = g.getFontMetrics();
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split("\\s+"))
		{
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (line.length() == 0 || metrics.stringWidth(candidate) <= maxWidth)
			{
				line.setLength(0);
				line.append(candidate);
				continue;
			}

			lines.add(line.toString());
			line.setLength(0);
			line.append(word);
		}

		if (line.length() > 0)
		{
			lines.add(line.toString());
		}
		return lines;
	}

	private int clamp(int value, int minimum, int maximum)
	{
		return Math.max(minimum, Math.min(value, maximum));
	}

	private void dismissFromButton()
	{
		Runnable callback = dismissAction;
		callback.run();
		hidePanel();
	}

	private void disableFromButton()
	{
		Runnable callback = disableAction;
		callback.run();
		hidePanel();
	}
}
