package ru.riverx;

import java.util.ArrayList;
import java.util.List;

public class CompilationEngine {
    private final JackTokenizer tokenizer;
    private final List<String> tokensXml;
    private Token currentToken;
    private SymbolTable symbolTable;
    private VMWriter writer;
    private String className;
    private int paramListIndex;
    private int labelCount;
    private int subIndex;
    private String anotherClass;
    private List<String> identifiers;

    public CompilationEngine(JackTokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.tokensXml = new ArrayList<>();
        this.writer = new VMWriter();
        this.labelCount = 0;
        this.identifiers = new ArrayList<>();
        parse();
    }

    public List<String> getTokensXml() { return tokensXml; }

    public List<String> getGeneratedVMCode() { return writer.vmCode; }

    private void parse() {
        while (tokenizer.hasNextToken()) {
            currentToken = tokenizer.getNextToken();
            if (isGivenToken(TokenType.keyword, "class")) {
                symbolTable = new SymbolTable();
                compileClass();
            }
        }
    }

    private void compileClass() {
        tokensXml.add("<class>");
        checkToken(TokenType.keyword, "class");
        className = currentToken.getValue();
        compileClassName();
        checkToken(TokenType.symbol, "{");
        compileClassVarDec();
        compileSubroutineDec();
        checkToken(TokenType.symbol, "}");
        symbolTable.resetKindCountClass();
        tokensXml.add("</class>");
    }

    private void compileClassName() {
        if (!Character.isUpperCase(className.charAt(0))) {
            throw new IllegalArgumentException("Class name should starts with the capital letter: " + className);
        }
        checkIdentifier();
    }

    private void compileClassVarDec() {
        String kind = currentToken.getValue();
        if (compileSpecialVarDec()) {
            String type = currentToken.getValue();
            compileType();
            //String name = currentToken.getValue();
            compileVarName();
            checkToken(TokenType.symbol, ";");
            tokensXml.add("</classVarDec>");
            for (String identifier : identifiers) {
                symbolTable.defineClass(identifier, type, kind);
            }
            identifiers.clear();
            compileClassVarDec(); // For additional class var declarations
        }
    }

    private boolean compileSpecialVarDec() {
        if (isGivenToken(TokenType.keyword, "static")) {
            tokensXml.add("<classVarDec>");
            checkToken(TokenType.keyword, "static");
            return true;
        } else if (isGivenToken(TokenType.keyword, "field")) {
            tokensXml.add("<classVarDec>");
            checkToken(TokenType.keyword, "field");
            return true;
        }
        return false;
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
        identifiers.add(currentToken.getValue());
        checkIdentifier();
        compileAddVarName();
    }

    private void compileTypeVarName() {
        String type = currentToken.getValue();
        compileType();
        String name = currentToken.getValue();
        checkIdentifier();
        symbolTable.defineSubroutine(name, type, "arg");
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
        subIndex = compileSpecialSubroutineDec();
        if (subIndex >= 0) {
            ArrayList<Token> tokenList = new ArrayList<>();
            tokenList.add(new Token("void", TokenType.keyword));
            if (!checkTokenList(tokenList, false)) {
                compileType();
            }
            String funcName = currentToken.getValue();
            compileSubroutineName();
            checkToken(TokenType.symbol, "(");
            compileParameterList();
            checkToken(TokenType.symbol, ")");

            compileSubroutineBody(funcName, subIndex);
            tokensXml.add("</subroutineDec>");
            symbolTable.resetKindCountSubroutine();
            compileSubroutineDec();
        }
    }

