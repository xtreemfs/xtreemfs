# gf-complete autoconf template

# FIXME - add project url as the last argument
AC_INIT(gf-complete, 1.0)

# Override default CFLAGS
: ${CFLAGS="-Wall -Wpointer-arith -O3 -g"}

AC_PREREQ([2.61])

AM_INIT_AUTOMAKE([no-dependencies foreign parallel-tests])
LT_INIT # libtool

AC_CONFIG_HEADER(include/config.h)

dnl Needed when reconfiguring with 'autoreconf -i -s'
AC_CONFIG_MACRO_DIR([m4])

# This prevents './configure; make' from trying to run autotools.
AM_MAINTAINER_MODE([disable])

dnl Compiling with per-target flags requires AM_PROG_CC_C_O.
AC_PROG_CC

# Check for functions to provide aligned memory
#
AC_CHECK_FUNCS([posix_memalign],
 [found_memalign=yes; break])

AS_IF([test "x$found_memalign" != "xyes"], [AC_MSG_WARN([No function for aligned memory allocation found])])

AX_EXT()

AC_ARG_ENABLE([neon],
              AS_HELP_STRING([--disable-neon], [Build without NEON optimizations]))

AS_IF([test "x$enable_neon" != "xno"],
      [noneon_CPPFLAGS=$CPPFLAGS
       CPPFLAGS="$CPPFLAGS $SIMD_FLAGS"
       AC_CHECK_HEADER([arm_neon.h],
                       [have_neon=yes],
                       [have_neon=no
                        CPPFLAGS=$noneon_CPPFLAGS])],
      [have_neon=no
       AS_IF([test "x$ax_cv_have_neon_ext" = "xyes"],
             [SIMD_FLAGS=""])
      ])

AS_IF([test "x$have_neon" = "xno"],
      [AS_IF([test "x$enable_neon" = "xyes"],
             [AC_MSG_ERROR([neon requested but arm_neon.h not found])])
      ])
AM_CONDITIONAL([HAVE_NEON], [test "x$have_neon" = "xyes"])

AC_ARG_ENABLE([sse],
              AS_HELP_STRING([--disable-sse], [Build without SSE optimizations]),
              [if   test "x$enableval" = "xno" ; then
                SIMD_FLAGS=""
                echo "DISABLED SSE!!!"
              fi]
)

AC_CONFIG_FILES([Makefile src/Makefile tools/Makefile test/Makefile examples/Makefile])
AC_OUTPUT
