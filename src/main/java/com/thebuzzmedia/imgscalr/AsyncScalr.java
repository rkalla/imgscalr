package com.thebuzzmedia.imgscalr;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.thebuzzmedia.imgscalr.Scalr.Method;
import com.thebuzzmedia.imgscalr.Scalr.Mode;
import com.thebuzzmedia.imgscalr.Scalr.Rotation;

public class AsyncScalr {
	private static ExecutorService service = Executors.newFixedThreadPool(4);

	/*
	 * Configure the ThreadPool used to service the Executor.
	 * 
	 * Mirrors Scalr, returns all Future<BufferedImage> results.
	 */

	public static Future<BufferedImage> resize(final BufferedImage src,
			final int targetSize, final BufferedImageOp... ops)
			throws IllegalArgumentException {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, targetSize, ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final Rotation rotation, final int targetSize,
			final BufferedImageOp... ops) throws IllegalArgumentException {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, rotation, targetSize, ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final Method scalingMethod, final int targetSize,
			final BufferedImageOp... ops) throws IllegalArgumentException {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, scalingMethod, targetSize, ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final Method scalingMethod, final Rotation rotation,
			final int targetSize, final BufferedImageOp... ops)
			throws IllegalArgumentException {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, scalingMethod, rotation, targetSize,
						ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final Mode resizeMode, final int targetSize,
			final BufferedImageOp... ops) throws IllegalArgumentException {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, resizeMode, targetSize, ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final Mode resizeMode, final Rotation rotation,
			final int targetSize, final BufferedImageOp... ops)
			throws IllegalArgumentException {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, resizeMode, rotation, targetSize, ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final Method scalingMethod, final Mode resizeMode,
			final int targetSize, final BufferedImageOp... ops)
			throws IllegalArgumentException {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, scalingMethod, resizeMode, targetSize,
						ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final Method scalingMethod, final Mode resizeMode,
			final Rotation rotation, final int targetSize,
			final BufferedImageOp... ops) throws IllegalArgumentException {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, scalingMethod, resizeMode, rotation,
						targetSize, ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final int targetWidth, final int targetHeight,
			final BufferedImageOp... ops) throws IllegalArgumentException {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, targetWidth, targetHeight, ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final Rotation rotation, final int targetWidth,
			final int targetHeight, final BufferedImageOp... ops)
			throws IllegalArgumentException {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, rotation, targetWidth, targetHeight,
						ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final Method scalingMethod, final int targetWidth,
			final int targetHeight, final BufferedImageOp... ops) {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, scalingMethod, targetWidth,
						targetHeight, ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final Method scalingMethod, final Rotation rotation,
			final int targetWidth, final int targetHeight,
			final BufferedImageOp... ops) {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, scalingMethod, rotation, targetWidth,
						targetHeight, ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final Mode resizeMode, final int targetWidth,
			final int targetHeight, final BufferedImageOp... ops)
			throws IllegalArgumentException {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, resizeMode, targetWidth, targetHeight,
						ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final Mode resizeMode, final Rotation rotation,
			final int targetWidth, final int targetHeight,
			final BufferedImageOp... ops) throws IllegalArgumentException {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, resizeMode, rotation, targetWidth,
						targetHeight, ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final Method scalingMethod, final Mode resizeMode,
			final int targetWidth, final int targetHeight,
			final BufferedImageOp... ops) throws IllegalArgumentException {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, scalingMethod, resizeMode,
						targetWidth, targetHeight, ops);
			}
		});
	}

	public static Future<BufferedImage> resize(final BufferedImage src,
			final Method scalingMethod, final Mode resizeMode,
			final Rotation rotation, final int targetWidth,
			final int targetHeight, final BufferedImageOp... ops)
			throws IllegalArgumentException {
		return service.submit(new Callable<BufferedImage>() {
			public BufferedImage call() throws Exception {
				return Scalr.resize(src, scalingMethod, resizeMode, rotation,
						targetWidth, targetHeight, ops);
			}
		});
	}
}