// Revision: 2162

#include "yield/concurrency.h"
using namespace yield::concurrency;


// color_stage_group.cpp
using yidl::runtime::auto_Object;


class ColorStageGroup::Thread : public ::yield::platform::Thread
{
public:
  Thread
  (
    STLEventQueue& event_queue,
    uint16_t logical_processor_i,
    const char* name
  )
    : event_queue( event_queue ),
      logical_processor_i( logical_processor_i ),
      name( name )
  {
    should_run = true;
  }

  void stop()
  {
    should_run = false;

    auto_Object<Stage::ShutdownEvent> stage_shutdown_event
      = new Stage::ShutdownEvent;

    event_queue.enqueue( stage_shutdown_event->inc_ref() );

    join();
  }

  // Thread
  void run()
  {
    this->set_name( name.c_str() );
    this->set_processor_affinity( logical_processor_i );

    while ( should_run )
    {
      Event* event = event_queue.dequeue();
      if ( event != NULL )
      {
        DebugBreak();
        //if ( event->get_next_stage() != NULL )
        //  event->get_next_stage()->visit( *event );
      }
    }
  }

private:
  STLEventQueue& event_queue;
  uint16_t logical_processor_i;
  string name;

  bool should_run;
};


ColorStageGroup::ColorStageGroup
(
  const char* name,
  uint16_t start_logical_processor_i,
  int16_t thread_count
)
{
  event_queue = new STLEventQueue;

  uint16_t online_logical_processor_count
    = yield::platform::ProcessorSet::getOnlineLogicalProcessorCount();

  if ( thread_count == -1 )
    thread_count = static_cast<int16_t>( online_logical_processor_count );

  for
  (
    uint16_t logical_processor_i = start_logical_processor_i;
    logical_processor_i < start_logical_processor_i + thread_count;
    logical_processor_i++
  )
  {
    Thread* thread
      = new Thread
            (
              *event_queue,
              logical_processor_i % online_logical_processor_count,
              name
            );
    thread->start();
    threads.push_back( thread );
  }
}

ColorStageGroup::~ColorStageGroup()
{
  for
  (
    vector<Thread*>::iterator thread_i = threads.begin();
    thread_i != threads.end();
    thread_i++
  )
  {
    ( *thread_i )->stop();
    Thread::dec_ref( **thread_i );
  }
}


// event_handler_mux.cpp
EventHandlerMux::EventHandlerMux()
{
  event_handlers = NULL;
  event_handlers_len = 0;
}

EventHandlerMux::~EventHandlerMux()
{
  for
  (
    size_t event_handler_i = 0;
    event_handler_i < event_handlers_len;
    event_handler_i++
  )
    EventHandler::dec_ref( *event_handlers[event_handler_i] );

  delete [] event_handlers;
}

void EventHandlerMux::add( EventHandler& event_handler )
{
  EventHandler** new_event_handlers = new EventHandler*[event_handlers_len+1];
  if ( event_handlers != NULL )
  {
    memcpy_s
    (
      new_event_handlers,
      ( event_handlers_len + 1 ) * sizeof( EventHandler* ),
      event_handlers,
      event_handlers_len * sizeof( EventHandler* )
    );

    delete [] event_handlers;
  }
  event_handlers = new_event_handlers;
  event_handlers[event_handlers_len] = &event_handler.inc_ref();
  event_handlers_len++;
}

void EventHandlerMux::handle( Event& event )
{
  next_event_handler_i = ( next_event_handler_i + 1 ) % event_handlers_len;
  event_handlers[next_event_handler_i]->handle( event );
}


// message_handler.cpp
void MessageHandler::handle( Event& event )
{
  if ( event.is_message() )
    handle( static_cast<Message&>( event ) );
  else
  {
    cerr << get_type_name() << ": received non-Message event: " <<
            event.get_type_name() << "." << endl;
  }
}


// mg1_visit_policy.cpp
using std::sort;

#include <cmath>
using std::sqrt;

#define YIELD_MG1_MIN_RO 0.005
// Higher smoothing factor discounts older values faster
#define YIELD_MG1_RHO_EMA_SMOOTHING_FACTOR 0.7


