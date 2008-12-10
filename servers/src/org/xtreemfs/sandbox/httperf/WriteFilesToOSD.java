package org.xtreemfs.sandbox.httperf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.HttpErrorException;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.osd.OSDClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.common.striping.RAID0;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.speedy.MultiSpeedy;

/**
 * writes the files to the OSD which httperf uses for tests later
 * @author clorenz
 */
public class WriteFilesToOSD {

    private final long STRIPE_SIZE = 2;

    private Locations loc;
    private OSDClient client;
    private String capSecret;
    InetSocketAddress osdAddr;
    
    ReusableBuffer buf;

    public WriteFilesToOSD(String host, int port, int filesize, String capSecret) throws IOException{
        Logging.start(Logging.LEVEL_DEBUG);

		ServiceUUID serverID = new ServiceUUID("http://"+java.net.InetAddress.getLocalHost().getCanonicalHostName()+":"+port);
		osdAddr = new InetSocketAddress(host,port);

		List<Location> locations = new ArrayList<Location>(1);
		StripingPolicy sp = new RAID0(STRIPE_SIZE,1);
		List<ServiceUUID> osd = new ArrayList<ServiceUUID>(1);
		osd.add(serverID);
		locations.add(new Location(sp,osd));
		loc = new Locations(locations);

		buf = ReusableBuffer.wrap(generateRandomBytes(filesize));

		MultiSpeedy speedy = new MultiSpeedy();
		speedy.start();
		client = new OSDClient(speedy);
		this.capSecret = capSecret;
    }

    public void writeFiles(String filename) throws NumberFormatException, HttpErrorException, IOException, JSONException, InterruptedException{
        BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));

        String fileId;
        Capability cap;
        int objNo;
        String[] uri;

        String line;
        while((line = file.readLine()) != null) {
        	line = line.trim();
        	if(line.length()==0){	// whitespace line
        		continue;
        	}
        	uri = line.split("&");
            fileId = uri[0];
            objNo = Integer.parseInt(uri[1]);

            cap = new Capability(fileId,"DebugCapability",0, capSecret);

            RPCResponse r = client.put(osdAddr,loc,cap,fileId,objNo,buf);
            r.waitForResponse();
            r.freeBuffers();
        }
        file.close();
    }

    private void init(String filename, int fileAmount, int maxObjAmount, int burstLength){
    	Random rand = new Random();
		LinkedList<String> requests = new LinkedList<String>();

		String fileId;
		int objects;
		// TODO: better algorithm for sessions
		for (int i = 0; i < fileAmount; i++) {
			fileId = generateFileId(rand);
			objects = rand.nextInt(maxObjAmount);
			for(int objNo=0; objNo<objects; objNo++){
				requests.add(fileId + "&" + objNo + "&" + "0");
			}
		}
		Collections.shuffle(requests, rand);

		try {
			BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
			int curBurstLength = 0;
			String writeLine;
			for(String line : requests){
				if(curBurstLength == 0){
					writeLine = line+" \n";
				}else if(curBurstLength > burstLength){
//					writeLine =" \n";
					curBurstLength = -1;
					continue;
				}else{
					writeLine = "   "+line+" \n";
				}
				output.write(writeLine);
				curBurstLength++;
			}
			output.flush();
			output.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private void cleanup(){
    	this.client.getSpeedy().shutdown();
    }

	public static void main(String[] args){
		if(args.length >= 4){
			String host = args[0];
	        int port = Integer.parseInt(args[1]);
	        String fileWithFilenames = args[2];
	        String filesize = args[3];
	        String capSecret = args[4];

	        WriteFilesToOSD writer = null;
			try {
				writer = new WriteFilesToOSD(host,port,Integer.parseInt(filesize), capSecret);
				if(args.length==8 && args[4].equals("init")){
					int fileAmount = Integer.parseInt(args[5]);
					int maxObjAmount = Integer.parseInt(args[6]);
					int burstLength = Integer.parseInt(args[7]);
					writer.init(fileWithFilenames, fileAmount, maxObjAmount, burstLength);
				}
				writer.writeFiles(fileWithFilenames);
			} catch (NullPointerException e) {
				System.out.println("usage: HttPerfRequestController <OSD host> <OSD port> <httPerf wlog-file> <file-size in byte> [init <file amount> <max object amount/file> <session burst length>]");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				if(writer!=null) writer.cleanup();
			}
		}else System.out.println("usage: HttPerfRequestController <OSD host> <OSD port> <httPerf wlog-file> <file-size in byte> [init <file amount> <max object amount/file> <session burst length>]");
	}

	/**
	 * generates randomly filled byte-array
	 *
	 * @param length
	 *            length of the byte-array
	 */
	public static byte[] generateRandomBytes(int length) {
		Random r = new Random(15619681);
		byte[] bytes = new byte[length];

		r.nextBytes(bytes);
		return bytes;
	}

    /**
     * generates randomly Filename
     */
    public static String generateFileId(Random r) throws IllegalArgumentException {
        String id = r.nextInt(100000000) + ":" + r.nextInt(1000000000);
        return id;
    }

}
