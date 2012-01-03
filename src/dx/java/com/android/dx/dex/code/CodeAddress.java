/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.dx.dex.code;

import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.SourcePosition;

/**
 * Pseudo-instruction which is used to track an address within a code
 * array. Instances are used for such things as branch targets and
 * exception handler ranges. Its code size is zero, and so instances
 * do not in general directly wind up in any output (either
 * human-oriented or binary file).
 */
public final class CodeAddress extends ZeroSizeInsn {
    /**
     * Constructs an instance. The output address of this instance is initially
     * unknown ({@code -1}).
     *
     * @param position {@code non-null;} source position
     */
    public CodeAddress(SourcePosition position) {
        super(position);
    }

    /** {@inheritDoc} */
    @Override
    public final DalvInsn withRegisters(RegisterSpecList registers) {
        return new CodeAddress(getPosition());
    }

    /** {@inheritDoc} */
    @Override
    protected String argString() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected String listingString0(boolean noteIndices) {
        return "code-address";
    }
}
