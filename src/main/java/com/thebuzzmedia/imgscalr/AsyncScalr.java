package com.thebuzzmedia.imgscalr;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImagingOpException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.thebuzzmedia.imgscalr.Scalr.Method;
import com.thebuzzmedia.imgscalr.Scalr.Mode;
import com.thebuzzmedia.imgscalr.Scalr.Rotation;

// TODO: update class javadoc

/**
 * Class used to provide the asynchronous versions of all the methods defined in
 * {@link Scalr} for the purpose of efficiently handling large amounts of image
 * operations via a select number of processing threads.
 * <p/>
 * Given that image-scaling operations, especially when working with large
 * images, can be very hardware-intensive (both CPU and memory), in large-scale
 * deployments (e.g. a busy web application) it becomes increasingly important
 * that the scale operations performed by imgscalr be manageable so as not to
 * fire off too many simultaneous operations that the JVM's heap explodes and
 * runs out of memory or pegs the CPU on the host machine, staving all other
 * running processes.
 * <p/>
 * Up until now it was left to the caller to implement their own serialization
 * or limiting logic to handle these use-cases. Given imgscalr's popularity in
 * web applications it was determined that this requirement be common enough
 * that it should be integrated directly into the imgscalr library for everyone
 * to benefit from.
 * <p/>
 * Every method in this class wraps the mirrored calls in the {@link Scalr}
 * class in new {@link Callable} instances that are submitted to an internal
 * {@link ExecutorService} for execution at a later date. A {@link Future} is
 * returned to the caller representing the task that will perform the scale
 * operation. {@link Future#get()} or {@link Future#get(long, TimeUnit)} can be
 * used to block on the returned <code>Future</code>, waiting for the scale
 * operation to complete and return the resultant {@link BufferedImage} to the
 * caller.
 * <p/>
 * This design provides the following features:
 * <ul>
 * <li>Non-blocking, asynchronous scale operations that can continue execution
 * while waiting on the scaled result.</li>
 * <li>Serialize all scale requests down into a maximum number of
 * <em>simultaneous</em> scale operations with no additional/complex logic. The
 * number of simultaneous scale operations is caller-configurable so as best to
 * optimize the host system (e.g. 1 scale thread per core).</li>
 * <li>No need to worry about overloading the host system with too many scale
 * operations, they will simply queue up in this class and execute in-order.</li>
 * <li>Synchronous/blocking behavior can still be achieved (if desired) by
 * calling <code>get()</code> or <code>get(long, TimeUnit)</code> immediately on
 * the returned {@link Future} from any of the methods below.</li>
 * </ul>
 * <h3>Performance</h3>
 * When tuning this class for optimal performance, benchmarking your particular
 * hardware is the best approach. For some rough guidelines though, there are
 * two resources you want to watch closely:
 * <ol>
 * <li>JVM Heap Memory (Assume physical machine memory is always sufficiently
 * large)</li>
 * <li># of CPU Cores</li>
 * </ol>
 * You never want to allocate more scaling threads than you have CPU cores and
 * on a sufficiently busy host where some of the cores may be busy running a
 * database or a web server, you will want to allocate even less scaling
 * threads.
 * <p/>
 * So as a maximum you would never want more scaling threads than CPU cores in
 * any situation and less so on a busy server.
 * <p/>
 * If you allocate more threads than you have available CPU cores, your scaling
 * operations will slow down as the CPU will spend a considerable amount of time
 * context-switching between threads on the same core trying to finish all the
 * tasks in parallel. You might still be tempted to do this because of the I/O
 * delay some threads will encounter reading images off disk, but when you do
 * your own benchmarking you'll likely find (as I did) that the actual disk I/O
 * necessary to pull the image data off disk is a much smaller portion of the
 * execution time than the actual scaling operations.
 * <p/>
 * If you are executing on a storage medium that is unexpectedly slow and I/O is
 * a considerable portion of the scaling operation (e.g. S3 or EBS volumes),
 * feel free to try using more threads than CPU cores to see if that helps; but
 * in most normal cases, it will only slow down all other parallel scaling
 * operations.
 * <p/>
 * As for memory, every time an image is scaled it is decoded into a
 * {@link BufferedImage} and stored in the JVM Heap space (decoded image
 * instances are always larger than the source images on-disk). For larger
 * images, that can use up quite a bit of memory. You will need to benchmark
 * your particular use-cases on your hardware to get an idea of where the sweet
 * spot is for this; if you are operating within tight memory bounds, you may
 * want to limit simultaneous scaling operations to 1 or 2 regardless of the
 * number of cores just to avoid having too many {@link BufferedImage} instances
 * in JVM Heap space at the same time.
 * <p/>
 * These are rough metrics and behaviors to give you an idea of how best to tune
 * this class for your deployment, but nothing can replacement writing a small
 * Java class that scales a handful of images in a number of different ways and
 * testing that directly on your deployment hardware.
 * <h3>Resource Overhead</h3>
 * The {@link ExecutorService} utilized by this class won't be initialized until
 * one of the operation methods are called, at which point the
 * <code>service</code> will be instantiated for the first time and operation
 * queued up.
 * <p/>
 * More specifically, if you have no need for asynchronous image processing
 * offered by this class, you don't need to worry about wasted resources or
 * hanging/idle threads as they will never be created if you never use this
 * class.
 * <h3>Cleaning up Service Threads</h3>
 * By default the {@link Thread}s created by the internal
 * {@link ThreadPoolExecutor} do not run in <code>daemon</code> mode; which
 * means they will block the host VM from exiting until they are explicitly shut
 * down.
 * <p/>
 * If you have used the {@link AsyncScalr} class and are trying to shut down a
 * client application, you will need to call {@link #getService()} then
 * {@link ExecutorService#shutdown()} or <code>shutdownNow()</code> to have the
 * threads terminated.
 * <p/>
 * Alternatively if you are deploying imgscalr and using the async functionality
 * in a client application and this seems like a pain, you can subclass this
 * class and provide a {@link ExecutorService} that uses a {@link ThreadFactory}
 * that creates {@link Thread}s who do run in <code>daemon</code> mode and they
 * will shut themselves down immediately (killing any in-progress work) when the
 * JVM exits.
 * <h3>Custom ExecutorService Implementations</h3>
 * By default this class uses a {@link ThreadPoolExecutor} to handle execution
 * of queued image operations. If a different {@link ExecutorService}
 * implementation is desired, this class can be easily subclasses and the
 * internal {@link #verifyService()} method overridden with custom logic to
 * verify (and if necessary) create a new executor that is assigned to the
 * internal variable <code>service</code>.
 * 
 * @author Riyad Kalla (software@thebuzzmedia.com)
 * @since 3.2
 */
