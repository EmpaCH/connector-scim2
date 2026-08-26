package com.exclamationlabs.connid.base.scim2.adapter.dynamic;

import com.exclamationlabs.connid.base.connector.attribute.ConnectorAttribute;
import com.exclamationlabs.connid.base.connector.attribute.ConnectorAttributeDataType;
import com.exclamationlabs.connid.base.scim2.adapter.Scim2UserAdapter;
import com.exclamationlabs.connid.base.scim2.model.Scim2Schema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.identityconnectors.framework.common.objects.AttributeInfo;

import java.io.IOException;
import java.util.*;

public class Scim2DynamicUserAdapter extends Scim2UserAdapter {
    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    String config;

    /**
     * Builds the ConnId attribute name for an extension schema attribute following the SCIM2
     * extended-attribute filter notation (RFC 7644 §3.4.2.2):
     * {@code <schemaURN>:<attributePath>}
     *
     * <p>Sub-attributes use dots: {@code <schemaURN>:<complexAttr>.<subAttr>}.
     * Parsing splits on the LAST colon, which is safe because SCIM attribute names never
     * contain colons.
     */
    public static String extensionAttrName(String schemaUrn, String fieldPath) {
        return schemaUrn + ":" + fieldPath;
    }

    /**
     * Extracts the schema URN from an extension attribute name (everything before the last colon).
     * Returns null if the name is not an extension attribute (i.e. does not start with "urn:").
     */
    public static String extensionSchemaUrn(String attrName) {
        if (!attrName.startsWith("urn:")) return null;
        int idx = attrName.lastIndexOf(':');
        // must have something after the last colon and a URN with at least one segment before it
        return (idx > 4 && idx < attrName.length() - 1) ? attrName.substring(0, idx) : null;
    }

    /**
     * Extracts the field path from an extension attribute name (everything after the last colon).
     * Returns null if the name is not an extension attribute.
     */
    public static String extensionFieldPath(String attrName) {
        if (!attrName.startsWith("urn:")) return null;
        int idx = attrName.lastIndexOf(':');
        return (idx > 4 && idx < attrName.length() - 1) ? attrName.substring(idx + 1) : null;
    }

    private void addExtensionAttributesToInfoSet(
            Set<ConnectorAttribute> attributeInfos,
            List<Scim2Schema.Attribute> schemaAttributes,
            String schemaUrn,
            List<Scim2Schema> allSchemas) {
        for (Scim2Schema.Attribute schemaAttr : schemaAttributes) {
            if (schemaAttr.name == null) continue;
            // Some servers (e.g. sGuard/Entra) embed a nested extension URN as an attribute
            // name — either inline with subAttributes, or as a leaf reference to another schema.
            if (schemaAttr.name.startsWith("urn:")) {
                if (schemaAttr.subAttributes != null && !schemaAttr.subAttributes.isEmpty()) {
                    // Inline nested extension with its own sub-attributes
                    addExtensionAttributesToInfoSet(attributeInfos, schemaAttr.subAttributes,
                            schemaAttr.name, allSchemas);
                } else {
                    // Leaf reference: look up the schema by URN in the full list
                    allSchemas.stream()
                            .filter(s -> schemaAttr.name.equals(s.getId()))
                            .filter(s -> s.getAttributes() != null && !s.getAttributes().isEmpty())
                            .findFirst()
                            .ifPresent(s -> addExtensionAttributesToInfoSet(
                                    attributeInfos, s.getAttributes(), s.getId(), allSchemas));
                }
                continue;
            }
            if (schemaAttr.subAttributes != null && !schemaAttr.subAttributes.isEmpty()) {
                for (Scim2Schema.Attribute sub : schemaAttr.subAttributes) {
                    String fieldPath = schemaAttr.name + "." + sub.name;
                    ConnectorAttribute ca = buildExtensionConnectorAttribute(
                            extensionAttrName(schemaUrn, fieldPath),
                            sub.type,
                            buildFlags(sub));
                    if (ca != null) attributeInfos.add(ca);
                }
            } else {
                ConnectorAttribute ca = buildExtensionConnectorAttribute(
                        extensionAttrName(schemaUrn, schemaAttr.name),
                        schemaAttr.type,
                        buildFlags(schemaAttr));
                if (ca != null) attributeInfos.add(ca);
            }
        }
    }

    private ConnectorAttribute buildExtensionConnectorAttribute(
            String fullName, String type, Set<AttributeInfo.Flags> flags) {
        if (type == null) return null;
        switch (type.toLowerCase()) {
            case "boolean":
                return new ConnectorAttribute(fullName, ConnectorAttributeDataType.BOOLEAN, flags);
            case "integer":
                return new ConnectorAttribute(fullName, ConnectorAttributeDataType.INTEGER, flags);
            case "decimal":
                return new ConnectorAttribute(fullName, ConnectorAttributeDataType.BIG_DECIMAL, flags);
            case "datetime":
                return new ConnectorAttribute(fullName, ConnectorAttributeDataType.ZONED_DATE_TIME, flags);
            default:
                return new ConnectorAttribute(fullName, ConnectorAttributeDataType.STRING, flags);
        }
    }

