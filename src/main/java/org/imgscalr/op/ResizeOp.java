package org.imgscalr.op;

import static org.imgscalr.Scalr.DEBUG;
import static org.imgscalr.util.ImageUtil.createOptimalImage;
import static org.imgscalr.util.LogUtil.log;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

public class ResizeOp implements IOp {
	protected Mode resizeMode;
	protected Method scalingMethod;

	protected int targetWidth;
	protected int targetHeight;

	public ResizeOp(int targetSize) throws IllegalArgumentException {
		this(Method.AUTOMATIC, Mode.AUTOMATIC, targetSize, targetSize);
	}

	public ResizeOp(Method scalingMethod, int targetSize)
			throws IllegalArgumentException {
		this(scalingMethod, Mode.AUTOMATIC, targetSize, targetSize);
	}

	public ResizeOp(Mode resizeMode, int targetSize)
			throws IllegalArgumentException {
		this(Method.AUTOMATIC, resizeMode, targetSize, targetSize);
	}

	public ResizeOp(Method scalingMethod, Mode resizeMode, int targetSize)
			throws IllegalArgumentException {
		this(scalingMethod, resizeMode, targetSize, targetSize);
	}

	public ResizeOp(int targetWidth, int targetHeight)
			throws IllegalArgumentException {
		this(Method.AUTOMATIC, Mode.AUTOMATIC, targetWidth, targetHeight);
	}

	public ResizeOp(Method scalingMethod, int targetWidth, int targetHeight)
			throws IllegalArgumentException {
		this(scalingMethod, Mode.AUTOMATIC, targetWidth, targetHeight);
	}

	public ResizeOp(Mode resizeMode, int targetWidth, int targetHeight)
			throws IllegalArgumentException {
		this(Method.AUTOMATIC, resizeMode, targetWidth, targetHeight);
	}

	public ResizeOp(Method scalingMethod, Mode resizeMode, int targetWidth,
			int targetHeight) throws IllegalArgumentException {
		if (targetWidth < 0)
			throw new IllegalArgumentException("targetWidth must be >= 0");
		if (targetHeight < 0)
			throw new IllegalArgumentException("targetHeight must be >= 0");
		if (scalingMethod == null)
			throw new IllegalArgumentException(
					"scalingMethod cannot be null. A good default value is Method.AUTOMATIC.");
		if (resizeMode == null)
			throw new IllegalArgumentException(
					"resizeMode cannot be null. A good default value is Mode.AUTOMATIC.");

		this.targetWidth = targetWidth;
		this.targetHeight = targetHeight;
		this.scalingMethod = scalingMethod;
		this.resizeMode = resizeMode;
	}

	@Override
	public String toString() {
		return getClass().getName() + "@" + hashCode() + " [scalingMethod="
				+ scalingMethod + ", resizeMode=" + resizeMode
				+ ", targetWidth=" + targetWidth + ", targetHeight="
				+ targetHeight + "]";
	}

