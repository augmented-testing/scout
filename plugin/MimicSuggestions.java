package plugin;

import java.awt.Rectangle;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import plugin.DiffMatch.Diff;
import scout.AppState;
import scout.Path;
import scout.StateController;
import scout.Widget;
import scout.Widget.WidgetType;
import scout.Widget.WidgetVisibility;

public class MimicSuggestions
{
	private AppState currentState=null;
	private String checkMetadata=null;
	
	public void updateSuggestions()
	{
		currentState=StateController.getCurrentState();
		List<Widget> suggestions=new ArrayList<Widget>();

		// Select one visible type widget from a parallel state
  	List<Widget> clonesAndMutationList=StateController.getClonesAndMutations();
		Widget widget=getFirstVisibleCheck(clonesAndMutationList);
		if(widget!=null)		
		{
			DynamicPart.setCurrentDateFormat((String)widget.getMetadata("class_date_format"));
			
			List<Widget> clonesAndMutations=getSimilarChecks(widget, clonesAndMutationList);

			// Get texts from all clones and mutations
			List<String> allTexts=new ArrayList<String>();
			for(Widget cloneWidget:clonesAndMutations)
			{
				String text=getTextFromCheck(cloneWidget);
				if(text!=null)
				{
					allTexts.add(text);
				}
			}

			if(allTexts.size()>=2)
			{
				// We have two or more texts - possible to give a suggestion

				// Get the static texts that they all have in common
				List<String> commonParts=getCommonParts(allTexts);

				// Get the text and parameters from all clones
				List<TextParameters> textParams=new ArrayList<TextParameters>();
				for(Widget cloneWidget:clonesAndMutations)
				{
					String text=getTextFromCheck(cloneWidget);
					if(text!=null)
					{
						textParams.add(new TextParameters(text, getWidgetParams(cloneWidget, commonParts)));
					}
				}

				// Get parameters from the current path
				List<KeyValue> params=getWidgetParamsFromCurrentPath(commonParts);

				// Create a model or get an existing
				DynamicPart part=null;
				Mimic mimic=new Mimic(currentState);
				DynamicPart modelPart=getModel(clonesAndMutations);
				if(modelPart!=null && mimic.isModelValid(modelPart, textParams))
				{
					// We already have a model that is valid for all clones and mutations
					part=modelPart;
				}
				else
				{
					// Try to find a model that is valid for all clones and mutations
					part=mimic.findModel(params, textParams, isSimpleType(widget));
					if(part!=null)
					{
						for(Widget clone:clonesAndMutations)
						{
							// Remember the model
							clone.putMetadata("mimic_part", part);
						}
					}
					else
					{
						for(Widget clone:clonesAndMutations)
						{
							// Remove the model
							clone.removeMetadata("mimic_part");
							clone.setPriority(0);
						}
					}
				}

				if(part!=null)
				{
					// Found a model - get the resulting text suggestion based on parameters from current path
					String text=part.getText(params);
					if(text!=null && text.trim().length()>0 && checkMetadata!=null)
					{
						Widget currentWidget=getHiddenCheck(widget, clonesAndMutationList);
						if(currentWidget!=null)
						{
							String expression="{"+checkMetadata+"} = "+text.trim();
							Widget suggestion=new Widget(currentWidget);
							suggestion.setWidgetVisibility(WidgetVisibility.SUGGESTION);
							suggestion.setValidExpression(expression);
							suggestion.setComment(expression+". "+StateController.translate("SincePattern")+": {"+checkMetadata+"} = "+part.toString());
							suggestion.setPriority(1);
							suggestion.removeMetadata("neighbors");
							suggestions.add(suggestion);
							currentState.replaceSuggestedWidgets(suggestions, "MimicSuggestions");
						}
					}
				}
			}
		}
	}

	/**
	 * Get a stored model
	 * @param clonesAndMutations
	 * @return A model or null
	 */
	private DynamicPart getModel(List<Widget> clonesAndMutations)
	{
		for(Widget clone:clonesAndMutations)
		{
			DynamicPart part=(DynamicPart)clone.getMetadata("mimic_part");
			if(part!=null)
			{
				return part;
			}
		}
		return null;
	}
	
