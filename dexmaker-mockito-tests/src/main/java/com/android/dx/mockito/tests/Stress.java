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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class Stress {
    private static final String LOG_TAG = Stress.class.getSimpleName();
    private static final int NUM_TESTS = 80000;

    public static class TestClass {
        public String echo(String in) {
            return in;
        }
    }

    @Test
    public void mockALot() {
        for (int i = 0; i < NUM_TESTS; i++) {
            if (i % 1024 == 0) {
                Log.i(LOG_TAG, "Ran " + i + "/" + NUM_TESTS + " tests");
            }

            TestClass m = mock(TestClass.class);
            when(m.echo(eq("marco!"))).thenReturn("polo");
            assertEquals("polo", m.echo("marco!"));
            verify(m).echo("marco!");
        }
    }
}
