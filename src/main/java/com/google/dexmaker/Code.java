/*
 * Copyright (C) 2011 The Android Open Source Project
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

import com.android.dx.rop.code.BasicBlockList;
import com.android.dx.rop.code.Insn;
import com.android.dx.rop.code.PlainCstInsn;
import com.android.dx.rop.code.PlainInsn;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.Rop;
import static com.android.dx.rop.code.Rop.BRANCH_GOTO;
import static com.android.dx.rop.code.Rop.BRANCH_NONE;
import static com.android.dx.rop.code.Rop.BRANCH_RETURN;
import com.android.dx.rop.code.Rops;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.rop.code.ThrowingCstInsn;
import com.android.dx.rop.code.ThrowingInsn;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.type.StdTypeList;
import static com.android.dx.rop.type.Type.BT_BYTE;
import static com.android.dx.rop.type.Type.BT_CHAR;
import static com.android.dx.rop.type.Type.BT_INT;
import static com.android.dx.rop.type.Type.BT_SHORT;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Builds a sequence of instructions.
 */
/*
 * TODO: documentation:
 * UnaryOp, BinaryOp
 * Try/catch
 * Arrays
 * Comparisons
 * Locals
 * Fields
 * Type: Instanceof, cast, typeCast, numericCast
 * Invoke
 * Jump
 * Constants
 * NewInstance
 * Return
 */
public final class Code {
    private final MethodId<?, ?> method;
    /**
     * All allocated labels. Although the order of the labels in this list
     * shouldn't impact behavior, it is used to determine basic block indices.
     */
    private final List<Label> labels = new ArrayList<Label>();

    /**
     * The label currently receiving instructions. This is null if the most
     * recent instruction was a return or goto.
     */
    private Label currentLabel;

    /** true once we've fixed the positions of the parameter registers */
    private boolean localsInitialized;

    private final Local<?> thisLocal;

    /**
     * The parameters on this method. If this is non-static, the first parameter
     * is 'thisLocal' and we have to offset the user's indices by one.
     */
    private final List<Local<?>> parameters = new ArrayList<Local<?>>();
    private final List<Local<?>> locals = new ArrayList<Local<?>>();
    private SourcePosition sourcePosition = SourcePosition.NO_INFO;
    private final List<TypeId<?>> catchTypes = new ArrayList<TypeId<?>>();
    private final List<Label> catchLabels = new ArrayList<Label>();
    private StdTypeList catches = StdTypeList.EMPTY;

    Code(DexMaker.MethodDeclaration methodDeclaration) {
        this.method = methodDeclaration.method;
        if (methodDeclaration.isStatic()) {
            thisLocal = null;
        } else {
            thisLocal = Local.get(this, method.declaringType);
            parameters.add(thisLocal);
        }
        for (TypeId<?> parameter : method.parameters.types) {
            parameters.add(Local.get(this, parameter));
        }
        this.currentLabel = new Label();
        adopt(this.currentLabel);
        this.currentLabel.marked = true;
    }

    public <T> Local<T> newLocal(TypeId<T> type) {
        if (localsInitialized) {
            throw new IllegalStateException("Cannot allocate locals after adding instructions");
        }
        Local<T> result = Local.get(this, type);
        locals.add(result);
        return result;
    }

    public <T> Local<T> getParameter(int index, TypeId<T> type) {
        if (thisLocal != null) {
            index++; // adjust for the hidden 'this' parameter
        }
        return coerce(parameters.get(index), type);
    }

    public <T> Local<T> getThis(TypeId<T> type) {
        if (thisLocal == null) {
            throw new IllegalStateException("static methods cannot access 'this'");
        }
        return coerce(thisLocal, type);
    }

    @SuppressWarnings("unchecked") // guarded by an equals check
    private <T> Local<T> coerce(Local<?> local, TypeId<T> expectedType) {
        if (!local.type.equals(expectedType)) {
            throw new IllegalArgumentException(
                    "requested " + expectedType + " but was " + local.type);
        }
        return (Local<T>) local;
    }

