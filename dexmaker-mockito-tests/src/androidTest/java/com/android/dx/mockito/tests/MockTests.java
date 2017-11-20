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

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class MockTests {
    public static class TestClass {
        public String returnA() {
            return "A";
        }
    }

    public interface TestInterface {
        String returnA();
    }

    @Test
    public void mockClass() throws Exception {
        TestClass t = mock(TestClass.class);

        assertNull(t.returnA());

        when(t.returnA()).thenReturn("B");
        assertEquals("B", t.returnA());
    }

    @Test
    public void mockInterface() throws Exception {
        TestInterface t = mock(TestInterface.class);

        assertNull(t.returnA());

        when(t.returnA()).thenReturn("B");
        assertEquals("B", t.returnA());
    }

    @Test
    public void spyClass() throws Exception {
        TestClass originalT = new TestClass();
        TestClass t = spy(originalT);

        assertEquals("A", t.returnA());

        when(t.returnA()).thenReturn("B");
        assertEquals("B", t.returnA());

        // Wrapped object is not affected by mocking
        assertEquals("A", originalT.returnA());
    }

    @Test
    public void spyNewClass() throws Exception {
        TestClass t = spy(TestClass.class);

        assertEquals("A", t.returnA());

        when(t.returnA()).thenReturn("B");
        assertEquals("B", t.returnA());
    }
}
