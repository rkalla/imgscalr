package com.thebuzzmedia.imgscalr;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

public class ScalrTest {
	public static final Dimension LANDSCAPE_DIMENSION = new Dimension(1600, 1200);
	public static final Dimension LANDSCAPE_DOWNSIZED_DIMENSION = new Dimension(320, 240);
	public static final Dimension LANDSCAPE_UPSIZED_DIMENSION = new Dimension(2560, 1600);

	public static final Dimension PORTRAIT_DIMENSION = new Dimension(1200, 1600);
	public static final Dimension PORTRAIT_DOWNSIZED_DIMENSION = new Dimension(240, 320);
	public static final Dimension PORTRAIT_UPSIZED_DIMENSION = new Dimension(1600, 2650);

	public static final Dimension SQUARE_DIMENSION = new Dimension(1600, 1600);
	public static final Dimension SQUARE_DOWNSIZED_DIMENSION = new Dimension(320, 320);
	public static final Dimension SQUARE_UPSIZED_DIMENSION = new Dimension(2560, 2560);

	public static final float LANDSCAPE_RATIO = (float) LANDSCAPE_DIMENSION.height / (float) LANDSCAPE_DIMENSION
			.width;
	public static final float PORTRAIT_RATIO = (float) PORTRAIT_DIMENSION.height / (float) PORTRAIT_DIMENSION.width;
	public static final float SQUARE_RATIO = (float) SQUARE_DIMENSION.height / (float) SQUARE_DIMENSION.width;

	public static final BufferedImage LANDSCAPE_IMAGE = new BufferedImage(LANDSCAPE_DIMENSION.width,
			LANDSCAPE_DIMENSION.height, BufferedImage.TYPE_INT_RGB);
	public static final BufferedImage PORTRAIT_IMAGE = new BufferedImage(PORTRAIT_DIMENSION.width,
			PORTRAIT_DIMENSION.height, BufferedImage.TYPE_INT_RGB);
	public static final BufferedImage SQUARE_IMAGE = new BufferedImage(SQUARE_DIMENSION.width,
			SQUARE_DIMENSION.height, BufferedImage.TYPE_INT_RGB);

	//TODO: Add tests where width or height is more restrictive (e.g. 1600x200) and ensure the lowest-dimension is used to cap the resize op.

//	@Test
//	public void downsizeLandscape() {
//		BufferedImage image = Scalr.resize(LANDSCAPE_IMAGE, LANDSCAPE_DOWNSIZED_DIMENSION.width,
//				LANDSCAPE_DOWNSIZED_DIMENSION.height, null, false);
//
//		assertNotNull(image);
//		assertEquals(LANDSCAPE_IMAGE.getType(), image.getType());
//		assertEquals(LANDSCAPE_DOWNSIZED_DIMENSION.width, image.getWidth());
//		assertEquals(LANDSCAPE_DOWNSIZED_DIMENSION.height, image.getHeight());
//	}
//
//	@Test
//	public void upsizeLandscape() {
//		BufferedImage image = Scalr.resize(LANDSCAPE_IMAGE, LANDSCAPE_UPSIZED_DIMENSION.width,
//				LANDSCAPE_UPSIZED_DIMENSION.height, null, false);
//
//		assertNotNull(image);
//		assertEquals(LANDSCAPE_IMAGE.getType(), image.getType());
//		assertEquals(LANDSCAPE_UPSIZED_DIMENSION.width, image.getWidth());
//		assertEquals(LANDSCAPE_UPSIZED_DIMENSION.height, image.getHeight());
//	}
//
//	@Test
//	public void downsizePortrait() {
//		BufferedImage image = Scalr.resize(PORTRAIT_IMAGE, PORTRAIT_DOWNSIZED_DIMENSION.width,
//				PORTRAIT_DOWNSIZED_DIMENSION.height, null, false);
//
//		assertNotNull(image);
//		assertEquals(PORTRAIT_IMAGE.getType(), image.getType());
//		assertEquals(PORTRAIT_DOWNSIZED_DIMENSION.width, image.getWidth());
//		assertEquals(PORTRAIT_DOWNSIZED_DIMENSION.height, image.getHeight());
//	}
//
//	@Test
//	public void upsizePortrait() {
//		BufferedImage image = Scalr.resize(PORTRAIT_IMAGE, PORTRAIT_UPSIZED_DIMENSION.width,
//				PORTRAIT_UPSIZED_DIMENSION.height, null, false);
//
//		assertNotNull(image);
//		assertEquals(PORTRAIT_IMAGE.getType(), image.getType());
//		assertEquals(PORTRAIT_UPSIZED_DIMENSION.width, image.getWidth());
//		assertEquals(PORTRAIT_UPSIZED_DIMENSION.height, image.getHeight());
//	}
//
//	@Test
//	public void performanceTest() throws IOException {
//		for(int i = 0; i < 10; i++) {
//			BufferedImage image = ImageIO.read(ScalrTest.class.getResourceAsStream("/landscape-huge.jpg"));
//			long startTime = System.currentTimeMillis();
//
//			Scalr.resize(image, 1024, 200, null, false);
//
//			System.err.println("Elapse Time: " + (System.currentTimeMillis() - startTime) + "ms");
//			image.flush();
//		}
//
//		System.out.println("\n\n");
//
//		for(int i = 0; i < 10; i++) {
//			BufferedImage image = ImageIO.read(ScalrTest.class.getResourceAsStream("/landscape-huge.jpg"));
//			long startTime = System.currentTimeMillis();
//
////			Scalr.getScaledInstance(image, 1024, 200, Scalr.DEFAULT_INTERPOLATION_METHOD, true);
//
//			System.err.println("Elapse Time: " + (System.currentTimeMillis() - startTime) + "ms");
//			image.flush();
//		}
//	}

	
}
