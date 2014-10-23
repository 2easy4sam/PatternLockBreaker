package com.uob.msproject;

import android.app.Activity;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class Settings extends Activity {
	private SeekBar mLT;
	private SeekBar mUT;
	private Button mResetBtn;
	private Button mSaveBtn;
	private Button mReloadBtn;
	private Button mLoadAllBtn;
	private Button mBackBtn;
	private static final int LOWER_THRESHOLD_MAX_VAL = 100;
	private TextView mLtTextView;
	private TextView mUtTextView;
	
	private DbAdapter mDbApater;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		init();
	}

	private void init() {
		mDbApater = new DbAdapter(Settings.this, false);
		mDbApater.open();
		int[] data = mDbApater.loadCannyParams();

		Utility.ImgProc.sLowerThreshold = data[0];
		Utility.ImgProc.sUpperThreshold = data[1];

		mLT = (SeekBar) findViewById(R.id.lower_threshold_seekBar);
		mUT = (SeekBar) findViewById(R.id.upper_threshold_seekBar);
		mResetBtn = (Button) findViewById(R.id.reset_btn);
		mSaveBtn = (Button) findViewById(R.id.save_btn);
		mReloadBtn = (Button) findViewById(R.id.btn_reloadPatterns);
		mLoadAllBtn = (Button) findViewById(R.id.loadAllPatterns_btn);
		mBackBtn = (Button) findViewById(R.id.back_btn);
		mLtTextView = (TextView) findViewById(R.id.lt_textView);
		mUtTextView = (TextView) findViewById(R.id.ut_textView);
		
		mLT.setProgress(Utility.ImgProc.sLowerThreshold);
		mUT.setProgress(Utility.ImgProc.sUpperThreshold);
		mLtTextView.setText(Integer.toString(Utility.ImgProc.sLowerThreshold));
		mUtTextView.setText(Integer.toString(Utility.ImgProc.sUpperThreshold));
		
		mLT.setMax(LOWER_THRESHOLD_MAX_VAL);

		setListners();
	}

	/**
	 * Set event listeners
	 */
	private void setListners() {
		mLT.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				updateText(true, progress);
			}
		});

		mUT.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				updateText(false, progress);
			}
		});

		mSaveBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				save();
				finish();
			}
		});

		mResetBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// update database values
				mLT.setProgress(35);
				mUT.setProgress(75);

				// recreate all tables
				// but pattern table

				save();
			}
		});

		mReloadBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// reload all training patterns from files
				mDbApater.recreateAllTables();
				mDbApater.readTrainingPatternsFromFiles();
			}
		});

		mBackBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		mLoadAllBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mDbApater.recreateTables(mDbApater.ALL_PATTERN_TABLE);
				mDbApater.loadAllPatterns();
			}
		});
	}

	private void updateText(boolean lowerThreshold, int progress) {
		if (lowerThreshold) {
			mLtTextView.setText(Integer.toString(progress));
		} else {
			mUtTextView.setText(Integer.toString(progress));
		}
	}

	/**
	 * Clean up variables
	 */
	private void cleanup() {
		mDbApater.close();
		mDbApater = null;
	}

	private void save() {
		Utility.ImgProc.sLowerThreshold = mLT.getProgress();
		Utility.ImgProc.sUpperThreshold = mUT.getProgress();
		
		mDbApater.updateCannyParams(mLT.getProgress(), mUT.getProgress());
		cleanup();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mDbApater != null) {
			cleanup();
		}
	}
}