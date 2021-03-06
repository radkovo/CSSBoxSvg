/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.fit.cssbox.svg.layout;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.NodeData;
import cz.vutbr.web.css.TermColor;
import cz.vutbr.web.css.TermLengthOrPercent;
import cz.vutbr.web.css.TermList;

import java.util.ArrayList;

import org.fit.cssbox.layout.CSSDecoder;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.LengthSet;
import org.fit.cssbox.layout.Rectangle;

/**
 * Trida pro implementaci renderovani zaoblenych ramecku v SVG.
 *
 * @author safar
 */
public class Border
{

    // sirky jednotlivych stran ramecku
    public LengthSet border;

    // obdelnik vymezujici vnejsi hranici ramecku
    public Rectangle borderBounds;

    // krajni body, od kterych zacina zaobleni rohu
    public DPoint topRightH = new DPoint();
    public DPoint topRightV = new DPoint();
    public DPoint topLeftH = new DPoint();
    public DPoint topLeftV = new DPoint();
    public DPoint bottomRightH = new DPoint();
    public DPoint bottomRightV = new DPoint();
    public DPoint bottomLeftH = new DPoint();
    public DPoint bottomLeftV = new DPoint();

    // pro kazdy roh trida, ktera uklada jendotlive body potrebne k vykresleni ramecku do SVG
    public CornerRadius topLeft;
    public CornerRadius topRight;
    public CornerRadius bottomLeft;
    public CornerRadius bottomRight;

    // barvy jednotlivych stran ramecku
    public TermColor colorLeft;
    public TermColor colorRight;
    public TermColor colorBottom;
    public TermColor colorTop;

    /**
     * konstruktor, zde probiha inicializace promennych a nasledne vypocet vsech
     * bodu potrebnych k vykresleni ramecku
     *
     * @param b
     * @param bb
     * @param eb
     */
    public Border(LengthSet lengths, Rectangle bb, ElementBox eb)
    {
        border = lengths;
        borderBounds = bb;
        topRightH = new DPoint();
        topRightV = new DPoint();
        topLeftH = new DPoint();
        topLeftV = new DPoint();
        bottomRightH = new DPoint();
        bottomRightV = new DPoint();
        bottomLeftH = new DPoint();
        bottomLeftV = new DPoint();

        // vygenerovani tridy pro reprezentaci rohu ramecku pro kazdy roh
        final CSSDecoder dec = new CSSDecoder(eb.getVisualContext());
        setRadiusByStyle(1, eb.getStyle(), dec, "border-top-right-radius");
        setRadiusByStyle(2, eb.getStyle(), dec, "border-top-left-radius");
        setRadiusByStyle(3, eb.getStyle(), dec, "border-bottom-right-radius");
        setRadiusByStyle(4, eb.getStyle(), dec, "border-bottom-left-radius");

        // nastaveni barev jednotlivych stran
        colorTop = eb.getStyle().getValue(TermColor.class, "border-top-color");
        colorRight = eb.getStyle().getValue(TermColor.class, "border-right-color");
        colorBottom = eb.getStyle().getValue(TermColor.class, "border-bottom-color");
        colorLeft = eb.getStyle().getValue(TermColor.class, "border-left-color");

        // vypocet hranicnich bodu, od kterych zacina zaobleni
        calculateBorderPoints();

        // vypocet ostatnich bodu potrebnych k vykresleni zaobleneho ramecku
        // dopocitavaji se body A, B, C, D, E, O, G a H, viz technicka zprava u diplomove prace
        calculateRadiusPoints();

    }

    /**
     * Gets the corner radius for the given corner.
     *
     * @param s The corner index (1..4)
     * @return
     */
    public CornerRadius getRadius(int s)
    {
        switch (s)
        {
            default:
            case 1:
                return topRight;
            case 2:
                return topLeft;
            case 3:
                return bottomRight;
            case 4:
                return bottomLeft;
        }
    }

