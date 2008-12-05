#include <stdlib.h>
#include <stdio.h>

#include <transl_engine.h>
#include <transl_mod.h>

char *id = "RAID5";

int init(struct transl_mod *mod)
{
	int err = 0;

	return err;
}

int finish(struct transl_mod *mod)
{
	int err = 0;

	return err;
}

ssize_t read_fobj(struct transl_mod *tm,
		  struct file_replica *r,
		  int fobj_num,
		  void *data)
{
	ssize_t rv = 0;

	return rv;
}

int write_fobj(struct transl_mod *tm,
	       struct file_replica *r,
	       int fobj_num,
	       void *data)
{
	int err = 0;

	return err;
}
