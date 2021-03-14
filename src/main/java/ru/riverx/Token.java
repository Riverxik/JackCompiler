package ru.riverx;

public class Token {
    private final String value;
    private final TokenType type;

    public Token(String value, TokenType type) {
        this.value = value;
        this.type = type;
    }

    public String getValue() { return value; }
    public TokenType getType() { return type; }
    public String toString() { return String.format("[%s][%s]", type, value); }
    public String toXmlString() {
        if (value.equals("<"))
            return String.format("<%s> &lt; </%s>",type, type);
        if (value.equals(">"))
            return String.format("<%s> &gt; </%s>",type, type);
        if (value.equals("&"))
            return String.format("<%s> &amp; </%s>",type, type);
        return String.format("<%s> %s </%s>",type, value, type);
    }
}
