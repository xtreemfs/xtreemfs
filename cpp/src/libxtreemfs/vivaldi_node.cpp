/*
 * Copyright (c)  2009 Juan Gonzalez de Benito.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <cmath>
#include <cstdlib>
#include <iostream>

#include "libxtreemfs/vivaldi_node.h"

namespace xtreemfs {

  /**
   *
   * @return the current coordinates of the node.
   */
  xtreemfs::pbrpc::VivaldiCoordinates * VivaldiNode::getCoordinates() {
    return &this->ownCoordinates;
  }

  /**
   * Multiplies a pair of coordinates by a given real number and stores the result in coord
   *
   * @param coordA the coordinates to be multiplied.
   * @param value the real number to multiply by.
   */
  void VivaldiNode::multiplyValueCoordinates(xtreemfs::pbrpc::VivaldiCoordinates& coord,
                                             double value) {

    coord.set_x_coordinate(coord.x_coordinate() * value);
    coord.set_y_coordinate(coord.y_coordinate() * value);
  }

  /**
   * Adds two pairs of coordinates and stores the result in coordA
   */
  void VivaldiNode::addCoordinates(xtreemfs::pbrpc::VivaldiCoordinates &coordA,
                                   xtreemfs::pbrpc::VivaldiCoordinates coordB) {

    coordA.set_x_coordinate(coordA.x_coordinate() + coordB.x_coordinate());
    coordA.set_y_coordinate(coordA.y_coordinate() + coordB.y_coordinate());
  }

  /**
   * Subtracts two pairs of coordinates and stores the result in coordA
   */
  void VivaldiNode::subtractCoordinates(xtreemfs::pbrpc::VivaldiCoordinates &coordA,
                                        xtreemfs::pbrpc::VivaldiCoordinates coordB) {

    multiplyValueCoordinates(coordB, -1.0);
    addCoordinates(coordA, coordB);

  }

  /**
   * Multiplies two pairs of coordinates using the scalar product.
   *      A Â· B = Ax*Bx + Ay*By
   *
   * @param coordA a pair of coordinates.
   * @param coordB a pair of coordinates.
   * @return the result of the scalar product.
   */
  double VivaldiNode::scalarProductCoordinates(xtreemfs::pbrpc::VivaldiCoordinates coordA,
                                               xtreemfs::pbrpc::VivaldiCoordinates coordB) {

    double retval = 0.0;

    // A Â· B = Ax*Bx + Ay*By
    retval += coordA.x_coordinate() * coordB.x_coordinate();
    retval += coordA.y_coordinate() * coordB.y_coordinate();

    return retval;

  }

  /**
   * Calculates the magnitude of a given vector.
   *
   * @param coordA the coordinates whose magnitude must be calculated.
   * @return the distance from the position defined by the coordinates to the
   * origin of the system.
   */
  double VivaldiNode::magnitudeCoordinates(xtreemfs::pbrpc::VivaldiCoordinates coordA) {
    // ||A|| = sqrt( AxÂ² + AyÂ² )
    double sProd = scalarProductCoordinates(coordA, coordA);
    return sqrt(sProd);
  }

  /**
   * Calculates the unitary vector of a given vector and stores the result in coord
   *
   * @return true if it's been possible to calculate the vector or false otherwise
   */
  bool VivaldiNode::getUnitaryCoordinates(xtreemfs::pbrpc::VivaldiCoordinates & coord) {
    // unit(A) = A * ( 1 / ||A|| )
    // ||unit(A)|| = 1

    bool retval = false;

    double magn = magnitudeCoordinates(coord);

    if (magn > 0) { //cannot be == 0

      multiplyValueCoordinates(coord, 1.0 / magn);

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
  void VivaldiNode::modifyCoordinatesRandomly(xtreemfs::pbrpc::VivaldiCoordinates & coord) {

    //static_cast<double>(rand()))/RAND_MAX) generates real values btw 0 and 1
    coord.set_x_coordinate(((static_cast<double> (rand()) / RAND_MAX) *2) - 1);
    coord.set_y_coordinate(((static_cast<double> (rand()) / RAND_MAX) *2) - 1);
  }

  /**
   * Modifies the position of the node according to the current distance to a
   * given point in the coordinate space and the real RTT measured against it.
   */
  bool VivaldiNode::recalculatePosition(xtreemfs::pbrpc::VivaldiCoordinates& coordinatesJ,
                                        uint64_t measuredRTT,
                                        bool forceRecalculation) {
    bool retval = true;

    double localError = ownCoordinates.local_error();

    //SUBTRACTION = Xi - Xj
    xtreemfs::pbrpc::VivaldiCoordinates subtractionVector(ownCoordinates);
    subtractCoordinates(subtractionVector, coordinatesJ);

    // ||SUBTRACTION|| should be ~= RTT
    double subtractionMagnitude = magnitudeCoordinates(subtractionVector);

    //Sample weight balances local and remote error
    //If it's close to 1, J knows more than me: localError > errorJ
    //If it's close to 0.5, we both know the same: A/2A = 1/2
    //If it's close to 0, I know more than it: localError < errorJ
    double weight = 0.0;

    //Two nodes shouldn't be in the same position
    if (measuredRTT == 0) {
      measuredRTT = 1;
    }

    //Compute relative error of this sample
    double relativeError = static_cast<double> (abs(subtractionMagnitude - measuredRTT)) /
        static_cast<double> (measuredRTT);

    //Calculate weight
    if (localError <= 0.0) {
      weight = 1;
    } else {
      if (coordinatesJ.local_error() > 0.0) {
        weight = localError /
            (localError + static_cast<double> (abs(coordinatesJ.local_error())));
      } else {
        /*The OSD has not determined its position yet (it has not even started),
          so we just modify limitly ours. (To allow "One client-One OSD" situations).*/
        weight = WEIGHT_IF_OSD_UNINITIALIZED;
      }
    }

    //Calculate proposed movement
    double delta;
    delta = CONSTANT_C * weight;

    double estimatedMovement = (static_cast<double> (measuredRTT) - subtractionMagnitude) * delta;

    //Is the proposed movement too big?
    if (forceRecalculation || //Movement must be made anyway
        (subtractionMagnitude <= 0.0) || //They both are in the same position
        (estimatedMovement < 0.0) || //They must get closer
        (abs(estimatedMovement) < subtractionMagnitude * MAX_MOVEMENT_RATIO)) {

      //Update local error
      if (localError <= 0) {
        //We initialize the local error with the first absolute error measured
        localError = static_cast<double> (abs(subtractionMagnitude -  \
                                        static_cast<double> (measuredRTT)));
      } else {
        //Compute relative weight moving average of local error
        localError = (relativeError * CONSTANT_E * weight) +
            localError * (1 - (CONSTANT_E * weight));
      }


      if (subtractionMagnitude > 0.0) {

        //Xi = Xi + delta * (rtt - || Xi - Xj ||) * u(Xi - Xj)
        xtreemfs::pbrpc::VivaldiCoordinates additionVector(subtractionVector);
        if (getUnitaryCoordinates(additionVector)) {

          multiplyValueCoordinates(additionVector, estimatedMovement);
          //Move the node according to the calculated addition vector
          addCoordinates(ownCoordinates, additionVector);
          ownCoordinates.set_local_error(localError);
        }
      } else { //subtractionMagnitude == 0.0

        //Both points have the same Coordinates, so we just pull them apart in a random direction
        xtreemfs::pbrpc::VivaldiCoordinates randomCoords;
        modifyCoordinatesRandomly(randomCoords);
        xtreemfs::pbrpc::VivaldiCoordinates additionVector(randomCoords);

        //Xi = Xi + delta * (rtt - || Xi - Xj ||) * u(randomVector)
        if (getUnitaryCoordinates(additionVector)) {

          multiplyValueCoordinates(additionVector, estimatedMovement);

          //Move the node according to the calculated addition vector
          addCoordinates(ownCoordinates, additionVector);
          ownCoordinates.set_local_error(localError);
        }
      }

    } else {

      //The proposed movement is too big according to the current distance btw nodes
      retval = false;
    }

    return retval;
  }

  double VivaldiNode::calculateDistance(xtreemfs::pbrpc::VivaldiCoordinates coordA,
                                        xtreemfs::pbrpc::VivaldiCoordinates & coordB) {
    subtractCoordinates(coordA, coordB);
    return magnitudeCoordinates(coordA);
  }

  char const OutputUtils::trHex [16] = {'0', '1', '2', '3', '4', '5', '6', '7',  \
                                        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
  char const OutputUtils::frHex [22][2] = {
    {'0', 0},
    {'1', 1},
    {'2', 2},
    {'3', 3},  \
                                          {'4', 4},
    {'5', 5},
    {'6', 6},
    {'7', 7},  \
                                          {'8', 8},
    {'9', 9},
    {'a', 10},
    {'A', 10},  \
                                          {'b', 11},
    {'B', 11},
    {'c', 12},
    {'C', 12}, \
                                          {'d', 13},
    {'D', 13},
    {'e', 14},
    {'E', 14}, \
                                          {'f', 15},
    {'F', 15}
  };

  void OutputUtils::writeHexInt(std::ostringstream &oss, const int value) {
    oss << trHex[(value & 0x0F)];
    oss << trHex[((value >> 4) & 0x0F)];
    oss << trHex[((value >> 8) & 0x0F)];
    oss << trHex[((value >> 12) & 0x0F)];
    oss << trHex[((value >> 16) & 0x0F)];
    oss << trHex[((value >> 20) & 0x0F)];
    oss << trHex[((value >> 24) & 0x0F)];
    oss << trHex[((value >> 28) & 0x0F)];
  }

  char OutputUtils::getEquivalentByte(char ch) {

    char retval = 0xFF;

    for (int i = 0; i < 22; i++) {
      if (frHex[i][0] == ch) {
        retval = frHex[i][1];
        break;
      }
    }

    return retval;
  }

  int OutputUtils::readHexInt(const std::string &str, const int position) {
    int value = getEquivalentByte(str[position]);
    value += static_cast<int> (getEquivalentByte(str[position + 1])) << 4;
    value += static_cast<int> (getEquivalentByte(str[position + 2])) << 8;
    value += static_cast<int> (getEquivalentByte(str[position + 3])) << 12;
    value += static_cast<int> (getEquivalentByte(str[position + 4])) << 16;
    value += static_cast<int> (getEquivalentByte(str[position + 5])) << 20;
    value += static_cast<int> (getEquivalentByte(str[position + 6])) << 24;
    value += static_cast<int> (getEquivalentByte(str[position + 7])) << 28;

    return value;
  }

  void OutputUtils::writeHexLongLong(std::ostringstream &oss, const long long value) {
    writeHexInt(oss, static_cast<int> (value & 0xFFFFFFFF));
    writeHexInt(oss, static_cast<int> (value >> 32));
  }

  long long OutputUtils::readHexLongLong(const std::string &str, const int position) {
    int low = readHexInt(str, position);
    int high = readHexInt(str, position + 8);

    // calculate the value: left-shift the upper 4 bytes by 32 bit and
    // append the lower 32 bit
    long long value = (static_cast<long long> (high)) << 32 |
        ((static_cast<long long> (low)) & 4294967295L);
    return value;
  }

  void OutputUtils::stringToCoordinates(
      const std::string &str,
      xtreemfs::pbrpc::VivaldiCoordinates & vc) {
    long long aux_long_x = readHexLongLong(str, 0);
    long long aux_long_y = readHexLongLong(str, 16);
    long long aux_long_err = readHexLongLong(str, 32);


    vc.set_x_coordinate(static_cast<double> (aux_long_x));
    vc.set_y_coordinate(static_cast<double> (aux_long_y));
    vc.set_local_error(static_cast<double> (aux_long_err));
  }

}
