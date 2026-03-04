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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.exceptions.verification.NoInteractionsWanted;

@RunWith(AndroidJUnit4.class)
public class GeneralMocking {
    public static class TestClass {
        public String returnA() {
            return "A";
        }

        public String throwThrowable() throws Throwable {
            throw new Throwable();
        }

        public String throwOutOfMemoryError() throws OutOfMemoryError {
            throw new OutOfMemoryError();
        }

        public void throwNullPointerException() {
            throw new NullPointerException();
        }

        public String concat(String a, String b) {
            return a + b;
        }
    }

    public static class TestSubClass extends TestClass {

    }

    public interface TestInterface {
        String returnA();

        String concat(String a, String b);
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

    @Test
    public void verifyAdditionalInvocations() {
        TestClass t = mock(TestClass.class);

        t.returnA();
        t.returnA();

        try {
            verifyNoMoreInteractions(t);
        } catch (NoInteractionsWanted e) {
            try {
                throw new Exception();
            } catch (Exception here) {
                // The error message should indicate where the additional invocations have been made
                assertTrue(e.getMessage(),
                        e.getMessage().contains(here.getStackTrace()[0].getMethodName()));
            }
        }
    }

    @Test
    public void spyThrowingMethod() throws Exception {
        TestClass t = spy(TestClass.class);

        try {
            t.throwThrowable();
        } catch (Throwable e) {
            assertEquals("throwThrowable", e.getStackTrace()[0].getMethodName());
            return;
        }

        fail();
    }

    @Test()
    public void spyErrorMethod() throws Exception {
        TestClass t = spy(TestClass.class);

        try {
            t.throwOutOfMemoryError();
            fail();
        } catch (OutOfMemoryError e) {
            assertEquals("throwOutOfMemoryError", e.getStackTrace()[0].getMethodName());
        }
    }

    @Test()
    public void spyExceptingMethod() throws Exception {
        TestClass t = spy(TestClass.class);

        try {
            t.throwNullPointerException();
            fail();
        } catch (NullPointerException e) {
            assertEquals("throwNullPointerException", e.getStackTrace()[0].getMethodName());
        }
    }


    @Test
    public void callAbstractRealMethod() throws Exception {
        TestInterface t = mock(TestInterface.class);

        try {
            when(t.returnA()).thenCallRealMethod();
            fail();
        } catch (MockitoException e) {
            assertEquals("callAbstractRealMethod", e.getStackTrace()[0].getMethodName());
        }
    }

    @Test
    public void callInterfaceWithoutMatcher() throws Exception {
        TestInterface t = mock(TestInterface.class);

        when(t.concat("a", "b")).thenReturn("match");

        assertEquals("match", t.concat("a", "b"));
        assertNull(t.concat("b", "a"));
    }

    @Test
    public void callInterfaceWithMatcher() throws Exception {
        TestInterface t = mock(TestInterface.class);

        when(t.concat(eq("a"), anyString())).thenReturn("match");

        assertEquals("match", t.concat("a", "b"));
        assertNull(t.concat("b", "a"));
    }

    @Test
    public void callInterfaceWithNullMatcher() throws Exception {
        TestInterface t = mock(TestInterface.class);

        when(t.concat(eq("a"), (String) isNull())).thenReturn("match");

        assertEquals("match", t.concat("a", null));
        assertNull(t.concat("a", "b"));
    }

    @Test
    public void callClassWithoutMatcher() throws Exception {
        TestClass t = spy(TestClass.class);

        when(t.concat("a", "b")).thenReturn("match");

        assertEquals("match", t.concat("a", "b"));
        assertEquals("ba", t.concat("b", "a"));
    }

    @Test
    public void callClassWithMatcher() throws Exception {
        TestClass t = spy(TestClass.class);

        when(t.concat(eq("a"), anyString())).thenReturn("match");

        assertEquals("match", t.concat("a", "b"));
        assertEquals("ba", t.concat("b", "a"));
    }

    @Test
    public void callClassWithNullMatcher() throws Exception {
        TestClass t = spy(TestClass.class);

        when(t.concat(eq("a"), (String) isNull())).thenReturn("match");

        assertEquals("match", t.concat("a", null));
        assertEquals("ab", t.concat("a", "b"));
    }

    @Test
    public void callSubClassWithoutMatcher() throws Exception {
        TestSubClass t = spy(TestSubClass.class);

        when(t.concat("a", "b")).thenReturn("match");

        assertEquals("match", t.concat("a", "b"));
        assertEquals("ba", t.concat("b", "a"));
    }

    @Test
    public void callSubClassWithMatcher() throws Exception {
        TestSubClass t = spy(TestSubClass.class);

        when(t.concat(eq("a"), anyString())).thenReturn("match");

        assertEquals("match", t.concat("a", "b"));
        assertEquals("ba", t.concat("b", "a"));
    }

    @Test
    public void callSubClassWithNullMatcher() throws Exception {
        TestSubClass t = spy(TestSubClass.class);

        when(t.concat(eq("a"), (String) isNull())).thenReturn("match");

        assertEquals("match", t.concat("a", null));
        assertEquals("ab", t.concat("a", "b"));
    }
}
