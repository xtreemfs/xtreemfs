/*  Copyright (c) 2009 Barcelona Supercomputing Center - Centro Nacional
  de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

  This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
  Grid Operating System, see <http://www.xtreemos.eu> for more details.
  The XtreemOS project has been developed with the financial support of the
  European Commission's IST program under contract #FP6-033576.

  XtreemFS is free software: you can redistribute it and/or modify it under
  the terms of the GNU General Public License as published by the Free
  Software Foundation, either version 2 of the License, or (at your option)
  any later version.

  XtreemFS is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Juan Gonzalez de Benito (BSC) & Jonathan Marti (BSC)
 */
package org.xtreemfs.osd.vivaldi;

import java.util.Random;

public class ZipfGenerator {

    private Random rnd;
    //private Random rnd = new Random(0);
    private int size;
    private double skew;
    private double bottom;

    public ZipfGenerator(int size, double skew) {
        this.rnd = new Random(System.currentTimeMillis());
        this.skew = skew;
        this.setSize(size);
        

    }

    /**
     * Method that returns a rank id between 0 and this.size (exclusive).
     * The frequency of returned rank id follows the Zipf distribution represented by this class.
     * @return a rank id between 0 and this.size.
     * @throws lptracegen.DistributionGenerator.DistributionException
     */

    public int next(){
        int rank = -1;
        double frequency = 0.0d;
        double dice = 0.0d;
        while (dice >= frequency) {
            rank = this.rnd.nextInt(this.size);
            frequency = getProbability(rank + 1);//(0 is not allowed for probability computation)
            dice = this.rnd.nextDouble();
        }
        return rank;
    }

    /**
     * Method that computes the probability (0.0 .. 1.0) that a given rank occurs.
     * The rank cannot be zero.
     * @param rank
     * @return probability that the given rank occurs (over 1)
     * @throws lptracegen.DistributionGenerator.DistributionException
     */
    public double getProbability(int rank) {
        if (rank == 0) {
            throw new RuntimeException("getProbability - rank must be > 0");
        }
        return (1.0d / Math.pow(rank, this.skew)) / this.bottom;
    }
    
    public void setSize(int newSize){
      this.size = newSize;
      //calculate the generalized harmonic number of order 'size' of 'skew' 
      //http://en.wikipedia.org/wiki/Harmonic_number
      this.bottom = 0;
      for (int i = 1; i <= size; i++) {
          this.bottom += (1.0d / Math.pow(i, this.skew));
      }
    }

    /**
     * Method that returns a Zipf distribution
     * result[i] = probability that rank i occurs
     * @return the zipf distribution
     * @throws lptracegen.DistributionGenerator.DistributionException
     */
    public double[] getDistribution() {
        double[] result = new double[this.size];
        for ( int i = 1; i <= this.size; i++) { //i==0 is  not allowed to compute probability
            result[i - 1] = getProbability(i);
        }
        return result;
    }

    /**
     * Method that computes an array of length == this.size
     * with the occurrences for every rank i (following the Zipf)
     * result[i] = #occurrences of rank i
     * @param size
     * @return result[i] = #occurrences of rank i
     * @throws lptracegen.DistributionGenerator.DistributionException
     */
    public int[] getRankArray(int totalEvents) {
        int[] result = new int[this.size];
        for (int i = 0; i < totalEvents; i++) {
            int rank = next();
            result[rank] += 1;
        }
        return result;
    }
}
