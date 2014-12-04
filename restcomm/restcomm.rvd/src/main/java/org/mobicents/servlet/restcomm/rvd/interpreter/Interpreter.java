package org.mobicents.servlet.restcomm.rvd.interpreter;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import java.net.URLEncoder;

import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.ProjectAwareRvdContext;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.ProjectLogger;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.exceptions.UndefinedTarget;
import org.mobicents.servlet.restcomm.rvd.interpreter.InternalVariables.Scope;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.BadExternalServiceResponse;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.InvalidAccessOperationAction;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.model.StepJsonDeserializer;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlResponse;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;
import org.mobicents.servlet.restcomm.rvd.model.server.NodeName;
import org.mobicents.servlet.restcomm.rvd.model.server.ProjectOptions;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.ClientNounConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.ConferenceNounConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.NumberNounConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.RcmlClientNoun;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.RcmlConferenceNoun;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.RcmlDialStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.RcmlNumberNoun;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.RcmlSipuriNoun;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.SipuriNounConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.es.AccessOperation;
import org.mobicents.servlet.restcomm.rvd.model.steps.es.ValueExtractor;
import org.mobicents.servlet.restcomm.rvd.model.steps.fax.FaxStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.fax.RcmlFaxStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.gather.RcmlGatherStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.hangup.RcmlHungupStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.pause.RcmlPauseStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.play.PlayStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.play.RcmlPlayStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.record.RcmlRecordStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.redirect.RcmlRedirectStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.redirect.RedirectStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.reject.RcmlRejectStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.say.RcmlSayStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.say.SayStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.sms.RcmlSmsStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.sms.SmsStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdcollect.UssdCollectRcml;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdlanguage.UssdLanguageConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdlanguage.UssdLanguageRcml;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdsay.UssdSayRcml;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdsay.UssdSayStepConverter;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thoughtworks.xstream.XStream;


public class Interpreter {

    static final Logger logger = Logger.getLogger(Interpreter.class.getName());

    private RvdConfiguration rvdSettings;
    private ProjectStorage projectStorage;
    private HttpServletRequest httpRequest;
    private ProjectLogger projectLogger;
    private ProjectAwareRvdContext rvdContext;

    public ProjectLogger getProjectLogger() {
        return projectLogger;
    }

    public ProjectAwareRvdContext getRvdContext() {
        return rvdContext;
    }

    public void setProjectLogger(ProjectLogger projectLogger) {
        this.projectLogger = projectLogger;
    }

    public void setRvdSettings(RvdConfiguration rvdSettings) {
        this.rvdSettings = rvdSettings;
    }

    public void setProjectStorage(ProjectStorage projectStorage) {
        this.projectStorage = projectStorage;
    }

    private WorkspaceStorage workspaceStorage;
    private ModelMarshaler marshaler;

    private XStream xstream;
    private Gson gson;
    private String targetParam;
    private Target target;  // the target that is currently being interpreted
    private String appName;
   // private Map<String,String> requestParameters; // parameters like digits, callSid etc.
    MultivaluedMap<String, String> requestParams;

    private String contextPath;

    private String rcmlResult;
    //private Map<String, String> variables = new HashMap<String, String>();
    private InternalVariables internalVariables;
    private List<NodeName> nodeNames;

    public static String rcmlOnException() {
        return "<Response><Hangup/></Response>";
    }

    public Interpreter(ProjectAwareRvdContext rvdContext, String targetParam, String appName, HttpServletRequest httpRequest, MultivaluedMap<String, String> requestParams, WorkspaceStorage workspaceStorage) {
        this.rvdContext = rvdContext;
        this.rvdSettings = rvdContext.getSettings();
        this.projectStorage = rvdContext.getProjectStorage();
        this.httpRequest = httpRequest;
        this.targetParam = requestParams.getFirst("target");
        //this.targetParam = targetParam;
        this.appName = appName;
        this.requestParams = requestParams;
        this.workspaceStorage = workspaceStorage;
        this.marshaler = rvdContext.getMarshaler();
        this.projectLogger = rvdContext.getProjectLogger();

        this.contextPath = httpRequest.getContextPath();
        init();
    }

