package com.arber.pvmtools;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.widgets.Widget;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Mockito.mock;

public class PvmChatTabWidgetStateTest
{
	@Test
	public void restoresEveryTextAndGeometryPropertyAfterTrackerMovesTheRows()
	{
		WidgetFixture widget = new WidgetFixture(17, "<col=ffffff>Private</col><br> ", 7);
		Map<String, Object> nativeValues = new HashMap<>(widget.values);
		PvmChatTabWidgetState state = new PvmChatTabWidgetState(widget.widget);
		widget.applyTracker("458K", 0);
		state.restore();
		assertEquals(nativeValues, widget.values);

		// Disabling twice must not reintroduce the tracker height or offsets.
		state.restore();
		assertEquals(nativeValues, widget.values);
	}

	@Test
	public void siblingsSharingTheSamePackedIdRestoreTheirOwnNativeState()
	{
		WidgetFixture title = new WidgetFixture(17, "Private<br> ", 3);
		WidgetFixture filter = new WidgetFixture(17, "Friends", 31);
		Map<String, Object> nativeTitle = new HashMap<>(title.values);
		Map<String, Object> nativeFilter = new HashMap<>(filter.values);
		assertEquals(title.widget.getId(), filter.widget.getId());
		assertNotSame(title.widget, filter.widget);
		PvmChatTabWidgetState titleState = new PvmChatTabWidgetState(title.widget);
		PvmChatTabWidgetState filterState = new PvmChatTabWidgetState(filter.widget);
		title.applyTracker("6.78M", 0);
		filter.applyTracker("Slayer", 10);

		filterState.restore();
		titleState.restore();
		assertEquals(nativeTitle, title.values);
		assertEquals(nativeFilter, filter.values);
	}

	@Test
	public void fallbackWritingIntoAnEmptyWidgetRestoresTheEmptyNativeText()
	{
		for (String originalText : new String[]{"", null})
		{
			WidgetFixture widget = new WidgetFixture(17, originalText, 5);
			Map<String, Object> nativeValues = new HashMap<>(widget.values);
			nativeValues.put("Text", "");
			PvmChatTabWidgetState state = new PvmChatTabWidgetState(widget.widget);
			widget.applyTracker("37.6M<br>Ranged", 0);

			state.restore();
			assertEquals(nativeValues, widget.values);
		}
	}

	@Test
	public void detachedOriginalRestoresWithoutTouchingAReplacementWithTheSameId()
	{
		WidgetFixture oldWidget = new WidgetFixture(17, "Trade<br> ", 9);
		Map<String, Object> oldNative = new HashMap<>(oldWidget.values);
		PvmChatTabWidgetState state = new PvmChatTabWidgetState(oldWidget.widget);
		oldWidget.applyTracker("15:42:01", 0);
		// Chatbox reconstruction installs a fresh widget under the same packed ID.
		WidgetFixture replacement = new WidgetFixture(17, "Group<br> ", 47);
		Map<String, Object> replacementNative = new HashMap<>(replacement.values);

		state.restore();
		assertEquals(oldNative, oldWidget.values);
		assertEquals(replacementNative, replacement.values);
	}

	/** Stateful getters/setters model the mutable native widget, not Mockito call counts. */
	private static final class WidgetFixture
	{
		private final Map<String, Object> values = new HashMap<>();
		private final Widget widget;

		private WidgetFixture(int id, String text, int seed)
		{
			values.put("Id", id);
			values.put("Text", text);
			values.put("FontId", 495 + seed);
			values.put("TextColor", 0x123400 + seed);
			values.put("TextShadowed", false);
			values.put("XTextAlignment", 0);
			values.put("YTextAlignment", 2);
			values.put("OriginalX", seed);
			values.put("OriginalY", seed + 1);
			values.put("RelativeX", seed + 2);
			values.put("RelativeY", seed + 3);
			values.put("OriginalWidth", 56 + seed);
			values.put("OriginalHeight", 22 + seed);
			values.put("Width", 58 + seed);
			values.put("Height", 24 + seed);
			widget = mock(Widget.class, call ->
			{
				String method = call.getMethod().getName();
				if (method.startsWith("get") && values.containsKey(method.substring(3)))
				{
					return values.get(method.substring(3));
				}
				if (method.startsWith("set") && values.containsKey(method.substring(3)))
				{
					values.put(method.substring(3), call.getArgument(0));
					return call.getMethod().getReturnType() == Void.TYPE ? null : call.getMock();
				}
				return org.mockito.Mockito.RETURNS_DEFAULTS.answer(call);
			});
		}

		private void applyTracker(String text, int y)
		{
			widget.setText(text);
			widget.setFontId(494);
			widget.setTextColor(0xffd050);
			widget.setTextShadowed(true);
			widget.setXTextAlignment(1);
			widget.setYTextAlignment(1);
			widget.setOriginalX(0);
			widget.setOriginalY(y);
			widget.setRelativeX(0);
			widget.setRelativeY(y);
			widget.setOriginalWidth(56);
			widget.setOriginalHeight(14);
			widget.setWidth(56);
			widget.setHeight(14);
		}
	}
}
