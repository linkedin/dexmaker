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

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoSession;
import org.mockito.exceptions.verification.NoInteractionsWanted;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.ignoreStubs;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.staticMockMarker;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.verify;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.verifyNoMoreInteractions;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.verifyZeroInteractions;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;

public class VerifyStatic {
    @Test
    public void verifyMockedStringMethod() throws Exception {
        MockitoSession session = mockitoSession().mockStatic(EchoClass.class).startMocking();
        try {
            assertNull(EchoClass.echo("marco!"));

            ArgumentCaptor<String> echoCaptor = ArgumentCaptor.forClass(String.class);
            verify(() -> {return EchoClass.echo(echoCaptor.capture());});
            assertEquals("marco!", echoCaptor.getValue());

            verifyNoMoreInteractions(staticMockMarker(EchoClass.class));
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void verifyMockedVoidMethod() throws Exception {
        MockitoSession session = mockitoSession().mockStatic(ConsumeClass.class).startMocking();
        try {
            ConsumeClass.consume("donut");

            ArgumentCaptor<String> yumCaptor = ArgumentCaptor.forClass(String.class);
            verify(() -> ConsumeClass.consume(yumCaptor.capture()));

            verifyNoMoreInteractions(staticMockMarker(ConsumeClass.class));
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void verifyWithTwoMocks() throws Exception {
        MockitoSession session = mockitoSession().mockStatic(EchoClass.class)
                .mockStatic(ConsumeClass.class).startMocking();
        try {
            ConsumeClass.consume("donut");
            assertNull(EchoClass.echo("marco!"));

            ArgumentCaptor<String> yumCaptor = ArgumentCaptor.forClass(String.class);
            verify(() -> ConsumeClass.consume(yumCaptor.capture()));

            ArgumentCaptor<String> echoCaptor = ArgumentCaptor.forClass(String.class);
            verify(() -> {return EchoClass.echo(echoCaptor.capture());});
            assertEquals("marco!", echoCaptor.getValue());

            verifyNoMoreInteractions(staticMockMarker(ConsumeClass.class));
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void verifySpiedStringMethod() throws Exception {
        MockitoSession session = mockitoSession().spyStatic(EchoClass.class).startMocking();
        try {
            assertEquals("marco!", EchoClass.echo("marco!"));

            ArgumentCaptor<String> echoCaptor = ArgumentCaptor.forClass(String.class);
            verify(() -> {return EchoClass.echo(echoCaptor.capture());});
            assertEquals("marco!", echoCaptor.getValue());

            verifyNoMoreInteractions(staticMockMarker(EchoClass.class));
        } finally {
            session.finishMocking();
        }
    }

    @Test(expected = NoInteractionsWanted.class)
    public void zeroInvocationsThrowsIfThereWasAnInvocation() throws Exception {
        MockitoSession session = mockitoSession().mockStatic(EchoClass.class).startMocking();
        try {
            EchoClass.echo("marco!");
            verifyZeroInteractions(staticMockMarker(EchoClass.class));
            fail();
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void verifyWithIgnoreStubs() throws Exception {
        MockitoSession session = mockitoSession().spyStatic(EchoClass.class).startMocking();
        try {
            // 'ignoreStubs' only ignore stubs
            when(EchoClass.echo("marco!")).thenReturn("polo");
            assertEquals("polo", EchoClass.echo("marco!"));
            assertEquals("echo", EchoClass.echo("echo"));

            verify(() -> {return EchoClass.echo(eq("echo"));});
            verifyNoMoreInteractions(ignoreStubs(staticMockMarker(EchoClass.class)));
        } finally {
            session.finishMocking();
        }
    }

    private static class EchoClass {
        static final String echo(String echo) {
            return echo;
        }
    }

    private static class ConsumeClass {
        static final void consume(String yum) {
            // empty
        }
    }
}
