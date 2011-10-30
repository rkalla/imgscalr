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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.IndexColorModel;
import java.awt.image.Kernel;
import java.awt.image.RasterFormatException;
import java.awt.image.RescaleOp;

import javax.imageio.ImageIO;

/**
 * Class used to implement performant, good-quality and intelligent image
 * scaling algorithms in native Java 2D. This class utilizes the Java2D
 * "best practices" for image-scaling, ensuring that images are hardware
 * accelerated at all times if provided by the platform and host-VM.
 * <p/>
 * Hardware acceleration also includes execution of optional caller-supplied
 * {@link BufferedImageOp}s that are applied to the resultant images before
 * returning them as well as any optional rotations specified.
 * <h3>Image Proportions</h3>
 * All scaling operations implemented by this class maintain the proportion of
 * the original image. If image-cropping is desired the caller will need to
 * perform those edits before calling one of the <code>resize</code> methods
 * provided by this class.
 * <p/>
 * In order to maintain the proportionality of the original images, this class
 * implements the following behavior:
 * <ol>
 * <li>If the image is LANDSCAPE-oriented or SQUARE, treat the
 * <code>targetWidth</code> as the primary dimension and re-calculate the
 * <code>targetHeight</code> regardless of what is passed in.</li>
 * <li>If image is PORTRAIT-oriented, treat the <code>targetHeight</code> as the
 * primary dimension and re-calculate the <code>targetWidth</code> regardless of
 * what is passed in.</li>
 * <li>If a {@link Mode} value of {@link Mode#FIT_TO_WIDTH} or
 * {@link Mode#FIT_TO_HEIGHT} is passed in to the <code>resize</code> method,
 * the image's orientation is ignored and the scaled image is fit to the
 * dimension the user specified with the {@link Mode}.</li>
 * </ol>
 * Recalculation of the secondary dimensions is extremely cheap and this
 * approach provides users with better expected-behavior from the library.
 * <h3>Image Quality</h3>
 * This class implements a few different methods for scaling an image, providing
 * either the best-looking result, the fastest result or a balanced result
 * between the two depending on the scaling hint provided (see {@link Method}).
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
 * Only when scaling using the {@link Method#AUTOMATIC} method will this class
 * look at the size of the image before selecting an approach to scaling the
 * image. If {@link Method#QUALITY} is specified, the best-looking algorithm
 * possible is always used.
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
 * source image down, an explicit effort is made to call
 * {@link BufferedImage#flush()} on the interim temporary {@link BufferedImage}
 * instances created by the algorithm in an attempt to ensure a more complete GC
 * cycle by the VM when cleaning up the temporary instances (this is in addition
 * to disposing of the temporary {@link Graphics2D} references as well).</li>
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
 * generated by imgscalr are one of two types:
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
 * the logging to be light weight and easy to capture while adding no
 * dependencies to the library.
 * <p/>
 * Implementation of logging in this class is as efficient as possible; avoiding
 * any calls to the logger or passing of arguments if logging is not enabled to
 * avoid the (hidden) cost of constructing the Object[] argument for the varargs
 * method call.
 * <h3>GIF Transparency</h3>
 * Unfortunately in Java 6 and earlier, support for GIF's
 * {@link IndexColorModel} is sub-par, both in accurate color-selection and in
 * maintaining transparency when moving to an image of type
 * {@link BufferedImage#TYPE_INT_ARGB}; because of this issue when a GIF image
 * is processed by imgscalr and the result saved as a GIF file, it is possible
 * to lose the alpha channel of a transparent image or in the case of applying
 * an optional {@link BufferedImageOp}, lose the entire picture all together in
 * the result (long standing JDK bugs are filed for these).
 * <p/>
 * imgscalr currently does nothing to work around this manually because it is a
 * defect in the native platform code itself. Fortunately it looks like the
 * issues are half-fixed in Java 7 and any manual workarounds we could attempt
 * internally are relatively expensive, in the form of hand-creating and setting
 * RGB values pixel-by-pixel with a custom {@link ColorModel} in the scaled
 * image. This would lead to a very measurable negative impact on performance
 * without the caller understanding why.
 * <p>
 * <strong>Workaround</strong>: A workaround to this issue with all version of
 * Java is to simply save a GIF as a PNG; no change to your code needs to be
 * made except when the image is saved out, e.g. using {@link ImageIO}. When a
 * file type of "PNG" is used, both the transparency and high color quality will
 * be maintained as the PNG code path in Java2D is superior to the GIF
 * implementation.
 * <p>
 * If the issue with optional {@link BufferedImageOp}s destroying GIF image
 * content is ever fixed in the platform, saving out resulting images as GIFs
 * should suddenly start working.
 * <p>
 * More can be read about the issue <a
 * href="http://gman.eichberger.de/2007/07/transparent-gifs-in-java.html"
 * >here</a> and <a
 * href="http://ubuntuforums.org/archive/index.php/t-1060128.html">here</a>.
 * 
 * @author Riyad Kalla (software@thebuzzmedia.com)
 * @since 1.1
 */
public class Scalr {
	/**
	 * Flag used to indicate if debugging output has been enabled by setting the
	 * "<code>imgscalr.debug</code>" system property to <code>true</code>. This
	 * value will be <code>false</code> if the "<code>imgscalr.debug</code>"
	 * system property is undefined or set to <code>false</code>.
	 * <p/>
	 * This system property can be set on startup with:<br/>
	 * <code>
	 * -Dimgscalr.debug=true
	 * </code> or by calling {@link System#setProperty(String, String)} before
	 * this class is loaded.
	 * <p/>
	 * Default value is <code>false</code>.
	 */
	public static final boolean DEBUG = Boolean.getBoolean("imgscalr.debug");

	/**
	 * Prefix to every log message this library logs. Using a well-defined
	 * prefix helps make it easier both visually and programmatically to scan
	 * log files for messages produced by this library.
	 * <p/>
	 * The value is "<code>[imgscalr] </code>" (including the space).
	 */
	public static final String LOG_PREFIX = "[imgscalr] ";

