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

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.thebuzzmedia.imgscalr.Scalr.Method;

/**
 * Easy class used to generate comparison images using the different landscape
 * and portrait examples found in /src/test/resources for easy eye-ball
 * comparisons of different scaling methods.
 * <p>
 * Images generated from this class are in the format: (image
 * name)-(size)-(method).(extension)
 * <p>
 * An example would be: screenshot-240-SPEED.png
 * <p>
 * What is so nice about this format is that the images of the same size of
 * varying qualities all list right next to each other in a directory listing
 * (all 240px images are next to each other, all 480px are next to each other
 * and so on) making quick visual comparisons of the photos easier.
 * <p>
 * NOTE: The reason the method portion of the file names are qualified with 'a',
 * 'b' or 'c' is just to make them alphabetically align nicely in a directory
 * file listing.
 * 
 * @author Riyad Kalla (software@thebuzzmedia.com)
 */
public class ComparisonGenerator {
	public static final int[] SIZES = new int[] { 240, 320, 480, 640, 800,
			1024, 1280, 1600, 1920, 2400, 3200 };

	public static int genCount = 0;

	/**
	 * Method used to fire off calls to
	 * {@link #generateComparisonsForImage(String)} which generates 3 different
	 * scaled versions (SPEED, BALANCED and QUALITY) of the specified image for
	 * each size defined in {@link #SIZES}.
	 * <p/>
	 * Simply add more calls to {@link #generateComparisonsForImage(String)}
	 * with a file path pointing at the image you want to generate comparisons
	 * for.
	 * 
	 * @throws IOException
	 *             If anything goes wrong with reading or writing the image
	 *             files.
	 */
	@Test
	public void generateComparisons() throws IOException {
		long totalTimeStart = System.currentTimeMillis();
		genCount = 0;

		generateComparisonsForImage("bin/L-screenshot.png");
		generateComparisonsForImage("bin/L-time-square.jpg");
		generateComparisonsForImage("bin/P-bicycle-race.jpg");

		/*
		 * Huge (5616x3744, 5.5MB) example image from a Canon 5D Mark II used
		 * for memory profile and a worst-case scenario for performance testing.
		 * http
		 * ://www.dpreview.com/galleries/reviewsamples/photos/111116/sample-20
		 * ?inalbum=canon-eos-5d-mark-ii-review-samples
		 */
		// generateComparisonsForImage("bin/L-huge-newspaper-dock.jpg");

		/*
		 * Huge (5616x3744, 4.7MB) example image from a Canon 5D Mark II used
		 * for memory profile and a worst-case scenario for performance testing.
		 * http
		 * ://www.dpreview.com/galleries/reviewsamples/photos/111103/sample-7
		 * ?inalbum=canon-eos-5d-mark-ii-review-samples
		 */
		// generateComparisonsForImage("bin/L-huge-flower.jpg");

		long totalRunTime = System.currentTimeMillis() - totalTimeStart;
		System.out.println("\n\nGenerated " + (SIZES.length * 3 * genCount)
				+ " images in " + (System.currentTimeMillis() - totalTimeStart)
				+ " ms (" + ((double) totalRunTime / (double) 1000)
				+ " seconds)");
		genCount = 0;
	}

	public void generateComparisonsForImage(String filename) throws IOException {
		generateComparisonForMethod(filename, Method.SPEED);
		generateComparisonForMethod(filename, Method.BALANCED);
		generateComparisonForMethod(filename, Method.QUALITY);
		genCount++;
	}

	public void generateComparisonForMethod(String filename, Method method)
			throws IOException {
		for (int size : SIZES) {
			File file = new File(filename);
			String name = file.getName().substring(0,
					file.getName().lastIndexOf('.'));
			String extension = file.getName().substring(
					file.getName().lastIndexOf('.') + 1,
					file.getName().length());

			File outputdir = new File("generated-comparisons");

			if (!outputdir.exists()) {
				outputdir.mkdir();
			}

			// We do this so the files list alphabetically
			String methodName = "UNKNOWN";

			switch (method) {
			case SPEED:
				methodName = "a-SPEED";
				break;
			case BALANCED:
				methodName = "b-BALANCED";
				break;
			case QUALITY:
				methodName = "c-QUALITY";
				break;
			}

			File newFile = new File(outputdir, name + "-" + size + "-"
					+ methodName + "." + extension);
			ImageIO.write(Scalr.resize(ImageIO.read(file), method, size, size),
					extension, newFile);
			System.out.println("Generated: " + newFile.getAbsolutePath());
		}
	}
}