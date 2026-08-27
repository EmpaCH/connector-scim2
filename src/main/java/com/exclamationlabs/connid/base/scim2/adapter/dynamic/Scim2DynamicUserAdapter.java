package com.exclamationlabs.connid.base.scim2.adapter.dynamic;

import com.exclamationlabs.connid.base.connector.attribute.ConnectorAttribute;
import com.exclamationlabs.connid.base.connector.attribute.ConnectorAttributeDataType;
import com.exclamationlabs.connid.base.scim2.adapter.Scim2UserAdapter;
import com.exclamationlabs.connid.base.scim2.model.Scim2Schema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;

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
     * Builds the ConnId attribute name for an extension schema attribute.
     *
     * <p>Colons in the URN are replaced with underscores so the name is safe for XML-based
     * frameworks (e.g. MidPoint) that treat colons as namespace separators. The URN and the
     * field path are joined with {@code $} so the boundary is unambiguous.
     *
     * <p>Example: {@code urn:ietf:...:User} + {@code officeLocation}
     *          → {@code urn_ietf_..._User$officeLocation}
     */
    public static String extensionAttrName(String schemaUrn, String fieldPath) {
        return schemaUrn.replace(":", "_") + "$" + fieldPath;
    }

    /**
     * Decodes an extension ConnId attribute name back to the original SCIM2 schema URN.
     * Returns null if {@code attrName} is not an encoded extension attribute.
     */
    public static String extensionSchemaUrn(String attrName) {
        if (!attrName.startsWith("urn_")) return null;
        int idx = attrName.indexOf('$');
        return (idx > 4 && idx < attrName.length() - 1) ? attrName.substring(0, idx) : null;
    }

    /**
     * Returns the field path portion of an encoded extension attribute name (after the {@code $}).
     * Returns null if {@code attrName} is not an encoded extension attribute.
     */
    public static String extensionFieldPath(String attrName) {
        if (!attrName.startsWith("urn_")) return null;
        int idx = attrName.indexOf('$');
        return (idx > 4 && idx < attrName.length() - 1) ? attrName.substring(idx + 1) : null;
    }

    /**
     * Converts an encoded extension schema URN (underscores) back to the original SCIM2 URN
     * (colons). SCIM2 URNs do not contain underscores, so the mapping is unambiguous.
     */
    public static String decodeExtensionUrn(String encodedUrn) {
        if (encodedUrn == null) return null;
        return encodedUrn.replace("_", ":");
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
                            schemaUrn + ":" + fieldPath,
                            sub.type, buildFlags(sub));
                    if (ca != null) attributeInfos.add(ca);
                }
            } else {
                ConnectorAttribute ca = buildExtensionConnectorAttribute(
                        extensionAttrName(schemaUrn, schemaAttr.name),
                        schemaUrn + ":" + schemaAttr.name,
                        schemaAttr.type, buildFlags(schemaAttr));
                if (ca != null) attributeInfos.add(ca);
            }
        }
    }

    private ConnectorAttribute buildExtensionConnectorAttribute(
            String connIdName, String nativeName, String type, Set<AttributeInfo.Flags> flags) {
        if (type == null) return null;
        switch (type.toLowerCase()) {
            case "boolean":
                return new ConnectorAttribute(connIdName, nativeName, ConnectorAttributeDataType.BOOLEAN, flags);
            case "integer":
                return new ConnectorAttribute(connIdName, nativeName, ConnectorAttributeDataType.INTEGER, flags);
            case "decimal":
                return new ConnectorAttribute(connIdName, nativeName, ConnectorAttributeDataType.BIG_DECIMAL, flags);
            case "datetime":
                return new ConnectorAttribute(connIdName, nativeName, ConnectorAttributeDataType.ZONED_DATE_TIME, flags);
            default:
                return new ConnectorAttribute(connIdName, nativeName, ConnectorAttributeDataType.STRING, flags);
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

            String attrType = schemaAttr.type != null ? schemaAttr.type : "string";
            if (attrType.equalsIgnoreCase("string") || attrType.equalsIgnoreCase("complex")) {
                builder1 = new ConnectorAttribute(fullPath, ConnectorAttributeDataType.STRING, buildFlags(schemaAttr));
            } else if (attrType.equalsIgnoreCase("boolean")) {
                builder1 = new ConnectorAttribute(fullPath, ConnectorAttributeDataType.BOOLEAN, buildFlags(schemaAttr));
            } else if (attrType.equalsIgnoreCase("decimal")) {
                builder1 = new ConnectorAttribute(fullPath, ConnectorAttributeDataType.BIG_DECIMAL, buildFlags(schemaAttr));
            } else if (attrType.equalsIgnoreCase("integer")) {
                builder1 = new ConnectorAttribute(fullPath, ConnectorAttributeDataType.INTEGER, buildFlags(schemaAttr));
            } else if (attrType.equalsIgnoreCase("datetime")) {
                builder1 = new ConnectorAttribute(fullPath, ConnectorAttributeDataType.ZONED_DATE_TIME, buildFlags(schemaAttr));
            } else {
                builder1 = new ConnectorAttribute(fullPath, ConnectorAttributeDataType.STRING, buildFlags(schemaAttr));
            }

            // Complex attributes are kept opaque (flat String); sub-attributes are not expanded.
            attributeInfos.add(builder1);
        }
    }

    public static Set<AttributeInfo.Flags> buildFlags(
            com.exclamationlabs.connid.base.scim2.model.Attribute attribute) {
        return buildModelFlags(
                attribute.getMultiValued(), attribute.getRequired(),
                attribute.getMutability(), attribute.getReturned());
    }

    public static Set<AttributeInfo.Flags> buildFlags(
            com.exclamationlabs.connid.base.scim2.model.SubAttribute attribute) {
        return buildModelFlags(
                attribute.getMultiValued(), attribute.getRequired(),
                attribute.getMutability(), attribute.getReturned());
    }

    private static Set<AttributeInfo.Flags> buildModelFlags(
            Boolean multiValued, Boolean required, String mutability, String returned) {
        Set<AttributeInfo.Flags> flags = new HashSet<>();
        if (Boolean.TRUE.equals(multiValued)) flags.add(AttributeInfo.Flags.MULTIVALUED);
        if (Boolean.TRUE.equals(required))    flags.add(AttributeInfo.Flags.REQUIRED);
        String mut = mutability != null ? mutability : "";
        switch (mut.toLowerCase()) {
            case "readonly":
                flags.add(AttributeInfo.Flags.NOT_CREATABLE);
                flags.add(AttributeInfo.Flags.NOT_UPDATEABLE);
                break;
            case "immutable":
                flags.add(AttributeInfo.Flags.NOT_UPDATEABLE);
                break;
            case "writeonly":
                flags.add(AttributeInfo.Flags.NOT_READABLE);
                break;
        }
        String ret = returned != null ? returned : "";
        switch (ret.toLowerCase()) {
            case "never":   flags.add(AttributeInfo.Flags.NOT_READABLE); break;
            case "request": flags.add(AttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT); break;
        }
        return flags;
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
        if (multiValued) flagsSet.add(AttributeInfo.Flags.MULTIVALUED);
        if (required)    flagsSet.add(AttributeInfo.Flags.REQUIRED);

        switch (mutability.toLowerCase()) {
            case "readonly":
                // Server-managed: must not be sent on create or update (RFC 7644 §2.2)
                flagsSet.add(AttributeInfo.Flags.NOT_CREATABLE);
                flagsSet.add(AttributeInfo.Flags.NOT_UPDATEABLE);
                break;
            case "immutable":
                // Can be set at create-time but not changed afterwards
                flagsSet.add(AttributeInfo.Flags.NOT_UPDATEABLE);
                break;
            case "writeonly":
                flagsSet.add(AttributeInfo.Flags.NOT_READABLE);
                break;
            // "readwrite" (default) — no extra flags
        }

        switch (returned.toLowerCase()) {
            case "never":
                // Attribute is never returned by the server
                flagsSet.add(AttributeInfo.Flags.NOT_READABLE);
                break;
            case "request":
                // Returned only when explicitly requested
                flagsSet.add(AttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT);
                break;
            // "always" / "default" — no extra flags
        }
        // "uniqueness: server/global" is a server-enforcement hint, not a client restriction
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

        // Wire SCIM 'id' → __UID__ and 'userName' → __NAME__ exactly as the static schema does.
        // Remove any plain registrations of these names that came from addAttributesToInfoSet,
        // then inject the proper ConnId identity attributes.
        attributeInfos.removeIf(a -> "id".equals(a.getName())
                || "userName".equals(a.getName())
                || "name".equals(a.getName()));
        attributeInfos.add(new ConnectorAttribute(
                Uid.NAME, "id", ConnectorAttributeDataType.STRING,
                AttributeInfo.Flags.NOT_UPDATEABLE, AttributeInfo.Flags.REQUIRED));
        attributeInfos.add(new ConnectorAttribute(
                Name.NAME, "userName", ConnectorAttributeDataType.STRING,
                AttributeInfo.Flags.REQUIRED));
        // SCIM 'name' (complex Name object) serialized as opaque JSON; renamed to avoid
        // clash with ConnId __NAME__ which MidPoint renders as "name" in the schema UI.
        attributeInfos.add(new ConnectorAttribute(
                "scim_name", "name", ConnectorAttributeDataType.STRING));

        return attributeInfos;
    }
}
