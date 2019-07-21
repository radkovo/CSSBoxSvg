
package org.fit.cssbox.svg.layout;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * trida pro reprezentaci rohu ramecku
 *
 * @author Martin Safar
 */
public class CornerRadius
{
    // zaobleni ve smeru osy x a osy y
    public int x;
    public int y;

    // jednotlive body
    public Point a;
    public Point b;
    public Point c;
    public Point d;
    public Point e;
    public Point o;
    public DPoint g;
    public DPoint h;

    public Point q;

    // smernice a vysek na ose y pro hranicni primku
    public double z;
    public double k;

    // obdelnik vymezeny body AOCE, ve kterem budeme hledat pruseciky
    public Rectangle bounds;

    public int s;

    public boolean isDrawn;

    /**
     * v konstruktoru jsou pouze inicializovany hodnoty
     * 
     * @param radx
     * @param rady
     * @param ss
     */
    public CornerRadius(int radx, int rady, int ss)
    {
        this.o = new Point();
        this.e = new Point();
        this.d = new Point();
        this.g = new DPoint();
        this.h = new DPoint();
        this.a = new Point();
        this.b = new Point();
        this.c = new Point();
        q = null;
        x = radx;
        y = rady;
        s = ss;

        isDrawn = true;
    }

    /**
     * vygenerovani SVG kodu do atributu pro tag path. tato cast vykresluje
     * jednu pulku rohu (mezi body C, D, G a H)
     * 
     * @param widthVer
     * @param widthHor
     * @return
     */
    public String getPathRadiusC(int widthVer, int widthHor)
    {
        String path1 = "";
        path1 += "M " + d.x + " " + d.y + " ";
        if (widthVer > y || widthHor > x)
        {
            path1 += " L " + Math.round(g.x) + " " + Math.round(g.y) + " ";
        }
        else
        {
            path1 += " A " + (x - widthHor) + " " + (y - widthVer) + " 0 0 0 " + Math.round(g.x) + " "
                    + Math.round(g.y);
        }
        path1 += " L " + Math.round(h.x) + " " + Math.round(h.y) + " ";
        path1 += " A " + x + " " + y + " 0 0 1 " + c.x + " " + c.y;

        if (widthVer > y || widthHor > x)
        {
            if (s == 1 || s == 4)
            {
                path1 += " L " + o.x + " " + d.y + " ";
            }
            else
            {
                path1 += " L " + d.x + " " + o.y + " ";
            }
        }

        return path1;

    }

    /**
     * vygenerovani SVG kodu do atributu pro tag path. tato cast vykresluje
     * jednu pulku rohu (mezi body A, B, G a H)
     * 
     * @param widthVer
     * @param widthHor
     * @return
     */
    public String getPathRadiusA(int widthVer, int widthHor)
    {
        String path2 = "";

        path2 += " M " + b.x + " " + b.y + " ";
        if (widthVer > y || widthHor > x)
        {
            path2 += " L " + Math.round(g.x) + " " + Math.round(g.y) + " ";
        }
        else
        {
            path2 += " A " + (x - widthHor) + " " + (y - widthVer) + " 0 0 1 " + Math.round(g.x) + " "
                    + Math.round(g.y);
        }
        path2 += " L " + Math.round(h.x) + " " + Math.round(h.y) + " ";

        path2 += " A " + x + " " + y + " 0 0 0 " + a.x + " " + a.y;

        if (widthVer > y || widthHor > x)
        {
            if (s == 1 || s == 4)
            {
                path2 += " L " + b.x + " " + o.y + " ";
            }
            else
            {
                path2 += " L " + o.x + " " + b.y + " ";

            }
        }
        return path2;
    }

}
