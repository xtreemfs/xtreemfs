/*
 * Copyright (c)  2009 Juan Gonzalez de Benito.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_UTIL_ZIPF_GENERATOR_H_
#define CPP_INCLUDE_UTIL_ZIPF_GENERATOR_H_

namespace xtreemfs {
namespace util {

class ZipfGenerator {
 public:
  /**
   * Creates a new ZipfGenerator with the given skewness
   *    skew: Desired skewness
   */
  explicit ZipfGenerator(const double skew);

  /**
   *  Returns a number from [0,this.size)
   */
  int next();

  /**
   * Modifies the rank of the generated indexes
   */
  void set_size(const int new_size);

 private:
  /**
   * Returns the probability (0.0,1.0) to choose a given index
   */
  double get_probability(const int rank);

  int size;
  double skew;
  double bottom;
};
}  // namespace util
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_UTIL_ZIPF_GENERATOR_H_
