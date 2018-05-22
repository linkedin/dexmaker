/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.dx.mockito;

import com.android.dx.stock.ProxyBuilder;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.exceptions.stacktrace.StackTraceCleaner;
import org.mockito.internal.util.reflection.LenientCopyTool;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockMaker;
import org.mockito.plugins.StackTraceCleanerProvider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Set;

/**
 * Generates mock instances on Android's runtime.
 */
public final class DexmakerMockMaker implements MockMaker, StackTraceCleanerProvider {
    private final UnsafeAllocator unsafeAllocator = UnsafeAllocator.create();
    private final boolean isApi28;

    public DexmakerMockMaker() throws Exception {
        Class buildVersion = Class.forName("android.os.Build$VERSION");

        isApi28 = buildVersion.getDeclaredField("SDK_INT").getInt(null) >= 28;

        if (isApi28) {
            // Blacklisted APIs were introduced in Android P:
            //
            // https://android-developers.googleblog.com/2018/02/
            // improving-stability-by-reducing-usage.html
            //
            // This feature prevents access to blacklisted fields and calling of blacklisted APIs
            // if the calling class is not trusted.
            Method allowHiddenApiReflectionFromMethod;
            try {
                Class vmDebug = Class.forName("dalvik.system.VMDebug");
                allowHiddenApiReflectionFromMethod = vmDebug.getDeclaredMethod(
                        "allowHiddenApiReflectionFrom", Class.class);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                throw new IllegalStateException(
                        "Cannot find VMDebug#allowHiddenApiReflectionFrom. Method is needed to "
                                + "allow spies to copy blacklisted fields.");
            }

            // The LenientCopyTool copies the fields to a spy when creating the copy from an
            // existing object. Some of the fields might be blacklisted. Marking the LenientCopyTool
            // as trusted allows the tool to copy all fields, including the blacklisted ones.
            try {
                allowHiddenApiReflectionFromMethod.invoke(null, LenientCopyTool.class);
            } catch (InvocationTargetException | IllegalAccessException e) {
                System.err.println("Cannot allow LenientCopyTool to copy spies of blacklisted "
                        + "fields. This might break spying on system classes.");
            }
        }
    }

    @Override
    public <T> T createMock(MockCreationSettings<T> settings, MockHandler handler) {
        Class<T> typeToMock = settings.getTypeToMock();
        Set<Class<?>> interfacesSet = settings.getExtraInterfaces();
        Class<?>[] extraInterfaces = interfacesSet.toArray(new Class[interfacesSet.size()]);
        InvocationHandler invocationHandler = new InvocationHandlerAdapter(handler);

        if (typeToMock.isInterface()) {
            // support interfaces via java.lang.reflect.Proxy
            Class[] classesToMock = new Class[extraInterfaces.length + 1];
            classesToMock[0] = typeToMock;
            System.arraycopy(extraInterfaces, 0, classesToMock, 1, extraInterfaces.length);
            // newProxyInstance returns the type of typeToMock
            @SuppressWarnings("unchecked")
            T mock = (T) Proxy.newProxyInstance(typeToMock.getClassLoader(), classesToMock, invocationHandler);
            return mock;

        } else {
            // support concrete classes via dexmaker's ProxyBuilder
            try {
                ProxyBuilder builder = ProxyBuilder.forClass(typeToMock)
                        .implementing(extraInterfaces);

                if (isApi28) {
                    builder.markTrusted();
                }

                Class<? extends T> proxyClass = builder.buildProxyClass();
                T mock = unsafeAllocator.newInstance(proxyClass);
                ProxyBuilder.setInvocationHandler(mock, invocationHandler);
                return mock;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new MockitoException("Failed to mock " + typeToMock, e);
            }
        }
    }

    @Override
    public void resetMock(Object mock, MockHandler newHandler, MockCreationSettings settings) {
        InvocationHandlerAdapter adapter = getInvocationHandlerAdapter(mock);
        adapter.setHandler(newHandler);
    }

    @Override
    public TypeMockability isTypeMockable(final Class<?> type) {
        return new TypeMockability() {
            @Override
            public boolean mockable() {
                return !type.isPrimitive() && !Modifier.isFinal(type.getModifiers());
            }

            @Override
            public String nonMockableReason() {
                if (type.isPrimitive()) {
                    return "primitive type";
                }

                if (Modifier.isFinal(type.getModifiers())) {
                    return "final or anonymous class";
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

    @Override
    public StackTraceCleaner getStackTraceCleaner(final StackTraceCleaner defaultCleaner) {
        return new StackTraceCleaner() {
            @Override
            public boolean isIn(StackTraceElement candidate) {
                String className = candidate.getClassName();

                return defaultCleaner.isIn(candidate)
                        && !className.endsWith("_Proxy") // dexmaker class proxies
                        && !className.startsWith("$Proxy") // dalvik interface proxies
                        && !className.startsWith("java.lang.reflect.Proxy")
                        && !(className.startsWith("com.android.dx.mockito.")
                             // Do not clean unit tests
                             && !className.startsWith("com.android.dx.mockito.tests"));
            }
        };
    }

    private InvocationHandlerAdapter getInvocationHandlerAdapter(Object mock) {
        if (mock == null) {
            return null;
        }
        if (Proxy.isProxyClass(mock.getClass())) {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(mock);
            return invocationHandler instanceof InvocationHandlerAdapter
                    ? (InvocationHandlerAdapter) invocationHandler
                    : null;
        }

        if (ProxyBuilder.isProxyClass(mock.getClass())) {
            InvocationHandler invocationHandler = ProxyBuilder.getInvocationHandler(mock);
            return invocationHandler instanceof InvocationHandlerAdapter
                    ? (InvocationHandlerAdapter) invocationHandler
                    : null;
        }

        return null;
    }
}
