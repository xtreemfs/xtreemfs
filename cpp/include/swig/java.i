%module xtreemfs_jni
%javaconst(1);


%include <stdint.i>
%include <std_string.i>
%include <std_vector.i>
%include "std_list.i"
%include <std_map.i>
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
COLLECTION(StringVector, std::vector<std::string>, String)
COLLECTION(IntVector, std::vector<int>, Integer)

// Enable lists of Strings
LIST(StringList, std::string, String)

// Enable String Key-Value Maps
%template(StringMap) std::map<std::string, std::string>;


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

// TODO (jdillmann): Move somewhere else
// Ensure the OptionsProxy won't get gc'ed prior to the Volume
%typemap(javacode) xtreemfs::Volume %{
  private OptionsProxy optionsReference;
  protected void addReference(OptionsProxy options) {
    optionsReference = options;
  }
%}

%typemap(javaout) xtreemfs::Volume* OpenVolume(const std::string& volume_name,
      const xtreemfs::rpc::SSLOptions* ssl_options,
      const Options& options) {
    long cPtr = $jnicall;
    $javaclassname ret = null;
    if (cPtr != 0) {
      ret = new $javaclassname(cPtr, $owner);
      ret.addReference(options);
    }
    return ret;
  }





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


// Add a createVolume implementation, that is using a map instead of a list of KeyValuePairs
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
      const std::map<std::string, std::string>& volume_attributes_map) {

    std::list<xtreemfs::pbrpc::KeyValuePair*> volume_attributes;

    // Copy the attributes from the map to a KeyValuePair list.
    for (std::map<std::string, std::string>::const_iterator it = volume_attributes_map.begin();
          it != volume_attributes_map.end();
          ++it) {
      xtreemfs::pbrpc::KeyValuePair* kv = new xtreemfs::pbrpc::KeyValuePair();
      kv->set_key(it->first);
      kv->set_value(it->second);
      volume_attributes.push_back(kv);
    }

    // Call the actual implementation.
    $self->CreateVolume(mrc_address, auth, user_credentials, volume_name, mode,
      owner_username, owner_groupname, access_policy_type, quota,
      default_striping_policy_type, default_stripe_size, default_stripe_width,
      volume_attributes
      );

    // Cleanup and delete the KeyValuePairs.
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

