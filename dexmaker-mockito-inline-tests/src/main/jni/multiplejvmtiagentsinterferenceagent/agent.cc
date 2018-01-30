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

#include <jni.h>

#include <cstring>
#include <cstdlib>
#include <sstream>

#include "jvmti.h"

#include <dex_ir.h>
#include <writer.h>
#include <reader.h>

using namespace dex;

namespace com_android_dx_mockito_inline_tests {
    static jvmtiEnv *localJvmtiEnv;

    // Converts a class name to a type descriptor
    // (ex. "java.lang.String" to "Ljava/lang/String;")
    static std::string
    ClassNameToDescriptor(const char* class_name) {
        std::stringstream ss;
        ss << "L";
        for (auto p = class_name; *p != '\0'; ++p) {
            ss << (*p == '.' ? '/' : *p);
        }
        ss << ";";
        return ss.str();
    }

    static void
    Transform(jvmtiEnv *jvmti_env,
              JNIEnv *env,
              jclass classBeingRedefined,
              jobject loader,
              const char *name,
              jobject protectionDomain,
              jint classDataLen,
              const unsigned char *classData,
              jint *newClassDataLen,
              unsigned char **newClassData) {
        // Isolate byte code of class class. This is needed as Android usually gives us more
        // than the class we need.
        // Then just return the isolated byte code without modification.
        Reader reader(classData, (size_t) classDataLen);

        u4 index = reader.FindClassIndex(ClassNameToDescriptor(name).c_str());
        reader.CreateClassIr(index);
        std::shared_ptr<ir::DexFile> ir = reader.GetIr();

        class Allocator : public Writer::Allocator {
            jvmtiEnv *jvmti_env;

        public:
            Allocator(jvmtiEnv *jvmti_env) : Writer::Allocator(), jvmti_env(jvmti_env) {
            }

            virtual void *Allocate(size_t size) {
                unsigned char *mem;
                jvmti_env->Allocate(size, &mem);
                return mem;
            }

            virtual void Free(void *ptr) { ::free(ptr); }
        };

        Allocator allocator(jvmti_env);
        Writer writer(ir);
        size_t newClassLen;
        *newClassData = writer.CreateImage(&allocator, &newClassLen);
        *newClassDataLen = (jint) newClassLen;
    }

    // Initializes the agent
    extern "C" jint Agent_OnAttach(JavaVM *vm,
                                   char *options,
                                   void *reserved) {
        jint jvmError = vm->GetEnv(reinterpret_cast<void **>(&localJvmtiEnv), JVMTI_VERSION_1_2);
        if (jvmError != JNI_OK) {
            return jvmError;
        }

        jvmtiCapabilities caps;
        memset(&caps, 0, sizeof(caps));
        caps.can_retransform_classes = 1;

        jvmtiError error = localJvmtiEnv->AddCapabilities(&caps);
        if (error != JVMTI_ERROR_NONE) {
            return error;
        }

        jvmtiEventCallbacks cb;
        memset(&cb, 0, sizeof(cb));
        cb.ClassFileLoadHook = Transform;

        error = localJvmtiEnv->SetEventCallbacks(&cb, sizeof(cb));
        if (error != JVMTI_ERROR_NONE) {
            return error;
        }

        error = localJvmtiEnv->SetEventNotificationMode(JVMTI_ENABLE,
                                                        JVMTI_EVENT_CLASS_FILE_LOAD_HOOK,
                                                        NULL);
        if (error != JVMTI_ERROR_NONE) {
            return error;
        }

        return JVMTI_ERROR_NONE;
    }


    // Triggers retransformation of classes via this file's Transform method
    extern "C" JNIEXPORT jint JNICALL
    Java_com_android_dx_mockito_inline_tests_MultipleJvmtiAgentsInterference_nativeRetransformClasses(
            JNIEnv *env,
            jobject thiz,
            jobjectArray classes) {
        jsize numTransformedClasses = env->GetArrayLength(classes);
        jclass *transformedClasses = (jclass *) malloc(numTransformedClasses * sizeof(jclass));
        for (int i = 0; i < numTransformedClasses; i++) {
            transformedClasses[i] = (jclass) env->NewGlobalRef(env->GetObjectArrayElement(classes,
                                                                                          i));
        }

        jvmtiError error = localJvmtiEnv->RetransformClasses(numTransformedClasses,
                                                             transformedClasses);

        for (int i = 0; i < numTransformedClasses; i++) {
            env->DeleteGlobalRef(transformedClasses[i]);
        }
        free(transformedClasses);

        return error;
    }

    // Disable hook to not slow down test
    extern "C" JNIEXPORT jint JNICALL
    Java_com_android_dx_mockito_inline_tests_MultipleJvmtiAgentsInterference_disableRetransformHook(
            JNIEnv *env,
            jclass ignored) {
        return localJvmtiEnv->SetEventNotificationMode(JVMTI_DISABLE,
                                                       JVMTI_EVENT_CLASS_FILE_LOAD_HOOK,
                                                       NULL);

    }
}