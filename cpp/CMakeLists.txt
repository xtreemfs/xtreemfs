cmake_minimum_required(VERSION 2.6)
PROJECT(cpp CXX)

#SET (CMAKE_VERBOSE_MAKEFILE true)

add_definitions(-D_FILE_OFFSET_BITS=64)

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
set(CLIENT_GOOGLE_PROTOBUF_CPP "${CMAKE_SOURCE_DIR}/thirdparty/protobuf-2.3.0")
if (WIN32)
  set(CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY "${CLIENT_GOOGLE_PROTOBUF_CPP}/vsprojects/Release/libprotobuf.lib")
else()
  set(CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY "${CLIENT_GOOGLE_PROTOBUF_CPP}/src/.libs/libprotobuf.a")
endif(WIN32)
set(CLIENT_GOOGLE_TEST_CPP "${CMAKE_SOURCE_DIR}/thirdparty/gtest-1.5.0")
if (WIN32)
  set(CLIENT_GOOGLE_TEST_CPP_LIBRARY "${CLIENT_GOOGLE_TEST_CPP}/msvc/gtest-md/Release/gtest.lib")
else()
  set(CLIENT_GOOGLE_TEST_CPP_LIBRARY "${CLIENT_GOOGLE_TEST_CPP}/lib/.libs/libgtest.a")
endif(WIN32)
if (WIN32)
  set(CLIENT_GOOGLE_TEST_CPP_MAIN "${CLIENT_GOOGLE_TEST_CPP}/msvc/gtest-md/Release/gtest_main-md.lib")
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
  
  ## Find dokan
  #  DOKAN_ROOT        - Input variable: Set to 
  #  DOKAN_FOUND       - Output variable: Dokan was found.
  #  DOKAN_INCLUDE_DIR - Output variable: Directory which contains the dokan.h file.
  #  DOKAN_LIBRARIES   - Output variable: Will be set to dokan.lib location.
  find_path(DOKAN_INCLUDE_DIR dokan.h PATHS $ENV{ProgramFiles}/Dokan/DokanLibrary ${DOKAN_ROOT})
  find_library(DOKAN_LIBRARY dokan $ENV{ProgramFiles}/Dokan/DokanLibrary ${DOKAN_ROOT})

  include(FindPackageHandleStandardArgs)
  FIND_PACKAGE_HANDLE_STANDARD_ARGS(DOKAN DEFAULT_MSG DOKAN_LIBRARY DOKAN_INCLUDE_DIR)

  if (DOKAN_FOUND)
    set(DOKAN_LIBRARIES ${DOKAN_LIBRARY})
    include_directories(${DOKAN_INCLUDE_DIR})
  else(DOKAN_FOUND)
    message(FATAL_ERROR "The required library 'dokan' was not found. Please install it from http://dokan-dev.net/ first. If you installed it to a non-default location, please set DOKAN_ROOT to the directory which contains the dokan.h and dokan.lib files.")
  endif (DOKAN_FOUND)
  mark_as_advanced(DOKAN_LIBRARY)
ENDIF(WIN32)

# Set required thirdparty libraries.
if (BOOST_ROOT)
  message(STATUS "Info: BOOST_ROOT is set to: ${BOOST_ROOT}")
endif(BOOST_ROOT)
# Finding the boost libraries may fail. If existing, we point FIND_PACKAGE to /usr/lib64.
if(NOT BOOST_ROOT AND EXISTS "/usr/lib64/libboost_system.so")
  set(BOOST_LIBRARYDIR "/usr/lib64")
endif(NOT BOOST_ROOT AND EXISTS "/usr/lib64/libboost_system.so")
FIND_PACKAGE(Boost COMPONENTS system thread program_options regex REQUIRED)
if (NOT Boost_FOUND)
  message(FATAL_ERROR "The boost library was not found on your system. If needed, you can also download and compile it on your own. After compiling boost locally, set the the environment variable BOOST_ROOT to the boost base directory before executing 'make' e.g., 'export BOOST_ROOT=/Users/xyz/boost_1_47_0'.")
endif(NOT Boost_FOUND)
include_directories(${Boost_INCLUDE_DIRS})

find_package(OpenSSL QUIET)
if (NOT OPENSSL_FOUND)
# OpenSSL find_package script cannot cope with /usr/lib64 directory under Cmake 2.8.3.
  set(LIBCRYPTO "crypto")
  set(LIBSSL "ssl")
  set(OPENSSL_LIBRARIES ${LIBCRYPTO} ${LIBSSL})
endif(NOT OPENSSL_FOUND)
include_directories(${OPENSSL_INCLUDE_DIR})

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
  if(CMAKE_SIZEOF_VOID_P MATCHES "8")
    if(APPLE)
      set(LIBFUSE "fuse_ino64")
    endif(APPLE)
  endif(CMAKE_SIZEOF_VOID_P MATCHES "8")
endif(UNIX AND NOT WIN32)

SET(REQUIRED_LIBRARIES ${LIBFUSE} ${LIBATTR} ${LIBCRYPTO} ${LIBSSL} ${LIBPTHREAD})
foreach(LIB ${REQUIRED_LIBRARIES})
  #message(STATUS "checking for library: ${LIB}")
  # For unknown reasons, setting "FIND_LIBRARY_USE_LIB64_PATHS" does not fix the issue that libraries in /usr/lib64/ are not found under openSuse.
  # Therefore we just specify a list of possible library directories.
  find_library(FOUND${LIB} ${LIB} PATHS "/lib64" "/usr/lib64")
  if (NOT FOUND${LIB})
    message(FATAL_ERROR "The required library '${LIB}' was not found. Please install it on your system first.")
  #else()
  #  message(STATUS "Result of find_library: ${FOUND${LIB}}")
  endif(NOT FOUND${LIB})
