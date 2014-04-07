package org.xtreemfs.contrib.provisioning;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.contrib.provisioning.JsonRPC.METHOD;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler.freeResourcesResponse;

public abstract class LibJSON extends AbstractRequestHandler {

  public LibJSON(Client c, METHOD[] methodNames) {
    super(c, methodNames);
  }


  public static Volume openVolume(
      String volume_name, 
      SSLOptions sslOptions,
      Client client)
      throws AddressToUUIDNotFoundException, VolumeNotFoundException,
      IOException {
    Options options = new Options();
    return client.openVolume(volume_name, sslOptions, options);
  }



  public static String generateSchedulerAddress(InetSocketAddress schedulerAddress) {
    InetSocketAddress address = schedulerAddress;
    return address.getHostName() + ":" + address.getPort();
  }
  
  
  public static String[] generateDirAddresses(InetSocketAddress[] dirAddresses) {
    String[] dirAddressesString = new String[dirAddresses.length];
    for (int i = 0; i < dirAddresses.length; i++) {
      InetSocketAddress address = dirAddresses[i];
      dirAddressesString[i] = address.getHostName() + ":" + address.getPort();
    }
    return dirAddressesString;
  }

  
  public static String createNormedVolumeName(String volume_name, InetSocketAddress[] dirAddresses) {
    String[] dirAddressesString = generateDirAddresses(dirAddresses);
    StringBuffer normed_volume_names = new StringBuffer();
    for (String s : dirAddressesString) {
      normed_volume_names.append(s);
      normed_volume_names.append(",");
    }
    normed_volume_names.deleteCharAt(normed_volume_names.length() - 1);
    normed_volume_names.append("/" + volume_name);

    return normed_volume_names.toString();
  }
  
  public static Reservation createReservation(
      Resource resource, 
      String schedulerAddress,
      InetSocketAddress[] dirAddresses,
      UserCredentials uc, 
      Auth auth, 
      Client client) throws IOException {
    String volume_name = "volume-"+UUID.randomUUID().toString();            
    Integer capacity = (int)resource.Attributes.Capacity;
    Integer throughput = (int)resource.Attributes.Throughput;
    ReservationTypes accessType = resource.Attributes.ReservationType;

    boolean randomAccess = accessType == ReservationTypes.RANDOM;

    int octalMode = Integer.parseInt("700", 8);

    // Create volume.
    client.createVolume(
        schedulerAddress, 
        auth,
        uc,
        volume_name,
        octalMode,
        "user",
        "user",
        AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX,          
        128*1024,          
        new ArrayList<KeyValuePair>(),  // volume attributes
        capacity,
        randomAccess? throughput : 0,
            !randomAccess? throughput : 0,
                false);

    // create a string similar to:
    // [<protocol>://]<DIR-server-address>[:<DIR-server-port>]/<Volume Name>
    Reservation result = new Reservation(
        createNormedVolumeName(volume_name, dirAddresses)
        );
    return result;
  }
  

  public static Addresses releaseReservation(
      Reservation res,
      String schedulerAddress,
      UserCredentials uc, 
      Auth auth, 
      Client client) throws IOException,
      PosixErrorException, AddressToUUIDNotFoundException {
    String volume_name = res.InfReservID;
    volume_name = AbstractRequestHandler.stripVolumeName(volume_name);

    // first delete the volume
    client.deleteVolume(
        auth,
        uc, 
        volume_name);

    // now delete the reservation of rerources
    client.deleteReservation(
        schedulerAddress, 
        auth, 
        uc, 
        volume_name);

    Addresses addresses = new Addresses(
        volume_name 
        );

    return addresses;
  }
  
  
  public static Addresses checkReservation(
      Reservation res,
      InetSocketAddress[] dirAddresses,
      SSLOptions sslOptions,
      Client client
      )
          throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException {
    String volume_name = res.InfReservID; // required
    volume_name = AbstractRequestHandler.stripVolumeName(volume_name);

    LibJSON.openVolume(volume_name, sslOptions, client);

    // return a string like
    // [<protocol>://]<DIR-server-address>[:<DIR-server-port>]/<Volume Name>
    Addresses addresses = new Addresses(
        createNormedVolumeName(volume_name, dirAddresses)
        );
    return addresses;
  }
  
  
  public static Addresses listReservations(
      InetSocketAddress[] dirAddresses,
      Client client) throws IOException {
    // list volumes           
    ArrayList<String> volumeNames = new ArrayList<String>();

    String[] volumes = client.listVolumeNames();
    for (String volume_name : volumes) {
      volumeNames.add(createNormedVolumeName(volume_name, dirAddresses));
    }

    Addresses addresses = new Addresses(
        volumeNames
        );
    return addresses;
  }
  
