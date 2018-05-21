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

package com.android.dx.mockito.inline.extended.tests;

import android.app.Instrumentation;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.doAnswer;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.mock;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.spy;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.spyOn;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.verify;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

@RunWith(AndroidJUnit4.class)
public class SpyOn {
    @Rule
    public ActivityTestRule<EmptyActivity> activityRule =
            new ActivityTestRule<>(EmptyActivity.class);

    public class TestClass {
        Object field;

        public String echo(String in) {
            return in;
        }
    }

    @Test
    public void spyOnLocalClass() {
        TestClass t = new TestClass();
        assertEquals("one", t.echo("one"));

        spyOn(t);
        assertEquals("two", t.echo("two"));
        verify(t).echo("two");

        when(t.echo("three")).thenReturn("not three");
        assertEquals("not three", t.echo("three"));
        verify(t).echo("three");
    }

    @Test
    public void localFieldStaysTheSame() {
        TestClass t = new TestClass();

        Object marker = mock(Object.class);
        t.field = marker;

        spyOn(t);
        assertSame(marker, t.field);
    }

    @Test
    public void spiesAreUsuallyClones() {
        TestClass original = new TestClass();

        Object marker = new Object();
        original.field = marker;

        TestClass spy = spy(original);
        assertSame(marker, spy.field);

        assertNotSame(original, spy);
    }

    @Test
    public void spyOnActivity() throws Exception {
        EmptyActivity a = activityRule.getActivity();
        spyOn(a);

        // Intercept a#onDestroy(). The first time this is called isDestroyed[0] is set to true,
        // the second time it is called, it calls the real method.
        boolean isDestroyed[] = new boolean[]{false};
        doAnswer((inv) -> {
            synchronized (isDestroyed) {
                isDestroyed[0] = true;
                isDestroyed.notifyAll();
            }

            // Call a second time to call super method before returning. Android requires onDestroy
            // to always call it's super-method.
            a.onDestroy();
            return null;
        }).doCallRealMethod().when(a).onDestroy();

        activityRule.finishActivity();

        synchronized (isDestroyed) {
            while (!isDestroyed[0]) {
                isDestroyed.wait();
            }
        }
    }
}
