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
import java.awt.geom.Arc2D;
import java.util.List;

import scout.StateController;
import scout.Widget;
import scout.Widget.WidgetSubtype;
import scout.Widget.WidgetType;
import scout.Widget.WidgetVisibility;

public class AugmentCoverage
{
	private static Font textFont = new Font("Arial", Font.PLAIN, 24);
	private static BasicStroke mediumStroke=new BasicStroke(4);
	private static BasicStroke largeStroke=new BasicStroke(6);
	private static Color transparentColor = new Color(247, 160, 14, 200);
	private static Color transparentStateColor = new Color(122, 182, 244, 200);
	private static Color transparentPageColor = new Color(121, 221, 107, 200);
	
	/**
	 * Called by the PluginController when time to draw overlay graphics
	 * @param g
	 */
	public void paintCaptureForeground(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;

		if(StateController.isOngoingSession())
		{
			// Draw total state coverage

			int percentCovered = StateController.getStateTree().coveredPercent(StateController.getProductVersion());
			if(percentCovered>0)
			{
				int visibleY=StateController.getVisibleY();
				int visibleHeight=StateController.getVisibleHeight();
				drawOval(g2, 50, visibleY+visibleHeight-50, 20, 20, transparentStateColor, percentCovered, false);
			}

			// Draw total page coverage
			percentCovered = StateController.getStateTree().coveredPagesPercent(StateController.getProductVersion());
			if(percentCovered>0)
			{
				int visibleY=StateController.getVisibleY();
				int visibleHeight=StateController.getVisibleHeight();
				drawOval(g2, 50, visibleY+visibleHeight-50, 15, 15, transparentPageColor, percentCovered, false);
			}

			if(!StateController.isToolbarVisible())
			{
				int mouseX=StateController.getMouseX();
				int mouseY=StateController.getMouseY();
				int mouseScaledX=StateController.getMouseScaledX();
				int mouseScaledY=StateController.getMouseScaledY();

				List<Widget> widgets=StateController.getWidgetsAt(StateController.getCurrentState(), mouseX, mouseY);
				if(widgets.size()>0)
				{
					// Draw coverage and arrows of widget hovering over
					Widget widget=widgets.get(0);
					boolean severalOptions=widgets.size()>1;
					if(widget.getWidgetType()==WidgetType.ACTION)
					{
						if(widget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
						{
							percentCovered = widget.coveredPercent(StateController.getProductVersion(), true);
							if(percentCovered>0)
							{
								drawOval(g2, mouseScaledX, mouseScaledY, 20, 20, transparentStateColor, percentCovered, false);
							}
						}
						if(severalOptions || widget.getWidgetSubtype()==WidgetSubtype.SELECT_ACTION)
				    {
				    	// Draw arrows
							drawArrows(g2, mouseScaledX, mouseScaledY, 10, 10, transparentColor);
				    }
					}
				}
			}
		}
	}

	private void drawOval(Graphics2D g2, int centerX, int centerY, int radiusX, int radiusY, Color color, int percentCovered, boolean displayText)
	{
		int width = radiusX * 2;
		int height = radiusY * 2;

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setColor(color);

		int pieDegree = (360 * percentCovered) / 100;
		if (pieDegree == 360)
		{
			g2.setStroke(largeStroke);
			g2.drawOval(centerX - radiusX*2, centerY - radiusY*2, width*2, height*2);
		}
		else if (pieDegree >= 0)
		{
			Arc2D.Float arc = new Arc2D.Float(Arc2D.PIE);
			arc.setFrame(centerX - radiusX * 2-5, centerY - radiusY * 2-5, width * 2+10, height * 2+10);
			arc.setAngleStart(90);
			arc.setAngleExtent(-pieDegree);
			g2.setStroke(largeStroke);
			g2.setClip(arc);
			g2.drawOval(centerX - radiusX*2, centerY - radiusY*2, width*2, height*2);
			g2.setClip(null);
		}
		if(displayText)
		{
			String text=""+percentCovered+"%";
			drawText(g2, text, centerX, centerY);
		}
	}

	private void drawArrows(Graphics2D g2, int centerX, int centerY, int radiusX, int radiusY, Color color)
	{
		// Draw up arrow
		g2.setColor(color);
		g2.setStroke(mediumStroke);
  	g2.drawLine(centerX - radiusX, centerY-5 - radiusY, centerX, centerY-5 - radiusY*2);
  	g2.drawLine(centerX + radiusX, centerY-5 - radiusY, centerX, centerY-5 - radiusY*2);

  	// Draw down arrow
  	g2.drawLine(centerX - radiusX, centerY+5 + radiusY, centerX, centerY+5 + radiusY*2);
  	g2.drawLine(centerX + radiusX, centerY+5 + radiusY, centerX, centerY+5 + radiusY*2);
	}

	private void drawText(Graphics2D g2, String text, int centerX, int centerY)
	{
		g2.setFont(textFont);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		FontMetrics fm = g2.getFontMetrics();
		int width = fm.stringWidth(text);
		int height = fm.getHeight();
		int startX=centerX-width/2;
		int startY=centerY+fm.getMaxAscent()-height/2;

		// Draw text
		g2.drawString(text, startX, startY);
	}
}
