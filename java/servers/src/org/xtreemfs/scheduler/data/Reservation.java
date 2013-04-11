package org.xtreemfs.scheduler.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.foundation.buffer.ReusableBuffer;

public class Reservation {

	private String volumeIdentifier;
	private double ramdomThroughput;
	private double streamingThroughput;
	private double capacity;
	private ReservationType type;
	private List<String> schedule;
	private static final byte CURRENT_VERSION = 1;

	public Reservation(String volumeIdentifier, ReservationType type,
			double randomThroughput, double streamingThroughput, double capacity) {
		this.schedule = new ArrayList<String>();
		this.volumeIdentifier = volumeIdentifier;
		this.type = type;
		this.ramdomThroughput = randomThroughput;
		this.streamingThroughput = streamingThroughput;
		this.capacity = capacity;
	}

	public Reservation(byte[] bytes) throws IOException {
		this.schedule = new ArrayList<String>();
		ReusableBuffer rb = ReusableBuffer.wrap(bytes);
		byte version = rb.get();
		
		if(version == CURRENT_VERSION) {
			this.volumeIdentifier = rb.getString();
			this.setType(rb.getInt());
			this.ramdomThroughput = rb.getDouble();
			this.streamingThroughput = rb.getDouble();
			this.capacity = rb.getDouble();
			
			int scheduleLength = rb.getInt();
			
			for(int i = 0; i < scheduleLength; i++) {
				this.schedule.add(rb.getString());
			}
		} else {
			throw new IOException("don't know how to handle version "+version);
		}
	}

	public byte[] getBytes() {
		int size = getSize();
		byte[] result = new byte[size];
		ReusableBuffer rb = ReusableBuffer.wrap(result);
		
		rb.put(CURRENT_VERSION);
		rb.putString(volumeIdentifier);
		rb.putInt(typeToInt());
		rb.putDouble(ramdomThroughput);
		rb.putDouble(streamingThroughput);
		rb.putDouble(capacity);
		rb.putInt(schedule.size());
		
		for(String s: schedule) {
			rb.putString(s);
		}
		
		return result;
	}

	public double getRamdomThroughput() {
		return ramdomThroughput;
	}

	public double getStreamingThroughput() {
		return streamingThroughput;
	}

	public double getCapacity() {
		return capacity;
	}

	public String getVolumeIdentifier() {
		return volumeIdentifier;
	}

	public ReservationType getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof Reservation) {
			Reservation r = (Reservation) o;
			return (r.getVolumeIdentifier().equals(this.volumeIdentifier) &&
					r.getCapacity() == this.getCapacity() &&
					r.getRamdomThroughput() == this.getRamdomThroughput() &&
					r.getStreamingThroughput() == this.getStreamingThroughput());
		} else {
			return false;
		}
	}

	public enum ReservationType {
		STREAMING_RESERVATION, RANDOM_IO_RESERVATION, BEST_EFFORT_RESERVATION, COLD_STORAGE_RESERVATION
	}

	public List<String> getSchedule() {
		return schedule;
	}

	public void setSchedule(List<String> schedule) {
		this.schedule = schedule;
	}
	
	private int typeToInt() {
		switch(type) {
		case STREAMING_RESERVATION:
			return 0;
		case RANDOM_IO_RESERVATION:
			return 1;
		case BEST_EFFORT_RESERVATION:
			return 2;
		case COLD_STORAGE_RESERVATION:
			return 3;
		default:
			return 2;
		}
	}
	
	private void setType(int x) {
		switch(x) {
		case 0:
			type = ReservationType.STREAMING_RESERVATION;
		case 1:
			type = ReservationType.RANDOM_IO_RESERVATION;
		case 2:
			type = ReservationType.BEST_EFFORT_RESERVATION;
		case 3:
			type = ReservationType.COLD_STORAGE_RESERVATION;
		default:
			type = ReservationType.BEST_EFFORT_RESERVATION;
		}
	}
	
	private int getSize() {
		int result = 1 + volumeIdentifier.getBytes().length + 3 * Double.SIZE + 2 * Integer.SIZE;
		
		for(String s: schedule) {
			result += s.getBytes().length;
		}
		
		return result;
	}
}
