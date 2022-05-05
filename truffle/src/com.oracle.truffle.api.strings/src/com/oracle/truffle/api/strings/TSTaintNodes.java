package com.oracle.truffle.api.strings;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;

import java.util.Arrays;

import static com.oracle.truffle.api.strings.AbstractTruffleString.*;

/**
 * Collection of taint tracking related {@link Node}s.
 */
public class TSTaintNodes {

    @GeneratePackagePrivate
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
            boundsCheckRegionI(from, to - from, taint.length);
            return anyNonNull(taint, from, to);
        }

        @Specialization(guards = "taint == null")
        static boolean isTaintedNull(Object[] taint, int from, int to) {
            return false;
        }

        private static boolean anyNonNull(Object[] taint, int from, int to) {
            for (int i = from; i < to; i++) {
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
                                 @Cached IsArrayTaintedNode isArrayTaintedNode) {
            final Object data = a.data();
            if (data instanceof TaintedString) {
                return isArrayTaintedNode.execute(((TaintedString) data).taint());
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
        static AbstractTruffleString addTaint(MutableTruffleString a, Object taint) {
            throw new UnsupportedOperationException("Mutable TruffleStrings cannot be tainted");
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
        @Specialization
        static Object[] getTaint(AbstractTruffleString a) {
            final Object data = a.data();
            if (data instanceof TaintedString) {
                return ((TaintedString) data).taint();
            }
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
        static AbstractTruffleString removeTaintUntainted(AbstractTruffleString a, int from, int to,
                                                  @Cached IsSubArrayTaintedNode isSubArrayTaintedNode,
                                                  @Cached GetTaintNode getTaintNode) {
            return a;
        }


        @SuppressWarnings("unused")
        @Specialization(guards = "isSubArrayTaintedNode.execute(getTaintNode.execute(a), from, to)")
        static TruffleString removeTaintTainted(TruffleString a, int from, int to,
                                                @Cached IsSubArrayTaintedNode isSubArrayTaintedNode,
                                                @Cached CopyTaintArrayNode copyTaintArrayNode,
                                                @Cached GetTaintNode getTaintNode,
                                                @Cached IsArrayTaintedNode isArrayTaintedNode,
                                                @Cached ConditionProfile isStillTainted,
                                                @Cached TruffleString.ToIndexableNode toIndexableNode) {
            final Object[] taintArr = copyTaintArrayNode.execute(getTaintNode.execute(a));
            final Object data = ((TaintedString) a.data()).data(); // this cast is safe as we know a is tainted
            Arrays.fill(taintArr, from, to, null);
            if (isStillTainted.profile(isArrayTaintedNode.execute(taintArr))) {
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
            return TruffleString.createFromArray(
                    data,
                    a.offset(),
                    a.length(),
                    a.stride(),
                    a.encoding(),
                    a.codePointLength(),
                    a.codeRange()
            );
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isSubArrayTaintedNode.execute(getTaintNode.execute(a), from, to)")
        static MutableTruffleString removeTaintTainted(MutableTruffleString a, int from, int to,
                                                @Cached IsSubArrayTaintedNode isSubArrayTaintedNode,
                                                @Cached GetTaintNode getTaintNode) {
            throw new UnsupportedOperationException("Mutable TruffleStrings cannot be tainted");
        }

        static RemoveTaintNode getUncached() {
            return TSTaintNodesFactory.RemoveTaintNodeGen.getUncached();
        }
    }
}
