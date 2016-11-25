#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <getopt.h>
#include <math.h>

#include "potrace/potracelib.h"
#include "potrace/backend_pdf.h"
#include "potrace/backend_eps.h"
#include "potrace/backend_pgm.h"
#include "potrace/backend_svg.h"
#include "potrace/backend_xfig.h"
#include "potrace/backend_dxf.h"
#include "potrace/backend_geojson.h"
#include "potrace/potracelib.h"
#include "potrace/bitmap.h"
#include "potrace/platform.h"
#include "potrace/auxiliary.h"
#include "potrace/progress_bar.h"
#include "potrace/trans.h"
#include "potrace/curve.h"
#include <android/bitmap.h>
#include <android/log.h>
#define LOG_TAG "libbitmaputils"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

struct info_s info;
extern "C"
{
union pixel32_t {
  uint32_t rgba32;
  uint8_t rgba8[4];  // 0: red 1:green 2:blue 3:alpha
};

typedef union pixel32_t pixel32_t;
potrace_state_t* s_state = NULL;

#define UNDEF ((double)(1e30))   /* a value to represent "undefined" */

/* ---------------------------------------------------------------------- */
/* some data structures for option processing */

struct pageformat_s {
  char *name;
  int w, h;
};
typedef struct pageformat_s pageformat_t;

/* dimensions of the various page formats, in postscript points */
static pageformat_t pageformat[] = {
  { "a4",        595,  842 },
  { "a3",        842, 1191 },
  { "a5",        421,  595 },
  { "b5",        516,  729 },
  { "letter",    612,  792 },
  { "legal",     612, 1008 },
  { "tabloid",   792, 1224 },
  { "statement", 396,  612 },
  { "executive", 540,  720 },
  { "folio",     612,  936 },
  { "quarto",    610,  780 },
  { "10x14",     720, 1008 },
  { NULL, 0, 0 },
};

struct turnpolicy_s {
  char *name;
  int n;
};
typedef struct turnpolicy_s turnpolicy_t;

/* names of turn policies */
static turnpolicy_t turnpolicy[] = {
  {"black",    POTRACE_TURNPOLICY_BLACK},
  {"white",    POTRACE_TURNPOLICY_WHITE},
  {"left",     POTRACE_TURNPOLICY_LEFT},
  {"right",    POTRACE_TURNPOLICY_RIGHT},
  {"minority", POTRACE_TURNPOLICY_MINORITY},
  {"majority", POTRACE_TURNPOLICY_MAJORITY},
  {"random",   POTRACE_TURNPOLICY_RANDOM},
  {NULL, 0},
};

/* backends and their characteristics */
struct backend_s {
  char *name;       /* name of this backend */
  char *ext;        /* file extension */
  int fixed;        /* fixed page size backend? */
  int pixel;        /* pixel-based backend? */
  int multi;        /* multi-page backend? */
  int (*init_f)(FILE *fout);                 /* initialization function */
  int (*page_f)(FILE *fout, potrace_path_t *plist, imginfo_t *imginfo);
                                             /* per-bitmap function */
  int (*term_f)(FILE *fout);                 /* finalization function */
  int opticurve;    /* opticurve capable (true Bezier curves?) */
};
typedef struct backend_s backend_t;

static backend_t backend[] = {
  { "eps",        ".eps", 0, 0, 0,   NULL,     page_eps,     NULL,     1 },
  { "postscript", ".ps",  1, 0, 1,   init_ps,  page_ps,      term_ps,  1 },
  { "ps",         ".ps",  1, 0, 1,   init_ps,  page_ps,      term_ps,  1 },
  { "pdf",        ".pdf", 0, 0, 1,   init_pdf, page_pdf,     term_pdf, 1 },
  { "pdfpage",    ".pdf", 1, 0, 1,   init_pdf, page_pdfpage, term_pdf, 1 },
  { "svg",        ".svg", 0, 0, 0,   NULL,     page_svg,     NULL,     1 },
  { "dxf",        ".dxf", 0, 1, 0,   NULL,     page_dxf,     NULL,     1 },
  { "geojson",    ".json",0, 1, 0,   NULL,     page_geojson, NULL,     1 },
  { "pgm",        ".pgm", 0, 1, 1,   NULL,     page_pgm,     NULL,     1 },
  { "gimppath",   ".svg", 0, 1, 0,   NULL,     page_gimp,    NULL,     1 },
  { "xfig",       ".fig", 1, 0, 0,   NULL,     page_xfig,    NULL,     0 },
  { NULL, NULL, 0, 0, 0, NULL, NULL, NULL, 0 },
};

/* look up a backend by name. If found, return 0 and set *bp. If not
   found leave *bp unchanged and return 1, or 2 on ambiguous
   prefix. */
static int backend_lookup(char *name, backend_t **bp) {
  int i;
  int m=0;  /* prefix matches */
  backend_t *b = NULL;

  for (i=0; backend[i].name; i++) {
    if (strcasecmp(backend[i].name, name)==0) {
      *bp = &backend[i];
      return 0;
    } else if (strncasecmp(backend[i].name, name, strlen(name))==0) {
      m++;
      b = &backend[i];
    }
  }
  /* if there was no exact match, and exactly one prefix match, use that */
  if (m==1) {
    *bp = b;
    return 0;
  } else if (m) {
    return 2;
  } else {
    return 1;
  }
}

/* list all available backends by name, in a comma separated list.
   Assume the cursor starts in column j, and break lines at length
   linelen. Do not output any trailing punctuation. Return the column
   the cursor is in. */
static int backend_list(FILE *fout, int j, int linelen) {
  int i;

  for (i=0; backend[i].name; i++) {
    if (j + (int)strlen(backend[i].name) > linelen) {
      fprintf(fout, "\n");
      j = 0;
    }
    j += fprintf(fout, "%s", backend[i].name);
    if (backend[i+1].name) {
      j += fprintf(fout, ", ");
    }
  }
  return j;
}

/* ---------------------------------------------------------------------- */
/* some info functions */


/* ---------------------------------------------------------------------- */
/* auxiliary functions for parameter parsing */

/* parse a dimension of the kind "1.5in", "7cm", etc. Return result in
   postscript points (=1/72 in). If endptr!=NULL, store pointer to
   next character in *endptr in the manner of strtod(3). */
static dim_t parse_dimension(char *s, char **endptr) {
  char *p;
  dim_t res;

  res.x = strtod(s, &p);
  res.d = 0;
  if (p!=s) {
    if (!strncasecmp(p, "in", 2)) {
      res.d = DIM_IN;
      p += 2;
    } else if (!strncasecmp(p, "cm", 2)) {
      res.d = DIM_CM;
      p += 2;
    } else if (!strncasecmp(p, "mm", 2)) {
      res.d = DIM_MM;
      p += 2;
    } else if (!strncasecmp(p, "pt", 2)) {
      res.d = DIM_PT;
      p += 2;
    }
  }
  if (endptr!=NULL) {
    *endptr = p;
  }
  return res;
}

/* parse a pair of dimensions, such as "8.5x11in", "30mmx4cm" */
static void parse_dimensions(char *s, char **endptr, dim_t *dxp, dim_t *dyp) {
  char *p, *q;
  dim_t dx, dy;

  dx = parse_dimension(s, &p);
  if (p==s) {
    goto fail;
  }
  if (*p != 'x') {
    goto fail;
  }
  p++;
  dy = parse_dimension(p, &q);
  if (q==p) {
    goto fail;
  }
  if (dx.d && !dy.d) {
    dy.d = dx.d;
  } else if (!dx.d && dy.d) {
    dx.d = dy.d;
  }
  *dxp = dx;
  *dyp = dy;
  if (endptr != NULL) {
    *endptr = q;
  }
  return;

 fail:
  dx.x = dx.d = dy.x = dy.d = 0;
  *dxp = dx;
  *dyp = dy;
  if (endptr != NULL) {
    *endptr = s;
  }
  return;
}

static inline double double_of_dim(dim_t d, double def) {
  if (d.d) {
    return d.x * d.d;
  } else {
    return d.x * def;
  }
}

static int parse_color(char *s) {
  int i, d;
  int col = 0;

  if (s[0] != '#' || strlen(s) != 7) {
    return -1;
  }
  for (i=0; i<6; i++) {
    d = s[6-i];
    if (d >= '0' && d <= '9') {
      col |= (d-'0') << (4*i);
    } else if (d >= 'a' && d <= 'f') {
      col |= (d-'a'+10) << (4*i);
    } else if (d >= 'A' && d <= 'F') {
      col |= (d-'A'+10) << (4*i);
    } else {
      return -1;
    }
  }
  return col;
}

/* ---------------------------------------------------------------------- */
/* option processing */

/* codes for options that don't have short form */
enum {
  OPT_TIGHT = 300,
  OPT_FILLCOLOR,
  OPT_OPAQUE,
  OPT_GROUP,
  OPT_FLAT,
  OPT_PROGRESS,
  OPT_TTY
};

static struct option longopts[] = {
  {"help",          0, 0, 'h'},
  {"version",       0, 0, 'v'},
  {"show-defaults", 0, 0, 'V'}, /* undocumented option for compatibility */
  {"license",       0, 0, 'l'},
  {"width",         1, 0, 'W'},
  {"height",        1, 0, 'H'},
  {"resolution",    1, 0, 'r'},
  {"scale",         1, 0, 'x'},
  {"stretch",       1, 0, 'S'},
  {"margin",        1, 0, 'M'},
  {"leftmargin",    1, 0, 'L'},
  {"rightmargin",   1, 0, 'R'},
  {"topmargin",     1, 0, 'T'},
  {"bottommargin",  1, 0, 'B'},
  {"tight",         0, 0, OPT_TIGHT},
  {"rotate",        1, 0, 'A'},
  {"pagesize",      1, 0, 'P'},
  {"turdsize",      1, 0, 't'},
  {"unit",          1, 0, 'u'},
  {"cleartext",     0, 0, 'c'},
  {"level2",        0, 0, '2'},
  {"level3",        0, 0, '3'},
  {"eps",           0, 0, 'e'},
  {"postscript",    0, 0, 'p'},
  {"svg",           0, 0, 's'},
  {"pgm",           0, 0, 'g'},
  {"backend",       1, 0, 'b'},
  {"debug",         1, 0, 'd'},
  {"color",         1, 0, 'C'},
  {"fillcolor",     1, 0, OPT_FILLCOLOR},
  {"turnpolicy",    1, 0, 'z'},
  {"gamma",         1, 0, 'G'},
  {"longcurve",     0, 0, 'n'},
  {"longcoding",    0, 0, 'q'},
  {"alphamax",      1, 0, 'a'},
  {"opttolerance",  1, 0, 'O'},
  {"output",        1, 0, 'o'},
  {"blacklevel",    1, 0, 'k'},
  {"invert",        0, 0, 'i'},
  {"opaque",        0, 0, OPT_OPAQUE},
  {"group",         0, 0, OPT_GROUP},
  {"flat",          0, 0, OPT_FLAT},
  {"progress",      0, 0, OPT_PROGRESS},
  {"tty",           1, 0, OPT_TTY},

  {0, 0, 0, 0}
};

static char *shortopts = "hvVlW:H:r:x:S:M:L:R:T:B:A:P:t:u:c23epsgb:d:C:z:G:nqa:O:o:k:i";


/* ---------------------------------------------------------------------- */
/* calculations with bitmap dimensions, positioning etc */

/* determine the dimensions of the output based on command line and
   image dimensions, and optionally, based on the actual image outline. */
static void calc_dimensions(imginfo_t *imginfo, potrace_path_t *plist) {
  double dim_def;
  double maxwidth, maxheight, sc;
  int default_scaling = 0;

  /* we take care of a special case: if one of the image dimensions is
     0, we change it to 1. Such an image is empty anyway, so there
     will be 0 paths in it. Changing the dimensions avoids division by
     0 error in calculating scaling factors, bounding boxes and
     such. This doesn't quite do the right thing in all cases, but it
     is better than causing overflow errors or "nan" output in
     backends.  Human users don't tend to process images of size 0
     anyway; they might occur in some pipelines. */
  if (imginfo->pixwidth == 0) {
    imginfo->pixwidth = 1;
  }
  if (imginfo->pixheight == 0) {
    imginfo->pixheight = 1;
  }

  /* set the default dimension for width, height, margins */
  if (info.backend->pixel) {
    dim_def = DIM_PT;
  } else {
    dim_def = DEFAULT_DIM;
  }

  /* apply default dimension to width, height, margins */
  imginfo->width = info.width_d.x == UNDEF ? UNDEF : double_of_dim(info.width_d, dim_def);
  imginfo->height = info.height_d.x == UNDEF ? UNDEF : double_of_dim(info.height_d, dim_def);
  imginfo->lmar = info.lmar_d.x == UNDEF ? UNDEF : double_of_dim(info.lmar_d, dim_def);
  imginfo->rmar = info.rmar_d.x == UNDEF ? UNDEF : double_of_dim(info.rmar_d, dim_def);
  imginfo->tmar = info.tmar_d.x == UNDEF ? UNDEF : double_of_dim(info.tmar_d, dim_def);
  imginfo->bmar = info.bmar_d.x == UNDEF ? UNDEF : double_of_dim(info.bmar_d, dim_def);

  /* start with a standard rectangle */
  trans_from_rect(&imginfo->trans, imginfo->pixwidth, imginfo->pixheight);

  /* if info.tight is set, tighten the bounding box */
  if (info.tight) {
    trans_tighten(&imginfo->trans, plist);
  }

  /* sx/rx is just an alternate way to specify width; sy/ry is just an
     alternate way to specify height. */
  if (info.backend->pixel) {
    if (imginfo->width == UNDEF && info.sx != UNDEF) {
      imginfo->width = imginfo->trans.bb[0] * info.sx;
    }
    if (imginfo->height == UNDEF && info.sy != UNDEF) {
      imginfo->height = imginfo->trans.bb[1] * info.sy;
    }
  } else {
    if (imginfo->width == UNDEF && info.rx != UNDEF) {
      imginfo->width = imginfo->trans.bb[0] / info.rx * 72;
    }
    if (imginfo->height == UNDEF && info.ry != UNDEF) {
      imginfo->height = imginfo->trans.bb[1] / info.ry * 72;
    }
  }

  /* if one of width/height is specified, use stretch to determine the
     other */
  if (imginfo->width == UNDEF && imginfo->height != UNDEF) {
    imginfo->width = imginfo->height / imginfo->trans.bb[1] * imginfo->trans.bb[0] / info.stretch;
  } else if (imginfo->width != UNDEF && imginfo->height == UNDEF) {
    imginfo->height = imginfo->width / imginfo->trans.bb[0] * imginfo->trans.bb[1] * info.stretch;
  }

  /* if width and height are still variable, tenatively use the
     default scaling factor of 72dpi (for dimension-based backends) or
     1 (for pixel-based backends). For fixed-size backends, this will
     be adjusted later to fit the page. */
  if (imginfo->width == UNDEF && imginfo->height == UNDEF) {
    imginfo->width = imginfo->trans.bb[0];
    imginfo->height = imginfo->trans.bb[1] * info.stretch;
    default_scaling = 1;
  }

  /* apply scaling */
  trans_scale_to_size(&imginfo->trans, imginfo->width, imginfo->height);

  /* apply rotation, and tighten the bounding box again, if necessary */
  if (info.angle != 0.0) {
    trans_rotate(&imginfo->trans, info.angle);
    if (info.tight) {
      trans_tighten(&imginfo->trans, plist);
    }
  }

  /* for fixed-size backends, if default scaling was in effect,
     further adjust the scaling to be the "best fit" for the given
     page size and margins. */
  if (default_scaling && info.backend->fixed) {

    /* try to squeeze it between margins */
    maxwidth = UNDEF;
    maxheight = UNDEF;

    if (imginfo->lmar != UNDEF && imginfo->rmar != UNDEF) {
      maxwidth = info.paperwidth - imginfo->lmar - imginfo->rmar;
    }
    if (imginfo->bmar != UNDEF && imginfo->tmar != UNDEF) {
      maxheight = info.paperheight - imginfo->bmar - imginfo->tmar;
    }
    if (maxwidth == UNDEF && maxheight == UNDEF) {
      maxwidth = max(info.paperwidth - 144, info.paperwidth * 0.75);
      maxheight = max(info.paperheight - 144, info.paperheight * 0.75);
    }

    if (maxwidth == UNDEF) {
      sc = maxheight / imginfo->trans.bb[1];
    } else if (maxheight == UNDEF) {
      sc = maxwidth / imginfo->trans.bb[0];
    } else {
      sc = min(maxwidth / imginfo->trans.bb[0], maxheight / imginfo->trans.bb[1]);
    }

    /* re-scale coordinate system */
    imginfo->width *= sc;
    imginfo->height *= sc;
    trans_rescale(&imginfo->trans, sc);
  }

  /* adjust margins */
  if (info.backend->fixed) {
    if (imginfo->lmar == UNDEF && imginfo->rmar == UNDEF) {
      imginfo->lmar = (info.paperwidth-imginfo->trans.bb[0])/2;
    } else if (imginfo->lmar == UNDEF) {
      imginfo->lmar = (info.paperwidth-imginfo->trans.bb[0]-imginfo->rmar);
    } else if (imginfo->lmar != UNDEF && imginfo->rmar != UNDEF) {
      imginfo->lmar += (info.paperwidth-imginfo->trans.bb[0]-imginfo->lmar-imginfo->rmar)/2;
    }
    if (imginfo->bmar == UNDEF && imginfo->tmar == UNDEF) {
      imginfo->bmar = (info.paperheight-imginfo->trans.bb[1])/2;
    } else if (imginfo->bmar == UNDEF) {
      imginfo->bmar = (info.paperheight-imginfo->trans.bb[1]-imginfo->tmar);
    } else if (imginfo->bmar != UNDEF && imginfo->tmar != UNDEF) {
      imginfo->bmar += (info.paperheight-imginfo->trans.bb[1]-imginfo->bmar-imginfo->tmar)/2;
    }
  } else {
    if (imginfo->lmar == UNDEF) {
      imginfo->lmar = 0;
    }
    if (imginfo->rmar == UNDEF) {
      imginfo->rmar = 0;
    }
    if (imginfo->bmar == UNDEF) {
      imginfo->bmar = 0;
    }
    if (imginfo->tmar == UNDEF) {
      imginfo->tmar = 0;
    }
  }
}

/* ---------------------------------------------------------------------- */
/* auxiliary functions for file handling */

/* open a file for reading. Return stdin if filename is NULL or "-" */
static FILE *my_fopen_read(char *filename) {
  if (filename == NULL || strcmp(filename, "-") == 0) {
    return stdin;
  }
  return fopen(filename, "rb");
}

/* open a file for writing. Return stdout if filename is NULL or "-" */
static FILE *my_fopen_write(char *filename) {
  if (filename == NULL || strcmp(filename, "-") == 0) {
    return stdout;
  }
  return fopen(filename, "wb");
}

/* close a file, but do nothing is filename is NULL or "-" */
static void my_fclose(FILE *f, char *filename) {
  if (filename == NULL || strcmp(filename, "-") == 0) {
    return;
  }
  fclose(f);
}

/* make output filename from input filename. Return an allocated value. */
static char *make_outfilename(char *infile, char *ext) {
  char *outfile;
  char *p;

  if (strcmp(infile, "-") == 0) {
    return strdup("-");
  }

  outfile = (char *) malloc(strlen(infile)+strlen(ext)+5);
  if (!outfile) {
    return NULL;
  }
  strcpy(outfile, infile);
  p = strrchr(outfile, '.');
  if (p) {
    *p = 0;
  }
  strcat(outfile, ext);

  /* check that input and output filenames are different */
  if (strcmp(infile, outfile) == 0) {
    strcpy(outfile, infile);
    strcat(outfile, "-out");
  }

  return outfile;
}

/* ---------------------------------------------------------------------- */
/* Process one infile */

/* Process one or more bitmaps from fin, and write the results to fout
   using the page_f function of the appropriate backend. */


/* ---------------------------------------------------------------------- */
/* main: handle file i/o */

#define TRY(x) if (x) goto try_error

/*
jobject createPoint(JNIEnv* env, point_s* pt)
{
	jobject ret;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/point");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
	ret = env->NewObject(cls, constructor);

	jfieldID fid = env->GetFieldID(cls, "x", "L");
	env->SetLongField(ret, fid, pt->x);

	fid = env->GetFieldID(cls, "y", "L");
	env->SetLongField(ret, fid, pt->y);
	return ret;
}

jobject createiPoint(JNIEnv* env, ipoint_t* pt)
{
	jobject ret;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/ipoint");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "void(V)");
	ret = env->NewObject(cls, constructor);

	jfieldID fid = env->GetFieldID(cls, "x", "I");
	env->SetIntField(ret, fid, pt->x);

	fid = env->GetFieldID(cls, "y", "I");
	env->SetIntField(ret, fid, pt->y);
	return ret;
}
*/

jobject createdPoint(JNIEnv* env, dpoint_t* pt)
{
	jobject ret;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/dpoint");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
	ret = env->NewObject(cls, constructor);

	jfieldID fid = env->GetFieldID(cls, "x", "D");
	env->SetDoubleField(ret, fid, pt->x);

	fid = env->GetFieldID(cls, "y", "D");
	env->SetDoubleField(ret, fid, pt->y);
	env->DeleteLocalRef(cls);
	return ret;
}

/*
jobject createDummyDPoint(JNIEnv* env)
{
	jobject ret;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/dpoint");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
	ret = env->NewObject(cls, constructor);

	jfieldID fid = env->GetFieldID(cls, "x", "D");
	env->SetDoubleField(ret, fid, 0.0);

	fid = env->GetFieldID(cls, "y", "D");
	env->SetDoubleField(ret, fid, 0.0);
	return ret;
}

jobject createSums(JNIEnv* env, sums_t* s)
{
	jobject ret;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/sums");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
	ret = env->NewObject(cls, constructor);

	jfieldID fid = env->GetFieldID(cls, "x", "I");
	env->SetIntField(ret, fid, s->x);

	fid = env->GetFieldID(cls, "y", "I");
	env->SetIntField(ret, fid, s->y);

	fid = env->GetFieldID(cls, "xy", "I");
	env->SetIntField(ret, fid, s->xy);

	fid = env->GetFieldID(cls, "x2", "I");
	env->SetIntField(ret, fid, s->x2);

	fid = env->GetFieldID(cls, "y2", "I");
	env->SetIntField(ret, fid, s->y2);
	return ret;
}

jobject createprevCurve(JNIEnv* env, privcurve_t* c)
{
	jobject ret;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/privcurve");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
	ret = env->NewObject(cls, constructor);

	jfieldID fid = env->GetFieldID(cls, "n", "I");
	env->SetIntField(ret, fid, c->n);

	jintArray tags = env->NewIntArray(c->n);
	/*
	//jint* ia = new jint[c->n];
	//for(int i = 0; i < c->n; ++c->n)
	//{
	//	ia[i] = c->tag[i];
	//}
	env->SetIntArrayRegion(tags, 0, c->n, ia);
	delete[] ia;
	env->SetIntArrayRegion(tags, 0, c->n, c->tag);
	fid = env->GetFieldID(cls, "tag", "[I");
	env->SetObjectField(ret, fid, tags);

	cls = env->FindClass("com/jiangpeng/android/antrace/Objects/dpoint");
//	jobject newDPt = createDummyDPoint(env);
//	jobjectArray initarr = env->NewObjectArray(3, cls, newDPt);
	jobjectArray arr = env->NewObjectArray(c->n, env->FindClass("[Ljava/lang/Object;"), 0);
	for(int i = 0; i < c->n; ++i)
	{
		jobjectArray ptarr = env->NewObjectArray(3, cls, 0);
		for(int j = 0; j < 3; ++j)
		{
			jobject pt = createdPoint(env, &c->c[i][j]);
			env->SetObjectArrayElement(ptarr, j, pt);
		}
		env->SetObjectArrayElement(arr, i, ptarr);
	}
	fid = env->GetFieldID(cls, "c", "[[O");
	env->SetObjectField(ret, fid, arr);

	fid = env->GetFieldID(cls, "alphacurve", "I");
	env->SetIntField(ret, fid, c->alphacurve);
	int hasAlpha = c->alphacurve;
	if(hasAlpha)
	{
		cls = env->FindClass("com/jiangpeng/android/antrace/Objects/dpoint");
		jobjectArray arr = env->NewObjectArray(c->n, env->FindClass("[Ljava/lang/Object;"), 0);
		for(int i = 0; i < c->n; ++i)
		{
			jobject pt = createdPoint(env, &c->vertex[i]);
			env->SetObjectArrayElement(arr, i, pt);
		}

		fid = env->GetFieldID(cls, "vertex", "[O");
		env->SetObjectField(ret, fid, arr);

		jdoubleArray alpha = env->NewDoubleArray(c->n);
		env->SetDoubleArrayRegion(alpha, 0, c->n, c->alpha);
		fid = env->GetFieldID(cls, "alpha", "[D");
		env->SetObjectField(ret, fid, alpha);

		jdoubleArray alpha0 = env->NewDoubleArray(c->n);
		env->SetDoubleArrayRegion(alpha0, 0, c->n, c->alpha0);
		fid = env->GetFieldID(cls, "alpha0", "[D");
		env->SetObjectField(ret, fid, alpha0);

		jdoubleArray beta = env->NewDoubleArray(c->n);
		env->SetDoubleArrayRegion(beta, 0, c->n, c->beta);
		fid = env->GetFieldID(cls, "beta", "[D");
		env->SetObjectField(ret, fid, beta);
	}
	return ret;
}
*/

jobject createCurve(JNIEnv* env, potrace_curve_t* c)
{
	jobject ret;
	jclass crvcls = env->FindClass("com/jiangpeng/android/antrace/Objects/curve");
	jmethodID constructor = env->GetMethodID(crvcls, "<init>", "()V");
	ret = env->NewObject(crvcls, constructor);

	jfieldID fid = env->GetFieldID(crvcls, "n", "I");
	env->SetIntField(ret, fid, c->n);

	jintArray tags = env->NewIntArray(c->n);
	jint* ia = new jint[c->n];
	for(int i = 0; i < c->n; ++i)
	{
		ia[i] = c->tag[i];
	}
	env->SetIntArrayRegion(tags, 0, c->n, ia);
	delete[] ia;

	fid = env->GetFieldID(crvcls, "tag", "[I");
	env->SetObjectField(ret, fid, tags);
	env->DeleteLocalRef(tags);
	
	// the class is com.jiangpeng.android.antrace.Objects.dpoint
	jclass dptcls = env->FindClass("com/jiangpeng/android/antrace/Objects/dpoint");
	// one dimension array
	jclass d1cls = env->FindClass("[Lcom/jiangpeng/android/antrace/Objects/dpoint;");
	// two dimension array object
	jobjectArray arr = env->NewObjectArray(c->n, d1cls, 0);
	// fill arrays
	for(int i = 0; i < c->n; ++i)
	{
		jobjectArray ptarr = env->NewObjectArray(3, dptcls, 0);
		for(int j = 0; j < 3; ++j)
		{
			jobject pt = createdPoint(env, &c->c[i][j]);
			env->SetObjectArrayElement(ptarr, j, pt);
			env->DeleteLocalRef(pt);
		}
		env->SetObjectArrayElement(arr, i, ptarr);
		env->DeleteLocalRef(ptarr);
	}
	// set field with arr, note the last string parameter, ret is the object we created, crvcls is class type
	fid = env->GetFieldID(crvcls, "c", "[[Lcom/jiangpeng/android/antrace/Objects/dpoint;");
	env->SetObjectField(ret, fid, arr);
	// do not forget delete local references
	env->DeleteLocalRef(dptcls);
	env->DeleteLocalRef(arr);
	env->DeleteLocalRef(d1cls);
	env->DeleteLocalRef(crvcls);
	return ret;
}

/*
jobject createPath(JNIEnv* env, potrace_path_t* path, jobject parent)
{
	jobject ret = 0;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/path");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
	ret = env->NewObject(cls, constructor);

	if(parent != 0)
	{
		jfieldID fid = env->GetFieldID(cls, "next", "Lcom/jiangpeng/android/antrace/Objects/path;");
		env->SetObjectField(parent, fid, ret);
	}
	env->DeleteLocalRef(cls);

	potrace_curve_t *curve = &path->curve;
	jobject jcurve = createCurve(env, curve);

	jfieldID fid = env->GetFieldID(cls, "curve", "Lcom/jiangpeng/android/antrace/Objects/curve;");
	env->SetObjectField(ret, fid, jcurve);
	env->DeleteLocalRef(jcurve);

	if(path->next != NULL)
	{
		jobject dummy = createPath(env, path->next, ret);
		env->DeleteLocalRef(dummy);
	}
	return ret;
}
*/

jobject createPath(JNIEnv* env, potrace_path_t* path)
{
	jobject ret = 0;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/path");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
	ret = env->NewObject(cls, constructor);

	potrace_curve_t *curve = &path->curve;
	jobject jcurve = createCurve(env, curve);

	jfieldID fid = env->GetFieldID(cls, "curve", "Lcom/jiangpeng/android/antrace/Objects/curve;");
	env->SetObjectField(ret, fid, jcurve);
	env->DeleteLocalRef(jcurve);
	env->DeleteLocalRef(cls);

	return ret;
}

JNIEXPORT jobject JNICALL Java_com_jiangpeng_android_antrace_Utils_traceImage(JNIEnv* env, jobject thiz, jobject bitmap)
{
	AndroidBitmapInfo info;
	int ret = 0;
	void* src_pixels = 0;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
		return NULL;
	}

	if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		return NULL;
	}
	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &src_pixels)) < 0) {
		return NULL;
	}

	potrace_param_t* param_t = potrace_param_default();
    param_t->turdsize = 15;
    param_t->opttolerance = 0.8;
	potrace_bitmap_t* bmp_t = bm_new(info.width, info.height);
	//memcpy(bmp_t->map, src_pixels, bmp_t->dy * bmp_t->h * BM_WORDSIZE);

	const int kShiftBits = 20;
	const int32_t kRedRatio = static_cast<int32_t>((1 << kShiftBits) * 0.21f);
	const int32_t kGreenRatio = static_cast<int32_t>((1 << kShiftBits) * 0.71f);
	const int32_t kBlueRatio = static_cast<int32_t>((1 << kShiftBits) * 0.07f);
	for (uint32_t scan_line = 0; scan_line < info.height; scan_line++) {
	    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
	    pixel32_t* src_line_end = src + info.width;
	    int x = 0;
	    while (src < src_line_end) {
	    	int32_t src_red = src->rgba8[0];
	    	int32_t src_green = src->rgba8[1];
	    	int32_t src_blue = src->rgba8[2];
	    	int32_t src_alpha = src->rgba8[3];

	    	int32_t dst_color = (kRedRatio * src_red + kGreenRatio * src_green +
	    			kBlueRatio * src_blue) >> kShiftBits;
	    	if (dst_color > 128) {
    	      BM_PUT(bmp_t, x, info.height - 1 - scan_line, 1);
	    	}
	    	else
	    	{
    	      BM_PUT(bmp_t, x, info.height - 1 - scan_line, 0);
	    	}
	    	src++;
	    	++x;
	    }
	    src_pixels = reinterpret_cast<char*>(src_pixels) + info.stride;
	}
	if(s_state != NULL)
	{
		potrace_state_free(s_state);
		s_state = NULL;
	}
	s_state = potrace_trace(param_t, bmp_t);
	potrace_param_free(param_t);
	bm_free(bmp_t);

	AndroidBitmap_unlockPixels(env, bitmap);

    if (!s_state || s_state->status != POTRACE_STATUS_OK) {
    	return NULL;
    }

    potrace_path_t* start = s_state->plist;
    jobject prev = 0;
    jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/path");
    jobject retPath = 0;
    for(potrace_path_t* n = start; n != 0; n = n->next)
    {
    	jobject path = createPath(env, n);
    	if(retPath == 0)
    	{
    		retPath = path;
    	}
    	if(prev != 0)
    	{
    		jfieldID fid = env->GetFieldID(cls, "next", "Lcom/jiangpeng/android/antrace/Objects/path;");
    		env->SetObjectField(prev, fid, path);
    		if(prev != retPath && prev != 0)
    		{
    			env->DeleteLocalRef(prev);
    		}
    	}
    	prev = path;
    }
    env->DeleteLocalRef(cls);
	return retPath;
}

