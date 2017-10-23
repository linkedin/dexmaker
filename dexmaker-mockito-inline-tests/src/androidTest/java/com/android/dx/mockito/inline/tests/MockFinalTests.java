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

import android.content.Intent;
import android.os.IBinder;
import android.print.PrintAttributes;
import android.printservice.PrintService;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class MockFinalTests {
    @Test
    public void mockFinalJavaMethod() throws Exception {
        ClassLoader fakeParent = mock(ClassLoader.class);
        ClassLoader mockClassLoader = mock(ClassLoader.class);

        assertNull(mockClassLoader.getParent());

        // ClassLoader#getParent is final
        when(mockClassLoader.getParent()).thenReturn(fakeParent);

        assertSame(fakeParent, mockClassLoader.getParent());
    }

    @Test
    public void mockFinalAndroidFrameworkClass() throws Exception {
        // PrintAttributes is final
        PrintAttributes mockAttributes = mock(PrintAttributes.class);

        assertEquals(0, mockAttributes.getColorMode());

        when(mockAttributes.getColorMode()).thenReturn(42);

        assertEquals(42, mockAttributes.getColorMode());
    }

    @Test
    public void mockFinalMethodOfAndroidFrameworkClass() throws Exception {
        IBinder fakeBinder = mock(IBinder.class);
        PrintService mockService = mock(PrintService.class);

        assertNull(mockService.onBind(new Intent()));

        // PrintService#onBind is final
        when(mockService.onBind(any(Intent.class))).thenReturn(fakeBinder);

        assertSame(fakeBinder, mockService.onBind(new Intent()));
    }

    private final class FinalNonDefaultConstructorClass {
        public FinalNonDefaultConstructorClass(int i) {
        }

        String returnA() {
            return "A";
        }
    }

    @Test
    public void mockNonDefaultConstructorClass() throws Exception {
        FinalNonDefaultConstructorClass mock = mock(FinalNonDefaultConstructorClass.class);

        assertNull(mock.returnA());
        when(mock.returnA()).thenReturn("fakeA");

        assertEquals("fakeA", mock.returnA());
    }

    private interface NonDefaultConstructorInterface {
        String returnA();
    }

    @Test
    public void mockNonDefaultConstructorInterface() throws Exception {
        NonDefaultConstructorInterface mock = mock(NonDefaultConstructorInterface.class);

        assertNull(mock.returnA());
        when(mock.returnA()).thenReturn("fakeA");

        assertEquals("fakeA", mock.returnA());
    }

    private static class SuperClass {
        final String returnA() {
            return "superA";
        }

        String returnB() {
            return "superB";
        }

        String returnC() {
            return "superC";
        }
    }

    private static final class SubClass extends SuperClass {
        String returnC() {
            return "subC";
        }
    }

    @Test
    public void mockSubClass() throws Exception {
        SubClass mocked = mock(SubClass.class);
        SuperClass mockedSuper = mock(SuperClass.class);
        SubClass nonMocked = new SubClass();
        SuperClass nonMockedSuper = new SuperClass();

        // Mock returns dummy value by default
        assertNull(mocked.returnA());
        assertNull(mocked.returnB());
        assertNull(mocked.returnC());
        assertNull(mockedSuper.returnA());
        assertNull(mockedSuper.returnB());
        assertNull(mockedSuper.returnC());

        // Set fake values for mockedSuper
        when(mockedSuper.returnA()).thenReturn("fakeA");
        when(mockedSuper.returnB()).thenReturn("fakeB");
        when(mockedSuper.returnC()).thenReturn("fakeC");

        // mocked is unaffected
        assertNull(mocked.returnA());
        assertNull(mocked.returnB());
        assertNull(mocked.returnC());

        // Verify fake values of mockedSuper
        assertEquals("fakeA", mockedSuper.returnA());
        assertEquals("fakeB", mockedSuper.returnB());
        assertEquals("fakeC", mockedSuper.returnC());

        // Set fake values for mocked
        when(mocked.returnA()).thenReturn("fake2A");
        when(mocked.returnB()).thenReturn("fake2B");
        when(mocked.returnC()).thenReturn("fake2C");

        // Verify fake values of mocked
        assertEquals("fake2A", mocked.returnA());
        assertEquals("fake2B", mocked.returnB());
        assertEquals("fake2C", mocked.returnC());

        // non mocked instances are unaffected
        assertEquals("superA", nonMocked.returnA());
        assertEquals("superB", nonMocked.returnB());
        assertEquals("subC", nonMocked.returnC());
        assertEquals("superA", nonMockedSuper.returnA());
        assertEquals("superB", nonMockedSuper.returnB());
        assertEquals("superC", nonMockedSuper.returnC());
    }

    @Test
    public void spySubClass() throws Exception {
        SubClass spied = spy(SubClass.class);
        SuperClass spiedSuper = spy(SuperClass.class);
        SubClass nonSpied = new SubClass();
        SuperClass nonSpiedSuper = new SuperClass();

        // Spies call real method by default
        assertEquals("superA", spied.returnA());
        assertEquals("superB", spied.returnB());
        assertEquals("subC", spied.returnC());
        assertEquals("superA", spiedSuper.returnA());
        assertEquals("superB", spiedSuper.returnB());
        assertEquals("superC", spiedSuper.returnC());

        // Set fake values for spiedSuper
        when(spiedSuper.returnA()).thenReturn("fakeA");
        when(spiedSuper.returnB()).thenReturn("fakeB");
        when(spiedSuper.returnC()).thenReturn("fakeC");

        // spied is unaffected
        assertEquals("superA", spied.returnA());
        assertEquals("superB", spied.returnB());
        assertEquals("subC", spied.returnC());

        // Verify fake values of spiedSuper
        assertEquals("fakeA", spiedSuper.returnA());
        assertEquals("fakeB", spiedSuper.returnB());
        assertEquals("fakeC", spiedSuper.returnC());

        // Set fake values for spied
        when(spied.returnA()).thenReturn("fake2A");
        when(spied.returnB()).thenReturn("fake2B");
        when(spied.returnC()).thenReturn("fake2C");

        // Verify fake values of spied
        assertEquals("fake2A", spied.returnA());
        assertEquals("fake2B", spied.returnB());
        assertEquals("fake2C", spied.returnC());

        // non spied instances are unaffected
        assertEquals("superA", nonSpied.returnA());
        assertEquals("superB", nonSpied.returnB());
        assertEquals("subC", nonSpied.returnC());
        assertEquals("superA", nonSpiedSuper.returnA());
        assertEquals("superB", nonSpiedSuper.returnB());
        assertEquals("superC", nonSpiedSuper.returnC());
    }
}
