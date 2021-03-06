/* 
 * Copyright (C) 2009 Roman Masek
 * 
 * This file is part of OpenSudoku.
 * 
 * OpenSudoku is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * OpenSudoku is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with OpenSudoku.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package cz.romario.opensudoku.gui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

import com.fairket.app.opensudoku.R;
import com.fairket.sdk.android.FairketApiClient;
import com.fairket.sdk.android.FairketAppTimeHelper;
import com.fairket.sdk.android.FairketResult;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.tapjoy.TapjoyConnect;

import cz.romario.opensudoku.db.FolderColumns;
import cz.romario.opensudoku.db.SudokuDatabase;
import cz.romario.opensudoku.game.FolderInfo;
import cz.romario.opensudoku.gui.FolderDetailLoader.FolderDetailCallback;
import cz.romario.opensudoku.utils.AndroidUtils;

/**
 * List of puzzle's folder. This activity also serves as root activity of
 * application.
 * 
 * @author romario
 */
public class FolderListActivity extends ListActivity {

	public static final String FAIRKET_LOG = "SUDOKU-FairketApiClient";

	// Dev server key
	 public static final String FAIRKET_APP_PUB_KEY =
	 "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAy5m7I8+f4O+9G0d8QVITG79fJzWoEFcu5IQtLlLwbv82d5dVvs6dbWigTLgr2Z6LdydoLTEaoWFAm+6oiMrcnEELfUh0hhQGZ7ACntA0+ogcEBKJaCWV9LouwLHRj6M1a9Ig/O40irDrq6G/+p7ZKnG5xhZuElSqMXR8cgIf2QNko6bjMGgo97wt0YKaoyNalK/HpcgSyUVjwFGLnKvxddz57Ojino59e8dXNOAJPeyAn8c5OkDIE5bRoXiZvWFTL3Ir9p3Ih4Gn6mqTgT2LJdTFcxd8qbbbAbSWN/ppzjeI/vSqf7Hp37GwZiNpYyCQuBWEQ0lVoRm9V99IhLAfDQIDAQAB";
	// Prod server key
//	public static final String FAIRKET_APP_PUB_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhbrBvLv0jObVMp5gojh3Pz95L15SAo3Va0xD+gT+Q4G2f3O13TY7b5XpQUw8QmIGt/UWg0tMDr/VeG8qOpBcIzbpp4brYrhvNnycnVL5+Q4nrcfN4VNaiXHJF88za4rcHWfyXh40DwQ0PZEq6TruCVaP7zpHvk2ymMud9n4y4kYF0sR/Rv/1VV+Sv7XWVceM/bVw7TIazzUJHgmRSFYBXauJ5XHD4i59tG6s8TsLF6ZxiCQlVEQ7frvGBJBsh28gj+jwXpnYFIfaQo7+l0kwCBh/vsOkITj8cBGoqlyg28uKBXI+/UXVMi8vUFos06lp4qida/2PmjqRXP+sqpzE7QIDAQAB";

	private FairketApiClient mFairket;

	public static final int MENU_ITEM_ADD = Menu.FIRST;
	public static final int MENU_ITEM_RENAME = Menu.FIRST + 1;
	public static final int MENU_ITEM_DELETE = Menu.FIRST + 2;
	public static final int MENU_ITEM_ABOUT = Menu.FIRST + 3;
	public static final int MENU_ITEM_EXPORT = Menu.FIRST + 4;
	public static final int MENU_ITEM_EXPORT_ALL = Menu.FIRST + 5;
	public static final int MENU_ITEM_IMPORT = Menu.FIRST + 6;

	private static final int DIALOG_ABOUT = 0;
	private static final int DIALOG_ADD_FOLDER = 1;
	private static final int DIALOG_RENAME_FOLDER = 2;
	private static final int DIALOG_DELETE_FOLDER = 3;

	private static final String TAG = "FolderListActivity";

	private Cursor mCursor;
	private SudokuDatabase mDatabase;
	private FolderListViewBinder mFolderListBinder;

