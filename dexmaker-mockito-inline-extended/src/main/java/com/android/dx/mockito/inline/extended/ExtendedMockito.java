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
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.internal.matchers.LocalizedMatcher;
import org.mockito.internal.progress.ArgumentMatcherStorageImpl;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.android.dx.mockito.inline.InlineDexmakerMockMaker.onSpyInProgressInstance;
import static com.android.dx.mockito.inline.InlineStaticMockMaker.onMethodCallDuringVerification;
import static org.mockito.internal.progress.ThreadSafeMockingProgress.mockingProgress;

/**
 * Mockito extended with the ability to stub static methods.
 * <p>E.g.
 * <pre>
 *     private class C {
 *         static int staticMethod(String arg) {
 *             return 23;
 *         }
 *     }
 *
 *    {@literal @}Test
 *     public void test() {
 *         // static mocking
 *         MockitoSession session = mockitoSession().staticSpy(C.class).startMocking();
 *         try {
 *             doReturn(42).when(() -> {return C.staticMethod(eq("Arg"));});
 *             assertEquals(42, C.staticMethod("Arg"));
 *             verify(() -> C.staticMethod(eq("Arg"));
 *         } finally {
 *             session.finishMocking();
 *         }
 *     }
 * </pre>
 * <p>It is possible to use this class for instance mocking too. Hence you can use it as a full
 * replacement for {@link Mockito}.
 * <p>This is a prototype that is intended to eventually be upstreamed into mockito proper. Some
 * APIs might change. All such APIs are annotated with {@link UnstableApi}.
 */
@UnstableApi
public class ExtendedMockito extends Mockito {
    /**
     * Currently active {@link #mockitoSession() sessions}
     */
    private static ArrayList<StaticMockitoSession> sessions = new ArrayList<>();

    /**
     * Same as {@link Mockito#doAnswer(Answer)} but adds the ability to stub static method calls via
     * {@link StaticCapableStubber#when(MockedMethod)} and
     * {@link StaticCapableStubber#when(MockedVoidMethod)}.
     */
    public static StaticCapableStubber doAnswer(Answer answer) {
        return new StaticCapableStubber(Mockito.doAnswer(answer));
    }

    /**
     * Same as {@link Mockito#doCallRealMethod()} but adds the ability to stub static method calls
     * via {@link StaticCapableStubber#when(MockedMethod)} and
     * {@link StaticCapableStubber#when(MockedVoidMethod)}.
     */
    public static StaticCapableStubber doCallRealMethod() {
        return new StaticCapableStubber(Mockito.doCallRealMethod());
    }

    /**
     * Same as {@link Mockito#doNothing()} but adds the ability to stub static method calls via
     * {@link StaticCapableStubber#when(MockedMethod)} and
     * {@link StaticCapableStubber#when(MockedVoidMethod)}.
     */
    public static StaticCapableStubber doNothing() {
        return new StaticCapableStubber(Mockito.doNothing());
    }

    /**
     * Same as {@link Mockito#doReturn(Object)} but adds the ability to stub static method calls
     * via {@link StaticCapableStubber#when(MockedMethod)} and
     * {@link StaticCapableStubber#when(MockedVoidMethod)}.
     */
    public static StaticCapableStubber doReturn(Object toBeReturned) {
        return new StaticCapableStubber(Mockito.doReturn(toBeReturned));
    }

    /**
     * Same as {@link Mockito#doReturn(Object, Object...)} but adds the ability to stub static
     * method calls via {@link StaticCapableStubber#when(MockedMethod)} and
     * {@link StaticCapableStubber#when(MockedVoidMethod)}.
     */
    public static StaticCapableStubber doReturn(Object toBeReturned, Object... toBeReturnedNext) {
        return new StaticCapableStubber(Mockito.doReturn(toBeReturned, toBeReturnedNext));
    }

    /**
     * Same as {@link Mockito#doThrow(Class)} but adds the ability to stub static method calls via
     * {@link StaticCapableStubber#when(MockedMethod)} and
     * {@link StaticCapableStubber#when(MockedVoidMethod)}.
     */
    public static StaticCapableStubber doThrow(Class<? extends Throwable> toBeThrown) {
        return new StaticCapableStubber(Mockito.doThrow(toBeThrown));
    }

    /**
     * Same as {@link Mockito#doThrow(Class, Class...)} but adds the ability to stub static method
     * calls via {@link StaticCapableStubber#when(MockedMethod)} and
     * {@link StaticCapableStubber#when(MockedVoidMethod)}.
     */
    @SafeVarargs
    public static StaticCapableStubber doThrow(Class<? extends Throwable> toBeThrown,
                                               Class<? extends Throwable>... toBeThrownNext) {
        return new StaticCapableStubber(Mockito.doThrow(toBeThrown, toBeThrownNext));
    }

