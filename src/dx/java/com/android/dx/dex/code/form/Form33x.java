/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.dx.dex.code.form;

import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.dex.code.SimpleInsn;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.util.AnnotatedOutput;

import java.util.BitSet;

/**
 * Instruction format {@code 33x}. See the instruction format spec
 * for details.
 */
public final class Form33x extends InsnFormat {
    /** {@code non-null;} unique instance of this class */
    public static final InsnFormat THE_ONE = new Form33x();

    /**
     * Constructs an instance. This class is not publicly
     * instantiable. Use {@link #THE_ONE}.
     */
    private Form33x() {
        // This space intentionally left blank.
    }

    /** {@inheritDoc} */
    @Override
    public String insnArgString(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        return regs.get(0).regString() + ", " + regs.get(1).regString() +
            ", " + regs.get(2).regString();
    }

    /** {@inheritDoc} */
    @Override
    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        // This format has no comment.
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public int codeSize() {
        return 3;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(DalvInsn insn) {
        if (! ALLOW_EXTENDED_OPCODES) {
            return false;
        }

        RegisterSpecList regs = insn.getRegisters();

        return (insn instanceof SimpleInsn) &&
            (regs.size() == 3) &&
            unsignedFitsInByte(regs.get(0).getReg()) &&
            unsignedFitsInByte(regs.get(1).getReg()) &&
            unsignedFitsInShort(regs.get(2).getReg());
    }

    /** {@inheritDoc} */
    @Override
    public BitSet compatibleRegs(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        BitSet bits = new BitSet(3);

        bits.set(0, unsignedFitsInByte(regs.get(0).getReg()));
        bits.set(1, unsignedFitsInByte(regs.get(1).getReg()));
        bits.set(2, unsignedFitsInShort(regs.get(2).getReg()));
        return bits;
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        write(out,
                opcodeUnit(insn),
                codeUnit(regs.get(0).getReg(), regs.get(1).getReg()),
                (short) regs.get(2).getReg());
    }
}
