package plugin;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import scout.AppState;
import scout.StateController;
import scout.Widget;
import scout.Widget.WidgetStatus;
import scout.Widget.WidgetType;
import scout.Widget.WidgetVisibility;

public class AugmentState
{
	private static Font textBoxFont = new Font("Arial", Font.PLAIN, 32);
	private static Color textBoxRectangleAuraColor = new Color(255, 255, 255, 160);
	private static Color transparentRedColor = new Color(255, 0, 0, 160);
	private static Color transparentBlueColor = new Color(0, 0, 255, 160);
	private static Color transparentGreenColor = new Color(0, 255, 0, 160);
	private static Color transparentYellowColor = new Color(255, 255, 0, 160);
	private static Color transparentPurpleColor = new Color(255, 0, 255, 160);
	private static Color transparentGreyColor = new Color(160, 160, 160, 160);
	private static Color transparentOrangeColor = new Color(255, 165, 0, 160);
	private static BasicStroke narrowStroke=new BasicStroke(1);
	private static BasicStroke stroke=new BasicStroke(3);
	private static BasicStroke auraStroke=new BasicStroke(5);
	private static BasicStroke recommendedStroke=new BasicStroke(7);
	private static BasicStroke recommendedAuraStroke=new BasicStroke(9);
	
	public void paintCapture(Graphics g)
	{
		AppState currentState=StateController.getCurrentState();
		Graphics2D g2=(Graphics2D)g;

		if(StateController.isOngoingSession() && currentState!=null)
		{
			List<Widget> allWidgets=currentState.getNonHiddenWidgets();
			allWidgets=StateController.sortWidgets(allWidgets);
			Widget recommendedWidget=StateController.findRecommendedWidget(currentState);
			StateController.setRecommendedWidget(recommendedWidget);
			Widget selectedWidget=StateController.getWidgetAt(StateController.getCurrentState(), StateController.getMouseX(), StateController.getMouseY());
			
			if(selectedWidget!=null && !StateController.isToolbarVisible())
			{
				if(selectedWidget.getWidgetVisibility()==WidgetVisibility.HIDDEN)
				{
					// Display a rectangle around the widget that the mouse hovers over
					Rectangle area=selectedWidget.getLocationArea();
					if(area!=null && (int)area.getWidth()*(int)area.getHeight()<=200000)
					{
						drawRectangle(g2, (int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight(), transparentGreyColor, false, narrowStroke, narrowStroke);
					}
				}
				else
				{
					Widget matchingWidget=(Widget)selectedWidget.getMetadata("matching_widget");
					if(matchingWidget!=null && selectedWidget.getWidgetStatus()==WidgetStatus.UNLOCATED)
					{
						Rectangle area=matchingWidget.getLocationArea();
						if(area!=null && (int)area.getWidth()*(int)area.getHeight()<=200000)
						{
							drawRectangle(g2, (int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight(), transparentOrangeColor, false);
						}
					}
				}
			}

			// Determine the widgets that should be visible
			List<Widget> displayedWidgets=new ArrayList<Widget>();
			for(Widget widget:allWidgets)
			{
				boolean displayWidget=true;
				Rectangle area=widget.getLocationArea();
				if(area!=null && area.contains(StateController.getMouseX(), StateController.getMouseY()))
				{
					// Mouse on widget - only display the selected widget
					if(widget!=selectedWidget)
					{
						// Not the selected one
						displayWidget=false;
					}
				}
				else if(StateController.isOverlapping(widget, recommendedWidget))
				{
					// Overlaps the recommended
					displayWidget=false;
				}
				if(isOverlapping(displayedWidgets, widget))
				{
					displayWidget=false;
				}
				if(displayWidget && widget.getWidgetVisibility()!=WidgetVisibility.HIDDEN)
				{
					displayedWidgets.add(widget);
				}
			}

			// Draw the visible widgets (in reverse order)
			for(int i=displayedWidgets.size()-1; i>=0; i--)
			{
				Widget widget=displayedWidgets.get(i);
				boolean isRecommended=(widget==recommendedWidget);
				Rectangle area=widget.getLocationArea();
				if(widget.getWidgetType()==WidgetType.ACTION)
				{
					if(widget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
					{
						// Show performed actions only
						if(widget.getWidgetStatus()==WidgetStatus.LOCATED)
						{
							drawRectangle(g2, (int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight(), transparentBlueColor, isRecommended);
						}
						else
						{
							drawRectangle(g2, (int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight(), transparentGreyColor, isRecommended);
						}
					}
					else if(widget.getWidgetVisibility()==WidgetVisibility.SUGGESTION)
					{
						if(widget.getWidgetStatus()==WidgetStatus.LOCATED)
						{
							drawRectangle(g2, (int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight(), transparentPurpleColor, isRecommended);
						}
						else
						{
							drawRectangle(g2, (int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight(), transparentGreyColor, isRecommended);
						}
					}
				}
				else if(widget.getWidgetType()==WidgetType.CHECK)
				{
					if(widget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
					{
						if(widget.getWidgetStatus()==WidgetStatus.VALID)
						{
							drawRectangle(g2, (int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight(), transparentGreenColor, isRecommended);
						}
						else if(widget.getWidgetStatus()==WidgetStatus.LOCATED)
						{
							drawRectangle(g2, (int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight(), transparentYellowColor, isRecommended);
						}
						else
						{
							drawRectangle(g2, (int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight(), transparentGreyColor, isRecommended);
						}
					}
					else if(widget.getWidgetVisibility()==WidgetVisibility.SUGGESTION)
					{
						if(widget.getWidgetStatus()==WidgetStatus.VALID)
						{
							drawRectangle(g2, (int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight(), transparentPurpleColor, isRecommended);
						}
						else if(widget.getWidgetStatus()==WidgetStatus.LOCATED)
						{
							drawRectangle(g2, (int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight(), transparentYellowColor, isRecommended);
						}
					}
				}
				else if(widget.getWidgetType()==WidgetType.ISSUE)
				{
					if(widget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
					{
						if(widget.getWidgetStatus()==WidgetStatus.VALID)
						{
							drawRectangle(g2, (int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight(), transparentRedColor, isRecommended);
						}
						else if(widget.getWidgetStatus()==WidgetStatus.LOCATED)
						{
							drawRectangle(g2, (int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight(), transparentYellowColor, isRecommended);
						}
						else
						{
							drawRectangle(g2, (int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight(), transparentGreyColor, isRecommended);
						}
					}
				}
			}
		}
	}

	private boolean isOverlapping(List<Widget> widgets, Widget widget)
	{
		for(Widget w:widgets)
		{
			if(StateController.isOverlapping(widget, w))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Draw a rounded rectangle
	 * @param g2
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param color
	 * @param recommended
	 */
	private void drawRectangle(Graphics2D g2, int x, int y, int width, int height, Color color, boolean recommended)
	{
		drawRectangle(g2, x, y, width, height, color, recommended, stroke, auraStroke);
	}
	
	private void drawRectangle(Graphics2D g2, int x, int y, int width, int height, Color color, boolean recommended, BasicStroke stroke, BasicStroke auraStroke)
	{
		g2.setFont(textBoxFont);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		x=StateController.getScaledX(x);
		y=StateController.getScaledY(y);
		width=StateController.getScaledX(width);
		height=StateController.getScaledY(height);
		
		int margin=3;

		// Draw white aura
		if(recommended)
		{
			g2.setStroke(recommendedAuraStroke);
		}
		else
		{
			g2.setStroke(auraStroke);
		}
		g2.setColor(textBoxRectangleAuraColor);
		g2.drawRoundRect(x-margin, y-margin, width+margin*2, height+margin*2, 10, 10);

		// Draw rectangle
		g2.setColor(color);
		g2.setStroke(stroke);
		g2.drawRoundRect(x-margin, y-margin, width+margin*2, height+margin*2, 10, 10);

		if(recommended)
		{
			// Draw recommended rectangle
			g2.setStroke(recommendedStroke);
			g2.drawRoundRect(x-margin, y-margin, width+margin*2, height+margin*2, 10, 10);
		}
	}
}