    /**
     * Same as {@link Mockito#doThrow(Throwable...)} but adds the ability to stub static method
     * calls via {@link StaticCapableStubber#when(MockedMethod)} and
     * {@link StaticCapableStubber#when(MockedVoidMethod)}.
     */
    public static StaticCapableStubber doThrow(Throwable... toBeThrown) {
        return new StaticCapableStubber(Mockito.doThrow(toBeThrown));
    }

    /**
     * Many methods of mockito take mock objects. To be able to call the same methods for static
     * mocking, this method gets a marker object that can be used instead.
     *
     * @param clazz The class object the marker should be crated for
     * @return A marker object. This should not be used directly. It can only be passed into other
     * ExtendedMockito methods.
     * @see #inOrder(Object...)
     * @see #clearInvocations(Object...)
     * @see #ignoreStubs(Object...)
     * @see #mockingDetails(Object)
     * @see #reset(Object[])
     * @see #verifyNoMoreInteractions(Object...)
     * @see #verifyZeroInteractions(Object...)
     */
    @UnstableApi
    @SuppressWarnings("unchecked")
    public static <T> T staticMockMarker(Class<T> clazz) {
        for (StaticMockitoSession session : sessions) {
            T marker = session.staticMockMarker(clazz);

            if (marker != null) {
                return marker;
            }
        }
        return null;
    }

    /**
     * Same as {@link #staticMockMarker(Class)} but for multiple classes at once.
     */
    @UnstableApi
    public static Object[] staticMockMarker(Class<?>... clazz) {
        Object[] markers = new Object[clazz.length];

        for (int i = 0; i < clazz.length; i++) {
            for (StaticMockitoSession session : sessions) {
                markers[i] = session.staticMockMarker(clazz[i]);

                if (markers[i] != null) {
                    break;
                }
            }

            if (markers[i] == null) {
                return null;
            }
        }

        return markers;
    }

    /**
     * Make an existing object a spy.
     *
     * <p>This does <u>not</u> clone the existing objects. If a method is stubbed on a spy
     * converted by this method all references to the already existing object will be affected by
     * the stubbing.
     *
     * @param toMock The existing object to convert into a spy
     */
    @UnstableApi
    public static void spyOn(Object toMock) {
        if (onSpyInProgressInstance.get() != null) {
            throw new IllegalStateException("Cannot set up spying on an existing object while "
                    + "setting up spying for another existing object");
        }

        onSpyInProgressInstance.set(toMock);
        try {
            spy(toMock);
        } finally {
            onSpyInProgressInstance.remove();
        }
    }

    /**
     * To be used for static mocks/spies in place of {@link Mockito#verify(Object)} when calling
     * void methods.
     * <p>E.g.
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
     *         mock.instanceMethod("Hello");
     *         verify(mock).mockedVoidInstanceMethod(eq("Hello"));
     *
     *         // static mocking
     *         MockitoSession session = mockitoSession().staticMock(C.class).startMocking();
     *         C.staticMethod("World");
     *         verify(() -> C.staticMethod(eq("World"));
     *         session.finishMocking();
     *     }
     * </pre>
     */
    public static void verify(MockedVoidMethod method) {
        verify(method, times(1));
    }

    /**
     * To be used for static mocks/spies in place of {@link Mockito#verify(Object)}.
     * <p>E.g. (please notice the 'return' in the lambda when verifying the static call)
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
     *         verify(mock).mockedVoidInstanceMethod(eq("Hello"));
     *
     *         // static mocking
     *         MockitoSession session = mockitoSession().staticMock(C.class).startMocking();
     *         C.staticMethod("World");
     *         verify(() -> <b>{return</b> C.staticMethod(eq("World")<b>;}</b>);
     *         session.finishMocking();
     *     }
     * </pre>
     */
    @UnstableApi
    public static void verify(MockedMethod method) {
        verify(method, times(1));
    }

    /**
     * To be used for static mocks/spies in place of
     * {@link Mockito#verify(Object, VerificationMode)} when calling void methods.
     *
     * @see #verify(MockedVoidMethod)
     */
    @UnstableApi
    public static void verify(MockedVoidMethod method, VerificationMode mode) {
        verifyInt(method, mode, null);
    }

    /**
     * To be used for static mocks/spies in place of
     * {@link Mockito#verify(Object, VerificationMode)}.
     *
     * @see #verify(MockedMethod)
     */
    @UnstableApi
    public static void verify(MockedMethod method, VerificationMode mode) {
        verify((MockedVoidMethod) method::get, mode);
    }

