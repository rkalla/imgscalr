package org.imgscalr;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

public abstract class AbstractScalrTest {
	protected static BufferedImage src;

	@BeforeClass
	public static void setup() throws IOException {
		src = load("time-square.png");
	}

	@AfterClass
	public static void tearDown() {
		src.flush();
	}

	protected static BufferedImage load(String name) {
		BufferedImage i = null;

		try {
			i = ImageIO.read(AbstractScalrTest.class.getResource(name));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return i;
	}

	protected static void save(BufferedImage image, String name) {
		try {
			ImageIO.write(image, "PNG", new FileOutputStream(name));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void assertEquals(BufferedImage orig, BufferedImage tmp)
			throws AssertionError {
		// Ensure neither image is null.
		Assert.assertNotNull(orig);
		Assert.assertNotNull(tmp);

		// Ensure dimensions are equal.
		Assert.assertEquals(orig.getWidth(), tmp.getWidth());
		Assert.assertEquals(orig.getHeight(), tmp.getHeight());

		int w = orig.getWidth();
		int h = orig.getHeight();

		// Ensure every pixel RGB is the same.
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				Assert.assertEquals(orig.getRGB(i, j), tmp.getRGB(i, j));
			}
		}
	}
}