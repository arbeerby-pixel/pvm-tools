package com.arber.pvmtools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PvmChatTabOverlayTest
{
	@Test
	public void disablingRestoresLabelsAndStylesAndReenablingUsesCurrentTotals() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.update();
		assertNotEquals("Clan", fixture.clan.getText());
		assertNotEquals("Trade", fixture.trade.getText());

		fixture.setVisible(false);
		assertEquals("Clan", fixture.clan.getText());
		assertEquals("Trade", fixture.trade.getText());
		verify(fixture.clan).setFontId(42);
		verify(fixture.clan).setTextColor(123);
		verify(fixture.trade).setFontId(42);
		assertEquals(1000L, field(fixture.plugin, "npcLootValue"));
		assertTrue(fixture.config.clanLootTracker());
		assertTrue(fixture.config.tradeButtonClock());

		setField(fixture.plugin, "npcLootValue", 2000L);
		fixture.update();
		assertEquals("Clan", fixture.clan.getText());
		assertEquals("Trade", fixture.trade.getText());

		fixture.setVisible(true);
		assertNotEquals("Clan", fixture.clan.getText());
		assertNotEquals("Trade", fixture.trade.getText());
		assertEquals(2000L, field(fixture.plugin, "npcLootValue"));
	}

	@Test
	public void disabledAtStartupLeavesChatWidgetsUntouched() throws Exception
	{
		Fixture fixture = new Fixture();
		when(fixture.config.showChatTabOverlays()).thenReturn(false);
		fixture.update();
		assertEquals("Clan", fixture.clan.getText());
		assertEquals("Trade", fixture.trade.getText());
		verify(fixture.client, never()).getWidget(anyInt());
	}

	@Test
	public void quickToggleOnlyChangesMasterSetting() throws Exception
	{
		Fixture fixture = new Fixture();
		assertTrue(fixture.plugin.isChatTrackersQuickEnabled());
		fixture.plugin.setChatTrackersQuickEnabled(false);
		verify(fixture.manager).setConfiguration(PvmToolsConfig.GROUP, "showChatTabOverlays", false);
		verifyNoMoreInteractions(fixture.manager);
		when(fixture.config.showChatTabOverlays()).thenReturn(false);
		assertFalse(fixture.plugin.isChatTrackersQuickEnabled());
	}

	private static final class Fixture
	{
		private final PvmToolsPlugin plugin = new PvmToolsPlugin();
		private final PvmToolsConfig config = mock(PvmToolsConfig.class, CALLS_REAL_METHODS);
		private final Client client = mock(Client.class);
		private final ConfigManager manager = mock(ConfigManager.class);
		private final Widget clan = widget(ComponentID.CHATBOX_TAB_CLAN, "Clan");
		private final Widget trade = widget(ComponentID.CHATBOX_TAB_TRADE, "Trade");

		private Fixture() throws Exception
		{
			setField(plugin, "config", config);
			setField(plugin, "client", client);
			setField(plugin, "configManager", manager);
			setField(plugin, "clientThread", mock(ClientThread.class));
			setField(plugin, "npcLootValue", 1000L);
			when(manager.getConfiguration("arberToolkitUi", "activeOwner")).thenReturn("PVM");
			when(client.getTickCount()).thenReturn(10);
			when(client.getWidget(ComponentID.CHATBOX_TAB_CLAN)).thenReturn(clan);
			when(client.getWidget(ComponentID.CHATBOX_TAB_TRADE)).thenReturn(trade);
		}

		private void update() throws Exception
		{
			invoke(plugin, "updateChatTabTrackers");
			invoke(plugin, "updateTradeButtonClock");
		}

		private void setVisible(boolean visible) throws Exception
		{
			when(config.showChatTabOverlays()).thenReturn(visible);
			ConfigChanged event = new ConfigChanged();
			event.setGroup(PvmToolsConfig.GROUP);
			event.setKey("showChatTabOverlays");
			Method method = PvmToolsPlugin.class.getDeclaredMethod("onConfigChangedOnClientThread", ConfigChanged.class);
			method.setAccessible(true);
			method.invoke(plugin, event);
		}
	}

	private static Widget widget(int id, String originalText)
	{
		Widget widget = mock(Widget.class);
		AtomicReference<String> text = new AtomicReference<>(originalText);
		when(widget.getId()).thenReturn(id);
		when(widget.getText()).thenAnswer(call -> text.get());
		when(widget.getFontId()).thenReturn(42);
		when(widget.getTextColor()).thenReturn(123);
		doAnswer(call ->
		{
			text.set(call.getArgument(0));
			return widget;
		}).when(widget).setText(anyString());
		return widget;
	}

	private static Object field(Object target, String name) throws Exception
	{
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static void setField(Object target, String name, Object value) throws Exception
	{
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static void invoke(Object target, String name) throws Exception
	{
		Method method = target.getClass().getDeclaredMethod(name);
		method.setAccessible(true);
		method.invoke(target);
	}
}