    /**
     * Sets the corner radius by the element style.
     * @param s the corner index (1..4)
     * @param style the element style
     * @param dec the CSS decoder used to transform the CSS lengths to pixels
     * @param propertyName the property name to use (e.g. "border-top-right-radius")
     */
    private void setRadiusByStyle(int s, NodeData style, CSSDecoder dec, String propertyName)
    {
        TermList vals = null;
        CSSProperty.BorderRadius r = style.getProperty(propertyName);
        if (r == CSSProperty.BorderRadius.list_values)
            vals = style.getValue(TermList.class, propertyName);
        
        float radx, rady;
        if (vals != null && vals.size() == 2)
        {
            radx = dec.getLength((TermLengthOrPercent) vals.get(0), false, 0, 0, borderBounds.width);
            rady = dec.getLength((TermLengthOrPercent) vals.get(1), false, 0, 0, borderBounds.height);
        }
        else
        {
            radx = 0;
            rady = 0;
        }
        
        if (radx > borderBounds.width / 2)
        {
            radx = borderBounds.width / 2;
        }
        if (rady > borderBounds.height / 2)
        {
            rady = borderBounds.height / 2;
        }
        CornerRadius cr = new CornerRadius(radx, rady, s);
        switch (s)
        {
            default:
            case 1:
                topRight = cr;
                break;
            case 2:
                topLeft = cr;
                break;
            case 3:
                bottomRight = cr;
                break;
            case 4:
                bottomLeft = cr;
                break;
        }
    }
    
    /**
     * metoda por dopocitani hranicnich bodu hranicni body se dopocitavaji podle
     * rozmeru elementu, velikosti zaobleni rohu a sirky jednotlivych stran
     * ramecku
     */
    public void calculateBorderPoints()
    {

        topLeftH.x = borderBounds.x + Math.max(topLeft.x, border.left);//topLeft.x;
        topLeftH.y = borderBounds.y;

        topRightH.x = borderBounds.x + borderBounds.width - Math.max(topRight.x, border.right);//topRight.x;
        topRightH.y = borderBounds.y;

        topRightV.x = borderBounds.x + borderBounds.width;
        topRightV.y = borderBounds.y + Math.max(topRight.y, border.top);

        bottomRightV.x = borderBounds.x + borderBounds.width;
        bottomRightV.y = borderBounds.y + borderBounds.height - Math.max(bottomRight.y, border.bottom);

        bottomRightH.x = borderBounds.x + borderBounds.width - Math.max(bottomRight.x, border.right);
        bottomRightH.y = borderBounds.y + borderBounds.height;

        bottomLeftH.x = borderBounds.x + Math.max(bottomLeft.x, border.left);
        bottomLeftH.y = borderBounds.y + borderBounds.height;

        bottomLeftV.x = borderBounds.x;
        bottomLeftV.y = borderBounds.y + borderBounds.height - Math.max(bottomLeft.y, border.bottom);

        topLeftV.x = borderBounds.x;
        topLeftV.y = borderBounds.y + Math.max(topLeft.y, border.top);
    }