	// input parameters for dialogs
	private TextView mAddFolderNameInput;
	private TextView mRenameFolderNameInput;
	private long mRenameFolderID;
	private long mDeleteFolderID;

	private Button mBtnRemoveAds;

	// private Button mBtnAdsHere;

	private InterstitialAd interstitial;

	private AdView mAdView;
	private boolean isFairketFreePlan;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.folder_list);
		mBtnRemoveAds = (Button) findViewById(R.id.btnRemoveAds);
		// mBtnAdsHere = (Button) findViewById(R.id.btnAdsHere);

		setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
		// Inform the list we provide context menus for items
		getListView().setOnCreateContextMenuListener(this);

		mBtnRemoveAds.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Intent intent = new Intent(
				// Intent.ACTION_VIEW,
				// Uri.parse("http://code.google.com/p/opensudoku-android/wiki/Puzzles"));
				// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				// startActivity(intent);

				mFairket.startPlanSubscribeFlow(FolderListActivity.this,
						FairketApiClient.PRODUCT_APPTIME,
						new FairketApiClient.OnPlanSubscribeListener() {

							@Override
							public void onPlanSubscribeFinished(
									FairketResult result) {
								fairketChkForFreePlan();
							}
						});
			}
		});

		mDatabase = new SudokuDatabase(getApplicationContext());
		mCursor = mDatabase.getFolderList();
		startManagingCursor(mCursor);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.folder_list_item, mCursor, new String[] {
						FolderColumns.NAME, FolderColumns._ID }, new int[] {
						R.id.name, R.id.detail });
		mFolderListBinder = new FolderListViewBinder(this);
		adapter.setViewBinder(mFolderListBinder);

		setListAdapter(adapter);

		// show changelog on first run
		Changelog changelog = new Changelog(this);
		changelog.showOnFirstRun();

		// FairketApiClient Integration
		mFairket = FairketAppTimeHelper.onCreate(this,
				FolderListActivity.FAIRKET_APP_PUB_KEY,
				FolderListActivity.FAIRKET_LOG);

		// Look up the AdView as a resource and load a request.
		mAdView = (AdView) this.findViewById(R.id.adView);
		AdRequest adRequest = new AdRequest.Builder().build();
		mAdView.loadAd(adRequest);

		// Create the interstitial.
		interstitial = new InterstitialAd(this);
		interstitial.setAdUnitId("ca-app-pub-6299089167657957/2573058620");

		// Create ad request.
		adRequest = new AdRequest.Builder().build();

		// Begin loading your interstitial.
		interstitial.loadAd(adRequest);

		// Tapjoy connect
		String tapjoyAppID = "6d0adcda-a592-4dd0-b97b-733aaa313e2d";
		String tapjoySecretKey = "zfeaQ9qz7gcUedicUG0D";
		TapjoyConnect.requestTapjoyConnect(this, tapjoyAppID, tapjoySecretKey);

	}

	// Invoke displayInterstitial() when you are ready to display an
	// interstitial.
	public void displayInterstitial() {
		if (interstitial.isLoaded()) {
			interstitial.show();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		System.out.println("Sudoku start");
		updateList();
	}

	@Override
	protected void onResume() {
		super.onResume();

		System.out.println("Sudoku resume");
		FairketAppTimeHelper.onResume(mFairket,
				new FairketApiClient.OnInitializeListener() {

					@Override
					public void onInitializeFinished(FairketResult result) {
						if (result.isSuccess()) {
							fairketChkForFreePlan();
						}
					}

				});
	}

	private void fairketChkForFreePlan() {
		// Check if the subscribed to free plan
		mFairket.isFreePlanSubscribedAsync(FairketApiClient.PRODUCT_APPTIME,
				new FairketApiClient.OnAsyncOperationListener() {

					@Override
					public void onAsyncOperationFinished(FairketResult result) {
						Log.d(FAIRKET_LOG, "isFreePlanSubscribedAsync: "
								+ result.isSuccess());
						int visibility;
						if (result.isSuccess()) {
							isFairketFreePlan = true;
							visibility = View.VISIBLE;
						} else {
							isFairketFreePlan = false;
							visibility = View.GONE;
						}
						mAdView.setVisibility(visibility);
						mBtnRemoveAds.setVisibility(visibility);
					}
				});
	}

	@Override
	protected void onPause() {
		super.onPause();

		FairketAppTimeHelper.onPause(mFairket);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDatabase.close();
		mFolderListBinder.destroy();

		FairketAppTimeHelper.onDestroy(mFairket);

		if (isFairketFreePlan) {
			displayInterstitial();
		}

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putLong("mRenameFolderID", mRenameFolderID);
		outState.putLong("mDeleteFolderID", mDeleteFolderID);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);

		mRenameFolderID = state.getLong("mRenameFolderID");
		mDeleteFolderID = state.getLong("mDeleteFolderID");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// This is our one standard application action -- inserting a
		// new note into the list.
		menu.add(0, MENU_ITEM_ADD, 0, R.string.add_folder)
				.setShortcut('3', 'a').setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, MENU_ITEM_IMPORT, 0, R.string.import_file)
				.setShortcut('8', 'i')
				.setIcon(android.R.drawable.ic_menu_upload);
		menu.add(0, MENU_ITEM_EXPORT_ALL, 1, R.string.export_all_folders)
				.setShortcut('7', 'e')
				.setIcon(android.R.drawable.ic_menu_share);
		menu.add(0, MENU_ITEM_ABOUT, 2, R.string.about).setShortcut('1', 'h')
				.setIcon(android.R.drawable.ic_menu_info_details);

		// Generate any additional actions that can be performed on the
		// overall list. In a normal install, there are no additional
		// actions found here, but this allows other applications to extend
		// our menu with their own actions.
		Intent intent = new Intent(null, getIntent().getData());
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
				new ComponentName(this, FolderListActivity.class), null,
				intent, 0, null);

		return true;

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			Log.e(TAG, "bad menuInfo", e);
			return;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			// For some reason the requested item isn't available, do nothing
			return;
		}
		menu.setHeaderTitle(cursor.getString(cursor
				.getColumnIndex(FolderColumns.NAME)));

		menu.add(0, MENU_ITEM_EXPORT, 0, R.string.export_folder);
		menu.add(0, MENU_ITEM_RENAME, 1, R.string.rename_folder);
		menu.add(0, MENU_ITEM_DELETE, 2, R.string.delete_folder);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		LayoutInflater factory = LayoutInflater.from(this);

		switch (id) {
		case DIALOG_ABOUT:
			final View aboutView = factory.inflate(R.layout.about, null);
			TextView versionLabel = (TextView) aboutView
					.findViewById(R.id.version_label);
			String versionName = AndroidUtils
					.getAppVersionName(getApplicationContext());
			versionLabel.setText(getString(R.string.version, versionName));
			return new AlertDialog.Builder(this)
					.setIcon(R.drawable.opensudoku_logo_72)
					.setTitle(R.string.app_name).setView(aboutView)
					.setPositiveButton("OK", null).create();
		case DIALOG_ADD_FOLDER:
			View addFolderView = factory.inflate(R.layout.folder_name, null);
			mAddFolderNameInput = (TextView) addFolderView
					.findViewById(R.id.name);
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_menu_add)
					.setTitle(R.string.add_folder)
					.setView(addFolderView)
					.setPositiveButton(R.string.save,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									mDatabase.insertFolder(mAddFolderNameInput
											.getText().toString().trim(),
											System.currentTimeMillis());
									updateList();
								}
							}).setNegativeButton(android.R.string.cancel, null)
					.create();
		case DIALOG_RENAME_FOLDER:
			final View renameFolderView = factory.inflate(R.layout.folder_name,
					null);
			mRenameFolderNameInput = (TextView) renameFolderView
					.findViewById(R.id.name);

			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_menu_edit)
					.setTitle(R.string.rename_folder_title)
					.setView(renameFolderView)
					.setPositiveButton(R.string.save,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									mDatabase.updateFolder(mRenameFolderID,
											mRenameFolderNameInput.getText()
													.toString().trim());
									updateList();
								}
							}).setNegativeButton(android.R.string.cancel, null)
					.create();
		case DIALOG_DELETE_FOLDER:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_delete)
					.setTitle(R.string.delete_folder_title)
					.setMessage(R.string.delete_folder_confirm)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// TODO: this could take a while, I should
									// show progress dialog
									mDatabase.deleteFolder(mDeleteFolderID);
									updateList();
								}
							}).setNegativeButton(android.R.string.no, null)
					.create();

		}

		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);

		switch (id) {
		case DIALOG_ADD_FOLDER:
			break;
		case DIALOG_RENAME_FOLDER: {
			FolderInfo folder = mDatabase.getFolderInfo(mRenameFolderID);
			String folderName = folder != null ? folder.name : "";
			dialog.setTitle(getString(R.string.rename_folder_title, folderName));
			mRenameFolderNameInput.setText(folderName);
			break;
		}
		case DIALOG_DELETE_FOLDER: {
			FolderInfo folder = mDatabase.getFolderInfo(mDeleteFolderID);
			String folderName = folder != null ? folder.name : "";
			dialog.setTitle(getString(R.string.delete_folder_title, folderName));
			break;
		}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			Log.e(TAG, "bad menuInfo", e);
			return false;
		}

		switch (item.getItemId()) {
		case MENU_ITEM_EXPORT:
			Intent intent = new Intent();
			intent.setClass(this, SudokuExportActivity.class);
			intent.putExtra(SudokuExportActivity.EXTRA_FOLDER_ID, info.id);
			startActivity(intent);
			return true;
		case MENU_ITEM_RENAME:
			mRenameFolderID = info.id;
			showDialog(DIALOG_RENAME_FOLDER);
			return true;
		case MENU_ITEM_DELETE:
			mDeleteFolderID = info.id;
			showDialog(DIALOG_DELETE_FOLDER);
			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case MENU_ITEM_ADD:
			showDialog(DIALOG_ADD_FOLDER);
			return true;
		case MENU_ITEM_IMPORT:
			intent = new Intent();
			intent.setClass(this, FileListActivity.class);
			intent.putExtra(FileListActivity.EXTRA_FOLDER_NAME, "/sdcard");
			startActivity(intent);
			return true;
		case MENU_ITEM_EXPORT_ALL:
			intent = new Intent();
			intent.setClass(this, SudokuExportActivity.class);
			intent.putExtra(SudokuExportActivity.EXTRA_FOLDER_ID,
					SudokuExportActivity.ALL_FOLDERS);
			startActivity(intent);
			return true;
		case MENU_ITEM_ABOUT:
			showDialog(DIALOG_ABOUT);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent i = new Intent(this, SudokuListActivity.class);
		i.putExtra(SudokuListActivity.EXTRA_FOLDER_ID, id);
		startActivity(i);
	}

	private void updateList() {
		mCursor.requery();
	}

	private static class FolderListViewBinder implements ViewBinder {
		private Context mContext;
		private FolderDetailLoader mDetailLoader;

		public FolderListViewBinder(Context context) {
			mContext = context;
			mDetailLoader = new FolderDetailLoader(context);
		}

		@Override
		public boolean setViewValue(View view, Cursor c, int columnIndex) {

			switch (view.getId()) {
			case R.id.name:
				((TextView) view).setText(c.getString(columnIndex));
				break;
			case R.id.detail:
				final long folderID = c.getLong(columnIndex);
				final TextView detailView = (TextView) view;
				detailView.setText(mContext.getString(R.string.loading));
				mDetailLoader.loadDetailAsync(folderID,
						new FolderDetailCallback() {
							@Override
							public void onLoaded(FolderInfo folderInfo) {
								if (folderInfo != null)
									detailView.setText(folderInfo
											.getDetail(mContext));
							}
						});
			}

			return true;
		}

		public void destroy() {
			mDetailLoader.destroy();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		try {
			mFairket.onActivityResult(requestCode, resultCode, data);
		} catch (Exception e) {
			Log.wtf(FAIRKET_LOG, e);
		}
	}

}
