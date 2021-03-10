package plugin;

import java.awt.Point;
import java.util.List;

import scout.AppState;
import scout.GoHomeAction;
import scout.LeftClickAction;
import scout.PluginController;
import scout.StateController;
import scout.StateController.Mode;
import scout.StateController.Route;
import scout.Widget;
import scout.Widget.WidgetStatus;
import scout.Widget.WidgetType;
import scout.Widget.WidgetVisibility;

public class Autopilot
{
	private static final int CLICK_DELAY=0;
	private static final int TIMEOUT=30000;

	private static long changedStateTime=0;
	private static AppState currentState=null;
	private static boolean hasPerformedAction=false;
	private static boolean hasCoverageFactor=true;

	public void startSession()
	{
		changedStateTime=System.currentTimeMillis();
		currentState=StateController.getCurrentState();
		hasPerformedAction=false;
	}

	public void changeState()
	{
		changedStateTime=System.currentTimeMillis();
		currentState=StateController.getCurrentState();
		hasPerformedAction=false;
		hasCoverageFactor=!"0".equals(StateController.getSystemProperty("coverage_factor", "1"));

		if(StateController.getRoute()==Route.NAVIGATING && StateController.getNavigationTargetState()!=null && StateController.getNavigationTargetState()==StateController.getCurrentState())
		{
			// Is navigating and is a target state
			StateController.displayMessage("Reached the destination");
			StateController.setMode(Mode.MANUAL);
			StateController.setRoute(Route.EXPLORING);
			return;
		}
		else
		{
			String bookmark=StateController.getCurrentState().getBookmark();
			if(bookmark!=null && bookmark.trim().length()>0)
			{
				StateController.displayMessage(bookmark, 3000);
			}
		}
	}

	public void updateState()
	{
		if(!hasPerformedAction)
		{
			// Has not performed action since state changed
			autoPerformAction();
		}
		if(StateController.getMode()==Mode.MANUAL)
		{
			// In manual mode - reset time
			changedStateTime=System.currentTimeMillis();
		}
	}

	private void autoPerformAction()
	{
		if(StateController.getMode()==Mode.AUTO)
		{
			long duration=System.currentTimeMillis()-changedStateTime;

			if(duration>=CLICK_DELAY && isReadyToPerformAction())
			{
				Widget recommendedWidget=StateController.findRecommendedWidget(currentState);
				if(recommendedWidget!=null)
				{
					Object matchPercent=recommendedWidget.getMetadata("match_percent");
					if(matchPercent==null || 100==(int)matchPercent)
					{
						if((recommendedWidget.getWidgetType()==WidgetType.ACTION || recommendedWidget.getWidgetType()==WidgetType.CHECK) && recommendedWidget.getWidgetVisibility()!=WidgetVisibility.HIDDEN)
						{
							// Click on the recommended action
							if(currentState==StateController.getCurrentState())
							{
								LeftClickAction action=new LeftClickAction();
								Point p=new Point((int)recommendedWidget.getLocationArea().getCenterX(), (int)recommendedWidget.getLocationArea().getCenterY());
								action.setLocation(p);
								PluginController.performAction(action);
								if(recommendedWidget.getWidgetType()==WidgetType.ACTION)
								{
									hasPerformedAction=true;
								}
							}
						}
					}
				}
			}

			int coveredPercent=StateController.getStateTree().coveredPercent(StateController.getProductVersion());
			if(hasCoverageFactor && coveredPercent==100 && StateController.getRoute()!=Route.NAVIGATING)
			{
				// Done
				StateController.displayMessage(coveredPercent+"% coverage");
				StateController.setMode(Mode.MANUAL);
				if(StateController.isAutoStopSession())
				{
					StateController.setAutoStopSession(false);
					StateController.stopSession();
				}
			}
			else if(changedStateTime!=0 && duration>TIMEOUT)
			{
				// Waited a while to find a recommended action - give up
				String goHome=StateController.getSystemProperty("go_home_on_timeout", "yes");
				if(!StateController.getCurrentState().isHome() && "yes".equalsIgnoreCase(goHome))
				{
					StateController.displayMessage("Go Home", 2000);
					PluginController.performAction(new GoHomeAction());
				}
				else
				{
					StateController.displayMessage(coveredPercent+"% coverage");
					StateController.setMode(Mode.MANUAL);
				}
			}
		}
	}
	
	/**
	 * @return true if ready to auto perform an action
	 */
	private boolean isReadyToPerformAction()
	{
		if(currentState!=null)
		{
			List<Widget> widgets=currentState.getNonHiddenWidgets();
			if(widgets==null || widgets.size()==0)
			{
				return false;
			}
			for(Widget widget:widgets)
			{
				if((widget.getWidgetType()==WidgetType.ACTION && widget.getWidgetStatus()==WidgetStatus.LOCATED) || 
						(widget.getWidgetType()==WidgetType.CHECK && widget.getWidgetStatus()==WidgetStatus.VALID) || 
						(widget.getWidgetType()==WidgetType.ISSUE && (widget.getWidgetStatus()==WidgetStatus.VALID || widget.getWidgetStatus()==WidgetStatus.UNLOCATED)))
				{
					// Located or valid
					Object matchPercent=widget.getMetadata("match_percent");
					if(matchPercent==null || 100==(int)matchPercent)
					{
						// Perfect match
					}
					else
					{
						return false;
					}
				}
				else
				{
					return false;
				}
			}
			return true;
		}
		return false;
	}
}
