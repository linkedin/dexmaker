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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Uses heuristics to guess the application's private data directory.
 */
class AppDataDirGuesser {
    public File guess() {
        try {
            ClassLoader classLoader = guessSuitableClassLoader();
            // Check that we have an instance of the PathClassLoader.
            Class<?> clazz = Class.forName("dalvik.system.PathClassLoader");
            clazz.cast(classLoader);
            // Use the toString() method to calculate the data directory.
            String pathFromThisClassLoader = getPathFromThisClassLoader(classLoader, clazz);
            File[] results = guessPath(pathFromThisClassLoader);
            if (results.length > 0) {
                return results[0];
            }
        } catch (ClassCastException ignored) {
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    private ClassLoader guessSuitableClassLoader() {
        return AppDataDirGuesser.class.getClassLoader();
    }

    private String getPathFromThisClassLoader(ClassLoader classLoader,
            Class<?> pathClassLoaderClass) {
        // Prior to ICS, we can simply read the "path" field of the
        // PathClassLoader.
        try {
            Field pathField = pathClassLoaderClass.getDeclaredField("path");
            pathField.setAccessible(true);
            return (String) pathField.get(classLoader);
        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (ClassCastException ignored) {
        }

        // Parsing toString() method: yuck.  But no other way to get the path.
        // Strip out the bit between angle brackets, that's our path.
        String result = classLoader.toString();
        int index = result.lastIndexOf('[');
        result = (index == -1) ? result : result.substring(index + 1);
        index = result.indexOf(']');
        return (index == -1) ? result : result.substring(0, index);
    }

    File[] guessPath(String input) {
        List<File> results = new ArrayList<File>();
        for (String potential : input.split(":")) {
            if (!potential.startsWith("/data/app/")) {
                continue;
            }
            int start = "/data/app/".length();
            int end = potential.lastIndexOf(".apk");
            if (end != potential.length() - 4) {
                continue;
            }
            int dash = potential.indexOf("-");
            if (dash != -1) {
                end = dash;
            }
            String packageName = potential.substring(start, end);
            File dataDir = new File("/data/data/" + packageName);
            if (isWriteableDirectory(dataDir)) {
                File cacheDir = new File(dataDir, "cache");
                // The cache directory might not exist -- create if necessary
                if (fileOrDirExists(cacheDir) || cacheDir.mkdir()) {
                    if (isWriteableDirectory(cacheDir)) {
                        results.add(cacheDir);
                    }
                }
            }
        }
        return results.toArray(new File[results.size()]);
    }

    boolean fileOrDirExists(File file) {
        return file.exists();
    }

    boolean isWriteableDirectory(File file) {
        return file.isDirectory() && file.canWrite();
    }
}
