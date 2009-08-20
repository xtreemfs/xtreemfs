// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTFS3_OBJECT_H_
#define _XTFS3_OBJECT_H_

#include "xtreemfs.h"


namespace xtfs3
{
  class Bucket;


  class Object : public yidl::Struct
  {
  public:
    Object( yidl::auto_Object<Bucket> bucket, const std::string& key, YIELD::auto_Volume volume );

    void delete_();
    yidl::auto_Buffer get();
    void put( yidl::auto_Buffer http_request_body );

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( xtfs3::Object, 0 );
    void marshal( yidl::Marshaller& marshaller ) const;

  private:
    yidl::auto_Object<Bucket> bucket;
    std::string key;
    YIELD::Path path;
    YIELD::auto_Volume volume;
  };

  typedef yidl::auto_Object<Object> auto_Object;
};

#endif
