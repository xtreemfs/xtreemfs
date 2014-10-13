package org.xtreemfs.contrib.provisioning;

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
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler.freeResourcesResponse;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


/**
 * FIXME - add a call calculateResourceCapacity which estimates the remaining 
 *         capacity for a schedule.
 *
 * DONE ...
 *       - add method getResourceTypes
 *       - semantics of verifyResources
 *       - semantics of releaseResources: gets a list of resourceIds
 *       - reservationtype => accesstype
 *       - drop numinstances
 *       - checkReservationStatus => verifyResources
 *       - verifyResources => Addresses => Address
 *       - releaseReservation => releaseResources
 *
 * @author bzcschae
 *
 */
public class LibJSON {

    private static IRMConfig irmConfig = null;

    public static void setIrmConfig(IRMConfig config) {
        LibJSON.irmConfig = config;
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

    public static Reservations reserveResources(
            Resources res,
            String schedulerAddress,
            InetSocketAddress[] dirAddresses,
            UserCredentials uc,
            Auth auth,
            Client client) throws Exception {
        Reservations reservations = new Reservations();

        try {
            // search for storage resource
            for (Resource resource : res.Resources) {
                if (resource.Type.toLowerCase().equals("storage")) {
                    Integer capacity = (int)resource.Attributes.Capacity;
                    Integer throughput = (int)resource.Attributes.Throughput;
                    AccessTypes accessType = resource.Attributes.AccessType;
                    boolean randomAccess = accessType == AccessTypes.RANDOM;

                    int octalMode = Integer.parseInt("700", 8);

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
                    reservations.addAll(new Reservations(
                            createNormedVolumeName(volume_name, dirAddresses)
                    ));
                }
            }
            return reservations;

        } catch (Exception e) {
            try {
                // free all resources, if we could not reserve all required resources.
                releaseResources(
                        reservations,
                        schedulerAddress,
                        uc,
                        auth,
                        client
                );
            } catch (Exception ee) {
                ee.printStackTrace();
            }
            throw e;
        }
    }


    public static void releaseResources(
            Reservations res,
            String schedulerAddress,
            UserCredentials uc,
            Auth auth,
            Client client) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        if (res != null && res.getReservations() != null) {
            for (String volume : res.getReservations()) {

                String volume_name = stripVolumeName(volume);

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
            }
        }
    }


