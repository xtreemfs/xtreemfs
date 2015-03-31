%module xtreemfs_jni
%javaconst(1);


%include <stdint.i>
%include <std_string.i>
%include <std_vector.i>
%include <various.i>
%include <typemaps.i>
%include <enums.swg>

%include "protobuf.i"
// Assure the protobuf header are included.
%{
#include "pbrpc/RPC.pb.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "xtreemfs/DIR.pb.h"
#include "xtreemfs/OSD.pb.h"
#include "xtreemfs/MRC.pb.h"
%}




// Extend generated collections with a static method, that allows to fill the
// C++ structure with elements from a Java collection.
%define COLLECTION(Name, CppCollectionType, JavaElementType)
%typemap(javacode) CppCollectionType %{
  public static $javaclassname from(java.util.Collection<JavaElementType> in) {
    $javaclassname out = new $javaclassname(in.size());
    for (JavaElementType entry : in) {
      out.add(entry);
    }
    return out;
  }
%}

%template(Name) CppCollectionType;
%enddef // end COLLECTION

// Enable vectors of Strings and Integers.
COLLECTION(VectorString, std::vector<std::string>, String)
COLLECTION(VectorInt, std::vector<int>, Integer)


// Cast int flags generated from enums typesafe to the native C++ interface.
//
// @param flag_type type of the C++ enum
// @param param_name the parameter name
%define ENUM_FLAG(flag_type, param_name)
%typemap(jni) flag_type param_name "jint"
%typemap(jtype) flag_type param_name "int"
%typemap(jstype) flag_type param_name "int"
%typemap(javain) flag_type param_name "$javainput"
%typemap(in) flag_type param_name {
  $1 = static_cast<flag_type>($input);
}
%enddef // end ENUM_FLAG


// Add a new typemap, that allows to use std::string as an OUPUT parameter.
// http://stackoverflow.com/a/11967859
%typemap(jni) std::string *OUTPUT, std::string &OUTPUT "jobjectArray"
%typemap(jtype) std::string *OUTPUT, std::string &OUTPUT "String[]"
%typemap(jstype) std::string *OUTPUT, std::string &OUTPUT "String[]"
%typemap(javain) std::string *OUTPUT, std::string &OUTPUT "$javainput"
%typemap(in) std::string *OUTPUT($*1_ltype temp), std::string &OUTPUT($*1_ltype temp)
{
  if (!$input) {
    SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "array null");
    return $null;
  }
  if (JCALL1(GetArrayLength, jenv, $input) == 0) {
    SWIG_JavaThrowException(jenv, SWIG_JavaIndexOutOfBoundsException, "Array must contain at least 1 element");
    return $null;
  }
  $1 = &temp; 
}

%typemap(freearg) std::string *OUTPUT, std::string &OUTPUT ""

%typemap(argout) std::string *OUTPUT, std::string &OUTPUT {
  jstring jvalue = JCALL1(NewStringUTF, jenv, temp$argnum.c_str()); 
  JCALL3(SetObjectArrayElement, jenv, $input, 0, jvalue);
}

%typemap(typecheck) std::string *OUTPUT = jobjectArray;
%typemap(typecheck) std::string &OUTPUT = jobjectArray;
// end std::string OUTPUT typemap

// Direct ByteBuffer typemap
// https://github.com/yuvalk/SWIGNIO
%typemap(jni) char* BUFFER "jobject"
%typemap(jtype) char* BUFFER "java.nio.ByteBuffer"
%typemap(jstype) char* BUFFER "java.nio.ByteBuffer"
%typemap(javain, pre=" assert $javainput.isDirect() : \"Buffer must be allocated direct.\";") char* BUFFER "$javainput"
%typemap(javaout) char* BUFFER {
  return $jnicall;
}
%typemap(in) char* BUFFER {
  $1 = (char*) jenv->GetDirectBufferAddress($input);
  if ($1 == NULL) {
    SWIG_JavaThrowException(jenv, SWIG_JavaRuntimeException, "Unable to get address of direct buffer. Buffer must be allocated direct.");
  }
}
%typemap(freearg) char* BUFFER ""
// end Direct ByteBuffer typemap


