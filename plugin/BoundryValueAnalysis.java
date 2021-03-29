// SPDX-FileCopyrightText: 2021 Michel Nass
//
// SPDX-License-Identifier: MIT

package plugin;

import java.util.ArrayList;
import java.util.List;

import scout.AppState;
import scout.StateController;
import scout.Widget;
import scout.Widget.WidgetSubtype;
import scout.Widget.WidgetVisibility;

public class BoundryValueAnalysis
{
	public void updateSuggestions()
	{
		AppState currentState=StateController.getCurrentState();

		List<Widget> suggestions=new ArrayList<Widget>();
		List<String> suggestedValues=new ArrayList<String>();
		List<Widget> widgets=currentState.getVisibleWidgets();
		for(Widget widget:widgets)
		{
			if(widget.getWidgetSubtype()==WidgetSubtype.TYPE_ACTION && widget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
			{
				// Get clones and mutations of a visible widget
		  	List<Widget> clonesAndMutations=StateController.getClonesAndMutations(widget);

				// Get values from all clones and mutations
				List<String> allValues=new ArrayList<String>();
				for(Widget cloneWidget:clonesAndMutations)
				{
					allValues.add(cloneWidget.getText());
				}
				
				if(allValues.size()>0)
				{
					// We have previous values
					String classType=(String)widget.getMetadata("class_type");
					if("integer".equals(classType))
					{
						final String[] integerSuggestions={"-1", "0", "1"};
						for(String integerSuggestion:integerSuggestions)
						{
							if(!containsValue(allValues, integerSuggestion) && !containsValue(suggestedValues, integerSuggestion))
							{
								// Suggestion not found among current values
								Widget suggestion=new Widget(widget);
								suggestion.setWidgetVisibility(WidgetVisibility.SUGGESTION);
								suggestion.setText(integerSuggestion);
								suggestion.setComment(StateController.translate("SinceBoundryValue"));
								suggestion.setPriority(0);
								suggestions.add(suggestion);
								suggestedValues.add(integerSuggestion);
							}
						}
					}
					else if("email".equals(classType))
					{
						List<String> emailSuggestions=new ArrayList<String>();
						String firstEmail=allValues.get(0);
						int index=firstEmail.indexOf('@');
						if(index>=0)
						{
							String withoutName=firstEmail.substring(index);
							emailSuggestions.add(withoutName);
						}
						int lastIndex=firstEmail.lastIndexOf('.');
						if(lastIndex>=0)
						{
							String withoutDomain=firstEmail.substring(0, lastIndex);
							emailSuggestions.add(withoutDomain);
						}
						for(String emailSuggestion:emailSuggestions)
						{
							if(!containsValue(allValues, emailSuggestion) && !containsValue(suggestedValues, emailSuggestion))
							{
								Widget suggestion=new Widget(widget);
								suggestion.setWidgetVisibility(WidgetVisibility.SUGGESTION);
								suggestion.setText(emailSuggestion);
								suggestion.setComment(StateController.translate("SinceNegativeTest"));
								suggestion.setPriority(0);
								suggestions.add(suggestion);
								suggestedValues.add(emailSuggestion);
							}
						}
					}
				}
			}
		}
		if(suggestions.size()>0)
		{
			currentState.replaceSuggestedWidgets(suggestions, "BoundryValueAnalysis");
		}
	}

	private boolean containsValue(List<String> list, String value)
	{
		for(String item:list)
		{
			if(item.trim().equals(value.trim()))
			{
				return true;
			}
		}
		return false;
	}
}
