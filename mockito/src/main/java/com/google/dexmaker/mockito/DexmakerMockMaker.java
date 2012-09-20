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

package com.google.dexmaker.mockito;

import com.google.dexmaker.stock.ProxyBuilder;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Set;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.exceptions.stacktrace.StackTraceCleaner;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockMaker;
import org.mockito.plugins.StackTraceCleanerProvider;

/**
 * Generates mock instances on Android's runtime.
 */
public final class DexmakerMockMaker implements MockMaker, StackTraceCleanerProvider {
    private final UnsafeAllocator unsafeAllocator = UnsafeAllocator.create();

    public <T> T createMock(MockCreationSettings<T> settings, MockHandler handler) {
        Class<T> typeToMock = settings.getTypeToMock();
        Set<Class> interfacesSet = settings.getExtraInterfaces();
        Class<?>[] extraInterfaces = interfacesSet.toArray(new Class[interfacesSet.size()]);
        InvocationHandler invocationHandler = new InvocationHandlerAdapter(handler);

        if (typeToMock.isInterface()) {
            // support interfaces via java.lang.reflect.Proxy
            Class[] classesToMock = new Class[extraInterfaces.length + 1];
            classesToMock[0] = typeToMock;
            System.arraycopy(extraInterfaces, 0, classesToMock, 1, extraInterfaces.length);
            @SuppressWarnings("unchecked") // newProxyInstance returns the type of typeToMock
            T mock = (T) Proxy.newProxyInstance(typeToMock.getClassLoader(),
                    classesToMock, invocationHandler);
            return mock;

        } else {
            // support concrete classes via dexmaker's ProxyBuilder
            try {
                Class<? extends T> proxyClass = ProxyBuilder.forClass(typeToMock)
                        .implementing(extraInterfaces)
                        .buildProxyClass();
                T mock = unsafeAllocator.newInstance(proxyClass);
                Field handlerField = proxyClass.getDeclaredField("$__handler");
                handlerField.setAccessible(true);
                handlerField.set(mock, invocationHandler);
                return mock;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new MockitoException("Failed to mock " + typeToMock, e);
            }
        }
    }

    public void resetMock(Object mock, MockHandler newHandler, MockCreationSettings settings) {
        InvocationHandlerAdapter adapter = getInvocationHandlerAdapter(mock);
        adapter.setHandler(newHandler);
    }

    public MockHandler getHandler(Object mock) {
        InvocationHandlerAdapter adapter = getInvocationHandlerAdapter(mock);
        return adapter != null ? adapter.getHandler() : null;
    }

    private InvocationHandlerAdapter getInvocationHandlerAdapter(Object mock) {
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

    public StackTraceCleaner getStackTraceCleaner(final StackTraceCleaner defaultCleaner) {
        return new StackTraceCleaner() {
            public boolean isOut(StackTraceElement candidate) {
                return defaultCleaner.isOut(candidate)
                        || candidate.getClassName().endsWith("_Proxy") // dexmaker class proxies
                        || candidate.getClassName().startsWith("$Proxy") // dalvik interface proxies
                        || candidate.getClassName().startsWith("com.google.dexmaker.mockito.");
            }
        };
    }
}
