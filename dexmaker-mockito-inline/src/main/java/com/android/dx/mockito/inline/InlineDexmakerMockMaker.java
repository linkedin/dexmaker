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

package com.android.dx.mockito.inline;

import com.android.dx.stock.ProxyBuilder;
import com.android.dx.stock.ProxyBuilder.MethodSetEntry;

import org.mockito.exceptions.base.MockitoException;
import org.mockito.internal.configuration.plugins.Plugins;
import org.mockito.internal.creation.instance.Instantiator;
import org.mockito.internal.util.Platform;
import org.mockito.internal.util.concurrent.WeakConcurrentMap;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockMaker;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

/**
 * Generates mock instances on Android's runtime that can mock final methods.
 *
 * <p>This is done by transforming the byte code of the classes to add method entry hooks.
 */
public final class InlineDexmakerMockMaker implements MockMaker {
    private static final String DISPATCHER_CLASS_NAME =
            "com.android.dx.mockito.inline.MockMethodDispatcher";
    private static final String DISPATCHER_JAR = "dispatcher.jar";

    /** {@link com.android.dx.mockito.inline.JvmtiAgent} set up during one time init */
    private static final JvmtiAgent AGENT;

    /** Error  during one time init or {@code null} if init was successful*/
    private static final Throwable INITIALIZATION_ERROR;

    /**
     * Class injected into the bootstrap classloader. All entry hooks added to methods will call
     * this class.
     */
    private static final Class DISPATCHER_CLASS;

    /*
     * One time setup to allow the system to mocking via this mock maker.
     */
    static {
        // Allow to mock package private classes
        System.setProperty("dexmaker.share_classloader", "true");

        JvmtiAgent agent;
        Throwable initializationError = null;
        Class dispatcherClass = null;
        try {
            try {
                agent = new JvmtiAgent();

                try (InputStream is = InlineDexmakerMockMaker.class.getClassLoader()
                        .getResource(DISPATCHER_JAR).openStream()) {
                    agent.appendToBootstrapClassLoaderSearch(is);
                }

                try {
                    dispatcherClass = Class.forName(DISPATCHER_CLASS_NAME, true,
                            Object.class.getClassLoader());

                    if (dispatcherClass == null) {
                        throw new IllegalStateException(DISPATCHER_CLASS_NAME
                                + " could not be loaded");
                    }
                } catch (ClassNotFoundException cnfe) {
                    throw new IllegalStateException(
                            "Mockito failed to inject the MockMethodDispatcher class into the "
                            + "bootstrap class loader\n\nIt seems like your current VM does not "
                            + "support the jvmti API correctly.", cnfe);
                }
            } catch (IOException ioe) {
                throw new IllegalStateException(
                        "Mockito could not self-attach a jvmti agent to the current VM. This "
                        + "feature is required for inline mocking.\nThis error occured due to an "
                        + "I/O error during the creation of this agent: " + ioe + "\n\n"
                        + "Potentially, the current VM does not support the jvmti API correctly",
                        ioe);
            }
        } catch (Throwable throwable) {
            agent = null;
            initializationError = throwable;
        }

        AGENT = agent;
        INITIALIZATION_ERROR = initializationError;
        DISPATCHER_CLASS = dispatcherClass;
    }

    /**
     * All currently active mocks. We modify the class's byte code. Some objects of the class are
     * modified, some are not. This list helps the {@link MockMethodAdvice} help figure out if a
     * object's method calls should be intercepted.
     */
    private final WeakConcurrentMap<Object, InvocationHandlerAdapter> mocks;

    /**
     * Class doing the actual byte code transformation.
     */
    private final ClassTransformer classTransformer;

    /**
     * Create a new mock maker.
     */
    public InlineDexmakerMockMaker() {
        if (INITIALIZATION_ERROR != null) {
            throw new RuntimeException(
                    "Could not initialize inline mock maker.\n"
                    + "\n"
                    + Platform.describe(), INITIALIZATION_ERROR);
        }

        mocks = new WeakConcurrentMap.WithInlinedExpunction<>();
        classTransformer = new ClassTransformer(AGENT, DISPATCHER_CLASS, mocks);
    }

