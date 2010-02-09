/* Copyright 2009 Juan González de Benito.
 * This source comes from the XtreemFS project. It is licensed under the GPLv2
 * (see COPYING for terms and conditions).
 */

#ifndef _XTFS_VIVALDI_VIVALDI_NODE_H_
#define _XTFS_VIVALDI_VIVALDI_NODE_H_

#include "xtreemfs/interfaces/types.h"
#include "xtreemfs/interfaces/osd_interface.h"

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

namespace xtfs_vivaldi
{
  class VivaldiNode
  {

    public:

    VivaldiNode( org::xtreemfs::interfaces::VivaldiCoordinates nodeCoordinates)\
              : ownCoordinates(nodeCoordinates) {}
    org::xtreemfs::interfaces::VivaldiCoordinates *getCoordinates();
    bool recalculatePosition(  org::xtreemfs::interfaces::VivaldiCoordinates& coordinatesJ,
                            uint64_t measuredRTT,
                            bool forceRecalculation);

    double calculateDistance( org::xtreemfs::interfaces::VivaldiCoordinates coordA,
                              org::xtreemfs::interfaces::VivaldiCoordinates& coordB);

    private:

    void multiplyValueCoordinates(  org::xtreemfs::interfaces::VivaldiCoordinates &coord,
                                  double value);
    void addCoordinates(org::xtreemfs::interfaces::VivaldiCoordinates &coordA,
                      org::xtreemfs::interfaces::VivaldiCoordinates coordB);
    void subtractCoordinates( org::xtreemfs::interfaces::VivaldiCoordinates &coordA,
                            org::xtreemfs::interfaces::VivaldiCoordinates coordB);
    double scalarProductCoordinates( org::xtreemfs::interfaces::VivaldiCoordinates coordA,
                                   org::xtreemfs::interfaces::VivaldiCoordinates coordB);
    double magnitudeCoordinates(org::xtreemfs::interfaces::VivaldiCoordinates coordA);
    bool getUnitaryCoordinates(org::xtreemfs::interfaces::VivaldiCoordinates &coord);
    void modifyCoordinatesRandomly(org::xtreemfs::interfaces::VivaldiCoordinates &coord);

    org::xtreemfs::interfaces::VivaldiCoordinates ownCoordinates;


  };

  class OutputUtils
  {
    public:

    static void writeHexInt(std::ostringstream &oss, const int value);
    static int readHexInt(const std::string &str,const int position);

    static void writeHexLongLong(std::ostringstream &oss, const long long value);
    static long long readHexLongLong(const std::string &str,const int position);

    static void stringToCoordinates( std::string &str,
                                   org::xtreemfs::interfaces::VivaldiCoordinates &vc);

    private:

    static const char trHex[16];
    static const char frHex[22][2];
    static char getEquivalentByte(char ch);
  };
};

#endif
