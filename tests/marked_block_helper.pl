#!/usr/bin/perl

# Trying to detect disk write errors...
# Write a bunch of large files with a fixed pattern for each 512 bytes
# block. The block and file name can be recognised from its content.
#
#   This program is free software; you can redistribute it and/or modify
#   it under the terms of the GNU General Public License as published by
#   the Free Software Foundation; either version 2 of the License, or
#   (at your option) any later version.
#
#   This program is distributed in the hope that it will be useful,
#   but WITHOUT ANY WARRANTY; without even the implied warranty of
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#   GNU General Public License for more details.
#
#   You should have received a copy of the GNU General Public License
#   along with this program; if not, write to the Free Software
#   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
#
# Copyright (c) Erich Focht <efocht@hpce.nec.com>
#               All rights reserved

use strict;
use Digest::MD5 qw(md5_hex);
use Getopt::Long;

sub usage {

    print <<'EOF';
Trying to detect disk write errors...
-------------------------------------
Write a bunch of large files with a fixed pattern for each 512 bytes
block. The block and file name can be recognised from its content,
each block contains a repetition of the filename and the block number
filled up to 512 bytes with "x" characters.

The md5sum of the file is computed on the fly and written to a corresponding
file, such that the integrity of each file can be checked separately.

The file checking is done in groups, the default group size is 50 files.
The group size should be chosen such that at check time the file is flushed
to disk. Whether this makes sense or not depends on the origin of the error,
of course...

Usage:
    marked_block.pl [--start=<startindex>] [--nfiles=<number_of_files>] \
                    [--group=<groupsize>] [--check] [--file <name>]

    --start=startindex : file number to start with (default = 1)
    --nfiles=n         : number of files to write (default = 100)
    --group=groupsize  : invoke md5sum after writing <groupsize> files
                         (default = 50)
    --check            : check files instead of writing, compare with
                         expected content and print deviating blocks.
    --file <name>      : write/check only one particular file
    --base <basename>  : basename for building the filename
    --size <megabytes> : size of testfiles (default: 100MB)
    --help             : print this help message

Typical usage:
  marked_blocks.pl --start=1 --nfiles=1000
      runs until it finds some checksum discrepancy in a file,
      suppose this is file testfile0054. In order to display the
      discrepancy to the file on disk:
  marked_blocks.pl --start=54 --nfiles=1 --check
      or
  marked_blocks.pl --file testfile0054 --check

Written and (c) by Erich Focht @ NEC. Use at your own risk!

EOF
    exit 0;
}

sub min {
    my ($a, $b) = @_;
    return $a if ($a <= $b);
    return $b;
}

my $filesize = 100 * 1024 * 1024;
my $nfiles = 100;
my $namebase = "testfile";
my $nfstart = 1;
my $group = 50;
my $check = 0;
my $file;

GetOptions(
	   "help|h"    => \&usage,
           "start=i"   => \$nfstart,
           "nfiles|n=i"=> \$nfiles,
	   "group=i"   => \$group,
	   "check"     => \$check,
	   "file=s"    => \$file,
           "base=s"    => \$namebase,
	   "size=i"    => \$filesize,
           ) || &usage();

my $nblocks = $filesize * 1024 * 1024 / 512;  # 100MB

if ($file) {
    $file =~ /^(\D+)(\d+)$/;
    $namebase = $1;
    $nfstart = int($2);
    $nfiles = 1;
}

if (!$check) {
    print "Writing $nfiles files with prefix $namebase, starting with $nfstart.\n";
} else {
    print "Checking $nfiles files with prefix $namebase, starting with $nfstart.\n";
}

my ($string, $block, $slen, $written, $read, $bread);

my $nfend = $nfstart + $nfiles - 1;

for (my $fb = $nfstart; $fb <= $nfend; $fb = $fb + $group) {

    for (my $f = $fb; $f <= min($fb + $group - 1, $nfend); $f++) {
	my $name = sprintf("%s%04d",$namebase,$f);
	my @bad;

	$| = 1;
	print "File: $name\n";

	if (!$check) {
	    open OUT, "> $name" or die "Could not open file $name: $!";
	} else {
	    open IN, "$name" or die "Could not open file $name: $!";
	}

	my $md5 = Digest::MD5->new;

	for (my $i = 0; $i < $nblocks; $i++) {
	    $string = "file $name  block $i:";
	    $slen = length($string);
	    $block = $string x int(512/$slen);
	    $block .= "x" x (512 - length($block));

	    if (!$check) {
		$written = syswrite(OUT, $block, 512);
		die "written = $written instead of 512!" if ($written != 512);
		$md5->add($block);
	    } else {
		$read = sysread(IN, $bread, 512);
		die "read = $read instead of 512!" if ($read != 512);
		if (substr($bread, 0, 512) ne substr($block, 0, 512)) {
		    push @bad, $i;
		    print "Block $i has unexpected content:\n";
		    print_hex($bread,512);
		    print "-" x 70 . "\n";
		    $bread =~ s/[:^print:]/./g;
		    print "$bread\n";
		    print "-" x 70 . "\n";
		}
	    }
	}
	if (!$check) {
	    close OUT;
	    open MD5, "> $name.md5" or die "Could not open $name.md5 : $!";
	    print MD5 $md5->hexdigest . "  $name\n";
	    close MD5;
	} else {
	    if (@bad) {
		print "Following 512 byte blocks were bad: "
		    . join(" ",@bad) . "\n";
	    }
	    close IN;
	}
    }
    if (!$check) {
	for (my $f = $fb; $f <= min($fb + $group - 1, $nfend); $f++) {
	    my $mdfile = sprintf("%s%04d%s",$namebase,$f,".md5");
	    !system("md5sum -c $mdfile") or die "md5sum failed!";
	}
    }
}


exit 0;

sub print_hex {
    my ($data, $len) = @_;
    for (my $i = 0; $i < $len; $i += 24) {
	printf "%03d: ",$i;
	for (my $j = $i; $j < min($i + 24, $len); $j++) {
	    my $b = unpack "C", substr($data,$j,1);
	    printf "%02x ", $b;
	}
	print "\n";
    }
}

