package org.xtreemfs.scheduler.data;

import java.util.HashMap;
import java.util.Map;

public class OSDPerformanceDescription {
	
	public OSDPerformanceDescription(double randomThroughput, Map<Integer, Double> streamingThroughput, double capacity) {
		this.ramdomThroughput = randomThroughput;
		this.streamingThroughput = streamingThroughput;
		this.capacity = capacity;
	}
	
	public OSDPerformanceDescription() {
		// default configuration of OSDs without benchmark results
		this.ramdomThroughput = 100.0;
		this.capacity = 100.0;
		Map<Integer, Double> streamingThroughput = new HashMap<Integer, Double>();
		for(int i = 1; i <= 100; i++)
			streamingThroughput.put(i, 100.0);
		this.streamingThroughput = streamingThroughput;
	}
	
	private double 					ramdomThroughput;
	
	private Map<Integer, Double> 	streamingThroughput;
	
	private double 					capacity;

	public double getIops() {
		return ramdomThroughput;
	}

	public void setIops(double iops) {
		this.ramdomThroughput = iops;
	}

	public Map<Integer, Double> getStreamingPerformance() {
		return streamingThroughput;
	}

	public void setStreamingPerformance(Map<Integer, Double> streamingPerformance) {
		this.streamingThroughput = streamingPerformance;
	}

	public double getCapacity() {
		return capacity;
	}

	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}
	
}