    public static ReservationStati verifyResources(
            Reservations res,
            String schedulerAddress,
            InetSocketAddress[] dirAddresses,
            SSLOptions sslOptions,
            UserCredentials uc,
            Auth auth,
            Client client
    ) throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException {

        ReservationStati stati = new ReservationStati();

        for (String volume_name : res.getReservations()) {

            volume_name = stripVolumeName(volume_name);

            LibJSON.openVolume(volume_name, sslOptions, client);

            // obtain the size of the reservation
            Scheduler.reservation reservation = client.getReservation(
                    schedulerAddress,
                    auth,
                    uc,
                    volume_name);

//      boolean sequential = reservation.getType() == Scheduler.reservationType.STREAMING_RESERVATION;
            boolean random = reservation.getType() == Scheduler.reservationType.RANDOM_IO_RESERVATION;

            // return a string like
            // [<protocol>://]<DIR-server-address>[:<DIR-server-port>]/<Volume Name>
            ReservationStatus reservStatus = new ReservationStatus(
                    true,
                    createNormedVolumeName(volume_name, dirAddresses),
                    new Resource(
                            // FIXME adapt to
                            //      "ID":"/DataCenterID/RackID/IP/type/host_name"
                            "/XtreemFS/RackID/"
                                    + dirAddresses[0].getAddress().getHostAddress()
              /*+ dirAddresses[0].getAddress().getCanonicalHostName()+*/
                                    + "/storage/" + (random? "random":"sequential"),
                            dirAddresses[0].getAddress().getHostAddress(),
                            "Storage",
                            new Attributes(
                                    reservation.getCapacity(),
                                    random? reservation.getRandomThroughput() : reservation.getStreamingThroughput(),
                                    random? AccessTypes.RANDOM : AccessTypes.SEQUENTIAL
                            ),
                            new Cost(
                                    getSequentialCost(),
                                    getCapacityCost()
                            )
                    )
            );

            stati.addReservationStatus(reservStatus);
        }

        return stati;
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


    public static Resource calculateResourceAgg(
            Resources resources,
            String schedulerAddress,
            UserCredentials uc,
            Auth auth,
            Client client) throws IOException {

        // obtain the free resources as upper limit
        freeResourcesResponse freeResources
                = client.getFreeResources(
                schedulerAddress,
                auth,
                uc);

        double newThroughput = 0.0;
        double newCapacity = 0.0;

        Resource firstResource = resources.getResources().iterator().next();
        AccessTypes type = firstResource.getAttributes().getAccessType();

        // calculate the aggregation
        for (Resource resource : resources.getResources()) {
            try {
                newCapacity += resource.getAttributes().getCapacity();
            } catch (Exception e) {
                // silent
            }
            try {
                newThroughput = Math.max(resource.getAttributes().getThroughput(), newThroughput);
            } catch (Exception e) {
                // silent
            }
        }

        // limit by available resources
        newCapacity = Math.min(
                type == AccessTypes.RANDOM ? freeResources.getRandomCapacity() : freeResources.getStreamingCapacity(),
                newCapacity);
        newThroughput = Math.min(
                type == AccessTypes.RANDOM ? freeResources.getRandomThroughput() : freeResources.getStreamingThroughput(),
                newThroughput);

        return new Resource(
                firstResource.getID(),
                firstResource.getIP(),
                "Storage",
                new Attributes(
                        newCapacity,
                        newThroughput,
                        type
                ),
                firstResource.getCost()
        );
    }


    public static ResourceMapper calculateResourceCapacity(
            ResourceCapacity resourceCapacity,
            String schedulerAddress,
            UserCredentials uc,
            Auth auth,
            Client client) throws IOException {

        freeResourcesResponse freeResources
                = client.getFreeResources(
                schedulerAddress,
                auth,
                uc);

        ReserveResource reserve = resourceCapacity.getReserve();
        ReleaseResource release = resourceCapacity.getRelease();

        AccessTypes type = resourceCapacity.getResource().getAttributes().getAccessType();
        double remainingCapacity = resourceCapacity.getResource().getAttributes().getCapacity();
        double remainingThrough = resourceCapacity.getResource().getAttributes().getThroughput();

        if (reserve != null && reserve.getAttributes() != null) {
            for (Attributes attr : reserve.getAttributes()) {
                try {
                    remainingCapacity -= attr.getCapacity();
                } catch (Exception e) {
                    // silent
                }
                try {
                    remainingThrough -= attr.getThroughput();
                } catch (Exception e) {
                    // silent
                }
            }
        }

        if (release != null && release.getAttributes() != null) {
            for (Attributes attr : release.getAttributes()) {
                try {
                    remainingCapacity += attr.getCapacity();
                } catch (Exception e) {
                    // silent
                }
                try {
                    remainingThrough += attr.getThroughput();
                } catch (Exception e) {
                    // silent
                }
            }
        }

        remainingCapacity = Math.min(
                type == AccessTypes.RANDOM ? freeResources.getRandomCapacity() : freeResources.getStreamingCapacity(),
                remainingCapacity);
        remainingThrough = Math.min(
                type == AccessTypes.RANDOM ? freeResources.getRandomThroughput() : freeResources.getStreamingThroughput(),
                remainingThrough);

        return new ResourceMapper(
                new Resource(
                        resourceCapacity.getResource().getID(),
                        resourceCapacity.getResource().getIP(),
                        resourceCapacity.getResource().getType(),
                        new Attributes(
                                remainingCapacity,
                                remainingThrough,
                                type
                        ),
                        resourceCapacity.getResource().getCost()
                )
        );
    }


    public static Types getResourceTypes() {
        Types types = new Types();
        types.addType(new Type(
                "Storage",
                new AttributesDesc(
                        new CapacityDesc("The capacity of the storage device.", "double"),
                        new ThroughputDesc("The throughput of the storage device. Either in MB/s or IOPS.", "double"),
                        new AccessTypeDesc("The access type of the storage device. One of SEQUENTIAL or RANDOM", "string"))
        ));
        return types;
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
                        // TODO adapt to
                        //      "ID":"/DataCenterID/RackID/IP/type/host_name"
                        "/XtreemFS/RackID/"
                                + dirAddresses[0].getAddress().getHostAddress()
            /*+ dirAddresses[0].getAddress().getCanonicalHostName()+*/
                                + "/storage/random",
                        dirAddresses[0].getAddress().getHostAddress(),
                        "Storage",
                        new Attributes(
                                freeResources.getRandomCapacity(),
                                freeResources.getRandomThroughput(),
                                AccessTypes.RANDOM
                        ),
                        new Cost(
                                getCapacityCost(),
                                getRandomCost()
                        )
                ));

