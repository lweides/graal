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

import java.util.stream.IntStream;

import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.from;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.getTaint;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.isTainted;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.length;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.taintInRange;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AddTaintInRangeTests {

    @Test
    public void addTaintInBounds() {
        AbstractTruffleString a = taintInRange("fooooo", "bar", 2, 5);
        assertTrue("a should be tainted", isTainted(a));

        assertArrayEquals(
                "Taint labels should match",
                new Object[] {null, null, "bar", "bar", "bar", null},
                getTaint(a)
        );
    }

    @Test
    public void addTaintMultipleTimes() {
        AbstractTruffleString a = from("0123456789");
        for (int i = 0; i < length(a); i++) {
            a = taintInRange(a, i, i, i + 1);
        }
        Object[] taintArr = getTaint(a);
        Integer[] expected = IntStream.range(0, length(a)).boxed().toArray(Integer[]::new);
        assertArrayEquals(
                "Taint labels should match",
                expected,
                taintArr
        );
    }

    @Test
    public void addTaintOutOfBounds1() {
        try {
            taintInRange("foo", "bar", 0, 4);
            fail("Should throw an exception");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void addTaintOutOfBounds2() {
        try {
            taintInRange("foo", "bar", -1, 3);
            fail("Should throw an exception");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void addTaintInvalidRegion() {
        try {
            taintInRange("foo", "bar", 2, 1);
            fail("Should throw an exception");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void addTaintEmptyRange() {
        AbstractTruffleString a = taintInRange("foo", "bar", 0, 0);
        assertFalse("Should not be tainted", isTainted(a));
    }
}
