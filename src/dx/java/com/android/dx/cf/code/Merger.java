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

package com.android.dx.cf.code;

import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeBearer;

/**
 * Utility methods to merge various frame information.
 */
public final class Merger {
    /**
     * Merges two frame types.
     *
     * @param ft1 {@code non-null;} a frame type
     * @param ft2 {@code non-null;} another frame type
     * @return {@code non-null;} the result of merging the two types
     */
    public static TypeBearer mergeType(TypeBearer ft1, TypeBearer ft2) {
        if ((ft1 == null) || ft1.equals(ft2)) {
            return ft1;
        } else if (ft2 == null) {
            return null;
        } else {
            Type type1 = ft1.getType();
            Type type2 = ft2.getType();

            if (type1 == type2) {
                return type1;
            } else if (type1.isReference() && type2.isReference()) {
                if (type1 == Type.KNOWN_NULL) {
                    /*
                     * A known-null merges with any other reference type to
                     * be that reference type.
                     */
                    return type2;
                } else if (type2 == Type.KNOWN_NULL) {
                    /*
                     * The same as above, but this time it's type2 that's
                     * the known-null.
                     */
                    return type1;
                } else if (type1.isArray() && type2.isArray()) {
                    TypeBearer componentUnion =
                        mergeType(type1.getComponentType(),
                                type2.getComponentType());
                    if (componentUnion == null) {
                        /*
                         * At least one of the types is a primitive type,
                         * so the merged result is just Object.
                         */
                        return Type.OBJECT;
                    }
                    return ((Type) componentUnion).getArrayType();
                } else {
                    /*
                     * All other unequal reference types get merged to be
                     * Object in this phase. This is fine here, but it
                     * won't be the right thing to do in the verifier.
                     */
                    return Type.OBJECT;
                }
            } else if (type1.isIntlike() && type2.isIntlike()) {
                /*
                 * Merging two non-identical int-like types results in
                 * the type int.
                 */
                return Type.INT;
            } else {
                return null;
            }
        }
    }
}
