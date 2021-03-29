// SPDX-FileCopyrightText: 2021 Michel Nass
//
// SPDX-License-Identifier: MIT

package plugin;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

import multilocator.MultiLocator;
import scout.AppState;
import scout.Path;
import scout.StateController;
import scout.StateController.Mode;
import scout.Widget;
import scout.Widget.WidgetSubtype;
import scout.Widget.WidgetType;

public class RemoteControl implements Runnable
{
	private static String[] LOCATORS = {"tag", "class", "type", "name", "id", "value", "href", "text", "placeholder"};
	private static SimpleDateFormat httpDateTime = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
	private static ServerSocket serverSocket;
	private static Thread serverThread;
	private static boolean isRunning=false;

	private PrintStream outStream;
	private int noActions=0;
	private int noValidChecks=0;

	public static int SERVER_PORT = 8080;

	public void paintCaptureForeground(Graphics g)
	{
		if(!isRunning)
		{
			// Start the server
			startServer();
			StateController.displayMessage("Ready for requests on port: "+SERVER_PORT);
		}
	}

	public void disablePlugin()
	{
		stopServer();
		StateController.displayMessage("Server stopped");
	}
	
	private void startServer()
	{
		try
		{
			serverSocket = new ServerSocket(SERVER_PORT);
			serverThread = new Thread(this);
			isRunning=true;
			serverThread.start();
		}
		catch (Throwable e)
		{
			StateController.displayMessage("Failed to initiate server: "+e.getMessage());
		}
	}

	private void stopServer()
	{
		isRunning=false;
		try
		{
			serverSocket.close();
		}
		catch (IOException e)
		{
		}
	}

	public void run()
	{
		while(isRunning)
		{
			try
			{
				Socket socket=serverSocket.accept();
				processRequest(socket);
				socket.close();
			}
			catch (SocketException e)
			{
				return;
			}
			catch (Throwable e)
			{
				continue;
			}
		}
	}

