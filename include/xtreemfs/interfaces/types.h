#ifndef _1256982394_H_
#define _1256982394_H_


#include "yidl.h"
#include <string>
#include <vector>


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
  
      class StringSet
        : public ::yidl::runtime::Sequence,
          public std::vector<std::string>
      {
      public:
        StringSet() { }
        StringSet( const std::string& first_value ) { std::vector<std::string>::push_back( first_value ); }
        StringSet( size_type size ) : std::vector<std::string>( size ) { }
        virtual ~StringSet() { }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( StringSet, 2010030315 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          size_type value_i_max = size();
          for ( size_type value_i = 0; value_i < value_i_max; value_i++ )
          {
            marshaller.write( "value", 0, ( *this )[value_i] );
          }
        }
  
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          std::string value;
          unmarshaller.read( "value", 0, value );
          push_back( value );
        }
  
        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };
  
      class UserCredentials : public ::yidl::runtime::Struct
      {
      public:
        UserCredentials() { }
        UserCredentials( const std::string& user_id, const org::xtreemfs::interfaces::StringSet& group_ids, const std::string& password ) : user_id( user_id ), group_ids( group_ids ), password( password ) { }
        virtual ~UserCredentials() {  }
  
        const std::string& get_user_id() const { return user_id; }
        const org::xtreemfs::interfaces::StringSet& get_group_ids() const { return group_ids; }
        const std::string& get_password() const { return password; }
        void set_user_id( const std::string& user_id ) { this->user_id = user_id; }
        void set_group_ids( const org::xtreemfs::interfaces::StringSet&  group_ids ) { this->group_ids = group_ids; }
        void set_password( const std::string& password ) { this->password = password; }
  
        bool operator==( const UserCredentials& other ) const { return user_id == other.user_id && group_ids == other.group_ids && password == other.password; }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( UserCredentials, 2010030316 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "user_id", 0, user_id ); marshaller.write( "group_ids", 0, group_ids ); marshaller.write( "password", 0, password ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "user_id", 0, user_id ); unmarshaller.read( "group_ids", 0, group_ids ); unmarshaller.read( "password", 0, password ); }
  
      protected:
        std::string user_id;
        org::xtreemfs::interfaces::StringSet group_ids;
        std::string password;
      };
  
      };
    };
  };
#endif
