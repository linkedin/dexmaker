/*
 * Copyright (c) 2018 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package com.android.dx.mockito.inline;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backend for the method entry hooks. Checks if the hooks should cause an interception or should
 * be ignored.
 */
class StaticMockMethodAdvice {
    /**
     * Pattern to decompose a instrumentedMethodWithTypeAndSignature
     */
    private final static Pattern methodPattern = Pattern.compile("(.*)#(.*)\\((.*)\\)");
    private final Map<Object, InvocationHandlerAdapter> markersToHandler;
    private final Map<Class, Object> classToMarker;
    @SuppressWarnings("ThreadLocalUsage")
    private final SelfCallInfo selfCallInfo = new SelfCallInfo();

    StaticMockMethodAdvice(Map<Object, InvocationHandlerAdapter> markerToHandler, Map<Class, Object>
            classToMarker) {
        this.markersToHandler = markerToHandler;
        this.classToMarker = classToMarker;
    }

    /**
     * Try to invoke the method {@code origin}.
     *
     * @param origin    method to invoke
     * @param arguments arguments to the method
     * @return result of the method
     * @throws Throwable Exception if thrown by the method
     */
    private static Object tryInvoke(Method origin, Object[] arguments)
            throws Throwable {
        try {
            return origin.invoke(null, arguments);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static Class<?> classForTypeName(String name) throws ClassNotFoundException {
        if (name.endsWith("[]")) {
            return Class.forName("[L" + name.substring(0, name.length() - 2) + ";");
        } else {
            return Class.forName(name);
        }
    }

    private static Class nameToType(String name) throws ClassNotFoundException {
        switch (name) {
            case "byte":
                return Byte.TYPE;
            case "short":
                return Short.TYPE;
            case "int":
                return Integer.TYPE;
            case "long":
                return Long.TYPE;
            case "char":
                return Character.TYPE;
            case "float":
                return Float.TYPE;
            case "double":
                return Double.TYPE;
            case "boolean":
                return Boolean.TYPE;
            case "byte[]":
                return byte[].class;
            case "short[]":
                return short[].class;
            case "int[]":
                return int[].class;
            case "long[]":
                return long[].class;
            case "char[]":
                return char[].class;
            case "float[]":
                return float[].class;
            case "double[]":
                return double[].class;
            case "boolean[]":
                return boolean[].class;
            default:
                return classForTypeName(name);
        }
    }

    /**
     * Would a call to SubClass.method handled by SuperClass.method ?
     * <p>This is the case when subclass or any intermediate parent does not override method.
     *
     * @param subclass         Class that might have been called
     * @param superClass       Class defining the method
     * @param methodName       Name of method
     * @param methodParameters Parameter of method
     * @return {code true} iff the method would have be handled by superClass
     */
    private static boolean isMethodDefinedBySuperClass(Class<?> subclass, Class<?> superClass,
                                                       String methodName,
                                                       Class<?>[] methodParameters) {
        do {
            if (subclass == superClass) {
                // The method is not overridden in the subclass or any class in between subClass
                // and superClass.
                return true;
            }

            try {
                subclass.getDeclaredMethod(methodName, methodParameters);

                // method is overridden is sub-class. hence the call could not have handled by
                // the super-class.
                return false;
            } catch (NoSuchMethodException e) {
                subclass = subclass.getSuperclass();
            }
        } while (subclass != null);

        // Subclass is not a sub class of superClass
        return false;
    }

    private static List<Class<?>> getAllSubclasses(Class<?> superClass, Collection<Class>
            possibleSubClasses) {
        ArrayList<Class<?>> subclasses = new ArrayList<>();
        for (Class<?> possibleSubClass : possibleSubClasses) {
            if (superClass.isAssignableFrom(possibleSubClass)) {
                subclasses.add(possibleSubClass);
            }
        }

        return subclasses;
    }

    private synchronized static native String nativeGetCalledClassName();

    private Class<?> getClassMethodWasCalledOn(MethodDesc methodDesc) throws ClassNotFoundException,
            NoSuchMethodException {
        Class<?> classDeclaringMethod = classForTypeName(methodDesc.className);

        /* If a sub-class does not override a static method, the super-classes method is called
         * directly. Hence 'classDeclaringMethod' will be the super class. As the mocking of
         * this and the class actually called might be different we need to find the class that
         * was actually called.
         */
        if (Modifier.isFinal(classDeclaringMethod.getModifiers())
                || Modifier.isFinal(classDeclaringMethod.getDeclaredMethod(methodDesc.methodName,
                methodDesc.methodParamTypes).getModifiers())) {
            return classDeclaringMethod;
        } else {
            boolean mightBeMocked = false;
            // if neither the defining class nor any subclass of it is mocked, no point of
            // trying to figure out the called class as isMocked will soon be checked.
            for (Class<?> subClass : getAllSubclasses(classDeclaringMethod, classToMarker.keySet())) {
                if (isMethodDefinedBySuperClass(subClass, classDeclaringMethod,
                        methodDesc.methodName, methodDesc.methodParamTypes)) {
                    mightBeMocked = true;
                    break;
                }
            }

            if (!mightBeMocked) {
                return null;
            }

            String calledClassName = nativeGetCalledClassName();
            return Class.forName(calledClassName);
        }
    }

    /**
     * Get the method specified by {@code methodWithTypeAndSignature}.
     *
     * @param ignored
     * @param methodWithTypeAndSignature the description of the method
     * @return method {@code methodWithTypeAndSignature} refer to
     */
    @SuppressWarnings("unused")
    public Method getOrigin(Object ignored, String methodWithTypeAndSignature) throws Throwable {
        MethodDesc methodDesc = new MethodDesc(methodWithTypeAndSignature);

        Class clazz = getClassMethodWasCalledOn(methodDesc);
        if (clazz == null) {
            return null;
        }

        Object marker = classToMarker.get(clazz);
        if (!isMocked(marker)) {
            return null;
        }

        return Class.forName(methodDesc.className).getDeclaredMethod(methodDesc.methodName,
                methodDesc.methodParamTypes);
    }

    /**
     * Handle a method entry hook.
     *
     * @param origin    method that contains the hook
     * @param arguments arguments to the method
     * @return A callable that can be called to get the mocked result or null if the method is not
     * mocked.
     */
    @SuppressWarnings("unused")
    public Callable<?> handle(Object methodDescStr, Method origin, Object[] arguments) throws
            Throwable {
        MethodDesc methodDesc = new MethodDesc((String) methodDescStr);
        Class clazz = getClassMethodWasCalledOn(methodDesc);

        Object marker = classToMarker.get(clazz);
        InvocationHandlerAdapter interceptor = markersToHandler.get(marker);
        if (interceptor == null) {
            return null;
        }

        return new ReturnValueWrapper(interceptor.interceptEntryHook(marker, origin, arguments,
                new SuperMethodCall(selfCallInfo, origin, marker, arguments)));
    }

    /**
     * Checks if an {@code marker} is a mock marker.
     *
     * @return {@code true} iff the marker is a mock marker
     */
    public boolean isMarker(Object marker) {
        return markersToHandler.containsKey(marker);
    }

    /**
     * Check if this method call should be mocked. Usually the same as {@link #isMarker(Object)} but
     * takes into account the state of {@link #selfCallInfo} that allows to temporary disable
     * mocking for a single method call.
     */
    public boolean isMocked(Object marker) {
        return selfCallInfo.shouldMockMethod(marker) && isMarker(marker);
    }

    private static class MethodDesc {
        final String className;
        final String methodName;
        final Class<?>[] methodParamTypes;

        private MethodDesc(String methodWithTypeAndSignature) throws ClassNotFoundException {
            Matcher methodComponents = methodPattern.matcher(methodWithTypeAndSignature);
            boolean wasFound = methodComponents.find();
            if (!wasFound) {
                throw new IllegalArgumentException();
            }

            className = methodComponents.group(1);
            methodName = methodComponents.group(2);
            String methodParamTypeNames[] = methodComponents.group(3).split(",");

            ArrayList<Class<?>> methodParamTypesList = new ArrayList<>(methodParamTypeNames.length);
            for (String methodParamName : methodParamTypeNames) {
                if (!methodParamName.equals("")) {
                    methodParamTypesList.add(nameToType(methodParamName));
                }
            }
            methodParamTypes = methodParamTypesList.toArray(new Class<?>[]{});
        }

        @Override
        public String toString() {
            return className + "#" + methodName;
        }
    }

    /**
     * Used to call the real (non mocked) method.
     */
    private static class SuperMethodCall implements InvocationHandlerAdapter.SuperMethod {
        private final SelfCallInfo selfCallInfo;
        private final Method origin;
        private final Object marker;
        private final Object[] arguments;

        private SuperMethodCall(SelfCallInfo selfCallInfo, Method origin, Object marker,
                                Object[] arguments) {
            this.selfCallInfo = selfCallInfo;
            this.origin = origin;
            this.marker = marker;
            this.arguments = arguments;
        }

        /**
         * Call the read (non mocked) method.
         *
         * @return Result of read method
         * @throws Throwable thrown by the read method
         */
        @Override
        public Object invoke() throws Throwable {
            if (!Modifier.isPublic(origin.getDeclaringClass().getModifiers()
                    & origin.getModifiers())) {
                origin.setAccessible(true);
            }

            // By setting instance in the the selfCallInfo, once single method call on this instance
            // and thread will call the read method as isMocked will return false.
            selfCallInfo.set(marker);
            return tryInvoke(origin, arguments);
        }

    }

    /**
     * Stores a return value of {@link #handle(Object, Method, Object[])} and returns in on
     * {@link #call()}.
     */
    private static class ReturnValueWrapper implements Callable<Object> {
        private final Object returned;

        private ReturnValueWrapper(Object returned) {
            this.returned = returned;
        }

        @Override
        public Object call() {
            return returned;
        }
    }

    /**
     * Used to call the original method. If a instance is {@link #set(Object)}
     * {@link #shouldMockMethod(Object)} returns false for this instance once.
     * <p>This is {@link ThreadLocal}, so a thread can {@link #set(Object)} and instance and then
     * call {@link #shouldMockMethod(Object)} without interference.
     *
     * @see SuperMethodCall#invoke()
     * @see #isMocked(Object)
     */
    private static class SelfCallInfo extends ThreadLocal<Object> {
        boolean shouldMockMethod(Object value) {
            Object current = get();

            if (current == value) {
                set(null);
                return false;
            } else {
                return true;
            }
        }
    }
}
