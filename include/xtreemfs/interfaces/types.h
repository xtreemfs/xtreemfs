#ifndef _1136993229_H_
#define _1136993229_H_


#include "yidl.h"


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {

      class StringSet
        : public ::yidl::runtime::Sequence,
          public vector<string>
      {
      public:
        StringSet() { }
        StringSet( const string& first_value ) { vector<string>::push_back( first_value ); }
        StringSet( size_type size ) : vector<string>( size ) { }
        virtual ~StringSet() { }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( StringSet, 2010030917 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          size_type value_i_max = size();
          for ( size_type value_i = 0; value_i < value_i_max; value_i++ )
          {
            marshaller.write( ::yidl::runtime::Marshaller::Key(), ( *this )[value_i] );
          }
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          string value;
          unmarshaller.read( ::yidl::runtime::Marshaller::Key(), value );
          push_back( value );
        }

        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };

      class UserCredentials : public ::yidl::runtime::Struct
      {
      public:
        UserCredentials() { }

        UserCredentials
        (
          const string& user_id,
          const org::xtreemfs::interfaces::StringSet& group_ids,
          const string& password
        )
          : user_id( user_id ), group_ids( group_ids ), password( password )
        { }

        virtual ~UserCredentials() {  }

        const string& get_user_id() const { return user_id; }
        const org::xtreemfs::interfaces::StringSet& get_group_ids() const { return group_ids; }
        const string& get_password() const { return password; }
        void set_user_id( const string& user_id ) { this->user_id = user_id; }
        void set_group_ids( const org::xtreemfs::interfaces::StringSet&  group_ids ) { this->group_ids = group_ids; }
        void set_password( const string& password ) { this->password = password; }

        bool operator==( const UserCredentials& other ) const
        {
          return user_id == other.user_id
                 &&
                 group_ids == other.group_ids
                 &&
                 password == other.password;
        }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( UserCredentials, 2010030918 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "user_id", 0 ), user_id );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "group_ids", 0 ), group_ids );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "password", 0 ), password );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "user_id", 0 ), user_id );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "group_ids", 0 ), group_ids );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "password", 0 ), password );
        }

      protected:
        string user_id;
        org::xtreemfs::interfaces::StringSet group_ids;
        string password;
      };

    };
  };
};
#endif
