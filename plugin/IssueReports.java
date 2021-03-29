// SPDX-FileCopyrightText: 2021 Michel Nass
//
// SPDX-License-Identifier: MIT

package plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import scout.AppState;
import scout.StateController;
import scout.Widget;
import scout.Widget.WidgetSubtype;
import scout.Widget.WidgetType;

public class IssueReports
{
	private String filename=null;

	public void generateReport()
	{
		// Make sure we have a reports folder
		File file=new File("reports/"+StateController.getProduct());
		file.mkdirs();

		List<Widget> issues=StateController.getStateTree().getAllIssues();
		if(issues.size()>0)
		{
			StateController.displayMessage("Generating "+issues.size()+" issue reports");
		}
		else
		{
			StateController.displayMessage("No issues to report");
		}

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd h-mm-ss");
		for(Widget issue:issues)
		{
			String dateStr=df.format(issue.getReportedDate());
			filename="reports/"+StateController.getProduct()+"/Issue "+issue.getId()+" "+dateStr+".txt";

			writeLine(filename, "Reported Date: "+dateStr, false);
			if(issue.getReportedBy()!=null && issue.getReportedBy().trim().length()>0)
			{
				log(filename, "Reported By: "+issue.getReportedBy());
			}
			log(filename, "Reported Text: "+issue.getReportedText());
			if(issue.getMetadata("report_issue_time")!=null)
			{
				long issueDuration=(long)issue.getMetadata("report_issue_time");
				log(filename, "Reported Duration: "+getDurationTime(issueDuration));
			}
			log(filename, "");
			log(filename, "Steps:");

			AppState state=StateController.getStateTree().findState(issue);
			if(state!=null)
			{
				List<Widget> widgets=StateController.getStateTree().getPathToState(state);
				if(widgets!=null)
				{
					if(!addWidgets(widgets))
					{
						StateController.displayMessage("Failed to generate report");
						return;
					}
				}
			}
		}
	}

	private boolean addWidgets(List<Widget> widgets)
	{
		for(Widget widget:widgets)
		{
			String comment=widget.getComment();
			if(comment!=null && comment.trim().length()>0)
			{
				addComment(comment);
			}
			if(widget.getWidgetType()==WidgetType.ACTION)
			{
				if(widget.getWidgetSubtype()==WidgetSubtype.LEFT_CLICK_ACTION)
				{
					String text=getWidgetText(widget);
					if(text!=null && text.trim().length()>0)
					{
						addText(StateController.translate("LeftClick")+" \""+text+"\"");
					}
					else
					{
						addText(StateController.translate("LeftClick"));
					}
				}
				else if(widget.getWidgetSubtype()==WidgetSubtype.TYPE_ACTION)
				{
					addText(StateController.translate("Type")+" \""+widget.getText()+"\"");
				}
				else if(widget.getWidgetSubtype()==WidgetSubtype.SELECT_ACTION)
				{
					String text=getWidgetText(widget);
					addText(StateController.translate("Select")+" \""+text+"\"");
				}
				else if(widget.getWidgetSubtype()==WidgetSubtype.GO_HOME_ACTION)
				{
					addText(StateController.translate("GoHome"));
				}
			}
		}
		return true;
	}

	private String getWidgetText(Widget widget)
	{
		if(widget.getMetadata("option_text")!=null)
		{
			String text=(String)widget.getMetadata("option_text");
			if(text.trim().length()>0)
			{
				return text.trim();
			}
		}
		if(widget.getMetadata("option_value")!=null)
		{
			String text=(String)widget.getMetadata("option_value");
			if(text.trim().length()>0)
			{
				return text.trim();
			}
		}
		if(widget.getMetadata("text")!=null)
		{
			String text=(String)widget.getMetadata("text");
			if(text.trim().length()>0)
			{
				return text.trim();
			}
		}
		if(widget.getMetadata("href")!=null)
		{
			String text=(String)widget.getMetadata("href");
			if(text.trim().length()>0)
			{
				return text;
			}
		}
		if(widget.getMetadata("name")!=null)
		{
			String text=(String)widget.getMetadata("name");
			if(text.trim().length()>0)
			{
				return text;
			}
		}
		if(widget.getMetadata("id")!=null)
		{
			String text=(String)widget.getMetadata("id");
			if(text.trim().length()>0)
			{
				return text;
			}
		}
		if(widget.getLocation()!=null)
		{
			return widget.getLocation().toString();
		}
		if(widget.getLocationArea()!=null)
		{
			return widget.getLocationArea().toString();
		}
		return null;
	}

	private boolean addText(String text)
	{
		log(filename, text);
		return true;
	}

	private boolean addComment(String text)
	{
		log(filename, "// "+text);
		return true;
	}

	private void log(String logFilename, String message)
	{
		writeLine(logFilename, message, true);
	}

	/**
	 * Write one line of text to filename
	 * @param filename
	 * @param text
	 * @param append
	 */
	private void writeLine(String filename, String text, boolean append)
	{
		String logMessage = text + "\r\n";
		File file = new File(filename);
		try
		{
			FileOutputStream o = new FileOutputStream(file, append);
			o.write(logMessage.getBytes());
			o.close();
		}
		catch (Exception e)
		{
			// Don't care if fails
		}
	}

	private String getDurationTime(long duration)
	{
		int seconds = (int) duration / 1000;
		int minutes = (int) seconds / 60;
		int remainingSeconds = (int) seconds % 60;
		String secondsStr;
		if (remainingSeconds < 10)
		{
			secondsStr = "0" + remainingSeconds;
		} else
		{
			secondsStr = "" + remainingSeconds;
		}
		return minutes + ":" + secondsStr;
	}
}
