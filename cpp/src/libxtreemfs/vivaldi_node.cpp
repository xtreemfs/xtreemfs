/*
 * Copyright (c)  2009 Juan Gonzalez de Benito.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <cmath>
#include <cstdlib>
#include <iostream>
#include <sstream>

#include "libxtreemfs/vivaldi_node.h"

namespace xtreemfs {

  /**
   *
   * @return the current coordinates of the node.
   */
  const xtreemfs::pbrpc::VivaldiCoordinates * VivaldiNode::GetCoordinates() const {
    return &this->ownCoordinates;
  }

  /**
   * Multiplies a pair of coordinates by a given real number and stores the
   * result in coord
   *
   * @param coordA the coordinates to be multiplied.
   * @param value the real number to multiply by.
   */
  void VivaldiNode::MultiplyValueCoordinates(
      xtreemfs::pbrpc::VivaldiCoordinates* coord,
      double value) {
    coord->set_x_coordinate(coord->x_coordinate() * value);
    coord->set_y_coordinate(coord->y_coordinate() * value);
  }

  /**
   * Adds two pairs of coordinates and stores the result in coordA
   */
  void VivaldiNode::AddCoordinates(
      xtreemfs::pbrpc::VivaldiCoordinates* coordA,
      const xtreemfs::pbrpc::VivaldiCoordinates& coordB) {
    coordA->set_x_coordinate(coordA->x_coordinate() + coordB.x_coordinate());
    coordA->set_y_coordinate(coordA->y_coordinate() + coordB.y_coordinate());
  }

  /**
   * Subtracts two pairs of coordinates and stores the result in coordA
   */
  void VivaldiNode::SubtractCoordinates(
      xtreemfs::pbrpc::VivaldiCoordinates* coordA,
      const xtreemfs::pbrpc::VivaldiCoordinates& coordB) {
    coordA->set_x_coordinate(coordA->x_coordinate() - coordB.x_coordinate());
    coordA->set_y_coordinate(coordA->y_coordinate() - coordB.y_coordinate());
  }

  /**
   * Multiplies two pairs of coordinates using the scalar product.
   *      A Â· B = Ax*Bx + Ay*By
   *
   * @param coordA a pair of coordinates.
   * @param coordB a pair of coordinates.
   * @return the result of the scalar product.
   */
  double VivaldiNode::ScalarProductCoordinates(
      const xtreemfs::pbrpc::VivaldiCoordinates& coordA,
      const xtreemfs::pbrpc::VivaldiCoordinates& coordB) {
    double retval = 0.0;
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
  double VivaldiNode::MagnitudeCoordinates(
      const xtreemfs::pbrpc::VivaldiCoordinates& coordA) {
    double sProd = ScalarProductCoordinates(coordA, coordA);
    return sqrt(sProd);
  }

  /**
   * Calculates the unitary vector of a given vector and stores the result
   * in coord
   *
   * @return true if it's been possible to calculate the vector or false
   * otherwise
   */
  bool VivaldiNode::GetUnitaryCoordinates(
      xtreemfs::pbrpc::VivaldiCoordinates* coord) {
    bool retval = false;
    double magn = MagnitudeCoordinates(*coord);

    if (magn > 0) {  // cannot be == 0
      MultiplyValueCoordinates(coord, 1.0 / magn);
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
  void VivaldiNode::ModifyCoordinatesRandomly(
      xtreemfs::pbrpc::VivaldiCoordinates* coord) {
    // static_cast<double>(rand()))/RAND_MAX) generates real values btw 0 and 1
    coord->set_x_coordinate(((static_cast<double> (rand()) / RAND_MAX) *2) - 1);
    coord->set_y_coordinate(((static_cast<double> (rand()) / RAND_MAX) *2) - 1);
  }

  /**
   * Modifies the position of the node according to the current distance to a
   * given point in the coordinate space and the real RTT measured against it.
   */
  bool VivaldiNode::RecalculatePosition(
      const xtreemfs::pbrpc::VivaldiCoordinates& coordinatesJ,
      uint64_t measuredRTT,
      bool forceRecalculation) {
    bool retval = true;
    double localError = ownCoordinates.local_error();

    // SUBTRACTION = Xi - Xj
    xtreemfs::pbrpc::VivaldiCoordinates subtractionVector(ownCoordinates);
    SubtractCoordinates(&subtractionVector, coordinatesJ);

    // ||SUBTRACTION|| should be ~= RTT
    double subtractionMagnitude = MagnitudeCoordinates(subtractionVector);

    // Sample weight balances local and remote error
    // If it's close to 1, J knows more than me: localError > errorJ
    // If it's close to 0.5, we both know the same: A/2A = 1/2
    // If it's close to 0, I know more than it: localError < errorJ
    double weight = 0.0;

    // Two nodes shouldn't be in the same position
    if (measuredRTT == 0) {
      measuredRTT = 1;
    }

    // Compute relative error of this sample
    double relativeError = static_cast<double> (
        std::abs(subtractionMagnitude - measuredRTT)) /
        static_cast<double> (measuredRTT);

    // Calculate weight
    if (localError <= 0.0) {
      weight = 1;
    } else {
      if (coordinatesJ.local_error() > 0.0) {
        weight = localError / (localError
                 + static_cast<double> (std::abs(coordinatesJ.local_error())));
      } else {
        /* The OSD has not determined its position yet (it has not even
         * started), so we just modify limitly ours. (To allow "One client-One
         * OSD" situations). */
        weight = WEIGHT_IF_OSD_UNINITIALIZED;
      }
    }

    // Calculate proposed movement
    double delta;
    delta = CONSTANT_C * weight;

    double estimatedMovement = (static_cast<double> (measuredRTT)
                                - subtractionMagnitude) * delta;

    // Is the proposed movement too big?
    if (forceRecalculation ||  // Movement must be made anyway
        (subtractionMagnitude <= 0.0) ||  // They both are in the same position
        (estimatedMovement < 0.0) ||  // They must get closer
        (std::abs(estimatedMovement) <
        subtractionMagnitude * MAX_MOVEMENT_RATIO)) {
      // Update local error
      if (localError <= 0) {
        // We initialize the local error with the first absolute error measured
        localError = static_cast<double> (std::abs(subtractionMagnitude -  \
                                        static_cast<double> (measuredRTT)));
      } else {
        // Compute relative weight moving average of local error
        localError = (relativeError * CONSTANT_E * weight) +
            localError * (1 - (CONSTANT_E * weight));
      }


      if (subtractionMagnitude > 0.0) {
        // Xi = Xi + delta * (rtt - || Xi - Xj ||) * u(Xi - Xj)
        xtreemfs::pbrpc::VivaldiCoordinates additionVector(subtractionVector);
        if (GetUnitaryCoordinates(&additionVector)) {
          MultiplyValueCoordinates(&additionVector, estimatedMovement);
          // Move the node according to the calculated addition vector
          AddCoordinates(&ownCoordinates, additionVector);
          ownCoordinates.set_local_error(localError);
        }
      } else {  // subtractionMagnitude == 0.0
        // Both points have the same Coordinates, so we just pull
        // them apart in a random direction
        xtreemfs::pbrpc::VivaldiCoordinates randomCoords;
        ModifyCoordinatesRandomly(&randomCoords);
        xtreemfs::pbrpc::VivaldiCoordinates additionVector(randomCoords);

        // Xi = Xi + delta * (rtt - || Xi - Xj ||) * u(randomVector)
        if (GetUnitaryCoordinates(&additionVector)) {
          MultiplyValueCoordinates(&additionVector, estimatedMovement);
          // Move the node according to the calculated addition vector
          AddCoordinates(&ownCoordinates, additionVector);
          ownCoordinates.set_local_error(localError);
        }
      }

    } else {
      // The proposed movement is too big according to the current distance
      //  between nodes
      retval = false;
    }

    return retval;
  }

  double VivaldiNode::CalculateDistance(
      xtreemfs::pbrpc::VivaldiCoordinates coordA,
      const xtreemfs::pbrpc::VivaldiCoordinates& coordB) {
    SubtractCoordinates(&coordA, coordB);
    return MagnitudeCoordinates(coordA);
  }

  static unsigned int ReadHexInt(const std::string &str, int position) {
    return strtoul(str.substr(position, 8).c_str(), NULL, 16);
  }

  static int64_t ReadHexLongLong(const std::string &str, int position) {
    int low = ReadHexInt(str, position);
    int high = ReadHexInt(str, position + 8);

    // calculate the value: left-shift the upper 4 bytes by 32 bit and
    // append the lower 32 bit
    int64_t value = (static_cast<int64_t> (high)) << 32 |
        ((static_cast<int64_t> (low)) & 0xFFFFFFFF);
    return value;
  }

  void OutputUtils::StringToCoordinates(
      const std::string& str,
      xtreemfs::pbrpc::VivaldiCoordinates & vc) {
    int64_t aux_long_x = ReadHexLongLong(str, 0);
    int64_t aux_long_y = ReadHexLongLong(str, 16);
    int64_t aux_long_err = ReadHexLongLong(str, 32);

    vc.set_x_coordinate(static_cast<double> (aux_long_x));
    vc.set_y_coordinate(static_cast<double> (aux_long_y));
    vc.set_local_error(static_cast<double> (aux_long_err));
  }
}
