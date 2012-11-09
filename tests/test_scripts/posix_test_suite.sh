#!/bin/bash

# Abort if any command fails.
set -e

XTFSUTIL="$1/bin/xtfsutil"
PJD_POSIX_TEST_SUITE="$1/tests/test_scripts/pjd-fstest-20090130-RC_XtreemFS"

if [ ! -e "$XTFSUTIL" ]
then
  echo "xtfsutil not found. Make sure that \$1 is set to the top directory of the XtreemFS source tree (current value: $1)"
  exit 1
fi

# We assume that we're already inside the root directory of the mounted XtreemFS volume to be tested.

# Check if this volume is mounted with allow_other.
this_mount_point=$(df -P .|grep -v ^Filesystem|awk '{ print $6 }')
mount_options=$(grep ^xtreemfs@ /proc/mounts | grep $this_mount_point | cut -d' ' -f4)
echo "$mount_options" | grep allow_other >/dev/null || {
  echo "Volume not mounted with option -o allow_other (options: $mount_options)"
  exit 1
}

# Check if --chown-non-root is set.
getfattr -d -m . "$this_mount_point" 2>&1 | grep 'xtreemfs.volattr.chown_non_root="true"' >/dev/null || {
  echo "Volume was not created with option --chown-non-root. This option is required to succeed the tests."
  exit 1
}

# Check if we are root or can run sudo without password
SUDO_CMD=""
if [ $UID -ne 0 ]
then
  echo | sudo -S prove "$PJD_POSIX_TEST_SUITE/bogus.t" &>/dev/null && SUDO_CMD="sudo" || {
  if [ "$(hostname -f)" = "xtreem.zib.de" ]
  then
    # Failing this sudo check is never on option on our own test machine.
    echo "Did not succeed to run 'prove' with sudo without password (this machine is: "$(hostname -f)")"
    exit 1
  else
    cat <<EOF
WARNING: This test has to be run as root.

However, this test script is neither executed as root nor is it allowed to run 'sudo' without a password.

Therefore this test will be skipped. Success will be returned to avoid confusion among non-developers who run the test suite.
EOF
    exit 0
  fi
}
fi

# Run the actual test:
if [ ! -d "$PJD_POSIX_TEST_SUITE" ]
then
  echo "The POSIX Test Suite files were not found. Make sure that \$1 is set to the top directory of the XtreemFS source tree (current value: $1)"
  exit 1
fi

# Create fstest binary first.
make -C "$PJD_POSIX_TEST_SUITE" >/dev/null
$SUDO_CMD prove -r "$PJD_POSIX_TEST_SUITE/tests" 2>/dev/null