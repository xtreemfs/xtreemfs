cmake_minimum_required(VERSION 2.6)
PROJECT(cpp CXX)
# enable_testing() will provide a target "test".
enable_testing()

#SET (CMAKE_VERBOSE_MAKEFILE true)

add_definitions(-D_FILE_OFFSET_BITS=64)

# Set variables and required libraries.
##########################################
set(CMAKE_CXX_FLAGS "-Wall -Wno-unused-function -Wno-sign-compare")

# Set paths for required thirdparty libraries.
set(CLIENT_GOOGLE_PROTOBUF_CPP "${CMAKE_SOURCE_DIR}/thirdparty/protobuf-2.3.0")
set(CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY "${CLIENT_GOOGLE_PROTOBUF_CPP}/src/.libs/libprotobuf.a")
set(CLIENT_GOOGLE_TEST_CPP "${CMAKE_SOURCE_DIR}/thirdparty/gtest-1.5.0")
set(CLIENT_GOOGLE_TEST_CPP_LIBRARY "${CLIENT_GOOGLE_TEST_CPP}/lib/.libs/libgtest.a")
set(CLIENT_GOOGLE_TEST_CPP_MAIN "${CLIENT_GOOGLE_TEST_CPP}/lib/.libs/libgtest_main.a")

# Set required thirdparty libraries.
SET(Boost_USE_STATIC_LIBS true)
# Finding the boost libraries may fail. If existing, we point FIND_PACKAGE to /usr/lib64.
if(EXISTS "/usr/lib64/libboost_system.a")
  set(BOOST_LIBRARYDIR "/usr/lib64")
endif(EXISTS "/usr/lib64/libboost_system.a")
FIND_PACKAGE(Boost COMPONENTS system thread program_options regex REQUIRED)
include_directories(${Boost_INCLUDE_DIRS})

SET(REQUIRED_STATIC_LIBRARIES ${CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY} ${CLIENT_GOOGLE_TEST_CPP_LIBRARY} ${CLIENT_GOOGLE_TEST_CPP_MAIN})
foreach (STATIC_LIB_FULL_PATH ${REQUIRED_STATIC_LIB_FULL_PATHRARIES})
  if(NOT EXISTS ${STATIC_LIB_FULL_PATH})
    message(FATAL_ERROR "The file ${STATIC_LIB_FULL_PATH} was not found. Please run 'make client' or 'make' from the base directory in order to build the required static libraries.")
  endif()
endforeach(STATIC_LIB_FULL_PATH)

CMAKE_POLICY(SET CMP0003 OLD)
ADD_LIBRARY(protobuf STATIC IMPORTED)
SET_PROPERTY(TARGET protobuf PROPERTY IMPORTED_LOCATION ${CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY})
include_directories(${CLIENT_GOOGLE_PROTOBUF_CPP}/src)

ADD_LIBRARY(gtest STATIC IMPORTED)
ADD_LIBRARY(gtest_main STATIC IMPORTED)
SET_PROPERTY(TARGET gtest PROPERTY IMPORTED_LOCATION ${CLIENT_GOOGLE_TEST_CPP_LIBRARY})
SET_PROPERTY(TARGET gtest_main PROPERTY IMPORTED_LOCATION ${CLIENT_GOOGLE_TEST_CPP_MAIN})
include_directories(${CLIENT_GOOGLE_TEST_CPP}/include)

# Set required libraries.
set(LIBFUSE "fuse")
SET(LIBATTR "attr")
IF(APPLE)
  # No libattr on mac.
  SET(LIBATTR "")
ENDIF(APPLE)

# Check if this is a 64 Bit system
if(UNIX AND NOT WIN32)
  if(CMAKE_SIZEOF_VOID_P MATCHES "8")
    if(APPLE)
      set(LIBFUSE "fuse_ino64")
    endif(APPLE)
  endif(CMAKE_SIZEOF_VOID_P MATCHES "8")
endif(UNIX AND NOT WIN32)

SET(REQUIRED_LIBRARIES ${LIBFUSE} ${LIBATTR} "crypto" "ssl" "pthread")
foreach(LIB ${REQUIRED_LIBRARIES})
  #message(STATUS "checking for library: ${LIB}")
  find_library(FOUND${LIB} ${LIB})
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

ADD_EXECUTABLE(example_libxtreemfs src/example_libxtreemfs/example_libxtreemfs.cpp)
TARGET_LINK_LIBRARIES(example_libxtreemfs xtreemfs protobuf crypto pthread ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${Boost_PROGRAM_OPTIONS_LIBRARY} ssl)

file(GLOB_RECURSE SRCS_FUSE_ADAPTER src/fuse/*.cpp)
file(GLOB_RECURSE SRCS_XTFS_UTIL src/xtfsutil/xtfsutil_server.cpp)
file(GLOB_RECURSE SRCS_JSONCPP src/json/*.cpp)
ADD_EXECUTABLE(mount.xtreemfs ${SRCS_FUSE_ADAPTER} ${SRCS_XTFS_UTIL} ${SRCS_JSONCPP})
TARGET_LINK_LIBRARIES(mount.xtreemfs xtreemfs protobuf crypto pthread ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${Boost_PROGRAM_OPTIONS_LIBRARY} ssl ${LIBFUSE})

file(GLOB_RECURSE SRCS_MKFS src/mkfs.xtreemfs/*.cpp)
ADD_EXECUTABLE(mkfs.xtreemfs ${SRCS_MKFS})
TARGET_LINK_LIBRARIES(mkfs.xtreemfs xtreemfs protobuf crypto pthread ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${Boost_PROGRAM_OPTIONS_LIBRARY} ssl)

file(GLOB_RECURSE SRCS_RMFS src/rmfs.xtreemfs/*.cpp)
ADD_EXECUTABLE(rmfs.xtreemfs ${SRCS_RMFS})
TARGET_LINK_LIBRARIES(rmfs.xtreemfs xtreemfs protobuf crypto pthread ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${Boost_PROGRAM_OPTIONS_LIBRARY} ssl)

file(GLOB_RECURSE SRCS_LSFS src/lsfs.xtreemfs/*.cpp)
ADD_EXECUTABLE(lsfs.xtreemfs ${SRCS_LSFS})
TARGET_LINK_LIBRARIES(lsfs.xtreemfs xtreemfs protobuf crypto pthread ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${Boost_PROGRAM_OPTIONS_LIBRARY} ssl)

ADD_EXECUTABLE(xtfsutil src/xtfsutil/xtfsutil.cpp ${SRCS_JSONCPP})
TARGET_LINK_LIBRARIES(xtfsutil ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${Boost_PROGRAM_OPTIONS_LIBRARY} ${Boost_REGEX_LIBRARY} ${LIBATTR})

# Define building of unittests.
##########################################
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
      TARGET_LINK_LIBRARIES(${testname} gtest_main gtest xtreemfs protobuf crypto pthread ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${Boost_PROGRAM_OPTIONS_LIBRARY} ssl)
      ADD_TEST(${testname} ${testname})
    endif()
  endif(IS_DIRECTORY ${testdir})
endforeach(testdir)
