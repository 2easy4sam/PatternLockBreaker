package com.uob.msproject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {
	/**
	 * private fields
	 */
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}

		// check if a camera exists
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			Toast.makeText(this, "No camera on this device.", Toast.LENGTH_LONG).show();
			finish();
		}

		init();
	}

	/**
	 * Initialise the database, i.e. load database and etc.
	 */
	private void init() {
		DbAdapter db = new DbAdapter(MainActivity.this, false);
		db.open();
		db.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}
	}

	/**
	 * Transition to the PostPhotoAcquisitionActivity and launches the inbuilt
	 * camera app
	 */
	public void takePhoto(View v) {
		Intent intent = new Intent(this, PostPhotoAcquisitionActivity.class);
		startActivity(intent);
	}

	/**
	 * Exit the app
	 */
	public void quit(View v) {
		finish();
	}

	/**
	 * Let the user select an image to process
	 */
	public void manageImages(View v) {
		Intent intent = new Intent(this, FileSelection.class);
		startActivity(intent);
	}

	/**
	 * Bring up the settings page
	 */
	public void showSettings(View v) {
		Intent intent = new Intent(this, Settings.class);
		startActivity(intent);
	}
}