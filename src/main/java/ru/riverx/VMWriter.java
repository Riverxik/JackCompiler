package ru.riverx;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VMWriter {
    List<String> vmCode;
    public VMWriter() {
        this.vmCode = new ArrayList<>();
    }

    public void writePush(SymbolKind kind, int index) {
        Segment segment = getSegmentFromKind(kind);
        vmCode.add("push " + segment.name().toLowerCase() + " " + index);
    }

    public void writePop(SymbolKind kind, int index) {
        Segment segment = getSegmentFromKind(kind);
        vmCode.add("pop " + segment.name().toLowerCase() + " " + index);
    }

    private Segment getSegmentFromKind(SymbolKind kind) {
        switch (kind) {
            case STATIC: return Segment.STATIC;
            case FIELD: return Segment.THIS;
            case constant: return Segment.constant;
            case ARG: return Segment.argument;
            case VAR: return Segment.local;
            case temp: return Segment.temp;
            case pointer: return Segment.pointer;
            case that: return Segment.that;
            default: throw new IllegalArgumentException("Not implemented kind: " + kind);
        }
    }

    public void writeArithmetic(ArithmeticCommand command) {
        switch (command) {
            case ADD: vmCode.add("add"); break;
            case SUB: vmCode.add("sub"); break;
            case NEG: vmCode.add("neg"); break;
            case EQ: vmCode.add("eq"); break;
            case GT: vmCode.add("gt"); break;
            case LT: vmCode.add("lt"); break;
            case AND: vmCode.add("and"); break;
            case OR: vmCode.add("or"); break;
            case NOT: vmCode.add("not"); break;
            case MULTIPLY: writeCall("Math.multiply", 2); break;
            case DIVIDE: writeCall("Math.divide", 2); break;
            default: throw new IllegalArgumentException("Not implemented cmd: " + command);
        }
    }

    public void writeConstant(String constant) {
        vmCode.add("push constant " + constant);
    }

    public void writeLabel(String label) {
        vmCode.add("label " + label);
    }

    public void writeGoto(String label) {
        vmCode.add("goto " + label);
    }

    public void writeIf(String label) {
        vmCode.add("if-goto " + label);
    }

    public void writeCall(String name, int nArgs) {
        vmCode.add("call " + name + " " + nArgs);
    }

    public void writeFunction(String name, int nLocals) {
        vmCode.add("function " + name + " " + nLocals);
    }

    public void writeReturn() {
        vmCode.add("return");
    }
}
