/*
 * Copyright (C) 2012-2014 Alessandro Lazzari
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.money.manager.ex.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.money.manager.ex.HelpActivity;
import com.money.manager.ex.MoneyManagerApplication;
import com.money.manager.ex.R;
import com.money.manager.ex.core.Core;
import com.money.manager.ex.core.RawFileUtils;
import com.money.manager.ex.dropbox.DropboxBrowserActivity;
import com.money.manager.ex.dropbox.DropboxHelper;
import com.money.manager.ex.dropbox.DropboxReceiver;
import com.money.manager.ex.dropbox.DropboxServiceIntent;
import com.money.manager.ex.fragment.BaseFragmentActivity;
import com.money.manager.ex.fragment.TipsDialogFragment;
import com.money.manager.ex.preferences.PreferencesConstant;

import java.io.File;

public class DropboxSettingsActivity extends BaseFragmentActivity {
    private static String LOGCAT = DropboxSettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new DropboxSettingsFragment()).commit();
    }

    public static class DropboxSettingsFragment extends PreferenceFragment {
        private static final int REQUEST_DROPBOX_FILE = 20;

        private DropboxHelper mDropboxHelper = null;
        private boolean mDropboxLoginBegin = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.dropbox_settings);
            PreferenceManager.getDefaultSharedPreferences(getActivity());

            final MoneyManagerApplication application = (MoneyManagerApplication) getActivity().getApplication();
            // dropbox preference screen
            mDropboxHelper = DropboxHelper.getInstance(getActivity().getApplicationContext());

            final PreferenceScreen pDropbox = (PreferenceScreen) findPreference(getString(PreferencesConstant.PREF_DROPBOX_HOWITWORKS));
            if (pDropbox != null) {
                pDropbox.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showWebTipsDialog(getString(PreferencesConstant.PREF_DROPBOX_HOWITWORKS), getString(R.string.dropbox_how_it_works), R.raw.help_dropbox, false);
                        return false;
                    }
                });
            }

            //login to dropbox
            final Preference pDropboxLink = findPreference(getString(PreferencesConstant.PREF_DROPBOX_LINK));
            pDropboxLink.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mDropboxHelper.logIn();
                    mDropboxLoginBegin = true;
                    return false;
                }
            });

            //logout from dropbox
            final Preference pDropboxUnlink = findPreference(getString(PreferencesConstant.PREF_DROPBOX_UNLINK));
            pDropboxUnlink.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mDropboxHelper.logOut();
                /* SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                sharedPreferences.edit().putString(PreferencesConstant.PREF_DROPBOX_TIMES_REPEAT, null).commit(); */
                    mDropboxHelper.sendBroadcastStartServiceScheduled(DropboxReceiver.ACTION_CANCEL);
                    // refresh ui
                    onResume();
                    return false;
                }
            });

            //wiki
            Preference pWiki = findPreference(getString(PreferencesConstant.PREF_DROPBOX_WIKI));
            if (pWiki != null) {
                pWiki.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        //startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://code.google.com/p/android-money-manager-ex/wiki/DropboxSync")));
                        Intent intent = new Intent(getActivity(), HelpActivity.class);
                        intent.setData(Uri.parse("android.resource://com.money.manager.ex/" + R.raw.help_dropbox));
                        //intent.setData(Uri.parse("http://code.google.com/p/android-money-manager-ex/wiki/DropboxSync"));
                        startActivity(intent);
                        return false;
                    }
                });
            }

            //link file
            final Preference pDropboxFile = findPreference(getString(PreferencesConstant.PREF_DROPBOX_LINKED_FILE));
            if (pDropboxFile != null) {
                pDropboxFile.setSummary(mDropboxHelper.getLinkedRemoteFile());
                // check if summary is null and
                if (TextUtils.isEmpty(pDropboxFile.getSummary())) {
                    pDropboxFile.setSummary(R.string.click_to_select_file_dropbox);
                }
                // open DropboxBrowse Activity
                pDropboxFile.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(getActivity(), DropboxBrowserActivity.class);
                        intent.putExtra(DropboxBrowserActivity.INTENT_DROBPOXFILE_PATH, mDropboxHelper.getLinkedRemoteFile());
                        startActivityForResult(intent, REQUEST_DROPBOX_FILE);
                        return false;
                    }
                });
            }

            //force download
            PreferenceScreen pDownload = (PreferenceScreen) findPreference(getString(PreferencesConstant.PREF_DROPBOX_DOWNLOAD));
            if (pDownload != null) {
                pDownload.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (!TextUtils.isEmpty(mDropboxHelper.getLinkedRemoteFile()))
                            downloadFileFromDropbox(mDropboxHelper.getLinkedRemoteFile());
                        return false;
                    }
                });
            }

            //force upload
            PreferenceScreen pUpload = (PreferenceScreen) findPreference(getString(PreferencesConstant.PREF_DROPBOX_UPLOAD));
            if (pUpload != null) {
                pUpload.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        String dropboxFile = mDropboxHelper.getLinkedRemoteFile();
                        if (TextUtils.isEmpty(dropboxFile)) {
                            dropboxFile = "/" + new File(MoneyManagerApplication.getDatabasePath(getActivity().getApplicationContext())).getName();
                        }
                        uploadFileFromDropbox(dropboxFile);

                        return false;
                    }
                });
            }

            //times repeat
            ListPreference pRepeats = (ListPreference) findPreference(getString(PreferencesConstant.PREF_DROPBOX_TIMES_REPEAT));
            if (pRepeats != null) {
                pRepeats.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        mDropboxHelper.sendBroadcastStartServiceScheduled(DropboxReceiver.ACTION_CANCEL);
                        mDropboxHelper.sendBroadcastStartServiceScheduled(DropboxReceiver.ACTION_START);
                        return true;
                    }
                });
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (resultCode == Activity.RESULT_OK && data != null) {
                final Preference pDropboxFile = findPreference(getString(PreferencesConstant.PREF_DROPBOX_LINKED_FILE));
                if (pDropboxFile != null) {
                    CharSequence oldFile = "", newFile;
                    if (!TextUtils.isEmpty(pDropboxFile.getSummary())) {
                        oldFile = pDropboxFile.getSummary();
                    }
                    newFile = data.getStringExtra(DropboxBrowserActivity.INTENT_DROBPOXFILE_PATH);

                    if (newFile == null) return;

                    // save value
                    mDropboxHelper.setLinkedRemoteFile(newFile.toString());
                    pDropboxFile.setSummary(newFile);
                    // check if files is modified
                    if (!oldFile.equals(newFile)) {
                        // force download file
                        downloadFileFromDropbox((String) newFile);
                    }
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            // complete process authentication
            if (mDropboxLoginBegin) {
                mDropboxHelper.completeAuthenticationDropbox();
                mDropboxHelper.sendBroadcastStartServiceScheduled(DropboxReceiver.ACTION_START);
                mDropboxLoginBegin = false;
            }

            // dropbox link and unlink
            if (findPreference(getString(PreferencesConstant.PREF_DROPBOX_LINK)) != null) {
                findPreference(getString(PreferencesConstant.PREF_DROPBOX_LINK)).setSelectable(!mDropboxHelper.isLinked());
                findPreference(getString(PreferencesConstant.PREF_DROPBOX_LINK)).setEnabled(!mDropboxHelper.isLinked());
            }
            if (findPreference(getString(PreferencesConstant.PREF_DROPBOX_UNLINK)) != null) {
                findPreference(getString(PreferencesConstant.PREF_DROPBOX_UNLINK)).setSelectable(mDropboxHelper.isLinked());
                findPreference(getString(PreferencesConstant.PREF_DROPBOX_UNLINK)).setEnabled(mDropboxHelper.isLinked());
            }
        }

        private void showWebTipsDialog(final String key, final CharSequence title, final int rawResources, boolean force) {
            if (!force) {
                if (getActivity().getSharedPreferences(TipsDialogFragment.PREF_DIALOG, 0).getBoolean(key, false))
                    return;
            }
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            // title and icons
            alertDialog.setTitle(title);
            // view body
            @SuppressLint("InflateParams")
            final LinearLayout view = (LinearLayout) LayoutInflater.from(getActivity()).inflate(R.layout.dialog_tips, null);
            // set invisible tips
            final TextView textTips = (TextView) view.findViewById(R.id.textViewTips);
            textTips.setVisibility(View.GONE);
            // set webView
            final WebView webTips = (WebView) view.findViewById(R.id.webViewTips);
            webTips.loadData(RawFileUtils.getRawAsString(getActivity().getApplicationContext(), rawResources), "text/html", "UTF-8");
            webTips.setVisibility(View.VISIBLE);

            final CheckBox checkDont = (CheckBox) view.findViewById(R.id.checkBoxDontShow);
            checkDont.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    getActivity().getSharedPreferences(TipsDialogFragment.PREF_DIALOG, 0).edit().putBoolean(key, isChecked).commit();
                }
            });
            // bug CheckBox object of Android
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                final double scale = getActivity().getResources().getDisplayMetrics().density;
                checkDont.setPadding(checkDont.getPaddingLeft() + (int) (40.0f * scale + 0.5f),
                        checkDont.getPaddingTop(),
                        checkDont.getPaddingRight(),
                        checkDont.getPaddingBottom());
            }
            alertDialog.setView(view);
            // set neutral button
            alertDialog.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            // set auto close to false
            alertDialog.setCancelable(false);
            // show dialog
            alertDialog.create().show();
        }

        private void uploadFileFromDropbox(String dropboxFile) {
            // compose intent to launch service for download
            Intent service = new Intent(getActivity().getApplicationContext(), DropboxServiceIntent.class);
            service.setAction(DropboxServiceIntent.INTENT_ACTION_UPLOAD);
            service.putExtra(DropboxServiceIntent.INTENT_EXTRA_LOCAL_FILE, MoneyManagerApplication.getDatabasePath(getActivity().getApplicationContext()));
            service.putExtra(DropboxServiceIntent.INTENT_EXTRA_REMOTE_FILE, dropboxFile);
            // toast to show
            Toast.makeText(getActivity().getApplicationContext(), R.string.dropbox_upload_is_starting, Toast.LENGTH_LONG).show();
            // start service
            getActivity().startService(service);
        }

        private void downloadFileFromDropbox(String fileDropbox) {
            Core core = new Core(getActivity().getApplicationContext());
            // compose intent to lauch service for download
            Intent service = new Intent(getActivity().getApplicationContext(), DropboxServiceIntent.class);
            service.setAction(DropboxServiceIntent.INTENT_ACTION_DOWNLOAD);
            service.putExtra(DropboxServiceIntent.INTENT_EXTRA_LOCAL_FILE, core.getExternalStorageDirectoryDropboxApplication().getPath() + fileDropbox);
            service.putExtra(DropboxServiceIntent.INTENT_EXTRA_REMOTE_FILE, fileDropbox);
            // toast to show
            Toast.makeText(getActivity().getApplicationContext(), R.string.dropbox_download_is_starting, Toast.LENGTH_LONG).show();
            // start service
            getActivity().startService(service);
        }
    }
}