	/**
	 * A {@link ConvolveOp} using a very light "blur" kernel that acts like an
	 * anti-aliasing filter (softens the image a bit) when applied to an image.
	 * <p/>
	 * A common request by users of the library was that they wished to "soften"
	 * resulting images when scaling them down drastically. After quite a bit of
	 * A/B testing, the kernel used by this Op was selected as the closest match
	 * for the target which was the softer results from the deprecated
	 * {@link AreaAveragingScaleFilter} (which is used internally by the
	 * deprecated {@link Image#getScaledInstance(int, int, int)} method in the
	 * JDK that imgscalr is meant to replace).
	 * <p/>
	 * This ConvolveOp uses a 3x3 kernel with the values:
	 * <table cellpadding="4" border="1">
	 * <tr>
	 * <td>.0f</td>
	 * <td>.08f</td>
	 * <td>.0f</td>
	 * </tr>
	 * <tr>
	 * <td>.08f</td>
	 * <td>.68f</td>
	 * <td>.08f</td>
	 * </tr>
	 * <tr>
	 * <td>.0f</td>
	 * <td>.08f</td>
	 * <td>.0f</td>
	 * </tr>
	 * </table>
	 * <p/>
	 * For those that have worked with ConvolveOps before, this Op uses the
	 * {@link ConvolveOp#EDGE_NO_OP} instruction to not process the pixels along
	 * the very edge of the image (otherwise EDGE_ZERO_FILL would create a
	 * black-border around the image). If you have not worked with a ConvolveOp
	 * before, it just means this default OP will "do the right thing" and not
	 * give you garbage results.
	 * <p/>
	 * This ConvolveOp uses no {@link RenderingHints} values as internally the
	 * {@link ConvolveOp} class only uses hints when doing a color conversion
	 * between the source and destination {@link BufferedImage} targets.
	 * imgscalr allows the {@link ConvolveOp} to create its own destination
	 * image every time, so no color conversion is ever needed and thus no
	 * hints.
	 * <h3>Performance</h3>
	 * Use of this (and other) {@link ConvolveOp}s are hardware accelerated when
	 * possible. For more information on if your image op is hardware
	 * accelerated or not, check the source code of the underlying JDK class
	 * that actually executes the Op code, <a href=
	 * "http://www.docjar.com/html/api/sun/awt/image/ImagingLib.java.html"
	 * >sun.awt.image.ImagingLib</a>.
	 * <h3>Known Issues</h3>
	 * In all versions of Java (tested up to Java 7 preview Build 131), running
	 * this op against a GIF with transparency and attempting to save the
	 * resulting image as a GIF results in a corrupted/empty file. The file must
	 * be saved out as a PNG to maintain the transparency.
	 */
	public static final ConvolveOp OP_ANTIALIAS = new ConvolveOp(
			new Kernel(3, 3, new float[] { .0f, .08f, .0f, .08f, .68f, .08f,
					.0f, .08f, .0f }), ConvolveOp.EDGE_NO_OP, null);

	/**
	 * A {@link RescaleOp} used to make any input image 10% darker.
	 * <p/>
	 * This operation can be applied multiple times in a row if greater than 10%
	 * changes in brightness are desired.
	 */
	public static final RescaleOp OP_DARKER = new RescaleOp(0.9f, 0, null);

	/**
	 * A {@link RescaleOp} used to make any input image 10% brighter.
	 * <p/>
	 * This operation can be applied multiple times in a row if greater than 10%
	 * changes in brightness are desired.
	 */
	public static final RescaleOp OP_BRIGHTER = new RescaleOp(1.1f, 0, null);