    /**
     * vypocet bodu A, B, C, atd pro kazdy roh zvlast
     */
    public void calculateRadiusPoints()
    {
        float widthHor, widthVer;
        float radx, rady;

        ////////////////////////////////
        // TOP RIGHT
        ////////////////////////////////
        radx = topRight.x;
        rady = topRight.y;
        widthHor = border.right;
        widthVer = border.top;

        //
        // body A, B, C, D, E a O jsou dopocitany podle elementu, ramecku a velikosti zaobleni
        //
        topRight.o.x = borderBounds.x + borderBounds.width;
        topRight.o.y = borderBounds.y;
        topRight.e.x = topRight.o.x - radx;
        topRight.e.y = topRight.o.y + rady;

        topRight.a.x = topRight.o.x - radx;
        topRight.a.y = topRight.o.y;

        topRight.b.x = topRight.o.x - Math.max(widthHor, radx);
        topRight.b.y = topRight.o.y + widthVer;

        topRight.c.x = topRight.o.x;
        topRight.c.y = topRight.o.y + rady;

        topRight.d.x = topRight.o.x - widthHor;
        topRight.d.y = topRight.o.y + Math.max(widthVer, rady);

        // zde je vypocet smernicoveho tvaru primky, ktera tvori hranici mezi dvema stranami ramecku
        topRight.k = Math.tan((widthVer / (2.0 * (widthVer + widthHor))) * Math.PI);
        topRight.z = (topRight.o.y + topRight.k * topRight.o.x);

        // y = k*0 + z;
        // x = -z/k;

        topRight.bounds = new Rectangle(topRight.o.x - radx, topRight.o.y, radx, rady);
        if (widthVer > rady || widthHor > radx)
        {
            topRight.q = new DPoint();
            topRight.q.x = topRight.d.x;
            topRight.q.y = topRight.b.y;
        }

        // ziskame pruseciky (body G a H, viz technicka zprava k diplomove praci), ktere jsou pouzity pro vykresleni jednotlivych casti
        calculateMiddlePoints(topRight, radx, rady, widthHor, widthVer, 1);

        ////////////////////////////////
        // TOP LEFT
        ////////////////////////////////

        // topleft
        radx = topLeft.x;
        rady = topLeft.y;
        widthHor = border.left;
        widthVer = border.top;
        topLeft.o.x = borderBounds.x;
        topLeft.o.y = borderBounds.y;

        topLeft.e.x = topLeft.o.x + radx;
        topLeft.e.y = topLeft.o.y + rady;

        topLeft.a.x = topLeft.o.x;
        topLeft.a.y = topLeft.o.y + rady;

        topLeft.b.x = topLeft.o.x + widthHor;
        topLeft.b.y = topLeft.o.y + Math.max(widthVer, rady);

        topLeft.c.x = topLeft.o.x + radx;
        topLeft.c.y = topLeft.o.y;

        topLeft.d.x = topLeft.o.x + Math.max(widthHor, radx);
        topLeft.d.y = topLeft.o.y + widthVer;

        topLeft.k = -Math.tan(((widthVer) / (2.0 * (widthHor + widthVer))) * Math.PI);
        topLeft.z = (topLeft.o.y + topLeft.k * topLeft.o.x);

        topLeft.bounds = new Rectangle(topLeft.o.x, topLeft.o.y, radx, rady);
        if (widthVer > rady || widthHor > radx)
        {
            topLeft.q = new DPoint();
            topLeft.q.x = topLeft.b.x;
            topLeft.q.y = topLeft.d.y;
        }
        calculateMiddlePoints(topLeft, radx, rady, widthHor, widthVer, 2);

        ////////////////////////////////
        // BOTTOM RIGHT
        ////////////////////////////////

        radx = bottomRight.x;
        rady = bottomRight.y;
        widthHor = border.right;
        widthVer = border.bottom;

        bottomRight.o.x = borderBounds.x + borderBounds.width;
        bottomRight.o.y = borderBounds.y + borderBounds.height;
        bottomRight.e.x = bottomRight.o.x - radx;
        bottomRight.e.y = bottomRight.o.y - rady;

        bottomRight.a.x = bottomRight.o.x;
        bottomRight.a.y = bottomRight.o.y - rady;

        bottomRight.b.x = bottomRight.o.x - widthHor;
        bottomRight.b.y = bottomRight.o.y - Math.max(widthVer, rady);

        bottomRight.c.x = bottomRight.o.x - radx;
        bottomRight.c.y = bottomRight.o.y;

        bottomRight.d.x = bottomRight.o.x - Math.max(widthHor, radx);
        bottomRight.d.y = bottomRight.o.y - widthVer;

        double mid = (widthVer) / (2.0 * (widthHor + widthVer));

        bottomRight.k = -Math.tan(mid * Math.PI);
        bottomRight.z = (bottomRight.o.y + bottomRight.k * bottomRight.o.x);

        bottomRight.bounds = new Rectangle(bottomRight.o.x - radx, bottomRight.o.y - rady, radx, rady);
        if (widthVer > rady || widthHor > radx)
        {
            bottomRight.q = new DPoint();
            bottomRight.q.x = bottomRight.b.x;
            bottomRight.q.y = bottomRight.d.y;
        }
        calculateMiddlePoints(bottomRight, radx, rady, widthHor, widthVer, 3);

        ////////////////////////////////
        // BOTTOM LEFT
        ////////////////////////////////

        radx = bottomLeft.x;
        rady = bottomLeft.y;
        widthHor = border.left;
        widthVer = border.bottom;

        bottomLeft.o.x = borderBounds.x;
        bottomLeft.o.y = borderBounds.y + borderBounds.height;

        bottomLeft.e.x = bottomLeft.o.x + radx;
        bottomLeft.e.y = bottomLeft.o.y - rady;

        bottomLeft.a.x = bottomLeft.o.x + radx;
        bottomLeft.a.y = bottomLeft.o.y;

        bottomLeft.b.x = bottomLeft.o.x + Math.max(widthHor, radx);
        bottomLeft.b.y = bottomLeft.o.y - widthVer;

        bottomLeft.c.x = bottomLeft.o.x;
        bottomLeft.c.y = bottomLeft.o.y - rady;

        bottomLeft.d.x = bottomLeft.o.x + widthHor;
        bottomLeft.d.y = bottomLeft.o.y - Math.max(widthVer, rady);

        bottomLeft.k = Math.tan(((widthVer) / (2.0 * (widthHor + widthVer))) * Math.PI);
        bottomLeft.z = (bottomLeft.o.y + bottomLeft.k * bottomLeft.o.x);

        bottomLeft.bounds = new Rectangle(bottomLeft.o.x, bottomLeft.o.y - rady, radx, rady);
        if (widthVer > rady || widthHor > radx)
        {
            bottomLeft.q = new DPoint();
            bottomLeft.q.x = bottomLeft.d.x;
            bottomLeft.q.y = bottomLeft.b.y;
        }
        calculateMiddlePoints(bottomLeft, radx, rady, widthHor, widthVer, 4);
    }

