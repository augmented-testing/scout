package plugin;

import java.awt.Desktop;
import java.net.URI;

import scout.StateController;

public class Help
{
	public void startTool()
	{
		String helpUrl=StateController.getSystemProperty("help_url", "https://www.youtube.com/watch?v=aPJdt1uBiI8");
		startWeb(helpUrl);
	}

	private static boolean startWeb(String url)
	{
		try
		{
			Desktop.getDesktop().browse(new URI(url));
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
}
