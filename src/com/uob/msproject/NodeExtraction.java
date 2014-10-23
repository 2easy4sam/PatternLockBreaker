package com.uob.msproject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * TODO: check if this activity is needed
 */
public class NodeExtraction extends Activity {
	private boolean[][] mNodes;
	private String mImagePath;
	private Bitmap[] mParts;
	private ImageView mImageView;
	private String mDetectedNodes;
	private final static int BLACK_PIXEL_VALUE = -16777216;

	// TODO: to be removed
	private int mIndex = 0;
	public static double THRESHOLD = 0.8;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.node_extraction);

		run();
	}

	private void run() {
		mNodes = new boolean[3][3];
		mImageView = (ImageView) findViewById(R.id.main_imageView);
		Intent intent = getIntent();
		// retrieve values
		Bundle bundle = intent.getExtras();
		mImagePath = bundle.getString("imagePath");

		findNodes();

		PatternSuggestion ps = new PatternSuggestion();
		String pattern = ps.randomMutationHC(100, mDetectedNodes);
		
		Toast.makeText(NodeExtraction.this, pattern, Toast.LENGTH_LONG).show();
		Log.d("Guessed pattern: ", pattern);
	}

	/**
	 * 
	 */
	private void findNodes() {
		Bitmap bmp = BitmapFactory.decodeFile(mImagePath);

		// divide the cropped image up into
		// 9 pieces evenly
		int width = Math.abs(bmp.getWidth() / 3);
		int height = Math.abs(bmp.getHeight() / 3);

		mParts = new Bitmap[9];
		mParts[0] = Bitmap.createBitmap(bmp, 0, 0, width, height);
		mParts[1] = Bitmap.createBitmap(bmp, width, 0, width, height);
		mParts[2] = Bitmap.createBitmap(bmp, width * 2, 0, width, height);

		mParts[3] = Bitmap.createBitmap(bmp, 0, height, width, height);
		mParts[4] = Bitmap.createBitmap(bmp, width, height, width, height);
		mParts[5] = Bitmap.createBitmap(bmp, width * 2, height, width, height);

		mParts[6] = Bitmap.createBitmap(bmp, 0, height * 2, width, height);
		mParts[7] = Bitmap.createBitmap(bmp, width, height * 2, width, height);
		mParts[8] = Bitmap.createBitmap(bmp, width * 2, height * 2, width, height);

		mImageView.setImageBitmap(mParts[mIndex]);

		// check one by one the ratio
		// between the number of
		// white pixels and the number of
		// black pixels

		// if the ratio is above/below a user-defined
		// value, then a node is asserted to be present
		StringBuilder strBuilder = new StringBuilder();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) { //
				mNodes[i][j] = hasNode(mParts[i * 3 + j]);
				// if the node is present
				if (mNodes[i][j]) {
					strBuilder.append(i * 3 + j);
				}
			}
		}
		mDetectedNodes = strBuilder.toString();
		Log.d("Detected nodes", "Detected nodes: " + mDetectedNodes);
	}

	/**
	 * Check if a particular part contains a node i.e. if the ratio between white
	 * and black pixels is above the specified threshold value, the part is
	 * asserted to contain a node
	 */
	private boolean hasNode(Bitmap bmp) {
		int width = bmp.getWidth();
		int height = bmp.getHeight();
		int[] pixels = new int[width * height];

		int nBlackPixels = 0;

		bmp.getPixels(pixels, 0, width, 0, 0, width, height);

		int index = 0;
		try {
			// count the number of white pixels
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					if (pixels[i * width + j] == BLACK_PIXEL_VALUE)
						nBlackPixels++;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			Log.d("Index out of bound",
					String.format("Actual length: %s\n Index: %s", Integer.toString(pixels.length), Integer.toString(index)));
		}
		
		return (Utility.divide(nBlackPixels, pixels.length, 4) < THRESHOLD);
	}
}
