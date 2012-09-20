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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.mockito.internal.invocation.InvocationImpl;
import org.mockito.internal.invocation.MockitoMethod;
import org.mockito.internal.invocation.realmethod.RealMethod;
import org.mockito.internal.progress.SequenceNumber;
import org.mockito.internal.util.ObjectMethodsGuru;
import org.mockito.invocation.MockHandler;

/**
 * Handles proxy method invocations to dexmaker's InvocationHandler by calling
 * a MockitoInvocationHandler.
 */
final class InvocationHandlerAdapter implements InvocationHandler {
    private MockHandler handler;
    private final ObjectMethodsGuru objectMethodsGuru = new ObjectMethodsGuru();

    public InvocationHandlerAdapter(MockHandler handler) {
        this.handler = handler;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (objectMethodsGuru.isEqualsMethod(method)) {
            return proxy == args[0];
        } else if (objectMethodsGuru.isHashCodeMethod(method)) {
            return System.identityHashCode(proxy);
        }

        if (args == null) {
            throw new IllegalArgumentException();
        }


        ProxiedMethod proxiedMethod = new ProxiedMethod(method);
        return handler.handle(new InvocationImpl(proxy, proxiedMethod, args, SequenceNumber.next(),
                proxiedMethod));
    }

    public MockHandler getHandler() {
        return handler;
    }

    public void setHandler(MockHandler handler) {
        this.handler = handler;
    }

    private static class ProxiedMethod implements MockitoMethod, RealMethod {
        private final Method method;

        public ProxiedMethod(Method method) {
            this.method = method;
        }

        public String getName() {
            return method.getName();
        }

        public Class<?> getReturnType() {
            return method.getReturnType();
        }

        public Class<?>[] getParameterTypes() {
            return method.getParameterTypes();
        }

        public Class<?>[] getExceptionTypes() {
            return method.getExceptionTypes();
        }

        public boolean isVarArgs() {
            return method.isVarArgs();
        }

        public Method getJavaMethod() {
            return method;
        }

        public Object invoke(Object target, Object[] arguments) throws Throwable {
            return ProxyBuilder.callSuper(target, method, arguments);
        }
    }
}