JNIEXPORT void JNICALL Java_com_jiangpeng_android_antrace_Utils_threshold(JNIEnv* env, jobject thiz, jobject input, jint t, jobject output)
{
	AndroidBitmapInfo inputInfo;
	AndroidBitmapInfo outputInfo;
	int ret = 0;
	void* src_pixels = 0;
	void* dst_pixels = 0;

	if ((ret = AndroidBitmap_getInfo(env, input, &inputInfo)) < 0) {
		return;
	}

	if (inputInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		return;
	}
	if ((ret = AndroidBitmap_lockPixels(env, input, &src_pixels)) < 0) {
		return;
	}

	if ((ret = AndroidBitmap_getInfo(env, output, &outputInfo)) < 0) {
		return;
	}

	if (outputInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		return;
	}
	if ((ret = AndroidBitmap_lockPixels(env, output, &dst_pixels)) < 0) {
		return;
	}

	if(dst_pixels != 0 && src_pixels != 0)
	{
		const int kShiftBits = 20;
		const int32_t kRedRatio = static_cast<int32_t>((1 << kShiftBits) * 0.21f);
		const int32_t kGreenRatio = static_cast<int32_t>((1 << kShiftBits) * 0.71f);
		const int32_t kBlueRatio = static_cast<int32_t>((1 << kShiftBits) * 0.07f);
		for (uint32_t scan_line = 0; scan_line < outputInfo.height; scan_line++) {
			uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
			pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
			pixel32_t* src_line_end = src + inputInfo.width;
			while (src < src_line_end) {
				int32_t src_red = src->rgba8[0];
				int32_t src_green = src->rgba8[1];
				int32_t src_blue = src->rgba8[2];
				int32_t src_alpha = src->rgba8[3];

				int32_t dst_color = src_red;
				if(src_red != src_green || src_green != src_blue)
				{
					dst_color = (kRedRatio * src_red + kGreenRatio * src_green +
							kBlueRatio * src_blue) >> kShiftBits;
				}
				if (dst_color > t) {
					dst_color = 255;
				}
				else
				{
					dst_color = 0;
				}
				*dst = (src_alpha << 24) | (dst_color << 16) | (dst_color << 8) | dst_color;
				dst++;
				src++;
			}
			dst_pixels = reinterpret_cast<char*>(dst_pixels) + outputInfo.stride;
			src_pixels = reinterpret_cast<char*>(src_pixels) + inputInfo.stride;
		}
	}

	AndroidBitmap_unlockPixels(env, input);
	AndroidBitmap_unlockPixels(env, output);
}

