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

package com.android.dx.mockito.inline;

import org.mockito.exceptions.stacktrace.StackTraceCleaner;
import org.mockito.plugins.StackTraceCleanerProvider;

/**
 * Cleans out mockito internal elements out of stack traces. This creates stack traces as if mockito
 * would have not intercepted any calls.
 */
public final class DexmakerStackTraceCleaner implements StackTraceCleanerProvider {
    @Override
    public StackTraceCleaner getStackTraceCleaner(final StackTraceCleaner defaultCleaner) {
        return new StackTraceCleaner() {
            @Override
            public boolean isIn(StackTraceElement candidate) {
                return defaultCleaner.isIn(candidate)
                        // dexmaker class proxies
                        && !candidate.getClassName().endsWith("_Proxy")

                        && !candidate.getClassName().startsWith("java.lang.reflect.Method")
                        && !candidate.getClassName().startsWith("java.lang.reflect.Proxy")
                        && !candidate.getClassName().startsWith("com.android.dx.mockito.")

                        // dalvik interface proxies
                        && !candidate.getClassName().startsWith("$Proxy")
                        && !candidate.getClassName().matches(".*\\.\\$Proxy[\\d]+");
            }
        };
    }

}
