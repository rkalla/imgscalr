package org.imgscalr.op;

import static org.imgscalr.Scalr.DEBUG;
import static org.imgscalr.util.ImageUtil.createOptimalImage;
import static org.imgscalr.util.LogUtil.log;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;

import org.imgscalr.Scalr.Rotation;

public class RotateOp implements IOp {
	protected Rotation rotation;

	public RotateOp(Rotation rotation) throws IllegalArgumentException {
		if (rotation == null)
			throw new IllegalArgumentException("rotation cannot be null");

		this.rotation = rotation;
	}

	@Override
	public BufferedImage apply(BufferedImage src)
			throws IllegalArgumentException, ImagingOpException {
		long t = System.currentTimeMillis();

		if (src == null)
			throw new IllegalArgumentException("src cannot be null");

		if (DEBUG)
			log(0, this, "Rotating Image [%s]...", rotation);

		/*
		 * Setup the default width/height values from our image.
		 * 
		 * In the case of a 90 or 270 (-90) degree rotation, these two values
		 * flip-flop and we will correct those cases down below in the switch
		 * statement.
		 */
		int newWidth = src.getWidth();
		int newHeight = src.getHeight();

		/*
		 * We create a transform per operation request as (oddly enough) it ends
		 * up being faster for the VM to create, use and destroy these instances
		 * than it is to re-use a single AffineTransform per-thread via the
		 * AffineTransform.setTo(...) methods which was my first choice (less
		 * object creation); after benchmarking this explicit case and looking
		 * at just how much code gets run inside of setTo() I opted for a new AT
		 * for every rotation.
		 * 
		 * Safely reusing AffineTransforms like that would have required
		 * ThreadLocal instances to avoid race conditions where two or more
		 * resize threads are manipulating the same transform before applying
		 * it.
		 * 
		 * ThreadLocals are one of the #1 reasons for memory leaks in server
		 * applications and since we have no nice way to hook into the
		 * init/destroy Servlet cycle or any other initialization cycle for this
		 * library to automatically call ThreadLocal.remove() to avoid the
		 * memory leak, it would have made using this library *safely* on the
		 * server side much harder.
		 * 
		 * So we opt for creating individual transforms per rotation op and let
		 * the VM clean them up in a GC.
		 */
		AffineTransform tx = new AffineTransform();

		switch (rotation) {
		case CW_90:
			/*
			 * A 90 or -90 degree rotation will cause the height and width to
			 * flip-flop from the original image to the rotated one.
			 */
			newWidth = src.getHeight();
			newHeight = src.getWidth();

			// Reminder: newWidth == result.getHeight() at this point
			tx.translate(newWidth, 0);
			tx.rotate(Math.toRadians(90));

			break;

		case CW_270:
			/*
			 * A 90 or -90 degree rotation will cause the height and width to
			 * flip-flop from the original image to the rotated one.
			 */
			newWidth = src.getHeight();
			newHeight = src.getWidth();

			// Reminder: newHeight == result.getWidth() at this point
			tx.translate(0, newHeight);
			tx.rotate(Math.toRadians(-90));
			break;

		case CW_180:
			tx.translate(newWidth, newHeight);
			tx.rotate(Math.toRadians(180));
			break;

		case FLIP_HORZ:
			tx.translate(newWidth, 0);
			tx.scale(-1.0, 1.0);
			break;

		case FLIP_VERT:
			tx.translate(0, newHeight);
			tx.scale(1.0, -1.0);
			break;
		}

		// Create our target image we will render the rotated result to.
		BufferedImage result = createOptimalImage(src, newWidth, newHeight);
		Graphics2D g2d = (Graphics2D) result.createGraphics();

		/*
		 * Render the resultant image to our new rotatedImage buffer, applying
		 * the AffineTransform that we calculated above during rendering so the
		 * pixels from the old position are transposed to the new positions in
		 * the resulting image.
		 */
		g2d.drawImage(src, tx, null);
		g2d.dispose();

		if (DEBUG)
			log(0, this,
					"Rotation Applied in %d ms, result [width=%d, height=%d]",
					System.currentTimeMillis() - t, result.getWidth(),
					result.getHeight());

		return result;
	}
}