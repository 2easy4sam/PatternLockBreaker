package com.uob.msproject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * This activity allows the user to perform a range of actions with the taken
 * photo.
 */
public class PostPhotoAcquisitionActivity extends Activity {
	/**
	 * private fields
	 */
	private ImageView mImgPreview;
	private Uri mFileUri;
	private File mImage;
	private Bitmap mThumbnail;
	private int mCurrentAPIVersion;
	private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;

	// entry point
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post_photo_acquisition);

		// initialises controls
		init();
		// starts the inbuilt camera app
		captureImage();
	}

	/**
	 * initialises all controls
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void init() {
		try {
			mImgPreview = (ImageView) findViewById(R.id.imgPreview);
			Button retakeBtn = (Button) findViewById(R.id.retake_btn);
			Button discardBtn = (Button) findViewById(R.id.discard_btn);
			Button processBtn = (Button) findViewById(R.id.process_btn);

			retakeBtn.setOnClickListener(new View.OnClickListener() {
				/**
				 * discards the image taken and launches the camera app
				 */
				@Override
				public void onClick(View v) {
					Utility.discardImage(mImage.getAbsolutePath());

					mCurrentAPIVersion = android.os.Build.VERSION.SDK_INT;
					if (mCurrentAPIVersion < android.os.Build.VERSION_CODES.HONEYCOMB) {
						Intent intent = getIntent();
						finish();
						startActivity(intent);
					} else {
						// for API versions beyond 11
						recreate();
					}
				}
			});

			discardBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Utility.discardImage(mImage.getAbsolutePath());
					// go back to the previous screen
					finish();
				}
			});

			processBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// aspect ratio definition
					Intent intent = new Intent(PostPhotoAcquisitionActivity.this, AspectRatioSelection.class);
					intent.putExtra("imagePath", Utility.ImgProc.sProcessedImg.getAbsolutePath());
					startActivity(intent);
				}
			});
		} catch (NullPointerException e) {
			Toast.makeText(this, "Failed to initialise controls!", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * gets called after an image is captured
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// if the result is capturing Image
		if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				preview();
			} else if (resultCode == RESULT_CANCELED) {
				// image capture was cancelled
				Toast.makeText(getApplicationContext(), "User cancelled image capture.", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getApplicationContext(), "Failed to capture image!", Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * Apply Canny edge operator to the captured image and display it
	 */
	private void preview() {
		mThumbnail = null;
		try {
			BitmapFactory.Options opts = new BitmapFactory.Options();

			// reduce the size of the image as an 'OutOfMemory'
			// Exception might be thrown for larger images
			opts.inSampleSize = 10;

			// apply Canny edge operator
			Utility.ImgProc.canny2(mImage);
			mThumbnail = BitmapFactory.decodeFile(Utility.ImgProc.sProcessedImg.getAbsolutePath(), opts);
			// Utility.scanImage(this,
			// Utility.ImgProc.sProcessedImg.getAbsolutePath());
			mImgPreview.setImageBitmap(mThumbnail);
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			Log.d("Out of memory exception", String.format("Insufficient memory for bitmap creation:\n%s", e.getMessage()));
		}
	}

	/**
	 * capture an image
	 */
	private void captureImage() {
		try {
			Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			// create a images folder if it does not exist
			File imagesFolder = new File(Environment.getExternalStorageDirectory(), "PatternLockBreaker");
			imagesFolder.mkdirs();

			// format the file name
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
			String date = dateFormat.format(new Date());
			String fileName = "image_" + date + ".jpg";

			// create a new image file
			mImage = new File(imagesFolder, fileName);

			// save the file URI
			mFileUri = Uri.fromFile(mImage);

			imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);
			startActivityForResult(imageIntent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
		} catch (NullPointerException e) {
			Log.d("Null Pointer Exception", e.getMessage());
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mThumbnail != null) {
			mThumbnail.recycle();
			mThumbnail = null;
		}
	}
}