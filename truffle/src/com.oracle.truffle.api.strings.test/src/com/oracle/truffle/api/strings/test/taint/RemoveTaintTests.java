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
import com.oracle.truffle.api.strings.TruffleString;
import org.junit.Test;

import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.from;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.isTainted;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.removeTaint;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.taint;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RemoveTaintTests {

    @Test
    public void removeUntainted() {
        TruffleString untainted = from("foo");
        AbstractTruffleString removed = removeTaint(untainted, 0, 3);
        assertFalse("TS should not be tainted", isTainted(removed));
        assertSame("Removing taint from untainted TS is a NOP", untainted, removed);
    }

    @Test
    public void removeTainted() {
        AbstractTruffleString tainted = taint("foo");
        assertTrue("TS should be tainted", isTainted(tainted));
        AbstractTruffleString removed = removeTaint(tainted, 0, 3);
        assertFalse("TS should not be tainted anymore", isTainted(removed));
        assertTrue("Original TS should still be tainted", isTainted(tainted));
    }

    @Test
    public void removeTaintPartial() {
        AbstractTruffleString tainted = taint("foo");

        AbstractTruffleString removed1 = removeTaint(tainted, 0, 1);
        assertTrue("TS should still be tainted", isTainted(removed1));
        assertNotSame("Remove is not a NOP here", tainted, removed1);

        AbstractTruffleString removed2 = removeTaint(removed1, 1, 2);
        assertTrue("TS should still be tainted", isTainted(removed2));
        assertNotSame("Remove is not a NOP here", removed1, removed2);

        AbstractTruffleString removed3 = removeTaint(removed2, 2, 3);
        assertFalse("TS should not be tainted", isTainted(removed3));
        assertNotSame("Remove is not a NOP here", removed2, removed3);

        assertTrue("Original TS should still be tainted", isTainted(tainted));
    }

    @Test
    public void removeTaintIndexOutOfBounds() {
        AbstractTruffleString untainted = from("foo");
        AbstractTruffleString removed1 = removeTaint(untainted, 0, 10);
        // this does not throw an exception, as removeTaint is a NOP here
        assertSame("Removing taint from untainted TS is a NOP", untainted, removed1);

        AbstractTruffleString tainted = taint("foo");

        try {
            removeTaint(tainted, 0, 10);
            fail("Removing taint out of bounds should throw an exception");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e);
        }
    }
}