MG1VisitPolicy::MG1VisitPolicy( Stage** stages ) : VisitPolicy( stages )
{
  memset( polling_table, 0, sizeof( polling_table ) );
  polling_table_pos = YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE;
  memset( last_rhos, 0, sizeof( last_rhos ) );

  double inverse_golden_ratio = ( sqrt( 5.0 ) - 1.0 ) / 2.0;
  double unit_circle[YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE],
         ordered_unit_circle[YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE+1];

  for
  (
    uint32_t uc_i = 0;
    uc_i < YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE;
    uc_i++
  )
  {
    unit_circle[uc_i] = fmod( uc_i * inverse_golden_ratio, 1.0 );
    ordered_unit_circle[uc_i] = unit_circle[uc_i];
  }

  sort
  (
    ordered_unit_circle,
    ordered_unit_circle+YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE
  );

  for
  (
    uint32_t uc_i = 0;
    uc_i < YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE;
    uc_i++
  )
  {
    bool found = false;
    for
    (
      uint32_t ouc_i = 0;
      ouc_i < YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE;
      ouc_i++
    )
    {
      if ( unit_circle[uc_i] == ordered_unit_circle[ouc_i] )
      {
        golden_ratio_circle[uc_i] = ouc_i;
        found = true;
        break;
      }
    }
    if ( !found ) DebugBreak();
  }
}

bool MG1VisitPolicy::populatePollingTable()
{
  double rho_sqrt[YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX], rho_sqrt_sum = 0;
  memset( rho_sqrt, 0, sizeof( rho_sqrt ) );

  for ( uint8_t s_i = 0; s_i < YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX; s_i++ )
  {
    if ( stages[s_i] )
    {
      double rho = stages[s_i]->get_rho();
      if ( rho < YIELD_MG1_MIN_RO ) rho = YIELD_MG1_MIN_RO;
      else if ( rho >= 1 ) rho = 0.99;
      rho = YIELD_MG1_RHO_EMA_SMOOTHING_FACTOR * rho +
            ( 1 - YIELD_MG1_RHO_EMA_SMOOTHING_FACTOR ) * last_rhos[s_i];
      last_rhos[s_i] = rho;
      rho_sqrt[s_i] = sqrt( rho * ( 1.0 - rho ) );
      rho_sqrt_sum += rho_sqrt[s_i];
    }
  }

  if ( rho_sqrt_sum > 0 )
  {
    // frequency[i] = sqrt( rho[i] * ( 1.0 - rho[i] ) ) /
    //                ( sum( sqrt( rho[j] * ( 1.0 - rho[j] ) ) for all j )
    // occurrences[i] = frequency[i] * YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE
    uint32_t m[YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX], m_count = 0, m_total = 0;
    uint32_t max_m = 0; uint8_t fill_s_i;

    for ( uint8_t s_i = 0; s_i < YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX; s_i++ )
    {
      if ( rho_sqrt[s_i] > 0 )
      {
        double f = rho_sqrt[s_i] / rho_sqrt_sum;
        m[s_i] = static_cast<uint32_t>
                (
                  floor
                  (
                    f * static_cast<double>
                       (
                         YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE
                       )
                  )
                );
        if ( m[s_i] == 0 )
          m[s_i] = 1;
        if ( m[s_i] > max_m )
        {
          max_m = m[s_i];
          fill_s_i = s_i;
        }
        m_total += m[s_i];
        m_count++;
      }
      else
        m[s_i] = 0;
    }

    if ( m_total == 0 )
    {
      // Frequencies were too low to get an uint32_t m[s_i] > 0
      uint32_t m_s_i
        = static_cast<uint32_t>
          (
            YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE / m_count
          );

      for ( uint8_t s_i = 0; s_i < YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX; s_i++ )
      {
        if ( rho_sqrt[s_i] > 0 )
        {
          m[s_i] = m_s_i;
          fill_s_i = s_i;
        }
      }
    }

    // Fill the polling table with the most frequently-occurring stage
    // to plug the gaps in the golden circle coverage
    memset( polling_table, fill_s_i, sizeof( polling_table ) );

    uint32_t m_i = 0, m_i_end;
    for ( uint8_t s_i = 0; s_i < YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX; s_i++ )
    {
      for ( m_i_end = m_i + m[s_i]; m_i < m_i_end; m_i++ )
      {
        // Allow low-frequency entries to overwrite high-frequency,
        // but not vice versa
        if ( m[polling_table[golden_ratio_circle[m_i]]] >= m[s_i] )
          polling_table[golden_ratio_circle[m_i]] = s_i;
      }
    }

#ifdef _DEBUG
    //// Make sure every stage with a nonzero rho is represented
    //// in the polling table
    //for ( uint8_t s_i = 0; s_i < YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX; s_i++ )
    //{
    //      if ( rho_sqrt[s_i] > 0 )
    //      {
    //              bool found_s_i = false;
//for ( uint8_t pt_i = 0; pt_i < YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE;
// pt_i++ )
    //              {
    //                      if ( polling_table[pt_i] == s_i )
    //                      {
    //                              found_s_i = true;
    //                              break;
    //                      }
    //              }
    //              if ( !found_s_i ) DebugBreak();
    //      }
    //}
#endif

    return true;
  }
  else // Ro's were all 0 => no activity => idle half a second
    return false;
}


