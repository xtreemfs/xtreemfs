#!/bin/bash

for p in flease foundation pbrpcgen servers
do
  for f in .classpath .project
  do
    dest="${p}/${f}"
    
    if [ ! -f "$dest" ]
    then
      source="${p}/eclipse-project/${f}"
      
      if [ -f "$source" ]
      then
        cp "$source" "$dest"
      else
        echo "$source not found and therefore not copied."
      fi
    fi
  done
done

echo Finished.
echo Press any key to continue...
read
