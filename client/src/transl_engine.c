#include <stdlib.h>
#include <stdio.h>

#include <dirent.h>

#include <sys/types.h>

#include "transl_engine.h"
#include "transl_mod.h"
#include "stripingpolicy.h"


int transl_engine_init(struct transl_engine *te, char *mod_dir)
{
	int err = 0;

	INIT_LIST_HEAD(&te->mod_list);

	if (!mod_dir)
		goto out;

	err = transl_engine_load_mods(te, mod_dir);

out:
	return err;
}

void transl_engine_del_contents(struct transl_engine *te)
{

}

/**
 * Find a module that corresponds to id
 */
struct transl_mod *transl_engine_find_by_id(struct transl_engine *te,
					    char *id)
{
	struct transl_mod *rv = NULL;

	return rv;
}

/**
 * Find a translation module that corresponds to striping
 * policy 't'
 */
struct transl_mod *transl_engine_find_by_type(struct transl_engine *te,
					      enum striping_policy_t t)
{
	struct transl_mod *rv = NULL;
	
	rv = transl_engine_find_by_id(te, striping_policy_name[t].name);

	return rv;
}


int transl_engine_load_mods(struct transl_engine *te, char *mod_dir)
{
	int err = 0;
	DIR *dir;
	struct dirent *dent;
	char fullname[1024];
	struct transl_mod *tm = NULL;

	dir = opendir(mod_dir);
	if (!dir) {
		err = 1;
		goto out;
	}

	while ((dent = readdir(dir)) != NULL) {
		if (dent->d_type != DT_REG)
			continue;
		snprintf(fullname, 1024, "%s/%s", mod_dir, dent->d_name);
		tm = transl_mod_load(fullname);
		if (!tm)
			continue;
		list_add(&tm->head, &te->mod_list);
	}

	if (list_empty(&te->mod_list))
		err = 2;

out:
	if (dir)
		closedir(dir);

	return err;
}

void transl_engine_unload_mods(struct transl_engine *te)
{
	struct list_head *lh, *tmp;
	struct transl_mod *tm;

	list_for_each_safe(lh, tmp, &te->mod_list) {
		tm = container_of(lh, struct transl_mod, head);
		list_del(lh);
		transl_mod_destroy(tm);
	}
}
