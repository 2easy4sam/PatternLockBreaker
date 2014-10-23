package com.uob.msproject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.io.FilenameUtils;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

public class Utility {
	private static Random rand;

	/**
	 * save the image to a specified location
	 * 
	 * @param dir
	 *           : the output directory
	 * @param output
	 *           : the output file
	 */
	public static void saveImage(String dir, File output) {
	}

	/**
	 * Divide two int values and return a float value
	 * 
	 * @param precision
	 *           : the number of decimal places to keep
	 */
	public static float divide(float a, float b, int precision) {
		StringBuilder strBuilder = new StringBuilder("1");

		for (int i = 0; i < precision; i++) {
			strBuilder.append(0);
		}

		int temp = Integer.parseInt(strBuilder.toString());

		return (float) Math.round((float) a / (float) b * temp) / temp;
	}

	/**
	 * Convert a character to integer
	 */
	public static int charToInt(Character c) {
		int converted = 0;
		try {
			converted = Integer.parseInt(Character.toString(c));
		} catch (Exception e) {
			System.out.println("Invalid conversion.");
		}
		return converted;
	}

	/**
	 * Generate a random number between 2 given numbers
	 */
	public static float randFloat(float a, float b) {
		if (rand == null) {
			rand = new Random();
			rand.setSeed(System.nanoTime());
		}

		float max = Math.max(a, b);
		float min = Math.min(a, b);
		float diff = Math.abs(max - min);
		float res = rand.nextFloat() * diff + min;
		return res;
	}

	/**
	 * Generate a random integer between a and b
	 */
	public static int randInt(int a, int b) {
		int res = 0;
//		try {
			if (rand == null) {
				rand = new Random();
				rand.setSeed(System.nanoTime());
			}

			int max = Math.max(a, b);
			int min = Math.min(a, b);
			int diff = Math.abs(max - min);
			res = rand.nextInt(diff) + min;
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		}
		
		return res;
	}

	/**
	 * Discard a taken image
	 */
	public static void discardImage(String filePath) {
		File file = new File(filePath);
		if (file.exists()) {
			if (file.delete()) {
				Log.d("Delete file", String.format("%s was deleted successfully.", filePath));
			} else {
				Log.d("Delete file", String.format("Failed to delete %s.", filePath));
			}
		}
	}

	/**
	 * Image Processing related utility methods
	 */
	public static class ImgProc {
		public static int sLowerThreshold;
		public static int sUpperThreshold;
		public static File sProcessedImg = null;

		/**
		 * @param file
		 *           : the file path of the image
		 */
		public static void canny1(File file) {
			Mat src = null, srcGray = null;
			Mat dst = null, detected_edges = null;
			int edgeThresh = 1;
			int lowerThreshold = 20;
			int ratio = 3;
			int kernel_size = 3;

			try {
				if (file.exists()) {
					src = Highgui.imread(file.getAbsolutePath());
				}

				if (src.empty()) {
					// if no data were read
					// TODO: think of a better way of handling this error
					return;
				}

				// initialise Mats
				dst = new Mat(src.size(), src.type());
				srcGray = new Mat(src.size(), CvType.CV_8UC4);
				detected_edges = new Mat(src.size(), CvType.CV_8UC4);

				// convert the colour image to grayscale
				Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);
				// TODO: compare the performance of 'blur' and 'GaussianBlur'
				// convolve the image with a 3*3 blur kernel
				Imgproc.blur(src, detected_edges, new Size(3, 3));
				// apply Canny edge detector
				int upperThreshold = lowerThreshold * ratio;
				// Imgproc.Canny(detected_edges, detected_edges, lowerThreshold,
				// upperThreshold);
				Imgproc.Canny(detected_edges, detected_edges, 35, 75); // TODO: to
																							// be
																							// removed
				// fill 'dst' with 0s, i.e. the image is black
				dst.setTo(Scalar.all(0));

				// copy 'src' onto 'dst', where ONLY non-zero values
				// are copied (from 'detected_edges')
				src.copyTo(dst, detected_edges);

				// overwrite the image
				Highgui.imwrite(file.getAbsolutePath(), dst);
			} catch (NullPointerException e) {
				Log.d("Null Pointer Exception", e.getMessage());
			}
		}

		/**
		 * This method is taken from
		 */
		public static void canny2(File file) {
			Mat src = null, gray, intermediate;

			if (file.exists()) {
				src = Highgui.imread(file.getAbsolutePath());
			}
			gray = new Mat();
			intermediate = new Mat();

			Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY);

			// doing a gaussian blur prevents getting a lot of false hits
			Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 2, 2);

			// int lowerThreshold = 10;
			// int upperThreshold = 30;

			Imgproc.Canny(gray, intermediate, sLowerThreshold, sUpperThreshold);
			// last parameter is the number of channels
			// in the destination image
			Imgproc.cvtColor(intermediate, src, Imgproc.COLOR_GRAY2BGRA, 4);

			File processedImageFolder = new File(file.getParentFile().getAbsolutePath(), "Processed Images");
			processedImageFolder.mkdir();

			String filePath = String.format("%s/%s", processedImageFolder.getAbsolutePath(),
					FilenameUtils.removeExtension(file.getName()) + "_processed.jpg");
			Highgui.imwrite(filePath, src);

			sProcessedImg = new File(filePath);
			// Highgui.imwrite(file.getAbsolutePath(), src);
		}

		/**
		 * load the native C++ library
		 */
		static {
			Log.d("OpenCV initialisation", OpenCVLoader.initDebug() ? "OpenCV loaded successfully!" : "OpenCV failed to load!");
		}
	}
}