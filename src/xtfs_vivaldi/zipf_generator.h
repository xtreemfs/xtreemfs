/* Copyright 2009 Juan González de Benito.
 * This source comes from the XtreemFS project. It is licensed under the GPLv2
 * (see COPYING for terms and conditions).
 */

#ifndef _XTFS_VIVALDI_ZIPF_GENERATOR_H
#define	_XTFS_VIVALDI_ZIPF_GENERATOR_H

#include <cstdio>
#include <vector>

namespace xtfs_vivaldi
{
  class ZipfGenerator
  {

  public:

    ZipfGenerator(const double skew);
    int next();
    void set_size(const int new_size);

  private:

    double get_probability(const int rank);

    int size;
    double skew;
    double bottom;
  };
};
#endif	/* _XTFS_VIVALDI_ZIPF_GENERATOR_H */

