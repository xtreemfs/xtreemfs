/*
 * Copyright (c) 2015 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

/*
 * Extensions to the standard SWIG typemaps. 
 */
 

// Extend generated vectors with a static method, that allows to fill the
// C++ structure with elements from a Java collection.
%define VECTOR(Name, CppCollectionType, JavaElementType)
%typemap(javacode) CppCollectionType %{
  public static $javaclassname from(java.util.Collection<JavaElementType> in) {
    $javaclassname out = new $javaclassname();
    for (JavaElementType entry : in) {
      out.add(entry);
    }
    return out;
  }

  public JavaElementType[] toArray() {
    int size = (int) this.size();
    JavaElementType[] out = new JavaElementType[size];
    for (int i = 0; i < size; ++i) {
      out[i] = this.get(i);
    }
    return out;
  }
%}

%template(Name) CppCollectionType;
%enddef // end VECTOR



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



// Add a new typemap, that allows to use std::string as an OUTPUT parameter.
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

