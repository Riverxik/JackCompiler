package ru.riverx;

import java.util.ArrayList;
import java.util.List;

public class CompilationEngine {
    private final JackTokenizer tokenizer;
    private final List<String> tokensXml;
    private Token currentToken;

    public CompilationEngine(JackTokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.tokensXml = new ArrayList<>();
        parse();
    }

    public List<String> getTokensXml() { return tokensXml; }

    private void parse() {
        while (tokenizer.hasNextToken()) {
            currentToken = tokenizer.getNextToken();
            if (isGivenToken(TokenType.keyword, "class")) {
                compileClass();
            }
        }
    }

    private void compileClass() {
        tokensXml.add("<class>");
        checkToken(TokenType.keyword, "class");
        compileClassName();
        checkToken(TokenType.symbol, "{");
        compileClassVarDec();
        compileSubroutineDec();
        checkToken(TokenType.symbol, "}");
        tokensXml.add("</class>");
    }

    private void compileClassName() {
        checkIdentifier();
    }

    private void compileClassVarDec() {
        List<Token> tokenList = new ArrayList<>();
        tokenList.add(new Token("static", TokenType.keyword));
        tokenList.add(new Token("field", TokenType.keyword));
        if (checkTokenList(tokenList, false)) {
            tokensXml.add("<classVarDec>");
            compileType();
            compileVarName();
            checkToken(TokenType.symbol, ";");
            tokensXml.add("</classVarDec>");
            compileClassVarDec(); // For additional class var declarations
        }
    }

    private void compileType() {
        List<Token> tokenList = new ArrayList<>();
        tokenList.add(new Token("int", TokenType.keyword));
        tokenList.add(new Token("char", TokenType.keyword));
        tokenList.add(new Token("boolean", TokenType.keyword));
        if (!checkTokenList(tokenList, false)) {
            if (isGivenToken(TokenType.identifier, currentToken.getValue())) {
                compileClassName();
            } else {
                // If type token is not found.
                checkToken(TokenType.keyword, "int, char, boolean, 'customType'");
            }
        }
    }

    private boolean isTypeNext() {
        return isGivenToken(TokenType.keyword, "int") || isGivenToken(TokenType.keyword, "char")
                || isGivenToken(TokenType.keyword, "boolean") || isGivenToken(TokenType.identifier, currentToken.getValue());
    }

    private void compileVarName() {
        checkIdentifier();
        compileAddVarName();
    }

    private void compileTypeVarName() {
        compileType();
        checkIdentifier();
        compileAddTypeVarName();
    }

    private void compileAddVarName() {
        if (isGivenToken(TokenType.symbol, ",")) {
            checkToken(TokenType.symbol, ",");
            compileVarName();
        }
    }

    private void compileAddTypeVarName() {
        if (isGivenToken(TokenType.symbol, ",")) {
            checkToken(TokenType.symbol, ",");
            compileTypeVarName();
        }
    }

    private void compileSubroutineDec() {
        List<Token> tokenList = new ArrayList<>();
        tokenList.add(new Token("constructor", TokenType.keyword));
        tokenList.add(new Token("function", TokenType.keyword));
        tokenList.add(new Token("method", TokenType.keyword));
        if (checkTokenList(tokenList, false)) {
            tokensXml.add("<subroutineDec>");
            tokenList.clear();
            tokenList.add(new Token("void", TokenType.keyword));
            if (!checkTokenList(tokenList, false)) {
                compileType();
            }
            compileSubroutineName();
            checkToken(TokenType.symbol, "(");
            compileParameterList();
            checkToken(TokenType.symbol, ")");
            compileSubroutineBody();
            tokensXml.add("</subroutineDec>");
            compileSubroutineDec();
        }
    }

    private void compileSubroutineName() {
        checkIdentifier();
    }

    private void compileParameterList() {
        tokensXml.add("<parameterList>");
        if (isTypeNext()) {
            compileTypeVarName();
        }
        tokensXml.add("</parameterList>");
    }

    private void compileSubroutineBody() {
        tokensXml.add("<subroutineBody>");
        checkToken(TokenType.symbol, "{");
        compileVarDec();
        tokensXml.add("<statements>");
        compileStatements();
        tokensXml.add("</statements>");
        checkToken(TokenType.symbol, "}");
        tokensXml.add("</subroutineBody>");
    }

    private void compileVarDec() {
        if (isGivenToken(TokenType.keyword, "var")) {
            tokensXml.add("<varDec>");
            checkToken(TokenType.keyword, "var");
            compileType();
            compileVarName();
            checkToken(TokenType.symbol, ";");
            tokensXml.add("</varDec>");
            compileVarDec();
        }
    }

