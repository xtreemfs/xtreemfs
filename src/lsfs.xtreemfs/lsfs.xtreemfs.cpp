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

#include "xtreemfs.h"
using namespace org::xtreemfs::interfaces;
using namespace xtreemfs;


int main( int argc, char** argv )
{
  if ( argc == 1 )
  {
    cout << "lsfs.xtreemfs: list volumes or examine one volume" << endl;
    cout << "Usage: lsfs.xtreemfs <options>" <<
            " [oncrpc://]<dir host>[:port][/volume_name]" << endl;
    cout << Options::usage();
    return 0;
  }

  try
  {
    Options options = Options::parse( argc, argv );

    DIRProxy& dir_proxy = DIRProxy::create( options );

    ServiceSet services;
    dir_proxy.xtreemfs_service_get_by_type( SERVICE_TYPE_VOLUME, services );

    const string& volume_name = options.get_uri()->get_resource();
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
           cout << "Volume '" << volume_name << "'" << endl;
           size_t volume_name_len = volume_name.size() + 9;
           for ( size_t dash_i = 0; dash_i < volume_name_len; dash_i++ )
             cout << '-';
           cout << endl;

           cout << "name: " << ( *service_i ).get_name() << endl;
           cout << "uuid: " << ( *service_i ).get_uuid() << endl;

           for
           (
             ServiceDataMap::const_iterator data_i
               = ( *service_i ).get_data().begin();
             data_i != ( *service_i ).get_data().end();
             ++data_i
           )
             cout << ( *data_i ).first << ": " << ( *data_i ).second << endl;
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
        cout << ( *service_i ).get_name() << " -> " <<
                ( *service_i ).get_uuid() << endl;
      }
    }

    return 0;
  }
  catch ( Exception& exception )
  {
    cerr << "lsfs.xtreemfs: error: " << exception.what() << endl;
    return exception.get_error_code();      
  }    
}
