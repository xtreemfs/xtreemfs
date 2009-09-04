/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.foundation.oncrpc.server;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_get_by_typeResponse;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.OSDInterface.writeRequest;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceDataMap;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import static org.junit.Assert.*;

/**
 *
 * @author bjko
 */
public class XDRTest extends TestCase {

    public XDRTest() {
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

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

    public void testXDR() {


        ServiceSet s = new ServiceSet();
        ServiceDataMap sdm = new ServiceDataMap();
        sdm.put("k", "v");
        sdm.put("k2", "v");
        s.add(new Service(ServiceType.SERVICE_TYPE_MRC, "UUID", 1, "name", 1, sdm));
        s.add(new Service(ServiceType.SERVICE_TYPE_MRC, "UUID2", 2, "name2", 2, sdm));
        xtreemfs_service_get_by_typeResponse r = new xtreemfs_service_get_by_typeResponse(s);

        ONCRPCBufferWriter wr = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        r.marshal(wr);
        wr.flip();
        XDRUnmarshaller um = new XDRUnmarshaller(wr.getBuffers().get(0));

        xtreemfs_service_get_by_typeResponse r2 = new xtreemfs_service_get_by_typeResponse();
        r2.unmarshal(um);

    }

    public void testBuffers() {

        ReusableBuffer rb = BufferPool.allocate(1023);
        for (int i = 0; i < 1023; i++)
            rb.put((byte)'A');


        writeRequest r = new writeRequest();
        r.setFile_credentials(new FileCredentials());
        r.setObject_data(new ObjectData(0, false, 0, rb));

        ONCRPCBufferWriter wr = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        r.marshal(wr);
        wr.flip();
        
        int size = 0;
        for (ReusableBuffer b : wr.getBuffers())
            size += b.limit();

        ReusableBuffer buf = BufferPool.allocate(size);
        for (ReusableBuffer b : wr.getBuffers())
            buf.put(b);

        buf.flip();

        XDRUnmarshaller um = new XDRUnmarshaller(buf);

        writeRequest r2 = new writeRequest();
        r2.unmarshal(um);
        assertNotNull(r2.getObject_data().getData());

    }

}