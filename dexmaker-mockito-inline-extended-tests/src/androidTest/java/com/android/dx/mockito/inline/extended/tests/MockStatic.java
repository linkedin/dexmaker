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

package com.android.dx.mockito.inline.extended.tests;

import static android.provider.Settings.Global.DEVICE_NAME;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.clearInvocations;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.mock;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockingDetails;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.reset;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.staticMockMarker;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.verifyZeroInteractions;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.when;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import android.content.ContentResolver;
import android.provider.Settings;

import androidx.test.InstrumentationRegistry;

import org.junit.Test;
import org.mockito.MockingDetails;
import org.mockito.MockitoSession;
import org.mockito.exceptions.misusing.MissingMethodInvocationException;
import org.mockito.quality.Strictness;

public class MockStatic {
    private static class SuperClass {
        final String returnA() {
            return "superA";
        }

        static String returnB() {
            return "superB";
        }

        static String returnC() {
            return "superC";
        }
    }

    private static final class SubClass extends SuperClass {
        static String recorded = null;

        static String returnC() {
            return "subC";
        }

        static final String record(String toRecord) {
            recorded = toRecord;
            return "record";
        }
    }

    private static class NoDefaultConstructorClass {
        private static int mLastId;

        NoDefaultConstructorClass(int id) {
            mLastId = id;
        }

        static int getLastId() {
            return mLastId;
        }
    }

