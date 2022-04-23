package com.oracle.truffle.api.strings;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;

import java.util.Arrays;

/**
 * Collection of taint tracking related {@link Node}s.
 */
public class TSTaintNodes {

    @GeneratePackagePrivate
    @GenerateUncached
    public abstract static class ConcatTaintNode extends Node {

        ConcatTaintNode() {}

        /**
         * Concatenates the taint of {@code a} and {@code b}, returning the combined taint.
         * If both taints are empty or {@code null}, this method may return {@code null}.
         */
        public abstract Object[] execute(AbstractTruffleString a, AbstractTruffleString b);

        @SuppressWarnings("unused")
        @Specialization(guards = { "isTaintedNodeA.execute(a)", "isTaintedNodeB.execute(b)" })
        static Object[] concatBothTainted(AbstractTruffleString a, AbstractTruffleString b,
                                          @Cached IsTaintedNode isTaintedNodeA,
                                          @Cached IsTaintedNode isTaintedNodeB) {
            final Object[] taint = new Object[a.length() + b.length()];
            System.arraycopy(a.taint, 0, taint, 0, a.length());
            System.arraycopy(b.taint, 0, taint, a.length(), b.length());
            return taint;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = { "isTaintedNodeA.execute(a)", "!isTaintedNodeB.execute(b)" })
        static Object[] concatATainted(AbstractTruffleString a, AbstractTruffleString b,
                                       @Cached IsTaintedNode isTaintedNodeA,
                                       @Cached IsTaintedNode isTaintedNodeB) {
            final Object[] taint = new Object[a.length() + b.length()];
            System.arraycopy(a.taint, 0, taint, 0, a.length());
            Arrays.fill(taint, a.length(), taint.length, null);
            return taint;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = { "!isTaintedNodeA.execute(a)", "isTaintedNodeB.execute(b)" })
        static Object[] concatBTainted(AbstractTruffleString a, AbstractTruffleString b,
                                       @Cached IsTaintedNode isTaintedNodeA,
                                       @Cached IsTaintedNode isTaintedNodeB) {
            final Object[] taint = new Object[a.length() + b.length()];
            Arrays.fill(taint, 0, a.length(), null);
            System.arraycopy(b.taint, 0, taint, a.length(), b.length());
            return taint;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = { "!isTaintedNodeA.execute(a)", "!isTaintedNodeB.execute(b)" })
        static Object[] concatNoneTainted(AbstractTruffleString a, AbstractTruffleString b,
                                          @Cached IsTaintedNode isTaintedNodeA,
                                          @Cached IsTaintedNode isTaintedNodeB) {
            return null;
        }

