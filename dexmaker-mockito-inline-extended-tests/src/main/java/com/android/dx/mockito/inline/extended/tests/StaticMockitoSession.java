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

import android.content.ContentResolver;
import android.provider.Settings;

import org.junit.Test;
import org.mockito.MockitoSession;
import org.mockito.exceptions.misusing.UnnecessaryStubbingException;
import org.mockito.quality.Strictness;

import static android.provider.Settings.Global.DEVICE_NAME;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class StaticMockitoSession {
    @Test
    public void strictUnnecessaryStubbing() throws Exception {
        MockitoSession session = mockitoSession().spyStatic(Settings.Global.class).startMocking();

        // Set up unnecessary stubbing
        doReturn("23").when(() -> Settings.Global.getString(any
                (ContentResolver.class), eq(DEVICE_NAME)));

        try {
            session.finishMocking();
            fail();
        } catch (UnnecessaryStubbingException e) {
            assertTrue("\"" + e.getMessage() + "\" does not contain 'Settings$Global.getString'",
                    e.getMessage().contains("Settings$Global.getString"));
        }
    }

    @Test
    public void lenientUnnecessaryStubbing() throws Exception {
        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(Settings.Global.class).startMocking();

        // Set up unnecessary stubbing
        doReturn("23").when(() -> Settings.Global.getString(any
                (ContentResolver.class), eq(DEVICE_NAME)));

        session.finishMocking();
    }
}
