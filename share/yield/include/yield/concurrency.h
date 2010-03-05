// Copyright (c) 2010 Minor Gordon
// With original implementations and ideas contributed by Felix Hupfeld
// All rights reserved
// 
// This source file is part of the Yield project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the Yield project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#ifndef _YIELD_CONCURRENCY_H_
#define _YIELD_CONCURRENCY_H_

#include "yield/platform.h"

#include <map>
#include <queue>


// YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE should be a Fibonnaci number:
// 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584
#define YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE 144
#define YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX 64


namespace YIELD
{
  namespace concurrency
  {
    class Event;
    typedef yidl::runtime::auto_Object<Event> auto_Event;

    class EventTarget;
    typedef yidl::runtime::auto_Object<EventTarget> auto_EventTarget;

    class EventHandler;
    typedef yidl::runtime::auto_Object<EventHandler> auto_EventHandler;

    class EventQueue;
    typedef yidl::runtime::auto_Object<EventQueue> auto_EventQueue;

    class STLEventQueue;
    typedef yidl::runtime::auto_Object<STLEventQueue> auto_STLEventQueue;

    class Request;
    typedef yidl::runtime::auto_Object<Request> auto_Request;

    class Response;
    typedef yidl::runtime::auto_Object<Response> auto_Response;

    class ExceptionResponse;
    typedef yidl::runtime::auto_Object<ExceptionResponse>
      auto_ExceptionResponse;

    class Stage;
    typedef yidl::runtime::auto_Object<Stage> auto_Stage;


    class Event : public yidl::runtime::Struct
    {
    public:
      Event()
        : next_stage( NULL )
      { }

      Stage* get_next_stage() const
      {
          return next_stage;
      }

      void set_next_stage( Stage* next_stage )
      {
          this->next_stage = next_stage;
      }

      // yidl::runtime::Object
      Event& inc_ref() { return yidl::runtime::Object::inc_ref( *this ); }

    protected:
      virtual ~Event() { }

    private:
      Stage* next_stage;
    };


    class EventFactory : public yidl::runtime::MarshallableObjectFactory
    {
    public:
      virtual ~EventFactory() { }

      virtual auto_ExceptionResponse 
      createExceptionResponse( uint32_t type_id )
      {
        return NULL;
      }

      virtual auto_Request createRequest( uint32_t type_id )
      {
        return NULL;
      }

      virtual auto_Response createResponse( uint32_t type_id )
      {
        return NULL;
      }

      // yidl::runtime::RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( EventFactory, 2 );
    };


    class EventTarget : public yidl::runtime::Object
    {
    public:
      virtual void send( Event& ) = 0;

      // yidl::runtime::Object
      EventTarget& inc_ref() 
      { 
        return yidl::runtime::Object::inc_ref( *this ); 
      }

    protected:
      EventTarget() { }
      virtual ~EventTarget() { }
    };


    class EventTargetMux : public EventTarget
    {
    public:
      EventTargetMux();

      void addEventTarget( auto_EventTarget event_target );

      // EventTarget
      void send( Event& );

    private:
      ~EventTargetMux();

      EventTarget** event_targets;
      size_t event_targets_len;
      size_t next_event_target_i;
    };


    class EventHandler : public EventTarget
    {
    public:
      virtual const char* get_event_handler_name() { return ""; }

      virtual void handleEvent( Event& ) = 0;

      // yidl::runtime::Object
      EventHandler& inc_ref() 
      { 
        return yidl::runtime::Object::inc_ref( *this ); 
      }

      // EventTarget
      void send( Event& ev )
      {
        // EventHandler = a pass-through EventTarget
        // handleEvent must do its own locking if necessary!
        handleEvent( ev );
      }

    protected:
      EventHandler() { }
      virtual ~EventHandler() { }
    };


    class EventQueue : public EventTarget
    {
    public:
      virtual Event* dequeue() = 0;
      virtual bool enqueue( Event& ) = 0;
      virtual Event* timed_dequeue( const YIELD::platform::Time& timeout ) = 0;
      virtual Event* try_dequeue() = 0;

      // EventTarget
      void send( Event& event )
      {
        enqueue( event );
      }
    };


    template <class ElementType, uint32_t QueueLength>
    class NonBlockingFiniteQueue
    {
    public:
      NonBlockingFiniteQueue()
      {
        head = 0;
        tail = 1;

        for ( size_t element_i = 0; element_i < QueueLength+2; element_i++ )
          elements[element_i] = reinterpret_cast<ElementType>( 0 );

        elements[0] = reinterpret_cast<ElementType>( 1 );
      }

