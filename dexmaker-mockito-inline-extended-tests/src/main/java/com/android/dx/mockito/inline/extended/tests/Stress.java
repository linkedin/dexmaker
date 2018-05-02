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

import android.util.Log;

import org.junit.Test;
import org.mockito.MockitoSession;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.staticMockMarker;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;


public class Stress {
    private static final String LOG_TAG = Stress.class.getSimpleName();

    private static class SuperClass {
        static String returnB() {
            return "superB";
        }

        final static String returnD() {
            return "superD";
        }
    }

    @Test
    public void stressFinalStaticMethod() throws Exception {
        MockitoSession session = mockitoSession().mockStatic(SuperClass.class).startMocking();
        try {
            assertNull(SuperClass.returnD());

            for (int i = 0; i < 1000; i++) {
                when(SuperClass.returnD()).thenReturn("fakeD");
                assertEquals("fakeD", SuperClass.returnD());

                reset(staticMockMarker(SuperClass.class));
                assertNull(SuperClass.returnD());

                if (i % 100 == 0) {
                    Log.i(LOG_TAG, "Ran " + i + " tests");
                }
            }
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void stressStaticMethod() throws Exception {
        MockitoSession session = mockitoSession().mockStatic(SuperClass.class).startMocking();
        try {
            assertNull(SuperClass.returnB());

            for (int i = 0; i < 10; i++) {
                when(SuperClass.returnB()).thenReturn("fakeB");
                assertEquals("fakeB", SuperClass.returnB());

                reset(staticMockMarker(SuperClass.class));
                assertNull(SuperClass.returnB());

                Log.i(LOG_TAG, "Ran " + i + " tests");
            }
        } finally {
            session.finishMocking();
        }
    }

}
