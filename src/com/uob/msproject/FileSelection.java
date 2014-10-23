package com.uob.msproject;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

public class FileSelection extends Activity {
	/**
	 * Private variables
	 */
	// a list of files
	private File[] mFileList;
	private File mPath;
	private ArrayList<BitmapDrawable> mBitmapList;
	private ArrayList<String> mImageTextList;
	private ArrayList<String> mFilePathList;
	private static final String[] FILE_TYPES = new String[] { ".jpg", ".jpeg", ".bmp", ".png" };

	public static int sSelectedOpt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_selection);

		init();
	}

	/**
	 * Initialise
	 */
	private void init() {
		mPath = new File(Environment.getExternalStorageDirectory(), "PatternLockBreaker");
		mImageTextList = new ArrayList<String>();
		mBitmapList = new ArrayList<BitmapDrawable>();
		mFilePathList = new ArrayList<String>();

		// find controls
		final ListView imageList = (ListView) findViewById(R.id.main_listView);

		final Listadapter adapter = new Listadapter(this);
		imageList.setAdapter(adapter);

		Button processBtn = (Button) findViewById(R.id.process_btn);
		Button discardBtn = (Button) findViewById(R.id.discard_btn);
		Button backBtn = (Button) findViewById(R.id.back_btn);
		final Spinner spinner = (Spinner) findViewById(R.id.options_spinner);
		String[] options = new String[] { "Unprocessed", "Processed" };
		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
				options);
		spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(spinnerArrayAdapter);

		processBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ArrayList<String> selected = adapter.getSelected();

				// if unprocessed images are displayed
				switch (sSelectedOpt) {
				case 0:
					for (int i = 0; i < selected.size(); i++) {
						Utility.ImgProc.canny2(new File(selected.get(i)));
					}
					break;
				case 1:
					// when processing processed images
					// the selection mode is switched to single
					// selection
					Intent intent = new Intent(FileSelection.this, AspectRatioSelection.class);
					intent.putExtra("imagePath", selected.get(0));
					startActivity(intent);
					break;
				}
			}
		});

		discardBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ArrayList<String> selected = adapter.getSelected();
				for (int i = 0; i < selected.size(); i++) {
					Utility.discardImage(selected.get(i));
				}
				adapter.removeSelected();
			}
		});

		backBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				sSelectedOpt = position;
				loadFileList(position);
				adapter.updateDataSet(mImageTextList, mBitmapList, mFilePathList);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
				// TODO: check if anything needs to be done here
			}
		});

		// by default, only the unprocessed images
		// will be displayed
		spinner.setSelection(0);
	}

	/**
	 * Generate file path list, bitmap list and image text list
	 */
	private void generateLists() {
		mImageTextList.clear();
		mBitmapList.clear();
		mFilePathList.clear();

		try {
			for (int i = 0; i < mFileList.length; i++) {
				mImageTextList.add(mFileList[i].getName());

				mFilePathList.add(mFileList[i].getAbsolutePath());

				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inSampleSize = 100;
				Bitmap bmp = BitmapFactory.decodeFile(mFileList[i].getAbsolutePath(), opts);
				BitmapDrawable thumbnail = new BitmapDrawable(bmp);
				thumbnail.setBounds(0, 0, 40, 40);
				mBitmapList.add(thumbnail);
			}
		} catch (OutOfMemoryError e) {
			Log.d("Insufficient memory", String.format("Insufficient memory for bitmap creation.\n%s", e.getMessage()));
		}
	}

	/**
	 * Fill the list with images
	 */
	private void loadFileList(final int option) {
		String[] filePathList;
		
		try {
			if (sSelectedOpt == 0) {
				mPath = new File(Environment.getExternalStorageDirectory(), "PatternLockBreaker");
			} else {
				mPath = new File(Environment.getExternalStorageDirectory(), "PatternLockBreaker/Processed Images");
			}
			
			mPath.mkdirs();
		} catch (SecurityException e) {
		}
		if (mPath.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					boolean isImage = false;
					for (int i = 0; i < FILE_TYPES.length; i++) {
						boolean hasExtension = filename.toLowerCase().contains(FILE_TYPES[i]);
						if (option == 0) {
							isImage = hasExtension && !filename.contains("processed");
						} else { // only show processed images
							isImage = hasExtension && filename.contains("processed");
						}
						if (isImage)
							break;
					}

					return isImage;
				}
			};

			// get a list of file paths
			// to images
			filePathList = mPath.list(filter);
		} else {
			filePathList = new String[0];
		}

		// create a list of files from the file paths
		mFileList = new File[filePathList.length];
		for (int i = 0; i < filePathList.length; i++) {
			mFileList[i] = new File(mPath, filePathList[i]);
		}

		generateLists();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		int i = 0;
		try {
			// recycle all bitmaps
			if (mBitmapList != null) {
				for (i = 0; i < mBitmapList.size(); i++) {
					mBitmapList.get(i).getBitmap().recycle();
				}
			}
		} catch (NullPointerException e) {
			Log.d("Null Pointer Exception", String.format("At mBitmapList, item: %s", Integer.toString(i)));
		}
	}
}
