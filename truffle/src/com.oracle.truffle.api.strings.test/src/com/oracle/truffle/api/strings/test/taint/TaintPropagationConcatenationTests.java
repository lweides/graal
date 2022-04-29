package com.oracle.truffle.api.strings.test.taint;

import com.oracle.truffle.api.strings.AbstractTruffleString;
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
        AbstractTruffleString a = taint("fooooooooooooooooooooooooooooooooooooooooo");
        AbstractTruffleString b = taint("baaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaar");
        AbstractTruffleString c = concat(a, b, DEFAULT_ENCODING, true);

        // TODO implement taint propagation for lazy concat
//        assertTrue("TS should be tainted", isTainted(c));
    }
}
