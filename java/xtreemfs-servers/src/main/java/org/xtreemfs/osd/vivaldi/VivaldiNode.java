/*
 * Copyright (c) 2009-2011 by Juan Gonzalez de Benito,
 *                            Barcelona Supercomputing Center
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.vivaldi;

import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

/**
 * Node of a Vivaldi Coordinate System.
 * 
 * @author Juan Gonzalez de Benito (BSC)
 */
public class VivaldiNode {
    
    /**
     * Coordinates that define a position in the coordinate system.
     */
    private VivaldiCoordinates vCoordinates;
    
    /**
     * Constant E used by the Vivaldi algorithm to manage the evolution of
     * the local error.
     */
    private final double CONSTANT_E = 0.10;
    /**
     * Constant C used by the Vivaldi algorithm to adjust the distance the node
     * moves in each recalculation.
     */
    private final double CONSTANT_C = 0.25;
    
    /**
     * Ratio of the current distance between two nodes, used to set the difference
     * between what we consider "right moves" and "wrong moves".
     */
    private final double MAX_MOVEMENT_RATIO = 0.10;
    

    public VivaldiNode(){

        this.vCoordinates = VivaldiCoordinates.newBuilder().setLocalError(0).setXCoordinate(0).setYCoordinate(0).build();
        
    }
    /**
     * 
     * @return the current coordinates of the node.
     */
    public VivaldiCoordinates getCoordinates(){
        return vCoordinates;
    }
    
    /**
     * Multiplies a pair of coordinates by a given real number.
     * 
     * @param coordA the coordinates to be multiplied.
     * @param value the real number to multiply by.
     * @return the result of the multiplication.
     */
    private VivaldiCoordinates multiplyValueCoordinates(VivaldiCoordinates coordA, double value){
        VivaldiCoordinates.Builder ret = VivaldiCoordinates.newBuilder();
        ret.setXCoordinate( coordA.getXCoordinate() * value );
        ret.setYCoordinate( coordA.getYCoordinate() * value );
        ret.setLocalError(0);
        return ret.build();
    }
    
    /**
     * Adds two pairs of coordinates.
     * 
     * @param coordA a pair of coordinates.
     * @param coordB a pair of coordinates.
     * @return a pair of coordinates that represent the result of the addition.
     */
    private VivaldiCoordinates addCoordinates(VivaldiCoordinates coordA,VivaldiCoordinates coordB){
        VivaldiCoordinates.Builder ret = VivaldiCoordinates.newBuilder();
        ret.setXCoordinate( coordA.getXCoordinate() + coordB.getXCoordinate() );
        ret.setYCoordinate( coordA.getYCoordinate() + coordB.getYCoordinate() );
        ret.setLocalError(0);
        return ret.build();
    }
    
    /**
     * Subtracts two pairs of coordinates.
     * 
     * @param coordA a pair of coordinates.
     * @param coordB a pair of coordinates.
     * @return a pair of coordinates that represent the result of the addition.
     */
    private VivaldiCoordinates subtractCoordinates(VivaldiCoordinates coordA,VivaldiCoordinates coordB){
        return addCoordinates( coordA, multiplyValueCoordinates(coordB,-1.0) );
    }
    
    /**
     * Multiplies two pairs of coordinates using the scalar product.
     *      A · B = Ax*Bx + Ay*By
     * 
     * @param coordA a pair of coordinates.
     * @param coordB a pair of coordinates.
     * @return the result of the scalar product.
     */
    private double scalarProductCoordinates(VivaldiCoordinates coordA, VivaldiCoordinates coordB){
        
        double ret = 0.0;
        
        // A · B = Ax*Bx + Ay*By
        ret += coordA.getXCoordinate() * coordB.getXCoordinate();
        ret += coordA.getYCoordinate() * coordB.getYCoordinate();
        
        return ret;

    }
    
    /**
     * Calculates the magnitude of a given vector.
     * 
     * @param coordA the coordinates whose magnitude must be calculated.
     * @return the distance from the position defined by the coordinates to the
     * origin of the system.
     */
    private double magnitudeCoordinates(VivaldiCoordinates coordA){
        // ||A|| = sqrt( Ax² + Ay² )
        return Math.sqrt( scalarProductCoordinates(coordA,coordA) );
    }
    
    /**
     * Calculates the unitary vector of a given vector.
     * 
     * @param coordA Coordinates to calculate the unitary vector from.
     * @return a vector with the same direction but with magnitude == 1.
     */
    private VivaldiCoordinates getUnitaryCoordinates(VivaldiCoordinates coordA){
        // unit(A) = A * ( 1 / ||A|| )
        // ||unit(A)|| = 1
        double magn = magnitudeCoordinates(coordA);
        return multiplyValueCoordinates( coordA, 1.0/magn );
    }
    
    /**
     * Modifies a pair of coordinates with a couple of random values, so they are
     * included in the interval (-1,1) and have also a random direction.
     *
     * @param coordA coordinates that must be modified.
     */
    private VivaldiCoordinates getRandomCoordinates(){
        VivaldiCoordinates.Builder coordA = VivaldiCoordinates.newBuilder();
        coordA.setXCoordinate( (Math.random()*2) - 1 );
        coordA.setYCoordinate( (Math.random()*2) - 1 );
        coordA.setLocalError(0.0); // this has to be set to avoid com.google.protobuf.UninitializedMessageException
        return coordA.build();
    }
    