// polling_stage_group.cpp
template <class VisitPolicyType>
class PollingStageGroup<VisitPolicyType>::Thread
  : public ::yield::platform::Thread
{
public:
  Thread( uint16_t logical_processor_i, const char* name, Stage** stages )
    : logical_processor_i( logical_processor_i ),
      name( name ),
      visit_policy( stages )
  {
    should_run = true;
  }

  void stop()
  {
    should_run = false;
    join();
  }

  // Thread
  void run()
  {
    this->set_name( name.c_str() );

    if ( !this->set_processor_affinity( logical_processor_i ) )
    {
      cerr << "yield::concurrency::PollingStageGroup::Thread: " <<
                   "error on set_processor_affinity( " <<
                   logical_processor_i << " ): " <<
                   Exception() <<
                   "." << endl;
    }

    Time visit_timeout( 0.5 );
    uint64_t successful_visits = 0, total_visits = 0;

    while ( should_run )
    {
      Stage* next_stage_to_visit
        = visit_policy.getNextStageToVisit( visit_timeout == static_cast<uint64_t>( 0 ) );

      if ( next_stage_to_visit != NULL )
      {
        total_visits++;

        if ( next_stage_to_visit->visit( visit_timeout ) )
        {
          successful_visits++;
          visit_timeout = static_cast<uint64_t>( 0 );
        }
        else if ( visit_timeout < 1.0 )
          visit_timeout += 0.001;
      }
    }

    cout <<
      "yield::concurrency::PollingStageGroup::Thread: visit efficiency = " <<
      static_cast<double>( successful_visits ) /
        static_cast<double>( total_visits )
      << endl;
  }

private:
  uint16_t logical_processor_i;
  string name;
  VisitPolicyType visit_policy;

  bool should_run;
};


template <class VisitPolicyType>
PollingStageGroup<VisitPolicyType>::PollingStageGroup
(
  const char* name,
  uint16_t start_logical_processor_i,
  int16_t thread_count,
  bool use_thread_local_event_queues
)
  : use_thread_local_event_queues( use_thread_local_event_queues )
{
  uint16_t online_logical_processor_count
    = yield::platform::ProcessorSet::getOnlineLogicalProcessorCount();

  if ( thread_count == -1 )
    thread_count = static_cast<int16_t>( online_logical_processor_count );

  for
  (
    uint16_t logical_processor_i = start_logical_processor_i;
    logical_processor_i < start_logical_processor_i + thread_count;
    logical_processor_i++
  )
  {
    Thread* thread
      = new Thread
        (
          logical_processor_i % online_logical_processor_count,
          name,
          this->get_stages()
        );
    thread->start();
    threads.push_back( thread );
  }
}

template <class VisitPolicyType>
PollingStageGroup<VisitPolicyType>::~PollingStageGroup()
{
  for
  (
    typename vector<Thread*>::iterator thread_i = threads.begin();
    thread_i != threads.end();
    thread_i++
  )
  {
    ( *thread_i )->stop();
    Thread::dec_ref( **thread_i );
  }
}


