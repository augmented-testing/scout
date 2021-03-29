// SPDX-FileCopyrightText: 2021 Michel Nass
//
// SPDX-License-Identifier: MIT

package plugin;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.IdentityHashMap;
import java.util.List;

import scout.AppState;
import scout.StateController;
import scout.Widget;

public class StateGraph
{
	private static Font textBoxFont = new Font("Arial", Font.PLAIN, 14);
	private static Color textBoxRectangleBackgroundColor = new Color(0, 0, 0, 160);
	private static Color textBoxRectangleForegroundColor = Color.white;
	private static Color auraColor = new Color(255, 255, 255, 160);
	private static Color markedColor = new Color(0, 0, 255, 160);
	private static Color coveredColor = new Color(122, 182, 244, 200);
	private static Color issueColor = new Color(255, 0, 0, 160);
	private static BasicStroke narrowStroke=new BasicStroke(1);
	private static BasicStroke stroke=new BasicStroke(3);

	public void paintCapture(Graphics g)
	{
		if(StateController.isOngoingSession())
		{
			Graphics2D g2=(Graphics2D)g;
			g2.setFont(textBoxFont);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int visibleY=StateController.getVisibleY();
			int visibleWidth=StateController.getVisibleWidth();
			
			drawGraph(g2, StateController.getStateTree(), visibleWidth/2, visibleY+100);
		}
	}
	
	private int drawGraph(Graphics2D g2, AppState state, int x, int y)
	{
		int dist=40;		
		int size=15;
		int halfSize=size/2+1;
		
		List<Widget> visibleWidgets=state.getVisibleActions();
		int noWidgets=visibleWidgets.size();
		int[] noLeaves=new int[noWidgets];
		int totalLeaves=0;
		int widgetNo=0;
		for(Widget widget:visibleWidgets)
		{
			AppState nextState=widget.getNextState();
			if(nextState!=null && !nextState.isHome())
			{
				int leaves=nextState.getLeafCount();
				noLeaves[widgetNo]=leaves;
				totalLeaves+=leaves;
				widgetNo++;
			}
		}

		widgetNo=0;
		int nextX=x-(dist*totalLeaves)/2;
		for(Widget widget:visibleWidgets)
		{
			AppState nextState=widget.getNextState();
			if(nextState!=null && !nextState.isHome())
			{
				int leaves=noLeaves[widgetNo];
				int totalDistX=leaves*dist;
				int x2=nextX+totalDistX/2;
				int y2=y+dist;
				int weight=(int)widget.getWeight();
				int strokeWidth=weight/25+1;
				BasicStroke stroke=new BasicStroke(strokeWidth);
				BasicStroke auraStroke=new BasicStroke(strokeWidth+2);
				drawLine(g2, x, y+halfSize, x2, y2-halfSize, markedColor, stroke, auraStroke);
				drawGraph(g2, nextState, x2, y2);
				nextX+=totalDistX;
				widgetNo++;
			}
		}

		Color color=coveredColor;
		boolean filled=state.containsProductVersion(StateController.getProductVersion());
		if(state==StateController.getCurrentState())
		{
			color=markedColor;
			filled=true;
		}
		else if(state.containIssues())
		{
			color=issueColor;
			filled=true;
		}
		drawCircle(g2, x, y, size, size, color, filled);
		
		String bookmark=state.getBookmark();
		if(bookmark!=null && bookmark.trim().length()>0)
		{
			drawText(g2, bookmark, x+20, y);
		}

		return 1;
	}

	private void drawCircle(Graphics2D g2, int x, int y, int width, int height, Color color, boolean filled)
	{
		drawCircle(g2, x, y, width, height, color, narrowStroke, stroke, filled);
	}

	private void drawCircle(Graphics2D g2, int x, int y, int width, int height, Color color, BasicStroke stroke, BasicStroke auraStroke, boolean filled)
	{
		int margin=1;

		// Draw aura
		g2.setStroke(auraStroke);
		g2.setColor(auraColor);
		g2.drawOval(x-width/2-margin, y-height/2-margin, width+margin*2, height+margin*2);

		// Draw rectangle
		g2.setColor(color);
		g2.setStroke(stroke);
		if(filled)
		{
			g2.fillOval(x-width/2, y-height/2, width, height);
		}
		else
		{
			g2.drawOval(x-width/2, y-height/2, width, height);
		}
	}

	private void drawLine(Graphics2D g2, int x, int y, int x2, int y2, Color color, BasicStroke stroke, BasicStroke auraStroke)
	{
		// Draw aura
		g2.setStroke(auraStroke);
		g2.setColor(auraColor);
		g2.drawLine(x, y, x2, y2);

		// Draw rectangle
		g2.setColor(color);
		g2.setStroke(stroke);
		g2.drawLine(x, y, x2, y2);
	}

	private void drawText(Graphics2D g2, String text, int startX, int centerY)
	{
		FontMetrics fm = g2.getFontMetrics();
		int width = fm.stringWidth(text);
		int height = fm.getHeight();
		int startY=centerY+fm.getMaxAscent()-height/2;
		int margin=2;

		// Fill text background
		g2.setColor(textBoxRectangleBackgroundColor);
		g2.fillRect(startX-margin, startY-fm.getMaxAscent()-margin, width+margin*2, height+margin*2);

		// Draw text
		g2.setColor(textBoxRectangleForegroundColor);
		g2.drawString(text, startX, startY);
	}
}
