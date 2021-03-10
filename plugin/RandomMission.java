package plugin;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Random;

import scout.Path;
import scout.StateController;

public class RandomMission
{
	private static Random generator = new Random(System.currentTimeMillis());

	private static Font textFont = new Font("Arial", Font.PLAIN, 48);
//	private static Color transparentColor = new Color(255, 0, 0, 160);
	private static Color transparentColor = new Color(247, 160, 14, 200);
	private final static int DISPLAY_MESSAGE_DURATION = 12000;
	
	private static long sessionTime=0;
	private static long remainingMissionTime=600000;
	private static long startDeltaTime=0;
	private static int randomMissionNo=0;
	private static int noInitialIssues=0;
	private static boolean missionOngoing=true;
	
	public void startSession()
	{
		sessionTime=0;
		remainingMissionTime=600000;
		startDeltaTime=System.currentTimeMillis();

		missionOngoing=true;
		int noMissions=4;
		noInitialIssues=StateController.getStateTree().getIssues().size();
		if(noInitialIssues==0)
		{
			// No issues
			noMissions=3;
		}
		if(StateController.getStateTree().getPaths().size()==0)
		{
			// No paths to explore
			noMissions=2;
		}
		randomMissionNo = generator.nextInt(noMissions);
		displayRandomMission(randomMissionNo);
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
			remainingMissionTime-=lastDuration;
			startDeltaTime=currentTime;
		}

		// Display session duration
		long displayTime=0;
		if(remainingMissionTime>=0)
		{
			displayTime=remainingMissionTime;
		}
		else
		{
			displayTime=sessionTime;
		}

		int visibleY=StateController.getVisibleY();
		int visibleWidth=StateController.getVisibleWidth();
		int visibleHeight=StateController.getVisibleHeight();
		String text=getDurationTime(displayTime);
		drawText(g2, text, transparentColor, visibleWidth-30, visibleY+visibleHeight-10);
		
		if(missionOngoing && remainingMissionTime<0)
		{
			// Mission time is over
			missionOngoing=false;
			
			if(randomMissionNo==0)
			{
				int noIssues=StateController.getStateTree().getIssues().size();
				if(noIssues>noInitialIssues)
				{
					StateController.displayMessage("Mission Completed", DISPLAY_MESSAGE_DURATION);
				}
				else
				{
					StateController.displayMessage("Mission Failed", DISPLAY_MESSAGE_DURATION);
				}
			}
			else
			{
				StateController.displayMessage("Mission Completed", DISPLAY_MESSAGE_DURATION);
			}
		}
	}

	private void displayRandomMission(int missionNo)
	{
		switch(missionNo)
		{
		case 0:
			StateController.displayMessage("Mission: Find and report an issue in 10 minutes", DISPLAY_MESSAGE_DURATION);
			break;
		case 1:
			StateController.displayMessage("Mission: Explore new paths for 10 minutes", DISPLAY_MESSAGE_DURATION);
			break;
		case 2:
			StateController.displayMessage("Mission: Test existing paths for 10 minutes", DISPLAY_MESSAGE_DURATION);
			break;
		case 3:
			StateController.displayMessage("Mission: Check reported defects for 10 minutes", DISPLAY_MESSAGE_DURATION);
			break;
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
