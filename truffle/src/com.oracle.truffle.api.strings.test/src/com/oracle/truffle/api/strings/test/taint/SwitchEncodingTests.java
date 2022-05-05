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

import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.convert;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.from;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.getTaint;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.isTainted;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.length;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.taint;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SwitchEncodingTests {

    @Test
    public void switchingEncodingShouldPersistTaintAscii() {
        for (TruffleString.Encoding source : TruffleString.Encoding.values()) {
            AbstractTruffleString a = taint(from("foo", source), "bar");
            Object[] taintArrSource = getTaint(a);
            assertTrue("Source should be tainted", isTainted(a));
            for (TruffleString.Encoding target : TruffleString.Encoding.values()) {
                AbstractTruffleString b = convert(a, target);
                assertTrue(
                        String.format("Target should be tainted (%s -> %s)", source, target),
                        isTainted(b)
                );
                Object[] taintArrTarget = getTaint(b);
                assertArrayEquals(
                        String.format("Taint labels should be the same (%s -> %s)", source, target),
                        taintArrSource,
                        taintArrTarget
                );
            }
        }
    }

    @Test
    public void switchingEncodingShouldPersistTaintNonAscii() {
        // UTF8_MAC causes a size increase of the converted string, needs to be addressed separately
        for (TruffleString.Encoding source : TruffleString.Encoding.values()) {
            if (source == TruffleString.Encoding.UTF8_MAC) {
                continue;
            }
            // Checkstyle: stop
            AbstractTruffleString a = taint(from("föö", source), "bar");
            // Checkstyle: resume
            Object[] taintArrSource = getTaint(a);
            assertTrue("Source should be tainted", isTainted(a));
            for (TruffleString.Encoding target : TruffleString.Encoding.values()) {
                if (target == TruffleString.Encoding.UTF8_MAC) {
                    continue;
                }
                AbstractTruffleString b = convert(a, target);
                assertTrue(
                        String.format("Target should be tainted (%s -> %s)", source, target),
                        isTainted(b)
                );
                Object[] taintArrTarget = getTaint(b);
                assertArrayEquals(
                        String.format("Taint labels should be the same (%s -> %s)", source, target),
                        taintArrSource,
                        taintArrTarget
                );
                assertEquals(
                        String.format("Taint labels should have same length as TS (%s -> %s)", source, target),
                        length(b, target),
                        taintArrTarget.length
                );
            }
        }
    }

    @Test
    public void taintShouldBePropagatedInEncodingCache() {
        AbstractTruffleString utf16 = from("foo");
        convert(utf16, TruffleString.Encoding.US_ASCII);
        AbstractTruffleString tainted = taint(utf16);
        AbstractTruffleString ascii2 = convert(tainted, TruffleString.Encoding.US_ASCII);

        assertTrue("TS should be tainted", isTainted(ascii2));
    }
}
