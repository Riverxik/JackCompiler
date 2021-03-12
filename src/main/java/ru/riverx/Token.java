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
    public String toXmlString() {
        return String.format("<%s> %s </%s>",type, value, type);
    }
}
