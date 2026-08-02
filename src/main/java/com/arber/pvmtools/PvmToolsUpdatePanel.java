package com.arber.pvmtools;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.LinkBrowser;

@Singleton
final class PvmToolsUpdatePanel extends Overlay implements MouseListener
{
	private static final Color PARCHMENT = new Color(213, 194, 145);
	private static final Color PARCHMENT_LIGHT = new Color(239, 224, 174);
	private static final Color PARCHMENT_DARK = new Color(117, 87, 43);
	private static final Color PARCHMENT_EDGE = new Color(93, 66, 31);
	private static final Color TEXT_COLOR = new Color(47, 34, 20);
	private static final Color UPDATE_TEXT_COLOR = new Color(25, 18, 10);
	private static final Color TITLE_COLOR = new Color(130, 19, 12);
	private static final Color GOLD_COLOR = new Color(181, 105, 0);
	private static final int MIN_WIDTH = 460;
	private static final int MAX_WIDTH = 820;
	private static final int MIN_HEIGHT = 320;
	private static final int MAX_HEIGHT = 440;
	private static final double CANVAS_WIDTH_RATIO = 0.64d;
	private static final double CANVAS_HEIGHT_RATIO = 0.55d;
	private static final int EDGE_GAP = 24;
	private static final int ROLL_HEIGHT = 34;
	private static final int ROLL_OVERHANG = 32;
	private static final int CONTENT_INSET = 38;
	private static final int CONTROL_HEIGHT = 32;
	private static final int DONT_SHOW_WIDTH = 252;
	private static final int CLOSE_WIDTH = 96;
	private static final int CONTROL_GAP = 12;
	private static final int CLOSE_VERTICAL_OFFSET = 7;
	private static final int CONTROL_SECTION_HEIGHT = 62;
	private static final int DISCORD_SECTION_HEIGHT = 42;
	private static final int DISCORD_LINK_WIDTH = 300;
	private static final int DISCORD_LINK_HEIGHT = 30;
	private static final String DISCORD_URL = "https://discord.gg/utYem4XhQS";

	private final IntSupplier canvasWidthSupplier;
	private final IntSupplier canvasHeightSupplier;
	private final Consumer<Overlay> addOverlay;
	private final Consumer<Overlay> removeOverlay;
	private final Consumer<MouseListener> addMouseListener;
	private final Consumer<MouseListener> removeMouseListener;
	private final Rectangle closeBounds = new Rectangle();
	private final Rectangle dontShowBounds = new Rectangle();
	private final Rectangle discordBounds = new Rectangle();

	private String version = "dev";
	private List<String> notes = List.of();
	private Runnable dismissAction = () -> { };
	private Runnable disableAction = () -> { };
	private boolean visible;
	private boolean registered;
	private boolean dontShowSelected;
	private boolean suppressNextClick;
	private Control pressedControl = Control.NONE;
	private Control hoveredControl = Control.NONE;

	@Inject
	PvmToolsUpdatePanel(Client client, OverlayManager overlayManager, MouseManager mouseManager)
	{
		this(
			client::getCanvasWidth,
			client::getCanvasHeight,
			overlayManager::add,
			overlayManager::remove,
			mouseManager::registerMouseListener,
			mouseManager::unregisterMouseListener);
	}

	PvmToolsUpdatePanel(
		IntSupplier canvasWidthSupplier,
		IntSupplier canvasHeightSupplier,
		Consumer<Overlay> addOverlay,
		Consumer<Overlay> removeOverlay,
		Consumer<MouseListener> addMouseListener,
		Consumer<MouseListener> removeMouseListener)
	{
		this.canvasWidthSupplier = canvasWidthSupplier;
		this.canvasHeightSupplier = canvasHeightSupplier;
		this.addOverlay = addOverlay;
		this.removeOverlay = removeOverlay;
		this.addMouseListener = addMouseListener;
		this.removeMouseListener = removeMouseListener;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(Overlay.PRIORITY_HIGHEST);
		setMovable(false);
		setSnappable(false);
		setResizable(false);
	}

