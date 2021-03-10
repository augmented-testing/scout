package plugin;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;

import scout.Action;
import scout.AppState;
import scout.Correction;
import scout.DragDropAction;
import scout.DragStartAction;
import scout.GoHomeAction;
import scout.LeftClickAction;
import scout.LongClickAction;
import scout.MouseScrollAction;
import scout.MoveAction;
import scout.Neighbor;
import scout.PluginController;
import scout.Scout;
import scout.StateController;
import scout.StateController.SessionState;
import scout.TypeAction;
import scout.Widget;
import scout.Widget.WidgetStatus;
import scout.Widget.WidgetSubtype;
import scout.Widget.WidgetType;
import scout.Widget.WidgetVisibility;
import scout.WidgetMatch;

public class SeleniumPlugin
{
	private static WebDriver webDriver=null;
	private static BufferedImage latestScreenCapture=null;
	private static boolean isCreatingCapture=false;
	private static AppState currentState=null;
	private static Widget widgetToRemove=null;
	private static long widgetRemoveUntilTime=0;
	private static Widget insertBeforeWidget=null;
	private static boolean isSwitchingFrame=false;
	private static long startTypeTime=0;
	private static Widget insertWidgetAfterChange=null;
	private static Widget performWidgetAfterChange=null;
	private static Widget widgetToMove=null;
	
	private static String[] seleniumKeyTag=null;
	private static String[] seleniumKeyValue=null;

	public void startSession()
	{
		// Initialize widget identifiers
		WidgetIdentifier.initIdentifiers();

		currentState=StateController.getCurrentState();
		StateController.displayMessage("Starting Browser", 10000);
		webDriver=getWebDriver(StateController.getProductView());
		if(webDriver!=null)
		{
	  	int width=StateController.getProductViewWidth();
	  	int height=StateController.getProductViewHeight();
	  	webDriver.manage().window().setSize(new Dimension(width, height));
			webDriver.get(StateController.getHomeLocator());
			loadCookiesForCurrentState();
			StateController.setSessionState(SessionState.RUNNING);
			StateController.displayMessageHide();
		}
		else
		{
			StateController.setSessionState(SessionState.STOPPED);
			StateController.displayMessage("Failed to start browser");
		}
	}

	public void stopSession()
	{
		StateController.displayMessage("Stopping Browser");
		if(webDriver!=null)
		{
			webDriver.quit();
		}
		StateController.setDoNotRecordNextAction(false);
		StateController.displayMessageHide();
	}

	public void changeState()
	{
		currentState=StateController.getCurrentState();
		loadCookiesForCurrentState();
	}

	public void storeHomeState()
	{
		if(currentState!=null)
		{
			saveCookies();
		}
	}
	
	public BufferedImage getCapture()
	{
		if(StateController.isOngoingSession())
		{
			return createCaptureAndReplaceWidgets();
		}
		else
		{
			return null;
		}
	}

	private void loadCookiesForCurrentState()
	{
		if(currentState.getMetadata("cookies")!=null)
		{
			loadCookies();
		}
	}

