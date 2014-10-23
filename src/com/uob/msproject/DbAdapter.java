package com.uob.msproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

/**
 * This class is used to handle database logic
 */
public class DbAdapter {
	private static final String DATABASE_NAME = "data";
	// TODO: increment this number EVERY TIME a change
	// is made to ANY of the tables
	private static final int DATABASE_VERSION = 6;

	/**
	 * Table properties
	 */
	public static final String CANNY_TABLE = "canny_params";
	public static final String TRAINING_PATTERN_TABLE = "training_patterns";
	public static final String ALL_PATTERN_TABLE = "all_patterns";
	// this table contains the probabilities of each node
	// being the starting node
	public static final String UNIGRAM_TABLE = "unigram_probabilitise";
	public static final String BIGRAM_TABLE = "bigram_probabilitise";
	public static final String TRIGRAM_TABLE = "trigram_probabilitise";
	// The frequencies of each node in all training patterns

	private static final String CANNY_TABLE_COLS = " (lower_threshold int, upper_threshold int);";
	// this is a generic set of properties shared between pattern tables
	private static final String PATTERN_TABLE_COLS = " (_id integer primary key autoincrement, pattern varchar(255));";
	private static final String UNIGRAM_TABLE_COLS = " (node int, probability float);";
	private static final String BIGRAM_TABLE_COLS = " (node int, next_node int, probability float);";
	private static final String TRIGRAM_TABLE_COLS = " (sequence varchar(255), next_node int, probability float);";

	/**
	 * SQL queries
	 */
	private static final String DROP_TABLE = "DROP TABLE IF EXISTS %s";
	private static final String CANNY_TABLE_CREATE = "create table " + CANNY_TABLE + CANNY_TABLE_COLS;
	private static final String ALL_PATTERN_TABLE_CREATE = "create table " + ALL_PATTERN_TABLE + PATTERN_TABLE_COLS;
	private static final String TRAINING_PATTERN_TABLE_CREATE = "create table " + TRAINING_PATTERN_TABLE + PATTERN_TABLE_COLS;
	private static final String UNIGRAM_TABLE_CREATE = "create table " + UNIGRAM_TABLE + UNIGRAM_TABLE_COLS;
	private static final String BIGRAM_TABLE_CREATE = "create table " + BIGRAM_TABLE + BIGRAM_TABLE_COLS;
	private static final String TRIGRAM_TABLE_CREATE = "create table " + TRIGRAM_TABLE + TRIGRAM_TABLE_COLS;

	private DbHelper mDbHelper;
	private final Context mCxt;
	private SQLiteDatabase mDb;
	private ProgressDialog mProgressBar;

	// this variable indicates whether or not we are conducting a cross
	// validation, so that patterns will be loaded correspondingly
	private boolean mCrossValidation;

	// The number of times each bi-combo has occurred
	private HashMap<String, Integer> mBiComboCount;
	private HashMap<String, Integer> mTriComboCount;
	private int[] mNodeCount;

	// Types of probabilities
	public enum Probability {
		UNIGRAM, BIGRAM, TRIGRAM
	};

	private static final String[] mTableNames = new String[] { CANNY_TABLE, ALL_PATTERN_TABLE, TRAINING_PATTERN_TABLE,
			UNIGRAM_TABLE, BIGRAM_TABLE, TRIGRAM_TABLE };

	/**
	 * Helper class that takes care of opening a database if it exists, creating
	 * it if it does not, and updating it if necessary
	 */
	private static class DbHelper extends SQLiteOpenHelper {
		/**
		 * Constructor
		 */
		public DbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CANNY_TABLE_CREATE);
			db.execSQL(ALL_PATTERN_TABLE_CREATE);
			db.execSQL(TRAINING_PATTERN_TABLE_CREATE);
			db.execSQL(UNIGRAM_TABLE_CREATE);
			db.execSQL(BIGRAM_TABLE_CREATE);
			db.execSQL(TRIGRAM_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w("Upgrading", "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", which will destroy all old data");
			// upgrade the database to a newer version
			// if the table already exists, drop it
			for (int i = 0; i < mTableNames.length; i++) {
				db.execSQL(String.format(DROP_TABLE, mTableNames[i]));
			}
			onCreate(db);
		}
	} // end of helper class

