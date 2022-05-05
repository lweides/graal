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
            //CHECKSTYLE:OFF
            AbstractTruffleString a = taint(from("föö", source), "bar");
            //CHECKSTYLE:ON
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
