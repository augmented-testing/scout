// SPDX-FileCopyrightText: 2021 Michel Nass
//
// SPDX-License-Identifier: MIT

package plugin;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import scout.Action;
import scout.LeftClickAction;
import scout.LongClickAction;
import scout.StateController;
import scout.StateController.Mode;

public class AnimateAction
{
	private static BasicStroke thinStroke=new BasicStroke(1);
	private static BasicStroke mediumStroke=new BasicStroke(3);
	private static Color transparentBlackColor = new Color(0, 0, 0, 160);
	private static Color transparentWhiteColor = new Color(255, 255, 255, 160);
	
	private static int currentRadius=50;
	private static int mouseScaledX=0;
	private static int mouseScaledY=0;

	public void performAction(Action action)
	{
		if(StateController.isRunningSession() && !action.isToolbarAction() && (action instanceof LeftClickAction || action instanceof LongClickAction) && StateController.getMode()==Mode.MANUAL)
		{
			mouseScaledX=StateController.getMouseScaledX();
			mouseScaledY=StateController.getMouseScaledY();
			currentRadius=2;
		}
	}

	public void paintCaptureForeground(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;

		if(StateController.isOngoingSession())
		{
			if(currentRadius<40)
			{
				drawOval(g2, mouseScaledX, mouseScaledY, currentRadius, transparentBlackColor, transparentWhiteColor);
				currentRadius+=2;
				// Trigger another screen refresh
				StateController.setRefreshScreen(true);
			}
		}
	}

	private void drawOval(Graphics2D g2, int centerX, int centerY, int radius, Color colorForeground, Color colorBackground)
	{
		int width=radius*2;
		int height=radius*2;

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setColor(colorBackground);
		g2.setStroke(mediumStroke);
		g2.drawOval(centerX-radius, centerY-radius, width, height);
		g2.setColor(colorForeground);
		g2.setStroke(thinStroke);
		g2.drawOval(centerX-radius, centerY-radius, width, height);
	}
}
