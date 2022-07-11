/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.dx;

import static com.android.dx.util.TestUtil.DELTA_DOUBLE;
import static com.android.dx.util.TestUtil.DELTA_FLOAT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import android.os.Build;

import androidx.test.InstrumentationRegistry;

import dalvik.system.BaseDexClassLoader;

import org.junit.Before;
import org.junit.Test;

import static java.lang.reflect.Modifier.ABSTRACT;
import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.NATIVE;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PROTECTED;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;
import static java.lang.reflect.Modifier.SYNCHRONIZED;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * This generates a class named 'Generated' with one or more generated methods
 * and fields. In loads the generated class into the current VM and uses
 * reflection to invoke its methods.
 *
 * <p>This test must run on a Dalvik VM.
 */
public final class DexMakerTest {
    private DexMaker dexMaker;
    private static TypeId<DexMakerTest> TEST_TYPE = TypeId.get(DexMakerTest.class);
    private static TypeId<?> INT_ARRAY = TypeId.get(int[].class);
    private static TypeId<boolean[]> BOOLEAN_ARRAY = TypeId.get(boolean[].class);
    private static TypeId<long[]> LONG_ARRAY = TypeId.get(long[].class);
    private static TypeId<Object[]> OBJECT_ARRAY = TypeId.get(Object[].class);
    private static TypeId<long[][]> LONG_2D_ARRAY = TypeId.get(long[][].class);
    private static TypeId<?> GENERATED = TypeId.get("LGenerated;");
    private static TypeId<Callable> CALLABLE = TypeId.get(Callable.class);
    private static MethodId<Callable, Object> CALL = CALLABLE.getMethod(TypeId.OBJECT, "call");

    @Before
    public void setup() {
        reset();
    }

    /**
     * The generator is mutable. Calling reset creates a new empty generator.
     * This is necessary to generate multiple classes in the same test method.
     */
    private void reset() {
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        clearDataDirectory();
    }

    private void clearDataDirectory() {
        for (File f : getDataDirectory().listFiles()) {
            if (f.getName().endsWith(".jar") || f.getName().endsWith(".dex")) {
                f.delete();
            }
        }
    }

    @Test
    public void testNewInstance() throws Exception {
        /*
         * public static Constructable call(long a, boolean b) {
         *   Constructable result = new Constructable(a, b);
         *   return result;
         * }
         */
        TypeId<Constructable> constructable = TypeId.get(Constructable.class);
        MethodId<?, Constructable> methodId = GENERATED.getMethod(
                constructable, "call", TypeId.LONG, TypeId.BOOLEAN);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Long> localA = code.getParameter(0, TypeId.LONG);
        Local<Boolean> localB = code.getParameter(1, TypeId.BOOLEAN);
        MethodId<Constructable, Void> constructor
                = constructable.getConstructor(TypeId.LONG, TypeId.BOOLEAN);
        Local<Constructable> localResult = code.newLocal(constructable);
        code.newInstance(localResult, constructor, localA, localB);
        code.returnValue(localResult);

        Constructable constructed = (Constructable) getMethod().invoke(null, 5L, false);
        assertEquals(5L, constructed.a);
        assertEquals(false, constructed.b);
    }

    public static class Constructable {
        private final long a;
        private final boolean b;
        public Constructable(long a, boolean b) {
            this.a = a;
            this.b = b;
        }
    }

    @Test
    public void testVoidNoArgMemberMethod() throws Exception {
        /*
         * public void call() {
         * }
         */
        MethodId<?, Void> methodId = GENERATED.getMethod(TypeId.VOID, "call");
        Code code = dexMaker.declare(methodId, PUBLIC);
        code.returnVoid();

        addDefaultConstructor();

        Class<?> generatedClass = generateAndLoad();
        Object instance = generatedClass.getDeclaredConstructor().newInstance();
        Method method = generatedClass.getMethod("call");
        method.invoke(instance);
    }

    @Test
    public void testInvokeStatic() throws Exception {
        /*
         * public static int call(int a) {
         *   int result = DexMakerTest.staticMethod(a);
         *   return result;
         * }
         */
        MethodId<?, Integer> methodId = GENERATED.getMethod(TypeId.INT, "call", TypeId.INT);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localA = code.getParameter(0, TypeId.INT);
        Local<Integer> localResult = code.newLocal(TypeId.INT);
        MethodId<?, Integer> staticMethod
                = TEST_TYPE.getMethod(TypeId.INT, "staticMethod", TypeId.INT);
        code.invokeStatic(staticMethod, localResult, localA);
        code.returnValue(localResult);

        assertEquals(10, getMethod().invoke(null, 4));
    }

    @Test
    public void testLoadDeferredClassConstant() throws Exception {
        /*
         * public static String call() {
         *   Class clazz = Generated.class;
         *   return clazz.getSimpleName();
         * }
         */
        MethodId<?, String> methodId = GENERATED.getMethod(TypeId.STRING, "call");
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Class> clazz = code.newLocal(TypeId.get(Class.class));
        Local<String> retValue = code.newLocal(TypeId.STRING);
        code.loadDeferredClassConstant(clazz, GENERATED);
        MethodId<Class, String> getSimpleName = TypeId.get(Class.class).getMethod(TypeId.STRING, "getSimpleName");
        code.invokeVirtual(getSimpleName, retValue, clazz);
        code.returnValue(retValue);

        assertEquals("Generated", getMethod().invoke(null));
    }

    @Test
    public void testCreateLocalMethodAsNull() throws Exception {
        /*
         * public void call(int value) {
         *   Method method = null;
         * }
         */
        MethodId<?, Void> methodId = GENERATED.getMethod(TypeId.VOID, "call", TypeId.INT);
        TypeId<Method> methodType = TypeId.get(Method.class);
        Code code = dexMaker.declare(methodId, PUBLIC);
        Local<Method> localMethod = code.newLocal(methodType);
        code.loadConstant(localMethod, null);
        code.returnVoid();

        addDefaultConstructor();

        Class<?> generatedClass = generateAndLoad();
        Object instance = generatedClass.getDeclaredConstructor().newInstance();
        Method method = generatedClass.getMethod("call", int.class);
        method.invoke(instance, 0);
    }

    @SuppressWarnings("unused") // called by generated code
    public static int staticMethod(int a) {
        return a + 6;
    }

    @Test
    public void testInvokeVirtual() throws Exception {
        /*
         * public static int call(DexMakerTest test, int a) {
         *   int result = test.virtualMethod(a);
         *   return result;
         * }
         */
        MethodId<?, Integer> methodId = GENERATED.getMethod(TypeId.INT, "call", TEST_TYPE, TypeId.INT);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<DexMakerTest> localInstance = code.getParameter(0, TEST_TYPE);
        Local<Integer> localA = code.getParameter(1, TypeId.INT);
        Local<Integer> localResult = code.newLocal(TypeId.INT);
        MethodId<DexMakerTest, Integer> virtualMethod
                = TEST_TYPE.getMethod(TypeId.INT, "virtualMethod", TypeId.INT);
        code.invokeVirtual(virtualMethod, localResult, localInstance, localA);
        code.returnValue(localResult);

        assertEquals(9, getMethod().invoke(null, this, 4));
    }

    @SuppressWarnings("unused") // called by generated code
    public int virtualMethod(int a) {
        return a + 5;
    }

    @Test
    public <G> void testInvokeDirect() throws Exception {
        /*
         * private int directMethod() {
         *   int a = 5;
         *   return a;
         * }
         *
         * public static int call(Generated g) {
         *   int b = g.directMethod();
         *   return b;
         * }
         */
        TypeId<G> generated = TypeId.get("LGenerated;");
        MethodId<G, Integer> directMethodId = generated.getMethod(TypeId.INT, "directMethod");
        Code directCode = dexMaker.declare(directMethodId, PRIVATE);
        directCode.getThis(generated); // 'this' is unused
        Local<Integer> localA = directCode.newLocal(TypeId.INT);
        directCode.loadConstant(localA, 5);
        directCode.returnValue(localA);

        MethodId<G, Integer> methodId = generated.getMethod(TypeId.INT, "call", generated);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localB = code.newLocal(TypeId.INT);
        Local<G> localG = code.getParameter(0, generated);
        code.invokeDirect(directMethodId, localB, localG);
        code.returnValue(localB);

        addDefaultConstructor();

        Class<?> generatedClass = generateAndLoad();
        Object instance = generatedClass.getDeclaredConstructor().newInstance();
        Method method = generatedClass.getMethod("call", generatedClass);
        assertEquals(5, method.invoke(null, instance));
    }

    @Test
    public <G> void testInvokeSuper() throws Exception {
        /*
         * public int superHashCode() {
         *   int result = super.hashCode();
         *   return result;
         * }
         * public int hashCode() {
         *   return 0;
         * }
         */
        TypeId<G> generated = TypeId.get("LGenerated;");
        MethodId<Object, Integer> objectHashCode = TypeId.OBJECT.getMethod(TypeId.INT, "hashCode");
        Code superHashCode = dexMaker.declare(
                GENERATED.getMethod(TypeId.INT, "superHashCode"), PUBLIC);
        Local<Integer> localResult = superHashCode.newLocal(TypeId.INT);
        Local<G> localThis = superHashCode.getThis(generated);
        superHashCode.invokeSuper(objectHashCode, localResult, localThis);
        superHashCode.returnValue(localResult);

        Code generatedHashCode = dexMaker.declare(
                GENERATED.getMethod(TypeId.INT, "hashCode"), PUBLIC);
        Local<Integer> localZero = generatedHashCode.newLocal(TypeId.INT);
        generatedHashCode.loadConstant(localZero, 0);
        generatedHashCode.returnValue(localZero);

        addDefaultConstructor();

        Class<?> generatedClass = generateAndLoad();
        Object instance = generatedClass.getDeclaredConstructor().newInstance();
        Method method = generatedClass.getMethod("superHashCode");
        assertEquals(System.identityHashCode(instance), method.invoke(instance));
    }

