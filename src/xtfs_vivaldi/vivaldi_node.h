// Copyright 2009 Juan Gonz�lez de Benito.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTFS_VIVALDI_VIVALDI_NODE_H_
#define _XTFS_VIVALDI_VIVALDI_NODE_H_

#include "xtreemfs/interfaces/types.h"
#include "xtreemfs/interfaces/osd_interface.h"


#define CONSTANT_E 0.10
#define CONSTANT_C 0.25
#define MAX_MOVEMENT_RATIO 0.10

namespace xtfs_vivaldi
{
	class VivaldiNode
	{

		public:

      VivaldiNode(org::xtreemfs::interfaces::VivaldiCoordinates nodeCoordinates): ownCoordinates(nodeCoordinates) {}
			org::xtreemfs::interfaces::VivaldiCoordinates *getCoordinates();
			bool recalculatePosition(	org::xtreemfs::interfaces::VivaldiCoordinates& coordinatesJ,
										long measuredRTT,
										bool forceRecalculation);

      //TOFIX:This method is just included to test the final results
      double calculateDistance(org::xtreemfs::interfaces::VivaldiCoordinates coordA,org::xtreemfs::interfaces::VivaldiCoordinates& coordB);

		private:

			void multiplyValueCoordinates(org::xtreemfs::interfaces::VivaldiCoordinates &coord,double value);
			void addCoordinates(org::xtreemfs::interfaces::VivaldiCoordinates &coordA,org::xtreemfs::interfaces::VivaldiCoordinates coordB);
			void subtractCoordinates(org::xtreemfs::interfaces::VivaldiCoordinates &coordA,org::xtreemfs::interfaces::VivaldiCoordinates coordB);
			double scalarProductCoordinates(org::xtreemfs::interfaces::VivaldiCoordinates coordA,org::xtreemfs::interfaces::VivaldiCoordinates coordB);
   		double magnitudeCoordinates(org::xtreemfs::interfaces::VivaldiCoordinates coordA);
  		bool getUnitaryCoordinates(org::xtreemfs::interfaces::VivaldiCoordinates &coord);
			void modifyCoordinatesRandomly(org::xtreemfs::interfaces::VivaldiCoordinates &coord);


			org::xtreemfs::interfaces::VivaldiCoordinates ownCoordinates;

	};
};

#endif
