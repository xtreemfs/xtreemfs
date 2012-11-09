#!/bin/bash
mkdir html
cat xtfs-guide.tex | tth -e2  > html/xtfs-guide.html
cp -r images html/images