	/**
	 * @return A type action from a parallel state
	 */
	private Widget getFirstVisibleCheck(List<Widget> clonesAndMutationList)
	{
		for(Widget clone:clonesAndMutationList)
		{
			// Get the state that is parallel to this
			AppState nextState=clone.getNextState();
			if(nextState!=null && nextState!=currentState)
			{
				// Get widgets available on the sibling state
				List<Widget> siblingStateWidgets=nextState.getVisibleWidgets();
				for(Widget widget:siblingStateWidgets)
				{
					if(widget.getWidgetType()==WidgetType.CHECK && widget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
					{
						return widget;
					}					
				}
			}
		}
		return null;
	}

	private List<Widget> getSimilarChecks(Widget checkWidget, List<Widget> clonesAndMutationList)
	{
		List<Widget> widgets=new ArrayList<Widget>();
		for(Widget clone:clonesAndMutationList)
		{
			// Get the state that is parallel to this
			AppState nextState=clone.getNextState();
			if(nextState!=null && nextState!=currentState)
			{
				// Get widgets available on the sibling state
				List<Widget> siblingStateWidgets=nextState.getVisibleWidgets();
				for(Widget widget:siblingStateWidgets)
				{
					if(widget.getWidgetType()==WidgetType.CHECK && widget.getWidgetVisibility()==WidgetVisibility.VISIBLE && isAtSameLocation(checkWidget, widget))
					{
						widgets.add(widget);
					}
				}
			}
		}
		return widgets;
	}

	/**
	 * Get the hidden widget that is located at the same place as checkWidget
	 * @param checkWidget
	 * @param clonesAndMutationList
	 * @return A check widget or null
	 */
	private Widget getHiddenCheck(Widget checkWidget, List<Widget> clonesAndMutationList)
	{
		for(Widget clone:clonesAndMutationList)
		{
			// Get the state that is parallel to this
			AppState nextState=clone.getNextState();
			if(nextState!=null && nextState==currentState)
			{
				// Get widgets available on the sibling state
				List<Widget> siblingStateWidgets=nextState.getHiddenWidgets();
				for(Widget widget:siblingStateWidgets)
				{
					if(widget.getWidgetType()==WidgetType.CHECK && isAtSameLocation(checkWidget, widget))
					{
						return widget;
					}
				}
			}
		}
		return null;
	}

	/**
	 * @param w1
	 * @param w2
	 * @return true if w1 and w2 have a similar location
	 */
	private boolean isAtSameLocation(Widget w1, Widget w2)
	{
		Rectangle location=w1.getLocationArea();
		double centerX=location.getCenterX();
		double centerY=location.getCenterY();
		Rectangle area=w2.getLocationArea();
		if(area!=null)
		{
			if(area.contains(centerX, centerY))
			{
				return true;
			}
		}
		return false;
	}

	private List<KeyValue> getWidgetParams(Widget widget, List<String> commonParts)
	{
		List<KeyValue> params=new ArrayList<KeyValue>();
		List<Widget> path=StateController.getStateTree().getPathToState(widget.getNextState());
		for(int i=0; i<path.size(); i++)
		{
			Widget w=path.get(path.size()-1-i);
			String text=removeEnter(w.getText());
			if(text!=null && text.trim().length()>0 && i>0)
			{
				params.add(new KeyValue("{text"+i+"}", text));
			}
		}
		Date date=null;
		if(path.size()>0)
		{
			date=path.get(path.size()-1).getCreatedDate();
		}
		addConstants(params, date, commonParts);
		return params;
	}

	private List<KeyValue> getWidgetParamsFromCurrentPath(List<String> commonParts)
	{
		List<KeyValue> params=new ArrayList<KeyValue>();
		Path path=StateController.getStateTree().getCurrentPath();
		
		List<Widget> widgets=path.getWidgets();
		for(int i=widgets.size()-1; i>=0; i--)
		{
			Widget w=widgets.get(i);
			if(w.getNextState().isHome())
			{
				addConstants(params, path.getCreatedDate(), commonParts);
				return params;
			}
			String text=removeEnter(w.getText());
			if(text!=null && text.trim().length()>0)
			{
				int index=widgets.size()-i;
				params.add(new KeyValue("{text"+index+"}", text));
			}
		}
		addConstants(params, path.getCreatedDate(), commonParts);
		return params;
	}
	
	private void addConstants(List<KeyValue> params, Date date, List<String> commonParts)
	{
		params.add(new KeyValue("\" \"", " "));
		params.add(new KeyValue("1", "1"));
		params.add(new KeyValue("2", "2"));
		params.add(new KeyValue("3", "3"));
		params.add(new KeyValue("7", "7"));
		
		if(commonParts!=null)
		{
			for(String commonPart:commonParts)
			{
				List<String> list=splitString(commonPart);
				for(String s:list)
				{
					params.add(new KeyValue("\""+s+"\"", s));
				}
			}
		}

		String dateFormat=DynamicPart.getCurrentDateFormat();
		if(dateFormat!=null && dateFormat.length()>0 && date!=null)
		{
			String dateText=dateToText(date, dateFormat);
			params.add(new KeyValue("{current_date}", dateText));
		}
	}

	private static List<String> splitString(String s)
	{
		List<String> list=new ArrayList<String>();
		int index=s.lastIndexOf(' ');
		if(index>0)
		{
			String first=s.substring(0, index+1);
			String last=s.substring(index);
			list.add(first);
			list.add(last);
		}
		return list;
	}

	private String dateToText(Date date, String dateFormat)
	{
		try
		{
			DateFormat df = new SimpleDateFormat(dateFormat);
			return df.format(date);
		}
		catch (Exception e)
		{
			return "";
		}
	}

	/**
	 * @param requestComparator
	 * @return A list of common parts in all texts
	 */
	private List<String> getCommonParts(List<String> texts)
	{
		if(texts.size()<2)
		{
			// Need at least two to detect a solution
			return null;
		}

		String firstText=texts.get(0);

		DiffMatch diffMatch=new DiffMatch();
		for(String text:texts)
		{
			if(text!=firstText)
			{
				List<String> similar=new ArrayList<String>();
				List<Diff> diffs=diffMatch.diffTexts(firstText, text);
				for(Diff diff:diffs)
				{
					if(diff.operation==DiffMatch.Operation.EQUAL && isValidPart(diff.text))
					{
						similar.add(diff.text);
					}
				}
				if(textsContainAllParts(texts, similar))
				{
					return similar;
				}
			}
		}

		return null;
	}

	private boolean textsContainAllParts(List<String> texts, List<String> parts)
	{
		for(String text:texts)
		{
			if(!textContainAllParts(text, parts))
			{
				return false;
			}
		}
		return true;
	}

	private boolean textContainAllParts(String text, List<String> parts)
	{
		int index=0;
		for(String s:parts)
		{
			int i=text.indexOf(s, index);
			if(i<0)
			{
				return false;
			}
			index=i;
		}
		return true;
	}

	private boolean isValidPart(String part)
	{
		if(isDigits(part))
		{
			// Only digits are not valid
			return false;
		}
		if(part.length()>1)
		{
			// More than one non digit
			return true;
		}
		if(!Character.isAlphabetic(part.charAt(0)))
		{
			// Not an alphanumeric
			return true;
		}
		return false;
	}

	private boolean isDigits(String text)
	{
		for(int i=0; i<text.length(); i++)
		{
			char c=text.charAt(i);
			if(!(Character.isDigit(c) || c=='-' || c==':'))
			{
				return false;
			}
		}
		return true;
	}

	private String removeEnter(String keyboardInput)
	{
		if(keyboardInput==null)
		{
			return null;
		}
		if(keyboardInput.endsWith("[ENTER]"))
		{
			keyboardInput=keyboardInput.substring(0, keyboardInput.length()-7);
			return keyboardInput;
		}
		return keyboardInput;
	}
	
	private boolean isSimpleType(Widget widget)
	{
		String classType=(String)widget.getMetadata("class_type");
		if(classType!=null && ("integer".equals(classType) || "date".equals(classType)))
		{
			return true;
		}
		return false;
	}
	
	private String getTextFromCheck(Widget widget)
	{
		String validExpression=widget.getValidExpression();
		if(validExpression!=null)
		{
			if(validExpression.startsWith("{text} ="))
			{
				String text=validExpression.substring(8).trim();
				checkMetadata="text";
				return text;
			}
			else if(validExpression.startsWith("{text}="))
			{
				String text=validExpression.substring(7).trim();
				checkMetadata="text";
				return text;
			}
			else if(validExpression.startsWith("{value} ="))
			{
				String text=validExpression.substring(9).trim();
				checkMetadata="value";
				return text;
			}
			else if(validExpression.startsWith("{value}="))
			{
				String text=validExpression.substring(8).trim();
				checkMetadata="value";
				return text;
			}
			else if(validExpression.startsWith("{placeholder} ="))
			{
				String text=validExpression.substring(15).trim();
				checkMetadata="placeholder";
				return text;
			}
			else if(validExpression.startsWith("{placeholder}="))
			{
				String text=validExpression.substring(14).trim();
				checkMetadata="placeholder";
				return text;
			}
		}
		return null;
	}
}
