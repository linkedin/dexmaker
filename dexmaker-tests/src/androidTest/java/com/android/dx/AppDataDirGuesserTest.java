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

package com.android.dx;

import android.os.Build;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class AppDataDirGuesserTest {
    @Test
    public void testGuessCacheDir_SimpleExample() {
        guessCacheDirFor("/data/app/a.b.c.apk").shouldGive("/data/data/a.b.c/cache");
        guessCacheDirFor("/data/app/a.b.c.tests.apk").shouldGive("/data/data/a.b.c.tests/cache");
    }

    @Test
    public void testGuessCacheDir_MultipleResultsSeparatedByColon() {
        guessCacheDirFor("/data/app/a.b.c.apk:/data/app/d.e.f.apk")
                .shouldGive("/data/data/a.b.c/cache", "/data/data/d.e.f/cache");
    }

    @Test
    public void testGuessCacheDir_NotWriteableSkipped() {
        guessCacheDirFor("/data/app/a.b.c.apk:/data/app/d.e.f.apk")
                .withNonWriteable("/data/data/a.b.c/cache")
                .shouldGive("/data/data/d.e.f/cache");
    }

    @Test
    public void testGuessCacheDir_StripHyphenatedSuffixes() {
        guessCacheDirFor("/data/app/a.b.c-2.apk").shouldGive("/data/data/a.b.c/cache");
    }

    @Test
    public void testGuessCacheDir_LeadingAndTrailingColonsIgnored() {
        guessCacheDirFor("/data/app/a.b.c.apk:asdf:").shouldGive("/data/data/a.b.c/cache");
        guessCacheDirFor(":asdf:/data/app/a.b.c.apk").shouldGive("/data/data/a.b.c/cache");
    }

    @Test
    public void testGuessCacheDir_InvalidInputsGiveEmptyArray() {
        guessCacheDirFor("").shouldGive();
    }

    @Test
    public void testGuessCacheDir_JarsIgnored() {
        guessCacheDirFor("/data/app/a.b.c.jar").shouldGive();
        guessCacheDirFor("/system/framework/android.test.runner.jar").shouldGive();
    }

    @Test
    public void testGuessCacheDir_RealWorldExample() {
        String realPath = "/system/framework/android.test.runner.jar:" +
                "/data/app/com.google.android.voicesearch.tests-2.apk:" +
                "/data/app/com.google.android.voicesearch-1.apk";
        guessCacheDirFor(realPath)
                .withNonWriteable("/data/data/com.google.android.voicesearch.tests/cache")
                .shouldGive("/data/data/com.google.android.voicesearch/cache");
    }

    @Test
    public void testSplitPathList() {
        final String[] expected = { "foo", "bar" };
        assertTrue(Arrays.equals(expected, AppDataDirGuesser.splitPathList("foo:bar")));
        assertTrue(Arrays.equals(expected,
                AppDataDirGuesser.splitPathList("dexPath=foo:bar")));
        assertTrue(Arrays.equals(expected,
                AppDataDirGuesser.splitPathList("dexPath=foo:bar,bazPath=bar:bar2")));
    }

    @Test
    public void testPre43PathProcessing() {
        String input = "dalvik.system.PathClassLoader[dexPath=/data/app/abc-1.apk," +
                       "libraryPath=/data/app-lib/abc-1]";
        String processed = AppDataDirGuesser.processClassLoaderString(input);
        assertTrue("dexPath=/data/app/abc-1.apk,libraryPath=/data/app-lib/abc-1".equals(processed));
    }

    @Test
    public void test43PathProcessing() {
        String input = "dalvik.system.PathClassLoader[DexPathList[[zip file " +
                       "\"/data/app/abc-1/base.apk\", zip file \"/data/app/def-1/base.apk\"], " +
                       "nativeLibraryDirectories=[/data/app-lib/abc-1]]]";
        String processed = AppDataDirGuesser.processClassLoaderString(input);
        assertTrue("/data/app/abc-1/base.apk:/data/app/def-1/base.apk".equals(processed));
    }

    @Test
    public void testApiLevel17PlusPathProcessing() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Our processing should work for anything >= Android 4.2.
            String input = getClass().getClassLoader().toString();
            String processed = AppDataDirGuesser.processClassLoaderString(input);
            // TODO: this comment is no longer true now that we run tests on Android instead of vogar
            // A tighter check would be interesting. But vogar doesn't run the tests in a directory
            // recognized by the guesser (usually under /data/local/tmp), so we cannot use the
            // processed result as input to guessPath.
            assertTrue(!input.equals(processed));
        }
    }

    private interface TestCondition {
        TestCondition withNonWriteable(String... files);
        void shouldGive(String... files);
    }

    private TestCondition guessCacheDirFor(final String path) {
        final Set<String> notWriteable = new HashSet<>();
        return new TestCondition() {
            @Override
            public void shouldGive(String... files) {
                AppDataDirGuesser guesser = new AppDataDirGuesser() {
                    @Override
                    public boolean isWriteableDirectory(File file) {
                        return !notWriteable.contains(file.getAbsolutePath());
                    }
                    @Override
                    boolean fileOrDirExists(File file) {
                        return true;
                    }
                };
                File[] results = guesser.guessPath(path);
                assertNotNull("Null results for " + path, results);
                assertEquals("Bad lengths for " + path, files.length, results.length);
                for (int i = 0; i < files.length; ++i) {
                    assertEquals("Element " + i, new File(files[i]), results[i]);
                }
            }

            @Override
            public TestCondition withNonWriteable(String... files) {
                notWriteable.addAll(Arrays.asList(files));
                return this;
            }
        };
    }
}
