package plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import scout.AppState;
import scout.Path;
import scout.StateController;
import scout.Widget;
import scout.Widget.WidgetSubtype;
import scout.Widget.WidgetType;

public class SessionReports
{
	private String filename = null;

	public void generateReport()
	{
		String productVersion = StateController.getProductVersion();

		// Make sure we have a reports folder
		File file = new File("reports/" + StateController.getProduct());
		file.mkdirs();

		List<Path> paths = StateController.getStateTree().getPaths(productVersion);

		if (paths.size() > 0)
		{
			StateController.displayMessage("Generating " + paths.size() + " session reports");
		}
		else
		{
			StateController.displayMessage("No sessions to report");
		}

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd h-mm-ss");
		for (Path path : paths)
		{
			String dateStr = df.format(path.getCreatedDate());
			filename = "reports/" + StateController.getProduct() + "/Session " + dateStr + ".txt";

			writeLine(filename, "Date: " + dateStr, false);
			log(filename, "Duration: " + getDurationTime(path.getSessionDuration()));
			log(filename, "");
			log(filename, "Steps:");

			if (!addWidgets(path.getWidgets()))
			{
				StateController.displayMessage("Failed to generate report");
				return;
			}
		}
	}

	private boolean addWidgets(List<Widget> widgets)
	{
		for (Widget widget : widgets)
		{
			String comment = widget.getComment();
			if (comment != null && comment.trim().length() > 0)
			{
				addComment(comment);
			}
			if (widget.getWidgetType() == WidgetType.ACTION)
			{
				if (widget.getWidgetSubtype() == WidgetSubtype.LEFT_CLICK_ACTION)
				{
					String text = getWidgetText(widget);
					if (text != null && text.trim().length() > 0)
					{
						addText(StateController.translate("LeftClick") + " \"" + text + "\"");
					} else
					{
						addText(StateController.translate("LeftClick"));
					}
				}
				else if (widget.getWidgetSubtype() == WidgetSubtype.TYPE_ACTION)
				{
					addText(StateController.translate("Type") + " \"" + widget.getText() + "\"");
				}
				else if (widget.getWidgetSubtype() == WidgetSubtype.SELECT_ACTION)
				{
					String text=getWidgetText(widget);
					addText(StateController.translate("Select") + " \"" + text + "\"");
				}
				else if (widget.getWidgetSubtype() == WidgetSubtype.GO_HOME_ACTION)
				{
					addText(StateController.translate("GoHome"));
				}
			}
			AppState state=widget.getNextState();
			if(state!=null)
			{
				List<Widget> allWidgets=state.getNonHiddenWidgets();
				for (Widget w : allWidgets)
				{
					if(w.getWidgetType()==WidgetType.CHECK && w.getValidExpression()!=null)
					{
						addText(StateController.translate("Check") + " \"" + w.getValidExpression() + "\"");
					}
				}
			}
		}
		return true;
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
		if (widget.getMetadata("text") != null)
		{
			String text = (String) widget.getMetadata("text");
			if (text.trim().length() > 0)
			{
				return text.trim();
			}
		}
		if (widget.getMetadata("href") != null)
		{
			String text = (String) widget.getMetadata("href");
			if (text.trim().length() > 0)
			{
				return text;
			}
		}
		if (widget.getMetadata("name") != null)
		{
			String text = (String) widget.getMetadata("name");
			if (text.trim().length() > 0)
			{
				return text;
			}
		}
		if (widget.getMetadata("id") != null)
		{
			String text = (String) widget.getMetadata("id");
			if (text.trim().length() > 0)
			{
				return text;
			}
		}
		if (widget.getLocation() != null)
		{
			return widget.getLocation().toString();
		}
		if (widget.getLocationArea() != null)
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
		log(filename, "// " + text);
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
}