	boolean showPanel(
		Canvas targetCanvas,
		String updateVersion,
		String[] updateNotes,
		Runnable onDismiss,
		Runnable onDisable)
	{
		if (targetCanvas == null || calculateDimensions().width == 0)
		{
			return false;
		}

		hidePanel();
		version = updateVersion == null || updateVersion.isBlank() ? "dev" : updateVersion;
		notes = updateNotes == null ? List.of() : new ArrayList<>(Arrays.asList(updateNotes));
		dismissAction = onDismiss == null ? () -> { } : onDismiss;
		disableAction = onDisable == null ? () -> { } : onDisable;
		dontShowSelected = false;
		visible = true;
		addOverlay.accept(this);
		addMouseListener.accept(this);
		registered = true;
		return true;
	}

	void hidePanel()
	{
		visible = false;
		pressedControl = Control.NONE;
		hoveredControl = Control.NONE;
		clearControlBounds();
		if (registered)
		{
			removeMouseListener.accept(this);
			removeOverlay.accept(this);
			registered = false;
		}

		dismissAction = () -> { };
		disableAction = () -> { };
	}

	boolean isPanelVisible()
	{
		return visible && registered;
	}

	@Override
	public Point getPreferredLocation()
	{
		Dimension size = calculateDimensions();
		return new Point(
			Math.max(0, (canvasWidthSupplier.getAsInt() - size.width) / 2),
			Math.max(0, (canvasHeightSupplier.getAsInt() - size.height) / 2));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!visible)
		{
			return null;
		}

		Dimension size = calculateDimensions();
		if (size.width == 0 || size.height == 0)
		{
			clearControlBounds();
			return null;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int bodyX = ROLL_OVERHANG;
		int bodyY = ROLL_HEIGHT / 2;
		int bodyWidth = size.width - ROLL_OVERHANG * 2;
		int bodyHeight = size.height - ROLL_HEIGHT;

		drawScrollBody(graphics, bodyX, bodyY, bodyWidth, bodyHeight);
		drawScrollRoll(graphics, 0, 0, size.width, ROLL_HEIGHT);
		drawScrollRoll(graphics, 0, size.height - ROLL_HEIGHT, size.width, ROLL_HEIGHT);
		layoutControls(bodyX, bodyY, bodyWidth, bodyHeight);
		drawContent(graphics, bodyX, bodyY, bodyWidth, bodyHeight);
		drawControls(graphics);
		return size;
	}

	private Dimension calculateDimensions()
	{
		int canvasWidth = canvasWidthSupplier.getAsInt();
		int canvasHeight = canvasHeightSupplier.getAsInt();
		int availableWidth = canvasWidth - EDGE_GAP * 2;
		int availableHeight = canvasHeight - EDGE_GAP * 2;
		if (availableWidth < MIN_WIDTH || availableHeight < MIN_HEIGHT)
		{
			return new Dimension();
		}

		return new Dimension(
			clamp((int) Math.round(canvasWidth * CANVAS_WIDTH_RATIO), MIN_WIDTH, Math.min(MAX_WIDTH, availableWidth)),
			clamp((int) Math.round(canvasHeight * CANVAS_HEIGHT_RATIO), MIN_HEIGHT, Math.min(MAX_HEIGHT, availableHeight)));
	}

	private void layoutControls(int bodyX, int bodyY, int bodyWidth, int bodyHeight)
	{
		int controlsWidth = DONT_SHOW_WIDTH + CONTROL_GAP + CLOSE_WIDTH;
		int controlsX = bodyX + (bodyWidth - controlsWidth) / 2;
		int controlsY = bodyY + bodyHeight - 45;
		dontShowBounds.setBounds(controlsX, controlsY, DONT_SHOW_WIDTH, CONTROL_HEIGHT);
		closeBounds.setBounds(
			controlsX + DONT_SHOW_WIDTH + CONTROL_GAP,
			controlsY - CLOSE_VERTICAL_OFFSET,
			CLOSE_WIDTH,
			CONTROL_HEIGHT);
		discordBounds.setBounds(
			bodyX + (bodyWidth - DISCORD_LINK_WIDTH) / 2,
			bodyY + bodyHeight - CONTROL_SECTION_HEIGHT - DISCORD_SECTION_HEIGHT + 3,
			DISCORD_LINK_WIDTH,
			DISCORD_LINK_HEIGHT);
	}

