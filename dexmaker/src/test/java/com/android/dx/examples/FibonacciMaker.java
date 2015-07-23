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

package com.android.dx.examples;

import com.android.dx.Code;
import com.android.dx.Comparison;
import com.android.dx.DexMaker;
import com.android.dx.Label;
import com.android.dx.Local;
import com.android.dx.MethodId;
import com.android.dx.TypeId;
import com.android.dx.BinaryOp;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class FibonacciMaker {
    public static void main(String[] args) throws Exception {
        TypeId<?> fibonacci = TypeId.get("Lcom/google/dexmaker/examples/Fibonacci;");

        String fileName = "Fibonacci.generated";
        DexMaker dexMaker = new DexMaker();
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
        Label baseCase = new Label();
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

    public static File getDataDirectory() {
        String envVariable = "ANDROID_DATA";
        String defaultLoc = "/data";
        String path = System.getenv(envVariable);
        return path == null ? new File(defaultLoc) : new File(path);
    }
}
