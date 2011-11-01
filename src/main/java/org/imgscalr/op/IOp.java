package org.imgscalr.op;

import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;

/*
 * Using this instead of defining custom BufferedImageOps because BIOs use
 * 
 * createCompatibleDestImage method that makes no sense in imgscalr context, 
 * also the get bounds methods aren't needed for our uses and the signature of
 * the filter method is obnoxious (d = filter(s,d)).
 * 
 * Abstract this all out into a consistent API that imgscalr uses, regardless of
 * the OP.
 * 
 * PROPOSED TYPES
 * 
 * - ApplyOp
 * - CropOp
 * - PadOp
 * - ResizeOp
 * - RotateOp
 */
public interface IOp {
	public BufferedImage apply(BufferedImage src)
			throws IllegalArgumentException, ImagingOpException;
}