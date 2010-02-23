/* Copyright 2009 Juan González de Benito.
 * This source comes from the XtreemFS project. It is licensed under the GPLv2
 * (see COPYING for terms and conditions).
 */

#include "zipf_generator.h"

#include <math.h>
#include <ctime>
#include <cstdlib>

using namespace xtfs_vivaldi;

/*
 * Creates a new ZipfGenerator with the given skewness
 *    skew: Desired skewness
 */
ZipfGenerator::ZipfGenerator(const double skew) : skew(skew)
{
  
  srand( static_cast<unsigned>(time(0)) );
    
  size = -1;
  
}
/*
 *  Returns a number from [0,this.size)
 */
int ZipfGenerator::next()
{
  int ret_val = -1;
  
  //Size must be set with function set_size() before generating any rank
  if( size > 0 )
  {
    int index = -1;
    double frequency = 0.0f;
    double dice = 0.0f;

    while (dice >= frequency)
    {
      index = rand() % size; //int in [0,size)
      frequency = get_probability(index + 1);//(0 is not allowed for probability computation)
      dice = static_cast<double>(rand())/RAND_MAX; //double in [0.0,1.0]
    }
    
    ret_val = index;
  }
  
  return ret_val;
}
/*
 * Returns the probability (0.0,1.0) to choose a given index
 */
double ZipfGenerator::get_probability(const int index)
{
  if (index == 0)
  {
      return -1.0f;
  }
  else
  {
    return (1.0f / pow(index, skew)) / bottom;
  }
}

/*
 * Modifies the rank of the generated indexes
 */
void ZipfGenerator::set_size(const int new_size)
{
    size = new_size;
    
  //calculate the generalized harmonic number of order 'size' of 'skew' 
  //http://en.wikipedia.org/wiki/Harmonic_number
  bottom = 0.0f;
  for (int i = 1; i <= size; i++)
  {
    bottom += (1.0f / pow(i, skew));
  }
}