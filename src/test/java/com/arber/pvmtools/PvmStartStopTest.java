package com.arber.pvmtools;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PvmStartStopTest
{
	@Test
	public void startUpRefreshesTheChatTabLatchFromTheSavedSetting() throws Exception
	{
		Fixture fixture = new Fixture();
		// The setting was switched back on while the plugin was off, so no ConfigChanged reached it.
		fixture.set("chatTabOverridesSuppressed", true);
		fixture.start();

		assertEquals(false, fixture.get("chatTabOverridesSuppressed"));
		assertTrue(fixture.plugin.isChatTrackersQuickEnabled());
		fixture.stop();
	}

	@Test
	public void shutDownLeavesSavingAndClearingStateToTheClientThread() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.start();
		fixture.set("cannonPlaced", true);
		fixture.set("npcLootValue", 1_234L);
		fixture.set("lootTrackerPersistenceDirty", true);
		fixture.stop();

		// Nothing the client thread owns is touched from the Swing thread.
		assertEquals(true, fixture.get("cannonPlaced"));
		assertEquals(true, fixture.get("lootTrackerPersistenceDirty"));
		// Once before startUp adds it, once more for shutDown.
		verify(fixture.overlayManager, times(2)).remove(fixture.lifetimeOverlay);

		fixture.queued.removeLast().run();
		assertEquals(false, fixture.get("cannonPlaced"));
		assertEquals(false, fixture.get("lootTrackerPersistenceDirty"));
		verify(fixture.lifetimeOverlay).reset();
	}

	@Test
	public void restartBeforeTheClientThreadRanStillSavesAndClearsTheOldStateFirst() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.start();
		fixture.set("cannonPlaced", true);
		fixture.set("lootTrackerPersistenceDirty", true);
		fixture.stop();
		Runnable shutdownOnClientThread = fixture.queued.removeLast();
		fixture.start();

		assertEquals(false, fixture.get("cannonPlaced"));
		assertEquals(false, fixture.get("lootTrackerPersistenceDirty"));

		// The late client-thread half must not wipe the state of the running plugin.
		fixture.set("cannonPlaced", true);
		shutdownOnClientThread.run();
		assertEquals(true, fixture.get("cannonPlaced"));
		assertEquals(true, fixture.get("started"));
		fixture.stop();
	}

	private static final class Fixture
	{
		private final PvmToolsPlugin plugin = new PvmToolsPlugin();
		private final Deque<Runnable> queued = new ArrayDeque<>();
		private final OverlayManager overlayManager = mock(OverlayManager.class);
		private final GroundItemLifetimeTextOverlay lifetimeOverlay = mock(GroundItemLifetimeTextOverlay.class);

		private Fixture() throws Exception
		{
			Client client = mock(Client.class);
			when(client.isClientThread()).thenReturn(true);
			ClientThread clientThread = mock(ClientThread.class);
			doAnswer(call ->
			{
				queued.add(call.getArgument(0));
				return null;
			}).when(clientThread).invokeLater(any(Runnable.class));
			PvmToolsConfig config = mock(PvmToolsConfig.class, call ->
				Modifier.isAbstract(call.getMethod().getModifiers())
					? RETURNS_DEFAULTS.answer(call) : CALLS_REAL_METHODS.answer(call));
			set("client", client);
			set("clientThread", clientThread);
			set("config", config);
			set("configManager", mock(ConfigManager.class));
			set("itemManager", mock(ItemManager.class));
			set("infoBoxManager", mock(InfoBoxManager.class));
			set("pluginManager", mock(PluginManager.class));
			set("hooks", mock(Hooks.class));
			set("overlayManager", overlayManager);
			set("groundItemLifetimeTextOverlay", lifetimeOverlay);
			set("clientToolbar", mock(ClientToolbar.class));
		}

		private void start() throws Exception
		{
			// The plugin manager starts and stops plugins on the Swing thread.
			SwingUtilities.invokeAndWait(plugin::startUp);
		}

		private void stop() throws Exception
		{
			SwingUtilities.invokeAndWait(plugin::shutDown);
		}

		private Object get(String name) throws Exception
		{
			Field field = PvmToolsPlugin.class.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(plugin);
		}

		private void set(String name, Object value) throws Exception
		{
			Field field = PvmToolsPlugin.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(plugin, value);
		}
	}
}
