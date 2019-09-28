
package org.fit.cssbox.svg.layout;

/**
 * Trida pro reprezentaci linearniho gradientu
 *
 * @author Martin Safar
 * @author burgetr
 */
public class LinearGradient extends Gradient
{
    //gradient starting and ending points in percentages of the width/height
    public double x1;
    public double y1;
    public double x2;
    public double y2;

    public LinearGradient()
    {
        super();
    }
    
    public double getGradientLength(int w, int h)
    {
        final double dx = Math.abs((x2 - x1) * w / 100.0);
        final double dy = Math.abs((y2 - y1) * h / 100.0);
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Sets the gradient angle and computes the coordinates.
     * 
     * @param deg the gradient angle
     * @param w containing element width
     * @param h containing element height
     */
    public void setAngleDeg(double deg, int w, int h)
    {
        double procDeg = (deg % 360 + 360) % 360;
        double normDeg = 90 - procDeg;
        double wRatio = 1;

        //the ratio used for recomputing the direction
        if (w != h)
        {
            wRatio = (double) w / h;
        }
        w = 100;
        h = 100;

        //element center
        int sx = w / 2;
        int sy = h / 2;

        //element corners (A, B, C, D)
        int ax = 0;
        int ay = 0;

        int bx = w;
        int by = 0;

        int cx = w;
        int cy = h;

        int dx = 0;
        int dy = h;

        //treat special angles
        if (procDeg == 0)
        {
            x1 = 0;
            y1 = 100;
            x2 = 0;
            y2 = 0;
        }
        else if (procDeg == 90)
        {
            x1 = 0;
            y1 = 0;
            x2 = 100;
            y2 = 0;
        }
        else if (procDeg == 180)
        {
            x1 = 0;
            y1 = 0;
            x2 = 0;
            y2 = 100;
        }
        else if (procDeg == 270)
        {
            x1 = 100;
            y1 = 0;
            x2 = 0;
            y2 = 0;
        }
        else
        {
            final double tan = Math.tan((normDeg / 180) * Math.PI);

            double qqq, kkk;
            double qqq1, kkk1;
            double qqq2, kkk2;

            //compute the direction of the gradient axis
            kkk = -tan / wRatio;
            qqq = sy - kkk * sx;

            //directions of the perpendiculars
            kkk1 = 1 / (tan / wRatio);
            kkk2 = 1 / (tan / wRatio);

            //gradient endpoins as necessary for SVG
            if (procDeg > 0 && procDeg <= 90)
            {
                qqq1 = dy - kkk2 * dx;
                qqq2 = by - kkk1 * bx;

                x2 = (qqq2 - qqq) / (kkk - kkk2);
                y2 = kkk * x2 + qqq;

                x1 = (qqq1 - qqq) / (kkk - kkk1);
                y1 = kkk * x1 + qqq;

            }
            else if (procDeg > 90 && procDeg < 180)
            {
                qqq1 = ay - kkk2 * ax;
                qqq2 = cy - kkk1 * cx;

                x2 = (qqq2 - qqq) / (kkk - kkk2);
                y2 = kkk * x2 + qqq;

                x1 = (qqq1 - qqq) / (kkk - kkk1);
                y1 = kkk * x1 + qqq;
            }
            else if (procDeg > 180 && procDeg < 270)
            {
                qqq1 = by - kkk2 * bx;
                qqq2 = dy - kkk1 * dx;

                x2 = (qqq2 - qqq) / (kkk - kkk2);
                y2 = kkk * x2 + qqq;

                x1 = (qqq1 - qqq) / (kkk - kkk1);
                y1 = kkk * x1 + qqq;
            }
            else if (procDeg > 270 && procDeg < 360)
            {
                qqq1 = cy - kkk2 * cx;
                qqq2 = ay - kkk1 * ax;

                x2 = (qqq2 - qqq) / (kkk - kkk2);
                y2 = kkk * x2 + qqq;

                x1 = (qqq1 - qqq) / (kkk - kkk1);
                y1 = kkk * x1 + qqq;
            }
        }
    }
    
}
