package plugin;

import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.openqa.selenium.Cookie;

import scout.AppState;
import scout.Path;
import scout.StateController;
import scout.Widget;
import scout.Widget.WidgetSubtype;
import scout.Widget.WidgetType;

public class GenerateJavaSource
{
	public static String[] LOCATORS = {"tag", "class", "type", "name", "id", "value", "href", "text", "placeholder"};
	private String filename = null;

	public void generateReport()
	{
		// Make sure we have a reports folder
		File file = new File("reports/" + StateController.getProduct());
		file.mkdirs();

		File locatorsFile = new File("reports/" + StateController.getProduct()+"/locators");
		locatorsFile.mkdirs();

		Path path = StateController.getStateTree().getLastPerformedPath();
		if(path!=null)
		{
			StateController.displayMessage("Generating Java source code for current/last session");

			DateFormat df = new SimpleDateFormat("yyyy-MM-dd h-mm-ss");

			String dateStr = df.format(path.getCreatedDate());
			filename = "reports/" + StateController.getProduct() + "/Session " + dateStr + ".java";

			writeLine(filename, "System.setProperty(\"webdriver.chrome.driver\", (new File(\"chromedriver.exe\")).getAbsolutePath());", false);
			addText("WebDriver webDriver=new ChromeDriver();");
			addText("webDriver.get(\""+StateController.getHomeLocator()+"\");");
			addText("MultiLocator multiLocator=new MultiLocator(webDriver);");
			addCookies(StateController.getStateTree());

			if (!addWidgets(path.getWidgets()))
			{
				StateController.displayMessage("Failed to generate report");
				return;
			}
			addText("webDriver.quit();"); 
		}
		else
		{
			StateController.displayMessage("No session to report");
		}
	}

	private void addCookies(AppState state)
	{
		Object object=state.getMetadata("cookies");
		if(object!=null)
		{
			Set<Cookie> cookies=(Set<Cookie>)object;
			String cookiesFilename="locators/"+state.getId()+".cookies";
			String cookiesPath="reports/" + StateController.getProduct()+"/"+cookiesFilename;
			saveCookies(cookiesPath, cookies);
			addText("multiLocator.loadCookies(\""+cookiesFilename+"\", webDriver);");
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
					addText(saveLocator(widget)+".click();");
				}
				else if (widget.getWidgetSubtype() == WidgetSubtype.TYPE_ACTION)
				{
					addText(saveLocator(widget)+".sendKeys(\""+widget.getText()+"\");");
				}
				else if (widget.getWidgetSubtype() == WidgetSubtype.SELECT_ACTION)
				{
					String optionText=(String)widget.getMetadata("option_text");
					String optionValue=(String)widget.getMetadata("option_value");
					if(optionValue!=null && optionValue.trim().length()>0 && !widget.isIgnoredMetadata("option_value"))
					{
						addText("(new Select("+saveLocator(widget)+")).selectByValue(\""+optionValue+"\");");
					}
					else if(optionText!=null && optionText.trim().length()>0 && !widget.isIgnoredMetadata("option_text"))
					{
						addText("(new Select("+saveLocator(widget)+")).selectByVisibleText(\""+optionText+"\");");
					}
				}
				else if (widget.getWidgetSubtype() == WidgetSubtype.GO_HOME_ACTION)
				{
					if("yes".equalsIgnoreCase(StateController.getProductProperty("home_clear_browser", "yes")))
					{
						addText("multiLocator.clearLocalStorage();");
					}
					addText("webDriver.get(\""+StateController.getHomeLocator()+"\");");
				}
			}

			// Add checks
			AppState state=widget.getNextState();
			if(state!=null)
			{
				if(!state.isHome())
				{
					addCookies(state);
				}
				List<Widget> allWidgets=state.getNonHiddenWidgets();
				for (Widget w : allWidgets)
				{
					if(w.getWidgetType()==WidgetType.CHECK && w.getValidExpression()!=null && !w.getValidExpression().contains("\n"))
					{
						saveLocator(w);
						String propertiesFilename="locators/"+w.getId()+".properties";
						addText("assertTrue(multiLocator.checkElement(\""+propertiesFilename+"\", \""+w.getValidExpression()+"\"));");
					}
				}
			}
		}
		return true;
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

	private String saveLocator(Widget widget)
	{
		String propertiesFilename="locators/"+widget.getId()+".properties";
		File file = new File("reports/" + StateController.getProduct()+"/"+propertiesFilename);

		Properties p=new Properties();

		for(String locator:LOCATORS)
		{
			String locatorValue=(String)widget.getMetadata(locator);
			if(locatorValue!=null && locatorValue.length()>0)
			{
				p.setProperty(locator, locatorValue);
			}
		}
		
		Rectangle area=widget.getLocationArea();
		if(area!=null)
		{
//			p.setProperty("x", ""+(int)area.getX());
//			p.setProperty("y", ""+(int)area.getY());
			p.setProperty("width", ""+(int)area.getWidth());
			p.setProperty("height", ""+(int)area.getHeight());
		}
		
		saveProperties(file, p);

		return "multiLocator.findElement(\""+propertiesFilename+"\")";
	}
/*
	private boolean saveProperties(File file, Properties p)
	{
		try
		{
			FileOutputStream out = new FileOutputStream(file);
			if(out != null)
			{
				p.store(out, null);
				out.close();
			}

			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
*/
	private boolean saveProperties(File file, Properties properties)
	{
		try
		{
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
			for(Object keyObject:properties.keySet())
			{
				String key=(String)keyObject;
				String value=(String)properties.getProperty(key);
				out.write(key+"="+value);
				out.newLine();
			}
			out.close();
		}
		catch(Exception e)
		{
			return false;
		}
		return true;
	}

	/**
	 * Save cookies to file
	 * @param filepath - Path to cookie file
	 * @param cookies
	 * @return true if saved
	 */
	private boolean saveCookies(String filepath, Set<Cookie> cookies)
	{
		try
		{
			return saveObject(filepath, cookies);
		}
		catch (Throwable e)
		{
			return false;
		}
	}

	private boolean saveObject(String filepath, Object object)
	{
		try
		{
			FileOutputStream fileOut = new FileOutputStream(filepath);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(object);
			out.close();
			fileOut.close();
			fileOut.getFD().sync();
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
}
