package org.xtreemfs.scheduler.algorithm;

import java.util.List;

import org.xtreemfs.scheduler.data.OSDDescription;

public class ReservationSchedulerFactory {
	public static ReservationScheduler getScheduler(List<OSDDescription> osds,
			double minAngle, double capacityGain, double randomIOGain,
			double streamingGain, boolean preferUsedOSDs) {
		return new ReservationSchedulerImplementation(osds, minAngle, capacityGain, randomIOGain, streamingGain, preferUsedOSDs);
	}
}
