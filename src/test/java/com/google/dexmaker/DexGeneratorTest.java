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

package com.google.dexmaker;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PROTECTED;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import junit.framework.TestCase;

/**
 * This generates a class named 'Generated' with one or more generated methods
 * and fields. In loads the generated class into the current VM and uses
 * reflection to invoke its methods.
 *
 * <p>This test must run on a Dalvik VM.
 */
public final class DexGeneratorTest extends TestCase {
    private DexGenerator generator;
    private static Type<DexGeneratorTest> TEST_TYPE = Type.get(DexGeneratorTest.class);
    private static Type<?> INT_ARRAY = Type.get(int[].class);
    private static Type<boolean[]> BOOLEAN_ARRAY = Type.get(boolean[].class);
    private static Type<long[]> LONG_ARRAY = Type.get(long[].class);
    private static Type<Object[]> OBJECT_ARRAY = Type.get(Object[].class);
    private static Type<long[][]> LONG_2D_ARRAY = Type.get(long[][].class);
    private static Type<?> GENERATED = Type.get("LGenerated;");
    private static Type<Callable> CALLABLE = Type.get(Callable.class);
    private static MethodId<Callable, Object> CALL = CALLABLE.getMethod(Type.OBJECT, "call");

    @Override protected void setUp() throws Exception {
        super.setUp();
        reset();
    }

    /**
     * The generator is mutable. Calling reset creates a new empty generator.
     * This is necessary to generate multiple classes in the same test method.
     */
    private void reset() {
        generator = new DexGenerator();
        generator.declare(GENERATED, "Generated.java", PUBLIC, Type.OBJECT);
    }

    public void testNewInstance() throws Exception {
        /*
         * public static Constructable call(long a, boolean b) {
         *   Constructable result = new Constructable(a, b);
         *   return result;
         * }
         */
        Type<Constructable> constructable = Type.get(Constructable.class);
        MethodId<?, Constructable> methodId = GENERATED.getMethod(
                constructable, "call", Type.LONG, Type.BOOLEAN);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Long> localA = code.getParameter(0, Type.LONG);
        Local<Boolean> localB = code.getParameter(1, Type.BOOLEAN);
        MethodId<Constructable, Void> constructor
                = constructable.getConstructor(Type.LONG, Type.BOOLEAN);
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

    public void testVoidNoArgMemberMethod() throws Exception {
        /*
         * public void call() {
         * }
         */
        MethodId<?, Void> methodId = GENERATED.getMethod(Type.VOID, "call");
        Code code = generator.declare(methodId, PUBLIC);
        code.returnVoid();

        addDefaultConstructor();

        Class<?> generatedClass = loadAndGenerate();
        Object instance = generatedClass.newInstance();
        Method method = generatedClass.getMethod("call");
        method.invoke(instance);
    }

    public void testInvokeStatic() throws Exception {
        /*
         * public static int call(int a) {
         *   int result = DexGeneratorTest.staticMethod(a);
         *   return result;
         * }
         */
        MethodId<?, Integer> methodId = GENERATED.getMethod(Type.INT, "call", Type.INT);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localA = code.getParameter(0, Type.INT);
        Local<Integer> localResult = code.newLocal(Type.INT);
        MethodId<?, Integer> staticMethod
                = TEST_TYPE.getMethod(Type.INT, "staticMethod", Type.INT);
        code.invokeStatic(staticMethod, localResult, localA);
        code.returnValue(localResult);

        assertEquals(10, getMethod().invoke(null, 4));
    }

    public void testCreateLocalMethodAsNull() throws Exception {
        /*
         * public void call(int value) {
         *   Method method = null;
         * }
         */
        MethodId<?, Void> methodId = GENERATED.getMethod(Type.VOID, "call", Type.INT);
        Type<Method> methodType = Type.get(Method.class);
        Code code = generator.declare(methodId, PUBLIC);
        Local<Method> localMethod = code.newLocal(methodType);
        code.loadConstant(localMethod, null);
        code.returnVoid();

        addDefaultConstructor();

        Class<?> generatedClass = loadAndGenerate();
        Object instance = generatedClass.newInstance();
        Method method = generatedClass.getMethod("call", int.class);
        method.invoke(instance, 0);
    }

    @SuppressWarnings("unused") // called by generated code
    public static int staticMethod(int a) {
        return a + 6;
    }

    public void testInvokeVirtual() throws Exception {
        /*
         * public static int call(DexGeneratorTest test, int a) {
         *   int result = test.virtualMethod(a);
         *   return result;
         * }
         */
        MethodId<?, Integer> methodId = GENERATED.getMethod(Type.INT, "call", TEST_TYPE, Type.INT);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<DexGeneratorTest> localInstance = code.getParameter(0, TEST_TYPE);
        Local<Integer> localA = code.getParameter(1, Type.INT);
        Local<Integer> localResult = code.newLocal(Type.INT);
        MethodId<DexGeneratorTest, Integer> virtualMethod
                = TEST_TYPE.getMethod(Type.INT, "virtualMethod", Type.INT);
        code.invokeVirtual(virtualMethod, localResult, localInstance, localA);
        code.returnValue(localResult);

        assertEquals(9, getMethod().invoke(null, this, 4));
    }

    @SuppressWarnings("unused") // called by generated code
    public int virtualMethod(int a) {
        return a + 5;
    }

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
        Type<G> generated = Type.get("LGenerated;");
        MethodId<G, Integer> directMethodId = generated.getMethod(Type.INT, "directMethod");
        Code directCode = generator.declare(directMethodId, PRIVATE);
        directCode.getThis(generated); // 'this' is unused
        Local<Integer> localA = directCode.newLocal(Type.INT);
        directCode.loadConstant(localA, 5);
        directCode.returnValue(localA);

        MethodId<G, Integer> methodId = generated.getMethod(Type.INT, "call", generated);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localB = code.newLocal(Type.INT);
        Local<G> localG = code.getParameter(0, generated);
        code.invokeDirect(directMethodId, localB, localG);
        code.returnValue(localB);

        addDefaultConstructor();

        Class<?> generatedClass = loadAndGenerate();
        Object instance = generatedClass.newInstance();
        Method method = generatedClass.getMethod("call", generatedClass);
        assertEquals(5, method.invoke(null, instance));
    }

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
        Type<G> generated = Type.get("LGenerated;");
        MethodId<Object, Integer> objectHashCode = Type.OBJECT.getMethod(Type.INT, "hashCode");
        Code superHashCode = generator.declare(
                GENERATED.getMethod(Type.INT, "superHashCode"), PUBLIC);
        Local<Integer> localResult = superHashCode.newLocal(Type.INT);
        Local<G> localThis = superHashCode.getThis(generated);
        superHashCode.invokeSuper(objectHashCode, localResult, localThis);
        superHashCode.returnValue(localResult);

