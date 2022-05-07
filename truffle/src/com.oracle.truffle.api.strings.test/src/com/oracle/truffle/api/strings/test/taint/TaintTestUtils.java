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
package com.oracle.truffle.api.strings.test.taint;

import com.oracle.truffle.api.strings.AbstractTruffleString;
import com.oracle.truffle.api.strings.MutableTruffleString;
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

    public static MutableTruffleString mutableFrom(String value) {
        return mutableFrom(value, DEFAULT_ENCODING);
    }

    public static MutableTruffleString mutableFrom(String value, TruffleString.Encoding encoding) {
        return MutableTruffleString.AsMutableTruffleStringNode.getUncached().execute(from(value, encoding), encoding);
    }

    public static int length(AbstractTruffleString a) {
        return length(a, DEFAULT_ENCODING);
    }

    public static int length(AbstractTruffleString a, TruffleString.Encoding encoding) {
        return TruffleString.CodePointLengthNode.getUncached().execute(a, encoding);
    }

    public static AbstractTruffleString concat(AbstractTruffleString ... strings) {
        AbstractTruffleString a = strings[0];
        for (int i = 1; i < strings.length; i++) {
            a = concat(a, strings[i]);
        }
        return a;
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
        return TSTaintNodesFactory.IsTaintedNodeGen.getUncached().execute(ts);
    }

    public static Object[] getTaint(AbstractTruffleString a) {
        return TSTaintNodesFactory.GetTaintNodeGen.getUncached().execute(a);
    }

    public static AbstractTruffleString removeTaint(AbstractTruffleString a, int from, int to) {
        return TSTaintNodesFactory.RemoveTaintNodeGen.getUncached().execute(a, from, to);
    }

    public static Object[] concatTaint(AbstractTruffleString a, AbstractTruffleString b) {
        return TSTaintNodesFactory.ConcatTaintArrayNodeGen.getUncached().execute(a, b);
    }
}
