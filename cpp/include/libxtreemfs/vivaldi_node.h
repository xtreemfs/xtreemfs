/*
 * Copyright (c)  2009 Juan Gonzalez de Benito.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_VIVALDI_NODE_H_
#define CPP_INCLUDE_LIBXTREEMFS_VIVALDI_NODE_H_

#include <string>

#include "pbrpc/RPC.pb.h"
#include "xtreemfs/GlobalTypes.pb.h"

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
  explicit VivaldiNode(
      const xtreemfs::pbrpc::VivaldiCoordinates& nodeCoordinates)
   : ownCoordinates(nodeCoordinates) {
  }
  const xtreemfs::pbrpc::VivaldiCoordinates* GetCoordinates() const;
  bool RecalculatePosition(
          const xtreemfs::pbrpc::VivaldiCoordinates& coordinatesJ,
          uint64_t measuredRTT,
          bool forceRecalculation);

  static double CalculateDistance(xtreemfs::pbrpc::VivaldiCoordinates coordA,
                           const xtreemfs::pbrpc::VivaldiCoordinates& coordB);

 private:
  static void MultiplyValueCoordinates(
          xtreemfs::pbrpc::VivaldiCoordinates* coord,
          double value);
  static void AddCoordinates(xtreemfs::pbrpc::VivaldiCoordinates* coordA,
                      const xtreemfs::pbrpc::VivaldiCoordinates& coordB);
  static void SubtractCoordinates(xtreemfs::pbrpc::VivaldiCoordinates* coordA,
                           const xtreemfs::pbrpc::VivaldiCoordinates& coordB);
  static double ScalarProductCoordinates(
          const xtreemfs::pbrpc::VivaldiCoordinates& coordA,
          const xtreemfs::pbrpc::VivaldiCoordinates& coordB);
  static double MagnitudeCoordinates(
          const xtreemfs::pbrpc::VivaldiCoordinates& coordA);
  static bool GetUnitaryCoordinates(xtreemfs::pbrpc::VivaldiCoordinates* coord);
  static void ModifyCoordinatesRandomly(
          xtreemfs::pbrpc::VivaldiCoordinates* coord);

  xtreemfs::pbrpc::VivaldiCoordinates ownCoordinates;
};


class OutputUtils {
 public:
  static void WriteHexInt(std::ostringstream &oss, const int value);
  static int ReadHexInt(const std::string &str, const int position);

  static void WriteHexLongLong(std::ostringstream &oss, const int64_t value);
  static int64_t ReadHexLongLong(const std::string &str, const int position);

  static void StringToCoordinates(const std::string &str,
                                  xtreemfs::pbrpc::VivaldiCoordinates &vc);

 private:
  static const char trHex[16];
  static const char frHex[22][2];
  static char GetEquivalentByte(char ch);
};
}

#endif  // CPP_INCLUDE_LIBXTREEMFS_VIVALDI_NODE_H_
