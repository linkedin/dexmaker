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

import android.os.Build;

import org.mockito.Mockito;
import org.mockito.creation.instance.Instantiator;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.InstantiatorProvider2;
import org.mockito.plugins.MockMaker;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Creates mock markers and adds stubbing hooks to static method
 *
 * <p>This is done by transforming the byte code of the classes to add method entry hooks.
 */
public final class InlineStaticMockMaker implements MockMaker {
    /**
     * {@link StaticJvmtiAgent} set up during one time init
     */
    private static final StaticJvmtiAgent AGENT;

    /**
     * Error  during one time init or {@code null} if init was successful
     */
    private static final Throwable INITIALIZATION_ERROR;
    public static ThreadLocal<Class> mockingInProgressClass = new ThreadLocal<>();
    public static ThreadLocal<BiConsumer<Class<?>, Method>> onMethodCallDuringStubbing
            = new ThreadLocal<>();

    /*
     * One time setup to allow the system to mocking via this mock maker.
     */
    static {
        StaticJvmtiAgent agent;
        Throwable initializationError = null;

        try {
            try {
                agent = new StaticJvmtiAgent();
            } catch (IOException ioe) {
                throw new IllegalStateException("Mockito could not self-attach a jvmti agent to " +
                        "the current VM. This feature is required for inline mocking.\nThis error" +
                        " occured due to an I/O error during the creation of this agent: " + ioe
                        + "\n\nPotentially, the current VM does not support the jvmti API " +
                        "correctly", ioe);
            }
        } catch (Throwable throwable) {
            agent = null;
            initializationError = throwable;
        }

        AGENT = agent;
        INITIALIZATION_ERROR = initializationError;
    }

    /**
     * All currently active mock markers. We modify the class's byte code. Some objects of the class
     * are modified, some are not. This list helps the {@link MockMethodAdvice} help figure out if a
     * object's method calls should be intercepted.
     */
    private final HashMap<Object, InvocationHandlerAdapter> markerToHandler = new HashMap<>();
    private final Map<Class, Object> classToMarker = new HashMap<>();

    /**
     * Class doing the actual byte code transformation.
     */
    private final StaticClassTransformer classTransformer;

    /**
     * Create a new mock maker.
     */
    public InlineStaticMockMaker() {
        if (INITIALIZATION_ERROR != null) {
            throw new RuntimeException("Could not initialize inline mock maker.\n" + "\n" +
                    "Release: Android " + Build.VERSION.RELEASE + " " + Build.VERSION.INCREMENTAL
                    + "Device: " + Build.BRAND + " " + Build.MODEL, INITIALIZATION_ERROR);
        }

        classTransformer = new StaticClassTransformer(AGENT, InlineDexmakerMockMaker
                .DISPATCHER_CLASS, markerToHandler, classToMarker);
    }

    @Override
    public <T> T createMock(MockCreationSettings<T> settings, MockHandler handler) {
        Class<T> typeToMock = settings.getTypeToMock();
        if (!typeToMock.equals(mockingInProgressClass.get()) || Modifier.isAbstract(typeToMock
                .getModifiers())) {
            return null;
        }

        Set<Class<?>> interfacesSet = settings.getExtraInterfaces();
        InvocationHandlerAdapter handlerAdapter = new InvocationHandlerAdapter(handler);

        classTransformer.mockClass(MockFeatures.withMockFeatures(typeToMock, interfacesSet));

        Instantiator instantiator = Mockito.framework().getPlugins().getDefaultPlugin
                (InstantiatorProvider2.class).getInstantiator(settings);

        T mock;
        try {
            mock = instantiator.newInstance(typeToMock);
        } catch (org.mockito.creation.instance.InstantiationException e) {
            throw new MockitoException("Unable to create mock instance of type '" + typeToMock
                    .getSimpleName() + "'", e);
        }

        if (classToMarker.containsKey(typeToMock)) {
            throw new MockitoException(typeToMock + " is already mocked");
        }
        classToMarker.put(typeToMock, mock);

        markerToHandler.put(mock, handlerAdapter);
        return mock;
    }

    @Override
    public void resetMock(Object mock, MockHandler newHandler, MockCreationSettings settings) {
        InvocationHandlerAdapter adapter = getInvocationHandlerAdapter(mock);
        if (adapter != null) {
            if (mockingInProgressClass.get() == mock.getClass()) {
                markerToHandler.remove(mock);
                classToMarker.remove(mock.getClass());
            } else {
                adapter.setHandler(newHandler);
            }
        }
    }

    @Override
    public TypeMockability isTypeMockable(final Class<?> type) {
        if (mockingInProgressClass.get() == type) {
            return new TypeMockability() {
                @Override
                public boolean mockable() {
                    return !Modifier.isAbstract(type.getModifiers()) && !type.isPrimitive() && type
                            != String.class;
                }

                @Override
                public String nonMockableReason() {
                    if (Modifier.isAbstract(type.getModifiers())) {
                        return "abstract type";
                    }

                    if (type.isPrimitive()) {
                        return "primitive type";
                    }

                    if (type == String.class) {
                        return "string";
                    }

                    return "not handled type";
                }
            };
        } else {
            return null;
        }
    }

    @Override
    public MockHandler getHandler(Object mock) {
        InvocationHandlerAdapter adapter = getInvocationHandlerAdapter(mock);
        return adapter != null ? adapter.getHandler() : null;
    }

    /**
     * Get the {@link InvocationHandlerAdapter} registered for a marker.
     *
     * @param marker marker of the class that might have mocking set up
     * @return adapter for this class, or {@code null} if not mocked
     */
    private InvocationHandlerAdapter getInvocationHandlerAdapter(Object marker) {
        if (marker == null) {
            return null;
        }

        return markerToHandler.get(marker);
    }
}