    /**
     * metoda pro dopocitani bodu G a H zde jsou osetreny specialni pripady
     * 
     * @param cr
     * @param radx
     * @param rady
     * @param widthHor
     * @param widthVer
     * @param s
     */
    public void calculateMiddlePoints(CornerRadius cr, float radx, float rady, float widthHor, float widthVer, int s)
    {
        // pokud je ramecek nastaven na 0px
        // ramecek je vykreslen bez zaobleni a body G a H jsou dopocitavany pouze podle sirky ramecku
        if (radx == 0 && rady == 0)
        {
            cr.h.x = cr.o.x;
            cr.h.y = cr.o.y;
            if (s == 1)
            { // top-right
                cr.g.x = cr.o.x - widthHor;
                cr.g.y = cr.o.y + widthVer;
            }
            else if (s == 2)
            { // top-left
                cr.g.x = cr.o.x + widthHor;
                cr.g.y = cr.o.y + widthVer;
            }
            else if (s == 3)
            { // bottom-right
                cr.g.x = cr.o.x - widthHor;
                cr.g.y = cr.o.y - widthVer;
            }
            else
            { // bottom-left
                cr.g.x = cr.o.x + widthHor;
                cr.g.y = cr.o.y - widthVer;
            }
            cr.isDrawn = false; // zaobleni se nevykresluje
        }
        else if (widthHor == 0)
        { // pokud je nulova sirka v horizontalnim smeru a ve vertikalnim je nenulova 
            if (s == 1 || s == 4)
            { // ramecek prechazi postupne do jednoho bodu (viz DP)
                cr.h.x = cr.c.x;
                cr.h.y = cr.c.y;
                cr.g.x = cr.c.x;
                cr.g.y = cr.c.y;
            }
            else
            {
                cr.h.x = cr.a.x;
                cr.h.y = cr.a.y;
                cr.g.x = cr.a.x;
                cr.g.y = cr.a.y;
            }
        }
        else if (widthVer == 0)
        {// pokud je nulova sirka ve vertikalnim smeru a v horizontalnim je nenulova 
            if (s == 1 || s == 4)
            {
                cr.h.x = cr.a.x;
                cr.h.y = cr.a.y;
                cr.g.x = cr.a.x;
                cr.g.y = cr.a.y;
            }
            else
            {
                cr.h.x = cr.c.x;
                cr.h.y = cr.c.y;
                cr.g.x = cr.c.x;
                cr.g.y = cr.c.y;
            }
        }
        else
        {
            // pokud nenastal zadny ze specialnich pripadu, spocitame prusecik obou elips s primkou 
            cr.h = getIntersectPoint(cr.e, radx, rady, -cr.k, cr.z, cr.bounds);
            cr.g = getIntersectPoint(cr.e, radx - widthHor, rady - widthVer, -cr.k, cr.z, cr.bounds);
        }

        if (cr.q != null)
        {
            cr.g.x = cr.q.x;
            cr.g.y = cr.q.y;
        }
    }

