package ru.omniverse.android.stargreeter;

/**
 * Created with IntelliJ IDEA.
 * User: irix
 * Date: 14.04.2013
 * Time: 23:57
 */

class Slide {
    private final String text;
    private final String fontName;
    private final int fontSize;
    private final int fontColor;

    public Slide(String text, String fontName, int fontSize, int fontColor) {
        this.text = text;
        this.fontName = fontName;
        this.fontSize = fontSize;
        this.fontColor = fontColor;
    }

    String getText() {
        return text;
    }

    String getFontName() {
        return fontName;
    }

    int getFontSize() {
        return fontSize;
    }

    int getFontColor() {
        return fontColor;
    }

    @Override
    public String toString() {
        return "Slide{" +
                "text='" + text + '\'' +
                ", fontName='" + fontName + '\'' +
                ", fontSize=" + fontSize +
                ", fontColor=" + fontColor +
                '}';
    }
}
