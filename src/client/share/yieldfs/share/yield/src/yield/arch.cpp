// Revision: 1399

#include "yield/arch.h"
using namespace YIELD;


// event_handler.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).



void EventHandler::handleUnknownEvent( Event& ev )
{
  switch ( ev.get_type_id() )
  {
    case YIELD_OBJECT_TYPE_ID( StageStartupEvent ):
    case YIELD_OBJECT_TYPE_ID( StageShutdownEvent ): Object::decRef( ev ); break;

    default:
    {
      std::cerr << getEventHandlerName() << " dropping unknown event: " << ev.get_type_name() << std::endl;
      Object::decRef( ev );
    }
    break;
  }
}

bool EventHandler::send( Event& ev )
{
  if ( redirect_to_event_target )
    return redirect_to_event_target->send( ev );
  else if ( isThreadSafe() )
    handleEvent( ev );
  else
  {
    handleEvent_lock.acquire();
    handleEvent( ev );
    handleEvent_lock.release();
  }

  return true;
}



// seda_stage_group.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).



namespace YIELD
{
  class SEDAStageGroupThread : public StageGroupThread
  {
  public:
    SEDAStageGroupThread( const std::string& stage_group_name, auto_Object<ProcessorSet> limit_logical_processor_set, auto_Object<Log> log, Stage& stage )
      : StageGroupThread( stage_group_name, limit_logical_processor_set, log ), stage( stage )
    { }

    Stage& get_stage() { return stage; }

    // Thread
    void run()
    {
      StageGroupThread::before_run( stage.get_stage_name() );

      stage.get_event_handler().handleEvent( *( new StageStartupEvent( stage ) ) );

      while ( shouldRun() )
        visitStage( stage );
    }

  private:
    ~SEDAStageGroupThread() { }

    Stage& stage;
  };
};


SEDAStageGroup::~SEDAStageGroup()
{
  auto_Object<StageShutdownEvent> stage_shutdown_event = new StageShutdownEvent;
  for ( std::vector<SEDAStageGroupThread*>::iterator thread_i = threads.begin(); thread_i != threads.end(); thread_i++ )
  {
    ( *thread_i )->stop();
    for ( ;; )
    {
      // Keep sending thread_i StageShutdownEvents until it stops
      // thread_i may not actually be the thread that processes the event if there are multiple threads dequeueing, which is why we have to keep trying until thread_i stops.
      ( *thread_i )->get_stage().send( stage_shutdown_event->incRef() );
      if ( ( *thread_i )->is_running() )
        Thread::sleep( 50 * NS_IN_MS );
      else
        break;
    }
  }

  for ( std::vector<SEDAStageGroupThread*>::iterator thread_i = threads.begin(); thread_i != threads.end(); thread_i++ )
    Object::decRef( **thread_i );
}

auto_Object<Stage> SEDAStageGroup::createStage( auto_Object<EventHandler> event_handler, int16_t threads, auto_Object<EventQueue> event_queue, EventTarget* stage_stats_event_target, auto_Object<Log> log )
{
  if ( event_queue == NULL )
    return createStage<EventHandler>( event_handler, threads, stage_stats_event_target, log );
  else
    return createStage<EventHandler, EventQueue>( event_handler, threads, event_queue, stage_stats_event_target, log );
}

void SEDAStageGroup::startThreads( auto_Object<Stage> stage, int16_t threads )
{
  for ( unsigned short thread_i = 0; thread_i < threads; thread_i++ )
  {
    SEDAStageGroupThread* thread = new SEDAStageGroupThread( get_name(), NULL, get_log(), *stage );
    thread->start();
    this->threads.push_back( thread );
  }
}


// stage_group.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).


#ifdef YIELD_ARCH_RECORD_PERFCTRS
#ifdef __sun
#include <cstdlib>
#else
#error
#endif
#endif


