// SPDX-FileCopyrightText: 2021 Michel Nass
//
// SPDX-License-Identifier: MIT

package plugin;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import scout.Action;
import scout.Correction;
import scout.Neighbor;
import scout.StateController;
import scout.Widget;

public class AutoRepair
{
	private final static int AUTO_REPAIR_DEFAULT_DELAY = 10000;
	private final static int AUTO_REPAIR_MATCH_SCORE = 100;

	private static long autoRepairDelay=AUTO_REPAIR_DEFAULT_DELAY;
	private static long changeStateTime=0;

	public void startSession()
	{
		String delay=StateController.getProductProperty("auto_repair_delay", null);
		if(delay!=null)
		{
			autoRepairDelay=Integer.parseInt(delay);
		}

		// Wait for the session to start
		for(int i=0; i<20 && !StateController.isOngoingSession(); i++)
		{
			StateController.delay(1000);
		}

		changeStateTime=System.currentTimeMillis();
	}

	public void changeState()
	{
		changeStateTime=System.currentTimeMillis();
	}

	public void performAction(Action action)
	{
		changeStateTime=System.currentTimeMillis();
	}
	
	public void updateState()
	{
		if(System.currentTimeMillis()>changeStateTime+autoRepairDelay)
		{
			List<Widget> nonHiddenWidgets=StateController.getCurrentState().getNonHiddenWidgets();
			for(Widget widget:nonHiddenWidgets)
			{
				if(widget.getWidgetStatus()==Widget.WidgetStatus.UNLOCATED)
				{
					Widget matchingWidget=(Widget)widget.getMetadata("matching_widget");
					int matchScore=(int)widget.getMetadata("match_score");
					if(matchingWidget!=null && matchScore>=AUTO_REPAIR_MATCH_SCORE)
					{
						String repairText="";
						
						if(repairWidget(widget, matchingWidget))
						{
							repairText+=getInvalidMetadataString(widget);
						}

						if(repairWidgetLocation(widget, matchingWidget))
						{
							if(repairText.length()>0)
							{
								repairText+=", ";
							}
							repairText+="location";
						}

						if(repairNeighbors(widget, matchingWidget))
						{
							if(repairText.length()>0)
							{
								repairText+=", ";
							}
							repairText+="neighbors";
						}
						
						if(repairText.length()>0)
						{
							StateController.displayMessage("Auto repairing: "+repairText, 2000);
						}
					}

					Rectangle area=widget.getLocationArea();
					if(area!=null)
					{
						if(area.getX()+area.getWidth()>StateController.getCaptureWidth())
						{
							// Move inside capture
							widget.setLocationArea(new Rectangle((int)(StateController.getCaptureWidth()-area.getWidth()), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight()));
							area=widget.getLocationArea();
						}
						if(area.getY()+area.getHeight()>StateController.getCaptureHeight())
						{
							// Move inside capture
							widget.setLocationArea(new Rectangle((int)area.getX(), (int)(StateController.getCaptureHeight()-area.getHeight()), (int)area.getWidth(), (int)area.getHeight()));
						}
					}
				}
			}
		}
	}

	/**
	 * Try to repair widget from a matching widget
	 * @param widget
	 * @return true if repaired
	 */
	private boolean repairWidget(Widget widget, Widget matchingWidget)
	{
		boolean hasRepaired=false;
  	List<Widget> clones=StateController.getClonesAndMutations(widget);
  	if(clones.size()==0)
  	{
  		clones.add(widget);
  	}
		for(Widget w:clones)
		{
			Correction correction=new Correction();
			List<String> keys=matchingWidget.getMetadataKeys();

			// Check if metadata should be repaired
			w.clearInvalidMetadata();
			for(String key:keys)
			{
				Object o1=w.getMetadata(key);
				Object o2=matchingWidget.getMetadata(key);
				if(o2 instanceof String)
				{
					String value1=(String)o1;
					String value2=(String)o2;
					if(value1==null)
					{
						w.putMetadata(key, value2);
					}
					else if(!value1.equals(value2))
					{
						// Has changed - repair
						w.putMetadata(key, value2);
						w.addInvalidMetadata(key);
						if(!w.isIgnoredMetadata(key))
						{
							// Repaired non-ignored metadata
							hasRepaired=true;
							correction.putMetadata("from_"+key, value1);
							correction.putMetadata("to_"+key, value2);
							if(shouldBeIgnored(w, key))
							{
								w.addIgnoredMetadata(key);
							}
						}
					}
				}
			}

			if(w.getLocationArea()!=null && !w.getLocationArea().equals(matchingWidget.getLocationArea()))
			{
				// Update location and size
				w.setLocationArea(matchingWidget.getLocationArea());
				w.addInvalidMetadata("location");
				hasRepaired=true;
			}

			if(!correction.isEmpty())
			{
				correction.setIteration(StateController.getCurrentState().getIteration());
				addCorrection(w, correction);
			}
		}
		return hasRepaired;
	}

