package plugin;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import scout.Action;
import scout.DoubleClickAction;
import scout.DragAction;
import scout.DragStartAction;
import scout.StateController;

public class EasterEgg
{
	private static BasicStroke thinStroke = new BasicStroke(1);
	private static DragStartAction dragStartAction = null;

	public void enablePlugin()
	{
		StateController.displayMessage("Drag to create egg and double-click to remove egg");
	}

	public void performAction(Action action)
	{
		if(!StateController.isRunningSession())
		{
			// Only perform actions during a running session
			return;
		}

		if (action instanceof DragStartAction)
		{
			dragStartAction = (DragStartAction) action;
		}

		if (action instanceof DragAction)
		{
			DragAction dragAction = (DragAction) action;

			if (dragStartAction != null)
			{
				Point start = dragStartAction.getLocation();
				Point end = dragAction.getLocation();
				Rectangle rect = new Rectangle(start,
						new Dimension((int) (end.getX() - start.getX()), (int) (end.getY() - start.getY())));
				StateController.getCurrentState().putMetadata("EasterEggRect", rect);
			}
		}

		if (action instanceof DoubleClickAction)
		{
			StateController.getCurrentState().removeMetadata("EasterEggRect");
		}
	}

	public void paintCaptureForeground(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		Rectangle rect = (Rectangle) StateController.getCurrentState().getMetadata("EasterEggRect");
		if (rect != null)
		{
			drawEgg(g2, rect);
		}
	}

	private void drawEgg(Graphics2D g2, Rectangle rect)
	{
		int x = StateController.getScaledX((int) rect.getX());
		int y = StateController.getScaledY((int) rect.getY());
		int width = StateController.getScaledX((int) rect.getWidth());
		int height = StateController.getScaledY((int) rect.getHeight());

		g2.setStroke(thinStroke);
		g2.setColor(Color.yellow);
		g2.fillOval(x, y, width, height);
		g2.setColor(Color.black);
		g2.drawOval(x, y, width, height);
	}
}