    /**
     * Get methods to proxy.
     *
     * <p>Only abstract methods will need to get proxied as all other methods will get an entry
     * hook.
     *
     * @param settings description of the current mocking process.
     *
     * @return methods to proxy.
     */
    private <T> Method[] getMethodsToProxy(MockCreationSettings<T> settings) {
        Set<MethodSetEntry> abstractMethods = new HashSet<>();
        Set<MethodSetEntry> nonAbstractMethods = new HashSet<>();

        Class<?> superClass = settings.getTypeToMock();
        while (superClass != null) {
            for (Method method : superClass.getDeclaredMethods()) {
                if (Modifier.isAbstract(method.getModifiers())
                        && !nonAbstractMethods.contains(new MethodSetEntry(method))) {
                    abstractMethods.add(new MethodSetEntry(method));
                } else {
                    nonAbstractMethods.add(new MethodSetEntry(method));
                }
            }

            superClass = superClass.getSuperclass();
        }

        for (Class<?> i : settings.getTypeToMock().getInterfaces()) {
            for (Method method : i.getMethods()) {
                if (!nonAbstractMethods.contains(new MethodSetEntry(method))) {
                    abstractMethods.add(new MethodSetEntry(method));
                }
            }
        }

        for (Class<?> i : settings.getExtraInterfaces()) {
            for (Method method : i.getMethods()) {
                if (!nonAbstractMethods.contains(new MethodSetEntry(method))) {
                    abstractMethods.add(new MethodSetEntry(method));
                }
            }
        }

        Method[] methodsToProxy = new Method[abstractMethods.size()];
        int i = 0;
        for (MethodSetEntry entry : abstractMethods) {
            methodsToProxy[i++] = entry.originalMethod;
        }

        return methodsToProxy;
    }

    @Override
    public <T> T createMock(MockCreationSettings<T> settings, MockHandler handler) {
        Class<T> typeToMock = settings.getTypeToMock();
        Set<Class<?>> interfacesSet = settings.getExtraInterfaces();
        Class<?>[] extraInterfaces = interfacesSet.toArray(new Class[interfacesSet.size()]);
        InvocationHandlerAdapter handlerAdapter = new InvocationHandlerAdapter(handler);

        T mock;
        if (typeToMock.isInterface()) {
            // support interfaces via java.lang.reflect.Proxy
            Class[] classesToMock = new Class[extraInterfaces.length + 1];
            classesToMock[0] = typeToMock;
            System.arraycopy(extraInterfaces, 0, classesToMock, 1, extraInterfaces.length);

            // newProxyInstance returns the type of typeToMock
            mock = (T) Proxy.newProxyInstance(typeToMock.getClassLoader(), classesToMock,
                    handlerAdapter);
        } else {
            boolean subclassingRequired = !interfacesSet.isEmpty()
                    || Modifier.isAbstract(typeToMock.getModifiers());

            // Add entry hooks to non-abstract methods.
            classTransformer.mockClass(MockFeatures.withMockFeatures(typeToMock, interfacesSet));

            Class<? extends T> proxyClass;

            Instantiator instantiator = Plugins.getInstantiatorProvider().getInstantiator(settings);

            if (subclassingRequired) {
                try {
                    // support abstract methods via dexmaker's ProxyBuilder
                    proxyClass = ProxyBuilder.forClass(typeToMock).implementing(extraInterfaces)
                            .onlyMethods(getMethodsToProxy(settings)).buildProxyClass();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new MockitoException("Failed to mock " + typeToMock, e);
                }

                try {
                    mock = instantiator.newInstance(proxyClass);
                } catch (org.mockito.internal.creation.instance.InstantiationException e) {
                    throw new MockitoException("Unable to create mock instance of type '"
                            + proxyClass.getSuperclass().getSimpleName() + "'", e);
                }

                ProxyBuilder.setInvocationHandler(mock, handlerAdapter);
            } else {
                try {
                    mock = instantiator.newInstance(typeToMock);
                } catch (org.mockito.internal.creation.instance.InstantiationException e) {
                    throw new MockitoException("Unable to create mock instance of type '"
                            + typeToMock.getSimpleName() + "'", e);
                }
            }
        }

        mocks.put(mock, handlerAdapter);
        return mock;
    }

    @Override
    public void resetMock(Object mock, MockHandler newHandler, MockCreationSettings settings) {
        InvocationHandlerAdapter adapter = getInvocationHandlerAdapter(mock);
        if (adapter != null) {
            adapter.setHandler(newHandler);
        }
    }

    @Override
    public TypeMockability isTypeMockable(final Class<?> type) {
        return new TypeMockability() {
            @Override
            public boolean mockable() {
                return !type.isPrimitive() && type != String.class;
            }

            @Override
            public String nonMockableReason() {
                if (type.isPrimitive()) {
                    return "primitive type";
                }

                if (type == String.class) {
                    return "string";
                }

                return "not handled type";
            }
        };
    }

    @Override
    public MockHandler getHandler(Object mock) {
        InvocationHandlerAdapter adapter = getInvocationHandlerAdapter(mock);
        return adapter != null ? adapter.getHandler() : null;
    }

    /**
     * Get the {@link InvocationHandlerAdapter} registered for a mock.
     *
     * @param instance instance that might be mocked
     *
     * @return adapter for this mock, or {@code null} if instance is not mocked
     */
    private InvocationHandlerAdapter getInvocationHandlerAdapter(Object instance) {
        if (instance == null) {
            return null;
        }

        return mocks.get(instance);
    }
}
