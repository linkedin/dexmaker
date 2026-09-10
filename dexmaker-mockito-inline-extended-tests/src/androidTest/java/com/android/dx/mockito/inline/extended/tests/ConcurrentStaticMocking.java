/*
 * Copyright (C) 2026 The Android Open Source Project
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
import org.mockito.MockitoSession;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.staticMockMarker;
import static org.junit.Assert.assertNull;

public class ConcurrentStaticMocking {

    public static class SuperClass {
        public static void unmockedMethod() {}
    }

    public static class SubClass extends SuperClass {
    }

    private static class DummyClass {
        static void dummyMethod() {}
    }

    @Test
    public void concurrentMockingWithBackgroundThread() throws Exception {
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicReference<Throwable> bgError = new AtomicReference<>();
        CountDownLatch threadStarted = new CountDownLatch(1);

        // 1. One-time setup: Spying SubClass forces Dexmaker to instrument SuperClass
        MockitoSession initSession = mockitoSession()
                .spyStatic(SubClass.class)
                .startMocking();
        initSession.finishMocking();

        // 2. Start background thread continuously executing unmocked method on SuperClass.
        // Dexmaker intercepts this call to check if any mocked subclass handles it,
        // which iterates over classToMarker.keySet().
        Thread bgThread = new Thread(() -> {
            threadStarted.countDown();
            while (running.get()) {
                try {
                    SuperClass.unmockedMethod();
                } catch (Throwable t) {
                    bgError.set(t);
                    break;
                }
            }
        }, "ConcurrentTestWorker");
        bgThread.start();
        threadStarted.await();

        // 3. Concurrently cycle through creating and finishing static mocking sessions,
        // which mutates classToMarker concurrently with the background thread's iteration.
        try {
            for (int i = 0; i < 500; i++) {
                if (bgError.get() != null) {
                    break;
                }
                MockitoSession session = mockitoSession()
                        .spyStatic(SubClass.class)
                        .mockStatic(DummyClass.class)
                        .startMocking();
                session.finishMocking();
            }
        } finally {
            running.set(false);
            bgThread.join(5000);
        }

        assertNull("Background thread threw exception: " + bgError.get(), bgError.get());
    }

    @Test
    public void staticMockMarkerWithNullClass() {
        assertNull(staticMockMarker((Class<?>) null));

        MockitoSession session = mockitoSession()
                .mockStatic(DummyClass.class)
                .startMocking();
        try {
            assertNull(staticMockMarker((Class<?>) null));
        } finally {
            session.finishMocking();
        }
    }
}