	private boolean shouldBeIgnored(Widget widget, String key)
	{
		Object correctionsObject=widget.getMetadata("corrections");
		if(correctionsObject!=null)
		{
			long currentIteration=StateController.getCurrentState().getIteration();
			List<Correction> corrections=(List<Correction>)correctionsObject;
			for(Correction correction:corrections)
			{
				if(correction.getMetadata("from_"+key)!=null)
				{
					long correctionIteration=correction.getIteration();
					if(correctionIteration==currentIteration-1)
					{
						// Was also corrected during the previous iteration
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean repairWidgetLocation(Widget widget, Widget matchingWidget)
	{
		if(widget.getLocationArea()!=null && !widget.getLocationArea().equals(matchingWidget.getLocationArea()))
		{
			// Update location and size
			widget.setLocationArea(matchingWidget.getLocationArea());
			widget.putMetadata("neighbors", matchingWidget.getMetadata("neighbors"));
			return true;
		}
		return false;
	}

	private boolean repairNeighbors(Widget widget, Widget matchingWidget)
	{
		List<Neighbor> neighbors1=(List<Neighbor>)widget.getMetadata("neighbors");
		List<Neighbor> neighbors2=(List<Neighbor>)matchingWidget.getMetadata("neighbors");
		if(!hasSameNeighbors(neighbors1, neighbors2))
		{
			if(neighbors2==null)
			{
				widget.removeMetadata("neighbors");
			}
			else
			{
				widget.putMetadata("neighbors", neighbors2);
			}
			return true;
		}
		return false;
	}

	private boolean hasSameNeighbors(List<Neighbor> neighbors1, List<Neighbor> neighbors2)
	{
		if(neighbors1==null && neighbors2==null)
		{
			// Both is missing neighbors - same
			return true;
		}
		if(neighbors1==null || neighbors2==null)
		{
			// One is missing neighbors - not same
			return false;
		}
		if(neighbors1.size()!=neighbors2.size())
		{
			return false;
		}
		for(int i=0; i<neighbors1.size(); i++)
		{
			Neighbor n1=neighbors1.get(i);
			Neighbor n2=neighbors2.get(i);
			if(n1.getText()!=null && !n1.getText().equals(n2.getText()))
			{
				return false;
			}
			if(!(n1.getDeltaX()==n2.getDeltaX() && n1.getDeltaY()==n2.getDeltaY() && n1.getHeight()==n2.getHeight() && n1.getWidth()==n2.getWidth()))
			{
				return false;
			}
		}
		return true;
	}

	private void addCorrection(Widget w, Correction correction)
	{
		Object correctionsObject=w.getMetadata("corrections");
		if(correctionsObject!=null)
		{
			List<Correction> corrections=(List<Correction>)correctionsObject;
			corrections.add(correction);
		}
		else
		{
			List<Correction> corrections=new ArrayList<Correction>();
			corrections.add(correction);
			w.putMetadata("corrections", corrections);
		}
	}
	
	private String getInvalidMetadataString(Widget widget)
	{
		List<String> invalidMetadata=widget.getInvalidMetadata();
		int count=0;
		String comment="";
		for(String metadata:invalidMetadata)
		{
			if(count>0)
			{
				comment+=", ";
			}
			comment+=metadata;
			count++;
		}
		return comment;
	}
}
