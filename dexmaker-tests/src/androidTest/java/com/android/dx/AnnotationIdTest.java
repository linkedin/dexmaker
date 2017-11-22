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
package com.android.dx;

import android.support.test.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.annotation.*;
import java.lang.reflect.Method;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class AnnotationIdTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    @interface MethodAnnotation {
        ElementEnum elementEnum() default ElementEnum.INSTANCE_0;
        boolean elementBoolean() default false;
        byte elementByte() default Byte.MIN_VALUE;
        char elementChar() default 'a';
        double elementDouble() default Double.MIN_NORMAL;
        float elementFloat() default Float.MIN_NORMAL;
        int elementInt() default Integer.MIN_VALUE;
        long elementLong() default Long.MIN_VALUE;
        short elementShort() default Short.MIN_VALUE;
        String elementString() default "foo";
        Class<?> elementClass() default Object.class;
    }

    enum ElementEnum {
        INSTANCE_0,
        INSTANCE_1,
    }

    private DexMaker dexMaker;
    private static TypeId<?> GENERATED = TypeId.get("LGenerated;");

    @Before
    public void setUp() throws Exception {
        reset();
    }

    @Test
    public void testAddToMethod() throws Exception {
        MethodId<?, Void> methodId = generateVoidMethod();

        TypeId<MethodAnnotation> annotationTypeId = TypeId.get(MethodAnnotation.class);
        AnnotationId<?, MethodAnnotation> annotationId = AnnotationId.get(GENERATED, annotationTypeId, ElementType.METHOD);

        // Test ElementEnum
        AnnotationId.Element element = new AnnotationId.Element(TypeId.get(ElementEnum.class),
                "elementEnum", ElementEnum.INSTANCE_1);
        annotationId.addElement(element);
        annotationId.addToMethod(dexMaker, methodId);
        Class<?> generatedClass = generateAndLoad();
        Method method = generatedClass.getMethod("call");
        Annotation[] methodAnnotations = method.getAnnotations();
        assertEquals(methodAnnotations.length, 1);
        ElementEnum elementEnum = ((MethodAnnotation)methodAnnotations[0]).elementEnum();
        assertEquals(ElementEnum.INSTANCE_1, elementEnum);


        // Test boolean
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        methodId = generateVoidMethod(TypeId.BOOLEAN);
        element = new AnnotationId.Element(TypeId.BOOLEAN, "elementBoolean", true);
        annotationId.addElement(element);
        annotationId.addToMethod(dexMaker, methodId);
        generatedClass = generateAndLoad();
        method = generatedClass.getMethod("call", boolean.class);
        methodAnnotations = method.getAnnotations();
        assertEquals(methodAnnotations.length, 1);
        Boolean elementBoolean = ((MethodAnnotation)methodAnnotations[0]).elementBoolean();
        assertEquals(true, elementBoolean);

        // Test byte
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        methodId = generateVoidMethod(TypeId.BYTE);
        element = new AnnotationId.Element(TypeId.BYTE, "elementByte", Byte.MAX_VALUE);
        annotationId.addElement(element);
        annotationId.addToMethod(dexMaker, methodId);
        generatedClass = generateAndLoad();
        method = generatedClass.getMethod("call", byte.class);
        methodAnnotations = method.getAnnotations();
        assertEquals(methodAnnotations.length, 1);
        byte elementByte = ((MethodAnnotation)methodAnnotations[0]).elementByte();
        assertEquals(Byte.MAX_VALUE, elementByte);

        // Test char
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        methodId = generateVoidMethod(TypeId.CHAR);
        element = new AnnotationId.Element(TypeId.CHAR, "elementChar", 'X');
        annotationId.addElement(element);
        annotationId.addToMethod(dexMaker, methodId);
        generatedClass = generateAndLoad();
        method = generatedClass.getMethod("call", char.class);
        methodAnnotations = method.getAnnotations();
        assertEquals(methodAnnotations.length, 1);
        char elementChar = ((MethodAnnotation)methodAnnotations[0]).elementChar();
        assertEquals('X', elementChar);

        // Test double
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        methodId = generateVoidMethod(TypeId.DOUBLE);
        element = new AnnotationId.Element(TypeId.DOUBLE, "elementDouble", Double.NaN);
        annotationId.addElement(element);
        annotationId.addToMethod(dexMaker, methodId);
        generatedClass = generateAndLoad();
        method = generatedClass.getMethod("call", double.class);
        methodAnnotations = method.getAnnotations();
        assertEquals(methodAnnotations.length, 1);
        double elementDouble = ((MethodAnnotation)methodAnnotations[0]).elementDouble();
        assertEquals(Double.NaN, elementDouble, 0);

        // Test float
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        methodId = generateVoidMethod(TypeId.FLOAT);
        element = new AnnotationId.Element(TypeId.FLOAT, "elementFloat", Float.NaN);
        annotationId.addElement(element);
        annotationId.addToMethod(dexMaker, methodId);
        generatedClass = generateAndLoad();
        method = generatedClass.getMethod("call", float.class);
        methodAnnotations = method.getAnnotations();
        assertEquals(methodAnnotations.length, 1);
        float elementFloat = ((MethodAnnotation)methodAnnotations[0]).elementFloat();
        assertEquals(Float.NaN, elementFloat, 0);

        // Test int
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        methodId = generateVoidMethod(TypeId.INT);
        element = new AnnotationId.Element(TypeId.INT, "elementInt", Integer.MAX_VALUE);
        annotationId.addElement(element);
        annotationId.addToMethod(dexMaker, methodId);
        generatedClass = generateAndLoad();
        method = generatedClass.getMethod("call", int.class);
        methodAnnotations = method.getAnnotations();
        assertEquals(methodAnnotations.length, 1);
        int elementInt = ((MethodAnnotation)methodAnnotations[0]).elementInt();
        assertEquals(Integer.MAX_VALUE, elementInt);

        // Test long
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        methodId = generateVoidMethod(TypeId.LONG);
        element = new AnnotationId.Element(TypeId.LONG, "elementLong", Long.MAX_VALUE);
        annotationId.addElement(element);
        annotationId.addToMethod(dexMaker, methodId);
        generatedClass = generateAndLoad();
        method = generatedClass.getMethod("call", long.class);
        methodAnnotations = method.getAnnotations();
        assertEquals(methodAnnotations.length, 1);
        long elementLong = ((MethodAnnotation)methodAnnotations[0]).elementLong();
        assertEquals(Long.MAX_VALUE, elementLong);

        // Test short
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        methodId = generateVoidMethod(TypeId.SHORT);
        element = new AnnotationId.Element(TypeId.SHORT, "elementShort", Short.MAX_VALUE);
        annotationId.addElement(element);
        annotationId.addToMethod(dexMaker, methodId);
        generatedClass = generateAndLoad();
        method = generatedClass.getMethod("call", short.class);
        methodAnnotations = method.getAnnotations();
        assertEquals(methodAnnotations.length, 1);
        short elementShort = ((MethodAnnotation)methodAnnotations[0]).elementShort();
        assertEquals(Short.MAX_VALUE, elementShort);

        // Test String
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        methodId = generateVoidMethod(TypeId.STRING);
        element = new AnnotationId.Element(TypeId.STRING, "elementString", "hello");
        annotationId.addElement(element);
        annotationId.addToMethod(dexMaker, methodId);
        generatedClass = generateAndLoad();
        method = generatedClass.getMethod("call", String.class);
        methodAnnotations = method.getAnnotations();
        assertEquals(methodAnnotations.length, 1);
        String elementString = ((MethodAnnotation)methodAnnotations[0]).elementString();
        assertEquals("hello", elementString);

        // Test class
        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
        methodId = generateVoidMethod(TypeId.get(AnnotationId.class));
        element = new AnnotationId.Element(TypeId.get(Class.class), "elementClass", AnnotationId.class);
        annotationId.addElement(element);
        annotationId.addToMethod(dexMaker, methodId);
        generatedClass = generateAndLoad();
        method = generatedClass.getMethod("call", AnnotationId.class);
        methodAnnotations = method.getAnnotations();
        assertEquals(methodAnnotations.length, 1);
        Class<?> elementClass = ((MethodAnnotation)methodAnnotations[0]).elementClass();
        assertEquals(AnnotationId.class, elementClass);


        // Test unsupported array
        try {
            int[] a = new int[2];
            element = new AnnotationId.Element(TypeId.INT, "elementInt", a);
            annotationId.addElement(element);
            fail();
        } catch (UnsupportedOperationException e) {
            System.out.println(e);
        }

        // Test unsupported TypeId
        try {
            TypeId<?> t = TypeId.INT;
            element = new AnnotationId.Element(TypeId.INT, "elementInt", t);
            annotationId.addElement(element);
            fail();
        } catch (UnsupportedOperationException e) {
            System.out.println(e);
        }

    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     *  Internal methods
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

    private static File getDataDirectory() {
        String dataDir = InstrumentationRegistry.getTargetContext().getApplicationInfo().dataDir;
        return new File(dataDir + "/cache" );
    }

    private MethodId<?, Void> generateVoidMethod(TypeId<?>... parameters) {
        MethodId<?, Void> methodId = GENERATED.getMethod(TypeId.VOID, "call", parameters);
        Code code = dexMaker.declare(methodId, PUBLIC);
        code.returnVoid();
        return methodId;
    }

    private Class<?> generateAndLoad() throws Exception {
        return dexMaker.generateAndLoad(getClass().getClassLoader(), getDataDirectory())
                .loadClass("Generated");
    }
}
