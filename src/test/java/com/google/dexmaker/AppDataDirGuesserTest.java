/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.google.dexmaker;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;

public final class AppDataDirGuesserTest extends TestCase {
    public void testGuessCacheDir_SimpleExample() {
        guessCacheDirFor("/data/app/a.b.c.apk").shouldGive("/data/data/a.b.c/cache");
        guessCacheDirFor("/data/app/a.b.c.tests.apk").shouldGive("/data/data/a.b.c.tests/cache");
    }

    public void testGuessCacheDir_MultipleResultsSeparatedByColon() {
        guessCacheDirFor("/data/app/a.b.c.apk:/data/app/d.e.f.apk")
                .shouldGive("/data/data/a.b.c/cache", "/data/data/d.e.f/cache");
    }

    public void testGuessCacheDir_NotWriteableSkipped() {
        guessCacheDirFor("/data/app/a.b.c.apk:/data/app/d.e.f.apk")
                .withNonWriteable("/data/data/a.b.c/cache")
                .shouldGive("/data/data/d.e.f/cache");
    }

    public void testGuessCacheDir_StripHyphenatedSuffixes() {
        guessCacheDirFor("/data/app/a.b.c-2.apk").shouldGive("/data/data/a.b.c/cache");
    }

    public void testGuessCacheDir_LeadingAndTrailingColonsIgnored() {
        guessCacheDirFor("/data/app/a.b.c.apk:asdf:").shouldGive("/data/data/a.b.c/cache");
        guessCacheDirFor(":asdf:/data/app/a.b.c.apk").shouldGive("/data/data/a.b.c/cache");
    }

    public void testGuessCacheDir_InvalidInputsGiveEmptyArray() {
        guessCacheDirFor("").shouldGive();
    }

    public void testGuessCacheDir_JarsIgnored() {
        guessCacheDirFor("/data/app/a.b.c.jar").shouldGive();
        guessCacheDirFor("/system/framework/android.test.runner.jar").shouldGive();
    }

    public void testGuessCacheDir_RealWorldExample() {
        String realPath = "/system/framework/android.test.runner.jar:" +
                "/data/app/com.google.android.voicesearch.tests-2.apk:" +
                "/data/app/com.google.android.voicesearch-1.apk";
        guessCacheDirFor(realPath)
                .withNonWriteable("/data/data/com.google.android.voicesearch.tests/cache")
                .shouldGive("/data/data/com.google.android.voicesearch/cache");
    }

    private interface TestCondition {
        TestCondition withNonWriteable(String... files);
        void shouldGive(String... files);
    }

    private TestCondition guessCacheDirFor(final String path) {
        final Set<String> notWriteable = new HashSet<String>();
        return new TestCondition() {
            public void shouldGive(String... files) {
                AppDataDirGuesser guesser = new AppDataDirGuesser() {
                    @Override
                    public boolean isWriteableDirectory(File file) {
                        return !notWriteable.contains(file.getAbsolutePath());
                    }
                };
                File[] results = guesser.guessPath(path);
                assertNotNull("Null results for " + path, results);
                assertEquals("Bad lengths for " + path, files.length, results.length);
                for (int i = 0; i < files.length; ++i) {
                    assertEquals("Element " + i, new File(files[i]), results[i]);
                }
            }

            public TestCondition withNonWriteable(String... files) {
                notWriteable.addAll(Arrays.asList(files));
                return this;
            }
        };
    }
}
