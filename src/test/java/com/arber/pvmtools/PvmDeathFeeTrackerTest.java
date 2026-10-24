package com.arber.pvmtools;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.ChatMessageType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PvmDeathFeeTrackerTest
{
	@Test
	public void confirmedFullChargeCountsWithoutRequiringAnOpenWindow()
	{
		Fixture fixture = new Fixture();
		fixture.message("Death charges you 250,000 x Coins. You have 331,655 x Coins left in Death's Coffer.", 100, false);
		fixture.tracker.onGameTick(110);
		assertEquals(List.of(250_000L), fixture.payments);
	}

	@Test
	public void mixedBankAndCofferChargeCountsTheFullFeeOnce()
	{
		Fixture fixture = new Fixture();
		fixture.message("Payment has been taken from your bank: 180,096 x Coins", 100, true);
		assertTrue(fixture.payments.isEmpty());
		fixture.message("Death charges you 250,000 x Coins. You have nothing left in Death's Coffer.", 100, true);
		fixture.message("You successfully retrieved everything from your gravestone.", 100, false);
		fixture.tracker.onGameTick(110);
		assertEquals(List.of(250_000L), fixture.payments);
	}

	@Test
	public void bossReclaimBankOnlyWaitsForAnyWholeFeeMessageThenCommitsOnce()
	{
		Fixture fixture = new Fixture();
		fixture.message("Payment has been taken from your bank: 100,000 x Coins.", 100, true);
		fixture.tracker.onGameTick(101);
		assertTrue(fixture.payments.isEmpty());
		fixture.tracker.onGameTick(102);
		fixture.tracker.onGameTick(103);
		assertEquals(List.of(100_000L), fixture.payments);
	}

	@Test
	public void deathOfficeBankOnlyUsesItsDifferentCoinWording()
	{
		Fixture fixture = new Fixture();
		fixture.message("Payment has been taken from your bank: 5,964 coins", 100, true);
		fixture.tracker.onGameTick(102);
		assertEquals(List.of(5_964L), fixture.payments);
	}

	@Test
	public void bankPaymentAfterTheReclaimWindowClosesStillCountsWithinTwoTicks()
	{
		Fixture fixture = new Fixture();
		fixture.tracker.onReclaimInterfaceSeen(100);
		fixture.message("Payment has been taken from your bank: 100,000 x Coins", 102, false);
		fixture.tracker.onGameTick(104);
		assertEquals(List.of(100_000L), fixture.payments);
	}

	@Test
	public void oldReclaimWindowCannotTurnALaterBankPaymentIntoDeathFees()
	{
		Fixture fixture = new Fixture();
		fixture.tracker.onReclaimInterfaceSeen(100);
		fixture.message("Payment has been taken from your bank: 100,000 x Coins", 103, false);
		fixture.tracker.onGameTick(110);
		assertTrue(fixture.payments.isEmpty());
	}

	@Test
	public void graveWithoutWindowRequiresTheSuccessfulRetrievalConfirmation()
	{
		Fixture fixture = new Fixture();
		fixture.message("Payment has been taken from your bank: 25,000 x Coins", 100, false);
		fixture.tracker.onGameTick(102);
		assertTrue(fixture.payments.isEmpty());
		fixture.message("You successfully retrieved everything from your gravestone.", 104, false);
		fixture.tracker.onGameTick(105);
		assertEquals(List.of(25_000L), fixture.payments);
	}

	@Test
	public void unrelatedBankPaymentsAndLateGraveConfirmationAreDiscarded()
	{
		Fixture fixture = new Fixture();
		fixture.message("Payment has been taken from your bank: 25,000 x Coins", 100, false);
		fixture.message("You successfully retrieved everything from your gravestone.", 106, false);
		fixture.tracker.onGameTick(110);
		assertTrue(fixture.payments.isEmpty());
	}

	@Test
	public void noWindowMixedPaymentAlsoUsesTheWholeChargeWithoutCountingBankAgain()
	{
		Fixture fixture = new Fixture();
		fixture.message("Payment has been taken from your bank: 25,000 x Coins", 100, false);
		fixture.message("Death charges you 50,000 x Coins.", 100, false);
		fixture.message("You successfully retrieved everything from your gravestone.", 100, false);
		fixture.tracker.onGameTick(110);
		assertEquals(List.of(50_000L), fixture.payments);
	}

	@Test
	public void identicalRealPaymentsInTheSameTickAreBothRetained()
	{
		Fixture fixture = new Fixture();
		for (int payment = 0; payment < 2; payment++)
		{
			fixture.message("Payment has been taken from your bank: 25,000 x Coins", 100, true);
			fixture.message("Death charges you 50,000 x Coins.", 100, true);
		}
		fixture.tracker.onGameTick(110);
		assertEquals(List.of(50_000L, 50_000L), fixture.payments);
	}

	@Test
	public void multipleDeathOfficeItemPaymentsInTheSameTickAreNotCollapsed()
	{
		Fixture fixture = new Fixture();
		fixture.message("Payment has been taken from your bank: 1,000 coins", 100, true);
		fixture.message("Payment has been taken from your bank: 1,000 coins", 100, true);
		fixture.tracker.onGameTick(102);
		assertEquals(List.of(1_000L, 1_000L), fixture.payments);
	}

	@Test
	public void wholeChargeOnlyReplacesOneBankPartAndRetainsOtherTransactions()
	{
		Fixture fixture = new Fixture();
		fixture.message("Payment has been taken from your bank: 7,000 coins", 100, true);
		fixture.message("Payment has been taken from your bank: 25,000 x Coins", 100, true);
		fixture.message("Death charges you 50,000 x Coins.", 100, true);
		fixture.tracker.onGameTick(102);
		assertEquals(List.of(50_000L, 7_000L), fixture.payments);
	}

	@Test
	public void cofferOnlyFeeDoesNotConsumeAnIndependentBankPaymentInTheSameTick()
	{
		Fixture fixture = new Fixture();
		fixture.message("Payment has been taken from your bank: 7,000 coins", 100, true);
		fixture.message("Death charges you 50,000 x Coins. You have 100,000 x Coins left in Death's Coffer.", 100, true);
		fixture.tracker.onGameTick(102);
		assertEquals(List.of(50_000L, 7_000L), fixture.payments);
	}

	@Test
	public void privateInstanceSpamAndAllPlayerChatTypesAreIgnored()
	{
		Fixture fixture = new Fixture();
		for (ChatMessageType type : ChatMessageType.values())
		{
			if (type != ChatMessageType.GAMEMESSAGE)
			{
				fixture.tracker.onChatMessage(type, "Death charges you 500,000 x Coins.", 100, true);
				fixture.tracker.onChatMessage(type, "Payment has been taken from your bank: 100,000 x Coins", 100, true);
			}
		}
		fixture.tracker.onGameTick(110);
		assertTrue(fixture.payments.isEmpty());
	}

	@Test
	public void quotesCofferDepositsBalanceMessagesAndFreeReclaimsAreNotCharges()
	{
		Fixture fixture = new Fixture();
		String[] messages = {
			"Fee to unlock: 100,000 x Coins",
			"Fee: Paid",
			"Death's Coffer: 1,000,000 coins",
			"You have 331,655 x Coins left in Death's Coffer.",
			"You sacrifice items worth 100,000 coins to Death's Coffer.",
			"Pay 100,000 x Coins?",
			"\"Death charges you 100,000 x Coins.\"",
			"Death charges you 100,000 x Coins. This is only an estimate.",
			"Some of your payment has been taken from your bank.",
			"Payment has been taken from your coffer: 100,000 x Coins",
			"Death charges you 0 x Coins.",
			"Payment has been taken from your bank: 0 coins",
			"You successfully retrieved everything from your gravestone."
		};
		for (String message : messages)
		{
			fixture.message(message, 100, true);
		}
		fixture.tracker.onGameTick(110);
		assertTrue(fixture.payments.isEmpty());
	}

	@Test
	public void fullAmountsUseLongsAndMalformedOrOverflowingAmountsAreIgnored()
	{
		Fixture fixture = new Fixture();
		fixture.message("Death charges you 3,000,000,000 x Coins.", 100, false);
		fixture.message("Death charges you 9223372036854775807 x Coins.", 100, false);
		for (String amount : List.of("1,,000", "1,00", "-100", "1.5M", "9223372036854775808"))
		{
			fixture.message("Death charges you " + amount + " x Coins.", 100, false);
			fixture.message("Payment has been taken from your bank: " + amount + " coins", 100, true);
		}
		fixture.tracker.onGameTick(110);
		assertEquals(List.of(3_000_000_000L, Long.MAX_VALUE), fixture.payments);
	}

	@Test
	public void accountResetDropsPendingBankPaymentsAndRecentReclaimContext()
	{
		Fixture fixture = new Fixture();
		fixture.message("Payment has been taken from your bank: 100,000 coins", 100, true);
		fixture.tracker.reset();
		fixture.message("Payment has been taken from your bank: 25,000 coins", 101, false);
		fixture.tracker.onGameTick(110);
		assertTrue(fixture.payments.isEmpty());
	}

	@Test
	public void logoutFlushPreservesConfirmedPaymentsBeforeThePairingDelayExpires()
	{
		Fixture fixture = new Fixture();
		fixture.message("Payment has been taken from your bank: 25,000 coins", 100, false);
		fixture.message("Payment has been taken from your bank: 100,000 coins", 100, true);
		fixture.tracker.flushConfirmed();
		fixture.tracker.flushConfirmed();
		fixture.tracker.reset();
		fixture.tracker.onGameTick(110);
		assertEquals(List.of(100_000L), fixture.payments);
	}

	@Test
	public void tickRollbackCannotCommitPaymentsFromThePreviousWorld()
	{
		Fixture fixture = new Fixture();
		fixture.message("Payment has been taken from your bank: 100,000 coins", 100, true);
		fixture.tracker.onGameTick(0);
		fixture.tracker.onGameTick(110);
		assertTrue(fixture.payments.isEmpty());
	}

	private static final class Fixture
	{
		private final List<Long> payments = new ArrayList<>();
		private final PvmDeathFeeTracker tracker = new PvmDeathFeeTracker(payments::add);

		private void message(String message, int tick, boolean reclaimOpen)
		{
			tracker.onChatMessage(ChatMessageType.GAMEMESSAGE, message, tick, reclaimOpen);
		}
	}
}
