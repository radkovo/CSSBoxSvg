/**
 * BoxBrowser.java
 * Copyright (c) 2005-2014 Radek Burget
 *
 * CSSBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CSSBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with CSSBox. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.fit.cssbox.svg.demo;

import javax.swing.*;

import java.net.URL;

import org.fit.cssbox.css.CSSNorm;
import org.fit.cssbox.css.DOMAnalyzer;
import org.fit.cssbox.io.DOMSource;
import org.fit.cssbox.io.DefaultDOMSource;
import org.fit.cssbox.io.DefaultDocumentSource;
import org.fit.cssbox.io.DocumentSource;
import org.fit.cssbox.layout.BrowserCanvas;
import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.BrowserConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.awt.GridBagConstraints;
import java.awt.GridLayout;

import javax.swing.tree.DefaultMutableTreeNode;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.FlowLayout;

import cz.vutbr.web.css.MediaSpec;
import java.awt.Font;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.batik.swing.JSVGCanvas;
import org.fit.cssbox.layout.Viewport;
import org.fit.cssbox.svg.render.SVGDOMRenderer;

/**
 * demo aplikace pouziva pro vykresleni svg kodu komponentu JSVGCanvas z balicku apache batik. 
 * Tato komponenta bohuzel neumi vykreslovat obrazky zadane pomoci kodovani base64, 
 * takze na vetsine webovych stranek skonci chybou renderovani obrazku
 * @author burgetr 
 * @author Martin Safar
 */
public class BoxBrowser {

    protected DefaultMutableTreeNode root;
    protected DefaultMutableTreeNode domRoot;
    protected BrowserConfig config;
    public static BoxBrowser browser;

    protected JFrame mainWindow = null;  //  @jve:decl-index=0:visual-constraint="67,17"
    protected JPanel mainPanel = null;
    protected JPanel urlPanel = null;
    protected JPanel contentPanel = null;
    protected JPanel structurePanel = null;
    protected JPanel statusPanel = null;
    protected JTextField statusText = null;
    protected JLabel jLabel = null;
    protected JTextField urlText = null;
    protected JButton okButton = null;
    protected JScrollPane contentScroll = null;
    protected JPanel contentCanvas = null;
    protected JToolBar showToolBar = null;
    protected JButton redrawButton = null;
    protected JPanel toolPanel = null;
    protected JScrollPane boxScroll = null;
    // protected JTree boxTree = null;
    protected JTabbedPane treeTabs = null;
    protected JPanel DOMPanel = null;
    protected JScrollPane domScroll = null;
    // protected JTree domTree = null;

    protected JSVGCanvas svgCanvas = null;

    private String mediaType = "screen";
    private Dimension windowSize;
    private boolean cropWindow = false;
    private boolean loadImages = true;
    private boolean loadBackgroundImages = true;

    public BoxBrowser() {
        this.config = new BrowserConfig();
        svgCanvas = new JSVGCanvas();

    }

    public BrowserConfig getConfig() {
        return config;
    }

