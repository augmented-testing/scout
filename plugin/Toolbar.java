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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import scout.Action;
import scout.AppState;
import scout.GoHomeAction;
import scout.JSortableButton;
import scout.LeftClickAction;
import scout.NavigateToDialog;
import scout.PluginController;
import scout.SelectProductDialog;
import scout.StartSessionDialog;
import scout.StateController;
import scout.StateController.Mode;
import scout.StateController.Route;
import scout.Widget.WidgetSubtype;
import scout.Widget.WidgetType;
import scout.Widget;

public class Toolbar
{
	private static Font textFont = new Font("Arial", Font.PLAIN, 12);
	private static Color transparentBarBackgroundColor = new Color(0, 0, 0, 200);
	private static Color buttonTextColor = Color.white;
	private static Color buttonBackgroundColor = new Color(0, 0, 0, 0);
	private static Color buttonHoverColor = new Color(0, 0, 0, 100);
	private static Color buttonSelectedColor = new Color(247, 160, 14);
	private static Color buttonSelectedHoverColor = new Color(220, 140, 8);
	private static Color buttonSelectedTextColor = Color.black;
	private static Color transparentHiddenMenuColor = new Color(247, 160, 14, 200);
	private static BasicStroke stroke=new BasicStroke(1);
	private static boolean showToolbar=false;
	private static String hoverButtonText=null;
	private static Rectangle menuRect=null;
	private static boolean isBookmarksMenuVisible=false;
	private static boolean isCookieMenuVisible=false;
	private static boolean isToolsMenuVisible=false;
	private static boolean isReportsMenuVisible=false;
	private static boolean isPluginsMenuVisible=false;
	private static long timeHideEmptyBar=0;

