// Copyright 2003-2008 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef YIELD_ARCH_H
#define YIELD_ARCH_H

#include "yield/platform.h"


#define YIELD_ARCH_STAGES_PER_GROUP_MAX 32

#ifdef __sun
#define YIELD_ARCH_RECORD_PERFCTRS 1
#include <libcpc.h>
#endif


namespace YIELD
{
  class Event : public Object
  {
  protected:
    Event() : enqueued_time_ns( 0 ) { }
    virtual ~Event() { }

    // Object
    Event& incRef() { return Object::incRef( *this ); }

  private:
    template <class, class, class> friend class StageImpl;
    uint64_t enqueued_time_ns;
  };


  class Response : public Event
  {
  public:
    // Object
    Response& incRef() { return Object::incRef( *this ); }

  protected:
    Response() { }
    virtual ~Response() { }
  };


  class ExceptionResponse : public Response, public Exception
  {
  public:
    ExceptionResponse() { }
    ExceptionResponse( uint32_t error_code ) : Exception( error_code ) { }
    ExceptionResponse( const char* what ) : Exception( what ) { }
    ExceptionResponse( const Exception& other ) : Exception( other ) { }
    ExceptionResponse( const ExceptionResponse& other ) : Exception( other ) { }
    virtual ~ExceptionResponse() throw() { }

    virtual ExceptionResponse* clone() const { return new ExceptionResponse( what() ); }
    virtual void throwStackClone() const { throw ExceptionResponse( what() ); }

    // Object
    YIELD_OBJECT_PROTOTYPES( ExceptionResponse, 639602091UL );
  };


  class Request : public Event
  {
  public:
    virtual uint32_t getDefaultResponseTypeId() const { return 0; }
    //virtual auto_Object<Response> createDefaultResponse() { return 0; }
    virtual Response& waitForDefaultResponse( uint64_t timeout_ns ) { throw new ExceptionResponse( "not implemented" ); }
    virtual bool respond( Response& response) { Object::decRef( response ); return true; }

    // For IDL-generated Requests
    virtual uint32_t getInterfaceNumber() const { return 0; }
    virtual uint32_t getOperationNumber() const { return 0; }

    // Object
    Request& incRef() { return Object::incRef( *this ); }

  protected:
    Request() { }
    virtual ~Request() { }
  };


  class EventTarget : public Object
  {
  public:
    virtual bool send( Event& ) = 0;

  protected:
    EventTarget() { }
    virtual ~EventTarget() { }
  };


  class EventHandler : public EventTarget
  {
  public:
    virtual bool isThreadSafe() const { return false; }
    virtual EventHandler* clone() const { return 0;  } // Some scheduling policies will try to replicate an EventHandler instead of sharing it (and possibly locking, if isThreadSafe() = false); EventHandlers that don't keep much state (such as e.g. caches) should implement this method

    virtual void handleEvent( Event& ) = 0;
    virtual void handleUnknownEvent( Event& );

    // EventTarget
    bool send( Event& );

    EventTarget* redirect_to_event_target;

  protected:
    EventHandler() : redirect_to_event_target( NULL ) { }
    virtual ~EventHandler() { }

  private:
    Mutex handleEvent_lock;
  };


  class EventQueue : public EventTarget
  {
  public:
    virtual EventQueue* clone() const = 0;

    virtual Event* dequeue() = 0; // Blocking
    virtual Event* dequeue( uint64_t timeout_ns ) = 0;

    template <class ExpectedEventType>
    ExpectedEventType& dequeue_typed()
    {
      Event* dequeued_ev = dequeue();
      if ( dequeued_ev )
        return _checkDequeuedEventAgainstExpectedEventType<ExpectedEventType>( *dequeued_ev );
      else
        throw Exception( "EventQueue::dequeue_typed: blocking dequeue was broken" );
    }

    template <class ExpectedEventType>
    ExpectedEventType& dequeue_typed( uint64_t timeout_ns )
    {
      Event* dequeued_ev = dequeue( timeout_ns );
      if ( dequeued_ev )
        return _checkDequeuedEventAgainstExpectedEventType<ExpectedEventType>( *dequeued_ev );
      else
        throw Exception( "EventQueue::dequeue_typed: timed out" );
    }

    virtual bool enqueue( Event& ev ) = 0;
    virtual Event* try_dequeue() = 0;

