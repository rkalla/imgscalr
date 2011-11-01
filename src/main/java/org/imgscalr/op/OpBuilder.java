package org.imgscalr.op;

import java.awt.Color;
import java.awt.image.BufferedImageOp;
import java.util.ArrayList;
import java.util.List;

import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;
import org.imgscalr.Scalr.Rotation;

public class OpBuilder {
	protected List<IOp> opList;

	public OpBuilder() {
		opList = new ArrayList<IOp>(8);
	}

	@Override
	public String toString() {
		return getClass().getName() + "@" + hashCode() + " [ops={"
				+ opList.toString() + "}]";
	}

	public OpBuilder apply(BufferedImageOp... ops)
			throws IllegalArgumentException {
		opList.add(new ApplyOp(ops));
		return this;
	}

	public OpBuilder crop(int width, int height)
			throws IllegalArgumentException {
		opList.add(new CropOp(width, height));
		return this;
	}

	public OpBuilder crop(int x, int y, int width, int height)
			throws IllegalArgumentException {
		opList.add(new CropOp(x, y, width, height));
		return this;
	}

	public OpBuilder pad(int padding) throws IllegalArgumentException {
		opList.add(new PadOp(padding));
		return this;
	}

	public OpBuilder pad(int padding, Color color)
			throws IllegalArgumentException {
		opList.add(new PadOp(padding, color));
		return this;
	}

	public OpBuilder resize(int targetSize) throws IllegalArgumentException {
		opList.add(new ResizeOp(targetSize));
		return this;
	}

	public OpBuilder resize(Method method, int targetSize)
			throws IllegalArgumentException {
		opList.add(new ResizeOp(method, targetSize));
		return this;
	}

	public OpBuilder resize(Mode mode, int targetSize)
			throws IllegalArgumentException {
		opList.add(new ResizeOp(mode, targetSize));
		return this;
	}

	public OpBuilder resize(Method method, Mode mode, int targetSize)
			throws IllegalArgumentException {
		opList.add(new ResizeOp(method, mode, targetSize));
		return this;
	}

	public OpBuilder resize(int targetWidth, int targetHeight)
			throws IllegalArgumentException {
		opList.add(new ResizeOp(targetWidth, targetHeight));
		return this;
	}

	public OpBuilder resize(Method method, int targetWidth, int targetHeight)
			throws IllegalArgumentException {
		opList.add(new ResizeOp(method, targetWidth, targetHeight));
		return this;
	}

	public OpBuilder resize(Mode mode, int targetWidth, int targetHeight)
			throws IllegalArgumentException {
		opList.add(new ResizeOp(mode, targetWidth, targetHeight));
		return this;
	}

	public OpBuilder resize(Method method, Mode mode, int targetWidth,
			int targetHeight) throws IllegalArgumentException {
		opList.add(new ResizeOp(method, mode, targetWidth, targetHeight));
		return this;
	}

	public OpBuilder rotate(Rotation rotation) throws IllegalArgumentException {
		opList.add(new RotateOp(rotation));
		return this;
	}

	public List<IOp> toList() {
		return opList;
	}
}