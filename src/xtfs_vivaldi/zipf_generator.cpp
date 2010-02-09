/* Copyright 2009 Juan González de Benito.
 * This source comes from the XtreemFS project. It is licensed under the GPLv2
 * (see COPYING for terms and conditions).
 */

#include "zipf_generator.h"

#include <math.h>
#include <ctime>
#include <cstdlib>

using namespace xtfs_vivaldi;

ZipfGenerator::ZipfGenerator(const double skew) : skew(skew)
{
  
  srand( static_cast<unsigned>(time(0)) );
    
  size = -1;
  
}
/*
 *  Returns a rank in [0,this.size)
 */
int ZipfGenerator::next()
{
  int ret_val = -1;
  
  //Size must be set with function set_size() before generating any rank
  if( size > 0 )
  {
    int rank = -1;
    double frequency = 0.0f;
    double dice = 0.0f;

    while (dice >= frequency)
    {
      rank = rand() % size; //int in [0,size)
      frequency = get_probability(rank + 1);//(0 is not allowed for probability computation)
      dice = static_cast<double>(rand())/RAND_MAX; //double in [0.0,1.0]
    }
    
    ret_val = rank;
  }
  
  return ret_val;
}
/*
 * Probability (0.0,1.0) to choose a given rank
 */
double ZipfGenerator::get_probability(const int rank)
{
  if (rank == 0)
  {
      return -1.0f;
  }
  else
  {
    return (1.0f / pow(rank, skew)) / bottom;
  }
}

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