    @Test
    public void testInvokeInterface() throws Exception {
        /*
         * public static Object call(Callable c) {
         *   Object result = c.call();
         *   return result;
         * }
         */
        MethodId<?, Object> methodId = GENERATED.getMethod(TypeId.OBJECT, "call", CALLABLE);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Callable> localC = code.getParameter(0, CALLABLE);
        Local<Object> localResult = code.newLocal(TypeId.OBJECT);
        code.invokeInterface(CALL, localResult, localC);
        code.returnValue(localResult);

        Callable<Object> callable = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return "abc";
            }
        };
        assertEquals("abc", getMethod().invoke(null, callable));
    }

    @Test
    public void testInvokeVoidMethodIgnoresTargetLocal() throws Exception {
        /*
         * public static int call() {
         *   int result = 5;
         *   DexMakerTest.voidMethod();
         *   return result;
         * }
         */
        MethodId<?, Integer> methodId = GENERATED.getMethod(TypeId.INT, "call");
        MethodId<?, Void> voidMethod = TEST_TYPE.getMethod(TypeId.VOID, "voidMethod");
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Integer> result = code.newLocal(TypeId.INT);
        code.loadConstant(result, 5);
        code.invokeStatic(voidMethod, null);
        code.returnValue(result);

        assertEquals(5, getMethod().invoke(null));
    }

    @SuppressWarnings("unused") // called by generated code
    public static void voidMethod() {}

    @Test
    public void testParameterMismatch() throws Exception {
        TypeId<?>[] argTypes = {
                TypeId.get(Integer.class), // should fail because the code specifies int
                TypeId.OBJECT,
        };
        MethodId<?, Integer> methodId = GENERATED.getMethod(TypeId.INT, "call", argTypes);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        try {
            code.getParameter(0, TypeId.INT);
        } catch (IllegalArgumentException e) {
        }
        try {
            code.getParameter(2, TypeId.INT);
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testInvokeTypeSafety() throws Exception {
        /*
         * public static boolean call(DexMakerTest test) {
         *   CharSequence cs = test.toString();
         *   boolean result = cs.equals(test);
         *   return result;
         * }
         */
        MethodId<?, Boolean> methodId = GENERATED.getMethod(TypeId.BOOLEAN, "call", TEST_TYPE);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<DexMakerTest> localTest = code.getParameter(0, TEST_TYPE);
        TypeId<CharSequence> charSequenceType = TypeId.get(CharSequence.class);
        MethodId<Object, String> objectToString
                = TypeId.OBJECT.getMethod(TypeId.STRING, "toString");
        MethodId<Object, Boolean> objectEquals
                = TypeId.OBJECT.getMethod(TypeId.BOOLEAN, "equals", TypeId.OBJECT);
        Local<CharSequence> localCs = code.newLocal(charSequenceType);
        Local<Boolean> localResult = code.newLocal(TypeId.BOOLEAN);
        code.invokeVirtual(objectToString, localCs, localTest);
        code.invokeVirtual(objectEquals, localResult, localCs, localTest);
        code.returnValue(localResult);

        assertEquals(false, getMethod().invoke(null, this));
    }

    @Test
    public void testReturnTypeMismatch() {
        MethodId<?, String> methodId = GENERATED.getMethod(TypeId.STRING, "call");
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        try {
            code.returnValue(code.newLocal(TypeId.BOOLEAN));
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            code.returnVoid();
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testDeclareStaticFields() throws Exception {
        /*
         * class Generated {
         *   public static int a;
         *   protected static Object b;
         * }
         */
        dexMaker.declare(GENERATED.getField(TypeId.INT, "a"), PUBLIC | STATIC, 3);
        dexMaker.declare(GENERATED.getField(TypeId.OBJECT, "b"), PROTECTED | STATIC, null);
        Class<?> generatedClass = generateAndLoad();

        Field a = generatedClass.getField("a");
        assertEquals(int.class, a.getType());
        assertEquals(3, a.get(null));

        Field b = generatedClass.getDeclaredField("b");
        assertEquals(Object.class, b.getType());
        b.setAccessible(true);
        assertEquals(null, b.get(null));
    }

    @Test
    public void testDeclareInstanceFields() throws Exception {
        /*
         * class Generated {
         *   public int a;
         *   protected Object b;
         * }
         */
        dexMaker.declare(GENERATED.getField(TypeId.INT, "a"), PUBLIC, null);
        dexMaker.declare(GENERATED.getField(TypeId.OBJECT, "b"), PROTECTED, null);

        addDefaultConstructor();

        Class<?> generatedClass = generateAndLoad();
        Object instance = generatedClass.getDeclaredConstructor().newInstance();

        Field a = generatedClass.getField("a");
        assertEquals(int.class, a.getType());
        assertEquals(0, a.get(instance));

        Field b = generatedClass.getDeclaredField("b");
        assertEquals(Object.class, b.getType());
        b.setAccessible(true);
        assertEquals(null, b.get(instance));
    }

    /**
     * Declare a constructor that takes an int parameter and assigns it to a
     * field.
     */
    @Test
    public <G> void testDeclareConstructor() throws Exception {
        /*
         * class Generated {
         *   public final int a;
         *   public Generated(int a) {
         *     this.a = a;
         *   }
         * }
         */
        TypeId<G> generated = TypeId.get("LGenerated;");
        FieldId<G, Integer> fieldId = generated.getField(TypeId.INT, "a");
        dexMaker.declare(fieldId, PUBLIC | FINAL, null);
        MethodId<?, Void> constructor = GENERATED.getConstructor(TypeId.INT);
        Code code = dexMaker.declare(constructor, PUBLIC);
        Local<G> thisRef = code.getThis(generated);
        Local<Integer> parameter = code.getParameter(0, TypeId.INT);
        code.invokeDirect(TypeId.OBJECT.getConstructor(), null, thisRef);
        code.iput(fieldId, thisRef, parameter);
        code.returnVoid();

        Class<?> generatedClass = generateAndLoad();
        Field a = generatedClass.getField("a");
        Object instance = generatedClass.getConstructor(int.class).newInstance(0xabcd);
        assertEquals(0xabcd, a.get(instance));
    }

    @Test
    public void testDeclareNativeMethod() throws Exception {
        /*
         * class Generated {
         *   public Generated() {
         *   }
         *   public native void nativeMethod();
         * }
         */

        addDefaultConstructor();
        String nativeMethodName = "nativeMethod";
        MethodId<?, Void> nativeMethodToGenerate = GENERATED.getMethod(TypeId.VOID, nativeMethodName);
        dexMaker.declare(nativeMethodToGenerate, java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.NATIVE);

        Class<?> generatedClass = generateAndLoad();
        Object instance = generatedClass.getConstructor().newInstance();
        Method nativeMethod = instance.getClass().getMethod(nativeMethodName);

        assertTrue((nativeMethod.getModifiers() & NATIVE) != 0);
        assertTrue((nativeMethod.getModifiers() & PUBLIC) != 0);
        assertEquals(void.class, nativeMethod.getReturnType());
        assertEquals(nativeMethodName, nativeMethod.getName());
        assertEquals(nativeMethod.getParameterTypes().length, 0);
    }

    @Test
    public void testDeclareAbstractClassWithAbstractMethod() throws Exception {
        /*
         * public abstract class AbstractClass {
         *   public abstract void abstractMethod();
         * }
         */

        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "AbstractClass.java", PUBLIC, TypeId.OBJECT);

        String abstractMethodName = "abstractMethod";
        MethodId<?, Void> nativeMethodToGenerate = GENERATED.getMethod(TypeId.VOID, abstractMethodName);
        dexMaker.declare(nativeMethodToGenerate, java.lang.reflect.Modifier.PUBLIC | ABSTRACT);

        Class<?> generatedClass = generateAndLoad();
        Method nativeMethod = generatedClass.getMethod(abstractMethodName);

        assertTrue((nativeMethod.getModifiers() & ABSTRACT) != 0);
        assertTrue((nativeMethod.getModifiers() & PUBLIC) != 0);
        assertEquals(void.class, nativeMethod.getReturnType());
        assertEquals(abstractMethodName, nativeMethod.getName());
        assertEquals(nativeMethod.getParameterTypes().length, 0);

    }

    @Test
    public void testReturnType() throws Exception {
        testReturnType(boolean.class, true);
        testReturnType(byte.class, (byte) 5);
        testReturnType(char.class, 'E');
        testReturnType(double.class, 5.0);
        testReturnType(float.class, 5.0f);
        testReturnType(int.class, 5);
        testReturnType(long.class, 5L);
        testReturnType(short.class, (short) 5);
        testReturnType(void.class, null);
        testReturnType(String.class, "foo");
        testReturnType(Class.class, List.class);
    }

    private <T> void testReturnType(Class<T> javaType, T value) throws Exception {
        /*
         * public int call() {
         *   int a = 5;
         *   return a;
         * }
         */
        reset();
        TypeId<T> returnType = TypeId.get(javaType);
        Code code = dexMaker.declare(GENERATED.getMethod(returnType, "call"), PUBLIC | STATIC);
        if (value != null) {
            Local<T> i = code.newLocal(returnType);
            code.loadConstant(i, value);
            code.returnValue(i);
        } else {
            code.returnVoid();
        }

        Class<?> generatedClass = generateAndLoad();
        Method method = generatedClass.getMethod("call");
        assertEquals(javaType, method.getReturnType());
        assertEquals(value, method.invoke(null));
    }

    @Test
    public void testBranching() throws Exception {
        Method lt = branchingMethod(Comparison.LT);
        assertEquals(Boolean.TRUE, lt.invoke(null, 1, 2));
        assertEquals(Boolean.FALSE, lt.invoke(null, 1, 1));
        assertEquals(Boolean.FALSE, lt.invoke(null, 2, 1));

        Method le = branchingMethod(Comparison.LE);
        assertEquals(Boolean.TRUE, le.invoke(null, 1, 2));
        assertEquals(Boolean.TRUE, le.invoke(null, 1, 1));
        assertEquals(Boolean.FALSE, le.invoke(null, 2, 1));

        Method eq = branchingMethod(Comparison.EQ);
        assertEquals(Boolean.FALSE, eq.invoke(null, 1, 2));
        assertEquals(Boolean.TRUE, eq.invoke(null, 1, 1));
        assertEquals(Boolean.FALSE, eq.invoke(null, 2, 1));

        Method ge = branchingMethod(Comparison.GE);
        assertEquals(Boolean.FALSE, ge.invoke(null, 1, 2));
        assertEquals(Boolean.TRUE, ge.invoke(null, 1, 1));
        assertEquals(Boolean.TRUE, ge.invoke(null, 2, 1));

        Method gt = branchingMethod(Comparison.GT);
        assertEquals(Boolean.FALSE, gt.invoke(null, 1, 2));
        assertEquals(Boolean.FALSE, gt.invoke(null, 1, 1));
        assertEquals(Boolean.TRUE, gt.invoke(null, 2, 1));

        Method ne = branchingMethod(Comparison.NE);
        assertEquals(Boolean.TRUE, ne.invoke(null, 1, 2));
        assertEquals(Boolean.FALSE, ne.invoke(null, 1, 1));
        assertEquals(Boolean.TRUE, ne.invoke(null, 2, 1));
    }

    private Method branchingMethod(Comparison comparison) throws Exception {
        /*
         * public static boolean call(int localA, int localB) {
         *   if (a comparison b) {
         *     return true;
         *   }
         *   return false;
         * }
         */
        reset();
        MethodId<?, Boolean> methodId = GENERATED.getMethod(
                TypeId.BOOLEAN, "call", TypeId.INT, TypeId.INT);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localA = code.getParameter(0, TypeId.INT);
        Local<Integer> localB = code.getParameter(1, TypeId.INT);
        Local<Boolean> result = code.newLocal(TypeId.get(boolean.class));
        Label afterIf = new Label();
        Label ifBody = new Label();
        code.compare(comparison, ifBody, localA, localB);
        code.jump(afterIf);

        code.mark(ifBody);
        code.loadConstant(result, true);
        code.returnValue(result);

        code.mark(afterIf);
        code.loadConstant(result, false);
        code.returnValue(result);
        return getMethod();
    }

    @Test
    public void testBranchingZ() throws Exception {
        Method lt = branchingZMethod(Comparison.LT);
        assertEquals(Boolean.TRUE, lt.invoke(null, -1));
        assertEquals(Boolean.FALSE, lt.invoke(null, 0));
        assertEquals(Boolean.FALSE, lt.invoke(null, 1));

        Method le = branchingZMethod(Comparison.LE);
        assertEquals(Boolean.TRUE, le.invoke(null, -1));
        assertEquals(Boolean.TRUE, le.invoke(null, 0));
        assertEquals(Boolean.FALSE, le.invoke(null, 1));

        Method eq = branchingZMethod(Comparison.EQ);
        assertEquals(Boolean.FALSE, eq.invoke(null, -1));
        assertEquals(Boolean.TRUE, eq.invoke(null, 0));
        assertEquals(Boolean.FALSE, eq.invoke(null, 1));

        Method ge = branchingZMethod(Comparison.GE);
        assertEquals(Boolean.FALSE, ge.invoke(null, -1));
        assertEquals(Boolean.TRUE, ge.invoke(null, 0));
        assertEquals(Boolean.TRUE, ge.invoke(null, 1));

        Method gt = branchingZMethod(Comparison.GT);
        assertEquals(Boolean.FALSE, gt.invoke(null, -1));
        assertEquals(Boolean.FALSE, gt.invoke(null, 0));
        assertEquals(Boolean.TRUE, gt.invoke(null, 1));

        Method ne = branchingZMethod(Comparison.NE);
        assertEquals(Boolean.TRUE, ne.invoke(null, -1));
        assertEquals(Boolean.FALSE, ne.invoke(null, 0));
        assertEquals(Boolean.TRUE, ne.invoke(null, 1));
    }

    private Method branchingZMethod(Comparison comparison) throws Exception {
        /*
         * public static boolean call(int localA) {
         *   if (a comparison 0) {
         *     return true;
         *   }
         *   return false;
         * }
         */
        reset();
        MethodId<?, Boolean> methodId = GENERATED.getMethod(
            TypeId.BOOLEAN, "call", TypeId.INT);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localA = code.getParameter(0, TypeId.INT);
        Local<Boolean> result = code.newLocal(TypeId.get(boolean.class));
        Label afterIf = new Label();
        Label ifBody = new Label();
        code.compareZ(comparison, ifBody, localA);
        code.jump(afterIf);

        code.mark(ifBody);
        code.loadConstant(result, true);
        code.returnValue(result);

        code.mark(afterIf);
        code.loadConstant(result, false);
        code.returnValue(result);
        return getMethod();
    }

    @Test
    public void testCastIntegerToInteger() throws Exception {
        Method intToLong = numericCastingMethod(int.class, long.class);
        assertEquals(0x0000000000000000L, intToLong.invoke(null, 0x00000000));
        assertEquals(0x000000007fffffffL, intToLong.invoke(null, 0x7fffffff));
        assertEquals(0xffffffff80000000L, intToLong.invoke(null, 0x80000000));
        assertEquals(0xffffffffffffffffL, intToLong.invoke(null, 0xffffffff));

        Method longToInt = numericCastingMethod(long.class, int.class);
        assertEquals(0x1234abcd, longToInt.invoke(null, 0x000000001234abcdL));
        assertEquals(0x1234abcd, longToInt.invoke(null, 0x123456781234abcdL));
        assertEquals(0x1234abcd, longToInt.invoke(null, 0xffffffff1234abcdL));

        Method intToShort = numericCastingMethod(int.class, short.class);
        assertEquals((short) 0x1234, intToShort.invoke(null, 0x00001234));
        assertEquals((short) 0x1234, intToShort.invoke(null, 0xabcd1234));
        assertEquals((short) 0x1234, intToShort.invoke(null, 0xffff1234));

        Method intToChar = numericCastingMethod(int.class, char.class);
        assertEquals((char) 0x1234, intToChar.invoke(null, 0x00001234));
        assertEquals((char) 0x1234, intToChar.invoke(null, 0xabcd1234));
        assertEquals((char) 0x1234, intToChar.invoke(null, 0xffff1234));

        Method intToByte = numericCastingMethod(int.class, byte.class);
        assertEquals((byte) 0x34, intToByte.invoke(null, 0x00000034));
        assertEquals((byte) 0x34, intToByte.invoke(null, 0xabcd1234));
        assertEquals((byte) 0x34, intToByte.invoke(null, 0xffffff34));
    }

    @Test
    public void testCastIntegerToFloatingPoint() throws Exception {
        Method intToFloat = numericCastingMethod(int.class, float.class);
        assertEquals(0.0f, intToFloat.invoke(null, 0));
        assertEquals(-1.0f, intToFloat.invoke(null, -1));
        assertEquals(16777216f, intToFloat.invoke(null, 16777216));
        assertEquals(16777216f, intToFloat.invoke(null, 16777217)); // precision

        Method intToDouble = numericCastingMethod(int.class, double.class);
        assertEquals(0.0, intToDouble.invoke(null, 0));
        assertEquals(-1.0, intToDouble.invoke(null, -1));
        assertEquals(16777216.0, intToDouble.invoke(null, 16777216));
        assertEquals(16777217.0, intToDouble.invoke(null, 16777217));

        Method longToFloat = numericCastingMethod(long.class, float.class);
        assertEquals(0.0f, longToFloat.invoke(null, 0L));
        assertEquals(-1.0f, longToFloat.invoke(null, -1L));
        assertEquals(16777216f, longToFloat.invoke(null, 16777216L));
        assertEquals(16777216f, longToFloat.invoke(null, 16777217L));

        Method longToDouble = numericCastingMethod(long.class, double.class);
        assertEquals(0.0, longToDouble.invoke(null, 0L));
        assertEquals(-1.0, longToDouble.invoke(null, -1L));
        assertEquals(9007199254740992.0, longToDouble.invoke(null, 9007199254740992L));
        assertEquals(9007199254740992.0, longToDouble.invoke(null, 9007199254740993L)); // precision
    }

    @Test
    @SuppressWarnings("FloatingPointLiteralPrecision")
    public void testCastFloatingPointToInteger() throws Exception {
        Method floatToInt = numericCastingMethod(float.class, int.class);
        assertEquals(0, floatToInt.invoke(null, 0.0f));
        assertEquals(-1, floatToInt.invoke(null, -1.0f));
        assertEquals(Integer.MAX_VALUE, floatToInt.invoke(null, 10e15f));
        assertEquals(0, floatToInt.invoke(null, 0.5f));
        assertEquals(Integer.MIN_VALUE, floatToInt.invoke(null, Float.NEGATIVE_INFINITY));
        assertEquals(0, floatToInt.invoke(null, Float.NaN));

        Method floatToLong = numericCastingMethod(float.class, long.class);
        assertEquals(0L, floatToLong.invoke(null, 0.0f));
        assertEquals(-1L, floatToLong.invoke(null, -1.0f));
        assertEquals(10000000272564224L, floatToLong.invoke(null, 10e15f));
        assertEquals(0L, floatToLong.invoke(null, 0.5f));
        assertEquals(Long.MIN_VALUE, floatToLong.invoke(null, Float.NEGATIVE_INFINITY));
        assertEquals(0L, floatToLong.invoke(null, Float.NaN));

        Method doubleToInt = numericCastingMethod(double.class, int.class);
        assertEquals(0, doubleToInt.invoke(null, 0.0));
        assertEquals(-1, doubleToInt.invoke(null, -1.0));
        assertEquals(Integer.MAX_VALUE, doubleToInt.invoke(null, 10e15));
        assertEquals(0, doubleToInt.invoke(null, 0.5));
        assertEquals(Integer.MIN_VALUE, doubleToInt.invoke(null, Double.NEGATIVE_INFINITY));
        assertEquals(0, doubleToInt.invoke(null, Double.NaN));

        Method doubleToLong = numericCastingMethod(double.class, long.class);
        assertEquals(0L, doubleToLong.invoke(null, 0.0));
        assertEquals(-1L, doubleToLong.invoke(null, -1.0));
        assertEquals(10000000000000000L, doubleToLong.invoke(null, 10e15));
        assertEquals(0L, doubleToLong.invoke(null, 0.5));
        assertEquals(Long.MIN_VALUE, doubleToLong.invoke(null, Double.NEGATIVE_INFINITY));
        assertEquals(0L, doubleToLong.invoke(null, Double.NaN));
    }

    @Test
    public void testCastFloatingPointToFloatingPoint() throws Exception {
        Method floatToDouble = numericCastingMethod(float.class, double.class);
        assertEquals(0.0, floatToDouble.invoke(null, 0.0f));
        assertEquals(-1.0, floatToDouble.invoke(null, -1.0f));
        assertEquals(0.5, floatToDouble.invoke(null, 0.5f));
        assertEquals(Double.NEGATIVE_INFINITY, floatToDouble.invoke(null, Float.NEGATIVE_INFINITY));
        assertEquals(Double.NaN, floatToDouble.invoke(null, Float.NaN));

        Method doubleToFloat = numericCastingMethod(double.class, float.class);
        assertEquals(0.0f, doubleToFloat.invoke(null, 0.0));
        assertEquals(-1.0f, doubleToFloat.invoke(null, -1.0));
        assertEquals(0.5f, doubleToFloat.invoke(null, 0.5));
        assertEquals(Float.NEGATIVE_INFINITY, doubleToFloat.invoke(null, Double.NEGATIVE_INFINITY));
        assertEquals(Float.NaN, doubleToFloat.invoke(null, Double.NaN));
    }

    private Method numericCastingMethod(Class<?> source, Class<?> target)
            throws Exception {
        /*
         * public static short call(int source) {
         *   short casted = (short) source;
         *   return casted;
         * }
         */
        reset();
        TypeId<?> sourceType = TypeId.get(source);
        TypeId<?> targetType = TypeId.get(target);
        MethodId<?, ?> methodId = GENERATED.getMethod(targetType, "call", sourceType);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<?> localSource = code.getParameter(0, sourceType);
        Local<?> localCasted = code.newLocal(targetType);
        code.cast(localCasted, localSource);
        code.returnValue(localCasted);
        return getMethod();
    }

    @Test
    public void testNot() throws Exception {
        Method notInteger = notMethod(int.class);
        assertEquals(0xffffffff, notInteger.invoke(null, 0x00000000));
        assertEquals(0x00000000, notInteger.invoke(null, 0xffffffff));
        assertEquals(0xedcba987, notInteger.invoke(null, 0x12345678));

        Method notLong = notMethod(long.class);
        assertEquals(0xffffffffffffffffL, notLong.invoke(null, 0x0000000000000000L));
        assertEquals(0x0000000000000000L, notLong.invoke(null, 0xffffffffffffffffL));
        assertEquals(0x98765432edcba987L, notLong.invoke(null, 0x6789abcd12345678L));
    }

    private <T> Method notMethod(Class<T> source) throws Exception {
        /*
         * public static short call(int source) {
         *   source = ~source;
         *   return not;
         * }
         */
        reset();
        TypeId<T> valueType = TypeId.get(source);
        MethodId<?, T> methodId = GENERATED.getMethod(valueType, "call", valueType);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<T> localSource = code.getParameter(0, valueType);
        code.op(UnaryOp.NOT, localSource, localSource);
        code.returnValue(localSource);
        return getMethod();
    }

    @Test
    public void testNegate() throws Exception {
        Method negateInteger = negateMethod(int.class);
        assertEquals(0, negateInteger.invoke(null, 0));
        assertEquals(-1, negateInteger.invoke(null, 1));
        assertEquals(Integer.MIN_VALUE, negateInteger.invoke(null, Integer.MIN_VALUE));

        Method negateLong = negateMethod(long.class);
        assertEquals(0L, negateLong.invoke(null, 0));
        assertEquals(-1L, negateLong.invoke(null, 1));
        assertEquals(Long.MIN_VALUE, negateLong.invoke(null, Long.MIN_VALUE));

        Method negateFloat = negateMethod(float.class);
        assertEquals(-0.0f, negateFloat.invoke(null, 0.0f));
        assertEquals(-1.0f, negateFloat.invoke(null, 1.0f));
        assertEquals(Float.NaN, negateFloat.invoke(null, Float.NaN));
        assertEquals(Float.POSITIVE_INFINITY, negateFloat.invoke(null, Float.NEGATIVE_INFINITY));

        Method negateDouble = negateMethod(double.class);
        assertEquals(-0.0, negateDouble.invoke(null, 0.0));
        assertEquals(-1.0, negateDouble.invoke(null, 1.0));
        assertEquals(Double.NaN, negateDouble.invoke(null, Double.NaN));
        assertEquals(Double.POSITIVE_INFINITY, negateDouble.invoke(null, Double.NEGATIVE_INFINITY));
    }

    private <T> Method negateMethod(Class<T> source) throws Exception {
        /*
         * public static short call(int source) {
         *   source = -source;
         *   return not;
         * }
         */
        reset();
        TypeId<T> valueType = TypeId.get(source);
        MethodId<?, T> methodId = GENERATED.getMethod(valueType, "call", valueType);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<T> localSource = code.getParameter(0, valueType);
        code.op(UnaryOp.NEGATE, localSource, localSource);
        code.returnValue(localSource);
        return getMethod();
    }

    @Test
    public void testIntBinaryOps() throws Exception {
        Method add = binaryOpMethod(int.class, int.class, BinaryOp.ADD);
        assertEquals(79, add.invoke(null, 75, 4));

        Method subtract = binaryOpMethod(int.class, int.class, BinaryOp.SUBTRACT);
        assertEquals(71, subtract.invoke(null, 75, 4));

        Method multiply = binaryOpMethod(int.class, int.class, BinaryOp.MULTIPLY);
        assertEquals(300, multiply.invoke(null, 75, 4));

        Method divide = binaryOpMethod(int.class, int.class, BinaryOp.DIVIDE);
        assertEquals(18, divide.invoke(null, 75, 4));
        try {
            divide.invoke(null, 75, 0);
            fail();
        } catch (InvocationTargetException expected) {
            assertEquals(ArithmeticException.class, expected.getCause().getClass());
        }

        Method remainder = binaryOpMethod(int.class, int.class, BinaryOp.REMAINDER);
        assertEquals(3, remainder.invoke(null, 75, 4));
        try {
            remainder.invoke(null, 75, 0);
            fail();
        } catch (InvocationTargetException expected) {
            assertEquals(ArithmeticException.class, expected.getCause().getClass());
        }

        Method and = binaryOpMethod(int.class, int.class, BinaryOp.AND);
        assertEquals(0xff000000, and.invoke(null, 0xff00ff00, 0xffff0000));

        Method or = binaryOpMethod(int.class, int.class, BinaryOp.OR);
        assertEquals(0xffffff00, or.invoke(null, 0xff00ff00, 0xffff0000));

        Method xor = binaryOpMethod(int.class, int.class, BinaryOp.XOR);
        assertEquals(0x00ffff00, xor.invoke(null, 0xff00ff00, 0xffff0000));

        Method shiftLeft = binaryOpMethod(int.class, int.class, BinaryOp.SHIFT_LEFT);
        assertEquals(0xcd123400, shiftLeft.invoke(null, 0xabcd1234, 8));

        Method shiftRight = binaryOpMethod(int.class, int.class, BinaryOp.SHIFT_RIGHT);
        assertEquals(0xffabcd12, shiftRight.invoke(null, 0xabcd1234, 8));

        Method unsignedShiftRight = binaryOpMethod(int.class,
                int.class, BinaryOp.UNSIGNED_SHIFT_RIGHT);
        assertEquals(0x00abcd12, unsignedShiftRight.invoke(null, 0xabcd1234, 8));
    }

    @Test
    public void testLongBinaryOps() throws Exception {
        Method add = binaryOpMethod(long.class, long.class, BinaryOp.ADD);
        assertEquals(30000000079L, add.invoke(null, 10000000075L, 20000000004L));

        Method subtract = binaryOpMethod(long.class, long.class, BinaryOp.SUBTRACT);
        assertEquals(20000000071L, subtract.invoke(null, 30000000075L, 10000000004L));

        Method multiply = binaryOpMethod(long.class, long.class, BinaryOp.MULTIPLY);
        assertEquals(-8742552812415203028L, multiply.invoke(null, 30000000075L, 20000000004L));

        Method divide = binaryOpMethod(long.class, long.class, BinaryOp.DIVIDE);
        assertEquals(-2L, divide.invoke(null, -8742552812415203028L, 4142552812415203028L));
        try {
            divide.invoke(null, -8742552812415203028L, 0L);
            fail();
        } catch (InvocationTargetException expected) {
            assertEquals(ArithmeticException.class, expected.getCause().getClass());
        }

        Method remainder = binaryOpMethod(long.class, long.class, BinaryOp.REMAINDER);
        assertEquals(10000000004L, remainder.invoke(null, 30000000079L, 20000000075L));
        try {
            remainder.invoke(null, 30000000079L, 0L);
            fail();
        } catch (InvocationTargetException expected) {
            assertEquals(ArithmeticException.class, expected.getCause().getClass());
        }

        Method and = binaryOpMethod(long.class, long.class, BinaryOp.AND);
        assertEquals(0xff00ff0000000000L,
                and.invoke(null, 0xff00ff00ff00ff00L, 0xffffffff00000000L));

        Method or = binaryOpMethod(long.class, long.class, BinaryOp.OR);
        assertEquals(0xffffffffff00ff00L,
                or.invoke(null, 0xff00ff00ff00ff00L, 0xffffffff00000000L));

        Method xor = binaryOpMethod(long.class, long.class, BinaryOp.XOR);
        assertEquals(0x00ff00ffff00ff00L,
                xor.invoke(null, 0xff00ff00ff00ff00L, 0xffffffff00000000L));

        Method shiftLeft = binaryOpMethod(long.class, int.class, BinaryOp.SHIFT_LEFT);
        assertEquals(0xcdef012345678900L, shiftLeft.invoke(null, 0xabcdef0123456789L, 8));

        Method shiftRight = binaryOpMethod(long.class, int.class, BinaryOp.SHIFT_RIGHT);
        assertEquals(0xffabcdef01234567L, shiftRight.invoke(null, 0xabcdef0123456789L, 8));

        Method unsignedShiftRight = binaryOpMethod(
                long.class, int.class, BinaryOp.UNSIGNED_SHIFT_RIGHT);
        assertEquals(0x00abcdef01234567L, unsignedShiftRight.invoke(null, 0xabcdef0123456789L, 8));
    }

    @Test
    public void testFloatBinaryOps() throws Exception {
        Method add = binaryOpMethod(float.class, float.class, BinaryOp.ADD);
        assertEquals(6.75f, add.invoke(null, 5.5f, 1.25f));

        Method subtract = binaryOpMethod(float.class, float.class, BinaryOp.SUBTRACT);
        assertEquals(4.25f, subtract.invoke(null, 5.5f, 1.25f));

        Method multiply = binaryOpMethod(float.class, float.class, BinaryOp.MULTIPLY);
        assertEquals(6.875f, multiply.invoke(null, 5.5f, 1.25f));

        Method divide = binaryOpMethod(float.class, float.class, BinaryOp.DIVIDE);
        assertEquals(4.4f, divide.invoke(null, 5.5f, 1.25f));
        assertEquals(Float.POSITIVE_INFINITY, divide.invoke(null, 5.5f, 0.0f));

        Method remainder = binaryOpMethod(float.class, float.class, BinaryOp.REMAINDER);
        assertEquals(0.5f, remainder.invoke(null, 5.5f, 1.25f));
        assertEquals(Float.NaN, remainder.invoke(null, 5.5f, 0.0f));
    }

    @Test
    public void testDoubleBinaryOps() throws Exception {
        Method add = binaryOpMethod(double.class, double.class, BinaryOp.ADD);
        assertEquals(6.75, add.invoke(null, 5.5, 1.25));

        Method subtract = binaryOpMethod(double.class, double.class, BinaryOp.SUBTRACT);
        assertEquals(4.25, subtract.invoke(null, 5.5, 1.25));

        Method multiply = binaryOpMethod(double.class, double.class, BinaryOp.MULTIPLY);
        assertEquals(6.875, multiply.invoke(null, 5.5, 1.25));

        Method divide = binaryOpMethod(double.class, double.class, BinaryOp.DIVIDE);
        assertEquals(4.4, divide.invoke(null, 5.5, 1.25));
        assertEquals(Double.POSITIVE_INFINITY, divide.invoke(null, 5.5, 0.0));

        Method remainder = binaryOpMethod(double.class, double.class, BinaryOp.REMAINDER);
        assertEquals(0.5, remainder.invoke(null, 5.5, 1.25));
        assertEquals(Double.NaN, remainder.invoke(null, 5.5, 0.0));
    }

    private <T1, T2> Method binaryOpMethod(
            Class<T1> valueAClass, Class<T2> valueBClass, BinaryOp op) throws Exception {
        /*
         * public static int binaryOp(int a, int b) {
         *   int result = a + b;
         *   return result;
         * }
         */
        reset();
        TypeId<T1> valueAType = TypeId.get(valueAClass);
        TypeId<T2> valueBType = TypeId.get(valueBClass);
        MethodId<?, T1> methodId = GENERATED.getMethod(valueAType, "call", valueAType, valueBType);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<T1> localA = code.getParameter(0, valueAType);
        Local<T2> localB = code.getParameter(1, valueBType);
        Local<T1> localResult = code.newLocal(valueAType);
        code.op(op, localResult, localA, localB);
        code.returnValue(localResult);
        return getMethod();
    }

    @Test
    public void testReadAndWriteInstanceFields() throws Exception {
        Instance instance = new Instance();

        Method intSwap = instanceSwapMethod(int.class, "intValue");
        instance.intValue = 5;
        assertEquals(5, intSwap.invoke(null, instance, 10));
        assertEquals(10, instance.intValue);

        Method longSwap = instanceSwapMethod(long.class, "longValue");
        instance.longValue = 500L;
        assertEquals(500L, longSwap.invoke(null, instance, 1234L));
        assertEquals(1234L, instance.longValue);

        Method booleanSwap = instanceSwapMethod(boolean.class, "booleanValue");
        instance.booleanValue = false;
        assertEquals(false, booleanSwap.invoke(null, instance, true));
        assertEquals(true, instance.booleanValue);

        Method floatSwap = instanceSwapMethod(float.class, "floatValue");
        instance.floatValue = 1.5f;
        assertEquals(1.5f, floatSwap.invoke(null, instance, 0.5f));
        assertEquals(0.5f, instance.floatValue, DELTA_FLOAT);

        Method doubleSwap = instanceSwapMethod(double.class, "doubleValue");
        instance.doubleValue = 155.5;
        assertEquals(155.5, doubleSwap.invoke(null, instance, 266.6));
        assertEquals(266.6, instance.doubleValue, DELTA_DOUBLE);

        Method objectSwap = instanceSwapMethod(Object.class, "objectValue");
        instance.objectValue = "before";
        assertEquals("before", objectSwap.invoke(null, instance, "after"));
        assertEquals("after", instance.objectValue);

        Method byteSwap = instanceSwapMethod(byte.class, "byteValue");
        instance.byteValue = 0x35;
        assertEquals((byte) 0x35, byteSwap.invoke(null, instance, (byte) 0x64));
        assertEquals((byte) 0x64, instance.byteValue);

        Method charSwap = instanceSwapMethod(char.class, "charValue");
        instance.charValue = 'A';
        assertEquals('A', charSwap.invoke(null, instance, 'B'));
        assertEquals('B', instance.charValue);

        Method shortSwap = instanceSwapMethod(short.class, "shortValue");
        instance.shortValue = (short) 0xabcd;
        assertEquals((short) 0xabcd, shortSwap.invoke(null, instance, (short) 0x1234));
        assertEquals((short) 0x1234, instance.shortValue);
    }

    public static class Instance {
        public int intValue;
        public long longValue;
        public float floatValue;
        public double doubleValue;
        public Object objectValue;
        public boolean booleanValue;
        public byte byteValue;
        public char charValue;
        public short shortValue;
    }

    private <V> Method instanceSwapMethod(
            Class<V> valueClass, String fieldName) throws Exception {
        /*
         * public static int call(Instance instance, int newValue) {
         *   int oldValue = instance.intValue;
         *   instance.intValue = newValue;
         *   return oldValue;
         * }
         */
        reset();
        TypeId<V> valueType = TypeId.get(valueClass);
        TypeId<Instance> objectType = TypeId.get(Instance.class);
        FieldId<Instance, V> fieldId = objectType.getField(valueType, fieldName);
        MethodId<?, V> methodId = GENERATED.getMethod(valueType, "call", objectType, valueType);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Instance> localInstance = code.getParameter(0, objectType);
        Local<V> localNewValue = code.getParameter(1, valueType);
        Local<V> localOldValue = code.newLocal(valueType);
        code.iget(fieldId, localOldValue, localInstance);
        code.iput(fieldId, localInstance, localNewValue);
        code.returnValue(localOldValue);
        return getMethod();
    }

    @Test
    public void testReadAndWriteStaticFields() throws Exception {
        Method intSwap = staticSwapMethod(int.class, "intValue");
        Static.intValue = 5;
        assertEquals(5, intSwap.invoke(null, 10));
        assertEquals(10, Static.intValue);

        Method longSwap = staticSwapMethod(long.class, "longValue");
        Static.longValue = 500L;
        assertEquals(500L, longSwap.invoke(null, 1234L));
        assertEquals(1234L, Static.longValue);

        Method booleanSwap = staticSwapMethod(boolean.class, "booleanValue");
        Static.booleanValue = false;
        assertEquals(false, booleanSwap.invoke(null, true));
        assertEquals(true, Static.booleanValue);

        Method floatSwap = staticSwapMethod(float.class, "floatValue");
        Static.floatValue = 1.5f;
        assertEquals(1.5f, floatSwap.invoke(null, 0.5f));
        assertEquals(0.5f, Static.floatValue, DELTA_FLOAT);

        Method doubleSwap = staticSwapMethod(double.class, "doubleValue");
        Static.doubleValue = 155.5;
        assertEquals(155.5, doubleSwap.invoke(null, 266.6));
        assertEquals(266.6, Static.doubleValue, DELTA_DOUBLE);

        Method objectSwap = staticSwapMethod(Object.class, "objectValue");
        Static.objectValue = "before";
        assertEquals("before", objectSwap.invoke(null, "after"));
        assertEquals("after", Static.objectValue);

        Method byteSwap = staticSwapMethod(byte.class, "byteValue");
        Static.byteValue = 0x35;
        assertEquals((byte) 0x35, byteSwap.invoke(null, (byte) 0x64));
        assertEquals((byte) 0x64, Static.byteValue);

        Method charSwap = staticSwapMethod(char.class, "charValue");
        Static.charValue = 'A';
        assertEquals('A', charSwap.invoke(null, 'B'));
        assertEquals('B', Static.charValue);

        Method shortSwap = staticSwapMethod(short.class, "shortValue");
        Static.shortValue = (short) 0xabcd;
        assertEquals((short) 0xabcd, shortSwap.invoke(null, (short) 0x1234));
        assertEquals((short) 0x1234, Static.shortValue);
    }

    public static class Static {
        public static int intValue;
        public static long longValue;
        public static float floatValue;
        public static double doubleValue;
        public static Object objectValue;
        public static boolean booleanValue;
        public static byte byteValue;
        public static char charValue;
        public static short shortValue;
    }

    private <V> Method staticSwapMethod(Class<V> valueClass, String fieldName)
            throws Exception {
        /*
         * public static int call(int newValue) {
         *   int oldValue = Static.intValue;
         *   Static.intValue = newValue;
         *   return oldValue;
         * }
         */
        reset();
        TypeId<V> valueType = TypeId.get(valueClass);
        TypeId<Static> objectType = TypeId.get(Static.class);
        FieldId<Static, V> fieldId = objectType.getField(valueType, fieldName);
        MethodId<?, V> methodId = GENERATED.getMethod(valueType, "call", valueType);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<V> localNewValue = code.getParameter(0, valueType);
        Local<V> localOldValue = code.newLocal(valueType);
        code.sget(fieldId, localOldValue);
        code.sput(fieldId, localNewValue);
        code.returnValue(localOldValue);
        return getMethod();
    }

    @Test
    public void testStaticInitializer() throws Exception {
        reset();

        StaticFieldSpec<?>[] fields = new StaticFieldSpec[] {
                new StaticFieldSpec<>(boolean.class, "booleanValue", true),
                new StaticFieldSpec<>(byte.class, "byteValue", Byte.MIN_VALUE),
                new StaticFieldSpec<>(short.class, "shortValue", Short.MAX_VALUE),
                new StaticFieldSpec<>(int.class, "intValue", Integer.MIN_VALUE),
                new StaticFieldSpec<>(long.class, "longValue", Long.MAX_VALUE),
                new StaticFieldSpec<>(float.class, "floatValue", Float.MIN_VALUE),
                new StaticFieldSpec<>(double.class, "doubleValue", Double.MAX_VALUE),
                new StaticFieldSpec<>(String.class, "stringValue", "qwerty"),
        };

        MethodId<?, Void> clinit = GENERATED.getStaticInitializer();
        assertTrue(clinit.isStaticInitializer());

        Code code = dexMaker.declare(clinit, Modifier.STATIC);

        for (StaticFieldSpec<?> field : fields) {
            field.createLocal(code);
        }

        for (StaticFieldSpec<?> field : fields) {
            field.initializeField(code);
        }

        code.returnVoid();

        Class<?> generated = generateAndLoad();
        for (StaticFieldSpec<?> fieldSpec : fields) {
            Field field = generated.getDeclaredField(fieldSpec.name);
            assertEquals(StaticFieldSpec.MODIFIERS, field.getModifiers());
            assertEquals(fieldSpec.value, field.get(null));
        }
    }

    private class StaticFieldSpec<T> {
        Class<T> type;
        TypeId<T> typeId;
        String name;
        T value;
        FieldId<?, T> fieldId;
        Local<T> local;

        static final int MODIFIERS = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;

        public StaticFieldSpec(Class<T> type, String name, T value) {
            this.type = type;
            this.name = name;
            this.value = value;

            typeId = TypeId.get(type);
            fieldId = GENERATED.getField(typeId, name);
            dexMaker.declare(fieldId, MODIFIERS, null);
        }

        public void createLocal(Code code) {
            local = code.newLocal(typeId);
        }

        public void initializeField(Code code) {
            code.loadConstant(local, value);
            code.sput(fieldId, local);
        }
    }

    @Test
    public void testTypeCast() throws Exception {
        /*
         * public static String call(Object o) {
         *   String s = (String) o;
         * }
         */
        MethodId<?, String> methodId = GENERATED.getMethod(TypeId.STRING, "call", TypeId.OBJECT);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Object> localObject = code.getParameter(0, TypeId.OBJECT);
        Local<String> localString = code.newLocal(TypeId.STRING);
        code.cast(localString, localObject);
        code.returnValue(localString);

        Method method = getMethod();
        assertEquals("s", method.invoke(null, "s"));
        assertEquals(null, method.invoke(null, (String) null));
        try {
            method.invoke(null, 5);
            fail();
        } catch (InvocationTargetException expected) {
            assertEquals(ClassCastException.class, expected.getCause().getClass());
        }
    }

    @Test
    public void testInstanceOf() throws Exception {
        /*
         * public static boolean call(Object o) {
         *   boolean result = o instanceof String;
         *   return result;
         * }
         */
        MethodId<?, Boolean> methodId = GENERATED.getMethod(TypeId.BOOLEAN, "call", TypeId.OBJECT);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Object> localObject = code.getParameter(0, TypeId.OBJECT);
        Local<Boolean> localResult = code.newLocal(TypeId.BOOLEAN);
        code.instanceOfType(localResult, localObject, TypeId.STRING);
        code.returnValue(localResult);

        Method method = getMethod();
        assertEquals(true, method.invoke(null, "s"));
        assertEquals(false, method.invoke(null, (String) null));
        assertEquals(false, method.invoke(null, 5));
    }

    /**
     * Tests that we can construct a for loop.
     */
    @Test
    public void testForLoop() throws Exception {
        /*
         * public static int call(int count) {
         *   int result = 1;
         *   for (int i = 0; i < count; i += 1) {
         *     result = result * 2;
         *   }
         *   return result;
         * }
         */
        MethodId<?, Integer> methodId = GENERATED.getMethod(TypeId.INT, "call", TypeId.INT);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localCount = code.getParameter(0, TypeId.INT);
        Local<Integer> localResult = code.newLocal(TypeId.INT);
        Local<Integer> localI = code.newLocal(TypeId.INT);
        Local<Integer> local1 = code.newLocal(TypeId.INT);
        Local<Integer> local2 = code.newLocal(TypeId.INT);
        code.loadConstant(local1, 1);
        code.loadConstant(local2, 2);
        code.loadConstant(localResult, 1);
        code.loadConstant(localI, 0);
        Label loopCondition = new Label();
        Label loopBody = new Label();
        Label afterLoop = new Label();
        code.mark(loopCondition);
        code.compare(Comparison.LT, loopBody, localI, localCount);
        code.jump(afterLoop);
        code.mark(loopBody);
        code.op(BinaryOp.MULTIPLY, localResult, localResult, local2);
        code.op(BinaryOp.ADD, localI, localI, local1);
        code.jump(loopCondition);
        code.mark(afterLoop);
        code.returnValue(localResult);

        Method pow2 = getMethod();
        assertEquals(1, pow2.invoke(null, 0));
        assertEquals(2, pow2.invoke(null, 1));
        assertEquals(4, pow2.invoke(null, 2));
        assertEquals(8, pow2.invoke(null, 3));
        assertEquals(16, pow2.invoke(null, 4));
    }

    /**
     * Tests that we can construct a while loop.
     */
    @Test
    public void testWhileLoop() throws Exception {
        /*
         * public static int call(int max) {
         *   int result = 1;
         *   while (result < max) {
         *     result = result * 2;
         *   }
         *   return result;
         * }
         */
        MethodId<?, Integer> methodId = GENERATED.getMethod(TypeId.INT, "call", TypeId.INT);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localMax = code.getParameter(0, TypeId.INT);
        Local<Integer> localResult = code.newLocal(TypeId.INT);
        Local<Integer> local2 = code.newLocal(TypeId.INT);
        code.loadConstant(localResult, 1);
        code.loadConstant(local2, 2);
        Label loopCondition = new Label();
        Label loopBody = new Label();
        Label afterLoop = new Label();
        code.mark(loopCondition);
        code.compare(Comparison.LT, loopBody, localResult, localMax);
        code.jump(afterLoop);
        code.mark(loopBody);
        code.op(BinaryOp.MULTIPLY, localResult, localResult, local2);
        code.jump(loopCondition);
        code.mark(afterLoop);
        code.returnValue(localResult);

        Method ceilPow2 = getMethod();
        assertEquals(1, ceilPow2.invoke(null, 1));
        assertEquals(2, ceilPow2.invoke(null, 2));
        assertEquals(4, ceilPow2.invoke(null, 3));
        assertEquals(16, ceilPow2.invoke(null, 10));
        assertEquals(128, ceilPow2.invoke(null, 100));
        assertEquals(1024, ceilPow2.invoke(null, 1000));
    }

    @Test
    public void testIfElseBlock() throws Exception {
        /*
         * public static int call(int a, int b, int c) {
         *   if (a < b) {
         *     if (a < c) {
         *       return a;
         *     } else {
         *       return c;
         *     }
         *   } else if (b < c) {
         *     return b;
         *   } else {
         *     return c;
         *   }
         * }
         */
        MethodId<?, Integer> methodId = GENERATED.getMethod(
                TypeId.INT, "call", TypeId.INT, TypeId.INT, TypeId.INT);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localA = code.getParameter(0, TypeId.INT);
        Local<Integer> localB = code.getParameter(1, TypeId.INT);
        Local<Integer> localC = code.getParameter(2, TypeId.INT);
        Label aLessThanB = new Label();
        Label aLessThanC = new Label();
        Label bLessThanC = new Label();
        code.compare(Comparison.LT, aLessThanB, localA, localB);
        code.compare(Comparison.LT, bLessThanC, localB, localC);
        code.returnValue(localC);
        // (a < b)
        code.mark(aLessThanB);
        code.compare(Comparison.LT, aLessThanC, localA, localC);
        code.returnValue(localC);
        // (a < c)
        code.mark(aLessThanC);
        code.returnValue(localA);
        // (b < c)
        code.mark(bLessThanC);
        code.returnValue(localB);

        Method min = getMethod();
        assertEquals(1, min.invoke(null, 1, 2, 3));
        assertEquals(1, min.invoke(null, 2, 3, 1));
        assertEquals(1, min.invoke(null, 2, 1, 3));
        assertEquals(1, min.invoke(null, 3, 2, 1));
    }

    @Test
    public void testRecursion() throws Exception {
        /*
         * public static int call(int a) {
         *   if (a < 2) {
         *     return a;
         *   }
         *   a -= 1;
         *   int x = call(a)
         *   a -= 1;
         *   int y = call(a);
         *   int result = x + y;
         *   return result;
         * }
         */
        MethodId<?, Integer> methodId = GENERATED.getMethod(TypeId.INT, "call", TypeId.INT);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localA = code.getParameter(0, TypeId.INT);
        Local<Integer> local1 = code.newLocal(TypeId.INT);
        Local<Integer> local2 = code.newLocal(TypeId.INT);
        Local<Integer> localX = code.newLocal(TypeId.INT);
        Local<Integer> localY = code.newLocal(TypeId.INT);
        Local<Integer> localResult = code.newLocal(TypeId.INT);
        Label baseCase = new Label();
        code.loadConstant(local1, 1);
        code.loadConstant(local2, 2);
        code.compare(Comparison.LT, baseCase, localA, local2);
        code.op(BinaryOp.SUBTRACT, localA, localA, local1);
        code.invokeStatic(methodId, localX, localA);
        code.op(BinaryOp.SUBTRACT, localA, localA, local1);
        code.invokeStatic(methodId, localY, localA);
        code.op(BinaryOp.ADD, localResult, localX, localY);
        code.returnValue(localResult);
        code.mark(baseCase);
        code.returnValue(localA);

        Method fib = getMethod();
        assertEquals(0, fib.invoke(null, 0));
        assertEquals(1, fib.invoke(null, 1));
        assertEquals(1, fib.invoke(null, 2));
        assertEquals(2, fib.invoke(null, 3));
        assertEquals(3, fib.invoke(null, 4));
        assertEquals(5, fib.invoke(null, 5));
        assertEquals(8, fib.invoke(null, 6));
    }

    @Test
    public void testCatchExceptions() throws Exception {
        /*
         * public static String call(int i) {
         *   try {
         *     DexMakerTest.thrower(i);
         *     return "NONE";
         *   } catch (IllegalArgumentException e) {
         *     return "IAE";
         *   } catch (IllegalStateException e) {
         *     return "ISE";
         *   } catch (RuntimeException e) {
         *     return "RE";
         *   }
         */
        MethodId<?, String> methodId = GENERATED.getMethod(TypeId.STRING, "call", TypeId.INT);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localI = code.getParameter(0, TypeId.INT);
        Local<String> result = code.newLocal(TypeId.STRING);
        Label catchIae = new Label();
        Label catchIse = new Label();
        Label catchRe = new Label();

        code.addCatchClause(TypeId.get(IllegalArgumentException.class), catchIae);
        code.addCatchClause(TypeId.get(IllegalStateException.class), catchIse);
        code.addCatchClause(TypeId.get(RuntimeException.class), catchRe);
        MethodId<?, ?> thrower = TEST_TYPE.getMethod(TypeId.VOID, "thrower", TypeId.INT);
        code.invokeStatic(thrower, null, localI);
        code.loadConstant(result, "NONE");
        code.returnValue(result);

        code.mark(catchIae);
        code.loadConstant(result, "IAE");
        code.returnValue(result);

        code.mark(catchIse);
        code.loadConstant(result, "ISE");
        code.returnValue(result);

        code.mark(catchRe);
        code.loadConstant(result, "RE");
        code.returnValue(result);

        Method method = getMethod();
        assertEquals("NONE", method.invoke(null, 0));
        assertEquals("IAE", method.invoke(null, 1));
        assertEquals("ISE", method.invoke(null, 2));
        assertEquals("RE", method.invoke(null, 3));
        try {
            method.invoke(null, 4);
            fail();
        } catch (InvocationTargetException expected) {
            assertEquals(IOException.class, expected.getCause().getClass());
        }
    }

    @SuppressWarnings("unused") // called by generated code
    public static void thrower(int a) throws Exception {
        switch (a) {
        case 0:
            return;
        case 1:
            throw new IllegalArgumentException();
        case 2:
            throw new IllegalStateException();
        case 3:
            throw new UnsupportedOperationException();
        case 4:
            throw new IOException();
        default:
            throw new AssertionError();
        }
    }

    @Test
    public void testNestedCatchClauses() throws Exception {
        /*
         * public static String call(int a, int b, int c) {
         *   try {
         *     DexMakerTest.thrower(a);
         *     try {
         *       DexMakerTest.thrower(b);
         *     } catch (IllegalArgumentException) {
         *       return "INNER";
         *     }
         *     DexMakerTest.thrower(c);
         *     return "NONE";
         *   } catch (IllegalArgumentException e) {
         *     return "OUTER";
         *   }
         */
        MethodId<?, String> methodId = GENERATED.getMethod(
                TypeId.STRING, "call", TypeId.INT, TypeId.INT, TypeId.INT);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localA = code.getParameter(0, TypeId.INT);
        Local<Integer> localB = code.getParameter(1, TypeId.INT);
        Local<Integer> localC = code.getParameter(2, TypeId.INT);
        Local<String> localResult = code.newLocal(TypeId.STRING);
        Label catchInner = new Label();
        Label catchOuter = new Label();

        TypeId<IllegalArgumentException> iaeType = TypeId.get(IllegalArgumentException.class);
        code.addCatchClause(iaeType, catchOuter);

        MethodId<?, ?> thrower = TEST_TYPE.getMethod(TypeId.VOID, "thrower", TypeId.INT);
        code.invokeStatic(thrower, null, localA);

        // for the inner catch clause, we stash the old label and put it back afterwards.
        Label previousLabel = code.removeCatchClause(iaeType);
        code.addCatchClause(iaeType, catchInner);
        code.invokeStatic(thrower, null, localB);
        code.removeCatchClause(iaeType);
        code.addCatchClause(iaeType, previousLabel);
        code.invokeStatic(thrower, null, localC);
        code.loadConstant(localResult, "NONE");
        code.returnValue(localResult);

        code.mark(catchInner);
        code.loadConstant(localResult, "INNER");
        code.returnValue(localResult);

        code.mark(catchOuter);
        code.loadConstant(localResult, "OUTER");
        code.returnValue(localResult);

        Method method = getMethod();
        assertEquals("OUTER", method.invoke(null, 1, 0, 0));
        assertEquals("INNER", method.invoke(null, 0, 1, 0));
        assertEquals("OUTER", method.invoke(null, 0, 0, 1));
        assertEquals("NONE", method.invoke(null, 0, 0, 0));
    }

    @Test
    public void testThrow() throws Exception {
        /*
         * public static void call() {
         *   throw new IllegalStateException();
         * }
         */
        MethodId<?, Void> methodId = GENERATED.getMethod(TypeId.VOID, "call");
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        TypeId<IllegalStateException> iseType = TypeId.get(IllegalStateException.class);
        MethodId<IllegalStateException, Void> iseConstructor = iseType.getConstructor();
        Local<IllegalStateException> localIse = code.newLocal(iseType);
        code.newInstance(localIse, iseConstructor);
        code.throwValue(localIse);

        try {
            getMethod().invoke(null);
            fail();
        } catch (InvocationTargetException expected) {
            assertEquals(IllegalStateException.class, expected.getCause().getClass());
        }
    }

    @Test
    public void testUnusedParameters() throws Exception {
        /*
         * public static void call(int unused1, long unused2, long unused3) {}
         */
        MethodId<?, Void> methodId = GENERATED.getMethod(
                TypeId.VOID, "call", TypeId.INT, TypeId.LONG, TypeId.LONG);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        code.returnVoid();
        getMethod().invoke(null, 1, 2, 3);
    }

    @Test
    public void testFloatingPointCompare() throws Exception {
        Method floatG = floatingPointCompareMethod(TypeId.FLOAT, 1);
        assertEquals(-1, floatG.invoke(null, 1.0f, Float.POSITIVE_INFINITY));
        assertEquals(-1, floatG.invoke(null, 1.0f, 2.0f));
        assertEquals(0, floatG.invoke(null, 1.0f, 1.0f));
        assertEquals(1, floatG.invoke(null, 2.0f, 1.0f));
        assertEquals(1, floatG.invoke(null, 1.0f, Float.NaN));
        assertEquals(1, floatG.invoke(null, Float.NaN, 1.0f));
        assertEquals(1, floatG.invoke(null, Float.NaN, Float.NaN));
        assertEquals(1, floatG.invoke(null, Float.NaN, Float.POSITIVE_INFINITY));

        Method floatL = floatingPointCompareMethod(TypeId.FLOAT, -1);
        assertEquals(-1, floatG.invoke(null, 1.0f, Float.POSITIVE_INFINITY));
        assertEquals(-1, floatL.invoke(null, 1.0f, 2.0f));
        assertEquals(0, floatL.invoke(null, 1.0f, 1.0f));
        assertEquals(1, floatL.invoke(null, 2.0f, 1.0f));
        assertEquals(-1, floatL.invoke(null, 1.0f, Float.NaN));
        assertEquals(-1, floatL.invoke(null, Float.NaN, 1.0f));
        assertEquals(-1, floatL.invoke(null, Float.NaN, Float.NaN));
        assertEquals(-1, floatL.invoke(null, Float.NaN, Float.POSITIVE_INFINITY));

        Method doubleG = floatingPointCompareMethod(TypeId.DOUBLE, 1);
        assertEquals(-1, doubleG.invoke(null, 1.0, Double.POSITIVE_INFINITY));
        assertEquals(-1, doubleG.invoke(null, 1.0, 2.0));
        assertEquals(0, doubleG.invoke(null, 1.0, 1.0));
        assertEquals(1, doubleG.invoke(null, 2.0, 1.0));
        assertEquals(1, doubleG.invoke(null, 1.0, Double.NaN));
        assertEquals(1, doubleG.invoke(null, Double.NaN, 1.0));
        assertEquals(1, doubleG.invoke(null, Double.NaN, Double.NaN));
        assertEquals(1, doubleG.invoke(null, Double.NaN, Double.POSITIVE_INFINITY));

        Method doubleL = floatingPointCompareMethod(TypeId.DOUBLE, -1);
        assertEquals(-1, doubleL.invoke(null, 1.0, Double.POSITIVE_INFINITY));
        assertEquals(-1, doubleL.invoke(null, 1.0, 2.0));
        assertEquals(0, doubleL.invoke(null, 1.0, 1.0));
        assertEquals(1, doubleL.invoke(null, 2.0, 1.0));
        assertEquals(-1, doubleL.invoke(null, 1.0, Double.NaN));
        assertEquals(-1, doubleL.invoke(null, Double.NaN, 1.0));
        assertEquals(-1, doubleL.invoke(null, Double.NaN, Double.NaN));
        assertEquals(-1, doubleL.invoke(null, Double.NaN, Double.POSITIVE_INFINITY));
    }

    private <T extends Number> Method floatingPointCompareMethod(
            TypeId<T> valueType, int nanValue) throws Exception {
        /*
         * public static int call(float a, float b) {
         *     int result = a <=> b;
         *     return result;
         * }
         */
        reset();
        MethodId<?, Integer> methodId = GENERATED.getMethod(
                TypeId.INT, "call", valueType, valueType);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<T> localA = code.getParameter(0, valueType);
        Local<T> localB = code.getParameter(1, valueType);
        Local<Integer> localResult = code.newLocal(TypeId.INT);
        code.compareFloatingPoint(localResult, localA, localB, nanValue);
        code.returnValue(localResult);
        return getMethod();
    }

    @Test
    public void testLongCompare() throws Exception {
        /*
         * public static int call(long a, long b) {
         *   int result = a <=> b;
         *   return result;
         * }
         */
        MethodId<?, Integer> methodId = GENERATED.getMethod(TypeId.INT, "call", TypeId.LONG, TypeId.LONG);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Long> localA = code.getParameter(0, TypeId.LONG);
        Local<Long> localB = code.getParameter(1, TypeId.LONG);
        Local<Integer> localResult = code.newLocal(TypeId.INT);
        code.compareLongs(localResult, localA, localB);
        code.returnValue(localResult);

        Method method = getMethod();
        assertEquals(0, method.invoke(null, Long.MIN_VALUE, Long.MIN_VALUE));
        assertEquals(-1, method.invoke(null, Long.MIN_VALUE, 0));
        assertEquals(-1, method.invoke(null, Long.MIN_VALUE, Long.MAX_VALUE));
        assertEquals(1, method.invoke(null, 0, Long.MIN_VALUE));
        assertEquals(0, method.invoke(null, 0, 0));
        assertEquals(-1, method.invoke(null, 0, Long.MAX_VALUE));
        assertEquals(1, method.invoke(null, Long.MAX_VALUE, Long.MIN_VALUE));
        assertEquals(1, method.invoke(null, Long.MAX_VALUE, 0));
        assertEquals(0, method.invoke(null, Long.MAX_VALUE, Long.MAX_VALUE));
    }

    @Test
    public void testArrayLength() throws Exception {
        Method booleanArrayLength = arrayLengthMethod(BOOLEAN_ARRAY);
        assertEquals(0, booleanArrayLength.invoke(null, new Object[] { new boolean[0] }));
        assertEquals(5, booleanArrayLength.invoke(null, new Object[] { new boolean[5] }));

        Method intArrayLength = arrayLengthMethod(INT_ARRAY);
        assertEquals(0, intArrayLength.invoke(null, new Object[] { new int[0] }));
        assertEquals(5, intArrayLength.invoke(null, new Object[] { new int[5] }));

        Method longArrayLength = arrayLengthMethod(LONG_ARRAY);
        assertEquals(0, longArrayLength.invoke(null, new Object[] { new long[0] }));
        assertEquals(5, longArrayLength.invoke(null, new Object[] { new long[5] }));

        Method objectArrayLength = arrayLengthMethod(OBJECT_ARRAY);
        assertEquals(0, objectArrayLength.invoke(null, new Object[] { new Object[0] }));
        assertEquals(5, objectArrayLength.invoke(null, new Object[] { new Object[5] }));

        Method long2dArrayLength = arrayLengthMethod(LONG_2D_ARRAY);
        assertEquals(0, long2dArrayLength.invoke(null, new Object[] { new long[0][0] }));
        assertEquals(5, long2dArrayLength.invoke(null, new Object[] { new long[5][10] }));
    }

    private <T> Method arrayLengthMethod(TypeId<T> valueType) throws Exception {
        /*
         * public static int call(long[] array) {
         *   int result = array.length;
         *   return result;
         * }
         */
        reset();
        MethodId<?, Integer> methodId = GENERATED.getMethod(TypeId.INT, "call", valueType);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<T> localArray = code.getParameter(0, valueType);
        Local<Integer> localResult = code.newLocal(TypeId.INT);
        code.arrayLength(localResult, localArray);
        code.returnValue(localResult);
        return getMethod();
    }

    @Test
    public void testNewArray() throws Exception {
        Method newBooleanArray = newArrayMethod(BOOLEAN_ARRAY);
        assertEquals("[]", Arrays.toString((boolean[]) newBooleanArray.invoke(null, 0)));
        assertEquals("[false, false, false]",
                Arrays.toString((boolean[]) newBooleanArray.invoke(null, 3)));

        Method newIntArray = newArrayMethod(INT_ARRAY);
        assertEquals("[]", Arrays.toString((int[]) newIntArray.invoke(null, 0)));
        assertEquals("[0, 0, 0]", Arrays.toString((int[]) newIntArray.invoke(null, 3)));

        Method newLongArray = newArrayMethod(LONG_ARRAY);
        assertEquals("[]", Arrays.toString((long[]) newLongArray.invoke(null, 0)));
        assertEquals("[0, 0, 0]", Arrays.toString((long[]) newLongArray.invoke(null, 3)));

        Method newObjectArray = newArrayMethod(OBJECT_ARRAY);
        assertEquals("[]", Arrays.toString((Object[]) newObjectArray.invoke(null, 0)));
        assertEquals("[null, null, null]",
                Arrays.toString((Object[]) newObjectArray.invoke(null, 3)));

        Method new2dLongArray = newArrayMethod(LONG_2D_ARRAY);
        assertEquals("[]", Arrays.deepToString((long[][]) new2dLongArray.invoke(null, 0)));
        assertEquals("[null, null, null]",
                Arrays.deepToString((long[][]) new2dLongArray.invoke(null, 3)));
    }

    private <T> Method newArrayMethod(TypeId<T> valueType) throws Exception {
        /*
         * public static long[] call(int length) {
         *   long[] result = new long[length];
         *   return result;
         * }
         */
        reset();
        MethodId<?, T> methodId = GENERATED.getMethod(valueType, "call", TypeId.INT);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localLength = code.getParameter(0, TypeId.INT);
        Local<T> localResult = code.newLocal(valueType);
        code.newArray(localResult, localLength);
        code.returnValue(localResult);
        return getMethod();
    }

    @Test
    public void testReadAndWriteArray() throws Exception {
        Method swapBooleanArray = arraySwapMethod(BOOLEAN_ARRAY, TypeId.BOOLEAN);
        boolean[] booleans = new boolean[3];
        assertEquals(false, swapBooleanArray.invoke(null, booleans, 1, true));
        assertEquals("[false, true, false]", Arrays.toString(booleans));

        Method swapIntArray = arraySwapMethod(INT_ARRAY, TypeId.INT);
        int[] ints = new int[3];
        assertEquals(0, swapIntArray.invoke(null, ints, 1, 5));
        assertEquals("[0, 5, 0]", Arrays.toString(ints));

        Method swapLongArray = arraySwapMethod(LONG_ARRAY, TypeId.LONG);
        long[] longs = new long[3];
        assertEquals(0L, swapLongArray.invoke(null, longs, 1, 6L));
        assertEquals("[0, 6, 0]", Arrays.toString(longs));

        Method swapObjectArray = arraySwapMethod(OBJECT_ARRAY, TypeId.OBJECT);
        Object[] objects = new Object[3];
        assertEquals(null, swapObjectArray.invoke(null, objects, 1, "X"));
        assertEquals("[null, X, null]", Arrays.toString(objects));

        Method swapLong2dArray = arraySwapMethod(LONG_2D_ARRAY, LONG_ARRAY);
        long[][] longs2d = new long[3][];
        assertEquals(null, swapLong2dArray.invoke(null, longs2d, 1, new long[] { 7 }));
        assertEquals("[null, [7], null]", Arrays.deepToString(longs2d));
    }

    private <A, T> Method arraySwapMethod(TypeId<A> arrayType, TypeId<T> singleType)
            throws Exception {
        /*
         * public static long swap(long[] array, int index, long newValue) {
         *   long result = array[index];
         *   array[index] = newValue;
         *   return result;
         * }
         */
        reset();
        MethodId<?, T> methodId = GENERATED.getMethod(
                singleType, "call", arrayType, TypeId.INT, singleType);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<A> localArray = code.getParameter(0, arrayType);
        Local<Integer> localIndex = code.getParameter(1, TypeId.INT);
        Local<T> localNewValue = code.getParameter(2, singleType);
        Local<T> localResult = code.newLocal(singleType);
        code.aget(localResult, localArray, localIndex);
        code.aput(localArray, localIndex, localNewValue);
        code.returnValue(localResult);
        return getMethod();
    }

    @Test
    public void testSynchronizedFlagImpactsDeclarationOnly() throws Exception {
        /*
         * public synchronized void call() {
         *   wait(100L);
         * }
         */
        MethodId<?, Void> methodId = GENERATED.getMethod(TypeId.VOID, "call");
        MethodId<Object, Void> wait = TypeId.OBJECT.getMethod(TypeId.VOID, "wait", TypeId.LONG);
        Code code = dexMaker.declare(methodId, PUBLIC | SYNCHRONIZED);
        Local<?> thisLocal = code.getThis(GENERATED);
        Local<Long> timeout = code.newLocal(TypeId.LONG);
        code.loadConstant(timeout, 100L);
        code.invokeVirtual(wait, null, thisLocal, timeout);
        code.returnVoid();

        addDefaultConstructor();

        Class<?> generatedClass = generateAndLoad();
        Object instance = generatedClass.getDeclaredConstructor().newInstance();
        Method method = generatedClass.getMethod("call");
        assertTrue(Modifier.isSynchronized(method.getModifiers()));
        try {
            method.invoke(instance);
            fail();
        } catch (InvocationTargetException expected) {
            assertTrue(expected.getCause() instanceof IllegalMonitorStateException);
        }
    }

    @Test
    public void testMonitorEnterMonitorExit() throws Exception {
        /*
         * public synchronized void call() {
         *   synchronized (this) {
         *     wait(100L);
         *   }
         * }
         */
        MethodId<?, Void> methodId = GENERATED.getMethod(TypeId.VOID, "call");
        MethodId<Object, Void> wait = TypeId.OBJECT.getMethod(TypeId.VOID, "wait", TypeId.LONG);
        Code code = dexMaker.declare(methodId, PUBLIC);
        Local<?> thisLocal = code.getThis(GENERATED);
        Local<Long> timeout = code.newLocal(TypeId.LONG);
        code.monitorEnter(thisLocal);
        code.loadConstant(timeout, 100L);
        code.invokeVirtual(wait, null, thisLocal, timeout);
        code.monitorExit(thisLocal);
        code.returnVoid();

        addDefaultConstructor();

        Class<?> generatedClass = generateAndLoad();
        Object instance = generatedClass.getDeclaredConstructor().newInstance();
        Method method = generatedClass.getMethod("call");
        assertFalse(Modifier.isSynchronized(method.getModifiers()));
        method.invoke(instance); // will take 100ms
    }

    @Test
    public void testMoveInt() throws Exception {
        /*
         * public static int call(int a) {
         *   int b = a;
         *   int c = a + b;
         *   return c;
         * }
         */
        MethodId<?, Integer> methodId = GENERATED.getMethod(TypeId.INT, "call", TypeId.INT);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        Local<Integer> a = code.getParameter(0, TypeId.INT);
        Local<Integer> b = code.newLocal(TypeId.INT);
        Local<Integer> c = code.newLocal(TypeId.INT);
        code.move(b, a);
        code.op(BinaryOp.ADD, c, a, b);
        code.returnValue(c);

        assertEquals(6, getMethod().invoke(null, 3));
    }

    @Test
    public void testPrivateClassesAreUnsupported() {
        try {
            dexMaker.declare(TypeId.get("LPrivateClass;"), "PrivateClass.generated", PRIVATE,
                    TypeId.OBJECT);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testSynchronizedFieldsAreUnsupported() {
        try {
            FieldId<?, ?> fieldId = GENERATED.getField(TypeId.OBJECT, "synchronizedField");
            dexMaker.declare(fieldId, SYNCHRONIZED, null);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInitialValueWithNonStaticField() {
        try {
            FieldId<?, ?> fieldId = GENERATED.getField(TypeId.OBJECT, "nonStaticField");
            dexMaker.declare(fieldId, 0, 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    // TODO: cast primitive to non-primitive
    // TODO: cast non-primitive to primitive
    // TODO: cast byte to integer
    // TODO: cast byte to long
    // TODO: cast long to byte
    // TODO: fail if a label is unreachable (never navigated to)
    // TODO: more strict type parameters: Integer on methods
    // TODO: don't generate multiple times (?)
    // TODO: test array types
    // TODO: test generating an interface
    // TODO: get a thrown exception 'e' into a local
    // TODO: move a primitive or reference

    private void addDefaultConstructor() {
        Code code = dexMaker.declare(GENERATED.getConstructor(), PUBLIC);
        Local<?> thisRef = code.getThis(GENERATED);
        code.invokeDirect(TypeId.OBJECT.getConstructor(), null, thisRef);
        code.returnVoid();
    }

    @Test
    public void testCaching_Methods() throws Exception {
        int origSize = getDataDirectory().listFiles().length;
        final String defaultMethodName = "call";

        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.INT);
        generateAndLoad();
        int numFiles = getDataDirectory().listFiles().length;
        // DexMaker writes two files to disk at a time: Generated_XXXX.jar and Generated_XXXX.dex.
        assertTrue(origSize < numFiles);

        long lastModified  = getJarFiles()[0].lastModified();

        // Create new dexmaker generator with same method signature.
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.INT);
        generateAndLoad();
        assertEquals(numFiles, getDataDirectory().listFiles().length);
        assertEquals(lastModified, getJarFiles()[0].lastModified());

        // Create new dexmaker generators with different params.
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.DOUBLE);
        generateAndLoad();
        assertTrue(numFiles < getDataDirectory().listFiles().length);
        numFiles = getDataDirectory().listFiles().length;

        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.INT, TypeId.DOUBLE);
        generateAndLoad();
        assertTrue(numFiles < getDataDirectory().listFiles().length);
        numFiles = getDataDirectory().listFiles().length;

        // Create new dexmaker generator with different return types.
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addMethodToDexMakerGenerator(TypeId.DOUBLE, defaultMethodName, TypeId.INT);
        generateAndLoad();
        assertTrue(numFiles < getDataDirectory().listFiles().length);
        numFiles = getDataDirectory().listFiles().length;

        // Create new dexmaker generators with multiple methods.
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.INT);
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.INT, TypeId.BOOLEAN); // new method
        generateAndLoad();
        assertTrue(numFiles < getDataDirectory().listFiles().length);
        numFiles = getDataDirectory().listFiles().length;

        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.INT, TypeId.BOOLEAN);
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.INT);
        generateAndLoad();
        assertEquals(numFiles, getDataDirectory().listFiles().length); // should already be cached.

        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.INT);
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.INT, TypeId.INT, TypeId.BOOLEAN); // new method
        generateAndLoad();
        assertTrue(numFiles < getDataDirectory().listFiles().length);
        numFiles = getDataDirectory().listFiles().length;

        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.INT);
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.INT, TypeId.INT); // new method
        generateAndLoad();
        assertTrue(numFiles < getDataDirectory().listFiles().length);
        numFiles = getDataDirectory().listFiles().length;

        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addMethodToDexMakerGenerator(TypeId.INT, "differentName", TypeId.INT); // new method
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.INT, TypeId.BOOLEAN);
        generateAndLoad();
        assertTrue(numFiles < getDataDirectory().listFiles().length);
    }

    public static class BlankClassA {}

    public static class BlankClassB {}

    @Test
    public void testCaching_DifferentParentClasses() throws Exception {
        int origSize = getDataDirectory().listFiles().length;
        final String defaultMethodName = "call";

        // Create new dexmaker generator with BlankClassA as supertype.
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.get(BlankClassA.class));
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.INT);
        generateAndLoad();
        // DexMaker writes two files to disk at a time: Generated_XXXX.jar and Generated_XXXX.dex.
        int numFiles = getDataDirectory().listFiles().length;
        assertTrue(origSize < numFiles);

        // Create new dexmaker generator with BlankClassB as supertype.
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.get(BlankClassB.class));
        addMethodToDexMakerGenerator(TypeId.INT, defaultMethodName, TypeId.INT);
        generateAndLoad();
        assertTrue(numFiles < getDataDirectory().listFiles().length);
    }

    private void addMethodToDexMakerGenerator(TypeId<?> typeId, String methodName, TypeId<?>... params) throws Exception {
        MethodId<?, ?> methodId = GENERATED.getMethod(typeId, methodName, params);
        Code code = dexMaker.declare(methodId, PUBLIC | STATIC);
        TypeId<IllegalStateException> iseType = TypeId.get(IllegalStateException.class);
        Local<IllegalStateException> localIse = code.newLocal(iseType);
        if (params.length > 0) {
            if (params[0].equals(typeId)) {
                Local<?> localResult = code.getParameter(0, TypeId.INT);
                code.returnValue(localResult);
            } else {
                code.throwValue(localIse);
            }
        } else {
            code.throwValue(localIse);
        }
    }

    public interface BlankInterfaceA {}

    public interface BlankInterfaceB {}

    @Test
    public void testCaching_DifferentInterfaces() throws Exception {
        int origSize = getDataDirectory().listFiles().length;

        // Create new dexmaker generator with BlankInterfaceA.
        dexMaker = new DexMaker();
        TypeId interfaceA = TypeId.get(BlankInterfaceA.class);
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT, interfaceA);
        generateAndLoad();
        int numFiles = getDataDirectory().listFiles().length;
        assertTrue(origSize < numFiles);

        // Create new dexmaker generator with BlankInterfaceB.
        dexMaker = new DexMaker();
        TypeId interfaceB = TypeId.get(BlankInterfaceB.class);
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT, interfaceB);
        generateAndLoad();
        assertTrue(numFiles < getDataDirectory().listFiles().length);
    }

    @Test
    public void testCaching_Constructors() throws Exception {
        int origSize = getDataDirectory().listFiles().length;

        // Create new dexmaker generator with Generated(int) constructor.
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addConstructorToDexMakerGenerator(TypeId.INT);
        generateAndLoad();
        // DexMaker writes two files to disk at a time: Generated_XXXX.jar and Generated_XXXX.dex.
        int numFiles = getDataDirectory().listFiles().length;
        assertTrue(origSize < numFiles);

        long lastModified  = getJarFiles()[0].lastModified();

        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addConstructorToDexMakerGenerator(TypeId.INT);
        generateAndLoad();
        assertEquals(numFiles, getDataDirectory().listFiles().length);
        assertEquals(lastModified, getJarFiles()[0].lastModified());

        // Create new dexmaker generator with Generated(boolean) constructor.
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addConstructorToDexMakerGenerator(TypeId.BOOLEAN);
        generateAndLoad();
        assertTrue(numFiles < getDataDirectory().listFiles().length);
        numFiles = getDataDirectory().listFiles().length;

        // Create new dexmaker generator with multiple constructors.
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addConstructorToDexMakerGenerator(TypeId.INT);
        addConstructorToDexMakerGenerator(TypeId.BOOLEAN);
        generateAndLoad();
        assertTrue(numFiles < getDataDirectory().listFiles().length);
        numFiles = getDataDirectory().listFiles().length;

        // Ensure that order of constructors does not affect caching decision.
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        addConstructorToDexMakerGenerator(TypeId.BOOLEAN);
        addConstructorToDexMakerGenerator(TypeId.INT);
        generateAndLoad();
        assertEquals(numFiles, getDataDirectory().listFiles().length);
    }

    private void addConstructorToDexMakerGenerator(TypeId<?>... params) throws Exception {
        MethodId<?, Void> constructor = GENERATED.getConstructor(params);
        Code code = dexMaker.declare(constructor, PUBLIC);
        code.returnVoid();
    }

    private File[] getJarFiles() {
        return getDataDirectory().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
    }

    /**
     * Returns the generated method.
     */
    private Method getMethod() throws Exception {
        Class<?> generated = generateAndLoad();
        for (Method method : generated.getMethods()) {
            if (method.getName().equals("call")) {
                return method;
            }
        }
        throw new IllegalStateException("no call() method");
    }

    public static File getDataDirectory() {
        String dataDir = InstrumentationRegistry.getTargetContext().getApplicationInfo().dataDir;
        return new File(dataDir);
    }

    private Class<?> generateAndLoad() throws Exception {
        return dexMaker.generateAndLoad(getClass().getClassLoader(), getDataDirectory())
                .loadClass("Generated");
    }

    private final ClassLoader commonClassLoader = new BaseDexClassLoader(
            getDataDirectory().getPath(), getDataDirectory(), getDataDirectory().getPath(),
            DexMakerTest.class.getClassLoader());

    private final ClassLoader uncommonClassLoader = new ClassLoader() {
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new IllegalStateException("Not used");
        }
    };

    private static void loadWithSharedClassLoader(ClassLoader cl, boolean markAsTrusted,
                                                  boolean shouldUseCL) throws Exception {
        DexMaker d = new DexMaker();
        d.setSharedClassLoader(cl);

        if (markAsTrusted) {
            d.markAsTrusted();
        }

        ClassLoader selectedCL = d.generateAndLoad(null, getDataDirectory());

        if (shouldUseCL) {
            assertSame(cl, selectedCL);
        } else {
            assertNotSame(cl, selectedCL);

            // An appropriate fallback should have been selected
            assertNotNull(selectedCL);
        }
    }

    @Test
    public void loadWithUncommonSharedClassLoader() throws Exception{
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);

        loadWithSharedClassLoader(uncommonClassLoader, false, false);
    }

    @Test
    public void loadWithCommonSharedClassLoader() throws Exception{
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);

        loadWithSharedClassLoader(commonClassLoader, false, true);
    }

    @Test
    public void loadAsTrustedWithUncommonSharedClassLoader() throws Exception{
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P);

        loadWithSharedClassLoader(uncommonClassLoader, true, false);
    }

    @Test
    public void loadAsTrustedWithCommonSharedClassLoader() throws Exception{
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P);

        loadWithSharedClassLoader(commonClassLoader, true, true);
    }
}
