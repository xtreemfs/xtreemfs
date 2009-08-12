// Revision: 1805

#include "yield/concurrency.h"
using namespace YIELD;


// color_stage_group.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
class ColorStageGroup::Thread : public ::YIELD::Thread
{
public:
  Thread( auto_STLEventQueue event_queue, uint16_t logical_processor_i, const char* name )
    : event_queue( event_queue ), logical_processor_i( logical_processor_i ), name( name )
  {
    is_running = false;
    should_run = true;
  }
  void stop()
  {
    should_run = false;
    yidl::auto_Object<Stage::ShutdownEvent> stage_shutdown_event( new Stage::ShutdownEvent );
    while ( is_running )
    {
      event_queue->enqueue( stage_shutdown_event->incRef() );
      Thread::sleep( 5 * NS_IN_MS );
    }
  }
  // Thread
  void run()
  {
    is_running = true;
    this->set_name( name.c_str() );
    this->set_processor_affinity( logical_processor_i );
    while ( should_run )
    {
      Event* event = event_queue->dequeue();
      if ( event != NULL )
      {
        if ( event->get_next_stage() != NULL )
          event->get_next_stage()->visit( *event );
      }
    }
    is_running = false;
  }
private:
  auto_STLEventQueue event_queue;
  uint16_t logical_processor_i;
  std::string name;
  bool is_running, should_run;
};
ColorStageGroup::ColorStageGroup( const char* name, uint16_t start_logical_processor_i, int16_t thread_count )
{
  event_queue = new STLEventQueue;
  uint16_t online_logical_processor_count = Machine::getOnlineLogicalProcessorCount();
  if ( thread_count == -1 )
    thread_count = static_cast<int16_t>( online_logical_processor_count );
  for ( uint16_t logical_processor_i = start_logical_processor_i; logical_processor_i < start_logical_processor_i + thread_count; logical_processor_i++ )
  {
    Thread* thread = new Thread( event_queue, logical_processor_i % online_logical_processor_count, name );
    thread->start();
    threads.push_back( thread );
  }
}
ColorStageGroup::~ColorStageGroup()
{
  for ( std::vector<Thread*>::iterator thread_i = threads.begin(); thread_i != threads.end(); thread_i++ )
  {
    ( *thread_i )->stop();
    Object::decRef( **thread_i );
  }
}


// event_handler.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
void EventHandler::handleUnknownEvent( Event& ev )
{
  switch ( ev.get_type_id() )
  {
    case YIDL_OBJECT_TYPE_ID( Stage::StartupEvent ):
    case YIDL_OBJECT_TYPE_ID( Stage::ShutdownEvent ): Object::decRef( ev ); break;
    default:
    {
      std::cerr << get_type_name() << " dropping unknown event: " << ev.get_type_name() << std::endl;
      Object::decRef( ev );
    }
    break;
  }
}
void EventHandler::send( Event& ev )
{
  if ( isThreadSafe() )
    handleEvent( ev );
  else
  {
    handleEvent_lock.acquire();
    handleEvent( ev );
    handleEvent_lock.release();
  }
}


// event_target_mux.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
EventTargetMux::EventTargetMux()
{
  event_targets = NULL;
  event_targets_len = 0;
}
EventTargetMux::~EventTargetMux()
{
  for ( size_t event_target_i = 0; event_target_i < event_targets_len; event_target_i++ )
    Object::decRef( *event_targets[event_target_i] );
  delete [] event_targets;
}
void EventTargetMux::addEventTarget( auto_EventTarget event_target )
{
  EventTarget** new_event_targets = new EventTarget*[event_targets_len+1];
  if ( event_targets != NULL )
  {
    memcpy_s( new_event_targets, ( event_targets_len + 1 ) * sizeof( EventTarget* ), event_targets, event_targets_len * sizeof( EventTarget* ) );
    delete [] event_targets;
  }
  event_targets = new_event_targets;
  event_targets[event_targets_len] = event_target.release();
  event_targets_len++;
}
void EventTargetMux::send( Event& ev )
{
  next_event_target_i = ( next_event_target_i + 1 ) % event_targets_len;
  event_targets[next_event_target_i]->send( ev );
}


