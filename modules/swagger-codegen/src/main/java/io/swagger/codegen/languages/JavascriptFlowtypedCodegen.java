package io.swagger.codegen.languages;

import io.swagger.codegen.*;
import io.swagger.models.Info;
import io.swagger.models.License;
import io.swagger.models.ModelImpl;
import io.swagger.models.Swagger;
import io.swagger.models.properties.*;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;

public class JavascriptFlowtypedCodegen extends AbstractTypeScriptClientCodegen {
    private static final SimpleDateFormat SNAPSHOT_SUFFIX_FORMAT = new SimpleDateFormat("yyyyMMddHHmm");

    public static final String NPM_NAME = "npmName";
    public static final String NPM_VERSION = "npmVersion";
    public static final String NPM_REPOSITORY = "npmRepository";
    public static final String SNAPSHOT = "snapshot";

    protected String npmName = null;
    protected String npmVersion = "1.0.0";
    protected String npmRepository = null;

    public JavascriptFlowtypedCodegen() {
        super();

        // clear import mapping (from default generator) as TS does not use it
        // at the moment
        importMapping.clear();

        setReservedWordsLowerCase(Arrays.asList(
                // local variable names used in API methods (endpoints)
                "varLocalPath", "queryParameters", "headerParams", "formParams", "useFormData", "varLocalDeferred",
                "requestOptions",
                // Typescript reserved words
                "abstract", "arguments", "boolean", "break", "byte",
                "case", "catch", "char", "class", "const",
                "continue", "debugger", "default", "delete", "do",
                "double", "else", "enum", "eval", "export",
                "extends", "false", "final", "finally", "float",
                "for", "function", "goto", "if", "implements",
                "import", "in", "instanceof", "int", "interface",
                "let", "long", "native", "new", "null",
                "package", "private", "protected", "public", "return",
                "short", "static", "super", "switch", "synchronized",
                "this", "throw", "throws", "transient", "true",
                "try", "typeof", "var", "void", "volatile",
                "while", "with", "yield",
                "Array", "Date", "eval", "function", "hasOwnProperty",
                "Infinity", "isFinite", "isNaN", "isPrototypeOf",
                "Math", "NaN", "Number", "Object",
                "prototype", "String", "toString", "undefined", "valueOf"));

        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList("string", "Boolean", "number", "Array", "Object", "Date", "File", "Blob")
        );

        instantiationTypes.put("array", "Array");
        instantiationTypes.put("list", "Array");
        instantiationTypes.put("map", "Object");
        typeMapping.clear();
        typeMapping.put("array", "Array");
        typeMapping.put("map", "Object");
        typeMapping.put("List", "Array");
        typeMapping.put("boolean", "Boolean");
        typeMapping.put("string", "string");
        typeMapping.put("int", "number");
        typeMapping.put("float", "number");
        typeMapping.put("number", "number");
        typeMapping.put("DateTime", "Date");
        typeMapping.put("date", "Date");
        typeMapping.put("long", "number");
        typeMapping.put("short", "number");
        typeMapping.put("char", "string");
        typeMapping.put("double", "number");
        typeMapping.put("object", "Object");
        typeMapping.put("integer", "number");
        // binary not supported in JavaScript client right now, using String as a workaround
        typeMapping.put("binary", "string");
        typeMapping.put("ByteArray", "string");
        typeMapping.put("UUID", "string");

        defaultIncludes = new HashSet<String>(languageSpecificPrimitives);
        outputFolder = "generated-code/javascript-flowtyped";
        embeddedTemplateDir = templateDir = "Javascript-Flowtyped";

