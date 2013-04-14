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

    public Slide(String text, String fontName, int fontSize) {
        this.text = text;
        this.fontName = fontName;
        this.fontSize = fontSize;
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

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Slide slide = (Slide) o;

        if (fontSize != slide.fontSize) return false;
        if (!fontName.equals(slide.fontName)) return false;
        if (!text.equals(slide.text)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = text.hashCode();
        result = 31 * result + fontName.hashCode();
        result = 31 * result + fontSize;
        return result;
    }

    @Override
    public String toString() {
        return "Slide{" +
                "text='" + getText() + '\'' +
                ", fontName='" + getFontName() + '\'' +
                ", fontSize=" + getFontSize() +
                '}';
    }
}
