/*
 * Copyright (c) 2015 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */


%module xtreemfs_jni
%javaconst(1);

%include "base.i"
%include <stdint.i>
%include <std_string.i>
%include <std_vector.i>
%include "std_list.i"
%include <std_map.i>
%include <various.i>
%include <typemaps.i>
%include <enums.swg>


// Include protobuf specific functions and 
// assure the protobuf headers are included.
%include "protobuf.i"
%{
#include "pbrpc/RPC.pb.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "xtreemfs/DIR.pb.h"
#include "xtreemfs/OSD.pb.h"
#include "xtreemfs/MRC.pb.h"
%}


// Enable vectors of Strings and Integers.
VECTOR(StringVector, std::vector<std::string>, String)
VECTOR(IntVector, std::vector<int>, Integer)

// Enable lists of Strings.
LIST(StringList, std::string, String)

// Enable String Key-Value Maps.
%template(StringMap) std::map<std::string, std::string>;


// Include supplementary classes and enums required for libxtreemfs. 
%{ #include "libxtreemfs/typedefs.h" %}
namespace xtreemfs {
  class ServiceAddresses {
   public:
    ServiceAddresses(const std::string& address);
    ServiceAddresses(const std::vector<std::string>& addresses);
  };
}

// Ignore everything except the inner enums.
%{ #include "libxtreemfs/user_mapping.h" %}
%rename("$ignore", "not" %$isenum, "not" %$isenumitem, regextarget=1, fullname=1) "^xtreemfs::UserMapping::"; 
%include "libxtreemfs/user_mapping.h"

// Ignore everything except the inner enums.
%{ #include <boost/asio/ssl/context.hpp> %}
%rename("SSLContext") boost::asio::ssl::context_base;
%rename("$ignore", "not" %$isenum, "not" %$isenumitem, regextarget=1, fullname=1) "^boost::asio::ssl::context_base::"; 
%include <boost/asio/ssl/context_base.hpp>
%import <boost/asio/detail/config.hpp>
%import <boost/asio/ssl/context.hpp>


// Include the Options class. 
// Since every option is a public member variable functions can be ignored.
%{ #include "libxtreemfs/options.h" %}
%rename (OptionsProxy) xtreemfs::Options;
%rename("$ignore", %$isfunction, regextarget=1, fullname=1) "^xtreemfs::Options::";
%ignore xtreemfs::Options::was_interrupted_function;
%include "libxtreemfs/options.h"

// Include the SSLOptions.
// TODO (jdillmann): This could be empty in case HAS_OPENSSL is false.
%{ #include "rpc/ssl_options.h" %}
%rename (SSLOptionsProxy) xtreemfs::rpc::SSLOptions;
%include "rpc/ssl_options.h" 

// Include the Logging class.
%{ #include "util/logging.h" %}
%import "util/logging.h"
ENUM_FLAG(xtreemfs::util::LogLevel, level)
namespace xtreemfs {
namespace util {
  void initialize_logger(xtreemfs::util::LogLevel level);
  void shutdown_logger();
}}


/*******************************************************************************
 * Exception handling
 * C++ exceptions have to be casted to Java exceptions.
 ******************************************************************************/
%{ #include "libxtreemfs/xtreemfs_exception.h" %}
// TODO (jdillmann): JNI Error Handling if a method can not be found

%typemap(throws, throws="org.xtreemfs.common.libxtreemfs.exceptions.XtreemFSException") 
    xtreemfs::XtreemFSException, 
    xtreemfs::UnknownAddressSchemeException,
    xtreemfs::FileHandleNotFoundException,
    xtreemfs::FileInfoNotFoundException {
  jclass clazz = JCALL1(FindClass, jenv, "org/xtreemfs/common/libxtreemfs/exceptions/XtreemFSException");
  JCALL2(ThrowNew, jenv, clazz, $1.what());
  return $null;
}

%typemap(throws, throws="java.io.IOException") xtreemfs::IOException {
  SWIG_JavaThrowException(jenv, SWIG_JavaIOException, $1.what());
  return $null;
}

%typemap(throws, throws="org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException") 
      xtreemfs::AddressToUUIDNotFoundException {
    jclass clazz =  JCALL1(FindClass, jenv, "org/xtreemfs/common/libxtreemfs/exceptions/AddressToUUIDNotFoundException");
    JCALL2(ThrowNew, jenv, clazz, $1.what());
    return $null;
}

%typemap(throws, throws="org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException") 
      xtreemfs::VolumeNotFoundException {
    jclass clazz =  JCALL1(FindClass, jenv, "org/xtreemfs/common/libxtreemfs/exceptions/VolumeNotFoundException");
    JCALL2(ThrowNew, jenv, clazz, $1.what());
    return $null;
}

%typemap(throws, throws="org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException") 
      xtreemfs::PosixErrorException {
    jclass clazz = JCALL1(FindClass, jenv, "org/xtreemfs/common/libxtreemfs/exceptions/PosixErrorException");
    jmethodID mid = JCALL3(GetMethodID, jenv, clazz, "<init>", "(Lorg/xtreemfs/foundation/pbrpc/generatedinterfaces/RPC$POSIXErrno;Ljava/lang/String;)V");

    jclass clazz2 = JCALL1(FindClass, jenv, "org/xtreemfs/foundation/pbrpc/generatedinterfaces/RPC$POSIXErrno");
    jmethodID mid2 = JCALL3(GetStaticMethodID, jenv, clazz2, "valueOf", "(I)Lorg/xtreemfs/foundation/pbrpc/generatedinterfaces/RPC$POSIXErrno;");

    jobject posix_errno = JCALL3(CallStaticObjectMethod, jenv, clazz2, mid2, $1.posix_errno());
    jstring what = JCALL1(NewStringUTF, jenv, $1.what());
    jthrowable o = static_cast<jthrowable>(JCALL4(NewObject, jenv, clazz, mid, posix_errno, what));
    JCALL1(Throw, jenv, o);

    return $null;
}

%typemap(throws) xtreemfs::OpenFileHandlesLeftException {
  SWIG_JavaThrowException(jenv, SWIG_JavaRuntimeException, $1.what());
}

%typemap(throws, throws="org.xtreemfs.common.libxtreemfs.exceptions.UUIDNotInXlocSetException") 
      xtreemfs::UUIDNotInXlocSetException {
    jclass clazz =  JCALL1(FindClass, jenv, "org/xtreemfs/common/libxtreemfs/exceptions/UUIDNotInXlocSetException");
    JCALL2(ThrowNew, jenv, clazz, $1.what());
    return $null;
}

%define DEFAULT_EXCEPTIONS(METHOD)
%catches(const xtreemfs::AddressToUUIDNotFoundException,
         const xtreemfs::IOException,
         const xtreemfs::PosixErrorException,
         const xtreemfs::UnknownAddressSchemeException,
         const xtreemfs::XtreemFSException) METHOD;
%enddef



/*******************************************************************************
 * UUIDResolver
 ******************************************************************************/
%{ #include "libxtreemfs/uuid_resolver.h" %}

%apply std::string *OUTPUT { std::string *address } // UUIDToAddress
%apply std::string *OUTPUT { std::string *mrc_uuid } // VolumeNameToMRCUUID

%rename("$ignore") xtreemfs::UUIDResolver::UUIDToAddressWithOptions;
%rename("$ignore") xtreemfs::UUIDResolver::VolumeNameToMRCUUID(
      const std::string& volume_name,
      SimpleUUIDIterator* uuid_iterator);

// Add Exception Handling
%catches(const xtreemfs::AddressToUUIDNotFoundException, 
         const xtreemfs::UnknownAddressSchemeException,
         const xtreemfs::XtreemFSException) xtreemfs::UUIDResolver::UUIDToAddress;
%catches(const xtreemfs::VolumeNotFoundException,
         const xtreemfs::AddressToUUIDNotFoundException,
         const xtreemfs::XtreemFSException) xtreemfs::UUIDResolver::VolumeNameToMRCUUID;
%catches(const xtreemfs::VolumeNotFoundException,
         const xtreemfs::AddressToUUIDNotFoundException,
         const xtreemfs::XtreemFSException) xtreemfs::UUIDResolver::VolumeNameToMRCUUIDs;



/*******************************************************************************
 * Client
 ******************************************************************************/
%{ #include "libxtreemfs/client.h" %}

// Define protobuf parameters and return types
PROTO_INPUT(xtreemfs::pbrpc::UserCredentials, org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials, user_credentials)
PROTO_INPUT(xtreemfs::pbrpc::Auth, org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth, auth)

PROTO2_RETURN(xtreemfs::pbrpc::Volumes, org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes, true)

PROTO_ENUM(xtreemfs::pbrpc::AccessControlPolicyType, org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType, access_policy_type)
PROTO_ENUM(xtreemfs::pbrpc::StripingPolicyType, org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType, default_striping_policy_type)

// Ignore the deprecated implementation
%rename ("$ignore") xtreemfs::Client::CreateVolume(
      const ServiceAddresses& mrc_address,
      const xtreemfs::pbrpc::Auth& auth,
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& volume_name,
      int mode,
      const std::string& owner_username,
      const std::string& owner_groupname,
      const xtreemfs::pbrpc::AccessControlPolicyType& access_policy_type,
      long quota,
      const xtreemfs::pbrpc::StripingPolicyType& default_striping_policy_type,
      int default_stripe_size,
      int default_stripe_width,
      const std::list<xtreemfs::pbrpc::KeyValuePair*>& volume_attributes);


// Add Exception Handling
%catches(const xtreemfs::XtreemFSException) xtreemfs::Client::Start;
%catches(const xtreemfs::AddressToUUIDNotFoundException, 
         const xtreemfs::UnknownAddressSchemeException,
         const xtreemfs::VolumeNotFoundException,
         const xtreemfs::XtreemFSException) xtreemfs::Client::OpenVolume;
%catches(const xtreemfs::IOException,
         const xtreemfs::PosixErrorException,
         const xtreemfs::XtreemFSException) xtreemfs::Client::CreateVolume;
%catches(const xtreemfs::IOException,
         const xtreemfs::PosixErrorException,
         const xtreemfs::XtreemFSException) xtreemfs::Client::DeleteVolume;
%catches(const xtreemfs::AddressToUUIDNotFoundException, 
         const xtreemfs::IOException,
         const xtreemfs::PosixErrorException,
         const xtreemfs::XtreemFSException) xtreemfs::Client::ListVolumes;
%catches(const xtreemfs::AddressToUUIDNotFoundException, 
         const xtreemfs::IOException,
         const xtreemfs::PosixErrorException,
         const xtreemfs::XtreemFSException) xtreemfs::Client::ListVolumeNames;
%catches(const xtreemfs::AddressToUUIDNotFoundException, 
         const xtreemfs::UnknownAddressSchemeException,
         const xtreemfs::XtreemFSException) xtreemfs::Client::UUIDToAddress;



/*******************************************************************************
 * Volume
 ******************************************************************************/
%{ #include "libxtreemfs/volume.h" %}

// Apply Output argument typemaps.
%apply int *OUTPUT { int *size }; // GetXAttrSize
%apply std::string *OUTPUT { std::string *value } // GetXAttr
%apply std::string *OUTPUT { std::string *link_target_path } // ReadLink


// Adapt to the types defined in the java interface.
%clear off_t new_file_size;
%apply long { off_t new_file_size }; //FileHandle::Truncate
%clear uint64_t offset;
%apply long long { uint64_t offset }; // Volume::ReadDir

// Define protobuf parameters and return types
PROTO_INPUT(xtreemfs::pbrpc::UserCredentials, org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials, user_credentials)
PROTO_INPUT(xtreemfs::pbrpc::Stat, org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat, stat)
PROTO_INPUT(xtreemfs::pbrpc::Replica, org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica, new_replica)

PROTO2_RETURN(xtreemfs::pbrpc::Replicas, org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replicas, true)
PROTO2_RETURN(xtreemfs::pbrpc::DirectoryEntries, org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries, true)
PROTO2_RETURN(xtreemfs::pbrpc::StatVFS, org.xtreemfs.pbrpc.generatedinterfaces.MRC.StatVFS, true)
PROTO2_RETURN(xtreemfs::pbrpc::listxattrResponse, org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrResponse, true)

PROTO_OUTPUT(void GetAttr, stat, xtreemfs::pbrpc::Stat, org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat)

PROTO_ENUM(xtreemfs::pbrpc::XATTR_FLAGS, org.xtreemfs.pbrpc.generatedinterfaces.MRC.XATTR_FLAGS, flags)

ENUM_FLAG(xtreemfs::pbrpc::SYSTEM_V_FCNTL, flags)
ENUM_FLAG(xtreemfs::pbrpc::ACCESS_FLAGS, flags)
ENUM_FLAG(xtreemfs::pbrpc::Setattrs, to_set)

// Add Exception Handling
%catches(const xtreemfs::OpenFileHandlesLeftException) xtreemfs::Volume::Close;

DEFAULT_EXCEPTIONS(xtreemfs::Volume::StatFS);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::ReadLink);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::Symlink);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::Link);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::Access);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::OpenFile);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::Truncate);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::GetAttr);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::SetAttr);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::Unlink);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::Rename);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::MakeDirectory);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::DeleteDirectory);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::ReadDir);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::ListXAttrs);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::SetXAttr);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::GetXAttr);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::GetXAttrSize);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::RemoveXAttr);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::AddReplica);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::ListReplicas);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::RemoveReplica);
DEFAULT_EXCEPTIONS(xtreemfs::Volume::GetSuitableOSDs);