    /**
     * Assigns registers to locals. From the spec:
     *  "the N arguments to a method land in the last N registers of the
     *   method's invocation frame, in order. Wide arguments consume two
     *   registers. Instance methods are passed a this reference as their
     *   first argument."
     *
     * In addition to assigning registers to each of the locals, this creates
     * instructions to move parameters into their initial registers. These
     * instructions are inserted before the code's first real instruction.
     */
    void initializeLocals() {
        if (localsInitialized) {
            throw new AssertionError();
        }
        localsInitialized = true;

        int reg = 0;
        for (Local<?> local : locals) {
            reg += local.initialize(reg);
        }
        int firstParamReg = reg;
        List<Insn> moveParameterInstructions = new ArrayList<Insn>();
        for (Local<?> local : parameters) {
            CstInteger paramConstant = CstInteger.make(reg - firstParamReg);
            reg += local.initialize(reg);
            moveParameterInstructions.add(new PlainCstInsn(Rops.opMoveParam(local.type.ropType),
                    sourcePosition, local.spec(), RegisterSpecList.EMPTY, paramConstant));
        }
        labels.get(0).instructions.addAll(0, moveParameterInstructions);
    }

    /**
     * Returns the number of registers to hold the parameters. This includes the
     * 'this' parameter if it exists.
     */
    int paramSize() {
        int result = 0;
        for (Local<?> local : parameters) {
            result += local.size();
        }
        return result;
    }

    // labels

    /**
     * Assigns {@code target} to this code.
     */
    private void adopt(Label target) {
        if (target.code == this) {
            return; // already adopted
        }
        if (target.code != null) {
            throw new IllegalArgumentException("Cannot adopt label; it belongs to another Code");
        }
        target.code = this;
        labels.add(target);
    }

    /**
     * Start defining instructions for the named label.
     */
    public void mark(Label label) {
        adopt(label);
        if (label.marked) {
            throw new IllegalStateException("already marked");
        }
        label.marked = true;
        if (currentLabel != null) {
            jump(label); // blocks must end with a branch, return or throw
        }
        currentLabel = label;
    }

    public void jump(Label target) {
        adopt(target);
        addInstruction(new PlainInsn(Rops.GOTO, sourcePosition, null, RegisterSpecList.EMPTY),
                target);
    }

    public void addCatchClause(TypeId<?> throwable, Label catchClause) {
        if (catchTypes.contains(throwable)) {
            throw new IllegalArgumentException("Already caught: " + throwable);
        }
        adopt(catchClause);
        catchTypes.add(throwable);
        catches = toTypeList(catchTypes);
        catchLabels.add(catchClause);
    }

    public Label removeCatchClause(TypeId<?> throwable) {
        int index = catchTypes.indexOf(throwable);
        if (index == -1) {
            throw new IllegalArgumentException("No catch clause: " + throwable);
        }
        catchTypes.remove(index);
        catches = toTypeList(catchTypes);
        return catchLabels.remove(index);
    }

    public void throwValue(Local<?> throwable) {
        addInstruction(new ThrowingInsn(Rops.THROW, sourcePosition,
                RegisterSpecList.make(throwable.spec()), catches));
    }

    private StdTypeList toTypeList(List<TypeId<?>> types) {
        StdTypeList result = new StdTypeList(types.size());
        for (int i = 0; i < types.size(); i++) {
            result.set(i, types.get(i).ropType);
        }
        return result;
    }

    private void addInstruction(Insn insn) {
        addInstruction(insn, null);
    }

