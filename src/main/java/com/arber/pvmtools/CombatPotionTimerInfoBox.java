package com.arber.pvmtools;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;
import net.runelite.client.ui.overlay.infobox.Timer;

class CombatPotionTimerInfoBox extends Timer
{
	private static final Color WARNING_RED = new Color(255, 45, 45);

	private final CombatPotionTimerType type;
	private final PvmToolsPlugin plugin;
	private long sortValue;

	CombatPotionTimerInfoBox(CombatPotionTimerType type, Duration duration, BufferedImage image, PvmToolsPlugin plugin)
	{
		super(Math.max(1L, duration.toMillis()), ChronoUnit.MILLIS, image, plugin);
		this.type = type;
		this.plugin = plugin;
		setTooltip(type.getDisplayName());
		setPriority(InfoBoxPriority.HIGH);
	}

	CombatPotionTimerType getType()
	{
		return type;
	}

	long getSortValue()
	{
		return sortValue;
	}

	void setSortValue(long sortValue)
	{
		this.sortValue = sortValue;
	}

	Duration getRemainingDuration()
	{
		return Duration.between(Instant.now(), getEndTime());
	}

	boolean isInWarningWindow()
	{
		Duration remaining = getRemainingDuration();
		return !remaining.isNegative()
			&& !remaining.isZero()
			&& remaining.compareTo(Duration.ofSeconds(plugin.getWarningSeconds())) <= 0;
	}

	@Override
	public Color getTextColor()
	{
		return isInWarningWindow() ? WARNING_RED : Color.WHITE;
	}

	@Override
	public String getName()
	{
		return type.name();
	}
}
