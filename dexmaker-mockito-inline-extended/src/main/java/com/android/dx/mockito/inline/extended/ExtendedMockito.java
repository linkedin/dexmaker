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

import org.mockito.Mockito;

/**
 * Mockito extended with ... nothing yet
 * <p>It is possible to use this class for instance mocking too. Hence you can use it as a full
 * replacement for {@link Mockito}.
 * <p>This is a prototype that is intended to eventually be upstreamed into mockito proper. Some
 * APIs might change. All such APIs are annotated with {@link UnstableApi}.
 */
@UnstableApi
public class ExtendedMockito extends Mockito {

}
