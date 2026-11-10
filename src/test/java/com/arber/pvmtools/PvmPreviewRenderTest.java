package com.arber.pvmtools;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.config.Range;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayUtil;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Offscreen preview renderer for the side panel and the screen overlays.
 *
 * Skipped unless the PVM_PREVIEW_DIR environment variable names an output directory. It never opens a window:
 * components are laid out by hand and painted into images. Every picture is written twice, as name.png (1x) and
 * name@2x.png (the same pixels enlarged, so nothing can differ between the two). preview-info.txt lists the
 * files, every visible text and the text that does not fit.
 */
public class PvmPreviewRenderTest
{
	private static final String PREVIEW_DIR_ENV = "PVM_PREVIEW_DIR";
	private static final int SIDEBAR_WIDTH = PluginPanel.PANEL_WIDTH;
	private static final int SIDEBAR_VISIBLE_HEIGHT = 800;
	private static final int GAME_WIDTH = 765;
	private static final int GAME_HEIGHT = 503;
	private static final Color MID_GREY = new Color(128, 128, 128);
	private static final Color GAME_YELLOW = new Color(255, 255, 0);
	private static final Color GRASS_TOP = new Color(66, 114, 54);
	private static final Color EARTH_BOTTOM = new Color(116, 88, 52);
	private static final Color CHAT_BACKGROUND = new Color(52, 45, 34);

	private static final int COINS = 995;
	private static final int VILE_ASHES = 25769;
	private static final int ABYSSAL_ASHES = 25775;
	private static final int INFERNAL_ASHES = 25778;
	private static final int ASHES = 592;
	private static final int FIRE_RUNE = 554;
	private static final int CHAOS_RUNE = 562;
	private static final int DEATH_RUNE = 560;
	private static final int BLOOD_RUNE = 565;
	private static final int LAW_RUNE = 563;
	private static final int SOUL_RUNE = 566;
	private static final int SMOKE_RUNE = 4697;
	private static final int RUNE_PLATEBODY = 1127;
	private static final int RUNE_CHAINBODY = 1113;
	private static final int RUNE_KITESHIELD = 1201;
	private static final int DRAGON_DAGGER = 1215;
	private static final int BATTLESTAFF = 1391;
	private static final int RUNE_ARROW = 892;
	private static final int FIRE_ORB = 569;
	private static final int MAGIC_SHORTBOW_U = 72;
	private static final int MANTA_RAY = 391;
	private static final int PRAYER_POTION_4 = 2434;
	private static final int SMOULDERING_PILE_OF_FLESH = 30016;
	private static final int SMOULDERING_GLAND = 30019;
	private static final int GRIMY_RANARR_WEED = 207;
	private static final int ABYSSAL_WHIP = 4151;
	private static final int RUNITE_BAR = 2363;
	private static final int MAGIC_LOGS = 1513;
	private static final int DIAMOND = 1601;
	private static final int SANFEW_SERUM_4 = 10925;
	private static final int OCCULT_NECKLACE = 12002;
	private static final int DRAGON_BOOTS = 11840;
	private static final int ANCIENT_CEREMONIAL_TOP = 26221;
	private static final int DRAGON_BONES = 536;
	private static final int DRAGON_PLATELEGS = 4087;
	private static final int DRAGON_PLATESKIRT = 4585;
	private static final Map<Integer, String> ITEM_NAMES = new LinkedHashMap<>();

	static
	{
		ITEM_NAMES.put(COINS, "Coins");
		ITEM_NAMES.put(VILE_ASHES, "Vile ashes");
		ITEM_NAMES.put(ABYSSAL_ASHES, "Abyssal ashes");
		ITEM_NAMES.put(INFERNAL_ASHES, "Infernal ashes");
		ITEM_NAMES.put(ASHES, "Ashes");
		ITEM_NAMES.put(FIRE_RUNE, "Fire rune");
		ITEM_NAMES.put(CHAOS_RUNE, "Chaos rune");
		ITEM_NAMES.put(DEATH_RUNE, "Death rune");
		ITEM_NAMES.put(BLOOD_RUNE, "Blood rune");
		ITEM_NAMES.put(LAW_RUNE, "Law rune");
		ITEM_NAMES.put(SOUL_RUNE, "Soul rune");
		ITEM_NAMES.put(SMOKE_RUNE, "Smoke rune");
		ITEM_NAMES.put(RUNE_PLATEBODY, "Rune platebody");
		ITEM_NAMES.put(RUNE_CHAINBODY, "Rune chainbody");
		ITEM_NAMES.put(RUNE_KITESHIELD, "Rune kiteshield");
		ITEM_NAMES.put(DRAGON_DAGGER, "Dragon dagger");
		ITEM_NAMES.put(BATTLESTAFF, "Battlestaff");
		ITEM_NAMES.put(RUNE_ARROW, "Rune arrow");
		ITEM_NAMES.put(FIRE_ORB, "Fire orb");
		ITEM_NAMES.put(MAGIC_SHORTBOW_U, "Magic shortbow (u)");
		ITEM_NAMES.put(MANTA_RAY, "Manta ray");
		ITEM_NAMES.put(PRAYER_POTION_4, "Prayer potion(4)");
		ITEM_NAMES.put(SMOULDERING_PILE_OF_FLESH, "Smouldering pile of flesh");
		ITEM_NAMES.put(SMOULDERING_GLAND, "Smouldering gland");
		ITEM_NAMES.put(GRIMY_RANARR_WEED, "Grimy ranarr weed");
		ITEM_NAMES.put(ABYSSAL_WHIP, "Abyssal whip");
		ITEM_NAMES.put(RUNITE_BAR, "Runite bar");
		ITEM_NAMES.put(MAGIC_LOGS, "Magic logs");
		ITEM_NAMES.put(DIAMOND, "Diamond");
		ITEM_NAMES.put(SANFEW_SERUM_4, "Sanfew serum(4)");
		ITEM_NAMES.put(OCCULT_NECKLACE, "Occult necklace");
		ITEM_NAMES.put(DRAGON_BOOTS, "Dragon boots");
		ITEM_NAMES.put(ANCIENT_CEREMONIAL_TOP, "Ancient ceremonial top");
		ITEM_NAMES.put(DRAGON_BONES, "Dragon bones");
		ITEM_NAMES.put(DRAGON_PLATELEGS, "Dragon platelegs");
		ITEM_NAMES.put(DRAGON_PLATESKIRT, "Dragon plateskirt");
	}

	private static final Map<String, List<String>> INFO = new TreeMap<>();
	private static final List<String> WRITTEN_FILES = new ArrayList<>();
	private static LookAndFeel previousLookAndFeel;
	private static String lookAndFeelStatus;

	@Test
	public void mainViewWithRichDataInSimpleAndAdvancedMode() throws Exception
	{
		Assume.assumeTrue(System.getenv("PVM_PREVIEW_DIR") != null);
		ensureLookAndFeel();
		for (ToolkitPanelMode mode : ToolkitPanelMode.values())
		{
			PanelFixture fixture = new PanelFixture(richSample(), mode, true);
			fixture.click(PvmToolsStatsPeriod.ALL_TIME.getDisplayName());
			fixture.write("panel-" + mode.name().toLowerCase(Locale.ENGLISH));
		}
	}

	@Test
	public void mainViewForBrandNewUser() throws Exception
	{
		Assume.assumeTrue(System.getenv("PVM_PREVIEW_DIR") != null);
		ensureLookAndFeel();
		new PanelFixture(emptySample(), ToolkitPanelMode.SIMPLE, false).write("panel-empty-simple");
	}

