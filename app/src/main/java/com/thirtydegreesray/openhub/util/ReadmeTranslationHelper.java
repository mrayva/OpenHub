package com.thirtydegreesray.openhub.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thirtydegreesray.openhub.AppApplication;
import com.thirtydegreesray.openhub.R;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * On-device README translation (ML Kit Translate + Language ID - free, no
 * API key, no network beyond a one-time per-language model download that ML
 * Kit caches itself). Operates on the already-fetched, GitHub-rendered
 * README HTML (RepoInfoPresenter.readmeSource) rather than raw markdown:
 * GitHub's own markdown rendering has already resolved every edge case
 * (tables, footnotes, task lists, etc.) into plain HTML, which Jsoup can
 * walk reliably - re-deriving the same resilience for raw markdown syntax
 * would mean reimplementing a chunk of a markdown parser.
 *
 * Translates text NODES in place (via Jsoup), not the serialized HTML
 * string as a whole - ML Kit's model is a plain-text sentence translator
 * with no awareness of markup, so handing it raw HTML would translate tag
 * names and attributes right along with the content and likely corrupt the
 * structure. Walking the DOM keeps every tag/attribute/link untouched and
 * only swaps the human-readable text inside them, so the translated result
 * re-renders through the exact same CodeWebView/HtmlHelper pipeline
 * (styling, dark mode, syntax-highlighted code blocks) as the original.
 * <script>/<style>/<code>/<pre> subtrees are skipped outright - code
 * samples and shell commands are not meant to be translated.
 */
public class ReadmeTranslationHelper {

    private static final Set<String> SKIP_TAGS = new HashSet<>(Arrays.asList("script", "style", "code", "pre"));

    public interface LanguageDetectedCallback {
        /**
         * @param sourceLanguageCode an ML Kit TranslateLanguage constant if
         *                           translation is worth offering (a real,
         *                           supported, non-English/non-device-locale
         *                           language was confidently detected), or
         *                           null if no translate option should be
         *                           shown at all.
         */
        void onLanguageDetected(@Nullable String sourceLanguageCode);
    }

    public interface TranslateCallback {
        /** translate() hands back translated HTML; translatePlainText() hands back a plain translated string. */
        void onTranslated(@NonNull String translated);

        void onError(@NonNull String message);
    }

    /**
     * Identifies the README's language from a plain-text sample (ML Kit's
     * identifier wants running prose, not markup) and decides whether a
     * translate option is worth showing at all - skipped entirely for
     * undetermined text, English, the device's own language, or a language
     * ML Kit's translator doesn't support.
     */
    public static void detectLanguage(@NonNull String htmlSource, @NonNull LanguageDetectedCallback callback) {
        String sample = plainTextSample(htmlSource);
        if (StringUtils.isBlank(sample)) {
            callback.onLanguageDetected(null);
            return;
        }
        LanguageIdentification.getClient().identifyLanguage(sample)
                .addOnSuccessListener(languageCode -> callback.onLanguageDetected(resolveSourceLanguage(languageCode)))
                .addOnFailureListener(e -> callback.onLanguageDetected(null));
    }

    @Nullable
    private static String resolveSourceLanguage(@Nullable String identifiedLanguageCode) {
        if (identifiedLanguageCode == null || "und".equals(identifiedLanguageCode)) return null;
        String mlkitSource = TranslateLanguage.fromLanguageTag(identifiedLanguageCode);
        if (mlkitSource == null) return null;
        String mlkitTarget = TranslateLanguage.fromLanguageTag(Locale.getDefault().getLanguage());
        if (mlkitTarget == null || mlkitSource.equals(mlkitTarget)) return null;
        return mlkitSource;
    }

