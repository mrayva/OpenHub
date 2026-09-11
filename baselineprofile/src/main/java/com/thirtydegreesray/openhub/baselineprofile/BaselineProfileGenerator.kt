package com.thirtydegreesray.openhub.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates app/src/normalRelease/generated/baselineProfiles/baseline-prof.txt,
 * embedded in the release APK and installed into ART's profile cache on
 * first run so the recorded classes/methods get AOT-compiled instead of
 * interpreted/JIT-warmed.
 *
 * Covers cold start through the app's first real screen: on any fresh
 * install BaseActivity's auth gate (BaseActivity.java:76) sends every
 * unauthenticated user through Splash -> Login before anything else, so
 * this is the one journey literally every install goes through, regardless
 * of whether the user ever signs in. Authenticated journeys (opening a
 * repo, scrolling a list) would need a benchmark-only way to seed a fake
 * login session - not attempted here.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startupToLogin() = baselineProfileRule.collect(
        packageName = "com.future.openhub"
    ) {
        pressHome()
        startActivityAndWait()
    }
}
