package com.arber.pvmtools;

import java.awt.Canvas;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.Icon;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.ui.FontManager;

@Singleton
final class PvmToolsUpdatePanel extends JPanel
{
	private static final Color PARCHMENT = new Color(213, 194, 145);
	private static final Color PARCHMENT_LIGHT = new Color(239, 224, 174);
	private static final Color PARCHMENT_DARK = new Color(117, 87, 43);
	private static final Color PARCHMENT_EDGE = new Color(93, 66, 31);
	private static final Color TEXT_COLOR = new Color(47, 34, 20);
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
	private static final int CONTROL_SECTION_HEIGHT = 62;

	private final JButton closeButton = new ScrollButton("Close");
	private final JCheckBox dontShowCheckBox = new JCheckBox("Don't show update notes again");
	private final ScrollRoll topRoll = new ScrollRoll();
	private final ScrollRoll bottomRoll = new ScrollRoll();
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
		setName("PvM Toolkit update scroll");

		closeButton.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
		closeButton.setForeground(PARCHMENT_LIGHT);
		closeButton.setBorder(BorderFactory.createEmptyBorder());
		closeButton.setToolTipText("Close update notes");
		closeButton.setFocusPainted(false);
		closeButton.setFocusable(false);
		closeButton.setOpaque(false);
		closeButton.addActionListener(event -> dismissFromButton());
		add(closeButton);

		dontShowCheckBox.setFont(FontManager.getRunescapeFont().deriveFont(15f));
		dontShowCheckBox.setForeground(TEXT_COLOR);
		dontShowCheckBox.setToolTipText("Disable future PvM Toolkit update notes");
		dontShowCheckBox.setFocusPainted(false);
		dontShowCheckBox.setFocusable(false);
		dontShowCheckBox.setOpaque(false);
		dontShowCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		dontShowCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
		dontShowCheckBox.setIconTextGap(10);
		ScrollCheckIcon checkIcon = new ScrollCheckIcon();
		dontShowCheckBox.setIcon(checkIcon);
		dontShowCheckBox.setSelectedIcon(checkIcon);
		dontShowCheckBox.addActionListener(event ->
		{
			if (dontShowCheckBox.isSelected())
			{
				disableFromCheckBox();
			}
		});
		add(dontShowCheckBox);

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
		dontShowCheckBox.setSelected(false);