    /**
     * @param branch the branches to follow; interpretation depends on the
     *     instruction's branchingness.
     */
    private void addInstruction(Insn insn, Label branch) {
        if (currentLabel == null || !currentLabel.marked) {
            throw new IllegalStateException("no current label");
        }
        currentLabel.instructions.add(insn);

        switch (insn.getOpcode().getBranchingness()) {
        case BRANCH_NONE:
            if (branch != null) {
                throw new IllegalArgumentException("unexpected branch: " + branch);
            }
            return;

        case BRANCH_RETURN:
            if (branch != null) {
                throw new IllegalArgumentException("unexpected branch: " + branch);
            }
            currentLabel = null;
            break;

        case BRANCH_GOTO:
            if (branch == null) {
                throw new IllegalArgumentException("branch == null");
            }
            currentLabel.primarySuccessor = branch;
            currentLabel = null;
            break;

        case Rop.BRANCH_IF:
            if (branch == null) {
                throw new IllegalArgumentException("branch == null");
            }
            splitCurrentLabel(branch, Collections.<Label>emptyList());
            break;

        case Rop.BRANCH_THROW:
            if (branch != null) {
                throw new IllegalArgumentException("unexpected branch: " + branch);
            }
            splitCurrentLabel(null, new ArrayList<Label>(catchLabels));
            break;

        default:
            throw new IllegalArgumentException();
        }
    }

    /**
     * Closes the current label and starts a new one.
     *
     * @param catchLabels an immutable list of catch labels
     */
    private void splitCurrentLabel(Label alternateSuccessor, List<Label> catchLabels) {
        Label newLabel = new Label();
        adopt(newLabel);
        currentLabel.primarySuccessor = newLabel;
        currentLabel.alternateSuccessor = alternateSuccessor;
        currentLabel.catchLabels = catchLabels;
        currentLabel = newLabel;
        currentLabel.marked = true;
    }

    // instructions: constants

    public <T> void loadConstant(Local<T> target, T value) {
        Rop rop = value == null
                ? Rops.CONST_OBJECT_NOTHROW
                : Rops.opConst(target.type.ropType);
        if (rop.getBranchingness() == BRANCH_NONE) {
            addInstruction(new PlainCstInsn(rop, sourcePosition, target.spec(),
                    RegisterSpecList.EMPTY, Constants.getConstant(value)));
        } else {
            addInstruction(new ThrowingCstInsn(rop, sourcePosition,
                    RegisterSpecList.EMPTY, catches, Constants.getConstant(value)));
            moveResult(target, true);
        }
    }

    // instructions: unary

    public <T> void op(UnaryOp op, Local<T> target, Local<T> source) {
        unary(op.rop(source.type), target, source);
    }

    public void numericCast(Local<?> target, Local<?> source) {
        // TODO: overload the cast op?
        unary(getCastRop(source.type.ropType, target.type.ropType), target, source);
    }

    private Rop getCastRop(com.android.dx.rop.type.Type sourceType,
            com.android.dx.rop.type.Type targetType) {
        if (sourceType.getBasicType() == BT_INT) {
            switch (targetType.getBasicType()) {
            case BT_SHORT:
                return Rops.TO_SHORT;
            case BT_CHAR:
                return Rops.TO_CHAR;
            case BT_BYTE:
                return Rops.TO_BYTE;
            }
        }
        return Rops.opConv(targetType, sourceType);
    }

    private void unary(Rop rop, Local<?> target, Local<?> source) {
        addInstruction(new PlainInsn(rop, sourcePosition, target.spec(), source.spec()));
    }

    // instructions: binary

    public <T> void op(BinaryOp op, Local<T> target, Local<T> a, Local<T> b) {
        Rop rop = op.rop(StdTypeList.make(a.type.ropType, b.type.ropType));
        RegisterSpecList sources = RegisterSpecList.make(a.spec(), b.spec());

        if (rop.getBranchingness() == BRANCH_NONE) {
            addInstruction(new PlainInsn(rop, sourcePosition, target.spec(), sources));
        } else {
            addInstruction(new ThrowingInsn(rop, sourcePosition, sources, catches));
            moveResult(target, true);
        }
    }

    // instructions: branches

    /**
     * Compare ints. If the comparison is true, execution jumps to {@code
     * trueLabel}. If it is false, execution continues to the next instruction.
     */
    public <T> void compare(Comparison comparison, Label trueLabel, Local<T> a, Local<T> b) {
        adopt(trueLabel);
        Rop rop = comparison.rop(StdTypeList.make(a.type.ropType, b.type.ropType));
        addInstruction(new PlainInsn(rop, sourcePosition, null,
                RegisterSpecList.make(a.spec(), b.spec())), trueLabel);
    }

