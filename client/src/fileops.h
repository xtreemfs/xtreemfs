//
// C++ Interface: fileops
//
// Description: 
//
//
// Author: Matthias Hess <mhess@laptopmh.stg.hpce.tld>, (C) 2008
//
// Copyright: See COPYING file that comes with this distribution
//
//

#ifndef __XTRFS_FILEOPS_H__
#define __XTRFS_FILEOPS_H__

enum filerw_op {
	OP_FILERW_READ = 0,
	OP_FILERW_WRITE,
	OP_FILERW_DELETE
};

extern char *frwop_name[];

#ifdef __MINGW32__
typedef long long loff_t;
#endif

#endif
