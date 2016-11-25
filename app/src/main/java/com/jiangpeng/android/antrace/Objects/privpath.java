package com.jiangpeng.android.antrace.Objects;

public class privpath {
	public int len;
	public point[] pt;     /* pt[len]: path as extracted from bitmap */
	public int[] lon;        /* lon[len]: (i,lon[i]) = longest straight line from i */

	public int x0, y0;      /* origin for sums */
	public sums[] sums;    /* sums[len+1]: cache for fast summing */

	public int m;           /* length of optimal polygon */
	public int[] po;         /* po[m]: optimal polygon */

	public privcurve curve;   /* curve[m]: array of curve elements */
	public privcurve ocurve;  /* ocurve[om]: array of curve elements */
	public privcurve fcurve;  /* final curve: this points to either curve or
			       ocurve. Do not free this separately. */
}