    @Test
    public void spyStatic() throws Exception {
        ContentResolver resolver = InstrumentationRegistry.getTargetContext().getContentResolver();
        String deviceName = Settings.Global.getString(resolver, DEVICE_NAME);

        MockitoSession session = mockitoSession().spyStatic(Settings.Global.class).startMocking();
        try {
            // Cannot call when(Settings.getString(any(ContentResolver.class), eq("...")))
            // as any(ContentResolver.class) returns null which makes getString fail. Hence need to
            // use less lambda API
            doReturn("23").when(() -> Settings.Global.getString(any
                    (ContentResolver.class), eq("twenty three")));

            doReturn(42).when(() -> Settings.Global.getInt(any
                    (ContentResolver.class), eq("fourty two")));

            // Make sure behavior is changed
            assertEquals("23", Settings.Global.getString(resolver, "twenty three"));
            assertEquals(42, Settings.Global.getInt(resolver, "fourty two"));

            // Make sure non-mocked methods work as before
            assertEquals(deviceName, Settings.Global.getString(resolver, DEVICE_NAME));
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void spyStaticOnObjectWithNoDefaultConstructor() throws Exception {
        new NoDefaultConstructorClass(23);

        MockitoSession session = mockitoSession().spyStatic(NoDefaultConstructorClass.class)
                .startMocking();
        try {
            // Starting the spying hasn't change the static state of the class.
            assertEquals(23, NoDefaultConstructorClass.getLastId());

            when(NoDefaultConstructorClass.getLastId()).thenReturn(42);
            assertEquals(42, NoDefaultConstructorClass.getLastId());
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void mockStatic() throws Exception {
        ContentResolver resolver = InstrumentationRegistry.getTargetContext().getContentResolver();
        String deviceName = Settings.Global.getString(resolver, DEVICE_NAME);

        MockitoSession session = mockitoSession().mockStatic(Settings.Global.class).startMocking();
        try {
            // By default all static methods of the mocked class should return null/0/false
            assertNull(Settings.Global.getString(resolver, DEVICE_NAME));

            when(Settings.Global.getString(any(ContentResolver.class), eq(DEVICE_NAME)))
                    .thenReturn("This is a test");

            // Make sure behavior is changed
            assertEquals("This is a test", Settings.Global.getString(resolver, DEVICE_NAME));
        } finally {
            session.finishMocking();
        }

        // Once the mocking is removed, the behavior should be back to normal
        assertEquals(deviceName, Settings.Global.getString(resolver, DEVICE_NAME));
    }

    @Test
    public void mockOverriddenStaticMethod() throws Exception {
        MockitoSession session = mockitoSession().mockStatic(SubClass.class).startMocking();
        try {
            // By default all static methods of the mocked class should return the default answers
            assertNull(SubClass.returnB());
            assertNull(SubClass.returnC());

            // Super class is not mocked
            assertEquals("superB", SuperClass.returnB());
            assertEquals("superC", SuperClass.returnC());

            when(SubClass.returnB()).thenReturn("fakeB");
            when(SubClass.returnC()).thenReturn("fakeC");

            // Make sure behavior is changed
            assertEquals("fakeB", SubClass.returnB());
            assertEquals("fakeC", SubClass.returnC());

            // Super class should not be affected
            assertEquals("superB", SuperClass.returnB());
            assertEquals("superC", SuperClass.returnC());
        } finally {
            session.finishMocking();
        }

        // Mocking should be stopped
        assertEquals("superB", SubClass.returnB());
        assertEquals("subC", SubClass.returnC());
    }

    @Test
    public void mockSuperMethod() throws Exception {
        MockitoSession session = mockitoSession().mockStatic(SuperClass.class).startMocking();
        try {
            // By default all static methods of the mocked class should return the default answers
            assertNull(SuperClass.returnB());
            assertNull(SuperClass.returnC());

            // Sub class should not be affected
            assertEquals("superB", SubClass.returnB());
            assertEquals("subC", SubClass.returnC());

            when(SuperClass.returnB()).thenReturn("fakeB");
            when(SuperClass.returnC()).thenReturn("fakeC");

            // Make sure behavior is changed
            assertEquals("fakeB", SuperClass.returnB());
            assertEquals("fakeC", SuperClass.returnC());

            // Sub class should not be affected
            assertEquals("superB", SubClass.returnB());
            assertEquals("subC", SubClass.returnC());
        } finally {
            session.finishMocking();
        }

        // Mocking should be stopped
        assertEquals("superB", SuperClass.returnB());
        assertEquals("superC", SuperClass.returnC());
    }

    @Test(expected = MissingMethodInvocationException.class)
    public void nonMockedTest() throws Exception {
        when(SuperClass.returnB()).thenReturn("fakeB");
    }

    @Test
    public void resetMock() throws Exception {
        MockitoSession session = mockitoSession().mockStatic(SuperClass.class).startMocking();
        try {
            assertNull(SuperClass.returnB());

            when(SuperClass.returnB()).thenReturn("fakeB");
            assertEquals("fakeB", SuperClass.returnB());

            reset(staticMockMarker(SuperClass.class));
            assertNull(SuperClass.returnB());
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void resetSpy() throws Exception {
        MockitoSession session = mockitoSession().spyStatic(SuperClass.class).startMocking();
        try {
            assertEquals("superB", SuperClass.returnB());

            when(SuperClass.returnB()).thenReturn("fakeB");
            assertEquals("fakeB", SuperClass.returnB());

            reset(staticMockMarker(SuperClass.class));
            assertEquals("superB", SuperClass.returnB());
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void staticMockingIsSeparateFromNonStaticMocking() throws Exception {
        SuperClass objA = new SuperClass();
        SuperClass objB;

        MockitoSession session = mockitoSession().mockStatic(SuperClass.class).startMocking();
        try {
            assertNull(SuperClass.returnB());
            assertNull(objA.returnB());

            objB = mock(SuperClass.class);

            assertEquals("superA", objA.returnA());

            // Any kind of static method method call should be mocked
            assertNull(objB.returnA());

            assertNull(SuperClass.returnB());
            assertNull(objA.returnB());
            assertNull(objB.returnB());
        } finally {
            session.finishMocking();
        }

        assertEquals("superA", objA.returnA());
        assertNull(objB.returnA());

        // Any kind of static method method call should _not_ be mocked
        assertEquals("superB", SuperClass.returnB());
        assertEquals("superB", objA.returnB());
        assertEquals("superB", objB.returnB());
    }

    @Test
    public void mockWithTwoClasses() throws Exception {
        MockitoSession session = mockitoSession().mockStatic(SuperClass.class)
                .mockStatic(SubClass.class).startMocking();
        try {
            when(SuperClass.returnB()).thenReturn("fakeB");
            assertEquals("fakeB", SuperClass.returnB());

            when(SubClass.returnC()).thenReturn("fakeC");
            assertEquals("fakeC", SubClass.returnC());
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void doReturnMockWithTwoClasses() throws Exception {
        MockitoSession session = mockitoSession().mockStatic(SuperClass.class)
                .mockStatic(SubClass.class).startMocking();
        try {
            doReturn("fakeB").when(SuperClass::returnB);
            assertEquals("fakeB", SuperClass.returnB());

            doReturn("fakeD").when(() -> SubClass.record("test"));
            assertEquals("fakeD", SubClass.record("test"));
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void doReturnTwice() throws Exception {
        MockitoSession session = mockitoSession().mockStatic(SuperClass.class).startMocking();
        try {
            doReturn("fakeB").doReturn("fakeB2").when(SuperClass::returnB);
            assertEquals("fakeB", SuperClass.returnB());
            assertEquals("fakeB2", SuperClass.returnB());
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void doReturnSpyHasNoSideEffect() throws Exception {
        MockitoSession session = mockitoSession().spyStatic(SubClass.class).startMocking();
        try {
            SubClass.recorded = null;
            SubClass.record("no sideeffect");
            assertEquals("no sideeffect", SubClass.recorded);

            doReturn("faceRecord").when(() -> SubClass.record(eq("test")));
            // Verify that there was no side effect as the lambda gets intercepted
            assertEquals("no sideeffect", SubClass.recorded);

            assertEquals("faceRecord", SubClass.record("test"));
            // Verify that there was no side effect as the method is stubbed
            assertEquals("no sideeffect", SubClass.recorded);
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void onlyOneMethodCallDuringStubbing() throws Exception {
        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(SuperClass.class).startMocking();
        try {
            try {
                doReturn("").when(() -> {
                    SuperClass.returnB();
                    SuperClass.returnC();
                });
                fail();
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("returnB"));
                assertTrue(e.getMessage(), e.getMessage().contains("returnC"));

                assertFalse(e.getMessage(), e.getMessage().contains("returnA"));
            }
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void atLeastOneMethodCallDuringStubbing() throws Exception {
        Exception atLeastOneMethodCallException = null;

        try {
            MockitoSession session = mockitoSession().spyStatic(SuperClass.class).startMocking();
            try {
                try {
                    doReturn("").when(() -> {
                    });
                    fail();
                } catch (IllegalArgumentException expected) {
                    atLeastOneMethodCallException = expected;
                }
            } finally {
                session.finishMocking();
            }
        } catch (Throwable ignored) {
            // We don't want to test exceptions form MockitoSession
        }

        assertNotNull(atLeastOneMethodCallException);
    }

    @Test
    public void clearInvocationsRemovedInvocations() throws Exception {
        MockitoSession session = mockitoSession().mockStatic(SuperClass.class).startMocking();
        try {
            SuperClass.returnB();
            clearInvocations(staticMockMarker(SuperClass.class));
            verifyZeroInteractions(staticMockMarker(SuperClass.class));
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void verifyMockingDetails() throws Exception {
        MockitoSession session = mockitoSession().mockStatic(SuperClass.class)
                .spyStatic(SubClass.class).startMocking();
        try {
            when(SuperClass.returnB()).thenReturn("fakeB");
            SuperClass.returnB();
            SuperClass.returnC();

            MockingDetails superClassDetails = mockingDetails(staticMockMarker(SuperClass.class));
            assertTrue(superClassDetails.isMock());
            assertFalse(superClassDetails.isSpy());
            assertEquals(2, superClassDetails.getInvocations().size());
            assertEquals(1, superClassDetails.getStubbings().size());

            MockingDetails subClassDetails = mockingDetails(staticMockMarker(SubClass.class));
            assertTrue(subClassDetails.isMock());
            assertTrue(subClassDetails.isSpy());
        } finally {
            session.finishMocking();
        }
    }
}
