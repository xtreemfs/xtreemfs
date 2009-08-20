// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTFS3_BUCKET_H_
#define _XTFS3_BUCKET_H_

#include "xtreemfs.h"


namespace xtfs3
{
  class Bucket : public yidl::Object
  {
  public:
    Bucket( const std::string& name, YIELD::auto_Volume volume );

    void delete_();
    yidl::auto_Buffer get();
    void put();

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( Bucket, 0 );

  private:
    std::string name;
    YIELD::auto_Volume volume;
  };

  typedef yidl::auto_Object<Bucket> auto_Bucket;
};

#endif
