/* Copyright (C) 2001-2013 Peter Selinger.
   This file is part of Potrace. It is free software and it is covered
   by the GNU General Public License. See the file COPYING for details. */


#ifndef BACKEND_EPS_H
#define BACKEND_EPS_H

#include "potracelib.h"
#include "main.h"
#ifdef __cplusplus
extern "C" {
#endif
int init_ps(FILE *fout);
int page_ps(FILE *fout, potrace_path_t *plist, imginfo_t *imginfo);
int term_ps(FILE *fout);

int page_eps(FILE *fout, potrace_path_t *plist, imginfo_t *imginfo);
#ifdef  __cplusplus
} /* end of extern "C" */
#endif
#endif /* BACKEND_EPS_H */

