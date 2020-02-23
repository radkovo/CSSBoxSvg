
package org.fit.cssbox.svg.render;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import javax.imageio.ImageIO;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.TermColor;
import cz.vutbr.web.css.TermFunction;
import cz.vutbr.web.css.TermIdent;
import cz.vutbr.web.css.TermLengthOrPercent;
import cz.vutbr.web.csskit.Color;

import java.util.Collection;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.fit.cssbox.layout.BackgroundImage;
import org.fit.cssbox.layout.BlockBox;
import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.CSSDecoder;
import org.fit.cssbox.layout.LengthSet;
import org.fit.cssbox.layout.ListItemBox;
import org.fit.cssbox.layout.Rectangle;
import org.fit.cssbox.layout.ReplacedContent;
import org.fit.cssbox.layout.ReplacedImage;
import org.fit.cssbox.layout.ReplacedText;
import org.fit.cssbox.layout.Viewport;
import org.fit.cssbox.layout.VisualContext;
import org.fit.cssbox.misc.Base64Coder;
import org.fit.cssbox.render.BoxRenderer;
import org.fit.cssbox.svg.layout.Border;
import org.fit.cssbox.svg.layout.CornerRadius;
import org.fit.cssbox.svg.layout.DPoint;
import org.fit.cssbox.svg.layout.GradientStop;
import org.fit.cssbox.svg.layout.LinearGradient;
import org.fit.cssbox.svg.layout.RadialGradient;
import org.fit.cssbox.svg.layout.Transform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.ReplacedBox;
import org.fit.cssbox.layout.TextBox;

/**
 * A box rendered that produces an SVG DOM model as the output.
 *
 * @author Martin Safar
 * @author burgetr
 */
public class SVGDOMRenderer implements BoxRenderer
{
    private static final float MIN = 0.0001f; //minimal coordinate difference to take into account
    
    private PrintWriter out;

    private float rootw;
    private float rooth;

    private int idcounter;

    private Document doc;
    private final String svgNS = "http://www.w3.org/2000/svg";
    private final String xlinkNS = "http://www.w3.org/1999/xlink";

    /** Generated SVG root */
    private Element svgRoot;

    private Stack<Element> elemStack;

    private boolean streamResult;

    /**
     *
     * @param rootWidth
     * @param rootHeight
     * @param out
     */
    public SVGDOMRenderer(float rootWidth, float rootHeight, Writer out)
    {
        elemStack = new Stack<Element>();
        doc = createDocument();
        idcounter = 1;
        rootw = rootWidth;
        rooth = rootHeight;
        streamResult = true;
        this.out = new PrintWriter(out);

        writeHeader();
    }

    /**
     *
     * @param rootWidth
     * @param rootHeight
     */
    public SVGDOMRenderer(float rootWidth, float rootHeight)
    {
        elemStack = new Stack<Element>();
        doc = createDocument();
        idcounter = 1;
        rootw = rootWidth;
        rooth = rootHeight;
        streamResult = false;

        writeHeader();
    }

    //====================================================================================================

    public Element getCurrentElem()
    {
        return elemStack.peek();
    }

    public Document getDocument()
    {
        return doc;
    }

    //====================================================================================================
    