void unsharpMask(void* src_pixels, void* dst_pixels, int w, int h, int stride, int radius)
{
	pixel32_t* input = reinterpret_cast<pixel32_t*>(src_pixels);
	pixel32_t* output = reinterpret_cast<pixel32_t*>(dst_pixels);
	int wm = w - 1;
	int hm = h - 1;
	int wh = w * h;
	int whMax = max(w, h);
	int div = radius + radius + 1;

	int rsum, x, y, i, yp, yi, yw;
	pixel32_t p;
	int vmin[whMax];

	int divsum = (div + 1) >> 1;
	divsum *= divsum;
	int dv[256 * divsum];
	for (i = 0; i < 256 * divsum; i++) {
		dv[i] = (i / divsum);
	}

	yw = yi = 0;

	int stack[div];
	int stackpointer;
	int stackstart;
	int rbs;
	int ir;
	int ip;
	int r1 = radius + 1;
	int routsum;
	int rinsum;
	{
		for (y = 0; y < h; y++) {
			rinsum = routsum = rsum = 0;
			for (i = -radius; i <= radius; i++) {
				p = input[yi + min(wm, max(i, 0))];

				ir = i + radius; // same as sir

				stack[ir] = p.rgba8[0];
				rbs = r1 - abs(i);
				rsum += stack[ir] * rbs;
				if (i > 0) {
					rinsum += stack[ir];
				} else {
					routsum += stack[ir];
				}
			}
			stackpointer = radius;

			for (x = 0; x < w; x++) {

				input[yi].rgba8[1] = (uint8_t)dv[rsum];
				rsum -= routsum;

				stackstart = stackpointer - radius + div;
				ir = stackstart % div; // same as sir

				routsum -= stack[ir];

				if (y == 0) {
					vmin[x] = min(x + radius + 1, wm);
				}
				p = input[yw + vmin[x]];

				stack[ir] = p.rgba8[0];

				rinsum += stack[ir];
				rsum += rinsum;

				stackpointer = (stackpointer + 1) % div;
				ir = (stackpointer) % div; // same as sir

				routsum += stack[ir];
				rinsum -= stack[ir];

				yi++;
			}
			yw += w;
		}

		for (x = 0; x < w; x++) {
			rinsum = routsum = rsum = 0;
			yp = -radius * w;
			for (i = -radius; i <= radius; i++) {
				yi = max(0, yp) + x;

				ir = i + radius; // same as sir
				stack[ir] = input[yi].rgba8[1];
				rbs = r1 - abs(i);
				rsum += input[yi].rgba8[1] * rbs;

				if (i > 0) {
					rinsum += stack[ir];
				} else {
					routsum += stack[ir];
				}

				if (i < hm) {
					yp += w;
				}
			}
			yi = x;
			stackpointer = radius;
			for (y = 0; y < h; y++) {
				output[yi].rgba8[0] = (uint8_t)dv[rsum];

				rsum -= routsum;

				stackstart = stackpointer - radius + div;
				ir = stackstart % div; // same as sir

				routsum -= stack[ir];

				if (x == 0) vmin[y] = min(y + r1, hm) * w;
				ip = x + vmin[y];

				stack[ir] = input[ip].rgba8[1];

				rinsum += stack[ir];

				rsum += rinsum;

				stackpointer = (stackpointer + 1) % div;
				ir = stackpointer; // same as sir

				routsum += stack[ir];
				rinsum -= stack[ir];

				yi += w;
			}
		}
	}
	i = 0;
	for (x = 0; x < w; x++) {
		for (y = 0; y < h; y++) {
			output[i].rgba8[1] = output[i].rgba8[2] = output[i].rgba8[0];
			++i;
		}
	}
	int threshold = 0;
	int amount = 70;
	i = 0;
	for (x = 0; x < w; x++) {
		for (y = 0; y < h; y++) {
            int d = input[i].rgba8[0] - output[i].rgba8[0];
            if (abs(2*d) < threshold)
                d = 0 ;
            int p = input[i].rgba8[0] + amount * d / 100;
            if(p < 0)
            {
            	p = 0;
            }

            if(p > 255)
            {
            	p = 255;
            }
            unsigned char pixel = (unsigned char)p;
			output[i].rgba8[1] = output[i].rgba8[2] = output[i].rgba8[0] = p;
			++i;
		}
	}
}

