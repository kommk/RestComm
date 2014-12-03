package org.mobicents.servlet.restcomm.rvd.interpreter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.InternalVariableDoesNotExist;

public class InternalVariables {

    public enum Scope {
        module,
        sticky
    }

    public static String ScopeToStringPrefix(Scope scope) {
        switch (scope) {
            case module: return RvdConfiguration.MODULE_PREFIX;
            case sticky: return RvdConfiguration.STICKY_PREFIX;
            default: return "";
        }
    }

    Map<String,InternalVariable> values = new HashMap<String,InternalVariable>();

    public InternalVariables() {
        // TODO Auto-generated constructor stub
    }

    //public String getStringVariable(String key) {
    //    return values.get(key);
    //}

    public InternalVariable getVariable(String key) {
        return values.get(key);
    }

    public void addVariable(String name, InternalVariable variable) {
        values.put(name, variable);
    }

    public Iterator<Entry<String,InternalVariable>> getIterator() {
        return values.entrySet().iterator();
    }

    public Set<String> getVariableKeys() {
        return values.keySet();
    }

    public void updateVariable(String key, Object value) throws InternalVariableDoesNotExist {
        InternalVariable var = values.get(key);
        if ( var == null)
            throw new InternalVariableDoesNotExist("Cannot update non-existing variable " + key);
        var.setValue(value);
    }


    //public void init(Map<String,String> urlParameters)

}
