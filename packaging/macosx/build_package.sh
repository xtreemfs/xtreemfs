#!/bin/bash

MACOSX=$(dirname "$0")
MACOSX=$(cd "$MACOSX"; pwd)

VERSION=$1

if [ -z $VERSION ]; then
  echo "No version argument supplied."
  exit 1
fi

# mount is the first package and will contain the preinstall dependency
mkdir "$MACOSX/mount.xtreemfs"
cp "$MACOSX/../../cpp/build/mount.xtreemfs" "$MACOSX/mount.xtreemfs"
pkgbuild --root "$MACOSX/mount.xtreemfs" --identifier org.xtreemfs.MacOSXClient.mount.xtreemfs.pkg --version $VERSION --install-location /usr/local/bin --scripts "$MACOSX/scripts" \
  "$MACOSX/mount.xtreemfs.pkg"
rm -rf "$MACOSX/mount.xtreemfs"

# other /usr/local/bin components
for component in mkfs.xtreemfs rmfs.xtreemfs lsfs.xtreemfs xtfsutil
do
  mkdir "$MACOSX/$component"
  cp "$MACOSX/../../cpp/build/$component" "$MACOSX/$component"
  pkgbuild --root "$MACOSX/$component" --identifier org.xtreemfs.MacOSXClient.$component.pkg --version $VERSION --install-location /usr/local/bin \
    "$MACOSX/$component.pkg"
  rm -rf "$MACOSX/$component"
done

# uninstaller
mkdir "$MACOSX/uninstall_xtreemfs"
cp "$MACOSX/uninstall_xtreemfs.sh" "$MACOSX/uninstall_xtreemfs"
pkgbuild --root "$MACOSX/uninstall_xtreemfs" --identifier org.xtreemfs.MacOSXClient.uninstall_xtreemfs.pkg --version $VERSION --install-location /usr/local/bin \
  "$MACOSX/uninstall_xtreemfs.pkg"
rm -rf "$MACOSX/uninstall_xtreemfs"

# logo
mkdir "$MACOSX/xtreemfs_logo_transparent"
cp "$MACOSX/images/xtreemfs_logo_transparent.icns" "$MACOSX/xtreemfs_logo_transparent"
pkgbuild --root "$MACOSX/xtreemfs_logo_transparent" --identifier org.xtreemfs.MacOSXClient.xtreemfs_logo_transparent.pkg --version $VERSION --install-location /usr/local/share/xtreemfs \
  "$MACOSX/xtreemfs_logo_transparent.pkg"
rm -rf "$MACOSX/xtreemfs_logo_transparent"

# create distribution
productbuild --synthesize \
  --package "$MACOSX/mount.xtreemfs.pkg" \
  --package "$MACOSX/mkfs.xtreemfs.pkg" \
  --package "$MACOSX/rmfs.xtreemfs.pkg" \
  --package "$MACOSX/lsfs.xtreemfs.pkg" \
  --package "$MACOSX/xtfsutil.pkg" \
  --package "$MACOSX/uninstall_xtreemfs.pkg" \
  --package "$MACOSX/xtreemfs_logo_transparent.pkg" \
  --product "$MACOSX/requirements.plist" \
  "$MACOSX/distribution_synthesized.xml"

# add newline so our loop does not skip the last line, because productbuild does not add a newline at the end
echo "" >> "$MACOSX/distribution_synthesized.xml"

rm -f "$MACOSX/distribution.xml"

