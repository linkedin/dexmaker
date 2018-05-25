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

import android.os.Build;
import android.support.test.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.android.dx.TypeId.*;
import static java.lang.reflect.Modifier.PUBLIC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public final class AnnotationIdTest {

    /**
     *  Method Annotation definition for test
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    @interface MethodAnnotation {
        boolean elementBoolean() default false;
        byte elementByte() default Byte.MIN_VALUE;
        char elementChar() default 'a';
        double elementDouble() default Double.MIN_NORMAL;
        float elementFloat() default Float.MIN_NORMAL;
        int elementInt() default Integer.MIN_VALUE;
        long elementLong() default Long.MIN_VALUE;
        short elementShort() default Short.MIN_VALUE;
        String elementString() default "foo";
        ElementEnum elementEnum() default ElementEnum.INSTANCE_0;
        Class<?> elementClass() default Object.class;
    }

    enum ElementEnum {
        INSTANCE_0,
        INSTANCE_1,
    }

    private DexMaker dexMaker;
    private static TypeId<?> GENERATED = TypeId.get("LGenerated;");
    private static final Map<TypeId<?>, Class<?>> TYPE_TO_PRIMITIVE = new HashMap<>();
    static {
        TYPE_TO_PRIMITIVE.put(BOOLEAN, boolean.class);
        TYPE_TO_PRIMITIVE.put(BYTE, byte.class);
        TYPE_TO_PRIMITIVE.put(CHAR, char.class);
        TYPE_TO_PRIMITIVE.put(DOUBLE, double.class);
        TYPE_TO_PRIMITIVE.put(FLOAT, float.class);
        TYPE_TO_PRIMITIVE.put(INT, int.class);
        TYPE_TO_PRIMITIVE.put(LONG, long.class);
        TYPE_TO_PRIMITIVE.put(SHORT, short.class);
        TYPE_TO_PRIMITIVE.put(VOID, void.class);
    }

    @Before
    public void setUp() {
        init();
    }

    /**
     *  Test adding a method annotation with new value of Boolean element.
     */
    @Test
    public void addMethodAnnotationWithBooleanElement() throws Exception {
        MethodId<?, Void> methodId = generateVoidMethod(TypeId.BOOLEAN);
        AnnotationId.Element element = new AnnotationId.Element("elementBoolean", true);
        addAnnotationToMethod(methodId, element);

        Annotation[] methodAnnotations = getMethodAnnotations(methodId);
        assertEquals(methodAnnotations.length, 1);

        Boolean elementBoolean = ((MethodAnnotation)methodAnnotations[0]).elementBoolean();
        assertEquals(true, elementBoolean);
    }

    /**
     *  Test adding a method annotation with new value of Byte element.
     */
    @Test
    public void addMethodAnnotationWithByteElement() throws Exception {
        MethodId<?, Void> methodId = generateVoidMethod(TypeId.BYTE);
        AnnotationId.Element element = new AnnotationId.Element("elementByte", Byte.MAX_VALUE);
        addAnnotationToMethod(methodId, element);

        Annotation[] methodAnnotations = getMethodAnnotations(methodId);
        assertEquals(methodAnnotations.length, 1);

        byte elementByte = ((MethodAnnotation)methodAnnotations[0]).elementByte();
        assertEquals(Byte.MAX_VALUE, elementByte);
    }

    /**
     *  Test adding a method annotation with new value of Char element.
     */
    @Test
    public void addMethodAnnotationWithCharElement() throws Exception {
        MethodId<?, Void> methodId = generateVoidMethod(TypeId.CHAR);
        AnnotationId.Element element = new AnnotationId.Element("elementChar", 'X');
        addAnnotationToMethod(methodId, element);

        Annotation[] methodAnnotations = getMethodAnnotations(methodId);
        assertEquals(methodAnnotations.length, 1);

        char elementChar = ((MethodAnnotation)methodAnnotations[0]).elementChar();
        assertEquals('X', elementChar);
    }

    /**
     *  Test adding a method annotation with new value of Double element.
     */
    @Test
    public void addMethodAnnotationWithDoubleElement() throws Exception {
        MethodId<?, Void> methodId = generateVoidMethod(TypeId.DOUBLE);
        AnnotationId.Element element = new AnnotationId.Element("elementDouble", Double.NaN);
        addAnnotationToMethod(methodId, element);

        Annotation[] methodAnnotations = getMethodAnnotations(methodId);
        assertEquals(methodAnnotations.length, 1);

        double elementDouble = ((MethodAnnotation)methodAnnotations[0]).elementDouble();
        assertEquals(Double.NaN, elementDouble, 0);
    }

    /**
     *  Test adding a method annotation with new value of Float element.
     */
    @Test
    public void addMethodAnnotationWithFloatElement() throws Exception {
        MethodId<?, Void> methodId = generateVoidMethod(TypeId.FLOAT);
        AnnotationId.Element element = new AnnotationId.Element("elementFloat", Float.NaN);
        addAnnotationToMethod(methodId, element);

        Annotation[] methodAnnotations = getMethodAnnotations(methodId);
        assertEquals(methodAnnotations.length, 1);

        float elementFloat = ((MethodAnnotation)methodAnnotations[0]).elementFloat();
        assertEquals(Float.NaN, elementFloat, 0);
    }

    /**
     *  Test adding a method annotation with new value of Int element.
     */
    @Test
    public void addMethodAnnotationWithIntElement() throws Exception {
        MethodId<?, Void> methodId = generateVoidMethod(TypeId.INT);
        AnnotationId.Element element = new AnnotationId.Element("elementInt", Integer.MAX_VALUE);
        addAnnotationToMethod(methodId, element);

        Annotation[] methodAnnotations = getMethodAnnotations(methodId);
        assertEquals(methodAnnotations.length, 1);

        int elementInt = ((MethodAnnotation)methodAnnotations[0]).elementInt();
        assertEquals(Integer.MAX_VALUE, elementInt);
    }

    /**
     *  Test adding a method annotation with new value of Long element.
     */
    @Test
    public void addMethodAnnotationWithLongElement() throws Exception {
        MethodId<?, Void> methodId = generateVoidMethod(TypeId.LONG);
        AnnotationId.Element element = new AnnotationId.Element("elementLong", Long.MAX_VALUE);
        addAnnotationToMethod(methodId, element);

        Annotation[] methodAnnotations = getMethodAnnotations(methodId);
        assertEquals(methodAnnotations.length, 1);

        long elementLong = ((MethodAnnotation)methodAnnotations[0]).elementLong();
        assertEquals(Long.MAX_VALUE, elementLong);
    }

    /**
     *  Test adding a method annotation with new value of Short element.
     */
    @Test
    public void addMethodAnnotationWithShortElement() throws Exception {
        MethodId<?, Void> methodId = generateVoidMethod(TypeId.SHORT);
        AnnotationId.Element element = new AnnotationId.Element("elementShort", Short.MAX_VALUE);
        addAnnotationToMethod(methodId, element);

        Annotation[] methodAnnotations = getMethodAnnotations(methodId);
        assertEquals(methodAnnotations.length, 1);

        short elementShort = ((MethodAnnotation)methodAnnotations[0]).elementShort();
        assertEquals(Short.MAX_VALUE, elementShort);
    }

    /**
     *  Test adding a method annotation with new value of String element.
     */
    @Test
    public void addMethodAnnotationWithStingElement() throws Exception {
        MethodId<?, Void> methodId = generateVoidMethod(TypeId.STRING);
        AnnotationId.Element element = new AnnotationId.Element("elementString", "hello");
        addAnnotationToMethod(methodId, element);

        Annotation[] methodAnnotations = getMethodAnnotations(methodId);
        assertEquals(methodAnnotations.length, 1);

        String elementString = ((MethodAnnotation)methodAnnotations[0]).elementString();
        assertEquals("hello", elementString);
    }

    /**
     *  Test adding a method annotation with new value of Enum element.
     */
    @Test
    public void addMethodAnnotationWithEnumElement() throws Exception {
        assumeTrue(Build.VERSION.SDK_INT >= 21);

        MethodId<?, Void> methodId = generateVoidMethod(TypeId.get(Enum.class));
        AnnotationId.Element element = new AnnotationId.Element("elementEnum", ElementEnum.INSTANCE_1);
        addAnnotationToMethod(methodId, element);

        Annotation[] methodAnnotations = getMethodAnnotations(methodId);
        assertEquals(methodAnnotations.length, 1);

        ElementEnum elementEnum = ((MethodAnnotation)methodAnnotations[0]).elementEnum();
        assertEquals(ElementEnum.INSTANCE_1, elementEnum);
    }

    /**
     *  Test adding a method annotation with new value of Class element.
     */
    @Test
    public void addMethodAnnotationWithClassElement() throws Exception {
        MethodId<?, Void> methodId = generateVoidMethod(TypeId.get(AnnotationId.class));
        AnnotationId.Element element = new AnnotationId.Element("elementClass", AnnotationId.class);
        addAnnotationToMethod(methodId, element);

        Annotation[] methodAnnotations = getMethodAnnotations(methodId);
        assertEquals(methodAnnotations.length, 1);

        Class<?> elementClass = ((MethodAnnotation)methodAnnotations[0]).elementClass();
        assertEquals(AnnotationId.class, elementClass);
    }

    /**
     *  Test adding a method annotation with new multiple values of an element.
     */
    @Test
    public void addMethodAnnotationWithMultiElements() throws Exception {
        assumeTrue(Build.VERSION.SDK_INT >= 21);

        MethodId<?, Void> methodId = generateVoidMethod();
        AnnotationId.Element element1 = new AnnotationId.Element("elementClass", AnnotationId.class);
        AnnotationId.Element element2 = new AnnotationId.Element("elementEnum", ElementEnum.INSTANCE_1);
        AnnotationId.Element[] elements = {element1, element2};
        addAnnotationToMethod(methodId, elements);

        Annotation[] methodAnnotations = getMethodAnnotations(methodId);
        assertEquals(methodAnnotations.length, 1);

        ElementEnum elementEnum = ((MethodAnnotation)methodAnnotations[0]).elementEnum();
        assertEquals(ElementEnum.INSTANCE_1, elementEnum);
        Class<?> elementClass = ((MethodAnnotation)methodAnnotations[0]).elementClass();
        assertEquals(AnnotationId.class, elementClass);
    }

    /**
     *  Test adding a method annotation with duplicate values of an element. The previous value will
     *  be replaced by latter one.
     */
    @Test
    public void addMethodAnnotationWithDuplicateElements() throws Exception {
        assumeTrue(Build.VERSION.SDK_INT >= 21);

        MethodId<?, Void> methodId = generateVoidMethod();
        AnnotationId.Element element1 = new AnnotationId.Element("elementEnum", ElementEnum.INSTANCE_1);
        AnnotationId.Element element2 = new AnnotationId.Element("elementEnum", ElementEnum.INSTANCE_0);
        addAnnotationToMethod(methodId, element1, element2);

        Annotation[] methodAnnotations = getMethodAnnotations(methodId);
        assertEquals(methodAnnotations.length, 1);

        ElementEnum elementEnum = ((MethodAnnotation)methodAnnotations[0]).elementEnum();
        assertEquals(ElementEnum.INSTANCE_0, elementEnum);
    }


    /**
     *  Test adding a method annotation with new array value of an element. It's not supported yet.
     */
    @Test
    public void addMethodAnnotationWithArrayElementValue() {
        try {
            MethodId<?, Void> methodId = generateVoidMethod();
            int[] a = {1, 2};
            AnnotationId.Element element = new AnnotationId.Element("elementInt", a);
            addAnnotationToMethod(methodId, element);
            fail();
        } catch (UnsupportedOperationException e) {
            System.out.println(e);
        }
    }

    /**
     *  Test adding a method annotation with new TypeId value of an element. It's not supported yet.
     */
    @Test
    public void addMethodAnnotationWithTypeIdElementValue() {
        try {
            MethodId<?, Void> methodId = generateVoidMethod();
            AnnotationId.Element element = new AnnotationId.Element("elementInt", INT);
            addAnnotationToMethod(methodId, element);
            fail();
        } catch (UnsupportedOperationException e) {
            System.out.println(e);
        }
    }

    @After
    public void tearDown() {
    }

    /**
     *  Internal methods
     */
    private void init() {
        clearDataDirectory();

        dexMaker = new DexMaker();
        dexMaker.declare(GENERATED, "Generated.java", PUBLIC, TypeId.OBJECT);
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
        MethodId<?, Void> methodId = GENERATED.getMethod(VOID, "call", parameters);
        Code code = dexMaker.declare(methodId, PUBLIC);
        code.returnVoid();
        return methodId;
    }

    private void addAnnotationToMethod(MethodId<?, Void> methodId, AnnotationId.Element... elements) {
        TypeId<MethodAnnotation> annotationTypeId = TypeId.get(MethodAnnotation.class);
        AnnotationId<?, MethodAnnotation> annotationId = AnnotationId.get(GENERATED, annotationTypeId, ElementType.METHOD);
        for (AnnotationId.Element element : elements) {
            annotationId.set(element);
        }
        annotationId.addToMethod(dexMaker, methodId);
    }

    private Annotation[] getMethodAnnotations(MethodId<?, Void> methodId) throws Exception {
        Class<?> generatedClass = generateAndLoad();
        Class<?>[] parameters = getMethodParameters(methodId);
        Method method = generatedClass.getMethod(methodId.getName(), parameters);
        return method.getAnnotations();
    }

    private Class<?>[] getMethodParameters(MethodId<?, Void> methodId) throws ClassNotFoundException {
        List<TypeId<?>> paras = methodId.getParameters();
        Class<?>[] p = null;
        if (paras.size() > 0) {
            p = new Class<?>[paras.size()];
            for (int i = 0; i < paras.size(); i++) {
                p[i] = TYPE_TO_PRIMITIVE.get(paras.get(i));
                if (p[i] == null) {
                    String name = paras.get(i).getName().replace('/', '.');
                    if (name.charAt(0) == 'L') {
                        name = name.substring(1, name.length()-1);
                    }
                    p[i] = Class.forName(name);
                }
            }
        }
        return p;
    }

    private Class<?> generateAndLoad() throws Exception {
        return dexMaker.generateAndLoad(getClass().getClassLoader(), getDataDirectory())
                .loadClass("Generated");
    }
}
