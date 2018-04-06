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

package com.android.dx.mockito.inline.extended.tests;

import com.android.dx.mockito.inline.extended.StaticMockitoSession;

import org.junit.Test;
import org.mockito.Mock;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.mock;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.verify;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;

public class MockInstanceUsingExtendedMockito {
    @Mock
    private TestClass mockField;

    public static class TestClass {
        public String echo(String arg) {
            return arg;
        }
    }

    @Test
    public void mockClass() throws Exception {
        TestClass t = mock(TestClass.class);

        assertNull(t.echo("mocked"));

        when(t.echo(eq("stubbed"))).thenReturn("B");
        assertEquals("B", t.echo("stubbed"));
        verify(t).echo("mocked");
        verify(t).echo("stubbed");
    }

    @Test
    public void useMockitoSession() throws Exception {
        StaticMockitoSession session = mockitoSession().initMocks(this).startMocking();
        try {
            assertNull(mockField.echo("mocked"));

            when(mockField.echo(eq("stubbed"))).thenReturn("B");
            assertEquals("B", mockField.echo("stubbed"));
            verify(mockField).echo("mocked");
            verify(mockField).echo("stubbed");
        } finally {
            session.finishMocking();
        }
    }
}
