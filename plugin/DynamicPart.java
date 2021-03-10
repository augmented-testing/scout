package plugin;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DynamicPart implements Serializable, Comparable<DynamicPart>
{
	private static final long serialVersionUID = 5299884818431021588L;

	public static String[] commands = {"add", "sub", "mult", "div", "concat", "concat"};
	public static int[] noParams = {2, 2, 2, 2, 2, 3};
	private static String currentDateFormat=null;

	private String functionName=null;
	private int noParameters=0;
	private List<DynamicPart> parameters=new ArrayList<DynamicPart>();
	private int score=0;
	private int complexity=0;

	public DynamicPart()
	{
	}

	public DynamicPart(DynamicPart from)
	{
		functionName=from.getFunctionName();
		noParameters=from.getNoParameters();
		parameters=new ArrayList<DynamicPart>();
		List<DynamicPart> fromParameters=from.getParameters();
		for(DynamicPart fromParameter:fromParameters)
		{
			parameters.add(new DynamicPart(fromParameter));
		}
	}

	public String getFunctionName()
	{
		return functionName;
	}

	public void setFunctionName(String functionName)
	{
		this.functionName = functionName;
	}

	public int getNoParameters()
	{
		return noParameters;
	}

	public void setNoParameters(int noParameters)
	{
		this.noParameters = noParameters;
	}

	public List<DynamicPart> getParameters()
	{
		return parameters;
	}

	public void setParameters(List<DynamicPart> parameters)
	{
		this.parameters = parameters;
	}

	public String getText(List<KeyValue> params)
	{
		try
		{
			if("add".equalsIgnoreCase(functionName) && parameters.size()==2)
			{
				DynamicPart part0=parameters.get(0);
				DynamicPart part1=parameters.get(1);
				int delta=part1.getInt(params);
				return adjustText(part0, params, delta);
			}
			else if("sub".equalsIgnoreCase(functionName) && parameters.size()==2)
			{
				DynamicPart part0=parameters.get(0);
				DynamicPart part1=parameters.get(1);
				int delta=part1.getInt(params);
				return adjustText(part0, params, -delta);
			}
			else if("mult".equalsIgnoreCase(functionName) && parameters.size()==2)
			{
				DynamicPart part0=parameters.get(0);
				DynamicPart part1=parameters.get(1);
				return ""+(part0.getInt(params)*part1.getInt(params));
			}
			else if("div".equalsIgnoreCase(functionName) && parameters.size()==2)
			{
				DynamicPart part0=parameters.get(0);
				DynamicPart part1=parameters.get(1);
				try
				{
					return ""+(part0.getInt(params)/part1.getInt(params));
				}
				catch(Exception e)
				{
					return "Infinite";
				}
			}
			else if("concat".equalsIgnoreCase(functionName) && parameters.size()==2)
			{
				DynamicPart part0=parameters.get(0);
				DynamicPart part1=parameters.get(1);
				return part0.getText(params)+part1.getText(params);
			}
			else if("concat".equalsIgnoreCase(functionName) && parameters.size()==3)
			{
				DynamicPart part0=parameters.get(0);
				DynamicPart part1=parameters.get(1);
				DynamicPart part2=parameters.get(2);
				return part0.getText(params)+part1.getText(params)+part2.getText(params);
			}
/*
			else if("equal".equalsIgnoreCase(functionName) && parameters.size()==4)
			{
				DynamicPart part0=parameters.get(0);
				DynamicPart part1=parameters.get(1);
				DynamicPart part2=parameters.get(2);
				DynamicPart part3=parameters.get(3);
				if(part0.getText(params).equals(part1.getText(params)))
				{
					return part2.getText(params);
				}
				else
				{
					return part3.getText(params);
				}
			}
*/
			else
			{
				// Find parameters
				for(KeyValue param:params)
				{
					if(functionName.equalsIgnoreCase(param.getKey()))
					{
						return param.getValue();
					}
				}
			}
		}
		catch(Exception e)
		{
			return "";
		}
		return "";
	}

	private String adjustText(DynamicPart part, List<KeyValue> params, int delta)
	{
		String text=part.getText(params);
		Date date=parseDate(text);
		if(date!=null)
		{
			date=addDays(date, delta);
			String dateText=dateToText(date, currentDateFormat);
			return dateText;
		}
		else
		{
			return ""+(part.getInt(text)+delta);
		}
	}

	private int getInt(List<KeyValue> params)
	{
		return getInt(getText(params));
	}
	
	private int getInt(String text)
	{
		try
		{
			return Integer.parseInt(text);
		}
		catch(Exception e)
		{
			return 0;
		}
	}

	public String toString()
	{
		if(parameters.size()==0)
		{
			return functionName;
		}
		StringBuffer buf=new StringBuffer();
		buf.append(functionName+"(");
		boolean first=true;
		for(DynamicPart part:parameters)
		{
			if(part.toString()!=null && part.toString().length()>0)
			{
				if(!first)
				{
					buf.append(",");
				}
				buf.append(part.toString());
				first=false;
			}
		}
		buf.append(")");
		return buf.toString();
	}

	private Date parseDate(String dateString)
	{
		if(currentDateFormat==null || currentDateFormat.length()==0)
		{
			return null;
		}
		return parseDate(dateString, currentDateFormat);
	}

	private Date parseDate(String dateString, String dateFormat)
	{
		try
		{
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
			simpleDateFormat.setLenient(false);
			return simpleDateFormat.parse(dateString);
		}
		catch (Exception e)
		{
			return null;
		}
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

	private Date addDays(Date date, int days)
	{
		Calendar calendar = toCalendar(date);
		addDays(calendar, days);
		return calendar.getTime();
	}
	
	private void addDays(Calendar calendar, int days)
	{
		calendar.add(Calendar.DATE, days);
	}
	
	private Calendar toCalendar(Date date)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.setTime(date);
		return calendar;
	}

	public static String getCurrentDateFormat()
	{
		return currentDateFormat;
	}

	public static void setCurrentDateFormat(String currentDateFormat)
	{
		DynamicPart.currentDateFormat = currentDateFormat;
	}

	public int getScore()
	{
		return score;
	}

	public void setScore(int score)
	{
		this.score = score;
	}

	public int compareTo(DynamicPart compareTo)
	{
		if(compareTo.getScore()==this.getScore())
		{
			// Sort on Kolmogorov complexity
			return this.getComplexity()-compareTo.getComplexity();
		}
		return compareTo.getScore()-this.getScore();
	}

	/**
	 * Get the Kolmogorov complexity
	 * @return Kolmogorov complexity
	 */
	public int getComplexity()
	{
		return complexity;
	}

	/**
	 * Set the Kolmogorov complexity
	 * @param complexity Kolmogorov complexity
	 */
	public void setComplexity(int complexity)
	{
		this.complexity = complexity;
	}
}
