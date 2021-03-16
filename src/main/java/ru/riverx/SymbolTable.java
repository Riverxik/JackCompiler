package ru.riverx;

import java.util.Hashtable;

public class SymbolTable {
    private final Hashtable<String, Variable> classLevel;
    private final Hashtable<String, Variable> subroutineLevel;

    public SymbolTable() {
        this.classLevel = new Hashtable<>();
        this.subroutineLevel = new Hashtable<>();
    }

    public void defineClass(String name, String type, String kind) {
        SymbolKind _kind = getKindFromString(kind);
        Variable var = new Variable(type, _kind);
        classLevel.put(name, var);
    }

    public void defineSubroutine(String name, String type, String kind) {
        SymbolKind _kind = getKindFromString(kind);
        Variable var = new Variable(type, _kind);
        subroutineLevel.put(name, var);
    }

    private SymbolKind getKindFromString(String kind) {
        switch (kind) {
            case "field": return SymbolKind.FIELD;
            case "static": return SymbolKind.STATIC;
            case "arg": return SymbolKind.ARG;
            case "var": return SymbolKind.VAR;
            default: throw new IllegalArgumentException("There is no kind: " + kind);
        }
    }

    public Variable findVariable(String name) {
        for (String n : subroutineLevel.keySet()) {
            if (n.equals(name)) {
                return subroutineLevel.get(n);
            }
        }
        for (String n : classLevel.keySet()) {
            if (n.equals(name)) {
                return classLevel.get(n);
            }
        }
        throw new RuntimeException("Undefined variable: "+ name);
    }

    public void resetKindCountSubroutine() {
        Variable.argumentCount = 0;
        Variable.localCount = 0;
    }

    public void resetKindCountClass() {
        Variable.staticCount = 0;
        Variable.fieldCount = 0;
        resetKindCountSubroutine();
    }

    private int varCount(SymbolKind kind) {
        switch (kind) {
            case STATIC: return Variable.staticCount;
            case FIELD: return Variable.fieldCount;
            case ARG: return Variable.argumentCount;
            case VAR: return Variable.localCount;
            default: throw new IllegalArgumentException("There is no kind: " + kind);
        }
    }

    private SymbolKind kindOf(String name) {
        return findVariable(name).getKind();
    }

    private String typeOf(String name) {
        return findVariable(name).getType();
    }

    private int indexOf(String name) {
        return findVariable(name).getIndex();
    }
}