    // common intializations for all constructors
    private void init() {
        xstream = new XStream();
        xstream.registerConverter(new SayStepConverter());
        xstream.registerConverter(new PlayStepConverter());
        xstream.registerConverter(new RedirectStepConverter());
        xstream.registerConverter(new SmsStepConverter());
        xstream.registerConverter(new FaxStepConverter());
        xstream.registerConverter(new NumberNounConverter());
        xstream.registerConverter(new ClientNounConverter());
        xstream.registerConverter(new ConferenceNounConverter());
        xstream.registerConverter(new SipuriNounConverter());
        xstream.registerConverter(new UssdSayStepConverter());
        xstream.registerConverter(new UssdLanguageConverter());
        xstream.addImplicitCollection(RcmlDialStep.class, "nouns");
        xstream.alias("Response", RcmlResponse.class);
        xstream.addImplicitCollection(RcmlResponse.class, "steps");
        xstream.alias("Say", RcmlSayStep.class);
        xstream.alias("Play", RcmlPlayStep.class);
        xstream.alias("Gather", RcmlGatherStep.class);
        xstream.alias("Dial", RcmlDialStep.class);
        xstream.alias("Hangup", RcmlHungupStep.class);
        xstream.alias("Redirect", RcmlRedirectStep.class);
        xstream.alias("Reject", RcmlRejectStep.class);
        xstream.alias("Pause", RcmlPauseStep.class);
        xstream.alias("Sms", RcmlSmsStep.class);
        xstream.alias("Record", RcmlRecordStep.class);
        xstream.alias("Fax", RcmlFaxStep.class);
        xstream.alias("Number", RcmlNumberNoun.class);
        xstream.alias("Client", RcmlClientNoun.class);
        xstream.alias("Conference", RcmlConferenceNoun.class);
        xstream.alias("Sip", RcmlSipuriNoun.class);
        xstream.alias("UssdMessage", UssdSayRcml.class);
        xstream.alias("UssdCollect", UssdCollectRcml.class);
        xstream.alias("Language", UssdLanguageRcml.class);
        xstream.addImplicitCollection(RcmlGatherStep.class, "steps");
        xstream.addImplicitCollection(UssdCollectRcml.class, "messages");
        xstream.useAttributeFor(UssdCollectRcml.class, "action");
        xstream.useAttributeFor(RcmlGatherStep.class, "action");
        xstream.useAttributeFor(RcmlGatherStep.class, "timeout");
        xstream.useAttributeFor(RcmlGatherStep.class, "finishOnKey");
        xstream.useAttributeFor(RcmlGatherStep.class, "method");
        xstream.useAttributeFor(RcmlGatherStep.class, "numDigits");
        xstream.useAttributeFor(RcmlSayStep.class, "voice");
        xstream.useAttributeFor(RcmlSayStep.class, "language");
        xstream.useAttributeFor(RcmlSayStep.class, "loop");
        xstream.useAttributeFor(RcmlPlayStep.class, "loop");
        xstream.useAttributeFor(RcmlRejectStep.class, "reason");
        xstream.useAttributeFor(RcmlPauseStep.class, "length");
        xstream.useAttributeFor(RcmlRecordStep.class, "action");
        xstream.useAttributeFor(RcmlRecordStep.class, "method");
        xstream.useAttributeFor(RcmlRecordStep.class, "timeout");
        xstream.useAttributeFor(RcmlRecordStep.class, "finishOnKey");
        xstream.useAttributeFor(RcmlRecordStep.class, "maxLength");
        xstream.useAttributeFor(RcmlRecordStep.class, "transcribe");
        xstream.useAttributeFor(RcmlRecordStep.class, "transcribeCallback");
        xstream.useAttributeFor(RcmlRecordStep.class, "playBeep");
        xstream.useAttributeFor(RcmlDialStep.class, "action");
        xstream.useAttributeFor(RcmlDialStep.class, "method");
        xstream.useAttributeFor(RcmlDialStep.class, "timeout");
        xstream.useAttributeFor(RcmlDialStep.class, "timeLimit");
        xstream.useAttributeFor(RcmlDialStep.class, "callerId");
        xstream.useAttributeFor(RcmlDialStep.class, "record");
        xstream.aliasField("Number", RcmlDialStep.class, "number");
        xstream.aliasField("Client", RcmlDialStep.class, "client");
        xstream.aliasField("Conference", RcmlDialStep.class, "conference");
        xstream.aliasField("Uri", RcmlDialStep.class, "sipuri");

        // xstream.aliasField(alias, definedIn, fieldName);
        gson = new GsonBuilder().registerTypeAdapter(Step.class, new StepJsonDeserializer()).create();
    }

