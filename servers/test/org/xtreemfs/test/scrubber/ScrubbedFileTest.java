/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Nele Andersen (ZIB)
 */
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
