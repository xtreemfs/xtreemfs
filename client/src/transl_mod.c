#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <dlfcn.h>

#include "transl_mod.h"

struct transl_mod *transl_mod_new()
{
	struct transl_mod *rv = NULL;

	rv = (struct transl_mod *)malloc(sizeof(struct transl_mod));
	if (rv && transl_mod_init(rv)) {
		transl_mod_destroy(rv);
		rv = NULL;
	}

	return rv;
}

int transl_mod_init(struct transl_mod *tm)
{
	int err = 0;

	tm->id = NULL;
	tm->init = NULL;
	tm->finish = NULL;
	tm->read_fobj = NULL;
	tm->write_fobj = NULL;

	INIT_LIST_HEAD(&tm->head);
	tm->dlhandle = NULL;

	return err;
}

void transl_mod_del_contents(struct transl_mod *tm)
{
	free(tm->id);
	if (tm->dlhandle)
		dlclose(tm->dlhandle);
}

void transl_mod_destroy(struct transl_mod *tm)
{
	transl_mod_del_contents(tm);
	free(tm);
}


/**
 * Load a translation module
 *
 * If the given file contains a translation module, the module structure
 * is returned.
 *
 * @return module pointer if the file contains a module, NULL otherwise
 */
struct transl_mod *transl_mod_load(char *path)
{
	struct transl_mod *rv = NULL;
	int err = 0;
	char **id;

	dbg_msg("Trying to load '%s'\n", path);

	rv = transl_mod_new();
	if (!rv)
		goto out;

	rv->dlhandle = dlopen(path, RTLD_NOW | RTLD_LOCAL);
	if (rv->dlhandle == NULL) {
		err = 1;
		goto out;
	}

	/* Now check if we find an 'id' in the module */
	id = (char **)dlsym(rv->dlhandle, "id");
	if (!id) {
		err = 2;
		goto out;
	}
	rv->id = strdup(*id);
	dbg_msg("Module ID seems to be '%s'\n", rv->id);

	/* ... and try to load the module functions */

	rv->init = (int (*)(struct transl_mod *))dlsym(rv->dlhandle, "init");
	rv->finish = (int (*)(struct transl_mod *))dlsym(rv->dlhandle, "finish");
	rv->read_fobj = (ssize_t (*)(struct transl_mod *,
				     struct file_replica *,
				     int,
				     void *))
			dlsym(rv->dlhandle, "read_fobj");
	rv->write_fobj = (int (*)(struct transl_mod *,
				  struct file_replica *,
				  int,
				  void *))
			 dlsym(rv->dlhandle, "write_fobj");

	if (!rv->init || !rv->finish || !rv->read_fobj || !rv->write_fobj) {
		err = 3;
		goto out;
	}

	dbg_msg("Found all required symbols\n");

out:
	if (err) {
		transl_mod_destroy(rv);
		rv = NULL;
	}

	return rv;
}
