package org.xtreemfs.contrib.provisioning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.contrib.provisioning.LibJSON.Addresses;
import org.xtreemfs.contrib.provisioning.LibJSON.Machines;
import org.xtreemfs.contrib.provisioning.LibJSON.Reservation;
import org.xtreemfs.contrib.provisioning.LibJSON.ReservationStatus;
import org.xtreemfs.contrib.provisioning.LibJSON.Resource;
import org.xtreemfs.contrib.provisioning.LibJSON.Resources;


@Consumes("application/json")
@Produces("application/json")
public class Rest extends JsonRPC {

  @POST
  @Path("/reserveResources")
  public Machines reserveResources(Resources res) throws Exception {
    List<Reservation> reservations = new ArrayList<Reservation>();
    
    // search for storage resource
    for (Resource resource : res.Resources) {
      if (resource.Type.toLowerCase().equals("storage")) {          
        // check for datacenter ID to match DIR ID:
        if (resource.ID.contains(dirAddresses[0].getAddress().getCanonicalHostName())) {

          List<Reservation> currentResult = LibJSON.createReservation(
              resource, 
              LibJSON.generateSchedulerAddress(schedulerAddress), 
              dirAddresses,
              AbstractRequestHandler.getGroups(), 
              AbstractRequestHandler.getAuth(this.adminPassword), 
              client);

          reservations.addAll(currentResult);
        }
      }
    }
    return new Machines(reservations);
  }   
  
  @POST
  @Path("/releaseResources")
  public void releaseResources(Reservation res) throws PosixErrorException, AddressToUUIDNotFoundException, IOException {
    LibJSON.releaseReservation(
        res,
        LibJSON.generateSchedulerAddress(schedulerAddress),
        AbstractRequestHandler.getGroups(),
        AbstractRequestHandler.getAuth(this.adminPassword),
        client
        );
  }
  
  @POST
  @Path("/checkReservationStatus")
  public ReservationStatus checkReservationStatus(Reservation res) throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException {    
    return LibJSON.checkReservation(
        res, 
        dirAddresses, 
        sslOptions, 
        this.client);
  }
  
  @POST
  @Path("/getAvailableResources")
  public Resources getAvailableResources() throws IOException {
    return LibJSON.getAvailableResources(
        LibJSON.generateSchedulerAddress(schedulerAddress),
        dirAddresses,
        AbstractRequestHandler.getGroups(), 
        AbstractRequestHandler.getAuth(adminPassword),
        client);  
  }
  
  
  @POST
  @Path("/listReservations")
  public Addresses listReservations() throws IOException {
    return LibJSON.listReservations(dirAddresses, client);  
  }

}
