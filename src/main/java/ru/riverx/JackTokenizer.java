package ru.riverx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JackTokenizer {
    private static final String SYMBOLS = "{}()[].,;+-*/&|<>=~";
    private static final String[] _KEYWORDS = {"class", "constructor", "function", "method", "field", "static", "var", "int", "char", "boolean", "void", "true", "false", "null", "this", "let", "do", "if", "else", "while", "return"};
    private final List<String> KEYWORDS;
    private final List<Token> tokenList;
    private final String input;
    private final int length;
    private final int tokenLength;
    private int symbolCount;
    private int tokenCount;

    public JackTokenizer(String input) {
        this.input = input;
        this.length = input.length();
        this.symbolCount = 0;
        this.tokenCount = 0;
        this.KEYWORDS = Arrays.asList(_KEYWORDS);
        this.tokenList = tokenize();
        this.tokenLength = tokenList.size();
    }

    public List<String> getTokenListAsStringList() {
        List<String> tmp = new ArrayList<>();
        tmp.add("<tokens>");
        for (Token token : tokenList) {
            tmp.add(token.toXmlString());
        }
        tmp.add("</tokens>");
        return tmp;
    }

    public static String getSymbols() { return SYMBOLS; }

    public boolean hasNextToken() {
        return tokenCount < tokenLength;
    }

    public Token getNextToken() {
        return tokenList.get(tokenCount++);
    }

    public Token getSecondNextToken() { return tokenList.get(tokenCount); }

    private List<Token> tokenize() {
        List<Token> tokenList = new ArrayList<>();
        while (hasNext()) {
            char current = getNext();
            if ('\n' == current || '\t' == current || ' ' == current) continue; // Ignore space and \r\n
            if (Character.isDigit(current)) {
                tokenList.add(tokenizeDigit(current)); continue;
            }
            if (Character.isLetter(current)) {
                tokenList.add(tokenizeWord(current)); continue;
            }
            if ('"' == current) {
                tokenList.add(tokenizeString()); continue;
            }
            if ('/' == current && isComment()) {
                ignoreSpecialComments(); continue;
            }
            tokenList.add(tokenizeSymbol(current));
        }
        return tokenList;
    }

    private Token tokenizeDigit(char current) {
        StringBuilder buffer = new StringBuilder().append(current);
        while (hasNext()) {
            char next = getNext();
            if (Character.isDigit(next)) {
                buffer.append(next);
            } else {
                symbolCount--;
                break;
            }
        }
        return new Token(buffer.toString(), TokenType.integerConstant);
    }

    private Token tokenizeWord(char current) {
        StringBuilder buffer = new StringBuilder().append(current);
        while (hasNext()) {
            char next = getNext();
            if (Character.isSpaceChar(next) || SYMBOLS.indexOf(next) != -1) {
                break;
            } else if (Character.isLetterOrDigit(next) || '_' == next) {
                buffer.append(next);
            }
        }
        symbolCount--;

        if (KEYWORDS.contains(buffer.toString())) {
            return new Token(buffer.toString(), TokenType.keyword);
        } else {
            return new Token(buffer.toString(), TokenType.identifier);
        }
    }

    private Token tokenizeString() {
        StringBuilder buffer = new StringBuilder();
        while (hasNext()) {
            char next = getNext();
            if ('"' == next || (Character.isSpaceChar(next) && !Character.isWhitespace(next))) break;
            buffer.append(next);
        }
        return new Token(buffer.toString(), TokenType.stringConstant);
    }

    private Token tokenizeSymbol(char current) {
        int index = SYMBOLS.indexOf(current);
        if (index != -1) {
            return new Token(String.valueOf(current), TokenType.symbol);
        } else {
            throw new RuntimeException("Unsupported symbol: " + current);
        }
    }

    private boolean isComment() {
        boolean isComment = false;
        if (hasNext()) {
            char next = getNext();
            if ('/' == next || '*' == next) {
                isComment = true;
            }
            if (!Character.isSpaceChar(next) && !isComment) {
                throw new RuntimeException("Unexpected symbol: " + next);
            }
            symbolCount--; // Undo counter to the first char
            return isComment;
        }
        return false;
    }

    private void ignoreSpecialComments() {
        char current = ' ';
        if (hasNext()) {
            current = getNext();
        }
        switch (current) {
            case '/': {
                if (hasNext()) {
                    char next = getNext();
                    while (!"\n".equals(String.valueOf(next))) {
                        if (hasNext()) next = getNext();
                        else break;
                    }
                }
            } break;
            case '*': {
                while (hasNext()) {
                    char next = getNext();
                    if ('*' == next) {
                        char second = getNext();
                        if ('/' == second) break;
                    }
                }
                break;
            }
            default: break;
        }
    }

    private boolean hasNext() {
        return symbolCount < length;
    }

    private char getNext() {
        return input.charAt(symbolCount++);
    }
}
