package com.thirtydegreesray.openhub.util;

import androidx.annotation.Nullable;

import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Description-based predicates for the "hide repos without a description" /
 * "hide repos not in English or the system language" list filters.
 *
 * Language detection uses the same on-device ML Kit identifier as
 * ReadmeTranslationHelper. The filters run synchronously on whichever thread
 * the list presenter is on (often main), where Tasks.await is forbidden and
 * a default-executor listener would deadlock, so this waits on a latch fed
 * by a direct-executor listener (fires on ML Kit's own thread) instead.
 * Short blurbs often come back "und" (undetermined) - those are KEPT, as is
 * anything that errors or times out, so the filter only ever hides text it
 * is reasonably sure is in some other language.
 */
public class RepoDescriptionFilter {

    private static final int MAX_CACHE = 2000;
    private static final long TIMEOUT_MS = 1500;

    private static final ConcurrentHashMap<String, Boolean> CACHE = new ConcurrentHashMap<>();
    private static LanguageIdentifier identifier;

    public static boolean hasDescription(@Nullable String description) {
        return description != null && !description.trim().isEmpty();
    }

    /**
     * True when the text is English, the device language, or can't be
     * identified. Blank text counts as acceptable (that's the other
     * filter's business).
     */
    public static boolean isEnglishOrSystemLanguage(@Nullable String description) {
        if (!hasDescription(description)) return true;
        String text = description.trim();
        Boolean cached = CACHE.get(text);
        if (cached != null) return cached;

        boolean keep = true;
        try {
            final String[] result = new String[1];
            final CountDownLatch latch = new CountDownLatch(1);
            getIdentifier().identifyLanguage(text).addOnCompleteListener(Runnable::run, task -> {
                if (task.isSuccessful()) result[0] = task.getResult();
                latch.countDown();
            });
            if (latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                String code = result[0];
                if (code != null && !"und".equals(code)) {
                    keep = "en".equals(code)
                            || code.equals(Locale.getDefault().getLanguage());
                }
                if (CACHE.size() >= MAX_CACHE) CACHE.clear();
                CACHE.put(text, keep);
            }
        } catch (Exception e) {
            // fail open
        }
        return keep;
    }

    private static synchronized LanguageIdentifier getIdentifier() {
        if (identifier == null) identifier = LanguageIdentification.getClient();
        return identifier;
    }
}
