package com.android.dx.mockito;

import java.lang.reflect.Method;

public abstract class ObjectMethodsGuru2 {
	
    public static boolean isEqualsMethod(Method method) {
        return method.getName().equals("equals")
                && method.getParameterTypes().length == 1
                && method.getParameterTypes()[0] == Object.class;
	}
	
    public static boolean isHashCodeMethod(Method method) {
        return method.getName().equals("hashCode")
                && method.getParameterTypes().length == 0;
    }
	
}
