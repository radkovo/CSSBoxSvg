package org.fit.cssbox.svg.render;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import javax.imageio.ImageIO;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.TermColor;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.anim.dom.SVGDOMImplementation;

import org.fit.cssbox.layout.BackgroundImage;
import org.fit.cssbox.layout.BlockBox;
import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.LengthSet;
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
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.ReplacedBox;
import org.fit.cssbox.layout.TextBox;

/**
 * A box rendered that produces an SVG DOM model as the output.
 *
 * @author Martin Safar
 */
public class SVGDOMRenderer implements BoxRenderer {

    private PrintWriter out;

    private int rootw;
    private int rooth;

    private int idcounter;

    private DOMImplementation impl;
    private Document doc;
    private String svgNS;

    /** Generated SVG root */
    private Element svgRoot;

    private Stack<Element> elemStack;

    private boolean streamResult;

    private Element backgroundStore;

    public Element getCurrentElem() {
        return elemStack.peek();
    }

    public Document getDocument() {
        return doc;
    }

    /**
     *
     * @param rootWidth
     * @param rootHeight
     * @param out
     */
    public SVGDOMRenderer(int rootWidth, int rootHeight, Writer out) {
        elemStack = new Stack<Element>();
        impl = SVGDOMImplementation.getDOMImplementation();
        svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        doc = impl.createDocument(svgNS, "svg", null);
        backgroundStore = null;
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
    public SVGDOMRenderer(int rootWidth, int rootHeight) {
        elemStack = new Stack<Element>();
        impl = SVGDOMImplementation.getDOMImplementation();
        svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        doc = impl.createDocument(svgNS, "svg", null);
        backgroundStore = null;
        idcounter = 1;
        rootw = rootWidth;
        rooth = rootHeight;
        streamResult = false;

        writeHeader();
    }

    //====================================================================================================

    private void writeHeader() {
        svgRoot = doc.getDocumentElement();
        elemStack.push(svgRoot);

        svgRoot.setAttributeNS(null, "width", Integer.toString(rootw) + "px");
        svgRoot.setAttributeNS(null, "height", Integer.toString(rooth) + "px");
        svgRoot.setAttributeNS(null, "viewBox", "0 0 " + rootw + " " + rooth);
        svgRoot.setAttributeNS(null, "zoomAndPan", "disable");
        svgRoot.setAttributeNS(null, "xmlns", "http://www.w3.org/2000/svg");
        svgRoot.setAttributeNS(null, "xlink", "http://www.w3.org/1999/xlink");
        svgRoot.setAttributeNS("xmlns", "space", "preserve");

    }

    private void writeFooter() {
        if (streamResult) {
            try {
                TransformerFactory tFactory
                        = TransformerFactory.newInstance();
                Transformer transformer;
                transformer = tFactory.newTransformer();

                transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(doc);
                StreamResult result = new StreamResult(out);

                transformer.transform(source, result);
            } catch (TransformerConfigurationException ex) {
                Logger.getLogger(SVGDOMRenderer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerException ex) {
                Logger.getLogger(SVGDOMRenderer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void startElementContents(ElementBox elem) {

        Element g;
        g = createElement("g");
//        if (backgroundStore != null) {
//            g.appendChild(backgroundStore);
//            //elemStack.pop();
//            backgroundStore = null;
//            
//        }
        if (elem instanceof BlockBox && ((BlockBox) elem).getOverflow() != BlockBox.OVERFLOW_VISIBLE) {
            Rectangle cb = elem.getClippedContentBounds();
            String clip = "cssbox-clip-" + idcounter;
            Element clipPath = createElement("clipPath");
            clipPath.setAttributeNS(null, "id", clip);

            clipPath.appendChild(createRect(cb.x, cb.y, cb.width, cb.height, ""));
            getCurrentElem().appendChild(clipPath);

            g.setAttributeNS(null, "id", "cssbox-obj-" + (idcounter++));
            g.setAttributeNS(null, "clip-path", "url(#" + clip + ")");
        }

        Transform t = new Transform();
        String tm = t.createTransform(elem);
        if (!tm.equals("")) {
            g.setAttributeNS(null, "transform", tm);
        }

        String opacity = elem.getStylePropertyValue("opacity");
        if (opacity != "") {
            g.setAttributeNS(null, "opacity", opacity);
        }
        elemStack.push(g);
    }

    public void finishElementContents(ElementBox elem) {
        if (elemStack.peek() != svgRoot) {
            Element buf;
            buf = elemStack.pop();
            getCurrentElem().appendChild(buf);
        }

    }

    public void renderElementBackground(ElementBox eb) {

        backgroundStore = createElement("g");

        backgroundStore.setAttributeNS(null, "id", "bgstore" + (idcounter++));

        //elemStack.push(backgroundStore);
        Element wrap;
        wrap = createElement("g");

        String background = eb.getStylePropertyValue("background-color");
        Rectangle bb = eb.getAbsoluteBorderBounds();
        if (eb instanceof Viewport) {
            bb = eb.getClippedBounds();
        }
        Color bg = eb.getBgcolor();
        if (background.equals("#112233")) { // simulace linearniho gradientu
            simulateLinearGradient(eb, 35);
        } else if (background.equals("#332233")) { // simulace radialniho gradientu
            simulateRadialGradient(eb);
        } else if (bg != null) { // pozadi urcene barvou
            String style = "stroke:none;fill-opacity:1;fill:" + colorString(bg);
            wrap.appendChild(createRect(bb.x, bb.y, bb.width, bb.height, style));
        }

        // pozadi urcene obrazkem
        if (eb.getBackgroundImages() != null && eb.getBackgroundImages().size() > 0) {
            for (BackgroundImage bimg : eb.getBackgroundImages()) {
                BufferedImage img = bimg.getBufferedImage();
                if (img != null) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try {
                        ImageIO.write(img, "png", os);
                    } catch (IOException e) {
                        out.println("<!-- I/O error: " + e.getMessage() + " -->");
                    }
                    char[] data = Base64Coder.encode(os.toByteArray());
                    String imgdata = "data:image/png;base64," + new String(data);
                    int ix = bb.x + eb.getBorder().left;
                    int iy = bb.y + eb.getBorder().top;
                    int iw = bb.width - eb.getBorder().right - eb.getBorder().left;
                    int ih = bb.height - eb.getBorder().bottom - eb.getBorder().top;
                    wrap.appendChild(createImage(ix, iy, iw, ih, imgdata));
                }
            }

        }

        // vygenerovani obaloveho elementu pro ramecek
        Element gBorder = createElement("g");
        gBorder.setAttributeNS(null, "id", "borders-" + (idcounter++));
        // obalovy element se nastavi jako hlavni
        elemStack.push(gBorder);
        // nasledne je vygenerovan ramecek pro cely element
        Border border = new Border(eb.getBorder(), bb, eb);
        writeBorders(eb, border);

        elemStack.pop();
        //border
        String clip = "cssbox-clip-" + idcounter;
        // podle vnejsi hranice ramecku je vygenerovan orezovy element
        Element clipPath = createElement("clipPath");
        clipPath.setAttributeNS(null, "id", clip);
        Element q = getClipPathElementForBorder(border);
        clipPath.appendChild(q);
        wrap.setAttributeNS(null, "clip-path", "url(#" + clip + ")");

        backgroundStore.appendChild(clipPath);

        backgroundStore.appendChild(wrap);
        backgroundStore.appendChild(gBorder);

        // pokud je na element aplikovana transformace, vygeneruje se prislusny transformacni paremtr
        Transform t = new Transform();
        String tm = t.createTransform(eb);
        if (!tm.equals("")) {
            backgroundStore.setAttributeNS(null, "transform", tm);
        }

        // pokud je na element aplikovana pruhlednost, vygeneruje se prislusny parametr do elementu
        String opacity = eb.getStylePropertyValue("opacity");
        if (opacity != "") {
            backgroundStore.setAttributeNS(null, "opacity", opacity);
        }

        getCurrentElem().appendChild(backgroundStore);
    }

    public void renderTextContent(TextBox text) {
        Rectangle b = text.getAbsoluteBounds();
        VisualContext ctx = text.getVisualContext();

        // priprava stylovacich parametru
        String style = "font-size:" + ctx.getFontSize() + "pt;"
                + "font-weight:" + (ctx.getFont().isBold() ? "bold" : "normal") + ";"
                + "font-style:" + (ctx.getFont().isItalic() ? "italic" : "normal") + ";"
                + "font-family:" + ctx.getFont().getFamily() + ";"
                + "fill:" + colorString(ctx.getColor()) + ";"
                + "stroke:none";

        if (!ctx.getTextDecoration().isEmpty()) {
            style += ";text-decoration:" + ctx.getTextDecorationString();
        }
        // generovani SVG elementu
        Element txt = doc.createElementNS(svgNS, "text");
        txt.setAttributeNS(null, "x", Integer.toString(b.x));
        txt.setAttributeNS(null, "y", Integer.toString((b.y + text.getBaselineOffset())));
        txt.setAttributeNS(null, "width", Integer.toString(b.width));
        txt.setAttributeNS(null, "height", Integer.toString(b.height));
        txt.setAttributeNS(null, "style", style);
        txt.setTextContent(text.getText());
        getCurrentElem().appendChild(txt);
    }

    public void renderReplacedContent(ReplacedBox box) {
        ReplacedContent cont = box.getContentObj();
        if (cont != null) {
            if (cont instanceof ReplacedImage) {
                BufferedImage img = ((ReplacedImage) cont).getBufferedImage();
                if (img != null) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try {
                        ImageIO.write(img, "png", os);
                    } catch (IOException e) {
                        out.println("<!-- I/O error: " + e.getMessage() + " -->");
                    }
                    char[] data = Base64Coder.encode(os.toByteArray());
                    String imgdata = "data:image/png;base64," + new String(data);
                    Rectangle cb = ((Box) box).getAbsoluteContentBounds();

                    Element image = doc.createElementNS(svgNS, "image");
                    image.setAttributeNS(null, "x", Integer.toString(cb.x));
                    image.setAttributeNS(null, "y", Integer.toString(cb.y));
                    image.setAttributeNS(null, "width", Integer.toString(cb.width));
                    image.setAttributeNS(null, "height", Integer.toString(cb.height));
                    image.setAttributeNS(null, "xlink:href", imgdata);
                    getCurrentElem().appendChild(image);
                }
            } else if (cont instanceof ReplacedText) {//HTML objekty
                Rectangle cb = ((Box) box).getClippedBounds();
                String clip = "cssbox-clip-" + idcounter;

                Element clipPath = doc.createElementNS(svgNS, "clipPath");
                clipPath.setAttributeNS(null, "id", clip);
                clipPath.appendChild(createRect(cb.x, cb.y, cb.width, cb.height, ""));
                getCurrentElem().appendChild(clipPath);

                Element g = doc.createElementNS(svgNS, "g");
                g.setAttributeNS(null, "id", "cssbox-obj-" + (idcounter++));
                g.setAttributeNS(null, "clip-path", "url(#" + clip + ")");
                getCurrentElem().appendChild(g);

            }
        }
    }

    public void close() {
        writeFooter();
    }

    private String colorString(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     *
     * @param border
     * @return
     */
    private Element getClipPathElementForBorder(Border border) {
        Element q;
        CornerRadius crTopLeft = border.getRadius(2);
        CornerRadius crTopRight = border.getRadius(1);
        CornerRadius crBottomLeft = border.getRadius(4);
        CornerRadius crBottomRight = border.getRadius(3);

        // postupne dokola kolem celeho elementu, pomoci SVG path prikazu elipticky oblouk a linka
        String path = "M " + (crTopLeft.d.x) + " " + (crTopLeft.d.y) + " ";

        path += " L " + crTopRight.b.x + " " + (crTopRight.b.y) + " ";

        if (crTopRight.isDrawn) {
            path += " A " + (crTopRight.x - border.border.right) + " "
                    + (crTopRight.y - border.border.top) + " 0 0 1 "
                    + Math.round(crTopRight.d.x) + " " + Math.round(crTopRight.d.y);
        } else {
            path += " L " + crTopRight.g.x + " " + crTopRight.g.y;
            path += " L " + crTopRight.d.x + " " + crTopRight.d.y;

        }

        path += " L " + crBottomRight.b.x + " " + (crBottomRight.b.y) + " ";

        if (crBottomRight.isDrawn) {
            path += " A " + (crBottomRight.x - border.border.right) + " "
                    + (crBottomRight.y - border.border.bottom) + " 0 0 1 "
                    + Math.round(crBottomRight.d.x) + " " + Math.round(crBottomRight.d.y);
        } else {
            path += " L " + crBottomRight.g.x + " " + crBottomRight.g.y;
            path += " L " + crBottomRight.d.x + " " + crBottomRight.d.y;
        }
        path += " L " + crBottomLeft.b.x + " " + (crBottomLeft.b.y) + " ";
        if (crBottomLeft.isDrawn) {
            path += " A " + (crBottomLeft.x - border.border.left) + " "
                    + (crBottomLeft.y - border.border.bottom) + " 0 0 1 "
                    + Math.round(crBottomLeft.d.x) + " " + Math.round(crBottomLeft.d.y);
        } else {
            path += " L " + crBottomLeft.g.x + " " + crBottomLeft.g.y;
            path += " L " + crBottomLeft.d.x + " " + crBottomLeft.d.y;
        }

        path += " L " + crTopLeft.b.x + " " + (crTopLeft.b.y) + " ";
        if (crTopLeft.isDrawn) {
            path += " A " + (crTopLeft.x - border.border.left) + " "
                    + (crTopLeft.y - border.border.top) + " 0 0 1 "
                    + Math.round(crTopLeft.d.x) + " " + Math.round(crTopLeft.d.y);
        } else {
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
     */
    private void writeBorders(ElementBox eb, Border b) {
        LengthSet borders = b.border;

        // vygenerovani rovnych casti ramecku
        writeBorderSVG(eb, b.topLeftH, b.topRightH, "top", borders.top);
        writeBorderSVG(eb, b.topRightV, b.bottomRightV, "right", borders.right);
        writeBorderSVG(eb, b.bottomLeftH, b.bottomRightH, "bottom", borders.bottom);
        writeBorderSVG(eb, b.topLeftV, b.bottomLeftV, "left", borders.left);

        // vygenerovani jednotlivych rohu
        writeBorderCorner(b, 1);
        writeBorderCorner(b, 2);
        writeBorderCorner(b, 3);
        writeBorderCorner(b, 4);
    }

    /**
     *
     * @param border
     * @param s
     */
    private void writeBorderCorner(Border border, int s) {
        int rady, radx;
        CornerRadius cr = border.getRadius(s);
        radx = cr.x;
        rady = cr.y;
        // if (radx != 0 || rady != 0) {

        TermColor ccc1;
        TermColor ccc2;
        int widthHor, widthVer;

        // podle toho, ktery roh je vykreslovan ziskame sirky ramecku a barvy v prislusnych smerech 
        if (s == 1) { // top-right
            widthHor = border.border.right;
            widthVer = border.border.top;

            ccc1 = border.colorRight;
            ccc2 = border.colorTop;
        } else if (s == 2) { // topleft
            widthHor = border.border.left;
            widthVer = border.border.top;
            ccc1 = border.colorTop;
            ccc2 = border.colorLeft;
        } else if (s == 3) { // bottomright
            widthHor = border.border.right;
            widthVer = border.border.bottom;

            ccc1 = border.colorBottom;
            ccc2 = border.colorRight;
        } else { // bottomleft
            widthHor = border.border.left;
            widthVer = border.border.bottom;

            ccc1 = border.colorLeft;
            ccc2 = border.colorBottom;
        }
        String cString1;
        String cString2;
        if (ccc1 != null) {
            cString1 = colorString(ccc1.getValue());
        } else {
            cString1 = "none";
        }
        if (ccc2 != null) {
            cString2 = colorString(ccc2.getValue());
        } else {
            cString2 = "none";
        }

        Element q;
        String path1 = "", path2 = "";
        // vygenerovani prislusnych SVG atributu pro element path

        path1 = cr.getPathRadiusC(widthVer, widthHor);
        path2 = cr.getPathRadiusA(widthVer, widthHor);

        if (widthVer > rady || widthHor > radx) {
            cr.isDrawn = false;
        }

        // vzgenerovani SVG elementu path, ktere tvori zaobleny ramecek
        q = createPath(path1, cString1, "none", 1);
        getCurrentElem().appendChild(q);

        q = createPath(path2, cString2, "none", 1);
        getCurrentElem().appendChild(q);

    }

    /**
     *
     * @param eb
     * @param a
     * @param b
     * @param side
     * @param width
     */
    private void writeBorderSVG(ElementBox eb, Point a, Point b, String side, int width) {

        CSSProperty.BorderColor bclr = eb.getStyle().getProperty("border-" + side + "-color");
        TermColor tclr = eb.getStyle().getValue(TermColor.class, "border-" + side + "-color");
        CSSProperty.BorderStyle bst = eb.getStyle().getProperty("border-" + side + "-style");
        if (bst != CSSProperty.BorderStyle.HIDDEN && bclr != CSSProperty.BorderColor.TRANSPARENT) {
            Color clr = null;
            if (tclr != null) {
                clr = tclr.getValue();
            }
            if (clr == null) {
                clr = eb.getVisualContext().getColor();
                if (clr == null) {
                    clr = Color.BLACK;
                }
            }
            String coords = "";
            switch (side) {
                case "left":
                    coords = "M " + a.x + "," + a.y + " L " + b.x + "," + b.y
                            + " L " + (b.x + width) + "," + b.y + " L " + (a.x + width) + "," + a.y;

                    break;
                case "top":
                    coords = "M " + a.x + "," + a.y + " L " + b.x + "," + b.y
                            + " L " + (b.x) + "," + (b.y + width) + " L " + a.x + "," + (a.y + width);

                    break;
                case "right":
                    coords = "M " + a.x + "," + a.y + " L " + b.x + "," + b.y
                            + " L " + (b.x - width) + "," + b.y + " L " + (a.x - width) + "," + a.y;

                    break;
                case "bottom":
                    coords = "M " + a.x + "," + a.y + " L " + b.x + "," + b.y
                            + " L " + (b.x) + "," + (b.y - width) + " L " + a.x + "," + (a.y - width);

                    break;
            }

            Element path = createPath(coords, colorString(clr), colorString(clr), 0);
            getCurrentElem().appendChild(path);
        }
    }

    /**
     *
     * @param eb
     */
    private void simulateRadialGradient(ElementBox eb) {
        Rectangle bb = eb.getAbsoluteBorderBounds();
        // rozmery elementu, pro ktery gradient vykreslujeme
        int ix = bb.x + eb.getBorder().left;
        int iy = bb.y + eb.getBorder().top;
        int iw = bb.width - eb.getBorder().right - eb.getBorder().left;
        int ih = bb.height - eb.getBorder().bottom - eb.getBorder().top;

        RadialGradient grad = new RadialGradient(eb.getClippedContentBounds());
        grad.data = new ArrayList<GradientStop>();
        grad.data.add(new GradientStop(Color.decode("#A9CDC3"), 0));
        grad.data.add(new GradientStop(Color.decode("#665178"), 20));
        grad.data.add(new GradientStop(Color.decode("#ff0000"), 80));
        grad.data.add(new GradientStop(Color.decode("#A9CDC3"), 100));
        grad.isCircle = true;

        // nastaveni hodnot simulovaneho gradientu podle CSS
//        grad.setCircleData(100, 20, 20);
        //grad.setCircleDataPercentRadLengths(RadialGradient.radLengths.FARTHEST_SIDE, 40, 40);
        // grad.setEllipseDataPercent(70, 20, 40, 40);
        grad.setEllipseDataRadLengths(RadialGradient.radLengths.FARTHEST_CORNER, 40, 80);

        String url = "cssbox-gradient-" + idcounter;
        idcounter++;
        Element defs = createElement("defs");
        Element image;
        image = createElement("radialGradient");

        // vygenerovani SVG elementu pro gradient, vcetne jednotlivych gradientovych zastavek
        image.setAttributeNS(null, "r", "" + Double.toString(grad.r) + "%");
        image.setAttributeNS(null, "cx", "" + Double.toString(grad.cx) + "%");
        image.setAttributeNS(null, "cy", "" + Double.toString(grad.cy) + "%");
        image.setAttributeNS(null, "fx", "" + Double.toString(grad.fx) + "%");
        image.setAttributeNS(null, "fy", "" + Double.toString(grad.fy) + "%");
        image.setAttributeNS(null, "id", url);
        for (int i = 0; i < grad.data.size(); i++) {
            Element stop = createElement("stop");
            Color cc = grad.data.get(i).c;
            stop.setAttributeNS(null, "offset", "" + grad.data.get(i).i + "%");
            stop.setAttributeNS(null, "style", "stop-color:rgb(" + cc.getRed() + "," + cc.getGreen() + "," + cc.getBlue() + ");stop-opacity:1");
            image.appendChild(stop);
        }

        defs.appendChild(image);
        getCurrentElem().appendChild(defs);
        String style = "stroke:none;fill-opacity:1;fill:url(#" + url + ");";
        String clip = "cssbox-clip-" + idcounter;
        Element clipPath = createElement("clipPath");
        clipPath.setAttributeNS(null, "id", clip);

        clipPath.appendChild(createRect(ix, iy, iw, ih, ""));
        getCurrentElem().appendChild(clipPath);

        // vygenerovani elementu, na ktery bude gradient aplikovan
        // navic je zde vytvoren i orezovy element clippath, ktery orizne element s gradientem na velikost puvodniho elementu
        if (grad.isCircle) {
            int max = Math.max(iw, ih);
            double x = (max == iw ? ix : ix - ((max - iw) * grad.cx / 100));
            double y = (max == ih ? iy : iy - ((max - ih) * grad.cy / 100));
            Element e = createRect(x, y, max, max, style);
            e.setAttributeNS(null, "clip-path", "url(#" + clip + ")");
            getCurrentElem().appendChild(e);
        } else {
            //   int max = Math.max(iw, ih);
            double x = (grad.newWidth == iw ? ix : ix - ((grad.newWidth - iw) * grad.cx / 100));
            double y = (grad.newHeight == ih ? iy : iy - ((grad.newHeight - ih) * grad.cy / 100));

            Element e = createRect(x, y, grad.newWidth, grad.newHeight, style);
            e.setAttributeNS(null, "clip-path", "url(#" + clip + ")");
            getCurrentElem().appendChild(e);
            //getCurrentElem().appendChild(createRect(ix, y, iw, grad.newHeight, style));
        }

    }

    /**
     * metoda pro simulaci linearniho gradinetu
     *
     * @param eb
     * @param angle
     */
    private void simulateLinearGradient(ElementBox eb, int angle) {
        Rectangle bb = eb.getAbsoluteBorderBounds();
        // ziskani rozmeru elementu 
        int ix = bb.x + eb.getBorder().left;
        int iy = bb.y + eb.getBorder().top;
        int iw = bb.width - eb.getBorder().right - eb.getBorder().left;
        int ih = bb.height - eb.getBorder().bottom - eb.getBorder().top;

        // vygenerovani simulovaneho gradientu, tak jak je zadan v CSS
        LinearGradient grad = new LinearGradient();
        grad.data = new ArrayList<GradientStop>();
        grad.data.add(new GradientStop(Color.decode("#A9CDC3"), 0));
        grad.data.add(new GradientStop(Color.decode("#665178"), 20));
        grad.data.add(new GradientStop(Color.decode("#ff0000"), 80));
        grad.data.add(new GradientStop(Color.decode("#A9CDC3"), 100));
        grad.setAngleDeg(angle, iw, ih);
        //grad.isLinear = false;
        String url = "cssbox-gradient-" + idcounter;
        idcounter++;
        // vygenerovani SVG gradientu, vcetne barevnych zastavek
        Element defs = createElement("defs");
        Element image;
        image = createElement("linearGradient");
        image.setAttributeNS(null, "x1", "" + Double.toString(grad.x1) + "%");
        image.setAttributeNS(null, "y1", "" + Double.toString(grad.y1) + "%");
        image.setAttributeNS(null, "x2", "" + Double.toString(grad.x2) + "%");
        image.setAttributeNS(null, "y2", "" + Double.toString(grad.y2) + "%");
        image.setAttributeNS(null, "id", url);
        for (int i = 0; i < grad.data.size(); i++) {
            Element stop = createElement("stop");
            Color cc = grad.data.get(i).c;
            stop.setAttributeNS(null, "offset", "" + grad.data.get(i).i + "%");
            stop.setAttributeNS(null, "style", "stop-color:rgb(" + cc.getRed() + "," + cc.getGreen() + "," + cc.getBlue() + ");stop-opacity:1");
            image.appendChild(stop);
        }

        defs.appendChild(image);
        getCurrentElem().appendChild(defs);
        
        String style = "stroke:none;fill-opacity:1;fill:url(#" + url + ");";

        // vyggenerovani SVG elementu s gradientem jako pozadi
        getCurrentElem().appendChild(createRect(ix, iy, iw, ih, style));
    }

    private void addText(String text, int x, int y, String color) {
        addText(text, (double) x, (double) y, color);
    }

    private void addText(String text, double x, double y, String color) {
        Element textElem;
        String style = "font-size: 18pt;"
                + "font-weight:normal;"
                + "font-variant: normal ;"
                + "font-family: Arial;"
                + "fill:" + color + ";"
                + "stroke:none";

        textElem = doc.createElementNS(svgNS, "text");
        textElem.setAttributeNS(null, "x", Double.toString(x));
        textElem.setAttributeNS(null, "y", Double.toString(y));
        textElem.setAttributeNS(null, "style", style);
        textElem.setTextContent(text);
        getCurrentElem().appendChild(textElem);
    }

    private void addText(String text, Point a, String color) {
        addText(text, (double) a.x, (double) a.y, color);
    }

    private void addText(String text, DPoint a, String color) {
        addText(text, (double) a.x, (double) a.y, color);
    }

    public Element createPath(String dPath, String fill, String stroke, int strokeWidth) {
        Element e = createElement("path");
        e.setAttributeNS(null, "d", dPath);
        e.setAttributeNS(null, "stroke", stroke);
        e.setAttributeNS(null, "stroke-width", Integer.toString(strokeWidth));
        e.setAttributeNS(null, "fill", fill);
        return e;
    }

    public Element createRect(double x, double y, double width, double height, String style) {
        Element e = createElement("rect");
        e.setAttributeNS(null, "x", Double.toString(x));
        e.setAttributeNS(null, "y", Double.toString(y));
        e.setAttributeNS(null, "width", Double.toString(width));
        e.setAttributeNS(null, "height", Double.toString(height));
        e.setAttributeNS(null, "style", style);
        return e;
    }

    public Element createRect(int x, int y, int width, int height, String style) {
        Element e = createElement("rect");
        e.setAttributeNS(null, "x", Integer.toString(x));
        e.setAttributeNS(null, "y", Integer.toString(y));
        e.setAttributeNS(null, "width", Integer.toString(width));
        e.setAttributeNS(null, "height", Integer.toString(height));
        e.setAttributeNS(null, "style", style);
        return e;
    }

    public Element createImage(int x, int y, int width, int height, String imgData) {
        Element image = createElement("image");
        //text.setAttributeNS(null, "id", );
        image.setAttributeNS(null, "x", Integer.toString(x));
        image.setAttributeNS(null, "y", Integer.toString(y));
        image.setAttributeNS(null, "width", Integer.toString(width));
        image.setAttributeNS(null, "height", Integer.toString(height));
        image.setAttributeNS(null, "xlink:href", imgData);
        return image;
    }

    public Element createElement(String elementName) {
        Element e = doc.createElementNS(svgNS, elementName);
        return e;
    }

}
