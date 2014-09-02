cmake_minimum_required(VERSION 2.6)
PROJECT(cpp CXX C)

#SET (CMAKE_VERBOSE_MAKEFILE true)

# used for everything, but the preload library (which struggles with implicitly
# generated symbols suffixed with 64, e.g. open64)
set(COMPILE_DEFS "_FILE_OFFSET_BITS=64")

# Uncomment this to enable boost::asio debug output.
#add_definitions(-DBOOST_ASIO_ENABLE_HANDLER_TRACKING)

set(CMAKE_MODULE_PATH ${CMAKE_SOURCE_DIR}/cmake)

# Set variables and required libraries.
##########################################
if (CMAKE_COMPILER_IS_GNUCXX)
  set(CMAKE_CXX_FLAGS "-Wall -Wno-unused-function -Wno-sign-compare -pthread")
endif(CMAKE_COMPILER_IS_GNUCXX)
if (MSVC)
  # Windows uses Unicode internally, so we also use Unicode instead of the default multi-byte character set.
  add_definitions(-DUNICODE -D_UNICODE)
  # Require at least Windows XP.
  add_definitions(-D_WIN32_WINNT=0x0501)
  # autolink for boost::asio does wrongfully require date_time and regex libraries.
  # See: http://www.boost.org/doc/libs/1_46_1/doc/html/boost_asio/using.html
  add_definitions(-DBOOST_DATE_TIME_NO_LIB -DBOOST_REGEX_NO_LIB)
  ADD_DEFINITIONS(/D _CRT_SECURE_NO_WARNINGS)
endif(MSVC)
if (${CMAKE_SYSTEM_NAME} MATCHES "SunOS")
  set (SOLARIS true)
endif(${CMAKE_SYSTEM_NAME} MATCHES "SunOS")
if (${CMAKE_SYSTEM_NAME} MATCHES "FreeBSD")
  set (FREEBSD true)
endif(${CMAKE_SYSTEM_NAME} MATCHES "FreeBSD")

# Set paths for required thirdparty libraries.
set(CLIENT_GOOGLE_PROTOBUF_CPP "${CMAKE_SOURCE_DIR}/thirdparty/protobuf-2.5.0")
if (WIN32)
  set(CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY "${CLIENT_GOOGLE_PROTOBUF_CPP}/vsprojects/Release/libprotobuf.lib")
else()
  FIND_FILE(CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY
            "libprotobuf.a"
            PATHS "${CLIENT_GOOGLE_PROTOBUF_CPP}/src/.libs/")
  FIND_FILE(CLIENT_GOOGLE_PROTOBUF_CPP_DYNAMIC_LIBRARY
            "libprotobuf.so"
            PATHS "${CLIENT_GOOGLE_PROTOBUF_CPP}/src/.libs/")
endif(WIN32)
set(CLIENT_GOOGLE_TEST_CPP "${CMAKE_SOURCE_DIR}/thirdparty/gtest-1.7.0")
if (WIN32)
  set(CLIENT_GOOGLE_TEST_CPP_LIBRARY "${CLIENT_GOOGLE_TEST_CPP}/msvc/gtest-md/Debug/gtestd.lib")
else()
  set(CLIENT_GOOGLE_TEST_CPP_LIBRARY "${CLIENT_GOOGLE_TEST_CPP}/lib/.libs/libgtest.a")
endif(WIN32)
if (WIN32)
  set(CLIENT_GOOGLE_TEST_CPP_MAIN "${CLIENT_GOOGLE_TEST_CPP}/msvc/gtest-md/Debug/gtest_main-mdd.lib")
else()
  set(CLIENT_GOOGLE_TEST_CPP_MAIN "${CLIENT_GOOGLE_TEST_CPP}/lib/.libs/libgtest_main.a")
