package com.arber.pvmtools;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PvmStatsPanelViewsTest
{
	private static final int WHIP = 4151;
	private static final int CLUE = 2677;

	@Test
	public void openLootLogIsOnlyRebuiltWhenItWouldLookDifferent() throws Exception
	{
		Fixture fixture = new Fixture();
		when(fixture.plugin.getItemDisplayName(anyInt())).thenReturn("Abyssal whip");
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addCombatLoot("Abyssal demon", 124, List.of(new PvmDropStat(WHIP, 1, 1_400_000L, 1L),
			new PvmDropStat(CLUE, 1, 0L, 1L)), 1_000L, true);
		fixture.show(stats, Set.of());
		Component[] first = fixture.renderLootLog(true);

		assertArrayEquals(first, fixture.renderLootLog(false));

		// Leaving out an item that is worth nothing changes no total, but the cell must still redraw.
		fixture.show(stats, Set.of(PvmToolsPlugin.combatLootExclusionKey("Abyssal demon", CLUE)));
		Component[] excluded = fixture.renderLootLog(false);
		assertNotSame(first[0], excluded[0]);
		assertArrayEquals(excluded, fixture.renderLootLog(false));

		PvmToolsStats more = stats.copy();
		more.addCombatLoot("Abyssal demon", 124, List.of(new PvmDropStat(WHIP, 1, 1_400_000L, 1L)), 2_000L, true);
		fixture.show(more, Set.of(PvmToolsPlugin.combatLootExclusionKey("Abyssal demon", CLUE)));
		assertNotSame(excluded[0], fixture.renderLootLog(false)[0]);
	}

	@Test
	public void lootLogKeepsRefreshingWhileAnItemNameIsStillLoading() throws Exception
	{
		Fixture fixture = new Fixture();
		when(fixture.plugin.getItemDisplayName(anyInt())).thenReturn(PvmToolsPlugin.ITEM_NAME_LOADING_TEXT);
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addCombatLoot("Abyssal demon", 124, List.of(new PvmDropStat(WHIP, 1, 1_400_000L, 1L)), 1_000L, true);
		fixture.show(stats, Set.of());
		Component[] loading = fixture.renderLootLog(true);

		when(fixture.plugin.getItemDisplayName(anyInt())).thenReturn("Abyssal whip");
		Component[] named = fixture.renderLootLog(false);
		assertNotSame(loading[0], named[0]);
		assertArrayEquals(named, fixture.renderLootLog(false));
	}

	@Test
	public void chatTabLineIsHiddenWhileChatTabsAreOffAndDropNamesAreShortened() throws Exception
	{
		Fixture fixture = new Fixture();
		when(fixture.plugin.getItemDisplayName(anyInt())).thenReturn("Smouldering pile of flesh");
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addLoot(WHIP, 3L, 30_000L);
		fixture.show(stats, Set.of());
		fixture.onEdt("refreshPeriodStats");
		JPanel row = (JPanel) fixture.field("chatTabScopeRow");
		assertFalse(row.isVisible());

		when(fixture.plugin.isChatTrackersQuickEnabled()).thenReturn(true);
		fixture.onEdt("refreshPeriodStats");
		assertTrue(row.isVisible());
		@SuppressWarnings("unchecked")
		Map<String, JLabel> values = (Map<String, JLabel>) fixture.field("valueLabels");
		assertTrue(values.get("dropMostValuable").getText(), values.get("dropMostValuable").getText().startsWith("Smouldering. | "));
		assertTrue(values.get("dropMostCommon").getText(), values.get("dropMostCommon").getText().startsWith("Smouldering. | "));
	}

	@Test
	public void slayerLogShowsLocationAndDateOnSeparateLines() throws Exception
	{
		Fixture fixture = new Fixture();
		PvmTaskSnapshot task = new PvmTaskSnapshot("Greater demons", "Catacombs of Kourend", 0, 180, 1_000L, 3_600_000L,
			500_000L, 100_000L, 90_000L, 30_000L, 8L, 4L, 0L, 0L, 0L, 0L);
		PvmToolsPanelSnapshot snapshot = new PvmToolsPanelSnapshot(PvmToolsStatsPeriod.DAY, new PvmToolsStats("day"),
			PvmTaskSnapshot.EMPTY, List.of(new PvmTaskHistoryEntry(task, 1_700_000_000_000L)), "No cannon", Set.of());
		fixture.set("panelSnapshot", snapshot);
		SwingUtilities.invokeAndWait(() -> fixture.call("refreshTaskLog", new Class<?>[]{boolean.class}, true));

		List<String> texts = new ArrayList<>();
		collectLabelTexts((Container) fixture.field("taskLogContent"), texts);
		assertTrue(texts.toString(), texts.contains("Catacombs of Kourend"));
		for (String text : texts)
		{
			assertFalse(text, text.startsWith("Catacombs of Kourend |"));
		}
		int location = texts.indexOf("Catacombs of Kourend");
		assertTrue(texts.get(location + 1), texts.get(location + 1).matches("\\d{2} \\w{3} \\d{4}, \\d{2}:\\d{2}"));
	}

	private static void collectLabelTexts(Container container, List<String> texts)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JLabel)
			{
				texts.add(((JLabel) child).getText());
			}
			else if (child instanceof Container)
			{
				collectLabelTexts((Container) child, texts);
			}
		}
	}

	private static final class Fixture
	{
		private final PvmToolsPlugin plugin = mock(PvmToolsPlugin.class);
		private final PvmToolsStatsPanel panel;

		private Fixture() throws Exception
		{
			AtomicReference<PvmToolsStatsPanel> created = new AtomicReference<>();
			SwingUtilities.invokeAndWait(() -> created.set(new PvmToolsStatsPanel(plugin)));
			panel = created.get();
		}

		private void show(PvmToolsStats stats, Set<String> excludedLoot) throws Exception
		{
			set("panelSnapshot", new PvmToolsPanelSnapshot(PvmToolsStatsPeriod.ALL_TIME, stats, PvmTaskSnapshot.EMPTY,
				List.of(), "No cannon", excludedLoot));
		}

		private Component[] renderLootLog(boolean force) throws Exception
		{
			SwingUtilities.invokeAndWait(() -> call("refreshLootLog", new Class<?>[]{boolean.class}, force));
			return ((Container) field("lootLogContent")).getComponents();
		}

		private void onEdt(String method) throws Exception
		{
			SwingUtilities.invokeAndWait(() -> call(method, new Class<?>[0]));
		}

		private void call(String name, Class<?>[] types, Object... arguments)
		{
			try
			{
				Method method = PvmToolsStatsPanel.class.getDeclaredMethod(name, types);
				method.setAccessible(true);
				method.invoke(panel, arguments);
			}
			catch (ReflectiveOperationException exception)
			{
				throw new AssertionError(exception);
			}
		}

		private Object field(String name) throws Exception
		{
			Field field = PvmToolsStatsPanel.class.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(panel);
		}

		private void set(String name, Object value) throws Exception
		{
			Field field = PvmToolsStatsPanel.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(panel, value);
		}
	}
}
