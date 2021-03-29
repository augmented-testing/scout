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

import scout.Path;
import scout.StateController;

public class SessionTime
{
	private static Font textFont = new Font("Arial", Font.PLAIN, 48);
//	private static Color transparentColor = new Color(255, 0, 0, 160);
	private static Color transparentColor = new Color(247, 160, 14, 200);

	private static long sessionTime=0;
	private static long startDeltaTime=0;
	
	public void startSession()
	{
		sessionTime=0;
		startDeltaTime=System.currentTimeMillis();
	}

	public void enablePlugin()
	{
		startSession();
	}

	public void stopSession()
	{
		Path currentPath=StateController.getCurrentPath();
		if(currentPath!=null)
		{
			currentPath.setSessionDuration(sessionTime);
		}
	}

	public void resumeSession()
	{
		startDeltaTime=System.currentTimeMillis();
	}

	public void paintCapture(Graphics g)
	{
		Graphics2D g2=(Graphics2D)g;
		if(StateController.isRunningSession())
		{
			long currentTime=System.currentTimeMillis();
			long lastDuration=currentTime-startDeltaTime;
			sessionTime+=lastDuration;
			startDeltaTime=currentTime;
		}

		if(sessionTime>0)
		{
			// Display session duration
			int visibleY=StateController.getVisibleY();
			int visibleWidth=StateController.getVisibleWidth();
			int visibleHeight=StateController.getVisibleHeight();
			String text=getDurationTime(sessionTime);
			drawText(g2, text, transparentColor, visibleWidth-30, visibleY+visibleHeight-10);
		}
	}

	/**
	 * Format the time
	 * @param duration
	 * @return A formatted time
	 */
	private String getDurationTime(long duration)
	{
		int seconds=(int)duration/1000;
		int minutes=(int)seconds/60;
		int remainingSeconds=(int)seconds%60;
		String secondsStr;
		if(remainingSeconds<10)
		{
			secondsStr="0"+remainingSeconds;
		}
		else
		{
			secondsStr=""+remainingSeconds;
		}
		return minutes+":"+secondsStr;
	}

	private void drawText(Graphics2D g2, String text, Color color, int textEndX, int textEndY)
	{
		g2.setFont(textFont);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setColor(color);
		FontMetrics fm = g2.getFontMetrics();
		int textWidth = fm.stringWidth(text);
		int textHeight = fm.getHeight();
		int textAscent = fm.getMaxAscent();
		g2.drawString(text, textEndX-textWidth, textEndY-textHeight+textAscent);
	}
}
