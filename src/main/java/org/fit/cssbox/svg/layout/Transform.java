
package org.fit.cssbox.svg.layout;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.Term;
import cz.vutbr.web.css.TermAngle;
import cz.vutbr.web.css.TermFunction;
import cz.vutbr.web.css.TermInteger;
import cz.vutbr.web.css.TermLengthOrPercent;
import cz.vutbr.web.css.TermList;
import cz.vutbr.web.css.TermNumber;
import java.awt.Rectangle;
import org.fit.cssbox.layout.CSSDecoder;
import org.fit.cssbox.layout.ElementBox;

/**
 * trida pro generovani SVG 2D transformaci
 * 
 * @author safar
 */
public class Transform
{
    private CSSDecoder dec;

    private Rectangle bounds;

    public Transform()
    {
    }

    /**
     * trida pro vygenerovani transformaci
     * 
     * @param elem
     * @return
     */
    public String createTransform(ElementBox elem)
    {
        if (elem.isBlock() || elem.isReplaced())
        {

            // nejprve jsou vypocteny souradice pocatku transformace (transform origin)
            dec = new CSSDecoder(elem.getVisualContext());
            bounds = elem.getAbsoluteBorderBounds();

            //decode the origin
            int ox, oy;
            CSSProperty.TransformOrigin origin = elem.getStyle().getProperty("transform-origin");
            if (origin == CSSProperty.TransformOrigin.list_values)
            {
                TermList values = elem.getStyle().getValue(TermList.class, "transform-origin");
                ox = dec.getLength((TermLengthOrPercent) values.get(0), false, bounds.width / 2, 0, bounds.width);
                oy = dec.getLength((TermLengthOrPercent) values.get(1), false, bounds.height / 2, 0, bounds.height);
            }
            else
            {
                ox = bounds.width / 2;
                oy = bounds.height / 2;
            }
            ox += bounds.x;
            oy += bounds.y;
            CSSProperty.Transform trans = elem.getStyle().getProperty("transform");
            if (trans == CSSProperty.Transform.list_values)
            {
                // nasledne je vygenerovana transformace posun - translate, do pocatku transformace
                String transformStr = "translate(" + ox + " " + oy + ") ";
                TermList values = elem.getStyle().getValue(TermList.class, "transform");
                // v cyklu jsou prochazeny vschny transformace
                for (Term<?> term : values)
                {
                    if (term instanceof TermFunction)
                    {
                        final TermFunction func = (TermFunction) term;
                        final String fname = func.getFunctionName().toLowerCase();
                        // podle prikazu jsou volany ruzne metody, ktere generuji transformacni prikazy
                        if (fname.equals("rotate"))
                        {
                            transformStr += getRotate(func);
                        }
                        else if (fname.equals("translate"))
                        {
                            transformStr += getTranslate(func);
                        }
                        else if (fname.equals("translatex"))
                        {
                            transformStr += getTranslateX(func);
                        }
                        else if (fname.equals("translatey"))
                        {
                            transformStr += getTranslateY(func);
                        }
                        else if (fname.equals("scale"))
                        {
                            transformStr += getScale(func);
                        }
                        else if (fname.equals("scalex"))
                        {
                            transformStr += getScaleX(func);
                        }
                        else if (fname.equals("scaley"))
                        {
                            transformStr += getScaleY(func);
                        }
                        else if (fname.equals("skew"))
                        {
                            transformStr += getSkew(func);
                        }
                        else if (fname.equals("skewx"))
                        {
                            transformStr += getSkewX(func);
                        }
                        else if (fname.equals("skewy"))
                        {
                            transformStr += getSkewY(func);
                        }
                        else if (fname.equals("matrix"))
                        {
                            transformStr += getMatrix(func);
                        }
                    }

                    transformStr += " ";
                }
                // nakonec je element posunut zpet na puvodni misto
                transformStr += " translate( -" + ox + " -" + oy + ")";
                //  return "";
                return transformStr;
            }
        }
        return "";
    }

    //=================================================================================================
    
    private String getMatrix(TermFunction func)
    {
        if (func.size() == 6)
        {
            double[] vals = new double[6];
            boolean typesOk = true;
            for (int i = 0; i < 6; i++)
            {
                if (isNumber(func.get(i)))
                {
                    vals[i] = getNumber(func.get(i));
                }
                else
                {
                    typesOk = false;
                }
            }
            if (typesOk)
            {
            }
            return "matrix( " + vals[0] + " " + vals[1] + " " + vals[2] + " " + vals[3] + " " + vals[4] + " " + vals[5]
                    + " ) ";
        }
        return "";
    }

