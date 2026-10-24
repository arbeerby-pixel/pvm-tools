package com.arber.pvmtools;

import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PvmDeathFeesPanelTest
{
	@Test
	public void selectedPeriodShowsDeathFeesSeparatelyFromSuppliesInBothModes() throws Exception
	{
		for (boolean advanced : new boolean[]{false, true})
		{
			Fixture fixture = new Fixture(advanced);
			PvmToolsStats stats = new PvmToolsStats("day");
			stats.addLoot(995, 15_500_000L, 15_500_000L);
			stats.addSupplyCost(5_460_000L, 16_400L, PvmToolsPlugin.SupplyCostType.CANNONBALL);
			stats.addDeathFee(500_000L);
			stats.addDeathFee(100_000L);
			fixture.show(stats);

			SwingUtilities.invokeAndWait(() ->
			{
				JLabel fees = fixture.values.get("periodDeathFees");
				JLabel supplies = fixture.values.get("periodSupply");
				assertEquals("600K gp | 2 paid reclaims", fees.getText());
				assertEquals("600,000 gp in death fees from 2 paid reclaims; included in net profit", fees.getToolTipText());
				assertEquals("5.46M gp", supplies.getText());
				assertEquals("+9.44M gp", fixture.values.get("periodProfit").getText());
				assertNotSame(fees, supplies);
				assertEquals(supplies.getParent().getParent().getParent(), fees.getParent().getParent());
				for (Component parent = fees; parent != null; parent = parent.getParent())
				{
					assertTrue("Death fees must remain visible in " + (advanced ? "advanced" : "simple") + " mode", parent.isVisible());
				}
				fixture.layout(225);
				assertFits(fees);
			});
		}
	}

	@Test
	public void refreshChangesPaidReclaimCountAndClearsPreviousDeathFees() throws Exception
	{
		Fixture fixture = new Fixture(false);
		PvmToolsStats stats = new PvmToolsStats("day");
		stats.addDeathFee(500_000L);
		fixture.show(stats);
		SwingUtilities.invokeAndWait(() -> assertEquals("500K gp | 1 paid reclaim", fixture.values.get("periodDeathFees").getText()));
		fixture.show(new PvmToolsStats("day"));
		SwingUtilities.invokeAndWait(() ->
		{
			assertEquals("0 gp | 0 paid reclaims", fixture.values.get("periodDeathFees").getText());
			assertEquals("0 gp in death fees from 0 paid reclaims; included in net profit", fixture.values.get("periodDeathFees").getToolTipText());
		});
	}

	@Test
	public void deathFeesHaveAnIndependentUncheckedResetChoice() throws Exception
	{
		Fixture fixture = new Fixture(false);
		@SuppressWarnings("unchecked")
		Map<TrackerResetTarget, JCheckBox> choices = (Map<TrackerResetTarget, JCheckBox>) field(fixture.panel, "resetCheckboxes");
		assertEquals(Set.of(TrackerResetTarget.LOOT, TrackerResetTarget.SUPPLY_COST,
			TrackerResetTarget.COMBAT_XP, TrackerResetTarget.SLAYER_XP, TrackerResetTarget.DEATH_FEES), choices.keySet());
		SwingUtilities.invokeAndWait(() ->
		{
			JCheckBox fees = choices.get(TrackerResetTarget.DEATH_FEES);
			assertEquals("Death fees", fees.getText());
			assertFalse(fees.isSelected());
			fees.setSelected(true);
		});
		Method selected = PvmToolsStatsPanel.class.getDeclaredMethod("isResetSelected", TrackerResetTarget.class);
		selected.setAccessible(true);
		assertEquals(true, selected.invoke(fixture.panel, TrackerResetTarget.DEATH_FEES));
		assertEquals(false, selected.invoke(fixture.panel, TrackerResetTarget.SUPPLY_COST));
		assertEquals("Death fees", ChatTabDisplay.DEATH_FEES.toString());
	}

	private static final class Fixture
	{
		private final Deque<Consumer<PvmToolsPanelSnapshot>> captures = new ArrayDeque<>();
		private final PvmToolsStatsPanel panel;
		private final Map<String, JLabel> values;

		@SuppressWarnings("unchecked")
		private Fixture(boolean advanced) throws Exception
		{
			PvmToolsPlugin plugin = mock(PvmToolsPlugin.class);
			when(plugin.isAdvancedPanelMode()).thenReturn(advanced);
			doAnswer(call ->
			{
				captures.add(call.getArgument(1));
				return null;
			}).when(plugin).capturePanelSnapshot(any(PvmToolsStatsPeriod.class), any());
			AtomicReference<PvmToolsStatsPanel> created = new AtomicReference<>();
			SwingUtilities.invokeAndWait(() -> created.set(new PvmToolsStatsPanel(plugin)));
			panel = created.get();
			values = (Map<String, JLabel>) field(panel, "valueLabels");
		}

		private void show(PvmToolsStats stats) throws Exception
		{
			PvmToolsPanelSnapshot snapshot = new PvmToolsPanelSnapshot(PvmToolsStatsPeriod.DAY,
				stats, PvmTaskSnapshot.EMPTY, List.of(), "No cannon", Set.of());
			SwingUtilities.invokeAndWait(() ->
			{
				if (captures.isEmpty())
				{
					panel.refresh();
				}
				captures.removeFirst().accept(snapshot);
			});
			SwingUtilities.invokeAndWait(() -> { });
		}

		private void layout(int width)
		{
			invalidateTree(panel);
			panel.setSize(width, panel.getPreferredSize().height);
			layoutTree(panel);
		}
	}

	private static void assertFits(JLabel label)
	{
		Insets insets = label.getInsets();
		FontMetrics metrics = label.getFontMetrics(label.getFont());
		assertTrue("Death fee amount and count must fit at the normal sidebar width",
			metrics.stringWidth(label.getText()) <= label.getWidth() - insets.left - insets.right);
		assertTrue("Death fee row must have room for its text", metrics.getHeight() <= label.getHeight() - insets.top - insets.bottom);
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

	private static Object field(Object target, String name) throws Exception
	{
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