    /**
     * Reads the document, creates the layout and displays it
     *
     * @param urlstring The URL of the document to display.
     * @return The final URL of the displayed document or <code>null</code> when
     * the document couldn't be displayed.
     */
    public URL displayURL(String urlstring) {
        try {
            if (!urlstring.startsWith("http:")
                    && !urlstring.startsWith("https:")
                    && !urlstring.startsWith("ftp:")
                    && !urlstring.startsWith("file:")) {
                urlstring = "http://" + urlstring;
            }

            DocumentSource docSource = new DefaultDocumentSource(urlstring);
            urlText.setText(docSource.getURL().toString());

            DOMSource parser = new DefaultDOMSource(docSource);
            Document doc = parser.parse();
            String encoding = parser.getCharset();

            MediaSpec media = new MediaSpec("screen");
            updateCurrentMedia(media);

            DOMAnalyzer da = new DOMAnalyzer(doc, docSource.getURL());
            if (encoding == null) {
                encoding = da.getCharacterEncoding();
            }
            da.setDefaultEncoding(encoding);
            da.setMediaSpec(media);
            da.attributesToStyles();
            da.addStyleSheet(null, CSSNorm.stdStyleSheet(), DOMAnalyzer.Origin.AGENT);
            da.addStyleSheet(null, CSSNorm.userStyleSheet(), DOMAnalyzer.Origin.AGENT);
            da.addStyleSheet(null, CSSNorm.formsStyleSheet(), DOMAnalyzer.Origin.AGENT);
            da.getStyleSheets();

            contentCanvas = new BrowserCanvas(da.getRoot(), da, docSource.getURL());
            ((BrowserCanvas) contentCanvas).setConfig(config);
            ((BrowserCanvas) contentCanvas).createLayout(contentScroll.getSize(), contentScroll.getVisibleRect());

            docSource.close();

            //contentScroll.setViewportView(contentCanvas);
            //box tree
            Viewport viewport = ((BrowserCanvas) contentCanvas).getViewport();

            //Writer w = new OutputStreamWriter(os, "utf-8");
            //obtain the viewport bounds depending on whether we are clipping to viewport size or using the whole page
            int width = viewport.getClippedContentBounds().width;
            int height = viewport.getClippedContentBounds().height;

            SVGDOMRenderer render = new SVGDOMRenderer(width, height);
            viewport.draw(render);
            render.close();

            contentScroll.setViewportView(svgCanvas);
            svgCanvas.setDocument(render.getDocument());
//svgCanvas.setURI("file:///c:/work/outSVG.svg");

            //         root = createBoxTree(viewport);
            //        boxTree.setModel(new DefaultTreeModel(root));
            //dom tree
            domRoot = createDomTree(doc);
            //        domTree.setModel(new DefaultTreeModel(domRoot));

            //=============================================================================
            return docSource.getURL();

        } catch (Exception e) {
            System.err.println("*** Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public URL displayURL2(String urlstring) {
        try {
            if (!urlstring.startsWith("http:")
                    && !urlstring.startsWith("https:")
                    && !urlstring.startsWith("ftp:")
                    && !urlstring.startsWith("file:")) {
                urlstring = "http://" + urlstring;
            }

            //Open the network connection 
            DocumentSource docSource = new DefaultDocumentSource(urlstring);

            //Parse the input document
            DOMSource parser = new DefaultDOMSource(docSource);
            Document doc = parser.parse();

            //create the media specification
            MediaSpec media = new MediaSpec(mediaType);
            media.setDimensions(windowSize.width, windowSize.height);
            media.setDeviceDimensions(windowSize.width, windowSize.height);

            //Create the CSS analyzer
            DOMAnalyzer da = new DOMAnalyzer(doc, docSource.getURL());
            da.setMediaSpec(media);
            da.attributesToStyles(); //convert the HTML presentation attributes to inline styles
            da.addStyleSheet(null, CSSNorm.stdStyleSheet(), DOMAnalyzer.Origin.AGENT); //use the standard style sheet
            da.addStyleSheet(null, CSSNorm.userStyleSheet(), DOMAnalyzer.Origin.AGENT); //use the additional style sheet
            da.addStyleSheet(null, CSSNorm.formsStyleSheet(), DOMAnalyzer.Origin.AGENT); //render form fields using css
            da.getStyleSheets(); //load the author style sheets

            BrowserCanvas contentCanvas = new BrowserCanvas(da.getRoot(), da, docSource.getURL());
            contentCanvas.setAutoMediaUpdate(false); //we have a correct media specification, do not update
            contentCanvas.getConfig().setClipViewport(cropWindow);
            contentCanvas.getConfig().setLoadImages(loadImages);
            contentCanvas.getConfig().setLoadBackgroundImages(loadBackgroundImages);
            setDefaultFonts(contentCanvas.getConfig());
            contentCanvas.createLayout(windowSize);

            FileOutputStream os;
            os = new FileOutputStream("file:///c:/work/outBox.svg");

            Writer w = new OutputStreamWriter(os, "utf-8");

            //obtain the viewport bounds depending on whether we are clipping to viewport size or using the whole page
            int width = contentCanvas.getViewport().getClippedContentBounds().width;
            int height = contentCanvas.getViewport().getClippedContentBounds().height;

            SVGDOMRenderer render = new SVGDOMRenderer(width, height, w);
            contentCanvas.getViewport().draw(render);
            render.close();

            w.close();
            os.close();
            //=============================================================================
            return docSource.getURL();

        } catch (Exception e) {
            System.err.println("*** Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Sets some common fonts as the defaults for generic font families.
     */
    protected void setDefaultFonts(BrowserConfig config) {
        config.setDefaultFont(Font.SERIF, "Times New Roman");
        config.setDefaultFont(Font.SANS_SERIF, "Arial");
        config.setDefaultFont(Font.MONOSPACED, "Courier New");
    }

    /**
     * Updates the given media specification according to the real screen
     * parametres (if they may be obtained).
     *
     * @param media The media specification to be updated.
     */
    protected void updateCurrentMedia(MediaSpec media) {
        Dimension size = getContentScroll().getViewport().getSize();
        Dimension deviceSize = Toolkit.getDefaultToolkit().getScreenSize();
        ColorModel colors = Toolkit.getDefaultToolkit().getColorModel();

        media.setDimensions(size.width, size.height);
        media.setDeviceDimensions(deviceSize.width, deviceSize.height);
        media.setColor(colors.getComponentSize()[0]);
        if (colors instanceof IndexColorModel) {
            media.setColorIndex(((IndexColorModel) colors).getMapSize());
        }
        media.setResolution(Toolkit.getDefaultToolkit().getScreenResolution());
    }

//    /**
//     * Recursively creates a tree from the box tree
//     */
//    protected DefaultMutableTreeNode createBoxTree(Box root) {
//        DefaultMutableTreeNode ret = new DefaultMutableTreeNode(root);
//        if (root instanceof ElementBox) {
//            ElementBox el = (ElementBox) root;
//            for (int i = el.getStartChild(); i < el.getEndChild(); i++) {
//                ret.add(createBoxTree(el.getSubBox(i)));
//            }
//        }
//        return ret;
//    }
    /**
     * Recursively creates a tree from the dom tree
     */
    protected DefaultMutableTreeNode createDomTree(Node root) {
        DefaultMutableTreeNode ret = new DefaultMutableTreeNode(root) {
            private static final long serialVersionUID = 1L;

            @Override
            public String toString() {
                Object o = getUserObject();
                if (o instanceof Element) {
                    Element el = (Element) getUserObject();
                    String ret = "<" + el.getNodeName();
                    NamedNodeMap attrs = el.getAttributes();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        ret += " " + attrs.item(i).getNodeName() + "=\"" + attrs.item(i).getNodeValue() + "\"";
                    }
                    ret += ">";
                    return ret;
                } else if (o instanceof Text) {
                    return ((Text) o).getNodeValue();
                } else {
                    return super.toString();
                }
            }
        };
        NodeList child = root.getChildNodes();
        for (int i = 0; i < child.getLength(); i++) {
            ret.add(createDomTree(child.item(i)));
        }
        return ret;
    }

    /**
     * Locates a box from its position
     */
    private DefaultMutableTreeNode locateBox(DefaultMutableTreeNode root, int x, int y) {
        DefaultMutableTreeNode found = null;
        Box box = (Box) root.getUserObject();
        Rectangle bounds = box.getAbsoluteBounds();
        if (bounds.contains(x, y)) {
            found = root;
        }

        //find if there is something smallest that fits among the child boxes
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode inside = locateBox((DefaultMutableTreeNode) root.getChildAt(i), x, y);
            if (inside != null) {
                if (found == null) {
                    found = inside;
                } else {
                    Box fbox = (Box) found.getUserObject();
                    Box ibox = (Box) inside.getUserObject();
                    if (ibox.getAbsoluteBounds().width * ibox.getAbsoluteBounds().height
                            < fbox.getAbsoluteBounds().width * fbox.getAbsoluteBounds().height) {
                        found = inside;
                    }
                }
            }
        }

        return found;
    }

    /**
     * Locates a DOM node in the DOM tree
     */
    private DefaultMutableTreeNode locateObjectInTree(DefaultMutableTreeNode root, Object obj) {
        if (root.getUserObject().equals(obj)) {
            return root;
        } else {
            for (int i = 0; i < root.getChildCount(); i++) {
                DefaultMutableTreeNode ret = locateObjectInTree((DefaultMutableTreeNode) root.getChildAt(i), obj);
                if (ret != null) {
                    return ret;
                }
            }
            return null;
        }
    }

    class StyleListItem {

        private String text;
        private String tooltip;

        public StyleListItem(String text, String tooltip) {
            this.text = text;
            this.tooltip = tooltip;
        }

        public String getToolTipText() {
            return tooltip;
        }

        public String toString() {
            return text;
        }
    }

    public BrowserCanvas getBrowserCanvas() {
        return (BrowserCanvas) contentCanvas;
    }

    //===========================================================================
    /**
     * This method initializes jFrame
     *
     * @return javax.swing.JFrame
     */
    public JFrame getMainWindow() {
        if (mainWindow == null) {
            mainWindow = new JFrame();
            mainWindow.setTitle("Box Browser");
            mainWindow.setVisible(true);
            mainWindow.setBounds(new Rectangle(0, 0, 583, 251));
            mainWindow.setContentPane(getMainPanel());
            mainWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent e) {
                    mainWindow.setVisible(false);
                    System.exit(0);
                }
            });
        }
        return mainWindow;
    }

