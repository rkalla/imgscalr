package org.imgscalr.util;

import java.awt.Graphics;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

/*
 * TODO: Not sure if scaleImage should be moved in here from Scalr, because then
 * it is very confusing if THIS scaleImage should be used or if the one from
 * Scalr should be used for anyone reading the javadoc.
 * 
 *  If scaleImage isn't moved here, then neither should scaleIncrementally
 *  be moved here.
 *  
 *  
 *  Those are the bread and butter of the Scalr class, util classes should only
 *  be helpers.
 *  
 *  TODO: The contract for createOptimalImage implies a copy operation because
 *  the source IS being passed in. This is confusing.
 *  
 *  TODO: similarly the contract for ensureOptimalImage has the same contract
 *  but DOES perform a copy... this needs to be fixed up.
 *  
 *  
 *  
 *  TODO: copyToOptimalImage should likely be the only method that *always* performs
 *  a copy and it is up to the caller to be smart enough to call isOptimal to check
 *  if it is already optimal.
 *  
 *  TODO: getOptimalImageType()
 */
public class ImageUtil {
	public static boolean isOptimalImage(BufferedImage src)
			throws IllegalArgumentException {
		if (src == null)
			throw new IllegalArgumentException("src cannot be null");

		int type = src.getType();
		return (type == BufferedImage.TYPE_INT_RGB || type == BufferedImage.TYPE_INT_ARGB);
	}

	/**
	 * Used to create a {@link BufferedImage} with the most optimal RGB TYPE (
	 * {@link BufferedImage#TYPE_INT_RGB} or {@link BufferedImage#TYPE_INT_ARGB}
	 * ) capable of being rendered into from the given <code>src</code>. The
	 * width and height of both images will be identical.
	 * <p/>
	 * This does not perform a copy of the image data from <code>src</code> into
	 * the result image; see {@link #copyToOptimalImage(BufferedImage)} for
	 * that.
	 * <p/>
	 * We force all rendering results into one of these two types, avoiding the
	 * case where a source image is of an unsupported (or poorly supported)
	 * format by Java2D causing the rendering result to end up looking terrible
	 * (common with GIFs) or be totally corrupt (e.g. solid black image).
	 * <p/>
	 * Originally reported by Magnus Kvalheim from Movellas when scaling certain
	 * GIF and PNG images.
	 * 
	 * @param src
	 *            The source image that will be analyzed to determine the most
	 *            optimal image type it can be rendered into.
	 * 
	 * @return a new {@link BufferedImage} representing the most optimal target
	 *         image type that <code>src</code> can be rendered into.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>src</code> is <code>null</code>.
	 * 
	 * @see <a
	 *      href="http://www.mail-archive.com/java2d-interest@capra.eng.sun.com/msg05621.html">How
	 *      Java2D handles poorly supported image types</a>
	 * @see <a
	 *      href="http://code.google.com/p/java-image-scaling/source/browse/trunk/src/main/java/com/mortennobel/imagescaling/MultiStepRescaleOp.java">Thanks
	 *      to Morten Nobel for implementation hint</a>
	 */
	public static BufferedImage createOptimalImage(BufferedImage src)
			throws IllegalArgumentException {
		if (src == null)
			throw new IllegalArgumentException("src cannot be null");

		return createOptimalImage(src, src.getWidth(), src.getHeight());
	}

	/**
	 * Used to create a {@link BufferedImage} with the given dimensions and the
	 * most optimal RGB TYPE ( {@link BufferedImage#TYPE_INT_RGB} or
	 * {@link BufferedImage#TYPE_INT_ARGB} ) capable of being rendered into from
	 * the given <code>src</code>.
	 * <p/>
	 * This does not perform a copy of the image data from <code>src</code> into
	 * the result image; see {@link #copyToOptimalImage(BufferedImage)} for
	 * that.
	 * <p/>
	 * We force all rendering results into one of these two types, avoiding the
	 * case where a source image is of an unsupported (or poorly supported)
	 * format by Java2D causing the rendering result to end up looking terrible
	 * (common with GIFs) or be totally corrupt (e.g. solid black image).
	 * <p/>
	 * Originally reported by Magnus Kvalheim from Movellas when scaling certain
	 * GIF and PNG images.
	 * 
	 * @param src
	 *            The source image that will be analyzed to determine the most
	 *            optimal image type it can be rendered into.
	 * @param width
	 *            The width of the newly created resulting image.
	 * @param height
	 *            The height of the newly created resulting image.
	 * 
	 * @return a new {@link BufferedImage} representing the most optimal target
	 *         image type that <code>src</code> can be rendered into.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>src</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>width</code> or <code>height</code> are &lt; 0.
	 * 
	 * @see <a
	 *      href="http://www.mail-archive.com/java2d-interest@capra.eng.sun.com/msg05621.html">How
	 *      Java2D handles poorly supported image types</a>
	 * @see <a
	 *      href="http://code.google.com/p/java-image-scaling/source/browse/trunk/src/main/java/com/mortennobel/imagescaling/MultiStepRescaleOp.java">Thanks
	 *      to Morten Nobel for implementation hint</a>
	 */
	public static BufferedImage createOptimalImage(BufferedImage src,
			int width, int height) throws IllegalArgumentException {
		if (src == null)
			throw new IllegalArgumentException("src cannot be null");
		if (width < 0 || height < 0)
			throw new IllegalArgumentException("width [" + width
					+ "] and height [" + height + "] must be >= 0");

		return new BufferedImage(
				width,
				height,
				(src.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB
						: BufferedImage.TYPE_INT_ARGB));
	}

	/**
	 * Used to copy a {@link BufferedImage} from a non-optimal type into a new
	 * {@link BufferedImage} instance of an optimal type (RGB or ARGB). If
	 * <code>src</code> is already of an optimal type, then it is returned
	 * unmodified.
	 * <p/>
	 * This method is meant to be used by any calling code (imgscalr's or
	 * otherwise) to convert any inbound image from a poorly supported image
	 * type into the 2 most well-supported image types in Java2D (
	 * {@link BufferedImage#TYPE_INT_RGB} or {@link BufferedImage#TYPE_INT_ARGB}
	 * ) in order to ensure all subsequent graphics operations are performed as
	 * efficiently and correctly as possible.
	 * <p/>
	 * When using Java2D to work with image types that are not well supported,
	 * the results can be anything from exceptions bubbling up from the depths
	 * of Java2D to images being completely corrupted and just returned as solid
	 * black.
	 * 
	 * @param src
	 *            The image to copy (if necessary) into an optimally typed
	 *            {@link BufferedImage}.
	 * 
	 * @return a representation of the <code>src</code> image in an optimally
	 *         typed {@link BufferedImage}, otherwise <code>src</code> if it was
	 *         already of an optimal type.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>src</code> is <code>null</code>.
	 */
	public static BufferedImage copyToOptimalImage(BufferedImage src)
			throws IllegalArgumentException {
		if (src == null)
			throw new IllegalArgumentException("src cannot be null");

		// Calculate the type depending on the presence of alpha.
		int type = (src.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB
				: BufferedImage.TYPE_INT_ARGB);
		BufferedImage result = new BufferedImage(src.getWidth(),
				src.getHeight(), type);

		// Render the src image into our new optimal source.
		Graphics g = result.getGraphics();
		g.drawImage(src, 0, 0, null);
		g.dispose();

		return result;
	}
}