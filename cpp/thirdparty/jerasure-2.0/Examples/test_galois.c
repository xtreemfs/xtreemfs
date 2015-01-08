#include <assert.h>
#include "galois.h"

int main(int argc, char **argv)
{
  assert(galois_init_default_field(4) == 0);
  assert(galois_uninit_field(4) == 0);
  assert(galois_init_default_field(4) == 0);
  assert(galois_uninit_field(4) == 0);

  assert(galois_init_default_field(8) == 0);
  assert(galois_uninit_field(8) == 0);
  assert(galois_init_default_field(8) == 0);
  assert(galois_uninit_field(8) == 0);

  return 0;
}
/*
 * Local Variables:
 * compile-command: "make test_galois && 
 *    libtool --mode=execute valgrind --tool=memcheck --leak-check=full ./test_galois"
 * End:
 */
