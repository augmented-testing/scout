package plugin;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import scout.Action;
import scout.StateController;
import scout.TypeAction;

public class ManualInstruction
{
	private static BasicStroke mediumStroke=new BasicStroke(4);
	private static Font textBoxSmallFont = new Font("Arial", Font.PLAIN, 20);
	private static Color textBoxRectangleBackgroundColor = new Color(0, 0, 0, 160);
	private static Color textBoxRectangleForegroundColor = Color.white;
	private static Color transparentRedColor = new Color(255, 0, 0, 160);

	private static List<String> texts=null;
	private static int currentTextNo=0;

	public void startSession()
	{
		texts=null;
		currentTextNo=0;
		
		// Load manual instructions
		File file=new File("data/"+StateController.getProduct()+"/manual.txt");
		if(file.exists())
		{
			texts=readLines(file);
		}
	}

	public void enablePlugin()
	{
		FileDialog fd = new FileDialog((Dialog)null, "Select text file with manual instructions", FileDialog.LOAD);
		fd.setFile("*.txt");
		fd.setMultipleMode(false);
		fd.setVisible(true);
		File[] files=fd.getFiles();
		if(files.length>0)
		{
			try
			{
				// Copy the file
				File file=files[0];
				File destFile=new File("data/"+StateController.getProduct()+"/manual.txt");
				Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			catch (IOException e)
			{
			}
		}

		startSession();
	}
	
	public void paintCaptureForeground(Graphics g)
	{
		if(StateController.isOngoingSession() && texts!=null)
		{
			Graphics2D g2 = (Graphics2D)g;

			int x=StateController.getMouseScaledX();
			int y=StateController.getMouseScaledY();

			String text=(currentTextNo+1)+". "+texts.get(currentTextNo);
			if(text!=null && x>0 && y>0 && x<StateController.getCaptureScaledWidth() && y<StateController.getCaptureScaledHeight())
			{
				drawTextBox(g2, text, x, y+150, 0, 40, 5);
			}
		}
	}

	public void performAction(Action action)
	{
		if(!StateController.isRunningSession())
		{
			// Only perform actions during a running session
			return;
		}

		if(texts!=null && action instanceof TypeAction)
		{
			TypeAction typeAction=(TypeAction)action;
			KeyEvent keyEvent=typeAction.getKeyEvent();
			int keyCode = keyEvent.getKeyCode();
			if(keyCode==KeyEvent.VK_LEFT)
			{
				currentTextNo--;
				if(currentTextNo<0)
				{
					currentTextNo=0;
				}
			}
			else if(keyCode==KeyEvent.VK_RIGHT)
			{
				currentTextNo++;
				if(currentTextNo>=texts.size())
				{
					currentTextNo=texts.size()-1;
				}
			}
		}		
	}
	
	/**
	 * Draw a text box
	 * @param g2
	 * @param text
	 * @param x
	 * @param y
	 * @param deltaY
	 * @param maxCharsPerLine
	 * @param radius
	 */
	private void drawTextBox(Graphics2D g2, String text, int x, int y, int deltaY, int maxCharsPerLine, int radius)
	{
		g2.setFont(textBoxSmallFont);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// Determine if the text box should appear on the left side
		boolean leftSide=false;
		int scaledWidth=StateController.getCaptureScaledWidth();
		if(x>scaledWidth/2)
		{
			leftSide=true;
		}

		FontMetrics fm = g2.getFontMetrics();
		int textHeight = fm.getHeight();
		int textAscent = fm.getMaxAscent();

		int margin=10;
		int marginDist=10;

		String first=text;
		String last=null;
		int index=text.indexOf("<br>");
		if(index>=0)
		{
			first=text.substring(0, index);
			last=text.substring(index+4);
		}

		List<String> justifiedLines=justify(first, maxCharsPerLine);
		List<String> lines=new ArrayList<String>();
		for(String line:justifiedLines)
		{
			lines.add(line);
		}

		if(last!=null)
		{
			List<String> justifiedLastLines=justify(last, maxCharsPerLine);
			for(String line:justifiedLastLines)
			{
				lines.add(line);
			}
		}

		int textBoxWidth=getMaxWidth(g2, lines);
		int boxHeight=textHeight+margin*2+(lines.size()-1)*(textHeight+5);

		int boxWidth=textBoxWidth+margin*2;
		int boxStartX;
		int boxStartY;
		int textStartX;
		int textStartY;

		boxStartY=y-boxHeight/2;
		if(boxStartY<0)
		{
			boxStartY=0;
		}
		textStartY=boxStartY+margin*2-textHeight/2+textAscent;
		if(leftSide)
		{
			boxStartX=x-radius-margin-boxWidth-marginDist;
			textStartX=boxStartX+margin;
		}
		else
		{
			boxStartX=x+radius+margin+marginDist;
			textStartX=boxStartX+margin;
		}

		if(boxStartX<0)
		{
			boxStartX=0;
		}
		if(boxStartX+boxWidth>scaledWidth)
		{
			boxStartX=scaledWidth-boxWidth;
		}
		
		// Fill text background
		g2.setColor(textBoxRectangleBackgroundColor);
		g2.fillRect(boxStartX, boxStartY, boxWidth, boxHeight);

		// Draw text
		g2.setColor(textBoxRectangleForegroundColor);
		for(String line:lines)
		{
			g2.drawString(line, textStartX, textStartY);
			textStartY+=textHeight+5;
		}

		g2.setColor(transparentRedColor);
		g2.setStroke(mediumStroke);

		if(currentTextNo>0)
		{
			// Draw left arrow
			g2.drawLine(boxStartX-20, boxStartY+boxHeight/2, boxStartX-10, boxStartY+boxHeight/2-10);
			g2.drawLine(boxStartX-20, boxStartY+boxHeight/2, boxStartX-10, boxStartY+boxHeight/2+10);
		}

		if(currentTextNo<texts.size()-1)
		{
			// Draw right arrow
			g2.drawLine(boxStartX+boxWidth+20, boxStartY+boxHeight/2, boxStartX+boxWidth+10, boxStartY+boxHeight/2-10);
			g2.drawLine(boxStartX+boxWidth+20, boxStartY+boxHeight/2, boxStartX+boxWidth+10, boxStartY+boxHeight/2+10);
		}
	}

	/**
	 * Split a text into lines of max limit length trying not to break words
	 * @param s
	 * @param limit
	 * @return A list of lines
	 */
	private List<String> justify(String s, int limit)
	{
		List<String> lines=new ArrayList<String>();
		StringBuilder justifiedLine = new StringBuilder();
		String[] words = s.split(" ");
		for (int i = 0; i < words.length; i++)
		{
			justifiedLine.append(words[i]).append(" ");
			if (i + 1 == words.length || justifiedLine.length() + words[i + 1].length() > limit)
			{
				justifiedLine.deleteCharAt(justifiedLine.length() - 1);
				String nextLine=justifiedLine.toString();
				List<String> subLines=splitEqually(nextLine, limit);
				for(String subLine:subLines)
				{
					lines.add(subLine);
				}
				justifiedLine = new StringBuilder();
			}
		}
		return lines;
	}

	/**
	 * Split a text into multiple lines with max size
	 * @param text
	 * @param size
	 * @return A number of lines with max size
	 */
	private List<String> splitEqually(String text, int size)
	{
		List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);
		for (int start = 0; start < text.length(); start += size)
		{
			ret.add(text.substring(start, Math.min(text.length(), start + size)));
		}
		return ret;
	}

	/**
	 * @param g2
	 * @param lines
	 * @return The width of the widest line
	 */
	private int getMaxWidth(Graphics2D g2, List<String> lines)
	{
		int maxWidth=0;
		FontMetrics fm = g2.getFontMetrics();
		for(String line:lines)
		{
			int textWidth = fm.stringWidth(line);
			if(textWidth>maxWidth)
			{
				maxWidth=textWidth;
			}
		}
		return maxWidth;
	}

	private List<String> readLines(File file)
	{
		List<String> lines=new ArrayList<String>();
		try
		{
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine())
			{
				String line=scanner.nextLine().trim();
				if(line.length()>0)
				{
					lines.add(line);
				}
			}
			scanner.close();
		}
		catch (Exception e)
		{
		}
		return lines;
	}
}
