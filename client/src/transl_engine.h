#ifndef __XTRFS_TRANSL_ENGINE_H__
#define __XTRFS_TRANSL_ENGINE_H__

#include "transl_mod.h"
#include "stripingpolicy.h"

struct transl_mod;

struct transl_engine {
	struct list_head mod_list;
};

int transl_engine_init(struct transl_engine *te, char *mod_dir);
void transl_engine_del_contents(struct transl_engine *te);

int transl_engine_register(struct transl_engine *te,
			   struct transl_mod *tem);

struct transl_mod *transl_engine_find_by_id(struct transl_engine *te,
					    char *id);
struct transl_mod *transl_engine_find_by_type(struct transl_engine *te,
					      enum striping_policy_t t);

int transl_engine_load_mods(struct transl_engine *te, char *mod_dir);

#endif
