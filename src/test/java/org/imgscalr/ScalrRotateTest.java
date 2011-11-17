package org.imgscalr;

import org.imgscalr.Scalr.Rotation;
import org.junit.Assert;
import org.junit.Test;

public class ScalrRotateTest extends AbstractScalrTest {
	@Test
	public void testRotateEX() {
		try {
			Scalr.rotate(src, null);
			Assert.assertTrue(false);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void testRotate90() {
		assertEquals(load("time-square-rotate-90.png"),
				Scalr.rotate(load("time-square.png"), Rotation.CW_90));
	}

	@Test
	public void testRotate180() {
		assertEquals(load("time-square-rotate-180.png"),
				Scalr.rotate(load("time-square.png"), Rotation.CW_180));
	}

	@Test
	public void testRotate270() {
		assertEquals(load("time-square-rotate-270.png"),
				Scalr.rotate(load("time-square.png"), Rotation.CW_270));
	}

	@Test
	public void testRotateFlipH() {
		assertEquals(load("time-square-rotate-horz.png"),
				Scalr.rotate(load("time-square.png"), Rotation.FLIP_HORZ));
	}

	@Test
	public void testRotateFlipV() {
		assertEquals(load("time-square-rotate-vert.png"),
				Scalr.rotate(load("time-square.png"), Rotation.FLIP_VERT));
	}

	@Test
	public void testRotateFlipHOps() {
		assertEquals(load("time-square-rotate-horz-ops.png"),
				Scalr.rotate(load("time-square.png"), Rotation.FLIP_HORZ,
						Scalr.OP_GRAYSCALE));
	}
}