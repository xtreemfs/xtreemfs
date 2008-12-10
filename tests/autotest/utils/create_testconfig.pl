#!/usr/bin/perl -w

my $CERTDIR = "/home/bjko/test/certs/";

if ($#ARGV < 4) {
	print "usage: $0 dir|mrc|osd0..N <debug level> <dir_host> <dir_port> <tmpdirectory> {use_ssl}\n\n";
	exit(1);
}

my $service = $ARGV[0];
my $debuglevel = $ARGV[1];
my $dir_host = $ARGV[2];
my $dir_port = $ARGV[3];
if ($dir_port eq "default") {
	$dir_port = "32638";
}
my $tmpdir = $ARGV[4];

if ($#ARGV == 6) {
	if ($ARGV[6] eq "use_ssl") {
		my $use_ssl = "true";
	} else {
		my $use_ssl = "false";
	}
} else {
	$use_ssl = "false";
}

if ($service eq "dir") {

	print "uuid = http://localhost:$dir_port\n";
	print "debug_level = $debuglevel\n";
	print "listen.port = $dir_port\n";
	print "database.dir = $tmpdir/dirdb/\n";
	print "authentication_provider = org.xtreemfs.common.auth.NullAuthProvider\n"; 
	if ($use_ssl eq "false") {
		print "ssl.enabled = false\n";
	} else {
		print "ssl.enabled = true\n";
		print "ssl.service_creds = ".$CERTDIR."dir/dir.p12\n";
		print "ssl.service_creds.pw = passphrase\n";
		print "ssl.service_creds.container = PKCS12\n";
		print "ssl.trusted_certs = ".$CERTDIR."trust.jks\n";
		print "ssl.trusted_certs.pw = passphrase\n";
		print "ssl.trusted_certs.container = JKS";
	}

} elsif ($service eq "mrc") {

	if ($#ARGV < 5) {
		print "usage: $0 mrc <debug level> <dir_host> <dir_port> <tmpdirectory> <listen_port>\n\n";
		exit(1);
	}

	$listen_port = $ARGV[5];
	if ($listen_port eq "default") {
		$listen_port = "32636";
	}	

	#print "uuid = http://localhost:32636\n";
	print "uuid = MRC-autotest-localhost-32636\n";
	print "debug_level = $debuglevel\n";
	print "listen.port = $listen_port\n";
	print "dir_service.host = $dir_host\n";
	print "dir_service.port = $dir_port\n";
	print "database.log = $tmpdir/mrc_operations.log\n";
	print "database.dir = $tmpdir/mrcdb/\n";
	print "osd_check_interval = 300\n";
	print "no_atime = true\n";
	print "no_fsync = true\n";
	print "local_clock_renewal = 50\n";
	print "remote_time_sync = 60000\n";
        print "database.checkpoint.interval = 1800000\n";
        print "database.checkpoint.idle_interval = 1000\n";
        print "database.checkpoint.logfile_size = 16384\n";
	print "authentication_provider = org.xtreemfs.common.auth.NullAuthProvider\n";
	print "capability_secret = testsecret\n";
	if ($use_ssl eq "false") {
		print "ssl.enabled = false\n";
	} else {
		print "ssl.enabled = true\n";
		print "ssl.service_creds = ".$CERTDIR."mrc/mrc.p12\n";
		print "ssl.service_creds.pw = passphrase\n";
		print "ssl.service_creds.container = PKCS12\n";
		print "ssl.trusted_certs = ".$CERTDIR."trust.jks\n";
		print "ssl.trusted_certs.pw = passphrase\n";
		print "ssl.trusted_certs.container = JKS";
	}

} elsif ($service =~ /osd(\d+)/) {

	$osdid = $1;
	$listen_port = $ARGV[5];

	if ($listen_port eq "default") {
		$listen_port = 32640+$osdid;
	}

	if ($#ARGV < 5) {
		print "usage: $0 mrc <debug level> <dir_host> <dir_port> <tmpdirectory> <listen_port>\n\n";
		exit(1);
	}

	#print "uuid = http://localhost:$listen_port\n";
	print "uuid = OSD-autotest-localhost-$listen_port\n";
	print "debug_level = $debuglevel\n";
	print "listen.port = $listen_port\n";
	print "dir_service.host = $dir_host\n";
	print "dir_service.port = $dir_port\n";
	print "object_dir = $tmpdir/osd$osdid/\n";
	print "local_clock_renewal = 50\n";
	print "remote_time_sync = 60000\n";
	print "report_free_space = true\n";
        print "checksums.enabled = false\n";
        print "capability_secret = testsecret\n";
        #print "checksums.algorithm = Adler32\n";
	if ($use_ssl eq "false") {
		print "ssl.enabled = false\n";
	} else {
		print "ssl.enabled = true\n";
		print "ssl.service_creds = ".$CERTDIR."osd/osd.p12\n";
		print "ssl.service_creds.pw = passphrase\n";
		print "ssl.service_creds.container = PKCS12\n";
		print "ssl.trusted_certs = ".$CERTDIR."trust.jks\n";
		print "ssl.trusted_certs.pw = passphrase\n";
		print "ssl.trusted_certs.container = JKS";
	}

} else {
	print "ERROR: unknown service '".$service."'\n\n";
	exit(1);
}
	




