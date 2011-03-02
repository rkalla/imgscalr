/**   
 * Copyright 2010 The Buzz Media, LLC
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

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.thebuzzmedia.imgscalr.Scalr.Method;

public class ScalrTest extends AbstractTest {
	private static boolean SHOW_OUTPUT = true;

	private static int LANDSCAPE_IMAGE_WIDTH = 800;
	private static int LANDSCAPE_IMAGE_HEIGHT = 600;
	private static int LANDSCAPE_IMAGE_TYPE = BufferedImage.TYPE_INT_RGB;

	private static int PORTRAIT_IMAGE_WIDTH = LANDSCAPE_IMAGE_HEIGHT;
	private static int PORTRAIT_IMAGE_HEIGHT = LANDSCAPE_IMAGE_WIDTH;
	private static int PORTRAIT_IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;

	private static int LANDSCAPE_TARGET_WIDTH = 320;
	private static int LANDSCAPE_TARGET_HEIGHT = 240;

	private static int PORTRAIT_TARGET_WIDTH = LANDSCAPE_TARGET_HEIGHT;
	private static int PORTRAIT_TARGET_HEIGHT = LANDSCAPE_TARGET_WIDTH;

	private BufferedImage landscapeImage;
	private BufferedImage portraitImage;

	@Before
	public void setup() {
		landscapeImage = new BufferedImage(LANDSCAPE_IMAGE_WIDTH,
				LANDSCAPE_IMAGE_HEIGHT, LANDSCAPE_IMAGE_TYPE);
		portraitImage = new BufferedImage(PORTRAIT_IMAGE_WIDTH,
				PORTRAIT_IMAGE_HEIGHT, PORTRAIT_IMAGE_TYPE);
	}

	@After
	public void after() {
		landscapeImage.flush();
		portraitImage.flush();
	}

	/*
	 * ========================================================================
	 * METHOD TESTS
	 * ========================================================================
	 */

	/**
	 * Used to test the {@link Scalr#resize(java.awt.image.BufferedImage, int)}
	 * method.
	 */
	@Test
	public void testResizeBILandscape() {
		BufferedImage result = Scalr.resize(landscapeImage,
				LANDSCAPE_TARGET_WIDTH);

		if (SHOW_OUTPUT)
			System.out.println("testResizeBILandscape: " + result.getWidth()
					+ "x" + result.getHeight() + " Type: " + result.getType());

		assertNotNull(result);
		assertEquals(LANDSCAPE_IMAGE_TYPE, result.getType());
		assertEquals(LANDSCAPE_TARGET_WIDTH, result.getWidth());
		assertEquals(LANDSCAPE_TARGET_HEIGHT, result.getHeight());
	}

	/**
	 * Used to test the {@link Scalr#resize(java.awt.image.BufferedImage, int)}
	 * method.
	 */
	@Test
	public void testResizeBIPortrait() {
		BufferedImage result = Scalr.resize(portraitImage,
				PORTRAIT_TARGET_HEIGHT);

		if (SHOW_OUTPUT)
			System.out.println("testResizeBIPortrait: " + result.getWidth()
					+ "x" + result.getHeight() + " Type: " + result.getType());

		assertNotNull(result);
		assertEquals(PORTRAIT_IMAGE_TYPE, result.getType());
		assertEquals(PORTRAIT_TARGET_WIDTH, result.getWidth());
		assertEquals(PORTRAIT_TARGET_HEIGHT, result.getHeight());
	}

	/**
	 * Used to test the
	 * {@link Scalr#resize(java.awt.image.BufferedImage, int, int)} method.
	 */
	@Test
	public void testResizeBIILandscape() {
		BufferedImage result = Scalr.resize(landscapeImage,
				LANDSCAPE_TARGET_WIDTH, LANDSCAPE_TARGET_HEIGHT);

		if (SHOW_OUTPUT)
			System.out.println("testResizeBIILandscape: " + result.getWidth()
					+ "x" + result.getHeight() + " Type: " + result.getType());

		assertNotNull(result);
		assertEquals(LANDSCAPE_IMAGE_TYPE, result.getType());
		assertEquals(LANDSCAPE_TARGET_WIDTH, result.getWidth());
		assertEquals(LANDSCAPE_TARGET_HEIGHT, result.getHeight());
	}

	/**
	 * Used to test the
	 * {@link Scalr#resize(java.awt.image.BufferedImage, int, int)} method.
	 */
	@Test
	public void testResizeBIIPortrait() {
		BufferedImage result = Scalr.resize(portraitImage,
				PORTRAIT_TARGET_WIDTH, PORTRAIT_TARGET_HEIGHT);

		if (SHOW_OUTPUT)
			System.out.println("testResizeBIIPortrait: " + result.getWidth()
					+ "x" + result.getHeight() + " Type: " + result.getType());

		assertNotNull(result);
		assertEquals(PORTRAIT_IMAGE_TYPE, result.getType());
		assertEquals(PORTRAIT_TARGET_WIDTH, result.getWidth());
		assertEquals(PORTRAIT_TARGET_HEIGHT, result.getHeight());
	}

	/**
	 * Used to test the
	 * {@link Scalr#resize(java.awt.image.BufferedImage, com.thebuzzmedia.imgscalr.Scalr.Method, int)}
	 * method.
	 */
	@Test
	public void testResizeBMI() {
		BufferedImage result = Scalr.resize(portraitImage, Method.SPEED,
				PORTRAIT_TARGET_HEIGHT);

		if (SHOW_OUTPUT)
			System.out.println("testResizeBMI: " + result.getWidth() + "x"
					+ result.getHeight() + " Type: " + result.getType());

		assertNotNull(result);
		assertEquals(PORTRAIT_IMAGE_TYPE, result.getType());
		assertEquals(PORTRAIT_TARGET_WIDTH, result.getWidth());
		assertEquals(PORTRAIT_TARGET_HEIGHT, result.getHeight());
	}

	/**
	 * Used to test the
	 * {@link Scalr#resize(java.awt.image.BufferedImage, com.thebuzzmedia.imgscalr.Scalr.Method, int, int)}
	 * method.
	 */
	@Test
	public void testResizeBMII() {
		BufferedImage result = Scalr.resize(portraitImage, Method.SPEED,
				PORTRAIT_TARGET_WIDTH, PORTRAIT_TARGET_HEIGHT);

		if (SHOW_OUTPUT)
			System.out.println("testResizeBMII: " + result.getWidth() + "x"
					+ result.getHeight() + " Type: " + result.getType());

		assertNotNull(result);
		assertEquals(PORTRAIT_IMAGE_TYPE, result.getType());
		assertEquals(PORTRAIT_TARGET_WIDTH, result.getWidth());
		assertEquals(PORTRAIT_TARGET_HEIGHT, result.getHeight());
	}

	/*
	 * ========================================================================
	 * COMMON SENSE TESTS
	 * ========================================================================
	 */

	/**
	 * "Common Sense" Test: Resize a landscape image (800x600) to 200x200. The
	 * primary dimension is width because this is a landscape image; height
	 * should be ignored.
	 * <p/>
	 * Expected Result: An image sized 200x150
	 */
	@Test
	public void testLandscapeResizeProportional() {
		BufferedImage result = Scalr.resize(landscapeImage, 200);

		if (SHOW_OUTPUT)
			System.out.println("testLandscapeResizeProportional: "
					+ result.getWidth() + "x" + result.getHeight());

		assertNotNull(result);
		assertEquals(200, result.getWidth());
		assertEquals(150, result.getHeight());
	}

	/**
	 * "Common Sense" Test: Resize a landscape image (800x600) to 200x10. The
	 * primary dimension is width because this is a landscape image; height
	 * should be ignored.
	 * <p/>
	 * Expected Result: An image sized 200x150
	 */
	@Test
	public void testLandscapeResizeUnproportional() {
		BufferedImage result = Scalr.resize(landscapeImage, 200, 10);

		if (SHOW_OUTPUT)
			System.out.println("testLandscapeResizeUnproportional: "
					+ result.getWidth() + "x" + result.getHeight());

		assertNotNull(result);
		assertEquals(200, result.getWidth());
		assertEquals(150, result.getHeight());
	}

	/**
	 * "Common Sense" Test: Resize a portrait image (600x800) to 200x200. The
	 * primary dimension is height because this is a portrait image; width
	 * should be ignored.
	 * <p/>
	 * Expected Result: An image sized 150x200
	 */
	@Test
	public void testPortraitResizeProportional() {
		BufferedImage result = Scalr.resize(portraitImage, 200);

		if (SHOW_OUTPUT)
			System.out.println("testPortraitResizeProportional: "
					+ result.getWidth() + "x" + result.getHeight());

		assertNotNull(result);
		assertEquals(150, result.getWidth());
		assertEquals(200, result.getHeight());
	}

	/**
	 * "Common Sense" Test: Resize a portrait image (600x800) to 10x200. The
	 * primary dimension is height because this is a portrait image; width
	 * should be ignored.
	 * <p/>
	 * Expected Result: An image sized 150x200
	 */
	@Test
	public void testPortraitResizeUnproportional() {
		BufferedImage result = Scalr.resize(portraitImage, 10, 200);

		if (SHOW_OUTPUT)
			System.out.println("testPortraitResizeUnproportional: "
					+ result.getWidth() + "x" + result.getHeight());

		assertNotNull(result);
		assertEquals(150, result.getWidth());
		assertEquals(200, result.getHeight());
	}

	/**
	 * Resize a portrait (600x800) down to a super-tiny size that would require
	 * a sub-pixel result (600 down to 7.5), ensure that the fractional pixel
	 * value is cropped and not scaled up.
	 */
	@Test
	public void testPortraitResizeUnproportionalTiny() {
		BufferedImage result = Scalr.resize(portraitImage, 10);

		if (SHOW_OUTPUT)
			System.out.println("testPortraitResizeUnproportional2: "
					+ result.getWidth() + "x" + result.getHeight());

		assertNotNull(result);
		assertEquals(8, result.getWidth());
		assertEquals(10, result.getHeight());
	}

	@Test
	public void testModeAuto() {
		BufferedImage result = Scalr.resize(landscapeImage,
				Scalr.Mode.AUTOMATIC, LANDSCAPE_TARGET_WIDTH,
				LANDSCAPE_TARGET_HEIGHT);

		if (SHOW_OUTPUT)
			System.out.println("testModeAuto: " + result.getWidth() + "x"
					+ result.getHeight());

		assertNotNull(result);
		assertEquals(LANDSCAPE_TARGET_WIDTH, result.getWidth());
		assertEquals(LANDSCAPE_TARGET_HEIGHT, result.getHeight());
	}

	@Test
	public void testModeHeight() {
		BufferedImage result = Scalr.resize(landscapeImage,
				Scalr.Mode.FIT_TO_HEIGHT, LANDSCAPE_TARGET_WIDTH);

		if (SHOW_OUTPUT)
			System.out.println("testModeHeight: " + result.getWidth() + "x"
					+ result.getHeight());

		assertNotNull(result);
		assertEquals(427, result.getWidth());
		assertEquals(LANDSCAPE_TARGET_WIDTH, result.getHeight());
	}
	
	@Test
	public void testModeWidth() {
		BufferedImage result = Scalr.resize(portraitImage,
				Scalr.Mode.FIT_TO_WIDTH, PORTRAIT_TARGET_HEIGHT);

		if (SHOW_OUTPUT)
			System.out.println("testModeWidth: " + result.getWidth() + "x"
					+ result.getHeight());

		assertNotNull(result);
		assertEquals(PORTRAIT_TARGET_HEIGHT, result.getWidth());
		assertEquals(427, result.getHeight());
	}
}