package com.thirtydegreesray.openhub.util;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thirtydegreesray.openhub.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Global uncaught-exception logger. This app has no crash-reporting service
 * wired up, so an unhandled crash otherwise leaves no trace anywhere the
 * user (or whoever's helping them) can see afterward - just writes each
 * crash's stack trace to a capped set of files under filesDir/crashes/,
 * then always defers to whatever the previous default handler was (usually
 * the platform's, which shows the normal "app has stopped" dialog and kills
 * the process) - this class only observes a crash, never swallows one.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String CRASH_DIR = "crashes";
    private static final int MAX_CRASH_FILES = 20;

    private static CrashHandler instance;

    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final Context appContext;

    private CrashHandler(Context context) {
        this.appContext = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void install(@NonNull Context context) {
        if (instance != null) return;
        instance = new CrashHandler(context);
        Thread.setDefaultUncaughtExceptionHandler(instance);
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
        try {
            // Checked here (not just at install() time) so toggling the
            // Settings switch takes effect immediately, without needing an
            // app restart to re-install/uninstall the handler.
            if (PrefUtils.isCrashLoggingEnable()) {
                saveCrash(ex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, ex);
        } else {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    private void saveCrash(Throwable ex) {
        File dir = new File(appContext.getFilesDir(), CRASH_DIR);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, "crash_" + System.currentTimeMillis() + ".txt");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        pw.println("Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        pw.println();
        ex.printStackTrace(pw);
        pw.flush();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(sw.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        trimOldCrashFiles(dir);
    }

    private void trimOldCrashFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null || files.length <= MAX_CRASH_FILES) return;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        int toDelete = files.length - MAX_CRASH_FILES;
        for (int i = 0; i < toDelete; i++) {
            files[i].delete();
        }
    }

    @Nullable
    public static File getLastCrashFile(@NonNull Context context) {
        File dir = new File(context.getApplicationContext().getFilesDir(), CRASH_DIR);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return null;
        File newest = files[0];
        for (File file : files) {
            if (file.lastModified() > newest.lastModified()) newest = file;
        }
        return newest;
    }

    public static void clearCrashFiles(@NonNull Context context) {
        File dir = new File(context.getApplicationContext().getFilesDir(), CRASH_DIR);
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            file.delete();
        }
    }

    /**
     * Manual read loop rather than InputStream.readAllBytes() - that's only
     * available from API 33, and this app's minSdk is 21.
     */
    @NonNull
    public static String readCrashFile(@NonNull File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            return "";
        }
    }

}