    private void compileStatements() {
        compileStatement();
    }

    private void compileStatement() {
        // Current statement.
        boolean hasStatement = false;
        if (isGivenToken(TokenType.keyword, "let")) {
            compileLetStatement();
            hasStatement = true;
        }
        else if (isGivenToken(TokenType.keyword, "if")) {
            compileIfStatement();
            hasStatement = true;
        }
        else if (isGivenToken(TokenType.keyword, "while")) {
            compileWhileStatement();
            hasStatement = true;
        }
        else if (isGivenToken(TokenType.keyword, "do")) {
            compileDoStatement();
            hasStatement = true;
        }
        else if (isGivenToken(TokenType.keyword, "return")) {
            compileReturnStatement();
            hasStatement = true;
        }
        if (hasStatement) {
            compileStatements(); // for another statements.
        }
    }

    private void compileLetStatement() {
        tokensXml.add("<letStatement>");
        checkToken(TokenType.keyword, "let");
        checkIdentifier();              //compileVarName() but without checking next.
        if (isGivenToken(TokenType.symbol, "[")) {
            compileArrayExpression();
        }
        checkToken(TokenType.symbol, "=");
        tokensXml.add("<expression>");
        compileExpression();
        tokensXml.add("</expression>");
        checkToken(TokenType.symbol, ";");
        tokensXml.add("</letStatement>");
    }

    private void compileArrayExpression() {
        checkToken(TokenType.symbol, "[");
        tokensXml.add("<expression>");
        compileExpression();
        tokensXml.add("</expression>");
        checkToken(TokenType.symbol, "]");
    }

    private void compileIfStatement() {
        tokensXml.add("<ifStatement>");
        checkToken(TokenType.keyword, "if");
        compileBracketExpression();
        compileBlockStatements();
        if (isGivenToken(TokenType.keyword, "else")) {
            checkToken(TokenType.keyword, "else");
            compileBlockStatements();
        }
        tokensXml.add("</ifStatement>");
    }

    private void compileBracketExpression() {
        checkToken(TokenType.symbol, "(");
        tokensXml.add("<expression>");
        compileExpression();
        tokensXml.add("</expression>");
        checkToken(TokenType.symbol, ")");
    }

    private void compileBlockStatements() {
        checkToken(TokenType.symbol, "{");
        tokensXml.add("<statements>");
        compileStatements();
        tokensXml.add("</statements>");
        checkToken(TokenType.symbol, "}");
    }

    private void compileWhileStatement() {
        tokensXml.add("<whileStatement>");
        checkToken(TokenType.keyword, "while");
        checkToken(TokenType.symbol, "(");
        tokensXml.add("<expression>");
        compileExpression();
        tokensXml.add("</expression>");
        checkToken(TokenType.symbol, ")");
        compileBlockStatements();
        tokensXml.add("</whileStatement>");
    }

    private void compileDoStatement() {
        tokensXml.add("<doStatement>");
        checkToken(TokenType.keyword, "do");
        compileSubroutineCall();
        checkToken(TokenType.symbol, ";");
        tokensXml.add("</doStatement>");
    }

    private void compileReturnStatement() {
        tokensXml.add("<returnStatement>");
        checkToken(TokenType.keyword, "return");
        if (!tryCompileExpression()) {
            tokensXml.remove(tokensXml.size() - 1);
            tokensXml.remove(tokensXml.size() - 1);
        }
        checkToken(TokenType.symbol, ";");
        tokensXml.add("</returnStatement>");
    }

    // I don't like the way it gives in the course, maybe i will change it later
    private void compileExpression() {
        //tokensXml.add("<expression>");
        compileTerm();
        if (isOpNext()) {
            compileOpTerm();
        }
        //tokensXml.add("</expression>");
    }

    private void compileOpTerm() {
        compileOp();
        compileExpression();
    }

    private void compileTerm() {
        tokensXml.add("<term>");
        switch (currentToken.getType()) {
            case integerConstant: checkToken(TokenType.integerConstant, currentToken.getValue()); break;
            case stringConstant: checkToken(TokenType.stringConstant, currentToken.getValue()); break;
            case keyword: compileKeyWordConstant(); break;
            case identifier: compileIdentifier(); break;
            case symbol: {
                if (currentToken.getValue().equals("(")) { compileBracketExpression(); }
                else if (isUnaryOp()) { compileUnaryTerm(); }
                else throw new RuntimeException("Unexpected token: " + currentToken.toString() + ", expected: term");
            } break;
            default: throw new RuntimeException("Unexpected token: " + currentToken.toString() + ", expected: term");
        }
        tokensXml.add("</term>");
    }