      ElementType dequeue()
      {
        yidl::runtime::atomic_t copied_head, try_pos;
        ElementType try_element;

        for ( ;; )
        {
          copied_head = head;
          try_pos = ( copied_head + 1 ) % ( QueueLength + 2 );
          try_element = reinterpret_cast<ElementType>( elements[try_pos] );

          while
          (
            try_element == reinterpret_cast<ElementType>( 0 ) ||
            try_element == reinterpret_cast<ElementType>( 1 )
          )
          {
            if ( copied_head != head )
              break;

            if ( try_pos == tail )
              return 0;

            try_pos = ( try_pos + 1 ) % ( QueueLength + 2 );

            try_element = reinterpret_cast<ElementType>( elements[try_pos] );
          }

          if ( copied_head != head )
            continue;

          if ( try_pos == tail )
          {
            yidl::runtime::atomic_cas
            (
              &tail,
              ( try_pos + 1 ) % ( QueueLength + 2 ),
              try_pos
            );

            continue;
          }

          if ( copied_head != head )
            continue;

          if
          (
            yidl::runtime::atomic_cas
            (
              // Memory
              reinterpret_cast<volatile yidl::runtime::atomic_t*>
              (
                &elements[try_pos]
              ),

              // New value
              (
                reinterpret_cast<yidl::runtime::atomic_t>
                (
                  try_element
                ) & POINTER_HIGH_BIT
              ) ? 1 : 0,

              // New value
              reinterpret_cast<yidl::runtime::atomic_t>( try_element )

            ) // Test against old value
            == reinterpret_cast<yidl::runtime::atomic_t>( try_element )
          )
          {
            if ( try_pos % 2 == 0 )
            {
              yidl::runtime::atomic_cas
              (
                &head,
                try_pos,
                copied_head
              );
            }

            return
              reinterpret_cast<ElementType>
              (
                (
                  reinterpret_cast<yidl::runtime::atomic_t>( try_element )
                  & POINTER_LOW_BITS
              ) << 1
            );
          }
        }
      }

      bool enqueue( ElementType element )
      {
#ifdef _DEBUG
        if ( reinterpret_cast<yidl::runtime::atomic_t>( element ) & 0x1 )
          DebugBreak();
#endif

        element = reinterpret_cast<ElementType>
        (
          reinterpret_cast<yidl::runtime::atomic_t>( element ) >> 1
        );

#ifdef _DEBUG
        if
        (
          reinterpret_cast<yidl::runtime::atomic_t>( element ) &
            POINTER_HIGH_BIT
        )
          DebugBreak();
#endif

        yidl::runtime::atomic_t copied_tail,
                                last_try_pos,
                                try_pos; // te, ate, temp
        ElementType try_element;

        for ( ;; )
        {
          copied_tail = tail;
          last_try_pos = copied_tail;
          try_element
            = reinterpret_cast<ElementType>
            (
              elements[last_try_pos]
            );
          try_pos = ( last_try_pos + 1 ) % ( QueueLength + 2 );

          while
          (
            try_element != reinterpret_cast<ElementType>( 0 ) &&
            try_element != reinterpret_cast<ElementType>( 1 )
          )
          {
            if ( copied_tail != tail )
              break;

            if ( try_pos == head )
              break;

            try_element = reinterpret_cast<ElementType>( elements[try_pos] );
            last_try_pos = try_pos;
            try_pos = ( last_try_pos + 1 ) % ( QueueLength + 2 );
          }

          if ( copied_tail != tail ) // Someone changed tail
            continue;                // while we were looping

          if ( try_pos == head )
          {
            last_try_pos = ( try_pos + 1 ) % ( QueueLength + 2 );
            try_element
              = reinterpret_cast<ElementType>( elements[last_try_pos] );

            if ( try_element != reinterpret_cast<ElementType>( 0 ) &&
                 try_element != reinterpret_cast<ElementType>( 1 ) )
              return false; // Queue is full

            yidl::runtime::atomic_cas
            (
              &head,
              last_try_pos,
              try_pos
            );

            continue;
          }

          if ( copied_tail != tail )
            continue;

          // diff next line
          if
          (
            yidl::runtime::atomic_cas
            (
              // Memory
              reinterpret_cast<volatile yidl::runtime::atomic_t*>
              (
                &elements[last_try_pos]
              ),

              // New value
              try_element == reinterpret_cast<ElementType>( 1 ) ?
                ( reinterpret_cast<yidl::runtime::atomic_t>( element )
                  | POINTER_HIGH_BIT ) :
                reinterpret_cast<yidl::runtime::atomic_t>( element ),

              // Old value
              reinterpret_cast<yidl::runtime::atomic_t>( try_element )

            ) // Test against old value
            == reinterpret_cast<yidl::runtime::atomic_t>( try_element )
          )
          {
            if ( try_pos % 2 == 0 )
            {
              yidl::runtime::atomic_cas
              (
                &tail,
                try_pos,
                copied_tail
              );
            }

            return true;
          }
        }
      }

