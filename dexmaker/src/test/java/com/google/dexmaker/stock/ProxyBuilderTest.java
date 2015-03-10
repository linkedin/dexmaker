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

package com.google.dexmaker.stock;

import com.google.dexmaker.DexMakerTest;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class ProxyBuilderTest extends TestCase {
    private FakeInvocationHandler fakeHandler = new FakeInvocationHandler();
    private File versionedDxDir = new File(DexMakerTest.getDataDirectory(), "v1");

    public void setUp() throws Exception {
        super.setUp();
        versionedDxDir.mkdirs();
        clearVersionedDxDir();
    }

    public void tearDown() throws Exception {
        clearVersionedDxDir();
        super.tearDown();
    }

    private void clearVersionedDxDir() {
        for (File f : versionedDxDir.listFiles()) {
            f.delete();
        }
    }

    public static class SimpleClass {
        public String simpleMethod() {
            throw new AssertionFailedError();
        }
    }

    public void testExampleOperation() throws Throwable {
        fakeHandler.setFakeResult("expected");
        SimpleClass proxy = proxyFor(SimpleClass.class).build();
        assertEquals("expected", proxy.simpleMethod());
        assertEquals(2, versionedDxDir.listFiles().length);
    }

    public static class ConstructorTakesArguments {
        private final String argument;

        public ConstructorTakesArguments(String arg) {
            argument = arg;
        }

        public String method() {
            throw new AssertionFailedError();
        }
    }

    public void testConstruction_SucceedsIfCorrectArgumentsProvided() throws Throwable {
        ConstructorTakesArguments proxy = proxyFor(ConstructorTakesArguments.class)
                .constructorArgTypes(String.class)
                .constructorArgValues("hello")
                .build();
        assertEquals("hello", proxy.argument);
        proxy.method();
    }

    public void testConstruction_FailsWithWrongNumberOfArguments() throws Throwable {
        try {
            proxyFor(ConstructorTakesArguments.class).build();
            fail();
        } catch (IllegalArgumentException expected) {}
    }

    public void testClassIsNotAccessbile_FailsWithUnsupportedOperationException() throws Exception {
        class MethodVisibilityClass {
        }
        try {
            proxyFor(MethodVisibilityClass.class).build();
            fail();
        } catch (UnsupportedOperationException expected) {}
    }

    private static class PrivateVisibilityClass {
    }

    public void testPrivateClass_FailsWithUnsupportedOperationException() throws Exception {
        try {
            proxyFor(PrivateVisibilityClass.class).build();
            fail();
        } catch (UnsupportedOperationException expected) {}
    }

    protected static class ProtectedVisibilityClass {
        public String foo() {
            throw new AssertionFailedError();
        }
    }

    public void testProtectedVisibility_WorksFine() throws Exception {
        assertEquals("fake result", proxyFor(ProtectedVisibilityClass.class).build().foo());
    }

    public static class HasFinalMethod {
        public String nonFinalMethod() {
            return "non-final method";
        }

        public final String finalMethod() {
            return "final method";
        }
    }

    public void testCanProxyClassesWithFinalMethods_WillNotCallTheFinalMethod() throws Throwable {
        HasFinalMethod proxy = proxyFor(HasFinalMethod.class).build();
        assertEquals("final method", proxy.finalMethod());
        assertEquals("fake result", proxy.nonFinalMethod());
    }

    public static class HasPrivateMethod {
        private String result() {
            return "expected";
        }
    }

    public void testProxyingPrivateMethods_NotIntercepted() throws Throwable {
        HasPrivateMethod proxy = proxyFor(HasPrivateMethod.class).build();
        try {
            proxy.getClass().getDeclaredMethod("result");
            fail();
        } catch (NoSuchMethodException expected) {

        }

        assertEquals("expected", proxy.result());
    }

    public static class HasPackagePrivateMethod {
        String result() {
            throw new AssertionFailedError();
        }
    }

    public void testProxyingPackagePrivateMethods_NotIntercepted()
            throws Throwable {
        HasPackagePrivateMethod proxy = proxyFor(HasPackagePrivateMethod.class)
                .build();
        try {
            proxy.getClass().getDeclaredMethod("result");
            fail();
        } catch (NoSuchMethodException expected) {

        }

        try {
            proxy.result();
            fail();
        } catch (AssertionFailedError expected) {

        }
    }

    public static class HasProtectedMethod {
        protected String result() {
            throw new AssertionFailedError();
        }
    }

    public void testProxyingProtectedMethods_AreIntercepted() throws Throwable {
        assertEquals("fake result", proxyFor(HasProtectedMethod.class).build().result());
    }

    public static class MyParentClass {
        String someMethod() {
            return "package";
        }
    }

    public static class MyChildClassWithProtectedMethod extends MyParentClass {
        @Override
        protected String someMethod() {
            return "protected";
        }
    }

    public static class MyChildClassWithPublicMethod extends MyParentClass {
        @Override
        public String someMethod() {
            return "public";
        }
    }

    public void testProxying_ClassHierarchy() throws Throwable {
        assertEquals("package", proxyFor(MyParentClass.class).build().someMethod());
        assertEquals("fake result", proxyFor(MyChildClassWithProtectedMethod.class).build().someMethod());
        assertEquals("fake result", proxyFor(MyChildClassWithPublicMethod.class).build().someMethod());
    }

    public static class HasVoidMethod {
        public void dangerousMethod() {
            fail();
        }
    }

    public void testVoidMethod_ShouldNotThrowRuntimeException() throws Throwable {
        proxyFor(HasVoidMethod.class).build().dangerousMethod();
    }

    public void testObjectMethodsAreAlsoProxied() throws Throwable {
        Object proxy = proxyFor(Object.class).build();
        fakeHandler.setFakeResult("mystring");
        assertEquals("mystring", proxy.toString());
        fakeHandler.setFakeResult(-1);
        assertEquals(-1, proxy.hashCode());
        fakeHandler.setFakeResult(false);
        assertEquals(false, proxy.equals(proxy));
    }

    public static class AllReturnTypes {
        public boolean getBoolean() { return true; }
        public int getInt() { return 1; }
        public byte getByte() { return 2; }
        public long getLong() { return 3L; }
        public short getShort() { return 4; }
        public float getFloat() { return 5f; }
        public double getDouble() { return 6.0; }
        public char getChar() { return 'c'; }
        public int[] getIntArray() { return new int[] { 8, 9 }; }
        public String[] getStringArray() { return new String[] { "d", "e" }; }
    }

    public void testAllReturnTypes() throws Throwable {
        AllReturnTypes proxy = proxyFor(AllReturnTypes.class).build();
        fakeHandler.setFakeResult(false);
        assertEquals(false, proxy.getBoolean());
        fakeHandler.setFakeResult(8);
        assertEquals(8, proxy.getInt());
        fakeHandler.setFakeResult((byte) 9);
        assertEquals(9, proxy.getByte());
        fakeHandler.setFakeResult(10L);
        assertEquals(10, proxy.getLong());
        fakeHandler.setFakeResult((short) 11);
        assertEquals(11, proxy.getShort());
        fakeHandler.setFakeResult(12f);
        assertEquals(12f, proxy.getFloat());
        fakeHandler.setFakeResult(13.0);
        assertEquals(13.0, proxy.getDouble());
        fakeHandler.setFakeResult('z');
        assertEquals('z', proxy.getChar());
        fakeHandler.setFakeResult(new int[] { -1, -2 });
        assertEquals("[-1, -2]", Arrays.toString(proxy.getIntArray()));
        fakeHandler.setFakeResult(new String[] { "x", "y" });
        assertEquals("[x, y]", Arrays.toString(proxy.getStringArray()));
    }

    public static class PassThroughAllTypes {
        public boolean getBoolean(boolean input) { return input; }
        public int getInt(int input) { return input; }
        public byte getByte(byte input) { return input; }
        public long getLong(long input) { return input; }
        public short getShort(short input) { return input; }
        public float getFloat(float input) { return input; }
        public double getDouble(double input) { return input; }
        public char getChar(char input) { return input; }
        public String getString(String input) { return input; }
        public Object getObject(Object input) { return input; }
        public void getNothing() {}
    }

    public static class InvokeSuperHandler implements InvocationHandler {
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return ProxyBuilder.callSuper(proxy, method, args);
        }
    }

    public void testPassThroughWorksForAllTypes() throws Exception {
        PassThroughAllTypes proxy = proxyFor(PassThroughAllTypes.class)
                .handler(new InvokeSuperHandler())
                .build();
        assertEquals(false, proxy.getBoolean(false));
        assertEquals(true, proxy.getBoolean(true));
        assertEquals(0, proxy.getInt(0));
        assertEquals(1, proxy.getInt(1));
        assertEquals((byte) 2, proxy.getByte((byte) 2));
        assertEquals((byte) 3, proxy.getByte((byte) 3));
        assertEquals(4L, proxy.getLong(4L));
        assertEquals(5L, proxy.getLong(5L));
        assertEquals((short) 6, proxy.getShort((short) 6));
        assertEquals((short) 7, proxy.getShort((short) 7));
        assertEquals(8f, proxy.getFloat(8f));
        assertEquals(9f, proxy.getFloat(9f));
        assertEquals(10.0, proxy.getDouble(10.0));
        assertEquals(11.0, proxy.getDouble(11.0));
        assertEquals('a', proxy.getChar('a'));
        assertEquals('b', proxy.getChar('b'));
        assertEquals("asdf", proxy.getString("asdf"));
        assertEquals("qwer", proxy.getString("qwer"));
        assertEquals(null, proxy.getString(null));
        Object a = new Object();
        assertEquals(a, proxy.getObject(a));
        assertEquals(null, proxy.getObject(null));
        proxy.getNothing();
    }

    public static class ExtendsAllReturnTypes extends AllReturnTypes {
        public int example() { return 0; }
    }

    public void testProxyWorksForSuperclassMethodsAlso() throws Throwable {
        ExtendsAllReturnTypes proxy = proxyFor(ExtendsAllReturnTypes.class).build();
        fakeHandler.setFakeResult(99);
        assertEquals(99, proxy.example());
        assertEquals(99, proxy.getInt());
        assertEquals(99, proxy.hashCode());
    }

    public static class HasOddParams {
        public long method(int first, Integer second) {
            throw new AssertionFailedError();
        }
    }

    public void testMixingBoxedAndUnboxedParams() throws Throwable {
        HasOddParams proxy = proxyFor(HasOddParams.class).build();
        fakeHandler.setFakeResult(99L);
        assertEquals(99L, proxy.method(1, Integer.valueOf(2)));
    }

    public static class SingleInt {
        public String getString(int value) {
            throw new AssertionFailedError();
        }
    }

    public void testSinglePrimitiveParameter() throws Throwable {
        InvocationHandler handler = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return "asdf" + ((Integer) args[0]).intValue();
            }
        };
        assertEquals("asdf1", proxyFor(SingleInt.class).handler(handler).build().getString(1));
    }

    public static class TwoConstructors {
        private final String string;

        public TwoConstructors() {
            string = "no-arg";
        }

        public TwoConstructors(boolean unused) {
            string = "one-arg";
        }
    }

    public void testNoConstructorArguments_CallsNoArgConstructor() throws Throwable {
        TwoConstructors twoConstructors = proxyFor(TwoConstructors.class).build();
        assertEquals("no-arg", twoConstructors.string);
    }

    public void testWithoutInvocationHandler_ThrowsIllegalArgumentException() throws Throwable {
        try {
            ProxyBuilder.forClass(TwoConstructors.class)
                    .dexCache(DexMakerTest.getDataDirectory())
                    .build();
            fail();
        } catch (IllegalArgumentException expected) {}
    }

    public static class HardToConstructCorrectly {
        public HardToConstructCorrectly() { fail(); }
        public HardToConstructCorrectly(Runnable ignored) { fail(); }
        public HardToConstructCorrectly(Exception ignored) { fail(); }
        public HardToConstructCorrectly(Boolean ignored) { /* safe */ }
        public HardToConstructCorrectly(Integer ignored) { fail(); }
    }

    public void testHardToConstruct_WorksIfYouSpecifyTheConstructorCorrectly() throws Throwable {
        proxyFor(HardToConstructCorrectly.class)
                .constructorArgTypes(Boolean.class)
                .constructorArgValues(true)
                .build();
    }

    public void testHardToConstruct_EvenWorksWhenArgsAreAmbiguous() throws Throwable {
        proxyFor(HardToConstructCorrectly.class)
                .constructorArgTypes(Boolean.class)
                .constructorArgValues(new Object[] { null })
                .build();
    }

    public void testHardToConstruct_DoesNotInferTypesFromValues() throws Throwable {
        try {
            proxyFor(HardToConstructCorrectly.class)
                    .constructorArgValues(true)
                    .build();
            fail();
        } catch (IllegalArgumentException expected) {}
    }

    public void testDefaultProxyHasSuperMethodToAccessOriginal() throws Exception {
        Object objectProxy = proxyFor(Object.class).build();
        assertNotNull(objectProxy.getClass().getMethod("super$hashCode$int"));
    }

    public static class PrintsOddAndValue {
        public String method(int value) {
            return "odd " + value;
        }
    }

    public void testSometimesDelegateToSuper() throws Exception {
        InvocationHandler delegatesOddValues = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("method")) {
                    int intValue = ((Integer) args[0]).intValue();
                    if (intValue % 2 == 0) {
                        return "even " + intValue;
                    }
                }
                return ProxyBuilder.callSuper(proxy, method, args);
            }
        };
        PrintsOddAndValue proxy = proxyFor(PrintsOddAndValue.class)
                .handler(delegatesOddValues)
                .build();
        assertEquals("even 0", proxy.method(0));
        assertEquals("odd 1", proxy.method(1));
        assertEquals("even 2", proxy.method(2));
        assertEquals("odd 3", proxy.method(3));
    }
    
    public void testCallSuperThrows() throws Exception {
        InvocationHandler handler = new InvocationHandler() {
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                return ProxyBuilder.callSuper(o, method, objects);
            }
        };

        FooThrows fooThrows = proxyFor(FooThrows.class)
                .handler(handler)
                .build();

        try {
            fooThrows.foo();
            fail();
        } catch (IllegalStateException expected) {
            assertEquals("boom!", expected.getMessage());
        }
    }
    
    public static class FooThrows {
        public void foo() {
            throw new IllegalStateException("boom!");
        }
    }

    public static class DoubleReturn {
        public double getValue() {
            return 2.0;
        }
    }

    public void testUnboxedResult() throws Exception {
        fakeHandler.fakeResult = 2.0;
        assertEquals(2.0, proxyFor(DoubleReturn.class).build().getValue());
    }

    public static void staticMethod() {
    }

    public void testDoesNotOverrideStaticMethods() throws Exception {
        // Method should exist on this test class itself.
        ProxyBuilderTest.class.getDeclaredMethod("staticMethod");
        // Method should not exist on the subclass.
        try {
            proxyFor(ProxyBuilderTest.class).build().getClass().getDeclaredMethod("staticMethod");
            fail();
        } catch (NoSuchMethodException expected) {}
    }

    public void testIllegalCacheDirectory() throws Exception {
        try {
          proxyFor(ProxyForIllegalCacheDirectory.class)
                  .dexCache(new File("/poop/"))
                  .build();
          fail();
        } catch (IOException expected) {
        }
    }

    public static class ProxyForIllegalCacheDirectory {
    }

    public void testInvalidConstructorSpecification() throws Exception {
        try {
            proxyFor(Object.class)
                    .constructorArgTypes(String.class, Boolean.class)
                    .constructorArgValues("asdf", true)
                    .build();
            fail();
        } catch (IllegalArgumentException expected) {}
    }

    public static abstract class AbstractClass {
        public abstract Object getValue();
    }

    public void testAbstractClassBehaviour() throws Exception {
        assertEquals("fake result", proxyFor(AbstractClass.class).build().getValue());
    }

    public static class CtorHasDeclaredException {
        public CtorHasDeclaredException() throws IOException {
            throw new IOException();
        }
    }

    public static class CtorHasRuntimeException {
        public CtorHasRuntimeException() {
            throw new RuntimeException("my message");
        }
    }

    public static class CtorHasError {
        public CtorHasError() {
            throw new Error("my message again");
        }
    }

    public void testParentConstructorThrowsDeclaredException() throws Exception {
        try {
            proxyFor(CtorHasDeclaredException.class).build();
            fail();
        } catch (UndeclaredThrowableException expected) {
            assertTrue(expected.getCause() instanceof IOException);
        }
        try {
            proxyFor(CtorHasRuntimeException.class).build();
            fail();
        } catch (RuntimeException expected) {
            assertEquals("my message", expected.getMessage());
        }
        try {
            proxyFor(CtorHasError.class).build();
            fail();
        } catch (Error expected) {
            assertEquals("my message again", expected.getMessage());
        }
    }

    public void testGetInvocationHandler_NormalOperation() throws Exception {
        Object proxy = proxyFor(Object.class).build();
        assertSame(fakeHandler, ProxyBuilder.getInvocationHandler(proxy));
    }

    public void testGetInvocationHandler_NotAProxy() {
        try {
            ProxyBuilder.getInvocationHandler(new Object());
            fail();
        } catch (IllegalArgumentException expected) {}
    }

    public static class ReturnsObject {
        public Object getValue() {
            return new Object();
        }
    }

    public static class ReturnsString extends ReturnsObject {
        @Override
        public String getValue() {
            return "a string";
        }
    }

    public void testCovariantReturnTypes_NormalBehaviour() throws Exception {
        String expected = "some string";
        fakeHandler.setFakeResult(expected);
        assertSame(expected, proxyFor(ReturnsObject.class).build().getValue());
        assertSame(expected, proxyFor(ReturnsString.class).build().getValue());
    }

    public void testCovariantReturnTypes_WrongReturnType() throws Exception {
        try {
            fakeHandler.setFakeResult(new Object());
            proxyFor(ReturnsString.class).build().getValue();
            fail();
        } catch (ClassCastException expected) {}
    }

    public void testCaching() throws Exception {
        SimpleClass a = proxyFor(SimpleClass.class).build();
        SimpleClass b = proxyFor(SimpleClass.class).build();
        assertSame(a.getClass(), b.getClass());
    }

    public void testCachingWithMultipleConstructors() throws Exception {
        HasMultipleConstructors a = ProxyBuilder.forClass(HasMultipleConstructors.class)
                .constructorArgTypes()
                .constructorArgValues()
                .handler(fakeHandler)
                .dexCache(DexMakerTest.getDataDirectory()).build();
        assertEquals("no args", a.calledConstructor);
        HasMultipleConstructors b = ProxyBuilder.forClass(HasMultipleConstructors.class)
                .constructorArgTypes(int.class)
                .constructorArgValues(2)
                .handler(fakeHandler)
                .dexCache(DexMakerTest.getDataDirectory()).build();
        assertEquals("int 2", b.calledConstructor);
        assertEquals(a.getClass(), b.getClass());

        HasMultipleConstructors c = ProxyBuilder.forClass(HasMultipleConstructors.class)
                .constructorArgTypes(Integer.class)
                .constructorArgValues(3)
                .handler(fakeHandler)
                .dexCache(DexMakerTest.getDataDirectory()).build();
        assertEquals("Integer 3", c.calledConstructor);
        assertEquals(a.getClass(), c.getClass());
    }

    public static class HasMultipleConstructors {
        private final String calledConstructor;
        public HasMultipleConstructors() {
            calledConstructor = "no args";
        }
        public HasMultipleConstructors(int b) {
            calledConstructor = "int " + b;
        }
        public HasMultipleConstructors(Integer c) {
            calledConstructor = "Integer " + c;
        }
    }

    public void testClassNotCachedWithDifferentParentClassLoaders() throws Exception {
        ClassLoader classLoaderA = newPathClassLoader();
        SimpleClass a = proxyFor(SimpleClass.class)
                .parentClassLoader(classLoaderA)
                .build();
        assertEquals(classLoaderA, a.getClass().getClassLoader().getParent());

        ClassLoader classLoaderB = newPathClassLoader();
        SimpleClass b = proxyFor(SimpleClass.class)
                .parentClassLoader(classLoaderB)
                .build();
        assertEquals(classLoaderB, b.getClass().getClassLoader().getParent());

        assertTrue(a.getClass() != b.getClass());
    }
    
    public void testAbstractClassWithUndeclaredInterfaceMethod() throws Throwable {
        DeclaresInterface declaresInterface = proxyFor(DeclaresInterface.class)
                .build();
        assertEquals("fake result", declaresInterface.call());
        try {
            ProxyBuilder.callSuper(declaresInterface, Callable.class.getMethod("call"));
            fail();
        } catch (AbstractMethodError expected) {
        }
    }
    
    public static abstract class DeclaresInterface implements Callable<String> {
    }

    public void testImplementingInterfaces() throws Throwable {
        SimpleClass simpleClass = proxyFor(SimpleClass.class)
                .implementing(Callable.class)
                .implementing(Comparable.class)
                .build();
        assertEquals("fake result", simpleClass.simpleMethod());

        Callable<?> asCallable = (Callable<?>) simpleClass;
        assertEquals("fake result", asCallable.call());

        Comparable<?> asComparable = (Comparable<?>) simpleClass;
        fakeHandler.fakeResult = 3;
        assertEquals(3, asComparable.compareTo(null));
    }

    public void testCallSuperWithInterfaceMethod() throws Throwable {
        SimpleClass simpleClass = proxyFor(SimpleClass.class)
                .implementing(Callable.class)
                .build();
        try {
            ProxyBuilder.callSuper(simpleClass, Callable.class.getMethod("call"));
            fail();
        } catch (AbstractMethodError expected) {
        } catch (NoSuchMethodError expected) {
        }
    }

    public void testImplementInterfaceCallingThroughConcreteClass() throws Throwable {
        InvocationHandler invocationHandler = new InvocationHandler() {
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                assertEquals("a", ProxyBuilder.callSuper(o, method, objects));
                return "b";
            }
        };
        ImplementsCallable proxy = proxyFor(ImplementsCallable.class)
                .implementing(Callable.class)
                .handler(invocationHandler)
                .build();
        assertEquals("b", proxy.call());
        assertEquals("a", ProxyBuilder.callSuper(
                proxy, ImplementsCallable.class.getMethod("call")));
    }
    
    /**
     * This test is a bit unintuitive because it exercises the synthetic methods
     * that support covariant return types. Calling 'Object call()' on the
     * interface bridges to 'String call()', and so the super method appears to
     * also be proxied.
     */
    public void testImplementInterfaceCallingThroughInterface() throws Throwable {
        final AtomicInteger count = new AtomicInteger();

        InvocationHandler invocationHandler = new InvocationHandler() {
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                count.incrementAndGet();
                return ProxyBuilder.callSuper(o, method, objects);
            }
        };

        Callable<?> proxy = proxyFor(ImplementsCallable.class)
                .implementing(Callable.class)
                .handler(invocationHandler)
                .build();

        // the invocation handler is called twice!
        assertEquals("a", proxy.call());
        assertEquals(2, count.get());

        // the invocation handler is called, even though this is a callSuper() call!
        assertEquals("a", ProxyBuilder.callSuper(proxy, Callable.class.getMethod("call")));
        assertEquals(3, count.get());
    }
    
    public static class ImplementsCallable implements Callable<String> {
        public String call() throws Exception {
            return "a";
        }
    }

    /**
     * This test shows that our generated proxies follow the bytecode convention
     * where methods can have the same name but unrelated return types. This is
     * different from javac's convention where return types must be assignable
     * in one direction or the other.
     */
    public void testInterfacesSameNamesDifferentReturnTypes() throws Throwable {
        InvocationHandler handler = new InvocationHandler() {
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                if (method.getReturnType() == void.class) {
                    return null;
                } else if (method.getReturnType() == String.class) {
                    return "X";
                } else if (method.getReturnType() == int.class) {
                    return 3;
                } else {
                    throw new AssertionFailedError();
                }
            }
        };
        
        Object o = proxyFor(Object.class)
                .implementing(FooReturnsVoid.class, FooReturnsString.class, FooReturnsInt.class)
                .handler(handler)
                .build();
        
        FooReturnsVoid a = (FooReturnsVoid) o;
        a.foo();
        
        FooReturnsString b = (FooReturnsString) o;
        assertEquals("X", b.foo());
        
        FooReturnsInt c = (FooReturnsInt) o;
        assertEquals(3, c.foo());
    }
    
    public void testInterfacesSameNamesSameReturnType() throws Throwable {
        Object o = proxyFor(Object.class)
                .implementing(FooReturnsInt.class, FooReturnsInt2.class)
                .build();
        
        fakeHandler.setFakeResult(3);

        FooReturnsInt a = (FooReturnsInt) o;
        assertEquals(3, a.foo());

        FooReturnsInt2 b = (FooReturnsInt2) o;
        assertEquals(3, b.foo());
    }
    
    public interface FooReturnsVoid {
        void foo();
    }

    public interface FooReturnsString {
        String foo();
    }

    public interface FooReturnsInt {
        int foo();
    }
    
    public interface FooReturnsInt2 {
        int foo();
    }
    
    private ClassLoader newPathClassLoader() throws Exception {
        return (ClassLoader) Class.forName("dalvik.system.PathClassLoader")
                .getConstructor(String.class, ClassLoader.class)
                .newInstance("", getClass().getClassLoader());

    }

    public void testSubclassOfRandom() throws Exception {
        proxyFor(Random.class)
                .handler(new InvokeSuperHandler())
                .build();
    }

    public static class FinalToString {
        @Override public final String toString() {
            return "no proxy";
        }
    }

    // https://code.google.com/p/dexmaker/issues/detail?id=12
    public void testFinalToString() throws Throwable {
        assertEquals("no proxy", proxyFor(FinalToString.class).build().toString());
    }

    public static class FinalInterfaceImpl implements FooReturnsString {
        @Override public final String foo() {
          return "no proxy";
        }
    }

    public static class ExtenstionOfFinalInterfaceImpl extends FinalInterfaceImpl
            implements FooReturnsString {
    }

    public void testFinalInterfaceImpl() throws Throwable {
        assertEquals("no proxy", proxyFor(ExtenstionOfFinalInterfaceImpl.class).build().foo());
    }

    // https://code.google.com/p/dexmaker/issues/detail?id=9
    public interface DeclaresMethodLate {
        void thisIsTheMethod();
    }

    public static class MakesMethodFinalEarly {
        public final void thisIsTheMethod() {}
    }

    public static class YouDoNotChooseYourFamily
            extends MakesMethodFinalEarly implements DeclaresMethodLate {}

    public void testInterfaceMethodMadeFinalBeforeActualInheritance() throws Exception {
        proxyFor(YouDoNotChooseYourFamily.class).build();
    }

    /** Simple helper to add the most common args for this test to the proxy builder. */
    private <T> ProxyBuilder<T> proxyFor(Class<T> clazz) throws Exception {
        return ProxyBuilder.forClass(clazz)
                .handler(fakeHandler)
                .dexCache(DexMakerTest.getDataDirectory());
    }

    private static class FakeInvocationHandler implements InvocationHandler {
        private Object fakeResult = "fake result";

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return fakeResult;
        }

        public void setFakeResult(Object result) {
            fakeResult = result;
        }
    }

    public static class TestOrderingClass {
        public int returnsInt() {
            return 0;
        }

        public int returnsInt(int param1, int param2) {
            return 1;
        }

        public String returnsString() {
            return "string";
        }

        public boolean returnsBoolean() {
            return false;
        }

        public double returnsDouble() {
            return 1.0;
        }

        public Object returnsObject() {
            return new Object();
        }
    }

    @SuppressWarnings("unchecked")
    public void testMethodsGeneratedInDeterministicOrder() throws Exception {
        // Grab the static methods array from the original class.
        Method[] methods1 = getMethodsForProxyClass(TestOrderingClass.class);
        assertNotNull(methods1);

        // Clear ProxyBuilder's in-memory cache of classes. This will force
        // it to rebuild the class and reset the static methods field.
        Map<Class<?>, Class<?>> map = getGeneratedProxyClasses();
        assertNotNull(map);
        map.clear();

        // Grab the static methods array from the rebuilt class.
        Method[] methods2 = getMethodsForProxyClass(TestOrderingClass.class);;
        assertNotNull(methods2);

        // Ensure that the two method arrays are equal.
        assertTrue(Arrays.equals(methods1, methods2));
    }

    // Returns static methods array from a proxy class.
    private Method[] getMethodsForProxyClass(Class<?> parentClass) throws Exception {
        Class<?> proxyClass = proxyFor(parentClass).buildProxyClass();
        Method[] methods = null;
        for (Field f : proxyClass.getDeclaredFields()) {
            if (Method[].class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                methods = (Method[]) f.get(null);
                break;
            }
        }

        return methods;
    }

    private Map<Class<?>, Class<?>> getGeneratedProxyClasses() throws Exception {
        Field mapField = ProxyBuilder.class
                .getDeclaredField("generatedProxyClasses");
        mapField.setAccessible(true);
        return (Map<Class<?>, Class<?>>) mapField.get(null);
    }
}
