package plugin;

import java.awt.image.BufferedImage;

public class Gray
{
	public BufferedImage processCapture(BufferedImage capture)
	{
		toGray(capture);
		return capture;
	}

	private void toGray(BufferedImage img)
	{
		int width = img.getWidth();
		int height = img.getHeight();

		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				int p = img.getRGB(x, y);
				int a = (p >> 24) & 0xff;
				int r = (p >> 16) & 0xff;
				int g = (p >> 8) & 0xff;
				int b = p & 0xff;
				int avg = (r + g + b) / 3;
				p = (a << 24) | (avg << 16) | (avg << 8) | avg;
				img.setRGB(x, y, p);
			}
		}
	}
}
