package com.jiangpeng.android.antrace;

import android.graphics.BitmapFactory;

public class ImageUtils {
	public static double epsilon = 1e-6;
    public static int computeSampleSize(BitmapFactory.Options options, int height, int width, boolean use16Bit)
    {
        float h = options.outHeight;
        float w = options.outWidth;
        
        float hr = h / height;
        float wr = w / width;
        float ratio = hr < wr ? hr : wr;
        if(ratio < ImageUtils.epsilon)
        {
            ratio = 1;
        }
        int i = 1;
        long pixels = options.outHeight * options.outWidth;
        long maxPixels = use16Bit ? 8000000 : 4000000;
        while(i * 2 < ratio || pixels > maxPixels)
        {
            i *= 2;
            pixels /= 2;
        }
        return i;
    } 
    
    public static BitmapFactory.Options getBmpOptions(String filename)
    {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, opts);
        
        return opts;
    }
    
    public static BitmapFactory.Options calculateSampleSize(String filename, int height, int width, boolean use16Bit)
    {
        BitmapFactory.Options opts = getBmpOptions(filename);
 
        opts.inSampleSize = ImageUtils.computeSampleSize(opts, height, width, use16Bit);      
        opts.inJustDecodeBounds = false;
        return opts;
    }
 
    public static int computeMaxSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels)
    {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);
        int roundedSize;
        if (initialSize <= 8)
        {
            roundedSize = 1;
            while (roundedSize < initialSize)
            {
                roundedSize <<= 1;
            }
        } 
        else
        {
            roundedSize = (initialSize + 7) / 8 * 8;
        }
        return roundedSize;
    }

    public static int computeInitialSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels)
    {
        double w = options.outWidth;
        double h = options.outHeight;
        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(Math.floor(w / minSideLength), Math.floor(h / minSideLength));
        if (upperBound < lowerBound)
        {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }
        if ((maxNumOfPixels == -1) && (minSideLength == -1))
        {
            return 1;
        } 
        else if (minSideLength == -1)
        {
            return lowerBound;
        } 
        else
        {
            return upperBound;
        }
    }
}
