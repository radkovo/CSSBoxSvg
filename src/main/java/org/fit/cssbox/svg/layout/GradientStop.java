
package org.fit.cssbox.svg.layout;

import java.awt.Color;

/**
 * Trida pro reprezentaci zastavky gradientu obsahuje hodnotu v procentech a
 * barvu
 * 
 * @author Martin Safar
 */
public class GradientStop
{

    public Color c;
    public int i;

    public GradientStop(Color cVal, int iVal)
    {
        c = cVal;
        i = iVal;
    }

}