@SuppressWarnings("javadoc")
public class AsyncScalr {
	/**
	 * Number of threads the internal {@link ExecutorService} will use to
	 * simultaneously execute scale requests.
	 * <p/>
	 * This value can be changed by setting the
	 * <code>imgscalr.async.threadCount</code> system property to a valid
	 * integer value &gt; 0.
	 * <p/>
	 * Default value is <code>2</code>.
	 */
	public static final int THREAD_COUNT = Integer.getInteger(
			"imgscalr.async.threadCount", 2);

	/**
	 * Initializer used to verify the THREAD_COUNT system property.
	 */
	static {
		if (THREAD_COUNT <= 0)
			throw new RuntimeException(
					"System property 'imgscalr.async.threadCount' set THREAD_COUNT to "
							+ THREAD_COUNT + ", but THREAD_COUNT must be > 0.");
	}

	protected static ExecutorService service;

	/**
	 * Used to get access to the internal {@link ExecutorService} used by this
	 * class to process scale operations.
	 * <p/>
	 * <strong>NOTE</strong>: You will need to explicitly shutdown any service
	 * currently set on this class before the host JVM exits.
	 * <p/>
	 * You can call {@link ExecutorService#shutdown()} to wait for all scaling
	 * operations to complete first or call
	 * {@link ExecutorService#shutdownNow()} to kill any in-process operations
	 * and purge all pending operations before exiting.
	 * <p/>
	 * Additionally you can use
	 * {@link ExecutorService#awaitTermination(long, TimeUnit)} after issuing a
	 * shutdown command to try and wait until the service has finished all
	 * tasks.
	 * 
	 * @return the current {@link ExecutorService} used by this class to process
	 *         scale operations.
	 */
	public static ExecutorService getService() {
		return service;
	}

