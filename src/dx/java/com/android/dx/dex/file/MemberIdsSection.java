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

package com.android.dx.dex.file;

import com.android.dx.util.DexException;

/**
 * Member (field or method) refs list section of a {@code .dex} file.
 */
public abstract class MemberIdsSection extends UniformItemSection {
    /** The largest addressable member is 0xffff, in the dex spec as field@CCCC or meth@CCCC. */
    private static final int MAX_MEMBERS = 0x10000;

    /**
     * Constructs an instance. The file offset is initially unknown.
     *
     * @param name {@code null-ok;} the name of this instance, for annotation
     * purposes
     * @param file {@code non-null;} file that this instance is part of
     */
    public MemberIdsSection(String name, DexFile file) {
        super(name, file, 4);
    }

    /** {@inheritDoc} */
    @Override
    protected void orderItems() {
        int idx = 0;

        if (items().size() > MAX_MEMBERS) {
            String memberType = this instanceof MethodIdsSection ? "methods" : "fields";
            throw new DexException("Too many " + memberType + ": " + items().size()
                    + "; max is " + MAX_MEMBERS);
        }

        for (Object i : items()) {
            ((MemberIdItem) i).setIndex(idx);
            idx++;
        }
    }
}