        this.cliOptions.add(new CliOption(NPM_NAME, "The name under which you want to publish generated npm package"));
        this.cliOptions.add(new CliOption(NPM_VERSION, "The version of your npm package"));
        this.cliOptions.add(new CliOption(NPM_REPOSITORY, "Use this property to set an url your private npmRepo in the package.json"));
        this.cliOptions.add(new CliOption(SNAPSHOT, "When setting this property to true the version will be suffixed with -SNAPSHOT.yyyyMMddHHmm", BooleanProperty.TYPE).defaultValue(Boolean.FALSE.toString()));

    }

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    protected void addAdditionPropertiesToCodeGenModel(CodegenModel codegenModel, ModelImpl swaggerModel) {
        codegenModel.additionalPropertiesType = getTypeDeclaration(swaggerModel.getAdditionalProperties());
        addImport(codegenModel, codegenModel.additionalPropertiesType);
    }

    @Override
    public void processOpts() {
        super.processOpts();
        supportingFiles.add(new SupportingFile("index.mustache", "", "index.js"));
        supportingFiles.add(new SupportingFile("api.mustache", "", "api.js"));
        supportingFiles.add(new SupportingFile("configuration.mustache", "", "configuration.js"));
        supportingFiles.add(new SupportingFile("gitignore", "", ".gitignore"));

        addNpmPackageGeneration();
    }

    @Override
    public String getTypeDeclaration(Property p) {
        Property inner;
        if(p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty)p;
            inner = ap.getItems();
            return this.getSwaggerType(p) + "<" + this.getTypeDeclaration(inner) + ">";
        } else if(p instanceof MapProperty) {
            MapProperty mp = (MapProperty)p;
            inner = mp.getAdditionalProperties();
            return "{ [key: string]: " + this.getTypeDeclaration(inner) + "; }";
        } else if(p instanceof FileProperty || p instanceof ObjectProperty) {
            return "any";
        } else {
            return super.getTypeDeclaration(p);
        }
    }

    private void addNpmPackageGeneration() {
        if (additionalProperties.containsKey(NPM_NAME)) {
            this.setNpmName(additionalProperties.get(NPM_NAME).toString());
        }

        if (additionalProperties.containsKey(NPM_VERSION)) {
            this.setNpmVersion(additionalProperties.get(NPM_VERSION).toString());
        }

        if (additionalProperties.containsKey(SNAPSHOT) && Boolean.valueOf(additionalProperties.get(SNAPSHOT).toString())) {
            this.setNpmVersion(npmVersion + "-SNAPSHOT." + SNAPSHOT_SUFFIX_FORMAT.format(new Date()));
        }
        additionalProperties.put(NPM_VERSION, npmVersion);

        if (additionalProperties.containsKey(NPM_REPOSITORY)) {
            this.setNpmRepository(additionalProperties.get(NPM_REPOSITORY).toString());
        }

        //Files for building our lib
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));
        supportingFiles.add(new SupportingFile("package.mustache", "", "package.json"));
        supportingFiles.add(new SupportingFile("flowconfig.mustache", "", ".flowconfig"));
    }

    @Override
    public void preprocessSwagger(Swagger swagger) {
        if (swagger.getInfo() != null) {
            Info info = swagger.getInfo();
            if (StringUtils.isBlank(npmName) && info.getTitle() != null) {
                // when projectName is not specified, generate it from info.title
                npmName = sanitizeName(dashize(info.getTitle()));
            }
            if (StringUtils.isBlank(npmVersion)) {
                // when projectVersion is not specified, use info.version
                npmVersion = escapeUnsafeCharacters(escapeQuotationMark(info.getVersion()));
            }
        }

        // default values
        if (StringUtils.isBlank(npmName)) {
            npmName = "swagger-js-client";
        }
        if (StringUtils.isBlank(npmVersion)) {
            npmVersion = "1.0.0";
        }

        additionalProperties.put(NPM_NAME, npmName);
        additionalProperties.put(NPM_VERSION, npmVersion);
        additionalProperties.put(CodegenConstants.API_PACKAGE, apiPackage);
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        // process enum in models
        List<Object> models = (List<Object>) postProcessModelsEnum(objs).get("models");
        for (Object _mo : models) {
            Map<String, Object> mo = (Map<String, Object>) _mo;
            CodegenModel cm = (CodegenModel) mo.get("model");
            cm.imports = new TreeSet(cm.imports);
            // name enum with model name, e.g. StatusEnum => Pet.StatusEnum
            for (CodegenProperty var : cm.vars) {
                if (Boolean.TRUE.equals(var.isEnum)) {
                    var.datatypeWithEnum = var.datatypeWithEnum.replace(var.enumName, cm.classname + "" + var.enumName);
                }
            }
            if (cm.parent != null) {
                for (CodegenProperty var : cm.allVars) {
                    if (Boolean.TRUE.equals(var.isEnum)) {
                        var.datatypeWithEnum = var.datatypeWithEnum
                                .replace(var.enumName, cm.classname + "" + var.enumName);
                    }
                }
            }
        }

        return objs;
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove ', " to avoid code injection
        return input.replace("\"", "").replace("'", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    @Override
    public String getName() {
        return "javascript-flowtyped";
    }

    @Override
    public String getHelp() {
        return "Generates a Javascript client library using Flow types and Fetch API.";
    }

    public String getNpmName() {
        return npmName;
    }

    public void setNpmName(String npmName) {
        this.npmName = npmName;
    }

    public String getNpmVersion() {
        return npmVersion;
    }

    public void setNpmVersion(String npmVersion) {
        this.npmVersion = npmVersion;
    }

    public String getNpmRepository() {
        return npmRepository;
    }

    public void setNpmRepository(String npmRepository) {
        this.npmRepository = npmRepository;
    }

}
