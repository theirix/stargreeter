package ru.omniverse.android.stargreeter;

import android.graphics.Color;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

public class StarGreeterData {

    private final List<Slide> slides;
    private final int slideTime;
    private final boolean keepLastSlide;

    public StarGreeterData(Document document) {

        try {
            XPath xPath = XPathFactory.newInstance().newXPath();

            slides = new ArrayList<Slide>();
            // should be first
            Element slide = (Element) xPath.evaluate("/stargreeter/beginning",
                    document, XPathConstants.NODE);
            slides.add(parseSlide(xPath, slide));

            // add to the end
            NodeList nlSlides = (NodeList) xPath.evaluate("/stargreeter/slides/slide",
                    document, XPathConstants.NODESET);
            for (int i = 0; i < nlSlides.getLength(); i++) {
                slides.add(parseSlide(xPath, (Element) nlSlides.item(i)));
            }

            slideTime = Integer.parseInt((String) xPath.evaluate("/stargreeter/settings/slide-time",
                    document, XPathConstants.STRING));
            keepLastSlide = (Boolean) xPath.evaluate("/stargreeter/settings/keep-last-slide",
                    document, XPathConstants.BOOLEAN);

        } catch (XPathExpressionException e) {
            throw new RuntimeException("Can not parse settings", e);
        }
    }

    private Slide parseSlide(XPath xPath, Element slide) throws XPathExpressionException {
        String text = (String) xPath.evaluate("text", slide, XPathConstants.STRING);
        String fontName = (String) xPath.evaluate("font", slide, XPathConstants.STRING);
        int fontSize = Integer.parseInt((String) xPath.evaluate("font/@size", slide, XPathConstants.STRING));
        int fontColor = Color.parseColor((String) xPath.evaluate("font/@color", slide, XPathConstants.STRING));

        return new Slide(text, fontName, fontSize, fontColor);
    }

    public Slide getBeginning() {
        assert (!slides.isEmpty());
        return slides.get(0);
    }

    public List<Slide> getSlides() {
        return slides.subList(1, slides.size());
    }

    public List<Slide> getAllSlides() {
        return slides;
    }

    public int getSlideTime() {
        return slideTime;
    }

    public boolean isKeepLastSlide() {
        return keepLastSlide;
    }

    @Override
    public String toString() {
        return "StarGreeterData{" +
                "beginning=" + getBeginning() +
                ", slides=" + getSlides() +
                ", slideTime=" + getSlideTime() +
                ", keepLastSlide=" + isKeepLastSlide() +
                '}';
    }
}