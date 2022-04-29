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
