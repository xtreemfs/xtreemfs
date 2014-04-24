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
import org.xtreemfs.contrib.provisioning.LibJSON.Response;


@Consumes("application/json")
@Produces("application/json")
public class Rest extends JsonRPC {

  @POST
  @Path("/reserveResources")
  public Response<Machines> reserveResources(Resources res) {
    try {

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
      return new Response<Machines>(
                new Machines(reservations)
             );
      
    } catch (Exception e) {
      return new Response<Machines>(
            null, new LibJSON.Error(e.getLocalizedMessage(), -1)
            );
    }  
  }   
  
  @POST
  @Path("/releaseResources")
  public Response<Object> releaseResources(Reservation res) {
    try {
      LibJSON.releaseReservation(
          res,
          LibJSON.generateSchedulerAddress(schedulerAddress),
          AbstractRequestHandler.getGroups(),
          AbstractRequestHandler.getAuth(this.adminPassword),
          client
          );
      return new Response<Object>(null);
    } catch (Exception e) {
      return new Response<Object>(
            null, new LibJSON.Error(e.getLocalizedMessage(), -1)
            );
    }
  }
  
  @POST
  @Path("/checkReservationStatus")
  public Response<ReservationStatus> checkReservationStatus(Reservation res) throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException {
    try {
      return new Response<ReservationStatus>(
          LibJSON.checkReservation(
            res, 
            dirAddresses, 
            sslOptions, 
            this.client)
          );    
    } catch (Exception e) {
      return new Response<ReservationStatus>(
            null, new LibJSON.Error(e.getLocalizedMessage(), -1)
            );
    }
  }
  
  @POST
  @Path("/getAvailableResources")
  public Response<Resources> getAvailableResources() {
    try {
      return new Response<Resources>(
          LibJSON.getAvailableResources(
            LibJSON.generateSchedulerAddress(schedulerAddress),
            dirAddresses,
            AbstractRequestHandler.getGroups(), 
            AbstractRequestHandler.getAuth(adminPassword),
            client)
          );
    } catch (Exception e) {
      return new Response<Resources>(
            null, new LibJSON.Error(e.getLocalizedMessage(), -1)
            );
    }
  }
  
  
  @POST
  @Path("/listReservations")
  public Response<Addresses> listReservations() {
    try {
      return new Response<Addresses>(
            LibJSON.listReservations(dirAddresses, client)
          );  
    } catch (Exception e) {
      return new Response<Addresses>(
            null, new LibJSON.Error(e.getLocalizedMessage(), -1)
            );
    }
  }

}
