// SPDX-FileCopyrightText: 2021 Michel Nass
//
// SPDX-License-Identifier: MIT

package plugin;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import scout.StateController;

public class DisplayMessage
{
	private static Font textFont = new Font("Arial", Font.PLAIN, 36);
	private static Color textBoxRectangleBackgroundColor = new Color(0, 0, 0, 160);
	private static Color textBoxRectangleForegroundColor = Color.white;

	public void paintCapture(Graphics g)
	{
		Graphics2D g2=(Graphics2D)g;
		g2.setFont(textFont);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		String message=StateController.getDisplayMessage();
		long untilTime=StateController.getDisplayMessageUntilTime();
		if(message!=null && message.length()>0 && untilTime>System.currentTimeMillis())
		{
			int visibleY=StateController.getVisibleY();
			int visibleWidth=StateController.getVisibleWidth();
			int visibleHeight=StateController.getVisibleHeight();
			int centerX=visibleWidth/2;
			int centerY=visibleHeight/2+visibleY;
			drawText(g2, message, centerX, centerY);
		}
	}

	private void drawText(Graphics2D g2, String text, int centerX, int centerY)
	{
		FontMetrics fm = g2.getFontMetrics();
		int width = fm.stringWidth(text);
		int height = fm.getHeight();
		int startX=centerX-width/2;
		int startY=centerY+fm.getMaxAscent()-height/2;

		// Fill text background
		g2.setColor(textBoxRectangleBackgroundColor);
		g2.fillRect(startX-10, startY-fm.getMaxAscent()-10, width+20, height+20);

		// Draw text
		g2.setColor(textBoxRectangleForegroundColor);
		g2.drawString(text, startX, startY);
	}
}
