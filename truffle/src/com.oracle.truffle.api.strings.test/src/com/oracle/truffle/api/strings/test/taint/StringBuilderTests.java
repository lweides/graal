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

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import org.junit.Test;

import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.DEFAULT_ENCODING;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.concat;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.from;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.getTaint;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.isTainted;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.length;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.taint;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StringBuilderTests {

    @Test
    public void appendTaintedString() {
        TruffleStringBuilder tsb = TruffleStringBuilder.create(DEFAULT_ENCODING);
        tsb.appendStringUncached((TruffleString) taint("foo"));
        TruffleString a = tsb.toStringUncached();

        assertTrue("Built TS should be tainted", isTainted(a));
    }

    @Test
    public void appendUntainted() {
        TruffleStringBuilder tsb = TruffleStringBuilder.create(DEFAULT_ENCODING);
        tsb.appendJavaStringUTF16Uncached("foo");
        tsb.appendJavaStringUTF16Uncached("bar");
        TruffleString a = tsb.toStringUncached();

        assertFalse("Built TS should not be tainted", isTainted(a));
    }

    @Test
    public void appendUntaintedTainted() {
        TruffleStringBuilder tsb = TruffleStringBuilder.create(DEFAULT_ENCODING);
        tsb.appendJavaStringUTF16Uncached("foo");
        tsb.appendStringUncached((TruffleString) taint("bar", 1));
        TruffleString a = tsb.toStringUncached();

        assertTrue("Built TS should be tainted", isTainted(a));
        assertArrayEquals(
                "Taint labels should be persistent",
                new Object[] {null, null, null, 1, 1, 1},
                getTaint(a)
        );
    }

    @Test
    public void appendTaintedUntainted() {
        TruffleStringBuilder tsb = TruffleStringBuilder.create(DEFAULT_ENCODING);
        tsb.appendStringUncached((TruffleString) taint("bar", 1));
        tsb.appendJavaStringUTF16Uncached("foooo"); // has to be more than double length of "bar"
        TruffleString a = tsb.toStringUncached();

        assertTrue("Built TS should be tainted", isTainted(a));
        assertArrayEquals(
                "Taint labels should be persistent",
                new Object[] {1, 1, 1, null, null, null, null, null},
                getTaint(a)
        );
    }

    @Test
    public void append() {
        TruffleStringBuilder tsb = TruffleStringBuilder.create(DEFAULT_ENCODING);
        tsb.appendStringUncached((TruffleString) taint("abc"));
        tsb.appendJavaStringUTF16Uncached("def");
        tsb.appendCodePointUncached('h');
        tsb.appendCharUTF16Uncached('i');
        tsb.appendSubstringByteIndexUncached(from("jkl"), 2, 4);
        tsb.appendIntNumberUncached(73);
        tsb.appendLongNumberUncached(42L);
        TruffleString a = tsb.toStringUncached();
        assertTrue("Built TS should be tainted", isTainted(a));
        Object[] taint = getTaint(a);
        assertEquals("Taint Lengths should math", length(a), taint.length);
    }

    @Test
    public void appendByteIndex() {
        TruffleStringBuilder tsb = TruffleStringBuilder.create(DEFAULT_ENCODING);
        TruffleString tainted = (TruffleString) concat(
                taint("a", 1),
                taint("b", 2),
                taint("c", 3),
                taint("d", 4),
                taint("e", 5)
        );
        tsb.appendSubstringByteIndexUncached(tainted, 8, 2);
        tsb.appendSubstringByteIndexUncached(tainted, 0, 2);
        tsb.appendSubstringByteIndexUncached(tainted, 2, 2);
        tsb.appendSubstringByteIndexUncached(tainted, 6, 2);
        tsb.appendSubstringByteIndexUncached(tainted, 4, 2);

        TruffleString built = tsb.toStringUncached();
        Object[] taint = getTaint(built);

        assertEquals("TS should be built as expected", built.toString(), "eabdc");
        assertArrayEquals(
                "Taint labels should be inserted accordingly",
                new Object[] {5, 1, 2, 4, 3},
                taint
        );
    }
}