    // EventTarget
    bool send( Event& ev )
    {
      return enqueue( ev );
    }

  protected:
    EventQueue() { }
    virtual ~EventQueue() { }

  private:
    template <class ExpectedEventType>
    inline ExpectedEventType& _checkDequeuedEventAgainstExpectedEventType( Event& dequeued_ev )
    {
      switch ( dequeued_ev.get_type_id() )
      {
        case YIELD_OBJECT_TYPE_ID( ExpectedEventType ): return static_cast<ExpectedEventType&>( dequeued_ev );

        case YIELD_OBJECT_TYPE_ID( ExceptionResponse ):
        {
          try
          {
            static_cast<ExceptionResponse&>( dequeued_ev ).throwStackClone();
          }
          catch ( ExceptionResponse& )
          {
            Object::decRef( dequeued_ev );
            throw;
          }

          throw Exception( "should never reach this point" );
        }

       default: throw Exception( "EventQueue::deqeue_typed: received unexpected, non-exception event type" );
      }        
    }
  };


  template <class InternalQueueType = NonBlockingFiniteQueue<Event*, 2048> >
  class OneSignalEventQueue : public EventQueue, private InternalQueueType
  {
  public:
    ~OneSignalEventQueue() { }

    // Object
    YIELD_OBJECT_PROTOTYPES( OneSignalEventQueue, 1631491096UL );

    // EventQueue
    virtual EventQueue* clone() const { return new OneSignalEventQueue<InternalQueueType>; }

    Event* dequeue()
    {
      Event* result = InternalQueueType::try_dequeue();

      if ( result != 0 ) // Hot case
        return result; // Don't dec the semaphore, just let a cold acquire succeed on an empty queue
      else
      {
        do
        {
          empty.acquire();
          result = InternalQueueType::try_dequeue();
        }
        while ( result == 0 );

        return result;
      }
    }

    Event* dequeue( uint64_t timeout_ns )
    {
      Event* result = InternalQueueType::try_dequeue();

      if ( result != 0 )
        return result;
      else
      {
        for ( ;; )
        {

          uint64_t before_acquire_time_ns = Time::getCurrentUnixTimeNS();

          empty.timed_acquire( timeout_ns );

          if ( ( result = InternalQueueType::try_dequeue() ) != 0 )
            break;

          uint64_t waited_ns = Time::getCurrentUnixTimeNS() - before_acquire_time_ns;
          if ( waited_ns >= timeout_ns )
            break;
          else
            timeout_ns -= static_cast<uint64_t>( waited_ns );
        }

        return result;
      }
    }

    bool enqueue( Event& ev )
    {
      if ( InternalQueueType::enqueue( &ev ) )
      {
        empty.release();
        return true;
      }
      else
        return false;
    }

    inline Event* try_dequeue()
    {
      return InternalQueueType::try_dequeue();
    }

  protected:
    CountingSemaphore empty;
  };


  class Stage : public EventTarget
  {
  public:
    virtual const char* get_stage_name() const = 0;
    virtual EventHandler& get_event_handler() = 0;
    virtual EventQueue& get_event_queue() = 0;

    virtual double get_rho() const { return 0; }

    virtual bool visit() { return false; }; // Blocking visit, for SEDA
    virtual bool visit( uint64_t ) { return false; }; // Timed visit, for CohortS

    // Object
    YIELD_OBJECT_PROTOTYPES( Stage, 89781919UL );

  protected:
    Stage()
    {
#ifdef YIELD_ARCH_RECORD_PERFCTRS
#ifdef __sun
      last_recorded_cpc_events_time_ns = 0;
      pic0_total = pic1_total = 0;
#endif
#endif
    }

    virtual ~Stage() { }

  private:
#ifdef YIELD_ARCH_RECORD_PERFCTRS
#ifdef __sun
    friend class StageGroupThread;
    uint64_t pic0_total, pic1_total;
    uint64_t last_recorded_cpc_events_time_ns;
#endif
#endif
  };


