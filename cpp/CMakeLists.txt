cmake_minimum_required(VERSION 2.6)
PROJECT(cpp CXX)
# enable_testing() will provide a target "test".
enable_testing()
option(build_thirdparty "Executes ./configure and make commands on required thirdparty libraries" ON)

#SET (CMAKE_VERBOSE_MAKEFILE true)

ADD_DEFINITIONS(-D_FILE_OFFSET_BITS=64)

##########################################

CMAKE_POLICY(SET CMP0003 OLD)
ADD_LIBRARY(protobuf STATIC IMPORTED)

# build the protobuf library
IF(UNIX)
  IF (build_thirdparty)
    MESSAGE(STATUS "building protobuf")
    EXECUTE_PROCESS(COMMAND ./configure WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/thirdparty/protobuf-2.3.0)
    EXECUTE_PROCESS(COMMAND make WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/thirdparty/protobuf-2.3.0)
  ENDIF(build_thirdparty)

  SET_PROPERTY(TARGET protobuf PROPERTY IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/thirdparty/protobuf-2.3.0/src/.libs/libprotobuf.a)
ENDIF(UNIX)
# TODO: add ELSEIF for WIN32/WIN64

include_directories(${CMAKE_SOURCE_DIR}/thirdparty/protobuf-2.3.0/src)
include_directories(${CMAKE_SOURCE_DIR}/thirdparty/protobuf-2.3.0)

##########################################
MESSAGE(STATUS "Configuring XtreemFS client (libxtreemfs, FuseAdapter(mount.xtreemfs) and Volume Tools ({mkfs,rmfs,lsfs}.xtreemfs).")

SET(BOOST_LIBRARYDIR "/usr/lib64")
FIND_PACKAGE(Boost COMPONENTS system thread program_options regex REQUIRED)

INCLUDE_DIRECTORIES(include generated)

file(GLOB_RECURSE SRCS_RPC src/rpc/*.cpp)
file(GLOB_RECURSE SRCS_UTIL src/util/*.cpp)
file(GLOB_RECURSE SRCS_GENERATED generated/*.cc)
file(GLOB_RECURSE SRCS_XTREEMFS src/libxtreemfs/*.cpp)
add_library(xtreemfs ${SRCS_RPC} ${SRCS_UTIL} ${SRCS_GENERATED} ${SRCS_XTREEMFS})

set(CMAKE_CXX_FLAGS "-Wall -Wno-unused-function -Wno-sign-compare")

ADD_EXECUTABLE(example_libxtreemfs src/example_libxtreemfs/example_libxtreemfs.cpp)
TARGET_LINK_LIBRARIES(example_libxtreemfs xtreemfs protobuf crypto pthread ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${Boost_PROGRAM_OPTIONS_LIBRARY} ssl)

file(GLOB_RECURSE SRCS_FUSE_ADAPTER src/fuse/*.cpp)
file(GLOB_RECURSE SRCS_XTFS_UTIL src/xtfsutil/xtfsutil_server.cpp)
file(GLOB_RECURSE SRCS_JSONCPP src/json/*.cpp)
ADD_EXECUTABLE(mount.xtreemfs ${SRCS_FUSE_ADAPTER} ${SRCS_XTFS_UTIL} ${SRCS_JSONCPP})
TARGET_LINK_LIBRARIES(mount.xtreemfs xtreemfs protobuf crypto pthread ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${Boost_PROGRAM_OPTIONS_LIBRARY} ssl fuse)

file(GLOB_RECURSE SRCS_MKFS src/mkfs.xtreemfs/*.cpp)
ADD_EXECUTABLE(mkfs.xtreemfs ${SRCS_MKFS})
TARGET_LINK_LIBRARIES(mkfs.xtreemfs xtreemfs protobuf crypto pthread ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${Boost_PROGRAM_OPTIONS_LIBRARY} ssl fuse)

file(GLOB_RECURSE SRCS_RMFS src/rmfs.xtreemfs/*.cpp)
ADD_EXECUTABLE(rmfs.xtreemfs ${SRCS_RMFS})
TARGET_LINK_LIBRARIES(rmfs.xtreemfs xtreemfs protobuf crypto pthread ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${Boost_PROGRAM_OPTIONS_LIBRARY} ssl fuse)

file(GLOB_RECURSE SRCS_LSFS src/lsfs.xtreemfs/*.cpp)
ADD_EXECUTABLE(lsfs.xtreemfs ${SRCS_LSFS})
TARGET_LINK_LIBRARIES(lsfs.xtreemfs xtreemfs protobuf crypto pthread ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${Boost_PROGRAM_OPTIONS_LIBRARY} ssl fuse)

ADD_EXECUTABLE(xtfsutil src/xtfsutil/xtfsutil.cpp ${SRCS_JSONCPP})
TARGET_LINK_LIBRARIES(xtfsutil ${Boost_SYSTEM_LIBRARY} ${Boost_THREAD_LIBRARY} ${Boost_PROGRAM_OPTIONS_LIBRARY} ${Boost_REGEX_LIBRARY} attr)

##########################################

CMAKE_POLICY(SET CMP0003 OLD)
ADD_LIBRARY(gtest STATIC IMPORTED)
ADD_LIBRARY(gtest_main STATIC IMPORTED)

# build the gtest library
IF(UNIX)
  IF (build_thirdparty)
    MESSAGE(STATUS "building gtest")
    EXECUTE_PROCESS(COMMAND ./configure WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/thirdparty/gtest-1.5.0)
    EXECUTE_PROCESS(COMMAND make WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/thirdparty/gtest-1.5.0)
  ENDIF(build_thirdparty)

  SET_PROPERTY(TARGET gtest PROPERTY IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/thirdparty/gtest-1.5.0/lib/.libs/libgtest.a)
  SET_PROPERTY(TARGET gtest_main PROPERTY IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/thirdparty/gtest-1.5.0/lib/.libs/libgtest_main.a)
ENDIF(UNIX)
# TODO: add ELSEIF for WIN32/WIN64

include_directories(${CMAKE_SOURCE_DIR}/thirdparty/gtest-1.5.0/include)

##########################################
MESSAGE(STATUS "Configuring libxtreemfs unittests.")

SET(BOOST_LIBRARYDIR "/usr/lib64")
FIND_PACKAGE(Boost COMPONENTS system thread program_options REQUIRED)

INCLUDE_DIRECTORIES(include generated)

set(CMAKE_CXX_FLAGS "-Wall -Wno-unused-function -Wno-sign-compare")

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
  #endif(if(IS_DIRECTORY $entry)
  endif()
endforeach()
