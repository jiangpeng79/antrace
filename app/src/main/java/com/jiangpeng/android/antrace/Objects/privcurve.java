package com.jiangpeng.android.antrace.Objects;

public class privcurve {
	public int n;            /* number of segments */
	public int[] tag;         /* tag[n]: POTRACE_CORNER or POTRACE_CURVETO */
	public dpoint[][] c; /* c[n][i]: control points. 
			       c[n][0] is unused for tag[n]=POTRACE_CORNER */
	  /* the remainder of this structure is special to privcurve, and is
	     used in EPS debug output and special EPS "short coding". These
	     fields are valid only if "alphacurve" is set. */
	public int alphacurve;   /* have the following fields been initialized? */
	public dpoint[] vertex; /* for POTRACE_CORNER, this equals c[1] */
	public double[] alpha;    /* only for POTRACE_CURVETO */
	public double[] alpha0;   /* "uncropped" alpha parameter - for debug output only */
	public double[] beta;

	public privcurve()
	{
	}
}
