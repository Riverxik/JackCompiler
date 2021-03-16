package ru.riverx;

public class Variable {
    private final String type;
    private final SymbolKind kind;
    private int index;
    public static int staticCount = 0;
    public static int fieldCount = 0;
    public static int argumentCount = 0;
    public static int localCount = 0;

    public Variable(String type, SymbolKind kind) {
        this.type = type;
        this.kind = kind;
        increaseKindCount();
    }

    private void increaseKindCount() {
        switch (this.kind) {
            case STATIC: index = staticCount++; break;
            case FIELD: index = fieldCount++; break;
            case ARG: index = argumentCount++; break;
            case VAR: index = localCount++; break;
            default: break;
        }
    }

    public String getType() {
        return type;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public int getIndex() {
        return index;
    }
}
