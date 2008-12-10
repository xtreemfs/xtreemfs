package org.xtreemfs.test.scrubber;

import junit.framework.TestCase;
import org.junit.After;
import org.xtreemfs.common.clients.scrubber.FileState;
import org.xtreemfs.common.logging.Logging;

public class ScrubbedFileTest extends TestCase{

    public ScrubbedFileTest() {
        Logging.start(Logging.LEVEL_WARN);
    }

    public void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName() + "."
                + getName());
    }

    @After
    public void tearDown() throws Exception {

    }

    public void testFileState() throws Exception {
        FileState state = new FileState(10,3);
        for(int i = 0; i < 3; i++)
            assertEquals(state.isTodo(i), true);
        
        state.markObjectAsInFlight(0);
        state.markObjectAsInFlight(1);
        state.incorporateReadResult(0, 5);
        assertEquals(state.isFileDone(), true);
        state.incorporateReadResult(1, 0);
        assertEquals(state.isFileDone(), true);
        assertEquals(state.getFileSize(), 5);

        state = new FileState(10,3);
        
        state.markObjectAsInFlight(0);
        state.markObjectAsInFlight(1);
        state.incorporateReadResult(1, 0);
        assertEquals(state.isFileDone(), false);
        state.incorporateReadResult(0, 5);
        assertEquals(state.isFileDone(), true);
        assertEquals(state.getFileSize(), 5);

        state = new FileState(10,3);
        
        state.markObjectAsInFlight(0);
        state.markObjectAsInFlight(1);
        state.incorporateReadResult(0, 10);
        assertEquals(state.isFileDone(), false);
        state.incorporateReadResult(1, 0);
        assertEquals(state.isFileDone(), true);
        assertEquals(state.getFileSize(), 10);

        state = new FileState(10,3);
        
        state.markObjectAsInFlight(0);
        state.markObjectAsInFlight(1);
        state.incorporateReadResult(1, 0);
        assertEquals(state.isFileDone(), false);
        state.incorporateReadResult(0, 10);
        assertEquals(state.isFileDone(), true);
        assertEquals(state.getFileSize(), 10);

        state = new FileState(10,3);
        
        state.markObjectAsInFlight(1);
        state.incorporateReadResult(1, 5);
        assertEquals(state.isFileDone(), false);
        state.markObjectAsInFlight(0);
        state.incorporateReadResult(0, 10);
        assertEquals(state.isFileDone(), true);
        assertEquals(state.getFileSize(), 15);
    }
    
}
