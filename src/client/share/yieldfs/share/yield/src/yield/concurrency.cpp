// Revision: 1674

#include "yield/concurrency.h"
using namespace YIELD;


// event_handler.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
void EventHandler::handleUnknownEvent( Event& ev )
{
  switch ( ev.get_tag() )
  {
    case YIELD_OBJECT_TAG( Stage::StartupEvent ):
    case YIELD_OBJECT_TAG( Stage::ShutdownEvent ): Object::decRef( ev ); break;
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


// event_queue.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
Event* EventQueue::dequeue()
{
  if ( empty.acquire() )
  {
    if ( lock.try_acquire() )
    {
      if ( std::queue<Event*>::size() > 0 )
      {
        Event* event = std::queue<Event*>::front();
        std::queue<Event*>::pop();
        lock.release();
        return event;
      }
      else
        lock.release();
    }
  }
  return NULL;
}
Event* EventQueue::dequeue( uint64_t timeout_ns )
{
  if ( empty.timed_acquire( timeout_ns ) )
  {
    if ( lock.try_acquire() )
    {
      if ( std::queue<Event*>::size() > 0 )
      {
        Event* event = std::queue<Event*>::front();
        std::queue<Event*>::pop();
        lock.release();
        return event;
      }
      else
        lock.release();
    }
  }
  return NULL;
}
void EventQueue::enqueue( Event& ev )
{
  lock.acquire();
  std::queue<Event*>::push( &ev );
  lock.release();
  empty.release();
}


// instrumented_stage.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
template <class LockType> TimerQueue InstrumentedStageImpl<LockType>::statistics_timer_queue;
template <class LockType>
InstrumentedStageImpl<LockType>::InstrumentedStageImpl( auto_Object<EventHandler> event_handler, unsigned long running_stage_tls_key )
  : event_handler( event_handler ), running_stage_tls_key( running_stage_tls_key )
{
  event_queue_length = event_queue_arrival_count = 1; // send() would normally inc these, but we can't use send() because it's a virtual function; instead we enqueue directly and inc the lengths ourselves
  statistics_timer_queue.addTimer( new StatisticsTimer( incRef() ) );
}
template <class LockType>
void InstrumentedStageImpl<LockType>::_callEventHandler( Event& ev )
{
  uint64_t start_time_ns = Time::getCurrentUnixTimeNS();
  event_handler->handleEvent( ev );
  uint64_t event_processing_time_ns = Time::getCurrentUnixTimeNS() - start_time_ns;
  if ( event_processing_time_ns < 10 * NS_IN_S )
    event_processing_time_sampler.setNextSample( event_processing_time_ns );
}
template <class LockType>
void InstrumentedStageImpl<LockType>::send( Event& ev )
{
  ++event_queue_length;
  ++event_queue_arrival_count;
/*
  Stage* running_stage = static_cast<Stage*>( Thread::getTLS( running_stage_tls_key ) );
  if ( running_stage != NULL )
  {
    running_stage->send_counters_lock.acquire();
    std::map<const char*, uint64_t>::iterator send_counter_i = running_stage->send_counters.find( this->get_stage_name() );
    if ( send_counter_i != running_stage->send_counters.end() )
      send_counter_i->second++;
    else
      running_stage->send_counters.insert( std::make_pair( this->get_stage_name(), 1 ) );
    running_stage->send_counters_lock.release();
  }
  */
  event_queue.enqueue( ev );
}
template <class LockType>
bool InstrumentedStageImpl<LockType>::visit()
{
  if ( lock.try_acquire() )
  {
    Thread::setTLS( running_stage_tls_key, this );
    Event* ev = event_queue.dequeue();
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
template <class LockType>
bool InstrumentedStageImpl<LockType>::visit( uint64_t timeout_ns )
{
  if ( lock.try_acquire() )
  {
    Thread::setTLS( running_stage_tls_key, this );
    Event* ev = event_queue.dequeue( timeout_ns );
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
template <class LockType>
class InstrumentedStageImpl<LockType>::StatisticsTimer : public TimerQueue::Timer
{
public:
  StatisticsTimer( auto_Object<Stage> stage )
    : Timer( 1 * NS_IN_S, 1 * NS_IN_S ), stage( stage )
  { }
  // Timer
  bool fire( const Time& )
  {
    // double arrival_rate_s = static_cast<double>( stage->event_queue_arrival_count ) / elapsed_time.as_unix_time_s();
    // stage->event_queue_arrival_count = 0;
    //if ( strcmp( stage->get_stage_name(), "HTTPBenchmarkDriver" ) != 0 )
    //{
    //  double service_rate_s_max = static_cast<double>( NS_IN_S ) / stage->event_processing_time_sampler.get_min();
    //  double service_rate_s_min = static_cast<double>( NS_IN_S ) / stage->event_processing_time_sampler.get_max();
    //  double service_rate_s_25 = static_cast<double>( NS_IN_S ) / stage->event_processing_time_sampler.get_percentile( 0.25 );
    //  double service_rate_s_50 = static_cast<double>( NS_IN_S ) / stage->event_processing_time_sampler.get_percentile( 0.50 );
    //  double service_rate_s_75 = static_cast<double>( NS_IN_S ) / stage->event_processing_time_sampler.get_percentile( 0.75 );
    //  double service_rate_s_95 = static_cast<double>( NS_IN_S ) / stage->event_processing_time_sampler.get_percentile( 0.95 );
    //  double service_rate_s_mean = static_cast<double>( NS_IN_S ) / stage->event_processing_time_sampler.get_mean();
    //  double service_rate_s_median = static_cast<double>( NS_IN_S ) / stage->event_processing_time_sampler.get_median();
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
    return true;
  }
private:
  auto_Object<Stage> stage;
};
template class InstrumentedStageImpl<NOPLock>;
template class InstrumentedStageImpl<Mutex>;


// mg1_stage_group.cpp
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


// per_processor_stage_group.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
template <class VisitPolicyType>
PerProcessorStageGroup<VisitPolicyType>::PerProcessorStageGroup( const char* name, auto_ProcessorSet limit_physical_processor_set, int16_t threads_per_physical_processor )
  : StageGroupImpl< PerProcessorStageGroup<VisitPolicyType > >( limit_physical_processor_set )
{
  uint16_t logical_processors_per_physical_processor = Machine::getLogicalProcessorsPerPhysicalProcessor();
  uint16_t online_physical_processor_count = Machine::getOnlinePhysicalProcessorCount();
  if ( threads_per_physical_processor < 0 )
    this->threads_per_physical_processor = logical_processors_per_physical_processor;
  else if ( threads_per_physical_processor == 0 )
    this->threads_per_physical_processor = 1;
  else
    this->threads_per_physical_processor = threads_per_physical_processor;
  for ( uint16_t physical_processor_i = 0; physical_processor_i < online_physical_processor_count; physical_processor_i++ )
  {
    if ( limit_physical_processor_set == NULL || limit_physical_processor_set->isset( physical_processor_i ) )
    {
      PhysicalProcessorThread* physical_processor_thread = new PhysicalProcessorThread( new ProcessorSet( physical_processor_i * logical_processors_per_physical_processor ), name );
      physical_processor_threads.push_back( physical_processor_thread );
      logical_processor_threads.push_back( physical_processor_thread );
      for ( int16_t logical_thread_per_physical_processor_i = 1; logical_thread_per_physical_processor_i < this->threads_per_physical_processor; logical_thread_per_physical_processor_i++ )
        logical_processor_threads.push_back( new LogicalProcessorThread( new ProcessorSet( ( logical_processors_per_physical_processor * physical_processor_i ) + ( logical_thread_per_physical_processor_i % logical_processors_per_physical_processor ) ), *physical_processor_thread, name ) );
    }
  }
  next_stage_for_logical_processor_i = 0;
}
template <class VisitPolicyType>
PerProcessorStageGroup<VisitPolicyType>::~PerProcessorStageGroup()
{
  for ( typename std::vector<Thread*>::iterator logical_processor_thread_i = logical_processor_threads.begin(); logical_processor_thread_i != logical_processor_threads.end(); logical_processor_thread_i++ )
  {
    ( *logical_processor_thread_i )->stop();
    Object::decRef( **logical_processor_thread_i );
  }
  Stage::ShutdownEvent stage_shutdown_ev;
  for ( std::vector<Stage*>::iterator stage_i = stages.begin(); stage_i != stages.end(); stage_i++ )
    ( *stage_i )->get_event_handler()->handleEvent( stage_shutdown_ev.incRef() );
}
template <class VisitPolicyType>
class PerProcessorStageGroup<VisitPolicyType>::PhysicalProcessorThread : public Thread
{
public:
  PhysicalProcessorThread( auto_Object<ProcessorSet> limit_logical_processor_set, const std::string& stage_group_name )
    : Thread( limit_logical_processor_set, stage_group_name, stages )
  {
    memset( stages, 0, sizeof( stages ) );
    stage = NULL;
  }
  PhysicalProcessorThread& operator=( const PhysicalProcessorThread& ) { return *this; }
  Stage** get_stages() { return stages; }
  // PerProcessorStageGroup::Thread
  void addStage( Stage& stage )
  {
    if ( !this->is_running ) this->start();
    // Use a single pointer as a "queue" instead of a non-blocking finite queue, which is mostly overhead after startup
    while ( this->stage != NULL ) Thread::sleep( 1 * NS_IN_MS );
    this->stage = &stage;
  }
private:
  ~PhysicalProcessorThread() { }
  Stage* stage;
  Stage* stages[YIELD_STAGES_PER_GROUP_MAX];
  // StageGroup::Thread
  void _run()
  {
    // Per-thread variables on the stack
    uint64_t visit_timeout_ns = 0;
    uint64_t last_successful_visit_time_ns = Time::getCurrentUnixTimeNS();
    while ( this->should_run )
    {
      Stage* next_stage_to_visit = this->visit_policy.getNextStageToVisit( visit_timeout_ns == 0 );
      if ( next_stage_to_visit && this->visitStage( *next_stage_to_visit, visit_timeout_ns ) )
      {
        visit_timeout_ns = 0;
        last_successful_visit_time_ns = Time::getCurrentUnixTimeNS();
      }
      else
      {
        if ( Time::getCurrentUnixTimeNS() - last_successful_visit_time_ns > 200 * NS_IN_MS &&
            visit_timeout_ns < 20 * NS_IN_MS )
         visit_timeout_ns += NS_IN_MS;
      }
      if ( stage )
      {
        Stage* stage = this->stage;
        this->stage = NULL;
        unsigned char stage_i;
        for ( stage_i = 0; stage_i < YIELD_STAGES_PER_GROUP_MAX; stage_i++ )
        {
          if ( stages[stage_i] == NULL )
          {
            stages[stage_i] = stage;
            break;
          }
        }
        if ( stage_i == YIELD_STAGES_PER_GROUP_MAX )
        {
          std::ostringstream cerr_str;
          cerr_str << "PerProcessorStageGroupThread: too many stages on thread " << this->get_id() << ", failing." << std::endl;
          std::cerr << cerr_str;
          DebugBreak();
        }
      }
    }
  }
};
template <class VisitPolicyType>
class PerProcessorStageGroup<VisitPolicyType>::LogicalProcessorThread : public Thread
{
public:
  LogicalProcessorThread( auto_Object<ProcessorSet> limit_logical_processor_set, PhysicalProcessorThread& physical_processor_thread, const std::string& stage_group_name )
    : Thread( limit_logical_processor_set, stage_group_name, physical_processor_thread.get_stages() ),
      physical_processor_thread( physical_processor_thread )
  { }
  LogicalProcessorThread& operator=( const LogicalProcessorThread& ) { return *this; }
  // PerProcessorStageGroup::Thread
  void addStage( Stage& stage )
  {
    physical_processor_thread.addStage( stage );
  }
private:
  PhysicalProcessorThread& physical_processor_thread;
  // StageGroup::Thread
  void _run()
  {
    // Per-thread variables on the stack
    uint64_t visit_timeout_ns = 0;
    uint64_t last_successful_visit_time_ns = Time::getCurrentUnixTimeNS();
    while ( this->should_run )
    {
      Stage* next_stage_to_visit = this->visit_policy.getNextStageToVisit( visit_timeout_ns == 0 );
      if ( next_stage_to_visit && this->visitStage( *next_stage_to_visit, visit_timeout_ns ) )
      {
        visit_timeout_ns = 0;
        last_successful_visit_time_ns = Time::getCurrentUnixTimeNS();
      }
      else
      {
        if ( Time::getCurrentUnixTimeNS() - last_successful_visit_time_ns > 200 * NS_IN_MS &&
            visit_timeout_ns < 20 * NS_IN_MS )
         visit_timeout_ns += NS_IN_MS;
      }
    }
  }
};
template class PerProcessorStageGroup<MG1VisitPolicy>;
template class PerProcessorStageGroup<SRPTVisitPolicy>;
template class PerProcessorStageGroup<WavefrontVisitPolicy>;


// seda_stage_group.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
class SEDAStageGroup::Thread : public StageGroup::Thread
{
public:
  Thread( auto_ProcessorSet limit_logical_processor_set, auto_Stage stage )
    : StageGroup::Thread( limit_logical_processor_set ), stage( stage )
  { }
  auto_Stage get_stage() { return stage; }
  // Object
  YIELD_OBJECT_PROTOTYPES( SEDAStageGroup::Thread, 0 );
  // StageGroup::Thread
  void stop()
  {
    should_run = false;
    auto_Object<Stage::ShutdownEvent> stage_shutdown_event = new Stage::ShutdownEvent;
    for ( ;; )
    {
      stage->send( stage_shutdown_event->incRef() );
      if ( is_running )
        Thread::sleep( 50 * NS_IN_MS );
      else
        break;
    }
  }
private:
  ~Thread() { }
  auto_Stage stage;
  // StageGroup::Thread
  void _run()
  {
    Thread::set_name( stage->get_stage_name() );
    while ( should_run )
      visitStage( *stage );
  }
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
    Thread* thread = new Thread( get_limit_logical_processor_set(), stage );
    thread->start();
    this->threads.push_back( thread );
  }
}


// stage_group.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
StageGroup::StageGroup( auto_ProcessorSet limit_physical_processor_set )
: limit_physical_processor_set( limit_physical_processor_set )
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
  running_stage_tls_key = Thread::createTLSKey();
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
      stages[stage_i] = stage.release();
      return;
    }
  }
  DebugBreak();
}
StageGroup::Thread::Thread( auto_ProcessorSet limit_logical_processor_set )
  : limit_logical_processor_set( limit_logical_processor_set )
{
  is_running = false;
  should_run = true;
}
void StageGroup::Thread::start()
{
  YIELD::Thread::start();
  while ( !is_running )
    Thread::yield();
}
void StageGroup::Thread::run()
{
  if ( limit_logical_processor_set != NULL )
    this->set_processor_affinity( *limit_logical_processor_set );
//  Thread::setTLS( stage_group.get_running_stage_group_thread_tls_key(), this );
  is_running = true;
  _run();
  is_running = false;
}

