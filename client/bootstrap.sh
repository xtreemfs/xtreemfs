#!/bin/sh

libtoolize --force
automake --add-missing
aclocal
autoconf

