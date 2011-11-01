package org.imgscalr.op;

import static org.imgscalr.Scalr.DEBUG;
import static org.imgscalr.util.LogUtil.log;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;

public class PadOp implements IOp {
	protected int padding;
	protected Color color;

	public PadOp(int padding, Color color) throws IllegalArgumentException {
		if (padding <= 0)
			throw new IllegalArgumentException("padding [" + padding
					+ "] must be > 0");
		if (color == null)
			throw new IllegalArgumentException("color cannot be null");

		this.padding = padding;
		this.color = color;
	}

	@Override
	public String toString() {
		return getClass().getName() + "@" + hashCode() + " [padding=" + padding
				+ ", color=" + color + "]";
	}

	@Override
	public BufferedImage apply(BufferedImage src)
			throws IllegalArgumentException, ImagingOpException {
		long t = System.currentTimeMillis();

		if (src == null)
			throw new IllegalArgumentException("src cannot be null");

		int srcWidth = src.getWidth();
		int srcHeight = src.getHeight();

		/*
		 * Double the padding to account for all sides of the image. More
		 * specifically, if padding is "1" we add 2 pixels to width and 2 to
		 * height, so we have 1 new pixel of padding all the way around our
		 * image.
		 */
		int sizeDiff = (padding * 2);
		int newWidth = srcWidth + sizeDiff;
		int newHeight = srcHeight + sizeDiff;

		if (DEBUG)
			log(0,
					this,
					"Padding Image from [originalWidth=%d, originalHeight=%d, padding=%d] to [newWidth=%d, newHeight=%d]...",
					srcWidth, srcHeight, padding, newWidth, newHeight);

		boolean colorHasAlpha = (color.getAlpha() != 255);
		boolean imageHasAlpha = (src.getTransparency() != BufferedImage.OPAQUE);

		BufferedImage result;

		// Type of result image must contain alpha if either inputs had it.
		if (colorHasAlpha || imageHasAlpha) {
			if (DEBUG)
				log(1, this,
						"Transparency FOUND in source image or color, using ARGB image type...");

			result = new BufferedImage(newWidth, newHeight,
					BufferedImage.TYPE_INT_ARGB);
		} else {
			if (DEBUG)
				log(1, this,
						"Transparency NOT FOUND in source image or color, using RGB image type...");

			result = new BufferedImage(newWidth, newHeight,
					BufferedImage.TYPE_INT_RGB);
		}

		Graphics g = result.getGraphics();

		// "Clear" the background of the new image with our padding color.
		g.setColor(color);
		g.fillRect(0, 0, newWidth, newHeight);

		// Draw the image into the center of the new padded image.
		g.drawImage(src, padding, padding, null);
		g.dispose();

		if (DEBUG)
			log(0, this, "Padding Applied in %d ms", System.currentTimeMillis()
					- t);

		return result;
	}
}