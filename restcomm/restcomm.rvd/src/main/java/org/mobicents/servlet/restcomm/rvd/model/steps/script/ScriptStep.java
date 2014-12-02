package org.mobicents.servlet.restcomm.rvd.model.steps.script;

import java.io.IOException;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.ProjectAwareRvdContext;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Node;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public class ScriptStep extends Step {

    static final Logger logger = Logger.getLogger(ScriptStep.class.getName());

    private String sourceCode;
    private String language;

    public ScriptStep() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public RcmlStep render(Interpreter interpreter) throws InterpreterException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String process(Interpreter interpreter, HttpServletRequest httpRequest, ProjectAwareRvdContext rvdContext ) throws InterpreterException {
        logger.info("running ScriptStep process");

        String scriptPath = getScriptPath(rvdContext);
        logger.info("Script path: " + scriptPath);

        String[] roots = new String[] { scriptPath };
        GroovyScriptEngine gse;

        try {
            gse = new GroovyScriptEngine(roots);
            Binding binding = new Binding();
            //binding.setVariable("input", "world");
            gse.run( getScriptName(), binding);
            //System.out.println(binding.getVariable("output"));
        } catch (IOException | ResourceException | ScriptException e) {
            throw new InterpreterException("Error executing script " + scriptPath, e);
        }

        return null;
    }



    public String getSourceCode() {
        return sourceCode;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public void build(Node parentNode, String projectName, WorkspaceStorage storage) throws StorageException {
        FsProjectStorage.storeScriptSourceFile(this, parentNode, projectName, storage);
    }

    public String getScriptPath(ProjectAwareRvdContext rvdcontext) {
        String scriptPath = rvdcontext.getWorkspaceStorage().getRootPath() + "/" + rvdcontext.getProjectName() + "/data/scripts/" + getName() + "." + getLanguage();
        return scriptPath;
    }

    public String getScriptName() {
        return getName() + "." + getLanguage();
    }
}