	/**
	 * @see Scalr#apply(BufferedImage, BufferedImageOp...)
	 */
	public static Future<BufferedImage> apply(final BufferedImage src,
			final BufferedImageOp... ops) throws IllegalArgumentException,
			ImagingOpException {
		verifyService();

		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.apply(src, ops);
			}
		});
	}

	/**
	 * @see Scalr#crop(BufferedImage, int, int, BufferedImageOp...)
	 */
	public static Future<BufferedImage> crop(final BufferedImage src,
			final int width, final int height, final BufferedImageOp... ops)
			throws IllegalArgumentException, ImagingOpException {
		verifyService();

		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.crop(src, width, height, ops);
			}
		});
	}

	/**
	 * @see Scalr#crop(BufferedImage, int, int, int, int, BufferedImageOp...)
	 */
	public static Future<BufferedImage> crop(final BufferedImage src,
			final int x, final int y, final int width, final int height,
			final BufferedImageOp... ops) throws IllegalArgumentException,
			ImagingOpException {
		verifyService();

		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.crop(src, x, y, width, height, ops);
			}
		});
	}

	/**
	 * @see Scalr#pad(BufferedImage, int, Color, BufferedImageOp...)
	 */
	public static Future<BufferedImage> pad(final BufferedImage src,
			final int padding, final Color color, final BufferedImageOp... ops)
			throws IllegalArgumentException, ImagingOpException {
		verifyService();

		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.pad(src, padding, color, ops);
			}
		});
	}

	/**
	 * @see Scalr#resize(BufferedImage, int, BufferedImageOp...)
	 */
	public static Future<BufferedImage> resize(final BufferedImage src,
			final int targetSize, final BufferedImageOp... ops)
			throws IllegalArgumentException, ImagingOpException {
		verifyService();

		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, targetSize, ops);
			}
		});
	}

	/**
	 * @see Scalr#resize(BufferedImage, Method, int, BufferedImageOp...)
	 */
	public static Future<BufferedImage> resize(final BufferedImage src,
			final Method scalingMethod, final int targetSize,
			final BufferedImageOp... ops) throws IllegalArgumentException,
			ImagingOpException {
		verifyService();

		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, scalingMethod, targetSize, ops);
			}
		});
	}

	/**
	 * @see Scalr#resize(BufferedImage, Mode, int, BufferedImageOp...)
	 */
	public static Future<BufferedImage> resize(final BufferedImage src,
			final Mode resizeMode, final int targetSize,
			final BufferedImageOp... ops) throws IllegalArgumentException,
			ImagingOpException {
		verifyService();

		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, resizeMode, targetSize, ops);
			}
		});
	}

	/**
	 * @see Scalr#resize(BufferedImage, Method, Mode, int, BufferedImageOp...)
	 */
	public static Future<BufferedImage> resize(final BufferedImage src,
			final Method scalingMethod, final Mode resizeMode,
			final int targetSize, final BufferedImageOp... ops)
			throws IllegalArgumentException, ImagingOpException {
		verifyService();

		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, scalingMethod, resizeMode, targetSize,
						ops);
			}
		});
	}

	/**
	 * @see Scalr#resize(BufferedImage, int, int, BufferedImageOp...)
	 */
	public static Future<BufferedImage> resize(final BufferedImage src,
			final int targetWidth, final int targetHeight,
			final BufferedImageOp... ops) throws IllegalArgumentException,
			ImagingOpException {
		verifyService();

		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, targetWidth, targetHeight, ops);
			}
		});
	}

	/**
	 * @see Scalr#resize(BufferedImage, Method, int, int, BufferedImageOp...)
	 */
	public static Future<BufferedImage> resize(final BufferedImage src,
			final Method scalingMethod, final int targetWidth,
			final int targetHeight, final BufferedImageOp... ops) {
		verifyService();

		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, scalingMethod, targetWidth,
						targetHeight, ops);
			}
		});
	}

	/**
	 * @see Scalr#resize(BufferedImage, Mode, int, int, BufferedImageOp...)
	 */
	public static Future<BufferedImage> resize(final BufferedImage src,
			final Mode resizeMode, final int targetWidth,
			final int targetHeight, final BufferedImageOp... ops)
			throws IllegalArgumentException, ImagingOpException {
		verifyService();

		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, resizeMode, targetWidth, targetHeight,
						ops);
			}
		});
	}

	/**
	 * @see Scalr#resize(BufferedImage, Method, Mode, int, int,
	 *      BufferedImageOp...)
	 */
	public static Future<BufferedImage> resize(final BufferedImage src,
			final Method scalingMethod, final Mode resizeMode,
			final int targetWidth, final int targetHeight,
			final BufferedImageOp... ops) throws IllegalArgumentException,
			ImagingOpException {
		verifyService();

		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, scalingMethod, resizeMode,
						targetWidth, targetHeight, ops);
			}
		});
	}

	/**
	 * @see Scalr#rotate(BufferedImage, Rotation, BufferedImageOp...)
	 */
	public static Future<BufferedImage> rotate(final BufferedImage src,
			final Rotation rotation, final BufferedImageOp... ops)
			throws IllegalArgumentException, ImagingOpException {
		verifyService();

		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.rotate(src, rotation, ops);
			}
		});
	}

	/**
	 * Used to verify that the underlying <code>service</code> points at an
	 * active {@link ExecutorService} instance that can be used by this class.
	 * <p/>
	 * If <code>service</code> is <code>null</code>, has been shutdown or
	 * terminated then this method will replace it with a new
	 * {@link ThreadPoolExecutor} (using {@link #THREAD_COUNT} threads) by
	 * assigning a new value to the <code>service</code> member variable.
	 * <p/>
	 * Custom implementations need to assign a new {@link ExecutorService}
	 * instance to <code>service</code>, but it doesn't necessarily need to be
	 * an instance of {@link ThreadPoolExecutor}; they are free to use the
	 * executor of choice.
	 */
	protected static void verifyService() {
		if (service == null || service.isShutdown() || service.isTerminated()) {
			/*
			 * Assigning a new value will free the previous reference to service
			 * if it was non-null, allowing it to be GC'ed when it is done
			 * shutting down (assuming it hadn't already).
			 */
			service = Executors.newFixedThreadPool(THREAD_COUNT);
		}
	}
}