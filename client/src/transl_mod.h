#ifndef __XTRFS_TRANSL_MOD_H__
#define __XTRFS_TRANSL_MOD_H__

#include "file.h"
#include "list.h"
#include "kernel_substitutes.h"

struct transl_mod {
	char *id;

	int (*init)(struct transl_mod *tm);
	int (*finish)(struct transl_mod *tm);

	ssize_t (*read_fobj)(struct transl_mod *tm,
			     struct file_replica *r,
			     int fobj_num,
			     void *data);
	int (*write_fobj)(struct transl_mod *tm,
			  struct file_replica *r,
			  int fobj_num,
			  void *data);

	/* Internal book keeping, please do not use! */
	void *dlhandle;
	struct list_head head;
	atomic_t use_count;
};

struct transl_mod *transl_mod_new();
int transl_mod_init(struct transl_mod *tm);
void transl_mod_del_contents(struct transl_mod *tm);
void transl_mod_destroy(struct transl_mod *tm);

struct transl_mod *transl_mod_load(char *path);

/* These are 'static' functions that can be used from any other module */


#endif