	@Override
	public BufferedImage apply(BufferedImage src)
			throws IllegalArgumentException, ImagingOpException {
		long t = System.currentTimeMillis();

		if (src == null)
			throw new IllegalArgumentException("src cannot be null");

		BufferedImage result = null;

		int currentWidth = src.getWidth();
		int currentHeight = src.getHeight();

		// <= 1 is a square or landscape-oriented image, > 1 is a portrait.
		float ratio = ((float) currentHeight / (float) currentWidth);

		if (DEBUG)
			log(0,
					this,
					"Resizing Image [size=%dx%d, resizeMode=%s, orientation=%s, ratio(H/W)=%f] to [targetSize=%dx%d]",
					currentWidth, currentHeight, resizeMode,
					(ratio <= 1 ? "Landscape/Square" : "Portrait"), ratio,
					targetWidth, targetHeight);

		/*
		 * First determine if ANY size calculation needs to be done, in the case
		 * of FIT_EXACT, ignore image proportions and orientation and just use
		 * what the user sent in, otherwise the proportion of the picture must
		 * be honored.
		 * 
		 * The way that is done is to figure out if the image is in a
		 * LANDSCAPE/SQUARE or PORTRAIT orientation and depending on its
		 * orientation, use the primary dimension (width for LANDSCAPE/SQUARE
		 * and height for PORTRAIT) to recalculate the alternative (height and
		 * width respectively) value that adheres to the existing ratio.
		 * 
		 * This helps make life easier for the caller as they don't need to
		 * pre-compute proportional dimensions before calling the API, they can
		 * just specify the dimensions they would like the image to roughly fit
		 * within and it will do the right thing without mangling the result.
		 */
		if (resizeMode != Mode.FIT_EXACT) {
			if ((ratio <= 1 && resizeMode == Mode.AUTOMATIC)
					|| (resizeMode == Mode.FIT_TO_WIDTH)) {
				// First make sure we need to do any work in the first place
				if (targetWidth == src.getWidth())
					return src;

				// Save for detailed logging (this is cheap).
				int originalTargetHeight = targetHeight;

				/*
				 * Landscape or Square Orientation: Ignore the given height and
				 * re-calculate a proportionally correct value based on the
				 * targetWidth.
				 */
				targetHeight = Math.round((float) targetWidth * ratio);

				if (DEBUG && originalTargetHeight != targetHeight)
					log(1,
							this,
							"Auto-Corrected targetHeight [from=%d to=%d] to honor image proportions.",
							originalTargetHeight, targetHeight);
			} else {
				// First make sure we need to do any work in the first place
				if (targetHeight == src.getHeight())
					return src;

				// Save for detailed logging (this is cheap).
				int originalTargetWidth = targetWidth;

				/*
				 * Portrait Orientation: Ignore the given width and re-calculate
				 * a proportionally correct value based on the targetHeight.
				 */
				targetWidth = Math.round((float) targetHeight / ratio);

				if (DEBUG && originalTargetWidth != targetWidth)
					log(1,
							this,
							"Auto-Corrected targetWidth [from=%d to=%d] to honor image proportions.",
							originalTargetWidth, targetWidth);
			}
		} else
			log(1,
					this,
					"Resize Mode FIT_EXACT used, no width/height checking or re-calculation will be done.");

		// If AUTOMATIC was specified, determine the real scaling method.
		if (scalingMethod == Scalr.Method.AUTOMATIC)
			scalingMethod = determineScalingMethod(targetWidth, targetHeight,
					ratio);

		if (DEBUG)
			log(1, this, "Using Scaling Method: %s", scalingMethod);

		// Now we scale the image
		if (scalingMethod == Scalr.Method.SPEED) {
			result = scaleImage(src, targetWidth, targetHeight,
					RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		} else if (scalingMethod == Scalr.Method.BALANCED) {
			result = scaleImage(src, targetWidth, targetHeight,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		} else if (scalingMethod == Scalr.Method.QUALITY) {
			/*
			 * If we are scaling up (in either width or height - since we know
			 * the image will stay proportional we just check if either are
			 * being scaled up), directly using a single BICUBIC will give us
			 * better results then using Chris Campbell's incremental scaling
			 * operation (and take a lot less time).
			 * 
			 * If we are scaling down, we must use the incremental scaling
			 * algorithm for the best result.
			 */
			if (targetWidth > currentWidth || targetHeight > currentHeight) {
				if (DEBUG)
					log(1, this,
							"QUALITY scale-up, a single BICUBIC scale operation will be used...");

				/*
				 * BILINEAR and BICUBIC look similar the smaller the scale jump
				 * upwards is, if the scale is larger BICUBIC looks sharper and
				 * less fuzzy. But most importantly we have to use BICUBIC to
				 * match the contract of the QUALITY rendering scalingMethod.
				 * This note is just here for anyone reading the code and
				 * wondering how they can speed their own calls up.
				 */
				result = scaleImage(src, targetWidth, targetHeight,
						RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			} else {
				if (DEBUG)
					log(1, this,
							"QUALITY scale-down, incremental scaling will be used...");

				/*
				 * Originally we wanted to use BILINEAR interpolation here
				 * because it takes 1/3rd the time that the BICUBIC
				 * interpolation does, however, when scaling large images down
				 * to most sizes bigger than a thumbnail we witnessed noticeable
				 * "softening" in the resultant image with BILINEAR that would
				 * be unexpectedly annoying to a user expecting a "QUALITY"
				 * scale of their original image. Instead BICUBIC was chosen to
				 * honor the contract of a QUALITY scale of the original image.
				 */
				result = scaleImageIncrementally(src, targetWidth,
						targetHeight,
						RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			}
		}

		if (DEBUG)
			log(0, this, "Resized Image in %d ms", System.currentTimeMillis()
					- t);

		return result;
	}

	/**
	 * Used to determine the scaling {@link Method} that is best suited for
	 * scaling the image to the targeted dimensions.
	 * <p/>
	 * This method is intended to be used to select a specific scaling
	 * {@link Method} when a {@link Method#AUTOMATIC} method is specified. This
	 * method utilizes the {@link Scalr#THRESHOLD_QUALITY_BALANCED} and
	 * {@link Scalr#THRESHOLD_BALANCED_SPEED} thresholds when selecting which
	 * method should be used by comparing the primary dimension (width or
	 * height) against the threshold and seeing where the image falls. The
	 * primary dimension is determined by looking at the orientation of the
	 * image: landscape or square images use their width and portrait-oriented
	 * images use their height.
	 * 
	 * @param targetWidth
	 *            The target width for the scaled image.
	 * @param targetHeight
	 *            The target height for the scaled image.
	 * @param ratio
	 *            A height/width ratio used to determine the orientation of the
	 *            image so the primary dimension (width or height) can be
	 *            selected to test if it is greater than or less than a
	 *            particular threshold.
	 * 
	 * @return the fastest {@link Method} suited for scaling the image to the
	 *         specified dimensions while maintaining a good-looking result.
	 */
	protected Method determineScalingMethod(int targetWidth, int targetHeight,
			float ratio) {
		// Get the primary dimension based on the orientation of the image
		int length = (ratio <= 1 ? targetWidth : targetHeight);

		// Default to speed
		Method result = Method.SPEED;

		// Figure out which scalingMethod should be used
		if (length <= Scalr.THRESHOLD_QUALITY_BALANCED)
			result = Method.QUALITY;
		else if (length <= Scalr.THRESHOLD_BALANCED_SPEED)
			result = Method.BALANCED;

		if (DEBUG)
			log(2, this, "AUTOMATIC scaling method selected: %s", result.name());

		return result;
	}

	/**
	 * Used to implement a straight-forward image-scaling operation using Java
	 * 2D.
	 * <p/>
	 * This method uses the Oracle-encouraged method of
	 * <code>Graphics2D.drawImage(...)</code> to scale the given image with the
	 * given interpolation hint.
	 * 
	 * @param src
	 *            The image that will be scaled.
	 * @param targetWidth
	 *            The target width for the scaled image.
	 * @param targetHeight
	 *            The target height for the scaled image.
	 * @param interpolationHintValue
	 *            The {@link RenderingHints} interpolation value used to
	 *            indicate the method that {@link Graphics2D} should use when
	 *            scaling the image.
	 * 
	 * @return the result of scaling the original <code>src</code> to the given
	 *         dimensions using the given interpolation method.
	 */
	protected BufferedImage scaleImage(BufferedImage src, int targetWidth,
			int targetHeight, Object interpolationHintValue) {
		// Setup the rendering resources to match the source image's
		BufferedImage result = createOptimalImage(src, targetWidth,
				targetHeight);
		Graphics2D resultGraphics = result.createGraphics();

		// Scale the image to the new buffer using the specified rendering hint.
		resultGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				interpolationHintValue);
		resultGraphics.drawImage(src, 0, 0, targetWidth, targetHeight, null);

		// Just to be clean, explicitly dispose our temporary graphics object
		resultGraphics.dispose();

		// Return the scaled image to the caller.
		return result;
	}

	/**
	 * Used to implement Chris Campbell's incremental-scaling algorithm: <a
	 * href="http://today.java.net/pub/a/today/2007/04/03/perils
	 * -of-image-getscaledinstance
	 * .html">http://today.java.net/pub/a/today/2007/04/03/perils
	 * -of-image-getscaledinstance.html</a>.
	 * <p/>
	 * Modifications to the original algorithm are variable names and comments
	 * added for clarity and the hard-coding of using BICUBIC interpolation as
	 * well as the explicit "flush()" operation on the interim BufferedImage
	 * instances to avoid resource leaking.
	 * 
	 * @param src
	 *            The image that will be scaled.
	 * @param targetWidth
	 *            The target width for the scaled image.
	 * @param targetHeight
	 *            The target height for the scaled image.
	 * @param interpolationHintValue
	 *            The {@link RenderingHints} interpolation value used to
	 *            indicate the method that {@link Graphics2D} should use when
	 *            scaling the image.
	 * 
	 * @return an image scaled to the given dimensions using the given rendering
	 *         hint.
	 */
	protected BufferedImage scaleImageIncrementally(BufferedImage src,
			int targetWidth, int targetHeight, Object interpolationHintValue) {
		boolean hasReassignedSrc = false;
		int incrementCount = 0;
		int currentWidth = src.getWidth();
		int currentHeight = src.getHeight();

		do {
			/*
			 * If the current width is bigger than our target, cut it in half
			 * and sample again.
			 */
			if (currentWidth > targetWidth) {
				currentWidth /= 2;

				/*
				 * If we cut the width too far it means we are on our last
				 * iteration. Just set it to the target width and finish up.
				 */
				if (currentWidth < targetWidth)
					currentWidth = targetWidth;
			}

			/*
			 * If the current height is bigger than our target, cut it in half
			 * and sample again.
			 */

			if (currentHeight > targetHeight) {
				currentHeight /= 2;

				/*
				 * If we cut the height too far it means we are on our last
				 * iteration. Just set it to the target height and finish up.
				 */

				if (currentHeight < targetHeight)
					currentHeight = targetHeight;
			}

			// Render the incremental scaled image.
			BufferedImage incrementalImage = scaleImage(src, currentWidth,
					currentHeight, interpolationHintValue);

			/*
			 * Before re-assigning our interim (partially scaled)
			 * incrementalImage to be the new src image before we iterate around
			 * again to process it down further, we want to flush() the previous
			 * src image IF (and only IF) it was one of our own temporary
			 * BufferedImages created during this incremental down-sampling
			 * cycle. If it wasn't one of ours, then it was the original
			 * caller-supplied BufferedImage in which case we don't want to
			 * flush() it and just leave it alone.
			 */
			if (hasReassignedSrc)
				src.flush();

			/*
			 * Now treat our incremental partially scaled image as the src image
			 * and cycle through our loop again to do another incremental
			 * scaling of it (if necessary).
			 */
			src = incrementalImage;

			/*
			 * Keep track of us re-assigning the original caller-supplied source
			 * image with one of our interim BufferedImages so we know when to
			 * explicitly flush the interim "src" on the next cycle through.
			 */
			hasReassignedSrc = true;

			// Track how many times we go through this cycle to scale the image.
			incrementCount++;
		} while (currentWidth != targetWidth || currentHeight != targetHeight);

		if (DEBUG)
			log(2, this, "Incrementally Scaled Image in %d steps.",
					incrementCount);

		/*
		 * Once the loop has exited, the src image argument is now our scaled
		 * result image that we want to return.
		 */
		return src;
	}
}