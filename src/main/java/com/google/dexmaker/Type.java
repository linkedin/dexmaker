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

import com.android.dx.rop.cst.CstType;
import java.util.HashMap;
import java.util.Map;

/**
 * A primitive type, interface or class.
 */
public final class Type<T> {
    /** The {@code boolean} primitive type. */
    public static final Type<Boolean> BOOLEAN
            = new Type<Boolean>(com.android.dx.rop.type.Type.BOOLEAN);

    /** The {@code byte} primitive type. */
    public static final Type<Byte> BYTE = new Type<Byte>(com.android.dx.rop.type.Type.BYTE);

    /** The {@code char} primitive type. */
    public static final Type<Character> CHAR
            = new Type<Character>(com.android.dx.rop.type.Type.CHAR);

    /** The {@code double} primitive type. */
    public static final Type<Double> DOUBLE = new Type<Double>(com.android.dx.rop.type.Type.DOUBLE);

    /** The {@code float} primitive type. */
    public static final Type<Float> FLOAT = new Type<Float>(com.android.dx.rop.type.Type.FLOAT);

    /** The {@code int} primitive type. */
    public static final Type<Integer> INT = new Type<Integer>(com.android.dx.rop.type.Type.INT);

    /** The {@code long} primitive type. */
    public static final Type<Long> LONG = new Type<Long>(com.android.dx.rop.type.Type.LONG);

    /** The {@code short} primitive type. */
    public static final Type<Short> SHORT = new Type<Short>(com.android.dx.rop.type.Type.SHORT);

    /** The {@code void} primitive type. Only used as a return type. */
    public static final Type<Void> VOID = new Type<Void>(com.android.dx.rop.type.Type.VOID);

    /** The {@code Object} type. */
    public static final Type<Object> OBJECT = new Type<Object>(com.android.dx.rop.type.Type.OBJECT);

    /** The {@code String} type. */
    public static final Type<String> STRING = new Type<String>(com.android.dx.rop.type.Type.STRING);

    private static final Map<Class<?>, Type<?>> PRIMITIVE_TO_TYPE
            = new HashMap<Class<?>, Type<?>>();
    static {
        PRIMITIVE_TO_TYPE.put(boolean.class, BOOLEAN);
        PRIMITIVE_TO_TYPE.put(byte.class, BYTE);
        PRIMITIVE_TO_TYPE.put(char.class, CHAR);
        PRIMITIVE_TO_TYPE.put(double.class, DOUBLE);
        PRIMITIVE_TO_TYPE.put(float.class, FLOAT);
        PRIMITIVE_TO_TYPE.put(int.class, INT);
        PRIMITIVE_TO_TYPE.put(long.class, LONG);
        PRIMITIVE_TO_TYPE.put(short.class, SHORT);
        PRIMITIVE_TO_TYPE.put(void.class, VOID);
    }

    final String name;

    /** cached converted values */
    final com.android.dx.rop.type.Type ropType;
    final CstType constant;

    Type(com.android.dx.rop.type.Type ropType) {
        this(ropType.getDescriptor(), ropType);
    }

    Type(String name, com.android.dx.rop.type.Type ropType) {
        if (name == null || ropType == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.ropType = ropType;
        this.constant = CstType.intern(ropType);
    }

    /**
     * @param name a descriptor like "Ljava/lang/Class;".
     */
    public static <T> Type<T> get(String name) {
        return new Type<T>(name, com.android.dx.rop.type.Type.internReturnType(name));
    }

    public static <T> Type<T> get(Class<T> type) {
        if (type.isPrimitive()) {
            @SuppressWarnings("unchecked") // guarded by equals
            Type<T> result = (Type<T>) PRIMITIVE_TO_TYPE.get(type);
            return result;
        }
        String name = type.getName().replace('.', '/');
        return get(type.isArray() ? name : 'L' + name + ';');
    }

    public <V> FieldId<T, V> getField(Type<V> type, String name) {
        return new FieldId<T, V>(this, type, name);
    }

    public MethodId<T, Void> getConstructor(Type<?>... parameters) {
        return new MethodId<T, Void>(this, VOID, "<init>", new TypeList(parameters));
    }

    public <R> MethodId<T, R> getMethod(Type<R> returnType, String name, Type<?>... parameters) {
        return new MethodId<T, R>(this, returnType, name, new TypeList(parameters));
    }

    public String getName() {
        return name;
    }

    @Override public boolean equals(Object o) {
        return o instanceof Type
                && ((Type) o).name.equals(name);
    }

    @Override public int hashCode() {
        return name.hashCode();
    }

    @Override public String toString() {
        return name;
    }
}
