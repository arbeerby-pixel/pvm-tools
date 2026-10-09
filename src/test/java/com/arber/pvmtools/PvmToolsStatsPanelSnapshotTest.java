package com.arber.pvmtools;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class PvmToolsStatsPanelSnapshotTest
{
	@Test
	public void refreshesCoalesceAndPeriodSwitchDiscardsOutdatedCapture() throws Exception
	{
		PvmToolsPlugin plugin = mock(PvmToolsPlugin.class);
		List<PvmToolsStatsPeriod> requestedPeriods = new ArrayList<>();
		List<Consumer<PvmToolsPanelSnapshot>> callbacks = new ArrayList<>();
		doAnswer(invocation ->
		{
			requestedPeriods.add(invocation.getArgument(0));
			callbacks.add(invocation.getArgument(1));
			return null;
		}).when(plugin).capturePanelSnapshot(any(PvmToolsStatsPeriod.class), any());
		AtomicReference<PvmToolsStatsPanel> panelRef = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> panelRef.set(new PvmToolsStatsPanel(plugin)));
		PvmToolsStatsPanel panel = panelRef.get();
		assertEquals(List.of(PvmToolsStatsPeriod.DAY), requestedPeriods);

		Field selectedPeriod = PvmToolsStatsPanel.class.getDeclaredField("selectedPeriod");
		selectedPeriod.setAccessible(true);
		SwingUtilities.invokeAndWait(() ->
		{
			panel.refresh();
			panel.refresh();
			try
			{
				selectedPeriod.set(panel, PvmToolsStatsPeriod.MONTH);
			}
			catch (IllegalAccessException exception)
			{
				throw new AssertionError(exception);
			}
			panel.refresh();
		});
		assertEquals(1, callbacks.size());
		callbacks.get(0).accept(PvmToolsPanelSnapshot.empty(PvmToolsStatsPeriod.DAY));
		SwingUtilities.invokeAndWait(() -> { });
		assertEquals(List.of(PvmToolsStatsPeriod.DAY, PvmToolsStatsPeriod.MONTH), requestedPeriods);

		PvmToolsStats stats = new PvmToolsStats("month");
		stats.addCombatXp(Skill.MAGIC, 200L);
		callbacks.get(1).accept(new PvmToolsPanelSnapshot(
			PvmToolsStatsPeriod.MONTH, stats, PvmTaskSnapshot.EMPTY, List.of(), "No cannon", Set.of()));
		SwingUtilities.invokeAndWait(() -> { });
		Field snapshotField = PvmToolsStatsPanel.class.getDeclaredField("panelSnapshot");
		snapshotField.setAccessible(true);
		PvmToolsPanelSnapshot displayed = (PvmToolsPanelSnapshot) snapshotField.get(panel);
		assertEquals(PvmToolsStatsPeriod.MONTH, displayed.getPeriod());
		assertEquals(200L, displayed.getStats().getCombatXp(Skill.MAGIC));
		// Swing rendering must never copy or iterate the plugin's live model.
		verify(plugin, never()).getStatsSnapshot(any(PvmToolsStatsPeriod.class));
		verify(plugin, never()).getCurrentSlayerTaskSnapshot();
		verify(plugin, never()).getTaskHistorySnapshot();
		verify(plugin, never()).getCannonEstimateText();
	}
}
