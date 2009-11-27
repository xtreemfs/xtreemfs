// Copyright 2009 Juan González de Benito.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "vivaldi_node.h"
using namespace xtfs_vivaldi;

#include <cmath>
#include <cstdlib>

/**
 * 
 * @return the current coordinates of the node.
 */
org::xtreemfs::interfaces::VivaldiCoordinates *VivaldiNode::getCoordinates(){
  return &this->ownCoordinates;	
}

/**
 * Multiplies a pair of coordinates by a given real number and stores the result in coord
 * 
 * @param coordA the coordinates to be multiplied.
 * @param value the real number to multiply by.
 */
void VivaldiNode::multiplyValueCoordinates(org::xtreemfs::interfaces::VivaldiCoordinates& coord,double value){
    
  coord.set_x_coordinate( coord.get_x_coordinate() * value );
  coord.set_y_coordinate( coord.get_y_coordinate() * value );
}

/**
 * Adds two pairs of coordinates and stores the result in coordA
 */
void VivaldiNode::addCoordinates(org::xtreemfs::interfaces::VivaldiCoordinates &coordA,
								                 org::xtreemfs::interfaces::VivaldiCoordinates coordB){

	coordA.set_x_coordinate( coordA.get_x_coordinate() + coordB.get_x_coordinate() );
	coordA.set_y_coordinate( coordA.get_y_coordinate() + coordB.get_y_coordinate() );
}

/**
 * Subtracts two pairs of coordinates and stores the result in coordA
 */
void VivaldiNode::subtractCoordinates(	org::xtreemfs::interfaces::VivaldiCoordinates &coordA,
										                    org::xtreemfs::interfaces::VivaldiCoordinates coordB){
	
	multiplyValueCoordinates(coordB,-1.0);
  addCoordinates( coordA, coordB );
  
}

/**
 * Multiplies two pairs of coordinates using the scalar product.
 *      A · B = Ax*Bx + Ay*By
 * 
 * @param coordA a pair of coordinates.
 * @param coordB a pair of coordinates.
 * @return the result of the scalar product.
 */
double VivaldiNode::scalarProductCoordinates(	org::xtreemfs::interfaces::VivaldiCoordinates coordA,
                                              org::xtreemfs::interfaces::VivaldiCoordinates coordB){
    
  double retval = 0.0;
    
  // A · B = Ax*Bx + Ay*By
  retval += coordA.get_x_coordinate() * coordB.get_x_coordinate();
  retval += coordA.get_y_coordinate() * coordB.get_y_coordinate();
    
  return retval;

}

/**
 * Calculates the magnitude of a given vector.
 * 
 * @param coordA the coordinates whose magnitude must be calculated.
 * @return the distance from the position defined by the coordinates to the
 * origin of the system.
 */
double VivaldiNode::magnitudeCoordinates(org::xtreemfs::interfaces::VivaldiCoordinates coordA){
  // ||A|| = sqrt( Ax² + Ay² )
  double sProd = scalarProductCoordinates(coordA,coordA);
  return sqrt( sProd );
}

/**
 * Calculates the unitary vector of a given vector and stores the result in coord
 *
 * @return true if it's been possible to calculate the vector or false otherwise 
 */
bool VivaldiNode::getUnitaryCoordinates(org::xtreemfs::interfaces::VivaldiCoordinates &coord){
  // unit(A) = A * ( 1 / ||A|| )
  // ||unit(A)|| = 1
  
  bool retval = false;
    
  double magn = magnitudeCoordinates(coord);
    
  if( magn > 0){ //cannot be == 0
    	
    multiplyValueCoordinates( coord, 1.0/magn );
  	
  	retval = true;    	
  }
  
  return retval;
}

/**
 * Modifies a pair of coordinates with a couple of random values, so they are
 * included in the interval (-1,1) and have also a random direction.
 *
 * @param coord coordinates that must be modified.
 */
void VivaldiNode::modifyCoordinatesRandomly(org::xtreemfs::interfaces::VivaldiCoordinates &coord){
	
  //static_cast<double>(rand()))/RAND_MAX) generates real values btw 0 and 1 
  coord.set_x_coordinate( ( (static_cast<double>(rand()) /RAND_MAX ) *2)- 1 );
  coord.set_y_coordinate( ( (static_cast<double>(rand()) /RAND_MAX ) *2)- 1 );
}