template class PollingStageGroup<DBRVisitPolicy>;
template class PollingStageGroup<MG1VisitPolicy>;
template class PollingStageGroup<SRPTVisitPolicy>;
template class PollingStageGroup<WavefrontVisitPolicy>;


// request.cpp
Request::Request()
{
  credentials = NULL;
  response_handler = NULL;
}

Request::~Request()
{
  MarshallableObject::dec_ref( credentials );
  EventHandler::dec_ref( response_handler );
}

void Request::respond( Response& response )
{
  if ( response_handler != NULL )
    response_handler->handle( response );
  else
    Response::dec_ref( response );
}

void Request::set_credentials( MarshallableObject* credentials )
{
  MarshallableObject::dec_ref( this->credentials );
  this->credentials = credentials;
}

void Request::set_response_handler( EventHandler* response_handler )
{
  EventHandler::dec_ref( this->response_handler );
  this->response_handler = response_handler;
}

void Request::set_response_handler( EventHandler& response_handler )
{
  EventHandler::dec_ref( this->response_handler );
  this->response_handler = &response_handler.inc_ref();
}


// request_handler.cpp
void RequestHandler::handle( Message& message )
{
  if ( message.is_request() )
    handle( static_cast<Request&>( message ) );
  else
  {
    cerr << get_type_name() << ": received non-Request message: " <<
            message.get_type_name() << "." << endl;
  }
}


// response_handler.cpp
void ResponseHandler::handle( Message& message )
{
  if ( !message.is_request() )
  {
    handle( static_cast<Response&>( message ) );
    return;
  }
  else
  {
    cerr << "ResponseHandler: received non-Response message: " <<
            message.get_type_name() << "." << endl;
  }
}


// seda_stage_group.cpp
class SEDAStageGroup::Thread : public yield::platform::Thread
{
public:
  Thread( Stage& stage )
    : stage( stage )
  {
    should_run = true;
  }

  Stage& get_stage() { return stage; }

  void stop()
  {
    should_run = false;

    stage.handle( *new Stage::ShutdownEvent );

    join();
  }

  // Thread
  void run()
  {
    Thread::set_name( stage.get_event_handler().get_type_name() );

    while ( should_run )
      stage.visit();
  }

private:
  ~Thread() { }

  Stage& stage;

  bool should_run;
};


SEDAStageGroup::~SEDAStageGroup()
{
  for
  (
    vector<Thread*>::iterator thread_i = threads.begin();
    thread_i != threads.end();
    thread_i++
  )
    ( *thread_i )->stop();

  for
  (
    vector<Thread*>::iterator thread_i = threads.begin();
    thread_i != threads.end();
    thread_i++
  )
    Thread::dec_ref( **thread_i );
}

void SEDAStageGroup::startThreads( Stage& stage, int16_t thread_count )
{
  for ( unsigned short thread_i = 0; thread_i < thread_count; thread_i++ )
  {
    Thread* thread = new Thread( stage );
    thread->start();
    this->threads.push_back( thread );
  }
}


// stage.cpp
using yield::platform::TimerQueue;


class Stage::StatisticsTimer : public TimerQueue::Timer
{
public:
  StatisticsTimer( Stage& stage )
    : Timer( 5.0, 5.0 ),
      stage( stage.inc_ref() ),
      last_fire_time( static_cast<uint64_t>( 0 ) )
  { }

  ~StatisticsTimer()
  {
    Stage::dec_ref( stage );
  }

  // TimerQueue::Timer
  void fire()
  {
    if ( stage.event_queue_arrival_count > 0 )
    {
      Time
        elapsed_time( Time() - last_fire_time );

      stage.arrival_rate_s
        = static_cast<double>( stage.event_queue_arrival_count ) /
          elapsed_time.as_unix_time_s();

      stage.event_queue_arrival_count = 0;

      stage.service_rate_s
        = static_cast<double>( Time::NS_IN_S ) /
          stage.event_processing_time_sampler
            .get_percentile( 0.95 );

      stage.rho = stage.arrival_rate_s / stage.service_rate_s;

      last_fire_time = Time();
    }
  }

private:
  Stage& stage;

  Time last_fire_time;
};