    private:
      volatile ElementType elements[QueueLength+2]; // extra 2 for sentinels
      volatile yidl::runtime::atomic_t head, tail;

#if defined(__LLP64__) || defined(__LP64__)
      const static yidl::runtime::atomic_t POINTER_HIGH_BIT
        = 0x8000000000000000;

      const static yidl::runtime::atomic_t POINTER_LOW_BITS
        = 0x7fffffffffffffff;
#else
      const static yidl::runtime::atomic_t POINTER_HIGH_BIT = 0x80000000;
      const static yidl::runtime::atomic_t POINTER_LOW_BITS = 0x7fffffff;
#endif
    };


    template <class ElementType, uint32_t QueueLength>
    class SynchronizedNonBlockingFiniteQueue
      : private NonBlockingFiniteQueue<ElementType, QueueLength>
    {
    public:
      ElementType dequeue()
      {
        ElementType element =
          NonBlockingFiniteQueue<ElementType, QueueLength>::dequeue();

        while ( element == 0 )
        {
          signal.acquire();
          element = NonBlockingFiniteQueue<ElementType, QueueLength>::dequeue();
        }

        return element;
      }

      bool enqueue( ElementType element )
      {
        bool enqueued =
          NonBlockingFiniteQueue<ElementType, QueueLength>::enqueue( element );
        signal.release();
        return enqueued;
      }

      ElementType timed_dequeue( const YIELD::platform::Time& timeout )
      {
        ElementType element
          = NonBlockingFiniteQueue<ElementType, QueueLength>::dequeue();

        if ( element != 0 )
          return element;
        else
        {
          signal.timed_acquire( timeout );
          return NonBlockingFiniteQueue<ElementType, QueueLength>::dequeue();
        }
      }

      ElementType try_dequeue()
      {
        return NonBlockingFiniteQueue<ElementType, QueueLength>::dequeue();
      }

    private:
      YIELD::platform::Semaphore signal;
    };


    class NonBlockingEventQueue
      : public EventQueue,
        private SynchronizedNonBlockingFiniteQueue<Event*,1024>
    {
    public:
      // EventQueue
      Event* dequeue()
      {
        return SynchronizedNonBlockingFiniteQueue<Event*,1024>::dequeue();
      }

      bool enqueue( Event& ev )
      {
        return SynchronizedNonBlockingFiniteQueue<Event*,1024>::enqueue( &ev );
      }

      Event* timed_dequeue( const YIELD::platform::Time& timeout )
      {
        return SynchronizedNonBlockingFiniteQueue<Event*,1024>
                 ::timed_dequeue( timeout );
      }

      Event* try_dequeue()
      {
        return SynchronizedNonBlockingFiniteQueue<Event*,1024>::try_dequeue();
      }
    };


    template <class ElementType>
    class SynchronizedSTLQueue : private std::queue<ElementType>
    {
    public:
      ElementType dequeue()
      {
        for ( ;; )
        {
          signal.acquire();
          lock.acquire();
          if ( std::queue<ElementType>::size() > 0 )
          {
            ElementType element = std::queue<ElementType>::front();
            std::queue<ElementType>::pop();
            lock.release();
            return element;
          }
          else
            lock.release();
        }
      }

      bool enqueue( ElementType element )
      {
        lock.acquire();
        std::queue<ElementType>::push( element );
        lock.release();
        signal.release();
        return true;
      }

      ElementType timed_dequeue( const YIELD::platform::Time& timeout )
      {
        YIELD::platform::Time timeout_left( timeout );

        for ( ;; )
        {
          YIELD::platform::Time start_time;

          if ( signal.timed_acquire( timeout_left ) )
          {
            if ( lock.try_acquire() )
            {
              if ( std::queue<ElementType>::size() > 0 )
              {
                ElementType element = std::queue<ElementType>::front();
                std::queue<ElementType>::pop();
                lock.release();
                return element;
              }
              else
                lock.release();
            }
          }

          YIELD::platform::Time elapsed_time; elapsed_time -= start_time;
          if ( elapsed_time < timeout_left )
            timeout_left -= elapsed_time;
          else
            return NULL;
        }
      }

      ElementType try_dequeue()
      {
        if ( lock.try_acquire() )
        {
          if ( std::queue<ElementType>::size() > 0 )
          {
            ElementType element = std::queue<ElementType>::front();
            std::queue<ElementType>::pop();
            lock.release();
            return element;
          }
          else
            lock.release();
        }

        return NULL;
      }

    private:
      YIELD::platform::Mutex lock;
      YIELD::platform::Semaphore signal;
    };