JNIEXPORT void JNICALL Java_com_jiangpeng_android_antrace_Utils_unsharpMask(JNIEnv* env, jobject thiz, jobject input, jobject output)
{
	AndroidBitmapInfo inputInfo;
	AndroidBitmapInfo outputInfo;
	void* src_pixels = 0;
	void* dst_pixels = 0;
	int ret = 0;

	if ((ret = AndroidBitmap_getInfo(env, input, &inputInfo)) < 0) {
		return;
	}

	if (inputInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		return;
	}
	if ((ret = AndroidBitmap_lockPixels(env, input, &src_pixels)) < 0) {
		return;
	}

	if ((ret = AndroidBitmap_getInfo(env, output, &outputInfo)) < 0) {
		return;
	}

	if (outputInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		return;
	}
	if ((ret = AndroidBitmap_lockPixels(env, output, &dst_pixels)) < 0) {
		return;
	}

	unsharpMask(src_pixels, dst_pixels, outputInfo.width, outputInfo.height, inputInfo.stride, 5);
	AndroidBitmap_unlockPixels(env, input);
	AndroidBitmap_unlockPixels(env, output);
}

void histogramNormalize(void* src_pixels, void* dst_pixels, int w, int h, int stride, int32_t histogram[])
{
	int32_t threshold_intensity, intense;
	int32_t x, y, i;
	uint32_t normalize_map[256];
	uint32_t high, low;
	memset( &normalize_map, 0, sizeof( uint32_t ) * 256 );

	// find histogram boundaries by locating the 1 percent levels
	threshold_intensity = (w * h) / 100;

	intense = 0;
	for( low = 0; low < 255; low++ ){
		intense += histogram[low];
		if( intense > threshold_intensity )	break;
	}

	intense = 0;
	for( high = 255; high != 0; high--){
		intense += histogram[ high ];
		if( intense > threshold_intensity ) break;
	}

	if ( low == high ){
		// Unreasonable contrast;  use zero threshold to determine boundaries.
		threshold_intensity = 0;
		intense = 0;
		for( low = 0; low < 255; low++){
			intense += histogram[low];
			if( intense > threshold_intensity )	break;
		}
		intense = 0;
		for( high = 255; high != 0; high-- ){
			intense += histogram [high ];
			if( intense > threshold_intensity )	break;
		}
	}
	if( low == high )
	{
		return;  // zero span bound
	}

	// Stretch the histogram to create the normalized image mapping.
	for(i = 0; i <= 255; i++){
		if ( i < (int32_t) low ){
			normalize_map[i] = 0;
		} else {
			if(i > (int32_t) high)
				normalize_map[i] = 255;
			else
				normalize_map[i] = ( 255 - 1) * ( i - low) / ( high - low );
		}
	}

	for (uint32_t scan_line = 0; scan_line < h; scan_line++) {
	    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
	    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
	    pixel32_t* src_line_end = src + w;
	    while (src < src_line_end) {
	    	int32_t src_alpha = src->rgba8[3];
	    	unsigned char dst_color = src->rgba8[0];
	    	unsigned char mapped = (uint8_t)normalize_map[dst_color];
	    	*dst = (src_alpha << 24) | (mapped << 16) | (mapped << 8) | mapped;
	    	dst++;
	    	src++;
	    }
	    dst_pixels = reinterpret_cast<char*>(dst_pixels) + stride;
	    src_pixels = reinterpret_cast<char*>(src_pixels) + stride;
	}
}