    /**
     * This method initializes jContentPane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getMainPanel() {
        if (mainPanel == null) {
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.gridy = -1;
            gridBagConstraints2.anchor = GridBagConstraints.WEST;
            gridBagConstraints2.gridx = -1;
            GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
            gridBagConstraints11.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints11.weighty = 1.0;
            gridBagConstraints11.gridx = 0;
            gridBagConstraints11.weightx = 1.0;
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.gridx = 0;
            gridBagConstraints3.weightx = 1.0;
            gridBagConstraints3.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints3.gridwidth = 1;
            gridBagConstraints3.gridy = 3;
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.gridy = 1;
            mainPanel = new JPanel();
            mainPanel.setLayout(new GridBagLayout());
            mainPanel.add(getJPanel3(), gridBagConstraints2);
            mainPanel.add(getUrlPanel(), gridBagConstraints);
            mainPanel.add(getContentPanel(), gridBagConstraints11);
            mainPanel.add(getStatusPanel(), gridBagConstraints3);
        }
        return mainPanel;
    }

    /**
     * This method initializes jPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getUrlPanel() {
        if (urlPanel == null) {
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints1.gridy = 0;
            gridBagConstraints1.weightx = 1.0;
            gridBagConstraints1.gridx = 1;
            GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
            gridBagConstraints7.gridx = 3;
            gridBagConstraints7.insets = new java.awt.Insets(4, 0, 5, 7);
            gridBagConstraints7.gridy = 1;
            GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
            gridBagConstraints6.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints6.gridy = 1;
            gridBagConstraints6.weightx = 1.0;
            gridBagConstraints6.insets = new java.awt.Insets(0, 5, 0, 5);
            GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
            gridBagConstraints5.gridy = 1;
            gridBagConstraints5.anchor = java.awt.GridBagConstraints.CENTER;
            gridBagConstraints5.insets = new java.awt.Insets(0, 6, 0, 0);
            gridBagConstraints5.gridx = 0;
            jLabel = new JLabel();
            jLabel.setText("Location :");
            urlPanel = new JPanel();
            urlPanel.setLayout(new GridBagLayout());
            urlPanel.add(jLabel, gridBagConstraints5);
            urlPanel.add(getUrlText(), gridBagConstraints6);
            urlPanel.add(getOkButton(), gridBagConstraints7);
        }
        return urlPanel;
    }

    /**
     * This method initializes jPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getContentPanel() {
        if (contentPanel == null) {
            GridLayout gridLayout1 = new GridLayout();
            gridLayout1.setRows(1);
            contentPanel = new JPanel();
            contentPanel.setLayout(gridLayout1);
            contentPanel.add(getContentScroll(), null);
        }
        return contentPanel;
    }

    /**
     * This method initializes jPanel2
     *
     * @return javax.swing.JPanel
     */
    private JPanel getStatusPanel() {
        if (statusPanel == null) {
            GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            gridBagConstraints4.gridx = 0;
            gridBagConstraints4.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints4.weightx = 1.0;
            gridBagConstraints4.insets = new java.awt.Insets(0, 7, 0, 0);
            gridBagConstraints4.gridy = 2;
            statusPanel = new JPanel();
            statusPanel.setLayout(new GridBagLayout());
            statusPanel.add(getStatusText(), gridBagConstraints4);
        }
        return statusPanel;
    }

