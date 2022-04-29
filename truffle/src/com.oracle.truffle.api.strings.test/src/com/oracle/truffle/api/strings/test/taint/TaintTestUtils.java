package com.oracle.truffle.api.strings.test.taint;

import com.oracle.truffle.api.strings.AbstractTruffleString;
import com.oracle.truffle.api.strings.TSTaintNodesFactory;
import com.oracle.truffle.api.strings.TruffleString;

public class TaintTestUtils {

    public static final TruffleString.Encoding DEFAULT_ENCODING = TruffleString.Encoding.UTF_16;

    public static TruffleString from(String value) {
        return from(value, DEFAULT_ENCODING);
    }

    public static TruffleString from(String value, TruffleString.Encoding encoding) {
        return TruffleString.FromJavaStringNode.getUncached().execute(value, encoding);
    }

    public static int length(AbstractTruffleString a) {
        return length(a, DEFAULT_ENCODING);
    }

    public static int length(AbstractTruffleString a, TruffleString.Encoding encoding) {
        return TruffleString.CodePointLengthNode.getUncached().execute(a, encoding);
    }

    public static AbstractTruffleString concat(AbstractTruffleString a, AbstractTruffleString b) {
        return concat(a, b, DEFAULT_ENCODING);
    }

    public static AbstractTruffleString concat(AbstractTruffleString a, AbstractTruffleString b, TruffleString.Encoding encoding) {
        return concat(a, b, encoding, false);
    }

    public static AbstractTruffleString concat(AbstractTruffleString a, AbstractTruffleString b, TruffleString.Encoding encoding, boolean lazy) {
        return TruffleString.ConcatNode.getUncached().execute(a, b, encoding, lazy);
    }

    public static AbstractTruffleString substring(AbstractTruffleString a, int from, int to) {
        return substring(a, from, to, DEFAULT_ENCODING, false);
    }

    public static AbstractTruffleString substring(AbstractTruffleString a, int from, int to, TruffleString.Encoding encoding, boolean lazy) {
        return a.substringUncached(from, to, encoding, lazy);
    }

    public static AbstractTruffleString convert(AbstractTruffleString a, TruffleString.Encoding encoding) {
        return a.switchEncodingUncached(encoding);
    }

    public static AbstractTruffleString taint(String value) {
        return taint(from(value));
    }

    public static AbstractTruffleString taint(String value, Object taint) {
        return taint(from(value), taint);
    }

    public static AbstractTruffleString taint(AbstractTruffleString ts) {
        return taint(ts, new Object());
    }

    public static AbstractTruffleString taint(AbstractTruffleString ts, Object taint) {
        return TSTaintNodesFactory.AddTaintNodeGen.getUncached().execute(ts, taint);
    }

    public static boolean isTainted(AbstractTruffleString ts) {
        // TODO maybe create methods on TruffleString for this
        return TSTaintNodesFactory.IsTaintedNodeGen.getUncached().execute(ts);
    }

    public static Object[] getTaint(AbstractTruffleString a) {
        return TSTaintNodesFactory.GetTaintNodeGen.getUncached().execute(a);
    }

    public static AbstractTruffleString removeTaint(AbstractTruffleString a, int from, int to) {
        return TSTaintNodesFactory.RemoveTaintNodeGen.getUncached().execute(a, from, to);
    }
}