  // A separate templated version for subclasses to inherit from (MG1Stage, CohortStage, etc.)
  // This helps eliminate some virtual function calls by having the specific EventHandler type instead of a generic EventHandler pointer
  template <class EventHandlerType, class EventQueueType, class LockType>
  class StageImpl : public Stage//, private StatsEventSource<LockType>
  {
  public:
    StageImpl( const std::string& stage_group_name, auto_Object<EventHandlerType> event_handler, auto_Object<EventQueueType> event_queue, EventTarget* stage_stats_event_target, auto_Object<Log> log )
      : Stage(), //StatsEventSource<LockType>( 2000, stage_stats_event_target ),
        event_handler( event_handler ), event_queue( event_queue ), log( log )
    {
      event_handler->redirect_to_event_target = this;
      event_queue_length = event_queue_arrival_count = 1; // send() would normally inc these, but we can't use send() because it's a virtual function; instead we enqueue directly and inc the lengths ourselves
      event_queue_full_count = 0;

      event_queue_length = event_queue_arrival_count = 0;
      rho = elapsed_ms = 0;
    }

    double get_rho() const { return rho; }

    // EventTarget
    bool send( Event& ev )
    {
      if ( log != NULL && log->get_level() >= Log::LOG_DEBUG )
      {
        std::ostringstream log_str;
        log_str << "StageImpl: thread #" << Thread::getCurrentThreadId() << " sending " << ev.get_type_name() << " to the " << get_stage_name() << " stage.";
        log->getStream( Log::LOG_DEBUG ) << log_str.str();
      }

      ++event_queue_length;
      ++event_queue_arrival_count;

      if ( event_queue->enqueue( ev ) )
        return true;
      else
      {
        if ( event_queue_full_count++ < 10 )
          std::cerr << Time() << ": queue full #" << static_cast<unsigned short>( event_queue_full_count ) << " at " << event_handler->get_type_name() << " stage." << std::endl;

        /*
        StageGroupThread* running_stage_group_thread = stage_group.get_running_stage_group_thread();

        for ( unsigned char lock_acquire_tries = 0; lock_acquire_tries < 15; lock_acquire_tries++ )
        {
          // Acquire the lock on the receiving stage
          if ( lock.timed_acquire( static_cast<uint64_t>( lock_acquire_tries * NS_IN_MS ) ) )
          {
            while ( running_stage_group_thread->shouldRun() )
            {
              // Keep trying to enqueue until it succeeds
              // Process an event from the head of the queue
              Event* head_ev = event_queue->try_dequeue();
              if ( head_ev )
              {
                --event_queue_length;
                _callEventHandler( *head_ev );
              }

              // Try to enqueue the send() event
              if ( event_queue->enqueue( ev ) )
              {
                lock.release();
                return true;
              }
            }
          }
        }
        */

        return false;
      }
    }

    // Stage
    const char* get_stage_name() const { return event_handler->get_type_name(); }
    EventHandler& get_event_handler() { return *event_handler; }
    EventQueue& get_event_queue() { return *event_queue; }

    bool visit()
    {
      if ( lock.try_acquire() )
      {
        Event* ev = event_queue->dequeue();
        if ( ev != NULL )
        {
          --event_queue_length;
          _callEventHandler( *ev );
          lock.release();
          return true;
        }
        else
        {
          lock.release();
          return false;
        }
      }
      else
        return false;
    }

    bool visit( uint64_t timeout_ns )
    {
      if ( lock.try_acquire() )
      {
        Event* ev = event_queue->dequeue( timeout_ns );
        if ( ev != NULL )
        {
          --event_queue_length;
          _callEventHandler( *ev );
          lock.release();
          return true;
        }
        else
        {
          lock.release();
          return false;
        }
      }
      else
        return false;
    }

  private:
    auto_Object<EventHandlerType> event_handler;
    auto_Object<EventQueueType> event_queue;
    auto_Object<Log> log;

    unsigned char event_queue_full_count;
    LockType lock;


    void _callEventHandler( Event& ev )
    {
      uint64_t event_queueing_time_ns = Time::getCurrentUnixTimeNS() - ev.enqueued_time_ns;
      if ( event_queueing_time_ns < 2 * NS_IN_S )
        event_queueing_time_sampler.setNextSample( static_cast<double>( event_queueing_time_ns * NS_IN_MS ) );

      if ( log != NULL && log->get_level() >= Log::LOG_DEBUG )
      {
        std::ostringstream log_str;
        log_str << "StageImpl: thread #" << Thread::getCurrentThreadId() << " processing " << ev.get_type_name() << " at the " << get_stage_name() << " stage.";
        log->getStream( Log::LOG_DEBUG ) << log_str.str();
      }

      double start_time_ms = Time::getCurrentUnixTimeMS();

      event_handler->handleEvent( ev );

      double event_processing_time_ms = Time::getCurrentUnixTimeMS() - start_time_ms;
      if ( event_processing_time_ms < 10 * MS_IN_S )
        event_processing_time_sampler.setNextSample( event_processing_time_ms );

      // StatsEventSource<LockType>::checkTimer();
    }


