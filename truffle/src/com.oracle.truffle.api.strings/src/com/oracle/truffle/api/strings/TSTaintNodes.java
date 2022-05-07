/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.api.strings;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;

import java.util.Arrays;

import static com.oracle.truffle.api.strings.AbstractTruffleString.LazyConcat;
import static com.oracle.truffle.api.strings.AbstractTruffleString.TaintedString;
import static com.oracle.truffle.api.strings.AbstractTruffleString.boundsCheckRegionI;

/**
 * Collection of taint tracking related {@link Node}s.
 */
public class TSTaintNodes {

    private static UnsupportedOperationException mutableTruffleStringsNotSupported() {
        return new UnsupportedOperationException("MutableTruffleStrings are not supported");
    }

    @GenerateUncached
    public abstract static class ConcatTaintArrayNode extends Node {

        ConcatTaintArrayNode() { }

        /**
         * Concatenates the taint arrays, returning the combined taint.
         * If both arrays are {@code null} or contains only {@code null}, this method will return {@code null}.
         */
        public abstract Object[] execute(AbstractTruffleString a, AbstractTruffleString b);

        @SuppressWarnings("unused")
        @Specialization(guards = { "isTaintedNodeA.execute(a)", "isTaintedNodeB.execute(b)" })
        static Object[] concatBothTainted(AbstractTruffleString a, AbstractTruffleString b,
                                          @Cached IsTaintedNode isTaintedNodeA,
                                          @Cached IsTaintedNode isTaintedNodeB,
                                          @Cached GetTaintNode getTaintNodeA,
                                          @Cached GetTaintNode getTaintNodeB,
                                          @Cached TStringInternalNodes.GetCodePointLengthNode getCodePointLengthNodeA,
                                          @Cached TStringInternalNodes.GetCodePointLengthNode getCodePointLengthNodeB) {
            final int lengthA = getCodePointLengthNodeA.execute(a);
            final int lengthB = getCodePointLengthNodeB.execute(b);
            final Object[] taint = new Object[lengthA + lengthB];
            System.arraycopy(getTaintNodeA.execute(a), 0, taint, 0, lengthA);
            System.arraycopy(getTaintNodeB.execute(b), 0, taint, lengthA, lengthB);
            return taint;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = { "isTaintedNodeA.execute(a)", "!isTaintedNodeB.execute(b)" })
        static Object[] concatATainted(AbstractTruffleString a, AbstractTruffleString b,
                                       @Cached IsTaintedNode isTaintedNodeA,
                                       @Cached IsTaintedNode isTaintedNodeB,
                                       @Cached GetTaintNode getTaintNodeA,
                                       @Cached TStringInternalNodes.GetCodePointLengthNode getCodePointLengthNodeA,
                                       @Cached TStringInternalNodes.GetCodePointLengthNode getCodePointLengthNodeB) {
            final int lengthA = getCodePointLengthNodeA.execute(a);
            final int lengthB = getCodePointLengthNodeB.execute(b);
            final Object[] taint = new Object[lengthA + lengthB];
            System.arraycopy(getTaintNodeA.execute(a), 0, taint, 0, lengthA);
            Arrays.fill(taint, lengthA, taint.length, null);
            return taint;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = { "!isTaintedNodeA.execute(a)", "isTaintedNodeB.execute(b)" })
        static Object[] concatBTainted(AbstractTruffleString a, AbstractTruffleString b,
                                       @Cached IsTaintedNode isTaintedNodeA,
                                       @Cached IsTaintedNode isTaintedNodeB,
                                       @Cached GetTaintNode getTaintNodeB,
                                       @Cached TStringInternalNodes.GetCodePointLengthNode getCodePointLengthNodeA,
                                       @Cached TStringInternalNodes.GetCodePointLengthNode getCodePointLengthNodeB) {
            final int lengthA = getCodePointLengthNodeA.execute(a);
            final int lengthB = getCodePointLengthNodeB.execute(b);
            final Object[] taint = new Object[lengthA + lengthB];
            Arrays.fill(taint, 0, lengthA, null);
            System.arraycopy(getTaintNodeB.execute(b), 0, taint, lengthA, lengthB);
            return taint;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = { "!isTaintedNodeA.execute(a)", "!isTaintedNodeB.execute(b)" })
        static Object[] concatNoneTainted(AbstractTruffleString a, AbstractTruffleString b,
                                          @Cached IsTaintedNode isTaintedNodeA,
                                          @Cached IsTaintedNode isTaintedNodeB) {
            return null;
        }

