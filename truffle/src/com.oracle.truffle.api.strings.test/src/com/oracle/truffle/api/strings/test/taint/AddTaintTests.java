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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
        // TODO should they be the same object?
//        assertSame("As taint is NOP should be same object", empty, tainted);
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
        AbstractTruffleString a = taint("foo", null);
        // TODO same as indexOutOfBounds - how should this be handled?
        assertFalse("Tainting with null is not supported", isTainted(a));
    }
}