        static ConcatTaintNode getUncached() {
            return TSTaintNodesFactory.ConcatTaintNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public static abstract class CopyTaintNode extends Node {

        CopyTaintNode() {}

        /**
         * Copies the given {@code taint} array into a new array.
         * The copy is only a shallow one.
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

        static CopyTaintNode getUncached() {
            return TSTaintNodesFactory.CopyTaintNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public static abstract class IsArrayTaintedNode extends Node {

        IsArrayTaintedNode() {}

        public abstract boolean execute(Object[] taint);

        @Specialization
        static boolean isTainted(Object[] a) {
            return a != null && anyNonNull(a);
        }

        private static boolean anyNonNull(Object[] taint) {
            for (final Object o : taint) {
                if (o != null) { return true; }
            }
            return false;
        }

        static IsArrayTaintedNode getUncached() {
            return TSTaintNodesFactory.IsArrayTaintedNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public static abstract class IsTaintedNode extends Node {

        IsTaintedNode() {}

        /**
         * Checks if the given {@link AbstractTruffleString} is tainted.
         * A {@link AbstractTruffleString} is tainted iff {@link AbstractTruffleString#taint} is not {@code null}
         * and contains at least one element, which is also not {@code null}.
         */
        public abstract boolean execute(AbstractTruffleString a);

        @SuppressWarnings("unused")
        @Specialization
        static boolean isTainted(AbstractTruffleString a,
                                 @Cached IsArrayTaintedNode isArrayTaintedNode) {
            return isArrayTaintedNode.execute(a.taint);
        }

        static IsTaintedNode getUncached() {
            return TSTaintNodesFactory.IsTaintedNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public static abstract class AddTaintNode extends Node {

        AddTaintNode() {}

        /**
         * Adds taint to the given {@link AbstractTruffleString}.
         */
        public abstract AbstractTruffleString execute(AbstractTruffleString a, Object taint);

        @SuppressWarnings("unused")
        @Specialization
        static AbstractTruffleString addTaint(TruffleString a, Object taint) {
            final Object[] taintArr = new Object[a.length()];
            Arrays.fill(taintArr, taint);
            return TruffleString.createFromArray(
                    a.data(),
                    a.offset(),
                    a.length(),
                    a.stride(),
                    a.encoding(),
                    a.codePointLength(),
                    a.codeRange(),
                    a.isCacheHead(),
                    taintArr
            );
        }

        // TODO should adding taint to a MutableTruffleString produce a new object?
        @SuppressWarnings("unused")
        @Specialization
        static AbstractTruffleString addTaint(MutableTruffleString a, Object taint,
                                              @Cached ConditionProfile taintLengthProfile) {
            if (taintLengthProfile.profile(a.taint == null || a.taint.length != a.length())) {
                a.taint = new Object[a.length()];
            }
            Arrays.fill(a.taint, taint);
            return a;
        }

        static AddTaintNode getUncached() {
            return TSTaintNodesFactory.AddTaintNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public static abstract class GetTaintNode extends Node {

        GetTaintNode() {}

        /**
         * Returns the taint of the given {@link AbstractTruffleString},
         * which can be {@code null}, if the argument is not tainted.
         */
        public abstract Object[] execute(AbstractTruffleString a);

        @SuppressWarnings("unused")
        @Specialization
        static Object[] getTaint(AbstractTruffleString a) {
            return a.taint;
        }

        static GetTaintNode getUncached() {
            return TSTaintNodesFactory.GetTaintNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public static abstract class RemoveTaintNode extends Node {

        RemoveTaintNode() {}

        /**
         * Removes the taint from the given {@link AbstractTruffleString} in the given range.
         */
        public abstract AbstractTruffleString execute(AbstractTruffleString a, int from, int to);

        @SuppressWarnings("unused")
        @Specialization(guards = "!isTaintedNode.execute(a)")
        static AbstractTruffleString removeTaintUntainted(AbstractTruffleString a, int from, int to,
                                                  @Cached IsTaintedNode isTaintedNode) {
            return a;
        }


        @SuppressWarnings("unused")
        @Specialization(guards = "isTaintedNode.execute(a)")
        static TruffleString removeTaintTainted(TruffleString a, int from, int to,
                                                  @Cached IsTaintedNode isTaintedNode,
                                                  @Cached CopyTaintNode copyTaintNode) {
            final Object[] taintArr = copyTaintNode.execute(a.taint);
            Arrays.fill(taintArr, from, to, null);
            return TruffleString.createFromArray(
                    a.data(),
                    a.offset(),
                    a.length(),
                    a.stride(),
                    a.encoding(),
                    a.codePointLength(),
                    a.codeRange(),
                    a.isCacheHead(),
                    taintArr
            );
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isTaintedNode.execute(a)")
        static MutableTruffleString removeTaintTainted(MutableTruffleString a, int from, int to,
                                                @Cached IsTaintedNode isTaintedNode) {
            Arrays.fill(a.taint, from, to, null);
            return a;
        }

        static RemoveTaintNode getUncached() {
            return TSTaintNodesFactory.RemoveTaintNodeGen.getUncached();
        }
    }
}
