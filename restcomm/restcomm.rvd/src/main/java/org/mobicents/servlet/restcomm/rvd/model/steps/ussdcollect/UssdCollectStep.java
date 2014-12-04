package org.mobicents.servlet.restcomm.rvd.model.steps.ussdcollect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.InternalVariable;
import org.mobicents.servlet.restcomm.rvd.interpreter.InternalVariables.Scope;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.interpreter.Target;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdsay.UssdSayStep;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public class UssdCollectStep extends Step {

    static final Logger logger = Logger.getLogger(UssdCollectStep.class.getName());

    public static class Mapping {
        String digits;
        String next;
    }
    public final class Menu {
        private List<Mapping> mappings;
    }
    public final class Collectdigits {
        private String next;
        private String collectVariable;
        private String scope;
    }

    String gatherType;
    String text;
    private Menu menu;
    private Collectdigits collectdigits;

    List<UssdSayStep> messages;

    public UssdCollectStep() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public UssdCollectRcml render(Interpreter interpreter) throws InterpreterException {
        // TODO Auto-generated method stub
        UssdCollectRcml rcml = new UssdCollectRcml();
        String newtarget = interpreter.getTarget().getNodename() + "." + getName() + ".handle";
        Map<String, String> pairs = new HashMap<String, String>();
        pairs.put("target", newtarget);

        rcml.action = interpreter.buildAction(pairs);
        for ( UssdSayStep message : messages ) {
            rcml.messages.add(message.render(interpreter));
        }

        return rcml;
    }

    @Override
    public void handleAction(Interpreter interpreter, Target originTarget) throws InterpreterException, StorageException {
        logger.info("UssdCollect handler");
        if ("menu".equals(gatherType)) {

            boolean handled = false;
            for (Mapping mapping : menu.mappings) {
                // use a string for USSD collect. Alpha is supported too
                String digits = interpreter.getRequestParams().getFirst("Digits");

                logger.debug("checking digits: " + mapping.digits + " - " + digits);

                if (mapping.digits != null && mapping.digits.equals(digits)) {
                    // seems we found out menu selection
                    logger.debug("seems we found our menu selection");
                    interpreter.interpret(mapping.next,null,null, originTarget);
                    handled = true;
                }
            }
            if (!handled) {
                interpreter.interpret(interpreter.getTarget().getNodename() + "." + interpreter.getTarget().getStepname(),null,null, originTarget);
            }
        }
        if ("collectdigits".equals(gatherType)) {
            String variableName = collectdigits.collectVariable;
            String variableValue = interpreter.getRequestParams().getFirst("Digits");
            if ( variableValue == null ) {
                logger.warn("'Digits' parameter was null. Is this a valid restcomm request?");
                variableValue = "";
            } else {
                //interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "Digits", digitsString);
                interpreter.getInternalVariables().addVariable(RvdConfiguration.CORE_VARIABLE_PREFIX + "Digits", new InternalVariable(variableValue).setCore(true));
            }

            // is this an application-scoped variable ?
            if ( "application".equals(collectdigits.scope) ) {
                logger.debug("'" + variableName + "' is application scoped");
                //interpreter.getVariables().put(RvdConfiguration.STICKY_PREFIX + variableName, variableValue);
                interpreter.getInternalVariables().addVariable(variableName, new InternalVariable(variableValue).setScope(Scope.sticky));
            } else
            if ( "module".equals(collectdigits.scope) ) {
                interpreter.getInternalVariables().addVariable(variableName, new InternalVariable(variableValue).setScope(Scope.module));
            }
            // in any case initialize the module-scoped variable
            //interpreter.getVariables().put(variableName, variableValue);

            interpreter.interpret(collectdigits.next,null,null, originTarget);
        }
    }
}