/**
 * Modifies the position of the node according to the current distance to a
 * given point in the coordinate space and the real RTT measured against it.
 */
bool VivaldiNode::recalculatePosition(org::xtreemfs::interfaces::VivaldiCoordinates& coordinatesJ,
                  										long measuredRTT,
                  										bool forceRecalculation){
  bool retval = true;
  
  double localError = ownCoordinates.get_local_error();
  
  //SUBTRACTION = Xi - Xj
  org::xtreemfs::interfaces::VivaldiCoordinates subtractionVector(ownCoordinates);
  subtractCoordinates(subtractionVector, coordinatesJ );
  
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
  double relativeError = static_cast<double>(abs(subtractionMagnitude - measuredRTT)) / static_cast<double>(measuredRTT);
  
  //Calculate weight
  if( localError <= 0.0 ){
    weight = 1;
  }else{
    if( coordinatesJ.get_local_error() > 0.0 ){
      weight = localError/ ( localError + static_cast<double>(abs(coordinatesJ.get_local_error())) );
    }else{
      //The OSD has not determined its position yet (it has not even started), so we just modify limitly ours. (To allow "One client-One OSD" situations).
      weight = WEIGHT_IF_OSD_UNINITIALIZED;
    }
  }
  
  //Calculate proposed movement
  double delta;
  delta = CONSTANT_C * weight;

  double estimatedMovement = (static_cast<double>(measuredRTT)-subtractionMagnitude) * delta;        
  
  //Is the proposed movement too big?
  if( forceRecalculation ||           //Movement must be made anyway
      (subtractionMagnitude<=0.0) ||  //They both are in the same position
      (estimatedMovement<0.0) ||      //They must get closer
      ( abs(estimatedMovement) < subtractionMagnitude * MAX_MOVEMENT_RATIO) ){
  
    //Update local error
    if( localError <= 0 ){
      //We initialize the local error with the first absolute error measured
      localError = static_cast<double>(abs( subtractionMagnitude - static_cast<double>(measuredRTT) ));
    }else{
      //Compute relative weight moving average of local error
      localError = (relativeError * CONSTANT_E * weight) + localError* (1-(CONSTANT_E*weight));
    }
  
      																
    if( subtractionMagnitude > 0.0 ){ 
  	
      //Xi = Xi + delta * (rtt - || Xi - Xj ||) * u(Xi - Xj)
      org::xtreemfs::interfaces::VivaldiCoordinates additionVector(subtractionVector);
      if( getUnitaryCoordinates(additionVector) ){

    	  multiplyValueCoordinates( additionVector , estimatedMovement);
        //Move the node according to the calculated addition vector
        addCoordinates( ownCoordinates, additionVector);
        ownCoordinates.set_local_error(localError);
      }
    }else{ //subtractionMagnitude == 0.0
      	 
      //Both points have the same Coordinates, so we just pull them apart in a random direction
      org::xtreemfs::interfaces::VivaldiCoordinates randomCoords;
      modifyCoordinatesRandomly(randomCoords);
      org::xtreemfs::interfaces::VivaldiCoordinates additionVector(randomCoords);
          
      //Xi = Xi + delta * (rtt - || Xi - Xj ||) * u(randomVector)
      if( getUnitaryCoordinates(additionVector) ){

    		multiplyValueCoordinates( additionVector , estimatedMovement);
   
        //Move the node according to the calculated addition vector
        addCoordinates( ownCoordinates, additionVector);
        ownCoordinates.set_local_error(localError);
      }
    }
  
  }else{
      
    //The proposed movement is too big according to the current distance btw nodes
    retval = false;
  }
      
  return retval;
}

double VivaldiNode::calculateDistance(org::xtreemfs::interfaces::VivaldiCoordinates coordA,org::xtreemfs::interfaces::VivaldiCoordinates& coordB){
  subtractCoordinates(coordA,coordB);
  return magnitudeCoordinates( coordA );
}