	@Test
	public void combatLootLogView() throws Exception
	{
		Assume.assumeTrue(System.getenv("PVM_PREVIEW_DIR") != null);
		ensureLookAndFeel();
		PanelFixture fixture = new PanelFixture(richSample(), ToolkitPanelMode.ADVANCED, true);
		fixture.click(PvmToolsStatsPeriod.ALL_TIME.getDisplayName());
		fixture.click("Open Combat Loot Log");
		fixture.write("panel-lootlog");
	}

	@Test
	public void slayerLogView() throws Exception
	{
		Assume.assumeTrue(System.getenv("PVM_PREVIEW_DIR") != null);
		ensureLookAndFeel();
		PanelFixture fixture = new PanelFixture(richSample(), ToolkitPanelMode.ADVANCED, true);
		fixture.click(PvmToolsStatsPeriod.ALL_TIME.getDisplayName());
		fixture.click("Open Slayer Log");
		fixture.write("panel-slayerlog");
	}

	@Test
	public void updateScrollOnFixedAndLargeCanvas() throws Exception
	{
		Assume.assumeTrue(System.getenv("PVM_PREVIEW_DIR") != null);
		ensureLookAndFeel();
		renderUpdateScroll("update-scroll-fixed", GAME_WIDTH, GAME_HEIGHT);
		renderUpdateScroll("update-scroll-large", 1600, 900);
	}

	@Test
	public void screenWarningAtDefaultAndMaximumIntensity() throws Exception
	{
		Assume.assumeTrue(System.getenv("PVM_PREVIEW_DIR") != null);
		ensureLookAndFeel();
		PvmToolsConfig defaults = mock(PvmToolsConfig.class, CALLS_REAL_METHODS);
		int defaultIntensity = defaults.warningIntensity();
		Range range = PvmToolsConfig.class.getMethod("warningIntensity").getAnnotation(Range.class);
		assertNotNull("warningIntensity lost its @Range", range);

		BufferedImage background = new BufferedImage(GAME_WIDTH, GAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = background.createGraphics();
		try
		{
			paintSyntheticGame(graphics);
		}
		finally
		{
			graphics.dispose();
		}
		save(background, null, "warning-background");
		info("warning-background").add("synthetic " + GAME_WIDTH + " x " + GAME_HEIGHT + " game frame without any overlay, for comparison");
		renderWarning("warning-default", defaultIntensity, background, "default Warning redness");
		renderWarning("warning-max", range.max(), background, "maximum Warning redness");
	}

	@AfterClass
	public static void writeInfoAndRestoreLookAndFeel() throws Exception
	{
		if (lookAndFeelStatus == null)
		{
			return;
		}

		List<String> lines = new ArrayList<>();
		lines.add("PvM Toolkit offscreen preview");
		lines.add("look and feel: " + lookAndFeelStatus);
		lines.add("java " + System.getProperty("java.version") + " on " + System.getProperty("os.name"));
		lines.add("name@2x.png is name.png enlarged 2x without smoothing");
		lines.add("");
		lines.add("files written:");
		synchronized (WRITTEN_FILES)
		{
			lines.addAll(WRITTEN_FILES);
		}
		synchronized (INFO)
		{
			for (Map.Entry<String, List<String>> section : INFO.entrySet())
			{
				lines.add("");
				lines.add("=== " + section.getKey());
				lines.addAll(section.getValue());
			}
		}
		Path file = previewDirectory().resolve("preview-info.txt");
		Files.createDirectories(file.getParent());
		Files.write(file, lines, StandardCharsets.UTF_8);
		System.out.println("PvM Toolkit preview written to " + file.getParent().toAbsolutePath());

		onEdt(() ->
		{
			if (previousLookAndFeel != null)
			{
				UIManager.setLookAndFeel(previousLookAndFeel);
			}
			return null;
		});
	}

	private static synchronized void ensureLookAndFeel() throws Exception
	{
		if (lookAndFeelStatus != null)
		{
			return;
		}

		onEdt(() ->
		{
			previousLookAndFeel = UIManager.getLookAndFeel();
			try
			{
				// The client calls exactly this before it builds any Swing component.
				Object applied = Class.forName("net.runelite.client.ui.laf.RuneLiteLAF").getMethod("setup").invoke(null);
				lookAndFeelStatus = Boolean.TRUE.equals(applied)
					? "RuneLiteLAF.setup() applied, UIManager look and feel is \"" + UIManager.getLookAndFeel().getName() + "\""
					: "RuneLiteLAF.setup() returned " + applied + ", using \"" + UIManager.getLookAndFeel().getName() + "\"";
			}
			catch (ReflectiveOperationException | LinkageError | RuntimeException exception)
			{
				lookAndFeelStatus = "RuneLite look and feel NOT applied (" + exception + "), using default \""
					+ UIManager.getLookAndFeel().getName() + "\"";
			}
			return null;
		});
	}

	private static final class PanelFixture
	{
		private final PvmToolsPlugin plugin = mock(PvmToolsPlugin.class);
		private final PvmToolsConfig config = mock(PvmToolsConfig.class, CALLS_REAL_METHODS);
		private final Sample sample;
		private final String setup;
		private final PvmToolsStatsPanel panel;

		private PanelFixture(Sample sample, ToolkitPanelMode mode, boolean someTogglesOff) throws Exception
		{
			this.sample = sample;
			this.setup = "panel mode " + mode + ", quick toggles "
				+ (someTogglesOff ? "at their defaults except Superior and Inv. spaces (off)" : "at their defaults");

			// The real plugin getters run against the real config defaults; only the listed settings are changed.
			setField(plugin, PvmToolsPlugin.class, "config", config);
			doReturn(mode).when(config).panelMode();
			if (someTogglesOff)
			{
				doReturn(false).when(config).flashSuperiorSpawns();
				doReturn(false).when(config).superiorExamineHints();
				doReturn(false).when(config).showInventorySpaces();
			}
			when(plugin.isAdvancedPanelMode()).thenCallRealMethod();
			when(plugin.isForeverTrackerMode()).thenCallRealMethod();
			when(plugin.getDropAlertThresholdText()).thenCallRealMethod();
			when(plugin.isPotionTimersQuickEnabled()).thenCallRealMethod();
			when(plugin.isScreenWarningsQuickEnabled()).thenCallRealMethod();
			when(plugin.isDropAlertQuickEnabled()).thenCallRealMethod();
			when(plugin.isCannonWarningsQuickEnabled()).thenCallRealMethod();
			when(plugin.isSuperiorAlertsQuickEnabled()).thenCallRealMethod();
			when(plugin.isSlayerTrackingQuickEnabled()).thenCallRealMethod();
			when(plugin.isInventorySpacesQuickEnabled()).thenCallRealMethod();
			when(plugin.isChatTrackersQuickEnabled()).thenCallRealMethod();
			when(plugin.getItemDisplayName(anyInt())).thenAnswer(call ->
				ITEM_NAMES.getOrDefault(call.<Integer>getArgument(0), "Unknown item"));
			doAnswer(call ->
			{
				JLabel label = call.getArgument(0);
				int itemId = call.getArgument(1);
				int quantity = call.getArgument(2);
				label.setIcon(new ImageIcon(placeholderItemImage(itemId, quantity)));
				return null;
			}).when(plugin).addItemImageTo(any(JLabel.class), anyInt(), anyInt());
			// Like the plugin, answer every request with a detached snapshot; the panel applies it on the EDT.
			doAnswer(call ->
			{
				PvmToolsStatsPeriod period = call.getArgument(0);
				Consumer<PvmToolsPanelSnapshot> callback = call.getArgument(1);
				callback.accept(this.sample.snapshot(period));
				return null;
			}).when(plugin).capturePanelSnapshot(any(PvmToolsStatsPeriod.class), any());

			panel = onEdt(() -> new PvmToolsStatsPanel(plugin));
			flushEdt();
		}

		private void click(String buttonText) throws Exception
		{
			onEdt(() ->
			{
				JButton button = findButton(panel, buttonText);
				assertNotNull("The panel shows no \"" + buttonText + "\" button", button);
				button.doClick(0);
				return null;
			});
			flushEdt();
		}

		private void write(String name) throws Exception
		{
			onEdt(() ->
			{
				List<String> info = info(name);
				info.add("sample setup: " + setup);
				int viewportWidth = measureSidebarViewportWidth(info);
				renderAt(name, SIDEBAR_WIDTH, null, info, true);
				if (viewportWidth != SIDEBAR_WIDTH)
				{
					renderAt(name, viewportWidth, "w" + viewportWidth, info, false);
				}
				return null;
			});
		}

		/** Lays out the real PluginPanel wrapper (scroll pane included) to see how wide RuneLite makes the panel. */
		private int measureSidebarViewportWidth(List<String> info)
		{
			JPanel wrapper = panel.getWrappedPanel();
			int wrapperWidth = wrapper.getPreferredSize().width;
			invalidateTree(wrapper);
			wrapper.setSize(wrapperWidth, SIDEBAR_VISIBLE_HEIGHT);
			layoutTree(wrapper);
			String scrollBar = "no scroll pane";
			if (wrapper.getComponentCount() > 0 && wrapper.getComponent(0) instanceof JScrollPane)
			{
				Component bar = ((JScrollPane) wrapper.getComponent(0)).getVerticalScrollBar();
				scrollBar = bar.isVisible() ? "vertical scroll bar " + bar.getWidth() + " px wide" : "no vertical scroll bar";
			}
			info.add("RuneLite's own PluginPanel wrapper, laid out at " + wrapperWidth + " x " + SIDEBAR_VISIBLE_HEIGHT
				+ " px, makes this panel " + panel.getWidth() + " px wide (" + scrollBar + ")");
			return panel.getWidth();
		}

		private void renderAt(String name, int width, String subDirectory, List<String> info, boolean listTexts) throws IOException
		{
			int height = settle(width);
			save(paint(panel), subDirectory, name);
			List<String> texts = new ArrayList<>();
			List<String> issues = new ArrayList<>();
			inspect(panel, panel, texts, issues);
			info.add("");
			info.add("--- laid out at " + width + " px: " + width + " x " + height + " px"
				+ (subDirectory == null ? "" : " (files in sub-directory " + subDirectory + ")"));
			info.add(issues.isEmpty() ? "fit check: nothing clipped, overlapping or low in contrast" : "fit check: " + issues.size() + " finding(s)");
			info.addAll(issues);
			if (listTexts)
			{
				info.add("visible texts, top to bottom:");
				info.addAll(texts);
			}
		}

		private int settle(int width)
		{
			int height = -1;
			for (int pass = 0; pass < 5; pass++)
			{
				invalidateTree(panel);
				int preferred = panel.getPreferredSize().height;
				panel.setSize(width, preferred);
				layoutTree(panel);
				// As on screen, the first paint tells wrapped HTML labels their real width; they then need a new height.
				paint(panel);
				if (preferred == height)
				{
					break;
				}
				height = preferred;
			}
			return height;
		}
	}

	private static BufferedImage paint(JComponent component)
	{
		BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			component.paint(graphics);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}

	private static JButton findButton(Container container, String text)
	{
		for (Component child : container.getComponents())
		{
			if (!child.isVisible())
			{
				continue;
			}
			if (child instanceof JButton && text.equals(((JButton) child).getText()))
			{
				return (JButton) child;
			}
			if (child instanceof Container)
			{
				JButton nested = findButton((Container) child, text);
				if (nested != null)
				{
					return nested;
				}
			}
		}
		return null;
	}

	private static void inspect(Component component, JComponent root, List<String> texts, List<String> issues)
	{
		if (!component.isVisible())
		{
			return;
		}

		Rectangle bounds = component == root
			? new Rectangle(0, 0, root.getWidth(), root.getHeight())
			: SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), root);
		String where = String.format(Locale.ENGLISH, "y=%4d x=%3d w=%3d h=%3d", bounds.y, bounds.x, bounds.width, bounds.height);
		if (component instanceof JLabel)
		{
			inspectLabel((JLabel) component, where, texts, issues);
		}
		else if (component instanceof AbstractButton)
		{
			inspectButton((AbstractButton) component, where, texts, issues);
		}
		if (!(component instanceof Container))
		{
			return;
		}

