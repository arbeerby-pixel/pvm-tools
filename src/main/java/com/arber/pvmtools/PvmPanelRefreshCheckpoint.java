package com.arber.pvmtools;

/** Keeps a trailing refresh pending when several updates share one interval. */
final class PvmPanelRefreshCheckpoint
{
	private boolean pending;
	private long lastRefreshMillis = -1L;

	void request()
	{
		pending = true;
	}

	boolean takeIfDue(long nowMillis, long intervalMillis, boolean force)
	{
		if (!pending || (!force && lastRefreshMillis >= 0L && nowMillis >= lastRefreshMillis
			&& nowMillis - lastRefreshMillis < Math.max(0L, intervalMillis)))
		{
			return false;
		}
		pending = false;
		lastRefreshMillis = nowMillis;
		return true;
	}
}
