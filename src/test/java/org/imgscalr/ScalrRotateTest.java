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