    private void compileIdentifier() {
        checkIdentifier();
        if (isGivenToken(TokenType.symbol, "[")) {
            compileArrayExpression();
        }
        if (isGivenToken(TokenType.symbol, "(")) {
            compileSubroutineCall();
        }
        if (isGivenToken(TokenType.symbol, ".")) {
            checkToken(TokenType.symbol, ".");
            compileSubroutineCall();
        }
    }

    private void compileSubroutineCall() {
        checkIdentifier(); // compileSubroutineName();
        if (isGivenToken(TokenType.symbol, ".")) {
            checkToken(TokenType.symbol, ".");
            checkIdentifier();
        }
        checkToken(TokenType.symbol, "(");
        tokensXml.add("<expressionList>");
        compileExpressionList();
        tokensXml.add("</expressionList>");
        checkToken(TokenType.symbol, ")");
    }

    private void compileExpressionList() {
        if (isGivenToken(TokenType.symbol, ")")) return;
        if (tryCompileExpression()) {
            if (isGivenToken(TokenType.symbol, ",")) {
                checkToken(TokenType.symbol, ",");
                compileExpressionList();
            }
        } else {
            // This case needs for cleaning xml from starting tags (<expression> <term>).
        }
    }

    private boolean tryCompileExpression() {
        try {
            tokensXml.add("<expression>");
            compileExpression();
            tokensXml.add("</expression>");
        } catch (RuntimeException e) {
            return false;
        }
        return true;
    }

    private void compileKeyWordConstant() {
        switch (currentToken.getValue()) {
            case "true": checkToken(TokenType.keyword, "true"); break;
            case "false": checkToken(TokenType.keyword, "false"); break;
            case "null": checkToken(TokenType.keyword, "null"); break;
            case "this": checkToken(TokenType.keyword, "this"); break;
            default: checkToken(TokenType.keyword, "true/false/null/this");
        }
    }

    private boolean isSubroutineNameNext() {
        return currentToken.getType() == TokenType.identifier
                && tokenizer.hasNextToken() && tokenizer.getSecondNextToken().getValue().equals("(");
    }

    private void compileUnaryTerm() {
        compileOp();
        compileTerm();
    }

    private boolean isUnaryOp() {
        return currentToken.getType() == TokenType.symbol
                && (currentToken.getValue().equals("-") || currentToken.getValue().equals("~"));
    }

    private boolean isOpNext() {
        //return currentToken.getType() == TokenType.symbol && JackTokenizer.getSymbols().contains(currentToken.getValue());
        if (currentToken.getType() == TokenType.symbol) {
            String opSymbols = "+-*/&|<>=";
            return opSymbols.contains(currentToken.getValue());
        }
        return false;
    }

    private void compileOp() {
        final String symbols = JackTokenizer.getSymbols();
        int index = symbols.indexOf(currentToken.getValue());
        if (currentToken.getType() == TokenType.symbol && index != -1) {
            checkToken(TokenType.symbol, String.valueOf(symbols.charAt(index)));
        } else {
            checkToken(TokenType.symbol, symbols);
        }
    }

    private void checkIdentifier() {
        checkToken(TokenType.identifier, currentToken.getValue());
    }

    private void checkToken(TokenType type, String value) {
        if (currentToken.getType() == type && currentToken.getValue().equals(value)) {
            advance();
        } else {
            Token tmp = new Token(value, type);
            throw new RuntimeException("Unexpected token: " + currentToken.toString() + ", expected: " + tmp.toString());
        }
    }

    /**
     * Checks currentToken equals to even one of the @tokenList.
     *
     * @param tokenList List of tokens to checks for equals.
     */
    private boolean checkTokenList(List<Token> tokenList, boolean isRequired) {
        boolean isFound = false;
        List<String> expected = new ArrayList<>();
        for (Token token : tokenList) {
            if (currentToken.getType() == token.getType() && currentToken.getValue().equals(token.getValue())) {
                advance();
                isFound = true;
                break;
            } else {
                expected.add(token.toString());
            }
        }
        if (!isFound && isRequired) {
            throw new RuntimeException("Unexpected token: " + currentToken.toString() + ", expected: " + expected.toString());
        }
        return isFound;
    }

    private boolean isGivenToken(TokenType type, String value) {
        return currentToken.getType() == type && currentToken.getValue().equals(value);
    }

    private void advance() {
        if (tokenizer.hasNextToken()) {
            tokensXml.add(currentToken.toXmlString());  // Adds to xml.
            currentToken = tokenizer.getNextToken();    // Gets the next.
        } else {
            tokensXml.add(currentToken.toXmlString());
            //throw new NullPointerException("Expecting a new token, but given a null");
        }
    }
}