	private void clearControlBounds()
	{
		closeBounds.setBounds(0, 0, 0, 0);
		dontShowBounds.setBounds(0, 0, 0, 0);
		discordBounds.setBounds(0, 0, 0, 0);
	}

	private void drawScrollBody(Graphics2D g, int x, int y, int width, int height)
	{
		g.setPaint(new GradientPaint(x, y, PARCHMENT_LIGHT, x, y + height, PARCHMENT));
		g.fillRect(x, y, width, height);

		g.setStroke(new BasicStroke(1.2f));
		g.setColor(new Color(PARCHMENT_EDGE.getRed(), PARCHMENT_EDGE.getGreen(), PARCHMENT_EDGE.getBlue(), 165));
		g.drawRect(x, y, width - 1, height - 1);
		g.setColor(new Color(255, 245, 205, 105));
		g.drawRect(x + 8, y + 8, width - 17, height - 17);

		int dividerY = y + height - CONTROL_SECTION_HEIGHT;
		g.setColor(new Color(PARCHMENT_EDGE.getRed(), PARCHMENT_EDGE.getGreen(), PARCHMENT_EDGE.getBlue(), 75));
		g.drawLine(x + 30, dividerY, x + width - 31, dividerY);
		g.setColor(new Color(255, 244, 201, 95));
		g.drawLine(x + 31, dividerY + 1, x + width - 32, dividerY + 1);
	}

	private void drawScrollRoll(Graphics2D g, int x, int y, int width, int height)
	{
		g.setColor(new Color(0, 0, 0, 55));
		g.fillRoundRect(x + 2, y + 3, width - 4, height - 3, height, height);
		g.setPaint(new LinearGradientPaint(
			x,
			y,
			x,
			y + height,
			new float[]{0f, 0.28f, 0.62f, 1f},
			new Color[]{PARCHMENT_DARK, PARCHMENT_LIGHT, PARCHMENT, new Color(91, 65, 31)}));
		g.fillRoundRect(x + 1, y + 1, width - 3, height - 4, height, height);
		g.setColor(new Color(255, 246, 207, 125));
		g.drawRoundRect(x + 4, y + 3, width - 9, height - 9, height - 6, height - 6);
		g.setColor(new Color(67, 45, 22, 165));
		g.drawRoundRect(x, y, width - 1, height - 1, height, height);
		g.setColor(new Color(80, 54, 25, 80));
		g.drawArc(x + 9, y + 4, ROLL_HEIGHT - 9, height - 9, 90, 180);
		g.drawArc(x + width - ROLL_HEIGHT, y + 4, ROLL_HEIGHT - 9, height - 9, -90, 180);
	}