        Code generatedHashCode = generator.declare(
                GENERATED.getMethod(Type.INT, "hashCode"), PUBLIC);
        Local<Integer> localZero = generatedHashCode.newLocal(Type.INT);
        generatedHashCode.loadConstant(localZero, 0);
        generatedHashCode.returnValue(localZero);

        addDefaultConstructor();

        Class<?> generatedClass = loadAndGenerate();
        Object instance = generatedClass.newInstance();
        Method method = generatedClass.getMethod("superHashCode");
        assertEquals(System.identityHashCode(instance), method.invoke(instance));
    }

    @SuppressWarnings("unused") // called by generated code
    public int superMethod(int a) {
        return a + 4;
    }

    public void testInvokeInterface() throws Exception {
        /*
         * public static Object call(Callable c) {
         *   Object result = c.call();
         *   return result;
         * }
         */
        MethodId<?, Object> methodId = GENERATED.getMethod(Type.OBJECT, "call", CALLABLE);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Callable> localC = code.getParameter(0, CALLABLE);
        Local<Object> localResult = code.newLocal(Type.OBJECT);
        code.invokeInterface(CALL, localResult, localC);
        code.returnValue(localResult);

        Callable<Object> callable = new Callable<Object>() {
            public Object call() throws Exception {
                return "abc";
            }
        };
        assertEquals("abc", getMethod().invoke(null, callable));
    }

    public void testParameterMismatch() throws Exception {
        Type<?>[] argTypes = {
                Type.get(Integer.class), // should fail because the code specifies int
                Type.OBJECT,
        };
        MethodId<?, Integer> methodId = GENERATED.getMethod(Type.INT, "call", argTypes);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        try {
            code.getParameter(0, Type.INT);
        } catch (IllegalArgumentException e) {
        }
        try {
            code.getParameter(2, Type.INT);
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testInvokeTypeSafety() throws Exception {
        /*
         * public static boolean call(DexGeneratorTest test) {
         *   CharSequence cs = test.toString();
         *   boolean result = cs.equals(test);
         *   return result;
         * }
         */
        MethodId<?, Boolean> methodId = GENERATED.getMethod(Type.BOOLEAN, "call", TEST_TYPE);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<DexGeneratorTest> localTest = code.getParameter(0, TEST_TYPE);
        Type<CharSequence> charSequenceType = Type.get(CharSequence.class);
        MethodId<Object, String> objectToString = Type.OBJECT.getMethod(Type.STRING, "toString");
        MethodId<Object, Boolean> objectEquals
                = Type.OBJECT.getMethod(Type.BOOLEAN, "equals", Type.OBJECT);
        Local<CharSequence> localCs = code.newLocal(charSequenceType);
        Local<Boolean> localResult = code.newLocal(Type.BOOLEAN);
        code.invokeVirtual(objectToString, localCs, localTest);
        code.invokeVirtual(objectEquals, localResult, localCs, localTest);
        code.returnValue(localResult);

        assertEquals(false, getMethod().invoke(null, this));
    }

    public void testReturnTypeMismatch() {
        MethodId<?, String> methodId = GENERATED.getMethod(Type.STRING, "call");
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        try {
            code.returnValue(code.newLocal(Type.BOOLEAN));
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            code.returnVoid();
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testDeclareStaticFields() throws Exception {
        /*
         * class Generated {
         *   public static int a;
         *   protected static Object b;
         * }
         */
        generator.declare(GENERATED.getField(Type.INT, "a"), PUBLIC | STATIC, 3);
        generator.declare(GENERATED.getField(Type.OBJECT, "b"), PROTECTED | STATIC, null);
        Class<?> generatedClass = loadAndGenerate();

        Field a = generatedClass.getField("a");
        assertEquals(int.class, a.getType());
        assertEquals(3, a.get(null));

        Field b = generatedClass.getDeclaredField("b");
        assertEquals(Object.class, b.getType());
        b.setAccessible(true);
        assertEquals(null, b.get(null));
    }

    public void testDeclareInstanceFields() throws Exception {
        /*
         * class Generated {
         *   public int a;
         *   protected Object b;
         * }
         */
        generator.declare(GENERATED.getField(Type.INT, "a"), PUBLIC, null);
        generator.declare(GENERATED.getField(Type.OBJECT, "b"), PROTECTED, null);

        addDefaultConstructor();

        Class<?> generatedClass = loadAndGenerate();
        Object instance = generatedClass.newInstance();

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
    public <G> void testDeclareConstructor() throws Exception {
        /*
         * class Generated {
         *   public final int a;
         *   public Generated(int a) {
         *     this.a = a;
         *   }
         * }
         */
        Type<G> generated = Type.get("LGenerated;");
        FieldId<G, Integer> fieldId = generated.getField(Type.INT, "a");
        generator.declare(fieldId, PUBLIC | FINAL, null);
        MethodId<?, Void> constructor = GENERATED.getConstructor(Type.INT);
        Code code = generator.declareConstructor(constructor, PUBLIC);
        Local<G> thisRef = code.getThis(generated);
        Local<Integer> parameter = code.getParameter(0, Type.INT);
        code.invokeDirect(Type.OBJECT.getConstructor(), null, thisRef);
        code.iput(fieldId, thisRef, parameter);
        code.returnVoid();

        Class<?> generatedClass = loadAndGenerate();
        Field a = generatedClass.getField("a");
        Object instance = generatedClass.getConstructor(int.class).newInstance(0xabcd);
        assertEquals(0xabcd, a.get(instance));
    }

    public void testReturnBoolean() throws Exception {
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
        Type<T> returnType = Type.get(javaType);
        Code code = generator.declare(GENERATED.getMethod(returnType, "call"), PUBLIC | STATIC);
        if (value != null) {
            Local<T> i = code.newLocal(returnType);
            code.loadConstant(i, value);
            code.returnValue(i);
        } else {
            code.returnVoid();
        }

        Class<?> generatedClass = loadAndGenerate();
        Method method = generatedClass.getMethod("call");
        assertEquals(javaType, method.getReturnType());
        assertEquals(value, method.invoke(null));
    }

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
                Type.BOOLEAN, "call", Type.INT, Type.INT);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localA = code.getParameter(0, Type.INT);
        Local<Integer> localB = code.getParameter(1, Type.INT);
        Local<Boolean> result = code.newLocal(Type.get(boolean.class));
        Label afterIf = code.newLabel();
        Label ifBody = code.newLabel();
        code.compare(comparison, localA, localB, ifBody);
        code.jump(afterIf);

        code.mark(ifBody);
        code.loadConstant(result, true);
        code.returnValue(result);

        code.mark(afterIf);
        code.loadConstant(result, false);
        code.returnValue(result);
        return getMethod();
    }

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
        Type<?> sourceType = Type.get(source);
        Type<?> targetType = Type.get(target);
        MethodId<?, ?> methodId = GENERATED.getMethod(targetType, "call", sourceType);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<?> localSource = code.getParameter(0, sourceType);
        Local<?> localCasted = code.newLocal(targetType);
        code.numericCast(localSource, localCasted);
        code.returnValue(localCasted);
        return getMethod();
    }

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
        Type<T> valueType = Type.get(source);
        MethodId<?, T> methodId = GENERATED.getMethod(valueType, "call", valueType);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<T> localSource = code.getParameter(0, valueType);
        code.not(localSource, localSource);
        code.returnValue(localSource);
        return getMethod();
    }

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
        Type<T> valueType = Type.get(source);
        MethodId<?, T> methodId = GENERATED.getMethod(valueType, "call", valueType);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<T> localSource = code.getParameter(0, valueType);
        code.negate(localSource, localSource);
        code.returnValue(localSource);
        return getMethod();
    }

    public void testIntBinaryOps() throws Exception {
        Method add = binaryOpMethod(int.class, BinaryOp.ADD);
        assertEquals(79, add.invoke(null, 75, 4));

        Method subtract = binaryOpMethod(int.class, BinaryOp.SUBTRACT);
        assertEquals(71, subtract.invoke(null, 75, 4));

        Method multiply = binaryOpMethod(int.class, BinaryOp.MULTIPLY);
        assertEquals(300, multiply.invoke(null, 75, 4));

        Method divide = binaryOpMethod(int.class, BinaryOp.DIVIDE);
        assertEquals(18, divide.invoke(null, 75, 4));
        try {
            divide.invoke(null, 75, 0);
            fail();
        } catch (InvocationTargetException expected) {
            assertEquals(ArithmeticException.class, expected.getCause().getClass());
        }

        Method remainder = binaryOpMethod(int.class, BinaryOp.REMAINDER);
        assertEquals(3, remainder.invoke(null, 75, 4));
        try {
            remainder.invoke(null, 75, 0);
            fail();
        } catch (InvocationTargetException expected) {
            assertEquals(ArithmeticException.class, expected.getCause().getClass());
        }

        Method and = binaryOpMethod(int.class, BinaryOp.AND);
        assertEquals(0xff000000, and.invoke(null, 0xff00ff00, 0xffff0000));

        Method or = binaryOpMethod(int.class, BinaryOp.OR);
        assertEquals(0xffffff00, or.invoke(null, 0xff00ff00, 0xffff0000));

        Method xor = binaryOpMethod(int.class, BinaryOp.XOR);
        assertEquals(0x00ffff00, xor.invoke(null, 0xff00ff00, 0xffff0000));

        Method shiftLeft = binaryOpMethod(int.class, BinaryOp.SHIFT_LEFT);
        assertEquals(0xcd123400, shiftLeft.invoke(null, 0xabcd1234, 8));

        Method shiftRight = binaryOpMethod(int.class, BinaryOp.SHIFT_RIGHT);
        assertEquals(0xffabcd12, shiftRight.invoke(null, 0xabcd1234, 8));

        Method unsignedShiftRight = binaryOpMethod(int.class,
                BinaryOp.UNSIGNED_SHIFT_RIGHT);
        assertEquals(0x00abcd12, unsignedShiftRight.invoke(null, 0xabcd1234, 8));
    }

    public void testLongBinaryOps() throws Exception {
        Method add = binaryOpMethod(long.class, BinaryOp.ADD);
        assertEquals(79L, add.invoke(null, 75L, 4L));

        Method subtract = binaryOpMethod(long.class, BinaryOp.SUBTRACT);
        assertEquals(71L, subtract.invoke(null, 75L, 4L));

        Method multiply = binaryOpMethod(long.class, BinaryOp.MULTIPLY);
        assertEquals(300L, multiply.invoke(null, 75L, 4L));

        Method divide = binaryOpMethod(long.class, BinaryOp.DIVIDE);
        assertEquals(18L, divide.invoke(null, 75L, 4L));
        try {
            divide.invoke(null, 75L, 0L);
            fail();
        } catch (InvocationTargetException expected) {
            assertEquals(ArithmeticException.class, expected.getCause().getClass());
        }

        Method remainder = binaryOpMethod(long.class, BinaryOp.REMAINDER);
        assertEquals(3L, remainder.invoke(null, 75L, 4L));
        try {
            remainder.invoke(null, 75L, 0L);
            fail();
        } catch (InvocationTargetException expected) {
            assertEquals(ArithmeticException.class, expected.getCause().getClass());
        }

        Method and = binaryOpMethod(long.class, BinaryOp.AND);
        assertEquals(0xff00ff0000000000L,
                and.invoke(null, 0xff00ff00ff00ff00L, 0xffffffff00000000L));

        Method or = binaryOpMethod(long.class, BinaryOp.OR);
        assertEquals(0xffffffffff00ff00L,
                or.invoke(null, 0xff00ff00ff00ff00L, 0xffffffff00000000L));

        Method xor = binaryOpMethod(long.class, BinaryOp.XOR);
        assertEquals(0x00ff00ffff00ff00L,
                xor.invoke(null, 0xff00ff00ff00ff00L, 0xffffffff00000000L));

        Method shiftLeft = binaryOpMethod(long.class, BinaryOp.SHIFT_LEFT);
        assertEquals(0xcdef012345678900L, shiftLeft.invoke(null, 0xabcdef0123456789L, 8L));

        Method shiftRight = binaryOpMethod(long.class, BinaryOp.SHIFT_RIGHT);
        assertEquals(0xffabcdef01234567L, shiftRight.invoke(null, 0xabcdef0123456789L, 8L));

        Method unsignedShiftRight = binaryOpMethod(long.class,
                BinaryOp.UNSIGNED_SHIFT_RIGHT);
        assertEquals(0x00abcdef01234567L, unsignedShiftRight.invoke(null, 0xabcdef0123456789L, 8L));
    }

    public void testFloatBinaryOps() throws Exception {
        Method add = binaryOpMethod(float.class, BinaryOp.ADD);
        assertEquals(6.75f, add.invoke(null, 5.5f, 1.25f));

        Method subtract = binaryOpMethod(float.class, BinaryOp.SUBTRACT);
        assertEquals(4.25f, subtract.invoke(null, 5.5f, 1.25f));

        Method multiply = binaryOpMethod(float.class, BinaryOp.MULTIPLY);
        assertEquals(6.875f, multiply.invoke(null, 5.5f, 1.25f));

        Method divide = binaryOpMethod(float.class, BinaryOp.DIVIDE);
        assertEquals(4.4f, divide.invoke(null, 5.5f, 1.25f));
        assertEquals(Float.POSITIVE_INFINITY, divide.invoke(null, 5.5f, 0.0f));

        Method remainder = binaryOpMethod(float.class, BinaryOp.REMAINDER);
        assertEquals(0.5f, remainder.invoke(null, 5.5f, 1.25f));
        assertEquals(Float.NaN, remainder.invoke(null, 5.5f, 0.0f));
    }

    public void testDoubleBinaryOps() throws Exception {
        Method add = binaryOpMethod(double.class, BinaryOp.ADD);
        assertEquals(6.75, add.invoke(null, 5.5, 1.25));

        Method subtract = binaryOpMethod(double.class, BinaryOp.SUBTRACT);
        assertEquals(4.25, subtract.invoke(null, 5.5, 1.25));

        Method multiply = binaryOpMethod(double.class, BinaryOp.MULTIPLY);
        assertEquals(6.875, multiply.invoke(null, 5.5, 1.25));

        Method divide = binaryOpMethod(double.class, BinaryOp.DIVIDE);
        assertEquals(4.4, divide.invoke(null, 5.5, 1.25));
        assertEquals(Double.POSITIVE_INFINITY, divide.invoke(null, 5.5, 0.0));

        Method remainder = binaryOpMethod(double.class, BinaryOp.REMAINDER);
        assertEquals(0.5, remainder.invoke(null, 5.5, 1.25));
        assertEquals(Double.NaN, remainder.invoke(null, 5.5, 0.0));
    }

    private <T> Method binaryOpMethod(Class<T> valueClass, BinaryOp op)
            throws Exception {
        /*
         * public static int binaryOp(int a, int b) {
         *   int result = a + b;
         *   return result;
         * }
         */
        reset();
        Type<T> valueType = Type.get(valueClass);
        MethodId<?, T> methodId = GENERATED.getMethod(valueType, "call", valueType, valueType);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<T> localA = code.getParameter(0, valueType);
        Local<T> localB = code.getParameter(1, valueType);
        Local<T> localResult = code.newLocal(valueType);
        code.op(op, localResult, localA, localB);
        code.returnValue(localResult);
        return getMethod();
    }

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
        assertEquals(0.5f, instance.floatValue);

        Method doubleSwap = instanceSwapMethod(double.class, "doubleValue");
        instance.doubleValue = 155.5;
        assertEquals(155.5, doubleSwap.invoke(null, instance, 266.6));
        assertEquals(266.6, instance.doubleValue);

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

    public class Instance {
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
        Type<V> valueType = Type.get(valueClass);
        Type<Instance> objectType = Type.get(Instance.class);
        FieldId<Instance, V> fieldId = objectType.getField(valueType, fieldName);
        MethodId<?, V> methodId = GENERATED.getMethod(valueType, "call", objectType, valueType);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Instance> localInstance = code.getParameter(0, objectType);
        Local<V> localNewValue = code.getParameter(1, valueType);
        Local<V> localOldValue = code.newLocal(valueType);
        code.iget(fieldId, localInstance, localOldValue);
        code.iput(fieldId, localInstance, localNewValue);
        code.returnValue(localOldValue);
        return getMethod();
    }

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
        assertEquals(0.5f, Static.floatValue);

        Method doubleSwap = staticSwapMethod(double.class, "doubleValue");
        Static.doubleValue = 155.5;
        assertEquals(155.5, doubleSwap.invoke(null, 266.6));
        assertEquals(266.6, Static.doubleValue);

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
        Type<V> valueType = Type.get(valueClass);
        Type<Static> objectType = Type.get(Static.class);
        FieldId<Static, V> fieldId = objectType.getField(valueType, fieldName);
        MethodId<?, V> methodId = GENERATED.getMethod(valueType, "call", valueType);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<V> localNewValue = code.getParameter(0, valueType);
        Local<V> localOldValue = code.newLocal(valueType);
        code.sget(fieldId, localOldValue);
        code.sput(fieldId, localNewValue);
        code.returnValue(localOldValue);
        return getMethod();
    }

    public void testTypeCast() throws Exception {
        /*
         * public static String call(Object o) {
         *   String s = (String) o;
         * }
         */
        MethodId<?, String> methodId = GENERATED.getMethod(Type.STRING, "call", Type.OBJECT);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Object> localObject = code.getParameter(0, Type.OBJECT);
        Local<String> localString = code.newLocal(Type.STRING);
        code.typeCast(localObject, localString);
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

    public void testInstanceOf() throws Exception {
        /*
         * public static boolean call(Object o) {
         *   boolean result = o instanceof String;
         *   return result;
         * }
         */
        MethodId<?, Boolean> methodId = GENERATED.getMethod(Type.BOOLEAN, "call", Type.OBJECT);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Object> localObject = code.getParameter(0, Type.OBJECT);
        Local<Boolean> localResult = code.newLocal(Type.BOOLEAN);
        code.instanceOfType(localResult, localObject, Type.STRING);
        code.returnValue(localResult);

        Method method = getMethod();
        assertEquals(true, method.invoke(null, "s"));
        assertEquals(false, method.invoke(null, (String) null));
        assertEquals(false, method.invoke(null, 5));
    }

    /**
     * Tests that we can construct a for loop.
     */
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
        MethodId<?, Integer> methodId = GENERATED.getMethod(Type.INT, "call", Type.INT);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localCount = code.getParameter(0, Type.INT);
        Local<Integer> localResult = code.newLocal(Type.INT);
        Local<Integer> localI = code.newLocal(Type.INT);
        Local<Integer> local1 = code.newLocal(Type.INT);
        Local<Integer> local2 = code.newLocal(Type.INT);
        code.loadConstant(local1, 1);
        code.loadConstant(local2, 2);
        code.loadConstant(localResult, 1);
        code.loadConstant(localI, 0);
        Label loopCondition = code.newLabel();
        Label loopBody = code.newLabel();
        Label afterLoop = code.newLabel();
        code.mark(loopCondition);
        code.compare(Comparison.LT, localI, localCount, loopBody);
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
        MethodId<?, Integer> methodId = GENERATED.getMethod(Type.INT, "call", Type.INT);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localMax = code.getParameter(0, Type.INT);
        Local<Integer> localResult = code.newLocal(Type.INT);
        Local<Integer> local2 = code.newLocal(Type.INT);
        code.loadConstant(localResult, 1);
        code.loadConstant(local2, 2);
        Label loopCondition = code.newLabel();
        Label loopBody = code.newLabel();
        Label afterLoop = code.newLabel();
        code.mark(loopCondition);
        code.compare(Comparison.LT, localResult, localMax, loopBody);
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
                Type.INT, "call", Type.INT, Type.INT, Type.INT);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localA = code.getParameter(0, Type.INT);
        Local<Integer> localB = code.getParameter(1, Type.INT);
        Local<Integer> localC = code.getParameter(2, Type.INT);
        Label aLessThanB = code.newLabel();
        Label aLessThanC = code.newLabel();
        Label bLessThanC = code.newLabel();
        code.compare(Comparison.LT, localA, localB, aLessThanB);
        code.compare(Comparison.LT, localB, localC, bLessThanC);
        code.returnValue(localC);
        // (a < b)
        code.mark(aLessThanB);
        code.compare(Comparison.LT, localA, localC, aLessThanC);
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
        MethodId<?, Integer> methodId = GENERATED.getMethod(Type.INT, "call", Type.INT);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localA = code.getParameter(0, Type.INT);
        Local<Integer> local1 = code.newLocal(Type.INT);
        Local<Integer> local2 = code.newLocal(Type.INT);
        Local<Integer> localX = code.newLocal(Type.INT);
        Local<Integer> localY = code.newLocal(Type.INT);
        Local<Integer> localResult = code.newLocal(Type.INT);
        Label baseCase = code.newLabel();
        code.loadConstant(local1, 1);
        code.loadConstant(local2, 2);
        code.compare(Comparison.LT, localA, local2, baseCase);
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

    public void testCatchExceptions() throws Exception {
        /*
         * public static String call(int i) {
         *   try {
         *     DexGeneratorTest.thrower(i);
         *     return "NONE";
         *   } catch (IllegalArgumentException e) {
         *     return "IAE";
         *   } catch (IllegalStateException e) {
         *     return "ISE";
         *   } catch (RuntimeException e) {
         *     return "RE";
         *   }
         */
        MethodId<?, String> methodId = GENERATED.getMethod(Type.STRING, "call", Type.INT);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localI = code.getParameter(0, Type.INT);
        Local<String> result = code.newLocal(Type.STRING);
        Label catchIae = code.newLabel();
        Label catchIse = code.newLabel();
        Label catchRe = code.newLabel();

        code.addCatchClause(Type.get(IllegalArgumentException.class), catchIae);
        code.addCatchClause(Type.get(IllegalStateException.class), catchIse);
        code.addCatchClause(Type.get(RuntimeException.class), catchRe);
        MethodId<?, ?> thrower = TEST_TYPE.getMethod(Type.VOID, "thrower", Type.INT);
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

    public void testNestedCatchClauses() throws Exception {
        /*
         * public static String call(int a, int b, int c) {
         *   try {
         *     DexGeneratorTest.thrower(a);
         *     try {
         *       DexGeneratorTest.thrower(b);
         *     } catch (IllegalArgumentException) {
         *       return "INNER";
         *     }
         *     DexGeneratorTest.thrower(c);
         *     return "NONE";
         *   } catch (IllegalArgumentException e) {
         *     return "OUTER";
         *   }
         */
        MethodId<?, String> methodId = GENERATED.getMethod(
                Type.STRING, "call", Type.INT, Type.INT, Type.INT);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localA = code.getParameter(0, Type.INT);
        Local<Integer> localB = code.getParameter(1, Type.INT);
        Local<Integer> localC = code.getParameter(2, Type.INT);
        Local<String> localResult = code.newLocal(Type.STRING);
        Label catchInner = code.newLabel();
        Label catchOuter = code.newLabel();

        Type<IllegalArgumentException> iaeType = Type.get(IllegalArgumentException.class);
        code.addCatchClause(iaeType, catchOuter);

        MethodId<?, ?> thrower = TEST_TYPE.getMethod(Type.VOID, "thrower", Type.INT);
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

    public void testThrow() throws Exception {
        /*
         * public static void call() {
         *   throw new IllegalStateException();
         * }
         */
        MethodId<?, Void> methodId = GENERATED.getMethod(Type.VOID, "call");
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Type<IllegalStateException> iseType = Type.get(IllegalStateException.class);
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

    public void testUnusedParameters() throws Exception {
        /*
         * public static void call(int unused1, long unused2, long unused3) {}
         */
        MethodId<?, Void> methodId = GENERATED.getMethod(
                Type.VOID, "call", Type.INT, Type.LONG, Type.LONG);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        code.returnVoid();
        getMethod().invoke(null, 1, 2, 3);
    }

    public void testFloatingPointCompare() throws Exception {
        Method floatG = floatingPointCompareMethod(Type.FLOAT, 1);
        assertEquals(-1, floatG.invoke(null, 1.0f, Float.POSITIVE_INFINITY));
        assertEquals(-1, floatG.invoke(null, 1.0f, 2.0f));
        assertEquals(0, floatG.invoke(null, 1.0f, 1.0f));
        assertEquals(1, floatG.invoke(null, 2.0f, 1.0f));
        assertEquals(1, floatG.invoke(null, 1.0f, Float.NaN));
        assertEquals(1, floatG.invoke(null, Float.NaN, 1.0f));
        assertEquals(1, floatG.invoke(null, Float.NaN, Float.NaN));
        assertEquals(1, floatG.invoke(null, Float.NaN, Float.POSITIVE_INFINITY));

        Method floatL = floatingPointCompareMethod(Type.FLOAT, -1);
        assertEquals(-1, floatG.invoke(null, 1.0f, Float.POSITIVE_INFINITY));
        assertEquals(-1, floatL.invoke(null, 1.0f, 2.0f));
        assertEquals(0, floatL.invoke(null, 1.0f, 1.0f));
        assertEquals(1, floatL.invoke(null, 2.0f, 1.0f));
        assertEquals(-1, floatL.invoke(null, 1.0f, Float.NaN));
        assertEquals(-1, floatL.invoke(null, Float.NaN, 1.0f));
        assertEquals(-1, floatL.invoke(null, Float.NaN, Float.NaN));
        assertEquals(-1, floatL.invoke(null, Float.NaN, Float.POSITIVE_INFINITY));

        Method doubleG = floatingPointCompareMethod(Type.DOUBLE, 1);
        assertEquals(-1, doubleG.invoke(null, 1.0, Double.POSITIVE_INFINITY));
        assertEquals(-1, doubleG.invoke(null, 1.0, 2.0));
        assertEquals(0, doubleG.invoke(null, 1.0, 1.0));
        assertEquals(1, doubleG.invoke(null, 2.0, 1.0));
        assertEquals(1, doubleG.invoke(null, 1.0, Double.NaN));
        assertEquals(1, doubleG.invoke(null, Double.NaN, 1.0));
        assertEquals(1, doubleG.invoke(null, Double.NaN, Double.NaN));
        assertEquals(1, doubleG.invoke(null, Double.NaN, Double.POSITIVE_INFINITY));

        Method doubleL = floatingPointCompareMethod(Type.DOUBLE, -1);
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
            Type<T> valueType, int nanValue) throws Exception {
        /*
         * public static int call(float a, float b) {
         *     int result = a <=> b;
         *     return result;
         * }
         */
        reset();
        MethodId<?, Integer> methodId = GENERATED.getMethod(Type.INT, "call", valueType, valueType);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<T> localA = code.getParameter(0, valueType);
        Local<T> localB = code.getParameter(1, valueType);
        Local<Integer> localResult = code.newLocal(Type.INT);
        code.compare(localA, localB, localResult, nanValue);
        code.returnValue(localResult);
        return getMethod();
    }

    public void testLongCompare() throws Exception {
        /*
         * public static int call(long a, long b) {
         *   int result = a <=> b;
         *   return result;
         * }
         */
        MethodId<?, Integer> methodId = GENERATED.getMethod(Type.INT, "call", Type.LONG, Type.LONG);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Long> localA = code.getParameter(0, Type.LONG);
        Local<Long> localB = code.getParameter(1, Type.LONG);
        Local<Integer> localResult = code.newLocal(Type.INT);
        code.compare(localA, localB, localResult);
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

    private <T> Method arrayLengthMethod(Type<T> valueType) throws Exception {
        /*
         * public static int call(long[] array) {
         *   int result = array.length;
         *   return result;
         * }
         */
        reset();
        MethodId<?, Integer> methodId = GENERATED.getMethod(Type.INT, "call", valueType);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<T> localArray = code.getParameter(0, valueType);
        Local<Integer> localResult = code.newLocal(Type.INT);
        code.arrayLength(localArray, localResult);
        code.returnValue(localResult);
        return getMethod();
    }

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

    private <T> Method newArrayMethod(Type<T> valueType) throws Exception {
        /*
         * public static long[] call(int length) {
         *   long[] result = new long[length];
         *   return result;
         * }
         */
        reset();
        MethodId<?, T> methodId = GENERATED.getMethod(valueType, "call", Type.INT);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<Integer> localLength = code.getParameter(0, Type.INT);
        Local<T> localResult = code.newLocal(valueType);
        code.newArray(localLength, localResult);
        code.returnValue(localResult);
        return getMethod();
    }

    public void testReadAndWriteArray() throws Exception {
        Method swapBooleanArray = arraySwapMethod(BOOLEAN_ARRAY, Type.BOOLEAN);
        boolean[] booleans = new boolean[3];
        assertEquals(false, swapBooleanArray.invoke(null, booleans, 1, true));
        assertEquals("[false, true, false]", Arrays.toString(booleans));

        Method swapIntArray = arraySwapMethod(INT_ARRAY, Type.INT);
        int[] ints = new int[3];
        assertEquals(0, swapIntArray.invoke(null, ints, 1, 5));
        assertEquals("[0, 5, 0]", Arrays.toString(ints));

        Method swapLongArray = arraySwapMethod(LONG_ARRAY, Type.LONG);
        long[] longs = new long[3];
        assertEquals(0L, swapLongArray.invoke(null, longs, 1, 6L));
        assertEquals("[0, 6, 0]", Arrays.toString(longs));

        Method swapObjectArray = arraySwapMethod(OBJECT_ARRAY, Type.OBJECT);
        Object[] objects = new Object[3];
        assertEquals(null, swapObjectArray.invoke(null, objects, 1, "X"));
        assertEquals("[null, X, null]", Arrays.toString(objects));

        Method swapLong2dArray = arraySwapMethod(LONG_2D_ARRAY, LONG_ARRAY);
        long[][] longs2d = new long[3][];
        assertEquals(null, swapLong2dArray.invoke(null, longs2d, 1, new long[] { 7 }));
        assertEquals("[null, [7], null]", Arrays.deepToString(longs2d));
    }

    private <A, T> Method arraySwapMethod(Type<A> arrayType, Type<T> singleType)
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
                singleType, "call", arrayType, Type.INT, singleType);
        Code code = generator.declare(methodId, PUBLIC | STATIC);
        Local<A> localArray = code.getParameter(0, arrayType);
        Local<Integer> localIndex = code.getParameter(1, Type.INT);
        Local<T> localNewValue = code.getParameter(2, singleType);
        Local<T> localResult = code.newLocal(singleType);
        code.aget(localArray, localIndex, localResult);
        code.aput(localArray, localIndex, localNewValue);
        code.returnValue(localResult);
        return getMethod();
    }

    // TODO: fail if a label is unreachable (never navigated to)

    // TODO: more strict type parameters: Integer on methods

    // TODO: don't generate multiple times (?)

    private void addDefaultConstructor() {
        Code code = generator.declareConstructor(GENERATED.getConstructor(), PUBLIC);
        Local<?> thisRef = code.getThis(GENERATED);
        code.invokeDirect(Type.OBJECT.getConstructor(), null, thisRef);
        code.returnVoid();
    }

    /**
     * Returns the generated method.
     */
    private Method getMethod() throws Exception {
        Class<?> generated = loadAndGenerate();
        for (Method method : generated.getMethods()) {
            if (method.getName().equals("call")) {
                return method;
            }
        }
        throw new IllegalStateException("no call() method");
    }

    public static File getDataDirectory() throws Exception {
        Class<?> environmentClass = Class.forName("android.os.Environment");
        Method method = environmentClass.getMethod("getDataDirectory");
        Object dataDirectory = method.invoke(null);
        return (File) dataDirectory;
    }

    private Class<?> loadAndGenerate() throws Exception {
        return generator.load(getClass().getClassLoader(),
                getDataDirectory(), getDataDirectory()).loadClass("Generated");
    }
}
