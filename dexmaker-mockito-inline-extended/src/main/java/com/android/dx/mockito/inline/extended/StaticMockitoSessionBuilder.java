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
import org.mockito.exceptions.misusing.UnfinishedMockingSessionException;
import org.mockito.quality.Strictness;
import org.mockito.session.MockitoSessionBuilder;
import org.mockito.session.MockitoSessionLogger;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;

/**
 * Same as {@link MockitoSessionBuilder} but adds the ability to stub static methods
 * calls via {@link #mockStatic(Class)}, {@link #mockStatic(Class, Answer)}, and
 * {@link #mockStatic(Class, MockSettings)};
 * <p>All mocks/spies will be reset once the session is finished.
 */
@UnstableApi
public class StaticMockitoSessionBuilder implements MockitoSessionBuilder {
    private final ArrayList<StaticMocking> staticMockings = new ArrayList<>(0);
    private MockitoSessionBuilder instanceSessionBuilder;

    StaticMockitoSessionBuilder(MockitoSessionBuilder instanceSessionBuilder) {
        this.instanceSessionBuilder = instanceSessionBuilder;
    }

    /**
     * Sets up mocking for all static methods of a class. All methods will return the default value.
     * <p>This changes the behavior of <u>all</u> static methods calls for <u>all</u>
     * invocations. In most cases using {@link #spyStatic(Class)} and stubbing only a few
     * methods can be used.
     *
     * @param clazz The class to set up static mocking for
     * @return This builder
     */
    @UnstableApi
    public <T> StaticMockitoSessionBuilder mockStatic(Class<T> clazz) {
        staticMockings.add(new StaticMocking<>(clazz, () -> Mockito.mock(clazz)));
        return this;
    }

    /**
     * Sets up mocking for sall tatic methods of a class. All methods will call the {@code
     * defaultAnswer}.
     * <p>This changes the behavior of <u>all</u> static methods calls for <u>all</u>
     * invocations. In most cases using {@link #spyStatic(Class)} and stubbing only a few
     * methods can be used.
     *
     * @param clazz         The class to set up static mocking for
     * @param defaultAnswer The answer to return by default
     * @return This builder
     */
    @UnstableApi
    public <T> StaticMockitoSessionBuilder mockStatic(Class<T> clazz, Answer defaultAnswer) {
        staticMockings.add(new StaticMocking<>(clazz, () -> Mockito.mock(clazz, defaultAnswer)));
        return this;
    }

    /**
     * Sets up mocking for all static methods of a class with custom {@link MockSettings}.
     * <p>This changes the behavior of <u>all</u> static methods calls for <u>all</u>
     * invocations. In most cases using {@link #spyStatic(Class)} and stubbing only a few
     * methods can be used.
     *
     * @param clazz    The class to set up static mocking for
     * @param settings Settings used to set up the mock.
     * @return This builder
     */
    @UnstableApi
    public <T> StaticMockitoSessionBuilder mockStatic(Class<T> clazz, MockSettings settings) {
        staticMockings.add(new StaticMocking<>(clazz, () -> Mockito.mock(clazz, settings)));
        return this;
    }

    /**
     * Sets up spying for static methods of a class.
     *
     * @param clazz The class to set up static spying for
     * @return This builder
     */
    @UnstableApi
    public <T> StaticMockitoSessionBuilder spyStatic(Class<T> clazz) {
        staticMockings.add(new StaticMocking<>(clazz, () -> Mockito.spy(clazz)));
        return this;
    }

    @Override
    public StaticMockitoSessionBuilder initMocks(Object testClassInstance) {
        instanceSessionBuilder = instanceSessionBuilder.initMocks(testClassInstance);
        return this;
    }

    @Override
    public StaticMockitoSessionBuilder initMocks(Object... testClassInstances) {
        instanceSessionBuilder = instanceSessionBuilder.initMocks(testClassInstances);
        return this;
    }

    @Override
    public StaticMockitoSessionBuilder name(String name) {
        instanceSessionBuilder = instanceSessionBuilder.name(name);
        return this;
    }

    @Override
    public StaticMockitoSessionBuilder strictness(Strictness strictness) {
        instanceSessionBuilder = instanceSessionBuilder.strictness(strictness);
        return this;
    }

    @Override
    public StaticMockitoSessionBuilder logger(MockitoSessionLogger logger) {
        instanceSessionBuilder = instanceSessionBuilder.logger(logger);
        return this;
    }

    @Override
    public StaticMockitoSession startMocking() throws UnfinishedMockingSessionException {
        StaticMockitoSession session
                = new StaticMockitoSession(instanceSessionBuilder.startMocking());
        try {
            for (StaticMocking mocking : staticMockings) {
                session.mockStatic((StaticMocking<?>) mocking);
            }
        } catch (Throwable t) {
            try {
                session.finishMocking();
            } catch (Throwable ignored) {
                // suppress all failures
            }
            throw t;
        }

        return session;
    }
}
