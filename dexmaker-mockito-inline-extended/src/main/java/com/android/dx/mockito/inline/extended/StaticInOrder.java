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

import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

/**
 * Same as {@link InOrder} but adds the ability to verify static method calls via
 * {@link #verify(MockedMethod)}, {@link #verify(MockedVoidMethod)},
 * {@link #verify(MockedMethod, VerificationMode)}, and
 * {@link #verify(MockedVoidMethod, VerificationMode)}.
 */
@UnstableApi
public class StaticInOrder implements InOrder {
    private final InOrder instanceInOrder;

    StaticInOrder(InOrder inOrder) {
        instanceInOrder = inOrder;
    }

    @Override
    public <T> T verify(T mock) {
        return instanceInOrder.verify(mock);
    }

    @Override
    public <T> T verify(T mock, VerificationMode mode) {
        return instanceInOrder.verify(mock, mode);
    }

    /**
     * To be used for static mocks/spies in place of {@link #verify(Object)} when calling void
     * methods.
     * <p>E.g.
     * <pre>
     *     private class C {
     *         void instanceMethod(String arg) {}
     *         void void staticMethod(String arg) {}
     *     }
     *
     *    {@literal @}Test
     *     public void test() {
     *         // instance mocking
     *         C mock = mock(C.class);
     *         mock.staticMethod("Hello");
     *         mock.instanceMethod("World");
     *         inOrder().verify(mock).mockedVoidInstanceMethod(eq("Hello"));
     *         inOrder().verify(mock).mockedVoidInstanceMethod(eq("World"));
     *
     *         // static mocking
     *         MockitoSession session = mockitoSession().staticMock(C.class).startMocking();
     *         C.staticMethod("Hello");
     *         C.staticMethod("World");
     *
     *         StaticInOrder inOrder = inOrder();
     *         inOrder.verify(() -> C.staticMethod(eq("Hello"));
     *         inOrder.verify(() -> C.staticMethod(eq("World"));
     *         session.finishMocking();
     *     }
     * </pre>
     */
    public void verify(MockedVoidMethod method) {
        verify(method, Mockito.times(1));
    }

    /**
     * To be used for static mocks/spies in place of {@link #verify(Object)}.
     * <p>E.g.
     * <pre>
     *     private class C {
     *         int instanceMethod(String arg) {
     *             return 1;
     *         }
     *
     *         int static staticMethod(String arg) {
     *             return 2;
     *         }
     *     }
     *
     *    {@literal @}Test
     *     public void test() {
     *         // instance mocking
     *         C mock = mock(C.class);
     *         mock.instanceMethod("Hello");
     *         mock.instanceMethod("World");
     *         inOrder().verify(mock).mockedVoidInstanceMethod(eq("Hello"));
     *         inOrder().verify(mock).mockedVoidInstanceMethod(eq("World"));
     *
     *         // static mocking
     *         MockitoSession session = mockitoSession().staticMock(C.class).startMocking();
     *         C.staticMethod("Hello");
     *         C.staticMethod("World");
     *
     *         StaticInOrder inOrder = inOrder();
     *         inOrder.verify(() -> C.staticMethod(eq("Hello"));
     *         inOrder.verify(() -> C.staticMethod(eq("World"));
     *         session.finishMocking();
     *     }
     * </pre>
     */
    @UnstableApi
    public void verify(MockedMethod method) {
        verify(method, Mockito.times(1));
    }

    /**
     * To be used for static mocks/spies in place of
     * {@link InOrder#verify(Object, VerificationMode)} when calling void methods.
     *
     * @see #verify(MockedVoidMethod)
     */
    @UnstableApi
    public void verify(MockedVoidMethod method, VerificationMode mode) {
        ExtendedMockito.verifyInt(method, mode, instanceInOrder);
    }

    /**
     * To be used for static mocks/spies in place of
     * {@link InOrder#verify(Object, VerificationMode)}.
     *
     * @see #verify(MockedMethod)
     */
    @UnstableApi
    public void verify(MockedMethod method, VerificationMode mode) {
        verify((MockedVoidMethod) method::get, mode);
    }

    @Override
    public void verifyNoMoreInteractions() {
        instanceInOrder.verifyNoMoreInteractions();
    }
}