  public static Resources getAvailableResources(
      String schedulerAddress,
      InetSocketAddress[] dirAddresses,
      UserCredentials uc, 
      Auth auth, 
      Client client) throws IOException {
    
    freeResourcesResponse freeResources
    = client.getFreeResources(
        schedulerAddress, 
        auth, 
        uc);    

    Resources res = new Resources();

    // random
    res.addResource(
        new Resource(
            dirAddresses[0].getAddress().getCanonicalHostName()+"/storage/random",
            dirAddresses[0].getAddress().getHostAddress(),
            "Storage",
            new Attributes(
                freeResources.getRandomCapacity(),
                freeResources.getRandomThroughput(),
                ReservationTypes.RANDOM
                ),
                new Costs(
                    0,
                    0
                    )
            ));

    // sequential
    res.addResource(
        new Resource(
            dirAddresses[0].getAddress().getCanonicalHostName()+"/storage/sequential",
            dirAddresses[0].getAddress().getHostAddress(),
            "Storage",
            new Attributes(
                freeResources.getStreamingCapacity(),
                freeResources.getStreamingThroughput(),
                ReservationTypes.SEQUENTIAL
                ),
                new Costs(
                    0,
                    0
                    )
            ));
    
    return res;
  }  
  
  public static class Addresses {
    public List<String> Addresses;
    public Addresses() {
      // no-args constructor
    }
    public Addresses(List<String> addresses) {
      this.Addresses = addresses;
    }
    public Addresses(String[] addresses) {
      this.Addresses = Arrays.asList(addresses);
    }
    public Addresses(String address) {
      this.Addresses = new ArrayList<String>();
      this.Addresses.add(address);
    }
  }
  
  public static class Reservation {
    public String InfReservID;
    public Reservation() {
      // no-args constructor
    }
    public Reservation(String reservationID) {
      this.InfReservID = reservationID;
    }
  }

  public static class Resources {
    public List<Resource> Resources;
    public Resources() {
      // no-args constructor
    }
    public void addResource(Resource r) {
      if (Resources == null) {
        Resources = new ArrayList<Resource>();
      }
      Resources.add(r);
    }
  }
  
  public static class Resource {
    public String ID;
    public String IP;
    public String Type;
    public Attributes Attributes;
    public Costs Costs;
    public Resource() {
      // no-args constructor
    }
    public Resource(
        String id,
        String ip,
        String type,
        Attributes attributes,
        Costs costs) {
      this.ID = id;
      this.IP = ip;
      this.Type = type;
      this.Attributes = attributes;
      this.Costs = costs;
    }
  }

  public static enum ReservationTypes {
    SEQUENTIAL, RANDOM
  }

  public static class Attributes {
    public double Capacity;
    public double Throughput;
    public ReservationTypes ReservationType;
    public Attributes() {
      // no-args constructor
    }
    public Attributes(
        double capacity,
        double throughput,
        ReservationTypes reservationType) {
      // no-args constructor
      this.Capacity = capacity;
      this.Throughput = throughput;
      this.ReservationType = reservationType;
    }
  }
  
  public static class Costs {
    public double Capacity;
    public double Throughput;
    public Costs() {
      // no-args constructor
    }
    public Costs(double capacity, double throughput) {
      this.Capacity = capacity;
      this.Throughput= throughput;
    }
  }

}
