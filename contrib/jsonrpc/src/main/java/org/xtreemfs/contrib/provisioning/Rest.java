package org.xtreemfs.contrib.provisioning;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.contrib.provisioning.LibJSON.Addresses;
import org.xtreemfs.contrib.provisioning.LibJSON.ReservationStati;
import org.xtreemfs.contrib.provisioning.LibJSON.Reservations;
import org.xtreemfs.contrib.provisioning.LibJSON.Resource;
import org.xtreemfs.contrib.provisioning.LibJSON.ResourceCapacity;
import org.xtreemfs.contrib.provisioning.LibJSON.ResourceMapper;
import org.xtreemfs.contrib.provisioning.LibJSON.Resources;
import org.xtreemfs.contrib.provisioning.LibJSON.Response;
import org.xtreemfs.contrib.provisioning.LibJSON.Types;


@Consumes("application/json")
@Produces("application/json")
public class Rest extends JsonRPC {

    @POST
    @Path("/reserveResources")
    public Response<Reservations> reserveResources(Resources res) {
        try {
            return new Response<Reservations>(LibJSON.reserveResources(
                    res,
                    LibJSON.generateSchedulerAddress(schedulerAddress),
                    dirAddresses,
                    AbstractRequestHandler.getGroups(),
                    AbstractRequestHandler.getAuth(this.adminPassword),
                    client));

        } catch (Exception e) {
            return new Response<Reservations>(
                    null, new LibJSON.Error(e.getLocalizedMessage(), -1)
            );
        }
    }

    @POST
    @Path("/releaseResources")
    public Response<Object> releaseResources(Reservations res) {
        try {
            LibJSON.releaseResources(
                    res,
                    LibJSON.generateSchedulerAddress(schedulerAddress),
                    AbstractRequestHandler.getGroups(),
                    AbstractRequestHandler.getAuth(this.adminPassword),
                    client
            );
        } catch (Exception e) {
            return new Response<Object>(
                    null, new LibJSON.Error(e.getLocalizedMessage(), -1)
            );
        }
        return new Response<Object>(null);
    }

    @POST
    @Path("/calculateResourceAgg")
    public Response<Resource> calculateResourceAgg(Resources res) {
        try {
            return new Response<Resource>(LibJSON.calculateResourceAgg(
                    res,
                    LibJSON.generateSchedulerAddress(schedulerAddress),
                    AbstractRequestHandler.getGroups(),
                    AbstractRequestHandler.getAuth(this.adminPassword),
                    client));
        } catch (Exception e) {
            return new Response<Resource>(
                    null, new LibJSON.Error(e.getLocalizedMessage(), -1)
            );
        }
    }

    @POST
    @Path("/calculateResourceCapacity")
    public Response<ResourceMapper> calculateResourceCapacity(ResourceCapacity res) {
        try {
            return new Response<ResourceMapper>(LibJSON.calculateResourceCapacity(
                    res,
                    LibJSON.generateSchedulerAddress(schedulerAddress),
                    AbstractRequestHandler.getGroups(),
                    AbstractRequestHandler.getAuth(this.adminPassword),
                    client));
        } catch (Exception e) {
            return new Response<ResourceMapper>(
                    null, new LibJSON.Error(e.getLocalizedMessage(), -1)
            );
        }
    }

    @POST
    @Path("/getResourceTypes")
    public Response<Types> getResourceTypes() {
        try {
            return new Response<Types>(LibJSON.getResourceTypes());
        } catch (Exception e) {
            return new Response<Types>(
                    null, new LibJSON.Error(e.getLocalizedMessage(), -1)
            );
        }
    }

    @POST
    @Path("/verifyResources")
    public Response<ReservationStati> verifyResources(Reservations res)
            throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException {
        try {
            return new Response<ReservationStati>(LibJSON.verifyResources(
                    res,
                    LibJSON.generateSchedulerAddress(schedulerAddress),
                    dirAddresses,
                    sslOptions,
                    AbstractRequestHandler.getGroups(),
                    AbstractRequestHandler.getAuth(this.adminPassword),
                    this.client
            ));
        } catch (Exception e) {
            return new Response<ReservationStati>(
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