    Sampler<double, 2000, Mutex> event_queueing_time_sampler, event_processing_time_sampler;
    Sampler<uint32_t, 2000, Mutex> queue_length_sampler;
    Sampler<double, 2000, Mutex> arrival_rate_sampler;
    uint32_t event_queue_length, event_queue_arrival_count;
    double rho, elapsed_ms;

    /*
    // StatsEventSource
    StatsEvent* createStatsEvent( double elapsed_ms )
    {
      elapsed_ms += elapsed_ms;

      uint32_t event_queue_length = this->event_queue_length;
      queue_length_sampler.setNextSample( event_queue_length );
      double arrival_rate = static_cast<double>( event_queue_arrival_count ) / elapsed_ms;
      event_queue_arrival_count = 0;
      arrival_rate_sampler.setNextSample( arrival_rate );

      if ( elapsed_ms < 1000 )
        return NULL;
      else
      {
        StageStatsEvent* stage_stats_ev = new StageStatsEvent( get_event_handler().get_type_name() );

        stage_stats_ev->queue_length_samples_count = queue_length_sampler.getSamplesCount();
        stage_stats_ev->queue_length_statistics = queue_length_sampler.getStatistics();
        stage_stats_ev->event_queueing_time_statistics = event_queueing_time_sampler.getStatistics();
        stage_stats_ev->event_processing_time_statistics = event_processing_time_sampler.getStatistics();

        stage_stats_ev->arrival_rate_s = arrival_rate_sampler.getStatistics().ninetieth_percentile * 1000.0;
        stage_stats_ev->service_rate_s = 1000.0 / static_cast<uint64_t>( stage_stats_ev->event_processing_time_statistics.ninetieth_percentile );
        stage_stats_ev->rho = rho = ( stage_stats_ev->service_rate_s > 0 ) ? stage_stats_ev->arrival_rate_s / stage_stats_ev->service_rate_s : 0;

        return stage_stats_ev;
      }
    }
    */
  };


  class StageGroupThread;

  class StageGroup : public Object
  {
  public:
    template <class EventHandlerType> auto_Object<Stage> createStage( auto_Object<EventHandlerType> event_handler ) { return createStage( static_cast<EventHandler*>( event_handler.release() ), 1, NULL, NULL, NULL ); }
    template <class EventHandlerType> auto_Object<Stage> createStage( auto_Object<EventHandlerType> event_handler, int16_t threads ) { return createStage( static_cast<EventHandler*>( event_handler.release() ), threads, NULL, NULL, NULL ); }
    template <class EventHandlerType, class EventQueueType> auto_Object<Stage> createStage( auto_Object<EventHandlerType> event_handler, auto_Object<EventQueueType> event_queue ) { return createStage( static_cast<EventHandler*>( event_handler.release() ), 1, static_cast<EventQueue*>( event_queue.release() ), NULL, NULL ); }
    template <class EventHandlerType, class EventQueueType> auto_Object<Stage> createStage( auto_Object<EventHandlerType> event_handler, auto_Object<EventQueueType> event_queue, auto_Object<Log> log ) { return createStage( static_cast<EventHandler*>( event_handler.release() ), 1, static_cast<EventQueue*>( event_queue.release() ), NULL, log ); }
    template <class EventHandlerType, class EventQueueType> auto_Object<Stage> createStage( auto_Object<EventHandlerType> event_handler, int16_t threads, auto_Object<EventQueueType> event_queue ) { return createStage( static_cast<EventHandler*>( event_handler.release() ), threads, static_cast<EventQueue*>( event_queue.release() ), NULL, NULL ); }
    template <class EventHandlerType, class EventQueueType> auto_Object<Stage> createStage( auto_Object<EventHandlerType> event_handler, int16_t threads, auto_Object<EventQueueType> event_queue, EventTarget& stage_stats_event_target ) { return createStage( static_cast<EventHandler*>( event_handler.release() ), threads, static_cast<EventQueue*>( event_queue.release() ), &stage_stats_event_target, NULL ); }
    template <class EventHandlerType, class EventQueueType> auto_Object<Stage> createStage( auto_Object<EventHandlerType> event_handler, int16_t threads, auto_Object<EventQueueType> event_queue, EventTarget& stage_stats_event_target, auto_Object<Log> log ) { return createStage( static_cast<EventHandler*>( event_handler.release() ), threads, static_cast<EventQueue*>( event_queue.release() ), &stage_stats_event_target, log ); }

