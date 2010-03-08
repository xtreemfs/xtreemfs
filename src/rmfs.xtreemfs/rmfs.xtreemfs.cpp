// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the XtreemFS project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the XtreemFS project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#include "xtreemfs/main.h"
using namespace xtreemfs;


namespace rmfs_xtreemfs
{
  class Main : public xtreemfs::Main
  {
  public:
    Main()
      : xtreemfs::Main
        (
          "rmfs.xtreemfs",
          "remove a volume",
          "[oncrpc://]<dir host>[:port]/<volume name>"
        )
    {
      addOption
      (
        RMFS_XTREEMFS_OPTION_PASSWORD,
        "--password",
        NULL,
        "password for volume"
      );
    }

  private:
    enum
    {
      RMFS_XTREEMFS_OPTION_PASSWORD = 20
    };

    yield::ipc::auto_URI dir_uri;
    std::string password;
    std::string volume_name;


    // yield::Main
    int _main( int, char** )
    {
      auto_DIRProxy dir_proxy = createDIRProxy( *dir_uri );

      yield::ipc::auto_URI mrc_uri
        = dir_proxy->getVolumeURIFromVolumeName( volume_name );

      auto_MRCProxy mrc_proxy = createMRCProxy( *mrc_uri, password.c_str() );

      mrc_proxy->xtreemfs_rmvol( volume_name );

      return 0;
    }

    void parseFiles( int files_count, char** files )
    {
      if ( files_count == 1 )
        dir_uri = parseVolumeURI( files[0], volume_name );
      else if ( files_count == 0 )
      {
        throw yield::platform::Exception
        (
          "must specify the DIR/volume URI"
        );
      }
      else
      {
        throw yield::platform::Exception
        (
          "extra parameters after the DIR/volume URI"
        );
      }
    }

    void parseOption( int id, char* arg )
    {
      if ( arg && id == RMFS_XTREEMFS_OPTION_PASSWORD )
        password = arg;
      else
        xtreemfs::Main::parseOption( id, arg );
    }
  };
};

int main( int argc, char** argv )
{
  return rmfs_xtreemfs::Main().main( argc, argv );
}