	public void performAction(Action action)
	{
		if(!StateController.isRunningSession() || action.isToolbarAction())
		{
			// Only perform actions during a running session and not actions aimed for the toolbar
			return;
		}
		try
		{
			insertWidgetAfterChange=null;

			if(action instanceof MouseScrollAction)
			{
				MouseScrollAction mouseScrollAction=(MouseScrollAction)action;
				StateController.setSelectedWidgetNo(StateController.getSelectedWidgetNo()+mouseScrollAction.getRotation());
				return;
			}

			if(action instanceof DragStartAction)
			{
				DragStartAction dragStartAction=(DragStartAction)action;
				Widget locatedWidget=StateController.getWidgetAt(currentState, dragStartAction.getLocation());
				if(locatedWidget!=null)
				{
					widgetToMove=locatedWidget;
				}
			}

			if(action instanceof DragDropAction)
			{
				DragDropAction dragDropAction=(DragDropAction)action;
				Widget locatedWidget=StateController.getWidgetAt(currentState, dragDropAction.getLocation());
				if(locatedWidget!=null && widgetToMove!=null && locatedWidget.getWidgetType()==widgetToMove.getWidgetType() && locatedWidget.getWidgetSubtype()==widgetToMove.getWidgetSubtype())
				{
					copyWidgetMetadata(locatedWidget, widgetToMove);
				}
				widgetToMove=null;
			}

			if(action instanceof TypeAction)
			{
				TypeAction typeAction=(TypeAction)action;
				KeyEvent keyEvent=typeAction.getKeyEvent();
				int keyCode = keyEvent.getKeyCode();
				char keyChar = keyEvent.getKeyChar();

				if(keyCode==KeyEvent.VK_ENTER)
				{
					// Send keyboardInput to selected widget (if any)
					StateController.addKeyboardInput("[ENTER]");
					performTypeAction(typeAction.getLocation());
				}
				else if(keyCode==KeyEvent.VK_DELETE || keyCode==KeyEvent.VK_BACK_SPACE)
				{
					if(StateController.getKeyboardInput().length()>0)
					{
						StateController.removeLastKeyboardInput();
					}
					else
					{
						// Remove selected widget
						Widget locatedWidget=StateController.getWidgetAt(currentState, typeAction.getLocation());
						if(locatedWidget!=null)
						{
							StateController.displayMessage("Press y to remove or c to cut the selected widget");
							// 5 seconds to press y
							widgetToRemove=locatedWidget;
							widgetRemoveUntilTime=System.currentTimeMillis()+5000;
						}
					}
				}
				else if(keyCode==KeyEvent.VK_INSERT)
				{
					if(insertBeforeWidget!=null)
					{
						StateController.displayMessage("Insert aborted");
						insertBeforeWidget=null;
					}
					else
					{
						Widget locatedWidget=StateController.getWidgetAt(currentState, typeAction.getLocation());
						if(locatedWidget!=null)
						{
							StateController.displayMessage("Click to insert the next action");
							insertBeforeWidget=locatedWidget;
						}
					}
				}
				else if(keyChar=='y' && widgetToRemove!=null && widgetRemoveUntilTime>System.currentTimeMillis())
				{
					currentState.removeWidget(widgetToRemove);
					widgetToRemove=null;
					StateController.displayMessageHide();
				}
				else if(keyChar=='c' && widgetToRemove!=null && widgetRemoveUntilTime>System.currentTimeMillis())
				{
					currentState.cutWidget(widgetToRemove);
					widgetToRemove=null;
					StateController.displayMessageHide();
				}
				else if(keyCode==KeyEvent.VK_ESCAPE)
				{
					if(StateController.getKeyboardInput().length()>0)
					{
						StateController.clearKeyboardInput();
					}
				}
				else if(keyCode==KeyEvent.VK_V && (keyEvent.isControlDown() || keyEvent.isMetaDown()))
				{
					String text=getClip();
					if(StateController.getKeyboardInput().length()==0)
					{
						// First typed char - remember the time
						startTypeTime=System.currentTimeMillis();
					}
					StateController.addKeyboardInput(text);
				}
				else if(keyCode==KeyEvent.VK_C && (keyEvent.isControlDown() || keyEvent.isMetaDown()))
				{
					if(StateController.getKeyboardInput().length()==0)
					{
						Widget locatedWidget=StateController.getWidgetAt(currentState, typeAction.getLocation());
						if(locatedWidget!=null)
						{
							String text=(String)locatedWidget.getMetadata("text");
							if(locatedWidget.getValidExpression()!=null)
							{
								setClip(locatedWidget.getValidExpression());
							}
							else if(locatedWidget.getComment()!=null)
							{
								setClip(locatedWidget.getComment());
							}
							else if(text!=null)
							{
								setClip(text);
							}
						}
					}
					else
					{
						setClip(StateController.getKeyboardInput());
					}
				}
				else if(keyCode==KeyEvent.VK_SPACE)
				{
					StateController.addKeyboardInput(" ");
				}
				else if(keyCode==KeyEvent.VK_UP)
				{
					StateController.setSelectedWidgetNo(StateController.getSelectedWidgetNo()-1);
				}
				else if(keyCode==KeyEvent.VK_DOWN)
				{
					StateController.setSelectedWidgetNo(StateController.getSelectedWidgetNo()+1);
				}
				else
				{
					if(StateController.getKeyboardInput().length()==0)
					{
						// First typed char - remember the time
						startTypeTime=System.currentTimeMillis();
					}
					StateController.addKeyboardInput(getKeyText(keyChar));
				}
			}

			if(action instanceof GoHomeAction)
			{
				if("yes".equalsIgnoreCase(StateController.getProductProperty("home_restart_browser", "no")))
				{
			    stopSession();
			    startSession();
				}
				else if("yes".equalsIgnoreCase(StateController.getProductProperty("home_clear_browser", "yes")))
				{
					clearLocalStorage();
					webDriver.get(StateController.getHomeLocator());
				}
				else
				{
					webDriver.get(StateController.getHomeLocator());
				}
				Widget goHomeWidget=new Widget();
				goHomeWidget.setWidgetType(WidgetType.ACTION);
				goHomeWidget.setWidgetSubtype(WidgetSubtype.GO_HOME_ACTION);
				goHomeWidget.setLocationArea(new Rectangle(10, 10, 80, 40));
				insertWidget(goHomeWidget, StateController.getStateTree());
				return;
			}

			if(action instanceof LeftClickAction || action instanceof LongClickAction)
			{
				// Left mouse click on a widget
				MoveAction moveAction=(MoveAction)action;
				Widget locatedWidget=StateController.getWidgetAt(currentState, moveAction.getLocation());
				if(locatedWidget!=null)
				{
					// Located a widget
					StateController.setSelectedWidgetNo(0);
					if(locatedWidget.getWidgetType()==WidgetType.ACTION)
					{
						if(locatedWidget.getWidgetSubtype()==WidgetSubtype.TYPE_ACTION)
						{
							if(StateController.getKeyboardInput().length()>0)
							{
								// We have keyboard input to type
								performTypeAction(moveAction.getLocation());
							}
							else
							{
								// Click on a type action
								WebElement locatedElement=findWebElement(locatedWidget);
								if(locatedElement!=null && locatedWidget.getText()!=null && locatedWidget.getText().trim().length()>0 && locatedWidget.getWidgetVisibility()!=WidgetVisibility.HIDDEN)
								{
									// Type text
									if(locatedWidget.getWidgetVisibility()==WidgetVisibility.SUGGESTION && !isAllowedToInsert())
									{
										// Not allowed to insert
										return;
									}

									typeSelenium(locatedElement, locatedWidget.getText());

									if(locatedWidget.getWidgetVisibility()==WidgetVisibility.SUGGESTION)
									{
										// Make the suggestion visible
										insertWidgetAfterChange(locatedWidget);
									}
									else
									{
										// Perform the type action
										performWidgetAfterChange(locatedWidget);
									}
								}
							}
						}
						else if(locatedWidget.getWidgetSubtype()==WidgetSubtype.LEFT_CLICK_ACTION || locatedWidget.getWidgetSubtype()==WidgetSubtype.LONG_CLICK_ACTION || locatedWidget.getWidgetSubtype()==WidgetSubtype.MOVE_ACTION)
						{
							// Click on a left click action
							if(StateController.getKeyboardInput().length()>0)
							{
								// Keyboard input on visible widget - add a comment
								locatedWidget.setComment(StateController.getKeyboardInput().trim());
								StateController.clearKeyboardInput();
							}

							Integer frameNo=(Integer)locatedWidget.getMetadata("frame_no");
							if(frameNo!=null)
							{
								isSwitchingFrame=true;
								webDriver.switchTo().frame(frameNo);
							}

							try
							{
								WebElement locatedElement=findWebElement(locatedWidget);
								if(locatedElement!=null)
								{
									if((locatedWidget.getWidgetVisibility()==WidgetVisibility.HIDDEN || locatedWidget.getWidgetVisibility()==WidgetVisibility.SUGGESTION) && !isAllowedToInsert())
									{
										// Not allowed to insert
										return;
									}

									String href=(String)locatedWidget.getMetadata("href");
									if((action instanceof LongClickAction || locatedWidget.getWidgetSubtype()==WidgetSubtype.LONG_CLICK_ACTION) && href!=null)
									{
										// Go to href instead of clicking on the link/button
										webDriver.get(href);
										locatedWidget.setWidgetSubtype(WidgetSubtype.LONG_CLICK_ACTION);
									}
									else if(action instanceof LeftClickAction)
									{
										if(locatedWidget.getWidgetSubtype()==WidgetSubtype.LEFT_CLICK_ACTION)
										{
											// Click element
											clickWebElement(locatedElement);
										}
										else if(locatedWidget.getWidgetSubtype()==WidgetSubtype.MOVE_ACTION)
										{
											// Move to element
											moveToWebElement(locatedElement);
										}
									}

									if(locatedWidget.getWidgetVisibility()==WidgetVisibility.HIDDEN || locatedWidget.getWidgetVisibility()==WidgetVisibility.SUGGESTION)
									{
										insertWidgetAfterChange(locatedWidget);
									}
									else
									{
										// Perform action on button or link
										performWidgetAfterChange(locatedWidget);
									}
								}
							}
							catch(Exception e)
							{
							}

							if(frameNo!=null)
							{
								webDriver.switchTo().defaultContent();
								isSwitchingFrame=false;
							}
						}
						else if(locatedWidget.getWidgetSubtype()==WidgetSubtype.SELECT_ACTION)
						{
							if(StateController.getKeyboardInput().length()>0)
							{
								// Keyboard input on visible widget - add a comment
								locatedWidget.setComment(StateController.getKeyboardInput().trim());
								StateController.clearKeyboardInput();
							}

							WebElement locatedElement=findWebElement(locatedWidget);
							if(locatedElement!=null)
							{
								// Select option
								if((locatedWidget.getWidgetVisibility()==WidgetVisibility.HIDDEN || locatedWidget.getWidgetVisibility()==WidgetVisibility.SUGGESTION) && !isAllowedToInsert())
								{
									// Not allowed to insert
									return;
								}
								
								Select select=new Select(locatedElement);
								String optionText=(String)locatedWidget.getMetadata("option_text");
								String optionValue=(String)locatedWidget.getMetadata("option_value");
								if(optionValue!=null && optionValue.trim().length()>0 && !locatedWidget.isIgnoredMetadata("option_value"))
								{
									select.selectByValue(optionValue);
								}
								else if(optionText!=null && optionText.trim().length()>0 && !locatedWidget.isIgnoredMetadata("option_text"))
								{
									select.selectByVisibleText(optionText);
								}
								else
								{
									StateController.displayMessage("Option not found");
									return;
								}

								if(locatedWidget.getWidgetVisibility()==WidgetVisibility.HIDDEN || locatedWidget.getWidgetVisibility()==WidgetVisibility.SUGGESTION)
								{
									insertWidgetAfterChange(locatedWidget);
								}
								else
								{
									// Perform action on button or link
									performWidgetAfterChange(locatedWidget);
								}
							}
						}
						else if(locatedWidget.getWidgetSubtype()==WidgetSubtype.GO_HOME_ACTION)
						{
							if(StateController.getKeyboardInput().length()>0 && locatedWidget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
							{
								// Keyboard input on visible widget - add a comment
								locatedWidget.setComment(StateController.getKeyboardInput().trim());
								StateController.clearKeyboardInput();
							}
							else
							{
								// Click on a home action
								if((locatedWidget.getWidgetVisibility()==WidgetVisibility.HIDDEN || locatedWidget.getWidgetVisibility()==WidgetVisibility.SUGGESTION) && !isAllowedToInsert())
								{
									// Not allowed to insert
									return;
								}

								if("yes".equalsIgnoreCase(StateController.getProductProperty("home_restart_browser", "no")))
								{
							    stopSession();
							    startSession();
								}
								else if("yes".equalsIgnoreCase(StateController.getProductProperty("home_clear_browser", "yes")))
								{
									clearLocalStorage();
									webDriver.get(StateController.getHomeLocator());
								}
								else
								{
									webDriver.get(StateController.getHomeLocator());
								}

								if(locatedWidget.getWidgetVisibility()==WidgetVisibility.HIDDEN || locatedWidget.getWidgetVisibility()==WidgetVisibility.SUGGESTION)
								{
									insertWidgetAfterChange(locatedWidget);
								}
								else
								{
									// Perform action on button or link
									performWidgetAfterChange(locatedWidget);
								}
							}
						}
					}
					else if(locatedWidget.getWidgetType()==WidgetType.CHECK)
					{
						if(StateController.getKeyboardInput().length()>0 && locatedWidget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
						{
							if(StateController.getKeyboardInput().indexOf("{")>=0 && StateController.getKeyboardInput().indexOf("}")>=0)
							{
								// An expression
								locatedWidget.setValidExpression(StateController.getKeyboardInput());
							}
							else
							{
								// Report an issue
								createIssue(locatedWidget, StateController.getKeyboardInput());
							}
							StateController.clearKeyboardInput();
						}
						else if(StateController.getKeyboardInput().length()==0 && locatedWidget.getWidgetVisibility()==WidgetVisibility.VISIBLE && locatedWidget.getWidgetStatus()==WidgetStatus.LOCATED)
						{
							if(locatedWidget.getValidExpression()!=null)
							{
								Widget matchingWidget=(Widget)locatedWidget.getMetadata("matching_widget");
								if(matchingWidget!=null)
								{
									// Auto correct check
									String matchingText=(String)matchingWidget.getMetadata("text");
									if(matchingText!=null)
									{
										// Create a correction
										Correction correction=new Correction();
										correction.putMetadata("from_valid_expression", locatedWidget.getValidExpression());

										// Perform change
										assignValidExpression(matchingWidget, locatedWidget);

										// Add a correction
										correction.putMetadata("to_valid_expression", locatedWidget.getValidExpression());
										addCorrection(locatedWidget, correction);
									}
								}
							}
						}
						else if(locatedWidget.getWidgetVisibility()!=WidgetVisibility.VISIBLE)
						{
							locatedWidget.setWidgetVisibility(WidgetVisibility.VISIBLE);
							locatedWidget.setCreatedBy(StateController.getTesterName());
							locatedWidget.setCreatedDate(new Date());
							locatedWidget.setComment(null);
							locatedWidget.setCreatedProductVersion(StateController.getProductVersion());
						}
					}
					else if(locatedWidget.getWidgetType()==WidgetType.ISSUE)
					{
						if(locatedWidget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
						{
							// Convert issue into check
							locatedWidget.setWidgetType(WidgetType.CHECK);
							locatedWidget.setResolvedBy(StateController.getTesterName());
							locatedWidget.setResolvedDate(new Date());
							locatedWidget.setResolvedProductVersion(StateController.getProductVersion());
							Widget matchingWidget=(Widget)locatedWidget.getMetadata("matching_widget");
							if(matchingWidget!=null)
							{
								assignValidExpression(matchingWidget, locatedWidget);
							}
							StateController.clearKeyboardInput();
						}
					}
				}
				return;
			}
		}
		catch (Exception e)
		{
			return;
		}
		
		return;
	}

	private void performTypeAction(Point location)
	{
		Widget existingTypeWidget=getTypeWidget(location, StateController.getKeyboardInput());
		if(existingTypeWidget!=null)
		{
			// Same text entered as existing widget - perform that widget again
			WebElement locatedElement=findWebElement(existingTypeWidget);
			if(locatedElement!=null)
			{
				typeSelenium(locatedElement, StateController.getKeyboardInput());
				StateController.clearKeyboardInput();
				performWidgetAfterChange(existingTypeWidget);
				return;
			}
		}
		else
		{
			// Not an identical existing widget
			existingTypeWidget=getTypeWidget(location);
			if(existingTypeWidget!=null)
			{
				// There is a type widget
				Widget insertTypeWidget=new Widget(existingTypeWidget);
				// Set the new text
				insertTypeWidget.setText(StateController.getKeyboardInput());
				// Find the element
				WebElement locatedElement=findWebElement(insertTypeWidget);
				if(locatedElement!=null)
				{
					if(!isAllowedToInsert())
					{
						// Not allowed to insert
						return;
					}
					// Type the text
					typeSelenium(locatedElement, StateController.getKeyboardInput());
					StateController.clearKeyboardInput();
					// Insert the new widget
					insertWidgetAfterChange(insertTypeWidget);
				}
			}
			else
			{
				// No visible or suggested type action found at that location
				Widget locatedWidget=StateController.getWidgetAt(currentState, location);
				if(locatedWidget!=null)
				{
					if(locatedWidget.getWidgetType()==WidgetType.ACTION && locatedWidget.getWidgetSubtype()==WidgetSubtype.TYPE_ACTION && locatedWidget.getWidgetVisibility()==WidgetVisibility.HIDDEN)
					{
						// Found a hidden type action
						WebElement locatedElement=findWebElement(locatedWidget);
						if(locatedElement!=null)
						{
							if(!isAllowedToInsert())
							{
								// Not allowed to insert
								return;
							}
							typeSelenium(locatedElement, StateController.getKeyboardInput());
							locatedWidget.setText(StateController.getKeyboardInput());
							StateController.clearKeyboardInput();
							insertWidgetAfterChange(locatedWidget);
						}
					}
					else if(locatedWidget.getWidgetType()==WidgetType.ACTION)
					{
						if(StateController.getKeyboardInput().length()>0 && locatedWidget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
						{
							// Keyboard input on visible widget - add a comment
							if(StateController.getKeyboardInput().endsWith("[ENTER]"))
							{
								StateController.removeLastKeyboardInput();
							}
							locatedWidget.setComment(StateController.getKeyboardInput().trim());
							StateController.clearKeyboardInput();
						}
					}
					else if(locatedWidget.getWidgetType()==WidgetType.CHECK)
					{
						if(StateController.getKeyboardInput().length()>0 && locatedWidget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
						{
							// Report an issue
							if(StateController.getKeyboardInput().endsWith("[ENTER]"))
							{
								StateController.removeLastKeyboardInput();
							}
							if(StateController.getKeyboardInput().indexOf("{")>=0 && StateController.getKeyboardInput().indexOf("}")>=0)
							{
								// An expression
								locatedWidget.setValidExpression(StateController.getKeyboardInput());
							}
							else
							{
								// Report an issue
								createIssue(locatedWidget, StateController.getKeyboardInput());
							}
							StateController.clearKeyboardInput();
						}
					}
				}
			}
		}
	}

	private void createIssue(Widget widget, String reportedText)
	{
		widget.setWidgetType(WidgetType.ISSUE);
		widget.setWidgetVisibility(WidgetVisibility.VISIBLE);
		widget.setReportedText(reportedText);
		widget.setReportedBy(StateController.getTesterName());
		widget.setReportedDate(new Date());
		widget.setReportedProductVersion(StateController.getProductVersion());
		Widget matchingWidget=(Widget)widget.getMetadata("matching_widget");
		if(matchingWidget!=null)
		{
			String matchingText=(String)matchingWidget.getMetadata("text");
			if(matchingText!=null)
			{
				widget.setValidExpression("{text} = "+matchingText);
			}
		}
		if(startTypeTime>0)
		{
			long deltaTime=System.currentTimeMillis()-startTypeTime;
			startTypeTime=0;
			widget.putMetadata("report_issue_time", deltaTime);
		}
	}
	
	/**
	 * @param typeAction
	 * @return An existing type widget at location and with the same text or null
	 */
	private Widget getTypeWidget(Point location, String text)
	{
		List<Widget> locatedWidgets=StateController.getWidgetsAt(location);
		for(Widget locatedWidget:locatedWidgets)
		{
			if(locatedWidget.getWidgetType()==WidgetType.ACTION && locatedWidget.getWidgetSubtype()==WidgetSubtype.TYPE_ACTION && locatedWidget.getWidgetVisibility()!=WidgetVisibility.HIDDEN)
			{
				// Found a non hidden type action
				if(text!=null && text.equals(locatedWidget.getText()))
				{
					// Same text
					return locatedWidget;
				}
			}
		}
		return null;
	}

	/**
	 * @param location
	 * @return An existing type widget at location or null
	 */
	private Widget getTypeWidget(Point location)
	{
		List<Widget> locatedWidgets=StateController.getWidgetsAt(location);
		for(Widget locatedWidget:locatedWidgets)
		{
			if(locatedWidget.getWidgetType()==WidgetType.ACTION && locatedWidget.getWidgetSubtype()==WidgetSubtype.TYPE_ACTION && locatedWidget.getWidgetVisibility()!=WidgetVisibility.HIDDEN)
			{
				// Found a non hidden type action
				return locatedWidget;
			}
		}
		return null;
	}

	/**
	 * Verify that visible widgets are still valid and replace hidden widgets
	 */
	private synchronized void verifyAndReplaceWidgets(List<Widget> availableWidgets)
	{
		try
		{
			if(availableWidgets==null)
			{
				return;
			}
			List<Widget> hiddenAvailableWidgets=new ArrayList<Widget>();
			List<Widget> nonHiddenWidgets=currentState.getNonHiddenWidgets();
			for(Widget widget:availableWidgets)
			{
				hiddenAvailableWidgets.add(widget);
			}
			for(Widget widget:nonHiddenWidgets)
			{
				if(currentState!=StateController.getCurrentState())
				{
					// The state has been changed - abort
					return;
				}

				if(widget.getWidgetSubtype()==WidgetSubtype.GO_HOME_ACTION)
				{
					// The Go Home widget is always located
					widget.setWidgetStatus(WidgetStatus.LOCATED);
					widget.putMetadata("match_percent", 100);
					widget.putMetadata("match_score", 1000);
				}
				else
				{
					long start=System.currentTimeMillis();
					WidgetMatch widgetMatch=findBestMatchingWidget(widget, availableWidgets);
					if(widgetMatch!=null)
					{
						// Found a matching widget
						Widget matchingWidget=widgetMatch.getWidget2();
						widget.putMetadata("matching_widget", matchingWidget);
						widget.putMetadata("match_percent", widgetMatch.getMatchPercent());
						widget.putMetadata("match_score", widgetMatch.getScore());
						
						long matchTime=System.currentTimeMillis()-start;
						widget.putMetadata("match_time", matchTime);
						widget.putMetadata("execute_time", matchingWidget.getMetadata("execute_time"));

						if(widgetMatch.getMatchPercent()==100)
						{
							hiddenAvailableWidgets.remove(widget);

							if(widget.getValidExpression()!=null)
							{
								// There is an expression to evaluate
								if(matchingWidget.isValid(widget.getValidExpression()))		// evaluateExpression
								{
									// Expression is valid
									widget.setWidgetStatus(WidgetStatus.VALID);
								}
								else
								{
									// Expression not valid
									widget.setWidgetStatus(WidgetStatus.LOCATED);
								}
							}
							else
							{
								widget.setWidgetStatus(WidgetStatus.LOCATED);
							}
						}
						else
						{
							// Could not locate widget
							widget.setWidgetStatus(WidgetStatus.UNLOCATED);
						}
					}
					else
					{
						// Could not locate widget
						widget.setWidgetStatus(WidgetStatus.UNLOCATED);
						widget.removeMetadata("matching_widget");
						widget.putMetadata("match_percent", 0);
						widget.putMetadata("match_score", 0);
					}
				}
			}

			currentState.replaceHiddenWidgets(hiddenAvailableWidgets, "SeleniumPlugin");
		}
		catch(Exception e)
		{
		}
	}

	/**
	 * @param widget
	 * @param availableWidgets
	 * @return The best matching widget in availableWidgets
	 */
	private WidgetMatch findBestMatchingWidget(Widget widget, List<Widget> availableWidgets)
	{
		int bestScore=0;
		WidgetMatch bestWidgetMatch=null;
		
		for(Widget availableWidget:availableWidgets)
		{
			WidgetMatch widgetMatch=matchScore(widget, availableWidget, availableWidgets);
			if(widgetMatch!=null)
			{
				if(widgetMatch.getScore()>bestScore)
				{
					bestScore=widgetMatch.getScore();
					bestWidgetMatch=widgetMatch;
				}
			}
		}

		if(bestScore<100)
		{
			// Match is not good enough
			return null;
		}

		return bestWidgetMatch;
	}

	/**
	 * @param widget
	 * @param availableElements
	 * @return The best matching element in availableElements
	 */
/*
	private WebElement findBestMatchingElement(Widget widget, List<WebElement> availableElements)
	{
		int bestScore=0;
		WebElement bestWidget=null;

		// Get the corresponding widgets
		List<Widget> availableWidgets=getWidgets(availableElements);

		for(int i=0; i<availableElements.size(); i++)
		{
			WebElement availableElement=availableElements.get(i);
			Widget availableWidget=getWidgetByIndex(availableWidgets, i);
			if(availableWidget!=null)
			{
				WidgetMatch widgetMatch=matchScore(widget, availableWidget, availableWidgets);
				if(widgetMatch!=null)
				{
					if(widgetMatch.getScore()>bestScore)
					{
						bestScore=widgetMatch.getScore();
						bestWidget=availableElement;
					}
				}
			}
		}

		if(bestScore<100)
		{
			// Match is not good enough
			return null;
		}
		
		return bestWidget;
	}
*/
/*
	private Widget getWidgetByIndex(List<Widget> widgets, int index)
	{
		for(Widget widget:widgets)
		{
			Long i=(Long)widget.getMetadata("index");
			if(index==i)
			{
				return widget;
			}
		}
		return null;
	}
*/

	/**
	 * Calculate a weighted match percent
	 * @param widget1
	 * @param widget2
	 * @return A match or null
	 */
	private WidgetMatch matchScore(Widget widget1, Widget widget2, List<Widget> availableWidgets)
	{
		String tag1=(String)widget1.getMetadata("tag");
		String className1=(String)widget1.getMetadata("class");
		String type1=(String)widget1.getMetadata("type");
		String name1=(String)widget1.getMetadata("name");
		String id1=(String)widget1.getMetadata("id");
		String href1=(String)widget1.getMetadata("href");
		String title1=(String)widget1.getMetadata("title");
		String alt1=(String)widget1.getMetadata("alt1");
		String optionText1=(String)widget1.getMetadata("option_text");
		String optionValue1=(String)widget1.getMetadata("option_value");
		String xpath1=(String)widget1.getMetadata("xpath");
		Rectangle locationArea1=widget1.getLocationArea();

		String tag2=(String)widget2.getMetadata("tag");
		String className2=(String)widget2.getMetadata("class");
		String type2=(String)widget2.getMetadata("type");
		String name2=(String)widget2.getMetadata("name");
		String id2=(String)widget2.getMetadata("id");
		String href2=(String)widget2.getMetadata("href");
		String title2=(String)widget2.getMetadata("title");
		String alt2=(String)widget2.getMetadata("alt1");
		String optionText2=(String)widget2.getMetadata("option_text");
		String optionValue2=(String)widget2.getMetadata("option_value");
		String xpath2=(String)widget2.getMetadata("xpath");
		Rectangle locationArea2=widget2.getLocationArea();

		int score=0;
		int perfectScore=0;

		if(isButton(tag1, type1, className1) && isButton(tag2, type2, className2))
		{
			perfectScore+=125;
			score+=125;
		}
		else
		{
			if(!widget1.isIgnoredMetadata("tag") && bothContainsValue(tag1, tag2))
			{
				perfectScore+=100;
				if(tag1.equals(tag2))
				{
					score+=100;
				}
			}

			if(!widget1.isIgnoredMetadata("type") && bothContainsValue(type1, type2))
			{
				perfectScore+=25;
				if(type1.equals(type2))
				{
					score+=25;
				}
			}
		}

		if(!widget1.isIgnoredMetadata("href") && bothContainsValue(href1, href2))
		{
			perfectScore+=25;
			score+=getPathScore(href1, href2, 25);
		}

		String visibleText1=getVisibleText(widget1);
		String visibleText2=getVisibleText(widget2);
		if(bothContainsValue(visibleText1, visibleText2))
		{
			perfectScore+=100;
			score+=getScore(visibleText1, visibleText2, 100);
		}

		if(!widget1.isIgnoredMetadata("title") && bothContainsValue(title1, title2))
		{
			perfectScore+=100;
			score+=getScore(title1, title2, 100);
		}

		if(!widget1.isIgnoredMetadata("alt") && bothContainsValue(alt1, alt2))
		{
			perfectScore+=100;
			score+=getScore(alt1, alt2, 100);
		}

		if(!widget1.isIgnoredMetadata("xpath") && bothContainsValue(xpath1, xpath2))
		{
			perfectScore+=25;
			score+=getPathScore(xpath1, xpath2, 25);
		}

		if(!widget1.isIgnoredMetadata("id") && bothContainsValue(id1, id2))
		{
			perfectScore+=100;
			if(id1.equals(id2))
			{
				score+=100;
			}
		}

		if(!widget1.isIgnoredMetadata("name") && bothContainsValue(name1, name2))
		{
			perfectScore+=100;
			if(name1.equals(name2))
			{
				score+=100;
			}
		}

		if(!widget1.isIgnoredMetadata("option_value") && bothContainsValue(optionValue1, optionValue2))
		{
			perfectScore+=100;
			if(optionValue1.equals(optionValue2))
			{
				score+=100;
			}
		}

		if(!widget1.isIgnoredMetadata("option_text") && bothContainsValue(optionText1, optionText2))
		{
			perfectScore+=100;
			if(optionText1.equals(optionText2))
			{
				score+=100;
			}
		}

		if(!widget1.isIgnoredMetadata("class") && bothContainsValue(className1, className2))
		{
			perfectScore+=50;
			score+=compareClass(className1, className2, 50);
		}

		if(locationArea1!=null && locationArea2!=null)
		{
			perfectScore+=150;
			score+=distanceScore(locationArea1, locationArea2, 400, 100);
			score+=diffScore((int)locationArea1.getWidth(), (int)locationArea2.getWidth(), 20, 25);
			score+=diffScore((int)locationArea1.getHeight(), (int)locationArea2.getHeight(), 10, 25);
		}

		perfectScore+=100;
		score+=compareNeighbors(widget1, widget2, 100);

		if(perfectScore==0)
		{
			return null;
		}
		
		return new WidgetMatch(widget1, widget2, score, perfectScore);
	}

	private boolean isButton(String tag, String type, String className)
	{
		if(tag.equalsIgnoreCase("a") && className!=null && className.indexOf("btn")>=0)
		{
			return true;
		}
		if(tag.equalsIgnoreCase("button"))
		{
			return true;
		}
		if(tag.equalsIgnoreCase("input") && ("button".equalsIgnoreCase(type) || "submit".equalsIgnoreCase(type) || "reset".equalsIgnoreCase(type)))
		{
			return true;
		}
		return false;
	}
	
	private int compareNeighbors(Widget widget1, Widget widget2, int maxScore)
	{
		List<Neighbor> neighbors1=(List<Neighbor>)widget1.getMetadata("neighbors");
		List<Neighbor> neighbors2=(List<Neighbor>)widget2.getMetadata("neighbors");
		if(neighbors1==null || neighbors2==null)
		{
			// Both is missing neighbors - same
			return maxScore;
		}
/*
		if(neighbors1==null || neighbors2==null)
		{
			// One is missing neighbors - not same
			return 0;
		}
*/
		int score=0;
		int perfectScore=0;
		for(int i=0; i<neighbors1.size(); i++)
		{
			Neighbor n1=neighbors1.get(i);
			perfectScore+=100;
			score+=bestNeighborScore(n1, neighbors2, 100);
		}
		if(perfectScore==0)
		{
			return maxScore;
		}
		return (score*maxScore)/perfectScore;
	}
	
	private int bestNeighborScore(Neighbor n1, List<Neighbor> neighbors, int maxScore)
	{
		int bestScore=0;
		for(int i=0; i<neighbors.size(); i++)
		{
			Neighbor n2=neighbors.get(i);
			int score=neighborScore(n1, n2, maxScore);
			if(score>bestScore)
			{
				bestScore=score;
			}
		}
		return bestScore;
	}
	
	private int neighborScore(Neighbor n1, Neighbor n2, int maxScore)
	{
		int score=0;
		int perfectScore=100;
		
		score+=diffScore(n1.getDeltaX(), n2.getDeltaX(), 20, 25);
		score+=diffScore(n1.getDeltaY(), n2.getDeltaY(), 10, 25);
		score+=diffScore(n1.getHeight(), n2.getHeight(), 5, 25);
		score+=diffScore(n1.getWidth(), n2.getWidth(), 10, 25);
		
		if(n1.getText()!=null)
		{
			perfectScore+=100;
			if(n2.getText()!=null)
			{
				score+=getScore(n1.getText(), n2.getText(), 100);
			}
		}
		
		return (score*maxScore)/perfectScore;
	}
	
	private int diffScore(int i1, int i2, int maxDiff, int maxScore)
	{
		int delta=Math.abs(i1-i2);
		int diff=maxDiff-delta;
		if(diff<0)
		{
			diff=0;
		}
		return (diff*maxScore)/maxDiff;
	}

	private boolean bothContainsValue(String value1, String value2)
	{
		if(value1!=null && value2!=null && value1.trim().length()>0 && value2.trim().length()>0)
		{
			return true;
		}
		return false;
	}

	public String[] getProductViews()
	{
		return new String[] {"Chrome", "Edge", "Firefox"};
	}
	
  public static WebDriver getWebDriver(String browser)
  {
  	boolean headlessBrowser=StateController.isHeadlessBrowser();

  	try
  	{
  		if("Firefox".equalsIgnoreCase(browser))
  		{
  			String driverName=getGeckoDriverName();
  			if(driverName!=null)
  			{
  				File driverFile=new File(getPath(), driverName);
  				String driverPath=driverFile.getAbsolutePath();
  				System.setProperty("webdriver.gecko.driver", driverPath);
  				FirefoxOptions options = new FirefoxOptions();
  				if(headlessBrowser)
  				{
  	        options.setHeadless(true);
  	        
  	        String lang=StateController.getProductProperty("browser_language", null);
  	        if(lang!=null)
  	        {
    	        options.addArguments("--lang="+lang);
  	        }
  				}
  				return new FirefoxDriver(options);
  			}
  		}
  		else if("Edge".equalsIgnoreCase(browser))
  		{
  			String driverName="bin/drivers/msedgedriver.exe";
  			if(driverName!=null)
  			{
  				File driverFile=new File(getPath(), driverName);
  				String driverPath=driverFile.getAbsolutePath();
  				System.setProperty("webdriver.edge.driver", driverPath);
  				EdgeOptions options = new EdgeOptions();
  				if(headlessBrowser)
  				{
//  	        options.setHeadless(true);
  				}
  				return new EdgeDriver(options);
  			}
  		}
  		else if("Chrome".equalsIgnoreCase(browser))
  		{
  			String driverName=getChromeDriverName();
  			if(driverName!=null)
  			{
  				File driverFile=new File(getPath(), driverName);
  				String driverPath=driverFile.getAbsolutePath();
  				System.setProperty("webdriver.chrome.driver", driverPath);
  				ChromeOptions options = new ChromeOptions();
  				if(headlessBrowser)
  				{
  	        options.setHeadless(true);
  	        
  	        String lang=StateController.getProductProperty("browser_language", null);
  	        if(lang!=null)
  	        {
    	        options.addArguments("--lang="+lang);
  	        }
  				}
  				return new ChromeDriver(options);
  			}
  		}
  	}
  	catch(Exception e)
  	{
  		return null;
  	}

		return null;
  }

	private static String getChromeDriverName()
	{
		String osName = System.getProperty("os.name");
		boolean isWin = osName.startsWith("Windows");
		boolean isOSX = osName.startsWith("Mac");
		boolean isLinux = osName.indexOf("nux")>=0;
		if(isWin)
		{
			return "bin/drivers/chromedriver.exe";
		}
		else if(isOSX)
		{
			return "bin/drivers/chromedriver_mac";
		}
		else if(isLinux)
		{
			return "bin/drivers/chromedriver_linux";
		}
		else
		{
			return null;
		}
	}

	private static String getGeckoDriverName()
	{
		String osName = System.getProperty("os.name");
		boolean isWin = osName.startsWith("Windows");
		boolean isOSX = osName.startsWith("Mac");
		boolean isLinux = osName.indexOf("nux")>=0;
		if(isWin)
		{
			return "bin/drivers/geckodriver.exe";
		}
		else if(isOSX)
		{
			return "bin/drivers/geckodriver_mac";
		}
		else if(isLinux)
		{
			return "bin/drivers/geckodriver_linux";
		}
		else
		{
			return null;
		}
	}

	/**
	 * @return The file path to the scout jar
	 */
	private static String getPath()
	{
		String path = Scout.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String decodedPath = path;
		try
		{
			decodedPath = URLDecoder.decode(path, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			return null;
		}

		String absolutePath = decodedPath.substring(0, decodedPath.lastIndexOf("/")) + "/";
		return absolutePath;
	}

	/**
	 * Create a browser capture and insert or update any state changes
	 * @return A capture from a local or remote browser.
	 */
	private BufferedImage createCaptureAndReplaceWidgets()
	{
		if(webDriver==null)
		{
			// No driver, has not performed any actions or not time to create a new capture
			return null;
		}
		try
		{
			if(isCreatingCapture && latestScreenCapture!=null)
			{
				return null;
			}
			isCreatingCapture=true;

			latestScreenCapture=getSeleniumCapture();

			// A change in the capture has occurred
			if(insertWidgetAfterChange!=null)
			{
				if(StateController.isDoNotRecordNextAction())
				{
					// Do not insert action
					StateController.displayMessageHide();
					PluginController.storeHomeState();
					StateController.setDoNotRecordNextAction(false);
				}
				else
				{
					if(insertWidgetAfterChange.getWidgetVisibility()==WidgetVisibility.SUGGESTION)
					{
						insertWidgetAfterChange.setComment(null);
					}
					insertWidgetAfterChange.setWidgetVisibility(WidgetVisibility.VISIBLE);

/* Only used by Goedel Test report
						if(previousScreenCapture!=null && latestScreenCapture!=null)
						{
							long diff=createQuickDiff(previousScreenCapture, latestScreenCapture);
							insertWidgetAfterChange.putMetadata("pixel_diff", diff);
							long maxDiff=latestScreenCapture.getHeight()*latestScreenCapture.getWidth();
							insertWidgetAfterChange.putMetadata("max_pixel_diff", maxDiff);
						}
*/

					insertWidget(insertWidgetAfterChange, insertWidgetAfterChange.getNextState());
				}
				insertWidgetAfterChange=null;
			}
			if(performWidgetAfterChange!=null)
			{
				performWidget(performWidgetAfterChange);
				performWidgetAfterChange=null;
			}

			List<Widget> availableWidgets=getAvailableWidgets();
			verifyAndReplaceWidgets(availableWidgets);

			isCreatingCapture=false;
      return latestScreenCapture;
		}
		catch (Exception e)
		{
			isCreatingCapture=false;
			return null;
		}
	}

	private BufferedImage getSeleniumCapture()
	{
		try
		{
			TakesScreenshot takesScreenshot=null;
			if(webDriver instanceof TakesScreenshot)
			{
				takesScreenshot=(TakesScreenshot)webDriver;
			}
			else if(webDriver.getClass()==RemoteWebDriver.class)
			{
				takesScreenshot=(TakesScreenshot)new Augmenter().augment(webDriver);
			}
			else
			{
				return null;
			}
			File source=takesScreenshot.getScreenshotAs(OutputType.FILE);
			BufferedImage capture=ImageIO.read(source);
			return capture;
		}
		catch (IOException e)
		{
			return null;
		}
	}

	/**
	 * @return All widgets available on the current page
	 */
	private List<Widget> getAvailableWidgets()
	{
		List<Widget> availableWidgets=new ArrayList<Widget>();
		List<Widget> checkWidgets=new ArrayList<Widget>();
		int frameNo=0;
		
		if(webDriver!=null)
		{
			try
			{
				long start=System.currentTimeMillis();
				JavascriptExecutor executor = (JavascriptExecutor) webDriver;
				executor.executeScript("window.scrollTo(0, 0)");
				Object object=executor.executeScript("var result = []; " +
					"var attributePriorizationList = ['name', 'class', 'title', 'alt', 'value']; " +
					"var attributeBlackList = ['href', 'src', 'onclick', 'onload', 'tabindex', 'width', 'height', 'style', 'size', 'maxlength']; " +
					"var attributePriorizationListRobula = []; " +
					"var attributeBlackListRobula = ['href', 'src', 'onclick', 'onload']; " +
					"var monotoAttributeList = ['id', 'name', 'class', 'title', 'alt', 'value']; " +
					"class XPath {constructor(value) {this.value = value;} getValue() {return this.value;} startsWith(value) {return this.value.startsWith(value);} substring(value) {return this.value.substring(value);} headHasAnyPredicates() {return this.value.split('/')[2].includes('[');} " + 
					"headHasPositionPredicate() {const splitXPath = this.value.split('/'); const regExp = new RegExp('[[0-9]]'); return splitXPath[2].includes('position()') || splitXPath[2].includes('last()') || regExp.test(splitXPath[2]);} headHasTextPredicate() {return this.value.split('/')[2].includes('text()');} addPredicateToHead(predicate) {const splitXPath = this.value.split('/'); splitXPath[2] += predicate; this.value = splitXPath.join('/');} getLength() {const splitXPath = this.value.split('/'); let length = 0; for (const piece of splitXPath) {if (piece) length++;} return length;}} " +
					"function getRobulaPlusXPath(element) {const xPathList = [new XPath('//*')]; while (xPathList.length > 0) {const xPath = xPathList.shift(); let temp = []; temp = temp.concat(transfConvertStar(xPath, element)); temp = temp.concat(transfAddId(xPath, element)); temp = temp.concat(transfAddText(xPath, element)); temp = temp.concat(transfAddAttribute(xPath, element)); temp = temp.concat(transfAddAttributeSet(xPath, element)); temp = temp.concat(transfAddPosition(xPath, element)); temp = temp.concat(transfAddLevel(xPath, element)); temp = [...new Set(temp)]; for (const x of temp) {if (uniquelyLocate(x.getValue(), element)) return x.getValue(); xPathList.push(x);}} return null;} " +
					"function getRobulaXPath(element) {const xPathList = [new XPath('//*')]; while (xPathList.length > 0) {const xPath = xPathList.shift(); let temp = []; temp = temp.concat(transfConvertStar(xPath, element)); temp = temp.concat(transfAddId(xPath, element)); temp = temp.concat(transfAddAttributeRobula(xPath, element)); temp = temp.concat(transfAddPosition(xPath, element)); temp = temp.concat(transfAddLevel(xPath, element)); temp = [...new Set(temp)]; for (const x of temp) {if (uniquelyLocate(x.getValue(), element)) return x.getValue(); xPathList.push(x);}} return null;} " +
					"function uniquelyLocate(xPath, element) {const nodesSnapshot = document.evaluate(xPath, document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null); return nodesSnapshot.snapshotLength === 1 && nodesSnapshot.snapshotItem(0) === element;} " +
					"function transfConvertStar(xPath, element) {const output = []; const ancestor = getAncestor(element, xPath.getLength() - 1); if (xPath.startsWith('//*')) output.push(new XPath('//' + ancestor.tagName.toLowerCase() + xPath.substring(3))); return output;} " +
					"function transfAddId(xPath, element) {const output = []; const ancestor = getAncestor(element, xPath.getLength() - 1); if (ancestor.id && !xPath.headHasAnyPredicates()) {const newXPath = new XPath(xPath.getValue()); newXPath.addPredicateToHead(\"[@id='\"+ancestor.id+\"']\"); output.push(newXPath);} return output;} " +
					"function transfAddText(xPath, element) {const output = []; const ancestor = getAncestor(element, xPath.getLength() - 1); if (ancestor.textContent && !xPath.headHasPositionPredicate() && !xPath.headHasTextPredicate()) {const newXPath = new XPath(xPath.getValue()); newXPath.addPredicateToHead(\"[contains(text(),'\"+cleanText(ancestor.textContent)+\"')]\"); output.push(newXPath);} return output;} " +
					"function transfAddAttribute(xPath, element) {const output = []; const ancestor = getAncestor(element, xPath.getLength() - 1); if (!xPath.headHasAnyPredicates()) {for (const priorityAttribute of attributePriorizationList) {for (const attribute of ancestor.attributes) {if (attribute.name === priorityAttribute) {const newXPath = new XPath(xPath.getValue()); newXPath.addPredicateToHead('[@'+attribute.name+\"='\"+attribute.value+\"']\"); output.push(newXPath); break;}}} for (const attribute of ancestor.attributes) {if (!attributeBlackList.includes(attribute.name) && !attributePriorizationList.includes(attribute.name)) {const newXPath = new XPath(xPath.getValue()); newXPath.addPredicateToHead('[@'+attribute.name+\"='\"+attribute.value+\"']\"); output.push(newXPath);}}} return output;} " +
					"function transfAddAttributeRobula(xPath, element) {const output = []; const ancestor = getAncestor(element, xPath.getLength() - 1); if (!xPath.headHasAnyPredicates()) {for (const priorityAttribute of attributePriorizationListRobula) {for (const attribute of ancestor.attributes) {if (attribute.name === priorityAttribute) {const newXPath = new XPath(xPath.getValue()); newXPath.addPredicateToHead('[@'+attribute.name+\"='\"+attribute.value+\"']\"); output.push(newXPath); break;}}} for (const attribute of ancestor.attributes) {if (!attributeBlackListRobula.includes(attribute.name) && !attributePriorizationListRobula.includes(attribute.name)) {const newXPath = new XPath(xPath.getValue()); newXPath.addPredicateToHead('[@'+attribute.name+\"='\"+attribute.value+\"']\"); output.push(newXPath);}}} return output;} " +
					"function transfAddAttributeSet(xPath, element) {const output = []; const ancestor = getAncestor(element, xPath.getLength() - 1); if (!xPath.headHasAnyPredicates()) {attributePriorizationList.unshift('id'); let attributes = [...ancestor.attributes]; attributes = attributes.filter(attribute => !attributeBlackList.includes(attribute.name)); let attributePowerSet = generatePowerSet(attributes); attributePowerSet = attributePowerSet.filter(attributeSet => attributeSet.length >= 2); for (const attributeSet of attributePowerSet) {attributeSet.sort(elementCompareFunction.bind(this));} attributePowerSet.sort((set1, set2) => {if (set1.length < set2.length) {return -1;} if (set1.length > set2.length) {return 1;} for (let i = 0; i < set1.length; i++) {if (set1[i] !== set2[i]) {return elementCompareFunction(set1[i], set2[i]);}} return 0;}); attributePriorizationList.shift(); for (const attributeSet of attributePowerSet) {let predicate = '[@'+attributeSet[0].name+\"='\"+attributeSet[0].value+\"'\"; for (let i = 1; i < attributeSet.length; i++) {predicate += ' and @'+attributeSet[i].name+\"='\"+attributeSet[i].value+\"'\";} predicate += ']'; const newXPath = new XPath(xPath.getValue()); newXPath.addPredicateToHead(predicate); output.push(newXPath);}} return output;} " +
					"function transfAddPosition(xPath, element) {const output = []; const ancestor = getAncestor(element, xPath.getLength() - 1); if (!xPath.headHasPositionPredicate()) {let position = 1; if (xPath.startsWith('//*')) {position = Array.from(ancestor.parentNode.children).indexOf(ancestor) + 1;} else {for (const child of ancestor.parentNode.children) {if (ancestor === child) {break;} if (ancestor.tagName === child.tagName) {position++;}}} const newXPath = new XPath(xPath.getValue()); newXPath.addPredicateToHead('['+position+']'); output.push(newXPath);} return output;} " +
					"function transfAddLevel(xPath, element) {const output = []; if (xPath.getLength() - 1 < getAncestorCount(element)) {output.push(new XPath('//*' + xPath.substring(1)));} return output;} " +
					"function generatePowerSet(input) {return input.reduce((subsets, value) => subsets.concat(subsets.map((set) => [value, ...set])), [[]],);} " +
					"function elementCompareFunction(attr1, attr2) {for (const element of attributePriorizationList) {if (element === attr1.name) {return -1;} if (element === attr2.name) {return 1;}} return 0;} " +
					"function getAncestor(element, index) {let output = element; for (let i = 0; i < index; i++) {output = output.parentElement;} return output;} " +
					"function getAncestorCount(element) {let count = 0; while (element.parentElement) {element = element.parentElement; count++;} return count;} " +
					"function getXPosition(el) {return el.getBoundingClientRect().left;} " +
					"function getYPosition(el) {return el.getBoundingClientRect().top+window.scrollY;} " +
					"function getMaxWidth(el) {return el.getBoundingClientRect().width;} " +
					"function getMaxHeight(el) {return el.getBoundingClientRect().height;} " +
					"function elementIsVisible(el) {if (getComputedStyle(el).visibility === 'hidden') return false; return true;} " +
					"function nullToEmpty(cStr) {return cStr == null ? [] : cStr;} " +
					"function getNodePosition(node) {if (!node.parentNode) return -1; var siblings = node.parentNode.childNodes, count = 0, position; for (var i = 0; i < siblings.length; i++) {var object = siblings[i]; if(object.nodeType == node.nodeType && object.nodeName == node.nodeName) {count++; if(object == node) position = count;}} return position;} " +
//					"function getXPath (element) {if (!element) return null; if (element.tagName === 'BODY') {return '/html/body';} else {const sameTagSiblings = Array.from(nullToEmpty(element.parentNode.childNodes)).filter(e => e.nodeName === element.nodeName); const i = sameTagSiblings.indexOf(element)+1; return getXPath(element.parentNode) + '/' + element.tagName.toLowerCase() + (sameTagSiblings.length > 1 ? '['+i+']' : '');}} " +
					"function getXPath(element) {const idx = (sib, name) => sib ? idx(sib.previousElementSibling, name||sib.localName) + (sib.localName == name): 1; const segs = elm => !elm || elm.nodeType !== 1 ? ['']: [...segs(elm.parentNode), elm instanceof HTMLElement? `${elm.localName}[${idx(elm)}]`: `*[local-name() = \"${elm.localName}\"][${idx(elm)}]`]; return segs(element).join('/');} " +
					"function getIdXPath(element) {const idx = (sib, name) => sib ? idx(sib.previousElementSibling, name||sib.localName) + (sib.localName == name): 1; const segs = elm => !elm || elm.nodeType !== 1 ? ['']: elm.id && document.getElementById(elm.id) === elm ? [`id(\"${elm.id}\")`]: [...segs(elm.parentNode), elm instanceof HTMLElement? `${elm.localName}[${idx(elm)}]`: `*[local-name() = \"${elm.localName}\"][${idx(elm)}]`]; return segs(element).join('/');} " +
//					"function getIdXPath(element) {const idx = (sib, name) => sib ? idx(sib.previousElementSibling, name||sib.localName) + (sib.localName == name) : 1; const segs = elm => !elm || elm.nodeType !== 1 ? [''] : elm.id && document.getElementById(elm.id) === elm ? [\"//*[@id='\"+elm.id+\"']\"] : [...segs(elm.parentNode), `${elm.localName.toLowerCase()}[${idx(elm)}]`]; return segs(element).join('/');} " +
					"function escapeQuotes(s) {return s.replace('\\'', ' ');} " +
					"function cleanText(s) {return s.replace(/[^\\w\\s]/gi, '');} " +
					"function getMonotoXPath(element) {var x=''; var n=element; while(true) {if ((n.children.length==0 || n.children.length==1 && n.children[0].nodeType==3) && n.textContent.length>0) {let xpath='//'+n.tagName+\"[text()='\"+escapeQuotes(n.textContent)+\"']\"+x; if (uniquelyLocate(xpath, element)) return xpath;} else {let xpath='//'+n.tagName+x; if (uniquelyLocate(xpath, element)) return xpath;} var e=''; for (const attribute of monotoAttributeList) {var attr=n.getAttributeNode(attribute); if(attr && attr.value.length>0) {if (e.length>0) e+=' and '; e+='@'+attr.name+\"='\"+attr.value+\"'\"; let xpath='/'+n.tagName+'['+e+']'+x; if (uniquelyLocate(xpath, element)) return xpath;}} if ((n.children.length==0 || n.children.length==1 && n.children[0].nodeType==3) && n.textContent.length>0) {if (e.length>0) e+=' and '; e+=\"text()='\"+escapeQuotes(n.textContent)+\"'\";} if (e.length>0) x='/'+n.tagName+'['+e+']'+x; else x='/'+n.tagName+x; let xpath='/'+x; if (uniquelyLocate(xpath, element)) return xpath; n=n.parentElement; if (n==null) return null;}} " +
					"var all = document.querySelectorAll('"+WidgetIdentifier.getElementsToExtract()+"'); " +
					"for (var i=0, max=all.length; i < max; i++) { " +
//					"    if (elementIsVisible(all[i])) result.push({'tag': all[i].tagName, 'class': all[i].className, 'type': all[i].type, 'name': all[i].name, 'id': all[i].id, 'value': all[i].value, 'href': all[i].href, 'text': all[i].textContent, 'placeholder': all[i].placeholder, 'title': all[i].title, 'alt': all[i].alt, 'x': getXPosition(all[i]), 'y': getYPosition(all[i]), 'width': getMaxWidth(all[i]), 'height': getMaxHeight(all[i]), 'children': all[i].children.length, 'xpath': getXPath(all[i]), 'xpath2': getIdXPath(all[i]), 'xpath3': getRobulaXPath(all[i]), 'xpath4': getRobulaPlusXPath(all[i]), 'xpath5': getMonotoXPath(all[i])}); " +
					"    if (elementIsVisible(all[i])) result.push({'tag': all[i].tagName, 'class': all[i].className, 'type': all[i].type, 'name': all[i].name, 'id': all[i].id, 'value': all[i].value, 'href': all[i].href, 'text': all[i].textContent, 'placeholder': all[i].placeholder, 'title': all[i].title, 'alt': all[i].alt, 'x': getXPosition(all[i]), 'y': getYPosition(all[i]), 'width': getMaxWidth(all[i]), 'height': getMaxHeight(all[i]), 'children': all[i].children.length, 'xpath': getXPath(all[i])}); " +
//					"    if (elementIsVisible(all[i])) result.push({'tag': all[i].tagName, 'class': all[i].className, 'type': all[i].type, 'name': all[i].name, 'id': all[i].id, 'value': all[i].value, 'href': all[i].href, 'text': all[i].textContent, 'placeholder': all[i].placeholder, 'title': all[i].title, 'alt': all[i].alt, 'x': getXPosition(all[i]), 'y': getYPosition(all[i]), 'width': getMaxWidth(all[i]), 'height': getMaxHeight(all[i]), 'children': all[i].children.length}); " +
					"} " +
					" return JSON.stringify(result); ");

				String json=object.toString();
				JSONParser parser = new JSONParser();
				JSONArray jsonArray = (JSONArray)parser.parse(json);
				
				long time=System.currentTimeMillis()-start;
	
				for(int i=0; i<jsonArray.size(); i++)
				{
					JSONObject jsonObject=(JSONObject)jsonArray.get(i);

					String tag=object2String(jsonObject.get("tag"));
					String className=object2String(jsonObject.get("class"));
					String type=object2String(jsonObject.get("type"));
					String name=object2String(jsonObject.get("name"));
					String id=object2String(jsonObject.get("id"));
					String value=object2String(jsonObject.get("value"));
					String href=object2String(jsonObject.get("href"));
					String text=object2String(jsonObject.get("text"));
					String placeholder=object2String(jsonObject.get("placeholder"));
					String title=object2String(jsonObject.get("title"));
					String alt=object2String(jsonObject.get("alt"));
					String children=object2String(jsonObject.get("children"));
					String xpath=object2String(jsonObject.get("xpath"));
					String xpath2=object2String(jsonObject.get("xpath2"));
					String xpath3=object2String(jsonObject.get("xpath3"));
					String xpath4=object2String(jsonObject.get("xpath4"));
					String xpath5=object2String(jsonObject.get("xpath5"));
					Long x=object2Long(jsonObject.get("x"));
					Long y=object2Long(jsonObject.get("y"));
					Long width=object2Long(jsonObject.get("width"));
					Long height=object2Long(jsonObject.get("height"));

					if(width>0 && height>0)
					{
						Widget widget=new Widget();
						widget.setLocationArea(new Rectangle(x.intValue(), y.intValue(), width.intValue(), height.intValue()));

						widget.putMetadata("tag", tag);
						widget.putMetadata("class", className);
						widget.putMetadata("type", type);
						widget.putMetadata("name", name);
						widget.putMetadata("id", id);
						widget.putMetadata("value", value);
						widget.putMetadata("href", href);
						if(isValidText(text))
						{
							widget.putMetadata("text", text);
						}
						widget.putMetadata("placeholder", placeholder);
						widget.putMetadata("title", title);
						widget.putMetadata("alt", alt);
						widget.putMetadata("children", children);
						widget.putMetadata("xpath", xpath);
						widget.putMetadata("xpath2", xpath2);
						widget.putMetadata("xpath3", xpath3);
						widget.putMetadata("xpath4", xpath4);
						widget.putMetadata("xpath5", xpath5);
						widget.putMetadata("execute_time", time);

						if(WidgetIdentifier.isClickWidget(widget))
						{
							widget.setWidgetType(WidgetType.ACTION);
							widget.setWidgetSubtype(WidgetSubtype.LEFT_CLICK_ACTION);
							availableWidgets.add(widget);

							// Create move widget if needed
							if(WidgetIdentifier.isMoveWidget(widget))
							{
								widget=new Widget(widget);
								widget.setWidgetSubtype(WidgetSubtype.MOVE_ACTION);
								availableWidgets.add(widget);
							}

							// Add check widget if needed
							if(WidgetIdentifier.isCheckWidget(widget))
							{
								addCheckWidget(widget, availableWidgets, text, value, placeholder);
							}
						}
						else if(WidgetIdentifier.isTypeWidget(widget))
						{
							widget.setWidgetType(WidgetType.ACTION);
							widget.setWidgetSubtype(WidgetSubtype.TYPE_ACTION);
							widget.setText(text);
							availableWidgets.add(widget);

							// Add check widget if needed
							if(WidgetIdentifier.isCheckWidget(widget))
							{
								addCheckWidget(widget, availableWidgets, text, value, placeholder);
							}
						}
						else if (WidgetIdentifier.isSelectWidget(widget))
						{
							widget.setWidgetType(WidgetType.ACTION);
							widget.setWidgetSubtype(WidgetSubtype.SELECT_ACTION);
							WebElement webElement=findWebElement(widget);
							if(webElement!=null)
							{
								Select select=new Select(webElement);
								List<WebElement> options=select.getOptions();
								for(WebElement option:options)
								{
									// Create a new option
									Widget optionWidget=new Widget(widget);
									String optionText=option.getText();
									if(optionText!=null)
									{
										optionWidget.setText(optionText);
										optionWidget.putMetadata("option_text", optionText);
									}
									String optionValue=option.getAttribute("value");
									if(optionValue!=null)
									{
										optionWidget.putMetadata("option_value", optionValue);
									}
									availableWidgets.add(optionWidget);

									// Add check widget if needed
									if(WidgetIdentifier.isCheckWidget(widget))
									{
										addCheckWidget(optionWidget, availableWidgets, text, value, placeholder);
									}
								}
							}
						}
						else if(WidgetIdentifier.isFrame(widget))
						{
							if(isSwitchingFrame)
							{
								// Abort
								return null;
							}
							webDriver.switchTo().frame(frameNo);
							List<Widget> frameWidgets=getAvailableWidgets();
							if(isSwitchingFrame)
							{
								// Abort
								return null;
							}
							webDriver.switchTo().defaultContent();
							for(Widget frameWidget:frameWidgets)
							{
								Rectangle area=frameWidget.getLocationArea();
								frameWidget.setLocationArea(new Rectangle((int)area.getX()+x.intValue(), (int)area.getY()+y.intValue(), (int)area.getWidth(), (int)area.getHeight()));
								frameWidget.putMetadata("frame_no", frameNo);
								frameWidget.putMetadata("location_in_frame", area);
								availableWidgets.add(frameWidget);
							}
							frameNo++;
						}
						else if(WidgetIdentifier.isContainerWidget(widget))
						{
							if(text!=null && text.trim().length()>0)
							{
								widget=new Widget(widget);
								widget.setWidgetType(WidgetType.CHECK);
								widget.setValidExpression("{text} = "+text.trim());
								checkWidgets.add(widget);
							}
						}
						else if(WidgetIdentifier.isMoveWidget(widget))
						{
							widget=new Widget(widget);
							widget.setWidgetSubtype(WidgetSubtype.MOVE_ACTION);
							availableWidgets.add(widget);
						}
						else if(WidgetIdentifier.isCheckWidget(widget))
						{
							addCheckWidget(widget, availableWidgets, text, value, placeholder);
						}
					}
				}

				// Add check widgets if they are not overlapping (smallest first)
				checkWidgets=StateController.sortWidgets(checkWidgets);
				for(Widget w:checkWidgets)
				{
					if(!isOverlapping(availableWidgets, w))
					{
						availableWidgets.add(w);
					}
				}

				addPageContent(availableWidgets);
				for(Widget w:availableWidgets)
				{
					addNeighbors(w, availableWidgets);
					addContains(w, availableWidgets);
				}
			}
			catch (Exception e)
			{
				return availableWidgets;
			}
		}

		return availableWidgets;
	}

	private boolean isValidText(String text)
	{
		if(text==null)
		{
			return false;
		}
		String trimmedText=text.trim();
		if(trimmedText.length()>100)
		{
			// Too long
			return false;
		}
		if(trimmedText.indexOf('\n')>=0)
		{
			// Contains newline
			return false;
		}
		if(trimmedText.indexOf('\t')>=0)
		{
			// Contains tab
			return false;
		}
		return true;
	}

	private void addCheckWidget(Widget widget, List<Widget> availableWidgets, String text, String value, String placeholder)
	{
		if(text!=null && text.trim().length()>0)
		{
			// Add a check
			widget=new Widget(widget);
			widget.setWidgetType(WidgetType.CHECK);
			widget.setValidExpression("{text} = "+text.trim());
			availableWidgets.add(widget);
		}
		else if(value!=null && value.trim().length()>0)
		{
			// Add a check
			widget=new Widget(widget);
			widget.setWidgetType(WidgetType.CHECK);
			widget.setValidExpression("{value} = "+value.trim());
			availableWidgets.add(widget);
		}
		else if(placeholder!=null && placeholder.trim().length()>0)
		{
			// Add a check
			widget=new Widget(widget);
			widget.setWidgetType(WidgetType.CHECK);
			widget.setValidExpression("{placeholder} = "+placeholder.trim());
			availableWidgets.add(widget);
		}
		else
		{
			// Add a check
			widget=new Widget(widget);
			widget.setWidgetType(WidgetType.CHECK);
			widget.setValidExpression("{tag} = "+(String)widget.getMetadata("tag"));
			availableWidgets.add(widget);
		}

	}

	private void assignValidExpression(Widget fromWidget, Widget toWidget)
	{
		if(fromWidget!=null && toWidget!=null)
		{
			String text=(String)fromWidget.getMetadata("text");
			String value=(String)fromWidget.getMetadata("value");
			String placeholder=(String)fromWidget.getMetadata("placeholder");

			if(text!=null && text.trim().length()>0)
			{
				toWidget.setValidExpression("{text} = "+text);
				toWidget.putMetadata("text", text);
			}
			else if(value!=null && value.trim().length()>0)
			{
				toWidget.setValidExpression("{value} = "+value);
				toWidget.putMetadata("value", value);
			}
			else if(placeholder!=null && placeholder.trim().length()>0)
			{
				toWidget.setValidExpression("{placeholder} = "+placeholder);
				toWidget.putMetadata("placeholder", placeholder);
			}
		}
	}
	
	private String getVisibleText(Widget w)
	{
		String text=(String)w.getMetadata("text");
		String value=(String)w.getMetadata("value");
		String placeholder=(String)w.getMetadata("placeholder");

		if(text!=null && text.trim().length()>0)
		{
			return text.trim();
		}
		else if(value!=null && value.trim().length()>0)
		{
			return value.trim();
		}
		else if(placeholder!=null && placeholder.trim().length()>0)
		{
			return placeholder.trim();
		}
		
		return null;
	}
	
	/**
	 * Add neighbors to w
	 * @param w
	 * @param availableWidgets
	 */
	private void addNeighbors(Widget w, List<Widget> availableWidgets)
	{
		if(w.getLocationArea().getHeight()<=200)
		{
			List<Neighbor> neighbors=new ArrayList<Neighbor>();
			for(Widget availableWidget:availableWidgets)
			{
				if(w!=availableWidget)
				{
					double distance=distance(w, availableWidget);
					if(!w.getLocationArea().equals(availableWidget.getLocationArea()) && distance<=25)
					{
						int dX=(int)availableWidget.getLocationArea().getX()-(int)w.getLocationArea().getX();
						int dY=(int)availableWidget.getLocationArea().getY()-(int)w.getLocationArea().getY();
						int width=(int)availableWidget.getLocationArea().getWidth();
						int height=(int)availableWidget.getLocationArea().getHeight();
						if(height<100 && !containsNeighbor(neighbors, dX, dY, width, height))
						{
							Neighbor neighbor=new Neighbor(distance, dX, dY, width, height);
							String visibleText=getVisibleText(availableWidget);
							if(isValidText(visibleText))
							{
								neighbor.setText(visibleText);
								neighbors.add(neighbor);
							}
						}
					}
				}
			}
			if(neighbors.size()>0)
			{
				Collections.sort(neighbors);
				w.putMetadata("neighbors", neighbors);
			}
		}
	}
	
	private boolean containsNeighbor(List<Neighbor> neighbors, int dX, int dY, int width, int height)
	{
		for(Neighbor neighbor:neighbors)
		{
			if(neighbor.getDeltaX()==dX && neighbor.getDeltaY()==dY && neighbor.getWidth()==width && neighbor.getHeight()==height)
			{
				return true;
			}
		}
		return false;
	}
	
	private void addContains(Widget w, List<Widget> availableWidgets)
	{
		int count=0;
		Rectangle r=w.getLocationArea();
		for(Widget w2:availableWidgets)
		{
			Rectangle r2=w2.getLocationArea();
			if(w!=w2 && !r.equals(r2) && r.contains(r2))
			{
				count++;
			}
		}
		w.putMetadata("contains", ""+count);
	}

	private void addPageContent(List<Widget> availableWidgets)
	{
		int countClickActions=0;
		int countTypeActions=0;
		int countCheckActions=0;
		for(Widget w2:availableWidgets)
		{
			if(w2.getWidgetType()==WidgetType.ACTION)
			{
				if(w2.getWidgetSubtype()==WidgetSubtype.LEFT_CLICK_ACTION)
				{
					countClickActions++;
				}
				else if(w2.getWidgetSubtype()==WidgetSubtype.TYPE_ACTION)
				{
					countTypeActions++;
				}
			}
			else if(w2.getWidgetType()==WidgetType.CHECK)
			{
				countCheckActions++;
			}
		}
		long pageContent=countCheckActions+countClickActions*1000+countTypeActions*1000000;
		currentState.putMetadata("page_content", pageContent);
	}

	private double distance(Widget w1, Widget w2)
	{
		return findClosest(w1.getLocationArea(), w2.getLocationArea());
	}
	
	private double findClosest(Rectangle rec1, Rectangle rec2)
	{
		double x1, x2, y1, y2;
		double w, h;
		if (rec1.x > rec2.x)
		{
			x1 = rec2.x;
			w = rec2.width;
			x2 = rec1.x;
		}
		else
		{
			x1 = rec1.x;
			w = rec1.width;
			x2 = rec2.x;
		}
		if (rec1.y > rec2.y)
		{
			y1 = rec2.y;
			h = rec2.height;
			y2 = rec1.y;
		}
		else
		{
			y1 = rec1.y;
			h = rec1.height;
			y2 = rec2.y;
		}
		double a = Math.max(0, x2 - x1 - w);
		double b = Math.max(0, y2 - y1 - h);
		return Math.sqrt(a * a + b * b);
	}
	
	private static boolean isOverlapping(List<Widget> widgets, Widget widget)
	{
		for(Widget w:widgets)
		{
			if(w.getLocationArea().intersects(widget.getLocationArea()))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Find a WebElement from a widget
	 * @param widget
	 * @return A WebElement or null
	 */
	private WebElement findWebElement(Widget widget)
	{
		WebElement element=findWebElementInt(widget);

		// Retry to find the web element if needed
		for(int i=0; i<10 && element==null; i++)
		{
			StateController.delay(500);
			element=findWebElementInt(widget);
		}

		if(element==null)
		{
			StateController.displayMessage("Widget is not available right now");
		}

		return element;
	}
	
	private WebElement findWebElementInt(Widget widget)
	{
		if(webDriver==null)
		{
			return null;
		}

		String xpath=(String)widget.getMetadata("xpath");
		if(xpath!=null)
		{
			try
			{
				WebElement element=webDriver.findElement(By.xpath(xpath));
				return element;
			}
			catch(Exception e)
			{
			}
		}
/*
		String tag=(String)widget.getMetadata("tag");
		if(tag!=null)
		{
			try
			{
				List<WebElement> elements=webDriver.findElements(By.tagName(tag));
				if(elements!=null && elements.size()>0)
				{
					WebElement bestElement=findBestMatchingElement(widget, elements);
					if(bestElement!=null)
					{
						return bestElement;
					}
				}
			}
			catch(Exception e)
			{
			}
		}
*/
		return null;
	}

	/**
	 * Type a text using Selenium
	 * @param webElement
	 * @param text
	 * @return true if typed
	 */
	private boolean typeSelenium(WebElement webElement, String text)
	{
		if(seleniumKeyTag==null)
		{
			seleniumKeyTag=new String[]{"[ENTER]", "[TAB]", "[DELETE]", "[ESCAPE]", "[BACKSPACE]", "[UP]", "[DOWN]", "[LEFT]", "[RIGHT]", "[PAGE_UP]", "[PAGE_DOWN]", "[HOME]", "[END]", "[F1]", "[F2]", "[F3]", "[F4]", "[F5]", "[F6]", "[F7]", "[F8]", "[F9]", "[F10]", "[F11]", "[F12]", "[NUMPAD_ADD]", "[NUMPAD_SUBTRACT]", "[NUMPAD_MULTIPLY]", "[NUMPAD_DIVIDE]"};
			seleniumKeyValue=new String[]{Keys.RETURN.toString(), Keys.TAB.toString(), Keys.DELETE.toString(), Keys.ESCAPE.toString(), Keys.BACK_SPACE.toString(), Keys.UP.toString(), Keys.DOWN.toString(), Keys.LEFT.toString(), Keys.RIGHT.toString(), Keys.PAGE_UP.toString(), Keys.PAGE_DOWN.toString(), Keys.HOME.toString(), Keys.END.toString(), Keys.F1.toString(), Keys.F2.toString(), Keys.F3.toString(), Keys.F4.toString(), Keys.F5.toString(), Keys.F6.toString(), Keys.F7.toString(), Keys.F8.toString(), Keys.F9.toString(), Keys.F10.toString(), Keys.F11.toString(), Keys.F12.toString(), Keys.ADD.toString(), Keys.SUBTRACT.toString(), Keys.MULTIPLY.toString(), Keys.DIVIDE.toString()};
		}
		try
		{
			StringBuffer buffer=new StringBuffer();
			while(text.length()>0)
			{
				String remainingText=typeSeleniumKeyTag(webElement, text, buffer);
				if(remainingText.length()==text.length())
				{
					// No key tag found
					buffer.append(text.charAt(0));
					text=text.substring(1);
				}
				else
				{
					text=remainingText;
				}
			}
			if(buffer.length()>0)
			{
				webElement.clear();
				webElement.sendKeys(buffer.toString().trim());
			}
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}

	private String typeSeleniumKeyTag(WebElement webElement, String text, StringBuffer buffer)
	{
		for(int i=0; i<seleniumKeyTag.length; i++)
		{
			if(text.startsWith(seleniumKeyTag[i]))
			{
				buffer.append(seleniumKeyValue[i]);
				return text.substring(seleniumKeyTag[i].length());
			}
		}
		return text;
	}

	private void loadCookies()
	{
		loadCookies(webDriver);
	}

	/**
	 * Load cookies from file
	 * @param webDriver
	 */
	private void loadCookies(WebDriver webDriver)
	{
		try
		{
			Object object=currentState.getMetadata("cookies");
			if(object!=null)
			{
				Set<Cookie> cookies=(Set<Cookie>)object;
				for(Cookie cookie:cookies)
				{
					try
					{
						webDriver.manage().addCookie(cookie);
					}
					catch(Exception e)
					{
					}
				}
			}
			webDriver.navigate().refresh();
		}
		catch (Throwable e)
		{
		}
	}

	private void saveCookies()
	{
		saveCookies(webDriver);
	}

	/**
	 * Save cookies to file
	 * @param webDriver
	 * @return
	 */
	private boolean saveCookies(WebDriver webDriver)
	{
		try
		{
			Set<Cookie> cookies=webDriver.manage().getCookies();
			currentState.putMetadata("cookies", cookies);
			return true;
		}
		catch (Throwable e)
		{
			return false;
		}
	}

	/**
	 * Convert keyCode and keyChar into text
	 * @param keyChar
	 * @return A text
	 */
	private static String getKeyText(char keyChar)
	{
		if(Character.isAlphabetic(keyChar) || Character.isDigit(keyChar) || Character.isDefined(keyChar))
		{
			return String.valueOf(keyChar);
		}
		return "";
	}

	private void clickWebElement(WebElement element)
	{
		Actions actions = new Actions(webDriver);
		actions.moveToElement(element).click().build().perform();
	}

	private void moveToWebElement(WebElement element)
	{
		Actions actions = new Actions(webDriver);
		actions.moveToElement(element).build().perform();
	}

	/**
	 * Get widgets from elements
	 * @param elements
	 * @return A list of widgets
	 */
/*
	private List<Widget> getWidgets(List<WebElement> elements)
	{
		if(webDriver!=null)
		{
			try
			{
				List<Widget> widgets=new ArrayList<Widget>();

				JavascriptExecutor executor = (JavascriptExecutor) webDriver;
				Object object=executor.executeScript("var result = []; " +
					"function getXPosition(el) {return el.getBoundingClientRect().left;} " +
					"function getYPosition(el) {return el.getBoundingClientRect().top+window.scrollY;} " +
					"function getMaxWidth(el) {return el.getBoundingClientRect().width;} " +
					"function getMaxHeight(el) {return el.getBoundingClientRect().height;} " +
					"function elementIsVisible(el) {if (getComputedStyle(el).visibility === 'hidden') return false; return true;} " +
					"function getXPath (element) {if (!element) return null; if (element.tagName === 'BODY') {return '/html/body';} else {const sameTagSiblings = Array.from(element.parentNode.childNodes).filter(e => e.nodeName === element.nodeName); const i = sameTagSiblings.indexOf(element)+1; return getXPath(element.parentNode) + '/' + element.tagName.toLowerCase() + (sameTagSiblings.length > 1 ? '['+i+']' : '');}} " +
					"for (var i=0, max=arguments.length; i < max; i++) { " +
					"   if(elementIsVisible(arguments[i])) result.push({'tag': arguments[i].tagName, 'class': arguments[i].className, 'type': arguments[i].type, 'name': arguments[i].name, 'id': arguments[i].id, 'value': arguments[i].value, 'href': arguments[i].href, 'text': arguments[i].textContent, 'placeholder': arguments[i].placeholder, 'title': arguments[i].title, 'alt': arguments[i].alt, 'x': getXPosition(arguments[i]), 'y': getYPosition(arguments[i]), 'width': getMaxWidth(arguments[i]), 'height': getMaxHeight(arguments[i]), 'index': i, 'children': arguments[i].children.length, 'xpath': getXPath(arguments[i])}); " +
					"} " +
					"return JSON.stringify(result); ", elements.toArray());

				String json=object.toString();
				JSONParser parser = new JSONParser();
				JSONArray jsonArray = (JSONArray)parser.parse(json);
				for(int i=0; i<jsonArray.size(); i++)
				{
					JSONObject jsonObject=(JSONObject)jsonArray.get(i);

					String tag=object2String(jsonObject.get("tag"));
					String className=object2String(jsonObject.get("class"));
					String type=object2String(jsonObject.get("type"));
					String name=object2String(jsonObject.get("name"));
					String id=object2String(jsonObject.get("id"));
					String value=object2String(jsonObject.get("value"));
					String href=object2String(jsonObject.get("href"));
					String text=object2String(jsonObject.get("text"));
					String placeholder=object2String(jsonObject.get("placeholder"));
					String title=object2String(jsonObject.get("title"));
					String alt=object2String(jsonObject.get("alt"));
					String children=object2String(jsonObject.get("children"));
					String xpath=object2String(jsonObject.get("xpath"));
					Long index=(Long)jsonObject.get("index");
					Long x=object2Long(jsonObject.get("x"));
					Long y=object2Long(jsonObject.get("y"));
					Long width=object2Long(jsonObject.get("width"));
					Long height=object2Long(jsonObject.get("height"));

					if(width>0 && height>0)
					{
						Widget widget=new Widget();
						widget.setLocationArea(new Rectangle(x.intValue(), y.intValue(), width.intValue(), height.intValue()));
						
						widget.putMetadata("tag", tag);
						widget.putMetadata("class", className);
						widget.putMetadata("type", type);
						widget.putMetadata("name", name);
						widget.putMetadata("id", id);
						widget.putMetadata("value", value);
						widget.putMetadata("href", href);
						widget.putMetadata("text", text);
						widget.putMetadata("placeholder", placeholder);
						widget.putMetadata("title", title);
						widget.putMetadata("alt", alt);
						widget.putMetadata("children", children);
						widget.putMetadata("xpath", xpath);
						widget.putMetadata("index", index);

						widgets.add(widget);
					}
				}
				
				return widgets;
			}
			catch (Exception e)
			{
				return null;
			}
		}

		return null;
	}
*/

	private void addCorrection(Widget w, Correction correction)
	{
		Object correctionsObject=w.getMetadata("corrections");
		if(correctionsObject!=null)
		{
			List<Correction> corrections=(List<Correction>)correctionsObject;
			corrections.add(correction);
		}
		else
		{
			List<Correction> corrections=new ArrayList<Correction>();
			corrections.add(correction);
			w.putMetadata("corrections", corrections);
		}
	}

	private String object2String(Object o)
	{
		if(o==null)
		{
			return null;
		}
		if(o instanceof String)
		{
			String s=(String)o;
			return s.trim();
		}
		else if(o instanceof Integer)
		{
			Integer i=(Integer)o;
			return i.toString();
		}
		else if(o instanceof Long)
		{
			Long l=(Long)o;
			return l.toString();
		}
		return null;
	}
	
	private Long object2Long(Object o)
	{
		if(o==null)
		{
			return null;
		}
		if(o instanceof Double)
		{
			return ((Double) o).longValue();
		}
		else if(o instanceof Integer)
		{
			return ((Integer) o).longValue();
		}
		else
		{
			return ((Long) o).longValue();
		}
	}

	private void performWidgetAfterChange(Widget widget)
	{
		performWidgetAfterChange=widget;
	}

	private void performWidget(Widget widget)
	{
		StateController.performWidget(widget);
	}
	
	private void insertWidgetAfterChange(Widget widget)
	{
		insertWidgetAfterChange=widget;
	}

	private boolean isAllowedToInsert()
	{
		if(!StateController.isAllowedToInsert())
		{
			StateController.displayMessage("Not allowed to insert more actions");
			return false;
		}
		return true;
	}

	private void insertWidget(Widget widget, AppState nextState)
	{
		if(insertBeforeWidget!=null)
		{
			StateController.getCurrentState().removeWidget(insertBeforeWidget);
			StateController.insertWidget(widget, nextState);
			StateController.getCurrentState().addWidget(insertBeforeWidget);
			insertBeforeWidget=null;
		}
		else
		{
			StateController.insertWidget(widget, nextState);
		}
	}

	private boolean copyWidgetMetadata(Widget fromWidget, Widget toWidget)
	{
		if(fromWidget!=null)
		{
			toWidget.setLocationArea(fromWidget.getLocationArea());
			List<String> keys=fromWidget.getMetadataKeys();
			for(String key:keys)
			{
				toWidget.putMetadata(key, fromWidget.getMetadata(key));
			}
			return true;
		}
		return false;
	}

	private void clearLocalStorage()
	{
		if(webDriver!=null)
		{
			try
			{
				JavascriptExecutor executor = (JavascriptExecutor) webDriver;
				executor.executeScript("localStorage.clear();");
			}
			catch (Exception e)
			{
			}
		}
	}

	public static WebDriver getWebDriver()
	{
		return webDriver;
	}

	private int distanceScore(Rectangle r1, Rectangle r2, int maxDist, int maxScore)
	{
		int x1=(int)r1.getX();
		int y1=(int)r1.getY();
		int x2=(int)r2.getX();
		int y2=(int)r2.getY();
		int deltaX=Math.abs(x1-x2);
		int deltaY=Math.abs(y1-y2);
		if(deltaX==0 && deltaY==0)
		{
			return maxScore;
		}
		int dist=(int)Math.sqrt(deltaX*deltaX+deltaY*deltaY);
		int score=maxDist-dist;
		if(score<0)
		{
			score=0;
		}
		return (score*maxScore)/maxDist;
	}

	/**
	 * Return a score between 0 and maxScore depending on the similarity between t1 and t2.
	 * @param t1
	 * @param t2
	 * @param maxScore
	 * @return A similarity score between 0 and maxScore
	 */
	private static int getScore(String t1, String t2, int maxScore)
	{
		String s1=t1.toLowerCase();
		String s2=t2.toLowerCase();

		// Make sure s1 is longer (or equal)
		if(s1.length()<s2.length())
		{
			String swap = s1;
			s1 = s2;
			s2 = swap;
		}

		if(s1.startsWith(s2) || s1.endsWith(s2))
		{
			int score=(maxScore*s2.length())/s1.length();
			return score;
		}

		if(s1.length()>50 || s2.length()>50)
		{
			// Too long texts to waste resources on
			return 0;
		}

		int editDistance = 0;
		int bigLen = s1.length();
		editDistance = computeDistance(s1, s2);
		if (bigLen == 0)
		{
			return maxScore;
		}
		else
		{
			int score=(bigLen - editDistance) * maxScore / bigLen;
			return score;
		}

	}

	private static int computeDistance(String s1, String s2)
	{
		int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++)
		{
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++)
			{
				if (i == 0)
				{
					costs[j] = j;
				}
				else
				{
					if (j > 0)
					{
						int newValue = costs[j - 1];
						if (s1.charAt(i - 1) != s2.charAt(j - 1))
						{
							newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
						}
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0)
			{
				costs[s2.length()] = lastValue;
			}
		}
		return costs[s2.length()];
	}

	private static int compareClass(String class1, String class2, int maxScore)
	{
		if(class1==null || class2==null || class1.trim().length()==0 || class2.trim().length()==0)
		{
			return 0;
		}
		int totalCount=0;
		int count=0;
		String[] classArray1 = class1.trim().split("\\s+");
		String[] classArray2 = class2.trim().split("\\s+");
		for(String s:classArray1)
		{
			totalCount++;
			if(containsText(classArray2, s.trim()))
			{
				count++;
			}
		}
		for(String s:classArray2)
		{
			totalCount++;
			if(containsText(classArray1, s.trim()))
			{
				count++;
			}
		}
		if(totalCount==0)
		{
			return 0;
		}
		int score=(count*maxScore)/totalCount;
		return score;
	}

	private static int compareWords(String text1, String text2, int maxScore)
	{
		if(text1==null || text2==null || text1.trim().length()==0 || text2.trim().length()==0)
		{
			return 0;
		}
		int totalCount=0;
		int count=0;
		String[] classArray1 = text1.trim().split("\\s+");
		String[] classArray2 = text2.trim().split("\\s+");
		for(String s:classArray1)
		{
			String sTrim=s.trim();
			totalCount+=sTrim.length();
			if(containsText(classArray2, sTrim))
			{
				count+=sTrim.length();
			}
		}
		if(totalCount==0)
		{
			return 0;
		}
		int score=(count*maxScore)/totalCount;
		return score;
	}
	
	private static boolean containsText(String[] array, String text)
	{
		for(String s:array)
		{
			if(text.equalsIgnoreCase(s.trim()))
			{
				return true;
			}
		}
		return false;
	}

	private static int getPathScore(String s1, String s2, int maxScore)
	{
		String shortString=null;
		String longString=null;
		if(s1.length()>s2.length())
		{
			shortString=s2.toLowerCase();
			longString=s1.toLowerCase();
		}
		else
		{
			shortString=s1.toLowerCase();
			longString=s2.toLowerCase();
		}
		for(int i=0; i<shortString.length(); i++)
		{
			char c1=shortString.charAt(i);
			char c2=longString.charAt(i);
			if(c1!=c2)
			{
				// Not the same
				int score=(maxScore*i)/longString.length();
				return score;
			}
		}
		int score=(maxScore*shortString.length())/longString.length();
		return score;
	}

	private String getClip()
	{
		try
		{
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable cliptran = clipboard.getContents(this);
			String text = (String) cliptran.getTransferData(DataFlavor.stringFlavor);
			return text;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private boolean setClip(String text)
	{
		try
		{
			if (text != null)
			{
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection clipString = new StringSelection(text);
				clipboard.setContents(clipString, clipString);
				return true;
			}
		}
		catch (Exception e)
		{
		}
		return false;
	}

	public void generateReport()
	{
		// Make sure we have a reports folder
		File file = new File("reports/" + StateController.getProduct());
		file.mkdirs();

		String filename = "reports/" + StateController.getProduct() + "/" + StateController.getCurrentState().getId() + ".txt";
		String filenamePerformance1 = "reports/" + StateController.getProduct() + "/" + StateController.getCurrentState().getId() + "_performance_multi.txt";
		String filenamePerformance2 = "reports/" + StateController.getProduct() + "/" + StateController.getCurrentState().getId() + "_performance_ececute.txt";
		String filenamePerformance3 = "reports/" + StateController.getProduct() + "/" + StateController.getCurrentState().getId() + "_performance_match.txt";
		StateController.displayMessage("Generating performance test to: "+filename);

		writeLine(filename, "Locators:", false);
		
		checkLocators(StateController.getCurrentState(), filename, filenamePerformance1, filenamePerformance2, filenamePerformance3);
	}

	private void checkLocators(AppState state, String filename, String filenamePerformance1, String filenamePerformance2, String filenamePerformance3)
	{
		List<Widget> visibleWidgets=state.getVisibleWidgets();
		for(Widget widget:visibleWidgets)
		{
			long start=System.currentTimeMillis();
			String xpath=(String)widget.getMetadata("xpath");
			if(xpath!=null)
			{
				List<WebElement> elements=webDriver.findElements(By.xpath(xpath));
				String elementId=elements.size()==1?" LOCATED":" BROKEN: "+elements.size()+" located";
				log(filename, "Xpath: "+xpath+elementId);
			}
			xpath=(String)widget.getMetadata("xpath2");
			if(xpath!=null)
			{
				List<WebElement> elements=webDriver.findElements(By.xpath(xpath));
				String elementId=elements.size()==1?" LOCATED":" BROKEN: "+elements.size()+" located";
				log(filename, "IdXpath: "+xpath+elementId);
			}
			xpath=(String)widget.getMetadata("xpath3");
			if(xpath!=null)
			{
				List<WebElement> elements=webDriver.findElements(By.xpath(xpath));
				String elementId=elements.size()==1?" LOCATED":" BROKEN: "+elements.size()+" located";
				log(filename, "RobulaXpath: "+xpath+elementId);
			}
			xpath=(String)widget.getMetadata("xpath4");
			if(xpath!=null)
			{
				List<WebElement> elements=webDriver.findElements(By.xpath(xpath));
				String elementId=elements.size()==1?" LOCATED":" BROKEN: "+elements.size()+" located";
				log(filename, "RobulaPlusXpath: "+xpath+elementId);
			}
			xpath=(String)widget.getMetadata("xpath5");
			if(xpath!=null)
			{
				List<WebElement> elements=webDriver.findElements(By.xpath(xpath));
				String elementId=elements.size()==1?" LOCATED":" BROKEN: "+elements.size()+" located";
				log(filename, "MonotoXpath: "+xpath+elementId);
			}
			long delta=System.currentTimeMillis()-start;
			log(filenamePerformance1, ""+delta);
			long executeTime=(long)widget.getMetadata("execute_time");
			log(filenamePerformance2, ""+executeTime);
			long matchTime=(long)widget.getMetadata("match_time");
			log(filenamePerformance3, ""+matchTime);

			log(filename, "");
		}
		log(filenamePerformance1, "");
		log(filenamePerformance2, "");
		log(filenamePerformance3, "");
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