    private static String plainTextSample(String htmlSource) {
        try {
            String text = Jsoup.parseBodyFragment(htmlSource).body().text();
            return text.length() > 1000 ? text.substring(0, 1000) : text;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * sourceLanguageCode must be an ML Kit TranslateLanguage constant, i.e.
     * exactly what detectLanguage() handed back - not re-validated here.
     */
    public static void translate(@NonNull String htmlSource, @NonNull String sourceLanguageCode,
                                  @NonNull TranslateCallback callback) {
        getReadyTranslator(sourceLanguageCode, callback,
                translator -> translateHtml(translator, htmlSource, callback));
    }

    /**
     * For short plain-text fields (e.g. a repo's one-line description) -
     * no Jsoup/DOM walk needed, just the model download + a single
     * translate() call.
     */
    public static void translatePlainText(@NonNull String text, @NonNull String sourceLanguageCode,
                                           @NonNull TranslateCallback callback) {
        getReadyTranslator(sourceLanguageCode, callback, translator ->
                translator.translate(text)
                        .addOnSuccessListener(translated -> {
                            translator.close();
                            callback.onTranslated(translated);
                        })
                        .addOnFailureListener(e -> {
                            translator.close();
                            callback.onError(AppApplication.get().getString(R.string.translate_failed));
                        }));
    }

    private interface ReadyTranslatorCallback {
        void onReady(Translator translator);
    }

    private static void getReadyTranslator(@NonNull String sourceLanguageCode, @NonNull TranslateCallback callback,
                                            @NonNull ReadyTranslatorCallback onReady) {
        String targetLanguageCode = TranslateLanguage.fromLanguageTag(Locale.getDefault().getLanguage());
        if (targetLanguageCode == null) {
            callback.onError(AppApplication.get().getString(R.string.translate_not_available));
            return;
        }

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageCode)
                .setTargetLanguage(targetLanguageCode)
                .build();
        final Translator translator = Translation.getClient(options);

        // No Wi-Fi requirement: the per-language model is a few MB,
        // downloaded once and cached by ML Kit for every future use (this
        // repo's README and every other one) - requiring Wi-Fi here would
        // silently stall mobile-only users on every repo they open.
        DownloadConditions conditions = new DownloadConditions.Builder().build();
        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> onReady.onReady(translator))
                .addOnFailureListener(e -> {
                    translator.close();
                    callback.onError(AppApplication.get().getString(R.string.translate_failed));
                });
    }

    private static void translateHtml(Translator translator, String htmlSource, TranslateCallback callback) {
        Document doc;
        try {
            doc = Jsoup.parseBodyFragment(htmlSource);
        } catch (Exception e) {
            translator.close();
            callback.onError(AppApplication.get().getString(R.string.translate_failed));
            return;
        }

        List<TextNode> textNodes = new ArrayList<>();
        collectTranslatableTextNodes(doc.body(), textNodes);

        if (textNodes.isEmpty()) {
            translator.close();
            callback.onTranslated(htmlSource);
            return;
        }

        AtomicInteger remaining = new AtomicInteger(textNodes.size());
        AtomicBoolean anyFailed = new AtomicBoolean(false);
        for (TextNode node : textNodes) {
            translator.translate(node.text())
                    .addOnSuccessListener(translated -> {
                        node.text(translated);
                        if (remaining.decrementAndGet() == 0) {
                            finish(translator, doc, callback, anyFailed.get());
                        }
                    })
                    .addOnFailureListener(e -> {
                        // One node's translation failing is not worth
                        // discarding the whole README - that node just
                        // keeps its original-language text.
                        anyFailed.set(true);
                        if (remaining.decrementAndGet() == 0) {
                            finish(translator, doc, callback, anyFailed.get());
                        }
                    });
        }
    }

    private static void finish(Translator translator, Document doc, TranslateCallback callback, boolean anyFailed) {
        translator.close();
        callback.onTranslated(doc.body().html());
    }

    private static void collectTranslatableTextNodes(Element element, List<TextNode> out) {
        if (SKIP_TAGS.contains(element.tagName())) return;
        for (Node child : element.childNodes()) {
            if (child instanceof TextNode) {
                TextNode textNode = (TextNode) child;
                if (!StringUtils.isBlank(textNode.text())) {
                    out.add(textNode);
                }
            } else if (child instanceof Element) {
                collectTranslatableTextNodes((Element) child, out);
            }
        }
    }

}
