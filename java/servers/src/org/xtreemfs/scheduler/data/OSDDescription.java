package org.xtreemfs.scheduler.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.buffer.ReusableBuffer;


public class OSDDescription {

	private String 							identifier;
	
	private OSDPerformanceDescription		capabilities;
	
	private List<Reservation>				reservations;
	
	private final byte						VERSION = 1;

	public enum OSDType {
		DISK,
		SSD,
		UNKNOWN
	}
	
	public enum OSDUsage {
		RANDOM_IO,
		STREAMING,
		ALL,
		UNUSED
	}
	
	private OSDType type;
	
	private OSDUsage usage;
	
	public OSDDescription(String identifier, OSDPerformanceDescription capabilities, OSDType type) {
		this.identifier = identifier;
		this.capabilities = capabilities;
		this.type = type;
		this.reservations = new ArrayList<Reservation>();
		this.usage = OSDUsage.UNUSED;
	}
	
	public OSDDescription(byte[] bytes) throws IOException {
		ReusableBuffer rb = ReusableBuffer.wrap(bytes);
		byte version = rb.get();
		if(version == VERSION) {
			this.identifier = rb.getString();
			getTypeFromInt(rb.getInt());
			getUsageFromInt(rb.getInt());
			double capacity = rb.getDouble();
			double randomThroughput = rb.getDouble();
			int capLength = rb.getInt();
			Map<Integer, Double> streamingThroughput = new HashMap<Integer, Double>();
			for(int i = 1; i <= capLength; i++) {
				streamingThroughput.put(i, rb.getDouble());
			}
			this.capabilities = new OSDPerformanceDescription(randomThroughput, streamingThroughput, capacity);
			
			this.reservations = new ArrayList<Reservation>();
		} else {
			throw new IOException("Cannot handle version " + version);
		}
	}
	
	public byte[] getBytes() {
		byte[] bytes = new byte[getSize()];
		ReusableBuffer rb = ReusableBuffer.wrap(bytes);
		
		rb.put(VERSION);
		rb.putString(this.identifier);
		rb.putInt(typeToInt());
		rb.putInt(usageToInt());
		
		rb.putDouble(capabilities.getCapacity());
		rb.putDouble(capabilities.getIops());
		int capLength = capabilities.getStreamingPerformance().size();
		rb.putInt(capLength);
		for(int i = 1; i <= capLength; i++) {
			rb.putDouble(capabilities.getStreamingPerformance().get(i));
		}
		
		return bytes;
	}
	
	public void reset() {
		this.reservations = new ArrayList<Reservation>();
		this.usage = OSDUsage.UNUSED;
	}
	
	public OSDPerformanceDescription getCapabilities() {
		return capabilities;
	}

	public void setCapabilities(OSDPerformanceDescription capabilities) {
		this.capabilities = capabilities;
	}

	public List<Reservation> getReservations() {
		return reservations;
	}

	public void setReservations(List<Reservation> reservations) {
		this.reservations = reservations;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public OSDUsage getUsage() {
		return usage;
	}

	public void setUsage(OSDUsage usage) {
		this.usage = usage;
	}

	public OSDType getType() {
		return type;
	}
	
	public void allocateReservation(Reservation reservation) {
		this.reservations.add(reservation);
	}
	
	public void removeReservation(Reservation reservation) {
		this.reservations.remove(reservation);
	}
	
	public boolean hasFreeCapacity(Reservation r){
		double usedCapacity = 0;
		double usedRandomPerformance = 0;
		double usedStreamingPerformance = 0;
		double streamingPerformanceAtLoadLevel = 
				this.getCapabilities().getStreamingPerformance().get(this.getReservations().size() + 1) != null?
				this.getCapabilities().getStreamingPerformance().get(this.getReservations().size() + 1):0;
		
		for(Reservation reservation: reservations){
			usedCapacity += reservation.getCapacity();
			usedRandomPerformance += reservation.getRamdomThroughput();
			usedStreamingPerformance += reservation.getStreamingThroughput();
		}
		
		if((usedCapacity + r.getCapacity() > this.getCapabilities().getCapacity()) ||
				(usedRandomPerformance + r.getRamdomThroughput() > this.getCapabilities().getIops()) ||
				(usedStreamingPerformance + r.getStreamingThroughput() > streamingPerformanceAtLoadLevel)){
			return false;
		}
		else {
			return true;
		}
	}
	
	private void getTypeFromInt(int x) {
		switch(x) {
		case 0:
			this.type = OSDType.DISK;
			break;
		case 1:
			this.type = OSDType.SSD;
			break;
		case 2:
			this.type = OSDType.UNKNOWN;
			break;
		default:
			this.type = OSDType.UNKNOWN;
		}
	}
	
	private int typeToInt() {
		switch(this.type) {
		case DISK:
			return 0;
		case SSD:
			return 1;
		case UNKNOWN:
			return 2;
		default:
			return 2;
		}
	}
	
	private void getUsageFromInt(int x) {
		switch(x) {
		case 0:
			this.usage = OSDUsage.ALL;
			break;
		case 1:
			this.usage = OSDUsage.RANDOM_IO;
			break;
		case 2:
			this.usage = OSDUsage.STREAMING;
			break;
		case 3:
			this.usage = OSDUsage.UNUSED;
			break;
		default:
			this.usage = OSDUsage.UNUSED;
		}
	}
	
	private int usageToInt() {
		switch(this.usage) {
			case ALL:
				return 0;
			case RANDOM_IO:
				return 1;
			case STREAMING:
				return 2;
			case UNUSED:
				return 3;
			default: 
				return 3;
		}
	}
	
	private int getSize() {
		int result = 1 + 3 * Integer.SIZE + 
				(2+capabilities.getStreamingPerformance().size()) * 
				Double.SIZE + this.identifier.getBytes().length;
		return result;
	}
}
