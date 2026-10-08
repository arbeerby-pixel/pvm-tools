package com.arber.pvmtools;

import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class PvmTopSkillLayoutTest
{
	@Test
	public void everyTrackedTopSkillShowsItsFullNameAndLargeXpAtSidebarWidths() throws Exception
	{
		Fixture fixture = new Fixture();
		List<Skill> skills = new ArrayList<>(List.of(PvmToolsPlugin.COMBAT_TRACKER_SKILLS));
		skills.add(Skill.SLAYER);
		for (Skill skill : skills)
		{
			fixture.show(skill, 271_400_000L);
			for (int width : new int[]{225, 250, 300})
			{
				SwingUtilities.invokeAndWait(() ->
				{
					fixture.layout(width);
					fixture.assertTextFits(skill.getName(), "271.4M xp");
					String tooltip = skill.getName() + ": 271,400,000 xp";
					assertEquals(tooltip, fixture.name.getToolTipText());
					assertEquals(tooltip, fixture.xp.getToolTipText());
				});
			}
		}
	}

	@Test
	public void refreshingTopSkillAndEmptyStateKeepsBothRowsVisible() throws Exception
	{
		Fixture fixture = new Fixture();
		Skill[] skills = {Skill.HITPOINTS, Skill.STRENGTH, null, Skill.DEFENCE};
		long[] amounts = {271_400_000L, 12_345L, 0L, 42L};
		String[] values = {"271.4M xp", "12.3K xp", "-", "42 xp"};
		for (int i = 0; i < skills.length; i++)
		{
			fixture.show(skills[i], amounts[i]);
			String name = skills[i] == null ? "None" : skills[i].getName();
			String value = values[i];
			SwingUtilities.invokeAndWait(() ->
			{
				fixture.layout(225);
				fixture.assertTextFits(name, value);
			});
		}
	}

	@Test
	public void actualNameRowCanFitEverySkillNameAtTheNarrowestSidebarWidth() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.show(Skill.HITPOINTS, 271_400_000L);
		SwingUtilities.invokeAndWait(() ->
		{
			for (Skill skill : Skill.values())
			{
				fixture.name.setText(skill.getName());
				fixture.layout(225);
				fixture.assertTextFits(skill.getName(), "271.4M xp");
			}
		});
	}

	private static final class Fixture
	{
		private final Deque<Consumer<PvmToolsPanelSnapshot>> captures = new ArrayDeque<>();
		private final PvmToolsStatsPanel panel;
		private final JLabel name;
		private final JLabel xp;
		private final JPanel card;

		private Fixture() throws Exception
		{
			PvmToolsPlugin plugin = mock(PvmToolsPlugin.class);
			doAnswer(call ->
			{
				captures.add(call.getArgument(1));
				return null;
			}).when(plugin).capturePanelSnapshot(any(PvmToolsStatsPeriod.class), any());
			AtomicReference<PvmToolsStatsPanel> created = new AtomicReference<>();
			SwingUtilities.invokeAndWait(() -> created.set(new PvmToolsStatsPanel(plugin)));
			panel = created.get();
			name = (JLabel) field(panel, "topSkillNameLabel");
			@SuppressWarnings("unchecked")
			Map<String, JLabel> values = (Map<String, JLabel>) field(panel, "valueLabels");
			xp = values.get("periodTopSkill");
			card = (JPanel) name.getParent().getParent().getParent();
		}

		private void show(Skill skill, long amount) throws Exception
		{
			PvmToolsStats stats = new PvmToolsStats("day");
			if (skill == Skill.SLAYER)
			{
				stats.addSlayerXp(amount);
			}
			else if (skill != null)
			{
				stats.addCombatXp(skill, amount);
			}
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

		private void assertTextFits(String skillName, String value)
		{
			assertEquals(skillName, name.getText());
			assertEquals(value, xp.getText());
			assertSameParentAndSeparateRows();
			assertLabelFits(name);
			assertLabelFits(xp);
			Rectangle cardBounds = SwingUtilities.convertRectangle(card.getParent(), card.getBounds(), panel);
			assertTrue("Selected Period card overflows the sidebar", cardBounds.x >= 0
				&& cardBounds.x + cardBounds.width <= panel.getWidth());
		}

		private void assertSameParentAndSeparateRows()
		{
			assertEquals(name.getParent(), xp.getParent());
			assertFalse("Skill name and XP overlap", name.getBounds().intersects(xp.getBounds()));
		}

		private void assertLabelFits(JLabel label)
		{
			Insets insets = label.getInsets();
			FontMetrics metrics = label.getFontMetrics(label.getFont());
			int availableWidth = label.getWidth() - insets.left - insets.right;
			int availableHeight = label.getHeight() - insets.top - insets.bottom;
			String context = label.getText() + " at sidebar width " + panel.getWidth();
			assertTrue(context + " is truncated horizontally: needs " + metrics.stringWidth(label.getText())
				+ " px, has " + availableWidth + " px", metrics.stringWidth(label.getText()) <= availableWidth);
			assertTrue(context + " is truncated vertically", metrics.getHeight() <= availableHeight);
			Rectangle bounds = SwingUtilities.convertRectangle(label.getParent(), label.getBounds(), card);
			assertTrue(context + " overflows its card", bounds.x >= 0 && bounds.y >= 0
				&& bounds.x + bounds.width <= card.getWidth() && bounds.y + bounds.height <= card.getHeight());
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

	private static Object field(Object target, String name) throws Exception
	{
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