        static ConcatTaintArrayNode getUncached() {
            return TSTaintNodesFactory.ConcatTaintArrayNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class CopyTaintArrayNode extends Node {

        CopyTaintArrayNode() { }

        /**
         * Copies the given {@code taint} array into a new array.
         * The copy is only a shallow one.
         * This method will return {@code null} is the array is not tainted.
         */
        public abstract Object[] execute(Object[] taint);

        @Specialization(guards = "!isArrayTaintedNode.execute(taint)")
        static Object[] unTaintedCopy(Object[] taint,
                                      @Cached IsArrayTaintedNode isArrayTaintedNode) {
            return null;
        }

        @Specialization(guards = "isArrayTaintedNode.execute(taint)")
        static Object[] taintedCopy(Object[] taint,
                                    @Cached IsArrayTaintedNode isArrayTaintedNode) {
            return Arrays.copyOf(taint, taint.length);
        }

        static CopyTaintArrayNode getUncached() {
            return TSTaintNodesFactory.CopyTaintArrayNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class SubTaintArrayNode extends Node {

        SubTaintArrayNode() { }

        /**
         * Creates a subTaint array of the given taint array,
         * which ranges from {@code from} inclusive and {@code to} exclusive.
         * This method will return {@code null} if the array is not tainted
         * in the range {@code from - to}.
         * The sub array is only a shallow copy.
         */
        public abstract Object[] execute(Object[] taint, int from, int to);

        @Specialization(guards = "!isSubArrayTaintedNode.execute(taint, from, to)")
        static Object[] subTaintUntainted(Object[] taint, int from, int to,
                                          @Cached IsSubArrayTaintedNode isSubArrayTaintedNode) {
            return null;
        }

        @Specialization(guards = "isSubArrayTaintedNode.execute(taint, from, to)")
        static Object[] subTaintTainted(Object[] taint, int from, int to,
                                          @Cached IsSubArrayTaintedNode isSubArrayTaintedNode) {
            return Arrays.copyOfRange(taint, from, to);
        }

        static SubTaintArrayNode getUncached() {
            return TSTaintNodesFactory.SubTaintArrayNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class IsSubArrayTaintedNode extends Node {

        IsSubArrayTaintedNode() { }

        /**
         * Checks whether the given array is tainted in the range {@code from - to}.
         * @param taint taint array
         * @param from inclusive
         * @param to exclusive
         */
        public abstract boolean execute(Object[] taint, int from, int to);

        @Specialization(guards = "taint != null")
        static boolean isTaintedNonNull(Object[] taint, int from, int to) {
            return anyNonNull(taint, from, to);
        }

        @Specialization(guards = "taint == null")
        static boolean isTaintedNull(Object[] taint, int from, int to) {
            return false;
        }

        private static boolean anyNonNull(Object[] taint, int from, int to) {
            for (int i = Math.max(0, from); i < Math.min(taint.length, to); i++) {
                if (taint[i] != null) {
                    return true;
                }
            }
            return false;
        }

        static IsSubArrayTaintedNode getUncached() {
            return TSTaintNodesFactory.IsSubArrayTaintedNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class IsArrayTaintedNode extends Node {

        IsArrayTaintedNode() { }

        /**
         * Checks whether the given array is tainted.
         * An array is tainted if it is not {@code null} and has at least
         * one entry which is also not {@code null}.
         */
        public abstract boolean execute(Object[] taint);

        @Specialization(guards = "taint != null")
        static boolean isTaintedNonNull(Object[] taint,
                                 @Cached IsSubArrayTaintedNode isSubArrayTaintedNode) {
            return isSubArrayTaintedNode.execute(taint, 0, taint.length);
        }

        @Specialization(guards = "taint == null")
        static boolean isTaintedNull(Object[] taint) {
            return false;
        }

        static IsArrayTaintedNode getUncached() {
            return TSTaintNodesFactory.IsArrayTaintedNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class IsTaintedNode extends Node {

        IsTaintedNode() { }

        /**
         * Checks if the given {@link AbstractTruffleString} is tainted.
         */
        public abstract boolean execute(AbstractTruffleString a);

        @SuppressWarnings("unused")
        @Specialization
        static boolean isTainted(AbstractTruffleString a,
                                 @Cached IsArrayTaintedNode isArrayTaintedNode,
                                 @Cached IsTaintedNode isTaintedNodeLeft,
                                 @Cached IsTaintedNode isTaintedNodeRight,
                                 @Cached ConditionProfile isTaintedString,
                                 @Cached ConditionProfile isLazyConcat) {
            final Object data = a.data();
            if (isTaintedString.profile(data instanceof TaintedString)) {
                return isArrayTaintedNode.execute(((TaintedString) data).taint());
            }
            if (isLazyConcat.profile(data instanceof LazyConcat)) {
                final TruffleString left = ((LazyConcat) data).left();
                final TruffleString right = ((LazyConcat) data).right();
                return isTaintedNodeLeft.execute(left) || isTaintedNodeRight.execute(right);
            }
           return false;
        }

        static IsTaintedNode getUncached() {
            return TSTaintNodesFactory.IsTaintedNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class AddTaintNode extends Node {

        AddTaintNode() { }

        /**
         * Adds taint to the given {@link AbstractTruffleString}.
         */
        public abstract AbstractTruffleString execute(AbstractTruffleString a, Object taint);

        @SuppressWarnings("unused")
        @Specialization
        static AbstractTruffleString addTaint(TruffleString a, Object taint,
                                        @Cached IsTaintedNode isTaintedNode,
                                        @Cached ConditionProfile isTainted) {
            if (taint == null) {
                throw new IllegalArgumentException("Taint must not be null");
            }
            final Object[] taintArr = new Object[a.codePointLength()];
            Arrays.fill(taintArr, taint);
            final Object data = isTainted.profile(isTaintedNode.execute(a))
                    ? ((TaintedString) a.data()).data()
                    : a.data();
            return TruffleString.createTainted(
                    data,
                    taintArr,
                    a.offset(),
                    a.length(),
                    a.stride(),
                    a.encoding(),
                    a.codePointLength(),
                    a.codeRange()
            );
        }

        @SuppressWarnings("unused")
        @Specialization
        static AbstractTruffleString unsupported(MutableTruffleString a, Object taint) {
            throw mutableTruffleStringsNotSupported();
        }

        static AddTaintNode getUncached() {
            return TSTaintNodesFactory.AddTaintNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class GetTaintNode extends Node {

        GetTaintNode() { }

        /**
         * Returns the taint of the given {@link AbstractTruffleString},
         * which can be {@code null}, if the argument is not tainted.
         */
        public abstract Object[] execute(AbstractTruffleString a);

        @SuppressWarnings("unused")
        @Specialization(guards = "isTaintedNode.execute(a)")
        static Object[] getTaintTainted(AbstractTruffleString a,
                                 @Cached IsTaintedNode isTaintedNode,
                                 @Cached ConditionProfile isLazyConcat) {
            final Object data = a.data();
            if (isLazyConcat.profile(data instanceof LazyConcat)) {
                return LazyConcat.flattenTaint((TruffleString) a);
            }
            assert data instanceof TaintedString;
            return ((TaintedString) data).taint();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTaintedNode.execute(a)")
        static Object[] getTaintNotTainted(AbstractTruffleString a,
                                 @Cached IsTaintedNode isTaintedNode) {
            return null;
        }

        static GetTaintNode getUncached() {
            return TSTaintNodesFactory.GetTaintNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class RemoveTaintNode extends Node {

        RemoveTaintNode() { }

        /**
         * Removes the taint from the given {@link AbstractTruffleString} in the given range.
         */
        public abstract AbstractTruffleString execute(AbstractTruffleString a, int from, int to);

        @SuppressWarnings("unused")
        @Specialization(guards = "!isSubArrayTaintedNode.execute(getTaintNode.execute(a), from, to)")
        static AbstractTruffleString removeTaintUntainted(TruffleString a, int from, int to,
                                                  @Cached IsSubArrayTaintedNode isSubArrayTaintedNode,
                                                  @Cached GetTaintNode getTaintNode) {
            return a;
        }


        @SuppressWarnings("unused")
        @Specialization(guards = "isSubArrayTaintedNode.execute(getTaintNode.execute(a), from, to)")
        static TruffleString removeTaintTainted(TruffleString a, int from, int to,
                                                @Cached IsSubArrayTaintedNode isSubArrayTaintedNode,
                                                @Cached CopyTaintArrayNode copyTaintArrayNode,
                                                @Cached ConditionProfile isTaintedString,
                                                @Cached GetTaintNode getTaintNode,
                                                @Cached IsArrayTaintedNode isArrayTaintedNode,
                                                @Cached ConditionProfile isStillTainted,
                                                @Cached TruffleString.ToIndexableNode toIndexableNode) {
            final Object[] taintArr = copyTaintArrayNode.execute(getTaintNode.execute(a));
            boundsCheckRegionI(from, to - from, taintArr.length);
            Arrays.fill(taintArr, from, to, null);
            final Object data = a.data();
            final Object nestedData = isTaintedString.profile(data instanceof TaintedString)
                    ? ((TaintedString) data).data()
                    : data;

            if (isStillTainted.profile(isArrayTaintedNode.execute(taintArr))) {
                return TruffleString.createTainted(
                        nestedData,
                        taintArr,
                        a.offset(),
                        a.length(),
                        a.stride(),
                        a.encoding(),
                        a.codePointLength(),
                        a.codeRange()
                );
            }
            return TruffleString.createFromArray(
                    nestedData,
                    a.offset(),
                    a.length(),
                    a.stride(),
                    a.encoding(),
                    a.codePointLength(),
                    a.codeRange()
            );
        }

        @SuppressWarnings("unused")
        @Specialization
        static MutableTruffleString unsupported(MutableTruffleString a, int from, int to) {
            throw mutableTruffleStringsNotSupported();
        }

        static RemoveTaintNode getUncached() {
            return TSTaintNodesFactory.RemoveTaintNodeGen.getUncached();
        }
    }
}
