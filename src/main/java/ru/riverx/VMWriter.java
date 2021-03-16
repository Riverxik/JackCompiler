package ru.riverx;

import java.util.ArrayList;
import java.util.List;

public class VMWriter {
    List<String> vmCode;
    List<String> buffer;
    public VMWriter() {
        this.vmCode = new ArrayList<>();
        this.buffer = new ArrayList<>();
    }

    public void writePush(SymbolKind kind, int index) {
        Segment segment = getSegmentFromKind(kind);
        vmCode.add("push" + segment.name() + index);
        writeLater();
    }

    public void writePop(SymbolKind kind, int index) {
        Segment segment = getSegmentFromKind(kind);
        vmCode.add("push" + segment.name() + index);
    }

    private void writeLater() {
        vmCode.addAll(buffer);
        buffer.clear();
    }

    private Segment getSegmentFromKind(SymbolKind kind) {
        switch (kind) {
            case STATIC: return Segment.STATIC;
            case FIELD: return Segment.THIS;
            case ARG: return Segment.argument;
            case VAR: return Segment.local;
            default: throw new IllegalArgumentException("Not implemented kind: " + kind);
        }
    }

    public void writeArithmetic(ArithmeticCommand command) {
        switch (command) {
            case ADD: buffer.add("add"); break;
            case SUB: buffer.add("sub"); break;
            case NEG: vmCode.add("neg"); break;
            case EQ: buffer.add("eq"); break;
            case GT: buffer.add("gt"); break;
            case LT: buffer.add("lt"); break;
            case AND: buffer.add("and"); break;
            case OR: buffer.add("or"); break;
            case NOT: vmCode.add("not"); break;
            case MULTIPLY: writeMathCall("Math.multiply"); break;
            case DIVIDE: writeMathCall("Math.divide"); break;
            default: throw new IllegalArgumentException("Not implemented cmd: " + command);
        }
    }

    public void writeConstant(String constant) {
        vmCode.add("push constant" + constant);
    }

    public void writeLabel(String label) {
        vmCode.add("label" + label);
    }

    public void writeGoto(String label) {
        vmCode.add("goto " + label);
    }

    public void writeIf(String label) {
        vmCode.add("if-goto " + label);
    }

    private void writeMathCall(String name) {
        buffer.add("call " + name + " " + 2);
    }

    public void writeCall(String name, int nArgs) {
        vmCode.add("call " + name + " " + nArgs);
    }

    public void writeFunction(String name, int nLocals) {

    }

    public void writeReturn() {

    }
}