    inline auto_Object<ProcessorSet> get_limit_physical_processor_set() const { return limit_physical_processor_set; }
    inline auto_Object<ProcessorSet> get_limit_logical_processor_set() const { return limit_logical_processor_set; }
    unsigned long get_running_stage_group_thread_tls_key() const { return running_stage_group_thread_tls_key; }
    inline StageGroupThread* get_running_stage_group_thread() const { return static_cast<StageGroupThread*>( Thread::getTLS( running_stage_group_thread_tls_key ) ); }
    inline EventTarget* get_stage_stats_event_target() const { return stage_stats_event_target; }

  protected:
    StageGroup( const std::string& name, auto_Object<ProcessorSet> limit_physical_processor_set = NULL, EventTarget* stage_stats_event_target = NULL, auto_Object<Log> log = NULL );
    virtual ~StageGroup();

    void addStage( auto_Object<Stage> stage )
    {
      unsigned char stage_i;
      for ( stage_i = 0; stage_i < YIELD_ARCH_STAGES_PER_GROUP_MAX; stage_i++ )
      {
        if ( stages[stage_i] == NULL )
        {
          stages[stage_i] = stage.release();
          return;
        }
      }

      DebugBreak();
    }

    auto_Object<Log> get_log() const { return log; }
    const std::string& get_name() const { return name; }

    virtual auto_Object<Stage> createStage( auto_Object<EventHandler> event_handler, int16_t threads, auto_Object<EventQueue> event_queue, EventTarget* stage_stats_event_target, auto_Object<Log> log ) = 0;

  private:
    std::string name;
    auto_Object<ProcessorSet> limit_physical_processor_set, limit_logical_processor_set;
    EventTarget* stage_stats_event_target;
    auto_Object<Log> log;

    unsigned long running_stage_group_thread_tls_key;
    Stage* stages[YIELD_ARCH_STAGES_PER_GROUP_MAX];
  };


  class StageGroupThread : public Thread
  {
  public:
    virtual ~StageGroupThread();

    inline bool shouldRun() const { return should_run; }
    void stop() { should_run = false; }

  protected:
    StageGroupThread( const std::string& stage_group_name, auto_Object<ProcessorSet> limit_logical_processor_set = NULL, auto_Object<Log> = NULL );


    void before_run( const char* thread_name = NULL );

    inline bool visitStage( Stage& stage )
    {
#ifdef YIELD_ARCH_RECORD_PERFCTRS
      startPerformanceCounterSampling();
#endif

      bool success = stage.visit();

#ifdef YIELD_ARCH_RECORD_PERFCTRS
      stopPerformanceCounterSampling( stage, success );
#endif

      return success;
    }

    inline bool visitStage( Stage& stage, uint64_t timeout_ns )
    {
#ifdef YIELD_ARCH_RECORD_PERFCTRS
      startPerformanceCounterSampling();
#endif

      bool success = stage.visit( timeout_ns );

#ifdef YIELD_ARCH_RECORD_PERFCTRS
      stopPerformanceCounterSampling( stage, success );
#endif

      return success;
    }

  private:
    std::string stage_group_name;
    auto_Object<ProcessorSet> limit_logical_processor_set;
    auto_Object<Log> log;

    bool should_run;

#ifdef YIELD_ARCH_RECORD_PERFCTRS
#ifdef __sun
    cpc_t* cpc; cpc_set_t* cpc_set;
    int pic0_index, pic1_index;
    cpc_buf_t *before_cpc_buf, *after_cpc_buf, *diff_cpc_buf;

    inline void startPerformanceCounterSampling()
    {
      cpc_set_sample( cpc, cpc_set, before_cpc_buf );
    }

