package com.thebuzzmedia.imgscalr;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ScalrBenchmark {
	public static final int CYCLES = 1;

	public static final Dimension DOWNSIZE = new Dimension(800, 800);
	public static final Dimension UPSIZE = new Dimension(2560, 1600);

	public static BufferedImage loadLandscapeHuge() {
		BufferedImage image = null;

		try {
			image = ImageIO.read(ScalrBenchmark.class.getResourceAsStream("/landscape-huge-2.jpg"));
		} catch (IOException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}

		return image;
	}

	public static BufferedImage loadPortraitLarge() {
		BufferedImage image = null;

		try {
			image = ImageIO.read(ScalrBenchmark.class.getResourceAsStream("/portrait-large.jpg"));
		} catch (IOException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}

		return image;
	}

	public static BufferedImage loadPortraitHuge() {
		BufferedImage image = null;

		try {
			image = ImageIO.read(ScalrBenchmark.class.getResourceAsStream("/portrait-huge.jpg"));
		} catch (IOException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}

		return image;
	}

	private static final BufferedImage LANDSCAPE_HUGE = loadLandscapeHuge();

	private static final BufferedImage PORTRAIT_LARGE = loadPortraitLarge();
	private static final BufferedImage PORTRAIT_HUGE = loadPortraitHuge();

//	@Test
//	public void generateComparison() throws IOException {
//		BufferedImage result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.SPEED, 75, 75, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-75x75-fastest.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.BALANCED, 75, 75, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-75x75-balanced.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.QUALITY, 75, 75, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-75x75-quality.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.SPEED, 150, 150, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-150x150-fastest.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.BALANCED, 150, 150, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-150x150-balanced.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.QUALITY, 150, 150, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-150x150-quality.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.SPEED, 300, 300, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-300x300-fastest.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.BALANCED, 300, 300, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-300x300-balanced.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.QUALITY, 300, 300, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-300x300-quality.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.SPEED, 600, 600, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-600x600-fastest.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.BALANCED, 600, 600, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-600x600-balanced.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.QUALITY, 600, 600, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-600x600-quality.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.SPEED, 900, 900, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-900x900-fastest.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.BALANCED, 900, 900, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-900x900-balanced.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.QUALITY, 900, 900, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-900x900-quality.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.SPEED, 1400, 1400, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-1400x1400-fastest.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.BALANCED, 1400, 1400, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-1400x1400-balanced.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.QUALITY, 1400, 1400, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-1400x1400-quality.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.SPEED, 2500, 2500, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-2500x2500-fastest.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.BALANCED, 2500, 2500, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-2500x2500-balanced.jpg"));
//		result.flush();
//
//		result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.QUALITY, 2500, 2500, false, true);
//		ImageIO.write(result, "JPEG", new File("compare-2500x2500-quality.jpg"));
//		result.flush();
//	}

	@Test
	public void performanceMethodDownsizeHugeBenchmark() throws IOException {
		System.out.println("\nBenchmark: Downsizing Huge, Method: SPEED");

		for (int i = 0; i < CYCLES; i++) {
			BufferedImage result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.SPEED, DOWNSIZE.width,
					DOWNSIZE.height, false, true);
			ImageIO.write(result, "PNG", new File("downsize-huge-performance-" + i + ".png"));
			result.flush();
		}
	}

	@Test
	public void qualityMethodDownsizeHugeBenchmark() throws IOException {
		System.out.println("\nBenchmark: Downsizing Huge, Method: QUALITY");

		for (int i = 0; i < CYCLES; i++) {
			BufferedImage result = Scalr.resize(LANDSCAPE_HUGE, Scalr.Method.QUALITY, DOWNSIZE.width,
					DOWNSIZE.height, false, true);
			ImageIO.write(result, "PNG", new File("downsize-huge-quality-" + i + ".png"));
			result.flush();
		}

	}

	@Test
	public void performanceMethodUpsizeLargeBenchmark() throws IOException {
		System.out.println("\nBenchmark: Upsizing Large, Method: SPEED");

		for (int i = 0; i < CYCLES; i++) {
			BufferedImage result = Scalr.resize(PORTRAIT_LARGE, Scalr.Method.SPEED, UPSIZE.width,
					UPSIZE.height, false, true);
			ImageIO.write(result, "PNG", new File("upsize-large-performance-" + i + ".png"));
			result.flush();
		}
	}

	@Test
	public void qualityMethodUpsizeLargeBenchmark() throws IOException {
		System.out.println("\nBenchmark: Upsizing Large, Method: QUALITY");

		for (int i = 0; i < CYCLES; i++) {
			BufferedImage result = Scalr.resize(PORTRAIT_LARGE, Scalr.Method.QUALITY, UPSIZE.width,
					UPSIZE.height, false, true);
			ImageIO.write(result, "PNG", new File("upsize-large-quality-" + i + ".png"));
			result.flush();
		}

	}
}
