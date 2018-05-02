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

package com.android.dx.mockito.tests;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CleanStackTrace {
    public abstract static class TestAbstractClass {
        public abstract String returnA();
    }

    public static class TestClass {
        public String returnA() {
            return "A";
        }
    }

    public interface TestInterface {
        String returnA();
    }

    @Test
    public void cleanStackTraceAbstractClass() {
        TestAbstractClass t = mock(TestAbstractClass.class);

        try {
            verify(t).returnA();
        } catch (Throwable verifyLocation) {
            try {
                throw new Exception();
            } catch (Exception here) {
                assertEquals(here.getStackTrace()[0].getMethodName(), verifyLocation
                        .getStackTrace()[0].getMethodName());
            }
        }
    }

    @Test
    public void cleanStackTraceRegularClass() {
        TestClass t = mock(TestClass.class);

        try {
            verify(t).returnA();
        } catch (Throwable verifyLocation) {
            try {
                throw new Exception();
            } catch (Exception here) {
                // Depending on the mock maker TestClass.returnA might be in the stack trace or not
                for (int i = 0; i < 2; i++) {
                    if (verifyLocation.getStackTrace()[i].getMethodName().equals(here
                            .getStackTrace()[0].getMethodName())) {
                        return;
                    }
                }

                fail();
            }
        }
    }

    @Test
    public void cleanStackTraceInterface() {
        TestInterface t = mock(TestInterface.class);

        try {
            verify(t).returnA();
        } catch (Throwable verifyLocation) {
            try {
                throw new Exception();
            } catch (Exception here) {
                assertEquals(here.getStackTrace()[0].getMethodName(), verifyLocation
                        .getStackTrace()[0].getMethodName());
            }
        }
    }
}
