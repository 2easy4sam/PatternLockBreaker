package com.uob.msproject;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class Listadapter extends BaseAdapter {
	private ArrayList<String> mFilePathList;
	// list of image captions
	private ArrayList<String> mImageTextList;
	private ArrayList<BitmapDrawable> mBitmapList;
	private ArrayList<String> mSelectedImages;

	private ArrayList<String> mFilePathDeleteList;
	private ArrayList<BitmapDrawable> mBitmapDeleteList;
	private ArrayList<String> mImageTextDeleteList;

	private ArrayList<CheckBox> mCheckBoxes;
	private Activity mContext;

	/**
	 * Constructor
	 */
	public Listadapter(Activity context) {
		super();
		mContext = context;

		mImageTextList = new ArrayList<String>();
		mFilePathList = new ArrayList<String>();
		mBitmapList = new ArrayList<BitmapDrawable>();

		mSelectedImages = new ArrayList<String>();
		mBitmapDeleteList = new ArrayList<BitmapDrawable>();
		mImageTextDeleteList = new ArrayList<String>();
		mFilePathDeleteList = new ArrayList<String>();
		mCheckBoxes = new ArrayList<CheckBox>();
	}

	/**
	 * Remove the current list and fill the list with new items
	 */
	public void updateDataSet(ArrayList<String> imageTextList, ArrayList<BitmapDrawable> bitmapList,
			ArrayList<String> filePathList) {
		notifyDataSetInvalidated();

		mImageTextList = imageTextList;
		mBitmapList = bitmapList;
		mFilePathList = filePathList;

		reset();
	}

	/**
	 * Data structure for holding apk name along with a checkbox
	 */
	private class ViewHolder {
		TextView imgName;
		CheckBox checkBox;
	}

	/**
	 * Clear lists and refresh UI
	 */
	private void reset() {
		notifyDataSetChanged();

		mSelectedImages.clear();
		mBitmapDeleteList.clear();
		mImageTextDeleteList.clear();
		mFilePathDeleteList.clear();
	}

	/**
	 * Get the size of the image list
	 */
	public int getCount() {
		return mFilePathList.size();
	}

	/**
	 * Get the image at the clicked position
	 */
	public Object getItem(int position) {
		return mFilePathList.get(position);
	}

	/**
	 * Get the item ID
	 */
	public long getItemId(int position) {
		return 0;
	}

	/**
	 * Return a list of selected images
	 */
	public ArrayList<String> getSelected() {
		return mSelectedImages;
	}

	/**
	 * Remove all selected items i.e. image, text and checkbox
	 */
	public void removeSelected() {
		for (int i = 0; i < mSelectedImages.size(); i++) {
			mFilePathList.remove(mFilePathDeleteList.get(i));
			mBitmapList.remove(mBitmapDeleteList.get(i));
			mImageTextList.remove(mImageTextDeleteList.get(i));
		}

		// update the UI
		reset();
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;

		LayoutInflater inflater = mContext.getLayoutInflater();

		if (convertView == null) {
			// get the layout xml file
			convertView = inflater.inflate(R.layout.image_list, null);
			holder = new ViewHolder();

			// get the controls
			holder.imgName = (TextView) convertView.findViewById(R.id.textView);
			holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
			mCheckBoxes.add(holder.checkBox);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// set the image of TextView
		holder.imgName.setCompoundDrawables(mBitmapList.get(position), null, null, null);
		holder.imgName.setCompoundDrawablePadding(15);
		holder.imgName.setTextColor(Color.WHITE);
		holder.imgName.setText(mImageTextList.get(position));

		// when clicked, it should open the original
		// image in album
		holder.imgName.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO: to be tested
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				File image = new File((String) getItem(position));
				intent.setDataAndType(Uri.parse(String.format("%s%s", "file://", image.getAbsolutePath())), "image/*");
				mContext.startActivity(intent);
			}
		});

		holder.checkBox.setChecked(false);

		holder.checkBox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (FileSelection.sSelectedOpt == 0) {
					if (holder.checkBox.isChecked()) {
						mSelectedImages.add(mFilePathList.get(position));
						mBitmapDeleteList.add(mBitmapList.get(position));
						mImageTextDeleteList.add(mImageTextList.get(position));
						mFilePathDeleteList.add(mFilePathList.get(position));
					} else {
						mSelectedImages.remove(mFilePathList.get(position));
						mBitmapDeleteList.remove(mBitmapList.get(position));
						mImageTextDeleteList.remove(mImageTextList.get(position));
						mFilePathDeleteList.remove(mFilePathList.get(position));
					}

				} else if (FileSelection.sSelectedOpt == 1) {
					// single selection mode
					// remove the previously added item
					if (!mSelectedImages.isEmpty()) {
						mCheckBoxes.get(mFilePathList.indexOf(mSelectedImages.get(0))).setChecked(false);
					}
					mSelectedImages.clear();
					mBitmapDeleteList.clear();
					mImageTextDeleteList.clear();
					mFilePathDeleteList.clear();
					if (holder.checkBox.isChecked()) {
						mSelectedImages.add(mFilePathList.get(position));
						mBitmapDeleteList.add(mBitmapList.get(position));
						mImageTextDeleteList.add(mImageTextList.get(position));
						mFilePathDeleteList.add(mFilePathList.get(position));
					}
				}
			}
		});

		return convertView;
	}
}