Stage::Stage()
{
  // event_processing_time_ns_total = 0;

  // send() would normally inc these, but we can't use send()
  // because it's a virtual function; instead we enqueue directly
  // and increment the lengths ourselves
  event_queue_length = event_queue_arrival_count = 1;

  // events_processed_total = 0;

#ifdef YIELD_PLATFORM_HAVE_PERFORMANCE_COUNTERS
  performance_counters = yield::platform::PerformanceCounterSet::create();
  performance_counters->addEvent( yield::platform::PerformanceCounterSet::EVENT_L1_DCM );
  performance_counters->addEvent( yield::platform::PerformanceCounterSet::EVENT_L2_ICM );
  memset( performance_counter_totals, 0, sizeof( performance_counter_totals ) );
#endif

  TimerQueue::getDefaultTimerQueue().addTimer( *new StatisticsTimer( *this ) );
}

Stage::~Stage()
{
#ifdef YIELD_PLATFORM_HAVE_PERFORMANCE_COUNTERS
  cout << get_event_handler().get_type_name() <<
          ": L1 data cache misses: " <<
          performance_counter_totals[0] <<
          endl;

  cout << get_event_handler().get_type_name() <<
          ": L2 instruction cache misses: " <<
          performance_counter_totals[1] <<
          endl;
#endif
}


// stage_group.cpp
StageGroup::StageGroup()
{
  memset( stages, 0, sizeof( stages ) );
}

StageGroup::~StageGroup()
{
  for ( uint8_t stage_i = 0; stage_i < YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX; stage_i++ )
    Stage::dec_ref( stages[stage_i] );
}

void StageGroup::addStage( Stage* stage )
{
  unsigned char stage_i;
  for ( stage_i = 0; stage_i < YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX; stage_i++ )
  {
    if ( stages[stage_i] == NULL )
    {
      stage->set_stage_id( stage_i );
      stages[stage_i] = stage;
      return;
    }
  }

  DebugBreak();
}


// thread_local_event_queue.cpp
#include <stack>


class ThreadLocalEventQueue::EventStack : private std::stack<Event*>
{
public:
  Event* pop()
  {
    if ( !std::stack<Event*>::empty() )
    {
      Event* event = std::stack<Event*>::top();
      std::stack<Event*>::pop();
      return event;
    }
    else
      return NULL;
  }

  void push( Event& event )
  {
    std::stack<Event*>::push( &event );
  }
};


ThreadLocalEventQueue::ThreadLocalEventQueue()
{
  tls_key = yield::platform::Thread::key_create();
}

ThreadLocalEventQueue::~ThreadLocalEventQueue()
{
  for
  (
    vector<EventStack*>::iterator event_stack_i = event_stacks.begin();
    event_stack_i != event_stacks.end();
    event_stack_i++
  )
    delete *event_stack_i;
}

Event* ThreadLocalEventQueue::dequeue()
{
  Event* event = getEventStack()->pop();
  if ( event != NULL )
    return event;
  else
    return all_processor_event_queue.dequeue();
}

Event* ThreadLocalEventQueue::dequeue( const Time& timeout )
{
  Event* event = getEventStack()->pop();
  if ( event != NULL )
    return event;
  else
    return all_processor_event_queue.dequeue( timeout );
}

bool ThreadLocalEventQueue::enqueue( Event& event )
{
  EventStack* event_stack
    = static_cast<EventStack*>
    (
      yield::platform::Thread::getspecific( tls_key )
    );

  if ( event_stack != NULL )
  {
    event_stack->push( event );
    return true;
  }
  else
    return all_processor_event_queue.enqueue( &event );
}

ThreadLocalEventQueue::EventStack* ThreadLocalEventQueue::getEventStack()
{
  EventStack* event_stack
    = static_cast<EventStack*>
    (
      yield::platform::Thread::getspecific( tls_key )
    );

  if ( event_stack == NULL )
  {
    event_stack = new EventStack;
    yield::platform::Thread::setspecific( tls_key, event_stack );
    event_stacks.push_back( event_stack );
  }
  return event_stack;
}

Event* ThreadLocalEventQueue::try_dequeue()
{
  Event* event = getEventStack()->pop();
  if ( event != NULL )
    return event;
  else
    return all_processor_event_queue.try_dequeue();
}

