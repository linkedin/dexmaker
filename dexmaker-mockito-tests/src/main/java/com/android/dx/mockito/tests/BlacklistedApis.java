/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dx.mockito.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simplified versions of bugs seen in the past
 */
@RunWith(AndroidJUnit4.class)
public class BlacklistedApis {
    @Before
    public void onlyRunOnPlatformsThatSupportBlacklisting() {
        assumeTrue(Build.VERSION.SDK_INT >= 28);
    }

    /**
     * Check if the application is marked as {@code android:debuggable} in the manifest
     *
     * @return {@code true} iff it is marked as such
     */
    private boolean isDebuggable() throws PackageManager.NameNotFoundException {
        Context context = InstrumentationRegistry.getTargetContext();
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                context.getPackageName(), 0);

        return (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    @Test
    public void callBlacklistedPublicMethodRealMethod() throws Exception {
        Context targetContext = InstrumentationRegistry.getTargetContext();

        FrameLayout child = new FrameLayout(targetContext);
        FrameLayout parent = spy(new FrameLayout(targetContext));

        if (isDebuggable()) {
            // This calls a blacklisted public method.
            // Since Android P these methods are not callable from outside of the Android framework
            // anymore:
            //
            // https://android-developers.googleblog.com/2018/02/
            // improving-stability-by-reducing-usage.html
            //
            // Hence if we use a subclass mock this will fail. Inline mocking does not have this
            // problem as the mock class is the same as the mocked class.
            parent.addView(child);
        } else {
            try {
                parent.addView(child);
                fail();
            } catch (NoSuchMethodError expected) {
                // expected
            }
        }
    }

    @Test
    public void copyBlacklistedFields() throws Exception {
        // Can only copy blacklisted fields when debuggable
        assumeTrue(isDebuggable());

        Context targetContext = InstrumentationRegistry.getTargetContext();

        FrameLayout child = new FrameLayout(targetContext);
        FrameLayout parent = spy(new FrameLayout(targetContext));

        parent.addView(child);

        // During cloning of the parent spy, all fields are copied. This accesses a blacklisted
        // fields. Since Android P these fields are not visible from outside of the Android
        // framework anymore:
        //
        // https://android-developers.googleblog.com/2018/02/
        // improving-stability-by-reducing-usage.html
        //
        // As 'measure' requires the fields to be initialized, this fails if the fields are not
        // copied.
        parent.measure(100, 100);
    }

    @SuppressLint({"PrivateApi", "CheckReturnValue"})
    @Test
    public void cannotCallBlackListedAfterSpying() {
        // Spying and mocking might change the View class's byte code
        spy(new View(InstrumentationRegistry.getTargetContext(), null));
        mock(View.class);

        // View#setNotifyAutofillManagerOnClick is a blacklisted method. Resolving it should fail
        try {
            View.class.getDeclaredMethod("setNotifyAutofillManagerOnClick", Boolean.TYPE);
            fail();
        } catch (NoSuchMethodException expected) {
            // expected
        }
    }

    public static class CallBlackListedMethod {
        @SuppressLint("PrivateApi")
        boolean callingBlacklistedMethodCausesException() {
            // Settings.Global#isValidZenMode is a blacklisted method. Resolving it should fail
            try {
                Settings.Global.class.getDeclaredMethod("isValidZenMode", Integer.TYPE);
                return false;
            } catch (NoSuchMethodException expected) {
                return true;
            }
        }
    }

    @Test
    public void spiesCannotBeUsedToCallHiddenMethods() {
        CallBlackListedMethod t = spy(new CallBlackListedMethod());
        assertTrue(t.callingBlacklistedMethodCausesException());
    }

    public static abstract class CallBlacklistedMethodAbstract {
        @SuppressLint("PrivateApi")
        public boolean callingBlacklistedMethodCausesException() {
            // Settings.Global#isValidZenMode is a blacklisted method. Resolving it should fail
            try {
                Settings.Global.class.getDeclaredMethod("isValidZenMode", Integer.TYPE);
                return false;
            } catch (NoSuchMethodException expected) {
                return true;
            }
        }

        public abstract void unused();
    }

    @Test
    public void mocksOfAbstractClassesCannotBeUsedToCallHiddenMethods() {
        CallBlacklistedMethodAbstract t = mock(CallBlacklistedMethodAbstract.class);
        doCallRealMethod().when(t).callingBlacklistedMethodCausesException();
        assertTrue(t.callingBlacklistedMethodCausesException());
    }
}