    /**
     * Same as {@link Mockito#inOrder(Object...)} but adds the ability to verify static method
     * calls via {@link StaticInOrder#verify(MockedMethod)},
     * {@link StaticInOrder#verify(MockedVoidMethod)},
     * {@link StaticInOrder#verify(MockedMethod, VerificationMode)}, and
     * {@link StaticInOrder#verify(MockedVoidMethod, VerificationMode)}.
     * <p>To verify static method calls, the result of {@link #staticMockMarker(Class)} has to be
     * passed to the {@code mocksAndMarkers} parameter. It is possible to mix static and instance
     * mocking.
     */
    @UnstableApi
    public static StaticInOrder inOrder(Object... mocksAndMarkers) {
        return new StaticInOrder(Mockito.inOrder(mocksAndMarkers));
    }

    /**
     * Same as {@link Mockito#mockitoSession()} but adds the ability to mock static methods
     * calls via {@link StaticMockitoSessionBuilder#mockStatic(Class)},
     * {@link StaticMockitoSessionBuilder#mockStatic(Class, Answer)}, and {@link
     * StaticMockitoSessionBuilder#mockStatic(Class, MockSettings)};
     * <p>All mocking spying will be removed once the session is finished.
     */
    public static StaticMockitoSessionBuilder mockitoSession() {
        return new StaticMockitoSessionBuilder(Mockito.mockitoSession());
    }

    /**
     * Common implementation of verification of static method calls.
     *
     * @param method          The static method call to be verified
     * @param mode            The verification mode
     * @param instanceInOrder If set, the {@link StaticInOrder} object
     */
    @SuppressWarnings({"CheckReturnValue", "MockitoUsage", "unchecked"})
    static void verifyInt(MockedVoidMethod method, VerificationMode mode, InOrder
            instanceInOrder) {
        if (onMethodCallDuringVerification.get() != null) {
            throw new IllegalStateException("Verification is already in progress on this "
                    + "thread.");
        }

        ArrayList<Method> verifications = new ArrayList<>();

        /* Set up callback that is triggered when the next static method is called on this thread.
         *
         * This is necessary as we don't know which class the method will be called on. Once the
         * call is intercepted this will
         *    1. Remove all matchers (e.g. eq(), any()) from the matcher stack
         *    2. Call verify on the marker for the class
         *    3. Add the markers back to the stack
         */
        onMethodCallDuringVerification.set((clazz, verifiedMethod) -> {
            // TODO: O holy reflection! Let's hope we can integrate this better.
            try {
                ArgumentMatcherStorageImpl argMatcherStorage = (ArgumentMatcherStorageImpl)
                        mockingProgress().getArgumentMatcherStorage();
                List<LocalizedMatcher> matchers;

                // Matcher are called before verify, hence remove the from the storage
                Method resetStackMethod
                        = argMatcherStorage.getClass().getDeclaredMethod("resetStack");
                resetStackMethod.setAccessible(true);

                matchers = (List<LocalizedMatcher>) resetStackMethod.invoke(argMatcherStorage);

                if (instanceInOrder == null) {
                    verify(staticMockMarker(clazz), mode);
                } else {
                    instanceInOrder.verify(staticMockMarker(clazz), mode);
                }

                // Add the matchers back after verify is called
                Field matcherStackField
                        = argMatcherStorage.getClass().getDeclaredField("matcherStack");
                matcherStackField.setAccessible(true);

                Method pushMethod = matcherStackField.getType().getDeclaredMethod("push",
                        Object.class);

                for (LocalizedMatcher matcher : matchers) {
                    pushMethod.invoke(matcherStackField.get(argMatcherStorage), matcher);
                }
            } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException
                    | InvocationTargetException | ClassCastException e) {
                throw new Error("Reflection failed. Do you use a compatible version of "
                        + "mockito?", e);
            }

            verifications.add(verifiedMethod);
        });
        try {
            try {
                // Trigger the method call. This call will be intercepted and trigger the
                // onMethodCallDuringVerification callback.
                method.run();
            } catch (Throwable t) {
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else if (t instanceof Error) {
                    throw (Error) t;
                }
                throw new RuntimeException(t);
            }

            if (verifications.isEmpty()) {
                // Make sure something was intercepted
                throw new IllegalArgumentException("Nothing was verified. Does the lambda call "
                        + "a static method on a 'static' mock/spy ?");
            } else if (verifications.size() > 1) {
                // A lambda might call several methods. In this case it is not clear what should
                // be verified. Hence throw an error.
                throw new IllegalArgumentException("Multiple intercepted calls on methods "
                        + verifications);
            }
        } finally {
            onMethodCallDuringVerification.remove();
        }
    }

    /**
     * Register a new session.
     *
     * @param session Session to register
     */
    static void addSession(StaticMockitoSession session) {
        sessions.add(session);
    }

    /**
     * Remove a finished session.
     *
     * @param session Session to remove
     */
    static void removeSession(StaticMockitoSession session) {
        sessions.remove(session);
    }
}
