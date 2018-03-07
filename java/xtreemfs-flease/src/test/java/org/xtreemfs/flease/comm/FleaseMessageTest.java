/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.flease.comm;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import static org.junit.Assert.*;

/**
 *
 * @author bjko
 */
public class FleaseMessageTest {

    public FleaseMessageTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSerialization() throws Exception {
        FleaseMessage m1 = new FleaseMessage(FleaseMessage.MsgType.MSG_LEARN);
        m1.setCellId(new ASCIIString("testcell"));
        m1.setLeaseHolder(new ASCIIString("yagga"));
        m1.setLeaseTimeout(123456789l);
        m1.setPrevProposalNo(new ProposalNumber(12, 178743687));
        m1.setProposalNo(new ProposalNumber(15, 736456));
        m1.setSendTimestamp(49789834);
        m1.setViewId(55685);
        m1.setMasterEpochNumber(52455115211l);

        final int size = m1.getSize();
        ReusableBuffer rb = BufferPool.allocate(size);

        m1.serialize(rb);


        rb.flip();

        FleaseMessage m2 = new FleaseMessage(rb);

        assertEquals(m1.getCellId(),m2.getCellId());
        assertEquals(m1.getLeaseHolder(),m2.getLeaseHolder());
        assertEquals(m1.getLeaseTimeout(),m2.getLeaseTimeout());
        assertEquals(m1.getMsgType(),m2.getMsgType());
        assertEquals(m1.getPrevProposalNo().getProposalNo(),m2.getPrevProposalNo().getProposalNo());
        assertEquals(m1.getPrevProposalNo().getSenderId(),m2.getPrevProposalNo().getSenderId());
        assertEquals(m1.getProposalNo().getProposalNo(),m2.getProposalNo().getProposalNo());
        assertEquals(m1.getProposalNo().getSenderId(),m2.getProposalNo().getSenderId());
        assertEquals(m1.getSendTimestamp(),m2.getSendTimestamp());
        assertEquals(m1.getViewId(),m2.getViewId());
        assertEquals(m1.getMasterEpochNumber(),m2.getMasterEpochNumber());
    }

}