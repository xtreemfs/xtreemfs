package org.xtreemfs.scheduler.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.xtreemfs.scheduler.data.OSDDescription;
import org.xtreemfs.scheduler.data.Reservation;
import org.xtreemfs.scheduler.data.ResourceSet;
import org.xtreemfs.scheduler.exceptions.SchedulerException;

public class ReservationSchedulerImplementation implements ReservationScheduler {

	private List<OSDDescription> osds;
	private double minAngle;
	private double capacityGain;
	private double randomIOGain;
	private double streamingGain;
	private boolean preferUsedOSDs;

	public ReservationSchedulerImplementation(List<OSDDescription> osds,
			double minAngle, double capacityGain, double randomIOGain,
			double streamingGain, boolean preferUsedOSDs) {
		this.osds = osds;
		this.minAngle = minAngle;
		this.capacityGain = capacityGain;
		this.randomIOGain = randomIOGain;
		this.streamingGain = streamingGain;
		this.preferUsedOSDs = preferUsedOSDs;
	}

	private static class OSDComparator implements Comparator<OSDDescription> {
		private Reservation reservation;
		private ReservationSchedulerImplementation scheduler;

		public OSDComparator(ReservationSchedulerImplementation s, Reservation r) {
			this.reservation = r;
			this.scheduler = s;
		}

		@Override
		public int compare(OSDDescription osd1, OSDDescription osd2) {
			double a1 = this.scheduler.getAngle(reservation, osd1);
			double a2 = this.scheduler.getAngle(reservation, osd2);

			if (this.scheduler.preferUsedOSDs) {
				if (osd1.getUsage() == OSDDescription.OSDUsage.UNUSED
						&& osd2.getUsage() != OSDDescription.OSDUsage.UNUSED) {
					return 1;
				}
				if (osd2.getUsage() == OSDDescription.OSDUsage.UNUSED
						&& osd1.getUsage() != OSDDescription.OSDUsage.UNUSED) {
					return -1;
				}
			}

			if (a1 > a2) {
				return 1;
			} else if (a1 < a2) {
				return -1;
			} else {
				return 0;
			}
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof OSDComparator;
		}
	}

	private boolean fittingUsageType(Reservation r, OSDDescription o) {
		if ((r.getType() == Reservation.ReservationType.STREAMING_RESERVATION &&
                o.getUsage() == OSDDescription.OSDUsage.RANDOM_IO)
				|| (r.getType() == Reservation.ReservationType.RANDOM_IO_RESERVATION && o
						.getUsage() == OSDDescription.OSDUsage.STREAMING)
				|| getAngle(r, o) < this.minAngle)
			return false;
		else
			return true;
	}

	@Override
	public List<OSDDescription> scheduleReservation(Reservation r)
			throws SchedulerException {
		List<OSDDescription> result = new ArrayList<OSDDescription>();
		Reservation stripedReservation = r;
		int stripeLength;

		for (stripeLength = 1; stripeLength <= this.osds.size(); stripeLength++) {
			// Decompose original reservation to #stripeLengh reservations
			stripedReservation = new Reservation(r.getVolumeIdentifier(),
					r.getType(), r.getRamdomThroughput() / stripeLength,
					r.getStreamingThroughput() / stripeLength, r.getCapacity()
							/ stripeLength);
			OSDComparator c = new OSDComparator(this, stripedReservation);
			Collections.sort(osds, c);

			for (OSDDescription osd : osds) {
				if (osd.hasFreeCapacity(stripedReservation) && fittingUsageType(stripedReservation, osd)) {
					result.add(osd);

                    if (result.size() == stripeLength) {
                        break;
                    }
                }
			}
			if (result.size() == stripeLength) {
				break;
			}
		}

		if (result.size() < stripeLength) {
			throw new SchedulerException("Cannot schedule reservation");
		}

		for (OSDDescription osd : result) {
			// Set usage type
			if (osd.getUsage() == OSDDescription.OSDUsage.UNUSED) {
				if (osd.getType() == OSDDescription.OSDType.DISK
						|| osd.getType() == OSDDescription.OSDType.UNKNOWN) {
                    if(r.getType() == Reservation.ReservationType.STREAMING_RESERVATION) {
					    osd.setUsage(OSDDescription.OSDUsage.STREAMING);
                    } else if(r.getType() == Reservation.ReservationType.RANDOM_IO_RESERVATION) {
                        osd.setUsage((OSDDescription.OSDUsage.RANDOM_IO));
                    }
				}
			}

			// Allocate reservations
			osd.allocateReservation(stripedReservation);
		}

		return result;
	}

