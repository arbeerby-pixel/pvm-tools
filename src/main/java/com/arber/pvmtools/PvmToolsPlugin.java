package com.arber.pvmtools;

import com.google.inject.Provides;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.GameState;
import net.runelite.api.GameObject;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.VarPlayer;
import net.runelite.api.WidgetNode;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemQuantityChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PostClientTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.FlashNotification;
import net.runelite.client.config.Notification;
import net.runelite.client.config.NotificationSound;
import net.runelite.client.config.RequestFocusType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.Text;
import static net.runelite.client.util.RSTimeUnit.GAME_TICKS;

@PluginDescriptor(
	name = "PvM Tools",
	configName = PvmToolsConfig.GROUP,
	description = "Combat potion timers, drop reminders, Slayer history, and a persistent PvM statistics panel.",
	tags = {"pvm", "combat", "slayer", "potions", "timers", "prayer", "inventory", "superior", "cannon", "clock", "loot", "drops", "stats"},
	enabledByDefault = true
)
public class PvmToolsPlugin extends Plugin
{
	private static final int FLASH_COUNT = 3;
	private static final int MAX_WARNING_ALPHA = 255;
	private static final int POISON_TICK_LENGTH = 30;
	private static final int OVERLOAD_TICK_LENGTH = 25;
	private static final int ANTIFIRE_TICK_LENGTH = 30;
	private static final int SUPER_ANTIFIRE_TICK_LENGTH = 20;
	private static final int REGULAR_ANTIFIRE_TICKS = 600;
	private static final int REGULAR_SUPER_ANTIFIRE_TICKS = 300;
	private static final int INVENTORY_SIZE = 28;
	private static final int CANNON_STATE_PLACED = 4;
	private static final int COLLECTION_LOG_POPUP_WIDGET = 660;
	private static final int COLLECTION_LOG_POPUP_SCRIPT = 3343;
	private static final int COLLECTION_LOG_POPUP_LAYER = 3;
	private static final int COLLECTION_LOG_POPUP_RESIZED_CHILD = 13;
	private static final int COLLECTION_LOG_POPUP_FIXED_CHILD = 43;
	private static final int COLLECTION_LOG_POPUP_CONTENT_CHILD = 1;
	private static final int NPC_DROP_TICK_WINDOW = 8;
	private static final int NPC_DROP_DISTANCE = 8;
	private static final int PENDING_PICKUP_TICK_WINDOW = 50;
	private static final int COMBAT_WARNING_GRACE_TICKS = 16;
	private static final int CANNON_PICKUP_SUPPRESS_TICKS = 6;
	private static final int UPDATE_SCROLL_READY_TICKS = 2;
	private static final int SLAYER_TASK_COMPLETION_CONFIRM_TICKS = 5;
	private static final long CANNON_ESTIMATE_WINDOW_MILLIS = Duration.ofMinutes(5).toMillis();
	private static final long MIN_CANNON_ESTIMATE_WINDOW_MILLIS = Duration.ofSeconds(20).toMillis();
	private static final String SAVED_LOOT_TRACKER_KEY = "savedLootTrackerValue";
	private static final String SAVED_SUPPLY_COST_TRACKER_KEY = "savedSupplyCostTrackerValue";
	private static final String SAVED_SUPPLY_COST_BREAKDOWN_KEY = "savedSupplyCostTrackerBreakdown";
	private static final String SAVED_COMBAT_XP_TRACKER_KEY = "savedCombatXpTrackerValuesV2";
	private static final String SAVED_SLAYER_XP_TRACKER_KEY = "savedSlayerXpTrackerValueV2";
	private static final String SAVED_STATS_DAY_KEY = "savedStatsDayV1";
	private static final String SAVED_STATS_WEEK_KEY = "savedStatsWeekV1";
	private static final String SAVED_STATS_MONTH_KEY = "savedStatsMonthV1";
	private static final String SAVED_STATS_YEAR_KEY = "savedStatsYearV1";
	private static final String SAVED_STATS_ALL_TIME_KEY = "savedStatsAllTimeV1";
	private static final String SAVED_CURRENT_SLAYER_TASK_KEY = "savedCurrentSlayerTaskV1";
	private static final String SAVED_SLAYER_TASK_HISTORY_KEY = "savedSlayerTaskHistoryV1";
	private static final String TRACKER_DEFAULTS_INITIALIZED_KEY = "trackerDefaultsInitializedV1";
	private static final String SEEN_UPDATE_SCROLL_VERSION_KEY = "seenUpdateScrollVersionV2";
	private static final String TASK_HISTORY_ENTRY_SEPARATOR = "\n";
	private static final String TASK_HISTORY_ENTRY_SEPARATOR_PATTERN = "\\R";
	private static final String TASK_STATE_SEPARATOR = "\t";
	private static final String TOOLKIT_UI_COORDINATION_GROUP = "arberToolkitUi";
	private static final String TOOLKIT_UI_OWNER_KEY = "activeOwner";
	private static final String TOOLKIT_UI_OWNER_TICK_KEY = "activeOwnerTick";
	private static final String TOOLKIT_UI_LAST_TOGGLE_TICK_KEY = "lastToggleTick";
	private static final String TOOLKIT_UI_OWNER_PVM = "PVM";
	private static final String TOOLKIT_UI_OWNER_SKILLING = "SKILLING";
	private static final String PVM_PLUGIN_CLASS_NAME = "com.arber.pvmtools.PvmToolsPlugin";
	private static final String SKILLING_PLUGIN_CLASS_NAME = "com.arber.skillingtoolkit.SkillingToolkitPlugin";
	private static final int LOOT_TRACKER_COLOR = 0x00FF00;
	private static final int SUPPLY_TRACKER_COLOR = 0xFF4040;
	private static final int COMBAT_XP_TRACKER_COLOR = 0xFFFF00;
	private static final int SLAYER_XP_TRACKER_COLOR = 0x40E0FF;
	private static final int DEFAULT_CHAT_TAB_COLOR = 0xFFFFFF;
	private static final int CHAT_TAB_VALUE_Y_OFFSET = 0;
	private static final int CHAT_TAB_LABEL_Y_OFFSET = 10;
	private static final int CHAT_TAB_TRACKER_TEXT_HEIGHT = 14;
	private static final int CHAT_TAB_LABEL_COLOR = 0xC8BFA7;
	private static final int STATS_NAVIGATION_PRIORITY = 0;
	private static final int STATS_NAVIGATION_ICON_SIZE = 24;
	private static final String[] UPDATE_SCROLL_NOTES = {
		"Potion and prayer timers keep combat boosts easy to follow.",
		"Drop reminders can alert you to valuable NPC loot.",
		"The PvM panel tracks loot, supplies, XP, and Slayer tasks.",
		"Superior alerts and inventory helpers make Slayer trips easier."
	};
	private static final int[] CHAT_TAB_TRACKER_SLOT_COMPONENTS = {
		ComponentID.CHATBOX_TAB_CLAN,
		ComponentID.CHATBOX_TAB_CHANNEL,
		ComponentID.CHATBOX_TAB_PRIVATE,
		ComponentID.CHATBOX_TAB_PUBLIC,
		ComponentID.CHATBOX_TAB_GAME
	};
	private static final String[] CHAT_TAB_TRACKER_SLOT_NAMES = {
		"clan",
		"channel",
		"private",
		"public",
		"game"
	};
	static final Skill[] COMBAT_TRACKER_SKILLS = {
		Skill.ATTACK,
		Skill.STRENGTH,
		Skill.DEFENCE,
		Skill.RANGED,
		Skill.MAGIC,
		Skill.HITPOINTS
	};
	private static final Notification SOUND_ONLY_NOTIFICATION = Notification.ON
		.withInitialized(true)
		.withOverride(true)
		.withTray(false)
		.withRequestFocus(RequestFocusType.OFF)
		.withSound(NotificationSound.CUSTOM)
		.withVolume(100)
		.withGameMessage(false)
		.withFlash(FlashNotification.DISABLED)
		.withSendWhenFocused(true);
	private static final Notification VALUABLE_DROP_SOUND_NOTIFICATION = Notification.ON
		.withInitialized(true)
		.withOverride(true)
		.withTray(false)
		.withRequestFocus(RequestFocusType.OFF)
		.withSound(NotificationSound.NATIVE)
		.withVolume(100)
		.withGameMessage(false)
		.withFlash(FlashNotification.DISABLED)
		.withSendWhenFocused(true);
	private static final DateTimeFormatter TRADE_CLOCK_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
	@Inject
	private Client client;

	@Inject
	private PvmToolsConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private Hooks hooks;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private CombatPotionWarningOverlay warningOverlay;

	@Inject
	private PvmToolsUpdatePanel updatePanel;

	@Inject
	private Notifier notifier;

	@Inject
	private ClientToolbar clientToolbar;

	private final Map<CombatPotionTimerType, CombatPotionTimerInfoBox> timers = new EnumMap<>(CombatPotionTimerType.class);
	private final Hooks.RenderableDrawListener drawListener = this::shouldDrawRenderable;
	private final Map<Integer, String> chatTabOriginalTexts = new HashMap<>();
	private final Map<Integer, ChatTabTextStyle> chatTabOriginalStyles = new HashMap<>();
	private final Map<Integer, String> activeChatTabTexts = new HashMap<>();
	private final Map<Integer, ChatTabTrackerType> activeChatTabTrackerSlots = new HashMap<>();
	private final Set<Integer> activeChatTabOverrides = new HashSet<>();
	private final List<ChatTabTrackerType> activeChatTabTrackerOrder = new ArrayList<>();
	private final List<NpcDeathDropSource> recentNpcDeaths = new ArrayList<>();
	private final List<PendingGroundItemPickup> pendingGroundItemPickups = new ArrayList<>();
	private final Map<GroundItemKey, Integer> npcDropQuantities = new HashMap<>();
	private final Set<GroundItemKey> valuableDropAlertKeys = new HashSet<>();
	private final Map<Integer, String> itemDisplayNames = new ConcurrentHashMap<>();
	private final Set<Integer> pendingItemDisplayNames = ConcurrentHashMap.newKeySet();
	private final EnumMap<Skill, Integer> lastSkillExperience = new EnumMap<>(Skill.class);
	private final EnumMap<Skill, Integer> trackerBaseExperience = new EnumMap<>(Skill.class);
	private final EnumMap<Skill, Long> combatXpGainedBySkill = new EnumMap<>(Skill.class);
	private final EnumMap<PvmToolsStatsPeriod, PvmToolsStats> statsByPeriod = new EnumMap<>(PvmToolsStatsPeriod.class);
	private final Deque<PvmTaskHistoryEntry> taskHistory = new ArrayDeque<>();
	private final Deque<CannonballUsageSample> cannonballUsageSamples = new ArrayDeque<>();

	private int nextPoisonTick = -1;
	private int nextOverloadRefreshTick = -1;
	private int nextAntifireTick = -1;
	private int nextSuperAntifireTick = -1;
	private int lastAntifireTicks = -1;
	private int lastSuperAntifireTicks = -1;
	private long estimatedPrayerEndTimeMillis;
	private int lastPrayerLevel = -1;
	private int lastPrayerBonus = Integer.MIN_VALUE;
	private int lastPrayerDrainEffect = -1;
	private boolean prayerCountdownActive;
	private boolean prayerExpirySoundTriggered;
	private long flashSequenceStartMillis = -1L;
	private boolean flashSequenceOneShot;
	private long valuableDropFlashSequenceStartMillis = -1L;
	private WidgetNode warningPopupWidgetNode;
	private boolean antiVenomPlusActive;
	private CombatPotionTimerType antifireType;
	private CombatPotionTimerType superAntifireType;
	private InventorySpacesInfoBox inventorySpacesInfoBox;
	private volatile boolean started;
	private volatile boolean updateScrollVisible;
	private volatile boolean updateScrollDisplayScheduled;
	private boolean updateScrollPreviewRequested;
	private int updateScrollReadyTicks;
	private int updateScrollGeneration;
	private String pluginVersion;
	private boolean inventoryFullWarningTriggered;
	private boolean cannonPlaced;
	private boolean cannonEmptyWarningTriggered;
	private boolean cannonRepairWarningTriggered;
	private boolean cannonNeedsRepair;
	private int cannonballsLeft;
	private int pendingCannonEmptyWarningTick = -1;
	private int cannonPickupSuppressUntilTick = -1;
	private WorldArea cannonPosition;
	private String lastTradeClockText = "";
	private long npcLootValue;
	private long supplyCostValue;
	private long potionSupplyCostValue;
	private long foodSupplyCostValue;
	private long cannonballSupplyCostValue;
	private long potionSupplyDoseCount;
	private long foodSupplyCount;
	private long cannonballSupplyCount;
	private long slayerXpGained;
	private String currentSlayerTaskName = "";
	private String currentSlayerTaskLocation = "";
	private int currentSlayerTaskAmount;
	private int currentSlayerTaskInitialAmount;
	private long currentSlayerTaskStartMillis;
	private long currentSlayerTaskElapsedMillis;
	private long currentSlayerTaskActiveSinceMillis;
	private long currentSlayerTaskLootValue;
	private long currentSlayerTaskSupplyCostValue;
	private long currentSlayerTaskCombatXp;
	private long currentSlayerTaskSlayerXp;
	private long currentSlayerTaskPotionDoseCount;
	private long currentSlayerTaskFoodCount;
	private long currentSlayerTaskCannonballCount;
	private boolean slayerTaskObservedActive;
	private int pendingSlayerTaskCompletionTicks;
	private boolean trackerValuesLoaded;
	private PvmToolsStatsPanel statsPanel;
	private NavigationButton statsNavigationButton;
	private int lastCombatActivityTick = Integer.MIN_VALUE;
	private final EnumSet<CombatPotionTimerType> warningTriggered = EnumSet.noneOf(CombatPotionTimerType.class);
	private final EnumSet<CombatPotionTimerType> potionExpirySoundTriggered = EnumSet.noneOf(CombatPotionTimerType.class);

