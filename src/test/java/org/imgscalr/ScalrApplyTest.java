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