// mg1_visit_policy.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#include <cmath>
#define YIELD_MG1_MIN_RO 0.005
// Higher smoothing factor discounts older values faster
#define YIELD_MG1_RHO_EMA_SMOOTHING_FACTOR 0.7
MG1VisitPolicy::MG1VisitPolicy( Stage** stages ) : VisitPolicy( stages )
{
  memset( polling_table, 0, sizeof( polling_table ) );
  polling_table_pos = YIELD_MG1_POLLING_TABLE_SIZE;
  memset( last_rhos, 0, sizeof( last_rhos ) );
  double inverse_golden_ratio = ( std::sqrt( 5.0 ) - 1.0 ) / 2.0;
  double unit_circle[YIELD_MG1_POLLING_TABLE_SIZE], ordered_unit_circle[YIELD_MG1_POLLING_TABLE_SIZE+1];
  for ( uint32_t uc_i = 0; uc_i < YIELD_MG1_POLLING_TABLE_SIZE; uc_i++ )
  {
    unit_circle[uc_i] = fmod( uc_i * inverse_golden_ratio, 1.0 );
    ordered_unit_circle[uc_i] = unit_circle[uc_i];
  }
  std::sort( ordered_unit_circle, ordered_unit_circle+YIELD_MG1_POLLING_TABLE_SIZE );
  for ( uint32_t uc_i = 0; uc_i < YIELD_MG1_POLLING_TABLE_SIZE; uc_i++ )
  {
    bool found = false;
    for ( uint32_t ouc_i = 0; ouc_i < YIELD_MG1_POLLING_TABLE_SIZE; ouc_i++ )
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
  double rho_sqrt[YIELD_STAGES_PER_GROUP_MAX], rho_sqrt_sum = 0;
  memset( rho_sqrt, 0, sizeof( rho_sqrt ) );
  for ( uint8_t s_i = 0; s_i < YIELD_STAGES_PER_GROUP_MAX; s_i++ )
  {
    if ( stages[s_i] )
    {
      double rho = stages[s_i]->get_rho();
      if ( rho < YIELD_MG1_MIN_RO ) rho = YIELD_MG1_MIN_RO;
      else if ( rho >= 1 ) rho = 0.99;
      rho = YIELD_MG1_RHO_EMA_SMOOTHING_FACTOR * rho + ( 1 - YIELD_MG1_RHO_EMA_SMOOTHING_FACTOR ) * last_rhos[s_i];
      last_rhos[s_i] = rho;
      rho_sqrt[s_i] = std::sqrt( rho * ( 1.0 - rho ) );
      rho_sqrt_sum += rho_sqrt[s_i];
    }
  }
  if ( rho_sqrt_sum > 0 )
  {
    // frequency[i] = sqrt( rho[i] * ( 1.0 - rho[i] ) ) / ( sum( sqrt( rho[j] * ( 1.0 - rho[j] ) ) for all j )
    // occurrences[i] = frequency[i] * YIELD_MG1_POLLING_TABLE_SIZE
    uint32_t m[YIELD_STAGES_PER_GROUP_MAX], m_count = 0, m_total = 0;
    uint32_t max_m = 0; uint8_t fill_s_i;
    for ( uint8_t s_i = 0; s_i < YIELD_STAGES_PER_GROUP_MAX; s_i++ )
    {
      if ( rho_sqrt[s_i] > 0 )
      {
        double f = rho_sqrt[s_i] / rho_sqrt_sum;
        m[s_i] = static_cast<uint32_t>( floor( f * static_cast<double>( YIELD_MG1_POLLING_TABLE_SIZE ) ) );
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
    if ( m_total == 0 ) // Frequencies were too low to get an uint32_t m[s_i] > 0
    {
      uint32_t m_s_i = static_cast<uint32_t>( YIELD_MG1_POLLING_TABLE_SIZE / m_count );
      for ( uint8_t s_i = 0; s_i < YIELD_STAGES_PER_GROUP_MAX; s_i++ )
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
    for ( uint8_t s_i = 0; s_i < YIELD_STAGES_PER_GROUP_MAX; s_i++ )
    {
      for ( m_i_end = m_i + m[s_i]; m_i < m_i_end; m_i++ )
      {
        if ( m[polling_table[golden_ratio_circle[m_i]]] >= m[s_i] ) // Allow low-frequency entries to overwrite high-frequency, but not vice versa
          polling_table[golden_ratio_circle[m_i]] = s_i;
      }
    }
#ifdef _DEBUG
    //// Make sure every stage with a nonzero rho is represented in the polling table
    //for ( uint8_t s_i = 0; s_i < YIELD_STAGES_PER_GROUP_MAX; s_i++ )
    //{
    //      if ( rho_sqrt[s_i] > 0 )
    //      {
    //              bool found_s_i = false;
    //              for ( uint8_t pt_i = 0; pt_i < YIELD_MG1_POLLING_TABLE_SIZE; pt_i++ )
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
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
template <class VisitPolicyType>
class PollingStageGroup<VisitPolicyType>::Thread : public ::YIELD::Thread
{
public:
  Thread( uint16_t logical_processor_i, const char* name, Stage** stages )
    : logical_processor_i( logical_processor_i ),
      name( name ),
      visit_policy( stages )
  {
    is_running = false;
    should_run = true;
  }
  void stop()
  {
    should_run = false;
    while ( is_running )
      Thread::yield();
  }
  // Thread
  void run()
  {
    is_running = true;
    this->set_name( name.c_str() );
    if ( !this->set_processor_affinity( logical_processor_i ) )
      std::cerr << "yield::PollingStageGroup::Thread: error on set_processor_affinity( " << logical_processor_i << " ): " << Exception::strerror() << std::endl;
    uint64_t visit_timeout_ns = 1 * NS_IN_MS;
    uint64_t successful_visits = 0, total_visits = 0;
    while ( should_run )
    {
      Stage* next_stage_to_visit = visit_policy.getNextStageToVisit( visit_timeout_ns == 0 );
      if ( next_stage_to_visit != NULL )
      {
        total_visits++;
        if ( next_stage_to_visit->visit( visit_timeout_ns ) )
        {
          successful_visits++;
          visit_timeout_ns = 0;
        }
        else if ( visit_timeout_ns < 1 * NS_IN_MS )
          visit_timeout_ns += 1;
      }
    }
    std::cout << "yield::PollingStageGroup::Thread: visit efficiency = " << static_cast<double>( successful_visits ) / static_cast<double>( total_visits ) << std::endl;
    is_running = false;
  }
private:
  uint16_t logical_processor_i;
  std::string name;
  VisitPolicyType visit_policy;
  bool is_running, should_run;
};
template <class VisitPolicyType>
PollingStageGroup<VisitPolicyType>::PollingStageGroup( const char* name, uint16_t start_logical_processor_i, int16_t thread_count, bool use_thread_local_event_queues )
  : use_thread_local_event_queues( use_thread_local_event_queues )
{
  uint16_t online_logical_processor_count = Machine::getOnlineLogicalProcessorCount();
  if ( thread_count == -1 )
    thread_count = static_cast<int16_t>( online_logical_processor_count );
  for ( uint16_t logical_processor_i = start_logical_processor_i; logical_processor_i < start_logical_processor_i + thread_count; logical_processor_i++ )
  {
    Thread* thread = new Thread( logical_processor_i % online_logical_processor_count, name, this->get_stages() );
    thread->start();
    threads.push_back( thread );
  }
}
template <class VisitPolicyType>
PollingStageGroup<VisitPolicyType>::~PollingStageGroup()
{
  for ( typename std::vector<Thread*>::iterator thread_i = threads.begin(); thread_i != threads.end(); thread_i++ )
  {
    ( *thread_i )->stop();
    Object::decRef( **thread_i );
  }
}
template class PollingStageGroup<DBRVisitPolicy>;
template class PollingStageGroup<MG1VisitPolicy>;
template class PollingStageGroup<SRPTVisitPolicy>;
template class PollingStageGroup<WavefrontVisitPolicy>;


// request.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
auto_EventTarget Request::get_response_target() const
{
  return response_target;
}
void Request::respond( Response& response )
{
  response_target->send( response );
}
void Request::set_response_target( auto_EventTarget response_target )
{
  this->response_target = response_target;
}


// seda_stage_group.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
class SEDAStageGroup::Thread : public ::YIELD::Thread
{
public:
  Thread( auto_Stage stage )
    : stage( stage )
  {
    is_running = false;
    should_run = true;
  }
  auto_Stage get_stage() { return stage; }
  void stop()
  {
    should_run = false;
    yidl::auto_Object<Stage::ShutdownEvent> stage_shutdown_event = new Stage::ShutdownEvent;
    for ( ;; )
    {
      stage->send( stage_shutdown_event->incRef() );
      if ( is_running )
        Thread::sleep( 50 * NS_IN_MS );
      else
        break;
    }
  }
  // Thread
  void run()
  {
    is_running = true;
    Thread::set_name( stage->get_stage_name() );
    while ( should_run )
      stage->visit();
    is_running = false;
  }
  void start()
  {
    ::YIELD::Thread::start();
    while ( !is_running )
      Thread::yield();
  }
  // yidl::Object
  YIDL_OBJECT_PROTOTYPES( SEDAStageGroup::Thread, 0 );
private:
  ~Thread() { }
  auto_Stage stage;
  bool is_running, should_run;
};
SEDAStageGroup::~SEDAStageGroup()
{
  for ( std::vector<Thread*>::iterator thread_i = threads.begin(); thread_i != threads.end(); thread_i++ )
    ( *thread_i )->stop();
  for ( std::vector<Thread*>::iterator thread_i = threads.begin(); thread_i != threads.end(); thread_i++ )
    Object::decRef( **thread_i );
}
void SEDAStageGroup::startThreads( auto_Stage stage, int16_t thread_count )
{
  for ( unsigned short thread_i = 0; thread_i < thread_count; thread_i++ )
  {
    Thread* thread = new Thread( stage );
    thread->start();
    this->threads.push_back( thread );
  }
}


// stage.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
class Stage::StatisticsTimer : public TimerQueue::Timer
{
public:
  StatisticsTimer( yidl::auto_Object<Stage> stage )
    : Timer( 5 * NS_IN_S, 5 * NS_IN_S ), stage( stage )
  { }
  // Timer
  bool fire( const Time& elapsed_time )
  {
    if ( stage->event_queue_arrival_count > 0 )
    {
      stage->arrival_rate_s = static_cast<double>( stage->event_queue_arrival_count ) / elapsed_time.as_unix_time_s();
      stage->event_queue_arrival_count = 0;
      stage->service_rate_s = static_cast<double>( NS_IN_S ) / stage->event_processing_time_ns_sampler.get_percentile( 0.95 );
      stage->rho = stage->arrival_rate_s / stage->service_rate_s;
      //if ( strcmp( stage->get_stage_name(), "HTTPBenchmarkDriver" ) != 0 )
      //{
      //  double service_rate_s_max = static_cast<double>( NS_IN_S ) / stage->event_processing_time_ns_sampler.get_min();
      //  double service_rate_s_min = static_cast<double>( NS_IN_S ) / stage->event_processing_time_ns_sampler.get_max();
      //  double service_rate_s_25 = static_cast<double>( NS_IN_S ) / stage->event_processing_time_ns_sampler.get_percentile( 0.25 );
      //  double service_rate_s_50 = static_cast<double>( NS_IN_S ) / stage->event_processing_time_ns_sampler.get_percentile( 0.50 );
      //  double service_rate_s_75 = static_cast<double>( NS_IN_S ) / stage->event_processing_time_ns_sampler.get_percentile( 0.75 );
      //  double service_rate_s_95 = static_cast<double>( NS_IN_S ) / stage->event_processing_time_ns_sampler.get_percentile( 0.95 );
      //  double service_rate_s_mean = static_cast<double>( NS_IN_S ) / stage->event_processing_time_ns_sampler.get_mean();
      //  double service_rate_s_median = static_cast<double>( NS_IN_S ) / stage->event_processing_time_ns_sampler.get_median();
      //  std::ostringstream cout_line;
      //  cout_line << "{\"" << stage->get_stage_name() << "\":{";
      //  cout_line << "\"service_rate_s_max\": " << service_rate_s_max;
      //  cout_line << ",\"service_rate_s_min\": " << service_rate_s_min;
      //  cout_line << ",\"service_rate_s_25\": " << service_rate_s_25;
      //  cout_line << ",\"service_rate_s_50\": " << service_rate_s_50;
      //  cout_line << ",\"service_rate_s_75\": " << service_rate_s_75;
      //  cout_line << ",\"service_rate_s_95\": " << service_rate_s_95;
      //  cout_line << ",\"service_rate_s_mean\": " << service_rate_s_mean;
      //  cout_line << ",\"service_rate_s_median\": " << service_rate_s_median;
      //  cout_line << "}" << std::endl;
      //  std::cout << cout_line.str();
      //}
      //if ( !stage->send_counters.empty() )
      //{
      //  std::ostringstream cout_line;
      //  cout_line << "{\"" << stage->get_stage_name() << "\":{";
      //  stage->send_counters_lock.acquire();
      //  double send_counters_total = 0;
      //  for ( std::map<const char*, uint64_t>::const_iterator send_counter_i = stage->send_counters.begin(); send_counter_i != stage->send_counters.end(); send_counter_i++ )
      //    send_counters_total += send_counter_i->second;
      //  for ( std::map<const char*, uint64_t>::const_iterator send_counter_i = stage->send_counters.begin(); send_counter_i != stage->send_counters.end(); send_counter_i++ )
      //    cout_line << "\"" << send_counter_i->first << "\":" << static_cast<double>( send_counter_i->second ) / send_counters_total << ",";
      //  stage->send_counters_lock.release();
      //  cout_line << "\"\":0}}";
      //  cout_line << std::endl;
      //  std::cout << cout_line.str();
      //}
    }
    return true;
  }
private:
  auto_Stage stage;
};
Stage::Stage( const char* name )
  : name( name )
{
  // event_processing_time_ns_total = 0;
  event_queue_length = event_queue_arrival_count = 1; // send() would normally inc these, but we can't use send() because it's a virtual function; instead we enqueue directly and inc the lengths ourselves
  // events_processed_total = 0;
#ifdef YIELD_HAVE_PERFORMANCE_COUNTERS
  performance_counters = PerformanceCounterSet::create();
  performance_counters->addEvent( PerformanceCounterSet::EVENT_L1_DCM );
  performance_counters->addEvent( PerformanceCounterSet::EVENT_L2_ICM );
  std::memset( performance_counter_totals, 0, sizeof( performance_counter_totals ) );
#endif
  TimerQueue::getDefaultTimerQueue().addTimer( new StatisticsTimer( this ) );
}
Stage::~Stage()
{
  //std::cout << get_stage_name() << ": event processing time (ns) total: " << event_processing_time_ns_total << std::endl;
  //std::cout << get_stage_name() << ": event processing time (ns) expected: " << event_processing_time_ns_sampler.get_percentile( 0.95 ) * events_processed_total << std::endl;
#ifdef YIELD_HAVE_PERFORMANCE_COUNTERS
  std::cout << get_stage_name() << ": L1 data cache misses: " << performance_counter_totals[0] << std::endl;
  std::cout << get_stage_name() << ": L2 instruction cache misses: " << performance_counter_totals[1] << std::endl;
#endif
}


// stage_group.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).



StageGroup::StageGroup()
{
  memset( stages, 0, sizeof( stages ) );
}

StageGroup::~StageGroup()
{
  for ( uint8_t stage_i = 0; stage_i < YIELD_STAGES_PER_GROUP_MAX; stage_i++ )
    Object::decRef( stages[stage_i] );
}

void StageGroup::addStage( auto_Stage stage )
{
  unsigned char stage_i;
  for ( stage_i = 0; stage_i < YIELD_STAGES_PER_GROUP_MAX; stage_i++ )
  {
    if ( stages[stage_i] == NULL )
    {
      stage->set_stage_id( stage_i );
      stages[stage_i] = stage.release();
      return;
    }
  }

  DebugBreak();
}


// thread_local_event_queue.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
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
  void push( Event& ev )
  {
    std::stack<Event*>::push( &ev );
  }
};
ThreadLocalEventQueue::ThreadLocalEventQueue()
{
  tls_key = Thread::createTLSKey();
}
ThreadLocalEventQueue::~ThreadLocalEventQueue()
{
  for ( std::vector<EventStack*>::iterator event_stack_i = event_stacks.begin(); event_stack_i != event_stacks.end(); event_stack_i++ )
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
bool ThreadLocalEventQueue::enqueue( Event& ev )
{
  EventStack* event_stack = static_cast<EventStack*>( Thread::getTLS( tls_key ) );
  if ( event_stack != NULL )
  {
    event_stack->push( ev );
    return true;
  }
  else
    return all_processor_event_queue.enqueue( &ev );
}
ThreadLocalEventQueue::EventStack* ThreadLocalEventQueue::getEventStack()
{
  EventStack* event_stack = static_cast<EventStack*>( Thread::getTLS( tls_key ) );
  if ( event_stack == NULL )
  {
    event_stack = new EventStack;
    Thread::setTLS( tls_key, event_stack );
    event_stacks.push_back( event_stack );
  }
  return event_stack;
}
Event* ThreadLocalEventQueue::timed_dequeue( uint64_t timeout_ns )
{
  Event* event = getEventStack()->pop();
  if ( event != NULL )
    return event;
  else
    return all_processor_event_queue.timed_dequeue( timeout_ns );
}
Event* ThreadLocalEventQueue::try_dequeue()
{
  Event* event = getEventStack()->pop();
  if ( event != NULL )
    return event;
  else
    return all_processor_event_queue.try_dequeue();
}

