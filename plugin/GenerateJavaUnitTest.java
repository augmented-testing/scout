package plugin;

import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.openqa.selenium.Cookie;

import scout.AppState;
import scout.StateController;
import scout.Widget;
import scout.Widget.WidgetSubtype;
import scout.Widget.WidgetType;

public class GenerateJavaUnitTest
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

		filename = "reports/" + StateController.getProduct() + "/JavaUnitTest.java";
		StateController.displayMessage("Generating unit test to: "+filename);

		writeLine(filename, "class JavaUnitTest {", false);
		addText("private WebDriver openWebDriver() {");
		addText("System.setProperty(\"webdriver.chrome.driver\", (new File(\"chromedriver.exe\")).getAbsolutePath());");
		addText("WebDriver webDriver=new ChromeDriver();");
		addText("webDriver.get(\""+StateController.getHomeLocator()+"\");");
		addText("return webDriver;");
		addText("}");
		addText("");

		int no=1;
		List<AppState> leaves=StateController.getStateTree().getLeaves();
		for(AppState leaf:leaves)
		{
			List<Widget> widgets=StateController.getStateTree().getPathToState(leaf);
			if(widgets!=null)
			{
				addText("@Test");
				addText("void test"+no+"() {");
				addText("WebDriver webDriver=openWebDriver();");
				addText("MultiLocator multiLocator=new MultiLocator(webDriver);");
				addCookies(StateController.getStateTree());

				if (!addWidgets(widgets))
				{
					StateController.displayMessage("Failed to generate test");
					return;
				}
				addText("webDriver.quit();"); 
				addText("}");
				addText("");
			}
			no++;
		}
		
		addText("}");
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

	private Widget getLocatorWidget(List<Widget> pathActions, Widget widget)
	{
		AppState stateTree=StateController.getStateTree();
  	List<Widget> clonesAndMutations=stateTree.getClonesAndMutations(pathActions);
		for(Widget cloneWidget:clonesAndMutations)
		{
			String locatorName=(String)cloneWidget.getMetadata("locator_name");
			if(locatorName!=null)
			{
				return cloneWidget;
			}
		}
		// None found - widget is the locator
		return widget;
	}
	
	private boolean addWidgets(List<Widget> widgets)
	{
		List<Widget> pathActions=new ArrayList<Widget>();
		AppState state=StateController.getStateTree();
		for (Widget widget : widgets)
		{
			pathActions.add(widget);
	  	Widget locatorWidget=getLocatorWidget(pathActions, widget);
	  	locatorWidget.putMetadata("locator_name", widget.getId());

			String comment = widget.getComment();
			if (comment != null && comment.trim().length() > 0)
			{
				addComment(comment);
			}

			if (widget.getWidgetType() == WidgetType.ACTION)
			{
				if (widget.getWidgetSubtype() == WidgetSubtype.LEFT_CLICK_ACTION)
				{
					addText(saveLocator(locatorWidget)+".click();");
				}
				else if (widget.getWidgetSubtype() == WidgetSubtype.TYPE_ACTION)
				{
					addText(saveLocator(locatorWidget)+".sendKeys(\""+widget.getText()+"\");");
				}
				else if (widget.getWidgetSubtype() == WidgetSubtype.SELECT_ACTION)
				{
					String optionText=(String)widget.getMetadata("option_text");
					String optionValue=(String)widget.getMetadata("option_value");
					if(optionValue!=null && optionValue.trim().length()>0 && !widget.isIgnoredMetadata("option_value"))
					{
						addText("(new Select("+saveLocator(locatorWidget)+")).selectByValue(\""+optionValue+"\");");
					}
					else if(optionText!=null && optionText.trim().length()>0 && !widget.isIgnoredMetadata("option_text"))
					{
						addText("(new Select("+saveLocator(locatorWidget)+")).selectByVisibleText(\""+optionText+"\");");
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
			AppState nextState=widget.getNextState();
			if(nextState!=null)
			{
				if(!nextState.isHome())
				{
					addCookies(nextState);
				}
				List<Widget> allWidgets=nextState.getNonHiddenWidgets();
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
			state=nextState;
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