# add some more information to the distribution
IFS=''
while read line
do
  # check what tag we are reading
  echo $line | grep -q -E "<installer-gui-script"
  is_installer_gui_script_tag=$?
  echo $line | grep -q -E "<options"
  is_options_tag=$?

  if [ $is_installer_gui_script_tag -eq 0 ]; then
    echo "<installer-gui-script minSpecVersion=\"2\">"                                                                                                       >> "$MACOSX/distribution.xml"
  	echo "    <title>XtreemFS Client for MacOSX</title>"                                                                                                     >> "$MACOSX/distribution.xml"
  	echo "    <background file=\"xtreemfs_installer_background.png\" mime-type=\"image/png\" />"                                                             >> "$MACOSX/distribution.xml"
  	echo "    <readme file=\"en.lproj/README\" mime-type=\"text/plain\" />"                                                                                  >> "$MACOSX/distribution.xml"
  	echo "    <conclusion file=\"en.lproj/conclusion.rtf\" mime-type=\"text/rtf\" />"                                                                        >> "$MACOSX/distribution.xml"

  	# Mac OS X 10.11 and later have added System Integrity Protection, which prevents us from creating the /sbin/mount_xtreemfs link.
  	# Inform the user about this problem and let them decide.
  	echo "    <installation-check script=\"check_sip()\" />"                                                                                                 >> "$MACOSX/distribution.xml"
  	echo "    <script>"                                                                                                                                      >> "$MACOSX/distribution.xml"
  	echo "        function check_sip() {"                                                                                                                    >> "$MACOSX/distribution.xml"
  	echo "            if (system.compareVersions(system.version, '10.11') >= 0) {"                                                                           >> "$MACOSX/distribution.xml"
    echo "                exit_code = system.runOnce('/bin/sh', '-c', 'echo $(csrutil status) | grep -q -E \"enabled\"');"                                   >> "$MACOSX/distribution.xml"
    echo "                if (exit_code == 0) {"                                                                                                             >> "$MACOSX/distribution.xml"
    echo "                    my.result.title = 'SIP detected';"                                                                                             >> "$MACOSX/distribution.xml"
    echo "                    my.result.type = 'Warn';"                                                                                                      >> "$MACOSX/distribution.xml"
    echo "                    my.result.message = 'Enabled System Integrity Protection detected, will not be able to install /sbin/mount_xtreemfs link.' + " >> "$MACOSX/distribution.xml"
    echo "                        ' Boot into Recovery OS, run \"csrutil disable\", reboot and run the installer again if you want this link.' + "           >> "$MACOSX/distribution.xml"
    echo "                        ' After the installation you can \"csrutil enable\" SIP again.';"                                                          >> "$MACOSX/distribution.xml"
    echo "                } else {"                                                                                                                          >> "$MACOSX/distribution.xml"
    echo "                    return true;"                                                                                                                  >> "$MACOSX/distribution.xml"
    echo "                }"                                                                                                                                 >> "$MACOSX/distribution.xml"
  	echo "            } else {"                                                                                                                              >> "$MACOSX/distribution.xml"
  	echo "                return true;"                                                                                                                      >> "$MACOSX/distribution.xml"
  	echo "            }"                                                                                                                                     >> "$MACOSX/distribution.xml"
  	echo "        }"                                                                                                                                         >> "$MACOSX/distribution.xml"
  	echo "    </script>"                                                                                                                                     >> "$MACOSX/distribution.xml"
  elif [ $is_options_tag -eq 0 ]; then
  	# enable system calls in installation-check-script tag
  	echo "    <options customize=\"never\" require-scripts=\"true\" allow-external-scripts=\"true\"/>"                                                       >> "$MACOSX/distribution.xml"
  else
  	echo "$line"                                                                                                                                             >> "$MACOSX/distribution.xml"
  fi
done < "$MACOSX/distribution_synthesized.xml"

# build the final package
mkdir "$MACOSX/dist"
productbuild --distribution "$MACOSX/distribution.xml" --resources "$MACOSX/images" --resources "$MACOSX/resources" --package-path "$MACOSX" "$MACOSX/dist/XtreemFS_Client_${VERSION}_MacOSX_installer.pkg"

# create a mountable volume from the package
hdiutil create -volname "XtreemFS Client" -srcfolder "$MACOSX/dist" -ov "$MACOSX/XtreemFS_Client_${VERSION}_MacOSX_installer.pkg"
mv "$MACOSX/XtreemFS_Client_${VERSION}_MacOSX_installer.pkg.dmg" "$MACOSX/XtreemFS_Client_${VERSION}_MacOSX_installer.dmg"

# clean up
rm "$MACOSX/distribution_synthesized.xml"
rm "$MACOSX/distribution.xml"
rm -rf "$MACOSX/dist"
rm "$MACOSX"/*.pkg