    private void addAttributesToInfoSet(
            Set<ConnectorAttribute> attributeInfos,
            List<Scim2Schema.Attribute> schemaAttributes,
            String parentPath) {
        for (Scim2Schema.Attribute schemaAttr : schemaAttributes) {
            String fullPath = parentPath.isEmpty() ? schemaAttr.name : parentPath + "." + schemaAttr.name;
            // AttributeInfoBuilder builder = new AttributeInfoBuilder(fullPath);

            ConnectorAttribute builder1 = null;

            // builder.setMultiValued(schemaAttr.multiValued);
            // builder.setRequired(schemaAttr.required);

            if (schemaAttr.type.equalsIgnoreCase("string")
                    || schemaAttr.type.equalsIgnoreCase("complex")) {
                // builder.setType(String.class);
                builder1 =
                        new ConnectorAttribute(
                                fullPath, ConnectorAttributeDataType.valueOf("STRING"), buildFlags(schemaAttr));
            } else if (schemaAttr.type.equalsIgnoreCase("boolean")) {
                // builder.setType(Boolean.class);
                builder1 =
                        new ConnectorAttribute(
                                fullPath, ConnectorAttributeDataType.valueOf("BOOLEAN"), buildFlags(schemaAttr));
            } else if (schemaAttr.type.equalsIgnoreCase("decimal")) {
                // builder.setType(Double.class);
                builder1 =
                        new ConnectorAttribute(
                                fullPath,
                                ConnectorAttributeDataType.valueOf("BIG_DECIMAL"),
                                buildFlags(schemaAttr));
            } else if (schemaAttr.type.equalsIgnoreCase("integer")) {
                // builder.setType(Integer.class);
                builder1 =
                        new ConnectorAttribute(
                                fullPath, ConnectorAttributeDataType.valueOf("INTEGER"), buildFlags(schemaAttr));
            } else if (schemaAttr.type.equalsIgnoreCase("datetime")) {
                // builder.setType(Long.class); // Typically UNIX timestamp
                builder1 =
                        new ConnectorAttribute(
                                fullPath,
                                ConnectorAttributeDataType.valueOf("ZONED_DATE_TIME"),
                                buildFlags(schemaAttr));
            }

            if (schemaAttr.subAttributes != null && !schemaAttr.subAttributes.isEmpty()) {
                addAttributesToInfoSet(attributeInfos, schemaAttr.subAttributes, fullPath);
            }

            // attributeInfos.add(builder.build());
            attributeInfos.add(builder1);
        }
    }

    public static final Set<AttributeInfo.Flags> buildFlags(
            com.exclamationlabs.connid.base.scim2.model.Attribute attribute) {

        Set<AttributeInfo.Flags> flagsSet = new HashSet<>();
        boolean multiValued = attribute.getMultiValued() != null ? attribute.getMultiValued() : false;
        boolean required = attribute.getRequired() != null ? attribute.getRequired() : false;
        boolean caseExact = attribute.getCaseExact() != null ? attribute.getCaseExact() : false;
        String mutability = attribute.getMutability() != null ? attribute.getMutability() : "";
        String returned = attribute.getReturned() != null ? attribute.getReturned() : "";
        String uniqueness = attribute.getUniqueness() != null ? attribute.getUniqueness() : "";
        if (multiValued) flagsSet.add(AttributeInfo.Flags.valueOf("MULTIVALUED"));

        if (required) flagsSet.add(AttributeInfo.Flags.valueOf("REQUIRED"));

        // if(caseExact)
        //   list.add(AttributeInfo.Subtypes.valueOf("MULTIVALUED"));

        if (mutability.equalsIgnoreCase("readOnly"))
            flagsSet.add(AttributeInfo.Flags.valueOf("NOT_UPDATEABLE"));

        if (mutability.equalsIgnoreCase("writeOnly"))
            flagsSet.add(AttributeInfo.Flags.valueOf("NOT_READABLE"));

        if (returned.equalsIgnoreCase("never"))
            flagsSet.add(AttributeInfo.Flags.valueOf("NOT_RETURNED_BY_DEFAULT"));

        if (uniqueness.equalsIgnoreCase("server"))
            flagsSet.add(AttributeInfo.Flags.valueOf("NOT_CREATABLE"));

        return flagsSet;
    }

