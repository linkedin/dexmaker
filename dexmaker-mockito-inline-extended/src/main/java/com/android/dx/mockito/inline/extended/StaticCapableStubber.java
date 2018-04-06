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

package com.android.dx.mockito.inline.extended;

import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import java.lang.reflect.Method;
import java.util.ArrayList;

import static com.android.dx.mockito.inline.InlineStaticMockMaker.onMethodCallDuringStubbing;

/**
 * Same as {@link Stubber} but supports settings up stubbing of static methods via
 * {@link #when(MockedMethod)} and {@link #when(MockedVoidMethod)}.
 */
@UnstableApi
public class StaticCapableStubber implements Stubber {
    private Stubber instanceStubber;

    StaticCapableStubber(Stubber instanceStubber) {
        this.instanceStubber = instanceStubber;
    }

    @Override
    public <T> T when(T mock) {
        return instanceStubber.when(mock);
    }

    /**
     * Common implementation of all {@code doReturn.when} calls.
     *
     * @param method The static method to be stubbed
     */
    private void whenInt(MockedVoidMethod method) {
        if (onMethodCallDuringStubbing.get() != null) {
            throw new IllegalStateException("Stubbing is already in progress on this thread.");
        }

        ArrayList<Method> stubbingsSetUp = new ArrayList<>();

        /* Set up interception of method. 'method' does not specify what class the stubbing is
         * set up on. Hence wait until the call is made and intercept it just before the code
         * is executed. At this time, start the stubbing operation.
         */
        onMethodCallDuringStubbing.set((clazz, stubbedMethod) -> {
            when(ExtendedMockito.staticMockMarker(clazz));
            stubbingsSetUp.add(stubbedMethod);
        });
        try {
            try {
                // Call the method. This will be intercepted by onMethodCallDuringStubbing
                method.run();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }

            if (stubbingsSetUp.isEmpty()) {
                // Make sure something was intercepted
                throw new IllegalArgumentException("Nothing was stubbed. Does the lambda call a"
                        + " static method on a 'static' mock/spy?");
            } else if (stubbingsSetUp.size() > 1) {
                // A lambda might call several methods. In this case it is not clear what should
                // be stubbed. Hence throw an error.
                throw new IllegalArgumentException("Multiple intercepted calls on method "
                        + stubbingsSetUp);
            }
        } finally {
            onMethodCallDuringStubbing.remove();
        }
    }

    /**
     * Set up stubbing for a static void method.
     * <pre>
     *     private class C {
     *         void instanceMethod(String arg) {}
     *         static void staticMethod(String arg) {}
     *     }
     *
     *    {@literal @}Test
     *     public void test() {
     *         // instance mocking
     *         C mock = mock(C.class);
     *         doThrow(Exception.class).when(mock).instanceMethod(eq("Hello));
     *         assertThrows(Exception.class, mock.instanceMethod("Hello"));
     *
     *         // static mocking
     *         MockitoSession session = mockitoSession().staticMock(C.class).startMocking();
     *         doThrow(Exception.class).when(() -> C.instanceMethod(eq("Hello));
     *         assertThrows(Exception.class, C.staticMethod("Hello"));
     *         session.finishMocking();
     *     }
     * </pre>
     *
     * @param method The method to stub as a lambda. This should only call a single stubbable
     *               static method.
     */
    @UnstableApi
    public void when(MockedVoidMethod method) {
        whenInt(method);
    }

    /**
     * Set up stubbing for a static method.
     * <pre>
     *     private class C {
     *         int instanceMethod(String arg) {
     *             return 1;
     *         }
     *
     *         int static staticMethod(String arg) {
     *             return 1;
     *         }
     *     }
     *
     *    {@literal @}Test
     *     public void test() {
     *         // instance mocking
     *         C mock = mock(C.class);
     *         doReturn(2).when(mock).instanceMethod(eq("Hello));
     *         assertEquals(2, mock.instanceMethod("Hello"));
     *
     *         // static mocking
     *         MockitoSession session = mockitoSession().staticMock(C.class).startMocking();
     *         doReturn(2).when(() -> C.instanceMethod(eq("Hello));
     *         assertEquals(2, C.staticMethod("Hello"));
     *         session.finishMocking();
     *     }
     * </pre>
     *
     * @param method The method to stub as a lambda. This should only call a single stubbable
     *               static method.
     * @param <T>    Return type of the stubbed method
     */
    @UnstableApi
    public <T> void when(MockedMethod<T> method) {
        whenInt(method::get);
    }

    @Override
    public StaticCapableStubber doThrow(Throwable... toBeThrown) {
        instanceStubber = instanceStubber.doThrow(toBeThrown);
        return this;
    }

    @Override
    public StaticCapableStubber doThrow(Class<? extends Throwable> toBeThrown) {
        instanceStubber = instanceStubber.doThrow(toBeThrown);
        return this;
    }

    @SafeVarargs
    @Override
    public final StaticCapableStubber doThrow(Class<? extends Throwable> toBeThrown,
                                              Class<? extends Throwable>... nextToBeThrown) {
        instanceStubber = instanceStubber.doThrow(toBeThrown, nextToBeThrown);
        return this;
    }

    @Override
    public StaticCapableStubber doAnswer(Answer answer) {
        instanceStubber = instanceStubber.doAnswer(answer);
        return this;
    }

    @Override
    public StaticCapableStubber doNothing() {
        instanceStubber = instanceStubber.doNothing();
        return this;
    }

    @Override
    public StaticCapableStubber doReturn(Object toBeReturned) {
        instanceStubber = instanceStubber.doReturn(toBeReturned);
        return this;
    }

    @Override
    public StaticCapableStubber doReturn(Object toBeReturned, Object... nextToBeReturned) {
        instanceStubber = instanceStubber.doReturn(toBeReturned, nextToBeReturned);
        return this;
    }

    @Override
    public StaticCapableStubber doCallRealMethod() {
        instanceStubber = instanceStubber.doCallRealMethod();
        return this;
    }
}
