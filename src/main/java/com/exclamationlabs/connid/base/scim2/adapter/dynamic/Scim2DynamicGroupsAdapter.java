package com.exclamationlabs.connid.base.scim2.adapter.dynamic;

import com.exclamationlabs.connid.base.connector.adapter.AdapterValueTypeConverter;
import com.exclamationlabs.connid.base.connector.adapter.BaseAdapter;
import com.exclamationlabs.connid.base.connector.attribute.ConnectorAttribute;
import com.exclamationlabs.connid.base.connector.attribute.ConnectorAttributeDataType;
import com.exclamationlabs.connid.base.scim2.configuration.Scim2Configuration;
import com.exclamationlabs.connid.base.scim2.model.Scim2Group;
import com.exclamationlabs.connid.base.scim2.model.Scim2Schema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ObjectClass;

/**
 * Dynamic Groups Adapter
 */
public class Scim2DynamicGroupsAdapter extends BaseAdapter<Scim2Group, Scim2Configuration> {

  @Override
  public ObjectClass getType() {
    return ObjectClass.GROUP;
  }

  @Override
  public Class<Scim2Group> getIdentityModelClass() {
    return Scim2Group.class;
  }

  public String getConfig() {
    return config;
  }

  public void setConfig(String config) {
    this.config = config;
  }

  String config;

  @Override
  public Set<ConnectorAttribute> getConnectorAttributes() {
    String rawJson = getConfig();

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    List<Scim2Schema> schemaPojo;
    try {
      schemaPojo = objectMapper.readValue(rawJson, new TypeReference<List<Scim2Schema>>() {});
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Set<ConnectorAttribute> attributeInfos = new HashSet<>();
    schemaPojo.forEach(
        obj -> {
          if (obj.getId().equalsIgnoreCase("urn:ietf:params:scim:schemas:core:2.0:Group")) {
            List<Scim2Schema.Attribute> userAttributes = obj.getAttributes();
            addAttributesToInfoSet(attributeInfos, userAttributes, "");
          }
        });
    attributeInfos.removeIf(Objects::isNull);
    return attributeInfos;
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

  Set<AttributeInfo.Flags> buildFlags(Scim2Schema.Attribute attribute) {
    return getFlags(
        attribute.multiValued,
        attribute.required,
        attribute.caseExact,
        attribute.mutability,
        attribute.returned,
        attribute.uniqueness);
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
        flagsSet.add(AttributeInfo.Flags.NOT_CREATABLE);
        flagsSet.add(AttributeInfo.Flags.NOT_UPDATEABLE);
        break;
      case "immutable":
        flagsSet.add(AttributeInfo.Flags.NOT_UPDATEABLE);
        break;
      case "writeonly":
        flagsSet.add(AttributeInfo.Flags.NOT_READABLE);
        break;
    }

    switch (returned.toLowerCase()) {
      case "never":   flagsSet.add(AttributeInfo.Flags.NOT_READABLE); break;
      case "request": flagsSet.add(AttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT); break;
    }
    // "uniqueness: server/global" is server-enforcement only, not a client restriction
  }

  @Override
  protected Set<Attribute> constructAttributes(Scim2Group group) {
    Set<Attribute> attributes = new HashSet<>();
    return attributes;
  }

  @Override
  protected Scim2Group constructModel(
      Set<Attribute> attributes,
      Set<Attribute> addedMultiValueAttributes,
      Set<Attribute> removedMultiValueAttributes,
      boolean isCreate) {

    Scim2Group group = new Scim2Group();
    group.setId(AdapterValueTypeConverter.getIdentityIdAttributeValue(attributes));
    return group;
  }
}
