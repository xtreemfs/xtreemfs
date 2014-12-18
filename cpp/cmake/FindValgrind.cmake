find_path(VALGRIND_INCLUDE_DIR
  NAMES
    valgrind/valgrind.h
  HINTS
    /usr
    /usr/local
  PATH_SUFFIXES
    include
)

include(FindPackageHandleStandardArgs)

find_package_handle_standard_args(VALGRIND
  DEFAULT_MSG
  VALGRIND_INCLUDE_DIR
)

mark_as_advanced(VALGRIND_INCLUDE_DIR)