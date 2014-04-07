/* Copyright (C) 2001-2013 Peter Selinger.
   This file is part of Potrace. It is free software and it is covered
   by the GNU General Public License. See the file COPYING for details. */


#ifndef BACKEND_PGM_H
#define BACKEND_PGM_H

#include <stdio.h>

#include "potracelib.h"
#include "main.h"
#ifdef __cplusplus
extern "C" {
#endif
int page_pgm(FILE *fout, potrace_path_t *plist, imginfo_t *imginfo);
#ifdef  __cplusplus
} /* end of extern "C" */
#endif
#endif /* BACKEND_PGM_H */