    private int compileSpecialSubroutineDec() {
        if (isGivenToken(TokenType.keyword, "constructor")) {
            tokensXml.add("<subroutineDec>");
            checkToken(TokenType.keyword, "constructor");
            return 0;
        } else if (isGivenToken(TokenType.keyword, "function")) {
            tokensXml.add("<subroutineDec>");
            checkToken(TokenType.keyword, "function");
            return 1;
        } else if (isGivenToken(TokenType.keyword, "method")) {
            tokensXml.add("<subroutineDec>");
            checkToken(TokenType.keyword, "method");
            symbolTable.defineSubroutine("this", className, "arg");
            return 2;
        }
        return -1;
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

    private void compileSubroutineBody(String funcName, int subIndex) {
        tokensXml.add("<subroutineBody>");
        checkToken(TokenType.symbol, "{");
        compileVarDec();
        writer.writeFunction(className+"."+funcName, Variable.localCount);
        if (subIndex == 0) { // This is constructor.
            writer.writePush(SymbolKind.constant, Variable.fieldCount);  // how much memory for instance
            writer.writeCall("Memory.alloc", 1);             // allocate new memory
            writer.writePop(SymbolKind.pointer, 0);                // anchor base address to this
        } else if (subIndex == 2) { // This is method.
            writer.writePush(SymbolKind.ARG, 0);                   // get base address from arg0
            writer.writePop(SymbolKind.pointer, 0);                // anchor it to this
        }
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
            String type = currentToken.getValue();
            compileType();
            //String name = currentToken.getValue();
            compileVarName();
            checkToken(TokenType.symbol, ";");
            tokensXml.add("</varDec>");
            for (String identifier : identifiers) {
                symbolTable.defineSubroutine(identifier, type, "var");
            }
            identifiers.clear();
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
        String name = currentToken.getValue();
        Variable var = symbolTable.findVariable(name);
        if (currentToken.getType() == TokenType.identifier && tokenizer.hasNextToken() &&
        tokenizer.getSecondNextToken().getValue().equals("[")) {
            compileIdentifier(); // array
        } else {
            checkIdentifier();  // if not array
            checkToken(TokenType.symbol, "=");
            tokensXml.add("<expression>");
            compileExpression();
            tokensXml.add("</expression>");
            writer.writePop(var.getKind(), var.getIndex());
        }
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
        int count = labelCount++;
        tokensXml.add("<ifStatement>");
        checkToken(TokenType.keyword, "if");
        compileBracketExpression();
        writer.writeArithmetic(ArithmeticCommand.NOT);
        writer.writeIf("if_L1_"+count);
        compileBlockStatements();
        writer.writeGoto("goto_L2_"+count);
        writer.writeLabel("if_L1_"+count);
        if (isGivenToken(TokenType.keyword, "else")) {
            checkToken(TokenType.keyword, "else");
            compileBlockStatements();
        }
        writer.writeLabel("goto_L2_"+count);
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
        int count = labelCount++;
        tokensXml.add("<whileStatement>");
        checkToken(TokenType.keyword, "while");
        checkToken(TokenType.symbol, "(");
        writer.writeLabel("while_L1_"+count);
        tokensXml.add("<expression>");
        compileExpression();
        tokensXml.add("</expression>");
        checkToken(TokenType.symbol, ")");
        writer.writeArithmetic(ArithmeticCommand.NOT);
        writer.writeIf("while_L2_"+count);
        compileBlockStatements();
        writer.writeGoto("while_L1_"+count);
        writer.writeLabel("while_L2_"+count);
        tokensXml.add("</whileStatement>");
    }

    private void compileDoStatement() {
        tokensXml.add("<doStatement>");
        checkToken(TokenType.keyword, "do");
        compileIdentifier();
        writer.writePop(SymbolKind.temp, 0); // We don't need to store result in 'do', so clear the stack.
        checkToken(TokenType.symbol, ";");
        tokensXml.add("</doStatement>");
    }

    private void compileReturnStatement() {
        tokensXml.add("<returnStatement>");
        checkToken(TokenType.keyword, "return");
        if (!tryCompileExpression()) {
            tokensXml.remove(tokensXml.size() - 1);
            tokensXml.remove(tokensXml.size() - 1);
            writer.writePush(SymbolKind.constant, 0); // if empty return we generate push 0 for it.
        }
        writer.writeReturn();
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
        // Get the op.
        final String symbols = JackTokenizer.getSymbols();
        int index = symbols.indexOf(currentToken.getValue());
        // Compile op.
        compileOp();
        // Compile next exp.
        compileExpression();
        // Write op after second expression.
        writeOp(symbols.charAt(index), false);
    }

    private void compileTerm() {
        tokensXml.add("<term>");
        switch (currentToken.getType()) {
            case integerConstant: {
                String numStr = currentToken.getValue();
                checkToken(TokenType.integerConstant, numStr);
                writer.writeConstant(numStr);
            } break;
            case stringConstant: {
                String stringConst = currentToken.getValue();
                checkToken(TokenType.stringConstant, stringConst);
                compileString(stringConst, stringConst.length());
            } break;
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

    private void compileString(String stringConst, int length) {
        writer.writePush(SymbolKind.constant, length);
        writer.writeCall("String.new", 1);
        for (char c : stringConst.toCharArray()) {
            writer.writePush(SymbolKind.constant, (int)c);
            writer.writeCall("String.appendChar", 2); // 0 is String base address, 1 is char
        }
    }

    private void compileIdentifier() {
        String name = currentToken.getValue();
        Variable var;
        try {
            var = symbolTable.findVariable(name);
        } catch (RuntimeException e) {
            // That means that we encounter an another class identifier, so we didn't have it in symbol-table.
            var = null;
            anotherClass = currentToken.getValue();
        }
        checkIdentifier();
        if (isGivenToken(TokenType.symbol, "[")) {        // arr'['exp] = exp2;
            writer.writePush(var.getKind(), var.getIndex());    // push arr
            compileArrayExpression();                           // [exp]
            writer.writeArithmetic(ArithmeticCommand.ADD);      // arr + [exp]
            if (isGivenToken(TokenType.symbol, "=")) {
                checkToken(TokenType.symbol, "=");            // =
                tokensXml.add("<expression>");
                compileExpression();                                // exp2
                tokensXml.add("</expression>");
                writer.writePop(SymbolKind.temp, 0);          // save exp2 to temp 0
                writer.writePop(SymbolKind.pointer, 1);       // pop pointer 1 (that is arr)
                writer.writePush(SymbolKind.temp, 0);         // temp 0 to stack
                writer.writePop(SymbolKind.that, 0);          // arr[exp] = temp 0 (exp2);
            } else {
                writer.writePop(SymbolKind.pointer, 1);
                writer.writePush(SymbolKind.that, 0);
            }
        } else if (isGivenToken(TokenType.symbol, "(")) { // print'('a);
            compileSubroutineCall(null);
        } else if (isGivenToken(TokenType.symbol, ".")) { // a'.'method();
            //checkToken(TokenType.symbol, ".");
            compileSubroutineCall(var);
        } else {
            writer.writePush(var.getKind(), var.getIndex());
        }
    }

    /**
     * Var is object which contains this subroutine.
     * @param var name of object which contain this subroutine call
     */
    private void compileSubroutineCall(Variable var) {
        boolean isCall = false;
        String name = "identifier";
        //checkIdentifier();
        if (isGivenToken(TokenType.symbol, ".")) {
            checkToken(TokenType.symbol, ".");
            if (anotherClass != null) {
                name = anotherClass + "." + currentToken.getValue();
                anotherClass = null;
            } else {
                name = currentToken.getValue();
            }
            checkIdentifier();
            isCall = true;
        }
        if (var != null) {
            writer.writePush(var.getKind(), var.getIndex()); // Implicit push of method object.
        }
        checkToken(TokenType.symbol, "(");
        this.paramListIndex = 0;
        tokensXml.add("<expressionList>");
        compileExpressionList();
        tokensXml.add("</expressionList>");
        checkToken(TokenType.symbol, ")");
        if (isCall && var != null) {
            writer.writeCall(var.getType()+"."+name, paramListIndex+1); // +1 cause of implicit push.
        } else {
            if (anotherClass != null) {
                if (!name.equals("identifier")) {
                    writer.writeCall(anotherClass+"."+name, paramListIndex);
                } else {
                    // Method.
                    if (subIndex == 2) {
                        writer.writePush(SymbolKind.pointer, 0);
                        int numberOfArgs = paramListIndex + 1;
                        int indexLast = writer.vmCode.size()-1;
                        writer.vmCode.add(writer.vmCode.size() - numberOfArgs,
                                writer.vmCode.get(indexLast)); // Copy last index before local args.
                        writer.vmCode.remove(indexLast+1);    // Delete moved element.
                        writer.writeCall(className+"."+anotherClass, paramListIndex+1);
                    }
                    // Function
                    if (subIndex == 1) {
                        writer.writeCall(className+"."+anotherClass, paramListIndex);
                    }
                }
                anotherClass = null;
            } else {
                writer.writeCall(name, paramListIndex);
            }
        }
        paramListIndex = 0;
    }

    private void compileExpressionList() {
        if (isGivenToken(TokenType.symbol, ")")) return;
        if (tryCompileExpression()) {
            paramListIndex++;
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
            case "true": {
                checkToken(TokenType.keyword, "true");
                writer.writePush(SymbolKind.constant, 1);   // True is -1.
                writer.writeArithmetic(ArithmeticCommand.NEG);
            } break;
            case "false": {
                checkToken(TokenType.keyword, "false");
                writer.writePush(SymbolKind.constant, 0);   // False is 0.
            } break;
            case "null": {
                checkToken(TokenType.keyword, "null");
                writer.writePush(SymbolKind.constant, 0);   // Null is 0.
            } break;
            case "this": {
                checkToken(TokenType.keyword, "this");
                writer.writePush(SymbolKind.pointer, 0);    // This is pointer 0.
            } break;
            default: checkToken(TokenType.keyword, "true/false/null/this");
        }
    }

    private boolean isSubroutineNameNext() {
        return currentToken.getType() == TokenType.identifier
                && tokenizer.hasNextToken() && tokenizer.getSecondNextToken().getValue().equals("(");
    }

    private void compileUnaryTerm() {
        final String symbols = JackTokenizer.getSymbols();
        int index = symbols.indexOf(currentToken.getValue());
        compileOp();
        compileTerm();
        // Write op after unary expression.
        writeOp(symbols.charAt(index), true);
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

    private void writeOp(char op, boolean isUnary) {
        switch (op) {
            case '+': writer.writeArithmetic(ArithmeticCommand.ADD); break;
            case '-': {
                if (isUnary) {
                    writer.writeArithmetic(ArithmeticCommand.NEG);
                } else {
                    writer.writeArithmetic(ArithmeticCommand.SUB);
                }
            } break;
            case '*': writer.writeArithmetic(ArithmeticCommand.MULTIPLY); break;
            case '/': writer.writeArithmetic(ArithmeticCommand.DIVIDE); break;
            case '=': writer.writeArithmetic(ArithmeticCommand.EQ); break;
            case '<': writer.writeArithmetic(ArithmeticCommand.LT); break;
            case '>': writer.writeArithmetic(ArithmeticCommand.GT); break;
            case '&': writer.writeArithmetic(ArithmeticCommand.AND); break;
            case '|': writer.writeArithmetic(ArithmeticCommand.OR); break;
            case '~': writer.writeArithmetic(ArithmeticCommand.NOT); break;
            default: throw new IllegalArgumentException("Not implemented op: " + op);
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
            throw new RuntimeException("["+ className +"]"+"Unexpected token: " + currentToken.toString() + ", expected: " + tmp.toString());
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
            throw new RuntimeException("["+ className +"]"+"Unexpected token: " + currentToken.toString() + ", expected: " + expected.toString());
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
