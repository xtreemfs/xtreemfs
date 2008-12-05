#include <stdlib.h>
#include <stdio.h>

#include <transl_engine.h>
#include <transl_mod.h>

#include <file.h>
#include <sobj_cache.h>

extern struct sobj_cache sobj_cache;


char *id = "RAID0";

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
	void *shandle;

	shandle = sobj_cache_start_op(&sobj_cache, r);
	/* We have a one to one mapping */
	sobj_cache_read_sobj(shandle, fobj_num, data);
	sobj_cache_exec_op(shandle);

	return rv;
}

int write_fobj(struct transl_mod *tm,
	       struct file_replica *r,
	       int fobj_num,
	       void *data)
{
	int err = 0;
	void *shandle = NULL;

	shandle = sobj_cache_start_op(&sobj_cache, r);
	sobj_cache_write_sobj(shandle, fobj_num, data);
	sobj_cache_exec_op(shandle);

	return err;
}

