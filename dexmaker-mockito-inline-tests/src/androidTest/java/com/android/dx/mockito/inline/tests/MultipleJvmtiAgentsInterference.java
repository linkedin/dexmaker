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

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import dalvik.system.BaseDexClassLoader;

import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

public class MultipleJvmtiAgentsInterference {
    private static final String AGENT_LIB_NAME = "multiplejvmtiagentsinterferenceagent";

    public class TestClass {
        public String returnA() {
            return "A";
        }
    }

    @BeforeClass
    public static void installTestAgent() throws Exception {
        // TODO (moltmann@google.com): Replace with proper check for >= P
        assumeTrue(Build.VERSION.CODENAME.equals("P"));

        // Currently Debug.attachJvmtiAgent requires a file in the right directory
        File copiedAgent = File.createTempFile("testagent", ".so");
        copiedAgent.deleteOnExit();

        try (InputStream is = new FileInputStream(((BaseDexClassLoader)
                MultipleJvmtiAgentsInterference.class.getClassLoader()).findLibrary
                (AGENT_LIB_NAME))) {
            try (OutputStream os = new FileOutputStream(copiedAgent)) {
                byte[] buffer = new byte[64 * 1024];

                while (true) {
                    int numRead = is.read(buffer);
                    if (numRead == -1) {
                        break;
                    }
                    os.write(buffer, 0, numRead);
                }
            }
        }

        // TODO (moltmann@google.com): Replace with regular method call once the API becomes public
        Class.forName("android.os.Debug").getMethod("attachJvmtiAgent", String.class, String
                .class).invoke(null, copiedAgent.getAbsolutePath(), null);
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

    private native int nativeRetransformClasses(Class<?>[] classes);
}