		Component[] children = ((Container) component).getComponents();
		for (int i = 0; i < children.length; i++)
		{
			Component child = children[i];
			if (!child.isVisible() || child.getWidth() <= 0 || child.getHeight() <= 0)
			{
				continue;
			}
			Rectangle childBounds = child.getBounds();
			if (childBounds.x < 0 || childBounds.y < 0
				|| childBounds.x + childBounds.width > component.getWidth()
				|| childBounds.y + childBounds.height > component.getHeight())
			{
				issues.add("OVERFLOW " + where + ": " + describe(child) + " is placed at x=" + childBounds.x + " y=" + childBounds.y
					+ " w=" + childBounds.width + " h=" + childBounds.height + " inside a " + component.getWidth() + " x "
					+ component.getHeight() + " px parent");
			}
			for (int j = i + 1; j < children.length; j++)
			{
				Component other = children[j];
				if (other.isVisible() && other.getWidth() > 0 && other.getHeight() > 0 && childBounds.intersects(other.getBounds()))
				{
					Rectangle shared = childBounds.intersection(other.getBounds());
					issues.add("OVERLAP " + where + ": " + describe(child) + " and " + describe(other) + " share "
						+ shared.width + " x " + shared.height + " px");
				}
			}
		}
		for (Component child : children)
		{
			inspect(child, root, texts, issues);
		}
	}

	private static void inspectLabel(JLabel label, String where, List<String> texts, List<String> issues)
	{
		String text = label.getText();
		Icon icon = label.isEnabled() ? label.getIcon() : label.getDisabledIcon();
		boolean hasText = text != null && !text.isEmpty();
		if (!hasText && icon == null)
		{
			return;
		}

		Insets insets = label.getInsets();
		int width = label.getWidth() - insets.left - insets.right;
		int height = label.getHeight() - insets.top - insets.bottom;
		if (icon != null && (icon.getIconWidth() > width || icon.getIconHeight() > height))
		{
			issues.add("ICON CUT " + where + ": a " + icon.getIconWidth() + " x " + icon.getIconHeight() + " px icon has "
				+ width + " x " + height + " px");
		}
		if (!hasText)
		{
			return;
		}

		String shown = plain(text);
		if (BasicHTML.isHTMLString(text))
		{
			inspectHtml(label, shown, width, height, where, issues);
		}
		else
		{
			FontMetrics metrics = label.getFontMetrics(label.getFont());
			String painted = SwingUtilities.layoutCompoundLabel(label, metrics, text, icon, label.getVerticalAlignment(),
				label.getHorizontalAlignment(), label.getVerticalTextPosition(), label.getHorizontalTextPosition(),
				new Rectangle(insets.left, insets.top, width, height), new Rectangle(), new Rectangle(), label.getIconTextGap());
			shown = reportClippedText(text, painted, metrics, width, height, where, issues);
		}
		Color background = label.isOpaque() ? label.getBackground() : backgroundBehind(label.getParent());
		texts.add(textLine(where, "label ", shown, label.getFont(), label.getForeground(), background, issues));
	}

	private static void inspectButton(AbstractButton button, String where, List<String> texts, List<String> issues)
	{
		String text = button.getText();
		if (text == null || text.isEmpty())
		{
			return;
		}

		Insets insets = button.getInsets();
		int width = button.getWidth() - insets.left - insets.right;
		int height = button.getHeight() - insets.top - insets.bottom;
		String shown = plain(text);
		if (BasicHTML.isHTMLString(text))
		{
			inspectHtml(button, shown, width, height, where, issues);
		}
		else
		{
			Icon icon = button.getIcon();
			if (icon == null && button instanceof JCheckBox)
			{
				icon = UIManager.getIcon("CheckBox.icon");
			}
			FontMetrics metrics = button.getFontMetrics(button.getFont());
			String painted = SwingUtilities.layoutCompoundLabel(button, metrics, text, icon, button.getVerticalAlignment(),
				button.getHorizontalAlignment(), button.getVerticalTextPosition(), button.getHorizontalTextPosition(),
				new Rectangle(insets.left, insets.top, width, height), new Rectangle(), new Rectangle(), button.getIconTextGap());
			shown = reportClippedText(text, painted, metrics, width, height, where, issues);
		}
		String kind = button instanceof JCheckBox ? (button.isSelected() ? "check [x] " : "check [ ] ") : "button ";
		texts.add(textLine(where, kind + (button.isEnabled() ? "" : "(disabled) "), shown, button.getFont(), button.getForeground(),
			button.getBackground(), issues));
	}

	private static void inspectHtml(JComponent component, String shown, int width, int height, String where, List<String> issues)
	{
		Object view = component.getClientProperty(BasicHTML.propertyKey);
		if (!(view instanceof View))
		{
			return;
		}
		int neededWidth = (int) Math.ceil(((View) view).getMinimumSpan(View.X_AXIS));
		int neededHeight = (int) Math.ceil(((View) view).getPreferredSpan(View.Y_AXIS));
		if (neededWidth > width || neededHeight > height)
		{
			issues.add("WRAPPED TEXT CUT " + where + ": \"" + shown + "\" needs " + neededWidth + " x " + neededHeight
				+ " px when wrapped, has " + width + " x " + height + " px");
		}
	}

	private static String reportClippedText(String text, String painted, FontMetrics metrics, int width, int height, String where,
		List<String> issues)
	{
		if (!painted.equals(text))
		{
			issues.add("ELLIPSIS " + where + ": \"" + text + "\" is painted as \"" + painted + "\" (needs "
				+ metrics.stringWidth(text) + " px, has " + width + " px)");
		}
		if (metrics.getAscent() + metrics.getDescent() > height)
		{
			issues.add("CUT VERTICALLY " + where + ": \"" + text + "\" needs " + (metrics.getAscent() + metrics.getDescent())
				+ " px of height, has " + height + " px");
		}
		return painted;
	}

	private static String textLine(String where, String kind, String shown, Font font, Color foreground, Color background,
		List<String> issues)
	{
		String colors = String.format(Locale.ENGLISH, "#%06X", foreground.getRGB() & 0xFFFFFF);
		if (background != null)
		{
			double contrast = contrast(foreground, background);
			colors += String.format(Locale.ENGLISH, " on #%06X, contrast %.1f:1", background.getRGB() & 0xFFFFFF, contrast);
			if (contrast < 4.5d)
			{
				issues.add("LOW CONTRAST " + where + ": \"" + shown + "\" is " + colors);
			}
		}
		return where + "  " + kind + "\"" + shown + "\"  [" + font.getFontName() + " " + font.getSize() + ", " + colors + "]";
	}

	private static Color backgroundBehind(Component component)
	{
		for (Component current = component; current != null; current = current.getParent())
		{
			if (current.isOpaque() && current.getBackground() != null)
			{
				return current.getBackground();
			}
		}
		return null;
	}

	private static String describe(Component component)
	{
		if (component instanceof JLabel)
		{
			return "label \"" + plain(((JLabel) component).getText()) + "\"";
		}
		if (component instanceof AbstractButton)
		{
			return "button \"" + plain(((AbstractButton) component).getText()) + "\"";
		}
		String text = firstText(component);
		return component.getClass().getSimpleName() + (text == null ? "" : " starting with \"" + text + "\"");
	}

	private static String firstText(Component component)
	{
		String own = null;
		if (component instanceof JLabel)
		{
			own = plain(((JLabel) component).getText());
		}
		else if (component instanceof AbstractButton)
		{
			own = plain(((AbstractButton) component).getText());
		}
		if (own != null && !own.isEmpty())
		{
			return own;
		}
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				String nested = firstText(child);
				if (nested != null)
				{
					return nested;
				}
			}
		}
		return null;
	}

	private static String plain(String text)
	{
		return text == null ? "" : text.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
	}

	private static void renderUpdateScroll(String name, int canvasWidth, int canvasHeight) throws Exception
	{
		String[] notes = (String[]) readField(null, PvmToolsPlugin.class, "UPDATE_SCROLL_NOTES");
		Canvas canvas = new Canvas();
		canvas.setSize(canvasWidth, canvasHeight);
		PvmToolsUpdatePanel panel = new PvmToolsUpdatePanel(
			() -> canvasWidth,
			() -> canvasHeight,
			overlay -> { },
			overlay -> { },
			listener -> { },
			listener -> { });
		assertTrue(name + ": the update scroll refused to open", panel.showPanel(canvas, pluginVersion(), notes, null, null));

		List<String> info = info(name);
		info.add(notes.length + " release notes on a " + canvasWidth + " x " + canvasHeight + " canvas over mid grey");
		while (true)
		{
			BufferedImage image = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = image.createGraphics();
			Dimension size;
			try
			{
				graphics.setColor(MID_GREY);
				graphics.fillRect(0, 0, canvasWidth, canvasHeight);
				size = renderOverlay(panel, graphics, canvasWidth, canvasHeight);
			}
			finally
			{
				graphics.dispose();
			}
			assertNotNull(name + ": the update scroll painted nothing", size);
			int page = panel.getNotePage();
			save(image, null, page == 0 ? name : name + "-page" + (page + 1));
			Rectangle bounds = panel.getBounds();
			info.add("page " + (page + 1) + " of " + panel.getNotePageCount() + ": scroll " + size.width + " x " + size.height
				+ " px at x=" + bounds.x + " y=" + bounds.y + ", " + panel.getRenderedNoteLines().size() + " text lines:");
			for (String line : panel.getRenderedNoteLines())
			{
				info.add("    " + line);
			}
			if (page + 1 >= panel.getNotePageCount())
			{
				break;
			}

			Rectangle next = (Rectangle) readField(panel, PvmToolsUpdatePanel.class, "nextNotesBounds");
			Point point = new Point(bounds.x + next.x + next.width / 2, bounds.y + next.y + next.height / 2);
			panel.mousePressed(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, point.x, point.y, 1, false, MouseEvent.BUTTON1));
			panel.mouseReleased(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, point.x, point.y, 1, false, MouseEvent.BUTTON1));
			assertTrue(name + ": the Next control did not turn the page", panel.getNotePage() == page + 1);
		}
		panel.hidePanel();
	}

	private static void renderWarning(String name, int intensity, BufferedImage background, String label) throws Exception
	{
		PvmToolsConfig config = mock(PvmToolsConfig.class, CALLS_REAL_METHODS);
		doReturn(intensity).when(config).warningIntensity();
		PvmToolsPlugin plugin = mock(PvmToolsPlugin.class);
		setField(plugin, PvmToolsPlugin.class, "config", config);
		// The plugin's own (private) intensity-to-alpha conversion.
		Method warningAlpha = PvmToolsPlugin.class.getDeclaredMethod("getWarningAlpha");
		warningAlpha.setAccessible(true);
		int alpha = (int) warningAlpha.invoke(plugin);
		// getScreenFlashColor() builds exactly this colour while a warning blink is on.
		Color flash = new Color(255, 0, 0, alpha);
		when(plugin.getScreenFlashColor()).thenReturn(flash);

		Canvas canvas = new Canvas();
		canvas.setSize(GAME_WIDTH, GAME_HEIGHT);
		Client client = mock(Client.class);
		when(client.getCanvas()).thenReturn(canvas);
		when(client.getCanvasWidth()).thenReturn(GAME_WIDTH);
		when(client.getCanvasHeight()).thenReturn(GAME_HEIGHT);
		Constructor<CombatPotionWarningOverlay> constructor =
			CombatPotionWarningOverlay.class.getDeclaredConstructor(Client.class, PvmToolsPlugin.class);
		constructor.setAccessible(true);
		CombatPotionWarningOverlay overlay = constructor.newInstance(client, plugin);

		BufferedImage image = new BufferedImage(GAME_WIDTH, GAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.drawImage(background, 0, 0, null);
			renderOverlay(overlay, graphics, GAME_WIDTH, GAME_HEIGHT);
		}
		finally
		{
			graphics.dispose();
		}
		save(image, null, name);

		// Measured from the painted pixels, so it holds for whatever shape the overlay paints.
		int untouched = 0;
		int light = 0;
		int heavy = 0;
		int hidden = 0;
		Rectangle clear = null;
		for (int y = 0; y < GAME_HEIGHT; y++)
		{
			for (int x = 0; x < GAME_WIDTH; x++)
			{
				double coverage = coverage(new Color(background.getRGB(x, y)), new Color(image.getRGB(x, y)), flash);
				if (Double.isNaN(coverage))
				{
					// The game pixel already had the flash colour, so it cannot show how strong the tint is.
					hidden++;
				}
				else if (coverage <= 0d)
				{
					untouched++;
					if (clear == null)
					{
						clear = new Rectangle(x, y, 1, 1);
					}
					else
					{
						clear.add(new Rectangle(x, y, 1, 1));
					}
				}
				else if (coverage < 0.5d)
				{
					light++;
				}
				else if (coverage < 0.97d)
				{
					heavy++;
				}
				else
				{
					hidden++;
				}
			}
		}
		double pixels = GAME_WIDTH * (double) GAME_HEIGHT;
		double spreadBefore = luminanceSpread(background);
		List<String> info = info(name);
		info.add(label + " = " + intensity + " of 100 -> flash colour red (255, 0, 0) with alpha " + alpha + " of 255 ("
			+ Math.round(alpha * 100d / 255d) + "% opaque at its strongest)");
		info.add(String.format(Locale.ENGLISH, "pixels of the %d x %d frame: %.1f%% untouched, %.1f%% tinted less than half, "
			+ "%.1f%% tinted half or more, %.1f%% completely red", GAME_WIDTH, GAME_HEIGHT, untouched * 100d / pixels,
			light * 100d / pixels, heavy * 100d / pixels, hidden * 100d / pixels));
		info.add(clear == null ? "no pixel is left untouched"
			: "untouched pixels lie within x=" + clear.x + ".." + (clear.x + clear.width - 1) + ", y=" + clear.y + ".."
				+ (clear.y + clear.height - 1));
		info.add(String.format(Locale.ENGLISH, "light/dark variation of the whole frame that survives: %.0f%%",
			spreadBefore <= 0d ? 0d : luminanceSpread(image) * 100d / spreadBefore));
	}

	/** How opaque the flash colour is over one pixel: 0 = untouched, 1 = nothing of the game left. */
	private static double coverage(Color before, Color after, Color flash)
	{
		double[] towardsFlash = {
			flash.getRed() - before.getRed(), flash.getGreen() - before.getGreen(), flash.getBlue() - before.getBlue()};
		double[] moved = {
			after.getRed() - before.getRed(), after.getGreen() - before.getGreen(), after.getBlue() - before.getBlue()};
		double length = towardsFlash[0] * towardsFlash[0] + towardsFlash[1] * towardsFlash[1] + towardsFlash[2] * towardsFlash[2];
		if (length <= 0d)
		{
			return Double.NaN;
		}
		if (before.getRGB() == after.getRGB())
		{
			return 0d;
		}
		double share = (moved[0] * towardsFlash[0] + moved[1] * towardsFlash[1] + moved[2] * towardsFlash[2]) / length;
		return Math.max(0d, Math.min(1d, share));
	}

	/** Positions and paints one overlay the way OverlayRenderer does for the ALWAYS_ON_TOP layer (RuneLite 1.13.6). */
	private static Dimension renderOverlay(Overlay overlay, Graphics2D target, int canvasWidth, int canvasHeight)
	{
		Graphics2D graphics = (Graphics2D) target.create();
		try
		{
			OverlayUtil.setGraphicProperties(graphics);
			graphics.setClip(0, 0, canvasWidth, canvasHeight);
			graphics.setFont(FontManager.getRunescapeSmallFont());
			Point preferred = overlay.getPreferredLocation();
			Point location = preferred != null ? new Point(preferred) : overlay.getBounds().getLocation();
			graphics.translate(location.x, location.y);
			overlay.getBounds().setLocation(location);
			Dimension size = overlay.render(graphics);
			overlay.getBounds().setSize(size == null ? new Dimension() : size);
			return size;
		}
		finally
		{
			graphics.dispose();
		}
	}

	/** A stand-in for the fixed-mode game frame: scene, minimap, inventory and chat box with in-game sized text. */
	private static void paintSyntheticGame(Graphics2D g)
	{
		Font chatFont = FontManager.getRunescapeFont();
		Font smallFont = FontManager.getRunescapeSmallFont();
		Font plain12 = new Font(Font.DIALOG, Font.PLAIN, 12);

		g.setColor(new Color(40, 35, 27));
		g.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

		Graphics2D scene = (Graphics2D) g.create(4, 4, 512, 334);
		try
		{
			scene.setPaint(new GradientPaint(0, 0, GRASS_TOP, 0, 334, EARTH_BOTTOM));
			scene.fillRect(0, 0, 512, 334);
			scene.setColor(new Color(156, 128, 84));
			scene.fillPolygon(
				new int[]{180, 250, 330, 420, 512, 512, 380, 290, 210, 120},
				new int[]{334, 250, 190, 150, 120, 170, 200, 240, 300, 334},
				10);
			scene.setColor(new Color(58, 104, 156));
			scene.fillOval(350, 28, 130, 58);
			scene.setColor(new Color(92, 138, 186));
			scene.drawOval(362, 38, 106, 38);
			int[][] trees = {{40, 80}, {112, 130}, {60, 226}, {236, 78}, {306, 52}, {472, 262}, {408, 300}};
			for (int[] tree : trees)
			{
				scene.setColor(new Color(84, 58, 34));
				scene.fillRect(tree[0] - 4, tree[1], 8, 24);
				scene.setColor(new Color(30, 78, 34));
				scene.fillOval(tree[0] - 24, tree[1] - 34, 48, 44);
				scene.setColor(new Color(44, 100, 44));
				scene.fillOval(tree[0] - 14, tree[1] - 30, 26, 22);
			}
			scene.setColor(new Color(120, 120, 116));
			scene.fillOval(150, 254, 34, 20);
			scene.fillOval(440, 194, 26, 16);
			scene.setColor(new Color(140, 40, 34));
			scene.fillRoundRect(270, 124, 46, 70, 16, 16);
			scene.setColor(new Color(200, 70, 50));
			scene.fillOval(280, 104, 26, 26);
			scene.setColor(new Color(200, 0, 0));
			scene.fillRect(278, 94, 30, 5);
			scene.setColor(new Color(0, 200, 0));
			scene.fillRect(278, 94, 19, 5);
			scene.setColor(new Color(52, 84, 170));
			scene.fillRoundRect(222, 200, 22, 42, 8, 8);
			scene.setColor(new Color(226, 190, 150));
			scene.fillOval(225, 184, 16, 16);
			shadowed(scene, chatFont, GAME_YELLOW, "Tormented Demon (level-450)", 212, 88);
			shadowed(scene, chatFont, Color.WHITE, "Player", 216, 178);
			shadowed(scene, chatFont, Color.WHITE, "Rune platebody (38,417 gp)", 330, 236);
			shadowed(scene, chatFont, GAME_YELLOW, "Infernal ashes", 352, 252);
			shadowed(scene, smallFont, Color.WHITE, "Greater demons: 11 / 195", 8, 18);
			shadowed(scene, smallFont, GAME_YELLOW, "Prayer 1:42   Antifire 3:10", 8, 32);
			shadowed(scene, plain12, GAME_YELLOW, "Yellow 12 px text: The quick brown fox 0123456789", 8, 304);
			shadowed(scene, plain12, Color.WHITE, "White 12 px text: The quick brown fox 0123456789", 8, 322);
		}
		finally
		{
			scene.dispose();
		}

		// Minimap with its orbs.
		g.setColor(new Color(58, 50, 38));
		g.fillRect(520, 0, 245, 168);
		g.setPaint(new GradientPaint(567, 8, new Color(72, 120, 60), 719, 160, new Color(120, 96, 58)));
		g.fillOval(567, 8, 152, 152);
		g.setColor(new Color(156, 128, 84));
		g.fillRect(640, 20, 8, 128);
		g.setColor(new Color(58, 104, 156));
		g.fillOval(668, 40, 34, 22);
		g.setColor(new Color(20, 18, 14));
		g.drawOval(567, 8, 152, 152);
		int[][] dots = {{620, 70, 0}, {655, 96, 1}, {690, 110, 2}, {612, 120, 1}, {676, 76, 2}, {643, 84, 0}};
		Color[] dotColors = {Color.WHITE, GAME_YELLOW, new Color(236, 44, 30)};
		for (int[] dot : dots)
		{
			g.setColor(dotColors[dot[2]]);
			g.fillRect(dot[0], dot[1], 4, 4);
		}
		g.setColor(new Color(90, 78, 60));
		g.fillOval(546, 4, 32, 32);
		shadowed(g, chatFont, Color.WHITE, "N", 558, 25);
		String[] orbTexts = {"99", "77", "100"};
		Color[] orbColors = {new Color(150, 24, 20), new Color(40, 130, 150), new Color(150, 130, 30)};
		for (int i = 0; i < orbTexts.length; i++)
		{
			int y = 50 + i * 36;
			g.setColor(new Color(30, 26, 20));
			g.fillRoundRect(524, y, 56, 30, 14, 14);
			g.setColor(orbColors[i]);
			g.fillOval(550, y + 2, 26, 26);
			shadowed(g, smallFont, i == 1 ? GAME_YELLOW : Color.WHITE, orbTexts[i], 528, y + 20);
		}

		// Inventory with a few stacks.
		g.setColor(new Color(58, 50, 38));
		g.fillRect(520, 168, 245, 335);
		g.setColor(new Color(72, 62, 46));
		g.fillRect(548, 205, 190, 261);
		for (int tab = 0; tab < 7; tab++)
		{
			g.setColor(new Color(92, 80, 60));
			g.fillRect(526 + tab * 34, 172, 30, 30);
			g.fillRect(526 + tab * 34, 469, 30, 30);
		}
		int[] stacks = {0, 1234, 0, 61700, 0, 0, 292, 0, 4, 0, 0, 0, 185, 0, 0, 2, 0, 0, 0, 0, 14, 0, 0, 0, 0, 0, 0, 0};
		for (int slot = 0; slot < 28; slot++)
		{
			int x = 556 + (slot % 4) * 44;
			int y = 212 + (slot / 4) * 36;
			if (slot % 3 != 2)
			{
				g.setColor(Color.getHSBColor((slot * 0.137f) % 1f, 0.55f, 0.82f));
				g.fillRoundRect(x + 4, y + 6, 26, 22, 8, 8);
			}
			if (stacks[slot] > 0)
			{
				shadowed(g, smallFont, GAME_YELLOW, Integer.toString(stacks[slot]), x, y + 10);
			}
		}

		// Chat box and its tabs.
		g.setColor(CHAT_BACKGROUND);
		g.fillRect(0, 338, 520, 142);
		g.setColor(new Color(90, 78, 60));
		g.drawRect(0, 338, 519, 141);
		String[] chat = {
			"Welcome to Old School RuneScape.",
			"You're assigned to kill greater demons; only 184 more to go.",
			"Your cannon is out of ammo!",
			"Valuable drop: Abyssal whip (1,420,000 coins)",
			"You drink some of your prayer potion.",
			"You have 3 doses of potion left.",
			"Player: *"
		};
		for (int i = 0; i < chat.length; i++)
		{
			shadowed(g, chatFont, i % 2 == 0 ? Color.WHITE : GAME_YELLOW, chat[i], 8, 356 + i * 17);
		}
		String[] tabs = {"All", "Game", "Public", "Private", "Channel", "Clan", "Trade"};
		for (int i = 0; i < tabs.length; i++)
		{
			g.setColor(new Color(72, 62, 46));
			g.fillRect(4 + i * 74, 482, 70, 19);
			shadowed(g, smallFont, i % 2 == 0 ? GAME_YELLOW : Color.WHITE, tabs[i], 12 + i * 74, 496);
		}
	}

	private static void shadowed(Graphics2D g, Font font, Color color, String text, int x, int y)
	{
		g.setFont(font);
		g.setColor(Color.BLACK);
		g.drawString(text, x + 1, y + 1);
		g.setColor(color);
		g.drawString(text, x, y);
	}

	/** Stands in for a 36 x 32 item sprite, with the stack size in the corner the way the game draws it. */
	private static BufferedImage placeholderItemImage(int itemId, int quantity)
	{
		BufferedImage image = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Color fill = Color.getHSBColor((itemId * 0.618034f) % 1f, 0.55f, 0.88f);
			int shape = Math.floorMod(itemId, 3);
			for (int outline = 1; outline >= 0; outline--)
			{
				g.setColor(outline == 1 ? fill.darker().darker() : fill);
				int grow = outline * 2;
				if (shape == 0)
				{
					g.fillRoundRect(8 - grow / 2, 9 - grow / 2, 22 + grow, 18 + grow, 7, 7);
				}
				else if (shape == 1)
				{
					g.fillOval(8 - grow / 2, 8 - grow / 2, 21 + grow, 21 + grow);
				}
				else
				{
					g.fillPolygon(new int[]{19, 31 + grow / 2, 19, 7 - grow / 2}, new int[]{7 - grow / 2, 19, 31 + grow / 2, 19}, 4);
				}
			}
			if (quantity > 1)
			{
				String stack = quantity >= 10_000_000 ? quantity / 1_000_000 + "M"
					: quantity >= 100_000 ? quantity / 1_000 + "K" : Integer.toString(quantity);
				Color stackColor = quantity >= 10_000_000 ? new Color(0, 255, 128) : quantity >= 100_000 ? Color.WHITE : GAME_YELLOW;
				g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
				shadowed(g, FontManager.getRunescapeSmallFont(), stackColor, stack, 0, 10);
			}
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	private static final class Sample
	{
		private final PvmToolsStats stats;
		private final PvmTaskSnapshot currentTask;
		private final List<PvmTaskHistoryEntry> history;
		private final String cannonEstimate;
		private final Set<String> excludedLoot;

		private Sample(PvmToolsStats stats, PvmTaskSnapshot currentTask, List<PvmTaskHistoryEntry> history, String cannonEstimate,
			Set<String> excludedLoot)
		{
			this.stats = stats;
			this.currentTask = currentTask;
			this.history = history;
			this.cannonEstimate = cannonEstimate;
			this.excludedLoot = excludedLoot;
		}

		private PvmToolsPanelSnapshot snapshot(PvmToolsStatsPeriod period)
		{
			return new PvmToolsPanelSnapshot(period, stats.copy(), currentTask, history, cannonEstimate, excludedLoot);
		}
	}

	private static final class SampleDrop
	{
		private final int itemId;
		private final long quantity;
		private final long unitValue;
		private final int every;
		private final int offset;

		private SampleDrop(int itemId, long quantity, long unitValue, int every, int offset)
		{
			this.itemId = itemId;
			this.quantity = quantity;
			this.unitValue = unitValue;
			this.every = every;
			this.offset = offset;
		}
	}

	private static Sample emptySample()
	{
		return new Sample(new PvmToolsStats(""), PvmTaskSnapshot.EMPTY, List.of(), PvmToolsPlugin.NO_CANNON_TEXT, Set.of());
	}

	/** A long-running account seen through the All-time period: big totals and long names on purpose. */
	private static Sample richSample()
	{
		long now = LocalDateTime.of(2026, 11, 10, 21, 43).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		long abyssalTaskFinished = now - Duration.ofDays(2).plusHours(3).plusMinutes(22).toMillis();
		long smokeDevilTaskFinished = now - Duration.ofDays(3).plusHours(22).plusMinutes(38).toMillis();
		long firstAbyssalTaskFinished = now - Duration.ofDays(6).plusMinutes(56).toMillis();
		PvmToolsStats stats = new PvmToolsStats("all");

		// Recent kills, as the Combat Loot Log records them: every kill is one addCombatLoot call.
		kills(stats, "Greater demon", 92, 11, now - 20_000L,
			drop(VILE_ASHES, 1, 2_104, 1, 0),
			drop(COINS, 132, 1, 2, 0),
			drop(FIRE_RUNE, 75, 5, 3, 0));
		stats.addCombatSupplyCost("Greater demon", 92, 64_870L);
		kills(stats, "Tormented Demon", 450, 234, now - Duration.ofDays(1).plusHours(2).plusMinutes(12).toMillis(),
			drop(INFERNAL_ASHES, 1, 3_950, 1, 0),
			drop(RUNE_PLATEBODY, 1, 38_417, 2, 0),
			drop(DRAGON_DAGGER, 1, 17_206, 3, 0),
			drop(BATTLESTAFF, 1, 7_912, 4, 1),
			drop(RUNE_KITESHIELD, 1, 31_884, 5, 0),
			drop(CHAOS_RUNE, 75, 61, 6, 1),
			drop(SOUL_RUNE, 60, 229, 7, 0),
			drop(RUNE_ARROW, 80, 52, 8, 3),
			drop(FIRE_ORB, 6, 1_184, 9, 0),
			drop(MAGIC_SHORTBOW_U, 1, 1_033, 10, 7),
			drop(MANTA_RAY, 2, 1_468, 12, 5),
			drop(PRAYER_POTION_4, 1, 9_480, 15, 0),
			drop(SMOULDERING_PILE_OF_FLESH, 1, 412_000, 26, 0),
			drop(SMOULDERING_GLAND, 1, 506_000, 39, 0));
		stats.addCombatSupplyCost("Tormented Demon", 450, 9_870_000L);
		kills(stats, "Abyssal demon", 124, 1_234, abyssalTaskFinished,
			drop(ABYSSAL_ASHES, 1, 499, 1, 0),
			drop(COINS, 132, 1, 2, 1),
			drop(RUNE_CHAINBODY, 1, 29_468, 4, 0),
			drop(BLOOD_RUNE, 7, 372, 5, 2),
			drop(LAW_RUNE, 3, 143, 9, 4),
			drop(GRIMY_RANARR_WEED, 1, 6_873, 11, 0),
			drop(ABYSSAL_WHIP, 1, 1_420_000, 617, 0));
		stats.addCombatSupplyCost("Abyssal demon", 124, 4_120_000L);
		kills(stats, "Thermonuclear smoke devil", 301, 96, smokeDevilTaskFinished,
			drop(ASHES, 1, 54, 1, 0),
			drop(SMOKE_RUNE, 100, 131, 2, 0),
			drop(RUNITE_BAR, 2, 12_150, 3, 0),
			drop(MAGIC_LOGS, 20, 1_005, 4, 1),
			drop(DIAMOND, 4, 1_712, 5, 0),
			drop(SANFEW_SERUM_4, 2, 21_890, 8, 0),
			drop(OCCULT_NECKLACE, 1, 512_340, 1_000, 57));
		stats.addCombatSupplyCost("Thermonuclear smoke devil", 301, 1_254_900L);
		kills(stats, "Spiritual mage", 121, 187, now - Duration.ofDays(5).plusHours(4).plusMinutes(9).toMillis(),
			drop(CHAOS_RUNE, 35, 61, 2, 1),
			drop(DEATH_RUNE, 15, 163, 2, 0),
			drop(DRAGON_BOOTS, 1, 81_330, 64, 0),
			drop(ANCIENT_CEREMONIAL_TOP, 1, 2_140_000, 1_000, 150));
		stats.addCombatSupplyCost("Spiritual mage", 121, 318_400L);
		long greaterDemonLoot = stats.getCombatLootSources().stream()
			.filter(source -> "Greater demon".equals(source.getName()))
			.mapToLong(PvmLootSourceStat::getTotalValue)
			.sum();

		// Loot picked up before the Combat Loot Log existed only counts towards the period totals.
		pickups(stats, ABYSSAL_ASHES, 8_642, 1, 499);
		pickups(stats, ABYSSAL_WHIP, 84, 1, 1_420_000);
		pickups(stats, COINS, 7_500, 14_000, 1);
		pickups(stats, DEATH_RUNE, 2_400, 250, 163);
		pickups(stats, BLOOD_RUNE, 2_300, 120, 372);
		pickups(stats, DRAGON_PLATELEGS, 620, 1, 162_300);
		pickups(stats, DRAGON_PLATESKIRT, 605, 1, 161_900);
		pickups(stats, RUNITE_BAR, 3_100, 3, 12_150);
		pickups(stats, GRIMY_RANARR_WEED, 4_300, 3, 6_873);
		pickups(stats, BATTLESTAFF, 4_100, 3, 7_912);
		pickups(stats, RUNE_PLATEBODY, 2_600, 1, 38_417);
		pickups(stats, MAGIC_LOGS, 3_300, 30, 1_005);
		long missing = 1_234_567_890L - stats.getLootValue();
		assertTrue("The sample loot overshoots its 1.23B target by " + -missing, missing >= 0L);
		long bonePickup = 4L * 2_811L;
		pickups(stats, DRAGON_BONES, (int) (missing / bonePickup), 4, 2_811);
		if (missing % bonePickup > 0L)
		{
			stats.addLoot(COINS, missing % bonePickup, missing % bonePickup);
		}

		// 55.5M of supplies; 4,936 potion doses are the 1,234 potions the panel shows.
		stats.addSupplyCost(21_600_000L, 4_936L, PvmToolsPlugin.SupplyCostType.POTION);
		stats.addSupplyCost(9_870_000L, 3_210L, PvmToolsPlugin.SupplyCostType.FOOD);
		stats.addSupplyCost(12_340_000L, 61_700L, PvmToolsPlugin.SupplyCostType.CANNONBALL);
		stats.addSupplyCost(7_500_000L, 45_678L, PvmToolsPlugin.SupplyCostType.RUNE);
		stats.addSupplyCost(3_200_000L, 12_345L, PvmToolsPlugin.SupplyCostType.AMMO);
		stats.addSupplyCost(990_000L, 5_500L, PvmToolsPlugin.SupplyCostType.ZULRAH_SCALE);
		stats.addDeathFee(500_000L);
		stats.addDeathFee(350_000L);
		stats.addDeathFee(384_500L);
		stats.addCombatXp(Skill.ATTACK, 152_600_000L);
		stats.addCombatXp(Skill.STRENGTH, 265_300_000L);
		stats.addCombatXp(Skill.DEFENCE, 118_400_000L);
		stats.addCombatXp(Skill.RANGED, 187_900_000L);
		stats.addCombatXp(Skill.MAGIC, 89_900_000L);
		stats.addCombatXp(Skill.HITPOINTS, 271_400_000L);
		stats.addSlayerXp(96_800_000L);

		// The active task is 11 kills in and still behind on supplies.
		PvmTaskSnapshot currentTask = new PvmTaskSnapshot("Greater demons", "", 184, 195,
			now - Duration.ofMinutes(5).toMillis(), Duration.ofMinutes(4).plusSeconds(38).toMillis(),
			greaterDemonLoot, 64_870L, 5_104L, 957L, 3L, 0L, 292L, 0L, 0L, 0L);

		// Newest first, like the plugin keeps it. The newest Abyssal task beats the older one in every record.
		List<PvmTaskHistoryEntry> history = List.of(
			finishedTask("Abyssal demons", "Catacombs of Kourend", 185, abyssalTaskFinished,
				Duration.ofMinutes(38).plusSeconds(12), 2_314_560L, 412_330L, 127_650L, 27_750L, 14L, 6L, 1_480L, 240L),
			finishedTask("Smoke devils", "", 142, smokeDevilTaskFinished,
				Duration.ofHours(1).plusMinutes(12).plusSeconds(40), 3_062_884L, 1_254_900L, 219_400L, 26_200L, 22L, 31L, 0L, 3_870L),
			finishedTask("Abyssal demons", "Slayer Tower", 142, firstAbyssalTaskFinished,
				Duration.ofMinutes(51).plusSeconds(40), 1_012_400L, 398_760L, 97_980L, 21_300L, 9L, 4L, 1_210L, 0L));

		Set<String> excluded = Set.of(
			PvmToolsPlugin.combatLootExclusionKey("Tormented Demon", INFERNAL_ASHES),
			PvmToolsPlugin.combatLootExclusionKey("Abyssal demon", ABYSSAL_ASHES));
		return new Sample(stats, currentTask, history, "12m", excluded);
	}

	private static SampleDrop drop(int itemId, long quantity, long unitValue, int every, int offset)
	{
		return new SampleDrop(itemId, quantity, unitValue, every, offset);
	}

	private static void kills(PvmToolsStats stats, String npc, int combatLevel, int kills, long lastKillMillis, SampleDrop... table)
	{
		for (int kill = 1; kill <= kills; kill++)
		{
			List<PvmDropStat> drops = new ArrayList<>();
			for (SampleDrop drop : table)
			{
				if (kill % drop.every == drop.offset)
				{
					long value = drop.quantity * drop.unitValue;
					drops.add(new PvmDropStat(drop.itemId, drop.quantity, value, 1L));
					stats.addLoot(drop.itemId, drop.quantity, value);
				}
			}
			stats.addCombatLoot(npc, combatLevel, drops, lastKillMillis - (kills - kill) * 20_000L);
		}
	}

	private static void pickups(PvmToolsStats stats, int itemId, int pickups, long quantity, long unitValue)
	{
		for (int pickup = 0; pickup < pickups; pickup++)
		{
			stats.addLoot(itemId, quantity, quantity * unitValue);
		}
	}

	private static PvmTaskHistoryEntry finishedTask(String name, String location, int amount, long finishedMillis, Duration elapsed,
		long loot, long supplies, long combatXp, long slayerXp, long potionDoses, long food, long cannonballs, long runes)
	{
		return new PvmTaskHistoryEntry(new PvmTaskSnapshot(name, location, 0, amount, finishedMillis - elapsed.toMillis(),
			elapsed.toMillis(), loot, supplies, combatXp, slayerXp, potionDoses, food, cannonballs, runes, 0L, 0L), finishedMillis);
	}

	private static String pluginVersion() throws IOException
	{
		try (InputStream stream = PvmToolsPlugin.class.getResourceAsStream("plugin.properties"))
		{
			if (stream == null)
			{
				return "dev";
			}
			Properties properties = new Properties();
			properties.load(stream);
			return properties.getProperty("version", "dev");
		}
	}

	private static double contrast(Color first, Color second)
	{
		double lighter = Math.max(luminance(first), luminance(second));
		double darker = Math.min(luminance(first), luminance(second));
		return (lighter + 0.05d) / (darker + 0.05d);
	}

	private static double luminance(Color color)
	{
		return 0.2126d * linear(color.getRed()) + 0.7152d * linear(color.getGreen()) + 0.0722d * linear(color.getBlue());
	}

	private static double linear(int channel)
	{
		double value = channel / 255d;
		return value <= 0.03928d ? value / 12.92d : Math.pow((value + 0.055d) / 1.055d, 2.4d);
	}

	private static double luminanceSpread(BufferedImage image)
	{
		double sum = 0d;
		double sumOfSquares = 0d;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				double value = luminance(new Color(image.getRGB(x, y)));
				sum += value;
				sumOfSquares += value * value;
			}
		}
		double count = (double) image.getWidth() * image.getHeight();
		double mean = sum / count;
		return Math.sqrt(Math.max(0d, sumOfSquares / count - mean * mean));
	}

	private static Path previewDirectory()
	{
		return Paths.get(System.getenv(PREVIEW_DIR_ENV));
	}

	private static List<String> info(String name)
	{
		synchronized (INFO)
		{
			return INFO.computeIfAbsent(name, ignored -> new ArrayList<>());
		}
	}

	private static void save(BufferedImage image, String subDirectory, String name) throws IOException
	{
		Path directory = subDirectory == null ? previewDirectory() : previewDirectory().resolve(subDirectory);
		Files.createDirectories(directory);
		writePng(image, directory.resolve(name + ".png"));

		BufferedImage enlarged = new BufferedImage(image.getWidth() * 2, image.getHeight() * 2, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = enlarged.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			graphics.drawImage(image, 0, 0, enlarged.getWidth(), enlarged.getHeight(), null);
		}
		finally
		{
			graphics.dispose();
		}
		writePng(enlarged, directory.resolve(name + "@2x.png"));
	}

	private static void writePng(BufferedImage image, Path file) throws IOException
	{
		if (!ImageIO.write(image, "png", file.toFile()))
		{
			throw new IOException("No PNG writer for " + file);
		}
		synchronized (WRITTEN_FILES)
		{
			WRITTEN_FILES.add(file.toAbsolutePath() + "  (" + image.getWidth() + " x " + image.getHeight() + ")");
		}
	}

	private static <T> T onEdt(Callable<T> task) throws Exception
	{
		AtomicReference<T> result = new AtomicReference<>();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				result.set(task.call());
			}
			catch (Throwable throwable)
			{
				failure.set(throwable);
			}
		});
		Throwable throwable = failure.get();
		if (throwable instanceof Error)
		{
			throw (Error) throwable;
		}
		if (throwable != null)
		{
			throw (Exception) throwable;
		}
		return result.get();
	}

	private static void flushEdt() throws Exception
	{
		// A refresh answers on the EDT and may queue one follow-up refresh; a few empty rounds drain them all.
		for (int round = 0; round < 5; round++)
		{
			SwingUtilities.invokeAndWait(() -> { });
		}
	}

	private static void invalidateTree(Container container)
	{
		container.invalidate();
		for (Component child : container.getComponents())
		{
			if (child instanceof Container)
			{
				invalidateTree((Container) child);
			}
		}
	}

	private static void layoutTree(Container container)
	{
		container.doLayout();
		for (Component child : container.getComponents())
		{
			if (child instanceof Container)
			{
				layoutTree((Container) child);
			}
		}
	}

	private static Object readField(Object target, Class<?> owner, String name) throws Exception
	{
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static void setField(Object target, Class<?> owner, String name, Object value) throws Exception
	{
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
