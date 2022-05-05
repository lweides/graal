/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.strings.TruffleString;
import org.junit.Test;

import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.from;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.getTaint;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.isTainted;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.length;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.taint;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AddTaintTests {

    @Test
    public void unTaintedShouldBeUntainted() {
        TruffleString untainted = from("foo");
        assertFalse("Untainted TS should not be tainted", isTainted(untainted));
    }

    @Test
    public void taintedShouldBeTainted() {
        TruffleString untainted = from("foo");
        AbstractTruffleString tainted = taint(untainted);
        assertFalse("TS taint state should be immutable", isTainted(untainted));
        assertTrue("TS should be tainted", isTainted(tainted));
        assertNotSame("tainting should create a new object", untainted, tainted);
    }

    @Test
    public void taintArrayLength() {
        AbstractTruffleString tainted = taint("foo");
        Object[] taintArr = getTaint(tainted);
        assertEquals("Taint array should have length of TS", length(tainted), taintArr.length);
    }

    @Test
    public void taintLabelsShouldPersist() {
        AbstractTruffleString tainted = taint("foo", "bar");
        Object[] taintArr = getTaint(tainted);
        assertArrayEquals(new String[] {"bar", "bar", "bar"}, taintArr);
    }

    @Test
    public void taintLabelsUntainted() {
        TruffleString untainted = from("foo");
        Object[] taintArr = getTaint(untainted);
        assertNull("Untainted should have null taint labels", taintArr);
    }

    @Test
    public void emptyString() {
        TruffleString empty = from("");
        AbstractTruffleString tainted = taint(empty);
        assertFalse("Empty TS cannot be tainted", isTainted(tainted));
        assertNotSame("Taint always creates a new TS", empty, tainted);
    }

    @Test
    public void doubleTaint() {
        TruffleString untainted = from("foo");
        AbstractTruffleString tainted1 = taint(untainted);
        AbstractTruffleString tainted2 = taint(tainted1);

        assertFalse("Initial TS should not be tainted", isTainted(untainted));
        assertTrue("First TS should be tainted", isTainted(tainted1));
        assertTrue("Second TS should be tainted", isTainted(tainted2));
    }

    @Test
    public void taintLabelsAreShared() {
        Object taintLabel = new Object();
        AbstractTruffleString tainted1 = taint("foo", taintLabel);
        AbstractTruffleString tainted2 = taint("bar", taintLabel);

        Object tl1 = getTaint(tainted1)[0];
        Object tl2 = getTaint(tainted2)[0];
        assertSame("Taint labels should be the same object", tl1, tl2);
    }

    @Test
    public void nullAsTaintLabel() {
        try {
           taint("foo", null);
            fail("Tainting with null is not supported");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }
}