JNIEXPORT void JNICALL Java_com_jiangpeng_android_antrace_Utils_grayScale( JNIEnv* env, jobject thiz, jobject input, jobject output )
{
	AndroidBitmapInfo inputInfo;
	AndroidBitmapInfo outputInfo;
	int ret = 0;
	void* src_pixels = 0;
	void* dst_pixels = 0;

	if ((ret = AndroidBitmap_getInfo(env, input, &inputInfo)) < 0) {
		return;
	}

	if (inputInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		return;
	}
	if ((ret = AndroidBitmap_lockPixels(env, input, &src_pixels)) < 0) {
		return;
	}

	if ((ret = AndroidBitmap_getInfo(env, output, &outputInfo)) < 0) {
		return;
	}

	if (outputInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		return;
	}
	if ((ret = AndroidBitmap_lockPixels(env, output, &dst_pixels)) < 0) {
		return;
	}

	int32_t histogram[256];
	memset( &histogram, 0, sizeof( int32_t ) * 256 );
	const int kShiftBits = 20;
	const int32_t kRedRatio = static_cast<int32_t>((1 << kShiftBits) * 0.21f);
	const int32_t kGreenRatio = static_cast<int32_t>((1 << kShiftBits) * 0.71f);
	const int32_t kBlueRatio = static_cast<int32_t>((1 << kShiftBits) * 0.07f);

	void* src_start = src_pixels;
	void* dst_start = dst_pixels;
	for (uint32_t scan_line = 0; scan_line < outputInfo.height; scan_line++) {
	    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
	    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
	    pixel32_t* src_line_end = src + inputInfo.width;
	    while (src < src_line_end) {
	    	int32_t src_red = src->rgba8[0];
	    	int32_t src_green = src->rgba8[1];
	    	int32_t src_blue = src->rgba8[2];
	    	int32_t src_alpha = src->rgba8[3];

	    	int32_t dst_color = (kRedRatio * src_red + kGreenRatio * src_green +
	    			kBlueRatio * src_blue) >> kShiftBits;
	    	if (dst_color > 255) {
	    		dst_color = 255;
	    	}
	    	*dst = (src_alpha << 24) | (dst_color << 16) | (dst_color << 8) | dst_color;
	    	unsigned char index = (unsigned char)*dst;
			histogram[index]++;
	    	dst++;
	    	src++;
	    }
	    dst_pixels = reinterpret_cast<char*>(dst_pixels) + outputInfo.stride;
	    src_pixels = reinterpret_cast<char*>(src_pixels) + inputInfo.stride;
	}

	unsharpMask(src_start, dst_start, outputInfo.width, outputInfo.height, inputInfo.stride, 5);
	histogramNormalize(src_start, dst_start, outputInfo.width, outputInfo.height, inputInfo.stride, histogram);

	AndroidBitmap_unlockPixels(env, input);
	AndroidBitmap_unlockPixels(env, output);
}

