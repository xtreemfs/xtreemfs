/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#define WIN32_LEAN_AND_MEAN
#include <windows.h>

#include <boost/scoped_ptr.hpp>
#include <CbFS.h>
#include <vector>

#include "cbfs/cbfs_adapter.h"
#include "cbfs/cbfs_options.h"
#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "pbrpc/RPC.pb.h"  // xtreemfs::pbrpc::UserCredentials
#include "util/logging.h"
#include "xtreemfs/MRC.pb.h"  // xtreemfs::pbrpc::Stat

#ifdef _MSC_VER
// Disable "warning C4996: 'strdup': The POSIX name for this item is deprecated. Instead, use the ISO C++ conformant name: _strdup.  // NOLINT
#pragma warning(push)
#pragma warning(disable:4996)
#endif  // _MSC_VER

using namespace std;
using namespace xtreemfs;
using namespace xtreemfs::util;

int __cdecl wmain(ULONG argc, PWCHAR argv[]) {
  vector<char*> argv_utf8;
  argv_utf8.reserve(argc);
  for (ULONG i = 0; i < argc; i++) {
    argv_utf8.push_back(strdup(ConvertWindowsToUTF8(argv[i]).c_str()));
  }
  
  CbFSOptions cbfs_options;
  bool invalid_commandline_parameters = false;
  try {
    cbfs_options.ParseCommandLine(argv_utf8.size(), &argv_utf8[0]);
  } catch(const xtreemfs::XtreemFSException& e) {
    cout << "Invalid parameters found, error: " << e.what() << endl << endl;
    invalid_commandline_parameters = true;
  }
  for (size_t i = 0; i < argv_utf8.size(); i++) {
    delete[] argv_utf8[i];
  }

  // Display help if needed.
  if (cbfs_options.empty_arguments_list || invalid_commandline_parameters) {
    cout << cbfs_options.ShowCommandLineUsage() << endl;
    return 1;
  }
  if (cbfs_options.show_help) {
    cout << cbfs_options.ShowCommandLineHelp() << endl;
    return 1;
  }
  // Show only the version.
  if (cbfs_options.show_version) {
    cout << cbfs_options.ShowVersion("mount.xtreemfs") << endl;
    return 1;
  }

  boost::scoped_ptr<CbFSAdapter> cbfs_adapter(new CbFSAdapter(&cbfs_options));

  try {
    cbfs_adapter->Start();
    cout << "Volume successfully mounted. Eject it in Windows to un-mount it." << endl;
  
    cbfs_adapter->WaitForEjection();

    cbfs_adapter->StopWithoutUnmountAndWithoutDelete();
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG) << "Did shutdown the XtreemFS client." << endl;
    }
  } catch (const XtreemFSException& e) {
    Logging::log->getLog(LEVEL_ERROR)
      << "Failed to mount the volume. Error: " << e.what() << endl;
  }

  // libxtreemfs shuts down logger.

  return 0;
}
