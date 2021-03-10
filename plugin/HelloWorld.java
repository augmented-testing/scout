package plugin;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import scout.Action;
import scout.AppState;
import scout.DoubleClickAction;
import scout.DragAction;
import scout.DragDropAction;
import scout.DragStartAction;
import scout.GoHomeAction;
import scout.LeftClickAction;
import scout.LongClickAction;
import scout.MiddleClickAction;
import scout.MouseScrollAction;
import scout.RightClickAction;
import scout.StateController;
import scout.TripleClickAction;
import scout.TypeAction;
import scout.Widget;

public class HelloWorld
{
	private static Font textBoxFont = new Font("Arial", Font.PLAIN, 32);

	/**
	 * Called when the plugin is enabled
	 */
	public void enablePlugin()
	{
		StateController.displayMessage("Plugin Enabled");
	}

	/**
	 * Called when the plugin is disabled
	 */
	public void disablePlugin()
	{
		StateController.displayMessage("Plugin Disabled");
	}

	/**
	 * Called when the session begins
	 */
	public void startSession()
	{
		StateController.displayMessage("Session Started");
	}

	/**
	 * Called when the session is stopped
	 */
	public void stopSession()
	{
	}

	/**
	 * Called when the session is paused
	 */
	public void pauseSession()
	{
		StateController.displayMessage("Session Paused");
	}

	/**
	 * Called when the session is resumed after being paused
	 */
	public void resumeSession()
	{
		StateController.displayMessage("Session Resumed");
	}

	/**
	 * Called when the state changes
	 */
	public void changeState()
	{
		StateController.displayMessage("Changed State");
	}

	/**
	 * Called periodically to update the state
	 */
	public void updateState()
	{
	}

	/**
	 * Called periodically to update widget classifications.
	 * This method is called just before updateSuggestions
	 */
	public void updateClassifications()
	{
	}

	/**
	 * Update the suggestions given by this plugin
	 */
	public void updateSuggestions()
	{
		AppState currentState=StateController.getCurrentState();
		List<Widget> suggestions=new ArrayList<Widget>();
		currentState.replaceSuggestedWidgets(suggestions, "HelloWorld");
	}

	/**
	 * Called when the user performs an action like a mouse or keyboard event
	 * @param action
	 */
	public void performAction(Action action)
	{
		if(!StateController.isRunningSession())
		{
			// Only perform actions during a running session
			return;
		}

		if(action instanceof GoHomeAction)
		{
			StateController.displayMessage("GoHome");
		}

		if(action instanceof MouseScrollAction)
		{
			StateController.displayMessage("MouseScroll");
		}

		if(action instanceof TypeAction)
		{
		}
		
		if(action instanceof LeftClickAction)
		{
			StateController.displayMessage("LeftClick");
		}

		if(action instanceof LongClickAction)
		{
			StateController.displayMessage("LongClick");
		}

		if(action instanceof MiddleClickAction)
		{
			StateController.displayMessage("MiddleClick");
		}
		
		if(action instanceof RightClickAction)
		{
			StateController.displayMessage("RightClick");
		}
		
		if(action instanceof DoubleClickAction)
		{
			StateController.displayMessage("DoubleClick");
		}
		
		if(action instanceof TripleClickAction)
		{
			StateController.displayMessage("TripleClick");
		}

		if (action instanceof DragStartAction)
		{
			StateController.displayMessage("DragStart");
		}

		if (action instanceof DragAction)
		{
		}

		if (action instanceof DragDropAction)
		{
			StateController.displayMessage("DragDrop");
		}
	}

	/**
	 * Called when a report should be generated
	 * Adding this method will display this plugin in the Reports drop-down
	 */
	public void generateReport()
	{
		// Make sure we have a reports folder
		File file=new File("reports/"+StateController.getProduct());
		file.mkdirs();
		
		// Log one line to the report
		String filename="reports/"+StateController.getProduct()+"/hello.txt";
		log(filename, "Hello World");

		StateController.displayMessage("Generating report");
	}

	/**
	 * Starts a tool from the Tools menu
	 */
	public void startTool()
	{
		StateController.displayMessage("StartTool");
	}

	/**
	 * Get all the available product views
	 * @return An array of product views
	 */
/*
	public String[] getProductViews()
	{
		return new String[] {"Chrome v80", "Chrome v79", "Chrome v78", "Chrome v77", "Firefox"};
	}
*/

	/**
	 * Store all cookies and other state information
	 */
/*
	public void storeHomeState()
	{
	}
*/

	/**
	 * Get the screen capture of the System Under Test (SUT)
	 * @return A capture of the SUT or null if no capture
	 */
/*
	public BufferedImage getCapture()
	{
	}
*/

	/**
	 * Process the capture after it has been captured
	 * @param capture
	 * @return A processed capture or null if no change
	 */
	public BufferedImage processCapture(BufferedImage capture)
	{
		return capture;
	}

	/**
	 * Called by the PluginController when time to draw graphics
	 * @param g
	 */
	public void paintCapture(Graphics g)
	{
	}

	/**
	 * Called by the PluginController when time to draw foreground graphics
	 * @param g
	 */
	public void paintCaptureForeground(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.white);
		g2.setFont(textBoxFont);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		String text="Hello";
		FontMetrics fm = g2.getFontMetrics();
		int textHeight = fm.getHeight();
		int textWidth = fm.stringWidth(text);
		int x=StateController.getVisibleWidth()/2-textWidth/2;
		int y=StateController.getVisibleY()+StateController.getVisibleHeight()/2-textHeight/2;
		g2.drawString(text, x, y);
	}

	/**
	 * Called by the PluginController when time to draw toolbar graphics
	 * @param g
	 */
	public void paintCaptureToolbar(Graphics g)
	{
	}

	/**
	 * Save the state tree for the current product
	 * @return true if done
	 */
/*
	public Boolean saveState()
	{
		AppState stateTree=StateController.getStateTree();
		String product=StateController.getProduct();
		return true;
	}
*/
	
	/**
	 * Load state tree for the current product or create a new home state if not found
	 * @return A state tree
	 */
/*
	public AppState loadState()
	{
		String product=StateController.getProduct();
		return new AppState("0", "Home");
	}
*/

	private void log(String logFilename, String message)
	{
		writeLine(logFilename, message, true);
	}

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
		}
	}
}