	public void paintCaptureToolbar(Graphics g)
	{
		Graphics2D g2=(Graphics2D)g;

		int marginY=5;
		int toolbarWidth=770;
		int toolbarHeight=50;
		int visibleY=StateController.getVisibleY();
		int visibleWidth=StateController.getVisibleWidth();
		int marginX=(visibleWidth-toolbarWidth)/2;
		int nextX=marginX+10;

		if(showToolbar || StateController.isStoppedSession())
		{
			StateController.setToolbarVisible(true);
			
			int y=StateController.getMouseScaledY();
			
			fillRectangle(g2, marginX, visibleY+marginY, toolbarWidth, toolbarHeight, transparentBarBackgroundColor, 10);

			hoverButtonText=null;
			
			if(StateController.isStoppedSession())
			{
				nextX+=drawTextRectangle(g2, "Start", nextX, visibleY+marginY+10, 70, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
			}
			else if(StateController.isRunningSession())
			{
				nextX+=drawTextRectangle(g2, "Stop", nextX, visibleY+marginY+10, 70, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
			}
			else
			{
				nextX+=75;
			}
			nextX+=5;
			if(StateController.getRoute()==Route.EXPLORING)
			{
				nextX+=drawTextRectangle(g2, "Explore", nextX, visibleY+marginY+10, 70, 30, buttonSelectedColor, buttonSelectedColor, buttonSelectedColor, buttonSelectedTextColor);
				nextX-=5;
				nextX+=drawTextRectangle(g2, "Navigate", nextX, visibleY+marginY+10, 70, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
			}
			else
			{
				nextX+=drawTextRectangle(g2, "Explore", nextX, visibleY+marginY+10, 70, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
				nextX-=5;
				nextX+=drawTextRectangle(g2, "Navigate", nextX, visibleY+marginY+10, 70, 30, buttonSelectedColor, buttonSelectedColor, buttonSelectedColor, buttonSelectedTextColor);
			}
			nextX+=5;
			if(StateController.getMode()==Mode.MANUAL)
			{
				nextX+=drawTextRectangle(g2, "Manual", nextX, visibleY+marginY+10, 70, 30, buttonSelectedColor, buttonSelectedColor, buttonSelectedColor, buttonSelectedTextColor);
				nextX-=5;
				nextX+=drawTextRectangle(g2, "Auto", nextX, visibleY+marginY+10, 70, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
			}
			else
			{
				nextX+=drawTextRectangle(g2, "Manual", nextX, visibleY+marginY+10, 70, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
				nextX-=5;
				nextX+=drawTextRectangle(g2, "Auto", nextX, visibleY+marginY+10, 70, 30, buttonSelectedColor, buttonSelectedColor, buttonSelectedColor, buttonSelectedTextColor);
			}
			nextX+=5;
/*
			if(StateController.isRunningSession())
			{
				nextX+=drawTextRectangle(g2, "Reset", nextX, visibleY+marginY+10, 70, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
			}
			else
			{
				nextX+=75;
			}
*/
			int bookmarskMenuX=nextX;
			if(StateController.isRunningSession())
			{
				nextX+=drawTextRectangle(g2, "Bookmarks", nextX, visibleY+marginY+10, 70, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
			}
			else
			{
				nextX+=75;
			}
			int cookieskMenuX=nextX;
			if(StateController.isRunningSession())
			{
				nextX+=drawTextRectangle(g2, "Cookies", nextX, visibleY+marginY+10, 70, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
			}
			else
			{
				nextX+=75;
			}
			int toolsMenuX=nextX;
			nextX+=drawTextRectangle(g2, "Tools", nextX, visibleY+marginY+10, 70, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
			int reportsMenuX=nextX;
			nextX+=drawTextRectangle(g2, "Reports", nextX, visibleY+marginY+10, 70, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
			int pluginsMenuX=nextX;
			nextX+=drawTextRectangle(g2, "Plugins", nextX, visibleY+marginY+10, 70, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);

			if("Bookmarks".equals(hoverButtonText))
			{
				isBookmarksMenuVisible=true;
			}
			else
			{
				if(isBookmarksMenuVisible && menuRect!=null)
				{
					int xc=StateController.getMouseScaledX();
					int yc=StateController.getMouseScaledY();
					if(!menuRect.contains(xc, yc))
					{
						isBookmarksMenuVisible=false;
					}
				}
			}

			if("Cookies".equals(hoverButtonText))
			{
				isCookieMenuVisible=true;
			}
			else
			{
				if(isCookieMenuVisible && menuRect!=null)
				{
					int xc=StateController.getMouseScaledX();
					int yc=StateController.getMouseScaledY();
					if(!menuRect.contains(xc, yc))
					{
						isCookieMenuVisible=false;
					}
				}
			}

			if("Tools".equals(hoverButtonText))
			{
				isToolsMenuVisible=true;
			}
			else
			{
				if(isToolsMenuVisible && menuRect!=null)
				{
					int xc=StateController.getMouseScaledX();
					int yc=StateController.getMouseScaledY();
					if(!menuRect.contains(xc, yc))
					{
						isToolsMenuVisible=false;
					}
				}
			}

			if("Reports".equals(hoverButtonText))
			{
				isReportsMenuVisible=true;
			}
			else
			{
				if(isReportsMenuVisible && menuRect!=null)
				{
					int xc=StateController.getMouseScaledX();
					int yc=StateController.getMouseScaledY();
					if(!menuRect.contains(xc, yc))
					{
						isReportsMenuVisible=false;
					}
				}
			}

			if("Plugins".equals(hoverButtonText))
			{
				isPluginsMenuVisible=true;
			}
			else
			{
				if(isPluginsMenuVisible && menuRect!=null)
				{
					int xc=StateController.getMouseScaledX();
					int yc=StateController.getMouseScaledY();
					if(!menuRect.contains(xc, yc))
					{
						isPluginsMenuVisible=false;
					}
				}
			}

			if(isBookmarksMenuVisible)
			{
				List<String> bookmarks=StateController.getStateTree().getBookmarks();
				int menuItemWidth=200;
				int menuWidth=menuItemWidth+20;
				int menuHeight=15+(3+bookmarks.size())*35;
				int menuX=bookmarskMenuX+70-menuWidth+10;
				int menuY=visibleY+marginY+20+35;
				menuRect=new Rectangle(menuX, menuY-15, menuWidth, menuHeight+15);
				fillRectangle(g2, menuX, menuY, menuWidth, menuHeight, transparentBarBackgroundColor, 10);
				int menuItemY=menuY+10;
				drawTextRectangle(g2, "Go Home", menuX+10, menuItemY, menuItemWidth, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
				for(String bookmark:bookmarks)
				{
					menuItemY+=35;
					drawTextRectangle(g2, "Is at "+bookmark, menuX+10, menuItemY, menuItemWidth, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
				}
				menuItemY+=35;
				drawTextRectangle(g2, "Add Bookmark", menuX+10, menuItemY, menuItemWidth, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
				menuItemY+=35;
				drawTextRectangle(g2, "Remove Bookmark", menuX+10, menuItemY, menuItemWidth, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
			}
			else if(isCookieMenuVisible)
			{
				int menuItemWidth=200;
				int menuWidth=menuItemWidth+20;
				int menuHeight=15+4*35;
				int menuX=cookieskMenuX+70-menuWidth+10;
				int menuY=visibleY+marginY+20+35;
				menuRect=new Rectangle(menuX, menuY-15, menuWidth, menuHeight+15);
				fillRectangle(g2, menuX, menuY, menuWidth, menuHeight, transparentBarBackgroundColor, 10);
				int menuItemY=menuY+10;
				drawTextRectangle(g2, "Accept Cookies", menuX+10, menuItemY, menuItemWidth, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
				menuItemY+=35;
				drawTextRectangle(g2, "Store Cookies", menuX+10, menuItemY, menuItemWidth, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
				menuItemY+=35;
				drawTextRectangle(g2, "Remove Cookies", menuX+10, menuItemY, menuItemWidth, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
				menuItemY+=35;
				drawTextRectangle(g2, "Remove All Cookies", menuX+10, menuItemY, menuItemWidth, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
			}
			else if(isToolsMenuVisible)
			{
				List<String> tools=PluginController.getTools();
				int menuItemWidth=200;
				int menuWidth=menuItemWidth+20;
				int menuHeight=15+tools.size()*35;
				int menuX=toolsMenuX+70-menuWidth+10;
				int menuY=visibleY+marginY+20+35;
				menuRect=new Rectangle(menuX, menuY-15, menuWidth, menuHeight+15);
				fillRectangle(g2, menuX, menuY, menuWidth, menuHeight, transparentBarBackgroundColor, 10);
				int menuItemY=menuY+10;
				for(String tool:tools)
				{
					String name=camelToText(tool.substring(7));
					drawTextRectangle(g2, name, menuX+10, menuItemY, menuItemWidth, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
					menuItemY+=35;
				}
			}
			else if(isReportsMenuVisible)
			{
				List<String> reports=PluginController.getReports();
				int menuItemWidth=200;
				int menuWidth=menuItemWidth+20;
				int menuHeight=15+reports.size()*35;
				int menuX=reportsMenuX+70-menuWidth+10;
				int menuY=visibleY+marginY+20+35;
				menuRect=new Rectangle(menuX, menuY-15, menuWidth, menuHeight+15);
				fillRectangle(g2, menuX, menuY, menuWidth, menuHeight, transparentBarBackgroundColor, 10);
				int menuItemY=menuY+10;
				for(String report:reports)
				{
					String name=camelToText(report.substring(7));
					drawTextRectangle(g2, name, menuX+10, menuItemY, menuItemWidth, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
					menuItemY+=35;
				}
			}
			else if(isPluginsMenuVisible)
			{
				List<String> plugins=PluginController.getAllClasses();
				int half=(plugins.size()-1)/2+(plugins.size()-1)%2;
				int menuItemWidth=200;
				int menuWidth=menuItemWidth*2+20+15;
				int menuHeight=15+half*35;
				int menuX=pluginsMenuX+70-menuWidth+10;
				int menuY=visibleY+marginY+20+35;
				menuRect=new Rectangle(menuX, menuY-15, menuWidth, menuHeight+15);
				fillRectangle(g2, menuX, menuY, menuWidth, menuHeight, transparentBarBackgroundColor, 10);
				int menuItemY=menuY+10;
				boolean left=true;
				for(String plugin:plugins)
				{
					String name=camelToText(plugin.substring(7));
					if(!"Toolbar".equals(name))
					{
						boolean enabled=PluginController.isPluginEnabled(plugin);
						int columnMargin=left?0:menuItemWidth+15;
						if(enabled)
						{
							drawTextRectangle(g2, name, menuX+columnMargin+10, menuItemY, menuItemWidth, 30, buttonSelectedColor, buttonSelectedHoverColor, buttonSelectedColor, buttonSelectedTextColor);
						}
						else
						{
							drawTextRectangle(g2, name, menuX+columnMargin+10, menuItemY, menuItemWidth, 30, buttonBackgroundColor, buttonHoverColor, buttonBackgroundColor, buttonTextColor);
						}
						if(!left)
						{
							menuItemY+=35;
						}
						left=!left;
					}
				}
			}
			else
			{
				menuRect=null;
			}

			int x=StateController.getMouseScaledX();
			y=StateController.getMouseScaledY()-visibleY;
			if(y>marginY+50 && menuRect==null || (x<marginX || x>marginX+toolbarWidth))
			{
				showToolbar=false;
				timeHideEmptyBar=System.currentTimeMillis()+5000;
			}
		}
		else
		{
			StateController.setToolbarVisible(false);

			if(timeHideEmptyBar>System.currentTimeMillis())
			{
				fillRectangle(g2, marginX, visibleY+marginY, visibleWidth-marginX*2, 5, transparentHiddenMenuColor, 3);
			}
			
			int x=StateController.getMouseScaledX();
			int y=StateController.getMouseScaledY()-visibleY;
			if(y<=15 && x>=marginX && x<=marginX+toolbarWidth)
			{
				showToolbar=true;
			}
		}
	}

	public void performAction(Action action)
	{
		if(!action.isToolbarAction())
		{
			// Not a toolbar action
			return;
		}

		if(action instanceof LeftClickAction)
		{
			if("Autopilot".equalsIgnoreCase(action.getCreatedByPlugin()))
			{
				// Do not perform auto generated actions
				return;
			}
			if(hoverButtonText!=null)
			{
				// Left mouse click on a button
				if("Start".equals(hoverButtonText) && StateController.isStoppedSession())
				{
					startSession();
				}
				if("Stop".equals(hoverButtonText))
				{
					StateController.setMode(Mode.MANUAL);
					if("yes".equalsIgnoreCase(StateController.getProductProperty("go_home_on_stop", "no")))
					{
						goHome();
					}
					StateController.stopSession();
				}
				if("Go Home".equals(hoverButtonText))
				{
					isBookmarksMenuVisible=false;
					StateController.setMode(Mode.MANUAL);
					if(StateController.getCurrentState().isHome())
					{
						StateController.displayMessage("Is already at Home");
					}
					else if(!StateController.isAllowedToInsert())
					{
						StateController.displayMessage("Not allowed to insert more actions");
					}
					else
					{
						goHome();
					}
				}
				if("Pause".equals(hoverButtonText))
				{
					StateController.setMode(Mode.MANUAL);
					StateController.pauseSession();
				}
				if("Resume".equals(hoverButtonText))
				{
					StateController.resumeSession();
				}
				if("Explore".equals(hoverButtonText) && StateController.getRoute()!=Route.EXPLORING)
				{
					StateController.setRoute(Route.EXPLORING);
				}
				if("Navigate".equals(hoverButtonText) && StateController.getRoute()!=Route.NAVIGATING)
				{
					selectNavigationTarget();
				}
				if("Manual".equals(hoverButtonText) && StateController.getMode()!=Mode.MANUAL)
				{
					StateController.setMode(Mode.MANUAL);
				}
				if("Auto".equals(hoverButtonText) && StateController.getMode()!=Mode.AUTO)
				{
					int percentCovered=StateController.getStateTree().coveredPercent(StateController.getProductVersion());
					if(percentCovered==100)
					{
						StateController.resetCoverage();
					}
					StateController.setMode(Mode.AUTO);
				}
				if("Reset".equals(hoverButtonText))
				{
					StateController.resetCoverage();
				}
				if("Add Bookmark".equals(hoverButtonText))
				{
					isBookmarksMenuVisible=false;
					String text = JOptionPane.showInputDialog(StateController.getParentFrame(), "Enter bookmark name");
					if (text != null && text.length()>0)
					{
						StateController.getCurrentState().setBookmark(text);
					}
				}
				if("Remove Bookmark".equals(hoverButtonText))
				{
					isBookmarksMenuVisible=false;
					StateController.getCurrentState().setBookmark(null);
					StateController.displayMessage("Bookmark has been removed");
				}
				if("Accept Cookies".equals(hoverButtonText))
				{
					isBookmarksMenuVisible=false;
					StateController.displayMessage("Click to accept cookies", 10000);
					StateController.setDoNotRecordNextAction(true);
				}
				if("Store Cookies".equals(hoverButtonText))
				{
					isBookmarksMenuVisible=false;
					PluginController.storeHomeState();
					StateController.displayMessage("Cookies have been stored");
				}
				if("Remove Cookies".equals(hoverButtonText))
				{
					isBookmarksMenuVisible=false;
					StateController.getCurrentState().removeMetadata("cookies");
					StateController.displayMessage("Cookies have been removed");
				}
				if("Remove All Cookies".equals(hoverButtonText))
				{
					isBookmarksMenuVisible=false;
					StateController.getCurrentState().removeMetadata("cookies");
					List<AppState> states=StateController.getStateTree().getVisibleStates(null);
					for(AppState state:states)
					{
						state.removeMetadata("cookies");
					}
					StateController.displayMessage("All cookies have been removed");
				}
				if(hoverButtonText.startsWith("Is at "))
				{
					isBookmarksMenuVisible=false;
					String bookmarkName=hoverButtonText.substring(6);
					StateController.markBookmark(bookmarkName);
				}
				if(isToolsMenuVisible)
				{
					List<String> tools=PluginController.getTools();
					String hoverItem=removeSpaces(hoverButtonText);
					for(String tool:tools)
					{
						String name=tool.substring(7);
						if(name.equals(hoverItem))
						{
							// Found a tool to start
							PluginController.startTool(tool);
							isToolsMenuVisible=false;
						}
					}
				}
				if(isReportsMenuVisible)
				{
					List<String> reports=PluginController.getReports();
					String hoverItem=removeSpaces(hoverButtonText);
					for(String report:reports)
					{
						String name=report.substring(7);
						if(name.equals(hoverItem))
						{
							// Found a report to generate
							PluginController.generateReport(report);
							isReportsMenuVisible=false;
						}
					}
				}
				if(isPluginsMenuVisible)
				{
					List<String> plugins=PluginController.getAllClasses();
					String hoverItem=removeSpaces(hoverButtonText);
					for(String plugin:plugins)
					{
						String name=plugin.substring(7);
						if(name.equals(hoverItem))
						{
							// Found a plugin to enable/disable
							boolean enabled=PluginController.isPluginEnabled(plugin);
							PluginController.setPluginEnabled(plugin, !enabled);
						}
					}
				}
			}
		}
	}

	/**
	 * Draw a rounded rectangle
	 * @param g2
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param color
	 */
	private void fillRectangle(Graphics2D g2, int x, int y, int width, int height, Color color, int arc)
	{
		// Draw rectangle
		g2.setColor(color);
		g2.setStroke(stroke);
		g2.fillRoundRect(x, y, width, height, arc, arc);
	}

	private int drawTextRectangle(Graphics2D g2, String text, int x, int y, int width, int height, Color backgroundColor, Color hoverBackgroundColor, Color borderColor, Color textColor)
	{
		g2.setFont(textFont);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int centerX=x+width/2;
		int centerY=y+height/2;
		
		FontMetrics fm = g2.getFontMetrics();
		int textWidth = fm.stringWidth(text);
		int textHeight = fm.getHeight();
		int startX=centerX-textWidth/2;
		int startY=centerY-textHeight/2+fm.getMaxAscent();

		int xc=StateController.getMouseScaledX();
		int yc=StateController.getMouseScaledY();
		
		boolean hover=xc>=x && xc<x+width && yc>=y && yc<y+height;
		
		// Set background color
		if(hover)
		{
			// Cursor hovers over button
			g2.setColor(hoverBackgroundColor);
		}
		else
		{
			g2.setColor(backgroundColor);
		}

		// Draw background
		g2.setStroke(stroke);
		g2.fillRoundRect(x, y, width, height, 5, 5);

		if(hover)
		{
			// Cursor hovers over button
			hoverButtonText=text;
			g2.setColor(textColor);
			g2.drawRoundRect(x, y, width, height, 5, 5);
		}

		// Draw text
		g2.setColor(textColor);
		g2.drawString(text, startX, startY);
		
		return width+5;
	}
	
	private void startSession()
	{
		SelectProductDialog selectProductDialog = new SelectProductDialog(StateController.getParentFrame());
		if(selectProductDialog.showDialog())
		{
			if(selectProductDialog.isCanceled())
			{
				return;
			}
			StateController.setProduct(selectProductDialog.getProduct());
		}
		StartSessionDialog dialog = new StartSessionDialog(StateController.getParentFrame());
		if(dialog.showDialog())
		{
			if(dialog.isCanceled())
			{
				return;
			}
			StateController.startSession(selectProductDialog.getProduct(), dialog.getProductVersion(), dialog.getTesterName(), dialog.getProductView(), dialog.getHomeLocator(), dialog.getProductWiewWidth(), 
					dialog.getProductWiewHeight(), dialog.isHeadlessBrowser());
			timeHideEmptyBar=System.currentTimeMillis()+30000;
		}
	}

	private boolean selectNavigationTarget()
	{
		List<JSortableButton> itemList=new ArrayList<JSortableButton>();
		NavigateToDialog dialog = new NavigateToDialog(StateController.getParentFrame(), itemList);

		List<Widget> issues=StateController.getStateTree().getAllIssues();
		ImageIcon issueIcon=new ImageIcon("icons/bug.png");
		for(Widget issue:issues)
		{
			JSortableButton sortableButton=new JSortableButton(issue.getReportedText(), issue.getId());
			sortableButton.setIcon(issueIcon);
			sortableButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent ev)
				{
					String targetWidgetId=sortableButton.getId();
					AppState targetState=StateController.getStateTree().findStateFromWidgetId(targetWidgetId);
					if(targetState!=null)
					{
						StateController.setNavigationTargetState(targetState);
						StateController.setRoute(Route.NAVIGATING);
					}
					dialog.dispose();
				}
			});
			itemList.add(sortableButton);
		}

		List<String> bookmarks=StateController.getStateTree().getBookmarks();
		ImageIcon bookmarkIcon=new ImageIcon("icons/book.png");
		for(String bookmark:bookmarks)
		{
			JSortableButton sortableButton=new JSortableButton(bookmark, bookmark);
			sortableButton.setIcon(bookmarkIcon);
			sortableButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent ev)
				{
					String targetBookmark=sortableButton.getId();
					AppState targetState=StateController.getStateTree().findStateFromBookmark(targetBookmark);
					if(targetState!=null)
					{
						StateController.setNavigationTargetState(targetState);
						StateController.setRoute(Route.NAVIGATING);
					}
					dialog.dispose();
				}
			});
			itemList.add(sortableButton);
		}

		dialog.updateList();
		dialog.showDialog();

		return false;
	}
	
	private String camelToText(String camel)
	{
		StringBuffer buf=new StringBuffer();
		for(int i=0; i<camel.length(); i++)
		{
			char c=camel.charAt(i);
			if(i>0)
			{
				char prevChar=camel.charAt(i-1);
				if(prevChar!=' ' && Character.isUpperCase(c))
				{
					buf.append(' ');
				}
			}
			buf.append(c);
		}
		return buf.toString();
	}
	
	private String removeSpaces(String text)
	{
		StringBuffer buf=new StringBuffer();
		for(int i=0; i<text.length(); i++)
		{
			char c=text.charAt(i);
			if(c!=' ')
			{
				buf.append(c);
			}
		}
		return buf.toString();
	}
	
	private void goHome()
	{
		if(!StateController.getCurrentState().isHome())
		{
			Widget goHome=getHomeWidget();
			if(goHome!=null)
			{
				LeftClickAction action=new LeftClickAction();
				Point p=new Point((int)goHome.getLocationArea().getCenterX(), (int)goHome.getLocationArea().getCenterY());
				action.setLocation(p);
				PluginController.performAction(action);
			}
			else
			{
				PluginController.performAction(new GoHomeAction());
			}
		}
	}
	
	private Widget getHomeWidget()
	{
		AppState state=StateController.getCurrentState();
		List<Widget> actions=state.getVisibleActions();
		for(Widget action:actions)
		{
			if(action.getWidgetType()==WidgetType.ACTION && action.getWidgetSubtype()==WidgetSubtype.GO_HOME_ACTION)
			{
				return action;
			}
		}
		return null;
	}
}
