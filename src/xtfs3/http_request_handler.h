// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTFS3_HTTP_REQUEST_HANDLER_H_
#define _XTFS3_HTTP_REQUEST_HANDLER_H_

#include "xtreemfs.h"


namespace xtfs3
{
  class HTTPRequestHandler : public YIELD::EventHandler
  {
  public:
    HTTPRequestHandler( const std::string& virtual_host_name, YIELD::auto_Volume volume );

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( HTTPRequestHandler, 0 );

    // YIELD::EventHandler
    void handleEvent( YIELD::Event& );

  private:
    std::string virtual_host_name;
    YIELD::auto_Volume volume;

    void handleHTTPRequest( YIELD::HTTPRequest& );
  };

  typedef yidl::auto_Object<HTTPRequestHandler> auto_HTTPRequestHandler;
};

#endif
