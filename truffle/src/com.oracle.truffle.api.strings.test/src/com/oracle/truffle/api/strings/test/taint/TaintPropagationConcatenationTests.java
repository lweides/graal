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
import org.junit.Test;

import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.DEFAULT_ENCODING;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.concat;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.concatTaint;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.from;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.getTaint;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.isTainted;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.length;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.taint;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TaintPropagationConcatenationTests {

    @Test
    public void basicConcat() {
        AbstractTruffleString a = from("foo");
        AbstractTruffleString b = from("bar");
        AbstractTruffleString c = concat(a, b);

        assertFalse("TS should not be tainted", isTainted(c));
        assertEquals("TS concat should work as expected", "foobar", c.toString());
    }

    @Test
    public void bothTaintedConcat() {
        AbstractTruffleString a = taint("foo", 1);
        AbstractTruffleString b = taint("bar", 2);
        AbstractTruffleString c = concat(a, b);

        assertTrue("TS should be tainted", isTainted(c));

        Object[] taintArr = getTaint(c);
        assertEquals("Taint should have combined length", length(a) + length(b), taintArr.length);
        assertArrayEquals("Arrays should be concatenated", new Object[] {1, 1, 1, 2, 2, 2}, taintArr);
    }

    @Test
    public void aTaintedConcat() {
        AbstractTruffleString a = taint("foo", 1);
        AbstractTruffleString b = from("bar");
        AbstractTruffleString c = concat(a, b);

        assertTrue("TS should be tainted", isTainted(c));

        Object[] taintArr = getTaint(c);
        assertEquals("Taint should have combined length", length(a) + length(b), taintArr.length);
        assertArrayEquals("Arrays should be concatenated", new Object[] {1, 1, 1, null, null, null}, taintArr);
    }

    @Test
    public void bTaintedConcat() {
        AbstractTruffleString a = from("foo");
        AbstractTruffleString b = taint("bar", 2);
        AbstractTruffleString c = concat(a, b);

        assertTrue("TS should be tainted", isTainted(c));

        Object[] taintArr = getTaint(c);
        assertEquals("Taint should have combined length", length(a) + length(b), taintArr.length);
        assertArrayEquals("Arrays should be concatenated", new Object[] {null, null, null, 2, 2, 2}, taintArr);
    }

    @Test
    public void aEmpty() {
        AbstractTruffleString a = from("");
        AbstractTruffleString b = taint("bar", 2);
        AbstractTruffleString c = concat(a, b);

        assertTrue("TS should be tainted", isTainted(c));

        Object[] taintArr = getTaint(c);
        assertEquals("Taint should have combined length", length(a) + length(b), taintArr.length);
        assertArrayEquals("Arrays should be concatenated", new Object[] {2, 2, 2}, taintArr);
    }

    @Test
    public void bEmpty() {
        AbstractTruffleString a = taint("foo", 1);
        AbstractTruffleString b = from("");
        AbstractTruffleString c = concat(a, b);

        assertTrue("TS should be tainted", isTainted(c));

        Object[] taintArr = getTaint(c);
        assertEquals("Taint should have combined length", length(a) + length(b), taintArr.length);
        assertArrayEquals("Arrays should be concatenated", new Object[] {1, 1, 1}, taintArr);
    }

    @Test
    public void bothEmpty() {
        AbstractTruffleString a = from("");
        AbstractTruffleString b = from("");
        AbstractTruffleString c = concat(a, b);

        assertFalse("Empty TS cannot be tainted", isTainted(c));

        Object[] taintArr = getTaint(c);
        assertNull(taintArr);
    }

    @Test
    public void lazyConcat() {
        // see min lazy concat length
        AbstractTruffleString a = concat(
                taint("fooooooooooooooooooooooooooooooooooooooooo"),
                taint("baaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaar"),
                DEFAULT_ENCODING,
                true
        );

        assertTrue("TS should be tainted", isTainted(a));
    }

    @Test
    public void lazyConcatTaintLabels() {
        AbstractTruffleString a = taint("fooooooooooooooooooooooooooooooooooooooooo", 1);
        AbstractTruffleString b = taint("baaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaar", 2);
        AbstractTruffleString c = concat(a, b, DEFAULT_ENCODING, true);

        Object[] taint = getTaint(c);
        Object[] expectedTaint = concatTaint(a, b);

        assertArrayEquals("Taint labels should be equal", expectedTaint, taint);
        assertTrue("TS should be tainted", isTainted(c));

    }

    @Test
    public void nestedLazyConcat() {
        AbstractTruffleString a = taint("fooooooooooooooooooooooooooooooooooooooooo", 1);
        AbstractTruffleString b = taint("baaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaar", 2);
        AbstractTruffleString c = concat(a, b, DEFAULT_ENCODING, true);
        AbstractTruffleString d = taint("baaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaar", 3);
        AbstractTruffleString e = concat(c, d, DEFAULT_ENCODING, true);

        Object[] taint = getTaint(e);
        Object[] expectedTaint = concatTaint(c, d);
        assertArrayEquals("Taint labels should be equal", expectedTaint, taint);
        assertTrue("TS should be tainted", isTainted(e));
    }

    @Test
    public void lazyConcatMaterialized() {
        AbstractTruffleString a = concat(
                taint("fooooooooooooooooooooooooooooooooooooooooo"),
                taint("baaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaar"),
                DEFAULT_ENCODING,
                true
        );

        AbstractTruffleString b = concat(a, from(""), DEFAULT_ENCODING, false);
        assertTrue("TS should be tainted", isTainted(a));
        assertTrue("TS should be tainted", isTainted(b));

    }

    @Test
    public void nestedLazyConcatMaterialized() {
        AbstractTruffleString a = concat(
                concat(
                        taint("fooooooooooooooooooooooooooooooooooooooooo"),
                        taint("baaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaar"),
                        DEFAULT_ENCODING,
                        true
                ),
                taint("baaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaar"),
                DEFAULT_ENCODING,
                true
        );
        AbstractTruffleString b = concat(a, from(""), DEFAULT_ENCODING, false);

        assertTrue("TS should be tainted", isTainted(a));
        assertTrue("TS should be tainted", isTainted(b));

        assertEquals(
                "TS concatenation should work as expected",
                "fooooooooooooooooooooooooooooooooooooooooobaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaarbaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaar",
                b.toString()
        );

    }
}
