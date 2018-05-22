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

package com.android.dx.mockito.inline;

import android.util.Log;

import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockMaker;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Multiplexes multiple mock makers
 */
public final class MockMakerMultiplexer implements MockMaker {
    private static final String LOG_TAG = MockMakerMultiplexer.class.getSimpleName();
    private final static MockMaker[] MOCK_MAKERS;

    static {
        String[] potentialMockMakers = new String[] {
                "com.android.dx.mockito.inline.InlineStaticMockMaker",
                InlineDexmakerMockMaker.class.getName()
        };

        ArrayList<MockMaker> mockMakers = new ArrayList<>();
        for (String potentialMockMaker : potentialMockMakers) {
            try {
                Class<? extends MockMaker> mockMakerClass = (Class<? extends MockMaker>)
                        Class.forName(potentialMockMaker);
                mockMakers.add(mockMakerClass.getDeclaredConstructor().newInstance());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                    | NoSuchMethodException | InvocationTargetException e) {
                if (potentialMockMaker.equals(InlineDexmakerMockMaker.class.getName())) {
                    Log.e(LOG_TAG, "Could not init mockmaker " + potentialMockMaker, e);
                } else {
                    // Additional mock makers might not be loaded
                    Log.e(LOG_TAG, "Could not init mockmaker " + potentialMockMaker);
                }
            }
        }

        MOCK_MAKERS = mockMakers.toArray(new MockMaker[]{});
    }

    @Override
    public <T> T createMock(MockCreationSettings<T> settings, MockHandler handler) {
        for (MockMaker mockMaker : MOCK_MAKERS) {
            T mock = mockMaker.createMock(settings, handler);

            if (mock != null) {
                return mock;
            }
        }

        return null;
    }

    @Override
    public MockHandler getHandler(Object mock) {
        for (MockMaker mockMaker : MOCK_MAKERS) {
            MockHandler handler = mockMaker.getHandler(mock);

            if (handler != null) {
                return handler;
            }
        }

        return null;
    }

    @Override
    public void resetMock(Object mock, MockHandler newHandler, MockCreationSettings settings) {
        for (MockMaker mockMaker : MOCK_MAKERS) {
            mockMaker.resetMock(mock, newHandler, settings);
        }
    }

    @Override
    public TypeMockability isTypeMockable(Class<?> type) {
        for (MockMaker mockMaker : MOCK_MAKERS) {
            TypeMockability mockability = mockMaker.isTypeMockable(type);

            if (mockability != null) {
                return mockability;
            }
        }

        return null;
    }
}
