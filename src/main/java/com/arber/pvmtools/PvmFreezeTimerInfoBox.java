package com.arber.pvmtools;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;
import net.runelite.client.ui.overlay.infobox.Timer;

final class PvmFreezeTimerInfoBox extends Timer
{
	private static final Color WARNING_RED = new Color(255, 45, 45);

	private final PvmToolsPlugin plugin;

	PvmFreezeTimerInfoBox(Duration duration, BufferedImage image, String spellName, PvmToolsPlugin plugin)
	{
		super(Math.max(1L, duration.toMillis()), ChronoUnit.MILLIS, image, plugin);
		this.plugin = plugin;
		setTooltip("PvM freeze: " + spellName);
		setPriority(InfoBoxPriority.HIGH);
	}

	boolean isExpired()
	{
		return getRemainingDuration().isNegative() || getRemainingDuration().isZero();
	}

	private Duration getRemainingDuration()
	{
		return Duration.between(Instant.now(), getEndTime());
	}

	@Override
	public Color getTextColor()
	{
		return !isExpired() && getRemainingDuration().compareTo(Duration.ofSeconds(plugin.getWarningSeconds())) <= 0
			? WARNING_RED
			: Color.WHITE;
	}

	@Override
	public String getName()
	{
		return "PVM_FREEZE";
	}
}