    class STLEventQueue
      : public EventQueue,
        private SynchronizedSTLQueue<Event*>
    {
    public:
      // EventQueue
      Event* dequeue()
      {
        return SynchronizedSTLQueue<Event*>::dequeue();
      }

      bool enqueue( Event& ev )
      {
        return SynchronizedSTLQueue<Event*>::enqueue( &ev );
      }

      Event* timed_dequeue( const YIELD::platform::Time& timeout )
      {
        return SynchronizedSTLQueue<Event*>::timed_dequeue( timeout );
      }

      Event* try_dequeue()
      {
        return SynchronizedSTLQueue<Event*>::try_dequeue();
      }
    };


    class ThreadLocalEventQueue : public EventQueue
    {
    public:
      ThreadLocalEventQueue();
      ~ThreadLocalEventQueue();

      // EventQueue
      Event* dequeue();
      bool enqueue( Event& );
      Event* timed_dequeue( const YIELD::platform::Time& timeout );
      Event* try_dequeue();

    private:
      class EventStack;

      unsigned long tls_key;
      std::vector<EventStack*> event_stacks;
      EventStack* getEventStack();

      SynchronizedSTLQueue<Event*> all_processor_event_queue;
    };


    class Request : public Event
    {
    public:
      auto_EventTarget get_response_target() const;
      virtual void respond( Response& response );
      void set_response_target( auto_EventTarget response_target );

    protected:
      Request() { }
      virtual ~Request() { }

    private:
      auto_EventTarget response_target;
    };


    class Response : public Event
    {
    protected:
      Response() { }
      virtual ~Response() { }
    };


    class ExceptionResponse
      : public Response,
        public YIELD::platform::Exception
    {
    public:
      ExceptionResponse() { }

      ExceptionResponse( uint32_t error_code )
        : Exception( error_code )
      { }

      ExceptionResponse( const char* error_message )
        : Exception( error_message )
      { }

      ExceptionResponse( uint32_t error_code, const char* error_message )
        : Exception( error_code, error_message )
      { }

      ExceptionResponse( const Exception& other )
        : Exception( other )
      { }

      ExceptionResponse( const ExceptionResponse& other )
        : Exception( other )
      { }

      virtual ~ExceptionResponse() throw()
      { }

      virtual ExceptionResponse* clone() const
      {
        return new ExceptionResponse( *this );
      }

      virtual void throwStackClone() const
      {
        throw ExceptionResponse( *this );
      }

      // yidl::runtime::RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ExceptionResponse, 102 );

      // yidl::runtime::MarshallableObject
      void marshal( yidl::runtime::Marshaller& ) const { }
      void unmarshal( yidl::runtime::Unmarshaller& ) { }
    };


    template <class ResponseType>
    class ResponseQueue
      : public EventTarget,
        private SynchronizedSTLQueue<Event*>
    {
    public:
      ResponseType& dequeue()
      {
        Event* dequeued_ev = SynchronizedSTLQueue<Event*>::dequeue();

        switch ( dequeued_ev->get_type_id() )
        {
          case ResponseType::TYPE_ID:
          {
            return static_cast<ResponseType&>( *dequeued_ev );
          }
          break;

          case ExceptionResponse::TYPE_ID:
          {
            try
            {
              static_cast<ExceptionResponse*>( dequeued_ev )
                ->throwStackClone();

              // Eliminate compiler warnings about control paths
              return static_cast<ResponseType&>( *dequeued_ev );
            }
            catch ( ExceptionResponse& )
            {
              Event::dec_ref( *dequeued_ev );
              throw;
            }
          }
          break;

          default:
          {
            throw YIELD::platform::Exception
                  (
                    "ResponseQueue::dequeue: received unexpected, " \
                    "non-exception event type"
                  );
          }
          break;
        }
      }

      void enqueue( Event& ev )
      {
        SynchronizedSTLQueue<Event*>::enqueue( &ev );
      }

      ResponseType& timed_dequeue( const YIELD::platform::Time& timeout )
      {
        Event* dequeued_ev 
          = SynchronizedSTLQueue<Event*>::timed_dequeue( timeout );

        if ( dequeued_ev != NULL )
        {
          switch ( dequeued_ev->get_type_id() )
          {
            case ResponseType::TYPE_ID:
            {
              return static_cast<ResponseType&>( *dequeued_ev );
            }
            break;

            case ExceptionResponse::TYPE_ID:
            {
              try
              {
                static_cast<ExceptionResponse*>( dequeued_ev )
                  ->throwStackClone();
              }
              catch ( ExceptionResponse& )
              {
                Event::dec_ref( *dequeued_ev );
                throw;
              }

              throw YIELD::platform::Exception( "should never reach this point" );
            }
            break;

            default:
            {
              throw YIELD::platform::Exception
                   (
                     "ResponseQueue::dequeue: received unexpected, " \
                     " non-exception event type"
                   );
            }
            break;
          }
        }
        else
          throw YIELD::platform::Exception( "ResponseQueue::dequeue: timed out" );
      }

      // EventTarget
      void send( Event& ev )
      {
        enqueue( ev );
      }
    };