    /**
     * Compose connid attributes flags from a SCIM2 sub attribute
     * @param attribute
     * @return Set of connid AttributeInfo Flags
     */
    public static final Set<AttributeInfo.Flags> buildFlags(
            com.exclamationlabs.connid.base.scim2.model.SubAttribute attribute) {

        Set<AttributeInfo.Flags> flagsSet = new HashSet<>();
        boolean multiValued = attribute.getMultiValued() != null ? attribute.getMultiValued() : false;
        boolean required = attribute.getRequired() != null ? attribute.getRequired() : false;
        boolean caseExact = attribute.getCaseExact() != null ? attribute.getCaseExact() : false;
        String mutability = attribute.getMutability() != null ? attribute.getMutability() : "";
        String returned = attribute.getReturned() != null ? attribute.getReturned() : "";
        String uniqueness = attribute.getUniqueness() != null ? attribute.getUniqueness() : "";
        if (multiValued) flagsSet.add(AttributeInfo.Flags.valueOf("MULTIVALUED"));

        if (required) flagsSet.add(AttributeInfo.Flags.valueOf("REQUIRED"));

        // if(caseExact)
        //   list.add(AttributeInfo.Subtypes.valueOf("MULTIVALUED"));

        if (mutability.equalsIgnoreCase("readOnly"))
            flagsSet.add(AttributeInfo.Flags.valueOf("NOT_UPDATEABLE"));

        if (mutability.equalsIgnoreCase("writeOnly"))
            flagsSet.add(AttributeInfo.Flags.valueOf("NOT_READABLE"));

        if (returned.equalsIgnoreCase("never"))
            flagsSet.add(AttributeInfo.Flags.valueOf("NOT_RETURNED_BY_DEFAULT"));

        if (uniqueness.equalsIgnoreCase("server"))
            flagsSet.add(AttributeInfo.Flags.valueOf("NOT_CREATABLE"));

        return flagsSet;
    }

    private Set<AttributeInfo.Flags> getFlags(
            Boolean multiValued,
            Boolean required,
            Boolean caseExact,
            String mutability,
            String returned,
            String uniqueness) {
        Set<AttributeInfo.Flags> flagsSet = new HashSet<>();
        processAttributeFlags(
                flagsSet,
                multiValued != null ? multiValued : false,
                required != null ? required : false,
                caseExact != null ? caseExact : false,
                mutability != null ? mutability : "",
                returned != null ? returned : "",
                uniqueness != null ? uniqueness : "");
        return flagsSet;
    }
    Set<AttributeInfo.Flags> buildFlags(Scim2Schema.Attribute attribute) {
        return getFlags(
                attribute.multiValued,
                attribute.required,
                attribute.caseExact,
                attribute.mutability,
                attribute.returned,
                attribute.uniqueness);
    }
    private void processAttributeFlags(
            Set<AttributeInfo.Flags> flagsSet,
            boolean multiValued,
            boolean required,
            boolean caseExact,
            String mutability,
            String returned,
            String uniqueness) {
        if (multiValued) {
            flagsSet.add(AttributeInfo.Flags.MULTIVALUED);
        }
        if (required) {
            flagsSet.add(AttributeInfo.Flags.REQUIRED);
        }
        // if (caseExact) {
        //     list.add(AttributeInfo.Subtypes.MULTIVALUED);
        // }
        if ("readOnly".equalsIgnoreCase(mutability)) {
            flagsSet.add(AttributeInfo.Flags.NOT_UPDATEABLE);
        }
        if ("writeOnly".equalsIgnoreCase(mutability)) {
            flagsSet.add(AttributeInfo.Flags.NOT_READABLE);
        }
        if ("never".equalsIgnoreCase(returned)) {
            flagsSet.add(AttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT);
        }
        if ("server".equalsIgnoreCase(uniqueness)) {
            flagsSet.add(AttributeInfo.Flags.NOT_CREATABLE);
        }
    }
    @Override
    public Set<ConnectorAttribute> getConnectorAttributes() {
        String rawJson = getConfig();
        System.out.println("RAW JSON ---> " + rawJson);
        ObjectMapper objectMapper = new ObjectMapper();
        List<Scim2Schema> schemaPojo = null;
        Map<String, Object> userMap = new HashMap<>();
        Set<ConnectorAttribute> attributeInfos = new HashSet<>();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            schemaPojo = objectMapper.readValue(rawJson, new TypeReference<List<Scim2Schema>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final List<Scim2Schema> allSchemas = schemaPojo;
        Set<ConnectorAttribute> result = new HashSet<>();
        allSchemas.forEach(
                obj -> {
                    if (obj.getId().equalsIgnoreCase("urn:ietf:params:scim:schemas:core:2.0:User")) {
                        List<Scim2Schema.Attribute> userAttributes = obj.getAttributes();
                        addAttributesToInfoSet(attributeInfos, userAttributes, "");
                    } else if (obj.getId() != null && obj.getId().startsWith("urn:")) {
                        // Extension schema: prefix each attribute with <schemaId>:
                        List<Scim2Schema.Attribute> extAttributes = obj.getAttributes();
                        if (extAttributes != null) {
                            addExtensionAttributesToInfoSet(attributeInfos, extAttributes,
                                    obj.getId(), allSchemas);
                        }
                    }
                });
        attributeInfos.removeIf(Objects::isNull);
        return attributeInfos;
    }
}