    /**
     * Compare floats or doubles.
     */
    public <T extends Number> void compare(Local<Integer> target, Local<T> a, Local<T> b,
            int nanValue) {
        Rop rop;
        if (nanValue == 1) {
            rop = Rops.opCmpg(a.type.ropType);
        } else if (nanValue == -1) {
            rop = Rops.opCmpl(a.type.ropType);
        } else {
            throw new IllegalArgumentException("expected 1 or -1 but was " + nanValue);
        }
        addInstruction(new PlainInsn(rop, sourcePosition, target.spec(),
                RegisterSpecList.make(a.spec(), b.spec())));
    }

    /**
     * Compare longs.
     */
    public <T> void compare(Local<?> target, Local<T> a, Local<T> b) {
        addInstruction(new PlainInsn(Rops.CMPL_LONG, sourcePosition, target.spec(),
                RegisterSpecList.make(a.spec(), b.spec())));
    }

    // instructions: fields

    public <D, V> void iget(FieldId<D, V> fieldId, Local<V> target, Local<D> instance) {
        addInstruction(new ThrowingCstInsn(Rops.opGetField(target.type.ropType), sourcePosition,
                RegisterSpecList.make(instance.spec()), catches, fieldId.constant));
        moveResult(target, true);
    }

    public <D, V> void iput(FieldId<D, V> fieldId, Local<D> instance, Local<V> source) {
        addInstruction(new ThrowingCstInsn(Rops.opPutField(source.type.ropType), sourcePosition,
                RegisterSpecList.make(source.spec(), instance.spec()), catches, fieldId.constant));
    }

    public <V> void sget(FieldId<?, V> fieldId, Local<V> target) {
        addInstruction(new ThrowingCstInsn(Rops.opGetStatic(target.type.ropType), sourcePosition,
                RegisterSpecList.EMPTY, catches, fieldId.constant));
        moveResult(target, true);
    }

    public <V> void sput(FieldId<?, V> fieldId, Local<V> source) {
        addInstruction(new ThrowingCstInsn(Rops.opPutStatic(source.type.ropType), sourcePosition,
                RegisterSpecList.make(source.spec()), catches, fieldId.constant));
    }

    // instructions: invoke

    public <T> void newInstance(Local<T> target, MethodId<T, Void> constructor, Local<?>... args) {
        if (target == null) {
            throw new IllegalArgumentException();
        }
        addInstruction(new ThrowingCstInsn(Rops.NEW_INSTANCE, sourcePosition,
                RegisterSpecList.EMPTY, catches, constructor.declaringType.constant));
        moveResult(target, true);
        invokeDirect(constructor, null, target, args);
    }

    public <R> void invokeStatic(MethodId<?, R> method, Local<? super R> target, Local<?>... args) {
        invoke(Rops.opInvokeStatic(method.prototype(true)), method, target, null, args);
    }

    public <D, R> void invokeVirtual(MethodId<D, R> method, Local<? super R> target,
            Local<? extends D> object, Local<?>... args) {
        invoke(Rops.opInvokeVirtual(method.prototype(true)), method, target, object, args);
    }

    public <D, R> void invokeDirect(MethodId<D, R> method, Local<? super R> target,
            Local<? extends D> object, Local<?>... args) {
        invoke(Rops.opInvokeDirect(method.prototype(true)), method, target, object, args);
    }

    public <D, R> void invokeSuper(MethodId<D, R> method, Local<? super R> target,
            Local<? extends D> object, Local<?>... args) {
        invoke(Rops.opInvokeSuper(method.prototype(true)), method, target, object, args);
    }

    public <D, R> void invokeInterface(MethodId<D, R> method, Local<? super R> target,
            Local<? extends D> object, Local<?>... args) {
        invoke(Rops.opInvokeInterface(method.prototype(true)), method, target, object, args);
    }

    private <D, R> void invoke(Rop rop, MethodId<D, R> method, Local<? super R> target,
            Local<? extends D> object, Local<?>... args) {
        addInstruction(new ThrowingCstInsn(rop, sourcePosition, concatenate(object, args),
                catches, method.constant));
        if (target != null) {
            moveResult(target, false);
        }
    }