    public RvdConfiguration getRvdSettings() {
        return rvdSettings;
    }

    public String getAppName() {
        return appName;
    }


    public void setAppName(String appName) {
        this.appName = appName;
    }


    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    //public Map<String, String> getVariables() {
    //    return variables;
    //}

    public InternalVariables getInternalVariables() {
        return internalVariables;
    }

    /*
    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }
    */


    public Target getTarget() {
        return target;
    }


    public void setTarget(Target target) {
        this.target = target;
    }


    public String interpret() throws RvdException {
        String response = null;

        ProjectOptions projectOptions = FsProjectStorage.loadProjectOptions(appName, workspaceStorage); //rvdContext.getRuntimeProjectOptions();
        nodeNames = projectOptions.getNodeNames();

        if (targetParam == null || "".equals(targetParam)) {
            // No target has been specified. Load the default from project file
            targetParam = projectOptions.getDefaultTarget();
            if (targetParam == null)
                throw new UndefinedTarget();
            logger.debug("override default target to " + targetParam);
        }

        processBootstrapParameters();
        processRequestParameters();
        //handleStickyParameters(); // create local copies of sticky_* parameters

        response = interpret(targetParam, null, null, null);
        return response;
    }






    public MultivaluedMap<String, String> getRequestParams() {
        return requestParams;
    }

