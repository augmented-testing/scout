// SPDX-FileCopyrightText: 2021 Michel Nass
//
// SPDX-License-Identifier: MIT

package plugin;

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
import scout.Widget.WidgetSubtype;
import scout.Widget.WidgetType;
import scout.Widget.WidgetVisibility;

public class MimicValues
{
	public void updateSuggestions()
	{
		AppState currentState=StateController.getCurrentState();
		List<Widget> suggestions=new ArrayList<Widget>();

		// Select one visible type widget from a parallel state
		Widget widget=getFirstVisibleTypeAction();
		if(widget!=null)		
		{
			DynamicPart.setCurrentDateFormat((String)widget.getMetadata("class_date_format"));

			// Get clones and mutations of a visible parallel widget
	  	List<Widget> clonesAndMutations=StateController.getClonesAndMutations(widget);

			// Get texts from all clones and mutations
			List<String> allTexts=new ArrayList<String>();
			for(Widget cloneWidget:clonesAndMutations)
			{
				if(cloneWidget.getText()!=null)
				{
					allTexts.add(cloneWidget.getText());
				}
			}

			if(allTexts.size()>=2)
			{
				// We have two or more texts - possible to give a suggestion

				// Get the static texts that they all have in common
				List<String> commonParts=getCommonParts(allTexts);

				// Add individual words (if they do not already exist)
//				addWords(commonParts, allTexts);

				// Get the text and parameters from all clones
				List<TextParameters> textParams=new ArrayList<TextParameters>();
				for(Widget cloneWidget:clonesAndMutations)
				{
					if(cloneWidget.getText()!=null)
					{
						textParams.add(new TextParameters(cloneWidget.getText(), getWidgetParams(cloneWidget, commonParts)));
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
					if(text!=null && text.trim().length()>0)
					{
						Widget suggestion=new Widget(widget);
						suggestion.setWidgetVisibility(WidgetVisibility.SUGGESTION);
						suggestion.setText(text);
						suggestion.setComment(StateController.translate("SincePattern")+": "+part.toString());
						suggestion.setPriority(1);
						suggestion.removeMetadata("neighbors");
						suggestions.add(suggestion);
						currentState.replaceSuggestedWidgets(suggestions, "MimicValues");
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
	private Widget getFirstVisibleTypeAction()
	{
		// Get clones and mutations of the last performed widget
  	List<Widget> clonesAndMutationList=StateController.getClonesAndMutations();
		for(Widget clone:clonesAndMutationList)
		{
			// Get the state that is parallel to this
			AppState nextState=clone.getNextState();
			if(nextState!=null)
			{
				// Get widgets available on the sibling state
				List<Widget> siblingStateWidgets=nextState.getVisibleWidgets();
				for(Widget widget:siblingStateWidgets)
				{
					if(widget.getWidgetType()==WidgetType.ACTION && widget.getWidgetSubtype()==WidgetSubtype.TYPE_ACTION && widget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
					{
						// Found a type action
						return widget;
					}					
				}
			}
		}
		return null;
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

	private void addWords(List<String> words, List<String> texts)
	{
		for(String text:texts)
		{
			String[] splitText=text.trim().split("\\s+");
			for(String word:splitText)
			{
				if(!isInt(word) && !words.contains(word))
				{
					words.add(word);
				}
			}
		}
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

	private boolean isInt(String text)
	{
		try
		{
			Integer.parseInt(text.trim());
			return true;
		}
		catch(Exception e)
		{
			// Not an int
			return false;
		}
	}
}