    template <class ResponseType>
    class auto_ResponseQueue
      : public yidl::runtime::auto_Object< ResponseQueue<ResponseType> >
    {
    public:
      auto_ResponseQueue( ResponseQueue<ResponseType>* response_queue )
        : yidl::runtime::auto_Object< ResponseQueue<ResponseType> >
         (
           response_queue
         )
      { }
    };


    template 
    <
      typename SampleType, 
      size_t ArraySize, 
      class LockType = YIELD::platform::NOPLock
    >
    class Sampler
    {
    public:
      Sampler()
      {
        std::memset( samples, 0, sizeof( samples ) );
        samples_pos = samples_count = 0;
        min = static_cast<SampleType>( ULONG_MAX ); max = 0; total = 0;
      }

      void clear()
      {
        lock.acquire();
        samples_count = 0;
        lock.release();
      }

      SampleType get_max() const
      {
        return max;
      }

      SampleType get_mean()
      {
        lock.acquire();
        SampleType mean;

        if ( samples_count > 0 )
          mean = static_cast<SampleType>
                 (
                   static_cast<double>( total ) /
                   static_cast<double>( samples_count )
                 );
        else
          mean = 0;

        lock.release();
        return mean;
      }

      SampleType get_median()
      {
        lock.acquire();
        SampleType median;

        if ( samples_count > 0 )
        {
          std::sort( samples, samples + samples_count );
          size_t sc_div_2 = samples_count / 2;
          if ( samples_count % 2 == 1 )
            median = samples[sc_div_2];
          else
          {
            SampleType median_temp = samples[sc_div_2] + samples[sc_div_2-1];
            if ( median_temp > 0 )
              median = static_cast<SampleType>
                       (
                         static_cast<double>( median_temp ) / 2.0
                       );
            else
              median = 0;
          }
        }
        else
          median = 0;

        lock.release();
        return median;
      }

      SampleType get_min() const
      {
        return min;
      }

      SampleType get_percentile( double percentile )
      {
        if ( percentile > 0 && percentile < 100 )
        {
          lock.acquire();
          SampleType value;

          if ( samples_count > 0 )
          {
            std::sort( samples, samples + samples_count );
            value =
              samples[static_cast<size_t>( percentile *
                static_cast<double>( samples_count ) )];
          }
          else
            value = 0;

          lock.release();
          return value;
        }
        else
          return 0;
      }

      uint32_t get_samples_count() const
      {
        return samples_count;
      }

      void set_next_sample( SampleType sample )
      {
        if ( lock.try_acquire() )
        {
          samples[samples_pos] = sample;
          samples_pos = ( samples_pos + 1 ) % ArraySize;
          if ( samples_count < ArraySize ) samples_count++;

          if ( sample < min )
            min = sample;
          if ( sample > max )
            max = sample;
          total += sample;

          lock.release();
        }
      }

    protected:
      SampleType samples[ArraySize+1], min, max; SampleType total;
      uint32_t samples_pos, samples_count;
      LockType lock;
    };


    class Stage : public EventTarget
    {
    public:
      class StartupEvent : public Event
      {
      public:
        StartupEvent( auto_Stage stage )
          : stage( stage )
        { }

        auto_Stage get_stage() { return stage; }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( Stage::StartupEvent, 104 );

        // yidl::runtime::MarshallableObject
        void marshal( yidl::runtime::Marshaller& ) const { }
        void unmarshal( yidl::runtime::Unmarshaller& ) { }

      private:
        auto_Stage stage;
      };


      class ShutdownEvent : public Event
      {
      public:
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( Stage::ShutdownEvent, 105 );

        // yidl::runtime::MarshallableObject
        void marshal( yidl::runtime::Marshaller& ) const { }
        void unmarshal( yidl::runtime::Unmarshaller& ) { }
      };


      double get_arrival_rate_s() const { return arrival_rate_s; }
      double get_rho() const { return rho; }
      double get_service_rate_s() const { return service_rate_s; }
      uint8_t get_stage_id() const { return id; }
      const char* get_stage_name() const { return name; }
      virtual auto_EventHandler get_event_handler() = 0;
      virtual bool visit() = 0;
      virtual bool visit( const YIELD::platform::Time& timeout ) = 0;
      virtual void visit( Event& event ) = 0;

