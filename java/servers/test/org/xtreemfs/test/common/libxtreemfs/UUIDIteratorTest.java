/*
 * Copyright (c) 2008-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.common.libxtreemfs;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.After;
import org.xtreemfs.common.libxtreemfs.UUIDIterator;
import org.xtreemfs.common.libxtreemfs.exceptions.UUIDIteratorListIsEmpyException;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;




/**
 *
 * <br>Sep 6, 2011
 */
public class UUIDIteratorTest extends TestCase {

    DIRRequestDispatcher dir;

    TestEnvironment      testEnv;

    DIRConfig            dirConfig;

    UserCredentials      userCredentials;

    Auth                 auth = RPCAuthentication.authNone;

    DIRClient            dirClient;
    
    /**
     * 
     */
    public UUIDIteratorTest() throws IOException {
        dirConfig = SetupUtils.createDIRConfig();
        Logging.start(Logging.LEVEL_DEBUG);
    }
    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        System.out.println("TEST: " + getClass().getSimpleName());

        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));
        
    }
    
    @After
    public void tearDown() throws Exception {


    }
    
    public void testUUIDIterator() throws Exception {
        
        final String UUID_STRING1 = "uuidstring1";
        final String UUID_STRING2 = "uuidstring2";
        final String UUID_STRING3 = "uuidstring3";
        
        UUIDIterator uuidIterator = new UUIDIterator();
        
        uuidIterator.addUUID(UUID_STRING1);
        uuidIterator.addUUID(UUID_STRING2);
        uuidIterator.addUUID(UUID_STRING3);
        
        // getting the current UUID should not increase the pointer
        assertEquals(UUID_STRING1, uuidIterator.getUUID());
        assertEquals(UUID_STRING1, uuidIterator.getUUID());
     
        
        // mark current UUID as faield. UUID pointer should be increased.
        uuidIterator.markUUIDAsFailed(uuidIterator.getUUID());
        assertEquals(UUID_STRING2, uuidIterator.getUUID());

        uuidIterator.markUUIDAsFailed(uuidIterator.getUUID());
        assertEquals(UUID_STRING3, uuidIterator.getUUID());
        
        // The end of the UUID List is now reached. uuidIterator should start from beginning of the
        // list.
        uuidIterator.markUUIDAsFailed(uuidIterator.getUUID());
        assertEquals(UUID_STRING1, uuidIterator.getUUID());
        
        // After clearing the UUIDIterator and getUUID call should raise an UUIDIteratorListIsEmpyException
        uuidIterator.clear();
        try {
            uuidIterator.getUUID();
            fail("UUIDIteratorListIsEmptyExcaption should have been raised");
        } catch (UUIDIteratorListIsEmpyException e) {
        }
        
        // clear and add should set current UUID to the added uuid.
        uuidIterator.clearAndAddUUID(UUID_STRING1);
        assertEquals(UUID_STRING1, uuidIterator.getUUID());
        
        // since there is only one UUID in the iterator after marking that as failed it should return
        // the same UUID
        uuidIterator.markUUIDAsFailed(uuidIterator.getUUID());
        assertEquals(UUID_STRING1, uuidIterator.getUUID());
        
        // debug string should also work!
        uuidIterator.debugString();
        
    }
    
    public void testAddUuidCollection() {
        List<String> uuidList = new LinkedList<String>();
        uuidList.add("uuidstring1");
        uuidList.add("uuidstring2");
        uuidList.add("uuidstring3");
        
        UUIDIterator iterator = new UUIDIterator();
        iterator.addUUIDs(uuidList);
        
        assertEquals(3, iterator.size());
    }
    
    public void testSetCurrentUuid() throws Exception {
        final String UUID_STRING1 = "uuidstring1";
        final String UUID_STRING2 = "uuidstring2";
        final String UUID_STRING3 = "uuidstring3";
        
        UUIDIterator iterator = new UUIDIterator();
        iterator.addUUID(UUID_STRING1);
        iterator.addUUID(UUID_STRING2);
        
        assertEquals(2, iterator.size());
        assertEquals(UUID_STRING1, iterator.getUUID());

        // uuid already in iterator. Should only be set.
        iterator.setCurrentUUID(UUID_STRING2);
        assertEquals(UUID_STRING2, iterator.getUUID());
        assertEquals(2, iterator.size());
        
        // new UUID, should be added and set!
        iterator.setCurrentUUID(UUID_STRING3);
        assertEquals(UUID_STRING3, iterator.getUUID());
        assertEquals(3, iterator.size());
    }

}