void initInfo(char const* filetype)
{
    backend_lookup((char*)filetype, &info.backend);
    info.debug = 0;
    info.width_d.x = UNDEF;
    info.height_d.x = UNDEF;
    info.rx = UNDEF;
    info.ry = UNDEF;
    info.sx = UNDEF;
    info.sy = UNDEF;
    info.stretch = 1;
    info.lmar_d.x = UNDEF;
    info.rmar_d.x = UNDEF;
    info.tmar_d.x = UNDEF;
    info.bmar_d.x = UNDEF;
    info.angle = 0;
    info.paperwidth = DEFAULT_PAPERWIDTH;
    info.paperheight = DEFAULT_PAPERHEIGHT;
    info.tight = 0;
    info.unit = 10;
    info.compress = 1;
    info.pslevel = 2;
    info.color = 0xffffff;
    info.gamma = 2.2;
    /*
    info.param = potrace_param_default();
    if (!info.param) {
    	return JNI_FALSE;
    }
    */
    info.longcoding = 0;
    info.outfile = NULL;
    info.blacklevel = 0.5;
    info.invert = 0;
    info.opaque = 1;
    info.grouping = 1;
    info.fillcolor = 0x000000;
    info.progress = 0;
    info.progress_bar = DEFAULT_PROGRESS_BAR;
}

