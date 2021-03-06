package dk.pfrandsen.xsd;

import dk.pfrandsen.check.AnalysisInformationCollector;
import dk.pfrandsen.util.Utilities;
import dk.pfrandsen.util.XQuery;
import dk.pfrandsen.util.XsdUtil;
import org.apache.commons.io.FilenameUtils;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SchemaChecker {
    public static String ASSERTION_ID_FORM_DEFAULT = "CA20-XSD-Form-Default";
    public static String ASSERTION_ID_NILLABLE = "CA19-XSD-Nillable";
    public static String ASSERTION_ID_MIN_MAX = "CA54-XSD-Redundant-Min-Max-Occurs";
    public static String ASSERTION_ID_TYPE = "CA24-XSD-Type-Validate";
    public static String ASSERTION_ID_CONCEPT = "CA34-XSD-Illegal-Content-In-Concept-Schema";
    public static String ASSERTION_ID_ELEMENT = "CA??-XSD-Element-Validation";
    public static String ASSERTION_ID_NAMESPACE = "CA25-XSD-Beta-Namespace-Not-Allowed";
    public static String ASSERTION_ID_ENUM_VALUE = "CA41-XSD-Enum-Value-Validate";
    public static String ASSERTION_ID_ELEMENT_DEFINITION = "CA26-XSD-Element-Definition-Validation";
    public static String ASSERTION_ID_REDEFINITION = "CA21-XSD-Redefinition-Validation";
    public static String ASSERTION_ID_SCHEMA_USE = "CA31-XSD-Schema-Use-Validation";
    public static String ASSERTION_ID_SCHEMA_TNS_VERSION = "CA33-XSD-Namespace-Version-Validation";
    public static String ASSERTION_ID_SCHEMA_TNS_CASE = "CA??-XSD-Namespace-Case-Validation";
    public static String ASSERTION_ID_SCHEMA_TNS_PREFIX = "CA??-XSD-Namespace-Prefix-Validation";
    public static String ASSERTION_ID_SCHEMA_TNS_CHARS = "CA??-XSD-Namespace-Characters-Validation";
    public static String ASSERTION_ID_ANY_TYPE = "CA52-XSD-AnyType-Validation";
    public static String ASSERTION_ID_ANY = "CA52A-XSD-Any-Validation";
    public static String ASSERTION_ID_ANY_ATTRIBUTE= "CA52B-XSD-AnyAttribute-Validation";
    public static String ASSERTION_ID_IDENTICAL_ELEMENTS= "CA51-XSD-Identical-Elements-Validation";
    public static String ASSERTION_ID_IMPORT_INCLUDE_LOCATION = "CA50-XSD-Import-Include-Schema-Location-Validate";
    public static String ASSERTION_ID_SCHEMA_FILENAME = "CA29-XSD-File-Name-Validate";
    public static String ASSERTION_ID_ENTERPRISE_CONCEPT_NAMESPACE = "CA44-XSD-Enterprise-Concept-Namespace-Validate";
    public static String ASSERTION_ID_SERVICE_CONCEPT_NAMESPACE = "CA45-XSD-Service-Concept-Namespace-Validate";
    public static String ASSERTION_ID_DEPRECATED = "CA43-XSD-Deprecated-Marker-Validate";
    public static String ASSERTION_ID_UNUSED_NS_PREFIX = "CA70-XSD/WSDL-Unused-Namespace-Prefix-validate";
    public static String ASSERTION_ID_UNUSED_IMPORT = "CA47-XSD/WSDL-Unused-Import-validate";
    public static String ASSERTION_ID_NAMESPACE_MATCH_PATH = "CA35-XSD-Target-Namespace-Match-Path-Validate";
    public static String ASSERTION_ID_INCLUDE = "Include-Validate";

    public static void checkFormDefault(String xsd, AnalysisInformationCollector collector) {
        // elementFormDefault = 'qualified' attributeFormDefault = 'unqualified'
        try {
            String formAttr = XQuery.runXQuery(Paths.get("xsd"), "formDefault.xq", xsd);
            String element = XQuery.mapSingleResult(formAttr, "element");
            String attribute = XQuery.mapSingleResult(formAttr, "attribute");
            if (!"qualified".equals(element)) {
                collector.addError(ASSERTION_ID_FORM_DEFAULT,
                        "Value of attribute elementFormDefault must be 'qualified'",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR);
            }
            if (!"unqualified".equals(attribute)) {
                collector.addError(ASSERTION_ID_FORM_DEFAULT,
                        "Value of attribute attributeFormDefault must be 'unqualified'",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR);
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_FORM_DEFAULT);
        }
    }

    public static void checkNillable(String xsd, AnalysisInformationCollector collector) {
        // nillable attribute must not be used
        try {
            String nill = XQuery.runXQuery(Paths.get("xsd"), "nillable.xq", xsd);
            List<Map<String,String>> elements = XQuery.mapResult(nill, "name", "nillable");
            for (Map<String,String> element : elements) {
                String elementName = element.get("name");
                String elementValue = element.get("nillable");
                collector.addError(ASSERTION_ID_NILLABLE, "Element must must not have nillable attribute",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Element '" + elementName +
                "', nillable='" + elementValue + "'");
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_NILLABLE);
        }
    }

    public static void checkMinMaxOccurs(String xsd, AnalysisInformationCollector collector) {
        try {
            String minMax = XQuery.runXQuery(Paths.get("xsd"), "minMaxOccurs.xq", xsd);
            List<Map<String,String>> items = XQuery.mapResult(minMax, "name", "node");
            for (Map<String,String> item : items) {
                String name = item.get("name");
                if ("".equals(name)) {
                    name = "<anonymous>";
                }
                String node = item.get("node");
                collector.addError(ASSERTION_ID_MIN_MAX, "Redundant minOccurs/maxOccurs='1'",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MINOR, "Node '" + name + "' (" + node + ")");
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_MIN_MAX);
        }
    }

    public static void checkConceptTypes(String xsd, AnalysisInformationCollector collector) {
        try {
            String tns = XsdUtil.getTargetNamespace(xsd);
            if (XsdUtil.isConcept(tns)) {
                // find illegal include, import, simpleType, complexType
                String illegal = XQuery.runXQuery(Paths.get("xsd"), "illegalConceptTypes.xq", xsd);
                List<Map<String,String>> items = XQuery.mapResult(illegal, "name", "node");
                for (Map<String,String> item : items) {
                    String name = item.get("name");
                    String node = item.get("node");
                    collector.addError(ASSERTION_ID_CONCEPT, "Illegal content in concept schema",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Node '" + name + "' (" + node + ")");
                }
                // check for unused legal top-level simpleType definitions (enumerations)
                String unused = XQuery.runXQuery(Paths.get("xsd"), "unusedEnumeration.xq", xsd);
                for (String name : XQuery.mapResult(unused, "name")) {
                    collector.addError(ASSERTION_ID_CONCEPT, "Unused enumeration in concept schema",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Enumeration '" + name + "'");
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_CONCEPT);
        }
    }

    public static void checkTypes(String xsd, AnalysisInformationCollector collector) {
        try {
            String embedded = XQuery.runXQuery(Paths.get("xsd"), "anonymousEnumeration.xq", xsd);
            List<Map<String,String>> items = XQuery.mapResult(embedded, "name", "node");
            for (Map<String,String> item : items) {
                String name = item.get("name");
                if ("".equals(name)) {
                    name = "<anonymous>";
                }
                String node = item.get("node");
                collector.addError(ASSERTION_ID_TYPE, "Embedded (anonymous) type found",
                        AnalysisInformationCollector.SEVERITY_LEVEL_CRITICAL, "Node '" + name + "' (" + node + ")");
            }
            // check name for all top level types
            String topLevel = XQuery.runXQuery(Paths.get("xsd"), "topLevelTypes.xq", xsd);
            items = XQuery.mapResult(topLevel, "name", "node");
            for (Map<String,String> item : items) {
                String name = item.get("name");
                String node = item.get("node");
                if (!XsdUtil.isValidTypeName(name)) {
                    collector.addError(ASSERTION_ID_TYPE, "Illegal type name",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Type '" + name + "' (" + node + ")");
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_TYPE);
        }
    }

    public static void checkElements(String xsd, AnalysisInformationCollector collector) {
        try {
            // check name for all explicitly named elements
            String elementNames = XQuery.runXQuery(Paths.get("xsd"), "elementNames.xq", xsd);
            for (String name : XQuery.mapResult(elementNames, "name")) {
                if (!XsdUtil.isValidElementName(name)) {
                    collector.addError(ASSERTION_ID_ELEMENT, "Illegal element name",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Element '" + name + "'");
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_ELEMENT);
        }
    }

    public static void checkBetaNamespace(String xsd, AnalysisInformationCollector collector) {
        try {
            String ns = XQuery.runXQuery(Paths.get("xsd"), "namespaces.xq", xsd);
            List<String> namespaces = XQuery.mapResult(ns, "namespace");
            // remove duplicates as XQuery distinct-values does not work in the current implementation
            Set<String> nsSet = new LinkedHashSet<>(namespaces);
            for (String namespace : nsSet) {
                if (namespace.contains("beta-")) {
                    collector.addError(ASSERTION_ID_NAMESPACE, "Namespace containing 'beta-' found",
                            AnalysisInformationCollector.SEVERITY_LEVEL_CRITICAL, "Namespace '" + namespace + "'");
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_NAMESPACE);
        }
    }

    public static void checkEnumerationValues(String xsd, AnalysisInformationCollector collector) {
        try {
            String enumValues = XQuery.runXQuery(Paths.get("xsd"), "enumerationValues.xq", xsd);
            List<Map<String,String>> items = XQuery.mapResult(enumValues, "name", "node", "value");
            for (Map<String,String> item : items) {
                String name = item.get("name");
                String node = item.get("node");
                String value = item.get("value");
                if (!XsdUtil.isValidEnumerationValue(value)) {
                    collector.addError(ASSERTION_ID_ENUM_VALUE, "Illegal enumeration value",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, node + ":" + name +
                                    ", value '" + value + "'");
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_ENUM_VALUE);
        }
    }

    public static void checkSimpleTypesInConcept(String xsd, AnalysisInformationCollector collector) {
        // named simple types must be in concept schema
        try {
            String tns = XsdUtil.getTargetNamespace(xsd);
            if (!XsdUtil.isConcept(tns)) {
                String namedSimple = XQuery.runXQuery(Paths.get("xsd"), "namedSimpleTypes.xq", xsd);
                for (String name : XQuery.mapResult(namedSimple, "name")) {
                    collector.addError(ASSERTION_ID_CONCEPT, "Simple type must be in concept schema",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Type '" + name + "'");
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_CONCEPT);
        }
    }

    public static void checkServiceElementDefinition(String xsd, AnalysisInformationCollector collector) {
        try {
            String tns = XsdUtil.getTargetNamespace(xsd);
            if (!XsdUtil.isConcept(tns)) { // only service schemas are checked
                String topLevel = XQuery.runXQuery(Paths.get("xsd"), "illegalElementDefinitionTopLevel.xq", xsd);
                for (Map<String,String> item : XQuery.mapResult(topLevel, "name", "message")) {
                    collector.addError(ASSERTION_ID_ELEMENT_DEFINITION, "Illegal element definition",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Element '" + item.get("name") + "': "
                                    + item.get("message"));
                }
                String embedded = XQuery.runXQuery(Paths.get("xsd"), "illegalElementDefinitionEmbedded.xq", xsd);
                for (Map<String,String> item : XQuery.mapResult(embedded, "node", "element", "message")) {
                    collector.addError(ASSERTION_ID_ELEMENT_DEFINITION, "Illegal element definition",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Node '" + item.get("node") +
                                    "', element '" + item.get("element") + "': " + item.get("message"));
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_ELEMENT_DEFINITION);
        }
    }

    public static void checkRedefinition(String xsd, AnalysisInformationCollector collector) {
        try {
            String res = XQuery.runXQuery(Paths.get("xsd"), "redefine.xq", xsd);
            for (String redefined : XQuery.mapResult(res, "location")) {
                collector.addError(ASSERTION_ID_REDEFINITION, "Illegal redefinition",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "schemaLocation '" + redefined + "'");
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_REDEFINITION);
        }
    }

    public static void checkSchemaUse(String xsd, AnalysisInformationCollector collector) {
        // service schemas must not import other service schemas
        try {
            String tns = XsdUtil.getTargetNamespace(xsd);
            if (!XsdUtil.isConcept(tns)) { // only service schemas are checked
                String res = XQuery.runXQuery(Paths.get("xsd"), "importedNamespaces.xq", xsd);
                for (String namespace : XQuery.mapResult(res, "namespace")) {
                    if (!XsdUtil.isConcept(namespace) && !namespace.equals(tns)
                            && !namespace.matches("^http://simpletype.schemas.nykreditnet.net/.*")
                            && !namespace.matches("^http://technical.schemas.nykreditnet.net/.*")) {
                        // handle special case for process schemas where concepts are not defined in a concept namespace
                        if (tns.matches("^http://process.schemas.nykreditnet.net/process/.*")) {
                            if (!namespace.matches("^http://process.schemas.nykreditnet.net/process/.*")) {
                                collector.addError(ASSERTION_ID_SCHEMA_USE, "Illegal process namespace usage",
                                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Imported namespace '" +
                                                namespace + "'");
                            }
                        } else {
                            collector.addError(ASSERTION_ID_SCHEMA_USE, "Illegal namespace usage",
                                    AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Imported namespace '" +
                                            namespace + "'");
                        }

                    }
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_SCHEMA_USE);
        }
    }

    public static void checkTargetNamespaceVersion(String xsd, AnalysisInformationCollector collector) {
        try {
            String tns = XsdUtil.getTargetNamespace(xsd);
            if (XsdUtil.isInternalNamespace(tns)) {
                if (!tns.matches(".*/v[1-9][0-9]*$")) {
                    collector.addError(ASSERTION_ID_SCHEMA_TNS_VERSION, "Target namespace must end with version (1..)",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Target namespace '" + tns + "'");
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_SCHEMA_TNS_VERSION);
        }
    }

    public static void checkTargetNamespaceCase(String xsd, AnalysisInformationCollector collector) {
        try {
            String tns = XsdUtil.getTargetNamespace(xsd);
            if (!tns.matches(tns.toLowerCase())) {
                collector.addError(ASSERTION_ID_SCHEMA_TNS_CASE, "Target namespace must be lower case",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Target namespace '" + tns + "'");
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_SCHEMA_TNS_CASE);
        }
    }

    public static void checkTargetNamespacePrefix(String xsd, AnalysisInformationCollector collector) {
        String prefix = "http://";
        try {
            String tns = XsdUtil.getTargetNamespace(xsd);
            if (!tns.startsWith(prefix)) {
                collector.addError(ASSERTION_ID_SCHEMA_TNS_PREFIX, "Target namespace must start with " + prefix,
                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Target namespace '" + tns + "'");
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_SCHEMA_TNS_PREFIX);
        }
    }

    public static void checkTargetNamespaceCharacters(String xsd, AnalysisInformationCollector collector) {
        String prefix = "http://";
        try {
            String tns = XsdUtil.getTargetNamespace(xsd);
            String ns = tns.toLowerCase().replace(prefix, "");
            if (!ns.matches("[a-z\\/0-9\\.]*")) {
                collector.addError(ASSERTION_ID_SCHEMA_TNS_CHARS, "Target namespace contains illegal characters",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Target namespace '" + tns + "'");
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_SCHEMA_TNS_CHARS);
        }
    }

    public static void checkAnyType(String xsd, AnalysisInformationCollector collector) {
        /* the exceptions to the no xsd:anyType rule */
        String[] exceptions = {
                "element:WorkflowData:http://service.schemas.nykreditnet.net/2830/documentpreprocessor/v1",
                "element:WorkflowData:http://service.schemas.nykreditnet.net/di/documentprocessor/v1",
                "element:Payload:http://service.schemas.nykreditnet.net/customer/case/concept/task/v1",
                "complexType:InactiveType:http://service.schemas.nykreditnet.net/enterprise/department/concept/department/v1",
                "complexType:InactiveType:http://service.schemas.nykreditnet.net/enterprise/worker/concept/worker/v1"
        };
        try {
            String res = XQuery.runXQuery(Paths.get("xsd"), "anyType.xq", xsd);
            List<Map<String,String>> items = XQuery.mapResult(res, "name", "node");
            if (items.size() > 0) {
                String tns = XsdUtil.getTargetNamespace(xsd);
                List<String> except = Arrays.asList(exceptions);
                for (Map<String,String> item : items) {
                    String name = item.get("name");
                    String node = item.get("node");
                    if (!except.contains(node + ":" + name + ":" + tns)) {
                        collector.addError(ASSERTION_ID_ANY_TYPE, "Illegal anyType",
                                AnalysisInformationCollector.SEVERITY_LEVEL_CRITICAL, node + " " + name +
                                        " namespace '" + tns + "'");
                    } else {
                        /* report usage even though it is permitted */
                        collector.addInfo(ASSERTION_ID_ANY_TYPE, "Use of anyType (permitted)",
                                AnalysisInformationCollector.SEVERITY_LEVEL_CRITICAL, node + " " + name +
                                        " namespace '" + tns + "'");
                    }
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_ANY_TYPE);
        }
    }

    public static void checkAny(String xsd, AnalysisInformationCollector collector) {
        /* the exceptions to the no xsd:any rule */
        String[] exceptions = {
                "element:DokumentData:http://service.schemas.nykreditnet.net/di/dokumentdannelse/v1"
        };
        try {
            String res = XQuery.runXQuery(Paths.get("xsd"), "any.xq", xsd);
            List<Map<String,String>> items = XQuery.mapResult(res, "name", "node");
            if (items.size() > 0) {
                String tns = XsdUtil.getTargetNamespace(xsd);
                List<String> except = Arrays.asList(exceptions);
                for (Map<String,String> item : items) {
                    String name = item.get("name");
                    String node = item.get("node");
                    if (!except.contains(node + ":" + name + ":" + tns)) {
                        collector.addError(ASSERTION_ID_ANY, "Illegal any",
                                AnalysisInformationCollector.SEVERITY_LEVEL_CRITICAL, node + " " + name +
                                        " namespace '" + tns + "'");
                    } else {
                        /* report usage even though it is permitted */
                        collector.addInfo(ASSERTION_ID_ANY, "Use of any (permitted)",
                                AnalysisInformationCollector.SEVERITY_LEVEL_CRITICAL, node + " " + name +
                                        " namespace '" + tns + "'");
                    }
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_ANY);
        }
    }

    public static void checkAnyAttribute(String xsd, AnalysisInformationCollector collector) {
        /* the exceptions to the no xsd:anyAttribute rule outside technical namespaces */
        String[] exceptions = {
                // currently no exceptions
        };
        try {
            String tns = XsdUtil.getTargetNamespace(xsd); // allowed in technical namespaces
            if (!XsdUtil.isTechnicalNamespace(tns)) {
                String res = XQuery.runXQuery(Paths.get("xsd"), "anyAttribute.xq", xsd);
                List<Map<String,String>> items = XQuery.mapResult(res, "name", "node");
                if (items.size() > 0) {
                    List<String> except = Arrays.asList(exceptions);
                    for (Map<String,String> item : items) {
                        String name = item.get("name");
                        String node = item.get("node");
                        if (!except.contains(node + ":" + name + ":" + tns)) {
                            collector.addError(ASSERTION_ID_ANY_ATTRIBUTE, "Illegal anyAttribute",
                                    AnalysisInformationCollector.SEVERITY_LEVEL_CRITICAL, node + " " + name +
                                            " namespace '" + tns + "'");
                        } else {
                        /* report usage even though it is permitted */
                            collector.addInfo(ASSERTION_ID_ANY_ATTRIBUTE, "Use of anyAttribute (permitted)",
                                    AnalysisInformationCollector.SEVERITY_LEVEL_CRITICAL, node + " " + name +
                                            " namespace '" + tns + "'");
                        }
                    }
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_ANY_ATTRIBUTE);
        }
    }

    public static void checkIdenticalElementNames(String xsd, AnalysisInformationCollector collector) {
        try {
            String res = XQuery.runXQuery(Paths.get("xsd"), "identicalElementNames.xq", xsd);
            for (Map<String,String> item : XQuery.mapResult(res, "name", "node", "local-node", "repeated")) {
                String name = item.get("name");
                String node = item.get("node");
                String localNode = item.get("local-node");
                String repeated = item.get("repeated");
                collector.addError(ASSERTION_ID_IDENTICAL_ELEMENTS, "Illegal repeated element name",
                        AnalysisInformationCollector.SEVERITY_LEVEL_CRITICAL, node + " " + name +
                                " child node '" + localNode + "', repeated [" + repeated + "]");
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_IDENTICAL_ELEMENTS);
        }
    }

    public static void checkImportAndIncludeLocation(String xsd, AnalysisInformationCollector collector) {
        try {
            String res = XQuery.runXQuery(Paths.get("xsd"), "importIncludeLocation.xq", xsd);
            for (Map<String,String> item : XQuery.mapResult(res, "type", "location", "namespace")) {
                String type = item.get("type");
                String location = "" + item.get("location");
                String namespace = item.get("namespace");
                if (!location.startsWith("http://")) {
                    if ("import".equals(type)) {
                        collector.addError(ASSERTION_ID_IMPORT_INCLUDE_LOCATION, "Illegal schema location",
                                AnalysisInformationCollector.SEVERITY_LEVEL_CRITICAL, "Import location '" + location +
                                        "', namespace '" + namespace + "'");
                    } else {
                        collector.addError(ASSERTION_ID_IMPORT_INCLUDE_LOCATION, "Illegal schema location",
                                AnalysisInformationCollector.SEVERITY_LEVEL_CRITICAL, "Include location '" +
                                        location + "'");
                    }
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_IMPORT_INCLUDE_LOCATION);
        }
    }

    public static void checkSchemaFilename(String filename, AnalysisInformationCollector collector) {
        String baseName = FilenameUtils.getBaseName(filename);
        String extension = FilenameUtils.getExtension(filename);
        String name = FilenameUtils.getName(filename);
        if (!filename.endsWith(".xsd")) {
            collector.addError(ASSERTION_ID_SCHEMA_FILENAME, "Illegal filename extension. Must be .xsd",
                    AnalysisInformationCollector.SEVERITY_LEVEL_MINOR, "Filename '" + name +
                            "', extension '" + extension + "'");
        }
        if (!Utilities.isUpperCamelCaseAscii(baseName)) {
            collector.addError(ASSERTION_ID_SCHEMA_FILENAME, "Illegal filename. Must be upper camel case ascii",
                    AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Filename '" + name +
                            "', extension '" + extension + "'");
        }
    }

    public static void checkEnterpriseConceptNamespace(String xsd, String filename,
                                                       AnalysisInformationCollector collector) {
        // namespace pattern: http://concept.schemas.nykreditnet.net/<domain>/[<sublevels>]/<concept>/<version>
        String prefix = "http://concept.schemas.nykreditnet.net/";
        try {
            String tns = XsdUtil.getTargetNamespace(xsd);
            if (XsdUtil.isEnterpriseConcept(tns)) {
                String[] parts = tns.replace(prefix, "").split("/");
                if (parts.length >= 2) {
                    String basename = FilenameUtils.getBaseName(filename);
                    String concept = parts[parts.length - 2];
                    if (!basename.toLowerCase().matches(concept)) {
                        collector.addError(ASSERTION_ID_ENTERPRISE_CONCEPT_NAMESPACE, "Illegal concept namespace",
                                AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Concept '" + concept +
                                        "' does not match filename '" + basename.toLowerCase() + "'");
                    }
                    if (parts.length < 3) {
                        // context part should contain at least <domain>/<concept>/<version>
                        collector.addWarning(ASSERTION_ID_ENTERPRISE_CONCEPT_NAMESPACE, "Illegal concept namespace",
                                AnalysisInformationCollector.SEVERITY_LEVEL_MINOR, "Namespace '" + tns +
                                        "' does not match " + prefix + "<domain>/<concept>/<version>");
                    }
                } else {
                    // unable to find concept from namespace
                    collector.addError(ASSERTION_ID_ENTERPRISE_CONCEPT_NAMESPACE, "Illegal concept namespace",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Concept part or namespace '" + tns +
                                    "' not found");
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_ENTERPRISE_CONCEPT_NAMESPACE);
        }
    }

    public static void checkServiceConceptNamespace(String xsd, String filename,
                                                    AnalysisInformationCollector collector) {
        // namespace pattern:
        // http://service.schemas.nykreditnet.net/<domain>/<service>/concept/[<sublevels>]/<concept>/<version>
        String prefix = "http://service.schemas.nykreditnet.net/";
        try {
            String tns = XsdUtil.getTargetNamespace(xsd);
            if (XsdUtil.isServiceConcept(tns)) {
                String[] parts = tns.replace(prefix, "").split("/");
                if (parts.length >= 2) {
                    String basename = FilenameUtils.getBaseName(filename);
                    String concept = parts[parts.length - 2];
                    if (!basename.toLowerCase().matches(concept)) {
                        collector.addError(ASSERTION_ID_SERVICE_CONCEPT_NAMESPACE, "Illegal service concept namespace",
                                AnalysisInformationCollector.SEVERITY_LEVEL_MINOR, "Concept '" + concept +
                                        "' does not match filename '" + basename.toLowerCase() + "'");
                    }
                    if (parts.length < 5) {
                        // context part should contain at least
                        // <domain>/<service>/concept/<concept>/<version>
                        collector.addWarning(ASSERTION_ID_SERVICE_CONCEPT_NAMESPACE, "Illegal service concept namespace",
                                AnalysisInformationCollector.SEVERITY_LEVEL_MINOR, "Namespace '" + tns +
                                        "' does not match " + prefix  +
                                        "<domain>/<service>/concept/<concept>/<version>");
                    }
                } else {
                    // unable to find concept from namespace
                    collector.addError(ASSERTION_ID_SERVICE_CONCEPT_NAMESPACE, "Illegal service concept namespace",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MINOR, "Concept part or namespace '" + tns +
                                    "' not found");
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_SERVICE_CONCEPT_NAMESPACE);
        }
    }

    public static void checkDeprecated(String xsd, AnalysisInformationCollector collector) {
        try {
            String res = XQuery.runXQuery(Paths.get("xsd"), "deprecated.xq", xsd);
            String ns = XQuery.mapSingleResult(res, "namespace");
            if (ns.length() > 0) {
                collector.addError(ASSERTION_ID_DEPRECATED, "Deprecated schema",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MINOR, "Namespace '" + ns + "'");
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_DEPRECATED);
        }
    }

    /**
     * Check for unused namespace prefixes in xsd or wsdl
     * @param xsd this can be either wsdl or xsd data
     * @param collector
     */
    public static void checkUnusedNamespacePrefix(String xsd, AnalysisInformationCollector collector) {
        try {
            String res = XQuery.runXQuery(Paths.get("shared"), "unusedPrefix.xq", xsd);
            for (Map<String,String> item : XQuery.mapResult(res, "prefix", "namespace")) {
                String prefix = item.get("prefix");
                String namespace = item.get("namespace");
                if ("tns".equals(prefix)) {
                    collector.addWarning(ASSERTION_ID_UNUSED_NS_PREFIX, "Unused namespace prefix",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MINOR, "Prefix '" + prefix + "', namespace '" +
                                    namespace + "'");
                } else {
                    collector.addError(ASSERTION_ID_UNUSED_NS_PREFIX, "Unused namespace prefix",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MINOR, "Prefix '" + prefix + "', namespace '" +
                                   namespace + "'");
                }
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_UNUSED_NS_PREFIX);
        }
    }

    /**
     * Check for unused namespace import in xsd or wsdl
     * @param xsd this can be either wsdl or xsd data
     * @param collector
     */
    public static void checkUnusedImport(String xsd, AnalysisInformationCollector collector) {
        try {
            String res = XQuery.runXQuery(Paths.get("shared"), "unusedImport.xq", xsd);
            for (String namespace : XQuery.mapResult(res, "namespace")) {
                collector.addError(ASSERTION_ID_UNUSED_IMPORT, "Unused import",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Namespace '" + namespace + "'");
            }

        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_UNUSED_IMPORT);
        }
    }

    public static void checkInclude(String xsd, String baseName, String domain,
                                     AnalysisInformationCollector collector) {
        try {
            if (!baseName.endsWith("Include")) {
                collector.addWarning(ASSERTION_ID_INCLUDE, "Filename must end with 'Include' (case sensitive)",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "File basename '" + baseName + "'");
            }
            String nakedName = baseName.substring(0, baseName.length() - "include".length());
            String tns = XsdUtil.getTargetNamespace(xsd);
            if (XsdUtil.isConcept(tns)) {
                // illegal use of include
                collector.addError(ASSERTION_ID_INCLUDE, "Include schema only legal in service schemas",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Namespace '" + tns + "'");
            } else {
                collector.addWarning(ASSERTION_ID_INCLUDE, "Include schemas should be avoided",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR);
            }
            String[] parts = tns.replace("http://" + domain + "/", "").split("/");
            if (parts.length >= 2) {
                String name = parts[parts.length - 2];
                if (!name.equals(nakedName.toLowerCase())) {
                    collector.addError(ASSERTION_ID_INCLUDE, "Include name does not match name element in namespace",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Name '" + name + "', include name '"
                                    + nakedName.toLowerCase() + "' ('Include' removed, converted to lowercase)");
                }
            } else {
                collector.addError(ASSERTION_ID_INCLUDE, "Could not match filename to namespace",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Namespace should end with '/"
                                + nakedName.toLowerCase() + "/v[\\d]+'");
            }
            // check unused namespaces, only 'tns' is allowed
            String res = XQuery.runXQuery(Paths.get("shared"), "unusedPrefix.xq", xsd);
            for (Map<String,String> item : XQuery.mapResult(res, "prefix", "namespace")) {
                String prefix = item.get("prefix");
                String namespace = item.get("namespace");
                if (!"tns".equals(prefix)) {
                    collector.addError(ASSERTION_ID_UNUSED_NS_PREFIX, "Unused namespace prefix",
                            AnalysisInformationCollector.SEVERITY_LEVEL_MINOR, "Prefix '" + prefix + "', namespace '" +
                                    namespace + "'");
                }
            }
            // check that only include nodes are present
            String topLevel = XQuery.runXQuery(Paths.get("xsd"), "topLevelElementsAndTypes.xq", xsd);
            List<Map<String,String>> items = items = XQuery.mapResult(topLevel, "name", "node");
            for (Map<String,String> item : items) {
                String name = item.get("name");
                String node = item.get("node");
                collector.addError(ASSERTION_ID_INCLUDE, "Illegal contents in include file",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Node name '" + name + "' (" + node + ")");
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_INCLUDE);
        }
    }

    private static void checkPathAndTargetNamespace(String xsd, String path, AnalysisInformationCollector collector) {
        try {
            String tns = XsdUtil.getTargetNamespace(xsd);
            if (!path.equals(tns)) {
                collector.addError(ASSERTION_ID_NAMESPACE_MATCH_PATH, "Target namespace must match path",
                        AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, "Target namespace '" + tns + "', path '" +
                                path + "'");
            }
        } catch (Exception e) {
            collectException(e, collector, ASSERTION_ID_NAMESPACE_MATCH_PATH);
        }
    }

    public static void checkPathAndTargetNamespace(String xsd, String domain, Path relativePath,
                                                   AnalysisInformationCollector collector) {
        checkPathAndTargetNamespace(xsd, Utilities.pathToNamespace(domain, relativePath), collector);
    }

    public static void checkPathAndTargetNamespace(String xsd, URL url, AnalysisInformationCollector collector) {
        checkPathAndTargetNamespace(xsd, url.toString(), collector);

    }

    private static void collectException(Exception e, AnalysisInformationCollector collector, String assertion) {
        collector.addInfo(assertion, "Exception while checking schema",
                AnalysisInformationCollector.SEVERITY_LEVEL_UNKNOWN, e.getMessage());
    }
}
