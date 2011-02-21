/**   
 * Copyright 2011 The Buzz Media, LLC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thebuzzmedia.imgscalr;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

/**
 * Class used to implement performant, good-quality and intelligent image
 * scaling algorithms in native Java. This class utilizes the Java2D
 * "best practices" for image-scaling, ensuring that images are hardware
 * accelerated at all times if provided by the platform and host-VM.
 * <p/>
 * All scaling operations implemented by this class maintain the proportion of
 * the original image. If image-cropping is desired the caller will need to
 * perform those edits before calling one of the <code>resize</code> methods
 * provided by this class.
 * <p/>
 * In order to maintain the proportionality of the original images, this class
 * implements the following default behavior:
 * <ol>
 * <li>If the image is LANDSCAPE-oriented or SQUARE, treat the
 * <code>targetWidth</code> as the primary dimension and re-calculate the
 * <code>targetHeight</code> regardless of what is passed in.</li>
 * <li>If image is PORTRAIT-oriented, treat the <code>targetHeight</code> as the
 * primary dimension and re-calculate the <code>targetWidth</code> regardless of
 * what is passed in.</li>
 * </ol>
 * Recalculation of the secondary dimensions is extremely cheap to the point of
 * it being negligible and this design provides users with better
 * expected-behavior from the library; which is why this approach was chosen.
 * <p/>
 * This class implements a few different methods for scaling an image, providing
 * either the best-looking result or the fastest result depending on the scaling
 * hint provided (see {@link Method}).
 * <p/>
 * This class also implements the incremental scaling algorithm presented by
 * Chris Campbell in his <a href="http://today.java
 * .net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html">Perils of
 * Image.getScaledInstance()</a> article in order to give the best-looking
 * results to images scaled down below roughly 800px in size where using a
 * single scaling operation (even with
 * {@link RenderingHints#VALUE_INTERPOLATION_BICUBIC} interpolation) would
 * produce a much worse-looking result.
 * <p/>
 * Minor modifications are made to Campbell's original implementation in the
 * form of:
 * <ol>
 * <li>Instead of accepting a user-supplied interpolation method,
 * {@link RenderingHints#VALUE_INTERPOLATION_BICUBIC} interpolation is always
 * used. This was done after A/B comparison testing with large images
 * down-scaled to thumbnail sizes showed noticeable "blurring" when BILINEAR
 * interpolation was used. Given that Campbell's algorithm is only used in
 * QUALITY mode when down-scaling, it was determined that the user's expectation
 * of a much less blurry picture would require that BICUBIC be the default
 * interpolation in order to meet the QUALITY expectation.</li>
 * <li>After each iteration of the do-while loop that incrementally scales the
 * source image down, an effort is made to explicitly call
 * {@link BufferedImage#flush()} on the interim temporary {@link BufferedImage}
 * instances created by the algorithm in an attempt to ensure a more complete GC
 * cycle by the VM when cleaning up the temporary instances.</li>
 * <li>Extensive comments have been added to increase readability of the code.</li>
 * <li>Variable names have been expanded to increase readability of the code.</li>
 * </ol>
 * <p/>
 * <strong>NOTE</strong>: This class does not call {@link BufferedImage#flush()}
 * on any of the <em>source images</em> passed in by calling code; it is up to
 * the original caller to dispose of their source images when they are no longer
 * needed so the VM can most efficiently GC them.
 * <h3>Generated Image Types</h3>
 * Java2D provides support for a number of different image types defined as
 * <code>BufferedImage.TYPE_*</code> variables, unfortunately not all image
 * types are supported equally in Java2D. Some more obscure image types either
 * have poor or no support, leading to severely degraded quality when an attempt
 * is made by imgscalr to create a scaled instance <em>of the same type</em> as
 * the source image.
 * <p/>
 * To avoid imgscalr generating significantly worse-looking results than
 * alternative scaling approaches (e.g.
 * {@link Image#getScaledInstance(int, int, int)}), all resultant images
 * generated by imgscalr are of one of two types:
 * <ol>
 * <li>{@link BufferedImage#TYPE_INT_RGB}</li>
 * <li>{@link BufferedImage#TYPE_INT_ARGB}</li>
 * </ol>
 * depending on if the source image utilizes transparency or not.
 * <p/>
 * This is a recommended approach by the Java2D team for dealing with poorly (or
 * non) supported image types. More can be read about this issue <a href=
 * "http://www.mail-archive.com/java2d-interest@capra.eng.sun.com/msg05621.html"
 * >here</a>.
 * <h3>Logging</h3>
 * This class implements all its debug logging via the
 * {@link #log(String, Object...)} method. At this time logging is done directly
 * to <code>System.out</code> via the <code>printf</code> method. This allows
 * the logging to be light weight and easy to capture.
 * <p/>
 * Implementation of logging in this class is as efficient as possible; avoiding
 * any calls to the logger or passing of arguments if logging is not enabled.
 * 
 * @author Riyad Kalla (software@thebuzzmedia.com)
 */
