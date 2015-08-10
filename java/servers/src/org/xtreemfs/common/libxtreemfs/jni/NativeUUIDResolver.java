package org.xtreemfs.common.libxtreemfs.jni;

import java.util.Arrays;
import java.util.List;

import org.xtreemfs.common.libxtreemfs.UUIDIterator;
import org.xtreemfs.common.libxtreemfs.UUIDResolver;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.XtreemFSException;
import org.xtreemfs.common.libxtreemfs.jni.generated.StringVector;
import org.xtreemfs.common.libxtreemfs.jni.generated.UUIDResolverProxy;

public class NativeUUIDResolver implements UUIDResolver {
    protected final UUIDResolverProxy proxy;

    public NativeUUIDResolver(UUIDResolverProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String uuidToAddress(String uuid) throws AddressToUUIDNotFoundException {
        String[] address = { null };

        try {
            proxy.uUIDToAddress(uuid, address);
        } catch (XtreemFSException e) {
            throw new AddressToUUIDNotFoundException(uuid);
        }

        return address[0];
    }

    @Override
    public String volumeNameToMRCUUID(String volumeName) throws VolumeNotFoundException, AddressToUUIDNotFoundException {
        String[] mrc_uuid = { null };

        try {
            proxy.volumeNameToMRCUUID(volumeName, mrc_uuid);
        } catch (XtreemFSException e) {
            throw new VolumeNotFoundException(volumeName);
        }

        return mrc_uuid[0];
    }

    @Override
    public void volumeNameToMRCUUID(String volumeName, UUIDIterator uuidIterator) throws VolumeNotFoundException,
            AddressToUUIDNotFoundException {
        assert (uuidIterator != null);

        StringVector result;
        try {
            result = proxy.volumeNameToMRCUUIDs(volumeName);
        } catch (XtreemFSException e) {
            throw new VolumeNotFoundException(volumeName);
        }

        for (int i = 0, c = (int) result.size(); i < c; ++i) {
            uuidIterator.addUUID(result.get(i));
        }

        result.delete();
    }

    @Override
    public List<String> volumeNameToMRCUUIDs(String volumeName) throws VolumeNotFoundException,
            AddressToUUIDNotFoundException {
        StringVector result;
        try {
            result = proxy.volumeNameToMRCUUIDs(volumeName);
        } catch (XtreemFSException e) {
            throw new VolumeNotFoundException(volumeName);
        }
        List<String> ret = Arrays.asList(result.toArray());

        result.delete();
        return ret;
    }

}
