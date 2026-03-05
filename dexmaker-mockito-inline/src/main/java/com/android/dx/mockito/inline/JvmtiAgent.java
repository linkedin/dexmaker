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

import android.os.Build;
import android.os.Debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.ProtectionDomain;
import java.util.ArrayList;

import dalvik.system.BaseDexClassLoader;

/**
 * Interface to the native jvmti agent in agent.cc
 */
class JvmtiAgent {
    private static final String AGENT_LIB_NAME = "libdexmakerjvmtiagent.so";

    private static final Object lock = new Object();

    /** Registered byte code transformers */
    private final ArrayList<ClassTransformer> transformers = new ArrayList<>();

    private native void nativeRegisterTransformerHook();

    /**
     * Enable jvmti and load agent.
     *
     * <p><b>If there are more than agent transforming classes the other agent might remove
     * transformations added by this agent.</b>
     *
     * @throws IOException If jvmti could not be enabled or agent could not be loaded
     */
    JvmtiAgent() throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            throw new IOException("Requires API level " + Build.VERSION_CODES.P + ". API level is "
                    + Build.VERSION.SDK_INT);
        }

        ClassLoader cl = JvmtiAgent.class.getClassLoader();
        if (!(cl instanceof BaseDexClassLoader)) {
            throw new IOException("Could not load jvmti plugin as JvmtiAgent class was not loaded "
                    + "by a BaseDexClassLoader");
        }

        String agentPath = resolveAgentPath(cl);
        Debug.attachJvmtiAgent(agentPath, null, cl);
        nativeRegisterTransformerHook();
    }

    /**
     * Resolve the agent library to a path that Debug.attachJvmtiAgent can load.
     *
     * <p>Debug.attachJvmtiAgent rejects paths containing '=' (used as options
     * separator). On Android, app install directories may contain '=' in their
     * path (e.g. /data/app/~~K6yx...A==/pkg-axiN...Q==/). When findLibrary
     * returns such a path, or returns null (extractNativeLibs=false), we copy
     * the library to a temp file.
     */
    private static String resolveAgentPath(ClassLoader cl) throws IOException {
        String path = ((BaseDexClassLoader) cl).findLibrary("dexmakerjvmtiagent");

        if (path != null && !path.contains("=")) {
            return path;
        }

        // Copy the library to a temp file with a clean path.
        File copiedAgent = File.createTempFile("org.mockito.android.agent", ".so");
        copiedAgent.deleteOnExit();

        InputStream is;
        if (path != null) {
            is = new FileInputStream(path);
        } else {
            // findLibrary returned null — try loading from APK resources
            String abi = Build.SUPPORTED_ABIS[0];
            String resourcePath = "lib/" + abi + "/" + AGENT_LIB_NAME;
            is = cl.getResourceAsStream(resourcePath);
            if (is == null) {
                throw new IOException("Could not find " + AGENT_LIB_NAME
                        + " via findLibrary or classloader resources");
            }
        }

        try (OutputStream os = new FileOutputStream(copiedAgent)) {
            byte[] buffer = new byte[64 * 1024];
            int numRead;
            while ((numRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, numRead);
            }
        } finally {
            is.close();
        }

        return copiedAgent.getAbsolutePath();
    }

    private native void nativeUnregisterTransformerHook();

    @Override
    protected void finalize() throws Throwable {
        nativeUnregisterTransformerHook();
    }

    private native static void nativeAppendToBootstrapClassLoaderSearch(String absolutePath);

    /**
     * Append the jar to be bootstrap class load. This makes the classes in the jar behave as if
     * they are loaded from the BCL. E.g. classes from java.lang can now call the classes in the
     * jar.
     *
     * @param jarStream stream of jar to be added
     */
    void appendToBootstrapClassLoaderSearch(InputStream jarStream) throws IOException {
        File jarFile = File.createTempFile("mockito-boot", ".jar");
        jarFile.deleteOnExit();

        byte[] buffer = new byte[64 * 1024];
        try (OutputStream os = new FileOutputStream(jarFile)) {
            while (true) {
                int numRead = jarStream.read(buffer);
                if (numRead == -1) {
                    break;
                }

                os.write(buffer, 0, numRead);
            }
        }

        nativeAppendToBootstrapClassLoaderSearch(jarFile.getAbsolutePath());
    }

    /**
     * Ask the agent to trigger transformation of some classes. This will extract the byte code of
     * the classes and the call back the {@link #addTransformer(ClassTransformer) transformers} for
     * each individual class.
     *
     * @param classes The classes to transform
     *
     * @throws UnmodifiableClassException If one of the classes can not be transformed
     */
    void requestTransformClasses(Class<?>[] classes) throws UnmodifiableClassException {
        synchronized (lock) {
            try {
                nativeRetransformClasses(classes);
            } catch (RuntimeException e) {
                throw new UnmodifiableClassException(e);
            }
        }
    }

    // called by JNI
    @SuppressWarnings("unused")
    public boolean shouldTransform(Class<?> classBeingRedefined) {
        for (ClassTransformer transformer : transformers) {
            if (transformer.shouldTransform(classBeingRedefined)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Register a transformer. These are called for each class when a transformation was triggered
     * via {@link #requestTransformClasses(Class[])}.
     *
     * @param transformer the transformer to add.
     */
    void addTransformer(ClassTransformer transformer) {
        transformers.add(transformer);
    }

    // called by JNI
    @SuppressWarnings("unused")
    public byte[] runTransformers(ClassLoader loader, String className,
                                  Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                  byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] transformedByteCode = classfileBuffer;
        for (ClassTransformer transformer : transformers) {
            transformedByteCode = transformer.transform(classBeingRedefined, transformedByteCode);
        }

        return transformedByteCode;
    }

    private native void nativeRetransformClasses(Class<?>[] classes);
}
