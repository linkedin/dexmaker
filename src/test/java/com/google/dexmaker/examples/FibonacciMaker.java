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

package com.google.dexmaker.examples;

import com.google.dexmaker.BinaryOp;
import com.google.dexmaker.Code;
import com.google.dexmaker.Comparison;
import com.google.dexmaker.DexMaker;
import com.google.dexmaker.Label;
import com.google.dexmaker.Local;
import com.google.dexmaker.MethodId;
import com.google.dexmaker.TypeId;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class FibonacciMaker {
    public static void main(String[] args) throws Exception {
        DexMaker dexMaker = new DexMaker();
        TypeId<?> fibonacci = TypeId.get("Lcom/google/dexmaker/examples/Fibonacci;");
        String fileName = "Fibonacci.generated";
        dexMaker.declare(fibonacci, fileName, Modifier.PUBLIC, TypeId.OBJECT);

        MethodId<?, Integer> fib = fibonacci.getMethod(TypeId.INT, "fib", TypeId.INT);
        Code code = dexMaker.declare(fib, Modifier.PUBLIC | Modifier.STATIC);
        Local<Integer> i = code.getParameter(0, TypeId.INT);
        Local<Integer> constant1 = code.newLocal(TypeId.INT);
        Local<Integer> constant2 = code.newLocal(TypeId.INT);
        Local<Integer> a = code.newLocal(TypeId.INT);
        Local<Integer> b = code.newLocal(TypeId.INT);
        Local<Integer> c = code.newLocal(TypeId.INT);
        Local<Integer> d = code.newLocal(TypeId.INT);
        Local<Integer> result = code.newLocal(TypeId.INT);
        code.loadConstant(constant1, 1);
        code.loadConstant(constant2, 2);
        Label baseCase = code.newLabel();
        code.compare(Comparison.LT, baseCase, i, constant2);
        code.op(BinaryOp.SUBTRACT, a, i, constant1);
        code.op(BinaryOp.SUBTRACT, b, i, constant2);
        code.invokeStatic(fib, c, a);
        code.invokeStatic(fib, d, b);
        code.op(BinaryOp.ADD, result, c, d);
        code.returnValue(result);
        code.mark(baseCase);
        code.returnValue(i);

        ClassLoader loader = dexMaker.generateAndLoad(
                FibonacciMaker.class.getClassLoader(), getDataDirectory());
        Class<?> fibonacciClass = loader.loadClass("com.google.dexmaker.examples.Fibonacci");
        Method fibMethod = fibonacciClass.getMethod("fib", int.class);
        System.out.println(fibMethod.invoke(null, 8));
    }

    public static File getDataDirectory() throws Exception {
        Class<?> environmentClass = Class.forName("android.os.Environment");
        Method method = environmentClass.getMethod("getDataDirectory");
        Object dataDirectory = method.invoke(null);
        return (File) dataDirectory;
    }
}
