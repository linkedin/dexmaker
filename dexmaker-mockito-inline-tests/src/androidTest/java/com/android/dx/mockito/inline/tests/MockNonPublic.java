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

package com.android.dx.mockito.inline.tests;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MockNonPublic {
    private interface SingleMethodInterface {
        String returnA();
    }

    private static <T extends Class> void mockSingleMethod(T clazz) {
        SingleMethodInterface c = (SingleMethodInterface) mock(clazz);
        assertNull(c.returnA());

        when(c.returnA()).thenReturn("fakeA");
        assertEquals("fakeA", c.returnA());
    }

    private static <T extends Class> void spySingleMethod(T clazz) {
        SingleMethodInterface c = (SingleMethodInterface) spy(clazz);
        assertEquals("A", c.returnA());

        when(c.returnA()).thenReturn("fakeA");
        assertEquals("fakeA", c.returnA());
    }

    private static <T extends SingleMethodInterface> void spyWrappedSingleMethod(T original) {
        T c = spy(original);
        assertEquals("A", c.returnA());

        when(c.returnA()).thenReturn("fakeA");
        assertEquals("fakeA", c.returnA());

        // original is unaffected
        assertEquals("A", original.returnA());
    }

    private interface DualMethodInterface {
        String returnA();
        String returnB();
    }

    private static <T extends Class> void mockDualMethod(T clazz) {
        DualMethodInterface c = (DualMethodInterface) mock(clazz);
        assertNull(c.returnA());
        assertNull(c.returnB());

        when(c.returnA()).thenReturn("fakeA");
        assertEquals("fakeA", c.returnA());
        assertNull(c.returnB());

        when(c.returnB()).thenReturn("fakeB");
        assertEquals("fakeA", c.returnA());
        assertEquals("fakeB", c.returnB());
    }

    private static <T extends Class> void spyDualMethod(T clazz) {
        DualMethodInterface c = (DualMethodInterface) spy(clazz);
        assertEquals("A", c.returnA());
        assertEquals("B", c.returnB());

        when(c.returnA()).thenReturn("fakeA");
        assertEquals("fakeA", c.returnA());
        assertEquals("B", c.returnB());

        when(c.returnB()).thenReturn("fakeB");
        assertEquals("fakeA", c.returnA());
        assertEquals("fakeB", c.returnB());
    }

    private static <T extends DualMethodInterface> void spyWrappedDualMethod(T original) {
        T c = spy(original);
        assertEquals("A", c.returnA());
        assertEquals("B", c.returnB());

        when(c.returnA()).thenReturn("fakeA");
        assertEquals("fakeA", c.returnA());
        assertEquals("B", c.returnB());

        when(c.returnB()).thenReturn("fakeB");
        assertEquals("fakeA", c.returnA());
        assertEquals("fakeB", c.returnB());

        // original is unaffected
        assertEquals("A", original.returnA());
        assertEquals("B", original.returnB());
    }

    private static class PrivateClass implements SingleMethodInterface {
        public String returnA() {
            return "A";
        }
    }

    @Test
    public void mockPrivateClass() {
        mockSingleMethod(PrivateClass.class);
    }

    @Test
    public void spyPrivateClass() {
        spySingleMethod(PrivateClass.class);
    }

    @Test
    public void spyWrappedPrivateClass() {
        spyWrappedSingleMethod(new PrivateClass());
    }

    private interface PrivateInterface extends SingleMethodInterface {
        String returnA();
    }

    @Test
    public void mockPrivateInterface() {
        mockSingleMethod(PrivateInterface.class);
    }

    private static class SubOfPrivateInterface implements PrivateInterface {
        public String returnA() {
            return "A";
        }
    }

    @Test
    public void mockSubOfPrivateInterface() {
        mockSingleMethod(SubOfPrivateInterface.class);
    }

    @Test
    public void spySubOfPrivateInterface() {
        spySingleMethod(SubOfPrivateInterface.class);
    }

    @Test
    public void spyWrappedSubOfPrivateInterface() {
        spyWrappedSingleMethod(new SubOfPrivateInterface());
    }

    private static abstract class PrivateAbstractClass implements DualMethodInterface {
        public String returnA() {
            return "A";
        }

        public abstract String returnB();
    }

    @Test
    public void mockPrivateAbstractClass() {
        mockDualMethod(PrivateAbstractClass.class);
    }

    private static class SubOfPrivateAbstractClass extends PrivateAbstractClass {
        public String returnB() {
            return "B";
        }
    }

    @Test
    public void mockSubOfPrivateAbstractClass() {
        mockDualMethod(SubOfPrivateAbstractClass.class);
    }

    @Test
    public void spySubOfPrivateAbstractClass() {
        spyDualMethod(SubOfPrivateAbstractClass.class);
    }

    @Test
    public void spyWrappedSubOfPrivateAbstractClass() {
        spyWrappedDualMethod(new SubOfPrivateAbstractClass());
    }

    static class PackagePrivateClass implements SingleMethodInterface {
        public String returnA() {
            return "A";
        }
    }

    @Test
    public void mockPackagePrivateClass() {
        mockSingleMethod(PackagePrivateClass.class);
    }

    static abstract class PackagePrivateAbstractClass implements DualMethodInterface {
        public String returnA() {
            return "A";
        }

        public abstract String returnB();
    }

    @Test
    public void mockPackagePrivateAbstractClass() {
        mockDualMethod(PackagePrivateAbstractClass.class);
    }

    static class SubOfPackagePrivateAbstractClass extends PackagePrivateAbstractClass {
        public String returnB() {
            return "B";
        }
    }

    @Test
    public void mockSubOfPackagePrivateAbstractClass() {
        mockDualMethod(SubOfPackagePrivateAbstractClass.class);
    }

    @Test
    public void spySubOfPackagePrivateAbstractClass() {
        spyDualMethod(SubOfPackagePrivateAbstractClass.class);
    }

    @Test
    public void spyWrappedSubOfPackagePrivateAbstractClass() {
        spyWrappedDualMethod(new SubOfPackagePrivateAbstractClass());
    }

    interface PackagePrivateInterface extends SingleMethodInterface {
        String returnA();
    }

    @Test
    public void mockPackagePrivateInterface() {
        mockSingleMethod(PackagePrivateInterface.class);
    }

    static class SubOfPackagePrivateInterface implements PackagePrivateInterface {
        public String returnA() {
            return "A";
        }
    }

    @Test
    public void mockSubOfPackagePrivateInterface() {
        mockSingleMethod(SubOfPackagePrivateInterface.class);
    }

    @Test
    public void spySubOfPackagePrivateInterface() {
        spySingleMethod(SubOfPackagePrivateInterface.class);
    }

    @Test
    public void spyWrappedSubOfPackagePrivateInterface() {
        spyWrappedSingleMethod(new SubOfPackagePrivateInterface());
    }

    // Cannot implement SingleMethodInterface as returnA would have to be public
    public static class ClassWithPackagePrivateMethod {
        String returnA() {
            return "A";
        }
    }

    @Test
    public void mockClassWithPackagePrivateMethod() {
        ClassWithPackagePrivateMethod c = mock(ClassWithPackagePrivateMethod.class);
        assertNull(c.returnA());

        when(c.returnA()).thenReturn("fakeA");
        assertEquals("fakeA", c.returnA());
    }

    @Test
    public void spyClassWithPackagePrivateMethod() {
        ClassWithPackagePrivateMethod c = spy(ClassWithPackagePrivateMethod.class);
        assertEquals("A", c.returnA());

        when(c.returnA()).thenReturn("fakeA");
        assertEquals("fakeA", c.returnA());
    }

    @Test
    public void spyWrappedClassWithPackagePrivateMethod() {
        ClassWithPackagePrivateMethod original = new ClassWithPackagePrivateMethod();
        ClassWithPackagePrivateMethod c = spy(original);
        assertEquals("A", c.returnA());

        when(c.returnA()).thenReturn("fakeA");
        assertEquals("fakeA", c.returnA());

        // original is unaffected
        assertEquals("A", original.returnA());
    }

    // Cannot implement DualMethodInterface as returnA/returnB would have to be public
    public static abstract class AbstractClassWithPackagePrivateMethod {
        String returnA() {
            return "A";
        }

        abstract String returnB();
    }

    @Test
    public void mockAbstractClassWithPackagePrivateMethod() {
        AbstractClassWithPackagePrivateMethod c = mock(AbstractClassWithPackagePrivateMethod.class);
        assertNull(c.returnA());
        assertNull(c.returnB());

        when(c.returnA()).thenReturn("fakeA");
        assertEquals("fakeA", c.returnA());
        assertNull(c.returnB());

        when(c.returnB()).thenReturn("fakeB");
        assertEquals("fakeA", c.returnA());
        assertEquals("fakeB", c.returnB());
    }

    public static class SubOfAbstractClassWithPackagePrivateMethod extends
            AbstractClassWithPackagePrivateMethod {
        String returnB() {
            return "B";
        }
    }

    @Test
    public void mockSubOfAbstractClassWithPackagePrivateMethod() {
        SubOfAbstractClassWithPackagePrivateMethod c = mock
                (SubOfAbstractClassWithPackagePrivateMethod.class);
        assertNull(c.returnA());
        assertNull(c.returnB());

        when(c.returnA()).thenReturn("fakeA");
        assertEquals("fakeA", c.returnA());
        assertNull(c.returnB());

        when(c.returnB()).thenReturn("fakeB");
        assertEquals("fakeA", c.returnA());
        assertEquals("fakeB", c.returnB());
    }

    @Test
    public void spySubOfAbstractClassWithPackagePrivateMethod() {
        SubOfAbstractClassWithPackagePrivateMethod c = spy
                (SubOfAbstractClassWithPackagePrivateMethod.class);
        assertEquals("A", c.returnA());
        assertEquals("B", c.returnB());

        when(c.returnA()).thenReturn("fakeA");
        assertEquals("fakeA", c.returnA());
        assertEquals("B", c.returnB());

        when(c.returnB()).thenReturn("fakeB");
        assertEquals("fakeA", c.returnA());
        assertEquals("fakeB", c.returnB());
    }

    @Test
    public void spyWrappedSubOfAbstractClassWithPackagePrivateMethod() {
        SubOfAbstractClassWithPackagePrivateMethod original = new
                SubOfAbstractClassWithPackagePrivateMethod();
        SubOfAbstractClassWithPackagePrivateMethod c = spy(original);
        assertEquals("A", c.returnA());
        assertEquals("B", c.returnB());

        when(c.returnA()).thenReturn("fakeA");
        assertEquals("fakeA", c.returnA());
        assertEquals("B", c.returnB());

        when(c.returnB()).thenReturn("fakeB");
        assertEquals("fakeA", c.returnA());
        assertEquals("fakeB", c.returnB());

        // original is unaffected
        assertEquals("A", original.returnA());
        assertEquals("B", original.returnB());
    }
}