/*******************************************************************************
 * FileHandle
 ******************************************************************************/
%{ #include "libxtreemfs/file_handle.h" %}

// Adapt to the types defined in the java interface.
%clear int64_t offset;
%apply long long { int64_t offset }; // FileHandle::Read, FileHandle::Write
%clear uint64_t offset, uint64_t length;
%apply long long { uint64_t offset, uint64_t length }; // FileHandle::AcquireLock FileHandle::CheckLock, FileHandle::ReleaseLock, Volume::readDir
%clear size_t count;
%apply long { size_t count }; // FileHandle::Read, FileHandle::Write

// Define protobuf parameters and return types
PROTO_INPUT(xtreemfs::pbrpc::Lock, org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock, lock)
PROTO2_RETURN(xtreemfs::pbrpc::Lock, org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock, true)

// Use java byte[] arrays or direct buffers for read and write.
%apply char *BYTE { const char *buf, char *buf };  // FileHandle::Read, FileHandle::Write
%apply char *BUFFER {const char *directBuffer, char *directBuffer}

// Add Exception Handling
DEFAULT_EXCEPTIONS(xtreemfs::FileHandle::Read);
DEFAULT_EXCEPTIONS(xtreemfs::FileHandle::read);
DEFAULT_EXCEPTIONS(xtreemfs::FileHandle::readDirect);
DEFAULT_EXCEPTIONS(xtreemfs::FileHandle::Write);
DEFAULT_EXCEPTIONS(xtreemfs::FileHandle::write);
DEFAULT_EXCEPTIONS(xtreemfs::FileHandle::writeDirect);
DEFAULT_EXCEPTIONS(xtreemfs::FileHandle::Flush);
DEFAULT_EXCEPTIONS(xtreemfs::FileHandle::Truncate);
DEFAULT_EXCEPTIONS(xtreemfs::FileHandle::GetAttr);
DEFAULT_EXCEPTIONS(xtreemfs::FileHandle::AcquireLock);
DEFAULT_EXCEPTIONS(xtreemfs::FileHandle::CheckLock);
DEFAULT_EXCEPTIONS(xtreemfs::FileHandle::ReleaseLock);
DEFAULT_EXCEPTIONS(xtreemfs::FileHandle::ReleaseLockOfProcess);

%catches(const xtreemfs::AddressToUUIDNotFoundException,
         const xtreemfs::IOException,
         const xtreemfs::PosixErrorException,
         const xtreemfs::UnknownAddressSchemeException,
         const xtreemfs::UUIDNotInXlocSetException,
         const xtreemfs::XtreemFSException) 
    xtreemfs::FileHandle::PingReplica;

%catches(const xtreemfs::AddressToUUIDNotFoundException,
         const xtreemfs::FileInfoNotFoundException,
         const xtreemfs::FileHandleNotFoundException,
         const xtreemfs::IOException,
         const xtreemfs::PosixErrorException,
         const xtreemfs::UnknownAddressSchemeException,
         const xtreemfs::XtreemFSException) 
    xtreemfs::FileHandle::Close;

// Add missing methods from the Java implementation.
%extend xtreemfs::FileHandle {
  public: 
  int readDirect(char *directBuffer, size_t count, int64_t offset) {
    return $self->Read(directBuffer, count, offset);
  }
  
  int writeDirect(const char *directBuffer, size_t count, int64_t offset) {
    return $self->Write(directBuffer, count, offset);
  }

  int read(char *buf, int buf_offset, size_t count, int64_t offset) {
    return $self->Read(buf + buf_offset, count, offset);
  }
  
  int write(const char *buf, int buf_offset, size_t count, int64_t offset) {
    return $self->Write(buf + buf_offset, count, offset);
  }
}



/*******************************************************************************
 * Garbage collection 
 ******************************************************************************/

%newobject xtreemfs::Client::CreateClient;

// Altough ServiceAddresses are passed by reference, their content will be 
// copied when the UUID Iterator is generated. Otherwise they would have to be 
// kept from being gc'ed.
// UserCredentials are also copied to a new variable.

// Options and SSLOptions have to prevented from getting garabage collected
// on Client::CreateClient and Client::OpenVolume because they are stored as
// references in the newly created objects.
%typemap(javacode) xtreemfs::Client, xtreemfs::Volume %{
  private OptionsProxy optionsReference;
  private SSLOptionsProxy sslOptionsReference;
  protected void addReferences(OptionsProxy options, SSLOptionsProxy sslOptions) {
    optionsReference = options;
    sslOptionsReference = sslOptions;
  }
%}

%typemap(javaout) xtreemfs::Client* xtreemfs::Client::CreateClient(
      const ServiceAddresses& dir_service_addresses,
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const xtreemfs::rpc::SSLOptions* ssl_options,
      const Options& options) {
    long cPtr = $jnicall;
    $javaclassname ret = null;
    if (cPtr != 0) {
      ret = new $javaclassname(cPtr, $owner);
      ret.addReferences(options, ssl_options);
    }
    return ret;
}

%typemap(javaout) xtreemfs::Volume* xtreemfs::Client::OpenVolume(
      const std::string& volume_name,
      const xtreemfs::rpc::SSLOptions* ssl_options,
      const Options& options) {
    long cPtr = $jnicall;
    $javaclassname ret = null;
    if (cPtr != 0) {
      ret = new $javaclassname(cPtr, $owner);
      ret.addReferences(options, ssl_options);
    }
    return ret;
}



/*******************************************************************************
 * Wrap-up
 ******************************************************************************/
// Change libxtreemfs class members to first letter lowercase in accordance to the Java interfaces.
%rename("%(firstlowercase)s", %$isfunction, %$ismember ) "";

// Include utility classes.
%rename (UUIDResolverProxy) xtreemfs::UUIDResolver;
%include "libxtreemfs/uuid_resolver.h"

// Include (and rename) the libxtreemfs.
%rename (openVolumeProxy) xtreemfs::Client::OpenVolume;
%rename (ClientProxy) xtreemfs::Client;
%include "libxtreemfs/client.h"

%rename (openFileProxy) xtreemfs::Volume::OpenFile;
%rename (VolumeProxy) xtreemfs::Volume;
%include "libxtreemfs/volume.h"

%rename (FileHandleProxy) xtreemfs::FileHandle;
%include "libxtreemfs/file_handle.h"