		layeredPane.add(this, JLayeredPane.POPUP_LAYER);
		layeredPane.add(topRoll, JLayeredPane.POPUP_LAYER);
		layeredPane.add(bottomRoll, JLayeredPane.POPUP_LAYER);
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
			parent.remove(topRoll);
			parent.remove(bottomRoll);
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
		int controlsWidth = DONT_SHOW_WIDTH + CONTROL_GAP + CLOSE_WIDTH;
		int controlsX = (getWidth() - controlsWidth) / 2;
		int controlsY = getHeight() - 45;
		dontShowCheckBox.setBounds(
			controlsX,
			controlsY,
			DONT_SHOW_WIDTH,
			CONTROL_HEIGHT);
		closeButton.setBounds(
			controlsX + DONT_SHOW_WIDTH + CONTROL_GAP,
			controlsY,
			CLOSE_WIDTH,
			CONTROL_HEIGHT);
	}

	@Override
	public boolean contains(int x, int y)
	{
		// Keep the decorative scroll click-through while its two controls remain interactive.
		return closeButton.getBounds().contains(x, y)
			|| dontShowCheckBox.getBounds().contains(x, y);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			drawScrollBody(g);
			drawContent(g);
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
		setBounds(
			x + ROLL_OVERHANG,
			y + ROLL_HEIGHT / 2,
			width - ROLL_OVERHANG * 2,
			height - ROLL_HEIGHT);
		topRoll.setBounds(x, y, width, ROLL_HEIGHT);
		bottomRoll.setBounds(x, y + height - ROLL_HEIGHT, width, ROLL_HEIGHT);
		setVisible(true);
		topRoll.setVisible(true);
		bottomRoll.setVisible(true);
		revalidate();
		repaint();
		topRoll.repaint();
		bottomRoll.repaint();
	}

	private void drawScrollBody(Graphics2D g)
	{
		g.setPaint(new GradientPaint(
			0,
			0,
			PARCHMENT_LIGHT,
			0,
			getHeight(),
			PARCHMENT));
		g.fillRect(0, 0, getWidth(), getHeight());

		g.setStroke(new BasicStroke(1.2f));
		g.setColor(new Color(PARCHMENT_EDGE.getRed(), PARCHMENT_EDGE.getGreen(), PARCHMENT_EDGE.getBlue(), 165));
		g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
		g.setColor(new Color(255, 245, 205, 105));
		g.drawRect(8, 8, getWidth() - 17, getHeight() - 17);

		int dividerY = getHeight() - CONTROL_SECTION_HEIGHT;
		g.setColor(new Color(PARCHMENT_EDGE.getRed(), PARCHMENT_EDGE.getGreen(), PARCHMENT_EDGE.getBlue(), 75));
		g.drawLine(30, dividerY, getWidth() - 31, dividerY);
		g.setColor(new Color(255, 244, 201, 95));
		g.drawLine(31, dividerY + 1, getWidth() - 32, dividerY + 1);
	}

	private void drawContent(Graphics2D g)
	{
		int contentX = CONTENT_INSET;
		int contentWidth = getWidth() - CONTENT_INSET * 2;
		int y = 48;

		Font titleFont = FontManager.getRunescapeBoldFont().deriveFont(25f);
		Font subtitleFont = FontManager.getRunescapeFont().deriveFont(16f);
		Font sectionFont = FontManager.getRunescapeBoldFont().deriveFont(17f);
		Font bodyFont = FontManager.getRunescapeFont().deriveFont(17f);

		g.setFont(titleFont);
		drawCenteredText(g, "PvM Toolkit Update", y, TITLE_COLOR);
		y += 29;

		g.setFont(subtitleFont);
		String versionLabel = "Version " + version;
		FontMetrics subtitleMetrics = g.getFontMetrics();
		int badgeWidth = subtitleMetrics.stringWidth(versionLabel) + 24;
		int badgeX = (getWidth() - badgeWidth) / 2;
		g.setColor(new Color(132, 88, 34, 32));
		g.fillRoundRect(badgeX, y - 17, badgeWidth, 23, 12, 12);
		g.setColor(new Color(126, 84, 33, 85));
		g.drawRoundRect(badgeX, y - 17, badgeWidth, 23, 12, 12);
		drawCenteredText(g, versionLabel, y, GOLD_COLOR);
		y += 37;

		g.setFont(sectionFont);
		FontMetrics sectionMetrics = g.getFontMetrics();
		String sectionTitle = "WHAT'S NEW";
		int sectionTitleWidth = sectionMetrics.stringWidth(sectionTitle);
		int sectionCenter = getWidth() / 2;
		int lineGap = 12;
		g.setColor(new Color(PARCHMENT_EDGE.getRed(), PARCHMENT_EDGE.getGreen(), PARCHMENT_EDGE.getBlue(), 90));
		g.drawLine(contentX, y - 5, sectionCenter - sectionTitleWidth / 2 - lineGap, y - 5);
		g.drawLine(sectionCenter + sectionTitleWidth / 2 + lineGap, y - 5, contentX + contentWidth, y - 5);
		drawCenteredText(g, sectionTitle, y, GOLD_COLOR);
		y += sectionMetrics.getHeight() + 5;

		g.setFont(bodyFont);
		for (String note : notes)
		{
			int bulletX = contentX + 3;
			int textX = contentX + 20;
			List<String> lines = wrapText(g, note, contentWidth - 20);
			g.setColor(GOLD_COLOR);
			g.fillOval(bulletX, y - 9, 6, 6);
			g.setColor(TEXT_COLOR);
			for (String line : lines)
			{
				if (y > getHeight() - CONTROL_SECTION_HEIGHT - 12)
				{
					break;
				}
				g.drawString(line, textX, y);
				y += g.getFontMetrics().getHeight();
			}
			y += 5;
		}
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

	private void disableFromCheckBox()
	{
		Runnable callback = disableAction;
		callback.run();
		hidePanel();
	}

	private static final class ScrollRoll extends JPanel
	{
		private ScrollRoll()
		{
			setOpaque(false);
			setFocusable(false);
		}

		@Override
		public void setBounds(int x, int y, int width, int height)
		{
			super.setBounds(x, y, width, height);
			setMixingCutoutShape(new RoundRectangle2D.Float(0, 0, width, height, height, height));
		}

		@Override
		public boolean contains(int x, int y)
		{
			return false;
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			try
			{
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(new Color(0, 0, 0, 55));
				g.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 3, getHeight(), getHeight());
				g.setPaint(new LinearGradientPaint(
					0,
					0,
					0,
					getHeight(),
					new float[]{0f, 0.28f, 0.62f, 1f},
					new Color[]{PARCHMENT_DARK, PARCHMENT_LIGHT, PARCHMENT, new Color(91, 65, 31)}));
				g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 4, getHeight(), getHeight());
				g.setColor(new Color(255, 246, 207, 125));
				g.drawRoundRect(4, 3, getWidth() - 9, getHeight() - 9, getHeight() - 6, getHeight() - 6);
				g.setColor(new Color(67, 45, 22, 165));
				g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ROLL_HEIGHT, ROLL_HEIGHT);
				g.setColor(new Color(80, 54, 25, 80));
				g.drawArc(9, 4, ROLL_HEIGHT - 9, getHeight() - 9, 90, 180);
				g.drawArc(getWidth() - ROLL_HEIGHT, 4, ROLL_HEIGHT - 9, getHeight() - 9, -90, 180);
			}
			finally
			{
				g.dispose();
			}
		}
	}

	private static final class ScrollButton extends JButton
	{
		private ScrollButton(String text)
		{
			super(text);
			setRolloverEnabled(true);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			try
			{
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Color top = getModel().isRollover() ? new Color(151, 111, 54) : new Color(130, 94, 47);
				Color bottom = getModel().isPressed() ? new Color(87, 59, 28) : new Color(99, 68, 33);
				g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
				g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
				g.setColor(new Color(66, 43, 20));
				g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
				g.setColor(new Color(255, 233, 174, 85));
				g.drawLine(5, 2, getWidth() - 6, 2);

				FontMetrics metrics = g.getFontMetrics(getFont());
				g.setFont(getFont());
				g.setColor(getForeground());
				g.drawString(
					getText(),
					(getWidth() - metrics.stringWidth(getText())) / 2,
					(getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
			}
			finally
			{
				g.dispose();
			}
		}
	}

	private static final class ScrollCheckIcon implements Icon
	{
		private static final int SIZE = 18;

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			try
			{
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(new Color(246, 229, 178));
				g.fillRoundRect(x, y, SIZE, SIZE, 4, 4);
				g.setColor(PARCHMENT_EDGE);
				g.drawRoundRect(x, y, SIZE - 1, SIZE - 1, 4, 4);

				if (component instanceof JCheckBox && ((JCheckBox) component).isSelected())
				{
					g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					g.setColor(new Color(52, 96, 35));
					g.drawLine(x + 4, y + 9, x + 8, y + 13);
					g.drawLine(x + 8, y + 13, x + 14, y + 5);
				}
			}
			finally
			{
				g.dispose();
			}
		}

		@Override
		public int getIconWidth()
		{
			return SIZE;
		}

		@Override
		public int getIconHeight()
		{
			return SIZE;
		}
	}
}
