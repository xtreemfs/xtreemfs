#ifndef _13609216080_H
#define _13609216080_H

#include "yield/platform.h"
#include <string>
#include <vector>


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
  
      class StringSet : public std::vector<std::string>, public YIELD::Serializable
      {
      public:
        StringSet() { }
        StringSet( const std::string& first_value ) { std::vector<std::string>::push_back( first_value ); }
        StringSet( size_type size ) : std::vector<std::string>( size ) { }
        virtual ~StringSet() { }
  
        // YIELD::RTTI
        TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::StringSet", 1366254439UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { std::string item; input_stream.readString( YIELD::StructuredStream::Declaration( "item" ), item ); push_back( item ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type i_max = size(); for ( size_type i = 0; i < i_max; i++ ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "item" ), ( *this )[size() - 1] ); } }
        size_t getSize() const { return std::vector<std::string>::size(); }
      };
  
      class UserCredentials : public YIELD::Serializable
      {
      public:
        UserCredentials() { }
        UserCredentials( const std::string& user_id, const org::xtreemfs::interfaces::StringSet& group_ids ) : user_id( user_id ), group_ids( group_ids ) { }
        UserCredentials( const char* user_id, size_t user_id_len, const org::xtreemfs::interfaces::StringSet& group_ids ) : user_id( user_id, user_id_len ), group_ids( group_ids ) { }
        virtual ~UserCredentials() { }
  
        void set_user_id( const std::string& user_id ) { set_user_id( user_id.c_str(), user_id.size() ); }
        void set_user_id( const char* user_id, size_t user_id_len = 0 ) { this->user_id.assign( user_id, ( user_id_len != 0 ) ? user_id_len : std::strlen( user_id ) ); }
        const std::string& get_user_id() const { return user_id; }
        void set_group_ids( const org::xtreemfs::interfaces::StringSet&  group_ids ) { this->group_ids = group_ids; }
        const org::xtreemfs::interfaces::StringSet& get_group_ids() const { return group_ids; }
  
        bool operator==( const UserCredentials& other ) const { return user_id == other.user_id && group_ids == other.group_ids; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::UserCredentials", 3975375778UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "user_id" ), user_id ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "group_ids" ), &group_ids ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "user_id" ), user_id ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "group_ids" ), group_ids ); }
  
      protected:
        std::string user_id;
        org::xtreemfs::interfaces::StringSet group_ids;
      };
  
  
  
    };
  
  
  
  };
  
  

};

#endif
