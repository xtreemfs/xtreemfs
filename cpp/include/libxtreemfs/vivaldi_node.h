/*
 * Copyright (c)  2009 Juan Gonzalez de Benito.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_VIVALDI_NODE_H
#define	CPP_INCLUDE_LIBXTREEMFS_VIVALDI_NODE_H

#include "pbrpc/RPC.pb.h"
#include "xtreemfs/GlobalTypes.pb.h"

#include <string>
#include <iostream>
#include <sstream>


#define CONSTANT_E 0.10
#define CONSTANT_C 0.25
#define MAX_MOVEMENT_RATIO 0.10
/*
 * If the client contacts an OSD which has not started recalculating its position
 * yet (and therefore has no information about the space) it just trusts it partially.
 * Next value is used to reduce the magnitude of the proposed movement.
 */
#define WEIGHT_IF_OSD_UNINITIALIZED 0.1

namespace xtreemfs {

class VivaldiNode {
 public:

  VivaldiNode(xtreemfs::pbrpc::VivaldiCoordinates nodeCoordinates)
              : ownCoordinates(nodeCoordinates) {
  }
  xtreemfs::pbrpc::VivaldiCoordinates* getCoordinates();
  bool recalculatePosition(xtreemfs::pbrpc::VivaldiCoordinates& coordinatesJ,
                           uint64_t measuredRTT,
                           bool forceRecalculation);

  double calculateDistance(xtreemfs::pbrpc::VivaldiCoordinates coordA,
                           xtreemfs::pbrpc::VivaldiCoordinates& coordB);

 private:

  void multiplyValueCoordinates(xtreemfs::pbrpc::VivaldiCoordinates &coord,
                                double value);
  void addCoordinates(xtreemfs::pbrpc::VivaldiCoordinates &coordA,
                      xtreemfs::pbrpc::VivaldiCoordinates coordB);
  void subtractCoordinates(xtreemfs::pbrpc::VivaldiCoordinates &coordA,
                           xtreemfs::pbrpc::VivaldiCoordinates coordB);
  double scalarProductCoordinates(xtreemfs::pbrpc::VivaldiCoordinates coordA,
                                  xtreemfs::pbrpc::VivaldiCoordinates coordB);
  double magnitudeCoordinates(xtreemfs::pbrpc::VivaldiCoordinates coordA);
  bool getUnitaryCoordinates(xtreemfs::pbrpc::VivaldiCoordinates &coord);
  void modifyCoordinatesRandomly(xtreemfs::pbrpc::VivaldiCoordinates &coord);

  xtreemfs::pbrpc::VivaldiCoordinates ownCoordinates;
};

class OutputUtils {
 public:

  static void writeHexInt(std::ostringstream &oss, const int value);
  static int readHexInt(const std::string &str, const int position);

  static void writeHexLongLong(std::ostringstream &oss, const long long value);
  static long long readHexLongLong(const std::string &str, const int position);

  static void stringToCoordinates(const std::string &str,
                                  xtreemfs::pbrpc::VivaldiCoordinates &vc);

 private:

  static const char trHex[16];
  static const char frHex[22][2];
  static char getEquivalentByte(char ch);
};

}


#endif	/* VIVALDI_NODE_H */

