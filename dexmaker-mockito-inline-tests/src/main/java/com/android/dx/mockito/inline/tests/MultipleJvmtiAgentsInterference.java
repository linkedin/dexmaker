/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.dx.mockito.inline.tests;

import android.os.Build;
import android.os.Debug;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

public class MultipleJvmtiAgentsInterference {
    private static final String AGENT_LIB_NAME = "libmultiplejvmtiagentsinterferenceagent.so";

    public static class TestClass {
        public String returnA() {
            return "A";
        }
    }

    @BeforeClass
    public static void installTestAgent() throws Exception {
        // TODO (moltmann@google.com): Replace with proper check for >= P
        assumeTrue(Build.VERSION.CODENAME.equals("P"));

        Debug.attachJvmtiAgent(AGENT_LIB_NAME, null,
                MultipleJvmtiAgentsInterference.class.getClassLoader());
    }

    @Test
    public void otherAgentTransformsWhileMocking() {
        TestClass t = mock(TestClass.class);

        assertNull(t.returnA());

        // Unrelated class re-transform does not affect mocking
        nativeRetransformClasses(new Class<?>[]{MultipleJvmtiAgentsInterference.class});
        assertNull(t.returnA());

        // Re-transform of classes that are mocked does not affect mocking
        nativeRetransformClasses(new Class<?>[]{TestClass.class});
        assertNull(t.returnA());
    }

    @AfterClass
    public static void DisableRetransfromHook() {
        disableRetransformHook();
    }

    private native int nativeRetransformClasses(Class<?>[] classes);
    private static native int disableRetransformHook();
}