    private Document createDocument() 
    {
        try
        {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            doc = builder.getDOMImplementation().createDocument(svgNS, "svg", null);
            return doc;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void writeHeader()
    {
        svgRoot = doc.getDocumentElement();
        elemStack.push(svgRoot);

        svgRoot.setAttribute("width", Float.toString(rootw) + "px");
        svgRoot.setAttribute("height", Float.toString(rooth) + "px");
        svgRoot.setAttribute("viewBox", "0 0 " + rootw + " " + rooth);
        svgRoot.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xlink", xlinkNS);
        
        svgRoot.appendChild(doc.createComment(" Rendered by CSSBox http://cssbox.sourceforge.net "));
    }

    private void writeFooter()
    {
        if (streamResult)
        {
            try
            {
                TransformerFactory tFactory = TransformerFactory.newInstance();
                Transformer transformer;
                transformer = tFactory.newTransformer();

                transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(doc);
                StreamResult result = new StreamResult(out);

                transformer.transform(source, result);
            } catch (TransformerConfigurationException ex)
            {
                Logger.getLogger(SVGDOMRenderer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerException ex)
            {
                Logger.getLogger(SVGDOMRenderer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void startElementContents(ElementBox elem)
    {

        boolean useGroup = false;
        final Element g = createElement("g");
        if (clippingUsed(elem))
        {
            final Rectangle cb = elem.getClippedContentBounds();
            final String clip = "cssbox-clip-" + idcounter;
            final Element clipPath = createElement("clipPath");
            clipPath.setAttribute("id", clip);
            clipPath.appendChild(createRect(cb.x, cb.y, cb.width, cb.height, ""));
            getCurrentElem().appendChild(clipPath);

            g.setAttribute("id", "cssbox-obj-" + (idcounter++));
            g.setAttribute("clip-path", "url(#" + clip + ")");
            useGroup = true;
        }

        Transform t = new Transform();
        String tm = t.createTransform(elem);
        if (!tm.equals(""))
        {
            g.setAttribute("transform", tm);
            useGroup = true;
        }

        String opacity = elem.getStylePropertyValue("opacity");
        if (opacity != "")
        {
            g.setAttribute("opacity", opacity);
            useGroup = true;
        }

        if (useGroup)
            elemStack.push(g);
    }

    public void finishElementContents(ElementBox elem)
    {
        if (elemStack.peek() != svgRoot)
        {
            Element buf = elemStack.pop();
            getCurrentElem().appendChild(buf);
        }
    }

    public void renderElementBackground(ElementBox eb)
    {
        Element backgroundStore = createElement("g");
        backgroundStore.setAttribute("id", "bgstore" + (idcounter++));

        Element bgWrap = createElement("g"); //background wrapper
        boolean bgUsed = false;

        final CSSProperty.BackgroundImage bgImageSpec = eb.getStyle().getProperty("background-image");
        if (bgImageSpec == CSSProperty.BackgroundImage.gradient)
        {
            TermFunction.Gradient gradFunction = eb.getStyle().getValue(TermFunction.Gradient.class, "background-image");
            if (gradFunction instanceof TermFunction.LinearGradient)
            {
                addLinearGradient(eb, (TermFunction.LinearGradient) gradFunction, bgWrap);
                bgUsed = true;
            }
            else if (gradFunction instanceof TermFunction.RadialGradient)
            {
                addRadialGradient(eb, (TermFunction.RadialGradient) gradFunction, bgWrap);
                bgUsed = true;
            }
        }
        
        Rectangle bb = eb.getAbsoluteBorderBounds();
        if (eb instanceof Viewport)
        {
            bb = eb.getClippedBounds();
        }
        final Color bg = eb.getBgcolor();
        if (bg != null) // color background
        {
            final String style = "stroke:none;fill-opacity:1;fill:" + colorString(bg);
            bgWrap.appendChild(createRect(bb.x, bb.y, bb.width, bb.height, style));
            bgUsed = true;
        }

        // image background
        if (eb.getBackgroundImages() != null && eb.getBackgroundImages().size() > 0)
        {
            for (BackgroundImage bimg : eb.getBackgroundImages())
            {
                BufferedImage img = bimg.getBufferedImage();
                if (img != null)
                {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try
                    {
                        ImageIO.write(img, "png", os);
                    } catch (IOException e)
                    {
                        //out.println("<!-- I/O error: " + e.getMessage() + " -->");
                    }
                    char[] data = Base64Coder.encode(os.toByteArray());
                    String imgdata = "data:image/png;base64," + new String(data);
                    float ix = bb.x + eb.getBorder().left;
                    float iy = bb.y + eb.getBorder().top;
                    float iw = bb.width - eb.getBorder().right - eb.getBorder().left;
                    float ih = bb.height - eb.getBorder().bottom - eb.getBorder().top;
                    bgWrap.appendChild(createImage(ix, iy, iw, ih, imgdata));
                    bgUsed = true;
                }
            }
        }

        // generate a border group
        final Element gBorder = createElement("g");
        gBorder.setAttribute("id", "borders-" + (idcounter++));
        // generate border parts
        elemStack.push(gBorder);
        final Border border = new Border(eb.getBorder(), bb, eb);
        final boolean bordersUsed = writeBorders(eb, border);
        elemStack.pop();
        
        // create a clip element for the border if necessary
        if (bordersUsed || clippingUsed(eb))
        {
            final String clipId = "cssbox-clip-" + (idcounter++);
            final Element clipPath = createElement("clipPath");
            clipPath.setAttribute("id", clipId);
            final Element q = getClipPathElementForBorder(border);
            clipPath.appendChild(q);
            bgWrap.setAttribute("clip-path", "url(#" + clipId + ")");
            bgUsed = true;
            backgroundStore.appendChild(clipPath);
        }
        // append the wrapper and borders when used
        if (bgUsed)
            backgroundStore.appendChild(bgWrap);
        if (bordersUsed)
            backgroundStore.appendChild(gBorder);

        // append the whole backgound group when something was used
        if (bgUsed || bordersUsed)
        {
            // if transform was used, transform the backgound as well
            final Transform t = new Transform();
            final String tm = t.createTransform(eb);
            if (!tm.isEmpty())
            {
                backgroundStore.setAttribute("transform", tm);
            }
    
            // if opacity is applied to the element, make the background opaque as well
            final String opacity = eb.getStylePropertyValue("opacity");
            if (!opacity.isEmpty())
            {
                backgroundStore.setAttribute("opacity", opacity);
            }
    
            getCurrentElem().appendChild(backgroundStore);
        }
    }

    @Override
    public void renderMarker(ListItemBox elem)
    {
        if (elem.getMarkerImage() != null)
        {
            if (!writeMarkerImage(elem)) writeBullet(elem);
        }
        else
            writeBullet(elem);
    }

    private boolean writeMarkerImage(ListItemBox lb)
    {
        VisualContext ctx = lb.getVisualContext();
        float ofs = lb.getFirstInlineBoxBaseline();
        if (ofs == -1) ofs = ctx.getBaselineOffset(); //use the font baseline
        float x = lb.getAbsoluteContentX() - 0.5f * ctx.getEm();
        float y = lb.getAbsoluteContentY() + ofs;
        BufferedImage img = lb.getMarkerImage().getBufferedImage();
        if (img != null)
        {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try
            {
                ImageIO.write(img, "png", os);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            char[] data = Base64Coder.encode(os.toByteArray());
            String imgdata = "data:image/png;base64," + new String(data);
            float iw = img.getWidth();
            float ih = img.getHeight();
            float ix = x - iw;
            float iy = y - ih;
            //out.println("<image x=\"" + ix + "\" y=\"" + iy + "\" width=\"" + iw + "\" height=\"" + ih + "\" xlink:href=\"" + imgdata + "\" />");
            Element image = doc.createElementNS(svgNS, "image");
            image.setAttribute("x", Float.toString(ix));
            image.setAttribute("y", Float.toString(iy));
            image.setAttribute("width", Float.toString(iw));
            image.setAttribute("height", Float.toString(ih));
            image.setAttributeNS(xlinkNS, "xlink:href", imgdata);
            getCurrentElem().appendChild(image);
            return true;
        }
        else
            return false;
    }

    private void writeBullet(ListItemBox lb)
    {
        if (lb.hasVisibleBullet())
        {
            VisualContext ctx = lb.getVisualContext();
            float x = lb.getAbsoluteContentX() - 1.2f * ctx.getEm();
            float y = lb.getAbsoluteContentY() + 0.5f * ctx.getEm();
            float r = 0.4f * ctx.getEm();

            String tclr = colorString(ctx.getColor());
            String style = "";

            switch (lb.getListStyleType())
            {
                case "circle":
                    style = "fill:none;fill-opacity:1;stroke:" + tclr
                            + ";stroke-width:1;stroke-miterlimit:4;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1";
                    //out.println("<circle style=\"" + style + "\" cx=\"" + (x + r / 2) + "\" cy=\"" + (y + r / 2) + "\" r=\"" + (r / 2) + "\" />");
                    Element circle = createElement("circle");
                    circle.setAttribute("cx", Float.toString(x + r / 2));
                    circle.setAttribute("cy", Float.toString(y + r / 2));
                    circle.setAttribute("r", Float.toString(r / 2));
                    circle.setAttribute("style", style);
                    getCurrentElem().appendChild(circle);
                    break;
                case "square":
                    style = "fill:" + tclr + ";fill-opacity:1;stroke:" + tclr
                            + ";stroke-width:1;stroke-miterlimit:4;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1";
                    //out.println("<rect style=\"" + style + "\" x=\"" + x + "\" y=\"" + y + "\" width=\"" + r + "\" height=\"" + r + "\" />");
                    Element rect = createElement("rect");
                    rect.setAttribute("x", Float.toString(x));
                    rect.setAttribute("y", Float.toString(y));
                    rect.setAttribute("width", Float.toString(r));
                    rect.setAttribute("height", Float.toString(r));
                    rect.setAttribute("style", style);
                    getCurrentElem().appendChild(rect);
                    break;
                case "disc":
                    style = "fill:" + tclr + ";fill-opacity:1;stroke:" + tclr
                            + ";stroke-width:1;stroke-miterlimit:4;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1";
                    //out.println("<circle style=\"" + style + "\" cx=\"" + (x + r / 2) + "\" cy=\"" + (y + r / 2) + "\" r=\"" + (r / 2) + "\" />");
                    Element disc = createElement("circle");
                    disc.setAttribute("cx", Float.toString(x + r / 2));
                    disc.setAttribute("cy", Float.toString(y + r / 2));
                    disc.setAttribute("r", Float.toString(r / 2));
                    disc.setAttribute("style", style);
                    getCurrentElem().appendChild(disc);
                    break;
                default:
                    float baseline = lb.getFirstInlineBoxBaseline();
                    if (baseline == -1) baseline = ctx.getBaselineOffset(); //use the font baseline
                    style = textStyle(ctx) + ";text-align:end;text-anchor:end";
                    addText(getCurrentElem(), lb.getAbsoluteContentX() - 0.5f * ctx.getEm(),
                            lb.getAbsoluteContentY() + baseline, style, lb.getMarkerText());
                    break;
            }
        }
    }

    private String textStyle(VisualContext ctx)
    {
        String style = "font-size:" + ctx.getFontSize() + "pt;" + "font-weight:"
                + (ctx.getFontInfo().isBold() ? "bold" : "normal") + ";" + "font-style:"
                + (ctx.getFontInfo().isItalic() ? "italic" : "normal") + ";" + "font-family:" + ctx.getFontInfo().getFamily()
                + ";" + "fill:" + colorString(ctx.getColor()) + ";" + "stroke:none";
        if (ctx.getLetterSpacing() > 0.0001)
            style += ";letter-spacing:" + ctx.getLetterSpacing() + "px";
        return style;
    }

    public void renderTextContent(TextBox text)
    {
        Rectangle b = text.getAbsoluteBounds();
        String style = textStyle(text.getVisualContext())
                + ";" + textDecorationStyle(text.getEfficientTextDecoration());
        if (text.getWordSpacing() == null && text.getExtraWidth() == 0)
            addText(getCurrentElem(), b.x, b.y + text.getBaselineOffset(), b.width, b.height, style, text.getText());
        else
            addTextByWords(getCurrentElem(), b.x, b.y + text.getBaselineOffset(), b.width, b.height, style, text);
    }

    private void addText(Element parent, float x, float y, String style, String text)
    {
        //out.print("<text x=\"" + x + "\" y=\"" + y + "\" style=\"" + style + "\">" + htmlEntities(text) + "</text>");
        Element txt = doc.createElementNS(svgNS, "text");
        txt.setAttributeNS(XMLConstants.XML_NS_URI, "space", "preserve");
        txt.setAttribute("x", Float.toString(x));
        txt.setAttribute("y", Float.toString(y));
        txt.setAttribute("style", style);
        txt.setTextContent(text);
        parent.appendChild(txt);
    }

    private void addText(Element parent, float x, float y, float width, float height, String style, String text)
    {
        //out.print("<text x=\"" + x + "\" y=\"" + y + "\" width=\"" + width + "\" height=\"" + height + "\" style=\"" + style + "\">" + htmlEntities(text) + "</text>");
        Element txt = doc.createElementNS(svgNS, "text");
        txt.setAttributeNS(XMLConstants.XML_NS_URI, "space", "preserve");
        txt.setAttribute("x", Float.toString(x));
        txt.setAttribute("y", Float.toString(y));
        txt.setAttribute("width", Float.toString(width));
        txt.setAttribute("height", Float.toString(height));
        txt.setAttribute("style", style);
        txt.setTextContent(text);
        parent.appendChild(txt);
    }

    private void addTextByWords(Element parent, float x, float y, float width, float height, String style, TextBox text)
    {
        final String[] words = text.getText().split(" ");
        if (words.length > 0)
        {
            Element g = doc.createElementNS(svgNS, "g");
            final float[][] offsets = text.getWordOffsets(words);
            for (int i = 0; i < words.length; i++)
                addText(g, x + offsets[i][0], y, offsets[i][1], height, style, words[i]);
            parent.appendChild(g);
        }
        else
            addText(parent, x, y, width, height, style, text.getText());
    }

    public void renderReplacedContent(ReplacedBox box)
    {
        ReplacedContent cont = box.getContentObj();
        if (cont != null)
        {
            if (cont instanceof ReplacedImage)
            {
                BufferedImage img = ((ReplacedImage) cont).getBufferedImage();
                if (img != null)
                {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try
                    {
                        ImageIO.write(img, "png", os);
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    char[] data = Base64Coder.encode(os.toByteArray());
                    String imgdata = "data:image/png;base64," + new String(data);
                    Rectangle cb = ((Box) box).getAbsoluteContentBounds();

                    Element image = doc.createElementNS(svgNS, "image");
                    image.setAttribute("x", Float.toString(cb.x));
                    image.setAttribute("y", Float.toString(cb.y));
                    image.setAttribute("width", Float.toString(cb.width));
                    image.setAttribute("height", Float.toString(cb.height));
                    image.setAttributeNS(xlinkNS, "xlink:href", imgdata);
                    getCurrentElem().appendChild(image);
                }
            }
            else if (cont instanceof ReplacedText)
            {//HTML objekty
                final Rectangle cb = ((Box) box).getClippedBounds();
                final String clip = "cssbox-clip-" + idcounter;

                final Element clipPath = doc.createElementNS(svgNS, "clipPath");
                clipPath.setAttribute("id", clip);
                clipPath.appendChild(createRect(cb.x, cb.y, cb.width, cb.height, ""));
                getCurrentElem().appendChild(clipPath);

                final Element g = doc.createElementNS(svgNS, "g");
                g.setAttribute("id", "cssbox-obj-" + (idcounter++));
                g.setAttribute("clip-path", "url(#" + clip + ")");
                getCurrentElem().appendChild(g);
            }
        }
    }

    public void close()
    {
        writeFooter();
    }

    private String colorString(TermColor color)
    {
        return colorString(color.getValue());
    }

    private String colorString(Color color)
    {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private String textDecorationStyle(Collection<CSSProperty.TextDecoration> textDecoration)
    {
        if (textDecoration.isEmpty())
            return "text-decoration:none";
        else
        {
            boolean first = true;
            StringBuilder ret = new StringBuilder("text-decoration:");
            for (CSSProperty.TextDecoration dec : textDecoration)
            {
                if (!first)
                    ret.append(' ');
                ret.append(dec.toString());
                first = false;
            }
            return ret.toString();
        }
    }

    /**
     * Checks whether the clipping is necessary for the element: it has overflow different from visible.
     * @param elem
     * @return
     */
    private boolean clippingUsed(ElementBox elem)
    {
        return (elem instanceof BlockBox && ((BlockBox) elem).getOverflowX() != BlockBox.OVERFLOW_VISIBLE);
    }
    
    /**
     *
     * @param border
     * @return
     */
    private Element getClipPathElementForBorder(Border border)
    {
        Element q;
        CornerRadius crTopLeft = border.getRadius(2);
        CornerRadius crTopRight = border.getRadius(1);
        CornerRadius crBottomLeft = border.getRadius(4);
        CornerRadius crBottomRight = border.getRadius(3);

        // postupne dokola kolem celeho elementu, pomoci SVG path prikazu elipticky oblouk a linka
        String path = "M " + (crTopLeft.d.x) + " " + (crTopLeft.d.y) + " ";

        path += " L " + crTopRight.b.x + " " + (crTopRight.b.y) + " ";

        if (crTopRight.isDrawn)
        {
            path += " A " + (crTopRight.x - border.border.right) + " " + (crTopRight.y - border.border.top) + " 0 0 1 "
                    + crTopRight.d.x + " " + crTopRight.d.y;
        }
        else
        {
            path += " L " + crTopRight.g.x + " " + crTopRight.g.y;
            path += " L " + crTopRight.d.x + " " + crTopRight.d.y;

        }

        path += " L " + crBottomRight.b.x + " " + (crBottomRight.b.y) + " ";

        if (crBottomRight.isDrawn)
        {
            path += " A " + (crBottomRight.x - border.border.right) + " " + (crBottomRight.y - border.border.bottom)
                    + " 0 0 1 " + crBottomRight.d.x + " " + crBottomRight.d.y;
        }
        else
        {
            path += " L " + crBottomRight.g.x + " " + crBottomRight.g.y;
            path += " L " + crBottomRight.d.x + " " + crBottomRight.d.y;
        }
        path += " L " + crBottomLeft.b.x + " " + (crBottomLeft.b.y) + " ";
        if (crBottomLeft.isDrawn)
        {
            path += " A " + (crBottomLeft.x - border.border.left) + " " + (crBottomLeft.y - border.border.bottom)
                    + " 0 0 1 " + crBottomLeft.d.x + " " + crBottomLeft.d.y;
        }
        else
        {
            path += " L " + crBottomLeft.g.x + " " + crBottomLeft.g.y;
            path += " L " + crBottomLeft.d.x + " " + crBottomLeft.d.y;
        }

        path += " L " + crTopLeft.b.x + " " + (crTopLeft.b.y) + " ";
        if (crTopLeft.isDrawn)
        {
            path += " A " + (crTopLeft.x - border.border.left) + " " + (crTopLeft.y - border.border.top) + " 0 0 1 "
                    + crTopLeft.d.x + " " + crTopLeft.d.y;
        }
        else
        {
            path += " L " + crTopLeft.g.x + " " + crTopLeft.g.y;
            path += " L " + crTopLeft.d.x + " " + crTopLeft.d.y;
        }
        q = createPath(path, "none", "none", 0);
        return q;
    }

    /**
     *
     * @param eb
     * @param b
     * @return {@code true} when something has been written, {@code false} means empty borders
     */
    private boolean writeBorders(ElementBox eb, Border b)
    {
        boolean ret = false;
        final LengthSet borders = b.border;

        // generate borders
        ret |= writeBorderSVG(eb, b.topLeftH, b.topRightH, "top", borders.top);
        ret |= writeBorderSVG(eb, b.topRightV, b.bottomRightV, "right", borders.right);
        ret |= writeBorderSVG(eb, b.bottomLeftH, b.bottomRightH, "bottom", borders.bottom);
        ret |= writeBorderSVG(eb, b.topLeftV, b.bottomLeftV, "left", borders.left);

        // generate corners
        ret |= writeBorderCorner(b, 1);
        ret |= writeBorderCorner(b, 2);
        ret |= writeBorderCorner(b, 3);
        ret |= writeBorderCorner(b, 4);
        
        return ret;
    }

    /**
     *
     * @param border
     * @param s
     */
    private boolean writeBorderCorner(Border border, int s)
    {
        final CornerRadius cr = border.getRadius(s);
        final float radx = cr.x;
        final float rady = cr.y;
        if (radx > MIN || rady > MIN)
        {
            TermColor startColor;
            TermColor stopColor;
            float widthHor, widthVer;
    
            // podle toho, ktery roh je vykreslovan ziskame sirky ramecku a barvy v prislusnych smerech 
            if (s == 1)
            { // top-right
                widthHor = border.border.right;
                widthVer = border.border.top;
                startColor = border.colorRight;
                stopColor = border.colorTop;
            }
            else if (s == 2)
            { // topleft
                widthHor = border.border.left;
                widthVer = border.border.top;
                startColor = border.colorTop;
                stopColor = border.colorLeft;
            }
            else if (s == 3)
            { // bottomright
                widthHor = border.border.right;
                widthVer = border.border.bottom;
                startColor = border.colorBottom;
                stopColor = border.colorRight;
            }
            else
            { // bottomleft
                widthHor = border.border.left;
                widthVer = border.border.bottom;
                startColor = border.colorLeft;
                stopColor = border.colorBottom;
            }
            
            if (startColor != null && stopColor != null)
            {
                final String cString1 = colorString(startColor);
                final String cString2 = colorString(stopColor);
        
                String path1 = cr.getPathRadiusC(widthVer, widthHor);
                String path2 = cr.getPathRadiusA(widthVer, widthHor);
        
                if (widthVer > rady || widthHor > radx)
                {
                    cr.isDrawn = false;
                }
        
                Element q = createPath(path1, cString1, "none", 1);
                getCurrentElem().appendChild(q);
                q = createPath(path2, cString2, "none", 1);
                getCurrentElem().appendChild(q);
                return true;
            }
            else
                return false;
        }
        else
            return false;
    }

    /**
     *
     * @param eb
     * @param a
     * @param b
     * @param side
     * @param width
     * @return {@code true} when something has been written
     */
    private boolean writeBorderSVG(ElementBox eb, DPoint a, DPoint b, String side, float width)
    {
        TermColor tclr = eb.getStyle().getValue(TermColor.class, "border-" + side + "-color");
        CSSProperty.BorderStyle bst = eb.getStyle().getProperty("border-" + side + "-style");
        if (bst != null
                && bst != CSSProperty.BorderStyle.NONE
                && bst != CSSProperty.BorderStyle.HIDDEN 
                && (tclr == null || !tclr.isTransparent()))
        {
            Color clr = null;
            if (tclr != null)
            {
                clr = tclr.getValue();
            }
            if (clr == null)
            {
                clr = eb.getVisualContext().getColor();
                if (clr == null)
                {
                    clr = new Color(0, 0, 0);
                }
            }
            String coords = "";
            switch (side)
            {
                case "left":
                    coords = "M " + a.x + "," + a.y + " L " + b.x + "," + b.y + " L " + (b.x + width) + "," + b.y
                            + " L " + (a.x + width) + "," + a.y;
                    break;
                case "top":
                    coords = "M " + a.x + "," + a.y + " L " + b.x + "," + b.y + " L " + (b.x) + "," + (b.y + width)
                            + " L " + a.x + "," + (a.y + width);
                    break;
                case "right":
                    coords = "M " + a.x + "," + a.y + " L " + b.x + "," + b.y + " L " + (b.x - width) + "," + b.y
                            + " L " + (a.x - width) + "," + a.y;
                    break;
                case "bottom":
                    coords = "M " + a.x + "," + a.y + " L " + b.x + "," + b.y + " L " + (b.x) + "," + (b.y - width)
                            + " L " + a.x + "," + (a.y - width);
                    break;
            }

            Element path = createPath(coords, colorString(clr), colorString(clr), 0);
            getCurrentElem().appendChild(path);
            return true;
        }
        else
            return false;
    }

    private void addRadialGradient(ElementBox eb, TermFunction.RadialGradient spec, Element dest)
    {
        CSSDecoder dec = new CSSDecoder(eb.getVisualContext());
        
        Rectangle bb = eb.getAbsoluteBorderBounds();
        // element sizes
        float ix = bb.x + eb.getBorder().left;
        float iy = bb.y + eb.getBorder().top;
        float iw = bb.width - eb.getBorder().right - eb.getBorder().left;
        float ih = bb.height - eb.getBorder().bottom - eb.getBorder().top;

        // stops
        RadialGradient grad = new RadialGradient(eb.getClippedContentBounds());
        for (TermFunction.Gradient.ColorStop stop : spec.getColorStops())
        {
            Color color = stop.getColor().getValue();
            Float percentage = decodePercentage(eb, stop.getLength(), dec, iw); //TODO iw?
            grad.addStop(new GradientStop(color, percentage));
        }
        grad.recomputeStops();

        // position
        float px = dec.getLength(spec.getPosition()[0], false, 0, 0, iw);
        float py = dec.getLength(spec.getPosition()[1], false, 0, 0, ih);
        
        if ("circle".equals(spec.getShape().getValue()))
        {
            RadialGradient.RadLengths ident = decodeSizeIdent(spec.getSizeIdent());
            if (ident != null)
            {
                grad.setCircleDataRadLengths(ident, px, py);
            }
            else
            {
                double r = dec.getLength(spec.getSize()[0], false, 0, 0, iw);
                grad.setCircleData(r, px, py);
            }
        }
        else //ellipse
        {
            RadialGradient.RadLengths ident = decodeSizeIdent(spec.getSizeIdent());
            if (ident != null)
            {
                grad.setEllipseDataRadLengths(ident, px, py);
            }
            else
            {
                double rx = dec.getLength(spec.getSize()[0], false, 0, 0, iw);
                double ry = dec.getLength(spec.getSize()[1], false, 0, 0, ih);
                grad.setEllipseData(rx, ry, px, py);
            }
        }
        
        final String url = "cssbox-gradient-" + (idcounter++);
        final Element defs = createElement("defs");
        final Element image;
        image = createElement("radialGradient");

        // geterate the SVG element for gradient
        image.setAttribute("r", String.valueOf(grad.r) + "%");
        image.setAttribute("cx", String.valueOf(grad.cx) + "%");
        image.setAttribute("cy", String.valueOf(grad.cy) + "%");
        image.setAttribute("fx", String.valueOf(grad.fx) + "%");
        image.setAttribute("fy", String.valueOf(grad.fy) + "%");
        image.setAttribute("id", url);
        for (int i = 0; i < grad.getStops().size(); i++)
        {
            Element stop = createElement("stop");
            Color cc = grad.getStops().get(i).getColor();
            stop.setAttribute("offset", "" + grad.getStops().get(i).getPercentage() + "%");
            stop.setAttribute("style",
                    "stop-color:rgb(" + cc.getRed() + "," + cc.getGreen() + "," + cc.getBlue() +
                    ");stop-opacity:" + (cc.getAlpha() / 255.0f));
            image.appendChild(stop);
        }

        defs.appendChild(image);
        dest.appendChild(defs);
        String style = "stroke:none;fill-opacity:1;fill:url(#" + url + ");";
        String clip = "cssbox-clip-" + idcounter;
        Element clipPath = createElement("clipPath");
        clipPath.setAttribute("id", clip);

        clipPath.appendChild(createRect(ix, iy, iw, ih, ""));
        dest.appendChild(clipPath);

        // generate the background element
        // additionally, a clip element is generated
        if (grad.isCircle())
        {
            float max = Math.max(iw, ih);
            double x = (max == iw ? ix : ix - ((max - iw) * grad.cx / 100));
            double y = (max == ih ? iy : iy - ((max - ih) * grad.cy / 100));
            Element e = createRect(x, y, max, max, style);
            e.setAttribute("clip-path", "url(#" + clip + ")");
            dest.appendChild(e);
        }
        else
        {
            //   float max = Math.max(iw, ih);
            double x = (grad.newWidth == iw ? ix : ix - ((grad.newWidth - iw) * grad.cx / 100));
            double y = (grad.newHeight == ih ? iy : iy - ((grad.newHeight - ih) * grad.cy / 100));

            Element e = createRect(x, y, grad.newWidth, grad.newHeight, style);
            e.setAttribute("clip-path", "url(#" + clip + ")");
            dest.appendChild(e);
        }
    }
    
    private void addLinearGradient(ElementBox eb, TermFunction.LinearGradient spec, Element dest)
    {
        CSSDecoder dec = new CSSDecoder(eb.getVisualContext());
        
        Rectangle bb = eb.getAbsoluteBorderBounds();
        // obtain the element size 
        float ix = bb.x + eb.getBorder().left;
        float iy = bb.y + eb.getBorder().top;
        float iw = bb.width - eb.getBorder().right - eb.getBorder().left;
        float ih = bb.height - eb.getBorder().bottom - eb.getBorder().top;

        // gradient angle and size
        LinearGradient grad = new LinearGradient();
        double angle = (spec.getAngle() != null) ? eb.getVisualContext().degAngle(spec.getAngle()) : 180.0;
        grad.setAngleDeg(angle, iw, ih);
        /*System.out.println("iw=" + iw + " ih=" + ih + " " 
                + " x1=" + grad.x1
                + " y1=" + grad.y1
                + " x2=" + grad.x2
                + " y2=" + grad.y2
                + " a=" + angle
                );
        System.out.println("len=" + grad.getGradientLength(iw, ih));*/
        // stops
        for (TermFunction.Gradient.ColorStop stop : spec.getColorStops())
        {
            Color color = stop.getColor().getValue();
            Float percentage = decodePercentage(eb, stop.getLength(), dec, Math.sqrt(iw*iw+ih*ih)); //TODO iw?
            grad.addStop(new GradientStop(color, percentage));
        }
        grad.recomputeStops();
        // generate code
        String url = "cssbox-gradient-" + idcounter;
        idcounter++;
        // generate svg gradient incl. stops
        Element defs = createElement("defs");
        Element image;
        image = createElement("linearGradient");
        image.setAttribute("x1", String.valueOf(grad.x1) + "%");
        image.setAttribute("y1", String.valueOf(grad.y1) + "%");
        image.setAttribute("x2", String.valueOf(grad.x2) + "%");
        image.setAttribute("y2", String.valueOf(grad.y2) + "%");
        image.setAttribute("id", url);
        for (int i = 0; i < grad.getStops().size(); i++)
        {
            Element stop = createElement("stop");
            Color cc = grad.getStops().get(i).getColor();
            stop.setAttribute("offset", "" + grad.getStops().get(i).getPercentage() + "%");
            stop.setAttribute("style",
                    "stop-color:rgb(" + cc.getRed() + "," + cc.getGreen() + "," + cc.getBlue() +
                    ");stop-opacity:" + (cc.getAlpha() / 255.0f));
            image.appendChild(stop);
        }

        defs.appendChild(image);
        dest.appendChild(defs);

        String style = "stroke:none;fill-opacity:1;fill:url(#" + url + ");";

        // generate the element with the gradient background
        dest.appendChild(createRect(ix, iy, iw, ih, style));
    }
    
    private Float decodePercentage(ElementBox eb, TermLengthOrPercent spec, CSSDecoder dec, double wholeLength)
    {
        if (spec != null)
        {
            if (spec.isPercentage())
            {
                return spec.getValue();
            }
            else
            {
                float abs = dec.getLength(spec, false, 0, 0, 0);
                return (float) ((abs / wholeLength) * 100.0);
            }
        }
        else
            return null;
    }
    
    private RadialGradient.RadLengths decodeSizeIdent(TermIdent ident)
    {
        if (ident != null)
        {
            switch (ident.getValue())
            {
                case "closest-side":
                    return RadialGradient.RadLengths.CLOSEST_SIDE;
                case "closest-corner":
                    return RadialGradient.RadLengths.CLOSEST_CORNER;
                case "farthest-side":
                    return RadialGradient.RadLengths.FARTHEST_SIDE;
                case "farthest-corner":
                    return RadialGradient.RadLengths.FARTHEST_CORNER;
                default:
                    return null;
            }
        }
        else
            return null;
    }
    
    public Element createPath(String dPath, String fill, String stroke, float strokeWidth)
    {
        Element e = createElement("path");
        e.setAttribute("d", dPath);
        e.setAttribute("stroke", stroke);
        e.setAttribute("stroke-width", Float.toString(strokeWidth));
        e.setAttribute("fill", fill);
        return e;
    }

    public Element createRect(double x, double y, double width, double height, String style)
    {
        Element e = createElement("rect");
        e.setAttribute("x", Double.toString(x));
        e.setAttribute("y", Double.toString(y));
        e.setAttribute("width", Double.toString(width));
        e.setAttribute("height", Double.toString(height));
        e.setAttribute("style", style);
        return e;
    }

    public Element createRect(float x, float y, float width, float height, String style)
    {
        Element e = createElement("rect");
        e.setAttribute("x", Float.toString(x));
        e.setAttribute("y", Float.toString(y));
        e.setAttribute("width", Float.toString(width));
        e.setAttribute("height", Float.toString(height));
        e.setAttribute("style", style);
        return e;
    }

    public Element createImage(float x, float y, float width, float height, String imgData)
    {
        Element image = createElement("image");
        //text.setAttribute("id", );
        image.setAttribute("x", Float.toString(x));
        image.setAttribute("y", Float.toString(y));
        image.setAttribute("width", Float.toString(width));
        image.setAttribute("height", Float.toString(height));
        image.setAttributeNS(xlinkNS, "xlink:href", imgData);
        return image;
    }

    public Element createElement(String elementName)
    {
        return doc.createElementNS(svgNS, elementName);
    }

}
