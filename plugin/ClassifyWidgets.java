package plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import scout.AppState;
import scout.StateController;
import scout.Widget;
import scout.Widget.WidgetSubtype;
import scout.Widget.WidgetType;
import scout.Widget.WidgetVisibility;

public class ClassifyWidgets
{
  private static final Map<String, String> DATE_FORMAT_REGEXPS = new HashMap<String, String>()
  {{
    put("^\\d{8}$", "yyyyMMdd");
    put("^\\d{1,2}-\\d{1,2}-\\d{4}$", "dd-MM-yyyy");
    put("^\\d{4}-\\d{1,2}-\\d{1,2}$", "yyyy-MM-dd");
    put("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy");
    put("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
    put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$", "dd MMM yyyy");
    put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$", "dd MMMM yyyy");
  }};
	
	public void updateClassifications()
	{
		// Select one visible type widget from a parallel state
		Widget widget=getFirstVisibleTypeAction();
		if(widget!=null)		
		{
			// Get clones and mutations of a visible parallel widget
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
				widget.putMetadata("class_type", "");
				widget.putMetadata("class_date_format", "");
				String dateFormat=isDate(allValues);
				if(dateFormat!=null)
				{
					for(Widget clone:clonesAndMutations)
					{
						clone.putMetadata("class_type", "date");
						clone.putMetadata("class_date_format", dateFormat);
					}
				}
				else if(isIntList(allValues))
				{
					// Integer
					for(Widget clone:clonesAndMutations)
					{
						clone.putMetadata("class_type", "integer");
					}
				}
				else if(isDoubleList(allValues))
				{
					// Integer
					for(Widget clone:clonesAndMutations)
					{
						clone.putMetadata("class_type", "double");
					}
				}
				else if(isEmail(allValues))
				{
					// Email
					for(Widget clone:clonesAndMutations)
					{
						clone.putMetadata("class_type", "email");
					}
				}
			}
		}
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

	private boolean isIntList(List<String> list)
	{
		for(String text:list)
		{
			if(!isInt(text))
			{
				return false;
			}
		}
		return true;
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

	private boolean isDoubleList(List<String> list)
	{
		for(String text:list)
		{
			if(!isDouble(text))
			{
				return false;
			}
		}
		return true;
	}

	private boolean isDouble(String text)
	{
		try
		{
			Double.parseDouble(text.trim());
			return true;
		}
		catch(Exception e)
		{
			// Not a double
			return false;
		}
	}

	private boolean isEmail(List<String> list)
	{
		for(String item:list)
		{
			if(!isEmail(item))
			{
				return false;
			}
		}
		return true;
	}

	private boolean isEmail(String email)
	{
		Pattern pattern = Pattern.compile("^.+@.+\\..+$");
		Matcher m = pattern.matcher(email);
		return m.matches();
	}

	/**
	 * Returns a date format if all items in the list shares the same
	 * @param list
	 * @return A common date format or null
	 */
	private String isDate(List<String> list)
	{
		String commonDateFormat=null;
		for(String item:list)
		{
			String dateFormat=determineDateFormat(item);
			if(dateFormat==null)
			{
				return null;
			}
			else
			{
				if(commonDateFormat==null)
				{
					commonDateFormat=dateFormat;
				}
				else if(!commonDateFormat.equalsIgnoreCase(dateFormat))
				{
					return null;
				}
			}
		}
		return commonDateFormat;
	}

	private String determineDateFormat(String dateString)
	
	{
		for (String regexp : DATE_FORMAT_REGEXPS.keySet())
		{
			if (dateString.toLowerCase().matches(regexp))
			{
				return DATE_FORMAT_REGEXPS.get(regexp);
			}
		}
		return null;
	}
}