	@Provides
	PvmToolsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PvmToolsConfig.class);
	}

	private void addStatsNavigation()
	{
		if (statsNavigationButton != null)
		{
			return;
		}

		statsPanel = new PvmToolsStatsPanel(this);
		BufferedImage icon = loadNavigationIcon();
		statsNavigationButton = NavigationButton.builder()
			.tooltip("PvM Tools Stats")
			.icon(icon)
			.panel(statsPanel)
			.priority(STATS_NAVIGATION_PRIORITY)
			.build();
		clientToolbar.addNavigation(statsNavigationButton);
		if (isDeveloperMode())
		{
			SwingUtilities.invokeLater(() -> clientToolbar.openPanel(statsNavigationButton));
		}
	}

	private void removeStatsNavigation()
	{
		if (statsNavigationButton != null)
		{
			clientToolbar.removeNavigation(statsNavigationButton);
			statsNavigationButton = null;
		}

		statsPanel = null;
	}

	private BufferedImage loadNavigationIcon()
	{
		try
		{
			BufferedImage icon = ImageUtil.loadImageResource(PvmToolsPlugin.class, "/com/arber/pvmtools/icon.png");
			if (icon == null)
			{
				icon = ImageUtil.loadImageResource(PvmToolsPlugin.class, "/icon.png");
			}

			if (icon != null)
			{
				return ImageUtil.resizeImage(icon, STATS_NAVIGATION_ICON_SIZE, STATS_NAVIGATION_ICON_SIZE, true);
			}
		}
		catch (RuntimeException ignored)
		{
			// Missing resources should never prevent the plugin itself from starting.
		}

		return itemManager.getImage(ItemID.COINS_995);
	}

	private boolean isDeveloperMode()
	{
		return System.getProperty("sun.java.command", "").contains("--developer-mode");
	}

	private void updateUpdateScroll()
	{
		if (config.dontShowUpdateScroll() && !updateScrollPreviewRequested)
		{
			return;
		}

		if (!isUpdateScrollWorldReady())
		{
			updateScrollReadyTicks = 0;
			return;
		}

		if (updateScrollVisible || updateScrollDisplayScheduled)
		{
			return;
		}

		if (!updateScrollPreviewRequested
			&& getPluginVersion().equals(configManager.getConfiguration(
				PvmToolsConfig.GROUP,
				SEEN_UPDATE_SCROLL_VERSION_KEY)))
		{
			return;
		}

		updateScrollReadyTicks++;
		if (updateScrollReadyTicks < UPDATE_SCROLL_READY_TICKS)
		{
			return;
		}

		showUpdateScroll();
	}

	private boolean isUpdateScrollWorldReady()
	{
		if (!started || !isLoggedIn() || client.getLocalPlayer() == null || client.getCanvas() == null)
		{
			return false;
		}

		return !isClickToPlayScreenVisible();
	}

	private boolean isClickToPlayScreenVisible()
	{
		Widget clickToPlayRoot = client.getWidget(WidgetID.LOGIN_CLICK_TO_PLAY_GROUP_ID, 0);
		return clickToPlayRoot != null
			&& !clickToPlayRoot.isHidden()
			&& clickToPlayRoot.getWidth() > 0
			&& clickToPlayRoot.getHeight() > 0;
	}

	private void showUpdateScroll()
	{
		Canvas canvas = client.getCanvas();
		if (canvas == null)
		{
			return;
		}

		String version = getPluginVersion();
		int generation = ++updateScrollGeneration;
		updateScrollDisplayScheduled = true;
		SwingUtilities.invokeLater(() ->
		{
			if (!started || generation != updateScrollGeneration)
			{
				updateScrollDisplayScheduled = false;
				return;
			}

			boolean shown = updatePanel.showPanel(
				canvas,
				version,
				UPDATE_SCROLL_NOTES,
				() -> dismissUpdateScroll(version),
				() -> disableUpdateScroll(version));
			updateScrollVisible = shown;
			updateScrollDisplayScheduled = false;
			if (!shown)
			{
				updateScrollReadyTicks = 0;
			}
		});
	}

	private void dismissUpdateScroll(String version)
	{
		updateScrollVisible = false;
		updateScrollPreviewRequested = false;
		updateScrollReadyTicks = 0;
		configManager.setConfiguration(
			PvmToolsConfig.GROUP,
			SEEN_UPDATE_SCROLL_VERSION_KEY,
			version);
	}

	private void disableUpdateScroll(String version)
	{
		configManager.setConfiguration(
			PvmToolsConfig.GROUP,
			"dontShowUpdateScroll",
			true);
		dismissUpdateScroll(version);
		refreshOpenDontShowCheckbox();
	}

	private void refreshOpenDontShowCheckbox()
	{
		SwingUtilities.invokeLater(() ->
		{
			Canvas canvas = client.getCanvas();
			JRootPane rootPane = canvas == null ? null : SwingUtilities.getRootPane(canvas);
			if (rootPane != null)
			{
				selectDontShowCheckbox(rootPane);
			}
		});
	}

	private boolean selectDontShowCheckbox(Component component)
	{
		if (!(component instanceof Container))
		{
			return false;
		}

		Component[] children = ((Container) component).getComponents();
		JLabel matchingLabel = null;
		JCheckBox matchingCheckbox = null;
		for (Component child : children)
		{
			if (child instanceof JLabel
				&& "Don't show updates".equals(((JLabel) child).getText()))
			{
				matchingLabel = (JLabel) child;
			}
			else if (child instanceof JCheckBox)
			{
				matchingCheckbox = (JCheckBox) child;
			}
		}

		if (matchingLabel != null && matchingCheckbox != null)
		{
			matchingCheckbox.setSelected(true);
			matchingCheckbox.repaint();
			return true;
		}

		for (Component child : children)
		{
			if (selectDontShowCheckbox(child))
			{
				return true;
			}
		}
		return false;
	}

	private void migrateUpdateScrollSetting()
	{
		String oldValue = configManager.getConfiguration(
			PvmToolsConfig.GROUP,
			"showUpdateScroll");
		String newValue = configManager.getConfiguration(
			PvmToolsConfig.GROUP,
			"dontShowUpdateScroll");
		if (oldValue != null && newValue == null)
		{
			configManager.setConfiguration(
				PvmToolsConfig.GROUP,
				"dontShowUpdateScroll",
				!Boolean.parseBoolean(oldValue));
		}

		configManager.unsetConfiguration(
			PvmToolsConfig.GROUP,
			"showUpdateScroll");
	}

	private void hideUpdateScroll()
	{
		updateScrollGeneration++;
		updateScrollVisible = false;
		updateScrollDisplayScheduled = false;
		updateScrollPreviewRequested = false;
		updatePanel.hidePanel();
	}

	private String getPluginVersion()
	{
		if (pluginVersion != null)
		{
			return pluginVersion;
		}

		try (InputStream input = PvmToolsPlugin.class.getResourceAsStream("/runelite-plugin.properties"))
		{
			if (input != null)
			{
				Properties properties = new Properties();
				properties.load(input);
				pluginVersion = properties.getProperty("version", "dev");
				return pluginVersion;
			}
		}
		catch (IOException ignored)
		{
			// A missing version must not prevent the plugin from starting.
		}

		pluginVersion = "dev";
		return pluginVersion;
	}

	@Override
	protected void startUp()
	{
		started = true;
		updateScrollVisible = false;
		updateScrollDisplayScheduled = false;
		updateScrollPreviewRequested = false;
		updateScrollReadyTicks = 0;
		updateScrollGeneration++;
		slayerTaskObservedActive = false;
		pendingSlayerTaskCompletionTicks = 0;
		clearPluginInfoBoxes();
		hooks.registerRenderableDrawListener(drawListener);
		overlayManager.remove(warningOverlay);
		overlayManager.add(warningOverlay);
		resetFamilyState();
		initializeTrackerDefaults();
		loadTrackerValues();
		loadStatsValues();
		loadCurrentSlayerTaskState();
		loadSlayerTaskHistory();
		migrateUpdateScrollSetting();
		syncSlayerTaskFromRuneLite();
		addStatsNavigation();
		syncAllTimersLater();
	}

	private void initializeTrackerDefaults()
	{
		if (config.trackerDefaultsInitialized())
		{
			return;
		}

		configManager.setConfiguration(PvmToolsConfig.GROUP, "trackerMode", TrackerPersistenceMode.FOREVER);
		configManager.setConfiguration(PvmToolsConfig.GROUP, "clanLootTracker", true);
		configManager.setConfiguration(PvmToolsConfig.GROUP, "publicSupplyCostTracker", true);
		configManager.setConfiguration(PvmToolsConfig.GROUP, "channelCombatXpTracker", true);
		configManager.setConfiguration(PvmToolsConfig.GROUP, "privateSlayerXpTracker", true);
		configManager.setConfiguration(PvmToolsConfig.GROUP, "topXpSkillTracker", true);
		configManager.setConfiguration(PvmToolsConfig.GROUP, TRACKER_DEFAULTS_INITIALIZED_KEY, true);
	}

	@Override
	protected void shutDown()
	{
		pauseCurrentSlayerTaskTimer();
		persistCurrentSlayerTaskState();
		started = false;
		hideUpdateScroll();
		hooks.unregisterRenderableDrawListener(drawListener);
		overlayManager.remove(warningOverlay);
		closeWarningPopupInterface();
		removeStatsNavigation();
		restoreChatTabOverridesLater();
		clearTimers();
		removeInventoryInfoBox();
		clearPluginInfoBoxes();
		clearNpcDropTracking();
		itemDisplayNames.clear();
		pendingItemDisplayNames.clear();
		resetFamilyState();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			syncAllTimers();
			return;
		}

		if (isResetGameState(event.getGameState()))
		{
			hideUpdateScroll();
			updateScrollReadyTicks = 0;
			slayerTaskObservedActive = false;
			pendingSlayerTaskCompletionTicks = 0;
			pauseCurrentSlayerTaskTimer();
			persistCurrentSlayerTaskState();
			restoreChatTabOverrides();
			clearNpcDropTracking();
			clearTimers();
			removeInventoryInfoBox();
			resetFamilyState();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (PvmToolsConfig.GROUP.equals(event.getGroup()))
		{
			if ("previewUpdateScroll".equals(event.getKey()) && config.previewUpdateScroll())
			{
				updateScrollPreviewRequested = true;
				updateScrollReadyTicks = UPDATE_SCROLL_READY_TICKS;
				configManager.setConfiguration(
					PvmToolsConfig.GROUP,
					"previewUpdateScroll",
					false);
			}

			if ("dontShowUpdateScroll".equals(event.getKey()))
			{
				if (config.dontShowUpdateScroll())
				{
					hideUpdateScroll();
				}
				else
				{
					updateScrollReadyTicks = 0;
				}
			}

			if ("tradeButtonClock".equals(event.getKey()) && !config.tradeButtonClock())
			{
				restoreChatTabOverrideLater(ComponentID.CHATBOX_TAB_TRADE);
			}

			if ("showPotionTimers".equals(event.getKey()) && !config.showPotionTimers())
			{
				removePotionTimers();
				removePrayerTimer();
				resetPrayerCountdown();
			}

			if ("showInventorySpaces".equals(event.getKey()))
			{
				clientThread.invokeLater(() ->
				{
					if (config.showInventorySpaces())
					{
						syncInventoryInfoBox();
					}
					else
					{
						removeInventoryInfoBox();
					}
				});
			}

			if (isChatTabTrackerConfigKey(event.getKey()))
			{
				updateChatTabTrackersLater();
			}

			if ("trackerMode".equals(event.getKey()))
			{
				loadTrackerValues();
			}

			if ("resetSelectedTracker".equals(event.getKey()) && config.resetSelectedTracker())
			{
				resetSelectedTracker();
			}

			syncAllTimersLater();
			refreshStatsPanel();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INVENTORY.getId())
		{
			syncInventoryInfoBox();
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (config.flashSuperiorSpawns() && isSuperiorSlayerMonster(event.getNpc()))
		{
			startFlashSequence();
			showWarningPopup(
				"Superior Slayer spawn",
				getSuperiorSpawnWarningMessage(event.getNpc())
			);
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (!isLoggedIn() || !(event.getActor() instanceof NPC))
		{
			return;
		}

		markCombatActivity();
		NPC npc = (NPC) event.getActor();
		recentNpcDeaths.add(new NpcDeathDropSource(npc.getWorldLocation(), client.getTickCount()));
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		if (!isLoggedIn() || !isNearRecentNpcDeath(event.getTile().getWorldLocation()))
		{
			return;
		}

		addNpcDropQuantity(event.getTile(), event.getItem().getId(), event.getItem().getQuantity());
	}

	@Subscribe
	public void onItemQuantityChanged(ItemQuantityChanged event)
	{
		if (!isLoggedIn())
		{
			return;
		}

		int quantityChange = event.getNewQuantity() - event.getOldQuantity();
		if (quantityChange > 0 && isNearRecentNpcDeath(event.getTile().getWorldLocation()))
		{
			addNpcDropQuantity(event.getTile(), event.getItem().getId(), quantityChange);
			return;
		}

		if (quantityChange < 0)
		{
			countPickedUpNpcDrop(event.getTile(), event.getItem().getId(), -quantityChange);
		}
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		if (!isLoggedIn())
		{
			return;
		}

		countPickedUpNpcDrop(event.getTile(), event.getItem().getId(), event.getItem().getQuantity());
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (isOwnBrokenCannon(event.getGameObject()))
		{
			setCannonNeedsRepair(true);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (isOwnBrokenCannon(event.getGameObject()))
		{
			setCannonNeedsRepair(false);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (handleToolkitTabToggle(event))
		{
			return;
		}

		if (isNpcCombatInteraction(event))
		{
			markCombatActivity();
		}

		if (isCannonPickupInteraction(event))
		{
			suppressCannonPickupWarnings();
		}

		trackGroundItemTake(event);
		trackSupplyConsumption(event);

		if (!config.superiorExamineHints() || event.getMenuAction() != MenuAction.EXAMINE_NPC)
		{
			return;
		}

		NPC npc = event.getMenuEntry().getNpc();
		SuperiorSlayerHint hint = getSuperiorSlayerHint(npc);
		if (hint == null)
		{
			return;
		}

		client.addChatMessage(
			ChatMessageType.GAMEMESSAGE,
			"",
			"PvM Tools: Pray " + hint.getPrayer() + ". Tip: " + hint.getTip(),
			null
		);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (!isLoggedIn())
		{
			return;
		}

		if (event.getVarpId() == VarPlayer.POISON)
		{
			syncAntiVenomTimer();
		}

		if (event.getVarpId() == VarPlayerID.ROCKTHROWER)
		{
			updateCannonballs(event.getValue());
		}
		else if (event.getVarpId() == VarPlayerID.OWNEDMCANNON)
		{
			WorldPoint cannonWorldPoint = WorldPoint.fromCoord(event.getValue());
			cannonPosition = buildCannonWorldArea(cannonWorldPoint);
		}
		else if (event.getVarpId() == VarPlayerID.DROPCANNON)
		{
			cannonPlaced = event.getValue() == CANNON_STATE_PLACED;
			if (!cannonPlaced)
			{
				suppressCannonPickupWarnings();
				resetCannonState();
			}
		}

		switch (event.getVarbitId())
		{
			case VarbitID.NZONE_OVERLOAD_POTION_EFFECTS:
			case VarbitID.RAIDS_OVERLOAD_TIMER:
			case VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS:
			case VarbitID.RAIDS_CLIENT_INDUNGEON:
				syncOverloadTimers();
				break;
			case VarbitID.ANTIFIRE_POTION:
				syncAntifireTimers();
				break;
			case VarbitID.SUPER_ANTIFIRE_POTION:
				syncSuperAntifireTimers();
				break;
			case VarbitID.DIVINEATTACK_POTION_TIME:
			case VarbitID.DIVINESTRENGTH_POTION_TIME:
			case VarbitID.DIVINEDEFENCE_POTION_TIME:
			case VarbitID.DIVINERANGE_POTION_TIME:
			case VarbitID.DIVINEMAGIC_POTION_TIME:
			case VarbitID.DIVINECOMBAT_POTION_TIME:
			case VarbitID.DIVINEBASTION_POTION_TIME:
			case VarbitID.DIVINEBATTLEMAGE_POTION_TIME:
				syncDivineTimers();
				break;
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (!isLoggedIn())
		{
			return;
		}

		updateTrackedSkillXp(event.getSkill(), event.getXp());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!isLoggedIn())
		{
			return;
		}

		syncPrayerTimer();
		syncInventoryInfoBox();
		cleanupNpcDropTracking();
		syncTrackerSkillBaselines();
		syncSlayerTaskFromRuneLite();
		checkPendingCannonEmptyWarning();
		cleanupCannonballUsageSamples(System.currentTimeMillis());
		updateUpdateScroll();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!isLoggedIn() || !isPotionMessage(event.getType()))
		{
			return;
		}

		String message = Text.removeTags(event.getMessage()).toLowerCase(Locale.ENGLISH);
		handleCannonChatMessage(message);

		if (!message.startsWith("you drink"))
		{
			return;
		}

		if (message.contains("anti-venom+"))
		{
			antiVenomPlusActive = true;
			syncAntiVenomTimer();
			return;
		}

		if (message.contains("anti-venom"))
		{
			antiVenomPlusActive = false;
			syncAntiVenomTimer();
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (!isLoggedIn())
		{
			restoreChatTabOverrides();
			return;
		}

		ensureToolkitOwnerAvailable();

		updatePrayerTimerDisplay();
		checkPotionExpirySounds();
	}

	@Subscribe
	public void onPostClientTick(PostClientTick event)
	{
		if (!isLoggedIn())
		{
			restoreChatTabOverrides();
			return;
		}

		updateTradeButtonClock();
		updateChatTabTrackers();
	}

	Color getScreenFlashColor()
	{
		long now = System.currentTimeMillis();
		int warningAlpha = config.flashScreenWarning() ? getWarningFlashAlpha(now) : 0;
		int valuableDropAlpha = getValuableDropFlashAlpha(now);
		if (valuableDropAlpha > 0)
		{
			return new Color(0, 255, 80, valuableDropAlpha);
		}

		return warningAlpha > 0 ? new Color(255, 0, 0, warningAlpha) : null;
	}

	private int getWarningFlashAlpha(long now)
	{
		boolean hasWarning = false;
		boolean startFlashSequence = false;
		List<String> warningMessages = null;
		warningTriggered.retainAll(timers.keySet());

		Iterator<Map.Entry<CombatPotionTimerType, CombatPotionTimerInfoBox>> iterator = timers.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<CombatPotionTimerType, CombatPotionTimerInfoBox> entry = iterator.next();
			CombatPotionTimerType type = entry.getKey();
			CombatPotionTimerInfoBox timer = entry.getValue();
			if (!shouldFlashTimer(type))
			{
				warningTriggered.remove(type);
				continue;
			}

			if (timer.isInWarningWindow())
			{
				hasWarning = true;
				if (!warningTriggered.contains(type))
				{
					warningTriggered.add(type);
					startFlashSequence = true;
					warningMessages = addWarningMessage(warningMessages, getTimerWarningMessage(type, timer));
				}
			}
			else
			{
				warningTriggered.remove(type);
			}
		}

		boolean inventoryFullWarning = config.flashInventoryFull()
			&& inventorySpacesInfoBox != null
			&& inventorySpacesInfoBox.getFreeSpaces() == 0;
		if (inventoryFullWarning)
		{
			hasWarning = true;
			if (!inventoryFullWarningTriggered)
			{
				inventoryFullWarningTriggered = true;
				startFlashSequence = true;
				warningMessages = addWarningMessage(warningMessages, "Make inventory space.");
			}
		}
		else
		{
			inventoryFullWarningTriggered = false;
		}

		if (config.warnCannonEmpty() && cannonPlaced && cannonballsLeft <= 0 && cannonEmptyWarningTriggered)
		{
			hasWarning = true;
		}

		if (config.warnCannonRepair() && cannonNeedsRepair && cannonRepairWarningTriggered)
		{
			hasWarning = true;
		}

		if (startFlashSequence)
		{
			startFlashSequence(now);
			showWarningPopup("Warning", warningMessages);
		}

		if (flashSequenceStartMillis < 0L)
		{
			return 0;
		}

		if (!hasWarning && !flashSequenceOneShot)
		{
			resetFlashSequence();
			closeWarningPopupInterface();
			return 0;
		}

		long elapsed = now - flashSequenceStartMillis;
		long totalSequenceDuration = getWarningSequenceDurationMillis();
		if (elapsed >= totalSequenceDuration)
		{
			resetFlashSequence();
			return 0;
		}

		long phase = elapsed / (getWarningFlashSeconds() * 1000L);
		return phase % 2L == 0L ? getWarningAlpha() : 0;
	}

	private int getValuableDropFlashAlpha(long now)
	{
		if (!config.valuableDropFlash() || valuableDropAlertKeys.isEmpty())
		{
			valuableDropAlertKeys.clear();
			valuableDropFlashSequenceStartMillis = -1L;
			return 0;
		}

		if (valuableDropFlashSequenceStartMillis < 0L)
		{
			return 0;
		}

		long elapsed = now - valuableDropFlashSequenceStartMillis;
		if (elapsed >= getWarningSequenceDurationMillis())
		{
			valuableDropFlashSequenceStartMillis = -1L;
			return 0;
		}

		long phase = elapsed / (getWarningFlashSeconds() * 1000L);
		return phase % 2L == 0L ? Math.max(90, getWarningAlpha()) : 0;
	}

	private void startFlashSequence()
	{
		startFlashSequence(System.currentTimeMillis(), true);
	}

	private void startFlashSequence(long now)
	{
		startFlashSequence(now, false);
	}

	private void startFlashSequence(long now, boolean oneShot)
	{
		flashSequenceStartMillis = now;
		flashSequenceOneShot = oneShot;
	}

	private void resetFlashSequence()
	{
		flashSequenceStartMillis = -1L;
		flashSequenceOneShot = false;
	}

	private void showWarningPopup(String title, String message)
	{
		showWarningPopup(title, Collections.singletonList(message));
	}

	private void showWarningPopup(String title, List<String> messages)
	{
		if (messages == null || messages.isEmpty())
		{
			return;
		}

		if (!config.warningPopup())
		{
			return;
		}

		String description = buildCollectionLogPopupDescription(title, messages);
		clientThread.invokeLater(() -> openCollectionLogPopup("PvM Tools", description));
	}

	private String buildCollectionLogPopupDescription(String title, List<String> messages)
	{
		StringBuilder description = new StringBuilder();
		description
			.append("<col=ff4040>")
			.append(title)
			.append("</col><br><br><col=ffffff>");

		for (int i = 0; i < messages.size(); i++)
		{
			if (i > 0)
			{
				description.append("<br>");
			}

			description.append(messages.get(i));
		}

		description.append("</col>");
		return description.toString();
	}

	private void openCollectionLogPopup(String title, String description)
	{
		if (!started || !config.warningPopup() || client.getGameState() == GameState.LOGIN_SCREEN)
		{
			return;
		}

		int topLevelInterfaceId = client.getTopLevelInterfaceId();
		if (topLevelInterfaceId < 0)
		{
			return;
		}

		closeWarningPopupInterface();

		int componentId = topLevelInterfaceId << 16
			| (client.isResized() ? COLLECTION_LOG_POPUP_RESIZED_CHILD : COLLECTION_LOG_POPUP_FIXED_CHILD);
		warningPopupWidgetNode = client.openInterface(componentId, COLLECTION_LOG_POPUP_WIDGET, COLLECTION_LOG_POPUP_LAYER);
		String popupTitle = ColorUtil.wrapWithColorTag(title, ColorUtil.fromHex("ffb000"));
		client.runScript(COLLECTION_LOG_POPUP_SCRIPT, popupTitle, description, -1);
		WidgetNode widgetNode = warningPopupWidgetNode;
		clientThread.invokeLater(() -> closeWarningPopupWhenDone(widgetNode));
	}

	private boolean closeWarningPopupWhenDone(WidgetNode widgetNode)
	{
		Widget widget = client.getWidget(COLLECTION_LOG_POPUP_WIDGET, COLLECTION_LOG_POPUP_CONTENT_CHILD);
		if (widget != null && widget.getWidth() > 0)
		{
			return false;
		}

		if (warningPopupWidgetNode == widgetNode)
		{
			closeWarningPopupInterface();
		}

		return true;
	}

	private void closeWarningPopupInterface()
	{
		if (warningPopupWidgetNode == null)
		{
			return;
		}

		try
		{
			client.closeInterface(warningPopupWidgetNode, true);
		}
		catch (IllegalArgumentException ignored)
		{
			// The popup can already be gone by the time RuneLite asks us to clean it up.
		}
		finally
		{
			warningPopupWidgetNode = null;
		}
	}

	private static List<String> addWarningMessage(List<String> messages, String message)
	{
		if (messages == null)
		{
			messages = new ArrayList<>();
		}

		messages.add(message);
		return messages;
	}

	private String getTimerWarningMessage(CombatPotionTimerType type, CombatPotionTimerInfoBox timer)
	{
		if (type == CombatPotionTimerType.PRAYER)
		{
			return "Drink prayer potion.";
		}

		return "Re-dose " + type.getDisplayName() + ".";
	}

	int getWarningSeconds()
	{
		return Math.max(3, Math.min(60, config.warningSeconds()));
	}

	private int getWarningAlpha()
	{
		int intensity = Math.max(0, Math.min(100, config.warningIntensity()));
		return MAX_WARNING_ALPHA * intensity / 100;
	}

	private long getWarningFlashSeconds()
	{
		return Math.max(1, Math.min(5, config.warningFlashSeconds()));
	}

	private long getWarningSequenceDurationMillis()
	{
		return getWarningFlashSeconds() * 1000L * FLASH_COUNT * 2L;
	}

	private void syncAllTimers()
	{
		if (!isLoggedIn())
		{
			clearTimers();
			removeInventoryInfoBox();
			resetFamilyState();
			return;
		}

		syncOverloadTimers();
		syncAntifireTimers();
		syncSuperAntifireTimers();
		syncAntiVenomTimer();
		syncDivineTimers();
		syncPrayerTimer();
		syncInventoryInfoBox();
		syncCannonState();
		syncTrackerSkillBaselines();
	}

	private void syncAllTimersLater()
	{
		clientThread.invokeLater(() ->
		{
			if (started)
			{
				syncAllTimers();
			}
		});
	}

	private void syncOverloadTimers()
	{
		int tickCount = client.getTickCount();
		int blightedOverload = client.getVarbitValue(VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS);
		int normalOverload = Math.max(
			client.getVarbitValue(VarbitID.NZONE_OVERLOAD_POTION_EFFECTS),
			client.getVarbitValue(VarbitID.RAIDS_OVERLOAD_TIMER)
		);

		if (blightedOverload > 0)
		{
			if (nextOverloadRefreshTick - tickCount <= 0)
			{
				nextOverloadRefreshTick = tickCount + OVERLOAD_TICK_LENGTH;
			}

			updateTimer(
				CombatPotionTimerType.BLIGHTED_OVERLOAD,
				nextOverloadRefreshTick - tickCount + (blightedOverload - 1) * OVERLOAD_TICK_LENGTH
			);
			removeTimers(CombatPotionTimerType.OVERLOAD, CombatPotionTimerType.RAID_OVERLOAD);
		}
		else if (normalOverload > 0)
		{
			if (nextOverloadRefreshTick - tickCount <= 0)
			{
				nextOverloadRefreshTick = tickCount + OVERLOAD_TICK_LENGTH;
			}

			CombatPotionTimerType overloadType = client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1
				? CombatPotionTimerType.RAID_OVERLOAD
				: CombatPotionTimerType.OVERLOAD;
			updateTimer(
				overloadType,
				nextOverloadRefreshTick - tickCount + (normalOverload - 1) * OVERLOAD_TICK_LENGTH
			);
			removeTimer(overloadType == CombatPotionTimerType.RAID_OVERLOAD ? CombatPotionTimerType.OVERLOAD : CombatPotionTimerType.RAID_OVERLOAD);
			removeTimer(CombatPotionTimerType.BLIGHTED_OVERLOAD);
		}
		else
		{
			nextOverloadRefreshTick = -1;
			expireTimers(CombatPotionTimerType.OVERLOAD, CombatPotionTimerType.RAID_OVERLOAD, CombatPotionTimerType.BLIGHTED_OVERLOAD);
		}

	}

	private void syncAntifireTimers()
	{
		int antifireValue = client.getVarbitValue(VarbitID.ANTIFIRE_POTION);
		int tickCount = client.getTickCount();

		if (antifireValue <= 0)
		{
			nextAntifireTick = -1;
			lastAntifireTicks = -1;
			antifireType = null;
			expireTimers(CombatPotionTimerType.ANTIFIRE, CombatPotionTimerType.EXTENDED_ANTIFIRE);
			return;
		}

		if (nextAntifireTick - tickCount <= 0)
		{
			nextAntifireTick = tickCount + ANTIFIRE_TICK_LENGTH;
		}

		int ticks = nextAntifireTick - tickCount + (antifireValue - 1) * ANTIFIRE_TICK_LENGTH;
		boolean increased = ticks > lastAntifireTicks;
		if (antifireType == CombatPotionTimerType.EXTENDED_ANTIFIRE && !increased)
		{
			antifireType = CombatPotionTimerType.EXTENDED_ANTIFIRE;
		}
		else
		{
			antifireType = ticks > REGULAR_ANTIFIRE_TICKS ? CombatPotionTimerType.EXTENDED_ANTIFIRE : CombatPotionTimerType.ANTIFIRE;
		}

		lastAntifireTicks = ticks;
		removeTimers(CombatPotionTimerType.ANTIFIRE, CombatPotionTimerType.EXTENDED_ANTIFIRE);
		updateTimer(antifireType, ticks);
	}

	private void syncSuperAntifireTimers()
	{
		int superAntifireValue = client.getVarbitValue(VarbitID.SUPER_ANTIFIRE_POTION);
		int tickCount = client.getTickCount();

		if (superAntifireValue <= 0)
		{
			nextSuperAntifireTick = -1;
			lastSuperAntifireTicks = -1;
			superAntifireType = null;
			expireTimers(CombatPotionTimerType.SUPER_ANTIFIRE, CombatPotionTimerType.EXTENDED_SUPER_ANTIFIRE);
			return;
		}

		if (nextSuperAntifireTick - tickCount <= 0)
		{
			nextSuperAntifireTick = tickCount + SUPER_ANTIFIRE_TICK_LENGTH;
		}

		int ticks = nextSuperAntifireTick - tickCount + (superAntifireValue - 1) * SUPER_ANTIFIRE_TICK_LENGTH;
		boolean increased = ticks > lastSuperAntifireTicks;
		if (superAntifireType == CombatPotionTimerType.EXTENDED_SUPER_ANTIFIRE && !increased)
		{
			superAntifireType = CombatPotionTimerType.EXTENDED_SUPER_ANTIFIRE;
		}
		else
		{
			superAntifireType = ticks > REGULAR_SUPER_ANTIFIRE_TICKS
				? CombatPotionTimerType.EXTENDED_SUPER_ANTIFIRE
				: CombatPotionTimerType.SUPER_ANTIFIRE;
		}

		lastSuperAntifireTicks = ticks;
		removeTimers(CombatPotionTimerType.SUPER_ANTIFIRE, CombatPotionTimerType.EXTENDED_SUPER_ANTIFIRE);
		updateTimer(superAntifireType, ticks);
	}

	private void syncAntiVenomTimer()
	{
		int poisonValue = client.getVarpValue(VarPlayer.POISON);
		int tickCount = client.getTickCount();
		if (!isAntiVenomActive(poisonValue))
		{
			nextPoisonTick = -1;
			antiVenomPlusActive = false;
			expireTimer(CombatPotionTimerType.ANTI_VENOM_PLUS);
			return;
		}

		if (nextPoisonTick - tickCount <= 0)
		{
			nextPoisonTick = tickCount + POISON_TICK_LENGTH;
		}

		int ticks = nextPoisonTick - tickCount + Math.abs((poisonValue + 39) * POISON_TICK_LENGTH);
		if (!antiVenomPlusActive)
		{
			removeTimer(CombatPotionTimerType.ANTI_VENOM_PLUS);
			return;
		}

		updateTimer(CombatPotionTimerType.ANTI_VENOM_PLUS, ticks);
	}

	private void syncDivineTimers()
	{
		int divineAttack = client.getVarbitValue(VarbitID.DIVINEATTACK_POTION_TIME);
		int divineStrength = client.getVarbitValue(VarbitID.DIVINESTRENGTH_POTION_TIME);
		int divineDefence = client.getVarbitValue(VarbitID.DIVINEDEFENCE_POTION_TIME);
		int divineRanging = client.getVarbitValue(VarbitID.DIVINERANGE_POTION_TIME);
		int divineMagic = client.getVarbitValue(VarbitID.DIVINEMAGIC_POTION_TIME);
		int divineCombat = client.getVarbitValue(VarbitID.DIVINECOMBAT_POTION_TIME);
		int divineBastion = client.getVarbitValue(VarbitID.DIVINEBASTION_POTION_TIME);
		int divineBattlemage = client.getVarbitValue(VarbitID.DIVINEBATTLEMAGE_POTION_TIME);

		updateEnabledTimer(
			CombatPotionTimerType.DIVINE_SUPER_ATTACK,
			!(divineCombat >= divineAttack && divineCombat > 0),
			divineAttack
		);
		updateEnabledTimer(
			CombatPotionTimerType.DIVINE_SUPER_STRENGTH,
			!(divineCombat >= divineStrength && divineCombat > 0),
			divineStrength
		);
		updateEnabledTimer(
			CombatPotionTimerType.DIVINE_SUPER_DEFENCE,
			!(divineCombat >= divineDefence && divineCombat > 0)
				&& !(divineBastion >= divineDefence && divineBastion > 0)
				&& !(divineBattlemage >= divineDefence && divineBattlemage > 0),
			divineDefence
		);
		updateEnabledTimer(
			CombatPotionTimerType.DIVINE_RANGING,
			!(divineBastion >= divineRanging && divineBastion > 0),
			divineRanging
		);
		updateEnabledTimer(
			CombatPotionTimerType.DIVINE_MAGIC,
			!(divineBattlemage >= divineMagic && divineBattlemage > 0),
			divineMagic
		);
		updateEnabledTimer(CombatPotionTimerType.DIVINE_SUPER_COMBAT, true, divineCombat);
		updateEnabledTimer(CombatPotionTimerType.DIVINE_BASTION, true, divineBastion);
		updateEnabledTimer(CombatPotionTimerType.DIVINE_BATTLEMAGE, true, divineBattlemage);
	}

	private void syncPrayerTimer()
	{
		if (!config.showPotionTimers())
		{
			removePrayerTimer();
			resetPrayerCountdown();
			return;
		}

		if (!isAnyPrayerActive())
		{
			if (client.getBoostedSkillLevel(Skill.PRAYER) <= 0 && (prayerCountdownActive || lastPrayerLevel > 0))
			{
				triggerPrayerExpiryPing();
			}

			removePrayerTimer();
			resetPrayerCountdown();
			return;
		}

		int drainEffect = getPrayerDrainEffect();
		if (drainEffect <= 0)
		{
			removePrayerTimer();
			resetPrayerCountdown();
			return;
		}

		int prayerBonus = getPrayerBonus();
		int currentPrayer = client.getBoostedSkillLevel(Skill.PRAYER);
		if (currentPrayer > lastPrayerLevel)
		{
			prayerExpirySoundTriggered = false;
		}

		updatePrayerCountdownAnchor(currentPrayer, prayerBonus, drainEffect);
		updatePrayerTimerDisplay();
	}

	private void syncInventoryInfoBox()
	{
		if (!isLoggedIn() || !config.showInventorySpaces() || !isToolkitUiReady())
		{
			removeInventoryInfoBox();
			return;
		}

		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null)
		{
			removeInventoryInfoBox();
			return;
		}

		int occupiedSlots = getOccupiedInventorySlots(inventory);
		if (occupiedSlots == 0)
		{
			removeInventoryInfoBox();
			return;
		}

		int freeSpaces = Math.max(0, INVENTORY_SIZE - occupiedSlots);
		long inventoryValue = getInventoryValue(inventory);
		if (inventorySpacesInfoBox == null)
		{
			inventorySpacesInfoBox = new InventorySpacesInfoBox(freeSpaces, this);
			infoBoxManager.addInfoBox(inventorySpacesInfoBox);
		}
		else
		{
			if (inventorySpacesInfoBox.setFreeSpaces(freeSpaces))
			{
				infoBoxManager.updateInfoBoxImage(inventorySpacesInfoBox);
			}
		}

		inventorySpacesInfoBox.setTooltip(buildInventoryTooltip(inventoryValue));
	}

	private void updatePrayerTimerDisplay()
	{
		if (!prayerCountdownActive || !config.showPotionTimers())
		{
			removePrayerTimer();
			return;
		}

		long remainingMillis = Math.max(0L, estimatedPrayerEndTimeMillis - System.currentTimeMillis());
		if (remainingMillis <= 0L)
		{
			removePrayerTimer();
			return;
		}

		long pseudoTicks = Math.max(1L, (remainingMillis + 599L) / 600L);
		updateDurationTimer(CombatPotionTimerType.PRAYER, Duration.ofMillis(remainingMillis), pseudoTicks);
	}

	private void updatePrayerCountdownAnchor(int currentPrayer, int prayerBonus, int drainEffect)
	{
		boolean prayerJumped = lastPrayerLevel >= 0 && Math.abs(currentPrayer - lastPrayerLevel) > 1;
		boolean shouldResetCountdown = !prayerCountdownActive
			|| currentPrayer > lastPrayerLevel
			|| prayerJumped
			|| prayerBonus != lastPrayerBonus
			|| drainEffect != lastPrayerDrainEffect;

		if (shouldResetCountdown)
		{
			long durationMillis = Math.max(0L, Math.round(getEstimatedPrayerSecondsRemaining(currentPrayer, prayerBonus, drainEffect) * 1000d));
			estimatedPrayerEndTimeMillis = System.currentTimeMillis() + durationMillis;
			prayerCountdownActive = true;
		}

		lastPrayerLevel = currentPrayer;
		lastPrayerBonus = prayerBonus;
		lastPrayerDrainEffect = drainEffect;
	}

	private boolean isAnyPrayerActive()
	{
		for (PrayerDrainEntry prayerDrainEntry : PrayerDrainEntry.values())
		{
			if (prayerDrainEntry.isActive(client))
			{
				return true;
			}
		}

		return false;
	}

	private int getPrayerDrainEffect()
	{
		int drainEffect = 0;

		for (PrayerDrainEntry prayerDrainEntry : PrayerDrainEntry.values())
		{
			if (prayerDrainEntry.isActive(client))
			{
				drainEffect += prayerDrainEntry.getDrainEffect();
			}
		}

		return drainEffect;
	}

	private int getPrayerBonus()
	{
		ItemContainer equipped = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipped == null)
		{
			return 0;
		}

		int totalPrayerBonus = 0;
		for (Item item : equipped.getItems())
		{
			ItemStats itemStats = itemManager.getItemStats(item.getId());
			if (itemStats != null && itemStats.getEquipment() != null)
			{
				totalPrayerBonus += itemStats.getEquipment().getPrayer();
			}
		}

		return totalPrayerBonus;
	}

	private double getEstimatedPrayerSecondsRemaining(int currentPrayer, int prayerBonus, int drainEffect)
	{
		int drainResistance = 2 * prayerBonus + 60;
		return Math.max(0d, currentPrayer * 0.6d * ((double) drainResistance / drainEffect));
	}

	private void updateEnabledTimer(CombatPotionTimerType type, boolean enabled, int ticks)
	{
		if (!enabled || ticks <= 0)
		{
			if (enabled && ticks <= 0)
			{
				expireTimer(type);
			}
			else
			{
				removeTimer(type);
			}

			return;
		}

		updateTimer(type, ticks);
	}

	private void updateTimer(CombatPotionTimerType type, int ticks)
	{
		updateDurationTimer(type, Duration.of(ticks, GAME_TICKS), ticks);
	}

	private void updateDurationTimer(CombatPotionTimerType type, Duration duration, long sortValue)
	{
		if (!isTypeVisible(type) || sortValue <= 0)
		{
			removeTimer(type);
			return;
		}

		CombatPotionTimerInfoBox timer = timers.get(type);
		if (timer == null || sortValue > timer.getSortValue())
		{
			removeTimer(type);
			timer = new CombatPotionTimerInfoBox(type, duration, itemManager.getImage(type.getImageItemId()), this);
			infoBoxManager.addInfoBox(timer);
			timers.put(type, timer);
		}
		else
		{
			timer.updateDuration(duration);
		}

		timer.setSortValue(sortValue);
		if (isPotionTimerType(type) && isPositive(timer.getRemainingDuration()))
		{
			potionExpirySoundTriggered.remove(type);
		}
	}

	private void checkPotionExpirySounds()
	{
		potionExpirySoundTriggered.retainAll(timers.keySet());
		if (!config.soundPotionExpired())
		{
			potionExpirySoundTriggered.clear();
			return;
		}

		for (Map.Entry<CombatPotionTimerType, CombatPotionTimerInfoBox> entry : timers.entrySet())
		{
			CombatPotionTimerType type = entry.getKey();
			if (!isPotionTimerType(type))
			{
				continue;
			}

			CombatPotionTimerInfoBox timer = entry.getValue();
			Duration remaining = timer.getRemainingDuration();
			if (!isPositive(remaining) && !potionExpirySoundTriggered.contains(type))
			{
				potionExpirySoundTriggered.add(type);
				playWarningPing();
			}
			else if (isPositive(remaining))
			{
				potionExpirySoundTriggered.remove(type);
			}
		}
	}

	private void triggerPrayerExpiryPing()
	{
		if (!config.soundPotionExpired() || prayerExpirySoundTriggered)
		{
			return;
		}

		prayerExpirySoundTriggered = true;
		playWarningPing();
	}

	private void syncCannonState()
	{
		cannonballsLeft = client.getVarpValue(VarPlayerID.ROCKTHROWER);
		cannonPlaced = client.getVarpValue(VarPlayerID.DROPCANNON) == CANNON_STATE_PLACED;
		int cannonCoord = client.getVarpValue(VarPlayerID.OWNEDMCANNON);
		if (cannonCoord > 0)
		{
			cannonPosition = buildCannonWorldArea(WorldPoint.fromCoord(cannonCoord));
		}
	}

	private void updateCannonballs(int newCannonballsLeft)
	{
		int oldCannonballsLeft = cannonballsLeft;
		cannonballsLeft = newCannonballsLeft;
		trackCannonballSupplyCost(oldCannonballsLeft, newCannonballsLeft);
		if (!cannonPlaced || !isCannonCurrentlyPlaced() || isCannonPickupSuppressed())
		{
			cannonEmptyWarningTriggered = false;
			pendingCannonEmptyWarningTick = -1;
			return;
		}

		if (cannonballsLeft > 0)
		{
			cannonEmptyWarningTriggered = false;
			pendingCannonEmptyWarningTick = -1;
			return;
		}

		if (oldCannonballsLeft > 0 || !cannonEmptyWarningTriggered)
		{
			pendingCannonEmptyWarningTick = client.getTickCount() + 2;
		}
	}

	private void checkPendingCannonEmptyWarning()
	{
		if (pendingCannonEmptyWarningTick < 0 || client.getTickCount() < pendingCannonEmptyWarningTick)
		{
			return;
		}

		pendingCannonEmptyWarningTick = -1;
		if (cannonPlaced && isCannonCurrentlyPlaced() && !isCannonPickupSuppressed() && cannonballsLeft <= 0)
		{
			triggerCannonEmptyWarning();
		}
	}

	private void triggerCannonEmptyWarning()
	{
		if (cannonEmptyWarningTriggered || !cannonPlaced || !isCannonCurrentlyPlaced() || isCannonPickupSuppressed())
		{
			return;
		}

		cannonEmptyWarningTriggered = true;
		if (config.warnCannonEmpty())
		{
			startFlashSequence(System.currentTimeMillis());
			showWarningPopup("Cannon", "Reload cannon.");
		}

		if (config.soundCannonEmpty())
		{
			playWarningPing();
		}

		if (config.notifyCannonWarnings())
		{
			notifier.notify("Your cannon is out of ammo.");
		}
	}

	private void setCannonNeedsRepair(boolean needsRepair)
	{
		cannonNeedsRepair = needsRepair;
		if (!needsRepair)
		{
			cannonRepairWarningTriggered = false;
			return;
		}

		if (!cannonRepairWarningTriggered)
		{
			cannonRepairWarningTriggered = true;
			if (config.warnCannonRepair())
			{
				startFlashSequence(System.currentTimeMillis());
				showWarningPopup("Cannon", "Repair cannon.");
			}

			if (config.notifyCannonWarnings())
			{
				notifier.notify("Your cannon needs repair.");
			}
		}
	}

	private void resetCannonState()
	{
		cannonPlaced = false;
		cannonPosition = null;
		cannonNeedsRepair = false;
		cannonEmptyWarningTriggered = false;
		cannonRepairWarningTriggered = false;
		cannonballsLeft = 0;
		pendingCannonEmptyWarningTick = -1;
		cannonballUsageSamples.clear();
	}

	private static WorldArea buildCannonWorldArea(WorldPoint worldPoint)
	{
		return new WorldArea(worldPoint.getX(), worldPoint.getY(), 3, 3, worldPoint.getPlane());
	}

	private void suppressCannonPickupWarnings()
	{
		cannonPickupSuppressUntilTick = client.getTickCount() + CANNON_PICKUP_SUPPRESS_TICKS;
		pendingCannonEmptyWarningTick = -1;
		cannonEmptyWarningTriggered = false;
	}

	private boolean isCannonPickupSuppressed()
	{
		return cannonPickupSuppressUntilTick >= client.getTickCount();
	}

	private void handleCannonChatMessage(String message)
	{
		if (message.contains("your cannon is out of ammo"))
		{
			if (cannonPlaced && isCannonCurrentlyPlaced() && !isCannonPickupSuppressed())
			{
				triggerCannonEmptyWarning();
			}
			return;
		}

		if (message.contains("pick") && message.contains("cannon"))
		{
			suppressCannonPickupWarnings();
			resetCannonState();
			return;
		}

		if (cannonPlaced && (message.contains("cannon has broken") || message.contains("cannon has decayed")))
		{
			setCannonNeedsRepair(true);
			return;
		}

		if (message.contains("repair") && message.contains("cannon"))
		{
			setCannonNeedsRepair(false);
		}
	}

	private boolean isOwnBrokenCannon(GameObject gameObject)
	{
		if (gameObject == null
			|| !cannonPlaced
			|| cannonPosition == null
			|| gameObject.getId() != ObjectID.BROKEN_MULTICANNON)
		{
			return false;
		}

		return cannonPosition.contains2D(gameObject.getWorldLocation());
	}

	private void updateTradeButtonClock()
	{
		if (!config.tradeButtonClock())
		{
			restoreChatTabOverride(ComponentID.CHATBOX_TAB_TRADE);
			return;
		}

		String clockText = LocalTime.now().format(TRADE_CLOCK_FORMATTER);
		applyChatTabOverride(ComponentID.CHATBOX_TAB_TRADE, "trade", clockText, DEFAULT_CHAT_TAB_COLOR);
		lastTradeClockText = clockText;
	}

	private void updateChatTabTrackers()
	{
		if (!isToolkitUiReady())
		{
			if (isOtherToolkitUiOwnerActive())
			{
				forgetChatTabOverrideState();
			}
			else
			{
				restoreChatTabTrackerOverrides();
			}
			return;
		}

		updateChatTabTrackerOrder();

		for (int i = 0; i < CHAT_TAB_TRACKER_SLOT_COMPONENTS.length; i++)
		{
			int componentId = CHAT_TAB_TRACKER_SLOT_COMPONENTS[i];
			ChatTabTrackerType trackerType = i < activeChatTabTrackerOrder.size()
				? activeChatTabTrackerOrder.get(i)
				: null;

			if (trackerType == null)
			{
				activeChatTabTrackerSlots.remove(componentId);
				restoreChatTabOverride(componentId);
				continue;
			}

			activeChatTabTrackerSlots.put(componentId, trackerType);
			applyChatTabTrackerOverride(componentId, CHAT_TAB_TRACKER_SLOT_NAMES[i], getChatTabTrackerText(trackerType), getChatTabTrackerLabel(trackerType), getChatTabTrackerColor(trackerType));
		}
	}

	private boolean isCannonCurrentlyPlaced()
	{
		return client.getVarpValue(VarPlayerID.DROPCANNON) == CANNON_STATE_PLACED;
	}

	private void restoreChatTabTrackerOverrides()
	{
		for (int componentId : CHAT_TAB_TRACKER_SLOT_COMPONENTS)
		{
			restoreChatTabOverride(componentId);
		}
	}

	private void updateChatTabTrackerOrder()
	{
		for (ChatTabTrackerType trackerType : ChatTabTrackerType.values())
		{
			boolean active = isChatTabTrackerActive(trackerType);
			if (active && !activeChatTabTrackerOrder.contains(trackerType))
			{
				activeChatTabTrackerOrder.add(trackerType);
			}
			else if (!active)
			{
				activeChatTabTrackerOrder.remove(trackerType);
			}
		}
	}

	private boolean isChatTabTrackerActive(ChatTabTrackerType trackerType)
	{
		switch (trackerType)
		{
			case LOOT:
				return config.clanLootTracker() && npcLootValue > 0;
			case SUPPLY_COST:
				return config.publicSupplyCostTracker() && supplyCostValue > 0;
			case COMBAT_XP:
				return config.channelCombatXpTracker() && getCombatXpGained() > 0;
			case SLAYER_XP:
				return config.privateSlayerXpTracker() && slayerXpGained > 0;
			case TOP_XP_SKILL:
				return config.topXpSkillTracker() && getTopXpSkill() != null;
			default:
				return false;
		}
	}

	private String getChatTabTrackerText(ChatTabTrackerType trackerType)
	{
		switch (trackerType)
		{
			case LOOT:
				return formatShortValue(npcLootValue);
			case SUPPLY_COST:
				return formatShortValue(supplyCostValue);
			case COMBAT_XP:
				return formatShortValue(getCombatXpGained());
			case SLAYER_XP:
				return formatShortValue(slayerXpGained);
			case TOP_XP_SKILL:
				return formatShortValue(getTopXpSkillValue());
			default:
				return "";
		}
	}

	private int getChatTabTrackerColor(ChatTabTrackerType trackerType)
	{
		switch (trackerType)
		{
			case LOOT:
				return LOOT_TRACKER_COLOR;
			case SUPPLY_COST:
				return SUPPLY_TRACKER_COLOR;
			case COMBAT_XP:
				return COMBAT_XP_TRACKER_COLOR;
			case SLAYER_XP:
				return SLAYER_XP_TRACKER_COLOR;
			case TOP_XP_SKILL:
				return COMBAT_XP_TRACKER_COLOR;
			default:
				return DEFAULT_CHAT_TAB_COLOR;
		}
	}

	private String getChatTabTrackerLabel(ChatTabTrackerType trackerType)
	{
		switch (trackerType)
		{
			case LOOT:
				return "Loot";
			case SUPPLY_COST:
				return "Cost";
			case COMBAT_XP:
				return "Combat";
			case SLAYER_XP:
				return "Slayer";
			case TOP_XP_SKILL:
				return getTopXpSkillText();
			default:
				return "";
		}
	}

	private void applyChatTabOverride(int componentId, String tabName, String text, int textColor)
	{
		Widget tab = client.getWidget(componentId);
		if (tab == null)
		{
			return;
		}

		List<Widget> widgets = new ArrayList<>();
		collectChatTabWidgets(tab, widgets, Collections.newSetFromMap(new IdentityHashMap<>()));
		Widget textWidget = findChatTabTextWidget(widgets, tabName);
		boolean appliedToTextWidget = false;

		for (Widget widget : widgets)
		{
			String widgetText = widget.getText();
			if (widgetText == null || widgetText.isEmpty())
			{
				continue;
			}

			rememberChatTabText(widget, widgetText);
			rememberChatTabStyle(widget);
			if (!appliedToTextWidget && widget == textWidget)
			{
				if (!text.equals(widgetText))
				{
					widget.setText(text);
				}

				applyChatTabStyle(widget, textColor);
				appliedToTextWidget = true;
			}
			else
			{
				widget.setText("");
			}
		}

		if (!appliedToTextWidget)
		{
			rememberChatTabText(tab, tab.getText());
			rememberChatTabStyle(tab);
			tab.setText(text);
			applyChatTabStyle(tab, textColor);
		}

		activeChatTabTexts.put(componentId, text);
		activeChatTabOverrides.add(componentId);
	}

	private void applyChatTabTrackerOverride(int componentId, String tabName, String valueText, String labelText, int textColor)
	{
		Widget tab = client.getWidget(componentId);
		if (tab == null)
		{
			return;
		}

		List<Widget> widgets = new ArrayList<>();
		collectChatTabWidgets(tab, widgets, Collections.newSetFromMap(new IdentityHashMap<>()));
		List<Widget> textWidgets = findChatTabTextWidgets(widgets, tabName);
		if (textWidgets.isEmpty())
		{
			rememberChatTabText(tab, tab.getText());
			rememberChatTabStyle(tab);
			tab.setText(valueText);
			applyChatTabValueStyle(tab, textColor);
			activeChatTabTexts.put(componentId, valueText);
			activeChatTabOverrides.add(componentId);
			return;
		}

		Widget valueWidget = textWidgets.get(0);
		Widget labelWidget = textWidgets.size() > 1 ? textWidgets.get(1) : null;
		int textX = valueWidget.getOriginalX();
		int textWidth = valueWidget.getOriginalWidth() > 0 ? valueWidget.getOriginalWidth() : valueWidget.getWidth();

		for (Widget widget : widgets)
		{
			String widgetText = widget.getText();
			if (widgetText == null || widgetText.isEmpty())
			{
				continue;
			}

			rememberChatTabText(widget, widgetText);
			rememberChatTabStyle(widget);
			if (widget == valueWidget)
			{
				if (!valueText.equals(widgetText))
				{
					widget.setText(valueText);
				}

				applyChatTabValueStyle(widget, textColor);
				positionChatTabTrackerText(widget, textX, CHAT_TAB_VALUE_Y_OFFSET, textWidth);
			}
			else if (widget == labelWidget)
			{
				if (!labelText.equals(widgetText))
				{
					widget.setText(labelText);
				}

				applyChatTabLabelStyle(widget);
				positionChatTabTrackerText(widget, textX, CHAT_TAB_LABEL_Y_OFFSET, textWidth);
			}
			else
			{
				widget.setText("");
			}
		}

		if (labelWidget == null)
		{
			valueWidget.setText(valueText + "<br>" + labelText);
			applyChatTabValueStyle(valueWidget, textColor);
		}

		activeChatTabTexts.put(componentId, valueText);
		activeChatTabTexts.put(-componentId, labelText);
		activeChatTabOverrides.add(componentId);
	}

	private void collectChatTabWidgets(Widget widget, List<Widget> widgets, Set<Widget> visitedWidgets)
	{
		if (widget == null || !visitedWidgets.add(widget))
		{
			return;
		}

		widgets.add(widget);
		collectChatTabWidgets(widget.getChildren(), widgets, visitedWidgets);
		collectChatTabWidgets(widget.getDynamicChildren(), widgets, visitedWidgets);
		collectChatTabWidgets(widget.getStaticChildren(), widgets, visitedWidgets);
		collectChatTabWidgets(widget.getNestedChildren(), widgets, visitedWidgets);
	}

	private void collectChatTabWidgets(Widget[] widgets, List<Widget> collectedWidgets, Set<Widget> visitedWidgets)
	{
		if (widgets == null)
		{
			return;
		}

		for (Widget widget : widgets)
		{
			collectChatTabWidgets(widget, collectedWidgets, visitedWidgets);
		}
	}

	private Widget findChatTabTextWidget(List<Widget> widgets, String tabName)
	{
		Widget firstTextWidget = null;
		for (Widget widget : widgets)
		{
			String text = widget.getText();
			if (text == null || text.isBlank())
			{
				continue;
			}

			if (firstTextWidget == null)
			{
				firstTextWidget = widget;
			}

			String cleanText = Text.removeTags(text).toLowerCase(Locale.ENGLISH);
			if (cleanText.contains(tabName))
			{
				return widget;
			}
		}

		return firstTextWidget;
	}

	private List<Widget> findChatTabTextWidgets(List<Widget> widgets, String tabName)
	{
		List<Widget> textWidgets = new ArrayList<>();
		List<Widget> matchingWidgets = new ArrayList<>();
		for (Widget widget : widgets)
		{
			String text = widget.getText();
			if (text == null || text.isBlank())
			{
				continue;
			}

			textWidgets.add(widget);
			String cleanText = Text.removeTags(text).toLowerCase(Locale.ENGLISH);
			if (cleanText.contains(tabName))
			{
				matchingWidgets.add(widget);
			}
		}

		if (matchingWidgets.size() >= 2)
		{
			return matchingWidgets;
		}

		if (matchingWidgets.size() == 1 && textWidgets.size() >= 2)
		{
			int index = textWidgets.indexOf(matchingWidgets.get(0));
			if (index >= 0 && index + 1 < textWidgets.size())
			{
				return textWidgets.subList(index, index + 2);
			}
		}

		return textWidgets.size() > 2 ? textWidgets.subList(0, 2) : textWidgets;
	}

	private void rememberChatTabText(Widget widget, String text)
	{
		if (text == null || text.isEmpty() || isChatTabOverrideText(text))
		{
			return;
		}

		chatTabOriginalTexts.putIfAbsent(widget.getId(), text);
	}

	private void rememberChatTabStyle(Widget widget)
	{
		chatTabOriginalStyles.putIfAbsent(widget.getId(), new ChatTabTextStyle(widget));
	}

	private void applyChatTabStyle(Widget widget, int textColor)
	{
		applyChatTabValueStyle(widget, textColor);
	}

	private void applyChatTabValueStyle(Widget widget, int textColor)
	{
		widget.setFontId(FontID.PLAIN_12);
		widget.setTextColor(textColor);
		widget.setTextShadowed(true);
		widget.setXTextAlignment(1);
		widget.setYTextAlignment(1);
	}

	private void applyChatTabLabelStyle(Widget widget)
	{
		widget.setFontId(FontID.PLAIN_11);
		widget.setTextColor(CHAT_TAB_LABEL_COLOR);
		widget.setTextShadowed(true);
		widget.setXTextAlignment(1);
		widget.setYTextAlignment(1);
	}

	private void positionChatTabTrackerText(Widget widget, int x, int y, int width)
	{
		widget.setOriginalX(x);
		widget.setRelativeX(x);
		widget.setOriginalY(y);
		widget.setRelativeY(y);
		if (width > 0)
		{
			widget.setOriginalWidth(width);
			widget.setWidth(width);
		}

		widget.setOriginalHeight(CHAT_TAB_TRACKER_TEXT_HEIGHT);
		widget.setHeight(CHAT_TAB_TRACKER_TEXT_HEIGHT);
	}

	private boolean isChatTabOverrideText(String text)
	{
		String cleanText = Text.removeTags(text);
		return activeChatTabTexts.containsValue(cleanText) || cleanText.equals(lastTradeClockText);
	}

	private void restoreChatTabOverrideLater(int componentId)
	{
		clientThread.invokeLater(() -> restoreChatTabOverride(componentId));
	}

	private void updateChatTabTrackersLater()
	{
		clientThread.invokeLater(this::updateChatTabTrackers);
	}

	private void restoreChatTabOverridesLater()
	{
		clientThread.invokeLater(this::restoreChatTabOverrides);
	}

	private void restoreChatTabOverride(int componentId)
	{
		if (!activeChatTabOverrides.contains(componentId))
		{
			return;
		}

		Widget tab = client.getWidget(componentId);
		if (tab == null)
		{
			return;
		}

		List<Widget> widgets = new ArrayList<>();
		collectChatTabWidgets(tab, widgets, Collections.newSetFromMap(new IdentityHashMap<>()));
		for (Widget widget : widgets)
		{
			String originalText = chatTabOriginalTexts.remove(widget.getId());
			if (originalText != null)
			{
				widget.setText(originalText);
			}

			ChatTabTextStyle originalStyle = chatTabOriginalStyles.remove(widget.getId());
			if (originalStyle != null)
			{
				originalStyle.restore(widget);
			}
		}

		activeChatTabTexts.remove(componentId);
		activeChatTabTrackerSlots.remove(componentId);
		activeChatTabOverrides.remove(componentId);
		if (componentId == ComponentID.CHATBOX_TAB_TRADE)
		{
			lastTradeClockText = "";
		}
	}

	private void restoreChatTabOverrides()
	{
		for (Map.Entry<Integer, String> entry : chatTabOriginalTexts.entrySet())
		{
			Widget widget = client.getWidget(entry.getKey());
			if (widget != null)
			{
				widget.setText(entry.getValue());
			}
		}

		for (Map.Entry<Integer, ChatTabTextStyle> entry : chatTabOriginalStyles.entrySet())
		{
			Widget widget = client.getWidget(entry.getKey());
			if (widget != null)
			{
				entry.getValue().restore(widget);
			}
		}

		chatTabOriginalTexts.clear();
		chatTabOriginalStyles.clear();
		activeChatTabTexts.clear();
		activeChatTabTrackerSlots.clear();
		activeChatTabOverrides.clear();
		lastTradeClockText = "";
	}

	private void forgetChatTabOverrideState()
	{
		chatTabOriginalTexts.clear();
		chatTabOriginalStyles.clear();
		activeChatTabTexts.clear();
		activeChatTabTrackerSlots.clear();
		activeChatTabOverrides.clear();
		lastTradeClockText = "";
	}

	private void loadTrackerValues()
	{
		loadLootTrackerValue();
		loadSupplyCostTrackerValue();
		loadCombatXpTrackerValues();
		loadSlayerXpTrackerValue();
		syncTrackerSkillBaselines();
		trackerValuesLoaded = true;
	}

	private void loadStatsValues()
	{
		for (PvmToolsStatsPeriod period : PvmToolsStatsPeriod.values())
		{
			statsByPeriod.put(period, PvmToolsStats.deserialize(getSavedStatsValue(period), getCurrentStatsPeriodId(period)));
		}
		refreshStatsPanel();
	}

	PvmToolsStats getStatsSnapshot(PvmToolsStatsPeriod period)
	{
		return getStats(period).copy();
	}

	PvmTaskSnapshot getCurrentSlayerTaskSnapshot()
	{
		return buildCurrentSlayerTaskSnapshot();
	}

	List<PvmTaskHistoryEntry> getTaskHistorySnapshot()
	{
		return new ArrayList<>(taskHistory);
	}

	void clearTaskHistory()
	{
		taskHistory.clear();
		persistSlayerTaskHistory();
		refreshStatsPanel();
	}

	boolean isAdvancedPanelMode()
	{
		return config.panelMode() == ToolkitPanelMode.ADVANCED;
	}

	String getCannonEstimateText()
	{
		if (!cannonPlaced)
		{
			return "No cannon";
		}

		if (cannonballsLeft <= 0)
		{
			return "Empty";
		}

		double ballsPerSecond = getCannonballsPerSecond(System.currentTimeMillis());
		if (ballsPerSecond <= 0d)
		{
			return "Learning";
		}

		return formatDurationShort(Math.round(cannonballsLeft / ballsPerSecond));
	}

	boolean isPotionTimersQuickEnabled()
	{
		return config.showPotionTimers();
	}

	void setPotionTimersQuickEnabled(boolean enabled)
	{
		setBooleanConfig("showPotionTimers", enabled);
		if (!enabled)
		{
			removePotionTimers();
			removePrayerTimer();
			resetPrayerCountdown();
		}
	}

	boolean isScreenWarningsQuickEnabled()
	{
		return config.flashScreenWarning();
	}

	void setScreenWarningsQuickEnabled(boolean enabled)
	{
		setBooleanConfig("flashScreenWarning", enabled);
	}

	boolean isDropAlertQuickEnabled()
	{
		return config.valuableDropFlash() || config.valuableDropSound();
	}

	void setDropAlertQuickEnabled(boolean enabled)
	{
		setBooleanConfig("valuableDropFlash", enabled);
		setBooleanConfig("valuableDropSound", enabled);
	}

	boolean isCannonWarningsQuickEnabled()
	{
		return config.warnCannonEmpty() || config.warnCannonRepair();
	}

	void setCannonWarningsQuickEnabled(boolean enabled)
	{
		setBooleanConfig("warnCannonEmpty", enabled);
		setBooleanConfig("warnCannonRepair", enabled);
	}

	boolean isSuperiorAlertsQuickEnabled()
	{
		return config.flashSuperiorSpawns() || config.superiorExamineHints();
	}

	void setSuperiorAlertsQuickEnabled(boolean enabled)
	{
		setBooleanConfig("flashSuperiorSpawns", enabled);
		setBooleanConfig("superiorExamineHints", enabled);
	}

	boolean isInventorySpacesQuickEnabled()
	{
		return config.showInventorySpaces();
	}

	void setInventorySpacesQuickEnabled(boolean enabled)
	{
		setBooleanConfig("showInventorySpaces", enabled);
		clientThread.invokeLater(() ->
		{
			if (enabled)
			{
				syncInventoryInfoBox();
			}
			else
			{
				removeInventoryInfoBox();
			}
		});
	}

	boolean isChatTrackersQuickEnabled()
	{
		return config.clanLootTracker()
			|| config.publicSupplyCostTracker()
			|| config.channelCombatXpTracker()
			|| config.privateSlayerXpTracker()
			|| config.topXpSkillTracker();
	}

	void setChatTrackersQuickEnabled(boolean enabled)
	{
		setBooleanConfig("clanLootTracker", enabled);
		setBooleanConfig("publicSupplyCostTracker", enabled);
		setBooleanConfig("channelCombatXpTracker", enabled);
		setBooleanConfig("privateSlayerXpTracker", enabled);
		setBooleanConfig("topXpSkillTracker", enabled);
	}

	String getDropAlertThresholdText()
	{
		return QuantityFormatter.quantityToStackSize(Math.max(0, config.valuableDropThreshold())).toLowerCase(Locale.ENGLISH) + " gp";
	}

	private void setBooleanConfig(String key, boolean enabled)
	{
		configManager.setConfiguration(PvmToolsConfig.GROUP, key, enabled);
		refreshStatsPanel();
		syncAllTimersLater();
	}

	private PvmToolsStats getStats(PvmToolsStatsPeriod period)
	{
		String currentPeriodId = getCurrentStatsPeriodId(period);
		PvmToolsStats stats = statsByPeriod.get(period);
		if (stats == null || !currentPeriodId.equals(stats.getPeriodId()))
		{
			stats = PvmToolsStats.deserialize(getSavedStatsValue(period), currentPeriodId);
			statsByPeriod.put(period, stats);
			persistStatsValue(period);
		}

		return stats;
	}

	private String getCurrentStatsPeriodId(PvmToolsStatsPeriod period)
	{
		return period.getCurrentPeriodId(LocalDate.now());
	}

	private String getSavedStatsValue(PvmToolsStatsPeriod period)
	{
		switch (period)
		{
			case DAY:
				return config.savedStatsDay();
			case WEEK:
				return config.savedStatsWeek();
			case MONTH:
				return config.savedStatsMonth();
			case YEAR:
				return config.savedStatsYear();
			case ALL_TIME:
			default:
				return config.savedStatsAllTime();
		}
	}

	private void addLootStats(int itemId, long quantity, long value)
	{
		resumeCurrentSlayerTaskTimer();
		currentSlayerTaskLootValue += Math.max(0L, value);
		for (PvmToolsStatsPeriod period : PvmToolsStatsPeriod.values())
		{
			getStats(period).addLoot(itemId, quantity, value);
			persistStatsValue(period);
		}
		persistCurrentSlayerTaskState();
	}

	private void addSupplyCostStats(long value, long count, SupplyCostType type)
	{
		resumeCurrentSlayerTaskTimer();
		addCurrentTaskSupplyCost(value, count, type);
		for (PvmToolsStatsPeriod period : PvmToolsStatsPeriod.values())
		{
			getStats(period).addSupplyCost(value, count, type);
			persistStatsValue(period);
		}
		persistCurrentSlayerTaskState();
	}

	private void addCombatXpStats(Skill skill, long xp)
	{
		resumeCurrentSlayerTaskTimer();
		currentSlayerTaskCombatXp += Math.max(0L, xp);
		for (PvmToolsStatsPeriod period : PvmToolsStatsPeriod.values())
		{
			getStats(period).addCombatXp(skill, xp);
			persistStatsValue(period);
		}
		persistCurrentSlayerTaskState();
		refreshStatsPanel();
	}

	private void addSlayerXpStats(long xp)
	{
		resumeCurrentSlayerTaskTimer();
		currentSlayerTaskSlayerXp += Math.max(0L, xp);
		for (PvmToolsStatsPeriod period : PvmToolsStatsPeriod.values())
		{
			getStats(period).addSlayerXp(xp);
			persistStatsValue(period);
		}
		persistCurrentSlayerTaskState();
		refreshStatsPanel();
	}

	private void addCurrentTaskSupplyCost(long value, long count, SupplyCostType type)
	{
		currentSlayerTaskSupplyCostValue += Math.max(0L, value);
		long safeCount = Math.max(0L, count);
		switch (type)
		{
			case POTION:
				currentSlayerTaskPotionDoseCount += safeCount;
				break;
			case FOOD:
				currentSlayerTaskFoodCount += safeCount;
				break;
			case CANNONBALL:
				currentSlayerTaskCannonballCount += safeCount;
				break;
		}
	}

	private void syncSlayerTaskFromRuneLite()
	{
		if (!isLoggedIn() || isClickToPlayScreenVisible())
		{
			return;
		}

		String taskName = normalizeTaskText(configManager.getRSProfileConfiguration("slayer", "taskName"));
		String taskLocation = normalizeTaskText(configManager.getRSProfileConfiguration("slayer", "taskLocation"));
		int amount = Math.max(0, parseIntConfig(configManager.getRSProfileConfiguration("slayer", "amount")));
		int sourceInitialAmount = Math.max(0, parseIntConfig(configManager.getRSProfileConfiguration("slayer", "initialAmount")));
		int initialAmount = Math.max(amount, sourceInitialAmount);

		if (taskName.isBlank() || amount <= 0)
		{
			confirmSlayerTaskCompletion();
			return;
		}

		pendingSlayerTaskCompletionTicks = 0;
		if (currentSlayerTaskName.isBlank())
		{
			startSlayerTask(taskName, taskLocation, amount, initialAmount);
			return;
		}

		boolean taskNameChanged = !taskName.equalsIgnoreCase(currentSlayerTaskName);
		boolean knownLocationChanged = !taskLocation.isBlank()
			&& !currentSlayerTaskLocation.isBlank()
			&& !taskLocation.equalsIgnoreCase(currentSlayerTaskLocation);
		boolean sameTaskCounterReset = !taskNameChanged
			&& !knownLocationChanged
			&& currentSlayerTaskAmount > 0
			&& amount > currentSlayerTaskAmount
			&& sourceInitialAmount > 0
			&& sourceInitialAmount != currentSlayerTaskInitialAmount;
		boolean taskChanged = taskNameChanged || knownLocationChanged || sameTaskCounterReset;

		if (taskChanged)
		{
			finishCurrentSlayerTask();
			startSlayerTask(taskName, taskLocation, amount, initialAmount);
			return;
		}

		slayerTaskObservedActive = true;
		currentSlayerTaskAmount = amount;
		if (sourceInitialAmount > 0)
		{
			currentSlayerTaskInitialAmount = Math.max(currentSlayerTaskInitialAmount, sourceInitialAmount);
		}
		if (currentSlayerTaskLocation.isBlank() && !taskLocation.isBlank())
		{
			currentSlayerTaskLocation = taskLocation;
		}
		persistCurrentSlayerTaskState();
		refreshStatsPanel();
	}

	private void confirmSlayerTaskCompletion()
	{
		if (currentSlayerTaskName.isBlank() || !slayerTaskObservedActive)
		{
			return;
		}

		pendingSlayerTaskCompletionTicks++;
		if (pendingSlayerTaskCompletionTicks < SLAYER_TASK_COMPLETION_CONFIRM_TICKS)
		{
			return;
		}

		currentSlayerTaskAmount = 0;
		finishCurrentSlayerTask();
	}

	private void startSlayerTask(String taskName, String taskLocation, int amount, int initialAmount)
	{
		currentSlayerTaskName = taskName;
		currentSlayerTaskLocation = taskLocation;
		currentSlayerTaskAmount = Math.max(0, amount);
		currentSlayerTaskInitialAmount = Math.max(currentSlayerTaskAmount, initialAmount);
		currentSlayerTaskStartMillis = System.currentTimeMillis();
		currentSlayerTaskElapsedMillis = 0L;
		currentSlayerTaskActiveSinceMillis = 0L;
		currentSlayerTaskLootValue = 0L;
		currentSlayerTaskSupplyCostValue = 0L;
		currentSlayerTaskCombatXp = 0L;
		currentSlayerTaskSlayerXp = 0L;
		currentSlayerTaskPotionDoseCount = 0L;
		currentSlayerTaskFoodCount = 0L;
		currentSlayerTaskCannonballCount = 0L;
		slayerTaskObservedActive = true;
		pendingSlayerTaskCompletionTicks = 0;
		persistCurrentSlayerTaskState();
		refreshStatsPanel();
	}

	private void finishCurrentSlayerTask()
	{
		pauseCurrentSlayerTaskTimer();
		PvmTaskSnapshot snapshot = buildCurrentSlayerTaskSnapshot();
		if (snapshot.isActive() && (snapshot.getKilled() > 0 || snapshot.getLootValue() > 0 || snapshot.getSupplyCostValue() > 0 || snapshot.getCombatXp() > 0 || snapshot.getSlayerXp() > 0))
		{
			addTaskHistoryEntry(snapshot, System.currentTimeMillis());
			persistSlayerTaskHistory();
		}

		currentSlayerTaskName = "";
		currentSlayerTaskLocation = "";
		currentSlayerTaskAmount = 0;
		currentSlayerTaskInitialAmount = 0;
		currentSlayerTaskStartMillis = 0L;
		currentSlayerTaskElapsedMillis = 0L;
		currentSlayerTaskActiveSinceMillis = 0L;
		currentSlayerTaskLootValue = 0L;
		currentSlayerTaskSupplyCostValue = 0L;
		currentSlayerTaskCombatXp = 0L;
		currentSlayerTaskSlayerXp = 0L;
		currentSlayerTaskPotionDoseCount = 0L;
		currentSlayerTaskFoodCount = 0L;
		currentSlayerTaskCannonballCount = 0L;
		slayerTaskObservedActive = false;
		pendingSlayerTaskCompletionTicks = 0;
		persistCurrentSlayerTaskState();
		refreshStatsPanel();
	}

	private PvmTaskSnapshot buildCurrentSlayerTaskSnapshot()
	{
		if (currentSlayerTaskName.isBlank())
		{
			return PvmTaskSnapshot.EMPTY;
		}

		return new PvmTaskSnapshot(
			currentSlayerTaskName,
			currentSlayerTaskLocation,
			currentSlayerTaskAmount,
			currentSlayerTaskInitialAmount,
			currentSlayerTaskStartMillis,
			getCurrentSlayerTaskElapsedMillis(),
			currentSlayerTaskLootValue,
			currentSlayerTaskSupplyCostValue,
			currentSlayerTaskCombatXp,
			currentSlayerTaskSlayerXp,
			currentSlayerTaskPotionDoseCount,
			currentSlayerTaskFoodCount,
			currentSlayerTaskCannonballCount);
	}

	private void resumeCurrentSlayerTaskTimer()
	{
		if (currentSlayerTaskName.isBlank() || currentSlayerTaskActiveSinceMillis > 0L)
		{
			return;
		}

		currentSlayerTaskActiveSinceMillis = System.currentTimeMillis();
	}

	private void pauseCurrentSlayerTaskTimer()
	{
		if (currentSlayerTaskActiveSinceMillis <= 0L)
		{
			return;
		}

		currentSlayerTaskElapsedMillis += Math.max(0L, System.currentTimeMillis() - currentSlayerTaskActiveSinceMillis);
		currentSlayerTaskActiveSinceMillis = 0L;
	}

	private long getCurrentSlayerTaskElapsedMillis()
	{
		long elapsedMillis = currentSlayerTaskElapsedMillis;
		if (currentSlayerTaskActiveSinceMillis > 0L)
		{
			elapsedMillis += Math.max(0L, System.currentTimeMillis() - currentSlayerTaskActiveSinceMillis);
		}
		return elapsedMillis;
	}

	private void loadCurrentSlayerTaskState()
	{
		String savedState = config.savedCurrentSlayerTask();
		if (savedState == null || savedState.isBlank())
		{
			return;
		}

		String[] parts = savedState.split(TASK_STATE_SEPARATOR, -1);
		if (parts.length < 13)
		{
			return;
		}

		currentSlayerTaskName = normalizeTaskText(parts[0]);
		currentSlayerTaskLocation = normalizeTaskText(parts[1]);
		currentSlayerTaskAmount = Math.max(0, parseIntConfig(parts[2]));
		currentSlayerTaskInitialAmount = Math.max(currentSlayerTaskAmount, parseIntConfig(parts[3]));
		currentSlayerTaskStartMillis = parseLongConfig(parts[4]);
		currentSlayerTaskElapsedMillis = Math.max(0L, parseLongConfig(parts[5]));
		currentSlayerTaskActiveSinceMillis = 0L;
		currentSlayerTaskLootValue = Math.max(0L, parseLongConfig(parts[6]));
		currentSlayerTaskSupplyCostValue = Math.max(0L, parseLongConfig(parts[7]));
		currentSlayerTaskCombatXp = Math.max(0L, parseLongConfig(parts[8]));
		currentSlayerTaskSlayerXp = Math.max(0L, parseLongConfig(parts[9]));
		currentSlayerTaskPotionDoseCount = Math.max(0L, parseLongConfig(parts[10]));
		currentSlayerTaskFoodCount = Math.max(0L, parseLongConfig(parts[11]));
		currentSlayerTaskCannonballCount = Math.max(0L, parseLongConfig(parts[12]));
	}

	private void persistCurrentSlayerTaskState()
	{
		if (currentSlayerTaskName.isBlank())
		{
			configManager.setConfiguration(PvmToolsConfig.GROUP, SAVED_CURRENT_SLAYER_TASK_KEY, "");
			return;
		}

		configManager.setConfiguration(PvmToolsConfig.GROUP, SAVED_CURRENT_SLAYER_TASK_KEY,
			sanitizeTaskStateText(currentSlayerTaskName)
				+ TASK_STATE_SEPARATOR + sanitizeTaskStateText(currentSlayerTaskLocation)
				+ TASK_STATE_SEPARATOR + currentSlayerTaskAmount
				+ TASK_STATE_SEPARATOR + currentSlayerTaskInitialAmount
				+ TASK_STATE_SEPARATOR + currentSlayerTaskStartMillis
				+ TASK_STATE_SEPARATOR + getCurrentSlayerTaskElapsedMillis()
				+ TASK_STATE_SEPARATOR + currentSlayerTaskLootValue
				+ TASK_STATE_SEPARATOR + currentSlayerTaskSupplyCostValue
				+ TASK_STATE_SEPARATOR + currentSlayerTaskCombatXp
				+ TASK_STATE_SEPARATOR + currentSlayerTaskSlayerXp
				+ TASK_STATE_SEPARATOR + currentSlayerTaskPotionDoseCount
				+ TASK_STATE_SEPARATOR + currentSlayerTaskFoodCount
				+ TASK_STATE_SEPARATOR + currentSlayerTaskCannonballCount);
	}

	private void loadSlayerTaskHistory()
	{
		taskHistory.clear();
		String savedHistory = config.savedSlayerTaskHistory();
		if (savedHistory == null || savedHistory.isBlank())
		{
			return;
		}

		boolean mergedInterruptedTasks = false;
		for (String serializedEntry : savedHistory.split(TASK_HISTORY_ENTRY_SEPARATOR_PATTERN, -1))
		{
			PvmTaskHistoryEntry entry = PvmTaskHistoryEntry.deserialize(serializedEntry);
			if (entry != null)
			{
				PvmTaskHistoryEntry previous = taskHistory.peekLast();
				if (previous != null && previous.canMergeInterruptedTask(entry))
				{
					taskHistory.removeLast();
					taskHistory.addLast(previous.merge(entry));
					mergedInterruptedTasks = true;
				}
				else
				{
					taskHistory.addLast(entry);
				}
			}
		}

		if (mergedInterruptedTasks)
		{
			persistSlayerTaskHistory();
		}
	}

	private void addTaskHistoryEntry(PvmTaskSnapshot snapshot, long finishedMillis)
	{
		PvmTaskHistoryEntry entry = new PvmTaskHistoryEntry(snapshot, finishedMillis);
		PvmTaskHistoryEntry previous = taskHistory.peekFirst();
		if (previous != null && previous.canMergeInterruptedTask(entry))
		{
			taskHistory.removeFirst();
			taskHistory.addFirst(previous.merge(entry));
			return;
		}

		taskHistory.addFirst(entry);
	}

	private void persistSlayerTaskHistory()
	{
		StringBuilder serializedHistory = new StringBuilder();
		for (PvmTaskHistoryEntry entry : taskHistory)
		{
			if (serializedHistory.length() > 0)
			{
				serializedHistory.append(TASK_HISTORY_ENTRY_SEPARATOR);
			}
			serializedHistory.append(entry.serialize());
		}

		configManager.setConfiguration(
			PvmToolsConfig.GROUP,
			SAVED_SLAYER_TASK_HISTORY_KEY,
			serializedHistory.toString());
	}

	private String sanitizeTaskStateText(String value)
	{
		return value == null ? "" : value.replace(TASK_STATE_SEPARATOR, " ").trim();
	}

	private String normalizeTaskText(String value)
	{
		return value == null ? "" : value.trim();
	}

	private void persistStatsValue(PvmToolsStatsPeriod period)
	{
		String key;
		switch (period)
		{
			case DAY:
				key = SAVED_STATS_DAY_KEY;
				break;
			case WEEK:
				key = SAVED_STATS_WEEK_KEY;
				break;
			case MONTH:
				key = SAVED_STATS_MONTH_KEY;
				break;
			case YEAR:
				key = SAVED_STATS_YEAR_KEY;
				break;
			case ALL_TIME:
			default:
				key = SAVED_STATS_ALL_TIME_KEY;
				break;
		}

		configManager.setConfiguration(PvmToolsConfig.GROUP, key, getStats(period).serialize());
	}

	private void refreshStatsPanel()
	{
		if (statsPanel != null)
		{
			statsPanel.refresh();
		}
	}

	private void loadLootTrackerValue()
	{
		npcLootValue = isForeverTrackerMode()
			? parseLongConfig(config.savedLootTrackerValue())
			: 0L;
	}

	private void loadSupplyCostTrackerValue()
	{
		supplyCostValue = isForeverTrackerMode()
			? parseLongConfig(config.savedSupplyCostTrackerValue())
			: 0L;
		potionSupplyCostValue = 0L;
		foodSupplyCostValue = 0L;
		cannonballSupplyCostValue = 0L;
		potionSupplyDoseCount = 0L;
		foodSupplyCount = 0L;
		cannonballSupplyCount = 0L;
		if (!isForeverTrackerMode())
		{
			return;
		}

		String savedBreakdown = config.savedSupplyCostTrackerBreakdown();
		if (savedBreakdown == null || savedBreakdown.isBlank())
		{
			return;
		}

		for (String savedValue : savedBreakdown.split(";"))
		{
			String[] parts = savedValue.split("=", 2);
			if (parts.length != 2)
			{
				continue;
			}

			long value = Math.max(0L, parseLongConfig(parts[1]));
			switch (parts[0])
			{
				case "potionValue":
					potionSupplyCostValue = value;
					break;
				case "foodValue":
					foodSupplyCostValue = value;
					break;
				case "cannonballValue":
					cannonballSupplyCostValue = value;
					break;
				case "potionDoses":
					potionSupplyDoseCount = value;
					break;
				case "foodCount":
					foodSupplyCount = value;
					break;
				case "cannonballCount":
					cannonballSupplyCount = value;
					break;
			}
		}
	}

	private void loadCombatXpTrackerValues()
	{
		combatXpGainedBySkill.clear();
		trackerBaseExperience.clear();
		for (Skill skill : COMBAT_TRACKER_SKILLS)
		{
			combatXpGainedBySkill.put(skill, 0L);
		}

		if (!isForeverTrackerMode())
		{
			return;
		}

		String savedValues = config.savedCombatXpTrackerValues();
		if (savedValues == null || savedValues.isBlank())
		{
			return;
		}

		for (String savedValue : savedValues.split(";"))
		{
			String[] parts = savedValue.split("=", 2);
			if (parts.length != 2)
			{
				continue;
			}

			try
			{
				Skill skill = Skill.valueOf(parts[0]);
				if (isCombatTrackerSkill(skill))
				{
					combatXpGainedBySkill.put(skill, Math.max(0L, parseLongConfig(parts[1])));
				}
			}
			catch (IllegalArgumentException ignored)
			{
				// Ignore stale config values from older versions or manual edits.
			}
		}
	}

	private void loadSlayerXpTrackerValue()
	{
		trackerBaseExperience.remove(Skill.SLAYER);
		slayerXpGained = isForeverTrackerMode()
			? parseLongConfig(config.savedSlayerXpTrackerValue())
			: 0L;
	}

	private boolean isForeverTrackerMode()
	{
		return config.trackerMode() == TrackerPersistenceMode.FOREVER;
	}

	private boolean isChatTabTrackerConfigKey(String key)
	{
		return "clanLootTracker".equals(key)
			|| "publicSupplyCostTracker".equals(key)
			|| "channelCombatXpTracker".equals(key)
			|| "privateSlayerXpTracker".equals(key)
			|| "topXpSkillTracker".equals(key);
	}

	String getItemDisplayName(int itemId)
	{
		String cachedName = itemDisplayNames.get(itemId);
		if (cachedName != null)
		{
			return cachedName;
		}

		if (pendingItemDisplayNames.add(itemId))
		{
			clientThread.invokeLater(() ->
			{
				String name = itemManager.getItemComposition(itemId).getName();
				itemDisplayNames.put(itemId, name == null || name.isBlank() ? "Unknown item" : name);
				pendingItemDisplayNames.remove(itemId);
				refreshStatsPanel();
			});
		}

		return "Item " + itemId;
	}

	private void syncTrackerSkillBaselines()
	{
		if (!isLoggedIn())
		{
			return;
		}

		for (Skill skill : COMBAT_TRACKER_SKILLS)
		{
			syncTrackerSkillBaseline(skill);
		}

		syncTrackerSkillBaseline(Skill.SLAYER);
	}

	private void syncTrackerSkillBaseline(Skill skill)
	{
		if (trackerBaseExperience.containsKey(skill) && lastSkillExperience.containsKey(skill))
		{
			return;
		}

		int xp = client.getSkillExperience(skill);
		if (xp <= 0)
		{
			return;
		}

		lastSkillExperience.put(skill, xp);
		trackerBaseExperience.put(skill, xp);
	}

	private void updateTrackedSkillXp(Skill skill, int currentXp)
	{
		if (!trackerValuesLoaded)
		{
			loadTrackerValues();
		}

		if (!isCombatTrackerSkill(skill) && skill != Skill.SLAYER)
		{
			return;
		}

		Integer previousXp = lastSkillExperience.get(skill);
		if (!trackerBaseExperience.containsKey(skill) || previousXp == null)
		{
			trackerBaseExperience.put(skill, currentXp);
			lastSkillExperience.put(skill, currentXp);
			return;
		}

		lastSkillExperience.put(skill, currentXp);
		int gainedXp = currentXp - previousXp;
		if (gainedXp <= 0)
		{
			if (currentXp < trackerBaseExperience.getOrDefault(skill, currentXp))
			{
				trackerBaseExperience.put(skill, currentXp);
			}
			return;
		}

		markCombatActivity();
		if (isCombatTrackerSkill(skill))
		{
			addCombatXpStats(skill, gainedXp);
			long gainedSinceBase = Math.max(0L, (long) currentXp - trackerBaseExperience.getOrDefault(skill, currentXp));
			long savedOffset = getCombatXpGainedOffset(skill);
			combatXpGainedBySkill.put(skill, savedOffset + gainedSinceBase);
			if (isForeverTrackerMode())
			{
				persistCombatXpTrackerValues();
			}
		}

		if (skill == Skill.SLAYER)
		{
			addSlayerXpStats(gainedXp);
			long gainedSinceBase = Math.max(0L, (long) currentXp - trackerBaseExperience.getOrDefault(Skill.SLAYER, currentXp));
			slayerXpGained = getSlayerXpGainedOffset() + gainedSinceBase;
			if (isForeverTrackerMode())
			{
				persistSlayerXpTrackerValue();
			}
		}
	}

	private boolean isCombatTrackerSkill(Skill skill)
	{
		for (Skill combatSkill : COMBAT_TRACKER_SKILLS)
		{
			if (combatSkill == skill)
			{
				return true;
			}
		}

		return false;
	}

	private long getCombatXpGainedOffset(Skill skill)
	{
		long currentGained = combatXpGainedBySkill.getOrDefault(skill, 0L);
		if (!isLoggedIn() || !trackerBaseExperience.containsKey(skill))
		{
			return currentGained;
		}

		int currentXp = client.getSkillExperience(skill);
		int baseXp = trackerBaseExperience.get(skill);
		return Math.max(0L, currentGained - Math.max(0, currentXp - baseXp));
	}

	private long getSlayerXpGainedOffset()
	{
		if (!isLoggedIn() || !trackerBaseExperience.containsKey(Skill.SLAYER))
		{
			return slayerXpGained;
		}

		int currentXp = client.getSkillExperience(Skill.SLAYER);
		int baseXp = trackerBaseExperience.get(Skill.SLAYER);
		return Math.max(0L, slayerXpGained - Math.max(0, currentXp - baseXp));
	}

	private long getCombatXpGained()
	{
		long total = 0L;
		for (long skillXp : combatXpGainedBySkill.values())
		{
			total += skillXp;
		}
		return total;
	}

	private Skill getTopXpSkill()
	{
		Skill topSkill = null;
		long topXp = 0L;
		for (Skill skill : COMBAT_TRACKER_SKILLS)
		{
			long skillXp = combatXpGainedBySkill.getOrDefault(skill, 0L);
			if (skillXp > topXp)
			{
				topXp = skillXp;
				topSkill = skill;
			}
		}

		if (slayerXpGained > topXp)
		{
			topSkill = Skill.SLAYER;
		}

		return topSkill;
	}

	private long getTopXpSkillValue()
	{
		Skill topSkill = getTopXpSkill();
		if (topSkill == null)
		{
			return 0L;
		}

		return topSkill == Skill.SLAYER ? slayerXpGained : combatXpGainedBySkill.getOrDefault(topSkill, 0L);
	}

	private String getTopXpSkillText()
	{
		Skill topSkill = getTopXpSkill();
		if (topSkill == null)
		{
			return "";
		}

		switch (topSkill)
		{
			case ATTACK:
				return "Atk";
			case STRENGTH:
				return "Str";
			case DEFENCE:
				return "Def";
			case RANGED:
				return "Range";
			case MAGIC:
				return "Mage";
			case HITPOINTS:
				return "HP";
			case SLAYER:
				return "Slayer";
			default:
				String name = topSkill.getName();
				return name == null || name.isBlank() ? topSkill.name() : name;
		}
	}

	private void trackGroundItemTake(MenuOptionClicked event)
	{
		if (!isGroundItemTakeAction(event)
			|| event.getMenuOption() == null
			|| !"take".equalsIgnoreCase(Text.removeTags(event.getMenuOption())))
		{
			return;
		}

		int itemId = event.getItemId() > 0 ? event.getItemId() : event.getId();
		if (itemId <= 0)
		{
			return;
		}

		WorldPoint worldPoint = WorldPoint.fromScene(client, event.getParam0(), event.getParam1(), client.getPlane());
		GroundItemKey key = new GroundItemKey(itemId, worldPoint);
		if (npcDropQuantities.getOrDefault(key, 0) > 0)
		{
			pendingGroundItemPickups.add(new PendingGroundItemPickup(key, client.getTickCount()));
		}
	}

	private void trackSupplyConsumption(MenuOptionClicked event)
	{
		if (event.getItemId() <= 0)
		{
			return;
		}

		String option = event.getMenuOption();
		if (option == null)
		{
			return;
		}

		String cleanOption = Text.removeTags(option).toLowerCase(Locale.ENGLISH);
		int itemId = event.getItemId();
		if ("drink".equals(cleanOption))
		{
			addSupplyCost(getPotionDoseValue(itemId), SupplyCostType.POTION, 1);
			return;
		}

		if ("eat".equals(cleanOption))
		{
			addSupplyCost(getItemValue(itemId, 1), SupplyCostType.FOOD, 1);
		}
	}

	private void trackCannonballSupplyCost(int oldCannonballsLeft, int newCannonballsLeft)
	{
		if (!cannonPlaced
			|| oldCannonballsLeft <= 0
			|| newCannonballsLeft < 0
			|| newCannonballsLeft >= oldCannonballsLeft)
		{
			return;
		}

		int spentCannonballs = oldCannonballsLeft - newCannonballsLeft;
		recordCannonballUsage(spentCannonballs);
		addSupplyCost(getItemValue(ItemID.STEEL_CANNONBALL, spentCannonballs), SupplyCostType.CANNONBALL, spentCannonballs);
	}

	private void recordCannonballUsage(int spentCannonballs)
	{
		if (spentCannonballs <= 0)
		{
			return;
		}

		long now = System.currentTimeMillis();
		cannonballUsageSamples.addLast(new CannonballUsageSample(now, spentCannonballs));
		cleanupCannonballUsageSamples(now);
		refreshStatsPanel();
	}

	private void cleanupCannonballUsageSamples(long now)
	{
		while (!cannonballUsageSamples.isEmpty() && now - cannonballUsageSamples.peekFirst().getTimeMillis() > CANNON_ESTIMATE_WINDOW_MILLIS)
		{
			cannonballUsageSamples.removeFirst();
		}
	}

	private double getCannonballsPerSecond(long now)
	{
		cleanupCannonballUsageSamples(now);
		if (cannonballUsageSamples.size() < 2)
		{
			return 0d;
		}

		long first = cannonballUsageSamples.peekFirst().getTimeMillis();
		long elapsedMillis = Math.max(0L, now - first);
		if (elapsedMillis < MIN_CANNON_ESTIMATE_WINDOW_MILLIS)
		{
			return 0d;
		}

		long spent = 0L;
		for (CannonballUsageSample sample : cannonballUsageSamples)
		{
			spent += sample.getCount();
		}

		return spent <= 0 ? 0d : spent / (elapsedMillis / 1000d);
	}

	private void addSupplyCost(long value, SupplyCostType type, long count)
	{
		if (value <= 0 && count <= 0)
		{
			return;
		}

		markPvmActivity();
		supplyCostValue += Math.max(0L, value);
		addSupplyCostStats(value, count, type);
		switch (type)
		{
			case POTION:
				potionSupplyCostValue += Math.max(0L, value);
				potionSupplyDoseCount += Math.max(0L, count);
				break;
			case FOOD:
				foodSupplyCostValue += Math.max(0L, value);
				foodSupplyCount += Math.max(0L, count);
				break;
			case CANNONBALL:
				cannonballSupplyCostValue += Math.max(0L, value);
				cannonballSupplyCount += Math.max(0L, count);
				break;
		}

		if (isForeverTrackerMode())
		{
			persistSupplyCostTrackerValue();
		}

		refreshStatsPanel();
	}

	private boolean isGroundItemTakeAction(MenuOptionClicked event)
	{
		switch (event.getMenuAction())
		{
			case GROUND_ITEM_FIRST_OPTION:
			case GROUND_ITEM_SECOND_OPTION:
			case GROUND_ITEM_THIRD_OPTION:
			case GROUND_ITEM_FOURTH_OPTION:
			case GROUND_ITEM_FIFTH_OPTION:
				return true;
			default:
				return false;
		}
	}

	private void addNpcDropQuantity(Tile tile, int itemId, int quantity)
	{
		if (quantity <= 0)
		{
			return;
		}

		markPvmActivity();
		cacheItemDisplayName(itemId);
		GroundItemKey key = new GroundItemKey(itemId, tile.getWorldLocation());
		npcDropQuantities.merge(key, quantity, Integer::sum);
		triggerValuableDropAlert(key, itemId, quantity, getItemValue(itemId, quantity));
	}

	private void countPickedUpNpcDrop(Tile tile, int itemId, int removedQuantity)
	{
		if (removedQuantity <= 0)
		{
			return;
		}

		GroundItemKey key = new GroundItemKey(itemId, tile.getWorldLocation());
		Integer trackedQuantity = npcDropQuantities.get(key);
		if (trackedQuantity == null || trackedQuantity <= 0)
		{
			return;
		}

		int countedQuantity = Math.min(removedQuantity, trackedQuantity);
		if (!removePendingGroundItemPickup(key) || !isLocalPlayerNear(tile.getWorldLocation()))
		{
			decrementNpcDropQuantity(key, countedQuantity);
			if (trackedQuantity <= countedQuantity)
			{
				clearValuableDropAlert(key);
			}
			return;
		}

		long value = getItemValue(itemId, countedQuantity);
		boolean removedDrop = trackedQuantity <= countedQuantity;
		if (config.clanLootTracker())
		{
			markPvmActivity();
			npcLootValue += value;
		}
		addLootStats(itemId, countedQuantity, value);
		decrementNpcDropQuantity(key, countedQuantity);
		if (removedDrop)
		{
			clearValuableDropAlert(key);
		}

		if (config.clanLootTracker() && isForeverTrackerMode())
		{
			persistLootTrackerValue();
		}

		refreshStatsPanel();
	}

	private void triggerValuableDropAlert(GroundItemKey key, int itemId, int quantity, long value)
	{
		long threshold = Math.max(0L, config.valuableDropThreshold());
		if (value < threshold || (!config.valuableDropFlash() && !config.valuableDropSound()))
		{
			return;
		}

		if (config.valuableDropFlash())
		{
			valuableDropAlertKeys.add(key);
			valuableDropFlashSequenceStartMillis = System.currentTimeMillis();
		}

		if (config.valuableDropSound())
		{
			playValuableDropPing(itemId, quantity, value);
		}

	}

	private void clearValuableDropAlert(GroundItemKey key)
	{
		if (!valuableDropAlertKeys.remove(key))
		{
			return;
		}

		if (valuableDropAlertKeys.isEmpty())
		{
			valuableDropFlashSequenceStartMillis = -1L;
		}
	}

	private void cacheItemDisplayName(int itemId)
	{
		String name = itemManager.getItemComposition(itemId).getName();
		itemDisplayNames.put(itemId, name == null || name.isBlank() ? "Unknown item" : name);
		pendingItemDisplayNames.remove(itemId);
	}

	private void playValuableDropPing(int itemId, int quantity, long value)
	{
		String itemName = itemManager.getItemComposition(itemId).getName();
		String message = "Valuable drop: " + QuantityFormatter.quantityToStackSize(value) + " gp";
		if (itemName != null && !itemName.isBlank())
		{
			message += " (" + quantity + " x " + itemName + ")";
		}

		notifier.notify(VALUABLE_DROP_SOUND_NOTIFICATION, message);
	}

	private void decrementNpcDropQuantity(GroundItemKey key, int quantity)
	{
		Integer trackedQuantity = npcDropQuantities.get(key);
		if (trackedQuantity == null)
		{
			return;
		}

		if (trackedQuantity > quantity)
		{
			npcDropQuantities.put(key, trackedQuantity - quantity);
		}
		else
		{
			npcDropQuantities.remove(key);
		}
	}

	private boolean removePendingGroundItemPickup(GroundItemKey key)
	{
		int tickCount = client.getTickCount();
		Iterator<PendingGroundItemPickup> iterator = pendingGroundItemPickups.iterator();
		while (iterator.hasNext())
		{
			PendingGroundItemPickup pendingPickup = iterator.next();
			if (tickCount - pendingPickup.getTick() > PENDING_PICKUP_TICK_WINDOW)
			{
				iterator.remove();
				continue;
			}

			if (pendingPickup.getKey().equals(key))
			{
				iterator.remove();
				return true;
			}
		}

		return false;
	}

	private boolean isNearRecentNpcDeath(WorldPoint worldPoint)
	{
		int tickCount = client.getTickCount();
		for (NpcDeathDropSource npcDeath : recentNpcDeaths)
		{
			if (tickCount - npcDeath.getTick() <= NPC_DROP_TICK_WINDOW
				&& npcDeath.getWorldPoint().getPlane() == worldPoint.getPlane()
				&& npcDeath.getWorldPoint().distanceTo2D(worldPoint) <= NPC_DROP_DISTANCE)
			{
				return true;
			}
		}

		return false;
	}

	private boolean isLocalPlayerNear(WorldPoint worldPoint)
	{
		Player localPlayer = client.getLocalPlayer();
		return localPlayer != null
			&& localPlayer.getWorldLocation().getPlane() == worldPoint.getPlane()
			&& localPlayer.getWorldLocation().distanceTo2D(worldPoint) <= 3;
	}

	private void cleanupNpcDropTracking()
	{
		int tickCount = client.getTickCount();
		recentNpcDeaths.removeIf(npcDeath -> tickCount - npcDeath.getTick() > NPC_DROP_TICK_WINDOW);
		pendingGroundItemPickups.removeIf(pendingPickup -> tickCount - pendingPickup.getTick() > PENDING_PICKUP_TICK_WINDOW);
	}

	private void clearNpcDropTracking()
	{
		recentNpcDeaths.clear();
		pendingGroundItemPickups.clear();
		npcDropQuantities.clear();
		valuableDropAlertKeys.clear();
		valuableDropFlashSequenceStartMillis = -1L;
	}

	private long getItemValue(int itemId, int quantity)
	{
		int price = getConfiguredItemPrice(ItemVariationMapping.map(itemId));
		if (price <= 0)
		{
			price = itemManager.getItemComposition(itemId).getPrice();
		}

		return Math.max(0L, (long) price) * quantity;
	}

	private int getConfiguredItemPrice(int itemId)
	{
		if (config.priceSource() == ToolkitPriceSource.RUNELITE)
		{
			return itemManager.getItemPrice(itemId);
		}

		return itemManager.getItemPriceWithSource(itemId, false);
	}

	private long getPotionDoseValue(int itemId)
	{
		int fullPotionId = getFourDosePotionId(itemId);
		if (fullPotionId > 0)
		{
			return Math.max(1L, getItemValue(fullPotionId, 1) / 4L);
		}

		return getItemValue(itemId, 1);
	}

	private int getFourDosePotionId(int itemId)
	{
		switch (itemId)
		{
			case ItemID.PRAYER_POTION4:
			case ItemID.PRAYER_POTION3:
			case ItemID.PRAYER_POTION2:
			case ItemID.PRAYER_POTION1:
				return ItemID.PRAYER_POTION4;
			case ItemID.PRAYER_POTION4_20393:
			case ItemID.PRAYER_POTION3_20394:
			case ItemID.PRAYER_POTION2_20395:
			case ItemID.PRAYER_POTION1_20396:
				return ItemID.PRAYER_POTION4_20393;
			case ItemID.SUPER_RESTORE4:
			case ItemID.SUPER_RESTORE3:
			case ItemID.SUPER_RESTORE2:
			case ItemID.SUPER_RESTORE1:
				return ItemID.SUPER_RESTORE4;
			case ItemID.SUPER_RESTORE4_23567:
			case ItemID.SUPER_RESTORE3_23569:
			case ItemID.SUPER_RESTORE2_23571:
			case ItemID.SUPER_RESTORE1_23573:
				return ItemID.SUPER_RESTORE4_23567;
			case ItemID.SARADOMIN_BREW4:
			case ItemID.SARADOMIN_BREW3:
			case ItemID.SARADOMIN_BREW2:
			case ItemID.SARADOMIN_BREW1:
				return ItemID.SARADOMIN_BREW4;
			case ItemID.SARADOMIN_BREW4_23575:
			case ItemID.SARADOMIN_BREW3_23577:
			case ItemID.SARADOMIN_BREW2_23579:
			case ItemID.SARADOMIN_BREW1_23581:
				return ItemID.SARADOMIN_BREW4_23575;
			case ItemID.SUPER_COMBAT_POTION4:
			case ItemID.SUPER_COMBAT_POTION3:
			case ItemID.SUPER_COMBAT_POTION2:
			case ItemID.SUPER_COMBAT_POTION1:
				return ItemID.SUPER_COMBAT_POTION4;
			case ItemID.SUPER_COMBAT_POTION4_23543:
			case ItemID.SUPER_COMBAT_POTION3_23545:
			case ItemID.SUPER_COMBAT_POTION2_23547:
			case ItemID.SUPER_COMBAT_POTION1_23549:
				return ItemID.SUPER_COMBAT_POTION4_23543;
			case ItemID.RANGING_POTION4:
			case ItemID.RANGING_POTION3:
			case ItemID.RANGING_POTION2:
			case ItemID.RANGING_POTION1:
				return ItemID.RANGING_POTION4;
			case ItemID.RANGING_POTION4_23551:
			case ItemID.RANGING_POTION3_23553:
			case ItemID.RANGING_POTION2_23555:
			case ItemID.RANGING_POTION1_23557:
				return ItemID.RANGING_POTION4_23551;
			case ItemID.MAGIC_POTION4:
			case ItemID.MAGIC_POTION3:
			case ItemID.MAGIC_POTION2:
			case ItemID.MAGIC_POTION1:
				return ItemID.MAGIC_POTION4;
			case ItemID.DIVINE_SUPER_COMBAT_POTION4:
			case ItemID.DIVINE_SUPER_COMBAT_POTION3:
			case ItemID.DIVINE_SUPER_COMBAT_POTION2:
			case ItemID.DIVINE_SUPER_COMBAT_POTION1:
				return ItemID.DIVINE_SUPER_COMBAT_POTION4;
			case ItemID.DIVINE_RANGING_POTION4:
			case ItemID.DIVINE_RANGING_POTION3:
			case ItemID.DIVINE_RANGING_POTION2:
			case ItemID.DIVINE_RANGING_POTION1:
				return ItemID.DIVINE_RANGING_POTION4;
			case ItemID.DIVINE_MAGIC_POTION4:
			case ItemID.DIVINE_MAGIC_POTION3:
			case ItemID.DIVINE_MAGIC_POTION2:
			case ItemID.DIVINE_MAGIC_POTION1:
				return ItemID.DIVINE_MAGIC_POTION4;
			case ItemID.DIVINE_BASTION_POTION4:
			case ItemID.DIVINE_BASTION_POTION3:
			case ItemID.DIVINE_BASTION_POTION2:
			case ItemID.DIVINE_BASTION_POTION1:
				return ItemID.DIVINE_BASTION_POTION4;
			case ItemID.DIVINE_BATTLEMAGE_POTION4:
			case ItemID.DIVINE_BATTLEMAGE_POTION3:
			case ItemID.DIVINE_BATTLEMAGE_POTION2:
			case ItemID.DIVINE_BATTLEMAGE_POTION1:
				return ItemID.DIVINE_BATTLEMAGE_POTION4;
			case ItemID.ANTIFIRE_POTION4:
			case ItemID.ANTIFIRE_POTION3:
			case ItemID.ANTIFIRE_POTION2:
			case ItemID.ANTIFIRE_POTION1:
				return ItemID.ANTIFIRE_POTION4;
			case ItemID.EXTENDED_ANTIFIRE4:
			case ItemID.EXTENDED_ANTIFIRE3:
			case ItemID.EXTENDED_ANTIFIRE2:
			case ItemID.EXTENDED_ANTIFIRE1:
				return ItemID.EXTENDED_ANTIFIRE4;
			case ItemID.SUPER_ANTIFIRE_POTION4:
			case ItemID.SUPER_ANTIFIRE_POTION3:
			case ItemID.SUPER_ANTIFIRE_POTION2:
			case ItemID.SUPER_ANTIFIRE_POTION1:
				return ItemID.SUPER_ANTIFIRE_POTION4;
			case ItemID.EXTENDED_SUPER_ANTIFIRE4:
			case ItemID.EXTENDED_SUPER_ANTIFIRE3:
			case ItemID.EXTENDED_SUPER_ANTIFIRE2:
			case ItemID.EXTENDED_SUPER_ANTIFIRE1:
				return ItemID.EXTENDED_SUPER_ANTIFIRE4;
			case ItemID.ANTIVENOM4:
			case ItemID.ANTIVENOM3:
			case ItemID.ANTIVENOM2:
			case ItemID.ANTIVENOM1:
				return ItemID.ANTIVENOM4;
			case ItemID.ANTIVENOM4_12913:
			case ItemID.ANTIVENOM3_12915:
			case ItemID.ANTIVENOM2_12917:
			case ItemID.ANTIVENOM1_12919:
				return ItemID.ANTIVENOM4_12913;
			case ItemID.EXTENDED_ANTIVENOM4:
			case ItemID.EXTENDED_ANTIVENOM3:
			case ItemID.EXTENDED_ANTIVENOM2:
			case ItemID.EXTENDED_ANTIVENOM1:
				return ItemID.EXTENDED_ANTIVENOM4;
			case ItemID.STAMINA_POTION4:
			case ItemID.STAMINA_POTION3:
			case ItemID.STAMINA_POTION2:
			case ItemID.STAMINA_POTION1:
				return ItemID.STAMINA_POTION4;
			case ItemID.STAMINA_POTION4_23583:
			case ItemID.STAMINA_POTION3_23585:
			case ItemID.STAMINA_POTION2_23587:
			case ItemID.STAMINA_POTION1_23589:
				return ItemID.STAMINA_POTION4_23583;
			case ItemID.EXTENDED_STAMINA_POTION4:
			case ItemID.EXTENDED_STAMINA_POTION3:
			case ItemID.EXTENDED_STAMINA_POTION2:
			case ItemID.EXTENDED_STAMINA_POTION1:
				return ItemID.EXTENDED_STAMINA_POTION4;
			case ItemID.BASTION_POTION4:
			case ItemID.BASTION_POTION3:
			case ItemID.BASTION_POTION2:
			case ItemID.BASTION_POTION1:
				return ItemID.BASTION_POTION4;
			case ItemID.BATTLEMAGE_POTION4:
			case ItemID.BATTLEMAGE_POTION3:
			case ItemID.BATTLEMAGE_POTION2:
			case ItemID.BATTLEMAGE_POTION1:
				return ItemID.BATTLEMAGE_POTION4;
			case ItemID.OVERLOAD_4:
			case ItemID.OVERLOAD_3:
			case ItemID.OVERLOAD_2:
			case ItemID.OVERLOAD_1:
				return ItemID.OVERLOAD_4;
			case ItemID.BLIGHTED_OVERLOAD_4:
			case ItemID.BLIGHTED_OVERLOAD_3:
			case ItemID.BLIGHTED_OVERLOAD_2:
			case ItemID.BLIGHTED_OVERLOAD_1:
				return ItemID.BLIGHTED_OVERLOAD_4;
			default:
				return 0;
		}
	}

	private void resetLootTracker()
	{
		npcLootValue = 0L;
		persistLootTrackerValue();
	}

	private void resetSupplyCostTracker()
	{
		supplyCostValue = 0L;
		potionSupplyCostValue = 0L;
		foodSupplyCostValue = 0L;
		cannonballSupplyCostValue = 0L;
		potionSupplyDoseCount = 0L;
		foodSupplyCount = 0L;
		cannonballSupplyCount = 0L;
		persistSupplyCostTrackerValue();
	}

	private void resetCombatXpTracker()
	{
		for (Skill skill : COMBAT_TRACKER_SKILLS)
		{
			combatXpGainedBySkill.put(skill, 0L);
			if (isLoggedIn())
			{
				trackerBaseExperience.put(skill, client.getSkillExperience(skill));
			}
		}
		persistCombatXpTrackerValues();
	}

	private void resetSlayerXpTracker()
	{
		slayerXpGained = 0L;
		if (isLoggedIn())
		{
			trackerBaseExperience.put(Skill.SLAYER, client.getSkillExperience(Skill.SLAYER));
		}
		persistSlayerXpTrackerValue();
	}

	void resetTrackers(boolean resetLoot, boolean resetSupplyCost, boolean resetCombatXp, boolean resetSlayerXp)
	{
		if (resetLoot)
		{
			resetLootTracker();
		}

		if (resetSupplyCost)
		{
			resetSupplyCostTracker();
		}

		if (resetCombatXp)
		{
			resetCombatXpTracker();
		}

		if (resetSlayerXp)
		{
			resetSlayerXpTracker();
		}

		resetStatsCategories(resetLoot, resetSupplyCost, resetCombatXp, resetSlayerXp);
		resetCurrentSlayerTaskCategories(resetLoot, resetSupplyCost, resetCombatXp, resetSlayerXp);
		updateChatTabTrackersLater();
		refreshStatsPanel();
	}

	private void resetStatsCategories(boolean resetLoot, boolean resetSupplyCost, boolean resetCombatXp, boolean resetSlayerXp)
	{
		for (PvmToolsStatsPeriod period : PvmToolsStatsPeriod.values())
		{
			PvmToolsStats stats = getStats(period);
			if (resetLoot)
			{
				stats.resetLoot();
			}
			if (resetSupplyCost)
			{
				stats.resetSupplyCost();
			}
			if (resetCombatXp)
			{
				stats.resetCombatXp();
			}
			if (resetSlayerXp)
			{
				stats.resetSlayerXp();
			}
			persistStatsValue(period);
		}
	}

	private void resetCurrentSlayerTaskCategories(boolean resetLoot, boolean resetSupplyCost, boolean resetCombatXp, boolean resetSlayerXp)
	{
		if (resetLoot)
		{
			currentSlayerTaskLootValue = 0L;
		}
		if (resetSupplyCost)
		{
			currentSlayerTaskSupplyCostValue = 0L;
			currentSlayerTaskPotionDoseCount = 0L;
			currentSlayerTaskFoodCount = 0L;
			currentSlayerTaskCannonballCount = 0L;
		}
		if (resetCombatXp)
		{
			currentSlayerTaskCombatXp = 0L;
		}
		if (resetSlayerXp)
		{
			currentSlayerTaskSlayerXp = 0L;
		}
		persistCurrentSlayerTaskState();
	}

	private void resetSelectedTracker()
	{
		switch (config.resetTrackerTarget())
		{
			case LOOT:
				resetTrackers(true, false, false, false);
				break;
			case SUPPLY_COST:
				resetTrackers(false, true, false, false);
				break;
			case COMBAT_XP:
				resetTrackers(false, false, true, false);
				break;
			case SLAYER_XP:
				resetTrackers(false, false, false, true);
				break;
			case ALL:
				resetTrackers(true, true, true, true);
				break;
		}

		configManager.setConfiguration(PvmToolsConfig.GROUP, "resetSelectedTracker", false);
	}

	private void persistLootTrackerValue()
	{
		configManager.setConfiguration(PvmToolsConfig.GROUP, SAVED_LOOT_TRACKER_KEY, Long.toString(npcLootValue));
	}

	private void persistSupplyCostTrackerValue()
	{
		configManager.setConfiguration(PvmToolsConfig.GROUP, SAVED_SUPPLY_COST_TRACKER_KEY, Long.toString(supplyCostValue));
		configManager.setConfiguration(PvmToolsConfig.GROUP, SAVED_SUPPLY_COST_BREAKDOWN_KEY,
			"potionValue=" + potionSupplyCostValue
				+ ";foodValue=" + foodSupplyCostValue
				+ ";cannonballValue=" + cannonballSupplyCostValue
				+ ";potionDoses=" + potionSupplyDoseCount
				+ ";foodCount=" + foodSupplyCount
				+ ";cannonballCount=" + cannonballSupplyCount);
	}

	private void persistCombatXpTrackerValues()
	{
		StringBuilder savedValues = new StringBuilder();
		for (Skill skill : COMBAT_TRACKER_SKILLS)
		{
			if (savedValues.length() > 0)
			{
				savedValues.append(';');
			}

			savedValues.append(skill.name())
				.append('=')
				.append(combatXpGainedBySkill.getOrDefault(skill, 0L));
		}

		configManager.setConfiguration(PvmToolsConfig.GROUP, SAVED_COMBAT_XP_TRACKER_KEY, savedValues.toString());
	}

	private void persistSlayerXpTrackerValue()
	{
		configManager.setConfiguration(PvmToolsConfig.GROUP, SAVED_SLAYER_XP_TRACKER_KEY, Long.toString(slayerXpGained));
	}

	private long parseLongConfig(String value)
	{
		if (value == null || value.isBlank())
		{
			return 0L;
		}

		try
		{
			return Long.parseLong(value.trim());
		}
		catch (NumberFormatException ex)
		{
			return 0L;
		}
	}

	private int parseIntConfig(String value)
	{
		if (value == null || value.isBlank())
		{
			return 0;
		}

		try
		{
			return Integer.parseInt(value.trim());
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
	}

	private String formatDurationShort(long totalSeconds)
	{
		if (totalSeconds <= 0)
		{
			return "0m";
		}

		long hours = totalSeconds / 3600L;
		long minutes = totalSeconds % 3600L / 60L;
		if (hours > 0)
		{
			return hours + "h " + minutes + "m";
		}

		if (minutes > 0)
		{
			return minutes + "m";
		}

		return totalSeconds + "s";
	}

	private String formatShortValue(long value)
	{
		return QuantityFormatter.quantityToStackSize(value).toLowerCase(Locale.ENGLISH);
	}

	private boolean isNpcCombatInteraction(MenuOptionClicked event)
	{
		if (event.getMenuEntry() == null || event.getMenuEntry().getNpc() == null)
		{
			return false;
		}

		String option = Text.removeTags(event.getMenuOption()).toLowerCase(Locale.ENGLISH);
		return option.equals("attack") || option.startsWith("attack ") || option.contains("cast");
	}

	private boolean isCannonPickupInteraction(MenuOptionClicked event)
	{
		String option = event.getMenuOption();
		String target = event.getMenuTarget();
		if (option == null || target == null)
		{
			return false;
		}

		String cleanOption = Text.removeTags(option).toLowerCase(Locale.ENGLISH);
		String cleanTarget = Text.removeTags(target).toLowerCase(Locale.ENGLISH);
		return (cleanOption.contains("pick-up") || cleanOption.contains("pickup"))
			&& cleanTarget.contains("cannon");
	}

	private void markPvmActivity()
	{
		resumeCurrentSlayerTaskTimer();
		if (!ownsToolkitUi())
		{
			setToolkitUiOwner(TOOLKIT_UI_OWNER_PVM);
		}
	}

	private void markCombatActivity()
	{
		lastCombatActivityTick = client.getTickCount();
		markPvmActivity();
	}

	private boolean isCombatWarningActive()
	{
		Player player = client.getLocalPlayer();
		if (player != null && player.getInteracting() instanceof NPC)
		{
			lastCombatActivityTick = client.getTickCount();
			return true;
		}

		return lastCombatActivityTick != Integer.MIN_VALUE
			&& client.getTickCount() - lastCombatActivityTick <= COMBAT_WARNING_GRACE_TICKS;
	}

	private boolean handleToolkitTabToggle(MenuOptionClicked event)
	{
		if (!isAllChatTabClick(event))
		{
			return false;
		}

		int currentTick = client.getTickCount();
		long lastToggleTick = parseLongConfig(configManager.getConfiguration(
			TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_LAST_TOGGLE_TICK_KEY));
		if (lastToggleTick == currentTick)
		{
			return true;
		}

		configManager.setConfiguration(
			TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_LAST_TOGGLE_TICK_KEY, Integer.toString(currentTick));
		boolean pvmEnabled = isToolkitPluginEnabled(PVM_PLUGIN_CLASS_NAME);
		boolean skillingEnabled = isToolkitPluginEnabled(SKILLING_PLUGIN_CLASS_NAME);
		String owner = configManager.getConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_KEY);
		String nextOwner;
		if (pvmEnabled && skillingEnabled)
		{
			nextOwner = TOOLKIT_UI_OWNER_PVM.equals(owner) ? TOOLKIT_UI_OWNER_SKILLING : TOOLKIT_UI_OWNER_PVM;
		}
		else
		{
			nextOwner = skillingEnabled ? TOOLKIT_UI_OWNER_SKILLING : TOOLKIT_UI_OWNER_PVM;
		}

		setToolkitUiOwner(nextOwner);
		return true;
	}

	private static boolean isAllChatTabClick(MenuOptionClicked event)
	{
		Widget widget = event.getWidget();
		return event.getParam1() == ComponentID.CHATBOX_TAB_ALL
			|| (widget != null && (widget.getId() == ComponentID.CHATBOX_TAB_ALL
				|| widget.getParentId() == ComponentID.CHATBOX_TAB_ALL));
	}

	private void ensureToolkitOwnerAvailable()
	{
		String owner = configManager.getConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_KEY);
		if (TOOLKIT_UI_OWNER_PVM.equals(owner) && isToolkitPluginEnabled(PVM_PLUGIN_CLASS_NAME))
		{
			return;
		}

		if (TOOLKIT_UI_OWNER_SKILLING.equals(owner) && isToolkitPluginEnabled(SKILLING_PLUGIN_CLASS_NAME))
		{
			return;
		}

		setToolkitUiOwner(TOOLKIT_UI_OWNER_PVM);
	}

	private boolean isToolkitPluginEnabled(String className)
	{
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (className.equals(plugin.getClass().getName()) && pluginManager.isPluginEnabled(plugin))
			{
				return true;
			}
		}

		return false;
	}

	private void setToolkitUiOwner(String owner)
	{
		configManager.setConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_KEY, owner);
		configManager.setConfiguration(
			TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_TICK_KEY, Integer.toString(client.getTickCount()));
		clientThread.invokeLater(() ->
		{
			syncInventoryInfoBox();
			updateChatTabTrackers();
		});
	}

	private boolean ownsToolkitUi()
	{
		return TOOLKIT_UI_OWNER_PVM.equals(configManager.getConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_KEY));
	}

	private boolean isOtherToolkitUiOwnerActive()
	{
		return TOOLKIT_UI_OWNER_SKILLING.equals(configManager.getConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_KEY))
			&& isToolkitPluginEnabled(SKILLING_PLUGIN_CLASS_NAME);
	}

	private boolean isToolkitUiReady()
	{
		if (!ownsToolkitUi())
		{
			return false;
		}

		long ownerTick = parseLongConfig(configManager.getConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_TICK_KEY));
		int currentTick = client.getTickCount();
		if (ownerTick > currentTick)
		{
			configManager.setConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_TICK_KEY, Integer.toString(Math.max(0, currentTick - 1)));
			return true;
		}

		return ownerTick < currentTick;
	}

	private boolean isTypeVisible(CombatPotionTimerType type)
	{
		return config.showPotionTimers();
	}

	private void removePotionTimers()
	{
		for (CombatPotionTimerType type : CombatPotionTimerType.values())
		{
			if (isPotionTimerType(type))
			{
				removeTimer(type);
			}
		}
	}

	private void removeTimers(CombatPotionTimerType... types)
	{
		for (CombatPotionTimerType type : types)
		{
			removeTimer(type);
		}
	}

	private void expireTimers(CombatPotionTimerType... types)
	{
		for (CombatPotionTimerType type : types)
		{
			expireTimer(type);
		}
	}

	private void expireTimer(CombatPotionTimerType type)
	{
		if (config.soundPotionExpired()
			&& isPotionTimerType(type)
			&& timers.containsKey(type)
			&& potionExpirySoundTriggered.add(type))
		{
			playWarningPing();
		}

		removeTimer(type);
	}

	private void removeTimer(CombatPotionTimerType type)
	{
		CombatPotionTimerInfoBox timer = timers.remove(type);
		if (timer != null)
		{
			infoBoxManager.removeInfoBox(timer);
		}
	}

	private void clearTimers()
	{
		for (CombatPotionTimerInfoBox timer : timers.values())
		{
			infoBoxManager.removeInfoBox(timer);
		}
		timers.clear();
	}

	private void clearPluginInfoBoxes()
	{
		infoBoxManager.removeIf(infoBox -> infoBox instanceof CombatPotionTimerInfoBox || infoBox instanceof InventorySpacesInfoBox);
	}

	private void resetFamilyState()
	{
		nextPoisonTick = -1;
		nextOverloadRefreshTick = -1;
		nextAntifireTick = -1;
		nextSuperAntifireTick = -1;
		lastAntifireTicks = -1;
		lastSuperAntifireTicks = -1;
		antiVenomPlusActive = false;
		antifireType = null;
		superAntifireType = null;
		resetPrayerCountdown();
		prayerExpirySoundTriggered = false;
		trackerBaseExperience.clear();
		warningTriggered.clear();
		potionExpirySoundTriggered.clear();
		resetFlashSequence();
		valuableDropAlertKeys.clear();
		valuableDropFlashSequenceStartMillis = -1L;
		closeWarningPopupInterface();
		inventoryFullWarningTriggered = false;
		resetCannonState();
	}

	private void removePrayerTimer()
	{
		removeTimer(CombatPotionTimerType.PRAYER);
	}

	private void resetPrayerCountdown()
	{
		prayerCountdownActive = false;
		estimatedPrayerEndTimeMillis = 0L;
		lastPrayerLevel = -1;
		lastPrayerBonus = Integer.MIN_VALUE;
		lastPrayerDrainEffect = -1;
	}

	private void removeInventoryInfoBox()
	{
		if (inventorySpacesInfoBox != null)
		{
			infoBoxManager.removeInfoBox(inventorySpacesInfoBox);
			inventorySpacesInfoBox = null;
		}
	}

	private int getOccupiedInventorySlots(ItemContainer inventory)
	{
		int occupiedSlots = 0;
		for (Item item : inventory.getItems())
		{
			if (isOccupiedInventorySlot(item))
			{
				occupiedSlots++;
			}
		}

		return occupiedSlots;
	}

	private long getInventoryValue(ItemContainer inventory)
	{
		long value = 0L;
		for (Item item : inventory.getItems())
		{
			if (!isOccupiedInventorySlot(item))
			{
				continue;
			}

			int itemId = itemManager.canonicalize(item.getId());
			long quantity = item.getQuantity();
			if (itemId == ItemID.COINS_995)
			{
				value += quantity;
				continue;
			}

			if (itemId == ItemID.PLATINUM_TOKEN)
			{
				value += quantity * 1000L;
				continue;
			}

			int price = getConfiguredItemPrice(itemId);
			if (price > 0)
			{
				value += quantity * price;
			}
		}

		return value;
	}

	private String buildInventoryTooltip(long inventoryValue)
	{
		return "Inventory value: " + QuantityFormatter.quantityToStackSize(inventoryValue) + " gp";
	}

	private boolean isOccupiedInventorySlot(Item item)
	{
		return item != null && item.getId() != -1 && item.getQuantity() > 0;
	}

	private boolean shouldDrawRenderable(Renderable renderable, boolean drawingUi)
	{
		if (!config.hideDeathSpawns() || !(renderable instanceof NPC))
		{
			return true;
		}

		return !isDeathSpawn((NPC) renderable);
	}

	private boolean isDeathSpawn(NPC npc)
	{
		switch (npc.getId())
		{
			case NpcID.SLAYER_NECHRYAEL_SPAWN:
			case NpcID.SUPERIOR_NECHRYAEL_MELEE_SPAWN:
			case NpcID.SUPERIOR_NECHRYAEL_RANGED_SPAWN:
			case NpcID.SUPERIOR_NECHRYAEL_MAGIC_SPAWN:
				return true;
			default:
				return false;
		}
	}

	private boolean isSuperiorSlayerMonster(NPC npc)
	{
		return getSuperiorSlayerHint(npc) != null;
	}

	private SuperiorSlayerHint getSuperiorSlayerHint(NPC npc)
	{
		if (npc == null)
		{
			return null;
		}

		switch (npc.getId())
		{
			case NpcID.SUPERIOR_CRAWLING_HAND:
				return new SuperiorSlayerHint("Melee", "Treat it like a stronger crawling hand and keep melee prayer up.");
			case NpcID.SUPERIOR_CAVE_CRAWLER:
			case NpcID.SUPERIOR_CAVE_CRAWLER_ICE:
				return new SuperiorSlayerHint("Melee", "Keep anti-poison ready and kill it like the normal crawler.");
			case NpcID.SUPERIOR_BANSHEE:
			case NpcID.SUPERIOR_KOUREND_BANSHEE:
				return new SuperiorSlayerHint("Magic", "Keep your earmuffs or Slayer helmet on and burst it down.");
			case NpcID.SUPERIOR_ROCKSLUG:
				return new SuperiorSlayerHint("Melee", "Keep melee prayer up and remember salt when it is low.");
			case NpcID.SUPERIOR_COCKATRICE:
				return new SuperiorSlayerHint("Melee", "Wear mirror protection and keep attacking safely.");
			case NpcID.SUPERIOR_PYREFIEND:
			case NpcID.SUPERIOR_PYRELORD:
				return new SuperiorSlayerHint("Magic", "Pray magic and keep DPS high before it drags the fight out.");
			case NpcID.SUPERIOR_BASILISK:
			case NpcID.SUPERIOR_BASILISK_KNIGHT:
				return new SuperiorSlayerHint("Melee", "Wear mirror protection and keep melee prayer up.");
			case NpcID.SUPERIOR_INFERNAL_MAGE:
				return new SuperiorSlayerHint("Magic", "Pray magic and kill it before it chips you down.");
			case NpcID.SUPERIOR_BLOODVELD:
			case NpcID.SUPERIOR_KOUREND_BLOODVELD:
				return new SuperiorSlayerHint("Melee", "Pray melee and keep distance only if your setup needs it.");
			case NpcID.SUPERIOR_JELLY:
			case NpcID.SUPERIOR_KOUREND_JELLY:
			case NpcID.SUPERIOR_CHILLED_JELLY:
				return new SuperiorSlayerHint("Melee", "Pray melee and focus it down before swapping back to the task.");
			case NpcID.SUPERIOR_CAVE_HORROR:
				return new SuperiorSlayerHint("Melee", "Wear witchwood icon or Slayer helmet and keep melee prayer up.");
			case NpcID.SUPERIOR_ABBERANT_SPECTRE:
			case NpcID.SUPERIOR_KOUREND_SPECTRE:
				return new SuperiorSlayerHint("Melee", "Wear nose protection and keep melee prayer up.");
			case NpcID.SUPERIOR_DUSTDEVIL:
				return new SuperiorSlayerHint("Melee", "Wear face protection and keep melee prayer up.");
			case NpcID.SUPERIOR_KURASK:
				return new SuperiorSlayerHint("Melee", "Use leaf-bladed or broad gear and keep melee prayer up.");
			case NpcID.SUPERIOR_SMOKE_DEVIL:
				return new SuperiorSlayerHint("Ranged", "Keep face protection on and pray ranged while you burn it down.");
			case NpcID.SUPERIOR_GARGOYLE:
				return new SuperiorSlayerHint("Melee", "Pray melee and use rock hammer if the fight requires it.");
			case NpcID.SUPERIOR_DARK_BEAST:
				return new SuperiorSlayerHint("Melee", "Pray melee up close and keep HP high.");
			case NpcID.SUPERIOR_ABYSSAL_DEMON:
				return new SuperiorSlayerHint("Melee", "Pray melee and be ready for teleports during the fight.");
			case NpcID.SUPERIOR_NECHRYAEL:
				return new SuperiorSlayerHint("Melee", "Pray melee and ignore the Death Spawns while killing the superior.");
			case NpcID.SUPERIOR_TUROTH:
				return new SuperiorSlayerHint("Melee", "Use leaf-bladed or broad gear and keep melee prayer up.");
			case NpcID.SUPERIOR_WYRM_DARK:
			case NpcID.SUPERIOR_WYRM_LIGHT:
				return new SuperiorSlayerHint("Magic", "Pray magic unless you are forcing melee range.");
			case NpcID.SUPERIOR_DRAKE:
				return new SuperiorSlayerHint("Ranged", "Pray ranged and avoid standing in special attack damage.");
			case NpcID.SUPERIOR_HYDRA:
				return new SuperiorSlayerHint("Ranged", "Pray ranged and keep moving if specials appear.");
			case NpcID.SUPERIOR_WARPED_TERRORBIRD:
				return new SuperiorSlayerHint("Melee", "Pray melee and keep your damage steady.");
			case NpcID.SUPERIOR_WARPED_TORTOISE:
				return new SuperiorSlayerHint("Melee", "Pray melee and stay close to finish it quickly.");
			case NpcID.SUPERIOR_ARAXYTE:
				return new SuperiorSlayerHint("Melee", "Pray melee and watch for venom or web pressure.");
			case NpcID.SUPERIOR_AQUANITE:
			case NpcID.SUPERIOR_AQUANITE_NOLURE:
				return new SuperiorSlayerHint("Magic", "Pray magic and keep attacking through its defensive stats.");
			case NpcID.SUPERIOR_LAVA_STRYKEWYRM:
				return new SuperiorSlayerHint("Magic", "Pray magic and stay ready for Wilderness pressure.");
			default:
				return null;
		}
	}

	private boolean shouldFlashTimer(CombatPotionTimerType type)
	{
		if (type == CombatPotionTimerType.PRAYER)
		{
			return config.flashPrayerWarnings();
		}

		return config.flashPotionWarnings() && isCombatWarningActive();
	}

	private boolean isPotionTimerType(CombatPotionTimerType type)
	{
		return type != CombatPotionTimerType.PRAYER;
	}

	private boolean isPositive(Duration duration)
	{
		return !duration.isNegative() && !duration.isZero();
	}

	private void playWarningPing()
	{
		notifier.notify(SOUND_ONLY_NOTIFICATION, "PvM Tools ping.");
	}

	private boolean isLoggedIn()
	{
		return client.getGameState() == GameState.LOGGED_IN;
	}

	private boolean isResetGameState(GameState gameState)
	{
		return gameState == GameState.STARTING
			|| gameState == GameState.LOGIN_SCREEN
			|| gameState == GameState.LOGIN_SCREEN_AUTHENTICATOR
			|| gameState == GameState.LOGGING_IN
			|| gameState == GameState.CONNECTION_LOST
			|| gameState == GameState.UNKNOWN;
	}

	private boolean isAntiVenomActive(int poisonValue)
	{
		return poisonValue < -38;
	}

	private boolean isPotionMessage(ChatMessageType type)
	{
		return type == ChatMessageType.SPAM || type == ChatMessageType.GAMEMESSAGE;
	}

	private static final class NpcDeathDropSource
	{
		private final WorldPoint worldPoint;
		private final int tick;

		private NpcDeathDropSource(WorldPoint worldPoint, int tick)
		{
			this.worldPoint = worldPoint;
			this.tick = tick;
		}

		private WorldPoint getWorldPoint()
		{
			return worldPoint;
		}

		private int getTick()
		{
			return tick;
		}
	}

	private static final class PendingGroundItemPickup
	{
		private final GroundItemKey key;
		private final int tick;

		private PendingGroundItemPickup(GroundItemKey key, int tick)
		{
			this.key = key;
			this.tick = tick;
		}

		private GroundItemKey getKey()
		{
			return key;
		}

		private int getTick()
		{
			return tick;
		}
	}

	private static final class CannonballUsageSample
	{
		private final long timeMillis;
		private final int count;

		private CannonballUsageSample(long timeMillis, int count)
		{
			this.timeMillis = timeMillis;
			this.count = count;
		}

		private long getTimeMillis()
		{
			return timeMillis;
		}

		private int getCount()
		{
			return count;
		}
	}

	private static final class GroundItemKey
	{
		private final int itemId;
		private final WorldPoint worldPoint;

		private GroundItemKey(int itemId, WorldPoint worldPoint)
		{
			this.itemId = itemId;
			this.worldPoint = worldPoint;
		}

		@Override
		public boolean equals(Object other)
		{
			if (this == other)
			{
				return true;
			}

			if (!(other instanceof GroundItemKey))
			{
				return false;
			}

			GroundItemKey that = (GroundItemKey) other;
			return itemId == that.itemId && Objects.equals(worldPoint, that.worldPoint);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(itemId, worldPoint);
		}
	}

	enum SupplyCostType
	{
		POTION,
		FOOD,
		CANNONBALL
	}

	private enum ChatTabTrackerType
	{
		LOOT,
		SUPPLY_COST,
		COMBAT_XP,
		SLAYER_XP,
		TOP_XP_SKILL
	}

	private String getSuperiorSpawnWarningMessage(NPC npc)
	{
		SuperiorSlayerHint hint = getSuperiorSlayerHint(npc);
		if (hint == null)
		{
			return "Kill superior.";
		}

		return "Kill superior - pray " + hint.getPrayer() + ".";
	}

	private static final class ChatTabTextStyle
	{
		private final int fontId;
		private final int textColor;
		private final boolean textShadowed;
		private final int xTextAlignment;
		private final int yTextAlignment;

		private ChatTabTextStyle(Widget widget)
		{
			fontId = widget.getFontId();
			textColor = widget.getTextColor();
			textShadowed = widget.getTextShadowed();
			xTextAlignment = widget.getXTextAlignment();
			yTextAlignment = widget.getYTextAlignment();
		}

		private void restore(Widget widget)
		{
			widget.setFontId(fontId);
			widget.setTextColor(textColor);
			widget.setTextShadowed(textShadowed);
			widget.setXTextAlignment(xTextAlignment);
			widget.setYTextAlignment(yTextAlignment);
		}
	}
}
