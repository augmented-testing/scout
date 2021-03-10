package plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import scout.AppState;
import scout.StateController;
import scout.Widget;
import scout.Widget.WidgetSubtype;
import scout.Widget.WidgetVisibility;

public class RandomSuggestion
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
						// Integers
						for(int i=0; i<3; i++)
						{
							Random rand=new Random(System.currentTimeMillis());
							int random=rand.nextInt(5)-2;
							String integerSuggestion=""+random;
							if(!containsValue(allValues, integerSuggestion) && !containsValue(suggestedValues, integerSuggestion))
							{
								// Suggestion not found among current values
								Widget suggestion=new Widget(widget);
								suggestion.setWidgetVisibility(WidgetVisibility.SUGGESTION);
								suggestion.setText(integerSuggestion);
								suggestion.setComment("Random suggestion");
								suggestion.setPriority(0);
								suggestions.add(suggestion);
								suggestedValues.add(integerSuggestion);
							}
						}
					}
				}
			}
		}
		if(suggestions.size()>0)
		{
			currentState.replaceSuggestedWidgets(suggestions, "RandomSuggestion");
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