public class Scalr {
	/**
	 * Flag used to indicate if debugging output has been enabled by setting the
	 * "imgscalr.debug" system property to <code>true</code>. This value will be
	 * <code>false</code> if the "imgscalr.debug" system property is undefined
	 * or set to <code>false</code>.
	 * <p/>
	 * This system property can be set on startup like so:<br/>
	 * <code>
	 * -Dimgscalr.debug=true
	 * </code>
	 */
	public static final boolean DEBUG = Boolean.getBoolean("imgscalr.debug");

	/**
	 * Prefix to every log message this library logs. Using a well-defined
	 * prefix helps make it easier both visually and programatically to scan log
	 * files for messages produced by this library.
	 */
	public static final String LOG_MESSAGE_PREFIX = "[imgscalr] ";

	static {
		if (DEBUG)
			log("Debug output ENABLED");
	}

	/**
	 * Used to define the different scaling hints that the algorithm can prefer.
	 */
	public static enum Method {
		/**
		 * Used to indicate that the scaling implementation should decide which
		 * method to use in order to get the best looking scaled image in the
		 * least amount of time.
		 * <p/>
		 * The scaling algorithm will use the
		 * {@link Scalr#QUALITY_BALANCED_THRESHOLD_PX} or
		 * {@link Scalr#BALANCED_SPEED_THRESHOLD_PX} thresholds as cut-offs to
		 * decide between selecting the <code>QUALITY</code>,
		 * <code>BALANCED</code> or <code>SPEED</code> scaling algorithms.
		 * <p/>
		 * By default the thresholds chosen will give nearly the best looking
		 * result in the fastest amount of time. We intent this method to work
		 * for 80% of people looking to scale an image quickly and get a good
		 * looking result.
		 */
		AUTOMATIC,
		/**
		 * Used to indicate that the scaling implementation should scale as fast
		 * as possible and return a result. For smaller images (800px in size)
		 * this can result in noticeable aliasing but it can be a few magnitudes
		 * times faster than using the QUALITY method.
		 */
		SPEED,
		/**
		 * Used to indicate that the scaling implementation should use a scaling
		 * operation balanced between SPEED and QUALITY. Sometimes SPEED looks
		 * too low quality to be useful (e.g. text can become unreadable when
		 * scaled using SPEED) but using QUALITY mode will increase the
		 * processing time too much. This mode provides a "better than SPEED"
		 * quality in a "less than QUALITY" amount of time.
		 */
		BALANCED,
		/**
		 * Used to indicate that the scaling implementation should do everything
		 * it can to create as nice of a result as possible. This approach is
		 * most important for smaller pictures (800px or smaller) and less
		 * important for larger pictures as the difference between this method
		 * and the SPEED method become less and less noticeable as the
		 * source-image size increases. Using the AUTOMATIC method will
		 * automatically prefer the QUALITY method when scaling an image down
		 * below 800px in size.
		 */
		QUALITY;
	}

	/**
	 * Threshold (in pixels) at which point the scaling operation using the
	 * {@link Method#AUTOMATIC} method will decide if a {@link Method#QUALITY}
	 * method will be used (if smaller than or equal to threshold) or a
	 * {@link Method#BALANCED} method will be used (if larger than threshold).
	 * <p/>
	 * The bigger the image is being scaled to, the less noticeable degradations
	 * in the image becomes and the faster algorithms can be selected.
	 * <p/>
	 * The value of this threshold (700) was chosen after visual, by-hand, A/B
	 * testing between different types of images scaled with this library; both
	 * photographs and screenshots. It was determined that images below this
	 * size need to use a {@link Method#QUALITY} scale method to look decent in
	 * most all cases while using the faster {@link Method#BALANCED} method for
	 * images bigger than this threshold showed no noticeable degradation over a
	 * <code>QUALITY</code> scale.
	 */
	public static final int QUALITY_BALANCED_THRESHOLD_PX = 700;

