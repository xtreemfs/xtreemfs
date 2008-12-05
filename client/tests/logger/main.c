#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

#include "logger.h"


int
main(int argc, char **argv)
{	
	// log_init(&logit, syslog_log, LOGLEV_DEBUG);
	
	dbg_msg("Doing info!\n");
	trace_msg("Here I am %s.\nAnd there we go!\n", "hallo");
	
	log_end(&logit);
	
	return 0;
}