    public String getContextPath() {
        return contextPath;
    }


    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }


    /**
     * Interprets a target (module or an action handler usually) and renders RCML
     *
     * @param targetParam - the target to interpret
     * @param rcmlModel - the object to append all generated RCML
     * @param prependStep - optional step to prepend in the RCML output. This is usually used when replaying a module to prepend a Say with a validation message
     * @param originTarget - in chained interpretations this is the parant target that invoked this one
     *
     * @return RCML markup as a string
     *
     * @throws InterpreterException
     * @throws StorageException
     */
    public String interpret(String targetParam, RcmlResponse rcmlModel, Step prependStep, Target originTarget ) throws InterpreterException, StorageException {

        logger.debug("starting interpeter for " + targetParam);
        if ( rvdContext.getProjectSettings().getLogging() )
            projectLogger.log("Running target: " + targetParam).tag("app",appName).done();

        target = Interpreter.parseTarget(targetParam);

        // if we are switching modules, remove module-scoped variables
        if ( originTarget != null  &&  ! RvdUtils.safeEquals(target.getNodename(), originTarget.getNodename() ) ) {
            clearModuleVariables();
        }


        // TODO make sure all the required components of the target are available here

        if (target.action != null) {
            // Event handling
            loadStep(target.stepname).handleAction(this, target);
        } else {
            // RCML Generation

            if (rcmlModel == null )
                rcmlModel = new RcmlResponse();
            List<String> nodeStepnames = FsProjectStorage.loadNodeStepnames(appName, target.getNodename(), workspaceStorage);

            // if no starting step has been specified in the target, use the first step of the node as default
            if (target.getStepname() == null && !nodeStepnames.isEmpty())
                target.setStepname(nodeStepnames.get(0));

            // Prepend step if required. Usually used for error messages
            if ( prependStep != null ) {
                RcmlStep rcmlStep = prependStep.render(this);
                logger.debug("Prepending say step: " + rcmlStep );
                rcmlModel.steps.add( rcmlStep );
            }

            boolean startstep_found = false;
            for (String stepname : nodeStepnames) {

                if (stepname.equals(target.getStepname()))
                    startstep_found = true;

                if (startstep_found) {
                    // we found our starting step. Let's start processing
                    Step step = loadStep(stepname);
                    String rerouteTo = step.process(this, httpRequest, rvdContext); // is meaningful only for some of the steps like ExternalService steps
                    // check if we have to break the currently rendered module
                    if ( rerouteTo != null )
                        return interpret(rerouteTo, rcmlModel, null, target);
                    // otherwise continue rendering the current module
                    RcmlStep rcmlStep = step.render(this);
                    if ( rcmlStep != null)
                        rcmlModel.steps.add(rcmlStep);
                }
            }

            rcmlResult = xstream.toXML(rcmlModel);
        }

        return rcmlResult; // this is in case of an error
    }

    private Step loadStep(String stepname) throws StorageException  {
        //String stepfile_json = FileUtils.readFileToString(new File(projectBasePath + File.separator + "data/"
        //        + target.getNodename() + "." + stepname));
        String stepfile_json = projectStorage.loadStep(appName, target.getNodename(), stepname);
        Step step = gson.fromJson(stepfile_json, Step.class);

        return step;
    }


    public String evaluateExtractorExpression( ValueExtractor extractor, JsonElement response_element) throws InvalidAccessOperationAction, BadExternalServiceResponse {
        String value = "";

        JsonElement element = response_element;
        for ( AccessOperation operation : extractor.getAccessOperations() ) {
            if ( element == null )
                throw new BadExternalServiceResponse();

            if ( "object".equals(operation.getKind()) ) {
                if ( !element.isJsonObject() )
                    throw new BadExternalServiceResponse("No JSON object found");
                if ("propertyNamed".equals(operation.getAction()) )
                    element = element.getAsJsonObject().get( operation.getProperty() );
                else
                    throw new InvalidAccessOperationAction();
            } else
            if ( "array".equals(operation.getKind()) ) {
                if ( !element.isJsonArray() )
                    throw new BadExternalServiceResponse("No JSON array found");
                if ("itemAtPosition".equals(operation.getAction()) )
                    element = element.getAsJsonArray().get( operation.getPosition() );
                else
                    throw new InvalidAccessOperationAction();
            } else
            if ( "value".equals(operation.getKind()) ) {
                if ( !element.isJsonPrimitive() )
                    throw new BadExternalServiceResponse("No primitive value found (maybe null returned?)");
                value = element.getAsString();
            }
        }

        return value;
    }

    /**
     * Processes a block of text typically used for <Say/>ing that may contain variable expressions. Replaces variable
     * expressions with their corresponding values from interpreter's variables map
     */
    public String populateVariables(String sourceText) {
        if ( sourceText == null )
            return sourceText;

        // This class serves strictly the purposes of the following algorithm
        final class VariableInText {
            String variableName;
            Integer position;

            VariableInText(String variableName, Integer position) {
                this.variableName = variableName;
                this.position = position;
            }
        }

        Pattern pattern = Pattern.compile("\\$([A-Za-z]+[A-Za-z0-9_]*)");
        Matcher matches = pattern.matcher(sourceText);

        int searchStart = 0;
        List<VariableInText> variablesInText = new ArrayList<VariableInText>();
        while (matches.find(searchStart)) {
            variablesInText.add(new VariableInText(matches.group(1), matches.start())); // always at position 1 (second
                                                                                        // position)
            searchStart = matches.end();
        }

        StringBuffer buffer = new StringBuffer(sourceText);
        Collections.reverse(variablesInText);
        Set<String> variableKeys = internalVariables.getVariableKeys();
        for (VariableInText v : variablesInText) {
            String replaceValue = "";
            if (variableKeys.contains(v.variableName))
                replaceValue = internalVariables.getVariable(v.variableName).getStringValue();

            buffer.replace(v.position, v.position + v.variableName.length() + 1, replaceValue == null ? "" : replaceValue); // +1 is for the $ character
        }

        return buffer.toString();
    }

    public String buildAction(Map<String, String> pairs) {
        String query = "";
        for (String key : pairs.keySet()) {
            if ("".equals(query))
                query += "?";
            else
                query += "&";

            String encodedValue = "";
            String value = pairs.get(key);
            if ( value != null )
                try {
                    encodedValue = URLEncoder.encode( value, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    logger.warn("Error encoding RVD variable " + key + ": " + value, e);
                }

            query += key + "=" + encodedValue;
        }

        // append sticky parameters and module-scoped variables
        for ( String variableName : internalVariables.getVariableKeys() ) {
            InternalVariable var = internalVariables.getVariable(variableName);
            if ( var.scope == Scope.module || var.scope == Scope.sticky ) {
                if ("".equals(query))
                    query += "?";
                else
                    query += "&";

                String encodedValue = "";
                String value = var.getStringValue();
                if ( value != null )
                    try {
                        encodedValue = URLEncoder.encode( value, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        logger.warn("Error encoding RVD variable " + variableName + ": " + value, e);
                    }

                query += InternalVariables.ScopeToStringPrefix(var.scope) + variableName + "=" + encodedValue;
            }
        }

        return "controller" + query;
    }

    /*
     * //$url = $scope['conf']['appContext'] . '/index.php'; $url = 'index.php'; $pairs = array(); foreach ( $scope['sticky'] as
     * $name => $value ) { $name = 'sticky_' . $name; $pairs[] = $name . '=' . $value; } foreach ($added_pairs as $name =>
     * $value) $pairs[] = $name.'='.$value;
     *
     * if ( !empty($pairs) ) $url .= '?' . implode( '&amp;', $pairs );
     *
     * return $url;
     */

    public static Target parseTarget(String targetParam) {
        Target target = new Target();

        // TODO accept only valid characters in the target i.e. alphanumeric

        Pattern pattern = Pattern.compile("^([^.]+)(.([^.]+))?(.([^.]+))?");
        Matcher matcher = pattern.matcher(targetParam);
        if (matcher.find()) {
            if (matcher.groupCount() >= 1)
                target.setNodename(matcher.group(1));
            if (matcher.groupCount() >= 3)
                target.setStepname(matcher.group(3));
            if (matcher.groupCount() >= 5)
                target.setAction(matcher.group(5));
        }

        return target;
    }

    /**
     * @param label
     * @return The 'name' of the first node with the specified label. If not found returns null
     */
    public String getNodeNameByLabel( String label ) {
        for ( NodeName nodename : nodeNames ) {
            if ( label.equals(nodename.getLabel()) )
                return nodename.getName();
        }
        return null;
    }

    /**
     * Build a relative url to the named module
     * @param moduleName
     * @return the url or null if the module does not exist
     */
    public String moduleUrl(String moduleName) {
        String url = null;
        for ( NodeName nodeName : nodeNames )  {
            if ( nodeName.getName().equals(moduleName)) {
                Map<String, String> pairs = new HashMap<String, String>();
                pairs.put("target", moduleName);
                url = buildAction(pairs);
                break; // found it
            }
        }
        return url;
    }

    /**
     * Converts a file resource to a recorded wav file into an http resource accessible over HTTP. The path generated path for the wav files is hardcoded to /restcomm/recordings
     * @param fileResource
     * @param interpreter
     * @return
     */
    public String convertRecordingFileResourceHttp(String fileResource, HttpServletRequest request) throws URISyntaxException {
        String httpResource = fileResource; // assume this is already an http resource

        URIBuilder fileUriBuilder = new URIBuilder(fileResource);

        if ( ! fileUriBuilder.isAbsolute() ) {
            logger.warn("Cannot convert file URL to http URL - " + fileResource);
            return "";
        }

        if ( fileUriBuilder.getScheme().startsWith("http") ) // http or https - nothing to worry about
            return fileResource;

        if ( fileUriBuilder.getScheme().startsWith("file") ) {
            String wavFilename = "";
            int filenameBeforeStartPos = fileResource.lastIndexOf('/');
            if ( filenameBeforeStartPos != -1 ) {
                wavFilename = fileResource.substring(filenameBeforeStartPos+1);
                String hostname = rvdSettings.getEffectiveRestcommIp(request);
                //URIBuilder httpUriBuilder = new URIBuilder().setScheme(request.getScheme()).setHost(request.getServerName()).setPort(request.getServerPort()).setPath("/restcomm/recordings/" + wavFilename);
                URIBuilder httpUriBuilder = new URIBuilder().setScheme(request.getScheme()).setHost(hostname).setPort(request.getServerPort()).setPath("/restcomm/recordings/" + wavFilename);
                httpResource = httpUriBuilder.build().toString();
            }
        }

        return httpResource;
    }

    /*
    public void putStickyVariable(String name, String value) {
            variables.put(RvdConfiguration.STICKY_PREFIX + name, value);
    }

    public void putModuleVariable(String name, String value) {
        variables.put(RvdConfiguration.MODULE_PREFIX + name, value);
    }

    public void putVariable(String name, String value) {
        variables.put(name, value);
    }
    */

    /**
     * Create rvd variables out of parameters passed in the URL. Restcomm request parameters such as 'CallSid', 'AccountSid' etc. are prefixed with the 'core_'
     * prefix in their names.
     */
    private void processRequestParameters() {
        internalVariables = new InternalVariables();

        Set<String> validNames = new HashSet<String>(Arrays.asList(new String[] {"CallSid","AccountSid","From","To","Body","CallStatus","ApiVersion","Direction","CallerName"}));
        for ( String anyVariableName : getRequestParams().keySet() ) {
            if ( validNames.contains(anyVariableName) ) {
                String variableValue = getRequestParams().getFirst(anyVariableName);
                //getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + anyVariableName, variableValue );

                InternalVariable variable = new InternalVariable(variableValue);
                variable.setCore(true);
                internalVariables.addVariable(RvdConfiguration.CORE_VARIABLE_PREFIX + anyVariableName, variable );
            } else
            if ( anyVariableName.startsWith(RvdConfiguration.STICKY_PREFIX) || anyVariableName.startsWith(RvdConfiguration.MODULE_PREFIX) ) {
                // set up sticky variables
                String variableValue = getRequestParams().getFirst(anyVariableName);
                //getVariables().put(anyVariableName, variableValue );

                if ( anyVariableName.startsWith(RvdConfiguration.MODULE_PREFIX) ) {
                    InternalVariable variable = new InternalVariable(variableValue);
                    variable.setScope(InternalVariables.Scope.module);
                    internalVariables.addVariable(anyVariableName.substring(RvdConfiguration.MODULE_PREFIX.length()), variable );
                } else
                if ( anyVariableName.startsWith(RvdConfiguration.STICKY_PREFIX) ) {
                    InternalVariable variable = new InternalVariable(variableValue);
                    variable.setScope(InternalVariables.Scope.sticky);
                    internalVariables.addVariable(anyVariableName.substring(RvdConfiguration.MODULE_PREFIX.length()), variable );
                }
            } else {
                //for the rest of the parameters simply create a variable with the same name
                String variableValue = getRequestParams().getFirst(anyVariableName);
                //getVariables().put(anyVariableName, variableValue );

                InternalVariable variable = new InternalVariable(variableValue);
                internalVariables.addVariable(anyVariableName, variable );
            }
        }

        this.internalVariables = internalVariables;
        logger.debug(internalVariables);
    }


    /** Add bootstrap parameters to the variables array. Usually these are used in application downloaded
     * from the app store.
     * @throws StorageException
     *
     *
     */
    private void processBootstrapParameters() throws StorageException {

        if ( ! FsProjectStorage.hasBootstrapInfo(appName, workspaceStorage) )
            return; // nothing to do

         String data = FsProjectStorage.loadBootstrapInfo(appName,workspaceStorage);
         JsonParser parser = new JsonParser();
         JsonElement rootElement = parser.parse(data);

        if ( rootElement.isJsonObject() ) {
            JsonObject rootObject = rootElement.getAsJsonObject();
            for ( Entry<String, JsonElement> entry : rootObject.entrySet() ) {
                String name = entry.getKey();
                JsonElement valueElement = entry.getValue();
                String value;
                if ( valueElement.isJsonPrimitive() && valueElement.getAsJsonPrimitive().isString() ) {
                    value = valueElement.getAsJsonPrimitive().getAsString();
                    //getVariables().put(name, value);

                    InternalVariable variable = new InternalVariable(value);
                    internalVariables.addVariable(name.substring(RvdConfiguration.MODULE_PREFIX.length()), variable );

                    logger.debug("Loaded bootstrap parameter: " + name + " - " + value);
                } else
                    logger.warn("Warning. Not-string bootstrap value found for parameter: " + name);
            }
        }
    }

    /**
     * When switching from one module to the next clears module-scoped variables.
     */
    public void clearModuleVariables() {
        /*
        Iterator<String> it = variables.keySet().iterator();
        while (it.hasNext()) {
          String variableName = it.next();
          if( variableName.startsWith(RvdConfiguration.MODULE_PREFIX) ) {
              it.remove();
          }
        }
        */

        Iterator<String> it2 = internalVariables.getVariableKeys().iterator();
        while (it2.hasNext()) {
          String varName = it2.next();
          if ( internalVariables.getVariable(varName).getScope() == Scope.module )
              it2.remove();
        }
    }
}
