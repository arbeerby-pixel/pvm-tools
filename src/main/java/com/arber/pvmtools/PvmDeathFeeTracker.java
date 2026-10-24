package com.arber.pvmtools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;

/** Recognises completed reclaim payments; quotes, coffer deposits and coin changes are not inputs. */
final class PvmDeathFeeTracker
{
	private static final String COINS = "(?:[0-9]+|[1-9][0-9]{0,2}(?:,[0-9]{3})+)";
	private static final Pattern CHARGED = Pattern.compile(
		"^Death charges you (" + COINS + ") x Coins\\."
			+ "(?:\\s*You have (?:(" + COINS + ") x Coins left|nothing left) in Death's Coffer\\.)?$");
	private static final Pattern BANK_PAYMENT = Pattern.compile(
		"^Payment has been taken from your bank: (" + COINS + ") (?:x Coins|coins)\\.?$");
	private static final String RETRIEVED = "You successfully retrieved everything from your gravestone.";
	private static final int BANK_CONFIRMATION_TICKS = 2;
	private static final int GRAVE_CONFIRMATION_TICKS = 5;
	private static final int RECLAIM_INTERFACE_GRACE_TICKS = 2;

	private final LongConsumer feeConsumer;
	private final List<BankPayment> pendingBankPayments = new ArrayList<>();
	private int lastTick = Integer.MIN_VALUE;
	private int lastReclaimInterfaceTick = Integer.MIN_VALUE;

	PvmDeathFeeTracker(LongConsumer feeConsumer)
	{
		this.feeConsumer = Objects.requireNonNull(feeConsumer);
	}

	void onChatMessage(ChatMessageType type, String plainText, int tick, boolean reclaimInterfaceOpen)
	{
		// Private-instance entry payments use SPAM, including similar bank/coffer text.
		// Player chat and quoted NPC dialogue cannot confirm that a reclaim was charged.
		if (type != ChatMessageType.GAMEMESSAGE || plainText == null)
		{
			return;
		}
		advance(tick);
		if (reclaimInterfaceOpen)
		{
			lastReclaimInterfaceTick = tick;
		}
		String message = plainText.trim();
		Matcher charged = CHARGED.matcher(message);
		if (charged.matches())
		{
			long amount = parseCoins(charged.group(1));
			if (amount > 0L)
			{
				// The bank remainder is announced first in the same tick. This later
				// line is the whole charge, so replace that one pending part.
				boolean cofferCoveredFee = charged.group(2) != null && parseCoins(charged.group(2)) > 0L;
				for (int index = pendingBankPayments.size() - 1; !cofferCoveredFee && index >= 0; index--)
				{
					BankPayment bank = pendingBankPayments.get(index);
					if (bank.tick == tick && bank.amount <= amount)
					{
						pendingBankPayments.remove(index);
						break;
					}
				}
				feeConsumer.accept(amount);
			}
			return;
		}

		Matcher bankPayment = BANK_PAYMENT.matcher(message);
		if (bankPayment.matches())
		{
			long amount = parseCoins(bankPayment.group(1));
			if (amount > 0L)
			{
				long sinceReclaim = age(tick, lastReclaimInterfaceTick);
				boolean recentReclaim = lastReclaimInterfaceTick != Integer.MIN_VALUE
					&& sinceReclaim >= 0 && sinceReclaim <= RECLAIM_INTERFACE_GRACE_TICKS;
				pendingBankPayments.add(new BankPayment(amount, tick, recentReclaim));
			}
			return;
		}

		if (RETRIEVED.equals(message))
		{
			for (int index = pendingBankPayments.size() - 1; index >= 0; index--)
			{
				BankPayment bank = pendingBankPayments.get(index);
				if (!bank.confirmed && age(tick, bank.tick) <= GRAVE_CONFIRMATION_TICKS)
				{
					bank.confirmed = true;
					break;
				}
			}
		}
	}

	void onGameTick(int tick)
	{
		advance(tick);
	}

	void onReclaimInterfaceSeen(int tick)
	{
		if (lastTick != Integer.MIN_VALUE && tick < lastTick)
		{
			reset();
		}
		lastReclaimInterfaceTick = tick;
	}

	void flushConfirmed()
	{
		List<Long> completed = new ArrayList<>();
		for (int index = 0; index < pendingBankPayments.size();)
		{
			BankPayment bank = pendingBankPayments.get(index);
			if (bank.confirmed)
			{
				completed.add(bank.amount);
				pendingBankPayments.remove(index);
			}
			else
			{
				index++;
			}
		}
		completed.forEach(feeConsumer::accept);
	}

	void reset()
	{
		pendingBankPayments.clear();
		lastTick = Integer.MIN_VALUE;
		lastReclaimInterfaceTick = Integer.MIN_VALUE;
	}

	private void advance(int tick)
	{
		if (lastTick != Integer.MIN_VALUE && tick < lastTick)
		{
			reset();
		}
		lastTick = tick;
		List<Long> completed = new ArrayList<>();
		for (int index = 0; index < pendingBankPayments.size();)
		{
			BankPayment bank = pendingBankPayments.get(index);
			long elapsed = age(tick, bank.tick);
			if (bank.confirmed && elapsed >= BANK_CONFIRMATION_TICKS)
			{
				completed.add(bank.amount);
				pendingBankPayments.remove(index);
			}
			else if (!bank.confirmed && elapsed > GRAVE_CONFIRMATION_TICKS)
			{
				pendingBankPayments.remove(index);
			}
			else
			{
				index++;
			}
		}
		// Finish removing state before notifying the plugin, which may reset us
		// while changing accounts or updating its persisted statistics.
		completed.forEach(feeConsumer::accept);
	}

	private static long age(int tick, int earlierTick)
	{
		return (long) tick - earlierTick;
	}

	private static long parseCoins(String value)
	{
		try
		{
			return Long.parseLong(value.replace(",", ""));
		}
		catch (NumberFormatException exception)
		{
			return 0L;
		}
	}

	private static final class BankPayment
	{
		private final long amount;
		private final int tick;
		private boolean confirmed;

		private BankPayment(long amount, int tick, boolean confirmed)
		{
			this.amount = amount;
			this.tick = tick;
			this.confirmed = confirmed;
		}
	}
}
