package org.xtreemfs.contrib.provisioning;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler.freeResourcesResponse;
public class LibJSON {


  public static Volume openVolume(
      String volume_name, 
      SSLOptions sslOptions,
      Client client)
      throws AddressToUUIDNotFoundException, VolumeNotFoundException,
      IOException {
    Options options = new Options();
    return client.openVolume(volume_name, sslOptions, options);
  }

  public static String stripVolumeName(String volume_name) {
    if (volume_name.contains("/")) {
      String[] parts = volume_name.split("/");
      volume_name = parts[parts.length-1];
    }
    return volume_name;
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
  
  public static List<Reservation> createReservation(
      Resource resource, 
      String schedulerAddress,
      InetSocketAddress[] dirAddresses,
      UserCredentials uc, 
      Auth auth, 
      Client client) throws IOException {
    Integer capacity = (int)resource.Attributes.Capacity;
    Integer throughput = (int)resource.Attributes.Throughput;
    ReservationTypes accessType = resource.Attributes.ReservationType;
    boolean randomAccess = accessType == ReservationTypes.RANDOM;

    int octalMode = Integer.parseInt("700", 8);
    List<Reservation> volumes = new ArrayList<Reservation>();
    
    for (int i = 0; i < resource.NumInstances; i++) {
      String volume_name = "volume-"+UUID.randomUUID().toString();            

      // Create the volumes
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
      volumes.add(
          new Reservation(
              createNormedVolumeName(volume_name, dirAddresses)
              )
          );
          
    }
    
    return volumes;
  }
  

  public static Addresses releaseReservation(
      Reservation res,
      String schedulerAddress,
      UserCredentials uc, 
      Auth auth, 
      Client client) throws IOException,
      PosixErrorException, AddressToUUIDNotFoundException {
    String volume_name = res.IResID;
    volume_name = stripVolumeName(volume_name);

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
  
  
  public static ReservationStatus checkReservation(
      Reservation res,
      InetSocketAddress[] dirAddresses,
      SSLOptions sslOptions,
      Client client
      )
          throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException {
        
    String volume_name = res.IResID; // required
    volume_name = stripVolumeName(volume_name);

    LibJSON.openVolume(volume_name, sslOptions, client);

    // return a string like
    // [<protocol>://]<DIR-server-address>[:<DIR-server-port>]/<Volume Name>
    ReservationStatus reservStatus = new ReservationStatus(
        true, 
        createNormedVolumeName(volume_name, dirAddresses)
        );
    
    return reservStatus;
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
                1,
                new Cost(
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
                1,
                new Cost(
                    0,
                    0
                    )
            ));
    
    return res;
  }  
  
  @XmlRootElement(name="ReservationStatus")
  @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
  public static class ReservationStatus implements Serializable {
    private static final long serialVersionUID = -5811456962763091947L;
    public boolean Ready;
    public List<String> Addresses;
    public ReservationStatus() {
      // no-args constructor
    }    
    public ReservationStatus(boolean ready, List<String> addresses) {
      this.Ready = ready;
      this.Addresses = addresses;
    }
    public ReservationStatus(boolean ready, String address) {
      this.Ready = ready;
      this.Addresses = new ArrayList<String>();
      this.Addresses.add(address);
    }
    public boolean isReady() {
      return Ready;
    }
    public void setReady(boolean ready) {
      Ready = ready;
    }
    public List<String> getAddresses() {
      return Addresses;
    }
    public void setAddresses(List<String> addresses) {
      Addresses = addresses;
    } 
  }
  
  @XmlRootElement(name="Addresses")
  @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
  public static class Addresses implements Serializable {
    private static final long serialVersionUID = -6291321674682669013L;
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
    public List<String> getAddresses() {
      return Addresses;
    }
    public void setAddresses(List<String> addresses) {
      Addresses = addresses;
    }
  }
  
  @XmlRootElement(name="Machines")
  @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
  public static class Machines implements Serializable {
    private static final long serialVersionUID = -7391050821048296071L;
    public List<Reservation> Resources;
    public Machines() {
      // no-args constructor
    }
    public Machines(List<Reservation> machines) {
      this.Resources = machines;
    }
    public List<Reservation> getResources() {
      return Resources;
    }
    public void setResources(List<Reservation> machines) {
      Resources = machines;
    }
  }
  
  @XmlRootElement(name="Reservation")
  @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
  public static class Reservation implements Serializable {
    private static final long serialVersionUID = 5629110247326464140L;
    public String IResID;
    public Reservation() {
      // no-args constructor
    }
    public Reservation(String reservationID) {
      this.IResID = reservationID;
    }
    public String getIResID() {
      return IResID;
    }
    public void setIResID(String infReservID) {
      IResID = infReservID;
    }
  }

  @XmlRootElement(name="Resources")
  @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)  
  public static class Resources implements Serializable {
    private static final long serialVersionUID = 9199843708622395018L;
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
    public List<Resource> getResources() {
      return Resources;
    }
    public void setResources(List<Resource> resources) {
      Resources = resources;
    }
  }
  