%{
#include "libxtreemfs/typedefs.h"
%}
namespace xtreemfs {
  class ServiceAddresses {
   public:
    ServiceAddresses(const std::string& address);
    ServiceAddresses(const std::vector<std::string>& addresses);
  };
}


%{
#include "libxtreemfs/user_mapping.h"
%}
%rename("$ignore", "not" %$isenum, "not" %$isenumitem, regextarget=1, fullname=1) "^xtreemfs::UserMapping::"; 
%include "libxtreemfs/user_mapping.h"


%{
#include "libxtreemfs/options.h"
%}
%rename (OptionsProxy) xtreemfs::Options;
%rename("$ignore", %$isfunction, regextarget=1, fullname=1) "^xtreemfs::Options::";
%ignore xtreemfs::Options::was_interrupted_function;
%include "libxtreemfs/options.h"


%{
#include <boost/asio/ssl/context.hpp>
%}
%rename("SSLContext") boost::asio::ssl::context_base;
%rename("$ignore", "not" %$isenum, "not" %$isenumitem, regextarget=1, fullname=1) "^boost::asio::ssl::context_base::"; 
%include <boost/asio/ssl/context_base.hpp>
%import <boost/asio/detail/config.hpp>
%import <boost/asio/ssl/context.hpp>


%{
#include "rpc/ssl_options.h"
%}
%rename (SSLOptionsProxy) xtreemfs::rpc::SSLOptions;
%include "rpc/ssl_options.h" 



// Change libxtreemfs class members to first letter lowercase in accordance to the Java interfaces.
%rename("%(firstlowercase)s", %$isfunction, %$ismember ) "";

%{ 
#include "libxtreemfs/client.h"
%}
// client.h
namespace xtreemfs {
  PROTO_INPUT(xtreemfs::pbrpc::UserCredentials, org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials, user_credentials)
  PROTO_INPUT(xtreemfs::pbrpc::Auth, org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth, auth)

  PROTO2_RETURN(xtreemfs::pbrpc::Volumes, org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes, true)

  PROTO_ENUM(xtreemfs::pbrpc::AccessControlPolicyType, org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType, access_policy_type)
  PROTO_ENUM(xtreemfs::pbrpc::StripingPolicyType, org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType, default_striping_policy_type)
}

// TODO (jdillmann): Could be moved to client_implementation
// TODO (jdillmann): Ensure requirements are met (e.g. pbrpc is available)
// TODO (jdillmann): Could use byte[] instead of strings
// TODO (jdillmann): Extend interface to use a std::map
%{
#include <stdexcept>
%}
%extend xtreemfs::Client {
  public: 
  void createVolume( /* has to be first lower cased manually */
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
      const std::vector<std::string>& volume_attributes_str_serialized) {

    std::list<xtreemfs::pbrpc::KeyValuePair*> volume_attributes;
    xtreemfs::pbrpc::KeyValuePair* kv = 0;

    // Parse the volume attributes that are serialized to strings.
    for (std::vector<std::string>::const_iterator it = volume_attributes_str_serialized.begin();
          it != volume_attributes_str_serialized.end();
          ++it) {
      kv = new xtreemfs::pbrpc::KeyValuePair();
      if (!kv->ParseFromString(*it)) {
        // TODO (jdillmann): Think about Exception handling.
        throw std::runtime_error("Unable to parse protocol message."); 
        /*
        SWIG_JavaThrowException(jenv,
                                SWIG_JavaRuntimeException,
                                "Unable to parse protocol message.");
        */

      }
      volume_attributes.push_back(kv);
    }

    // Call the actual implementation.
    $self->CreateVolume(mrc_address, auth, user_credentials, volume_name, mode,
      owner_username, owner_groupname, access_policy_type, quota,
      default_striping_policy_type, default_stripe_size, default_stripe_width,
      volume_attributes
      );

    // Cleanup and delete the KeyValuePairs
    for (std::list<xtreemfs::pbrpc::KeyValuePair*>::iterator it = volume_attributes.begin();
          it != volume_attributes.end();
          ++it) {
      delete *it;
    }
  }
}

// Ignore the actual implementation
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


%rename (openVolumeProxy) xtreemfs::Client::OpenVolume;
%rename (ClientProxy) xtreemfs::Client;
%include "libxtreemfs/client.h"




%{ 
#include "libxtreemfs/volume.h"
%}


