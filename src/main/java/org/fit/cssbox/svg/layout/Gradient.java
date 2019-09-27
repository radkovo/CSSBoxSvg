/**
 * Gradient.java
 *
 * Created on 27. 9. 2019, 12:34:00 by burgetr
 */
package org.fit.cssbox.svg.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * A base for all the gradients.
 * 
 * @author burgetr
 */
public class Gradient
{
    private List<GradientStop> stops;
    

    public Gradient()
    {
        stops = new ArrayList<>();
    }
    
    public List<GradientStop> getStops()
    {
        return stops;
    }
    
    public void addStop(GradientStop stop)
    {
        stops.add(stop);
    }

    /**
     * Computes missing percentages for stops.
     */
    public void recomputeStops()
    {
        if (!stops.isEmpty())
        {
            //the first one is 0% if missing
            GradientStop first = stops.get(0);
            if (first.getPercentage() == null)
                first.setPercentage(0.0f);
            //the last missing stop is 100%
            GradientStop last = stops.get(stops.size() - 1);
            if (last.getPercentage() == null)
                last.setPercentage(100.0f);
            //remaining stops are half between
            int f = -1; //first unknown percentage position
            for (int i = 1; i < stops.size(); i++)
            {
                GradientStop stop = stops.get(i);
                if (stop.getPercentage() == null)
                {
                    if (f == -1)
                    {
                        f = i;
                    }
                }
                else
                {
                    if (f != -1)
                    {
                        float fPerc = stops.get(f - 1).getPercentage();
                        float lPerc = stop.getPercentage();
                        float frac = (lPerc - fPerc) / (i - f + 1);
                        for (int j = f; j < i; j++)
                        {
                            stops.get(j).setPercentage(frac * (j - f + 1));
                        }
                        f = -1;
                    }
                }
            }
        }
    }
    
}