	private void drawContent(Graphics2D g, int bodyX, int bodyY, int bodyWidth, int bodyHeight)
	{
		int contentX = bodyX + CONTENT_INSET;
		int contentWidth = bodyWidth - CONTENT_INSET * 2;
		int y = bodyY + 48;

		Font titleFont = FontManager.getRunescapeBoldFont().deriveFont(25f);
		Font subtitleFont = FontManager.getRunescapeFont().deriveFont(16f);
		Font sectionFont = FontManager.getRunescapeBoldFont().deriveFont(17f);
		Font bodyFont = FontManager.getRunescapeFont().deriveFont(17f);

		g.setFont(titleFont);
		drawCenteredText(g, "PvM Toolkit Update", bodyX, bodyWidth, y, TITLE_COLOR);
		y += 29;

		g.setFont(subtitleFont);
		String versionLabel = "Version " + version;
		FontMetrics subtitleMetrics = g.getFontMetrics();
		int badgeWidth = subtitleMetrics.stringWidth(versionLabel) + 24;
		int badgeX = bodyX + (bodyWidth - badgeWidth) / 2;
		g.setColor(new Color(132, 88, 34, 32));
		g.fillRoundRect(badgeX, y - 17, badgeWidth, 23, 12, 12);
		g.setColor(new Color(126, 84, 33, 85));
		g.drawRoundRect(badgeX, y - 17, badgeWidth, 23, 12, 12);
		drawCenteredText(g, versionLabel, bodyX, bodyWidth, y, GOLD_COLOR);
		y += 37;

		g.setFont(sectionFont);
		FontMetrics sectionMetrics = g.getFontMetrics();
		String sectionTitle = "WHAT'S NEW";
		int sectionTitleWidth = sectionMetrics.stringWidth(sectionTitle);
		int sectionCenter = bodyX + bodyWidth / 2;
		int lineGap = 12;
		g.setColor(new Color(PARCHMENT_EDGE.getRed(), PARCHMENT_EDGE.getGreen(), PARCHMENT_EDGE.getBlue(), 90));
		g.drawLine(contentX, y - 5, sectionCenter - sectionTitleWidth / 2 - lineGap, y - 5);
		g.drawLine(sectionCenter + sectionTitleWidth / 2 + lineGap, y - 5, contentX + contentWidth, y - 5);
		drawCenteredText(g, sectionTitle, bodyX, bodyWidth, y, GOLD_COLOR);
		y += sectionMetrics.getHeight() + 5;

		int notesBottom = bodyY + bodyHeight - CONTROL_SECTION_HEIGHT - DISCORD_SECTION_HEIGHT - 12;
		bodyFont = fitNotesFont(g, bodyFont, notes, contentWidth - 20, notesBottom - y);
		g.setFont(bodyFont);
		for (String note : notes)
		{
			int bulletX = contentX + 3;
			int textX = contentX + 20;
			List<String> lines = wrapText(g, note, contentWidth - 20);
			int lineHeight = g.getFontMetrics().getHeight();
			if (lines.isEmpty() || y + lineHeight * lines.size() > notesBottom + lineHeight)
			{
				break;
			}
			g.setColor(GOLD_COLOR);
			g.fillOval(bulletX, y - 9, 6, 6);
			g.setColor(UPDATE_TEXT_COLOR);
			for (String line : lines)
			{
				g.drawString(line, textX, y);
				y += lineHeight;
			}
			y += 5;
		}
	}

	private Font fitNotesFont(Graphics2D g, Font preferred, List<String> updateNotes, int maxWidth, int availableHeight)
	{
		for (float size = preferred.getSize2D(); size >= 12f; size -= 1f)
		{
			Font candidate = preferred.deriveFont(size);
			g.setFont(candidate);
			int requiredHeight = 0;
			int lineHeight = g.getFontMetrics().getHeight();
			for (String note : updateNotes)
			{
				requiredHeight += wrapText(g, note, maxWidth).size() * lineHeight + 5;
			}
			if (requiredHeight <= availableHeight)
			{
				return candidate;
			}
		}
		return preferred.deriveFont(12f);
	}

	private void drawControls(Graphics2D g)
	{
		drawDiscordLink(g);
		drawDontShowControl(g);
		drawCloseButton(g);
	}

	private void drawDiscordLink(Graphics2D g)
	{
		g.setFont(FontManager.getRunescapeBoldFont().deriveFont(20f));
		FontMetrics metrics = g.getFontMetrics();
		String text = "Join the Arber Plugins Discord";
		int textX = discordBounds.x + (discordBounds.width - metrics.stringWidth(text)) / 2;
		int textY = discordBounds.y + (discordBounds.height - metrics.getHeight()) / 2 + metrics.getAscent();
		g.setColor(hoveredControl == Control.DISCORD ? TITLE_COLOR : PARCHMENT_EDGE);
		g.drawString(text, textX, textY);
		if (hoveredControl == Control.DISCORD)
		{
			g.drawLine(textX, textY + 2, textX + metrics.stringWidth(text), textY + 2);
		}
	}

	private void drawDontShowControl(Graphics2D g)
	{
		g.setFont(FontManager.getRunescapeFont().deriveFont(15f));
		FontMetrics metrics = g.getFontMetrics();
		String text = "Don't show update notes again";
		int iconSize = 18;
		int totalWidth = metrics.stringWidth(text) + 10 + iconSize;
		int startX = dontShowBounds.x + (dontShowBounds.width - totalWidth) / 2;
		int textY = dontShowBounds.y + (dontShowBounds.height - metrics.getHeight()) / 2 + metrics.getAscent();
		int iconX = startX + metrics.stringWidth(text) + 10;
		int iconY = dontShowBounds.y + (dontShowBounds.height - iconSize) / 2;

		g.setColor(hoveredControl == Control.DONT_SHOW ? TITLE_COLOR : TEXT_COLOR);
		g.drawString(text, startX, textY);
		g.setColor(PARCHMENT_LIGHT);
		g.fillRoundRect(iconX, iconY, iconSize, iconSize, 4, 4);
		g.setColor(PARCHMENT_EDGE);
		g.drawRoundRect(iconX, iconY, iconSize - 1, iconSize - 1, 4, 4);
		if (dontShowSelected)
		{
			g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(new Color(52, 96, 35));
			g.drawLine(iconX + 4, iconY + 9, iconX + 8, iconY + 13);
			g.drawLine(iconX + 8, iconY + 13, iconX + 14, iconY + 5);
		}
	}