    protected:
      Stage( const char* name );
      virtual ~Stage();

      Sampler<uint64_t, 1024, YIELD::platform::Mutex>
        event_processing_time_sampler;
      uint32_t event_queue_length, event_queue_arrival_count;
  #ifdef YIELD_PLATFORM_HAVE_PERFORMANCE_COUNTERS
      YIELD::platform::auto_PerformanceCounterSet performance_counters;
      uint64_t performance_counter_totals[2];
  #endif

    private:
      class StatisticsTimer;

      const char* name;
      uint8_t id;
      double arrival_rate_s, rho, service_rate_s;

      friend class StageGroup;
      void set_stage_id( uint8_t stage_id ) { this->id = stage_id; }
    };


    template <class EventHandlerType, class EventQueueType, class LockType>
    class StageImpl : public Stage
    {
    public:
      StageImpl
      (
        yidl::runtime::auto_Object<EventHandlerType> event_handler,
        yidl::runtime::auto_Object<EventQueueType> event_queue
      )
        : Stage( event_handler->get_event_handler_name() ),
          event_handler( event_handler ),
          event_queue( event_queue )
      { }

      // EventTarget
      void send( Event& event )
      {
      /*
        Stage* running_stage = static_cast<Stage*>
          ( Thread::getTLS( running_stage_tls_key ) );
        if ( running_stage != NULL )
        {
          running_stage->send_counters_lock.acquire();
          std::map<const char*, uint64_t>::iterator send_counter_i
            = running_stage->send_counters.find( this->get_stage_name() );
          if ( send_counter_i != running_stage->send_counters.end() )
            send_counter_i->second++;
          else
            running_stage->send_counters.insert
              ( std::make_pair( this->get_stage_name(), 1 ) );
          running_stage->send_counters_lock.release();
        }
        */

        event.set_next_stage( this );

        ++event_queue_length;
        ++event_queue_arrival_count;

        if ( event_queue->enqueue( event ) )
          return;
        else
        {
          std::cerr << get_stage_name() << ": event queue full, stopping.";
          DebugBreak();
        }
      }

      // Stage
      const char* get_stage_name() const
      {
          return event_handler->get_event_handler_name();
      }

      auto_EventHandler get_event_handler()
      {
        return event_handler->inc_ref();
      }

      bool visit()
      {
        lock.acquire();

        Event* event = event_queue->dequeue();
        if ( event != NULL )
        {
          --event_queue_length;
          callEventHandler( *event );

          for ( ;; )
          {
            event = event_queue->try_dequeue();
            if ( event != NULL )
            {
              --event_queue_length;
              callEventHandler( *event );
            }
            else
              break;
          }

          lock.release();

          return true;
        }
        else
        {
          lock.release();
          return false;
        }
      }

