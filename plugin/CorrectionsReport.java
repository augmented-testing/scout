package plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import scout.AppState;
import scout.Correction;
import scout.StateController;
import scout.Widget;

public class CorrectionsReport
{
	public void generateReport()
	{
		// Make sure we have a reports folder
		File file = new File("reports/" + StateController.getProduct());
		file.mkdirs();

		List<Correction> corrections=getCorrections(StateController.getStateTree());
		Collections.sort(corrections);

		if(corrections.size()==0)
		{
			StateController.displayMessage("There are no corrections");
		}
		else
		{
			StateController.displayMessage("Generating "+corrections.size()+" corrections");
			
			String filename="reports/" + StateController.getProduct()+"/corrections.txt";
			writeLine(filename, "Corrections:", false);
			
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd h:mm:ss");

			for(Correction correction:corrections)
			{
				String dateStr=df.format(new Date(correction.getTimestamp()));
				appendLine(filename, dateStr+":");
				List<String> keys=correction.getMetadataKeys();
				for(String key:keys)
				{
					String value=(String)correction.getMetadata(key);
					appendLine(filename, key+"="+value);
				}
			}

			StateController.displayMessageHide();
		}
	}

	private List<Correction> getCorrections(AppState state)
	{
		List<Correction> corrections=new ArrayList<Correction>();
		List<Widget> widgets=state.getAllIncludingChildWidgets();
		for(Widget widget:widgets)
		{
			Object correctionsObject=widget.getMetadata("corrections");
			if(correctionsObject!=null)
			{
				List<Correction> widgetCorrections=(List<Correction>)correctionsObject;
				corrections.addAll(widgetCorrections);
			}
		}
		return corrections;
	}
	
	private void appendLine(String logFilename, String text)
	{
		writeLine(logFilename, text, true);
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
