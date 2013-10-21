#!/bin/bash

# run pdflatex to generate aux files used by tth (3 times, just to make sure)
pdflatex xtfs-guide
pdflatex xtfs-guide
pdflatex xtfs-guide

mkdir html
cat xtfs-guide.tex | tth -Lxtfs-guide -e2  > html/xtfs-guide.html
#cat xtfs-guide.tex | tth -e2  > html/xtfs-guide.html
cp -r --force --remove-destination images html/