jboolean saveToFile(JNIEnv* env, jobject thiz, jstring path, int w, int h, const char* filetype)
{
    char const * filepath = env->GetStringUTFChars(path, NULL);
    imginfo_t imginfo;
    imginfo.pixwidth = w;
    imginfo.pixheight = h;
    initInfo(filetype);
    calc_dimensions(&imginfo, s_state->plist);
    FILE* f = fopen(filepath,"w+");
    if(f)
    {
    	struct backend_s * b = info.backend;
        if (b->init_f) {
          if(b->init_f(f) != 0)
          {
        	  fclose(f);
        	  return JNI_FALSE;
          }
        }
    	b->page_f(f, s_state->plist, &imginfo);
        if (b->term_f) {
       	  if(b->term_f(f) != 0)
          {
       		  fclose(f);
       		  return JNI_FALSE;
      	  }
        }
    	fclose(f);
    	return JNI_TRUE;
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_jiangpeng_android_antrace_Utils_saveSVG(JNIEnv* env, jobject thiz, jstring path, int w, int h)
{
	return saveToFile(env, thiz, path, w, h, "svg");
}

JNIEXPORT jboolean JNICALL Java_com_jiangpeng_android_antrace_Utils_saveDXF(JNIEnv* env, jobject thiz, jstring path, int w, int h)
{
	return saveToFile(env, thiz, path, w, h, "dxf");
}

JNIEXPORT jboolean JNICALL Java_com_jiangpeng_android_antrace_Utils_savePDF(JNIEnv* env, jobject thiz, jstring path, int w, int h)
{
	return saveToFile(env, thiz, path, w, h, "pdfpage");
}

JNIEXPORT void JNICALL Java_com_jiangpeng_android_antrace_Utils_clearState(JNIEnv* env)
{
	if(s_state != NULL)
	{
		potrace_state_free(s_state);
		s_state = NULL;
	}
}
}
