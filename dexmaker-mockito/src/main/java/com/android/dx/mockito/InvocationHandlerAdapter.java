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

import org.mockito.internal.creation.DelegatingMethod;
import org.mockito.internal.debugging.LocationImpl;
import org.mockito.internal.exceptions.VerificationAwareInvocation;
import org.mockito.internal.invocation.ArgumentsProcessor;
import org.mockito.internal.progress.SequenceNumber;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.Location;
import org.mockito.invocation.MockHandler;
import org.mockito.invocation.StubInfo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.mockito.internal.exceptions.Reporter.cannotCallAbstractRealMethod;

/**
 * Handles proxy method invocations to dexmaker's InvocationHandler by calling
 * a MockitoInvocationHandler.
 */
final class InvocationHandlerAdapter implements InvocationHandler {
    private MockHandler handler;

    public InvocationHandlerAdapter(MockHandler handler) {
        this.handler = handler;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // args can be null if the method invoked has no arguments, but Mockito expects a non-null array
        args = args != null ? args : new Object[0];
        if (isEqualsMethod(method)) {
            return proxy == args[0];
        } else if (isHashCodeMethod(method)) {
            return System.identityHashCode(proxy);
        }

        return handler.handle(new ProxyInvocation(proxy, method, args, new DelegatingMethod
                (method), SequenceNumber.next(), new LocationImpl()));
    }

    public MockHandler getHandler() {
        return handler;
    }

    public void setHandler(MockHandler handler) {
        this.handler = handler;
    }

    private static boolean isEqualsMethod(Method method) {
        return method.getName().equals("equals")
                && method.getParameterTypes().length == 1
                && method.getParameterTypes()[0] == Object.class;
    }

    private static boolean isHashCodeMethod(Method method) {
        return method.getName().equals("hashCode")
                && method.getParameterTypes().length == 0;
    }

    /**
     * Invocation on a proxy
     */
    private class ProxyInvocation implements Invocation, VerificationAwareInvocation {
        private final Object proxy;
        private final Method method;
        private final Object[] rawArgs;
        private final int sequenceNumber;
        private final Location location;
        private final Object[] args;

        private StubInfo stubInfo;
        private boolean isIgnoredForVerification;
        private boolean verified;

        private ProxyInvocation(Object proxy, Method method, Object[] rawArgs, DelegatingMethod
                mockitoMethod, int sequenceNumber, Location location) {
            this.rawArgs = rawArgs;
            this.proxy = proxy;
            this.method = method;
            this.sequenceNumber = sequenceNumber;
            this.location = location;
            args = ArgumentsProcessor.expandArgs(mockitoMethod, rawArgs);
        }

        @Override
        public Object getMock() {
            return proxy;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Object[] getArguments() {
            return args;
        }

        @Override
        public <T> T getArgument(int index) {
            return (T)args[index];
        }

        @Override
        public Object callRealMethod() throws Throwable {
            if (Modifier.isAbstract(method.getModifiers())) {
                throw cannotCallAbstractRealMethod();
            }
            return ProxyBuilder.callSuper(proxy, method, rawArgs);
        }

        @Override
        public boolean isVerified() {
            return verified || isIgnoredForVerification;
        }

        @Override
        public int getSequenceNumber() {
            return sequenceNumber;
        }

        @Override
        public Location getLocation() {
            return location;
        }

        @Override
        public Object[] getRawArguments() {
            return rawArgs;
        }

        @Override
        public Class<?> getRawReturnType() {
            return method.getReturnType();
        }

        @Override
        public void markVerified() {
            verified = true;
        }

        @Override
        public StubInfo stubInfo() {
            return stubInfo;
        }

        @Override
        public void markStubbed(StubInfo stubInfo) {
            this.stubInfo = stubInfo;
        }

        @Override
        public boolean isIgnoredForVerification() {
            return isIgnoredForVerification;
        }

        @Override
        public void ignoreForVerification() {
            isIgnoredForVerification = true;
        }
    }
}
