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
using namespace xtreemfs;

#include "yidl.h"
using yidl::runtime::auto_Object;


int main( int argc, char** argv )
{
  OptionParser::Options rmfs_options;
  rmfs_options.add( "--password", "MRC administrator password" );

  if ( argc == 1 )
  {
    cout << "rmfs.xtreemfs: remove a volume" << endl;
    cout << "Usage: rmfs.xtreemfs <options>" <<
            " oncrpc://]<dir host>[:port][/volume_name]" << endl;
    cout << Options::usage( rmfs_options );
    return 0;
  }

  try
  {
    string mrc_password;

    Options options = Options::parse( argc, argv, rmfs_options );

    for 
    (
      Options::const_iterator parsed_option_i = options.begin();
      parsed_option_i != options.end();
      ++parsed_option_i
    )
    {
      if ( *parsed_option_i == "--password" )
        mrc_password = ( parsed_option_i )->get_argument();
    }

    if ( options.get_uri() == NULL || options.get_uri()->get_resource() == "/" )
      throw Exception( "must specify the <DIR>/<volume name> URI" );
    const std::string& volume_name = options.get_uri()->get_resource();

    auto_Object<DIRProxy> dir_proxy = DIRProxy::create( options );
    URI mrc_uri = dir_proxy->getVolumeURIFromVolumeName( volume_name );
    auto_Object<MRCProxy> mrc_proxy 
      = MRCProxy::create( mrc_uri, options, mrc_password );
    mrc_proxy->xtreemfs_rmvol( volume_name );

    return 0;
  }
  catch ( Exception& exception )
  {
    cerr << "rmfs.xtreemfs: error: " << exception.what() << endl;
    return exception.get_error_code();      
  }
}
