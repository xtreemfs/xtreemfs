// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _YIELD_CONCURRENCY_H_
#define _YIELD_CONCURRENCY_H_

#include "yield/platform.h"

#include <map>


#define YIELD_STAGES_PER_GROUP_MAX 32
// YIELD_MG1_POLLING_TABLE_SIZE should be a Fibonnaci number: 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584
#define YIELD_MG1_POLLING_TABLE_SIZE 144


namespace YIELD
{
  class ExceptionResponse;
  class Request;
  class Response;


  class Event : public Struct
  {
  public:
    // Object
    Event& incRef() { return Object::incRef( *this ); }

  protected:
    virtual ~Event() { }
  };

  typedef auto_Object<Event> auto_Event;


  class EventTarget : public Object
  {
  public:
    virtual void send( Event& ) = 0;

    // Object
    EventTarget& incRef() { return Object::incRef( *this ); }

  protected:
    EventTarget() { }
    virtual ~EventTarget() { }
  };

  typedef auto_Object<EventTarget> auto_EventTarget;


  class EventHandler : public EventTarget
  {
  public:
    virtual bool isThreadSafe() const { return false; }

    virtual void handleEvent( Event& ) = 0;
    virtual void handleUnknownEvent( Event& );

    // Object
    EventHandler& incRef() { return Object::incRef( *this ); }

    // EventTarget
    void send( Event& );

  protected:
    EventHandler() { }
    virtual ~EventHandler() { }

  private:
    Mutex handleEvent_lock;
  };

  typedef auto_Object<EventHandler> auto_EventHandler;


  class EventQueue : public EventTarget
  {
  public:
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
    virtual Event* try_dequeue() { return dequeue( 0 ); }

    // EventTarget
    void send( Event& ev )
    {
#ifdef _DEBUG
      if ( !enqueue( ev ) ) 
        DebugBreak();
#else
      enqueue( ev );
#endif
    }

  protected:
    EventQueue() { }
    virtual ~EventQueue() { }

  private:
    template <class ExpectedEventType>
    ExpectedEventType& _checkDequeuedEventAgainstExpectedEventType( Event& dequeued_ev );
  };

  typedef auto_Object<EventQueue> auto_EventQueue;


  class OneSignalEventQueue : public EventQueue, private STLQueue<Event*>
  {
  public:
    // Object
    YIELD_OBJECT_PROTOTYPES( OneSignalEventQueue, 101 );

    // EventQueue
    Event* dequeue();
    Event* dequeue( uint64_t timeout_ns );
    bool enqueue( Event& ev );
    Event* try_dequeue();

  private:
    ~OneSignalEventQueue() { }

    CountingSemaphore empty;
  };

  typedef auto_Object<OneSignalEventQueue> auto_OneSignalEventQueue;


  class Interface : public EventHandler
  {
  public:
    virtual Request* checkRequest( Object& request ) = 0; // Casts an Object to a Request if the request belongs to the interface
    virtual Response* checkResponse( Object& response ) = 0; // Casts an Object to a Response if the request belongs to the interface
    virtual auto_Object<Request> createRequest( uint32_t tag ) = 0;
    virtual auto_Object<Response> createResponse( uint32_t tag ) = 0;
    virtual auto_Object<ExceptionResponse> createExceptionResponse( uint32_t tag ) = 0;
  };

  typedef auto_Object<Interface> auto_Interface;


  class Response : public Event
  {
  public:
    // Object
    Response& incRef() { return Object::incRef( *this ); }

  protected:
    Response() { }
    virtual ~Response() { }
  };

  typedef auto_Object<Response> auto_Response;


  class Request : public Event
  {
  public:
    virtual auto_Response createResponse() = 0;

    auto_EventTarget get_response_target() const 
    { 
      return response_target; 
    }

    virtual void respond( Response& response )
    {
      response_target->send( response );
    }

    void set_response_target( auto_EventTarget response_target ) 
    { 
      this->response_target = response_target; 
    }

    // Object
    Request& incRef() { return Object::incRef( *this ); }

  protected:
    Request() { }
    virtual ~Request() { }

  private:
    auto_EventTarget response_target;
  };

