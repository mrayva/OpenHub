package com.thirtydegreesray.openhub.util;

import android.content.Context;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.dao.MyTrendingLanguage;
import com.thirtydegreesray.openhub.dao.MyTrendingLanguageDao;
import com.thirtydegreesray.openhub.mvp.model.TrendingLanguage;

import java.util.ArrayList;
import java.util.List;

/**
 * The user's curated/sorted language list (managed via LanguagesEditorActivity)
 * is shared by every screen offering a language filter - Trending, Created
 * and My Topics all read/persist the same list rather than each keeping its
 * own copy. Extracted from TrendingPresenter, which originally owned this.
 */
public class TrendingLanguageHelper {

    public static ArrayList<TrendingLanguage> getLanguagesFromLocal(
            DaoSession daoSession, Context context) {
        List<MyTrendingLanguage> myLanguages = daoSession.getMyTrendingLanguageDao().queryBuilder()
                .orderAsc(MyTrendingLanguageDao.Properties.Order)
                .list();
        ArrayList<TrendingLanguage> languages;
        if (StringUtils.isBlankList(myLanguages)) {
            languages = JSONUtils.jsonToArrayList(
                    context.getString(R.string.trending_languages), TrendingLanguage.class);
            languages.addAll(0, getFixedLanguages(context));
            languages = sortLanguages(languages);
        } else {
            languages = TrendingLanguage.generateFromDB(myLanguages);
            fixFixedLanguagesName(languages, context);
        }
        fixLanguagesSlug(languages);
        return languages;
    }

    private static ArrayList<TrendingLanguage> sortLanguages(ArrayList<TrendingLanguage> languages) {
        for (int i = 0; i < languages.size(); i++) {
            languages.get(i).setOrder(i + 1);
        }
        return languages;
    }

    private static ArrayList<TrendingLanguage> getFixedLanguages(Context context) {
        ArrayList<TrendingLanguage> fixedLanguages = new ArrayList<>();
        fixedLanguages.add(new TrendingLanguage(context.getString(R.string.all_languages), "all"));
        fixedLanguages.add(new TrendingLanguage(context.getString(R.string.unknown_languages), "unknown"));
        return fixedLanguages;
    }

    private static void fixFixedLanguagesName(ArrayList<TrendingLanguage> languages, Context context) {
        for (TrendingLanguage language : languages) {
            if (language.getSlug().equals("all")) {
                language.setName(context.getString(R.string.all_languages));
            } else if (language.getSlug().equals("unknown")) {
                language.setName(context.getString(R.string.unknown_languages));
            }
        }
    }

    private static void fixLanguagesSlug(ArrayList<TrendingLanguage> languages) {
        for (TrendingLanguage language : languages) {
            String slug = language.getSlug();
            if (slug.contains("?")) {
                slug = slug.substring(0, slug.indexOf("?"));
                language.setSlug(slug);
            }
            //query all languages trending, should set "" in path, not "all" now.
            if ("all".equals(slug)) {
                language.setSlug("");
            }
        }
    }
}
