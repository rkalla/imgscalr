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

	@Test
	public void testPadAlphaOps() {
		assertEquals(load("time-square-pad-8-alpha-ops.png"),
				Scalr.pad(src, pad, alpha, Scalr.OP_GRAYSCALE));
	}
}