	public DbAdapter(Context cxt, boolean crossValidation) {
		mCxt = cxt;
		mCrossValidation = crossValidation;
		mProgressBar = new ProgressDialog(cxt);
		// Progress dialog
		mProgressBar.setCancelable(false);
		mProgressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);

		mBiComboCount = new HashMap<String, Integer>();
		mTriComboCount = new HashMap<String, Integer>();
		mNodeCount = new int[9];
		for (int i = 0; i < 9; i++) {
			mNodeCount[i] = -1;
		}
	}

	/**
	 * Initialisation
	 */
	public DbAdapter open() throws SQLException {
		mDbHelper = new DbHelper(mCxt);
		mDb = mDbHelper.getWritableDatabase();

		return this;
	}

	/**
	 * Close the helper
	 */
	public void close() {
		mDbHelper.close();
	}

	/**
	 * Check if a table already exists in the database create a new one if it
	 * does not exist
	 */
	private void createTable(String tableName) {
		String cols = "";
		switch (tableName) {
		case CANNY_TABLE:
			cols = CANNY_TABLE_COLS;
			break;
		case ALL_PATTERN_TABLE:
		case TRAINING_PATTERN_TABLE:
			cols = PATTERN_TABLE_COLS;
			break;
		case UNIGRAM_TABLE:
			cols = UNIGRAM_TABLE_COLS;
			break;
		case BIGRAM_TABLE:
			cols = BIGRAM_TABLE_COLS;
			break;
		case TRIGRAM_TABLE:
			cols = TRIGRAM_TABLE_COLS;
			break;
		}

		mDb.execSQL("CREATE TABLE IF NOT EXISTS " + tableName + cols);
	}

	/**
	 * Recreate tables
	 */
	public void recreateTables(String... tableNames) {
		for (int i = 0; i < tableNames.length; i++) {
			String tableName = tableNames[i];
			dropTables(tableName);
			createTable(tableName);
		}
	}

	/**
	 * Drop all tables and create them again
	 */
	public void recreateAllTables() {
		recreateTables(mTableNames);
	}

	/**
	 * Drop the specified tables
	 */
	public void dropTables(String... tables) {
		for (int i = 0; i < tables.length; i++) {
			String tableName = tables[i];
			mDb.execSQL(String.format(DROP_TABLE, tableName));
		}
	}

	/**
	 * Insert default values into 'canny_params'
	 */
	private void insertCannyParams(int lower, int upper) {
		ContentValues values = new ContentValues();
		values.put("lower_threshold", lower);
		values.put("upper_threshold", upper);
		mDb.insert(CANNY_TABLE, null, values);
	}

	/**
	 * Insert a training pattern to the database
	 */
	public void insertTrainingPattern(String pattern) {
		ContentValues values = new ContentValues();
		values.put("pattern", pattern);

		mDb.insert(TRAINING_PATTERN_TABLE, null, values);
	}

	public void insertPattern(String pattern) {
		ContentValues values = new ContentValues();
		values.put("pattern", pattern);

		mDb.insert(ALL_PATTERN_TABLE, null, values);
	}

	/**
	 * Insert the probability of a node into the table
	 */
	public void insertStartingProbability(int node, float probability) {
		ContentValues values = new ContentValues();
		values.put("node", node);
		values.put("probability", probability);

		mDb.insert(UNIGRAM_TABLE, null, values);
	}

	/**
	 * Insert a bigram probability into the database
	 */
	public void insertBigramProbability(int node, int nextNode, float probability) {
		ContentValues values = new ContentValues();
		values.put("node", node);
		values.put("next_node", nextNode);
		values.put("probability", probability);

		mDb.insert(BIGRAM_TABLE, null, values);
	}

	/**
	 * Insert a trigram probability into the database
	 */
	public void insertTrigramProbability(String sequence, int nextNode, float probability) {
		ContentValues values = new ContentValues();
		values.put("sequence", sequence);
		values.put("next_node", nextNode);
		values.put("probability", probability);

		mDb.insert(TRIGRAM_TABLE, null, values);
	}

	/**
	 * Retrieve the entire list of patterns for training the Markov models as
	 * well as the 5-fold cross validation
	 */
	public void getTrainingPatterns() {
		// Check if the training has already been loaded
		if (PatternSuggestion.sTrainingSet.size() != 0)
			return;
		String[] columns = new String[] { "_id", "pattern" };
		Cursor cursor = mDb.query(TRAINING_PATTERN_TABLE, columns, null, null, null, null, null);

		int patternColIdx = cursor.getColumnIndex("pattern");

		// loop through all entries and add each
		// entry to the return list
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			PatternSuggestion.sTrainingSet.add(cursor.getString(patternColIdx));
		}
	}

	/**
	 * Return a list of starting probabilities
	 */
	private void getStartingPorbabilities() {
		String[] columns = new String[] { "node", "probability" };
		Cursor cursor = mDb.query(UNIGRAM_TABLE, columns, null, null, null, null, null);

		int nodeColIdx = cursor.getColumnIndex("node");
		int probabilityColIdx = cursor.getColumnIndex("probability");

		// loop through all entries and add each
		// entry to the return list
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			PatternSuggestion.sStartingProbabilities[cursor.getInt(nodeColIdx)] = cursor.getFloat(probabilityColIdx);
		}
		cursor.close();
	}

	/**
	 * Read bigram probabilities from the table
	 */
	private void getBigramProbabilities() {
		String[] columns = new String[] { "node", "next_node", "probability" };
		Cursor cursor = mDb.query(BIGRAM_TABLE, columns, null, null, null, null, null);

		int nodeColIdx = cursor.getColumnIndex("node");
		int nextNodeColIdx = cursor.getColumnIndex("next_node");
		int probabilityColIdx = cursor.getColumnIndex("probability");

		// loop through all entries and add each
		// entry to the return list
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			PatternSuggestion.sBigramProbabilities[cursor.getInt(nodeColIdx)][cursor.getInt(nextNodeColIdx)] = cursor
					.getFloat(probabilityColIdx);
		}
		cursor.close();
	}

	/**
	 * Read trigram probabilities from the table
	 */
	private void getTrigramProbabilities() {
		String[] columns = new String[] { "sequence", "next_node", "probability" };
		Cursor cursor = mDb.query(TRIGRAM_TABLE, columns, null, null, null, null, null);

		int sequenceColIdx = cursor.getColumnIndex("sequence");
		int nextNodeColIdx = cursor.getColumnIndex("next_node");
		int probabilityColIdx = cursor.getColumnIndex("probability");

		// loop through all entries and add each
		// entry to the return list
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			String sequence = cursor.getString(sequenceColIdx);
			PatternSuggestion.sTrigramProbabilities[Utility.charToInt(sequence.charAt(0))][Utility
					.charToInt(sequence.charAt(1))][cursor.getInt(nextNodeColIdx)] = cursor.getFloat(probabilityColIdx);
		}
		cursor.close();
	}

	/**
	 * Load Canny edge detector parameters
	 */
	public int[] loadCannyParams() {
		// if there is no data, insert some
		// initial/default values
		if (!hasData(CANNY_TABLE)) {
			insertCannyParams(10, 35);
		}

		int[] data = new int[2];
		String[] columns = new String[] { "lower_threshold", "upper_threshold" };
		Cursor cursor = mDb.query(CANNY_TABLE, columns, null, null, null, null, null);
		// the table only has one row
		cursor.moveToFirst();
		data[0] = cursor.getInt(cursor.getColumnIndex("lower_threshold"));
		data[1] = cursor.getInt(cursor.getColumnIndex("upper_threshold"));
		cursor.close();
		return data;
	}

	/**
	 * Check if a table has data at all
	 */
	public boolean hasData(String tableName) {
		boolean res = false;
		try {
			String[] columns = null;

			switch (tableName) {
			case ALL_PATTERN_TABLE:
			case TRAINING_PATTERN_TABLE:
				columns = new String[] { "_id", "pattern" };
				break;
			case CANNY_TABLE:
				columns = new String[] { "lower_threshold", "upper_threshold" };
				break;
			case UNIGRAM_TABLE:
				columns = new String[] { "node", "probability" };
				break;
			case BIGRAM_TABLE:
				columns = new String[] { "node", "next_node", "probability" };
				break;
			case TRIGRAM_TABLE:
				columns = new String[] { "sequence", "next_node", "probability" };
				break;
			}

			Cursor cursor = mDb.query(tableName, columns, null, null, null, null, null);
			res = (cursor.getCount() > 0);
			cursor.close();
		} catch (SQLiteException e) {
			// the table does not exist
			// create a new one
			createTable(tableName);

			return false;
		}
		return res;
	}

	/**
	 * Update the Canny Edge detector parameters
	 */
	public void updateCannyParams(int lower, int upper) {
		ContentValues vals = new ContentValues();
		vals.put("lower_threshold", lower);
		vals.put("upper_threshold", upper);

		mDb.update(CANNY_TABLE, vals, null, null);
	}

	/**
	 * Load patterns from a text file
	 */
	public void loadPatternsFromTextFile() {
		BufferedReader br = null;

		try {
			File patternsFolder = new File(Environment.getExternalStorageDirectory(), "PatternLockBreaker/Patterns");
			if (patternsFolder.exists()) {
				File[] files = patternsFolder.listFiles();

				// sort the files by date
				Arrays.sort(files, new Comparator<File>() {
					public int compare(File f1, File f2) {
						return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
					}
				});

				// get the latest file
				br = new BufferedReader(new FileReader(files[files.length - 1]));
				String line = "";
				while ((line = br.readLine()) != null) {
					insertTrainingPattern(line);
				}
			}
		} catch (IOException e) {

		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Load starting probabilities
	 */
	public void loadStartingProbabilities() {
		// first of all, check if unigram table
		// contains any data
		if (!hasData(UNIGRAM_TABLE)) {
			// check if training patterns
			// are present in the database
			if (!hasData(TRAINING_PATTERN_TABLE)) {
				// the training list does not exist yet
				readTrainingPatternsFromFiles();
				calculateUnigramProbabilities();
			} else {
				getTrainingPatterns();
				calculateUnigramProbabilities();
			}
		} else {
			// load probabilities from the table
			getStartingPorbabilities();
		}
	}

	/**
	 * Load bigram probabilities from the table
	 */
	public void loadBigramProbabilities() {
		if (!hasData(BIGRAM_TABLE)) {
			if (!hasData(TRAINING_PATTERN_TABLE)) {
				// the training list does not exist yet
				readTrainingPatternsFromFiles();
				calculateBirgramProbabilities();
			} else {
				getTrainingPatterns();
				calculateBirgramProbabilities();
			}
		} else {
			getBigramProbabilities();
		}
	}

	public void loadTrigramProbabilities() {
		if (!hasData(TRIGRAM_TABLE)) {
			if (!hasData(TRAINING_PATTERN_TABLE)) {
				// the training list does not exist yet
				readTrainingPatternsFromFiles();
				calculateTrigramProbabilities();
			} else {
				getTrainingPatterns();
				calculateTrigramProbabilities();
			}
		} else {
			getTrigramProbabilities();
		}
	}

	/**
	 * Calculate the probabilities of each node being the starting node as well
	 * as the unigram probabilities, i.e. the number of times each node has
	 * appeared in the training patterns. These probabilities are independent of
	 * other nodes NB: the only instance when this function should be called is
	 * when the training pattern list has been updated
	 */
	public void calculateUnigramProbabilities() {
		int nTrainingPatterns = 0;
		int[] counts = new int[9];

		nTrainingPatterns = mCrossValidation ? CrossValidation.sTrainingSet.size() : PatternSuggestion.sTrainingSet.size();

		// loop through all training patterns
		// and work out the probabilities of
		// each node being the starting node
		for (int i = 0; i < nTrainingPatterns; i++) {
			String pattern = mCrossValidation ? CrossValidation.sTrainingSet.get(i) : PatternSuggestion.sTrainingSet.get(i);
			// get the first character
			counts[Character.getNumericValue(pattern.charAt(0))]++;
		} // end FOR

		for (int i = 0; i < 9; i++) {
			float probability = Utility.divide(counts[i], nTrainingPatterns, 4);
			PatternSuggestion.sStartingProbabilities[i] = probability;
			if (!mCrossValidation)
				insertStartingProbability(i, probability);
		}
	}

	/**
	 * Calculate the bigram probabilities using training patterns
	 */
	public void calculateBirgramProbabilities() {
		for (int i = 0; i < 9; i++) {
			for (int j = 0; j < 9; j++) {
				if (i == j)
					continue;
				int comboFreq = biComboFreqCount(i, j);
				int nodeFreq = nodeFreqCount(i);

				float probability = Utility.divide(comboFreq, nodeFreq, 4);
				// given that i has occurred, what are the chances that
				// j will occur next
				PatternSuggestion.sBigramProbabilities[i][j] = probability;
				if (!mCrossValidation)
					insertBigramProbability(i, j, probability);
			}
		}
	}

	/**
	 * Calculate the trigram probabilities using training patterns TODO: to be
	 * finished
	 */
	public void calculateTrigramProbabilities() {
		for (int i = 0; i < 9; i++) {
			for (int j = 0; j < 9; j++) {
				if (i == j)
					continue;
				for (int k = 0; k < 9; k++) {
					if (j == k || i == k)
						continue;
					// in this case, i and j have already occurred
					// what are the chances that k will occur next
					int comboFreq = triComboFreqCount(i, j, k);
					float denominator = nodeFreqCount(i) * PatternSuggestion.sBigramProbabilities[i][j];

					String sequence = Integer.toString(i) + Integer.toString(j);
					float probability = Utility.divide(comboFreq, denominator, 4);
					PatternSuggestion.sTrigramProbabilities[i][j][k] = probability;
					if (!mCrossValidation)
						insertTrigramProbability(sequence, k, probability);
				}
			}
		}
	}

	/**
	 * reinitialise local variables. Used in cross validation
	 */
	public void reinitLocalVars() {
		for (int i = 0; i < 9; i++) {
			mNodeCount[i] = -1;
		}
		
		mBiComboCount = new HashMap<String, Integer>();
		mTriComboCount = new HashMap<String, Integer>();
	}

	/**
	 * Count the number of times a node has appeared in training patterns
	 */
	private int nodeFreqCount(int node) {
		int count = 0;
		int size = mCrossValidation ? CrossValidation.sTrainingSet.size() : PatternSuggestion.sTrainingSet.size();

		if (mNodeCount[node] != -1) {
			count = mNodeCount[node];
		} else {
			for (int i = 0; i < size; i++) {
				String pattern = mCrossValidation ? CrossValidation.sTrainingSet.get(i) : PatternSuggestion.sTrainingSet.get(i);
				if (pattern.contains(Integer.toString(node))) {
					count++;
				}
			}
		}

		return count;
	}

	/**
	 * Count the number of times a combination has appeared in the training
	 * patterns
	 */
	private int biComboFreqCount(int x, int y) {
		int counter1 = 0;
		int counter2 = 0;

		int size = mCrossValidation ? CrossValidation.sTrainingSet.size() : PatternSuggestion.sTrainingSet.size();

		String combo1 = Integer.toString(x) + Integer.toString(y);
		String combo2 = Integer.toString(y) + Integer.toString(x);

		// if combo1 is in the map, combo2 must be present as well
		if (mBiComboCount.containsKey(combo1)) {
			counter1 = mBiComboCount.get(combo1);
			counter2 = mBiComboCount.get(combo2);
		} else {
			for (int i = 0; i < size; i++) {
				String pattern = mCrossValidation ? CrossValidation.sTrainingSet.get(i) : PatternSuggestion.sTrainingSet.get(i);
				// if the pattern contains neither sequence, skip
				if (pattern.contains(combo1))
					counter1++;
				if (pattern.contains(combo2))
					counter2++;
			}

			mBiComboCount.put(combo1, counter1);
			mBiComboCount.put(combo2, counter2);
		}

		return counter1 + counter2;
	}

	/**
	 * Count the number of times a tri-combo has appeared in the training set
	 */
	public int triComboFreqCount(int a, int b, int c) {
		int size = mCrossValidation ? CrossValidation.sTrainingSet.size() : PatternSuggestion.sTrainingSet.size();

		int[] counters = new int[6];
		String[] combos = new String[6];
		String sa = Integer.toString(a);
		String sb = Integer.toString(b);
		String sc = Integer.toString(c);
		// all possible combinations with
		// these three digits (factorial)
		combos[0] = sa + sb + sc;
		combos[1] = sa + sc + sb;
		combos[2] = sb + sa + sc;
		combos[3] = sb + sc + sa;
		combos[4] = sc + sa + sb;
		combos[5] = sc + sb + sa;

		if (mTriComboCount.containsKey(combos[0])) {
			for (int i = 0; i < 6; i++) {
				counters[i] = mTriComboCount.get(combos[i]);
			}
		} else {
			for (int i = 0; i < size; i++) {
				String pattern = mCrossValidation ? CrossValidation.sTrainingSet.get(i) : PatternSuggestion.sTrainingSet.get(i);
				for (int j = 0; j < 6; j++) {
					if (pattern.contains(combos[j])) {
						counters[j]++;
					}
				}
			} // end for

			for (int i = 0; i < 6; i++) {
				mTriComboCount.put(combos[i], counters[i]);
			}
		}

		return counters[0] + counters[1] + counters[2] + counters[3] + counters[4] + counters[5];
	}

	/**
	 * Load training patterns from files
	 */
	public void readTrainingPatternsFromFiles() {
		loadPatternsFromTextFile();
		loadPatternsFromCSVFiles();
		// put all training patterns into the list
		getTrainingPatterns();
	}

	private void loadPatternsFromCSV1() {
		AssetManager assets = mCxt.getAssets();
		String separator = ";";
		String line = "";
		BufferedReader br = null;

		try {
			br = new BufferedReader(new InputStreamReader(assets.open("questionaire-.csv")));
			while ((line = br.readLine()) != null) {
				String[] parts = line.split(separator);
				if (parts.length == 15) {
					String pattern = parts[parts.length - 3].replace("\"", "");
					if (pattern.contains(",")) {
						List<String> temp = Arrays.asList(pattern.split(","));
						insertTrainingPattern(temp.get(0));
						insertTrainingPattern(temp.get(1));
					} else {
						insertTrainingPattern(pattern);
					}
				}
			}
		} catch (IOException e) {

		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {

			}
		}
	}

	private void loadPatternsFromCSV2() {
		AssetManager assets = mCxt.getAssets();
		String separator = ";";
		String line = "";
		BufferedReader br = null;

		try {
			// start reading 'simpleANDcomplicated.csv'
			separator = ",";
			br = new BufferedReader(new InputStreamReader(assets.open("simpleANDcomplicated.csv")));
			// skip the first line as there is no data
			br.readLine();
			while ((line = br.readLine()) != null) {
				String[] parts = line.split(separator);
				int counter = 0;
				for (int i = parts.length - 1; i != 0; i--) {
					// there are 2 patterns only
					if (counter == 2)
						break;
					String temp = parts[i];
					if (temp.matches("[0-9]+")) {
						insertTrainingPattern(temp);
						counter++;
					}
				}
			}
		} catch (IOException e) {

		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {

			}
		}
	}

	public void loadAllPatterns() {
		BufferedReader br = null;
		String line = "";
		AssetManager assets = mCxt.getAssets();

		try {
			br = new BufferedReader(new InputStreamReader(assets.open("all_patterns_compact.txt"), "UTF-8"));

			// start timing here
			long startTime = System.currentTimeMillis();

			// keep reading until 'line' becomes null
			while ((line = br.readLine()) != null) {
				Collections.addAll(PatternSuggestion.sPatterns, line.split(","));
			} // end while

			long stopTime = System.currentTimeMillis();
			Log.d("Finsihed loading patterns", "Time taken to load all patterns: " + ((stopTime - startTime) / 1000));
		} catch (IOException e) {
			Log.d("IO Exception", "An error occurred while reading pattern list");
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {

				}
			}
		}
	}

	/**
	 * Load CSV files. Store all patterns in the database NB: this method should
	 * only be called if the files are updated (which is unlikely) or the
	 * 'patterns' table is empty
	 */
	public void loadPatternsFromCSVFiles() {
		loadPatternsFromCSV1();
		loadPatternsFromCSV2();
	}
}