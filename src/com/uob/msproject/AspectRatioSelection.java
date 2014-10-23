package com.uob.msproject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import eu.janmuller.android.simplecropimage.CropImage;

public class AspectRatioSelection extends Activity {
	/**
	 * Private variables
	 */
	private Bitmap mBmp;
	private Point[] mPts;
	private TextView mInfo;
	private int mIndex;
	private String mImagePath;
	private CustomView mView;
	private Button mConfirmBtn;

	Button mCancelBtn;
	private static final int REQUEST_CODE_CROP_IMAGE = 0x3;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.aspect_ratio_selection);

		init();
	}

	private void init() {
		mIndex = 0;
		mPts = new Point[2];

		for (int i = 0; i < 2; i++) {
			mPts[i] = new Point();
		}

		// find controls
		RelativeLayout layout = (RelativeLayout) findViewById(R.id.main_layout);
		mConfirmBtn = (Button) findViewById(R.id.confirm_btn);
		mCancelBtn = (Button) findViewById(R.id.cancel_btn);
		mInfo = (TextView) findViewById(R.id.info_textView);

		mInfo.setText("Select the top left corner of the pattern lock");
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		mImagePath = (String) bundle.get("imagePath");
		if (mImagePath != null) {
			// create a bitmap
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inSampleSize = 8;
			mBmp = BitmapFactory.decodeFile(mImagePath, opts);

			mView = new CustomView(this);
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.CENTER_IN_PARENT);
			mView.setLayoutParams(params);
			layout.addView(mView);
		}

		mConfirmBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mIndex != 1) {
					mIndex++;
					mInfo.setText("Select the bottom right corner of the pattern lock");
				} else {
					Intent intent = new Intent(AspectRatioSelection.this, CropImage.class);
					intent.putExtra(CropImage.IMAGE_PATH, mImagePath);
					intent.putExtra(CropImage.SCALE, true);
					intent.putExtra(CropImage.ASPECT_X, (int) Math.abs((double) mPts[0].x - mPts[1].x));
					intent.putExtra(CropImage.ASPECT_Y, (int) Math.abs((double) mPts[0].y - mPts[1].y));

					startActivityForResult(intent, REQUEST_CODE_CROP_IMAGE);
				}
			}
		});

		mCancelBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// this should transit back to the post photo acquisition
				finish();
			}
		});
	}

	class Point {
		float x, y;
	}

	class CustomView extends ImageView {
		private Paint mPaint;

		public CustomView(Context context) {
			super(context);
			init();
		}

		private void init() {
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setColor(Color.RED);
			if (mBmp != null) {
				setImageBitmap(mBmp);
			}
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			if (mPts != null) {
				for (int i = 0; i < mIndex + 1; i++) {
					canvas.drawCircle(mPts[i].x, mPts[i].y, 10, mPaint);
				}
			}
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (mPts == null)
				return false;
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mPts[mIndex].x = event.getX();
				mPts[mIndex].y = event.getY();
				break;
			}

			// cause the 'onDraw' event to take place
			invalidate();
			return true;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			return;
		}

		Bitmap bmp;
		final String path;
		switch (requestCode) {
		case REQUEST_CODE_CROP_IMAGE:
			// get the path to the cropped image
			path = data.getStringExtra(CropImage.IMAGE_PATH);
			if (path == null) {
				return;
			}

			bmp = BitmapFactory.decodeFile(path);
			// clear the dots
			mPts = null;

			mView.setImageBitmap(bmp);
			mInfo.setText("Are you happy with the cropped image?");
			mConfirmBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(AspectRatioSelection.this, NodeExtraction.class);
					intent.putExtra("imagePath", path);
					startActivity(intent);
					finish();
				}
			});
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	// clear up memory
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mBmp != null) {
			mBmp.recycle();
			mBmp = null;
		}
	}
}