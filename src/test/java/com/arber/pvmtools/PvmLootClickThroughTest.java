package com.arber.pvmtools;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WorldType;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PvmLootClickThroughTest
{
	private static final int ABYSSAL_WHIP = 4151;

	@Test
	public void visibleLootUnderAnNpcBecomesTheLeftClickOption() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.plugin.onPostMenuSort(new PostMenuSort());

		verify(fixture.take).setForceLeftClick(true);
		assertArrayEquals(new MenuEntry[]{fixture.attack, fixture.take}, fixture.entries);
		verify(fixture.client).setMenuEntries(fixture.entries);
	}

	@Test
	public void menuIsLeftAloneInTheWilderness() throws Exception
	{
		Fixture fixture = new Fixture();
		when(fixture.client.getVarbitValue(VarbitID.INSIDE_WILDERNESS)).thenReturn(1);
		fixture.plugin.onPostMenuSort(new PostMenuSort());

		fixture.assertUntouched();
	}

	@Test
	public void menuIsLeftAloneOnPvpWorlds() throws Exception
	{
		Fixture fixture = new Fixture();
		when(fixture.client.getWorldType()).thenReturn(EnumSet.of(WorldType.PVP));
		fixture.plugin.onPostMenuSort(new PostMenuSort());

		fixture.assertUntouched();
	}

	@Test
	public void menuWithAPlayerEntryIsLeftAlone() throws Exception
	{
		Fixture fixture = new Fixture();
		MenuEntry player = mock(MenuEntry.class);
		when(player.getPlayer()).thenReturn(mock(Player.class));
		fixture.entries = new MenuEntry[]{fixture.take, player, fixture.attack};
		when(fixture.client.getMenuEntries()).thenReturn(fixture.entries);
		fixture.plugin.onPostMenuSort(new PostMenuSort());

		fixture.assertUntouched();
	}

	private static final class Fixture
	{
		private final PvmToolsPlugin plugin = new PvmToolsPlugin();
		private final Client client = mock(Client.class);
		private final MenuEntry take = mock(MenuEntry.class);
		private final MenuEntry attack = mock(MenuEntry.class);
		private MenuEntry[] entries = {take, attack};

		private Fixture() throws Exception
		{
			when(take.getOption()).thenReturn("Take");
			when(take.getType()).thenReturn(MenuAction.GROUND_ITEM_THIRD_OPTION);
			when(take.getItemId()).thenReturn(ABYSSAL_WHIP);
			when(take.getParam0()).thenReturn(10);
			when(take.getParam1()).thenReturn(12);
			when(attack.getOption()).thenReturn("Attack");
			when(attack.getType()).thenReturn(MenuAction.NPC_SECOND_OPTION);
			when(attack.getNpc()).thenReturn(mock(NPC.class));

			TileItem whip = mock(TileItem.class);
			when(whip.getId()).thenReturn(ABYSSAL_WHIP);
			when(whip.getQuantity()).thenReturn(1);
			Tile tile = mock(Tile.class);
			when(tile.getGroundItems()).thenReturn(List.of(whip));
			Tile[][][] tiles = new Tile[4][104][104];
			tiles[0][10][12] = tile;
			Scene scene = mock(Scene.class);
			when(scene.getTiles()).thenReturn(tiles);

			when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
			when(client.getWorldType()).thenReturn(EnumSet.noneOf(WorldType.class));
			when(client.getScene()).thenReturn(scene);
			when(client.getMenuEntries()).thenAnswer(call -> entries);
			PvmToolsConfig config = mock(PvmToolsConfig.class);
			when(config.lootClickThrough()).thenReturn(true);
			ItemManager itemManager = mock(ItemManager.class);
			ItemComposition composition = mock(ItemComposition.class);
			when(composition.getName()).thenReturn("Abyssal whip");
			when(itemManager.getItemComposition(ABYSSAL_WHIP)).thenReturn(composition);

			set("client", client);
			set("config", config);
			set("configManager", mock(ConfigManager.class));
			set("itemManager", itemManager);
			set("started", true);
		}

		private void assertUntouched()
		{
			verify(take, never()).setForceLeftClick(true);
			verify(client, never()).setMenuEntries(any());
		}

		private void set(String name, Object value) throws Exception
		{
			Field field = PvmToolsPlugin.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(plugin, value);
		}
	}
}
