package mpicbg.imagefeatures;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: </p>
 *
 * @author Stephan Preibisch
 * @version 1.0
 */

import ij.process.*;
import ij.*;

import java.awt.Point;

public class ImageArrayConverter
{
    public static boolean CUTOFF_VALUES = true;
    public static boolean NORM_VALUES = false;

    public static ImagePlus FloatArrayToImagePlus(FloatArray2D image, String name, float min, float max)
    {
        ImagePlus imp = IJ.createImage(name,"32-Bit Black", image.width, image.height, 1);
        FloatProcessor ip = (FloatProcessor)imp.getProcessor();
        FloatArrayToFloatProcessor(ip, image);

        if (min == max)
            ip.resetMinAndMax();
        else
            ip.setMinAndMax(min, max);

        imp.updateAndDraw();

        return imp;
    }

    public static FloatArray2D ImageToFloatArray2D(ImageProcessor ip)
    {
        FloatArray2D image;
        Object pixelArray = ip.getPixels();
        int count = 0;

        if (ip instanceof ByteProcessor)
        {
            image = new FloatArray2D(ip.getWidth(),  ip.getHeight());
            byte[] pixels = (byte[])pixelArray;

            for (int y = 0; y < ip.getHeight(); y++)
                for (int x = 0; x < ip.getWidth(); x++)
                    image.data[count] = pixels[count++] & 0xff;
        }
        else if (ip instanceof ShortProcessor)
        {
            image = new FloatArray2D(ip.getWidth(),  ip.getHeight());
            short[] pixels = (short[])pixelArray;

            for (int y = 0; y < ip.getHeight(); y++)
                for (int x = 0; x < ip.getWidth(); x++)
                    image.data[count] = pixels[count++] & 0xffff;
        }
        else if (ip instanceof FloatProcessor)
        {
            image = new FloatArray2D(ip.getWidth(),  ip.getHeight());
            float[] pixels = (float[])pixelArray;

            for (int y = 0; y < ip.getHeight(); y++)
                for (int x = 0; x < ip.getWidth(); x++)
                    image.data[count] = pixels[count++];
        }
        else // if (ip instanceof ColorProcessor )
        {
            image = new FloatArray2D(ip.getWidth(),  ip.getHeight());
            int[] pixels = (int[])pixelArray;

            for (int y = 0; y < ip.getHeight(); y++)
            {
                for (int x = 0; x < ip.getWidth(); x++)
                {
                	int rgb = pixels[count++];
                	int b = rgb & 0xff;
                	rgb = rgb >> 8;
                	int g = rgb & 0xff;
                	rgb = rgb >> 8;
                	int r = rgb & 0xff;
                    image.data[count] = 0.3f * r + 0.6f * g + 0.1f * b;
                }
            }
        }
        return image;
    }

    public static void ArrayToByteProcessor(ImageProcessor ip, int[][] pixels)
    {
        byte[] data = new byte[pixels.length * pixels[0].length];

        int count = 0;
        for (int y = 0; y < pixels[0].length; y++)
            for (int x = 0; x < pixels.length; x++)
                data[count++] = (byte)(pixels[x][y] & 0xff);

        ip.setPixels(data);
    }

    public static void ArrayToByteProcessor(ImageProcessor ip, float[][] pixels)
    {
        byte[] data = new byte[pixels.length * pixels[0].length];

        int count = 0;
        for (int y = 0; y < pixels[0].length; y++)
            for (int x = 0; x < pixels.length; x++)
                data[count++] = (byte)(((int)pixels[x][y]) & 0xff);

        ip.setPixels(data);
    }

    public static void ArrayToFloatProcessor(ImageProcessor ip, double[] pixels, int width, int height)
    {
        float[] data = new float[width * height];

        int count = 0;
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                data[count] = (float)pixels[count++];

        ip.setPixels(data);
        ip.resetMinAndMax();
    }

    public static void ArrayToFloatProcessor(ImageProcessor ip, float[] pixels, int width, int height)
    {
        float[] data = new float[width * height];

        int count = 0;
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                data[count] = (float)pixels[count++];

        ip.setPixels(data);
        ip.resetMinAndMax();
    }

    public static void FloatArrayToFloatProcessor(ImageProcessor ip, FloatArray2D pixels)
    {
        float[] data = new float[pixels.width * pixels.height];

        int count = 0;
        for (int y = 0; y < pixels.height; y++)
            for (int x = 0; x < pixels.width; x++)
                data[count] = pixels.data[count++];

        ip.setPixels(data);
        ip.resetMinAndMax();
    }

    public static void normPixelValuesToByte(int[][] pixels, boolean cutoff)
    {
        int max = 0, min = 255;

        // check minmal and maximal values or cut of values that are higher or lower than 255 resp. 0
        for (int y = 0; y < pixels[0].length; y++)
            for (int x = 0; x < pixels.length; x++)
            {
                if (cutoff)
                {
                    if (pixels[x][y] < 0)
                        pixels[x][y] = 0;

                    if (pixels[x][y] > 255)
                        pixels[x][y] = 255;
                }
                else
                {
                    if (pixels[x][y] < min)
                        min = pixels[x][y];

                    if (pixels[x][y] > max)
                        max = pixels[x][y];
                }
            }

        if (cutoff)
            return;


        // if they do not match bytevalues we have to do something
        if (max > 255 || min < 0)
        {
            double factor;

            factor = (max-min) / 255.0;

            for (int y = 0; y < pixels[0].length; y++)
                for (int x = 0; x < pixels.length; x++)
                    pixels[x][y] = (int)((pixels[x][y] - min) / factor);
        }
    }

    public static void normPixelValuesToByte(float[][] pixels, boolean cutoff)
    {
        float max = 0, min = 255;

        // check minmal and maximal values or cut of values that are higher or lower than 255 resp. 0
        for (int y = 0; y < pixels[0].length; y++)
            for (int x = 0; x < pixels.length; x++)
            {
                if (cutoff)
                {
                    if (pixels[x][y] < 0)
                        pixels[x][y] = 0;

                    if (pixels[x][y] > 255)
                        pixels[x][y] = 255;
                }
                else
                {
                    if (pixels[x][y] < min)
                        min = pixels[x][y];

                    if (pixels[x][y] > max)
                        max = pixels[x][y];
                }
            }

        if (cutoff)
            return;


        // if they do not match bytevalues we have to do something
        if (max > 255 || min < 0)
        {
            double factor;

            factor = (max-min) / 255.0;

            for (int y = 0; y < pixels[0].length; y++)
                for (int x = 0; x < pixels.length; x++)
                    pixels[x][y] = (int)((pixels[x][y] - min) / factor);
        }
    }

}
