package com.thirtydegreesray.openhub.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.google.gson.Gson;
import com.thirtydegreesray.openhub.AppApplication;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.IgnoredRepo;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import es.dmoral.toasty.Toasty;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * One-tap "export ignore list to the same file every time" shortcut for the
 * toolbar icon on Trending/Created (menu_trending.xml, shared by both) -
 * distinct from IgnoredReposActivity's own Export menu item, which always
 * opens the system file picker. ACTION_CREATE_DOCUMENT has no "overwrite
 * this existing document" concept, so picking the same display name there
 * twice creates a second file with a disambiguating suffix rather than
 * overwriting - confirmed as expected SAF behavior, not a bug, but not what
 * this shortcut is for.
 *
 * The very first tap (from either entry point) still needs the file picker
 * once, to obtain a real document URI - after that, the chosen URI is
 * persisted with a durable grant (takePersistableUriPermission(), since
 * without it the write permission wouldn't survive the app process being
 * killed) and every later quick-export tap writes straight to it with zero
 * prompts, opening the stream in truncating "wt" mode so each write is a
 * true overwrite rather than an append.
 */
public class IgnoreListExportHelper {

    public static final int REQUEST_CODE = 950;

    private static String getRememberedUriString() {
        return PrefUtils.getDefaultSp(AppApplication.get())
                .getString(PrefUtils.IGNORE_LIST_QUICK_EXPORT_URI, null);
    }

    /**
     * Entry point for the toolbar shortcut. Writes straight to the
     * remembered destination with no prompt if one exists; otherwise
     * launches the system file picker - the caller's onActivityResult must
     * forward a matching result (requestCode == REQUEST_CODE) to
     * onExportLocationChosen().
     */
    public static void quickExport(Activity activity) {
        String rememberedUriString = getRememberedUriString();
        if (rememberedUriString == null) {
            launchPicker(activity);
            return;
        }
        writeTo(activity, Uri.parse(rememberedUriString), true);
    }

    private static void launchPicker(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "openhub_ignore_list.json");
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    public static void onExportLocationChosen(Activity activity, Uri uri) {
        try {
            activity.getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // A handful of document providers don't support persistable
            // grants - this one write still succeeds, it just won't survive
            // a process restart, at which point quickExport() transparently
            // falls back to prompting again instead of failing outright.
        }
        PrefUtils.getDefaultSp(AppApplication.get()).edit()
                .putString(PrefUtils.IGNORE_LIST_QUICK_EXPORT_URI, uri.toString())
                .apply();
        writeTo(activity, uri, false);
    }

    private static void writeTo(Activity activity, Uri uri, boolean rememberedFromBefore) {
        IgnoredRepoHelper.getAllForExport()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(repos -> {
                    if (repos.isEmpty()) {
                        Toasty.warning(activity, activity.getString(R.string.ignore_list_empty_export)).show();
                        return;
                    }
                    Observable.fromCallable(() -> writeJson(activity, uri, repos))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    count -> Toasty.success(activity, String.format(
                                            activity.getString(R.string.export_ignore_list_success), count)).show(),
                                    error -> {
                                        // The remembered destination is no longer
                                        // writable (file moved/deleted, permission
                                        // revoked) - clear it so the next tap
                                        // prompts fresh instead of failing the
                                        // same way forever.
                                        if (rememberedFromBefore) {
                                            PrefUtils.getDefaultSp(AppApplication.get()).edit()
                                                    .remove(PrefUtils.IGNORE_LIST_QUICK_EXPORT_URI)
                                                    .apply();
                                        }
                                        Toasty.error(activity, activity.getString(R.string.export_ignore_list_failed)).show();
                                    }
                            );
                }, error -> Toasty.error(activity, activity.getString(R.string.export_ignore_list_failed)).show());
    }

    private static int writeJson(Activity activity, Uri uri, ArrayList<IgnoredRepo> repos) throws IOException {
        String json = new Gson().toJson(repos);
        OutputStream os = activity.getContentResolver().openOutputStream(uri, "wt");
        if (os == null) throw new IOException("Could not open output stream for " + uri);
        try {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        } finally {
            os.close();
        }
        return repos.size();
    }

}
