package com.jiangpeng.android.antrace.Objects;

public class path {
	public int area;                         /* area of the bitmap path */
	public int sign;                         /* '+' or '-', depending on orientation */
	public curve curve;            /* this path's vector data */

	public path next;      /* linked list structure */

	public path childlist; /* tree structure */
	public path sibling;   /* tree structure */

	public privpath priv;  /* private state */

	public path()
	{
	}
}