    inline void stopPerformanceCounterSampling( Stage& stage, bool visit_was_successful )
    {
      cpc_set_sample( cpc, cpc_set, after_cpc_buf );
      cpc_buf_sub( cpc, diff_cpc_buf, after_cpc_buf, before_cpc_buf );
      uint64_t pic0; cpc_buf_get( cpc, diff_cpc_buf, pic0_index, &pic0 );
      stage.pic0_total += pic0;
      uint64_t pic1; cpc_buf_get( cpc, diff_cpc_buf, pic1_index, &pic1 );
      stage.pic1_total += pic1;

      if ( visit_was_successful )
      {
        uint64_t current_time_ns = Time::getCurrentUnixTimeNS();
        if ( stage.last_recorded_cpc_events_time_ns - current_time_ns >= 5 * NS_IN_S )
        {
          stage.last_recorded_cpc_events_time_ns = current_time_ns;
          std::ostringstream cout_str;
          cout_str << stage.get_stage_name() << " performance counter totals: " << stage.pic0_total << "/" << stage.pic1_total << std::endl;
          std::cout << cout_str.str();
        }
      }
    }
#endif
#endif
  };


  class StageStartupEvent : public Event
  {
  public:
    StageStartupEvent( Stage& stage ) : stage( stage )
    { }

    Stage& get_stage() { return stage; }

    // Object
    YIELD_OBJECT_PROTOTYPES( StageStartupEvent, 3496483221UL );

  private:
    Stage& stage;
  };


  class StageShutdownEvent : public Event
  {
  public:
    // Object
    YIELD_OBJECT_PROTOTYPES( StageShutdownEvent, 753093765UL );
  };


  class SEDAStageGroupThread;


  class SEDAStageGroup : public StageGroup
  {
  public:
    SEDAStageGroup( const char* name, ProcessorSet* limit_physical_processor_set = NULL, EventTarget* stage_stats_event_target = NULL, auto_Object<Log> log = NULL )
        : StageGroup( name, limit_physical_processor_set, stage_stats_event_target, log )
    { }

    // Templated createStage's that pass the real EventHandler and EventQueue types to StageImpl to bypass the interfaces
    template <class EventHandlerType>
    auto_Object<Stage> createStage( auto_Object<EventHandlerType> event_handler, int16_t threads, EventTarget* stage_stats_event_target, auto_Object<Log> log )
    {
      return createStage< EventHandlerType, OneSignalEventQueue<> >( event_handler, threads, new OneSignalEventQueue<>, stage_stats_event_target, log );
    }

    template <class EventHandlerType, class EventQueueType>
    auto_Object<Stage> createStage( auto_Object<EventHandlerType> event_handler, int16_t threads, auto_Object<EventQueueType> event_queue, EventTarget* stage_stats_event_target, auto_Object<Log> log )
    {
      if ( threads == -1 )
      {
        if ( get_limit_physical_processor_set() != NULL )
          threads = get_limit_physical_processor_set()->count();
        else
          threads = Machine::getOnlinePhysicalProcessorCount();
      }

      if ( stage_stats_event_target == NULL )
        stage_stats_event_target = this->get_stage_stats_event_target();

      if ( log == NULL )
        log = this->get_log();

      auto_Object<Stage> stage;
      if ( event_handler->isThreadSafe() )
        stage = new StageImpl<EventHandlerType, EventQueueType, NOPLock>( get_name(), event_handler, event_queue, stage_stats_event_target, log );
      else
        stage = new StageImpl<EventHandlerType, EventQueueType, Mutex>( get_name(), event_handler, event_queue, stage_stats_event_target, log );

      this->addStage( stage );
      startThreads( stage, threads );

      return stage;
    }

    // Object
    YIELD_OBJECT_PROTOTYPES( SEDAStageGroup, 1176649847UL );

    // StageGroup
    auto_Object<Stage> createStage( auto_Object<EventHandler> event_handler, int16_t threads, auto_Object<EventQueue> event_queue, EventTarget* stage_stats_event_target, auto_Object<Log> log );

  protected:
    virtual ~SEDAStageGroup();

  private:
    void startThreads( auto_Object<Stage> stage, int16_t threads );

    std::vector<SEDAStageGroupThread*> threads;
  };
};

#endif