	/**
	 * A {@link ColorConvertOp} used to convert any image to a grayscale color
	 * palette.
	 * <p/>
	 * Applying this op multiple times to the same image has no compounding
	 * effects.
	 */
	public static final ColorConvertOp OP_GRAYSCALE = new ColorConvertOp(
			ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

	/**
	 * Static initializer used to prepare some of the variables used by this
	 * class.
	 */
	static {
		log("Debug output ENABLED");
	}

	/**
	 * Used to define the different scaling hints that the algorithm can use.
	 * 
	 * @author Riyad Kalla (software@thebuzzmedia.com)
	 * @since 1.1
	 */
	public static enum Method {
		/**
		 * Used to indicate that the scaling implementation should decide which
		 * method to use in order to get the best looking scaled image in the
		 * least amount of time.
		 * <p/>
		 * The scaling algorithm will use the
		 * {@link Scalr#THRESHOLD_QUALITY_BALANCED} or
		 * {@link Scalr#THRESHOLD_BALANCED_SPEED} thresholds as cut-offs to
		 * decide between selecting the <code>QUALITY</code>,
		 * <code>BALANCED</code> or <code>SPEED</code> scaling algorithms.
		 * <p/>
		 * By default the thresholds chosen will give nearly the best looking
		 * result in the fastest amount of time. We intend this method to work
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
	 * Used to define the different modes of resizing that the algorithm can
	 * use.
	 * 
	 * @author Riyad Kalla (software@thebuzzmedia.com)
	 * @since 3.1
	 */
	public static enum Mode {
		/**
		 * Used to indicate that the scaling implementation should calculate
		 * dimensions for the resultant image by looking at the image's
		 * orientation and generating proportional dimensions that best fit into
		 * the target width and height given
		 * 
		 * See "Image Proportions" in the {@link Scalr} class description for
		 * more detail.
		 */
		AUTOMATIC,
		/**
		 * Used to fit the image to the exact dimensions given regardless of the
		 * image's proportions. If the dimensions are not proportionally
		 * correct, this will introduce vertical or horizontal stretching to the
		 * image.
		 * <p/>
		 * It is recommended that you use one of the other <code>FIT_TO</code>
		 * modes or {@link Mode#AUTOMATIC} if you want the image to look
		 * correct, but if dimension-fitting is the #1 priority regardless of
		 * how it makes the image look, that is what this mode is for.
		 */
		FIT_EXACT,
		/**
		 * Used to indicate that the scaling implementation should calculate
		 * dimensions for the resultant image that best-fit within the given
		 * width, regardless of the orientation of the image.
		 */
		FIT_TO_WIDTH,
		/**
		 * Used to indicate that the scaling implementation should calculate
		 * dimensions for the resultant image that best-fit within the given
		 * height, regardless of the orientation of the image.
		 */
		FIT_TO_HEIGHT;
	}

	/**
	 * Used to define the different types of rotations that can be applied to an
	 * image during a resize operation.
	 * 
	 * @author Riyad Kalla (software@thebuzzmedia.com)
	 * @since 3.2
	 */
	public static enum Rotation {
		/**
		 * 90-degree, clockwise rotation (to the right). This is equivalent to a
		 * quarter-turn of the image to the right; moving the picture on to its
		 * right side.
		 */
		CW_90,
		/**
		 * 180-degree, clockwise rotation (to the right). This is equivalent to
		 * 1 half-turn of the image to the right; rotating the picture around
		 * until it is upside down from the original position.
		 */
		CW_180,
		/**
		 * 270-degree, clockwise rotation (to the right). This is equivalent to
		 * a quarter-turn of the image to the left; moving the picture on to its
		 * left side.
		 */
		CW_270,
		/**
		 * Flip the image horizontally by reflecting it around the y axis.
		 * <p/>
		 * This is not a standard rotation around a center point, but instead
		 * creates the mirrored reflection of the image horizontally.
		 * <p/>
		 * More specifically, the vertical orientation of the image stays the
		 * same (the top stays on top, and the bottom on bottom), but the right
		 * and left sides flip. This is different than a standard rotation where
		 * the top and bottom would also have been flipped.
		 */
		FLIP_HORZ,
		/**
		 * Flip the image vertically by reflecting it around the x axis.
		 * <p/>
		 * This is not a standard rotation around a center point, but instead
		 * creates the mirrored reflection of the image vertically.
		 * <p/>
		 * More specifically, the horizontal orientation of the image stays the
		 * same (the left stays on the left and the right stays on the right),
		 * but the top and bottom sides flip. This is different than a standard
		 * rotation where the left and right would also have been flipped.
		 */
		FLIP_VERT;
	}

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
	public static final int THRESHOLD_BALANCED_SPEED = 1600;

	/**
	 * Threshold (in pixels) at which point the scaling operation using the
	 * {@link Method#AUTOMATIC} method will decide if a {@link Method#QUALITY}
	 * method will be used (if smaller than or equal to threshold) or a
	 * {@link Method#BALANCED} method will be used (if larger than threshold).
	 * <p/>
	 * The bigger the image is being scaled to, the less noticeable degradations
	 * in the image becomes and the faster algorithms can be selected.
	 * <p/>
	 * The value of this threshold (800) was chosen after visual, by-hand, A/B
	 * testing between different types of images scaled with this library; both
	 * photographs and screenshots. It was determined that images below this
	 * size need to use a {@link Method#QUALITY} scale method to look decent in
	 * most all cases while using the faster {@link Method#BALANCED} method for
	 * images bigger than this threshold showed no noticeable degradation over a
	 * <code>QUALITY</code> scale.
	 */
	public static final int THRESHOLD_QUALITY_BALANCED = 800;

	/*
	 * TODO: Temporarily removed, this should be done more robustly and possibly
	 * in a separate class so as not to make this one so huge and the
	 * class/memory footprint if no one wants to use CLI.
	 */
	// /**
	// * Used to provide command-line functionality to imgscalr so it can be
	// * easily utilized from scripts or other non-Java sources.
	// * <p/>
	// * The only requirements arguments to this method are either
	// * <code>-width</code> or <code>-height</code> and then an input image and
	// * an output image. All other arguments are optional.
	// * <p/>
	// * A full list of optional arguments are:
	// * <ul>
	// * <li>-width X</li>
	// * <li>-height Y</li>
	// * <li>-method (AUTOMATIC | SPEED | BALANCED | QUALITY)</li>
	// * <li>-mode (AUTOMATIC | FIT_TO_WIDTH | FIT_TO_HEIGHT)</li>
	// * <li>-rotation (NONE | CLOCKWISE | COUNTER_CLOCKWISE | FLIP)</li>
	// * <li>-antialias</li>
	// * </ul>
	// * A few command line examples might look like:
	// * <ul>
	// * <li>
	// * <code>java -jar imgscalr-lib-<ver>.jar -width 150
	// /uploads/screenshot.jpg /tmp/thumbnail.jpg</code>
	// * </li>
	// * <li>
	// * <code>java -jar imgscalr-lib-<ver>.jar -width 640 -height 480 -method
	// SPEED -mode FIT_TO_WIDTH -antialias photo.jpg /tmp/thumbnail.jpg</code>
	// * </li>
	// * </ul>
	// * Full usage information can be seen in the <code>printUsage</code>
	// method
	// * source or by running this class from the command line with no
	// arguments.
	// * <h3>Return Codes
	// * <h3>
	// * This method makes use of the following return codes:
	// * <ol>
	// * <li>0 - Success, ran and exited normally.</li>
	// * <li>1 - Failure, invalid number of arguments.</li>
	// * <li>2 - Failure, invalid argument value provided (e.g. '-width HAM' or
	// * '-mode CELERY').</li>
	// * <li>3 - Failure, unknown argument was provided.</li>
	// * <li>4 - Failure, path to source image does not exist or cannot be
	// read.</li>
	// * <li>5 - Failure, general failure while reading or writing the
	// image.</li>
	// * </ol>
	// *
	// * @param args
	// * The command line args to use.
	// */
	// public static void main(String[] args) {
	// // Default to a successful execution.
	// int exitCode = 0;
	// String message = null;
	//
	// if (args != null && args.length > 2) {
	// int width = -1;
	// int height = -1;
	// Method method = null;
	// Mode mode = null;
	// Rotation rotation = null;
	// boolean antialias = false;
	//
	// /*
	// * Stop 1 index short of the end so we can make the option-value
	// * retrieval below easier (i+1 without a check).
	// *
	// * Loop until the end or until an error condition is hit.
	// *
	// * We can also do this because the last 2 arguments should be the
	// * inFile and outFile which we don't process in this loop.
	// */
	// for (int i = 0, end = args.length - 1; exitCode == 0 && i < end; i++) {
	// String arg = args[i];
	//
	// if ("-width".equalsIgnoreCase(arg)) {
	// try {
	// width = Integer.parseInt(args[i + 1]);
	// i++;
	// } catch (Exception e) {
	// exitCode = 2;
	// message = "Invalid int value specified for -width: "
	// + args[i + 1];
	// }
	// } else if ("-height".equalsIgnoreCase(arg)) {
	// try {
	// height = Integer.parseInt(args[i + 1]);
	// i++;
	// } catch (Exception e) {
	// exitCode = 2;
	// message = "Invalid int value specified for -height: "
	// + args[i + 1];
	// }
	// } else if ("-method".equalsIgnoreCase(arg)) {
	// try {
	// method = Method.valueOf(args[i + 1]);
	// i++;
	// } catch (Exception e) {
	// exitCode = 2;
	// message = "Invalid String value specified for -method: "
	// + args[i + 1];
	// }
	// } else if ("-mode".equalsIgnoreCase(arg)) {
	// try {
	// mode = Mode.valueOf(args[i + 1]);
	// i++;
	// } catch (Exception e) {
	// exitCode = 2;
	// message = "Invalid String value specified for -mode: "
	// + args[i + 1];
	// }
	// } else if ("-rotation".equalsIgnoreCase(arg)) {
	// try {
	// rotation = Rotation.valueOf(args[i + 1]);
	// i++;
	// } catch (Exception e) {
	// exitCode = 2;
	// message = "Invalid String value specified for -rotation: "
	// + args[i + 1];
	// }
	// } else if ("-antialias".equalsIgnoreCase(arg))
	// antialias = true;
	// else if (arg.charAt(0) == '-') {
	// exitCode = 3;
	// message = "An unknown argument [" + arg + "] was provided.";
	// }
	// }
	//
	// /*
	// * At this point we have parsed all the given arguments, now we need
	// * to fill in (with defaults) all the values that weren't specified.
	// */
	// if (width == -1)
	// width = height;
	// if (height == -1)
	// height = width;
	//
	// if (method == null)
	// method = Method.AUTOMATIC;
	// if (mode == null)
	// mode = Mode.AUTOMATIC;
	// if (rotation == null)
	// rotation = Rotation.NONE;
	//
	// String inPath = args[args.length - 2];
	// String outPath = args[args.length - 1];
	//
	// File input = new File(inPath);
	// File output = new File(outPath);
	//
	// if (!input.canRead()) {
	// exitCode = 4;
	// message = "Unable to read source image ["
	// + args[args.length - 2]
	// +
	// "], either the path is incorrect or this process does not have read permissions to the file.";
	// } else if (exitCode == 0) {
	// try {
	// // Read file from filesystem.
	// BufferedImage image = ImageIO.read(input);
	//
	// // Scale image with all the settings we parsed.
	// image = Scalr.resize(image, method, mode, rotation, width,
	// height, (antialias ? OP_ANTIALIAS : null));
	//
	// // Determine the output format type.
	// String type = outPath
	// .substring(outPath.lastIndexOf('.') + 1);
	//
	// // Write result to the specified output path.
	// ImageIO.write(image, type, output);
	// } catch (Exception e) {
	// e.printStackTrace();
	//
	// exitCode = 5;
	// message =
	// "An exception occurred while trying to read or write the source or destination image. See the exception above for more information.";
	// }
	// }
	// } else {
	// exitCode = 1;
	// message = "Invalid number of arguments ("
	// + args.length
	// +
	// "). At least a width or height and source and destination image path must be provided.";
	// }
	//
	// /*
	// * If an error occurred above then we have a non-zero exit code, in
	// * which case print the additional error message if there was one and
	// * then the usage information to help the user.
	// */
	// if (exitCode > 0) {
	// if (message != null) {
	// System.out.println(message);
	// System.out.println();
	// }
	//
	// printUsage();
	// }
	//
	// System.exit(exitCode);
	// }
	//
	// private static void printUsage() {
	// System.out
	// .println("imgscalr - Java Image Scaling Library (c) The Buzz Media, LLC");
	// System.out
	// .println("http://www.thebuzzmedia.com/software/imgscalr-java-image-scaling-library/");
	// System.out
	// .println("-------------------------------------------------------------------------");
	// System.out.println("Usage");
	// System.out
	// .println("\tjava -jar imgscalr-lib-<ver>.jar [-options] <source> <destination>");
	// System.out.println("\t<source>\tPath to the source image to process.");
	// System.out
	// .println("\t<destination>\tPath where the resulting image will be written to.");
	// System.out.println("\nOptions");
	// System.out.println("\t-width\tTarget width of resized image.");
	// System.out.println("\t-height\tTarget height of resized image.");
	// System.out
	// .println("\n\tOne or both width/height values must be provided. If one dimension is missing,\n\tthe image's propotions are always honored and the missing dimension will be\n\tcalculated automatically.");
	// System.out
	// .println("\n\t-method\tScaling method to use. Effects the quality of the resulting image.");
	// System.out
	// .println("\t\t"
	// + Method.AUTOMATIC
	// +
	// "\tChoose the most optimal balance between speed/quality based on image size.");
	// System.out
	// .println("\t\t"
	// + Method.SPEED
	// + "\t\tScale as fast as possible, sacrificing some quality to do so.");
	// System.out
	// .println("\t\t"
	// + Method.BALANCED
	// + "\tPerfect balance between speed/quality regardless of image size.");
	// System.out
	// .println("\t\t"
	// + Method.QUALITY
	// +
	// "\t\tScale using the best-looking method, regardless of the impact on performance.");
	// System.out
	// .println("\n\t-mode\tDefines the 'fit-to' behavior used to calculate dimensions for the target image while maintaining the original image proportions.");
	// System.out
	// .println("\t\t"
	// + Mode.AUTOMATIC
	// +
	// "\tHonor the primary dimension, based on orientation, and recalculate the secondary dimension.");
	// System.out
	// .println("\t\t"
	// + Mode.FIT_TO_WIDTH
	// +
	// "\tRegardless of orientation, use the width as the primary fit-to dimension and recalculate the height.");
	// System.out
	// .println("\t\t"
	// + Mode.FIT_TO_HEIGHT
	// +
	// "\tRegardless of orientation, use the height as the primary fit-to dimension and recalculate the width.");
	// System.out
	// .println("\n\t-rotation Rotation to apply to the resulting image.");
	// System.out.println("\t\t" + Rotation.NONE + "\t\t\tApply no rotation.");
	// System.out.println("\t\t" + Rotation.CLOCKWISE
	// + "\t\tApply a 90-degree clockwise (right) rotation.");
	// System.out.println("\t\t" + Rotation.COUNTER_CLOCKWISE
	// + "\tApply a 90-degree counter-clockwise (left) rotation.");
	// System.out.println("\t\t" + Rotation.FLIP
	// + "\t\t\tApply a 180-degree (flip) rotation.");
	// System.out
	// .println("\n\t-antialias\tFlag used to indicate if the default antialiasing filter should be applied to the resulting image or not.");
	// System.out.println("\nExamples");
	// System.out
	// .println("\tjava -jar imgscalr-lib-<ver>.jar -width 150 /uploads/screenshot.jpg /tmp/thumbnail.jpg");
	// System.out
	// .println("\tjava -jar imgscalr-lib-<ver>.jar -width 640 -height 480 -method SPEED -mode FIT_TO_WIDTH -antialias photo.jpg /tmp/thumbnail.jpg");
	// System.out.println("\n");
	// System.out.println("See Javadoc for more detail:");
	// System.out
	// .println("http://www.thebuzzmedia.com/downloads/software/imgscalr/javadoc/index.html");
	// }

	/**
	 * Used to apply <code>0</code> or more {@link BufferedImageOp}s to a given
	 * {@link BufferedImage}.
	 * <p/>
	 * This implementation works around <a
	 * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4965606">a
	 * decade-old JDK bug</a> that can cause a {@link RasterFormatException}
	 * when applying a perfectly valid {@link BufferedImageOp}. It is
	 * recommended you always use this method to apply any custom ops instead of
	 * relying on directly using the op via the
	 * {@link BufferedImageOp#filter(BufferedImage, BufferedImage)} method.
	 * 
	 * @param src
	 *            The image that will have the ops applied to it.
	 * @param ops
	 *            <code>0</code> or more ops to apply to the image. If
	 *            <code>null</code> or empty then <code>src</code> is return
	 *            unmodified.
	 * 
	 * @return the resulting image after the given ops have been applied to it.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>src</code> is <code>null</code>
	 */
	public static BufferedImage apply(BufferedImage src, BufferedImageOp... ops)
			throws IllegalArgumentException {
		if (src == null)
			throw new IllegalArgumentException(
					"src cannot be null, a valid BufferedImage instance must be provided.");

		BufferedImage result = null;

		// Do nothing if no ops were specified.
		if (ops == null || ops.length == 0)
			result = src;
		else {
			log("Applying %d Image Ops", ops.length);

			for (BufferedImageOp op : ops) {
				// In case a null op was passed in, skip it instead of dying
				if (op == null)
					continue;

				long startTime = System.currentTimeMillis();
				Rectangle2D dims = op.getBounds2D(src);

				/*
				 * We must manually create the target image; we cannot rely on
				 * the null-dest filter() method to create a valid destination
				 * for us thanks to this JDK bug that has been filed for almost
				 * a decade:
				 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4965606
				 */
				BufferedImage dest = createOptimalImage(src,
						(int) Math.round(dims.getWidth()),
						(int) Math.round(dims.getHeight()));

				// Update the result we will return.
				result = op.filter(src, dest);

				if (DEBUG)
					log("\tOp Applied in %d ms, Resultant Image [width=%d, height=%d], Op: %s",
							(System.currentTimeMillis() - startTime),
							result.getWidth(), result.getHeight(), op);
			}
		}

		return result;
	}

	public static BufferedImage crop(BufferedImage src, int width, int height,
			BufferedImageOp... ops) throws IllegalArgumentException {
		return crop(src, 0, 0, width, height, ops);
	}

	public static BufferedImage crop(BufferedImage src, int x, int y,
			int width, int height, BufferedImageOp... ops)
			throws IllegalArgumentException {
		// TODO: implement
		return null;
	}

	public static BufferedImage pad(BufferedImage src, int padding,
			Color color, BufferedImageOp... ops)
			throws IllegalArgumentException {
		// TODO: allow filling with transparency.
		// http://stackoverflow.com/questions/4565909/java-graphics2d-filling-with-transparency
		// TODO: implement
		return null;
	}

	/**
	 * Resize a given image (maintaining its original proportion) to a width and
	 * height no bigger than <code>targetSize</code> and apply the given
	 * {@link BufferedImageOp}s (if any) to the result before returning it.
	 * <p/>
	 * A scaling method of {@link Method#AUTOMATIC} and mode of
	 * {@link Mode#AUTOMATIC} are used.
	 * <p/>
	 * <strong>Performance</strong>: Not all {@link BufferedImageOp}s are
	 * hardware accelerated operations, but many of the most popular (like
	 * {@link ConvolveOp}) are. For more information on if your image op is
	 * hardware accelerated or not, check the source code of the underlying JDK
	 * class that actually executes the Op code, <a href=
	 * "http://www.docjar.com/html/api/sun/awt/image/ImagingLib.java.html"
	 * >sun.awt.image.ImagingLib</a>.
	 * <p/>
	 * <strong>Performance</strong>: This operation leaves the original
	 * <code>src</code> image unmodified. If the caller is done with the
	 * <code>src</code> image after getting the result of this operation,
	 * remember to call {@link BufferedImage#flush()} on the <code>src</code> to
	 * free up native resources and make it easier for the GC to collect the
	 * unused image.
	 * 
	 * @param src
	 *            The image that will be scaled.
	 * @param targetSize
	 *            The target width and height (square) that you wish the image
	 *            to fit within.
	 * @param ops
	 *            Zero or more optional image operations (e.g. sharpen, blur,
	 *            etc.) that can be applied to the final result before returning
	 *            the image.
	 * 
	 * @return the proportionally scaled image with either a width or height of
	 *         the given target size.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>src</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>targetSize</code> is &lt; 0.
	 */
	public static BufferedImage resize(BufferedImage src, int targetSize,
			BufferedImageOp... ops) throws IllegalArgumentException {
		return resize(src, Method.AUTOMATIC, Mode.AUTOMATIC, targetSize,
				targetSize, ops);
	}

	/**
	 * Resize a given image (maintaining its original proportion) to a width and
	 * height no bigger than <code>targetSize</code> using the given scaling
	 * method and apply the given {@link BufferedImageOp}s (if any) to the
	 * result before returning it.
	 * <p/>
	 * A mode of {@link Mode#AUTOMATIC} is used.
	 * <p/>
	 * <strong>Performance</strong>: Not all {@link BufferedImageOp}s are
	 * hardware accelerated operations, but many of the most popular (like
	 * {@link ConvolveOp}) are. For more information on if your image op is
	 * hardware accelerated or not, check the source code of the underlying JDK
	 * class that actually executes the Op code, <a href=
	 * "http://www.docjar.com/html/api/sun/awt/image/ImagingLib.java.html"
	 * >sun.awt.image.ImagingLib</a>.
	 * <p/>
	 * <strong>Performance</strong>: This operation leaves the original
	 * <code>src</code> image unmodified. If the caller is done with the
	 * <code>src</code> image after getting the result of this operation,
	 * remember to call {@link BufferedImage#flush()} on the <code>src</code> to
	 * free up native resources and make it easier for the GC to collect the
	 * unused image.
	 * 
	 * @param src
	 *            The image that will be scaled.
	 * @param scalingMethod
	 *            The method used for scaling the image; preferring speed to
	 *            quality or a balance of both.
	 * @param targetSize
	 *            The target width and height (square) that you wish the image
	 *            to fit within.
	 * @param ops
	 *            Zero or more optional image operations (e.g. sharpen, blur,
	 *            etc.) that can be applied to the final result before returning
	 *            the image.
	 * 
	 * @return the proportionally scaled image with either a width or height of
	 *         the given target size.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>src</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>scalingMethod</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>targetSize</code> is &lt; 0.
	 * 
	 * @see Method
	 */
	public static BufferedImage resize(BufferedImage src, Method scalingMethod,
			int targetSize, BufferedImageOp... ops)
			throws IllegalArgumentException {
		return resize(src, scalingMethod, Mode.AUTOMATIC, targetSize,
				targetSize, ops);
	}

	/**
	 * Resize a given image (maintaining its original proportion) to a width and
	 * height no bigger than <code>targetSize</code> (or fitting the image to
	 * the given WIDTH or HEIGHT explicitly, depending on the {@link Mode}
	 * specified) and apply the given {@link BufferedImageOp}s (if any) to the
	 * result before returning it.
	 * <p/>
	 * A scaling method of {@link Method#AUTOMATIC} is used.
	 * <p/>
	 * <strong>Performance</strong>: Not all {@link BufferedImageOp}s are
	 * hardware accelerated operations, but many of the most popular (like
	 * {@link ConvolveOp}) are. For more information on if your image op is
	 * hardware accelerated or not, check the source code of the underlying JDK
	 * class that actually executes the Op code, <a href=
	 * "http://www.docjar.com/html/api/sun/awt/image/ImagingLib.java.html"
	 * >sun.awt.image.ImagingLib</a>.
	 * <p/>
	 * <strong>Performance</strong>: This operation leaves the original
	 * <code>src</code> image unmodified. If the caller is done with the
	 * <code>src</code> image after getting the result of this operation,
	 * remember to call {@link BufferedImage#flush()} on the <code>src</code> to
	 * free up native resources and make it easier for the GC to collect the
	 * unused image.
	 * 
	 * @param src
	 *            The image that will be scaled.
	 * @param resizeMode
	 *            Used to indicate how imgscalr should calculate the final
	 *            target size for the image, either fitting the image to the
	 *            given width ({@link Mode#FIT_TO_WIDTH}) or fitting the image
	 *            to the given height ({@link Mode#FIT_TO_HEIGHT}). If
	 *            {@link Mode#AUTOMATIC} is passed in, imgscalr will calculate
	 *            proportional dimensions for the scaled image based on its
	 *            orientation (landscape, square or portrait). Unless you have
	 *            very specific size requirements, most of the time you just
	 *            want to use {@link Mode#AUTOMATIC} to "do the right thing".
	 * @param targetSize
	 *            The target width and height (square) that you wish the image
	 *            to fit within.
	 * @param ops
	 *            Zero or more optional image operations (e.g. sharpen, blur,
	 *            etc.) that can be applied to the final result before returning
	 *            the image.
	 * 
	 * @return the proportionally scaled image with either a width or height of
	 *         the given target size.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>src</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>resizeMode</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>targetSize</code> is &lt; 0.
	 * 
	 * @see Mode
	 */
	public static BufferedImage resize(BufferedImage src, Mode resizeMode,
			int targetSize, BufferedImageOp... ops)
			throws IllegalArgumentException {
		return resize(src, Method.AUTOMATIC, resizeMode, targetSize,
				targetSize, ops);
	}

	/**
	 * Resize a given image (maintaining its original proportion) to a width and
	 * height no bigger than <code>targetSize</code> (or fitting the image to
	 * the given WIDTH or HEIGHT explicitly, depending on the {@link Mode}
	 * specified) using the given scaling method and apply the given
	 * {@link BufferedImageOp}s (if any) to the result before returning it.
	 * <p/>
	 * <strong>Performance</strong>: Not all {@link BufferedImageOp}s are
	 * hardware accelerated operations, but many of the most popular (like
	 * {@link ConvolveOp}) are. For more information on if your image op is
	 * hardware accelerated or not, check the source code of the underlying JDK
	 * class that actually executes the Op code, <a href=
	 * "http://www.docjar.com/html/api/sun/awt/image/ImagingLib.java.html"
	 * >sun.awt.image.ImagingLib</a>.
	 * <p/>
	 * <strong>Performance</strong>: This operation leaves the original
	 * <code>src</code> image unmodified. If the caller is done with the
	 * <code>src</code> image after getting the result of this operation,
	 * remember to call {@link BufferedImage#flush()} on the <code>src</code> to
	 * free up native resources and make it easier for the GC to collect the
	 * unused image.
	 * 
	 * @param src
	 *            The image that will be scaled.
	 * @param scalingMethod
	 *            The method used for scaling the image; preferring speed to
	 *            quality or a balance of both.
	 * @param resizeMode
	 *            Used to indicate how imgscalr should calculate the final
	 *            target size for the image, either fitting the image to the
	 *            given width ({@link Mode#FIT_TO_WIDTH}) or fitting the image
	 *            to the given height ({@link Mode#FIT_TO_HEIGHT}). If
	 *            {@link Mode#AUTOMATIC} is passed in, imgscalr will calculate
	 *            proportional dimensions for the scaled image based on its
	 *            orientation (landscape, square or portrait). Unless you have
	 *            very specific size requirements, most of the time you just
	 *            want to use {@link Mode#AUTOMATIC} to "do the right thing".
	 * @param targetSize
	 *            The target width and height (square) that you wish the image
	 *            to fit within.
	 * @param ops
	 *            Zero or more optional image operations (e.g. sharpen, blur,
	 *            etc.) that can be applied to the final result before returning
	 *            the image.
	 * 
	 * @return the proportionally scaled image with either a width or height of
	 *         the given target size.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>src</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>scalingMethod</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>resizeMode</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>targetSize</code> is &lt; 0.
	 * 
	 * @see Method
	 * @see Mode
	 */
	public static BufferedImage resize(BufferedImage src, Method scalingMethod,
			Mode resizeMode, int targetSize, BufferedImageOp... ops)
			throws IllegalArgumentException {
		return resize(src, scalingMethod, resizeMode, targetSize, targetSize,
				ops);
	}

	/**
	 * Resize a given image (maintaining its original proportion) to the target
	 * width and height and apply the given {@link BufferedImageOp}s (if any) to
	 * the result before returning it.
	 * <p/>
	 * A scaling method of {@link Method#AUTOMATIC} and mode of
	 * {@link Mode#AUTOMATIC} are used.
	 * <p/>
	 * <strong>TIP</strong>: See the class description to understand how this
	 * class handles recalculation of the <code>targetWidth</code> or
	 * <code>targetHeight</code> depending on the image's orientation in order
	 * to maintain the original proportion.
	 * <p/>
	 * <strong>Performance</strong>: Not all {@link BufferedImageOp}s are
	 * hardware accelerated operations, but many of the most popular (like
	 * {@link ConvolveOp}) are. For more information on if your image op is
	 * hardware accelerated or not, check the source code of the underlying JDK
	 * class that actually executes the Op code, <a href=
	 * "http://www.docjar.com/html/api/sun/awt/image/ImagingLib.java.html"
	 * >sun.awt.image.ImagingLib</a>.
	 * <p/>
	 * <strong>Performance</strong>: This operation leaves the original
	 * <code>src</code> image unmodified. If the caller is done with the
	 * <code>src</code> image after getting the result of this operation,
	 * remember to call {@link BufferedImage#flush()} on the <code>src</code> to
	 * free up native resources and make it easier for the GC to collect the
	 * unused image.
	 * 
	 * @param src
	 *            The image that will be scaled.
	 * @param targetWidth
	 *            The target width that you wish the image to have.
	 * @param targetHeight
	 *            The target height that you wish the image to have.
	 * @param ops
	 *            Zero or more optional image operations (e.g. sharpen, blur,
	 *            etc.) that can be applied to the final result before returning
	 *            the image.
	 * 
	 * @return the proportionally scaled image with either a width or height of
	 *         the given target size.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>src</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>targetWidth</code> is &lt; 0 or if
	 *             <code>targetHeight</code> is &lt; 0.
	 */
	public static BufferedImage resize(BufferedImage src, int targetWidth,
			int targetHeight, BufferedImageOp... ops)
			throws IllegalArgumentException {
		return resize(src, Method.AUTOMATIC, Mode.AUTOMATIC, targetWidth,
				targetHeight, ops);
	}

	/**
	 * Resize a given image (maintaining its original proportion) to the target
	 * width and height using the given scaling method and apply the given
	 * {@link BufferedImageOp}s (if any) to the result before returning it.
	 * <p/>
	 * A mode of {@link Mode#AUTOMATIC} is used.
	 * <p/>
	 * <strong>TIP</strong>: See the class description to understand how this
	 * class handles recalculation of the <code>targetWidth</code> or
	 * <code>targetHeight</code> depending on the image's orientation in order
	 * to maintain the original proportion.
	 * <p/>
	 * <strong>Performance</strong>: Not all {@link BufferedImageOp}s are
	 * hardware accelerated operations, but many of the most popular (like
	 * {@link ConvolveOp}) are. For more information on if your image op is
	 * hardware accelerated or not, check the source code of the underlying JDK
	 * class that actually executes the Op code, <a href=
	 * "http://www.docjar.com/html/api/sun/awt/image/ImagingLib.java.html"
	 * >sun.awt.image.ImagingLib</a>.
	 * <p/>
	 * <strong>Performance</strong>: This operation leaves the original
	 * <code>src</code> image unmodified. If the caller is done with the
	 * <code>src</code> image after getting the result of this operation,
	 * remember to call {@link BufferedImage#flush()} on the <code>src</code> to
	 * free up native resources and make it easier for the GC to collect the
	 * unused image.
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
	 * @param ops
	 *            Zero or more optional image operations (e.g. sharpen, blur,
	 *            etc.) that can be applied to the final result before returning
	 *            the image.
	 * 
	 * @return the proportionally scaled image no bigger than the given width
	 *         and height.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>src</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>scalingMethod</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>targetWidth</code> is &lt; 0 or if
	 *             <code>targetHeight</code> is &lt; 0.
	 * 
	 * @see Method
	 */
	public static BufferedImage resize(BufferedImage src, Method scalingMethod,
			int targetWidth, int targetHeight, BufferedImageOp... ops) {
		return resize(src, scalingMethod, Mode.AUTOMATIC, targetWidth,
				targetHeight, ops);
	}

	/**
	 * Resize a given image (maintaining its original proportion) to the target
	 * width and height (or fitting the image to the given WIDTH or HEIGHT
	 * explicitly, depending on the {@link Mode} specified) and apply the given
	 * {@link BufferedImageOp}s (if any) to the result before returning it.
	 * <p/>
	 * A scaling method of {@link Method#AUTOMATIC} is used.
	 * <p/>
	 * <strong>TIP</strong>: See the class description to understand how this
	 * class handles recalculation of the <code>targetWidth</code> or
	 * <code>targetHeight</code> depending on the image's orientation in order
	 * to maintain the original proportion.
	 * <p/>
	 * <strong>Performance</strong>: Not all {@link BufferedImageOp}s are
	 * hardware accelerated operations, but many of the most popular (like
	 * {@link ConvolveOp}) are. For more information on if your image op is
	 * hardware accelerated or not, check the source code of the underlying JDK
	 * class that actually executes the Op code, <a href=
	 * "http://www.docjar.com/html/api/sun/awt/image/ImagingLib.java.html"
	 * >sun.awt.image.ImagingLib</a>.
	 * <p/>
	 * <strong>Performance</strong>: This operation leaves the original
	 * <code>src</code> image unmodified. If the caller is done with the
	 * <code>src</code> image after getting the result of this operation,
	 * remember to call {@link BufferedImage#flush()} on the <code>src</code> to
	 * free up native resources and make it easier for the GC to collect the
	 * unused image.
	 * 
	 * @param src
	 *            The image that will be scaled.
	 * @param resizeMode
	 *            Used to indicate how imgscalr should calculate the final
	 *            target size for the image, either fitting the image to the
	 *            given width ({@link Mode#FIT_TO_WIDTH}) or fitting the image
	 *            to the given height ({@link Mode#FIT_TO_HEIGHT}). If
	 *            {@link Mode#AUTOMATIC} is passed in, imgscalr will calculate
	 *            proportional dimensions for the scaled image based on its
	 *            orientation (landscape, square or portrait). Unless you have
	 *            very specific size requirements, most of the time you just
	 *            want to use {@link Mode#AUTOMATIC} to "do the right thing".
	 * @param targetWidth
	 *            The target width that you wish the image to have.
	 * @param targetHeight
	 *            The target height that you wish the image to have.
	 * @param ops
	 *            Zero or more optional image operations (e.g. sharpen, blur,
	 *            etc.) that can be applied to the final result before returning
	 *            the image.
	 * 
	 * @return the proportionally scaled image with either a width or height of
	 *         the given target size.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>src</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>resizeMode</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>targetWidth</code> is &lt; 0 or if
	 *             <code>targetHeight</code> is &lt; 0.
	 * 
	 * @see Mode
	 */
	public static BufferedImage resize(BufferedImage src, Mode resizeMode,
			int targetWidth, int targetHeight, BufferedImageOp... ops)
			throws IllegalArgumentException {
		return resize(src, Method.AUTOMATIC, resizeMode, targetWidth,
				targetHeight, ops);
	}

	/**
	 * Resize a given image (maintaining its original proportion) to the target
	 * width and height (or fitting the image to the given WIDTH or HEIGHT
	 * explicitly, depending on the {@link Mode} specified) using the given
	 * scaling method and apply the given {@link BufferedImageOp}s (if any) to
	 * the result before returning it.
	 * <p/>
	 * <strong>TIP</strong>: See the class description to understand how this
	 * class handles recalculation of the <code>targetWidth</code> or
	 * <code>targetHeight</code> depending on the image's orientation in order
	 * to maintain the original proportion.
	 * <p/>
	 * <strong>Performance</strong>: Not all {@link BufferedImageOp}s are
	 * hardware accelerated operations, but many of the most popular (like
	 * {@link ConvolveOp}) are. For more information on if your image op is
	 * hardware accelerated or not, check the source code of the underlying JDK
	 * class that actually executes the Op code, <a href=
	 * "http://www.docjar.com/html/api/sun/awt/image/ImagingLib.java.html"
	 * >sun.awt.image.ImagingLib</a>.
	 * <p/>
	 * <strong>Performance</strong>: This operation leaves the original
	 * <code>src</code> image unmodified. If the caller is done with the
	 * <code>src</code> image after getting the result of this operation,
	 * remember to call {@link BufferedImage#flush()} on the <code>src</code> to
	 * free up native resources and make it easier for the GC to collect the
	 * unused image.
	 * 
	 * @param src
	 *            The image that will be scaled.
	 * @param scalingMethod
	 *            The method used for scaling the image; preferring speed to
	 *            quality or a balance of both.
	 * @param resizeMode
	 *            Used to indicate how imgscalr should calculate the final
	 *            target size for the image, either fitting the image to the
	 *            given width ({@link Mode#FIT_TO_WIDTH}) or fitting the image
	 *            to the given height ({@link Mode#FIT_TO_HEIGHT}). If
	 *            {@link Mode#AUTOMATIC} is passed in, imgscalr will calculate
	 *            proportional dimensions for the scaled image based on its
	 *            orientation (landscape, square or portrait). Unless you have
	 *            very specific size requirements, most of the time you just
	 *            want to use {@link Mode#AUTOMATIC} to "do the right thing".
	 * @param targetWidth
	 *            The target width that you wish the image to have.
	 * @param targetHeight
	 *            The target height that you wish the image to have.
	 * @param ops
	 *            Zero or more optional image operations (e.g. sharpen, blur,
	 *            etc.) that can be applied to the final result before returning
	 *            the image.
	 * 
	 * @return the proportionally scaled image no bigger than the given width
	 *         and height.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>src</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>scalingMethod</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>resizeMode</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if <code>targetWidth</code> is &lt; 0 or if
	 *             <code>targetHeight</code> is &lt; 0.
	 * 
	 * @see Method
	 * @see Mode
	 */
	public static BufferedImage resize(BufferedImage src, Method scalingMethod,
			Mode resizeMode, int targetWidth, int targetHeight,
			BufferedImageOp... ops) throws IllegalArgumentException {
		if (src == null)
			throw new IllegalArgumentException(
					"src cannot be null, a valid BufferedImage instance must be provided.");
		if (scalingMethod == null)
			throw new IllegalArgumentException(
					"scalingMethod cannot be null. A good default value is Method.AUTOMATIC.");
		if (resizeMode == null)
			throw new IllegalArgumentException(
					"resizeMode cannot be null. A good default value is Mode.AUTOMATIC.");
		if (targetWidth < 0)
			throw new IllegalArgumentException("targetWidth must be >= 0");
		if (targetHeight < 0)
			throw new IllegalArgumentException("targetHeight must be >= 0");

		BufferedImage result = null;

		long startTime = System.currentTimeMillis();

		int currentWidth = src.getWidth();
		int currentHeight = src.getHeight();

		// <= 1 is a square or landscape-oriented image, > 1 is a portrait.
		float ratio = ((float) currentHeight / (float) currentWidth);

		if (DEBUG)
			log("START Resizing Source Image [size=%dx%d, mode=%s, orientation=%s, ratio(H/W)=%f] to [targetSize=%dx%d]",
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
		}

		// If AUTOMATIC was specified, determine the real scaling method.
		if (scalingMethod == Scalr.Method.AUTOMATIC)
			scalingMethod = determineScalingMethod(targetWidth, targetHeight,
					ratio);

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
				log("\tQUALITY Up-scale, single BICUBIC scaling will be used...");

				/*
				 * BILINEAR and BICUBIC look similar the smaller the scale jump
				 * upwards is, if the scale is larger BICUBIC looks sharper and
				 * less fuzzy. But most importantly we have to use BICUBIC to
				 * match the contract of the QUALITY rendering method. This note
				 * is just here for anyone reading the code and wondering how
				 * they can speed their own calls up.
				 */
				result = scaleImage(src, targetWidth, targetHeight,
						RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			} else {
				log("\tQUALITY Down-scale, incremental scaling will be used...");

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

		// Apply the image ops if any were provided
		result = apply(result, ops);

		if (DEBUG)
			log("END Source Image Scaled from [%dx%d] to [%dx%d] and %d BufferedImageOp(s) Applied in %d ms",
					currentWidth, currentHeight, result.getWidth(),
					result.getHeight(), (ops == null ? 0 : ops.length),
					System.currentTimeMillis() - startTime);

		return result;
	}

	/**
	 * Used to apply a {@link Rotation} and then <code>0</code> or more
	 * {@link BufferedImageOp}s to a given image and return the result.
	 * <p/>
	 * <strong>Performance</strong>: Not all {@link BufferedImageOp}s are
	 * hardware accelerated operations, but many of the most popular (like
	 * {@link ConvolveOp}) are. For more information on if your image op is
	 * hardware accelerated or not, check the source code of the underlying JDK
	 * class that actually executes the Op code, <a href=
	 * "http://www.docjar.com/html/api/sun/awt/image/ImagingLib.java.html"
	 * >sun.awt.image.ImagingLib</a>.
	 * <p/>
	 * <strong>Performance</strong>: This operation leaves the original
	 * <code>src</code> image unmodified. If the caller is done with the
	 * <code>src</code> image after getting the result of this operation,
	 * remember to call {@link BufferedImage#flush()} on the <code>src</code> to
	 * free up native resources and make it easier for the GC to collect the
	 * unused image.
	 * 
	 * @param src
	 *            The image that will have the rotation applied to it.
	 * @param rotation
	 *            The rotation that will be applied to the image. If
	 *            <code>null</code> then no rotation is applied to the image.
	 * @param ops
	 *            Zero or more optional image operations (e.g. sharpen, blur,
	 *            etc.) that can be applied to the final result before returning
	 *            the image.
	 * 
	 * @return the rotated image with the given ops applied to it.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>src</code> is <code>null</code>.
	 * 
	 * @see Rotation
	 */
	public static BufferedImage rotate(BufferedImage src, Rotation rotation,
			BufferedImageOp... ops) throws IllegalArgumentException {
		if (src == null)
			throw new IllegalArgumentException(
					"src cannot be null, a valid BufferedImage instance must be provided.");

		BufferedImage result = null;

		// Do nothing if no rotation was requested.
		if (rotation == null)
			result = src;
		else {
			log("Applying %s rotation to image...", rotation);

			long startTime = System.currentTimeMillis();

			/*
			 * Setup the default width/height values from our image. In the case
			 * of a 90 or 270 (-90) degree rotation, these two values flip-flop
			 * and we will correct those cases down below in the switch
			 * statement.
			 */
			int newWidth = src.getWidth();
			int newHeight = src.getHeight();

			/*
			 * We create a transform per operation request as (oddly enough) it
			 * ends up being faster for the VM to create, use and destroy these
			 * instances than it is to re-use a single AffineTransform
			 * per-thread via the AffineTransform.setTo(...) methods which was
			 * my first choice (less object creation).
			 * 
			 * Reusing AffineTransforms like that would have required
			 * ThreadLocal instances to avoid race conditions where two or more
			 * resize threads are manipulating the same transform before
			 * applying it.
			 * 
			 * ThreadLocals are one of the #1 reasons for memory leaks in server
			 * applications and since we have no nice way to hook into the
			 * init/destroy Servlet cycle or any other initialization cycle for
			 * this library to automatically call ThreadLocal.remove() to avoid
			 * the memory leak, it would have made using this library *safely*
			 * on the server side much harder.
			 * 
			 * So we opt for creating individual transforms per rotation op and
			 * let the VM clean them up in a GC.
			 */
			AffineTransform tx = new AffineTransform();

			switch (rotation) {
			case CW_90:
				/*
				 * A 90 or -90 degree rotation will cause the height and width
				 * to flip-flop from the original image to the rotated one.
				 */
				newWidth = src.getHeight();
				newHeight = src.getWidth();

				// Reminder: newWidth == result.getHeight() at this point
				tx.translate(newWidth, 0);
				tx.rotate(Math.toRadians(90));

				break;

			case CW_270:
				/*
				 * A 90 or -90 degree rotation will cause the height and width
				 * to flip-flop from the original image to the rotated one.
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
			BufferedImage rotatedImage = createOptimalImage(src, newWidth,
					newHeight);
			Graphics2D g2d = (Graphics2D) rotatedImage.createGraphics();

			/*
			 * Render the resultant image to our new rotatedImage buffer,
			 * applying the AffineTransform that we calculated above during
			 * rendering so the pixels from the old position to the new
			 * transposed positions are mapped correctly.
			 */
			g2d.drawImage(src, tx, null);
			g2d.dispose();

			// Reassign the result to our rotated image before returning it.
			result = rotatedImage;

			if (DEBUG)
				log("\t%s Rotation Applied in %d ms, Resultant Image [width=%d, height=%d]",
						rotation, (System.currentTimeMillis() - startTime),
						result.getWidth(), result.getHeight());
		}

		// Apply any optional operations.
		result = apply(result, ops);

		return result;
	}

	/**
	 * Helper method used to ensure a message is loggable before it is logged
	 * and then pre-pend a universal prefix to all log messages generated by
	 * this library to make the log entries easy to parse visually or
	 * programmatically.
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
	 * @see #LOG_PREFIX
	 */
	protected static void log(String message, Object... params) {
		if (DEBUG) {
			System.out.print(LOG_PREFIX);
			System.out.printf(message, params);
			System.out.println();
		}
	}

	/**
	 * Used to determine the scaling {@link Method} that is best suited for
	 * scaling the image to the targeted dimensions.
	 * <p/>
	 * This method is intended to be used to select a specific scaling
	 * {@link Method} when a {@link Method#AUTOMATIC} method is specified. This
	 * method utilizes the {@link #THRESHOLD_QUALITY_BALANCED} and
	 * {@link #THRESHOLD_BALANCED_SPEED} thresholds when selecting which method
	 * should be used by comparing the primary dimension (width or height)
	 * against the threshold and seeing where the image falls. The primary
	 * dimension is determined by looking at the orientation of the image:
	 * landscape or square images use their width and portrait-oriented images
	 * use their height.
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
		if (length <= THRESHOLD_QUALITY_BALANCED)
			result = Method.QUALITY;
		else if (length <= THRESHOLD_BALANCED_SPEED)
			result = Method.BALANCED;

		if (DEBUG)
			log("AUTOMATIC Scaling Method Selected [%s] for Image [size=%dx%d]",
					result.name(), targetWidth, targetHeight);

		return result;
	}

	/**
	 * Used to create a {@link BufferedImage} instance with the most optimal RGB
	 * TYPE ( {@link BufferedImage#TYPE_INT_RGB} or
	 * {@link BufferedImage#TYPE_INT_ARGB} ) capable of being rendered into from
	 * the given <code>src</code>. The width and height of both images will be
	 * identical.
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
	 *            The image used to determine the optimal type for (or if it is
	 *            already is the optimal type).
	 * 
	 * @see <a
	 *      href="http://www.mail-archive.com/java2d-interest@capra.eng.sun.com/msg05621.html">How
	 *      Java2D handles poorly supported image types</a>
	 * @see <a
	 *      href="http://code.google.com/p/java-image-scaling/source/browse/trunk/src/main/java/com/mortennobel/imagescaling/MultiStepRescaleOp.java">Thanks
	 *      to Morten Nobel for implementation hint</a>
	 */
	protected static BufferedImage createOptimalImage(BufferedImage src) {
		return createOptimalImage(src, src.getWidth(), src.getHeight());
	}

	/**
	 * Used to create a {@link BufferedImage} instance with the given dimensions
	 * and the most optimal RGB TYPE ( {@link BufferedImage#TYPE_INT_RGB} or
	 * {@link BufferedImage#TYPE_INT_ARGB} ) capable of being rendered into from
	 * the given <code>src</code>.
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
	 *            The image used to determine the optimal type for (or if it is
	 *            already is the optimal type).
	 * @param targetWidth
	 *            The width of the newly created resulting image.
	 * @param targetHeight
	 *            The height of the newly created resulting image.
	 * 
	 * @see <a
	 *      href="http://www.mail-archive.com/java2d-interest@capra.eng.sun.com/msg05621.html">How
	 *      Java2D handles poorly supported image types</a>
	 * @see <a
	 *      href="http://code.google.com/p/java-image-scaling/source/browse/trunk/src/main/java/com/mortennobel/imagescaling/MultiStepRescaleOp.java">Thanks
	 *      to Morten Nobel for implementation hint</a>
	 */
	protected static BufferedImage createOptimalImage(BufferedImage src,
			int targetWidth, int targetHeight) {
		int optimalType = (src.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB
				: BufferedImage.TYPE_INT_ARGB);
		return new BufferedImage(targetWidth, targetHeight, optimalType);
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
	protected static BufferedImage scaleImage(BufferedImage src,
			int targetWidth, int targetHeight, Object interpolationHintValue) {
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