    // instructions: types

    public void instanceOfType(Local<?> target, Local<?> source, TypeId<?> type) {
        addInstruction(new ThrowingCstInsn(Rops.INSTANCE_OF, sourcePosition,
                RegisterSpecList.make(source.spec()), catches, type.constant));
        moveResult(target, true);
    }

    public void typeCast(Local<?> target, Local<?> source) {
        addInstruction(new ThrowingCstInsn(Rops.CHECK_CAST, sourcePosition,
                RegisterSpecList.make(source.spec()), catches, target.type.constant));
        moveResult(target, true);
    }

    // instructions: arrays

    public <T> void arrayLength(Local<Integer> target, Local<T> array) {
        addInstruction(new ThrowingInsn(Rops.ARRAY_LENGTH, sourcePosition,
                RegisterSpecList.make(array.spec()), catches));
        moveResult(target, true);
    }

    public <T> void newArray(Local<T> target, Local<Integer> length) {
        addInstruction(new ThrowingCstInsn(Rops.opNewArray(target.type.ropType), sourcePosition,
                RegisterSpecList.make(length.spec()), catches, target.type.constant));
        moveResult(target, true);
    }

    public void aget(Local<?> target, Local<?> array, Local<Integer> index) {
        addInstruction(new ThrowingInsn(Rops.opAget(target.type.ropType), sourcePosition,
                RegisterSpecList.make(array.spec(), index.spec()), catches));
        moveResult(target, true);
    }

    public void aput(Local<?> array, Local<Integer> index, Local<?> source) {
        addInstruction(new ThrowingInsn(Rops.opAput(source.type.ropType), sourcePosition,
                RegisterSpecList.make(source.spec(), array.spec(), index.spec()), catches));
    }

    // instructions: return

    public void returnVoid() {
        if (!method.returnType.equals(TypeId.VOID)) {
            throw new IllegalArgumentException("declared " + method.returnType
                    + " but returned void");
        }
        addInstruction(new PlainInsn(Rops.RETURN_VOID, sourcePosition, null,
                RegisterSpecList.EMPTY));
    }

    public void returnValue(Local<?> result) {
        if (!result.type.equals(method.returnType)) {
            // TODO: this is probably too strict.
            throw new IllegalArgumentException("declared " + method.returnType
                    + " but returned " + result.type);
        }
        addInstruction(new PlainInsn(Rops.opReturn(result.type.ropType), sourcePosition,
                null, RegisterSpecList.make(result.spec())));
    }

    private void moveResult(Local<?> target, boolean afterNonInvokeThrowingInsn) {
        Rop rop = afterNonInvokeThrowingInsn
                ? Rops.opMoveResultPseudo(target.type.ropType)
                : Rops.opMoveResult(target.type.ropType);
        addInstruction(new PlainInsn(rop, sourcePosition, target.spec(), RegisterSpecList.EMPTY));
    }

    // produce BasicBlocks for dex

    BasicBlockList toBasicBlocks() {
        if (!localsInitialized) {
            initializeLocals();
        }

        cleanUpLabels();

        BasicBlockList result = new BasicBlockList(labels.size());
        for (int i = 0; i < labels.size(); i++) {
            result.set(i, labels.get(i).toBasicBlock());
        }
        return result;
    }

    /**
     * Removes empty labels and assigns IDs to non-empty labels.
     */
    private void cleanUpLabels() {
        int id = 0;
        for (Iterator<Label> i = labels.iterator(); i.hasNext();) {
            Label label = i.next();
            if (label.isEmpty()) {
                i.remove();
            } else {
                label.compact();
                label.id = id++;
            }
        }
    }

    private static RegisterSpecList concatenate(Local<?> first, Local<?>[] rest) {
        int offset = (first != null) ? 1 : 0;
        RegisterSpecList result = new RegisterSpecList(offset + rest.length);
        if (first != null) {
            result.set(0, first.spec());
        }
        for (int i = 0; i < rest.length; i++) {
            result.set(i + offset, rest[i].spec());
        }
        return result;
    }
}
