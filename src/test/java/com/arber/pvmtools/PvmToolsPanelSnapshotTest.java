package com.arber.pvmtools;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Skill;
import net.runelite.client.callback.ClientThread;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class PvmToolsPanelSnapshotTest
{
	@Test
	public void captureWaitsForClientThreadAndDetachesLiveCollections() throws Exception
	{
		PvmToolsPlugin plugin = new PvmToolsPlugin();
		ClientThread clientThread = mock(ClientThread.class);
		Deque<Runnable> queued = new ArrayDeque<>();
		doAnswer(invocation ->
		{
			queued.add(invocation.getArgument(0));
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));
		setField(plugin, "clientThread", clientThread);
		setField(plugin, "started", true);
		EnumMap<PvmToolsStatsPeriod, PvmToolsStats> statsByPeriod = getField(plugin, "statsByPeriod");
		PvmToolsStats live = new PvmToolsStats("all");
		live.addCombatXp(Skill.MAGIC, 100L);
		live.addLoot(995, 10L, 10L);
		statsByPeriod.put(PvmToolsStatsPeriod.ALL_TIME, live);
		Deque<PvmTaskHistoryEntry> history = getField(plugin, "taskHistory");
		history.add(new PvmTaskHistoryEntry(PvmTaskSnapshot.EMPTY, 1L));
		Set<String> exclusions = getField(plugin, "excludedCombatLootItems");
		exclusions.add(PvmToolsPlugin.combatLootExclusionKey("Nechryael", 995));

		AtomicReference<PvmToolsPanelSnapshot> captured = new AtomicReference<>();
		plugin.capturePanelSnapshot(PvmToolsStatsPeriod.ALL_TIME, captured::set);
		assertNull(captured.get());
		assertEquals(1, queued.size());
		queued.removeFirst().run();

		live.addCombatXp(Skill.MAGIC, 200L);
		live.resetLoot();
		history.clear();
		exclusions.clear();
		assertEquals(100L, captured.get().getStats().getCombatXp(Skill.MAGIC));
		assertEquals(10L, captured.get().getStats().getLootValue());
		assertEquals(1, captured.get().getTaskHistory().size());
		assertTrue(captured.get().isLootExcluded("NECHRYAEL", 995));
	}

	@Test
	public void capturedHistoryCannotBeChangedBySwing() throws Exception
	{
		PvmToolsPanelSnapshot snapshot = new PvmToolsPanelSnapshot(
			PvmToolsStatsPeriod.DAY, new PvmToolsStats("day"), PvmTaskSnapshot.EMPTY,
			new ArrayList<>(), "No cannon", Set.of());
		boolean rejected = false;
		try
		{
			snapshot.getTaskHistory().add(new PvmTaskHistoryEntry(PvmTaskSnapshot.EMPTY, 1L));
		}
		catch (UnsupportedOperationException expected)
		{
			rejected = true;
		}
		assertTrue(rejected);
	}

	private static void setField(Object target, String name, Object value) throws Exception
	{
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	@SuppressWarnings("unchecked")
	private static <T> T getField(Object target, String name) throws Exception
	{
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return (T) field.get(target);
	}
}