      bool visit( const YIELD::platform::Time& timeout )
      {
        if ( lock.try_acquire() )
        {
          Event* event = event_queue->timed_dequeue( timeout );
          if ( event != NULL )
          {
            --event_queue_length;
            callEventHandler( *event );

            for ( ;; )
            {
              event = event_queue->try_dequeue();
              if ( event != NULL )
              {
                --event_queue_length;
                callEventHandler( *event );
              }
              else
                break;
            }

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

      void visit( Event& event )
      {
        --event_queue_length;
        lock.acquire();
        callEventHandler( event );
        lock.release();
      }

    private:
      yidl::runtime::auto_Object<EventHandlerType> event_handler;
      yidl::runtime::auto_Object<EventQueueType> event_queue;

      LockType lock;

      void callEventHandler( Event& event )
      {
        YIELD::platform::Time start_time;

  #ifdef YIELD_PLATFORM_HAVE_PERFORMANCE_COUNTERS
        performance_counters->startCounting();
  #endif

        event_handler->handleEvent( event );

  #ifdef YIELD_PLATFORM_HAVE_PERFORMANCE_COUNTERS
        uint64_t performance_counter_counts[2];
        performance_counters->stopCounting( performance_counter_counts );
        performance_counter_totals[0] += performance_counter_counts[0];
        performance_counter_totals[1] += performance_counter_counts[1];
  #endif

        YIELD::platform::Time event_processing_time;
        event_processing_time -= start_time;
        if ( event_processing_time < 10.0 )
        {
          event_processing_time_sampler.
            set_next_sample( event_processing_time );
        }
      }
    };


    class StageGroup : public yidl::runtime::Object
    {
    public:
      template <class EventHandlerType>
      auto_Stage createStage
      (
        yidl::runtime::auto_Object<EventHandlerType> event_handler
      )
      {
        return createStage
        (
          static_cast<EventHandler*>( event_handler.release() )
        );
      }

      template <class EventHandlerType>
      auto_Stage createStage
      (
        yidl::runtime::auto_Object<EventHandlerType> event_handler,
        int16_t thread_count
      )
      {
        return createStage
        (
          static_cast<EventHandler*>
          (
            event_handler.release()
          ),
          thread_count
        );
      }

      virtual auto_Stage createStage
      (
        yidl::runtime::auto_Object<EventHandler> event_handler,
        int16_t thread_count = 1
      ) = 0;

      Stage** get_stages() { return &stages[0]; }

    protected:
      StageGroup();
      virtual ~StageGroup();

      void addStage( auto_Stage stage );

    private:
      Stage* stages[YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX];
    };

    typedef yidl::runtime::auto_Object<StageGroup> auto_StageGroup;


    template <class StageGroupType> // CRTP
    class StageGroupImpl : public StageGroup
    {
    public:
      template <class EventHandlerType>
      auto_Stage createStage
      (
        yidl::runtime::auto_Object<EventHandlerType> event_handler
      )
      {
        return static_cast<StageGroupType*>( this )
          ->createStage<EventHandlerType, EventQueue>( event_handler, 1 );
      }

      template <class EventHandlerType>
      auto_Stage createStage
      (
        yidl::runtime::auto_Object<EventHandlerType> event_handler,
        int16_t thread_count
      )
      {
        return static_cast<StageGroupType*>( this )
          ->createStage<EventHandlerType>( event_handler, thread_count );
      }

      // StageGroup
      auto_Stage createStage
      (
        yidl::runtime::auto_Object<EventHandler> event_handler,
        int16_t thread_count = 1
      )
      {
        return createStage<EventHandler>( event_handler, thread_count );
      }

    protected:
      StageGroupImpl() { }
      virtual ~StageGroupImpl() { }
    };


    class ColorStageGroup : public StageGroupImpl<ColorStageGroup>
    {
    public:
      ColorStageGroup
      (
        const char* name = "Main stage group",
        uint16_t start_logical_processor_i = 0,
        int16_t thread_count = -1
      );

      ~ColorStageGroup();

      template <class EventHandlerType>
      auto_Stage createStage
      (
        yidl::runtime::auto_Object<EventHandlerType> event_handler,
        int16_t thread_count
      )
      {
        auto_Stage stage;
        if ( thread_count == 1 )
        {
          stage
            = new StageImpl
                  <
                    EventHandlerType,
                    STLEventQueue,
                    YIELD::platform::NOPLock
                  >( event_handler, event_queue );
        }
        else
        {
          stage
            = new StageImpl
                  <
                    EventHandlerType,
                    STLEventQueue,
                    YIELD::platform::Mutex
                  >( event_handler, event_queue );
        }

        // TODO: check flags before sending this
        //event_handler->handleEvent( *( new Stage::StartupEvent( stage ) ) );

        this->addStage( stage );

        return stage;
      }

    private:
      auto_STLEventQueue event_queue;

      class Thread;
      std::vector<Thread*> threads;
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
    class PollingStageGroup
      : public StageGroupImpl< PollingStageGroup<VisitPolicyType> >
    {
    public:
      PollingStageGroup
      (
        const char* name = "Main stage group",
        uint16_t start_logical_processor_i = 0,
        int16_t thread_count = -1,
        bool use_thread_local_event_queues = false
      );

      template <class EventHandlerType>
      auto_Stage createStage
      (
        yidl::runtime::auto_Object<EventHandlerType> event_handler,
        int16_t thread_count
      )
      {
        auto_Stage stage;
        if ( use_thread_local_event_queues )
        {
          if ( thread_count == 1 )
          {
            stage
              = new StageImpl
                    <
                      EventHandlerType,
                      ThreadLocalEventQueue,
                      YIELD::platform::Mutex
                    >( event_handler, new ThreadLocalEventQueue );
          }
          else
          {
            stage
              = new StageImpl
                    <
                      EventHandlerType,
                      ThreadLocalEventQueue,
                      YIELD::platform::NOPLock
                    >( event_handler, new ThreadLocalEventQueue );
          }
        }
        else
        {
          if ( thread_count == 1 )
          {
            stage
              = new StageImpl
                    <
                      EventHandlerType,
                      STLEventQueue,
                      YIELD::platform::Mutex
                    >( event_handler, new STLEventQueue );
          }
          else
          {
            stage
              = new StageImpl
                    <
                      EventHandlerType,
                      STLEventQueue,
                      YIELD::platform::NOPLock
                    >( event_handler, new STLEventQueue );
          }
        }

        // TODO: check flags before sending this
        //event_handler->handleEvent( *( new Stage::StartupEvent( stage ) ) );

        this->addStage( stage );

        return stage;
      }

    private:
      ~PollingStageGroup();

      bool use_thread_local_event_queues;

      class Thread;
      std::vector<Thread*> threads;
    };


    class DBRVisitPolicy : public VisitPolicy
    {
    public:
      DBRVisitPolicy( Stage** stages )
        : VisitPolicy( stages )
      {
        next_stage_i = YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX;
        memset( sorted_stages, 0, sizeof( sorted_stages ) );
      }

      // VisitPolicy
      inline Stage* getNextStageToVisit( bool last_visit_was_successful )
      {
        if ( last_visit_was_successful )
        {
          next_stage_i = 0;
          return sorted_stages[0];
        }
        else if ( next_stage_i < YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX )
          return sorted_stages[next_stage_i++];
        else
        {
          memcpy_s
          (
            sorted_stages,
            sizeof( sorted_stages ),
            stages,
            sizeof( sorted_stages )
          );

          std::sort
          (
            &sorted_stages[0],
            &sorted_stages[YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX-1],
            compare_stages()
          );

          next_stage_i = 0;

          return sorted_stages[0];
        }
      }

    private:
      uint8_t next_stage_i;
      Stage* sorted_stages[YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX];

      struct compare_stages : public std::binary_function<Stage*, Stage*, bool>
      {
        bool operator()( Stage* left, Stage* right )
        {
          if ( left != NULL )
          {
            if ( right != NULL )
              return left->get_service_rate_s() < right->get_service_rate_s();
            else
              return true;
          }
          else
            return false;
        }
      };
    };


    class MG1VisitPolicy : public VisitPolicy
    {
    public:
      MG1VisitPolicy( Stage** stages );

      // VisitPolicy
      inline Stage* getNextStageToVisit( bool )
      {
        if ( polling_table_pos < YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE )
          return stages[polling_table[polling_table_pos++]];
        else
        {
          populatePollingTable();
          polling_table_pos = 0;
          return stages[polling_table[polling_table_pos++]];
        }
      }

    private:
      uint8_t polling_table[YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE];
      uint32_t polling_table_pos;
      uint32_t golden_ratio_circle[YIELD_CONCURRENCY_MG1_POLLING_TABLE_SIZE];
      // These are only used in populating the polling table,
      // but we have to keep the values to use in smoothing
      double last_rhos[YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX];

      bool populatePollingTable();
    };


    class SRPTVisitPolicy : public VisitPolicy
    {
    public:
      SRPTVisitPolicy( Stage** stages ) : VisitPolicy( stages )
      {
        next_stage_i = 0;
      }

      // VisitPolicy
      inline Stage* getNextStageToVisit( bool last_visit_was_successful )
      {
        if ( last_visit_was_successful )
          next_stage_i = 0;
        else
          next_stage_i = ( next_stage_i + 1 ) % YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX;

        return stages[next_stage_i];
      }

    private:
      unsigned char next_stage_i;
    };


    class WavefrontVisitPolicy : public VisitPolicy
    {
    public:
      WavefrontVisitPolicy( Stage** stages )
        : VisitPolicy( stages )
      {
        forward = true;
        next_stage_i = 0;
      }

      // VisitPolicy
      inline Stage* getNextStageToVisit( bool )
      {
        if ( forward )
        {
          if ( next_stage_i < YIELD_CONCURRENCY_STAGES_PER_GROUP_MAX - 1 )
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


    class SEDAStageGroup : public StageGroupImpl<SEDAStageGroup>
    {
    public:
      SEDAStageGroup() { }

      template <class EventHandlerType>
      auto_Stage createStage
      (
        yidl::runtime::auto_Object<EventHandlerType> event_handler,
        int16_t thread_count
      )
      {
        if ( thread_count <= 0 )
          thread_count
          = YIELD::platform::Machine::getOnlinePhysicalProcessorCount();

        auto_Stage stage;
        if ( thread_count == 1 )
        {
          stage
            = new StageImpl
                  <
                    EventHandlerType,
                    STLEventQueue,
                    YIELD::platform::Mutex
                  >( event_handler, new STLEventQueue );
        }
        else
        {
          stage
            = new StageImpl
                  <
                    EventHandlerType,
                    STLEventQueue,
                    YIELD::platform::NOPLock
                  >( event_handler, new STLEventQueue );
        }

        // TODO: check flags before sending this
        //event_handler->handleEvent( *( new Stage::StartupEvent( stage ) ) );

        this->addStage( stage );

        startThreads( stage, thread_count );

        return stage;
      }

    protected:
      virtual ~SEDAStageGroup();

    private:
      class Thread;
      std::vector<Thread*> threads;
      void startThreads( auto_Stage stage, int16_t thread_count );
    };
  };
};


#endif
