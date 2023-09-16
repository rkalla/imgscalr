/**   
 * Copyright 2011 Riyad Kalla
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
package org.imgscalr;

import java.awt.image.BufferedImage;

import junit.framework.Assert;

import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;
import org.junit.Test;

public class ScalrResizeTest extends AbstractScalrTest {
	@Test
	public void testResizeEX() {
		try {
			Scalr.resize(src, -1);
			Assert.assertTrue(false);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}

		try {
			Scalr.resize(src, 240, -1);
			Assert.assertTrue(false);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}

		try {
			Scalr.resize(src, (Method) null, 240);
			Assert.assertTrue(false);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}

		try {
			Scalr.resize(src, (Mode) null, 240);
			Assert.assertTrue(false);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}

		try {
			Scalr.resize(src, (Method) null, 240, 240);
			Assert.assertTrue(false);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}

		try {
			Scalr.resize(src, (Mode) null, 240, 240);
			Assert.assertTrue(false);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}

		try {
			Scalr.resize(src, null, null, 240);
			Assert.assertTrue(false);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}

		try {
			Scalr.resize(src, null, null, 240, 240);
			Assert.assertTrue(false);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void testResizeSize() {
		assertEquals(load("time-square-resize-320.png"), Scalr.resize(src, 320));
	}

	@Test
	public void testResizeWH() {
		assertEquals(load("time-square-resize-640x480.png"),
				Scalr.resize(src, 640, 480));
	}

	@Test
	public void testResizeSizeSpeed() {
		assertEquals(load("time-square-resize-320-speed.png"),
				Scalr.resize(src, Method.SPEED, 320));
	}

	@Test
	public void testResizeWHSpeed() {
		assertEquals(load("time-square-resize-640x480-speed.png"),
				Scalr.resize(src, Method.SPEED, 640, 480));
	}

	@Test
	public void testResizeSizeExact() {
		System.setProperty(Scalr.DEBUG_PROPERTY_NAME, "true");
		assertEquals(load("time-square-resize-320-fit-exact.png"),
				Scalr.resize(src, Mode.FIT_EXACT, 320));
	}

	@Test
	public void testResizeWHExact() {
		assertEquals(load("time-square-resize-640x640-fit-exact.png"),
				Scalr.resize(src, Mode.FIT_EXACT, 640, 640));
	}

	@Test
	public void testResizeSizeSpeedExact() {
		assertEquals(load("time-square-resize-320-speed-fit-exact.png"),
				Scalr.resize(src, Method.SPEED, Mode.FIT_EXACT, 320));
	}

	@Test
	public void testResizeWHSpeedExact() {
		assertEquals(load("time-square-resize-640x640-speed-fit-exact.png"),
				Scalr.resize(src, Method.SPEED, Mode.FIT_EXACT, 640, 640));
	}

	@Test
	public void testResizeWHSpeedExactOps() {
		assertEquals(
				load("time-square-resize-640x640-speed-fit-exact-ops.png"),
				Scalr.resize(src, Method.SPEED, Mode.FIT_EXACT, 640, 640,
						Scalr.OP_GRAYSCALE));
	}

	@Test
	public void testResizeUltraQuality() {
		System.setProperty(Scalr.DEBUG_PROPERTY_NAME, "true");
		BufferedImage i = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
		Scalr.resize(i, Method.ULTRA_QUALITY, 1);

		// This test is really about having scaling to tiny sizes not looping
		// forever because of the fractional step-down calculation bottoming
		// out.
		Assert.assertTrue(true);
	}
	
	@Test
	public void testResizeFitExact() {
		BufferedImage i = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
		BufferedImage i2 = Scalr.resize(i, Mode.FIT_EXACT, 500, 250);
		
		Assert.assertEquals(i2.getWidth(), 500);
		Assert.assertEquals(i2.getHeight(), 250);
	}

    @Test
    public void testResizeAutoVsFitBoth() {
        // FitBoth will not allow the minor axis to grow beyond the specified box. The four commented
        // tests show how this interacts

        // For landscape images, AUTO will let targetWidth decide scaling, even if targetHeight is violated
        BufferedImage landscape = new BufferedImage(500, 250, BufferedImage.TYPE_INT_RGB);
        testResizeAutoVsBoth(landscape, 500, 250, 500, 250, 500, 250);
        testResizeAutoVsBoth(landscape, 500, 500, 500, 250, 500, 250);

        testResizeAutoVsBoth(landscape, 800, 300, 800, 400, 600, 300);  // FitBoth restricts y to 300, and adjusts x
        testResizeAutoVsBoth(landscape, 800, 400, 800, 400, 800, 400);
        testResizeAutoVsBoth(landscape, 800, 500, 800, 400, 800, 400);

        testResizeAutoVsBoth(landscape, 250, 150, 250, 125, 250, 125);
        testResizeAutoVsBoth(landscape, 250, 125, 250, 125, 250, 125);
        testResizeAutoVsBoth(landscape, 250, 100, 250, 125, 200, 100);  // FitBoth imposes smaller y, and adjusts x

        // For portrait images, AUTO will let targetHeight decide scaling, even if targetWidth is violated
        BufferedImage portrait = new BufferedImage(250, 500, BufferedImage.TYPE_INT_RGB);
        testResizeAutoVsBoth(portrait, 250, 500, 250, 500, 250, 500);
        testResizeAutoVsBoth(portrait, 500, 500, 250, 500, 250, 500);

        testResizeAutoVsBoth(portrait, 300, 800, 400, 800, 300, 600);   // FitBoth restricts x to 800, and adjusts y
        testResizeAutoVsBoth(portrait, 400, 800, 400, 800, 400, 800);
        testResizeAutoVsBoth(portrait, 500, 800, 400, 800, 400, 800);

        testResizeAutoVsBoth(portrait, 150, 250, 125, 250, 125, 250);
        testResizeAutoVsBoth(portrait, 125, 250, 125, 250, 125, 250);
        testResizeAutoVsBoth(portrait, 100, 250, 125, 250, 100, 200);   // FitBoth imposes smaller xj, and adjusts y

        // Squares are treated as a landscape
        BufferedImage square = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        testResizeAutoVsBoth(square, 500, 500, 500, 500, 500, 500);
        testResizeAutoVsBoth(square, 800, 800, 800, 800, 800, 800);
        testResizeAutoVsBoth(square, 400, 400, 400, 400, 400, 400);
        testResizeAutoVsBoth(square, 800, 600, 800, 800, 600, 600);     // FixBoth restricts both dimensions

    }

    // resize to (w,h) using AUTO and FIT_BOTH modes, then compare auto (w,h) and fitBoth (w,h)
    private void testResizeAutoVsBoth (BufferedImage i, int targetWidth, int targetHeight, int autoWidth, int autoHeight, int fitBothWidth, int fitBothHeight) {
        BufferedImage auto = Scalr.resize(i, Mode.AUTOMATIC, targetWidth, targetHeight);
        BufferedImage fitBoth = Scalr.resize(i, Mode.BEST_FIT_BOTH, targetWidth, targetHeight);

        Assert.assertEquals (autoWidth, auto.getWidth());
        Assert.assertEquals(autoHeight, auto.getHeight());

        Assert.assertEquals(fitBothWidth, fitBoth.getWidth());
        Assert.assertEquals(fitBothHeight, fitBoth.getHeight());
    }
}
