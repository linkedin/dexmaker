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
        assumeTrue("Requires API level 28 or higher", Build.VERSION.SDK_INT >= 28);
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

    /**
     * Check if blacklisting of non-SDK APIs is currently enforced in this process.
     *
     * @return {@code true} iff blacklisting is enforced
     */
    private boolean isBlacklistingEnforced() {
        return new CallBlackListedMethod().callingBlacklistedMethodCausesException();
    }

    @Test
    public void callBlacklistedPublicMethodRealMethod() throws Exception {
        Context targetContext = InstrumentationRegistry.getTargetContext();

        FrameLayout child = new FrameLayout(targetContext);
        FrameLayout parent = spy(new FrameLayout(targetContext));

        if (isDebuggable() || !isBlacklistingEnforced()) {
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
            boolean threw = false;
            try {
                parent.addView(child);
            } catch (NoSuchMethodError | NullPointerException expected) {
                // expected: on early Android P, accessing hidden ViewGroup.mChildren threw
                // NoSuchMethodError; on modern Android, mChildren cannot be copied into the
                // subclass spy because reflection on hidden fields is blocked, leaving mChildren
                // null and causing addView to throw NullPointerException.
                threw = true;
            }
            assertTrue("Calling blacklisted method parent.addView() should throw an exception or error",
                    threw);
        }
    }

    @Test
    public void copyBlacklistedFields() throws Exception {
        // Can only copy blacklisted fields when debuggable
        assumeTrue("Application must be debuggable to copy blacklisted fields", isDebuggable());

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

    @SuppressLint({"PrivateApi", "CheckReturnValue", "SoonBlockedPrivateApi"})
    @SuppressWarnings("ReturnValueIgnored") // Used for side effect of checking reflection resolution
    @Test
    public void cannotCallBlackListedAfterSpying() throws Exception {
        assumeTrue("Blacklisting is not enforced in this process", isBlacklistingEnforced());

        // Spying and mocking might change the View class's byte code
        spy(new View(InstrumentationRegistry.getTargetContext(), null));
        mock(View.class);

        // View#setNotifyAutofillManagerOnClick is a blacklisted method. Resolving it should fail
        try {
            View.class.getDeclaredMethod("setNotifyAutofillManagerOnClick", Boolean.TYPE);
            fail("Resolving blacklisted method should throw NoSuchMethodException");
        } catch (NoSuchMethodException expected) {
            // expected
        }
    }

    public static class CallBlackListedMethod {
        @SuppressLint({"PrivateApi", "SoonBlockedPrivateApi"})
        @SuppressWarnings("ReturnValueIgnored") // Used for side effect of checking reflection resolution
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
    public void spiesCannotBeUsedToCallHiddenMethods() throws Exception {
        assumeTrue("Blacklisting is not enforced in this process", isBlacklistingEnforced());

        CallBlackListedMethod t = spy(new CallBlackListedMethod());
        assertTrue("Calling blacklisted method on spy should cause an exception",
                t.callingBlacklistedMethodCausesException());
    }

    public static abstract class CallBlacklistedMethodAbstract {
        @SuppressLint({"PrivateApi", "SoonBlockedPrivateApi"})
        @SuppressWarnings("ReturnValueIgnored") // Used for side effect of checking reflection resolution
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
    public void mocksOfAbstractClassesCannotBeUsedToCallHiddenMethods() throws Exception {
        assumeTrue("Blacklisting is not enforced in this process", isBlacklistingEnforced());

        CallBlacklistedMethodAbstract t = mock(CallBlacklistedMethodAbstract.class);
        doCallRealMethod().when(t).callingBlacklistedMethodCausesException();
        assertTrue("Calling blacklisted method on mock of abstract class should cause an exception",
                t.callingBlacklistedMethodCausesException());
    }
}