	private void drawCloseButton(Graphics2D g)
	{
		Color top = hoveredControl == Control.CLOSE ? new Color(151, 111, 54) : new Color(130, 94, 47);
		Color bottom = pressedControl == Control.CLOSE ? new Color(87, 59, 28) : new Color(99, 68, 33);
		g.setPaint(new GradientPaint(closeBounds.x, closeBounds.y, top, closeBounds.x, closeBounds.y + closeBounds.height, bottom));
		g.fillRoundRect(closeBounds.x, closeBounds.y, closeBounds.width, closeBounds.height, 8, 8);
		g.setColor(new Color(66, 43, 20));
		g.drawRoundRect(closeBounds.x, closeBounds.y, closeBounds.width - 1, closeBounds.height - 1, 8, 8);
		g.setColor(new Color(255, 233, 174, 85));
		g.drawLine(closeBounds.x + 5, closeBounds.y + 2, closeBounds.x + closeBounds.width - 6, closeBounds.y + 2);

		g.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
		FontMetrics metrics = g.getFontMetrics();
		String text = "Close";
		g.setColor(PARCHMENT_LIGHT);
		g.drawString(
			text,
			closeBounds.x + (closeBounds.width - metrics.stringWidth(text)) / 2,
			closeBounds.y + (closeBounds.height - metrics.getHeight()) / 2 + metrics.getAscent());
	}

	private void drawCenteredText(Graphics2D g, String text, int x, int width, int y, Color color)
	{
		FontMetrics metrics = g.getFontMetrics();
		g.setColor(color);
		g.drawString(text, x + (width - metrics.stringWidth(text)) / 2, y);
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

	private Control controlAt(Point point)
	{
		if (!visible || point == null)
		{
			return Control.NONE;
		}

		Point local = new Point(point.x - getBounds().x, point.y - getBounds().y);
		if (closeBounds.contains(local))
		{
			return Control.CLOSE;
		}
		if (dontShowBounds.contains(local))
		{
			return Control.DONT_SHOW;
		}
		if (discordBounds.contains(local))
		{
			return Control.DISCORD;
		}
		return Control.NONE;
	}

	private void activate(Control control)
	{
		switch (control)
		{
			case CLOSE:
				Runnable dismiss = dismissAction;
				dismiss.run();
				hidePanel();
				break;
			case DONT_SHOW:
				dontShowSelected = true;
				Runnable disable = disableAction;
				disable.run();
				hidePanel();
				break;
			case DISCORD:
				LinkBrowser.browse(DISCORD_URL);
				break;
			default:
				break;
		}
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		if (!visible || event.getButton() != MouseEvent.BUTTON1)
		{
			return event;
		}

		Control control = controlAt(event.getPoint());
		if (control != Control.NONE)
		{
			pressedControl = control;
			suppressNextClick = true;
			event.consume();
		}
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		if (pressedControl == Control.NONE)
		{
			return event;
		}

		Control pressed = pressedControl;
		pressedControl = Control.NONE;
		event.consume();
		if (event.getButton() == MouseEvent.BUTTON1 && controlAt(event.getPoint()) == pressed)
		{
			activate(pressed);
		}
		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		if (suppressNextClick)
		{
			suppressNextClick = false;
			event.consume();
		}
		return event;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		hoveredControl = controlAt(event.getPoint());
		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		if (pressedControl != Control.NONE)
		{
			event.consume();
		}
		return event;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent event)
	{
		hoveredControl = Control.NONE;
		return event;
	}

	private enum Control
	{
		NONE,
		CLOSE,
		DONT_SHOW,
		DISCORD
	}
}
