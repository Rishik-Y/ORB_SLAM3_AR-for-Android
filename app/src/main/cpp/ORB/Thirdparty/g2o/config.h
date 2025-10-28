#ifndef G2O_CONFIG_H
#define G2O_CONFIG_H

/* #undef G2O_HAVE_CHOLMOD */
/* #undef G2O_HAVE_CSPARSE */
#define G2O_HAVE_OPENGL 1
/* #undef G2O_OPENGL_FOUND */
/* #undef G2O_OPENMP */
#define G2O_SHARED_LIBS 1
/* #undef G2O_LGPL_SHARED_LIBS */

// Version
#define G2O_CXX_COMPILER "Android NDK"

// available sparse matrix libraries
#define G2O_HAVE_EIGEN 1

#define G2O_USE_VENDORED_CERES

#endif // G2O_CONFIG_H
