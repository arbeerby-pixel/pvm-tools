package com.arber.pvmtools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.QuantityFormatter;

class PvmToolsStatsPanel extends PluginPanel
{
	private static final String DISCORD_URL = "https://discord.gg/utYem4XhQS";
	private static final Color PROFIT_COLOR = new Color(70, 255, 100);
	private static final Color COST_COLOR = new Color(255, 90, 90);
	private static final Color XP_COLOR = new Color(255, 220, 80);
	private static final Color MUTED_TEXT = new Color(190, 185, 170);
	private static final Color ON_COLOR = new Color(100, 255, 130);
	private static final Color OFF_COLOR = new Color(255, 110, 110);
	private static final Color CARD_BACKGROUND = new Color(34, 34, 34);
	private static final Color TOGGLE_ON_BACKGROUND = new Color(36, 58, 36);
	private static final Color TOGGLE_HOVER_BACKGROUND = new Color(54, 67, 54);
	private static final Color TOGGLE_OFF_HOVER_BACKGROUND = new Color(45, 45, 45);
	private static final int TOOLTIP_INITIAL_DELAY_MILLIS = 120;
	private static final int TOOLTIP_RESHOW_DELAY_MILLIS = 40;
	private static final int TASK_LOG_PAGE_SIZE = 20;
	private static final DateTimeFormatter TASK_DATE_FORMATTER = DateTimeFormatter
		.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH)
		.withZone(ZoneId.systemDefault());

	private final PvmToolsPlugin plugin;
	private final JPanel contentContainer = new JPanel(new BorderLayout());
	private final JPanel mainContent = verticalContentPanel();
	private final JPanel taskLogContent = verticalContentPanel();
	private final Map<PvmToolsStatsPeriod, JButton> periodButtons = new EnumMap<>(PvmToolsStatsPeriod.class);
	private final Map<String, JLabel> valueLabels = new java.util.HashMap<>();
	private final Map<TrackerResetTarget, JCheckBox> resetCheckboxes = new EnumMap<>(TrackerResetTarget.class);
	private final List<QuickToggle> quickToggles = new ArrayList<>();
	private final List<JPanel> advancedOnlyPanels = new ArrayList<>();
	private JPanel taskHistoryCard;
	private JLabel dropThresholdLabel;
	private JLabel quickStatusLabel;
	private JLabel resetStatusLabel;
	private PvmToolsStatsPeriod selectedPeriod = PvmToolsStatsPeriod.DAY;
	private boolean taskLogVisible;
	private int renderedTaskHistorySize = -1;
	private long renderedNewestTaskFinishedMillis = -1L;
	private int renderedTaskHistoryVisibleCount = -1;
	private int visibleTaskHistoryCount = TASK_LOG_PAGE_SIZE;

	PvmToolsStatsPanel(PvmToolsPlugin plugin)
	{
		this.plugin = plugin;
		ToolTipManager.sharedInstance().setInitialDelay(TOOLTIP_INITIAL_DELAY_MILLIS);
		ToolTipManager.sharedInstance().setReshowDelay(TOOLTIP_RESHOW_DELAY_MILLIS);
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		contentContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentContainer.add(mainContent, BorderLayout.CENTER);
		add(contentContainer, BorderLayout.NORTH);

		JLabel title = new JLabel("PvM Tools", SwingConstants.CENTER);
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setAlignmentX(CENTER_ALIGNMENT);
		mainContent.add(title);
		mainContent.add(Box.createVerticalStrut(8));

		addCurrentTask(mainContent);
		mainContent.add(Box.createVerticalStrut(8));
		addPeriodButtons(mainContent);
		mainContent.add(Box.createVerticalStrut(8));
		addStatsCards(mainContent);
		mainContent.add(Box.createVerticalStrut(8));
		addTaskHistory(mainContent);
		mainContent.add(Box.createVerticalStrut(10));
		addQuickControls(mainContent);
		mainContent.add(Box.createVerticalStrut(10));
		addResetControls(mainContent);

		refresh();
	}

	void refresh()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (taskLogVisible)
			{
				refreshTaskLog(false);
				return;
			}

			boolean advanced = plugin.isAdvancedPanelMode();
			for (JPanel panel : advancedOnlyPanels)
			{
				panel.setVisible(advanced);
			}

			refreshPeriodButtons();
			refreshCurrentTask();
			refreshPeriodStats();
			refreshTaskHistory();
			refreshQuickControls();
			revalidate();
			repaint();
		});
	}

	private void addCurrentTask(JPanel content)
	{
		JPanel taskCard = statsCard("Current Slayer Task");
		addHeroMetric(taskCard, "taskName", "Task", XP_COLOR);

		JPanel taskTiles = tileGrid();
		addStatTile(taskTiles, "taskProgress", "Progress", MUTED_TEXT);
		addStatTile(taskTiles, "taskTime", "Time", MUTED_TEXT);
		addStatTile(taskTiles, "taskProfit", "Profit", PROFIT_COLOR);
		addStatTile(taskTiles, "taskXp", "XP", XP_COLOR);
		taskCard.add(taskTiles);
		addInfoStrip(taskCard, "taskEfficiency", "Efficiency", MUTED_TEXT);
		content.add(taskCard);
	}

	private void addPeriodButtons(JPanel content)
	{
		JPanel periodPanel = new JPanel(new GridLayout(0, 2, 4, 4));
		periodPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		periodPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
		for (PvmToolsStatsPeriod period : PvmToolsStatsPeriod.values())
		{
			JButton button = new JButton(period.getDisplayName());
			button.setFocusable(false);
			button.addActionListener(e ->
			{
				selectedPeriod = period;
				refresh();
			});
			periodButtons.put(period, button);
			periodPanel.add(button);
		}
		content.add(periodPanel);
	}

	private void addStatsCards(JPanel content)
	{
		JPanel summaryCard = statsCard("Selected Period");
		addHeroMetric(summaryCard, "periodProfit", "Net profit", PROFIT_COLOR);

		JPanel overviewTiles = tileGrid();
		addStatTile(overviewTiles, "periodLoot", "Loot", PROFIT_COLOR);
		addStatTile(overviewTiles, "periodSupply", "Supplies", COST_COLOR);
		addStatTile(overviewTiles, "periodTotalXp", "Total XP", XP_COLOR);
		addStatTile(overviewTiles, "periodTopSkill", "Top skill", XP_COLOR);
		summaryCard.add(overviewTiles);
		addInfoStrip(summaryCard, "periodSupplyMix", "Supplies used", MUTED_TEXT);
		content.add(summaryCard);

		JPanel dropHighlightsCard = statsCard("Drop Highlights");
		addInfoStrip(dropHighlightsCard, "dropMostCommon", "Most picked", PROFIT_COLOR);
		addInfoStrip(dropHighlightsCard, "dropMostValuable", "Highest total", PROFIT_COLOR);
		addInfoStrip(dropHighlightsCard, "dropBestPickup", "Best pickup", PROFIT_COLOR);
		addInfoStrip(dropHighlightsCard, "dropUnique", "Unique drops", MUTED_TEXT);
		content.add(Box.createVerticalStrut(8));
		content.add(dropHighlightsCard);

		JPanel supplyDetailsCard = statsCard("Supply Details");
		addRow(supplyDetailsCard, "detailPotions", "Potions", COST_COLOR);
		addRow(supplyDetailsCard, "detailFood", "Food", COST_COLOR);
		addRow(supplyDetailsCard, "detailCannonballs", "Cannonballs", COST_COLOR);
		content.add(Box.createVerticalStrut(8));
		content.add(supplyDetailsCard);
		advancedOnlyPanels.add(supplyDetailsCard);

		JPanel xpDetailsCard = statsCard("XP Details");
		addRow(xpDetailsCard, "detailCombatXp", "Combat XP", XP_COLOR);
		addRow(xpDetailsCard, "detailSlayerXp", "Slayer XP", XP_COLOR);
		for (Skill skill : PvmToolsPlugin.COMBAT_TRACKER_SKILLS)
		{
			addRow(xpDetailsCard, skill.name(), formatSkillName(skill), MUTED_TEXT);
		}
		content.add(Box.createVerticalStrut(8));
		content.add(xpDetailsCard);
		advancedOnlyPanels.add(xpDetailsCard);

		JPanel statusCard = statsCard("Live Status");
		addRow(statusCard, "cannonEstimate", "Cannon", MUTED_TEXT);
		addRow(statusCard, "panelMode", "Panel mode", MUTED_TEXT);
		content.add(Box.createVerticalStrut(8));
		content.add(statusCard);
	}

	private void addTaskHistory(JPanel content)
	{
		taskHistoryCard = statsCard("Slayer Log");
		content.add(taskHistoryCard);
	}

	private void refreshPeriodButtons()
	{
		for (Map.Entry<PvmToolsStatsPeriod, JButton> entry : periodButtons.entrySet())
		{
			boolean selected = entry.getKey() == selectedPeriod;
			entry.getValue().setBackground(selected ? ColorScheme.BRAND_ORANGE : ColorScheme.DARKER_GRAY_COLOR);
			entry.getValue().setForeground(selected ? Color.BLACK : Color.WHITE);
		}
	}

	private void refreshCurrentTask()
	{
		PvmTaskSnapshot task = plugin.getCurrentSlayerTaskSnapshot();
		if (!task.isActive())
		{
			setValue("taskName", "No active task");
			setValue("taskProgress", "-");
			setValue("taskTime", "-");
			setValue("taskProfit", "0 gp");
			setValue("taskXp", "0 xp");
			setValue("taskEfficiency", "-");
			setValueColor("taskProfit", PROFIT_COLOR);
			return;
		}

		setValue("taskName", shorten(task.getName(), 16));
		setValue("taskProgress", formatTaskProgress(task));
		setValue("taskTime", formatDuration(task.getElapsedMillis()));
		setValue("taskProfit", formatSignedGp(task.getNetProfit()));
		setValueColor("taskProfit", task.getNetProfit() >= 0 ? PROFIT_COLOR : COST_COLOR);
		setValue("taskXp", formatXp(task.getCombatXp() + task.getSlayerXp()));
		setValue("taskEfficiency", formatTaskEfficiency(task));
	}

	private void refreshPeriodStats()
	{
		PvmToolsStats stats = plugin.getStatsSnapshot(selectedPeriod);
		setValue("periodProfit", formatSignedGp(stats.getNetProfit()));
		setValueColor("periodProfit", stats.getNetProfit() >= 0 ? PROFIT_COLOR : COST_COLOR);
		setValue("periodLoot", formatGp(stats.getLootValue()));
		setValue("periodSupply", formatGp(stats.getSupplyCostValue()));
		setValue("periodTotalXp", formatXp(stats.getCombatXp() + stats.getSlayerXp()));
		setValue("periodTopSkill", formatTopPeriodSkill(stats));
		long potions = (stats.getPotionDoseCount() + 3L) / 4L;
		setValue("periodSupplyMix", "Potions " + formatCount(potions) + " | Food " + formatCount(stats.getFoodCount()) + " | Balls " + formatCount(stats.getCannonballCount()));
		setValue("dropMostCommon", formatMostPicked(stats.getMostCommonDrop()));
		setValue("dropMostValuable", formatDropHighlight(stats.getMostValuableDrop()));
		setValue("dropBestPickup", formatDropHighlight(stats.getBestPickup()));
		setValue("dropUnique", formatCount(stats.getUniqueDropCount()) + " item types");
		setValue("detailPotions", formatSupplyDetail(stats.getPotionSupplyCostValue(), potions));
		setValue("detailFood", formatSupplyDetail(stats.getFoodSupplyCostValue(), stats.getFoodCount()));
		setValue("detailCannonballs", formatSupplyDetail(stats.getCannonballSupplyCostValue(), stats.getCannonballCount()));
		setValue("detailCombatXp", formatXp(stats.getCombatXp()));
		setValue("detailSlayerXp", formatXp(stats.getSlayerXp()));
		for (Skill skill : PvmToolsPlugin.COMBAT_TRACKER_SKILLS)
		{
			setValue(skill.name(), formatXp(stats.getCombatXp(skill)));
		}

		setValue("cannonEstimate", plugin.getCannonEstimateText());
		setValue("panelMode", plugin.isAdvancedPanelMode() ? "Advanced" : "Simple");
	}

	private void refreshTaskHistory()
	{
		taskHistoryCard.removeAll();
		taskHistoryCard.add(section("Slayer Log", CARD_BACKGROUND));

		List<PvmTaskHistoryEntry> history = plugin.getTaskHistorySnapshot();
		if (history.isEmpty())
		{
			addPlainLine(taskHistoryCard, "No finished tasks yet", MUTED_TEXT);
		}
		else
		{
			long totalProfit = history.stream().mapToLong(entry -> entry.getTask().getNetProfit()).sum();
			addPlainLine(
				taskHistoryCard,
				history.size() + (history.size() == 1 ? " task" : " tasks") + " | " + formatSignedGp(totalProfit),
				totalProfit >= 0 ? PROFIT_COLOR : COST_COLOR);
		}

		JButton openButton = actionButton("Open Slayer Log", ColorScheme.BRAND_ORANGE, Color.BLACK);
		openButton.setToolTipText("View every recorded Slayer task and its full breakdown.");
		openButton.addActionListener(e -> showTaskLog());
		taskHistoryCard.add(buttonRow(openButton));
	}

	private void showTaskLog()
	{
		taskLogVisible = true;
		renderedTaskHistorySize = -1;
		renderedNewestTaskFinishedMillis = -1L;
		renderedTaskHistoryVisibleCount = -1;
		visibleTaskHistoryCount = TASK_LOG_PAGE_SIZE;
		refreshTaskLog(true);
		showContent(taskLogContent);
	}

	private void showMainPanel()
	{
		taskLogVisible = false;
		showContent(mainContent);
		refresh();
	}

	private void showContent(JPanel content)
	{
		contentContainer.removeAll();
		contentContainer.add(content, BorderLayout.CENTER);
		contentContainer.revalidate();
		contentContainer.repaint();
	}

	private void refreshTaskLog(boolean force)
	{
		List<PvmTaskHistoryEntry> history = plugin.getTaskHistorySnapshot();
		long newestFinishedMillis = history.isEmpty() ? 0L : history.get(0).getFinishedMillis();
		if (!force
			&& history.size() == renderedTaskHistorySize
			&& newestFinishedMillis == renderedNewestTaskFinishedMillis
			&& visibleTaskHistoryCount == renderedTaskHistoryVisibleCount)
		{
			return;
		}

		renderedTaskHistorySize = history.size();
		renderedNewestTaskFinishedMillis = newestFinishedMillis;
		renderedTaskHistoryVisibleCount = visibleTaskHistoryCount;
		taskLogContent.removeAll();

		JLabel title = new JLabel("Slayer Task Log", SwingConstants.CENTER);
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setAlignmentX(CENTER_ALIGNMENT);
		taskLogContent.add(title);
		taskLogContent.add(Box.createVerticalStrut(8));

		JButton backButton = actionButton("Back to Stats", ColorScheme.DARKER_GRAY_COLOR, Color.WHITE);
		backButton.addActionListener(e -> showMainPanel());
		taskLogContent.add(buttonRow(backButton));
		taskLogContent.add(Box.createVerticalStrut(8));

		addTaskLogSummary(history);
		taskLogContent.add(Box.createVerticalStrut(8));
		if (history.isEmpty())
		{
			JPanel emptyCard = statsCard("Task History");
			addPlainLine(emptyCard, "Finished tasks will appear here.", MUTED_TEXT);
			taskLogContent.add(emptyCard);
		}
		else
		{
			int displayedTasks = Math.min(history.size(), visibleTaskHistoryCount);
			for (PvmTaskHistoryEntry entry : history.subList(0, displayedTasks))
			{
				taskLogContent.add(taskLogCard(entry));
				taskLogContent.add(Box.createVerticalStrut(8));
			}

			if (displayedTasks < history.size())
			{
				int remainingTasks = history.size() - displayedTasks;
				JButton showMoreButton = actionButton(
					"Show More (" + remainingTasks + " left)",
					ColorScheme.DARKER_GRAY_COLOR,
					Color.WHITE);
				showMoreButton.addActionListener(e ->
				{
					visibleTaskHistoryCount += TASK_LOG_PAGE_SIZE;
					refreshTaskLog(true);
				});
				taskLogContent.add(buttonRow(showMoreButton));
				taskLogContent.add(Box.createVerticalStrut(6));
			}
		}

		JButton clearButton = actionButton("Clear Slayer Log", new Color(105, 42, 42), Color.WHITE);
		clearButton.setEnabled(!history.isEmpty());
		clearButton.setToolTipText("Permanently delete all saved Slayer task history.");
		clearButton.addActionListener(e -> confirmClearTaskHistory());
		taskLogContent.add(buttonRow(clearButton));
		taskLogContent.revalidate();
		taskLogContent.repaint();
	}

	private void addTaskLogSummary(List<PvmTaskHistoryEntry> history)
	{
		long totalTime = 0L;
		long totalLoot = 0L;
		long totalCost = 0L;
		long totalCombatXp = 0L;
		long totalSlayerXp = 0L;
		for (PvmTaskHistoryEntry entry : history)
		{
			PvmTaskSnapshot task = entry.getTask();
			totalTime += task.getElapsedMillis();
			totalLoot += task.getLootValue();
			totalCost += task.getSupplyCostValue();
			totalCombatXp += task.getCombatXp();
			totalSlayerXp += task.getSlayerXp();
		}

		long totalProfit = totalLoot - totalCost;
		JPanel summary = statsCard("History Summary");
		addStaticRow(summary, "Tasks", formatCount(history.size()), MUTED_TEXT);
		addStaticRow(summary, "Time", formatDuration(totalTime), MUTED_TEXT);
		addStaticRow(summary, "Loot", formatGp(totalLoot), PROFIT_COLOR);
		addStaticRow(summary, "Supplies", formatGp(totalCost), COST_COLOR);
		addStaticRow(summary, "Profit", formatSignedGp(totalProfit), totalProfit >= 0 ? PROFIT_COLOR : COST_COLOR);
		addStaticRow(summary, "Combat XP", formatXp(totalCombatXp), XP_COLOR);
		addStaticRow(summary, "Slayer XP", formatXp(totalSlayerXp), XP_COLOR);
		taskLogContent.add(summary);
	}

	private JPanel taskLogCard(PvmTaskHistoryEntry entry)
	{
		PvmTaskSnapshot task = entry.getTask();
		JPanel card = statsCard(task.getName());
		String finished = entry.getFinishedMillis() > 0L
			? TASK_DATE_FORMATTER.format(Instant.ofEpochMilli(entry.getFinishedMillis()))
			: "Unknown date";
		String location = task.getLocation().isBlank() ? finished : task.getLocation() + " | " + finished;
		addPlainLine(card, location, MUTED_TEXT);

		JPanel metrics = tileGrid();
		addStaticTile(metrics, formatTaskProgress(task), "Kills", MUTED_TEXT);
		addStaticTile(metrics, formatDuration(task.getElapsedMillis()), "Time", MUTED_TEXT);
		addStaticTile(metrics, formatGp(task.getLootValue()), "Loot", PROFIT_COLOR);
		addStaticTile(metrics, formatGp(task.getSupplyCostValue()), "Supplies", COST_COLOR);
		addStaticTile(metrics, formatSignedGp(task.getNetProfit()), "Profit", task.getNetProfit() >= 0 ? PROFIT_COLOR : COST_COLOR);
		addStaticTile(metrics, formatXp(task.getCombatXp() + task.getSlayerXp()), "Total XP", XP_COLOR);
		card.add(metrics);

		addStaticRow(card, "Combat XP", formatXp(task.getCombatXp()), XP_COLOR);
		addStaticRow(card, "Slayer XP", formatXp(task.getSlayerXp()), XP_COLOR);
		long potions = (task.getPotionDoseCount() + 3L) / 4L;
		addPlainLine(
			card,
			"Potions " + formatCount(potions) + " | Food " + formatCount(task.getFoodCount()) + " | Balls " + formatCount(task.getCannonballCount()),
			MUTED_TEXT);
		return card;
	}

	private void confirmClearTaskHistory()
	{
		int result = JOptionPane.showConfirmDialog(
			this,
			"Permanently clear every saved Slayer task?",
			"Clear Slayer Log",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if (result == JOptionPane.YES_OPTION)
		{
			plugin.clearTaskHistory();
			renderedTaskHistorySize = -1;
			renderedTaskHistoryVisibleCount = -1;
			visibleTaskHistoryCount = TASK_LOG_PAGE_SIZE;
			refreshTaskLog(true);
		}
	}

	private void addQuickControls(JPanel content)
	{
		content.add(section("Quick controls"));

		JPanel settingsRow = new JPanel(new BorderLayout());
		settingsRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		settingsRow.setBorder(BorderFactory.createEmptyBorder(0, 40, 7, 40));
		settingsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
		JButton settingsButton = new JButton("Full settings");
		settingsButton.setFocusable(false);
		settingsButton.setBackground(ColorScheme.BRAND_ORANGE);
		settingsButton.setForeground(Color.BLACK);
		settingsButton.setToolTipText("<html><b>Full settings</b><br>Open RuneLite Configuration and search PvM Tools.</html>");
		settingsButton.addActionListener(e -> quickStatusLabel.setText("Open config: PvM Tools"));
		settingsRow.add(settingsButton, BorderLayout.CENTER);
		content.add(settingsRow);

		JPanel discordRow = new JPanel(new BorderLayout());
		discordRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		discordRow.setBorder(BorderFactory.createEmptyBorder(0, 40, 9, 40));
		discordRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
		JButton discordButton = new JButton("Join Discord");
		discordButton.setFocusable(false);
		discordButton.setBackground(new Color(88, 101, 242));
		discordButton.setForeground(Color.WHITE);
		discordButton.setToolTipText("<html><b>Join Discord</b><br>Open Arber Plugins Discord for suggestions,<br>support, bug reports, and plugin updates.</html>");
		discordButton.addActionListener(e -> LinkBrowser.browse(DISCORD_URL));
		discordRow.add(discordButton, BorderLayout.CENTER);
		content.add(discordRow);

		JPanel toggles = new JPanel(new GridLayout(0, 2, 4, 4));
		toggles.setBackground(ColorScheme.DARK_GRAY_COLOR);
		toggles.setMaximumSize(new Dimension(Integer.MAX_VALUE, 128));
		addQuickToggle(toggles, "Potion timers", "<html><b>Potion timers</b><br>Turns prayer and all supported potion timer infoboxes on/off.</html>", plugin::isPotionTimersQuickEnabled, plugin::setPotionTimersQuickEnabled);
		addQuickToggle(toggles, "Warnings", "<html><b>Warnings</b><br>Turns the red screen warning overlay on/off<br>for enabled warning types.</html>", plugin::isScreenWarningsQuickEnabled, plugin::setScreenWarningsQuickEnabled);
		addQuickToggle(toggles, "Drop alert", "<html><b>Drop alert</b><br>Turns green flash and sound on/off<br>for NPC drops over your threshold.</html>", plugin::isDropAlertQuickEnabled, plugin::setDropAlertQuickEnabled);
		addQuickToggle(toggles, "Cannon", "<html><b>Cannon</b><br>Turns cannon empty and repair screen warnings on/off.</html>", plugin::isCannonWarningsQuickEnabled, plugin::setCannonWarningsQuickEnabled);
		addQuickToggle(toggles, "Superior", "<html><b>Superior</b><br>Turns superior Slayer spawn flashes<br>and short examine prayer tips on/off.</html>", plugin::isSuperiorAlertsQuickEnabled, plugin::setSuperiorAlertsQuickEnabled);
		addQuickToggle(toggles, "Inventory", "<html><b>Inventory</b><br>Shows free inventory spaces in an infobox.<br>Hover it to see the inventory value.</html>", plugin::isInventorySpacesQuickEnabled, plugin::setInventorySpacesQuickEnabled);
		addQuickToggle(toggles, "Chat tabs", "<html><b>Chat tabs</b><br>Turns loot, supply cost, combat XP,<br>Slayer XP, and top skill chat-tab trackers on/off.</html>", plugin::isChatTrackersQuickEnabled, plugin::setChatTrackersQuickEnabled);
		content.add(toggles);

		dropThresholdLabel = new JLabel("", SwingConstants.CENTER);
		dropThresholdLabel.setForeground(MUTED_TEXT);
		dropThresholdLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
		content.add(centeredRow(dropThresholdLabel));

		quickStatusLabel = new JLabel("Panel mode controls detail level", SwingConstants.CENTER);
		quickStatusLabel.setForeground(MUTED_TEXT);
		quickStatusLabel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
		content.add(centeredRow(quickStatusLabel));
	}

	private void addResetControls(JPanel content)
	{
		content.add(section("Reset trackers"));

		JPanel resetCard = new JPanel(new GridLayout(2, 2, 4, 4));
		resetCard.setBackground(ColorScheme.DARK_GRAY_COLOR);
		resetCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
		addResetCheckbox(resetCard, TrackerResetTarget.LOOT);
		addResetCheckbox(resetCard, TrackerResetTarget.SUPPLY_COST);
		addResetCheckbox(resetCard, TrackerResetTarget.COMBAT_XP);
		addResetCheckbox(resetCard, TrackerResetTarget.SLAYER_XP);
		content.add(resetCard);

		JPanel resetButtonRow = new JPanel(new BorderLayout());
		resetButtonRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		resetButtonRow.setBorder(BorderFactory.createEmptyBorder(7, 40, 2, 40));
		resetButtonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
		JButton resetButton = new JButton("Reset selected");
		resetButton.setFocusable(false);
		resetButton.setBackground(ColorScheme.BRAND_ORANGE);
		resetButton.setForeground(Color.BLACK);
		resetButton.setToolTipText("<html><b>Reset selected</b><br>Resets only the tracker boxes selected above.</html>");
		resetButton.addActionListener(e -> resetSelectedTrackers());
		resetButtonRow.add(resetButton, BorderLayout.CENTER);
		content.add(resetButtonRow);

		resetStatusLabel = new JLabel("Select trackers, then reset", SwingConstants.CENTER);
		resetStatusLabel.setForeground(MUTED_TEXT);
		content.add(centeredRow(resetStatusLabel));
	}

	private void addResetCheckbox(JPanel parent, TrackerResetTarget target)
	{
		JCheckBox checkbox = new JCheckBox(target.toString());
		checkbox.setFocusable(false);
		checkbox.setOpaque(true);
		checkbox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		checkbox.setForeground(ColorScheme.TEXT_COLOR);
		checkbox.setToolTipText("Select " + target + " for the next reset.");
		resetCheckboxes.put(target, checkbox);
		parent.add(checkbox);
	}

	private void resetSelectedTrackers()
	{
		boolean resetLoot = isResetSelected(TrackerResetTarget.LOOT);
		boolean resetSupplyCost = isResetSelected(TrackerResetTarget.SUPPLY_COST);
		boolean resetCombatXp = isResetSelected(TrackerResetTarget.COMBAT_XP);
		boolean resetSlayerXp = isResetSelected(TrackerResetTarget.SLAYER_XP);

		if (!resetLoot && !resetSupplyCost && !resetCombatXp && !resetSlayerXp)
		{
			resetStatusLabel.setText("Choose at least one tracker");
			return;
		}

		plugin.resetTrackers(resetLoot, resetSupplyCost, resetCombatXp, resetSlayerXp);
		for (JCheckBox checkbox : resetCheckboxes.values())
		{
			checkbox.setSelected(false);
		}

		resetStatusLabel.setText("Selected trackers reset");
		refresh();
	}

	private boolean isResetSelected(TrackerResetTarget target)
	{
		JCheckBox checkbox = resetCheckboxes.get(target);
		return checkbox != null && checkbox.isSelected();
	}

	private void addQuickToggle(JPanel parent, String label, String tooltip, BooleanSupplier isEnabled, Consumer<Boolean> setEnabled)
	{
		JButton button = new JButton();
		button.setFocusable(false);
		button.setToolTipText(tooltip);
		QuickToggle toggle = new QuickToggle(label, isEnabled, setEnabled, button);
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent event)
			{
				button.setBackground(toggle.isEnabled.getAsBoolean() ? TOGGLE_HOVER_BACKGROUND : TOGGLE_OFF_HOVER_BACKGROUND);
			}

			@Override
			public void mouseExited(MouseEvent event)
			{
				refreshQuickControls();
			}
		});
		button.addActionListener(e ->
		{
			toggle.setEnabled.accept(!toggle.isEnabled.getAsBoolean());
			refresh();
		});
		quickToggles.add(toggle);
		parent.add(button);
	}

	private void refreshQuickControls()
	{
		for (QuickToggle toggle : quickToggles)
		{
			boolean enabled = toggle.isEnabled.getAsBoolean();
			toggle.button.setText("<html><center>" + toggle.label + "<br>" + (enabled ? "On" : "Off") + "</center></html>");
			toggle.button.setForeground(enabled ? ON_COLOR : OFF_COLOR);
			toggle.button.setBackground(enabled ? TOGGLE_ON_BACKGROUND : ColorScheme.DARKER_GRAY_COLOR);
		}

		if (dropThresholdLabel != null)
		{
			dropThresholdLabel.setText("Drop alert: " + plugin.getDropAlertThresholdText());
		}
	}

	private static JPanel verticalContentPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		return panel;
	}

	private JButton actionButton(String text, Color background, Color foreground)
	{
		JButton button = new JButton(text);
		button.setFocusable(false);
		button.setBackground(background);
		button.setForeground(foreground);
		return button;
	}

	private JPanel buttonRow(JButton button)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(7, 28, 2, 28));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 39));
		row.add(button, BorderLayout.CENTER);
		return row;
	}

	private JPanel statsCard(String title)
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(CARD_BACKGROUND);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR),
			BorderFactory.createEmptyBorder(6, 7, 7, 7)
		));
		card.setAlignmentX(CENTER_ALIGNMENT);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		card.add(section(title, CARD_BACKGROUND));
		return card;
	}

	private JPanel section(String text)
	{
		return section(text, ColorScheme.DARK_GRAY_COLOR);
	}

	private JPanel section(String text, Color background)
	{
		JLabel label = new JLabel(text);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setBorder(BorderFactory.createEmptyBorder(7, 0, 3, 0));
		return centeredRow(label, background);
	}

	private JPanel centeredRow(JLabel label)
	{
		return centeredRow(label, ColorScheme.DARK_GRAY_COLOR);
	}

	private JPanel centeredRow(JLabel label, Color background)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(background);
		row.setAlignmentX(CENTER_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height + 8));
		row.add(label, BorderLayout.CENTER);
		return row;
	}

	private void addHeroMetric(JPanel parent, String key, String labelText, Color valueColor)
	{
		JPanel hero = new JPanel();
		hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
		hero.setBackground(new Color(28, 28, 28));
		hero.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR),
			BorderFactory.createEmptyBorder(6, 4, 6, 4)
		));
		hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

		JLabel label = new JLabel(labelText, SwingConstants.CENTER);
		label.setForeground(MUTED_TEXT);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setAlignmentX(CENTER_ALIGNMENT);

		JLabel value = new JLabel("0", SwingConstants.CENTER);
		value.setForeground(valueColor);
		value.setFont(FontManager.getRunescapeBoldFont());
		value.setAlignmentX(CENTER_ALIGNMENT);
		valueLabels.put(key, value);

		hero.add(label);
		hero.add(value);
		parent.add(hero);
		parent.add(Box.createVerticalStrut(5));
	}

	private JPanel tileGrid()
	{
		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
		grid.setBackground(CARD_BACKGROUND);
		grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		return grid;
	}

	private void addStatTile(JPanel parent, String key, String labelText, Color valueColor)
	{
		JPanel tile = new JPanel();
		tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
		tile.setBackground(new Color(27, 27, 27));
		tile.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR),
			BorderFactory.createEmptyBorder(4, 3, 4, 3)
		));

		JLabel value = new JLabel("0", SwingConstants.CENTER);
		value.setForeground(valueColor);
		value.setFont(FontManager.getDefaultBoldFont());
		value.setAlignmentX(CENTER_ALIGNMENT);
		valueLabels.put(key, value);

		JLabel label = new JLabel(labelText, SwingConstants.CENTER);
		label.setForeground(MUTED_TEXT);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setAlignmentX(CENTER_ALIGNMENT);

		tile.add(value);
		tile.add(label);
		parent.add(tile);
	}

	private void addStaticTile(JPanel parent, String valueText, String labelText, Color valueColor)
	{
		JPanel tile = new JPanel();
		tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
		tile.setBackground(new Color(27, 27, 27));
		tile.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR),
			BorderFactory.createEmptyBorder(4, 3, 4, 3)
		));

		JLabel value = new JLabel(valueText, SwingConstants.CENTER);
		value.setForeground(valueColor);
		value.setFont(FontManager.getDefaultBoldFont());
		value.setAlignmentX(CENTER_ALIGNMENT);

		JLabel label = new JLabel(labelText, SwingConstants.CENTER);
		label.setForeground(MUTED_TEXT);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setAlignmentX(CENTER_ALIGNMENT);

		tile.add(value);
		tile.add(label);
		parent.add(tile);
	}

	private void addInfoStrip(JPanel parent, String key, String labelText, Color valueColor)
	{
		JPanel strip = new JPanel();
		strip.setLayout(new BoxLayout(strip, BoxLayout.Y_AXIS));
		strip.setBackground(parent.getBackground());
		strip.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

		JLabel label = new JLabel(labelText, SwingConstants.CENTER);
		label.setForeground(MUTED_TEXT);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setAlignmentX(CENTER_ALIGNMENT);

		JLabel value = new JLabel("0", SwingConstants.CENTER);
		value.setForeground(valueColor);
		value.setFont(FontManager.getRunescapeSmallFont());
		value.setAlignmentX(CENTER_ALIGNMENT);
		valueLabels.put(key, value);

		strip.add(label);
		strip.add(value);
		parent.add(strip);
	}

	private JPanel addRow(JPanel parent, String key, String labelText, Color valueColor)
	{
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(parent.getBackground());
		row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

		JLabel label = new JLabel(labelText);
		label.setForeground(ColorScheme.TEXT_COLOR);
		JLabel value = new JLabel("0", SwingConstants.RIGHT);
		value.setForeground(valueColor);
		value.setFont(FontManager.getDefaultBoldFont());
		valueLabels.put(key, value);

		row.add(label, BorderLayout.WEST);
		row.add(value, BorderLayout.EAST);
		parent.add(row);
		return row;
	}

	private void addStaticRow(JPanel parent, String labelText, String valueText, Color valueColor)
	{
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(parent.getBackground());
		row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

		JLabel label = new JLabel(labelText);
		label.setForeground(ColorScheme.TEXT_COLOR);
		JLabel value = new JLabel(valueText, SwingConstants.RIGHT);
		value.setForeground(valueColor);
		value.setFont(FontManager.getDefaultBoldFont());

		row.add(label, BorderLayout.WEST);
		row.add(value, BorderLayout.EAST);
		parent.add(row);
	}

	private void addPlainLine(JPanel parent, String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setForeground(color);
		label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		parent.add(centeredRow(label, parent.getBackground()));
	}

	private void setValue(String key, String value)
	{
		JLabel label = valueLabels.get(key);
		if (label != null)
		{
			label.setText(value);
		}
	}

	private void setValueColor(String key, Color color)
	{
		JLabel label = valueLabels.get(key);
		if (label != null)
		{
			label.setForeground(color);
		}
	}

	private String formatTaskProgress(PvmTaskSnapshot task)
	{
		if (task.getInitialAmount() > 0)
		{
			return task.getKilled() + " / " + task.getInitialAmount();
		}

		return task.getAmount() + " left";
	}

	private String formatTaskEfficiency(PvmTaskSnapshot task)
	{
		int kills = task.getKilled();
		if (kills <= 0)
		{
			return "Tracking";
		}

		long profitPerKill = task.getNetProfit() / kills;
		long breakEven = task.getSupplyCostValue() - task.getLootValue();
		String breakEvenText = breakEven > 0 ? formatCompactCount(breakEven) + " to even" : "profitable";
		return formatSignedGpShort(profitPerKill) + " gp/kill | " + breakEvenText;
	}

	private String shorten(String text, int maxLength)
	{
		if (text == null || text.length() <= maxLength)
		{
			return text == null ? "" : text;
		}

		return text.substring(0, Math.max(0, maxLength - 1)) + ".";
	}

	private String formatTopPeriodSkill(PvmToolsStats stats)
	{
		Skill topSkill = null;
		long topXp = 0L;
		for (Skill skill : PvmToolsPlugin.COMBAT_TRACKER_SKILLS)
		{
			long xp = stats.getCombatXp(skill);
			if (xp > topXp)
			{
				topXp = xp;
				topSkill = skill;
			}
		}

		if (stats.getSlayerXp() > topXp)
		{
			topXp = stats.getSlayerXp();
			topSkill = Skill.SLAYER;
		}

		if (topSkill == null || topXp <= 0L)
		{
			return "-";
		}

		return formatSkillName(topSkill) + " " + formatCompactCount(topXp);
	}

	private String formatSkillName(Skill skill)
	{
		String name = skill.getName();
		if (name == null || name.isBlank())
		{
			name = skill.name().toLowerCase(Locale.ENGLISH);
			return Character.toUpperCase(name.charAt(0)) + name.substring(1);
		}

		return name;
	}

	private String formatElapsed(long startedMillis)
	{
		if (startedMillis <= 0L)
		{
			return "-";
		}

		return formatDuration(System.currentTimeMillis() - startedMillis);
	}

	private String formatDuration(long millis)
	{
		long seconds = Math.max(0L, Duration.ofMillis(millis).getSeconds());
		long hours = seconds / 3600L;
		long minutes = seconds % 3600L / 60L;
		if (hours > 0)
		{
			return hours + "h " + minutes + "m";
		}

		if (minutes > 0)
		{
			return minutes + "m";
		}

		return seconds + "s";
	}

	private String formatGp(long value)
	{
		return QuantityFormatter.quantityToStackSize(value) + " gp";
	}

	private String formatGpShort(long value)
	{
		return QuantityFormatter.quantityToStackSize(value);
	}

	private String formatSignedGp(long value)
	{
		String prefix = value > 0 ? "+" : value < 0 ? "-" : "";
		return prefix + QuantityFormatter.quantityToStackSize(Math.abs(value)) + " gp";
	}

	private String formatSignedGpShort(long value)
	{
		String prefix = value > 0 ? "+" : value < 0 ? "-" : "";
		return prefix + QuantityFormatter.quantityToStackSize(Math.abs(value));
	}

	private String formatSupplyDetail(long value, long count)
	{
		return formatGpShort(value) + " gp / " + formatCount(count);
	}

	private String formatDropHighlight(PvmDropStat drop)
	{
		if (drop == null)
		{
			return "-";
		}
		if (drop.getItemId() == ItemID.COINS_995)
		{
			return formatCoinDrop(drop);
		}

		String itemName = shorten(plugin.getItemDisplayName(drop.getItemId()), 18);
		return itemName + " | " + formatCount(drop.getQuantity()) + "x | " + formatGpShort(drop.getValue()) + " gp";
	}

	private String formatMostPicked(PvmDropStat drop)
	{
		if (drop == null)
		{
			return "-";
		}
		if (drop.getItemId() == ItemID.COINS_995)
		{
			return formatCoinDrop(drop);
		}

		String itemName = shorten(plugin.getItemDisplayName(drop.getItemId()), 18);
		String pickupLabel = drop.getPickupCount() == 1L ? "drop" : "drops";
		return itemName + " | " + formatCount(drop.getPickupCount()) + " " + pickupLabel + " | " + formatGpShort(drop.getValue()) + " gp";
	}

	private String formatCoinDrop(PvmDropStat drop)
	{
		String pickupLabel = drop.getPickupCount() == 1L ? "drop" : "drops";
		return "Coins | " + formatCount(drop.getPickupCount()) + " " + pickupLabel + " | " + formatGpShort(drop.getValue()) + " gp";
	}

	private String formatXp(long xp)
	{
		return QuantityFormatter.quantityToStackSize(xp) + " xp";
	}

	private String formatCount(long count)
	{
		return QuantityFormatter.quantityToStackSize(count);
	}

	private String formatCompactCount(long count)
	{
		long absolute = Math.abs(count);
		if (absolute >= 1_000_000_000L)
		{
			return formatCompactDecimal(count / 1_000_000_000d) + "B";
		}
		if (absolute >= 1_000_000L)
		{
			return formatCompactDecimal(count / 1_000_000d) + "M";
		}
		if (absolute >= 1_000L)
		{
			return formatCompactDecimal(count / 1_000d) + "K";
		}
		return Long.toString(count);
	}

	private String formatCompactDecimal(double value)
	{
		String formatted = String.format(Locale.ENGLISH, "%.1f", value);
		return formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted;
	}

	private static final class QuickToggle
	{
		private final String label;
		private final BooleanSupplier isEnabled;
		private final Consumer<Boolean> setEnabled;
		private final JButton button;

		private QuickToggle(String label, BooleanSupplier isEnabled, Consumer<Boolean> setEnabled, JButton button)
		{
			this.label = label;
			this.isEnabled = isEnabled;
			this.setEnabled = setEnabled;
			this.button = button;
		}
	}
}
