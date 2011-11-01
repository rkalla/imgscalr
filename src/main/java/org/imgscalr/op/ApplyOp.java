package org.imgscalr.op;

import static org.imgscalr.Scalr.DEBUG;
import static org.imgscalr.util.ImageUtil.*;
import static org.imgscalr.util.LogUtil.log;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImagingOpException;

public class ApplyOp implements IOp {
	protected BufferedImageOp[] ops;

	public ApplyOp(BufferedImageOp... ops) throws IllegalArgumentException {
		if (ops == null || ops.length == 0)
			throw new IllegalArgumentException("ops cannot be null or empty");

		this.ops = ops;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();

		buffer.append(getClass().getName());
		buffer.append('@');
		buffer.append(hashCode());
		buffer.append(" [length=").append(ops.length).append(", ops={");

		for (int i = 0; i < ops.length; i++) {
			buffer.append(ops[i]);

			if (i < (ops.length - 1))
				buffer.append(',');
		}

		buffer.append("}]");
		return buffer.toString();
	}

	@Override
	public BufferedImage apply(BufferedImage src)
			throws IllegalArgumentException, ImagingOpException {
		long t = System.currentTimeMillis();

		if (src == null)
			throw new IllegalArgumentException("src cannot be null");

		/*
		 * Ensure the src image is in the best supported image type before we
		 * continue, otherwise it is possible our calls below to getBounds2D and
		 * certainly filter(...) may fail if not.
		 * 
		 * Java2D makes an attempt at applying most BufferedImageOps using
		 * hardware acceleration via the ImagingLib internal library.
		 * 
		 * Unfortunately may of the BufferedImageOp are written to simply fail
		 * with an ImagingOpException if the operation cannot be applied with no
		 * additional information about what went wrong or attempts at
		 * re-applying it in different ways.
		 * 
		 * In internal testing, EVERY failure I've ever seen was the result of
		 * the source image being in a poorly-supported BufferedImage Type like
		 * BGR or ABGR (even though it was loaded with ImageIO).
		 * 
		 * To avoid this nasty/stupid surprise with BufferedImageOps, we always
		 * ensure that the src image starts in an optimally supported format
		 * before we try and apply the filter.
		 */
		if (!isOptimalImage(src))
			src = copyToOptimalImage(src);

		BufferedImage result = null;
		boolean hasReassignedSrc = false;

		if (DEBUG)
			log(0, this, "Applying %d BufferedImageOps...", ops.length);

		for (int i = 0; i < ops.length; i++) {
			long subT = System.currentTimeMillis();
			BufferedImageOp op = ops[i];

			// Skip null ops instead of throwing an exception.
			if (op == null)
				continue;

			if (DEBUG)
				log(1, this,
						"Applying BufferedImageOp [class=%s, toString=%s]...",
						op.getClass(), op.toString());

			/*
			 * Must use op.getBounds instead of src.getWidth and src.getHeight
			 * because we are trying to create an image big enough to hold the
			 * result of this operation (which may be to scale the image
			 * smaller), in that case the bounds reported by this op and the
			 * bounds reported by the source image will be different.
			 */
			Rectangle2D resultBounds = op.getBounds2D(src);

			// Watch out for flaky/misbehaving ops that fail to work right.
			if (resultBounds == null)
				throw new ImagingOpException(
						"BufferedImageOp ["
								+ op.toString()
								+ "] getBounds2D(src) returned null bounds for the target image.");

			/*
			 * We must manually create the target image; we cannot rely on the
			 * null-destination filter() method to create a valid destination
			 * for us thanks to this JDK bug that has been filed for almost a
			 * decade:
			 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4965606
			 */
			BufferedImage dest = createOptimalImage(src,
					(int) Math.round(resultBounds.getWidth()),
					(int) Math.round(resultBounds.getHeight()));

			// Perform the operation, update our result to return.
			result = op.filter(src, dest);

			/*
			 * Flush the 'src' image ONLY IF it is one of our interim temporary
			 * images being used when applying 2 or more operations back to
			 * back. We never want to flush the original image passed in.
			 */
			if (hasReassignedSrc)
				src.flush();

			/*
			 * Incase there are more operations to perform, update what we
			 * consider the 'src' reference to our last result so on the next
			 * iteration the next op is applied to this result and not back
			 * against the original src passed in.
			 */
			src = result;

			/*
			 * Keep track of when we re-assign 'src' to an interim temporary
			 * image, so we know when we can explicitly flush it and clean up
			 * references on future iterations.
			 */
			hasReassignedSrc = true;

			if (DEBUG)
				log(1,
						this,
						"Applied BufferedImageOp in %d ms, result [width=%d, height=%d]",
						System.currentTimeMillis() - subT, result.getWidth(),
						result.getHeight());
		}

		if (DEBUG)
			log(0, this, "All %d BufferedImageOps applied in %d ms",
					ops.length, System.currentTimeMillis() - t);

		return result;
	}
}