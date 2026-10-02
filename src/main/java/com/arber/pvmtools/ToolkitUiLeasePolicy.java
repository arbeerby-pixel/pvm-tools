package com.arber.pvmtools;

final class ToolkitUiLeasePolicy
{
	private static final String MANUAL_SOURCE = "MANUAL";

	private ToolkitUiLeasePolicy()
	{
	}

	static boolean shouldClaim(
		String requester,
		String currentOwner,
		String currentSource,
		boolean currentOwnerActive,
		long leaseUntilMillis,
		long nowMillis)
	{
		if (requester != null && requester.equals(currentOwner))
		{
			return false;
		}
		if (MANUAL_SOURCE.equals(currentSource))
		{
			return false;
		}
		return !currentOwnerActive || leaseUntilMillis <= nowMillis;
	}

	static boolean shouldRenew(
		String requester,
		String currentOwner,
		String currentSource,
		long leaseUntilMillis,
		long nowMillis,
		long renewWindowMillis)
	{
		return requester != null
			&& requester.equals(currentOwner)
			&& !MANUAL_SOURCE.equals(currentSource)
			&& leaseUntilMillis <= safeAdd(nowMillis, Math.max(0L, renewWindowMillis));
	}

	static long newLeaseUntil(long nowMillis, long leaseDurationMillis)
	{
		return safeAdd(Math.max(0L, nowMillis), Math.max(0L, leaseDurationMillis));
	}

	private static long safeAdd(long value, long addition)
	{
		return value > Long.MAX_VALUE - addition ? Long.MAX_VALUE : value + addition;
	}
}
