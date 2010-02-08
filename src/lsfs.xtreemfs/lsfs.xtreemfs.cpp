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
using namespace org::xtreemfs::interfaces;

#include <iostream>


namespace lsfs_xtreemfs
{
  class Main : public xtreemfs::Main
  {
  public:
    Main()
      : xtreemfs::Main
        (
          "lsfs.xtreemfs",
          "list volumes or detailed information on one volume",
          "[oncrpc://]<dir host>[:port][/volume_name]"
        )
    { }

  private:
    YIELD::ipc::auto_URI dir_uri;
    std::string volume_name;


    // YIELD::Main
    int _main( int, char** )
    {
      ServiceSet services;
      createDIRProxy( *dir_uri )->xtreemfs_service_get_by_type
      (
        SERVICE_TYPE_VOLUME,
        services
      );

      if ( !volume_name.empty() ) // Print detailed info on one volume
      {
        for
        (
          ServiceSet::const_iterator service_i= services.begin();
          service_i != services.end();
          ++service_i
        )
        {
           if ( ( *service_i ).get_name() == volume_name )
           {
             std::cout << "Volume '" << volume_name << "'" << std::endl;
             size_t volume_name_len = volume_name.size() + 9;
             for ( size_t dash_i = 0; dash_i < volume_name_len; dash_i++ )
               std::cout << '-';
             std::cout << std::endl;

             std::cout << "name: " << ( *service_i ).get_name() << std::endl;
             std::cout << "uuid: " << ( *service_i ).get_uuid() << std::endl;

             for
             (
               ServiceDataMap::const_iterator data_i
                 = ( *service_i ).get_data().begin();
               data_i != ( *service_i ).get_data().end();
               ++data_i
             )
             {
               std::cout << ( *data_i ).first << ": " <<
                            ( *data_i ).second << std::endl;
             }
           }
        }
      }
      else // List all volumes
      {
        for
        (
          ServiceSet::const_iterator service_i = services.begin();
          service_i != services.end();
          ++service_i
        )
        {
          std::cout << ( *service_i ).get_name() << " -> " <<
                       ( *service_i ).get_uuid() <<
                       std::endl;
        }
      }

      return 0;
    }

    void parseFiles( int files_count, char** files )
    {
      if ( files_count == 1 )
      {
        dir_uri = parseURI( files[0] );
        if ( dir_uri->get_resource().size() > 1 )
          volume_name = dir_uri->get_resource().c_str() + 1;
      }
      else if ( files_count == 0 )
        throw YIELD::platform::Exception( "must specify a DIR[/volume] URI" );
      else
      {
        throw YIELD::platform::Exception
        (
          "extra parameters after the DIR[/volume] URI" 
        );
      }
    }
  };
};

int main( int argc, char** argv )
{
  return lsfs_xtreemfs::Main().main( argc, argv );
}