    /**
     * prusecik elipsy s primkou
     * 
     * @param x0
     * @param y0
     * @param a
     * @param b
     * @param k
     * @param c
     * @return
     */
    private ArrayList<Double> ellipseLineIntersect(float x0, float y0, float a, float b, double k, double c)
    {
        double delta = c + k * x0;
        double eps = c - y0;
        double div = a * a * k * k + b * b;
        double xLeft, yLeft, xRight, yRight, x1, x2, y1, y2;
        double mid = a * a * k * k + b * b - delta * delta - y0 * y0 + 2 * delta * y0;
        xLeft = x0 * b * b - k * a * a * eps;
        xRight = a * b * Math.sqrt(mid);
        x1 = (xLeft + xRight) / div;
        x2 = (xLeft - xRight) / div;
        yLeft = b * b * delta + y0 * a * a * k * k;
        yRight = a * b * k * Math.sqrt(mid);
        y1 = (yLeft + yRight) / div;
        y2 = (yLeft - yRight) / div;
        ArrayList<Double> arl;
        arl = new ArrayList<Double>();
        arl.add(x1);
        arl.add(y1);
        arl.add(x2);
        arl.add(y2);
        return arl;
    }

    /**
     * metoda pro vypocet pruseciku primky a elipsy, ktera vybere ten prusecik,
     * ktery je ve vymezenem obdelniku
     * 
     * @param center
     * @param sizex
     * @param sizey
     * @param slope
     * @param yIntercept
     * @param bounds
     * @return
     */
    public DPoint getIntersectPoint(DPoint center, float sizex, float sizey, double slope, double yIntercept,
            Rectangle bounds)
    {
        ArrayList<Double> arl = ellipseLineIntersect(center.x, center.y, sizex, sizey, slope, yIntercept);
        DPoint p = new DPoint();
        p.x = arl.get(0).floatValue();
        p.y = arl.get(1).floatValue();
        if (!isInBounds(bounds, p))
        {
            p.x = arl.get(2).floatValue();
            p.y = arl.get(3).floatValue();
        }
        return p;
    }

    public boolean isInBounds(Rectangle r, DPoint p)
    {
        return isInBounds(r.x, r.y, r.x + r.width, r.y + r.height, p.x, p.y);
    }

    public boolean isInBounds(float x1, float y1, float x2, float y2, float px, float py)
    {
        return px > x1 && px < x2 && py < y2 && py > y1;
    }

    public boolean isInBounds(float x1, float y1, float x2, float y2, double px, double py)
    {
        return px > x1 && px < x2 && py < y2 && py > y1;
    }

}
