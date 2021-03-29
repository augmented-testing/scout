// SPDX-FileCopyrightText: 2021 Michel Nass
//
// SPDX-License-Identifier: MIT

package plugin;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import scout.PluginController;
import scout.StateController;
import scout.Widget;
import scout.Widget.WidgetStatus;
import scout.Widget.WidgetSubtype;
import scout.Widget.WidgetType;
import scout.Widget.WidgetVisibility;

public class HoverInfo
{
	private static Font textBoxSmallFont = new Font("Arial", Font.PLAIN, 20);
	private static Color textBoxRectangleBackgroundColor = new Color(0, 0, 0, 160);
	private static Color textBoxRectangleForegroundColor = Color.white;
	private static Color commentForegroundColor = Color.green;
	private static Color helpForegroundColor = Color.yellow;

	public void paintCaptureForeground(Graphics g)
	{
		if(StateController.isOngoingSession() && !StateController.isToolbarVisible())
		{
			Graphics2D g2=(Graphics2D)g;
			g2.setFont(textBoxSmallFont);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int x=StateController.getMouseX();
			int y=StateController.getMouseY();
			String text=getHoverText(x, y);
			if(text!=null)
			{
				drawTextBox(g2, text, x, y, 40, 5);
			}
		}
	}

	/**
	 * @param mouseX
	 * @param mouseY
	 * @return Mouse hover text for mouse location
	 */
	private String getHoverText(int mouseX, int mouseY)
	{
		Widget widget=StateController.getWidgetAt(StateController.getCurrentState(), mouseX, mouseY);
		if(widget!=null)
		{
			if(widget.getWidgetType()==WidgetType.ACTION)
			{
				String action="Unknown";
				if(widget.getWidgetSubtype()==WidgetSubtype.LEFT_CLICK_ACTION || widget.getWidgetSubtype()==WidgetSubtype.LONG_CLICK_ACTION || widget.getWidgetSubtype()==WidgetSubtype.MOVE_ACTION)
				{
					if(widget.getWidgetSubtype()==WidgetSubtype.LONG_CLICK_ACTION)
					{
						action="Long Click";
					}
					else if(widget.getWidgetSubtype()==WidgetSubtype.MOVE_ACTION)
					{
						action="Move";
					}
					else
					{
						action="Click";
					}
					String text=(String)widget.getMetadata("text");
					String href=(String)widget.getMetadata("href");
					if(text!=null && text.trim().length()>0 && text.trim().length()<=60)
					{
						action+=" \""+text.trim()+"\"";
					}
					else if(href!=null && href.trim().length()>0 && href.trim().length()<=60)
					{
						action+=" \""+href.trim()+"\"";
					}

					if(StateController.getKeyboardInput().length()>0)
					{
						// There is keyboard input
						action+="<br>\""+StateController.getKeyboardInput()+"\"";
					}
					if(PluginController.isPluginEnabled("plugin.Help"))
					{
						action+="<help>Click to perform action";
					}
				}
				else if(widget.getWidgetSubtype()==WidgetSubtype.TYPE_ACTION)
				{
					if(StateController.getKeyboardInput().length()>0)
					{
						// There is keyboard input - display that instead
						action="Type \""+StateController.getKeyboardInput()+"\"";
						if(PluginController.isPluginEnabled("plugin.Help"))
						{
							action+="<help>Click or press enter to type";
						}
					}
					else
					{
						String text=widget.getText();
						if(text!=null && text.trim().length()>0)
						{
							action="Type \""+text+"\"";
							if(PluginController.isPluginEnabled("plugin.Help"))
							{
								action+="<help>Click to type or enter a new text using the keyboard";
							}
						}
						else
						{
							action="Type";
							if(PluginController.isPluginEnabled("plugin.Help"))
							{
								action+="<help>Enter a text using the keyboard";
							}
						}
					}
				}
				else if(widget.getWidgetSubtype()==WidgetSubtype.SELECT_ACTION)
				{
					String text=(String)widget.getMetadata("option_text");
					if(text!=null && text.trim().length()>0)
					{
						action="Select \""+text+"\"";
					}
					else
					{
						action="Select";
					}

					if(StateController.getKeyboardInput().length()>0)
					{
						// There is keyboard input
						action+="<br>\""+StateController.getKeyboardInput()+"\"";
					}

					if(PluginController.isPluginEnabled("plugin.Help"))
					{
						action+="<help>Use the scroll wheel or up/down keys to select another option";
					}
				}
				else if(widget.getWidgetSubtype()==WidgetSubtype.GO_HOME_ACTION)
				{
					action="Go Home";
					if(PluginController.isPluginEnabled("plugin.Help"))
					{
						action+="<help>Go back to the Home state";
					}
				}
				return action;
			}
			else if(widget.getWidgetType()==WidgetType.CHECK)
			{
				String action="Check";
				if(StateController.getKeyboardInput().length()>0 && widget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
				{
					// There is keyboard input - display that instead
					action="Report Issue \""+StateController.getKeyboardInput()+"\"";
					if(PluginController.isPluginEnabled("plugin.Help"))
					{
						action+="<help>Click or press enter to report an issue";
					}
				}
				else
				{
					if(widget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
					{
						if(widget.getWidgetStatus()==WidgetStatus.VALID)
						{
							if(widget.getComment()!=null && widget.getComment().trim().length()>0 && widget.getComment().trim().length()<=100)
							{
								action+="<br>"+widget.getComment();
							}
							else if(widget.getValidExpression()!=null)
							{
								action+="<br>"+widget.getValidExpression();
							}
							if(PluginController.isPluginEnabled("plugin.Help"))
							{
								action+="<help>Press delete to remove the check";
							}
						}
						else if(widget.getWidgetStatus()==WidgetStatus.LOCATED)
						{
							if(widget.getValidExpression()!=null)
							{
								action+="<br>Expression \""+widget.getValidExpression()+"\" is false.";
							}
							if(PluginController.isPluginEnabled("plugin.Help"))
							{
								action+="<help>Enter a comment to report an issue or Click to auto correct";
							}
						}
						else
						{
							if(PluginController.isPluginEnabled("plugin.Help"))
							{
								action+="<help>Enter a comment to report an issue";
							}
						}
					}
					else
					{
						if(widget.getComment()!=null && widget.getComment().trim().length()>0 && widget.getComment().trim().length()<=100)
						{
							action+="<br>"+widget.getComment();
						}
						else if(widget.getValidExpression()!=null)
						{
							action+="<br>"+widget.getValidExpression();
						}
						if(PluginController.isPluginEnabled("plugin.Help"))
						{
							action+="<help>Click to add a check";
						}
					}
				}
				return action;
			}
			else if(widget.getWidgetType()==WidgetType.ISSUE)
			{
				String action="Issue";
				if(widget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
				{
					action="Issue \""+widget.getReportedText()+"\"";
					if(PluginController.isPluginEnabled("plugin.Help"))
					{
						action+="<help>Click to resolve the issue";
					}
				}
				return action;
			}
		}
		else
		{
			// No widget selected
			if(StateController.getKeyboardInput().length()>0)
			{
				// There is keyboard input - display that instead
				String action="\""+StateController.getKeyboardInput()+"\"";
				if(PluginController.isPluginEnabled("plugin.Help"))
				{
					action+="<help>Press escape to reset text";
				}
				return action;
			}
		}
		return null;
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
	private void drawTextBox(Graphics2D g2, String text, int x, int y, int maxCharsPerLine, int radius)
	{
		x=StateController.getScaledX(x);
		y=StateController.getScaledY(y);

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
		int linebreakIndex=0;
		int linebreakHelpIndex=0;

		String first=text;
		String last=null;
		String help=null;
		int index=text.indexOf("<br>");
		int helpIndex=text.indexOf("<help>");
		if(index>=0)
		{
			first=text.substring(0, index);
			if(helpIndex>=0)
			{
				last=text.substring(index+4, helpIndex);
				help=text.substring(helpIndex+6);
			}
			else
			{
				last=text.substring(index+4);
			}
		}
		else if(helpIndex>=0)
		{
			first=text.substring(0, helpIndex);
			help=text.substring(helpIndex+6);
		}

		List<String> justifiedLines=justify(first, maxCharsPerLine);
		List<String> lines=new ArrayList<String>();
		for(String line:justifiedLines)
		{
			lines.add(line);
		}
		
		linebreakIndex=lines.size();
		
		if(last!=null)
		{
			List<String> justifiedLastLines=justify(last, maxCharsPerLine);
			for(String line:justifiedLastLines)
			{
				lines.add(line);
			}
		}
		
		linebreakHelpIndex=lines.size();
		
		if(help!=null)
		{
			List<String> justifiedLastLines=justify(help, maxCharsPerLine);
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
		int lineNo=0;
		for(String line:lines)
		{
			if(linebreakIndex>0 && linebreakIndex==lineNo)
			{
				g2.setColor(commentForegroundColor);
			}
			if(linebreakHelpIndex>0 && linebreakHelpIndex==lineNo)
			{
				g2.setColor(helpForegroundColor);
			}
			g2.drawString(line, textStartX, textStartY);
			textStartY+=textHeight+5;
			lineNo++;
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
}
