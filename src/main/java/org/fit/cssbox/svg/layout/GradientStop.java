
package org.fit.cssbox.svg.layout;

import java.awt.Color;

/**
 * A single color stop representation.
 * 
 * @author Martin Safar
 * @author burgetr
 */
public class GradientStop
{
    private Color color;
    private Float percentage;

    public GradientStop(Color color, Float percentage)
    {
        this.color = color;
        this.percentage = percentage;
    }

    public Color getColor()
    {
        return color;
    }

    public Float getPercentage()
    {
        return percentage;
    }

    public void setColor(Color color)
    {
        this.color = color;
    }

    public void setPercentage(Float percentage)
    {
        this.percentage = percentage;
    }

}
