/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static org.mockito.Mockito.framework;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import androidx.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MemoryLeaks {
    private static final int ARRAY_LENGTH = 1 << 20;  // 4 MB

    @Test
    public void callMethodWithMocksCyclically() {
        for (int i = 0; i < 100; ++i) {
            final A a = mock(A.class);
            a.largeArray = new int[ARRAY_LENGTH];
            final B b = mock(B.class);

            a.accept(b);
            b.accept(a);

            framework().clearInlineMocks();
        }
    }

    @Test
    public void spyRefersToItself() {
        for (int i = 0; i < 100; ++i) {
            final DeepRefSelfClass instance = spy(new DeepRefSelfClass());
            instance.refInstance(instance);

            framework().clearInlineMocks();
        }
    }

    private static class A {
        private int[] largeArray;

        void accept(B b) {}
    }

    private static class B {
        void accept(A a) {}
    }

    private static class DeepRefSelfClass {
        private final DeepRefSelfClass[] array = new DeepRefSelfClass[1];

        private final int[] largeArray = new int[ARRAY_LENGTH];

        private void refInstance(DeepRefSelfClass instance) {
            array[0] = instance;
        }
    }
}
