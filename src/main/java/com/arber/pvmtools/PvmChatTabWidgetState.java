package com.arber.pvmtools;

import net.runelite.api.widgets.Widget;

/** The exact native state of one widget instance, including dynamic children. */
final class PvmChatTabWidgetState
{
	private final Widget widget;
	private final String text;
	private final int fontId;
	private final int textColor;
	private final boolean textShadowed;
	private final int xTextAlignment;
	private final int yTextAlignment;
	private final int originalX;
	private final int originalY;
	private final int relativeX;
	private final int relativeY;
	private final int originalWidth;
	private final int originalHeight;
	private final int width;
	private final int height;

	PvmChatTabWidgetState(Widget widget)
	{
		this.widget = widget;
		text = widget.getText();
		fontId = widget.getFontId();
		textColor = widget.getTextColor();
		textShadowed = widget.getTextShadowed();
		xTextAlignment = widget.getXTextAlignment();
		yTextAlignment = widget.getYTextAlignment();
		originalX = widget.getOriginalX();
		originalY = widget.getOriginalY();
		relativeX = widget.getRelativeX();
		relativeY = widget.getRelativeY();
		originalWidth = widget.getOriginalWidth();
		originalHeight = widget.getOriginalHeight();
		width = widget.getWidth();
		height = widget.getHeight();
	}

	void restore()
	{
		widget.setText(text == null ? "" : text);
		widget.setFontId(fontId);
		widget.setTextColor(textColor);
		widget.setTextShadowed(textShadowed);
		widget.setXTextAlignment(xTextAlignment);
		widget.setYTextAlignment(yTextAlignment);
		widget.setOriginalX(originalX);
		widget.setOriginalY(originalY);
		widget.setOriginalWidth(originalWidth);
		widget.setOriginalHeight(originalHeight);
		widget.setRelativeX(relativeX);
		widget.setRelativeY(relativeY);
		widget.setWidth(width);
		widget.setHeight(height);
	}
}
