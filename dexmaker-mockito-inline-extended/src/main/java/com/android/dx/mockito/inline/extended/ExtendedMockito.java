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

import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;

/**
 * Mockito extended with the ability to stub static methods.
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