  private void processRequest(Socket socket)
	{
		BufferedOutputStream bufferedOutStream;
		try
		{
			bufferedOutStream = new BufferedOutputStream(socket.getOutputStream());
			outStream = new PrintStream(bufferedOutStream, false, "UTF-8");
		}
		catch (Exception e)
		{
			return;
		}

		try
		{
			HTTPRequest request = new HTTPRequest();

			if (request.readRequest(socket) == false)
			{
				return;
			}

			String command = request.getCommand().trim();

			if("start".equalsIgnoreCase(command))
			{
				String restartParam=request.getParameter("restart");
				if(restartParam!=null && "yes".equalsIgnoreCase(restartParam))
				{
					if(!StateController.isStoppedSession())
					{
						// Stop session first
						StateController.stopSession();
						// Wait for the session to stop
						while(!StateController.isStoppedSession())
						{
							Thread.sleep(1000);
						}
					}
				}
				else if(!StateController.isStoppedSession())
				{
					// Already running
					sendResponse("Session is already running");
					return;
				}
				
				String product=request.getParameter("product");
				if(product!=null)
				{
					StateController.setProduct(product);
				}

				String productVersion = StateController.getProductVersion();
				String testerName = StateController.getTesterName();
				String productView = StateController.getProductView();
				String homeLocator = StateController.getHomeLocator();
				int productViewWidth = StateController.getProductViewWidth();
				int productViewHeight = StateController.getProductViewHeight();
				boolean isHeadlessBrowser=StateController.isHeadlessBrowser();

				String productVersionParam=request.getParameter("product_version");
				if(productVersionParam!=null)
				{
					productVersion=productVersionParam;
				}
				String testerNameParam=request.getParameter("tester_name");
				if(testerNameParam!=null)
				{
					testerName=testerNameParam;
				}
				String productViewParam=request.getParameter("product_view");
				if(productViewParam!=null)
				{
					productView=productViewParam;
				}

				StateController.setMode(Mode.MANUAL);
				StateController.startSession(product, productVersion, testerName, productView, homeLocator, productViewWidth, productViewHeight, isHeadlessBrowser);
				// Wait until the session is running
				while(!StateController.isRunningSession())
				{
					Thread.sleep(1000);
				}
				StateController.resetCoverage();
				StateController.setAutoStopSession(true);
				StateController.setMode(Mode.AUTO);
				// Wait as long as in mode auto
				while(StateController.getMode()==Mode.AUTO)
				{
					Thread.sleep(1000);
				}

				// Determine if test failed
				int percentCovered = StateController.getStateTree().coveredPercent(StateController.getProductVersion());
				boolean failed=percentCovered<100;
				
				sendResponse("Percent covered: "+percentCovered, failed);
				return;
			}
			else if("stop".equalsIgnoreCase(command))
			{
				if(!StateController.isStoppedSession())
				{
					// Stop session
					StateController.stopSession();
					// Wait for the session to stop
					while(!StateController.isStoppedSession())
					{
						Thread.sleep(1000);
					}
					sendResponse("Session Stopped");
				}
				return;
			}
			else if("run".equalsIgnoreCase(command))
			{
				WebDriver webDriver=SeleniumPlugin.getWebDriver(StateController.getProductView());
				if(webDriver!=null)
				{
			  	int width=StateController.getProductViewWidth();
			  	int height=StateController.getProductViewHeight();
			  	webDriver.manage().window().setSize(new Dimension(width, height));
					webDriver.get(StateController.getHomeLocator());
					addCookies(webDriver, StateController.getStateTree());
				}

				// Create the adaptive and self-healing multi-locator
				MultiLocator multiLocator=new MultiLocator(webDriver);

				long start=System.currentTimeMillis();
				noActions=0;
				noValidChecks=0;
				
				Path path = StateController.getStateTree().getLastPerformedPath();
				if(path!=null)
				{
					if(!performWidgets(multiLocator, webDriver, path.getWidgets()))
					{
						long duration=(System.currentTimeMillis()-start)/1000;
						String message="Failed (actions performed: "+noActions+", valid checks: "+noValidChecks+", time: "+duration+" seconds)";
						sendResponse(message);
						return;
					}
				}

				long duration=(System.currentTimeMillis()-start)/1000;
				webDriver.quit();
				String message="Passed (actions performed: "+noActions+", valid checks: "+noValidChecks+", time: "+duration+" seconds)";
				sendResponse(message);
				return;
			}
			else
			{
				sendResponse("Unknown Request");
				return;
			}
		}
		catch (Exception e)
		{
			return;
		}
	}

	private String createHTTPDateString()
	{
		String httpDateString = httpDateTime.format(new Date());
		return httpDateString + " GMT";
	}

	private void sendResponse(String responseMessage)
	{
		sendResponse(responseMessage, "text/html", false);
	}

	private void sendResponse(String responseMessage, boolean error)
	{
		sendResponse(responseMessage, "text/html", error);
	}
	
	private void sendResponse(String responseMessage, String mime, boolean error)
	{
		try
		{
			String response = createHttpResponse(responseMessage, mime+"; charset=UTF-8", error);
			outStream.print(response);
			outStream.flush();
		}
		catch (Exception e)
		{
			System.out.println("Failed to send response: " + e.toString());
		}
		finally
		{
			if (outStream != null)
			{
				outStream.close();
				outStream = null;
			}
		}
	}

