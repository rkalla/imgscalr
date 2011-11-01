package org.imgscalr.op;

import static org.imgscalr.Scalr.DEBUG;
import static org.imgscalr.util.ImageUtil.createOptimalImage;
import static org.imgscalr.util.LogUtil.log;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;

public class CropOp implements IOp {
	protected int x;
	protected int y;
	protected int width;
	protected int height;

	public CropOp(int x, int y, int width, int height)
			throws IllegalArgumentException {
		if (x < 0 || y < 0 || width < 0 || height < 0)
			throw new IllegalArgumentException("Invalid crop bounds: x [" + x
					+ "], y [" + y + "], width [" + width + "] and height ["
					+ height + "] must all be >= 0");

		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	@Override
	public String toString() {
		return getClass().getName() + "@" + hashCode() + " [x=" + x + ", y="
				+ y + ", width=" + width + ", height=" + height + "]";
	}

	@Override
	public BufferedImage apply(BufferedImage src)
			throws IllegalArgumentException, ImagingOpException {
		long t = System.currentTimeMillis();

		if (src == null)
			throw new IllegalArgumentException("src cannot be null");

		int srcWidth = src.getWidth();
		int srcHeight = src.getHeight();

		if ((x + width) > srcWidth)
			throw new IllegalArgumentException(
					"Invalid crop bounds: x + width [" + (x + width)
							+ "] must be <= src.getWidth() [" + srcWidth + "]");
		if ((y + height) > srcHeight)
			throw new IllegalArgumentException(
					"Invalid crop bounds: y + height [" + (y + height)
							+ "] must be <= src.getHeight() [" + srcHeight
							+ "]");

		if (DEBUG)
			log(0,
					this,
					"Cropping Image [width=%d, height=%d] to [x=%d, y=%d, width=%d, height=%d]...",
					srcWidth, srcHeight, x, y, width, height);

		// Create a target image of an optimal type to render into.
		BufferedImage result = createOptimalImage(src, width, height);
		Graphics g = result.getGraphics();

		/*
		 * Render from the src image (coordinates defined by the crop
		 * dimensions) into the result image (which is exactly the same size as
		 * the crop dimensions).
		 */
		g.drawImage(src, 0, 0, width, height, x, y, (x + width), (y + height),
				null);
		g.dispose();

		if (DEBUG)
			log(0, this, "Cropped Image in %d ms", System.currentTimeMillis()
					- t);

		return result;
	}
}