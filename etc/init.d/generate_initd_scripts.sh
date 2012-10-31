#!/bin/bash

# This script generates the /etc/init.d/xtreemfs-{dir,mrc,osd} scripts
# based on the file 'xtreemfs-service.template'.


# Settings
services="dir mrc osd"

declare -A long_service_names
long_service_names["dir"]="Directory Service"
long_service_names["mrc"]="Metadata and Replica Catalog"
long_service_names["osd"]="Object Storage Device"

template_file="xtreemfs-service.template"

# Create files
script_directory=$(dirname "$0")

for service in $services
do
  short_service_name_lowercase=$service
  short_service_name_uppercase=$(echo $service | tr [[:lower:]] [[:upper:]])
  long_service_name=${long_service_names[$service]}
  if [ "$service" = "dir" ]
  then
    should_start="\$null"
  else
    should_start="xtreemfs-dir"
  fi

  initd_file="${script_directory}/xtreemfs-${service}"
  cp "${script_directory}/${template_file}" "$initd_file"
  chmod a+x "$initd_file"

  sed -i -e "s|@SHORT_SERVICE_NAME@|${short_service_name_uppercase}|g" "$initd_file"
  sed -i -e "s|@SHORT_SERVICE_NAME_LOWERCASE@|${short_service_name_lowercase}|g" "$initd_file"
  sed -i -e "s|@LONG_SERVICE_NAME@|${long_service_name}|g" "$initd_file"
  sed -i -e "s|@SHOULD_START@|${should_start}|g" "$initd_file"
done