%apply int *OUTPUT { int *size }; // GetXAttrSize
%apply std::string *OUTPUT { std::string *value } // GetXAttr
%apply std::string *OUTPUT { std::string *link_target_path } // ReadLink


// Adapt to the types defined in the java interface.
%clear off_t new_file_size;
%apply long { off_t new_file_size }; //FileHandle::Truncate
%clear uint64_t offset;
%apply long long { uint64_t offset }; // Volume::readDir



// Since lists are not supported, a intermediate vector is used for listing the OSDs.
// TODO(jdillmann): This is really inefficent, but should work for now
%ignore xtreemfs::Volume::GetSuitableOSDs; // ignore actual implementation
%extend xtreemfs::Volume {
public:
  void getSuitableOSDs( // has to be first lower cased manually
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      int number_of_osds,
      std::vector<std::string>* vector_of_osd_uuids) {
    std::list<std::string> list_of_osd_uuids;
    $self->GetSuitableOSDs(user_credentials, path, number_of_osds, &list_of_osd_uuids);

    vector_of_osd_uuids->reserve(list_of_osd_uuids.size());
    vector_of_osd_uuids->assign(list_of_osd_uuids.begin(), list_of_osd_uuids.end());
  }
}


// volume.h
namespace xtreemfs {
  PROTO_INPUT(xtreemfs::pbrpc::UserCredentials, org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials, user_credentials)
  PROTO2_RETURN(xtreemfs::pbrpc::StatVFS, org.xtreemfs.pbrpc.generatedinterfaces.MRC.StatVFS, true)
  PROTO_INPUT(xtreemfs::pbrpc::Stat, org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat, stat)
  PROTO2_RETURN(xtreemfs::pbrpc::DirectoryEntries, org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries, true)
  PROTO2_RETURN(xtreemfs::pbrpc::listxattrResponse, org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrResponse, true)

  PROTO_INPUT(xtreemfs::pbrpc::Replica, org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica, new_replica)
  PROTO2_RETURN(xtreemfs::pbrpc::Replicas, org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replicas, true)



  ENUM_FLAG(xtreemfs::pbrpc::SYSTEM_V_FCNTL, flags)
  ENUM_FLAG(xtreemfs::pbrpc::ACCESS_FLAGS, flags)
  ENUM_FLAG(xtreemfs::pbrpc::Setattrs, to_set)

  PROTO_ENUM(xtreemfs::pbrpc::XATTR_FLAGS, org.xtreemfs.pbrpc.generatedinterfaces.MRC.XATTR_FLAGS, flags)

  PROTO_OUTPUT(void GetAttr, stat, xtreemfs::pbrpc::Stat, org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat)
}



%rename (openFileProxy) xtreemfs::Volume::OpenFile;
%rename (VolumeProxy) xtreemfs::Volume;
%include "libxtreemfs/volume.h"




%{
#include "libxtreemfs/file_handle.h"
%}

// Adapt to the types defined in the java interface.
%clear int64_t offset;
%apply long long { int64_t offset }; // FileHandle::Read, FileHandle::Write
%clear uint64_t offset, uint64_t length;
%apply long long { uint64_t offset, uint64_t length }; // FileHandle::AcquireLock FileHandle::CheckLock, FileHandle::ReleaseLock, Volume::readDir
%clear size_t count;
%apply long { size_t count }; // FileHandle::Read, FileHandle::Write

%apply char *BYTE { const char *buf, char *buf };  // FileHandle::Read, FileHandle::Write


namespace xtreemfs {
  PROTO_INPUT(xtreemfs::pbrpc::Lock, org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock, lock)
  PROTO2_RETURN(xtreemfs::pbrpc::Lock, org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock, true)
} 


%extend xtreemfs::FileHandle {
  public: 
  int readDirect(char* directBuffer, size_t count, int64_t offset) {
    return $self->Read(directBuffer, count, offset);
  }
  int writeDirect(const char* directBuffer, size_t count, int64_t offset) {
    return $self->Write(directBuffer, count, offset);
  }
}

%apply char* BUFFER {const char* directBuffer, char* directBuffer}



%rename (FileHandleProxy) xtreemfs::FileHandle;
%include "libxtreemfs/file_handle.h"