    /**
     * This method initializes jTextField
     *
     * @return javax.swing.JTextField
     */
    private JTextField getStatusText() {
        if (statusText == null) {
            statusText = new JTextField();
            statusText.setEditable(false);
            statusText.setText("Browser ready.");
        }
        return statusText;
    }

    /**
     * This method initializes jTextField
     *
     * @return javax.swing.JTextField
     */
    private JTextField getUrlText() {
        if (urlText == null) {
            urlText = new JTextField();
            urlText.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    displayURL(urlText.getText());
                }
            });
        }
        return urlText;
    }

    /**
     * This method initializes jButton
     *
     * @return javax.swing.JButton
     */
    private JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton();
            okButton.setText("Go!");
            okButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    displayURL(urlText.getText());
                }
            });
        }
        return okButton;
    }

    /**
     * This method initializes jScrollPane
     *
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getContentScroll() {
        if (contentScroll == null) {
            contentScroll = new JScrollPane();
            contentScroll.setViewportView(getContentCanvas());
            contentScroll.addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentResized(java.awt.event.ComponentEvent e) {
                    if (contentCanvas != null && contentCanvas instanceof BrowserCanvas) {
                        ((BrowserCanvas) contentCanvas).createLayout(contentScroll.getSize(), contentScroll.getViewport().getViewRect());
                        contentScroll.repaint();
                        //new box tree
                        //   root = createBoxTree(((BrowserCanvas) contentCanvas).getViewport());
                    }
                }
            });
        }
        return contentScroll;
    }

    /**
     * This method initializes jPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getContentCanvas() {
        if (contentCanvas == null) {
            contentCanvas = new JPanel();
        }
        return contentCanvas;
    }

    /**
     * This method initializes jToolBar
     *
     * @return javax.swing.JToolBar
     */
    private JToolBar getShowToolBar() {
        if (showToolBar == null) {
            showToolBar = new JToolBar();
            showToolBar.add(getRedrawButton());
        }
        return showToolBar;
    }

    /**
     * This method initializes jButton
     *
     * @return javax.swing.JButton
     */
    private JButton getRedrawButton() {
        if (redrawButton == null) {
            redrawButton = new JButton();
            redrawButton.setText("Clear");
            redrawButton.setMnemonic(KeyEvent.VK_UNDEFINED);
            redrawButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    ((BrowserCanvas) contentCanvas).redrawBoxes();
                    contentCanvas.repaint();
                }
            });
        }
        return redrawButton;
    }

    /**
     * This method initializes jPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getJPanel3() {
        if (toolPanel == null) {
            FlowLayout flowLayout = new FlowLayout();
            flowLayout.setAlignment(java.awt.FlowLayout.LEFT);
            toolPanel = new JPanel();
            toolPanel.setLayout(flowLayout);
            toolPanel.add(getShowToolBar(), null);
        }
        return toolPanel;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        browser = new BoxBrowser();
        JFrame main = browser.getMainWindow();
        main.setSize(1200, 600);
        main.setVisible(true);
    }
}
