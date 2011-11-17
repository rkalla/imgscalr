package org.imgscalr;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;

public class ScalrPadTest extends AbstractScalrTest {
	// An absurdly bright color to easily/visually check for alpha channel.
	static int pad = 8;
	static Color alpha = new Color(255, 50, 255, 0);

	@Test
	public void testPadEX() {
		try {
			Scalr.pad(src, -1);
			Assert.assertTrue(false);
		} catch (IllegalArgumentException ex) {
			Assert.assertTrue(true);
		}

		try {
			Scalr.pad(src, 0);
			Assert.assertTrue(false);
		} catch (IllegalArgumentException ex) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void testPad() {
		assertEquals(load("time-square-pad-8.png"), Scalr.pad(src, pad));
	}

	@Test
	public void testPadColor() {
		assertEquals(load("time-square-pad-8-red.png"),
				Scalr.pad(src, pad, Color.RED));
	}

	@Test
	public void testPadAlpha() {
		assertEquals(load("time-square-pad-8-alpha.png"),
				Scalr.pad(src, pad, alpha));
	}
}