	/**
	 * Threshold (in pixels) at which point the scaling operation using the
	 * {@link Method#AUTOMATIC} method will decide if a {@link Method#BALANCED}
	 * method will be used (if smaller than or equal to threshold) or a
	 * {@link Method#SPEED} method will be used (if larger than threshold).
	 * <p/>
	 * The bigger the image is being scaled to, the less noticeable degradations
	 * in the image becomes and the faster algorithms can be selected.
	 * <p/>
	 * The value of this threshold (1600) was chosen after visual, by-hand, A/B
	 * testing between different types of images scaled with this library; both
	 * photographs and screenshots. It was determined that images below this
	 * size need to use a {@link Method#BALANCED} scale method to look decent in
	 * most all cases while using the faster {@link Method#SPEED} method for
	 * images bigger than this threshold showed no noticeable degradation over a
	 * <code>BALANCED</code> scale.
	 */
	public static final int BALANCED_SPEED_THRESHOLD_PX = 1600;

	/**
	 * Resize a given image (maintaining its original proportion) to a width and
	 * height of the given <code>targetSize</code> using the scaling method of
	 * {@link Method#AUTOMATIC}.
	 * 
	 * @param src
	 *            The image that will be scaled.
	 * @param targetSize
	 *            The target width and height (square) that you wish the image
	 *            to fit within.
	 * 
	 * @return the proportionally scaled image with either a width or height of
	 *         the given target size.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>targetSize</code> is &lt; 0.
	 */
	public static BufferedImage resize(BufferedImage src, int targetSize)
			throws IllegalArgumentException {
		return resize(src, Method.AUTOMATIC, targetSize, targetSize);
	}

	/**
	 * Resize a given image (maintaining its proportion) to the target width and
	 * height using the scaling method of {@link Method#AUTOMATIC}.
	 * <p/>
	 * <strong>TIP</strong>: See the class description to understand how this
	 * class handles recalculation of the <code>targetWidth</code> or
	 * <code>targetHeight</code> depending on the image's orientation in order
	 * to maintain the original proportion.
	 * 
	 * @param src
	 *            The image that will be scaled.
	 * @param targetWidth
	 *            The target width that you wish the image to have.
	 * @param targetHeight
	 *            The target height that you wish the image to have.
	 * 
	 * @return the proportionally scaled image with either a width or height of
	 *         the given target size.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>targetSize</code> is &lt; 0.
	 */
	public static BufferedImage resize(BufferedImage src, int targetWidth,
			int targetHeight) throws IllegalArgumentException {
		return resize(src, Method.AUTOMATIC, targetWidth, targetHeight);
	}

	/**
	 * Resize a given image (maintaining its original proportion) to a width and
	 * height of the given <code>targetSize</code> using the given scaling
	 * method.
	 * 
	 * @param src
	 *            The image that will be scaled.
	 * @param scalingMethod
	 *            The method used for scaling the image; preferring speed to
	 *            quality or a balance of both.
	 * @param targetSize
	 *            The target width and height (square) that you wish the image
	 *            to fit within.
	 * 
	 * @return the proportionally scaled image with either a width or height of
	 *         the given target size.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>scalingMethod</code> is <code>null</code> or if
	 *             <code>targetSize</code> is &lt; 0.
	 */
	public static BufferedImage resize(BufferedImage src, Method scalingMethod,
			int targetSize) throws IllegalArgumentException {
		return resize(src, scalingMethod, targetSize, targetSize);
	}