endif(WIN32)
# Windows requires for a Debug build also debug libraries from protobuf & co.
STRING(REPLACE "Release" "Debug" CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY_DEBUG ${CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY})
STRING(REPLACE "Release" "Debug" TEMP ${CLIENT_GOOGLE_TEST_CPP_LIBRARY})
STRING(REPLACE ".lib" "d.lib" CLIENT_GOOGLE_TEST_CPP_LIBRARY_DEBUG ${TEMP})
STRING(REPLACE "Release" "Debug" TEMP ${CLIENT_GOOGLE_TEST_CPP_MAIN})
STRING(REPLACE ".lib" "d.lib" CLIENT_GOOGLE_TEST_CPP_MAIN_DEBUG ${TEMP})

# Set required libraries.
if (NOT WIN32)
  set(LIBFUSE "fuse")
  SET(LIBATTR "attr")
  SET(LIBPTHREAD "pthread")
endif(NOT WIN32)

if (SOLARIS)
  SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pthreads") 
  SET(LIBSOCKET_SOLARIS "socket")
  SET(LIBNSL_SOLARIS "nsl")
  # No libattr on Solaris.
  SET(LIBATTR "")
endif(SOLARIS)

IF (APPLE OR FREEBSD)
  SET(LIBATTR "")
ENDIF(APPLE OR FREEBSD)

IF (WIN32 OR APPLE OR SOLARIS)
  # Run cmake with -DBOOST_ROOT=<root path of local boost installation> on Windows, Solaris and MacOSX.
  # We link against the boost libraries found in BOOST_ROOT.
  SET(Boost_USE_STATIC_LIBS true)
ENDIF(WIN32 OR APPLE OR SOLARIS)

IF (APPLE)
  # Update 8/12/2011: Setting macosx-version-min does break mount.xtreemfs for unknown reasons - so disabled for now.
  # Tell gcc to compile 10.4 compatible files (does not work otherwise on older Macs).
  #SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mmacosx-version-min=10.4")
  
  SET(OSXFUSE_FUSE_H_INCLUDE_DIR "/usr/local/include/osxfuse/")
  if (EXISTS ${OSXFUSE_FUSE_H_INCLUDE_DIR})
    include_directories(${OSXFUSE_FUSE_H_INCLUDE_DIR})
  endif(EXISTS ${OSXFUSE_FUSE_H_INCLUDE_DIR})
