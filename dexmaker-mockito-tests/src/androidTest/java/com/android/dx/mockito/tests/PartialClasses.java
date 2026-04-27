/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.dx.mockito.tests;

import org.junit.Test;
import org.mockito.exceptions.base.MockitoException;

import java.util.AbstractList;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests what happens if code tries to call real methods of abstract classed and in interfaces
 */
public class PartialClasses {
    @Test
    public void callRealMethodOnInterface() {
        Runnable r = mock(Runnable.class);

        try {
            doCallRealMethod().when(r).run();
            fail();
        } catch (MockitoException e) {
            // expected
        }
    }

    @Test
    public void callAbstractRealMethodOnAbstractClass() {
        AbstractList l = mock(AbstractList.class);

        try {
            when(l.size()).thenCallRealMethod();
            fail();
        } catch (MockitoException e) {
            // expected
        }
    }

    @Test
    public void callRealMethodOnAbstractClass() {
        AbstractList l = mock(AbstractList.class);

        doCallRealMethod().when(l).clear();

        l.clear();
    }
}
