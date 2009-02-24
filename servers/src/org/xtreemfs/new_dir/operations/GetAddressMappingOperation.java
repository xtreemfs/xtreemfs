/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.new_dir.operations;

import org.xtreemfs.interfaces.AddressMapping;
import org.xtreemfs.interfaces.AddressMappingSet;
import org.xtreemfs.interfaces.DIRInterface.getAddressMappingsRequest;
import org.xtreemfs.interfaces.DIRInterface.getAddressMappingsResponse;
import org.xtreemfs.new_dir.DIRRequest;
import org.xtreemfs.new_dir.DIRRequestDispatcher;

/**
 *
 * @author bjko
 */
public class GetAddressMappingOperation extends DIROperation {

    private final int operationNumber;

    public GetAddressMappingOperation(DIRRequestDispatcher master) {
        super(master);
        getAddressMappingsRequest tmp = new getAddressMappingsRequest();
        operationNumber = tmp.getOperationNumber();
    }

    @Override
    public int getProcedureId() {
        return operationNumber;
    }

    @Override
    public void startRequest(DIRRequest rq) {
        final getAddressMappingsRequest request = (getAddressMappingsRequest)rq.getRequestMessage();

        AddressMappingSet set = new AddressMappingSet();
        if (request.getUuid().equalsIgnoreCase("localhost")) {
            set.add(new AddressMapping("localhost", 1, "oncrpc", "localhost", 12345, "*", 3600));
        } else {
            set.add(new AddressMapping("YAGGA", 1, "oncrpc", "xtreem.zib.de", 12345, "*", 3600));
        }
        getAddressMappingsResponse response = new getAddressMappingsResponse(set);
        rq.sendSuccess(response);
    }

    @Override
    public boolean isAuthRequired() {
        return false;
    }

    @Override
    public void parseRPCMessage(DIRRequest rq) throws Exception {
        getAddressMappingsRequest amr = new getAddressMappingsRequest();
        rq.deserializeMessage(amr);
    }

}