	private String createHttpResponse(String data, String contentType, boolean error)
	{
		StringBuffer http = new StringBuffer();

		byte[] dataBytes;
		try
		{
			dataBytes = data.getBytes("UTF-8");
		}
		catch (Exception e)
		{
			// Unsupported
			return null;
		}

		// Create header
		if(error)
		{
			http.append("HTTP/1.1 500 Internal Server Error\r\n");
		}
		else
		{
			http.append("HTTP/1.1 200 OK\r\n");
		}
		http.append("Date: ");
		http.append(createHTTPDateString());
		http.append("\r\n");
		http.append("Server: Mimic Web Service\r\n");
		http.append("Last-Modified: ");
		http.append(createHTTPDateString());
		http.append("\r\n");
		http.append("Accept-Ranges: bytes\r\n");
		http.append("ETag: \"");
		http.append(createHTTPDateString());
		http.append("\"\r\n");
		http.append("Content-Length: ");
		http.append(dataBytes.length);
		http.append("\r\n");
		http.append("Connection: close\r\n");
		http.append("Content-Type: ");
		http.append(contentType);
		http.append("\r\n\r\n");

		// Add data
		http.append(data);

		return http.toString();
	}

	/**
	 * Add cookies from state
	 * @param webDriver
	 * @param The state
	 */
	private void addCookies(WebDriver webDriver, AppState state)
	{
		try
		{
			Object object=state.getMetadata("cookies");
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
						return;
					}
				}
			}
			webDriver.navigate().refresh();
		}
		catch (Throwable e)
		{
			return;
		}
	}

	private boolean performWidgets(MultiLocator multiLocator, WebDriver webDriver, List<Widget> widgets)
	{
		for (Widget widget : widgets)
		{
			try
			{
				if (widget.getWidgetType() == WidgetType.ACTION)
				{
					if (widget.getWidgetSubtype() == WidgetSubtype.LEFT_CLICK_ACTION)
					{
						Properties p=getWidgetProperties(widget);
						multiLocator.findElement(p).click();
						noActions++;
					}
					else if (widget.getWidgetSubtype() == WidgetSubtype.TYPE_ACTION)
					{
						Properties p=getWidgetProperties(widget);
						multiLocator.findElement(p).sendKeys(widget.getText());
						noActions++;
					}
					else if (widget.getWidgetSubtype() == WidgetSubtype.SELECT_ACTION)
					{
						String optionText=(String)widget.getMetadata("option_text");
						String optionValue=(String)widget.getMetadata("option_value");
						if(optionValue!=null && optionValue.trim().length()>0 && !widget.isIgnoredMetadata("option_value"))
						{
							Properties p=getWidgetProperties(widget);
							(new Select(multiLocator.findElement(p))).selectByValue(optionValue);
							noActions++;
						}
						else if(optionText!=null && optionText.trim().length()>0 && !widget.isIgnoredMetadata("option_text"))
						{
							Properties p=getWidgetProperties(widget);
							(new Select(multiLocator.findElement(p))).selectByVisibleText(optionText);
							noActions++;
						}
					}
					else if (widget.getWidgetSubtype() == WidgetSubtype.GO_HOME_ACTION)
					{
						if("yes".equalsIgnoreCase(StateController.getProductProperty("home_clear_browser", "yes")))
						{
							multiLocator.clearLocalStorage();
						}
						webDriver.get(StateController.getHomeLocator());
						noActions++;
					}
				}

				AppState state=widget.getNextState();
				if(state!=null)
				{
					if(!state.isHome())
					{
						addCookies(webDriver, state);
					}
					List<Widget> allWidgets=state.getNonHiddenWidgets();
					for (Widget w : allWidgets)
					{
						if(w.getWidgetType()==WidgetType.CHECK && w.getValidExpression()!=null && !w.getValidExpression().contains("\n"))
						{
							Properties p=getWidgetProperties(w);
							boolean check=multiLocator.checkElement(p, w.getValidExpression());
							if(check)
							{
								noValidChecks++;
							}
							else
							{
								return false;
							}
						}
					}
				}

			}
			catch(Exception e)
			{
				return false;
			}
		}
		return true;
	}

	private Properties getWidgetProperties(Widget widget)
	{
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
			p.setProperty("x", ""+(int)area.getX());
			p.setProperty("y", ""+(int)area.getY());
			p.setProperty("width", ""+(int)area.getWidth());
			p.setProperty("height", ""+(int)area.getHeight());
		}
		
		return p;
	}
}
