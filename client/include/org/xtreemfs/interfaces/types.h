// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_INTERFACES_TYPES_H
#define ORG_XTREEMFS_INTERFACES_TYPES_H

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
        UserCredentials( const std::string& user_id, const org::xtreemfs::interfaces::StringSet& group_ids, const std::string& password ) : user_id( user_id ), group_ids( group_ids ), password( password ) { }
        UserCredentials( const char* user_id, size_t user_id_len, const org::xtreemfs::interfaces::StringSet& group_ids, const char* password, size_t password_len ) : user_id( user_id, user_id_len ), group_ids( group_ids ), password( password, password_len ) { }
        virtual ~UserCredentials() { }

        void set_user_id( const std::string& user_id ) { set_user_id( user_id.c_str(), user_id.size() ); }
        void set_user_id( const char* user_id, size_t user_id_len = 0 ) { this->user_id.assign( user_id, ( user_id_len != 0 ) ? user_id_len : std::strlen( user_id ) ); }
        const std::string& get_user_id() const { return user_id; }
        void set_group_ids( const org::xtreemfs::interfaces::StringSet&  group_ids ) { this->group_ids = group_ids; }
        const org::xtreemfs::interfaces::StringSet& get_group_ids() const { return group_ids; }
        void set_password( const std::string& password ) { set_password( password.c_str(), password.size() ); }
        void set_password( const char* password, size_t password_len = 0 ) { this->password.assign( password, ( password_len != 0 ) ? password_len : std::strlen( password ) ); }
        const std::string& get_password() const { return password; }

        bool operator==( const UserCredentials& other ) const { return user_id == other.user_id && group_ids == other.group_ids && password == other.password; }

        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::UserCredentials", 3975375778UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "user_id" ), user_id ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "group_ids" ), &group_ids ); input_stream.readString( YIELD::StructuredStream::Declaration( "password" ), password ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "user_id" ), user_id ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "group_ids" ), group_ids ); output_stream.writeString( YIELD::StructuredStream::Declaration( "password" ), password ); }

      protected:
        std::string user_id;
        org::xtreemfs::interfaces::StringSet group_ids;
        std::string password;
      };

      class VivaldiCoordinates : public YIELD::Serializable
      {
      public:
        VivaldiCoordinates() : x_coordinate( 0 ), y_coordinate( 0 ), local_error( 0 ) { }
        VivaldiCoordinates( double x_coordinate, double y_coordinate, double local_error ) : x_coordinate( x_coordinate ), y_coordinate( y_coordinate ), local_error( local_error ) { }
        virtual ~VivaldiCoordinates() { }

        void set_x_coordinate( double x_coordinate ) { this->x_coordinate = x_coordinate; }
        double get_x_coordinate() const { return x_coordinate; }
        void set_y_coordinate( double y_coordinate ) { this->y_coordinate = y_coordinate; }
        double get_y_coordinate() const { return y_coordinate; }
        void set_local_error( double local_error ) { this->local_error = local_error; }
        double get_local_error() const { return local_error; }

        bool operator==( const VivaldiCoordinates& other ) const { return x_coordinate == other.x_coordinate && y_coordinate == other.y_coordinate && local_error == other.local_error; }

        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::VivaldiCoordinates", 3973037335UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { x_coordinate = input_stream.readDouble( YIELD::StructuredStream::Declaration( "x_coordinate" ) ); y_coordinate = input_stream.readDouble( YIELD::StructuredStream::Declaration( "y_coordinate" ) ); local_error = input_stream.readDouble( YIELD::StructuredStream::Declaration( "local_error" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeDouble( YIELD::StructuredStream::Declaration( "x_coordinate" ), x_coordinate ); output_stream.writeDouble( YIELD::StructuredStream::Declaration( "y_coordinate" ), y_coordinate ); output_stream.writeDouble( YIELD::StructuredStream::Declaration( "local_error" ), local_error ); }

      protected:
        double x_coordinate;
        double y_coordinate;
        double local_error;
      };



    };



  };



};

#endif