ENDIF(APPLE)
IF(WIN32)
  SET(Boost_USE_STATIC_LIBS true)
  LINK_DIRECTORIES(${Boost_LIBRARY_DIRS})
  
  SET(CBFS_LICENSE "include/cbfs/cbfs_license.h")
  IF(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/${CBFS_LICENSE}")
    ## Find CbFS
    #  CBFS_ROOT        - Input variable: Set to 
    #  CBFS_FOUND       - Output variable: CBFS was found.
    #  CBFS_INCLUDE_DIR - Output variable: Directory which contains the CbFS.h file.
    #  CBFS_LIBRARIES   - Output variable: Will be set to CbFS.lib location.
    find_path(CBFS_INCLUDE_DIR CbFS.h PATHS "$ENV{ProgramFiles}/EldoS/Callback File System/CPP/VC2010/32bit/dynamic_runtime(MD)" ${CBFS_ROOT})
    find_library(CBFS_LIBRARY CbFS "$ENV{ProgramFiles}/EldoS/Callback File System/CPP/VC2010/32bit/dynamic_runtime(MD)" ${CBFS_ROOT})
  
    include(FindPackageHandleStandardArgs)
    FIND_PACKAGE_HANDLE_STANDARD_ARGS(CBFS DEFAULT_MSG CBFS_LIBRARY CBFS_INCLUDE_DIR)
  
    if (CBFS_FOUND)
      set(CBFS_LIBRARIES ${CBFS_LIBRARY})
      include_directories(${CBFS_INCLUDE_DIR})
    else(CBFS_FOUND)
      message(FATAL_ERROR "The required library 'CbFS' was not found.")
    endif (CBFS_FOUND)
    mark_as_advanced(CBFS_LIBRARY)
  ELSE()
    message(STATUS "File ${CBFS_LICENSE} was not found. Compilation of the CbFSAdapter, which allows to mount XtreemFS volumes as network drive, will be skipped.\n\nPlease note: it is not possible to build the CbFSAdapter on your own unless you have a license for the CbFS library. The XtreemFS open-source project was granted a non-commercial license and therefore we can compile the CbFSAdapter and distribute binaries of it.\n")
  ENDIF(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/${CBFS_LICENSE}")
ENDIF(WIN32)

# Set required thirdparty libraries.

# Boost
#set(Boost_DEBUG ON)
set(REQUIRED_BOOST_LIBRARIES "system" "thread" "program_options" "regex")
if (BOOST_ROOT)
  message(STATUS "Info: BOOST_ROOT is set to: ${BOOST_ROOT}")
endif(BOOST_ROOT)
# Finding the boost libraries may fail. If existing, we point FIND_PACKAGE to /usr/lib64.
if(NOT BOOST_ROOT AND EXISTS "/usr/lib64/libboost_system.so")
  set(BOOST_LIBRARYDIR "/usr/lib64")
endif(NOT BOOST_ROOT AND EXISTS "/usr/lib64/libboost_system.so")
# Initial find boost only to retrieve the version number.
FIND_PACKAGE(Boost)
# Unset the Boost_FOUND variable because the result of the second find will be checked.
set(Boost_FOUND)
if(WIN32 AND Boost_VERSION VERSION_GREATER "104900")
  set(REQUIRED_BOOST_LIBRARIES ${REQUIRED_BOOST_LIBRARIES} "chrono")
endif(WIN32 AND Boost_VERSION VERSION_GREATER "104900")
# Actual find boost including all required libraries.
FIND_PACKAGE(Boost COMPONENTS ${REQUIRED_BOOST_LIBRARIES} REQUIRED)
if (NOT Boost_FOUND)
  message(FATAL_ERROR "The boost library was not found on your system. If needed, you can also download and compile it on your own. After compiling boost locally, set the the environment variable BOOST_ROOT to the boost base directory before executing 'make' e.g., 'export BOOST_ROOT=/Users/xyz/boost_1_47_0'.")
endif(NOT Boost_FOUND)
include_directories(${Boost_INCLUDE_DIRS})

find_package(OpenSSL)
if (NOT OPENSSL_FOUND)
# OpenSSL find_package script cannot cope with /usr/lib64 directory under Cmake 2.8.3.
  set(LIBCRYPTO "crypto")
  set(LIBSSL "ssl")
  set(OPENSSL_LIBRARIES ${LIBCRYPTO} ${LIBSSL})
endif(NOT OPENSSL_FOUND)
include_directories(${OPENSSL_INCLUDE_DIR})
# Comment this definition if the XtreemFS source should not depend on OpenSSL.
add_definitions(-DHAS_OPENSSL)

find_package(Valgrind)
if (VALGRIND_FOUND)
  include_directories(${VALGRIND_INCLUDE_DIR})
  add_definitions(-DHAS_VALGRIND)
else (VALGRIND_FOUND)
  message(WARNING "Valgrind headers not found, running memcheck might report false positives.")
endif (VALGRIND_FOUND)

SET(REQUIRED_STATIC_LIBRARIES ${CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY})
if(BUILD_CLIENT_TESTS)
  SET(REQUIRED_STATIC_LIBRARIES ${REQUIRED_STATIC_LIBRARIES} ${CLIENT_GOOGLE_TEST_CPP_LIBRARY} ${CLIENT_GOOGLE_TEST_CPP_MAIN})
endif(BUILD_CLIENT_TESTS)

foreach (STATIC_LIB_FULL_PATH ${REQUIRED_STATIC_LIBRARIES})
  if(NOT EXISTS ${STATIC_LIB_FULL_PATH})
    message(FATAL_ERROR "The file ${STATIC_LIB_FULL_PATH} was not found. Please run 'make client' or 'make' from the base directory in order to build the required static libraries.")
  endif()
endforeach(STATIC_LIB_FULL_PATH)

CMAKE_POLICY(SET CMP0003 OLD)
ADD_LIBRARY(protobuf STATIC IMPORTED)
ADD_LIBRARY(protobuf_debug STATIC IMPORTED)
SET_PROPERTY(TARGET protobuf PROPERTY IMPORTED_LOCATION ${CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY})
SET_PROPERTY(TARGET protobuf_debug PROPERTY IMPORTED_LOCATION ${CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY_DEBUG})
include_directories(${CLIENT_GOOGLE_PROTOBUF_CPP}/src)

ADD_LIBRARY(gtest STATIC IMPORTED)
ADD_LIBRARY(gtest_debug STATIC IMPORTED)
ADD_LIBRARY(gtest_main STATIC IMPORTED)
ADD_LIBRARY(gtest_main_debug STATIC IMPORTED)
SET_PROPERTY(TARGET gtest PROPERTY IMPORTED_LOCATION ${CLIENT_GOOGLE_TEST_CPP_LIBRARY})
SET_PROPERTY(TARGET gtest_debug PROPERTY IMPORTED_LOCATION ${CLIENT_GOOGLE_TEST_CPP_LIBRARY_DEBUG})
SET_PROPERTY(TARGET gtest_main PROPERTY IMPORTED_LOCATION ${CLIENT_GOOGLE_TEST_CPP_MAIN})
SET_PROPERTY(TARGET gtest_main_debug PROPERTY IMPORTED_LOCATION ${CLIENT_GOOGLE_TEST_CPP_MAIN_DEBUG})
include_directories(${CLIENT_GOOGLE_TEST_CPP}/include)

# Check if this is a 64 Bit system
if(UNIX AND NOT WIN32)
  if(APPLE)
    set(LIBFUSE "osxfuse")
  endif(APPLE)
endif(UNIX AND NOT WIN32)

SET(REQUIRED_LIBRARIES ${LIBATTR} ${LIBCRYPTO} ${LIBSSL} ${LIBPTHREAD})
if (SKIP_FUSE)
  MESSAGE(STATUS "INFO: Skipping the XtreemFS Fuse Adapter (mount.xtreemfs) because SKIP_FUSE was defined.")
else()
  SET(REQUIRED_LIBRARIES ${REQUIRED_LIBRARIES} ${LIBFUSE})
endif(SKIP_FUSE)

foreach(LIB ${REQUIRED_LIBRARIES})
  #message(STATUS "checking for library: ${LIB}")
  # For unknown reasons, setting "FIND_LIBRARY_USE_LIB64_PATHS" does not fix the issue that libraries in /usr/lib64/ are not found under openSuse.
  # Therefore we just specify a list of possible library directories.
  find_library(FOUND${LIB} ${LIB} PATHS "/lib64" "/usr/lib64")
  if (NOT FOUND${LIB})
    if (${LIB} STREQUAL ${LIBFUSE})
      message(STATUS "INFO: If you want to skip the compilation of the FuseAdapter e.g., because your system does not provide Fuse (library: ${LIBFUSE}), please define SKIP_FUSE e.g., run: SKIP_FUSE=true make client")
    endif (${LIB} STREQUAL ${LIBFUSE})
    message(FATAL_ERROR "The required library '${LIB}' was not found. Please install it on your system first.")
  #else()
  #  message(STATUS "Result of find_library: ${FOUND${LIB}}")
  endif(NOT FOUND${LIB})
endforeach(LIB)

# Define building of binaries.
##########################################
MESSAGE(STATUS "Configuring XtreemFS Client Library (libxtreemfs) and Volume Tools ({mkfs,rmfs,lsfs}.xtreemfs).")

INCLUDE_DIRECTORIES(include generated)
file(GLOB_RECURSE SRCS_RPC src/rpc/*.cpp include/rpc/*.h)
file(GLOB_RECURSE SRCS_UTIL src/util/*.cpp include/util/*.h)

file(GLOB_RECURSE SRCS_GENERATED generated/*.cc generated/*.h)
# Ignore SWIG generated files
file(GLOB_RECURSE SRCS_GENERATED_IGNORES generated/swig/*)
if(SRCS_GENERATED_IGNORES)
  foreach(SRCS_GENERATED_IGNORE "${SRCS_GENERATED_IGNORES}")
    list(REMOVE_ITEM SRCS_GENERATED ${SRCS_GENERATED_IGNORE})
  endforeach(SRCS_GENERATED_IGNORE)
endif(SRCS_GENERATED_IGNORES)

file(GLOB_RECURSE SRCS_XTREEMFS src/libxtreemfs/*.cpp include/libxtreemfs/*.h)
add_library(xtreemfs ${SRCS_RPC} ${SRCS_UTIL} ${SRCS_GENERATED} ${SRCS_XTREEMFS})
# set_property(TARGET xtreemfs PROPERTY COMPILE_FLAGS "-fPIC")
# WARNING: The resulting static libxtreemfs does NOT contain the required static library protobuf.
#          If you want to use a static libxtreemfs, you have to use a CMake file like this which does automatically add the static libxtreemfs and libprotobuf to the executable.
#          An alternative would be to change this file in such a way that the static libxtreemfs and libprotobuf are merged together into a static libxtreemfs.
#          But this would require additional CMake macros as presented here: http://www.mail-archive.com/cmake@cmake.org/msg28670.html
TARGET_LINK_LIBRARIES(xtreemfs optimized protobuf debug protobuf_debug ${Boost_LIBRARIES} ${LIBPTHREAD} ${OPENSSL_LIBRARIES} ${LIBSOCKET_SOLARIS} ${LIBNSL_SOLARIS})

ADD_EXECUTABLE(example_libxtreemfs src/example_libxtreemfs/example_libxtreemfs.cpp)
SET_TARGET_PROPERTIES(example_libxtreemfs PROPERTIES COMPILE_DEFINITIONS ${COMPILE_DEFS})
TARGET_LINK_LIBRARIES(example_libxtreemfs xtreemfs)

ADD_EXECUTABLE(example_replication src/example_libxtreemfs/example_replication.cpp)
SET_TARGET_PROPERTIES(example_replication PROPERTIES COMPILE_DEFINITIONS ${COMPILE_DEFS})
TARGET_LINK_LIBRARIES(example_replication xtreemfs)

file(GLOB_RECURSE SRCS_MKFS src/mkfs.xtreemfs/*.cpp)
ADD_EXECUTABLE(mkfs.xtreemfs ${SRCS_MKFS})
SET_TARGET_PROPERTIES(mkfs.xtreemfs PROPERTIES COMPILE_DEFINITIONS ${COMPILE_DEFS})
TARGET_LINK_LIBRARIES(mkfs.xtreemfs xtreemfs)

file(GLOB_RECURSE SRCS_RMFS src/rmfs.xtreemfs/*.cpp)
ADD_EXECUTABLE(rmfs.xtreemfs ${SRCS_RMFS})
SET_TARGET_PROPERTIES(rmfs.xtreemfs PROPERTIES COMPILE_DEFINITIONS ${COMPILE_DEFS})
TARGET_LINK_LIBRARIES(rmfs.xtreemfs xtreemfs)

file(GLOB_RECURSE SRCS_LSFS src/lsfs.xtreemfs/*.cpp)
ADD_EXECUTABLE(lsfs.xtreemfs ${SRCS_LSFS})
SET_TARGET_PROPERTIES(lsfs.xtreemfs PROPERTIES COMPILE_DEFINITIONS ${COMPILE_DEFS})
TARGET_LINK_LIBRARIES(lsfs.xtreemfs xtreemfs)

file(GLOB_RECURSE SRCS_XTFS_UTIL src/xtfsutil/xtfsutil_server.cpp)
file(GLOB_RECURSE SRCS_JSONCPP src/json/*.cpp)

if (NOT WIN32)
  if (FOUND${LIBFUSE})
    MESSAGE(STATUS "Configuring XtreemFS Fuse Adapter (mount.xtreemfs).")
    file(GLOB_RECURSE SRCS_FUSE_ADAPTER src/fuse/fuse*.cpp)
    file(GLOB_RECURSE SRCS_FUSE_ADAPTER_MAIN src/fuse/mount.xtreemfs.cpp)
    ADD_EXECUTABLE(mount.xtreemfs ${SRCS_FUSE_ADAPTER} ${SRCS_FUSE_ADAPTER_MAIN} ${SRCS_XTFS_UTIL} ${SRCS_JSONCPP})
    SET_TARGET_PROPERTIES(mount.xtreemfs PROPERTIES COMPILE_DEFINITIONS ${COMPILE_DEFS})
    TARGET_LINK_LIBRARIES(mount.xtreemfs xtreemfs ${LIBFUSE})

    set(UNITTESTS_REQUIRED_SOURCES_fuse ${SRCS_FUSE_ADAPTER} ${SRCS_XTFS_UTIL} ${SRCS_JSONCPP})
    set(UNITTESTS_REQUIRED_LIBRARIES_fuse ${LIBFUSE})
  endif(FOUND${LIBFUSE})

  ADD_EXECUTABLE(xtfsutil src/xtfsutil/xtfsutil.cpp ${SRCS_JSONCPP})
  TARGET_LINK_LIBRARIES(xtfsutil ${Boost_LIBRARIES} ${LIBATTR})

  set(UNITTESTS_SKIP_cbfs true)
endif(NOT WIN32)
if(WIN32 AND CBFS_FOUND)
  MESSAGE(STATUS "Configuring XtreemFS CbFS Adapter (mount.xtreemfs.exe).")
  file(GLOB_RECURSE SRCS_CBFS_ADAPTER src/cbfs/*.cpp)
  ADD_EXECUTABLE(mount.xtreemfs ${SRCS_CBFS_ADAPTER} ${SRCS_XTFS_UTIL} ${SRCS_JSONCPP})
  TARGET_LINK_LIBRARIES(mount.xtreemfs xtreemfs ${CBFS_LIBRARIES})

  set(UNITTESTS_SKIP_fuse true)
endif(WIN32 AND CBFS_FOUND)

function(find_source_files RESULT)
  set(RESULT "")
  foreach(GLOBEXPR ${ARGV})
    file(GLOB_RECURSE FILES ${GLOBEXPR})
    foreach(TEST ${FILES})
      get_filename_component(DIRNAME ${TEST} PATH)
      get_filename_component(COMPONENT ${DIRNAME} NAME)
      get_filename_component(FILENAME ${TEST} NAME)
      if (${FILENAME} MATCHES "_test.cpp$")
        #skip
      elseif (${FILENAME} MATCHES "_main.cpp$")
        #skip
      else()
        LIST(APPEND RESULT ${TEST})
      endif()
    endforeach(TEST)
  endforeach(GLOBEXPR)
  set(FIND_RESULT ${RESULT} PARENT_SCOPE)
endfunction(find_source_files)

#SET(BUILD_PRELOAD TRUE)
if (BUILD_PRELOAD)
  find_source_files("src/ld_preload/*.cpp" "include/ld_preload/*.h")
  set(SRCS_PRELOAD ${FIND_RESULT})

  # The LD_PRELOAD dynamic library
  add_library(xtreemfs_preload SHARED ${SRCS_PRELOAD}
              ${SRCS_RPC} ${SRCS_UTIL} ${SRCS_GENERATED} ${SRCS_XTREEMFS})

  IF(NOT ${CMAKE_BUILD_TYPE} MATCHES "Debug")
    SET_TARGET_PROPERTIES(xtreemfs_preload PROPERTIES COMPILE_DEFINITIONS "XTREEMFS_PRELOAD_QUIET")
  ENDIF(NOT ${CMAKE_BUILD_TYPE} MATCHES "Debug")

  SET_TARGET_PROPERTIES(xtreemfs_preload PROPERTIES LINKER_LANGUAGE C)
  TARGET_LINK_LIBRARIES(xtreemfs_preload
                        xtreemfs
                        -ldl
                        ${CLIENT_GOOGLE_PROTOBUF_CPP_DYNAMIC_LIBRARY})
  ADD_EXECUTABLE(preload_test "test/ld_preload/preload_test.cpp")

  ADD_EXECUTABLE(benchmark_preload "test/ld_preload/benchmark_preload.cpp")
  TARGET_LINK_LIBRARIES(benchmark_preload xtreemfs)
  
  ADD_EXECUTABLE(benchmark_libxtreemfs "test/ld_preload/benchmark_libxtreemfs.cpp")
  TARGET_LINK_LIBRARIES(benchmark_libxtreemfs xtreemfs)
endif(BUILD_PRELOAD)

if (GENERATE_JNI OR BUILD_JNI)
  MESSAGE(STATUS "Configuring JNI Library.")

  set(JNI_LIB_NAME "jni-xtreemfs")
  set(JNI_GEN_JAVA_PKG "org.xtreemfs.common.libxtreemfs.jni.generated")
  set(JNI_GEN_JAVA_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../java/servers/src/org/xtreemfs/common/libxtreemfs/jni/generated")
  set(JNI_GEN_C_DIR "${CMAKE_CURRENT_SOURCE_DIR}/generated/swig")
  set(JNI_GEN_C_FILE "${JNI_GEN_C_DIR}/xtreemfsJAVA_wrap.cpp")

  set(SRCS_LIBXTREEMFS ${SRCS_RPC} ${SRCS_UTIL} ${SRCS_GENERATED} ${SRCS_XTREEMFS})
  set(LIBS_LIBXTREEMFS protobuf ${Boost_LIBRARIES} ${LIBPTHREAD} ${OPENSSL_LIBRARIES} ${LIBSOCKET_SOLARIS} ${LIBNSL_SOLARIS})

  find_package(JNI REQUIRED)
  include_directories(${JNI_INCLUDE_DIRS})

  # The JNI library has to be a shared objects and requires to build the sources
  # itself or could use objects built with PIC.
  add_library(${JNI_LIB_NAME} MODULE ${JNI_GEN_C_FILE} ${SRCS_LIBXTREEMFS})
  target_link_libraries(${JNI_LIB_NAME} ${LIBS_LIBXTREEMFS})

  if (APPLE)
    # JNI will look for a library whose name is platform dependent, namely
    #   MacOS  : libLIBRARY.jnilib
    #   Windows: LIBRARY.dll
    #   Linux  : libLIBRARY.so
    set_target_properties (${JNI_LIB_NAME} PROPERTIES SUFFIX ".jnilib")
  endif ()


  if (GENERATE_JNI)
    MESSAGE(STATUS "Configuring JNI generator.")
    find_package(SWIG REQUIRED)
    # include(${SWIG_USE_FILE})
    # Use modified UseSWIG.cmake
    include(cmake/UseSWIG.cmake)
  
    set(CMAKE_SWIG_OUTDIR "${CMAKE_CURRENT_BINARY_DIR}/swig_generated/java")
    set(CMAKE_SWIG_FLAGS -package ${JNI_GEN_JAVA_PKG})
    
    # Workaround to pass -D defines to SWIG -> fixed in modified UseSWIG.cmake
    # set(CMAKE_SWIG_FLAGS "$(CXX_DEFINES)")
    
    set_source_files_properties("include/swig/xtreemfs.i" PROPERTIES CPLUSPLUS ON)
    # swig_add_module(${JNI_LIB_NAME} java "include/swig/xtreemfs.i" ${SRCS_LIBXTREEMFS})
    SWIG_MODULE_INITIALIZE(${JNI_LIB_NAME} java)
    SWIG_GENERATE_SOURCES(${JNI_LIB_NAME} swig_generated_sources "include/swig/xtreemfs.i")
    # This assumes, that only one file is created. This is currently true for 
    # the xtreemfs.i and the UseSWIG macros, but could fail with future versions
    list(GET swig_generated_sources 0 swig_generated_file)

    # Add a file target which invokes swig when used as a source
    # The JNI_GEN_C_FILE will be used by the jni library and thus the generated 
    # files are copied in time to the correct path.
    add_custom_command(
      OUTPUT ${JNI_GEN_C_FILE}
      COMMAND ${CMAKE_COMMAND} -E copy ${swig_generated_file} ${JNI_GEN_C_FILE}
      COMMAND ${CMAKE_COMMAND} -E remove_directory ${JNI_GEN_JAVA_DIR}
      COMMAND ${CMAKE_COMMAND} -E make_directory ${JNI_GEN_JAVA_DIR}
      COMMAND ${CMAKE_COMMAND} -E copy_directory ${CMAKE_SWIG_OUTDIR} ${JNI_GEN_JAVA_DIR}
      DEPENDS ${swig_generated_file})

  endif(GENERATE_JNI)
endif(GENERATE_JNI OR BUILD_JNI)


################################################################################
# Define building of unittests.
################################################################################
#
# Unit tests are only compiled if the CMake variable BUILD_CLIENT_TESTS is set
# (passed by the top level Makefile if the environment variable
#  BUILD_CLIENT_TESTS is set).
#
# The directory structure under test/ is identical to the one under src/ and
# include/.
# Since some components are not available on all plattforms, some variables
# are inspected for each component before the component's unit tests are
# compiled.
# A component here is given by the name of the directory below test/ i.e.,
# the component for the file test/fuse/fuse_options_test.cpp is "fuse".
#
# The following variables are inspected for each component:
# UNITTESTS_SKIP_${COMPONENT} - set if component unit tests must not be compiled
# UNITTESTS_REQUIRED_SOURCES_${COMPONENT}   - list of source files which have to
#                                             be compiled for the unit test
# UNITTESTS_REQUIRED_LIBRARIES_${COMPONENT} - list of libraries which have to be
#                                             linked to the unit test.
################################################################################
if(BUILD_CLIENT_TESTS)
  # enable_testing() will provide a target "test".
  enable_testing()
  include_directories("test")

  MESSAGE(STATUS "Configuring libxtreemfs unittests.")

  file(GLOB_RECURSE SRCS_TEST_COMMON test/common/*.cpp)
  add_library(test_common ${SRCS_TEST_COMMON})
  set (UNITTESTS_SKIP_common true)

  file(GLOB_RECURSE SRCS_TESTS test/*.cpp)
  foreach (TEST ${SRCS_TESTS})
    get_filename_component(DIRNAME ${TEST} PATH)
    get_filename_component(COMPONENT ${DIRNAME} NAME)
    get_filename_component(FILENAME ${TEST} NAME)
    if (NOT UNITTESTS_SKIP_${COMPONENT})
      if (${FILENAME} MATCHES "_test.cpp$")
        string(REGEX REPLACE "_test.cpp$" "" testname ${FILENAME})
        set(testname "test_${testname}")
        MESSAGE(STATUS "\tConfiguring test: ${testname}.")

        add_executable(${testname} ${TEST} ${UNITTESTS_REQUIRED_SOURCES_${COMPONENT}})
        SET_TARGET_PROPERTIES(${testname} PROPERTIES COMPILE_DEFINITIONS ${COMPILE_DEFS})
        TARGET_LINK_LIBRARIES(${testname} gtest_main gtest test_common xtreemfs ${UNITTESTS_REQUIRED_LIBRARIES_${COMPONENT}})
        ADD_TEST(${testname} ${testname})
      else()
        MESSAGE(STATUS "\tWARNING: Found file '${FILENAME}' below test/ does not end with '_test.cpp' and was therefore ignored as unit test.")
      endif(${FILENAME} MATCHES "_test.cpp$")
    endif(NOT UNITTESTS_SKIP_${COMPONENT})
  endforeach(TEST)
endif(BUILD_CLIENT_TESTS)
