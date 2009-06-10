// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "multi_response_target.h"
using namespace org::xtreemfs::client;


MultiResponseTarget::MultiResponseTarget( size_t expected_response_count, YIELD::auto_Object<YIELD::EventTarget> final_response_target )
  : expected_response_count( expected_response_count ), final_response_target( final_response_target )
{ }

MultiResponseTarget::~MultiResponseTarget()
{
  for ( std::vector<YIELD::Event*>::iterator response_i = responses.begin(); response_i != responses.end(); response_i++ )
    YIELD::Object::decRef( **response_i );
}

void MultiResponseTarget::respond( YIELD::auto_Object<YIELD::ExceptionResponse> exception_response, YIELD::auto_Object<YIELD::EventTarget> final_response_target )
{
  final_response_target->send( *exception_response.release() );
}

void MultiResponseTarget::respond( std::vector<YIELD::Event*>& responses, YIELD::auto_Object<YIELD::EventTarget> final_response_target )
{
  final_response_target->send( responses[responses.size() - 1]->incRef() );
}

bool MultiResponseTarget::send( YIELD::Event& ev )
{
  responses_lock.acquire();

  responses.push_back( &ev );

  if ( responses.size() == expected_response_count )
  {
    for ( std::vector<YIELD::Event*>::iterator response_i = responses.begin(); response_i != responses.end(); response_i++ )
    {
      if ( ( *response_i )->get_tag() == YIELD_OBJECT_TAG( YIELD::ExceptionResponse ) )
      {
        respond( static_cast<YIELD::ExceptionResponse*>( *response_i )->incRef(), final_response_target );
        responses_lock.release();
        return true;
      }
    }

    respond( responses, final_response_target );
  }

  responses_lock.release();

  return true;
}
