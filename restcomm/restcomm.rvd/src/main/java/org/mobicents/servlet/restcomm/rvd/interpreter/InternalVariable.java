package org.mobicents.servlet.restcomm.rvd.interpreter;

import org.mobicents.servlet.restcomm.rvd.interpreter.InternalVariables.Scope;

public class InternalVariable {

    Object value;
    Scope scope;
    boolean core = false;
    boolean bootstrapParameter = false;


    public InternalVariable(Object value) {
        this.value = value;
    }

    public Scope getScope() {
        return scope;
    }

    public InternalVariable setScope(Scope scope) {
        this.scope = scope;
        return this;
    }

    public boolean isCore() {
        return core;
    }

    public boolean isBootstrapParameter() {
        return bootstrapParameter;
    }

    public void setBootstrapParameter(boolean bootstrapParameter) {
        this.bootstrapParameter = bootstrapParameter;
    }

    public InternalVariable setCore(boolean core) {
        this.core = core;
        return this;
    }

    public String getStringValue() {
        return value.toString();
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