        // sequential
        res.addResource(
                new Resource(
                        // TODO adapt to
                        //      "ID":"/DataCenterID/RackID/IP/type/host_name"
                        "/XtreemFS/RackID/"
                                + dirAddresses[0].getAddress().getHostAddress()
            /*+ dirAddresses[0].getAddress().getCanonicalHostName()+*/
                                + "/storage/sequential",
                        dirAddresses[0].getAddress().getHostAddress(),
                        "Storage",
                        new Attributes(
                                freeResources.getStreamingCapacity(),
                                freeResources.getStreamingThroughput(),
                                AccessTypes.SEQUENTIAL
                        ),
                        new Cost(
                                getCapacityCost(),
                                getSequentialCost()
                        )
                ));

        return res;
    }


    @XmlRootElement(name="ReservationStati")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class ReservationStati implements Serializable {
        private static final long serialVersionUID = -6391605855750581473L;
        public List<ReservationStatus> Reservations;
        public ReservationStati() {
            // no-args constructor
        }
        public ReservationStati(List<ReservationStatus> reservationStatus) {
            this.Reservations = reservationStatus;
        }
        public List<ReservationStatus> getReservations() {
            return Reservations;
        }
        public void setReservations(List<ReservationStatus> reservations) {
            Reservations = reservations;
        }
        public void addReservationStatus(ReservationStatus reservation) {
            if (this.Reservations == null) {
                this.Reservations = new ArrayList<LibJSON.ReservationStatus>();
            }
            Reservations.add(reservation);
        }

    }

    @XmlRootElement(name="ReservationStatus")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class ReservationStatus implements Serializable {
        private static final long serialVersionUID = -5811456962763091947L;
        public boolean Ready;
        public String Address;
        public List<Resource> AvailableResources;

        public ReservationStatus() {
            // no-args constructor
        }
        public ReservationStatus(boolean ready, String address, List<Resource> availableResources) {
            this.Ready = ready;
            this.Address = address;
            this.AvailableResources = availableResources;
        }
        public ReservationStatus(boolean ready, String address, Resource availableResource) {
            this.Ready = ready;
            this.Address = address;
            if (this.AvailableResources == null) {
                this.AvailableResources = new ArrayList<LibJSON.Resource>();
            }
            this.AvailableResources.add(availableResource);
        }
        public boolean isReady() {
            return Ready;
        }
        public void setReady(boolean ready) {
            Ready = ready;
        }
        public String getAddress() {
            return Address;
        }
        public void setAddress(String address) {
            Address = address;
        }
        public List<Resource> getAvailableResources() {
            return AvailableResources;
        }
        public void setAvailableResources(List<Resource> availableResources) {
            AvailableResources = availableResources;
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

    @XmlRootElement(name="Reservations")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class Reservations implements Serializable {
        private static final long serialVersionUID = 5629110247326464140L;
        public List<String> Reservations;
        public Reservations() {
            // no-args constructor
        }
        public Reservations(List<String> reservations) {
            this.Reservations = reservations;
        }
        public Reservations(String reservation) {
            this.Reservations = new ArrayList<String>();
            this.Reservations.add(reservation);
        }
        public List<String> getReservations() {
            return Reservations;
        }
        public void setReservations(List<String> reservations) {
            this.Reservations = reservations;
        }
        public void addAll(Reservations reservations) {
            if (this.Reservations == null) {
                this.Reservations = reservations.getReservations();
            }
            else {
                this.Reservations.addAll(reservations.getReservations());
            }
        }
    }

    @XmlRootElement(name="Types")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class Types implements Serializable {
        private static final long serialVersionUID = 8877186029978897804L;
        public List<Type> Types;
        public Types() {
            // no-args constructor
        }
        public void addType(Type r) {
            if (Types == null) {
                Types = new ArrayList<Type>();
            }
            Types.add(r);
        }
        public List<Type> getTypes() {
            return Types;
        }
        public void setTypes(List<Type> types) {
            Types = types;
        }
    }

    @XmlRootElement(name="Type")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class Type implements Serializable {
        private static final long serialVersionUID = -4680765556555478239L;
        public String Type;
        public AttributesDesc Attributes;
        public Type() {
            // no-args constructor
        }
        public Type(String type, AttributesDesc attributes) {
            this.Type = type;
            this.Attributes = attributes;
        }
    }


    @XmlRootElement(name="AttributesDesc")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class AttributesDesc implements Serializable {
        private static final long serialVersionUID = -1688672880271364789L;
        public CapacityDesc Capacity;
        public ThroughputDesc Throughput;
        public AccessTypeDesc AccessType;

        public AttributesDesc() {
            // no-args constructor
        }

        public AttributesDesc(
                CapacityDesc capacity,
                ThroughputDesc throughput,
                AccessTypeDesc accessType
        ) {
            this.Capacity = capacity;
            this.Throughput = throughput;
            this.AccessType = accessType;
        }

        public CapacityDesc getCapacity() {
            return Capacity;
        }
        public void setCapacity(CapacityDesc capacity) {
            Capacity = capacity;
        }
        public ThroughputDesc getThroughput() {
            return Throughput;
        }
        public void setThroughput(ThroughputDesc throughput) {
            Throughput = throughput;
        }
        public AccessTypeDesc getAccessType() {
            return AccessType;
        }
        public void setAccessType(AccessTypeDesc accessType) {
            AccessType = accessType;
        }
    }

    @XmlRootElement(name="CapacityDesc")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class CapacityDesc implements Serializable {
        private static final long serialVersionUID = 5985090877536139766L;
        public String Description;
        public String DataType;

        public CapacityDesc() {
            // no-args constructor
        }

        public CapacityDesc(
                String description,
                String dataType
        ) {
            this.Description = description;
            this.DataType = dataType;
        }
        public String getDescription() {
            return Description;
        }
        public void setDescription(String description) {
            Description = description;
        }
        public String getDataType() {
            return DataType;
        }
        public void setDataType(String dataType) {
            DataType = dataType;
        }
    }

    @XmlRootElement(name="ThroughputDesc")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class ThroughputDesc implements Serializable {
        private static final long serialVersionUID = 387269859294140728L;
        public String Description;
        public String DataType;

        public ThroughputDesc() {
            // no-args constructor
        }
        public ThroughputDesc(
                String description,
                String dataType
        ) {
            this.Description = description;
            this.DataType = dataType;
        }
        public String getDescription() {
            return Description;
        }
        public void setDescription(String description) {
            Description = description;
        }
        public String getDataType() {
            return DataType;
        }
        public void setDataType(String dataType) {
            DataType = dataType;
        }
    }

    @XmlRootElement(name="AccessTypeDesc")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class AccessTypeDesc implements Serializable {
        private static final long serialVersionUID = 9080753186885532769L;
        public String Description;
        public String DataType;

        public AccessTypeDesc() {
            // no-args constructor
        }
        public AccessTypeDesc(
                String description,
                String dataType
        ) {
            this.Description = description;
            this.DataType = dataType;
        }
        public String getDescription() {
            return Description;
        }
        public void setDescription(String description) {
            Description = description;
        }
        public String getDataType() {
            return DataType;
        }
        public void setDataType(String dataType) {
            DataType = dataType;
        }
    }

    @XmlRootElement(name="ResourceCapacity")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class ResourceCapacity implements Serializable {
        private static final long serialVersionUID = -1408331636553103656L;
        public Resource Resource;
        public ReserveResource Reserve;
        public ReleaseResource Release;

        public ResourceCapacity() {
            // no-args constructor
        }

        public ResourceCapacity(Resource resource, ReserveResource reserve, ReleaseResource release) {
            this.Resource = resource;
            this.Reserve = reserve;
            this.Release = release;
        }

        public Resource getResource() {
            return Resource;
        }
        public void setResource(Resource resource) {
            this.Resource = resource;
        }
        public ReserveResource getReserve() {
            return Reserve;
        }
        public void setReserve(ReserveResource reserve) {
            this.Reserve = reserve;
        }
        public ReleaseResource getRelease() {
            return Release;
        }
        public void setRelease(ReleaseResource release) {
            this.Release = release;
        }
    }

    @XmlRootElement(name="ReserveResource")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class ReserveResource implements Serializable {
        private static final long serialVersionUID = 6310585568556634805L;
        public List<Attributes> Attributes;

        public ReserveResource() {
            // no-args constructor
        }
        public ReserveResource(Attributes attributes) {
            setAttributes(attributes);
        }
        public List<Attributes> getAttributes() {
            return Attributes;
        }
        public void setAttributes(Attributes attributes) {
            if (this.Attributes == null) {
                this.Attributes = new ArrayList<LibJSON.Attributes>();
            }
            this.Attributes.add(attributes);
        }
    }

    @XmlRootElement(name="ReleaseResource")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class ReleaseResource implements Serializable {
        private static final long serialVersionUID = 8910269518815734323L;
        public List<Attributes> Attributes;

        public ReleaseResource() {
            // no-args constructor
        }
        public ReleaseResource(Attributes attributes) {
            setAttributes(attributes);
        }
        public List<Attributes> getAttributes() {
            return Attributes;
        }
        public void setAttributes(Attributes attributes) {
            if (this.Attributes == null) {
                this.Attributes = new ArrayList<LibJSON.Attributes>();
            }
            this.Attributes.add(attributes);    }
    }


    @XmlRootElement(name="Resources")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class Resources implements Serializable {
        private static final long serialVersionUID = 9199843708622395018L;
        public List<Resource> Resources;
        public Resources() {
            // no-args constructor
        }
        public Resources(Resource r) {
            addResource(r);
        }
        public void addResource(Resource r) {
            if (this.Resources == null) {
                this.Resources = new ArrayList<Resource>();
            }
            this.Resources.add(r);
        }
        public List<Resource> getResources() {
            return this.Resources;
        }
        public void setResources(List<Resource> resources) {
            this.Resources = resources;
        }
    }


    @XmlRootElement(name="ResourceMapper")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class ResourceMapper implements Serializable {
        private static final long serialVersionUID = 3787591243319192070L;
        public Resource Resource;
        public ResourceMapper() {
            // no-args constructor
        }
        public ResourceMapper(
                Resource resource) {
            this.Resource = resource;
        }
        public Resource getResource() {
            return Resource;
        }
        public void setResource(Resource resource) {
            Resource = resource;
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
        public Cost Cost;
        public Resource() {
            // no-args constructor
        }
        public Resource(
                String id,
                String ip,
                String type,
                Attributes attributes,
                Cost costs) {
            this.ID = id;
            this.IP = ip;
            this.Type = type;
            this.Attributes = attributes;
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
    }

    @XmlRootElement(name="AccessTypes")
    public static enum AccessTypes implements Serializable {
        SEQUENTIAL, RANDOM
    }

    @XmlRootElement(name="Attributes")
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class Attributes implements Serializable {
        private static final long serialVersionUID = -5867485593384557874L;
        public double Capacity;
        public double Throughput;
        public AccessTypes AccessType;
        public Attributes() {
            // no-args constructor
        }
        public Attributes(
                double capacity,
                double throughput,
                AccessTypes accessType) {
            // no-args constructor
            this.Capacity = capacity;
            this.Throughput = throughput;
            this.AccessType = accessType;
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
        public AccessTypes getAccessType() {
            return AccessType;
        }
        public void setAccessType(AccessTypes accessType) {
            AccessType = accessType;
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

    public static String getMyAddress() {
        try {
            if (!irmConfig.getHostName().equals("")) {
                return irmConfig.getHostName();
            } else {
                return InetAddress.getLocalHost().getHostAddress();
            }
        } catch (Exception e) {
            return "localhost";
        }
    }

    private static double getSequentialCost() {
        if(irmConfig != null) {
            return irmConfig.getSequentialCost();
        } else {
            return 1.0;
        }
    }

    private static double getRandomCost() {
        if(irmConfig != null) {
            return irmConfig.getRandomCost();
        } else {
            return 1.0;
        }
    }

    private static double getCapacityCost() {
        if(irmConfig != null) {
            return irmConfig.getCapacityCost();
        } else {
            return 1.0;
        }
    }
}