	/**
	 * Resize a given image (maintaining its proportion) to the target width and
	 * height using the given scaling method.
	 * <p/>
	 * <strong>TIP</strong>: See the class description to understand how this
	 * class handles recalculation of the <code>targetWidth</code> or
	 * <code>targetHeight</code> depending on the image's orientation in order
	 * to maintain the original proportion.
	 * 
	 * @param src
	 *            The image that will be scaled.
	 * @param scalingMethod
	 *            The method used for scaling the image; preferring speed to
	 *            quality or a balance of both.
	 * @param targetWidth
	 *            The target width that you wish the image to have.
	 * @param targetHeight
	 *            The target height that you wish the image to have.
	 * 
	 * @return the proportionally scaled image no bigger than the given width
	 *         and height.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>scalingMethod</code> is <code>null</code>, if
	 *             <code>targetWidth</code> is &lt; 0 or if
	 *             <code>targetHeight</code> is &lt; 0.
	 */
	public static BufferedImage resize(BufferedImage src, Method scalingMethod,
			int targetWidth, int targetHeight) throws IllegalArgumentException {
		if (scalingMethod == null)
			throw new IllegalArgumentException("scalingMethod cannot be null");
		if (targetWidth < 0)
			throw new IllegalArgumentException("targetWidth must be >= 0");
		if (targetHeight < 0)
			throw new IllegalArgumentException("targetHeight must be >= 0");

		long startTime = System.currentTimeMillis();
		BufferedImage result = null;

		// Make sure we have something to do first.
		if (src != null) {
			int currentWidth = src.getWidth();
			int currentHeight = src.getHeight();
			float ratio = ((float) currentHeight / (float) currentWidth);

			if (DEBUG)
				log("START Resizing Source Image [size=%dx%d, orientation=%s, ratio(H/W)=%f] to [targetSize=%dx%d]",
						currentWidth, currentHeight,
						(ratio <= 1 ? "landscape/square" : "portrait"), ratio,
						targetWidth, targetHeight);

			/*
			 * The proportion of the picture must be honored, the way that is
			 * done is to figure out if the image is in a LANDSCAPE/SQUARE or
			 * PORTRAIT orientation and depending on its orientation, use the
			 * primary dimension (width for LANDSCAPE/SQUARE and height for
			 * PORTRAIT) to recalculate the alternative (height and width
			 * respectively) value that adheres to the existing ratio. This
			 * helps make life easier for the caller as they don't need to
			 * pre-compute proportional dimensions before calling the API, they
			 * can just specify the dimensions they would like the image to
			 * roughly fit within and it will do the right thing without
			 * mangling the result.
			 */
			if (ratio <= 1) {
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
					log("Auto-Corrected targetHeight [from=%d to=%d] to honor image proportions",
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
					log("Auto-Corrected targetWidth [from=%d to=%d] to honor image proportions",
							originalTargetWidth, targetWidth);
			}

			// If AUTOMATIC was specified, determine the real scaling method.
			if (scalingMethod == Scalr.Method.AUTOMATIC)
				scalingMethod = determineScalingMethod(targetWidth,
						targetHeight, ratio);

			if (DEBUG)
				log("Scaling Image to [size=%dx%d] using the %s method...",
						targetWidth, targetHeight, scalingMethod);

			// Now we scale the image
			if (scalingMethod == Scalr.Method.SPEED) {
				result = scaleImage(src, targetWidth, targetHeight,
						RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			} else if (scalingMethod == Scalr.Method.BALANCED) {
				result = scaleImage(src, targetWidth, targetHeight,
						RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			} else if (scalingMethod == Scalr.Method.QUALITY) {
				/*
				 * If we are scaling up (in either width or height - since we
				 * know the image will stay proportional we just check if either
				 * are being scaled up), directly using a single BICUBIC will
				 * give us better results then using Chris Campbell's
				 * incremental scaling operation (and take a lot less time). If
				 * we are scaling down, we must use the incremental scaling
				 * algorithm for the best result.
				 */
				if (targetWidth > currentWidth || targetHeight > currentHeight) {
					log("\tQUALITY Up-scale, single BICUBIC scaling will be used...");

					/*
					 * BILINEAR and BICUBIC look similar the smaller the scale
					 * jump upwards is, if the scale is larger BICUBIC looks
					 * sharper and less fuzzy. But most importantly we have to
					 * use BICUBIC to match the contract of the QUALITY
					 * rendering method. This note is just here for anyone
					 * reading the code and wondering how they can speed their
					 * own calls up.
					 */
					result = scaleImage(src, targetWidth, targetHeight,
							RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				} else {
					log("\tQUALITY Down-scale, incremental scaling will be used...");

					/*
					 * Originally we wanted to use BILINEAR interpolation here
					 * because it takes 1/3rd the time that the BICUBIC
					 * interpolation does, however, when scaling large images
					 * down to most sizes bigger than a thumbnail we witnessed
					 * noticeable "softening" in the resultant image with
					 * BILINEAR that would be unexpectedly annoying to a user
					 * expecting a "QUALITY" scale of their original image.
					 * Instead BICUBIC was chosen to honor the contract of a
					 * QUALITY scale of the original image.
					 */
					result = scaleImageIncrementally(src, targetWidth,
							targetHeight,
							RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				}
			}

			if (DEBUG) {
				long elapsedTime = System.currentTimeMillis() - startTime;
				log("END Source Image Scaled from [%dx%d] to [%dx%d] in %d ms (approx %f seconds)",
						currentWidth, currentHeight, targetWidth, targetHeight,
						elapsedTime, (double) elapsedTime / (double) 1000);
			}
		} else
			log("No Source Image Specified, exiting...");

		return result;
	}

	/**
	 * Helper method used to ensure a message is loggable before it is logged
	 * and then pre-pend a universal prefix to all log messages generated by
	 * this library to make the log entries easy to parse visually or
	 * programatically.
	 * <p/>
	 * If a message cannot be logged (logging is disabled) then this method
	 * returns immediately.
	 * <p/>
	 * <strong>NOTE</strong>: Because Java will auto-box primitive arguments
	 * into Objects when building out the <code>params</code> array, care should
	 * be taken not to call this method with primitive values unless
	 * {@link #DEBUG} is <code>true</code>; otherwise the VM will be spending
	 * time performing unnecessary auto-boxing calculations.
	 * 
	 * @param message
	 *            The log message in <a href=
	 *            "http://download.oracle.com/javase/6/docs/api/java/util/Formatter.html#syntax"
	 *            >format string syntax</a> that will be logged.
	 * @param params
	 *            The parameters that will be swapped into all the place holders
	 *            in the original messages before being logged.
	 * 
	 * @see #LOG_MESSAGE_PREFIX
	 */
	protected static void log(String message, Object... params) {
		if (DEBUG)
			System.out.printf(LOG_MESSAGE_PREFIX + message + '\n', params);
	}

	/**
	 * Used to determine the scaling {@link Method} that is best suited for
	 * scaling the image to the targeted dimensions.
	 * <p/>
	 * This method is intended to be used to select a specific scaling
	 * {@link Method} when a {@link Method#AUTOMATIC} method is specified. This
	 * method utilizes the {@link #QUALITY_BALANCED_THRESHOLD_PX} and
	 * {@link #BALANCED_SPEED_THRESHOLD_PX} thresholds when selecting which
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
	protected static Method determineScalingMethod(int targetWidth,
			int targetHeight, float ratio) {
		// Get the primary dimension based on the orientation of the image
		int length = (ratio <= 1 ? targetWidth : targetHeight);

		// Default to speed
		Method result = Method.SPEED;

		// Figure out which method should be used
		if (length <= QUALITY_BALANCED_THRESHOLD_PX)
			result = Method.QUALITY;
		else if (length <= BALANCED_SPEED_THRESHOLD_PX)
			result = Method.BALANCED;

		if (DEBUG)
			log("AUTOMATIC Scaling Method Selected [%s] for Image [size=%dx%d]",
					result.name(), targetWidth, targetHeight);

		return result;
	}

	/**
	 * Used to implement a straight-forward image-scaling operation using Java
	 * 2D.
	 * <p/>
	 * This method uses the Snoracle-encouraged method of
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
	protected static BufferedImage scaleImage(BufferedImage src,
			int targetWidth, int targetHeight, Object interpolationHintValue) {
		/*
		 * Determine the TYPE of image (plain RGB or RGB + Alpha) that we want
		 * to render the scaled instance into. We force all rendering results
		 * into one of these two types, avoiding the case where a source image
		 * is of an unsupported (or poorly supported) format by Java2D and the
		 * written results, when attempting to re-create and write out that
		 * format, is garbage.
		 * 
		 * Originally reported by Magnus Kvalheim from Movellas when scaling
		 * certain GIF and PNG images.
		 * 
		 * More information about Java2D and poorly supported image types:
		 * http:/
		 * /www.mail-archive.com/java2d-interest@capra.eng.sun.com/msg05621.html
		 * 
		 * Thanks to Morten Nobel for the implementation hint:
		 * http://code.google
		 * .com/p/java-image-scaling/source/browse/trunk/src/main
		 * /java/com/mortennobel/imagescaling/MultiStepRescaleOp.java
		 */
		int imageType = (src.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB
				: BufferedImage.TYPE_INT_ARGB);

		// Setup the rendering resources to match the source image's
		BufferedImage result = new BufferedImage(targetWidth, targetHeight,
				imageType);
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
	protected static BufferedImage scaleImageIncrementally(BufferedImage src,
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
			if (!hasReassignedSrc)
				hasReassignedSrc = true;

			// Track how many times we go through this cycle to scale the image.
			incrementCount++;
		} while (currentWidth != targetWidth || currentHeight != targetHeight);

		if (DEBUG)
			log("\tScaled Image in %d steps", incrementCount);

		/*
		 * Once the loop has exited, the src image argument is now our scaled
		 * result image that we want to return.
		 */
		return src;
	}
}