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

import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.rop.annotation.Annotation;
import com.android.dx.rop.annotation.AnnotationVisibility;
import com.android.dx.rop.annotation.Annotations;
import com.android.dx.rop.annotation.NameValuePair;
import com.android.dx.rop.cst.*;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.List;

/**
 * Identifies an annotation for a method.
 *
 * @param <D> the type declaring the method that this annotation is added to
 * @param <V> the type of value this annotation holds
 */
public final class AnnotationId<D, V> {
    private final TypeId<D> declaringType;
    private final TypeId<V> type;
    /** The type of this annotation which is for method, field or class */
    private final ElementType annotationType;
    /** The elements this annotation holds */
    private final List<NameValuePair> elements;

    private AnnotationId(TypeId<D> declaringType, TypeId<V> type, ElementType annotationType) {
        this.declaringType = declaringType;
        this.type = type;
        this.annotationType = annotationType;
        this.elements = new ArrayList<>();
    }

    /**
     *  Generate an annotation.
     *
     * @param declaringType the type declaring the method that this annotation is added to.
     * @param type the type of value this annotation holds.
     * @param annotationType The type of this annotation which is for method, field or class.
     * @return an annotation {@code AnnotationId<D,V>}
     */
    public static <D, V> AnnotationId<D, V> get(TypeId<D> declaringType, TypeId<V> type,
                                                ElementType annotationType) {
        return new AnnotationId<>(declaringType, type, annotationType);
    }

    /**
     * Add an element to this annotation.
     *
     * @param element annotation element to be added.
     */
    public void addElement(Element element) {
        CstString pairName = new CstString(element.getName());
        Constant pairValue = Element.toConstant(element.getValue());
        NameValuePair nameValuePair = new NameValuePair(pairName, pairValue);
        elements.add(nameValuePair);
    }

    /**
     * Add this annotation to a method.
     *
     * @param dexMaker DexMaker instance.
     * @param method Method to be added to.
     */
    public void addToMethod(DexMaker dexMaker, MethodId<?, ?> method) {
        if (annotationType != ElementType.METHOD) {
            throw new IllegalStateException("This annotation is not for method");
        }

        if (method.declaringType != declaringType) {
            throw new IllegalArgumentException("Method" + method + "'s declaring type is inconsistent with" + this);
        }

        ClassDefItem classDefItem = dexMaker.getTypeDeclaration(declaringType).toClassDefItem();

        if (classDefItem == null) {
            throw new NullPointerException("No class defined item is found");
        } else {
            CstMethodRef cstMethodRef = method.constant;

            if (cstMethodRef == null) {
                throw new NullPointerException("Method reference is NULL");
            } else {
                // Generate CstType
                CstType cstType = CstType.intern(type.ropType);

                // Generate Annotation
                Annotation annotation = new Annotation(cstType, AnnotationVisibility.RUNTIME);

                // Add generated annotation
                Annotations annotations = new Annotations();
                for (NameValuePair nvp : elements) {
                    annotation.add(nvp);
                }
                annotations.add(annotation);
                classDefItem.addMethodAnnotations(cstMethodRef, annotations, dexMaker.getDexFile());
            }
        }
    }

    /**
     *  Annotation Element
     */
    public static final class Element {
        final TypeId type;
        final String name;
        final Object value;

        public Element(TypeId type, String name, Object value) {
            this.type = type;
            this.name = name;
            this.value = value;
        }

        public TypeId getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }

        /**
         *  Convert a value of an element to a {@code Constant}.
         *  <p><strong>Warning:</strong> Array or TypeId value is not supported yet.
         *
         * @param value an element value.
         * @return a Constant
         */
        static Constant toConstant(Object value) {
            Class clazz = value.getClass();
            if (clazz.isEnum()) {
                CstString descriptor = new CstString(TypeId.get(clazz).getName());
                CstString name = new CstString(((Enum)value).name());
                CstNat cstNat = new CstNat(name, descriptor);
                return new CstEnumRef(cstNat);
            } else if (clazz.isArray()) {
                throw new UnsupportedOperationException("Array is not supported yet");
            } else if (value instanceof TypeId) {
                throw new UnsupportedOperationException("TypeId is not supported yet");
            } else {
                return  Constants.getConstant(value);
            }
        }
    }
}
