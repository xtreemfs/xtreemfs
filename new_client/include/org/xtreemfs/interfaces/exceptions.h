#ifndef _75469114364_H
#define _75469114364_H

#include "yield/arch.h"

#include <map>


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
  
      #ifndef ORG_XTREEMFS_INTERFACES_EXCEPTIONS_INTERFACE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACE_PARENT_CLASS
      #elif defined( ORG_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_INTERFACE_PARENT_CLASS ORG_INTERFACE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_INTERFACE_PARENT_CLASS YIELD::EventHandler
      #endif
      #endif
  
      #ifndef ORG_XTREEMFS_INTERFACES_EXCEPTIONS_REQUEST_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_REQUEST_PARENT_CLASS ORG_XTREEMFS_INTERFACES_REQUEST_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_REQUEST_PARENT_CLASS ORG_XTREEMFS_REQUEST_PARENT_CLASS
      #elif defined( ORG_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_REQUEST_PARENT_CLASS ORG_REQUEST_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_REQUEST_PARENT_CLASS YIELD::Request
      #endif
      #endif
  
      #ifndef ORG_XTREEMFS_INTERFACES_EXCEPTIONS_RESPONSE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_RESPONSE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_RESPONSE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_RESPONSE_PARENT_CLASS ORG_XTREEMFS_RESPONSE_PARENT_CLASS
      #elif defined( ORG_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_RESPONSE_PARENT_CLASS ORG_RESPONSE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_RESPONSE_PARENT_CLASS YIELD::Response
      #endif
      #endif
  
      #ifndef ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_EVENT_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_EVENT_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_EVENT_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_EVENT_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_EVENT_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS ORG_EXCEPTION_EVENT_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS YIELD::ExceptionEvent
      #endif
      #endif
  
  
  
      class Exceptions : public ORG_XTREEMFS_INTERFACES_EXCEPTIONS_INTERFACE_PARENT_CLASS
      {
      public:
        Exceptions() { }
        virtual ~Exceptions() { }  // Request/response pair Event type definitions for the operations in Exceptions
  
        class ProtocolException : public ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS
        {
        public:
          ProtocolException() : accept_stat( 0 ), error_code( 0 ) { }
        ProtocolException( uint32_t accept_stat, uint32_t error_code, const std::string& stack_trace ) : accept_stat( accept_stat ), error_code( error_code ), stack_trace( stack_trace ) { }
        ProtocolException( uint32_t accept_stat, uint32_t error_code, const char* stack_trace, size_t stack_trace_len ) : accept_stat( accept_stat ), error_code( error_code ), stack_trace( stack_trace, stack_trace_len ) { }
          ProtocolException( const char* what ) : ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS( what ) { }
          virtual ~ProtocolException() throw() { }
  
        void set_accept_stat( uint32_t accept_stat ) { this->accept_stat = accept_stat; }
        uint32_t get_accept_stat() const { return accept_stat; }
        void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
        uint32_t get_error_code() const { return error_code; }
        void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
        void set_stack_trace( const char* stack_trace, size_t stack_trace_len = 0 ) { this->stack_trace.assign( stack_trace, ( stack_trace_len != 0 ) ? stack_trace_len : std::strlen( stack_trace ) ); }
        const std::string& get_stack_trace() const { return stack_trace; }
  
          // YIELD::RTTI
          TYPE_INFO( EXCEPTION_EVENT, "org::xtreemfs::interfaces::Exceptions::ProtocolException", 1268393568UL );
  
          // YIELD::ExceptionEvent
          virtual ExceptionEvent* clone() const { return new ProtocolException( accept_stat, error_code, stack_trace); }
          virtual void throwStackClone() const { throw ProtocolException( accept_stat, error_code, stack_trace); }
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "accept_stat" ), accept_stat ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "error_code" ), error_code ); output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { accept_stat = input_stream.readUint32( YIELD::StructuredStream::Declaration( "accept_stat" ) ); error_code = input_stream.readUint32( YIELD::StructuredStream::Declaration( "error_code" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
  
      protected:
        uint32_t accept_stat;
        uint32_t error_code;
        std::string stack_trace;
        };
  
        class errnoException : public ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS
        {
        public:
          errnoException() : error_code( 0 ) { }
        errnoException( uint32_t error_code, const std::string& errro_message, const std::string& stack_trace ) : error_code( error_code ), errro_message( errro_message ), stack_trace( stack_trace ) { }
        errnoException( uint32_t error_code, const char* errro_message, size_t errro_message_len, const char* stack_trace, size_t stack_trace_len ) : error_code( error_code ), errro_message( errro_message, errro_message_len ), stack_trace( stack_trace, stack_trace_len ) { }
          errnoException( const char* what ) : ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS( what ) { }
          virtual ~errnoException() throw() { }
  
        void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
        uint32_t get_error_code() const { return error_code; }
        void set_errro_message( const std::string& errro_message ) { set_errro_message( errro_message.c_str(), errro_message.size() ); }
        void set_errro_message( const char* errro_message, size_t errro_message_len = 0 ) { this->errro_message.assign( errro_message, ( errro_message_len != 0 ) ? errro_message_len : std::strlen( errro_message ) ); }
        const std::string& get_errro_message() const { return errro_message; }
        void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
        void set_stack_trace( const char* stack_trace, size_t stack_trace_len = 0 ) { this->stack_trace.assign( stack_trace, ( stack_trace_len != 0 ) ? stack_trace_len : std::strlen( stack_trace ) ); }
        const std::string& get_stack_trace() const { return stack_trace; }
  
          // YIELD::RTTI
          TYPE_INFO( EXCEPTION_EVENT, "org::xtreemfs::interfaces::Exceptions::errnoException", 405273943UL );
  
          // YIELD::ExceptionEvent
          virtual ExceptionEvent* clone() const { return new errnoException( error_code, errro_message, stack_trace); }
          virtual void throwStackClone() const { throw errnoException( error_code, errro_message, stack_trace); }
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "error_code" ), error_code ); output_stream.writeString( YIELD::StructuredStream::Declaration( "errro_message" ), errro_message ); output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { error_code = input_stream.readUint32( YIELD::StructuredStream::Declaration( "error_code" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "errro_message" ), errro_message ); input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
  
      protected:
        uint32_t error_code;
        std::string errro_message;
        std::string stack_trace;
        };
  
        class RedirectException : public ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS
        {
        public:
          RedirectException() { }
        RedirectException( const std::string& to_uuid ) : to_uuid( to_uuid ) { }
        RedirectException( const char* to_uuid, size_t to_uuid_len ) : to_uuid( to_uuid, to_uuid_len ) { }
          RedirectException( const char* what ) : ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS( what ) { }
          virtual ~RedirectException() throw() { }
  
        void set_to_uuid( const std::string& to_uuid ) { set_to_uuid( to_uuid.c_str(), to_uuid.size() ); }
        void set_to_uuid( const char* to_uuid, size_t to_uuid_len = 0 ) { this->to_uuid.assign( to_uuid, ( to_uuid_len != 0 ) ? to_uuid_len : std::strlen( to_uuid ) ); }
        const std::string& get_to_uuid() const { return to_uuid; }
  
          // YIELD::RTTI
          TYPE_INFO( EXCEPTION_EVENT, "org::xtreemfs::interfaces::Exceptions::RedirectException", 3273969329UL );
  
          // YIELD::ExceptionEvent
          virtual ExceptionEvent* clone() const { return new RedirectException( to_uuid); }
          virtual void throwStackClone() const { throw RedirectException( to_uuid); }
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "to_uuid" ), to_uuid ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "to_uuid" ), to_uuid ); }
  
      protected:
        std::string to_uuid;
        };
  
        class MRCException : public ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS
        {
        public:
          MRCException() : error_code( 0 ) { }
        MRCException( uint32_t error_code, const std::string& errro_message, const std::string& stack_trace ) : error_code( error_code ), errro_message( errro_message ), stack_trace( stack_trace ) { }
        MRCException( uint32_t error_code, const char* errro_message, size_t errro_message_len, const char* stack_trace, size_t stack_trace_len ) : error_code( error_code ), errro_message( errro_message, errro_message_len ), stack_trace( stack_trace, stack_trace_len ) { }
          MRCException( const char* what ) : ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS( what ) { }
          virtual ~MRCException() throw() { }
  
        void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
        uint32_t get_error_code() const { return error_code; }
        void set_errro_message( const std::string& errro_message ) { set_errro_message( errro_message.c_str(), errro_message.size() ); }
        void set_errro_message( const char* errro_message, size_t errro_message_len = 0 ) { this->errro_message.assign( errro_message, ( errro_message_len != 0 ) ? errro_message_len : std::strlen( errro_message ) ); }
        const std::string& get_errro_message() const { return errro_message; }
        void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
        void set_stack_trace( const char* stack_trace, size_t stack_trace_len = 0 ) { this->stack_trace.assign( stack_trace, ( stack_trace_len != 0 ) ? stack_trace_len : std::strlen( stack_trace ) ); }
        const std::string& get_stack_trace() const { return stack_trace; }
  
          // YIELD::RTTI
          TYPE_INFO( EXCEPTION_EVENT, "org::xtreemfs::interfaces::Exceptions::MRCException", 32377859UL );
  
          // YIELD::ExceptionEvent
          virtual ExceptionEvent* clone() const { return new MRCException( error_code, errro_message, stack_trace); }
          virtual void throwStackClone() const { throw MRCException( error_code, errro_message, stack_trace); }
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "error_code" ), error_code ); output_stream.writeString( YIELD::StructuredStream::Declaration( "errro_message" ), errro_message ); output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { error_code = input_stream.readUint32( YIELD::StructuredStream::Declaration( "error_code" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "errro_message" ), errro_message ); input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
  
      protected:
        uint32_t error_code;
        std::string errro_message;
        std::string stack_trace;
        };
  
        class OSDException : public ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS
        {
        public:
          OSDException() : error_code( 0 ) { }
        OSDException( uint32_t error_code, const std::string& errro_message, const std::string& stack_trace ) : error_code( error_code ), errro_message( errro_message ), stack_trace( stack_trace ) { }
        OSDException( uint32_t error_code, const char* errro_message, size_t errro_message_len, const char* stack_trace, size_t stack_trace_len ) : error_code( error_code ), errro_message( errro_message, errro_message_len ), stack_trace( stack_trace, stack_trace_len ) { }
          OSDException( const char* what ) : ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS( what ) { }
          virtual ~OSDException() throw() { }
  
        void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
        uint32_t get_error_code() const { return error_code; }
        void set_errro_message( const std::string& errro_message ) { set_errro_message( errro_message.c_str(), errro_message.size() ); }
        void set_errro_message( const char* errro_message, size_t errro_message_len = 0 ) { this->errro_message.assign( errro_message, ( errro_message_len != 0 ) ? errro_message_len : std::strlen( errro_message ) ); }
        const std::string& get_errro_message() const { return errro_message; }
        void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
        void set_stack_trace( const char* stack_trace, size_t stack_trace_len = 0 ) { this->stack_trace.assign( stack_trace, ( stack_trace_len != 0 ) ? stack_trace_len : std::strlen( stack_trace ) ); }
        const std::string& get_stack_trace() const { return stack_trace; }
  
          // YIELD::RTTI
          TYPE_INFO( EXCEPTION_EVENT, "org::xtreemfs::interfaces::Exceptions::OSDException", 3594163714UL );
  
          // YIELD::ExceptionEvent
          virtual ExceptionEvent* clone() const { return new OSDException( error_code, errro_message, stack_trace); }
          virtual void throwStackClone() const { throw OSDException( error_code, errro_message, stack_trace); }
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "error_code" ), error_code ); output_stream.writeString( YIELD::StructuredStream::Declaration( "errro_message" ), errro_message ); output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { error_code = input_stream.readUint32( YIELD::StructuredStream::Declaration( "error_code" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "errro_message" ), errro_message ); input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
  
      protected:
        uint32_t error_code;
        std::string errro_message;
        std::string stack_trace;
        };
  
        class ConcurrentModificationException : public ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS
        {
        public:
          ConcurrentModificationException() { }
        ConcurrentModificationException( const std::string& stack_trace ) : stack_trace( stack_trace ) { }
        ConcurrentModificationException( const char* stack_trace, size_t stack_trace_len ) : stack_trace( stack_trace, stack_trace_len ) { }
          ConcurrentModificationException( const char* what ) : ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS( what ) { }
          virtual ~ConcurrentModificationException() throw() { }
  
        void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
        void set_stack_trace( const char* stack_trace, size_t stack_trace_len = 0 ) { this->stack_trace.assign( stack_trace, ( stack_trace_len != 0 ) ? stack_trace_len : std::strlen( stack_trace ) ); }
        const std::string& get_stack_trace() const { return stack_trace; }
  
          // YIELD::RTTI
          TYPE_INFO( EXCEPTION_EVENT, "org::xtreemfs::interfaces::Exceptions::ConcurrentModificationException", 769608203UL );
  
          // YIELD::ExceptionEvent
          virtual ExceptionEvent* clone() const { return new ConcurrentModificationException( stack_trace); }
          virtual void throwStackClone() const { throw ConcurrentModificationException( stack_trace); }
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
  
      protected:
        std::string stack_trace;
        };
  
        class InvalidArgumentException : public ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS
        {
        public:
          InvalidArgumentException() { }
        InvalidArgumentException( const std::string& error_message ) : error_message( error_message ) { }
        InvalidArgumentException( const char* error_message, size_t error_message_len ) : error_message( error_message, error_message_len ) { }
          InvalidArgumentException( const char* what ) : ORG_XTREEMFS_INTERFACES_EXCEPTIONS_EXCEPTION_EVENT_PARENT_CLASS( what ) { }
          virtual ~InvalidArgumentException() throw() { }
  
        void set_error_message( const std::string& error_message ) { set_error_message( error_message.c_str(), error_message.size() ); }
        void set_error_message( const char* error_message, size_t error_message_len = 0 ) { this->error_message.assign( error_message, ( error_message_len != 0 ) ? error_message_len : std::strlen( error_message ) ); }
        const std::string& get_error_message() const { return error_message; }
  
          // YIELD::RTTI
          TYPE_INFO( EXCEPTION_EVENT, "org::xtreemfs::interfaces::Exceptions::InvalidArgumentException", 690678936UL );
  
          // YIELD::ExceptionEvent
          virtual ExceptionEvent* clone() const { return new InvalidArgumentException( error_message); }
          virtual void throwStackClone() const { throw InvalidArgumentException( error_message); }
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); }
  
      protected:
        std::string error_message;
        };
  
  
  
        void registerSerializableFactories( YIELD::SerializableFactories& serializable_factories )
        {
          serializable_factories.registerSerializableFactory( 1268393568UL, new YIELD::SerializableFactoryImpl<ProtocolException> );
          serializable_factories.registerSerializableFactory( 405273943UL, new YIELD::SerializableFactoryImpl<errnoException> );
          serializable_factories.registerSerializableFactory( 3273969329UL, new YIELD::SerializableFactoryImpl<RedirectException> );
          serializable_factories.registerSerializableFactory( 32377859UL, new YIELD::SerializableFactoryImpl<MRCException> );
          serializable_factories.registerSerializableFactory( 3594163714UL, new YIELD::SerializableFactoryImpl<OSDException> );
          serializable_factories.registerSerializableFactory( 769608203UL, new YIELD::SerializableFactoryImpl<ConcurrentModificationException> );
          serializable_factories.registerSerializableFactory( 690678936UL, new YIELD::SerializableFactoryImpl<InvalidArgumentException> );
        }
  
  
        // EventHandler
        virtual const char* getEventHandlerName() const { return "Exceptions"; }    virtual void handleEvent( YIELD::Event& ev ) { YIELD::EventHandler::handleUnknownEvent( ev ); }
  
  
      protected:
      };
  
  
  
    };
  
  
  
  };
  
  

};

#endif