endforeach(LIB)

# Define building of binaries.
##########################################
MESSAGE(STATUS "Configuring XtreemFS client (libxtreemfs, FuseAdapter(mount.xtreemfs) and Volume Tools ({mkfs,rmfs,lsfs}.xtreemfs).")

INCLUDE_DIRECTORIES(include generated)
file(GLOB_RECURSE SRCS_RPC src/rpc/*.cpp)
file(GLOB_RECURSE SRCS_UTIL src/util/*.cpp)
file(GLOB_RECURSE SRCS_GENERATED generated/*.cc)
file(GLOB_RECURSE SRCS_XTREEMFS src/libxtreemfs/*.cpp)
add_library(xtreemfs ${SRCS_RPC} ${SRCS_UTIL} ${SRCS_GENERATED} ${SRCS_XTREEMFS})
# WARNING: The resulting static libxtreemfs does NOT contain the required static library protobuf.
#          If you want to use a static libxtreemfs, you have to use a CMake file like this which does automatically add the static libxtreemfs and libprotobuf to the executable.
#          An alternative would be to change this file in such a way that the static libxtreemfs and libprotobuf are merged together into a static libxtreemfs.
#          But this would require additional CMake macros as presented here: http://www.mail-archive.com/cmake@cmake.org/msg28670.html
TARGET_LINK_LIBRARIES(xtreemfs optimized protobuf debug protobuf_debug ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${LIBPTHREAD} ${Boost_PROGRAM_OPTIONS_LIBRARY} ${OPENSSL_LIBRARIES} ${LIBSOCKET_SOLARIS} ${LIBNSL_SOLARIS})

ADD_EXECUTABLE(example_libxtreemfs src/example_libxtreemfs/example_libxtreemfs.cpp)
TARGET_LINK_LIBRARIES(example_libxtreemfs xtreemfs)

file(GLOB_RECURSE SRCS_MKFS src/mkfs.xtreemfs/*.cpp)
ADD_EXECUTABLE(mkfs.xtreemfs ${SRCS_MKFS})
TARGET_LINK_LIBRARIES(mkfs.xtreemfs xtreemfs)

file(GLOB_RECURSE SRCS_RMFS src/rmfs.xtreemfs/*.cpp)
ADD_EXECUTABLE(rmfs.xtreemfs ${SRCS_RMFS})
TARGET_LINK_LIBRARIES(rmfs.xtreemfs xtreemfs)

file(GLOB_RECURSE SRCS_LSFS src/lsfs.xtreemfs/*.cpp)
ADD_EXECUTABLE(lsfs.xtreemfs ${SRCS_LSFS})
TARGET_LINK_LIBRARIES(lsfs.xtreemfs xtreemfs)

file(GLOB_RECURSE SRCS_XTFS_UTIL src/xtfsutil/xtfsutil_server.cpp)
file(GLOB_RECURSE SRCS_JSONCPP src/json/*.cpp)

if (NOT WIN32)
  file(GLOB_RECURSE SRCS_FUSE_ADAPTER src/fuse/*.cpp)
  ADD_EXECUTABLE(mount.xtreemfs ${SRCS_FUSE_ADAPTER} ${SRCS_XTFS_UTIL} ${SRCS_JSONCPP})
  TARGET_LINK_LIBRARIES(mount.xtreemfs xtreemfs ${LIBFUSE})

  ADD_EXECUTABLE(xtfsutil src/xtfsutil/xtfsutil.cpp ${SRCS_JSONCPP})
  TARGET_LINK_LIBRARIES(xtfsutil ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${Boost_PROGRAM_OPTIONS_LIBRARY} ${Boost_REGEX_LIBRARY} ${LIBATTR})
endif(NOT WIN32)
if(WIN32)
  file(GLOB_RECURSE SRCS_DOKAN_ADAPTER src/dokan/*.cpp)
  ADD_EXECUTABLE(mount.xtreemfs ${SRCS_DOKAN_ADAPTER} ${SRCS_XTFS_UTIL} ${SRCS_JSONCPP})
  TARGET_LINK_LIBRARIES(mount.xtreemfs xtreemfs ${DOKAN_LIBRARIES})
endif(WIN32)

# Define building of unittests.
##########################################
if(BUILD_CLIENT_TESTS)
  # enable_testing() will provide a target "test".
  enable_testing()

  MESSAGE(STATUS "Configuring libxtreemfs unittests.")

  file(GLOB TESTS test/*)
  foreach (testdir ${TESTS})
    if(IS_DIRECTORY ${testdir})
      get_filename_component(testname ${testdir} NAME)
      set(testname "test_${testname}")
      file(GLOB_RECURSE SRCS_TESTS ${testdir}/*.cpp)
      if (SRCS_TESTS)
        MESSAGE(STATUS "\tConfiguring test: ${testname}.")
        add_executable(${testname} ${SRCS_TESTS})
        TARGET_LINK_LIBRARIES(${testname} optimized gtest_main debug gtest_main_debug optimized gtest debug gtest_debug xtreemfs)
        ADD_TEST(${testname} ${testname})
      endif()
    endif(IS_DIRECTORY ${testdir})
  endforeach(testdir)
endif(BUILD_CLIENT_TESTS)
