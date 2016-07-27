package org.xtreemfs.common.libxtreemfs.exceptions;

public class AddressToUUIDNotFoundException extends XtreemFSException {

	public AddressToUUIDNotFoundException(String uuid) {
		super("UUID: service not found for uuid " + uuid);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
}
