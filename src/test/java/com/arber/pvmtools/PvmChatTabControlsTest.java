package com.arber.pvmtools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PvmChatTabControlsTest
{
	@Test
	public void masterSwitchRestoresEveryTabIncludingTradeAndKeepsChoices() throws Exception
	{
		Fixture fixture = new Fixture();
		for (int id : Fixture.TABS)
		{
			fixture.choices.put(id, ChatTabDisplay.SLAYER_XP);
		}
		fixture.update();
		for (TabFixture tab : fixture.tabs.values())
		{
			assertEquals("0", tab.title.text());
			assertEquals("Slayer", tab.filter.text());
		}

		fixture.enabled = false;
		fixture.update();
		for (Map.Entry<Integer, TabFixture> entry : fixture.tabs.entrySet())
		{
			entry.getValue().assertNative();
			assertEquals(ChatTabDisplay.SLAYER_XP, fixture.choices.get(entry.getKey()));
		}
		fixture.enabled = true;
		fixture.update();
		assertEquals("Slayer", fixture.tabs.get(ComponentID.CHATBOX_TAB_TRADE).filter.text());
	}

	@Test
	public void vanillaRestoresOneSlotWhileAnotherExplicitSelectionRemains() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.choices.put(ComponentID.CHATBOX_TAB_PRIVATE, ChatTabDisplay.SLAYER_XP);
		fixture.choices.put(ComponentID.CHATBOX_TAB_PUBLIC, ChatTabDisplay.ATTACK_XP);
		fixture.update();
		fixture.choices.put(ComponentID.CHATBOX_TAB_PRIVATE, ChatTabDisplay.VANILLA);
		fixture.update();

		fixture.tabs.get(ComponentID.CHATBOX_TAB_PRIVATE).assertNative();
		TabFixture publicTab = fixture.tabs.get(ComponentID.CHATBOX_TAB_PUBLIC);
		assertEquals("0", publicTab.title.text());
		assertEquals("Attack", publicTab.filter.text());
		assertEquals(14, publicTab.title.values.get("Height"));
	}

	@Test
	public void explicitSkillSelectionsDisplayZeroAndThenTheirOwnSkillXp() throws Exception
	{
		for (Skill skill : PvmToolsPlugin.COMBAT_TRACKER_SKILLS)
		{
			Fixture fixture = new Fixture();
			fixture.choices.put(ComponentID.CHATBOX_TAB_GAME, ChatTabDisplay.valueOf(skill.name() + "_XP"));
			fixture.update();
			TabFixture game = fixture.tabs.get(ComponentID.CHATBOX_TAB_GAME);
			assertEquals("0", game.title.text());
			assertEquals(skill.getName(), game.filter.text());

			fixture.combatXp.put(skill, 123L);
			fixture.update();
			assertEquals("123", game.title.text());
			assertEquals(skill.getName(), game.filter.text());
		}
	}

	@Test
	public void clockCanUseAnotherTabAndTradeCanShowAnExplicitMetric() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.choices.put(ComponentID.CHATBOX_TAB_GAME, ChatTabDisplay.CLOCK);
		fixture.choices.put(ComponentID.CHATBOX_TAB_TRADE, ChatTabDisplay.SLAYER_XP);
		fixture.update();

		TabFixture clock = fixture.tabs.get(ComponentID.CHATBOX_TAB_GAME);
		assertTrue(clock.title.text(), clock.title.text().matches("\\d{2}:\\d{2}"));
		assertEquals("", clock.filter.text());
		TabFixture trade = fixture.tabs.get(ComponentID.CHATBOX_TAB_TRADE);
		assertEquals("0", trade.title.text());
		assertEquals("Slayer", trade.filter.text());
	}

	@Test
	public void nativeRedrawRestoresBeforeScriptAndCapturesNewStatusAndGeometry() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.choices.put(ComponentID.CHATBOX_TAB_PRIVATE, ChatTabDisplay.SLAYER_XP);
		TabFixture tab = fixture.tabs.get(ComponentID.CHATBOX_TAB_PRIVATE);
		fixture.update();
		assertNotEquals(tab.title.nativeValues, tab.title.values);

		fixture.plugin.onScriptPreFired(new ScriptPreFired(178));
		tab.assertNative();
		// The native redraw changes a filter and resized coordinates before recapture.
		tab.filter.widget.setText("Friends");
		tab.title.widget.setOriginalY(6);
		tab.title.widget.setRelativeY(8);
		tab.title.widget.setOriginalHeight(25);
		tab.title.widget.setHeight(27);
		tab.filter.widget.setOriginalY(19);
		tab.filter.widget.setRelativeY(21);
		Map<String, Object> newTitle = new HashMap<>(tab.title.values);
		Map<String, Object> newFilter = new HashMap<>(tab.filter.values);
		fixture.update();
		assertEquals("Slayer", tab.filter.text());
		fixture.enabled = false;
		fixture.update();

		assertEquals(newTitle, tab.title.values);
		assertEquals(newFilter, tab.filter.values);
		assertEquals("Friends", tab.filter.text());
	}

	@Test
	public void replacingATabWithSameIdRestoresOldChildrenAndPreservesNewNativeState() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.choices.put(ComponentID.CHATBOX_TAB_PRIVATE, ChatTabDisplay.SLAYER_XP);
		TabFixture previous = fixture.tabs.get(ComponentID.CHATBOX_TAB_PRIVATE);
		fixture.update();
		TabFixture replacement = new TabFixture(ComponentID.CHATBOX_TAB_PRIVATE, "Private", 40);
		fixture.tabs.put(ComponentID.CHATBOX_TAB_PRIVATE, replacement);
		fixture.update();
		previous.assertNative();
		assertEquals("Slayer", replacement.filter.text());
		fixture.enabled = false;
		fixture.update();
		replacement.assertNative();
	}

	@Test
	public void quickToggleRestoresAllTabsAndPreservesExplicitAndAutomaticChoices() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.choices.put(ComponentID.CHATBOX_TAB_CLAN, ChatTabDisplay.AUTOMATIC);
		fixture.choices.put(ComponentID.CHATBOX_TAB_PRIVATE, ChatTabDisplay.SLAYER_XP);
		fixture.choices.put(ComponentID.CHATBOX_TAB_GAME, ChatTabDisplay.CLOCK);
		fixture.choices.put(ComponentID.CHATBOX_TAB_TRADE, ChatTabDisplay.NET_PROFIT);
		set(fixture.plugin, "npcLootValue", 123L);
		Map<Integer, ChatTabDisplay> choices = new HashMap<>(fixture.choices);
		fixture.update();
		assertEquals("Loot", fixture.tabs.get(ComponentID.CHATBOX_TAB_CLAN).filter.text());
		assertTrue(fixture.plugin.isChatTrackersQuickEnabled());

		fixture.plugin.setChatTrackersQuickEnabled(false);
		fixture.runQueued();
		assertFalse(fixture.plugin.isChatTrackersQuickEnabled());
		for (TabFixture tab : fixture.tabs.values())
		{
			tab.assertNative();
		}
		assertEquals(choices, fixture.choices);
		assertEquals(Set.of("chatTabOverridesEnabled"), fixture.writes.keySet());
		assertEquals(false, fixture.writes.get("chatTabOverridesEnabled"));

		fixture.plugin.setChatTrackersQuickEnabled(true);
		fixture.runQueued();
		assertTrue(fixture.plugin.isChatTrackersQuickEnabled());
		assertEquals(choices, fixture.choices);
		assertEquals("Loot", fixture.tabs.get(ComponentID.CHATBOX_TAB_CLAN).filter.text());
		assertEquals("Slayer", fixture.tabs.get(ComponentID.CHATBOX_TAB_PRIVATE).filter.text());
		assertEquals("Profit", fixture.tabs.get(ComponentID.CHATBOX_TAB_TRADE).filter.text());
		assertTrue(fixture.tabs.get(ComponentID.CHATBOX_TAB_GAME).title.text().matches("\\d{2}:\\d{2}"));
		assertEquals(Set.of("chatTabOverridesEnabled"), fixture.writes.keySet());
		assertEquals(true, fixture.writes.get("chatTabOverridesEnabled"));
	}

	@Test
	public void singleTextAndEmptyParentFallbacksFitBothLinesAndRestoreExactNativeState() throws Exception
	{
		for (boolean emptyParentFallback : new boolean[]{false, true})
		{
			Fixture fixture = new Fixture();
			TabFixture tab = fixture.tabs.get(ComponentID.CHATBOX_TAB_PRIVATE);
			WidgetFixture text = emptyParentFallback ? tab.root : tab.title;
			tab.root.values.put("Children", emptyParentFallback ? null : new Widget[]{tab.title.widget});
			Map<String, Object> nativeRoot = new HashMap<>(tab.root.values);
			Map<String, Object> nativeText = new HashMap<>(text.values);
			fixture.choices.put(ComponentID.CHATBOX_TAB_PRIVATE, ChatTabDisplay.SLAYER_XP);
			fixture.update();

			assertEquals("0<br>Slayer", text.text());
			assertEquals(28, text.values.get("OriginalHeight"));
			assertEquals(28, text.values.get("Height"));
			fixture.choices.put(ComponentID.CHATBOX_TAB_PRIVATE, ChatTabDisplay.VANILLA);
			fixture.update();
			assertEquals(nativeRoot, tab.root.values);
			assertEquals(nativeText, text.values);
		}
	}

	private static final class Fixture
	{
		private static final int[] TABS = {ComponentID.CHATBOX_TAB_CLAN, ComponentID.CHATBOX_TAB_CHANNEL,
			ComponentID.CHATBOX_TAB_PRIVATE, ComponentID.CHATBOX_TAB_PUBLIC, ComponentID.CHATBOX_TAB_GAME,
			ComponentID.CHATBOX_TAB_TRADE};
		private final PvmToolsPlugin plugin = new PvmToolsPlugin();
		private final Map<Integer, TabFixture> tabs = new HashMap<>();
		private final Map<Integer, ChatTabDisplay> choices = new HashMap<>();
		private final Map<String, Object> writes = new HashMap<>();
		private final Deque<Runnable> queued = new ArrayDeque<>();
		private final EnumMap<Skill, Long> combatXp;
		private boolean enabled = true;

		@SuppressWarnings("unchecked")
		private Fixture() throws Exception
		{
			String[] names = {"Clan", "Channel", "Private", "Public", "Game", "Trade"};
			for (int i = 0; i < TABS.length; i++)
			{
				tabs.put(TABS[i], new TabFixture(TABS[i], names[i], 4 + i));
				choices.put(TABS[i], ChatTabDisplay.VANILLA);
			}
			Client client = mock(Client.class);
			when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
			when(client.isClientThread()).thenReturn(true);
			when(client.getTickCount()).thenReturn(100);
			when(client.getWidget(anyInt())).thenAnswer(call ->
			{
				TabFixture tab = tabs.get(call.getArgument(0));
				return tab == null ? null : tab.root.widget;
			});
			ConfigManager manager = mock(ConfigManager.class);
			doAnswer(call ->
			{
				String key = call.getArgument(1);
				Object value = call.getArgument(2);
				writes.put(key, value);
				if ("chatTabOverridesEnabled".equals(key))
				{
					enabled = (Boolean) value;
				}
				return null;
			}).when(manager).setConfiguration(eq(PvmToolsConfig.GROUP), anyString(), any(Object.class));
			when(manager.getConfiguration(anyString(), anyString())).thenAnswer(call ->
			{
				String key = call.getArgument(1);
				return "activeOwner".equals(key) ? "PVM" : "activeOwnerTick".equals(key) ? "99" : null;
			});
			PvmToolsConfig config = mock(PvmToolsConfig.class);
			when(config.chatTabOverridesEnabled()).thenAnswer(call -> enabled);
			when(config.clanChatTabDisplay()).thenAnswer(call -> choices.get(TABS[0]));
			when(config.channelChatTabDisplay()).thenAnswer(call -> choices.get(TABS[1]));
			when(config.privateChatTabDisplay()).thenAnswer(call -> choices.get(TABS[2]));
			when(config.publicChatTabDisplay()).thenAnswer(call -> choices.get(TABS[3]));
			when(config.gameChatTabDisplay()).thenAnswer(call -> choices.get(TABS[4]));
			when(config.tradeChatTabDisplay()).thenAnswer(call -> choices.get(TABS[5]));
			when(config.tradeButtonClock()).thenReturn(true);
			when(config.clanLootTracker()).thenReturn(true);
			ClientThread clientThread = mock(ClientThread.class);
			doAnswer(call ->
			{
				queued.add(call.getArgument(0));
				return null;
			}).when(clientThread).invokeLater(any(Runnable.class));
			set(plugin, "client", client);
			set(plugin, "configManager", manager);
			set(plugin, "config", config);
			set(plugin, "clientThread", clientThread);
			set(plugin, "started", true);
			combatXp = (EnumMap<Skill, Long>) get(plugin, "combatXpGainedBySkill");
		}

		private void update() throws Exception
		{
			invoke(plugin, "updateTradeButtonClock");
			invoke(plugin, "updateChatTabTrackers");
		}

		private void runQueued()
		{
			while (!queued.isEmpty())
			{
				queued.removeFirst().run();
			}
		}
	}

	private static final class TabFixture
	{
		private final WidgetFixture root;
		private final WidgetFixture title;
		private final WidgetFixture filter;

		private TabFixture(int id, String name, int seed)
		{
			root = new WidgetFixture(id, "", seed);
			// Native dynamic siblings can share their packed ID while differing in index.
			title = new WidgetFixture(id, name + "<br> ", seed + 1);
			filter = new WidgetFixture(id, "On", seed + 2);
			root.values.put("Children", new Widget[]{title.widget, filter.widget});
			root.nativeValues.put("Children", root.values.get("Children"));
		}

		private void assertNative()
		{
			assertEquals(root.nativeValues, root.values);
			assertEquals(title.nativeValues, title.values);
			assertEquals(filter.nativeValues, filter.values);
		}
	}

	private static final class WidgetFixture
	{
		private final Map<String, Object> values = new HashMap<>();
		private final Map<String, Object> nativeValues;
		private final Widget widget;

		private WidgetFixture(int id, String text, int seed)
		{
			values.put("Id", id);
			values.put("Text", text);
			values.put("FontId", 495 + seed);
			values.put("TextColor", 0x123400 + seed);
			values.put("TextShadowed", false);
			values.put("XTextAlignment", 0);
			values.put("YTextAlignment", 2);
			values.put("OriginalX", seed);
			values.put("OriginalY", seed + 1);
			values.put("RelativeX", seed + 2);
			values.put("RelativeY", seed + 3);
			values.put("OriginalWidth", 56 + seed);
			values.put("OriginalHeight", 22 + seed);
			values.put("Width", 58 + seed);
			values.put("Height", 24 + seed);
			nativeValues = new HashMap<>(values);
			widget = mock(Widget.class, call ->
			{
				String method = call.getMethod().getName();
				if (method.startsWith("get") && values.containsKey(method.substring(3)))
				{
					return values.get(method.substring(3));
				}
				if (method.startsWith("set") && values.containsKey(method.substring(3)))
				{
					values.put(method.substring(3), call.getArgument(0));
					return call.getMethod().getReturnType() == Void.TYPE ? null : call.getMock();
				}
				return org.mockito.Mockito.RETURNS_DEFAULTS.answer(call);
			});
		}

		private String text()
		{
			return widget.getText();
		}
	}

	private static void invoke(Object target, String name) throws Exception
	{
		Method method = target.getClass().getDeclaredMethod(name);
		method.setAccessible(true);
		method.invoke(target);
	}

	private static Object get(Object target, String name) throws Exception
	{
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static void set(Object target, String name, Object value) throws Exception
	{
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