  @XmlRootElement(name="Resource")
  @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
  public static class Resource implements Serializable {
    private static final long serialVersionUID = 831790831396236645L;
    public String ID;
    public String IP;
    public String Type;
    public Attributes Attributes;
    public int NumInstances;
    public Cost Cost;
    public Resource() {
      // no-args constructor
    }
    public Resource(
        String id,
        String ip,
        String type,
        Attributes attributes,
        int numInstances,
        Cost costs) {
      this.ID = id;
      this.IP = ip;
      this.Type = type;
      this.Attributes = attributes;
      this.NumInstances = numInstances;
      this.Cost = costs;
    }
    public String getID() {
      return ID;
    }
    public void setID(String iD) {
      ID = iD;
    }
    public String getIP() {
      return IP;
    }
    public void setIP(String iP) {
      IP = iP;
    }
    public String getType() {
      return Type;
    }
    public void setType(String type) {
      Type = type;
    }
    public Attributes getAttributes() {
      return Attributes;
    }
    public void setAttributes(Attributes attributes) {
      Attributes = attributes;
    }
    public Cost getCost() {
      return Cost;
    }
    public void setCost(Cost costs) {
      Cost = costs;
    }
    public int getNumInstances() {
      return NumInstances;
    }
    public void setNumInstances(int numInstances) {
      NumInstances = numInstances;
    }
  }

  @XmlRootElement(name="ReservationTypes")
  public static enum ReservationTypes implements Serializable {
    SEQUENTIAL, RANDOM
  }

  @XmlRootElement(name="Attributes")
  @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
  public static class Attributes implements Serializable {
    private static final long serialVersionUID = -5867485593384557874L;
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
    public double getCapacity() {
      return Capacity;
    }
    public void setCapacity(double capacity) {
      Capacity = capacity;
    }
    public double getThroughput() {
      return Throughput;
    }
    public void setThroughput(double throughput) {
      Throughput = throughput;
    }
    public ReservationTypes getReservationType() {
      return ReservationType;
    }
    public void setReservationType(ReservationTypes reservationType) {
      ReservationType = reservationType;
    }
  }
  
  @XmlRootElement(name="Cost")
  @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
  public static class Cost implements Serializable {
    private static final long serialVersionUID = -1952012295370219752L;
    public double Capacity;
    public double Throughput;
    public Cost() {
      // no-args constructor
    }
    public Cost(double capacity, double throughput) {
      this.Capacity = capacity;
      this.Throughput= throughput;
    }
    public double getCapacity() {
      return Capacity;
    }
    public void setCapacity(double capacity) {
      Capacity = capacity;
    }
    public double getThroughput() {
      return Throughput;
    }
    public void setThroughput(double throughput) {
      Throughput = throughput;
    }
  }

  @XmlRootElement(name="Response")
  @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
  public static class Response<E> implements Serializable {
    private static final long serialVersionUID = 4850691130923087378L;
    public E result;
    public Error error;
    public Response() {
      // no-args constructor
    }
    public Response(E result, Error error) {
      this.error = error;
      this.result = result;
    }
    public Response(E result) {
      this.result = result;
      this.error = null;
    }
    public Object getResult() {
      return result;
    }
    public void setResult(E result) {
      this.result = result;
    }
    public Error getError() {
      return error;
    }
    public void setError(Error error) {
      this.error = error;
    }
  }
  
  
  @XmlRootElement(name="Error")
  @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
  public static class Error implements Serializable {
    private static final long serialVersionUID = -2989036582502777333L;
    public String message;
    public int code;
    public Error() {
      // no-args constructor
    }
    public Error(String message, int code) {
      this.message = message;
      this.code = code;
    }
    public String getMessage() {
      return message;
    }
    public void setMessage(String message) {
      this.message = message;
    }
  }
  
}