    private String getSkewX(TermFunction func)
    {
        if (func.size() == 1 && func.get(0) instanceof TermAngle)
        {
            TermAngle t = (TermAngle) func.get(0);
            return "skewX( " + t.getValue() + " ) ";
        }
        return "";
    }

    private String getSkewY(TermFunction func)
    {
        if (func.size() == 1 && func.get(0) instanceof TermAngle)
        {
            TermAngle t = (TermAngle) func.get(0);
            return "skewY( " + t.getValue() + " ) ";
        }
        return "";
    }

    private String getSkew(TermFunction func)
    {
        if (func.size() == 1 && func.get(0) instanceof TermAngle)
        {
            TermAngle t = (TermAngle) func.get(0);
            return "skewX( " + t.getValue() + " ) ";

        }
        else if (func.size() == 2 && func.get(0) instanceof TermAngle && func.get(1) instanceof TermAngle)
        {
            TermAngle tx = (TermAngle) func.get(0);
            TermAngle ty = (TermAngle) func.get(1);
            return "matrix( " + 1 + " " + Math.tan(Math.toRadians(tx.getValue())) + " "
                    + Math.tan(Math.toRadians(ty.getValue())) + " " + 1 + " " + 0 + " " + 0 + " ) ";
        }
        return "";
    }

    private String getScaleX(TermFunction func)
    {
        if (func.size() == 1 && isNumber(func.get(0)))
        {
            double sx = getNumber(func.get(0));
            return "scale( " + sx + " ) ";
        }
        return "";
    }

    private String getScaleY(TermFunction func)
    {
        if (func.size() == 1 && isNumber(func.get(0)))
        {
            double sy = getNumber(func.get(0));
            return "scale( 0 " + sy + " ) ";
        }
        return "";
    }

    private String getScale(TermFunction func)
    {
        if (func.size() == 1 && isNumber(func.get(0)))
        {
            double sx = getNumber(func.get(0));
            return "scale( " + sx + " ) ";
        }
        else if (func.size() == 2 && isNumber(func.get(0)) && isNumber(func.get(1)))
        {
            double sx = getNumber(func.get(0));
            double sy = getNumber(func.get(1));
            return "scale( " + sx + " " + sy + " ) ";
        }
        return "";
    }

    private String getTranslateX(TermFunction func)
    {
        if (func.size() == 1 && func.get(0) instanceof TermLengthOrPercent)
        {
            int tx = dec.getLength((TermLengthOrPercent) func.get(0), false, 0, 0, bounds.width);
            return "translate( " + tx + " ) ";
        }
        return "";
    }

    private String getTranslateY(TermFunction func)
    {
        if (func.size() == 1 && func.get(0) instanceof TermLengthOrPercent)
        {
            int ty = dec.getLength((TermLengthOrPercent) func.get(0), false, 0, 0, bounds.height);
            return "translate( 0 " + ty + " ) ";
        }
        return "";
    }

    private String getTranslate(TermFunction func)
    {
        if (func.size() == 1 && func.get(0) instanceof TermLengthOrPercent)
        {
            int tx = dec.getLength((TermLengthOrPercent) func.get(0), false, 0, 0, bounds.width);
            int ty = 0;
            return "translate( " + tx + " " + ty + " ) ";
        }
        else if (func.size() == 2 && func.get(0) instanceof TermLengthOrPercent
                && func.get(1) instanceof TermLengthOrPercent)
        {
            int tx = dec.getLength((TermLengthOrPercent) func.get(0), false, 0, 0, bounds.width);
            int ty = dec.getLength((TermLengthOrPercent) func.get(1), false, 0, 0, bounds.height);
            return "translate( " + tx + " " + ty + " ) ";
        }
        return "";
    }

    private String getRotate(TermFunction func)
    {
        if (func.size() == 1 && func.get(0) instanceof TermAngle)
        {
            TermAngle t = (TermAngle) func.get(0);
            return "rotate( " + t.getValue() + " " + 0 + " " + 0 + " ) ";
        }
        return "";
    }

    private static boolean isNumber(Term<?> term)
    {
        return term instanceof TermNumber || term instanceof TermInteger;
    }

    private static float getNumber(Term<?> term)
    {
        if (term instanceof TermNumber)
        {
            return ((TermNumber) term).getValue();
        }
        else
        {
            return ((TermInteger) term).getValue();
        }
    }
}