    public static double calculateDistance(VivaldiCoordinates pointA,VivaldiCoordinates pointB){
        VivaldiNode auxNode = new VivaldiNode();
        return auxNode.magnitudeCoordinates( auxNode.subtractCoordinates(pointA, pointB) );
        
    }
    public static String coordinatesToString(VivaldiCoordinates vc) {

        StringBuffer sb = new StringBuffer(3*Long.SIZE/8);
        OutputUtils.writeHexLong(sb, Double.doubleToRawLongBits(vc.getXCoordinate()));
        OutputUtils.writeHexLong(sb, Double.doubleToRawLongBits(vc.getYCoordinate()));
        OutputUtils.writeHexLong(sb, Double.doubleToRawLongBits(vc.getLocalError()));

        return sb.toString();
    }

    public static VivaldiCoordinates stringToCoordinates(String coordinates) {
        VivaldiCoordinates.Builder vc = VivaldiCoordinates.newBuilder();
        vc.setXCoordinate(Double.longBitsToDouble(OutputUtils.readHexLong(coordinates, 0)));
        vc.setYCoordinate(Double.longBitsToDouble(OutputUtils.readHexLong(coordinates, 16)));
        vc.setLocalError(Double.longBitsToDouble(OutputUtils.readHexLong(coordinates, 32)));
        return vc.build();
    }
    
    /**
     * Modifies the position of the node according to the current distance to a
     * given point in the coordinate space and the real RTT measured against it.
     * 
     * @param coordinatesJ coordinates of a different node.
     * @param measuredRTT RTT measured with the other node.
     */
    public boolean recalculatePosition(VivaldiCoordinates coordinatesJ, long measuredRTT,boolean forceRecalculation){

        assert( measuredRTT>=0 ) : "Wrong RTT";
        
        boolean ret = true;
        
        double localError = vCoordinates.getLocalError();
        
        //SUBTRACTION = Xi - Xj
        VivaldiCoordinates subtractionVector = subtractCoordinates(vCoordinates, coordinatesJ );
        // ||SUBTRACTION|| should be ~= RTT
        double subtractionMagnitude = magnitudeCoordinates(subtractionVector);
        
        //Sample weight balances local and remote error
        //If it's close to 1, J knows more than me: localError > errorJ
        //If it's close to 0.5, we both know the same: A/2A = 1/2
        //If it's close to 0, I know more than it: localError < errorJ
        double weight = 0.0;

        //Two nodes shouldn't be in the same position
        if( measuredRTT == 0 ){
            measuredRTT = 1;
        }
        
        //Compute relative error of this sample
        double relativeError = ((double)Math.abs( subtractionMagnitude - measuredRTT)) / (double)measuredRTT;

        //Calculate weight
        if( localError <= 0.0 ){
            weight = 1;
        }else{
            if( coordinatesJ.getLocalError() > 0.0 ){
                    weight = localError/ (localError + Math.abs(coordinatesJ.getLocalError()));
            } //else weight is 0.0
        }
        
        //Calculate proposed movement
        double delta;
        delta = CONSTANT_C * weight;
        double estimatedMovement = ((double)measuredRTT-subtractionMagnitude) * delta;        
        
        if( forceRecalculation ||                       //The recalculation must be done anyhow
            (subtractionMagnitude<=0.0) ||              //The nodes are in the same position
            (estimatedMovement<0) ||                    //The nodes must get closer
            (Math.abs(estimatedMovement) < subtractionMagnitude * MAX_MOVEMENT_RATIO) ){ //The movement is not too big

            //Update local error
            if( localError <= 0 ){
                //We initialize the local error with the first measured absolute error 
                localError = (double)Math.abs( subtractionMagnitude - (double)measuredRTT);
            }else{
                //Compute relative weight moving average of local error
                localError = (relativeError * CONSTANT_E * weight) + localError* (1-(CONSTANT_E*weight));
            }

            VivaldiCoordinates additionVector = null;

            if( subtractionMagnitude > 0.0 ){ 
                //Xi = Xi + delta * (rtt - || Xi - Xj ||) * u(Xi - Xj)
                additionVector = multiplyValueCoordinates( getUnitaryCoordinates(subtractionVector), estimatedMovement);
            }else{ 
                //Both points have the same Coordinates, so we just pull them apart in a random direction
                VivaldiCoordinates randomCoords = getRandomCoordinates();

                //Xi = Xi + delta * (rtt - || Xi - Xj ||) * u(Xi - Xj)
                additionVector = multiplyValueCoordinates( getUnitaryCoordinates(randomCoords), estimatedMovement);
            }

            //Move the node according to the calculated addition vector
            vCoordinates = addCoordinates( vCoordinates, additionVector);
            vCoordinates = vCoordinates.toBuilder().setLocalError(localError).build();

        } else {
            //The proposed movement is too big according to the current distance btw nodes
            ret = false;
        }
        
        return ret;
    }
}
