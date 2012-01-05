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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Random;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class ProxyBuilderTest extends TestCase {
    private FakeInvocationHandler fakeHandler = new FakeInvocationHandler();

    public static class SimpleClass {
        public String simpleMethod() {
            throw new AssertionFailedError();
        }
    }

    public void testExampleOperation() throws Throwable {
        fakeHandler.setFakeResult("expected");
        SimpleClass proxy = proxyFor(SimpleClass.class).build();
        assertEquals("expected", proxy.simpleMethod());
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
        assertEquals("expected", proxyFor(HasPrivateMethod.class).build().result());
    }

    public static class HasPackagePrivateMethod {
        String result() {
            throw new AssertionFailedError();
        }
    }

    public void testProxyingPackagePrivateMethods_AreIntercepted() throws Throwable {
        assertEquals("fake result", proxyFor(HasPackagePrivateMethod.class).build().result());
    }

    public static class HasProtectedMethod {
        protected String result() {
            throw new AssertionFailedError();
        }
    }

    public void testProxyingProtectedMethods_AreIntercepted() throws Throwable {
        assertEquals("fake result", proxyFor(HasProtectedMethod.class).build().result());
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

    public static class AllPrimitiveMethods {
        public boolean getBoolean() { return true; }
        public int getInt() { return 1; }
        public byte getByte() { return 2; }
        public long getLong() { return 3L; }
        public short getShort() { return 4; }
        public float getFloat() { return 5f; }
        public double getDouble() { return 6.0; }
        public char getChar() { return 'c'; }
    }

    public void testAllPrimitiveReturnTypes() throws Throwable {
        AllPrimitiveMethods proxy = proxyFor(AllPrimitiveMethods.class).build();
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
    }

    public static class PassThroughAllPrimitives {
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

    public void testPassThroughWorksForAllPrimitives() throws Exception {
        PassThroughAllPrimitives proxy = proxyFor(PassThroughAllPrimitives.class)
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

    public static class ExtendsAllPrimitiveMethods extends AllPrimitiveMethods {
        public int example() { return 0; }
    }

    public void testProxyWorksForSuperclassMethodsAlso() throws Throwable {
        ExtendsAllPrimitiveMethods proxy = proxyFor(ExtendsAllPrimitiveMethods.class).build();
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
        assertNotNull(objectProxy.getClass().getMethod("super_hashCode"));
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
          proxyFor(Object.class).dexCache(new File("//////")).build();
          fail();
        } catch (IOException expected) {
        }
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

    public void testCaching_ShouldWork() {
        // TODO: We're not supporting caching yet.  But we should as soon as possible.
        fail();
    }

    public void testSubclassOfRandom() throws Exception {
        proxyFor(Random.class)
                .handler(new InvokeSuperHandler())
                .build();
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
}