StageGroup::StageGroup( const std::string& name, auto_Object<ProcessorSet> limit_physical_processor_set, EventTarget* stage_stats_event_target, auto_Object<Log> log )
: name( name ), limit_physical_processor_set( limit_physical_processor_set ), stage_stats_event_target( stage_stats_event_target ), log( log )
{
  if ( limit_physical_processor_set != NULL )
  {
    limit_logical_processor_set = new ProcessorSet;
    uint16_t online_physical_processor_count = Machine::getOnlinePhysicalProcessorCount();
    uint16_t logical_processors_per_physical_processor = Machine::getOnlinePhysicalProcessorCount();

    for ( uint16_t physical_processor_i = 0; physical_processor_i < online_physical_processor_count; physical_processor_i++ )
    {
      if ( limit_physical_processor_set->isset( physical_processor_i ) )
      {
        for ( uint16_t logical_processor_i = physical_processor_i * logical_processors_per_physical_processor; logical_processor_i < ( physical_processor_i + 1 ) * logical_processors_per_physical_processor; logical_processor_i++ )
          limit_logical_processor_set->set( logical_processor_i );
      }
    }
  }

  running_stage_group_thread_tls_key = Thread::createTLSKey();

  memset( stages, 0, sizeof( stages ) );
}

StageGroup::~StageGroup()
{
  for ( uint8_t stage_i = 0; stage_i < YIELD_ARCH_STAGES_PER_GROUP_MAX; stage_i++ )
    Object::decRef( stages[stage_i] );
}


// stage_group_thread.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).


#ifdef YIELD_ARCH_RECORD_PERFCTRS
#ifdef __sun
#include <cstdlib>
#else
#error
#endif
#endif


StageGroupThread::StageGroupThread( const std::string& stage_group_name, auto_Object<ProcessorSet> limit_logical_processor_set, auto_Object<Log> log )
  : stage_group_name( stage_group_name ), limit_logical_processor_set( limit_logical_processor_set ), log( log )
{
  should_run = true;

#ifdef YIELD_ARCH_RECORD_PERFCTRS
#ifdef __sun
  if ( ( cpc = cpc_open( CPC_VER_CURRENT ) ) != NULL &&
         ( cpc_set = cpc_set_create( cpc ) ) != NULL )
  {
    const char* pic0_str = getenv( "PIC0" );
    if ( pic0_str == NULL ) pic0_str = "L2_imiss";
    const char* pic1_str = getenv( "PIC1" );
    if ( pic1_str == NULL ) pic1_str = "L2_dmiss_ld";

    if ( ( pic0_index = cpc_set_add_request( cpc, cpc_set, pic0_str, 0, CPC_COUNT_USER, 0, NULL ) ) != -1 &&
       ( pic1_index = cpc_set_add_request( cpc, cpc_set, pic1_str, 0, CPC_COUNT_USER, 0, NULL ) ) != -1 )
    {
      if ( ( before_cpc_buf = cpc_buf_create( cpc, cpc_set ) ) != NULL &&
         ( after_cpc_buf = cpc_buf_create( cpc, cpc_set ) ) != NULL &&
         ( diff_cpc_buf = cpc_buf_create( cpc, cpc_set ) ) != NULL )
      {
        if ( cpc_bind_curlwp( cpc, cpc_set, 0 ) != -1 )
        {
          cpc_unbind( cpc, cpc_set );
          return;
        }
      }
    }
  }

  if ( cpc != NULL )
    cpc_close( cpc );

  throw Exception();
#endif
#endif
}

StageGroupThread::~StageGroupThread()
{
#ifdef YIELD_ARCH_RECORD_PERFCTRS
#ifdef __sun
  cpc_unbind( cpc, cpc_set );
  cpc_set_destroy( cpc, cpc_set );
  cpc_close( cpc );
#endif
#endif
}

void StageGroupThread::before_run( const char* thread_name )
{
  if ( thread_name == NULL )
    thread_name = stage_group_name.c_str();
  this->set_name( thread_name );

  if ( log != NULL )
    log->getStream( Log::LOG_DEBUG ) << stage_group_name << ": starting thread #" << this->get_id() << " (name = " << thread_name;

  if ( limit_logical_processor_set != NULL )
  {
    if ( !this->set_processor_affinity( *limit_logical_processor_set ) && log != NULL )
      log->getStream( Log::LOG_DEBUG ) << stage_group_name << "could not set processor affinity of thread #" << this->get_id() << ", error: " << Exception::strerror();
  }

//  Thread::setTLS( stage_group.get_running_stage_group_thread_tls_key(), this );

#ifdef YIELD_ARCH_RECORD_PERFCTRS
#ifdef __sun
  cpc_bind_curlwp( cpc, cpc_set, 0 );
#endif
#endif
}