	@Override
	public void removeReservation(String volumeIdentifier) {
	}

	@Override
	public void reset() {
		for (OSDDescription osd : this.osds) {
			osd.reset();
		}
	}
	
	@Override
	public void addReservations(Map<String, Reservation> reservations) {
		for(String osdIdentifier: reservations.keySet()) {
			OSDDescription osd = this.getOSDByIdentifier(osdIdentifier);
			if(osd != null) {
				osd.allocateReservation(reservations.get(osdIdentifier));
			}
		}
	}

    @Override
    public ResourceSet getFreeResources() {
        ResourceSet result = new ResourceSet();
        double freeCapacity = 0.0;
        double freeIOPS = 0.0;
        double freeSeqTP = 0.0;

        for(OSDDescription osd: osds) {
            freeCapacity += osd.getFreeResources().getCapacity();
            freeIOPS += osd.getFreeResources().getIops();
            freeSeqTP += osd.getFreeResources().getSeqTP();
        }

        result.setCapacity(freeCapacity);
        result.setIops(freeIOPS);
        result.setSeqTP(freeSeqTP);
        return result;
    }
	
	private OSDDescription getOSDByIdentifier(String identifier) {
		for(OSDDescription osd: this.osds) {
			if(osd.getIdentifier().equals(identifier)) {
				return osd;
			}
		}
		
		return null;
	}

	double getAngle(Reservation r, OSDDescription o) {
		double reservedCapacity = r.getCapacity();
		double reservedRandomThroughput = r.getRamdomThroughput();
		double reservedStreamingThroughput = r.getStreamingThroughput();

		for (Reservation reservation : o.getReservations()) {
			reservedCapacity += reservation.getCapacity();
			reservedRandomThroughput += reservation.getRamdomThroughput();
			reservedStreamingThroughput += reservation.getStreamingThroughput();
		}

		return Math
				.acos((reservedCapacity
						* (o.getCapabilities().getCapacity() * this.capacityGain)
						+ reservedRandomThroughput
						* (o.getCapabilities().getIops() * this.randomIOGain) + reservedStreamingThroughput
						* (o.getCapabilities().getStreamingPerformance()
								.get(o.getReservations().size() + 1) * this.streamingGain))
						/ getUsageVectorLength(o, r)
						* getCapabilityVectorLength(o));
	}

	static double getUsageVectorLength(OSDDescription o, Reservation r) {
		double reservedCapacity = r.getCapacity();
		double reservedRandomThroughput = r.getRamdomThroughput();
		double reservedStreamingThroughput = r.getStreamingThroughput();

		for (Reservation reservation : o.getReservations()) {
			reservedCapacity += reservation.getCapacity();
			reservedRandomThroughput += reservation.getRamdomThroughput();
			reservedStreamingThroughput += reservation.getStreamingThroughput();
		}

		return Math.sqrt(Math.pow(reservedCapacity, 2.0)
				+ Math.pow(reservedRandomThroughput, 2.0)
				+ Math.pow(reservedStreamingThroughput, 2.0));
	}

	static double getCapabilityVectorLength(OSDDescription o) {
		return Math.sqrt(Math.pow(o.getCapabilities().getStreamingPerformance()
				.get(o.getReservations().size() + 1), 2.0)
				+ Math.pow(o.getCapabilities().getIops(), 2.0)
				+ Math.pow(o.getCapabilities().getCapacity(), 2.0));
	}
}
