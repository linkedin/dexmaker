/*
 * Copyright (c) 2016 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package com.android.dx.mockito;

import org.mockito.internal.debugging.LocationImpl;
import org.mockito.internal.exceptions.VerificationAwareInvocation;
import org.mockito.internal.invocation.ArgumentsProcessor;
import org.mockito.internal.invocation.MockitoMethod;
import org.mockito.internal.reporting.PrintSettings;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.Location;
import org.mockito.invocation.StubInfo;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.mockito.internal.exceptions.Reporter.cannotCallAbstractRealMethod;

/**
 * {@link Invocation} used when intercepting methods from an method entry hook.
 */
class InterceptedInvocation implements Invocation, VerificationAwareInvocation {
    /** The mocked instance */
    private final Object mock;

    /** The method invoked */
    private final MockitoMethod method;

    /** expanded arguments to the method */
    private final Object[] arguments;

    /** raw arguments to the method */
    private final Object[] rawArguments;

    /** The super method */
    private final SuperMethod superMethod;

    /** sequence number of the invocation (different for each invocation) */
    private final int sequenceNumber;

    /** the location of the invocation (i.e. the stack trace) */
    private final Location location;

    /** Was this invocation {@link #markVerified() marked as verified} */
    private boolean verified;

    /** Should this be {@link #ignoreForVerification()} ignored for verification?} */
    private boolean isIgnoredForVerification;

    /** The stubinfo is this was {@link #markStubbed(StubInfo) markes as stubbed}*/
    private StubInfo stubInfo;

    /**
     * Create a new invocation.
     *
     * @param mock mocked instance
     * @param method method invoked
     * @param arguments arguments to the method
     * @param superMethod super method
     * @param sequenceNumber sequence number of the invocation
     */
    InterceptedInvocation(Object mock, MockitoMethod method, Object[] arguments,
                          SuperMethod superMethod, int sequenceNumber) {
        this.mock = mock;
        this.method = method;
        this.arguments = ArgumentsProcessor.expandArgs(method, arguments);
        this.rawArguments = arguments;
        this.superMethod = superMethod;
        this.sequenceNumber = sequenceNumber;
        location = new LocationImpl();
    }

    @Override
    public boolean isVerified() {
        return verified || isIgnoredForVerification;
    }

    @Override
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public Object[] getRawArguments() {
        return rawArguments;
    }

    @Override
    public Class<?> getRawReturnType() {
        return method.getReturnType();
    }

    @Override
    public void markVerified() {
        verified = true;
    }

    @Override
    public StubInfo stubInfo() {
        return stubInfo;
    }

    @Override
    public void markStubbed(StubInfo stubInfo) {
        this.stubInfo = stubInfo;
    }

    @Override
    public boolean isIgnoredForVerification() {
        return isIgnoredForVerification;
    }

    @Override
    public void ignoreForVerification() {
        isIgnoredForVerification = true;
    }

    @Override
    public Object getMock() {
        return mock;
    }

    @Override
    public Method getMethod() {
        return method.getJavaMethod();
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getArgument(int index) {
        return (T) arguments[index];
    }

    @Override
    public Object callRealMethod() throws Throwable {
        if (!superMethod.isInvokable()) {
            throw cannotCallAbstractRealMethod();
        }
        return superMethod.invoke();
    }

    @Override
    public int hashCode() {
        // TODO SF we need to provide hash code implementation so that there are no unexpected,
        //         slight perf issues
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }
        InterceptedInvocation other = (InterceptedInvocation) o;
        return this.mock.equals(other.mock)
                && this.method.equals(other.method)
                && this.equalArguments(other.arguments);
    }

    private boolean equalArguments(Object[] arguments) {
        return Arrays.equals(arguments, this.arguments);
    }

    @Override
    public String toString() {
        return new PrintSettings().print(ArgumentsProcessor.argumentsToMatchers(getArguments()),
                this);
    }

    interface SuperMethod extends Serializable {
        boolean isInvokable();

        Object invoke() throws Throwable;
    }
}