  typedef auto_Object<Request> auto_Request;


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
    YIELD_OBJECT_PROTOTYPES( ExceptionResponse, 102 );
  };

  typedef auto_Object<ExceptionResponse> auto_ExceptionResponse;


  template <class ExpectedEventType>
  ExpectedEventType& EventQueue::_checkDequeuedEventAgainstExpectedEventType( Event& dequeued_ev )
  {
    switch ( dequeued_ev.get_tag() )
    {
      case YIELD_OBJECT_TAG( ExpectedEventType ): return static_cast<ExpectedEventType&>( dequeued_ev );

      case YIELD_OBJECT_TAG( ExceptionResponse ):
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


  class Stage : public EventTarget
  {
  public:
    class StartupEvent : public Event
    {
    public:
      StartupEvent( auto_Object<Stage> stage )
        : stage( stage )
      { }

      auto_Object<Stage> get_stage() { return stage; }

      // Object
      YIELD_OBJECT_PROTOTYPES( Stage::StartupEvent, 104 );

    private:
      auto_Object<Stage> stage;
    };


    class ShutdownEvent : public Event
    {
    public:
      // Object
      YIELD_OBJECT_PROTOTYPES( Stage::ShutdownEvent, 105 );
    };


    virtual double get_rho() const { return 0; }
    virtual const char* get_stage_name() const = 0;
    virtual auto_Object<EventHandler> get_event_handler() = 0;
    virtual auto_Object<EventQueue> get_event_queue() = 0;

    virtual bool visit() { return false; }; // Blocking visit
    virtual bool visit( uint64_t ) { return false; }; // Timed visit

    // Object
    YIELD_OBJECT_PROTOTYPES( Stage, 103 );

  protected:
    virtual ~Stage() { }
  };

  typedef auto_Object<Stage> auto_Stage;


  // A separate templated version for subclasses to inherit from (MG1Stage, CohortStage, etc.)
  // This helps eliminate some virtual function calls by having the specific EventHandler type instead of a generic EventHandler pointer
  template <class EventHandlerType, class EventQueueType, class LockType>
  class TemplatedStageImpl : public Stage
  {
  public:
    TemplatedStageImpl( auto_Object<EventHandlerType> event_handler, auto_Object<EventQueueType> event_queue, unsigned long running_stage_tls_key )
      : event_handler( event_handler ), event_queue( event_queue ), running_stage_tls_key( running_stage_tls_key )
    { }

    // EventTarget
    void send( Event& ev )
    {
      if ( event_queue->enqueue( ev ) )
        return;
      else
        DebugBreak();
    }

    // Stage
    const char* get_stage_name() const { return event_handler->get_type_name(); }
    auto_Object<EventHandler> get_event_handler() { return event_handler->incRef(); }
    auto_Object<EventQueue> get_event_queue() { return static_cast<EventQueue&>( event_queue->incRef() ); }

    bool visit()
    {
      if ( lock.try_acquire() )
      {
        Thread::setTLS( running_stage_tls_key, this );

        Event* ev = event_queue->dequeue();
        if ( ev != NULL )
        {
          event_handler->handleEvent( *ev );
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
        Thread::setTLS( running_stage_tls_key, this );

        Event* ev = event_queue->dequeue( timeout_ns );
        if ( ev != NULL )
        {
          event_handler->handleEvent( *ev );
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
    unsigned long running_stage_tls_key;

    LockType lock;
  };


  template <class LockType>
  class InstrumentedStageImpl : public Stage
  {
  public:
    InstrumentedStageImpl( auto_Object<EventHandler> event_handler, auto_Object<EventQueue> event_queue, unsigned long running_stage_tls_key );

    // EventTarget
    void send( Event& ev );

    // Stage
    const char* get_stage_name() const { return event_handler->get_type_name(); }
    auto_Object<EventHandler> get_event_handler() { return event_handler; }
    auto_Object<EventQueue> get_event_queue() { return event_queue; }
    bool visit();
    bool visit( uint64_t timeout_ns );

  private:
    auto_Object<EventHandler> event_handler;
    auto_Object<EventQueue> event_queue;
    unsigned long running_stage_tls_key;

    LockType lock;

    // Statistics
    static TimerQueue statistics_timer_queue;
    uint32_t event_queue_length, event_queue_arrival_count;
    Sampler<uint64_t, 2000, Mutex> event_processing_time_sampler;
    std::map<const char*, uint64_t> send_counters; Mutex send_counters_lock;


    class StatisticsTimer : public TimerQueue::Timer
    {
    public:
      StatisticsTimer( auto_Object<Stage> stage );

      // Timer
      bool fire( const Time& elapsed_time );

    private:
      auto_Object<Stage> stage;
    };


    // StageImpl
    void _callEventHandler( Event& );
  };


  class StageGroup : public Object
  {
  public:
    template <class EventHandlerType>
    auto_Stage createStage( auto_Object<EventHandlerType> event_handler )
    {
      return createStage( static_cast<EventHandler*>( event_handler.release() ) );
    }

    template <class EventHandlerType, class EventQueueType>
    auto_Stage createStage( auto_Object<EventHandlerType> event_handler, auto_Object<EventQueueType> event_queue )
    {
      return createStage( static_cast<EventHandler*>( event_handler.release() ), static_cast<EventQueue*>( event_queue.release() ) );
    }

    template <class EventHandlerType>
    auto_Stage createStage( auto_Object<EventHandlerType> event_handler, int16_t thread_count )
    {
      return createStage( static_cast<EventHandler*>( event_handler.release() ), NULL, thread_count );
    }

    template <class EventHandlerType, class EventQueueType>
    auto_Stage createStage( auto_Object<EventHandlerType> event_handler, auto_Object<EventQueueType> event_queue, int16_t thread_count )
    {
      return createStage( static_cast<EventHandler*>( event_handler.release() ), static_cast<EventQueue*>( event_queue.release() ), thread_count );
    }
    
    virtual auto_Stage createStage( auto_Object<EventHandler> event_handler,                                    
                                    auto_Object<EventQueue> event_queue = NULL,
                                    int16_t thread_count = 1 ) = 0;

    auto_ProcessorSet get_limit_physical_processor_set() const { return limit_physical_processor_set; }
    auto_ProcessorSet get_limit_logical_processor_set() const { return limit_logical_processor_set; }
    Stage** get_stages() { return &stages[0]; }

    // Object
    StageGroup& incRef() { return Object::incRef( *this ); }

  protected:
    StageGroup( auto_ProcessorSet limit_physical_processor_set );
    virtual ~StageGroup();


    void addStage( auto_Stage stage );  
    unsigned long get_running_stage_tls_key() const { return running_stage_tls_key; }


    class Thread : public ::YIELD::Thread
    {
    public:
      virtual ~Thread() { }

      virtual void stop() { should_run = false; }

      // YIELD::Thread
      void run();
      virtual void start();

    protected:
      Thread( auto_ProcessorSet limit_logical_processor_set );

      bool is_running, should_run;

      virtual void _run() = 0;

      inline bool visitStage( Stage& stage )
      {
        return stage.visit();
      }

      inline bool visitStage( Stage& stage, uint64_t timeout_ns )
      {
        return stage.visit( timeout_ns );
      }

    private:
      std::string stage_group_name;
      auto_ProcessorSet limit_logical_processor_set;

  //    auto_PerformanceCounterSet performance_counter_set;
    };

  private:
    auto_ProcessorSet limit_physical_processor_set, limit_logical_processor_set;

    unsigned long running_stage_tls_key;
    Stage* stages[YIELD_STAGES_PER_GROUP_MAX];
  };

  typedef auto_Object<StageGroup> auto_StageGroup;


  template <class StageGroupType> // CRTP
  class StageGroupImpl : public StageGroup
  {
  public:
    // Templated createStage's that pass the real EventHandler and EventQueue types to StageImpl to bypass the interfaces
    template <class EventHandlerType>
    auto_Stage createStage( auto_Object<EventHandlerType> event_handler )
    {
      return static_cast<StageGroupType*>( this )->createStage<EventHandlerType, OneSignalEventQueue>( event_handler, new OneSignalEventQueue, 1 );
    }

    template <class EventHandlerType>
    auto_Stage createStage( auto_Object<EventHandlerType> event_handler, auto_Object<EventQueue> event_queue, int16_t thread_count )
    {
      return static_cast<StageGroupType*>( this )->createStage<EventHandlerType, OneSignalEventQueue>( event_handler, new OneSignalEventQueue, thread_count );
    }

    // StageGroup
    auto_Stage createStage( auto_Object<EventHandler> event_handler, auto_Object<EventQueue> event_queue = NULL, int16_t thread_count = 1 )
    {
      if ( event_queue == NULL )
        return createStage<EventHandler>( event_handler, event_queue, thread_count );
      else
        return static_cast<StageGroupType*>( this )->createStage<EventHandler, EventQueue>( event_handler, event_queue, thread_count );
    }

  protected:
    StageGroupImpl( auto_ProcessorSet limit_physical_processor_set )
      : StageGroup( limit_physical_processor_set )
    { }

    virtual ~StageGroupImpl()
    { }
  };


  class VisitPolicy
  {
  protected:
    VisitPolicy( Stage** stages )
      : stages( stages )
    { }

    Stage** stages;
  };


  template <class VisitPolicyType>
  class PerProcessorStageGroup : public StageGroupImpl< PerProcessorStageGroup<VisitPolicyType> >
  {
  public:
    PerProcessorStageGroup( const char* name = "Main stage group", auto_Object<ProcessorSet> limit_physical_processor_set = NULL, int16_t threads_per_physical_processor = 1 );

    template <class EventHandlerType, class EventQueueType>
    auto_Object<Stage> createStage( auto_Object<EventHandlerType> event_handler, auto_Object<EventQueueType> event_queue, int16_t thread_count )
    {
      if ( thread_count <= 0 || thread_count > static_cast<int16_t>( this->physical_processor_threads.size() ) )
        thread_count = static_cast<int16_t>( this->physical_processor_threads.size() );

      auto_Object<Stage> stage;
      if ( event_handler->isThreadSafe() )
        stage = new TemplatedStageImpl<EventHandlerType, EventQueueType, NOPLock>( event_handler, event_queue, this->get_running_stage_tls_key() );
      else
        stage = new TemplatedStageImpl<EventHandlerType, EventQueueType, Mutex>( event_handler, event_queue, this->get_running_stage_tls_key() );

      stage->get_event_handler()->handleEvent( *( new Stage::StartupEvent( stage ) ) );

      for ( int16_t thread_i = 0; thread_i < thread_count; thread_i++ )
      {
        logical_processor_threads[next_stage_for_logical_processor_i]->addStage( stage->incRef() );
        next_stage_for_logical_processor_i = ( next_stage_for_logical_processor_i + 1 ) % logical_processor_threads.size();
      }

      this->addStage( stage );

      return stage;
    }

    // Object
    YIELD_OBJECT_PROTOTYPES( PerProcessorStageGroup<VisitPolicyType>, 0 );

  protected:
    virtual ~PerProcessorStageGroup();

  private:
    uint16_t threads_per_physical_processor;


    class Thread : public StageGroup::Thread
    {
    public:
      virtual void addStage( Stage& ) = 0;

      // StageGroupThread
      void stop();

    protected:
      Thread( auto_Object<ProcessorSet> limit_logical_processor_set, const std::string& stage_group_name, Stage** stages );    
      
      std::string stage_group_name;
      VisitPolicyType visit_policy;
    };

     
    class PhysicalProcessorThread : public Thread
    {
    public:
      PhysicalProcessorThread( auto_Object<ProcessorSet> limit_logical_processor_set, const std::string& stage_group_name );

      PhysicalProcessorThread& operator=( const PhysicalProcessorThread& ) { return *this; }

      Stage** get_stages() { return stages; }

      // PerProcessorStageGroup::Thread
      void addStage( Stage& );

    private:
      ~PhysicalProcessorThread() { }

      Stage* stage;
      Stage* stages[YIELD_STAGES_PER_GROUP_MAX];

      // StageGroup::Thread
      void _run();
    };


    class LogicalProcessorThread : public Thread
    {
    public:
      LogicalProcessorThread( auto_Object<ProcessorSet> limit_logical_processor_set, PhysicalProcessorThread& physical_processor_thread, const std::string& stage_group_name );
      
      LogicalProcessorThread& operator=( const LogicalProcessorThread& ) { return *this; }

      // PerProcessorStageGroup::Thread
      void addStage( Stage& );

    private:
      PhysicalProcessorThread& physical_processor_thread;

      // StageGroup::Thread
      void _run();
    };


    std::vector<Thread*> physical_processor_threads;
    std::vector<Thread*> logical_processor_threads;
    uint16_t next_stage_for_logical_processor_i;

    std::vector<Stage*> stages; // This can be a vector since it's only iterated on stage creation and destruction, unlike the fixed-length vectors used for visiting
  };


  class MG1VisitPolicy : public VisitPolicy
  {
  public:
    MG1VisitPolicy( Stage** stages );

    Stage* getNextStageToVisit( bool )
    {
      if ( polling_table_pos < YIELD_MG1_POLLING_TABLE_SIZE )
        return stages[polling_table[polling_table_pos++]];
      else
      {
        populatePollingTable();
        polling_table_pos = 0;
        return stages[polling_table[polling_table_pos++]];
      }
    }

  private:
    uint8_t polling_table[YIELD_MG1_POLLING_TABLE_SIZE]; uint32_t polling_table_pos;
    uint32_t golden_ratio_circle[YIELD_MG1_POLLING_TABLE_SIZE];
    double last_rhos[YIELD_STAGES_PER_GROUP_MAX]; // These are only used in populating the polling table, but we have to keep the values to use in smoothing

    bool populatePollingTable();
  };

  typedef PerProcessorStageGroup<MG1VisitPolicy> MG1StageGroup;


  class SEDAStageGroup : public StageGroupImpl<SEDAStageGroup>
  {
  public:
    SEDAStageGroup( auto_ProcessorSet limit_physical_processor_set = NULL )
        : StageGroupImpl<SEDAStageGroup>( limit_physical_processor_set )
    { }

    template <class EventHandlerType, class EventQueueType>
    auto_Stage createStage( auto_Object<EventHandlerType> event_handler, auto_Object<EventQueueType> event_queue, int16_t thread_count )
    {
      if ( thread_count == -1 )
      {
        if ( get_limit_physical_processor_set() != NULL )
          thread_count = get_limit_physical_processor_set()->count();
        else
          thread_count = Machine::getOnlinePhysicalProcessorCount();
      }

      auto_Stage stage;
      if ( event_handler->isThreadSafe() )
        stage = new TemplatedStageImpl<EventHandlerType, EventQueueType, NOPLock>( event_handler, event_queue, get_running_stage_tls_key() );
      else
        stage = new TemplatedStageImpl<EventHandlerType, EventQueueType, Mutex>( event_handler, event_queue, get_running_stage_tls_key() );

      event_handler->handleEvent( *( new Stage::StartupEvent( stage ) ) );

      this->addStage( stage );

      startThreads( stage, thread_count );

      return stage;
    }

    // Object
    YIELD_OBJECT_PROTOTYPES( SEDAStageGroup, 106 );

  protected:
    virtual ~SEDAStageGroup();

  private:
    class Thread : public StageGroup::Thread
    {
    public:
      Thread( auto_ProcessorSet limit_logical_processor_set, auto_Stage stage );

      auto_Stage get_stage() { return stage; }

      // Object
      YIELD_OBJECT_PROTOTYPES( SEDAStageGroup::Thread, 0 );

      // StageGroup::Thread
      void stop();

    private:
      ~Thread() { }

      auto_Stage stage;
      
      // StageGroup::Thread
      void _run();
    };

    std::vector<Thread*> threads;
    void startThreads( auto_Stage stage, int16_t thread_count );
  };


  class SRPTVisitPolicy : public VisitPolicy
  {
  public:
    SRPTVisitPolicy( Stage** stages ) : VisitPolicy( stages )
    {
      next_stage_i = 0;
    }

    inline Stage* getNextStageToVisit( bool last_visit_was_successful )
    {
      if ( last_visit_was_successful )
        return stages[0];
      else
      {
        next_stage_i = ( next_stage_i + 1 ) % YIELD_STAGES_PER_GROUP_MAX;
        return stages[next_stage_i];
      }
    }

  private:
    unsigned char next_stage_i;
  };

  typedef PerProcessorStageGroup<SRPTVisitPolicy> SRPTStageGroup;


  class WavefrontVisitPolicy : public VisitPolicy
  {
  public:
    WavefrontVisitPolicy( Stage** stages ) 
      : VisitPolicy( stages )
    {
      forward = true;
      next_stage_i = 0;
    }

    inline Stage* getNextStageToVisit( bool )
    {
      if ( forward )
      {
        if ( next_stage_i < YIELD_STAGES_PER_GROUP_MAX - 1 )
          ++next_stage_i;
        else
          forward = false;
      }
      else
      {
        if ( next_stage_i > 0 )
          --next_stage_i;
        else
          forward = true;
      }

      return stages[next_stage_i];
    }

  private:
    bool forward;
    unsigned char next_stage_i;
  };

  typedef PerProcessorStageGroup<WavefrontVisitPolicy> CohortStageGroup;
};

#endif
