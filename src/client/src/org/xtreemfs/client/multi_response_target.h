// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _CLIENT_SRC_ORG_XTREEMFS_CLIENT_MULTI_RESPONSE_TARGET_H_
#define _CLIENT_SRC_ORG_XTREEMFS_CLIENT_MULTI_RESPONSE_TARGET_H_

#include "yield.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class MultiResponseTarget : public YIELD::EventTarget
      {
      public:
        MultiResponseTarget( size_t expected_response_count, YIELD::auto_Object<YIELD::EventTarget> final_response_target );
        virtual ~MultiResponseTarget();

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( MultiResponseTarget, 0 );

        // YIELD::EventTarget
        bool send( YIELD::Event& );

      protected:
        virtual void respond( YIELD::auto_Object<YIELD::ExceptionResponse> exception_response, YIELD::auto_Object<YIELD::EventTarget> final_response_target );
        virtual void respond( std::vector<YIELD::Event*>& responses, YIELD::auto_Object<YIELD::EventTarget> final_response_target ); 

      private:
        size_t expected_response_count;
        YIELD::auto_Object<YIELD::EventTarget> final_response_target;

        std::vector<YIELD::Event*> responses;
        YIELD::Mutex responses_lock;
      };
    };
  };
};

#endif
