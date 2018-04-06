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

package com.android.dx.mockito.inline.extended;

import java.util.function.Supplier;

/**
 * Data class for a class and the way to create the marker object for the class. As all
 * invocations are routed to the marker the way we create the marker also determines the other
 * properties of the mock.
 */
class StaticMocking<T> {
    final Class<T> clazz;
    final Supplier<T> markerSupplier;

    StaticMocking(Class<T> clazz, Supplier<T> markerSupplier) {
        this.clazz = clazz;
        this.markerSupplier = markerSupplier;
    }
}
