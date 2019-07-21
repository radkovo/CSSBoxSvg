
package org.fit.cssbox.svg.layout;

import java.util.ArrayList;

/**
 * Trida pro reprezentaci linearniho gradientu
 *
 * @author Martin Safar
 */
public class LinearGradient
{
    public ArrayList<GradientStop> data;

    // uhel ve stupnich
    public Integer angle;

    public double x1;
    public double y1;
    public double x2;
    public double y2;

    /**
     * tato metoda slouzi k nastaveni smeru gradientu, tak jak je nastaven v CSS
     * podle rozmeru elementu, pro ktery je gradient vykreslovan vypocte
     * prislusne krajni body gradientu, podle kterych se pak generuje SVG kod
     * vsechny vypoctene souradnice jsou v procentualnich hodnotach pro vysledny
     * SVG element linearGradient
     * 
     * @param deg
     * @param w
     * @param h
     */
    public void setAngleDeg(double deg, int w, int h)
    {

        double procDeg = (deg % 360 + 360) % 360;
        double normDeg = 90 - procDeg;
        double tan;
        double wRatio = 1;

        // pokud nevykreslujeme gradient pro ctverec, je treba spocitat pomer stran, 
        // abychom podle nej mohli upravit smernici osy gradientu
        if (w != h)
        {
            wRatio = (double) w / h;
        }
        w = 100;
        h = 100;

        // stred elementu, ktery je zaroven stredem gradientu
        int sx = w / 2;
        int sy = h / 2;

        // vypocteme body A, B, C, D, coz jsou rohy elementu
        int ax = 0;
        int ay = 0;

        int bx = w;
        int by = 0;

        int cx = w;
        int cy = h;

        int dx = 0;
        int dy = h;

        // osetreni specialnich pripadu, kde bud neni treba provadet vypocet, nebo to ani neni mozne
        // (funkce tangens neni definovana pro uhly 90,270, atd)
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

            // vypocet hodnoty tangens
            tan = Math.tan((normDeg / 180) * Math.PI);

            double qqq, kkk;
            double qqq1, kkk1;
            double qqq2, kkk2;

            // vypocet smernicoveho tvaru osy gradientu 
            // smernice je vydelena pomerem stran obdelniku pro ktery gradient generujeme

            kkk = -tan / wRatio;
            qqq = sy - kkk * sx;

            // vypocet smernic pro kolmice, podle kterych nasledne vypocteme krajni body gradientu
            kkk1 = 1 / (tan / wRatio);
            kkk2 = 1 / (tan / wRatio);

            // dopocitani pruseciku osy gradientu a kolmic ktere prochazeji body A, B, C a D 
            // (podle toho v jakem smeru je osa gadientu vedena )
            // vypoctene pruseciky jsou krajni body gradientu, tak jak jsou potreba pro SVG element
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
