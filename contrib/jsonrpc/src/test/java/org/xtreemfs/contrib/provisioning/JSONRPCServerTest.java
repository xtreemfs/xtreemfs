package org.xtreemfs.contrib.provisioning;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;



public class JSONRPCServerTest {

    int requestId = 1;


    // can only be invoked manually after starting tomcat! @Test
    // to run this test,
    // - set the DIR-Port to 32638 in JsonRPC
    // - start tomcat
    // - run tomcat:redeploy
    // - uncomment the @Test annotation
    @SuppressWarnings("unchecked")
    public void testCreateListDeleteVolume() throws JSONRPC2ParseException, JSONRPC2SessionException {
        try {
            String owner = "myUser";
            String ownerGroup = "myGroup";
            String mode = "777";

            // The JSON-RPC 2.0 server URL
            URL serverURL = null;

            try {
                serverURL = new URL("http://mustafa.zib.de:8080/xtreemfs-jsonrpc/executeMethod");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                assert(false);
                return;
            }

            // Create new JSON-RPC 2.0 client session
            JSONRPC2Session mySession = new JSONRPC2Session(serverURL);

            String volumeName = "testVolume";

            JSONRPC2Response res = callJSONRPC(mySession, "listOSDsAndAttributes");
            checkSuccess(res, false);

            // Construct new request
            res = callJSONRPC(mySession, "listVolumes");
            checkSuccess(res, false);

            // cleanup testVolume
            List<Map<String, Object>> volumes = (List<Map<String, Object>>) res.getResult();
            for (Map<String, Object> v : volumes) {
                String volume = (String) v.get("name");
                if (volume.equals(volumeName)) {
                    res = callJSONRPC(mySession, "deleteVolume", volume, "");
                    checkSuccess(res, false);
                }
            }
            res = callJSONRPC(mySession, "createVolume", "testVolume", owner, ownerGroup, mode);
            checkSuccess(res, false);

            res = callJSONRPC(mySession, "listVolumes");
            checkSuccess(res, false);

            res = callJSONRPC(mySession, "deleteVolume", volumeName, "");
            checkSuccess(res, false);

            res = callJSONRPC(mySession, "listVolumes");
            checkSuccess(res, false);
        } catch (JSONRPC2SessionException e) {
            e.printStackTrace();
        }
    }

    protected JSONRPC2Response callJSONRPC(JSONRPC2Session mySession, String method, Object... parameters) throws JSONRPC2ParseException, JSONRPC2SessionException {
        JSONRPC2Request req = new JSONRPC2Request(
                method,
                parameters != null? Arrays.asList(parameters):new ArrayList<String>(),
                "id-"+(++this.requestId));
        mySession.getOptions().setRequestContentType("application/json");
        System.out.println("\tRequest: \n\t" + req);
        return mySession.send(req);
    }

    public static void main(String argv[]) throws Exception {
        JSONRPCServerTest test = new JSONRPCServerTest();
        test.testCreateListDeleteVolume();
    }

    protected void checkSuccess(JSONRPC2Response res, boolean errorExpected) {
        System.out.println("\tResponse: \n\t" + res + "\n");

        if (!errorExpected) {
            assert(res.indicatesSuccess());
            assert(res.getError()==null);
            assert(res.toString().contains("result"));
            assert(!res.toString().contains("error"));
        }
        else {
            assert(res.indicatesSuccess()==false);
            assert(res.getError()!=null);
            assert(!res.toString().contains("result"));
            assert(res.toString().contains("error"));
        }
    }
}
