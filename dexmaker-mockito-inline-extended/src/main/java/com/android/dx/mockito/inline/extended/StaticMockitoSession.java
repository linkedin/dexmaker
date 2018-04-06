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

import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.HashMap;

import static com.android.dx.mockito.inline.InlineStaticMockMaker.mockingInProgressClass;

/**
 * Same as {@link MockitoSession} but used when static methods are also stubbed.
 */
@UnstableApi
public class StaticMockitoSession implements MockitoSession {
    /**
     * For each class where static mocking is enabled there is one marker object.
     */
    private static final HashMap<Class, Object> classToMarker = new HashMap<>();

    private final MockitoSession instanceSession;
    private final ArrayList<Class<?>> staticMocks = new ArrayList<>(0);

    StaticMockitoSession(MockitoSession instanceSession) {
        ExtendedMockito.addSession(this);
        this.instanceSession = instanceSession;
    }

    @Override
    public void setStrictness(Strictness strictness) {
        instanceSession.setStrictness(strictness);
    }

    /**
     * {@inheritDoc}
     * <p><b>Extension:</b> This also resets all stubbing of static methods set up in the
     * {@link ExtendedMockito#mockitoSession() builder} of the session.
     */
    @Override
    public void finishMocking() {
        finishMocking(null);
    }

    /**
     * {@inheritDoc}
     * <p><b>Extension:</b> This also resets all stubbing of static methods set up in the
     * {@link ExtendedMockito#mockitoSession() builder} of the session.
     */
    @Override
    public void finishMocking(Throwable failure) {
        try {
            instanceSession.finishMocking(failure);
        } finally {
            for (Class<?> clazz : staticMocks) {
                mockingInProgressClass.set(clazz);
                try {
                    Mockito.reset(ExtendedMockito.staticMockMarker(clazz));
                } finally {
                    mockingInProgressClass.remove();
                }
                classToMarker.remove(clazz);
            }

            ExtendedMockito.removeSession(this);
        }
    }

    /**
     * Init mocking for a class.
     *
     * @param mocking Description and settings of the mocking
     * @param <T>     The class to mock
     */
    <T> void mockStatic(StaticMocking<T> mocking) {
        if (ExtendedMockito.staticMockMarker(mocking.clazz) != null) {
            throw new IllegalArgumentException(mocking.clazz + " is already mocked");
        }

        mockingInProgressClass.set(mocking.clazz);
        try {
            classToMarker.put(mocking.clazz, mocking.markerSupplier.get());
        } finally {
            mockingInProgressClass.remove();
        }

        staticMocks.add(mocking.clazz);
    }

    /**
     * Get marker for a mocked/spies class or {@code null}.
     *
     * @param clazz The class that is mocked
     * @return marker for a mocked class or {@code null} if class is not mocked in this session
     * @see ExtendedMockito#staticMockMarker(Class)
     */
    @SuppressWarnings("unchecked")
    <T> T staticMockMarker(Class<T> clazz) {
        return (T) classToMarker.get(clazz);
    }
}
