package com.oracle.truffle.api.strings.test.taint;

import com.oracle.truffle.api.strings.AbstractTruffleString;
import org.junit.Test;

import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.DEFAULT_ENCODING;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.from;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.getTaint;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.isTainted;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.length;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.substring;
import static com.oracle.truffle.api.strings.test.taint.TaintTestUtils.taint;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TaintPropagationSubstringTests {

    @Test
    public void unTaintedSubstring() {
        AbstractTruffleString a = from("foobar");
        AbstractTruffleString b = substring(a, 0, 3);

        assertFalse("TS should not be tainted", isTainted(a));
        assertEquals("Substring should work as expected", "foo", b.toString());
    }

    @Test
    public void taintedSubstring() {
        AbstractTruffleString a = taint("foo", "bar");
        AbstractTruffleString b = substring(a, 0, 2);

        assertTrue("TS should be tainted", isTainted(a));
        assertEquals("Substring should work as expected", "fo", b.toString());

        Object[] taintArr = getTaint(b);
        assertEquals("Taint array should have length of TS", length(b), taintArr.length);
        assertArrayEquals("Taint labels should be propagated", new Object[] {"bar", "bar"}, taintArr);
    }

    @Test
    public void emptySubstring() {
        AbstractTruffleString a = taint("foo", "bar");
        AbstractTruffleString b = substring(a, 0, 0);

        assertFalse("Empty TS cannot be tainted", isTainted(b));
        assertEquals("Substring should work as expected", "", b.toString());

        Object[] taintAr = getTaint(b);
        assertNull(taintAr);
    }

    @Test
    public void substringOutOfBounds() {
        AbstractTruffleString a = taint("foo", "bar");

        try {
            substring(a, 0, 6);
            fail("Substring out of bounds should throw an exception");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void lazySubstring() {
        AbstractTruffleString a = taint("foooooooooooooooooooooooooooooooooooooooooooooooo", "bar");
        AbstractTruffleString b = substring(a, 0, 41, DEFAULT_ENCODING, true);

        assertTrue("TS should be tainted", isTainted(a));

        Object[] taintArr = getTaint(b);
        assertEquals("Taint array should have length of TS", length(b), taintArr.length);
    }
}
