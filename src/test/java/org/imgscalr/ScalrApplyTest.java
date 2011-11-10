package org.imgscalr;

import static org.imgscalr.Scalr.OP_ANTIALIAS;
import static org.imgscalr.Scalr.OP_BRIGHTER;
import static org.imgscalr.Scalr.OP_DARKER;
import static org.imgscalr.Scalr.OP_GRAYSCALE;
import static org.imgscalr.Scalr.apply;

import java.awt.image.BufferedImageOp;

import org.junit.Assert;
import org.junit.Test;

public class ScalrApplyTest extends AbstractScalrTest {
	@Test
	public void testApplyEX() {
		try {
			apply(src, (BufferedImageOp[]) null);
			Assert.assertTrue(false);
		} catch (Exception e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void testApply1() {
		assertEquals(load("time-square-apply-1.png"), apply(src, OP_ANTIALIAS));
	}

	@Test
	public void testApply4() {
		assertEquals(
				load("time-square-apply-4.png"),
				apply(src, Scalr.OP_ANTIALIAS, OP_BRIGHTER, OP_DARKER,
						OP_GRAYSCALE));
	}
}