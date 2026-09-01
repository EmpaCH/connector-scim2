package com.exclamationlabs.connid.base.scim2;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.exclamationlabs.connid.base.connector.configuration.ConfigurationReader;
import com.exclamationlabs.connid.base.connector.test.ApiIntegrationTest;
import com.exclamationlabs.connid.base.scim2.configuration.Scim2Configuration;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.test.common.ToListResultsHandler;
import org.junit.jupiter.api.*;

import java.util.*;


/**
 * Integration tests for the sGuard SCIM2 endpoint.
 *
 * sGuard exposes only /Users — Groups are absent from its schema.
 * The connector must be configured with enableGroupsResource=false
 * so that the /Groups endpoint is never called and test() does not
 * check for a Group resource type.
 *
 * Properties file: sguard.properties
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Scim2SGuardApiIntegrationTest
    extends ApiIntegrationTest<Scim2Configuration, Scim2Connector> {

  @Override
  protected Scim2Configuration getConfiguration() {
    return new Scim2Configuration("sguard");
  }

  @Override
  protected Class<Scim2Connector> getConnectorClass() {
    return Scim2Connector.class;
  }

  @Override
  protected void readConfiguration(Scim2Configuration configuration) {
    ConfigurationReader.setupTestConfiguration(configuration);
  }

  @BeforeEach
  public void setup() {
    super.setup();
  }

  @Test
  @Order(10)
  public void test010Schema() {
    Schema schema = getConnectorFacade().schema();
    assertNotNull(schema);
    // User object class must be present
    boolean hasUser = schema.getObjectClassInfo().stream()
        .anyMatch(oc -> oc.getType().equals("Scim2User"));
    assertTrue(hasUser, "Schema must contain Scim2User object class");
  }

  @Test
  @Order(20)
  public void test020GetAllUsers() {
    ToListResultsHandler handler = new ToListResultsHandler();
    getConnectorFacade().search(
        new ObjectClass("Scim2User"),
        null,
        handler,
        new OperationOptionsBuilder().build());
    List<ConnectorObject> users = handler.getObjects();
    assertNotNull(users);
    assertFalse(users.isEmpty(), "sGuard should return at least one user");
  }

  /**
   * Regression test for the "Uid value must not be blank!" error that occurred when
   * Scim2DynamicUserAdapter did not wire SCIM 'id' → __UID__ in the dynamic schema.
   * After the fix the UID returned by create must be non-blank so MidPoint can store
   * the shadow reference and subsequent updates can target the correct resource object.
   */
  private static String createdUid;

  @Test
  @Order(15)
  public void test015CreateUserReturnsNonBlankUid() {
    Set<Attribute> attrs = new HashSet<>();
    attrs.add(new AttributeBuilder().setName(Name.NAME).addValue("mp-test-create@empa.ch").build());
    attrs.add(new AttributeBuilder().setName("externalId").addValue("99999999").build());
    // sGuard requires 'language'; without it the server returns 400 "[4] Anfrage-Parameter nicht gefunden"
    attrs.add(new AttributeBuilder().setName("language").addValue("DE").build());
    // sGuard requires name; sent as opaque JSON under 'scim_name' (renamed to avoid collision with __NAME__)
    attrs.add(new AttributeBuilder().setName("scim_name")
        .addValue("{\"familyName\":\"Test\",\"givenName\":\"MidPoint\"}").build());
    attrs.add(new AttributeBuilder().setName("emails")
        .addValue("{\"value\":\"mp-test-create@empa.ch\",\"type\":\"work\"}").build());
    attrs.add(new AttributeBuilder().setName("phoneNumbers")
        .addValue("{\"value\":\"\",\"type\":\"work\"}").build());

    Uid uid = getConnectorFacade().create(
        new ObjectClass("Scim2User"),
        attrs,
        new OperationOptionsBuilder().build());

    assertNotNull(uid, "create() must return a Uid");
    assertNotNull(uid.getUidValue(), "Uid value must not be null");
    assertFalse(uid.getUidValue().isBlank(), "Uid value must not be blank");
    createdUid = uid.getUidValue();
  }

  @Test
  @Order(16)
  public void test016CreatedUserIsRetrievable() {
    assumeTrue(createdUid != null, "Skipped: create in test015 did not produce a UID");
    ConnectorObject obj = getConnectorFacade().getObject(
        new ObjectClass("Scim2User"),
        new Uid(createdUid),
        new OperationOptionsBuilder().build());
    assertNotNull(obj, "Created user must be retrievable by its server-assigned UID");
    Attribute name = obj.getAttributeByName(Name.NAME);
    assertNotNull(name);
    assertEquals("mp-test-create@empa.ch", name.getValue().get(0));
  }

  @Test
  @Order(17)
  public void test017DeleteCreatedUser() {
    assumeTrue(createdUid != null, "Skipped: create in test015 did not produce a UID");
    assertDoesNotThrow(() ->
        getConnectorFacade().delete(
            new ObjectClass("Scim2User"),
            new Uid(createdUid),
            new OperationOptionsBuilder().build()));
    createdUid = null;
  }

  @Test
  @Order(21)
  public void test021GetUser() {
    ToListResultsHandler handler = new ToListResultsHandler();
    ConnectorObject ob = getConnectorFacade().getObject(
            new ObjectClass("Scim2User"),
            new Uid("1073905"),
            new OperationOptionsBuilder().build());
    assertNotNull(ob);
  }

  @Test
  @Order(25)
  public void test025UpdateUser() {
    Set<AttributeDelta> delta = new HashSet<>();
    delta.add(new AttributeDeltaBuilder()
        .setName(OperationalAttributes.ENABLE_NAME)
        .addValueToReplace(true)
        .build());
    delta.add(new AttributeDeltaBuilder()
        .setName("phoneNumbers")
        .addValueToReplace("{\"value\":\"+41442814141\",\"type\":\"work\",\"primary\":true}")
        .build());
    getConnectorFacade().updateDelta(
        new ObjectClass("Scim2User"),
        new Uid("1073905"),
        delta,
        new OperationOptionsBuilder().build());

    ConnectorObject updated = getConnectorFacade().getObject(
        new ObjectClass("Scim2User"),
        new Uid("1073905"),
        new OperationOptionsBuilder().build());
    assertNotNull(updated, "User must still exist after update");
    Attribute phoneAttr = updated.getAttributeByName("phoneNumbers");
    assertNotNull(phoneAttr, "phoneNumbers attribute must be present after update");
    assertTrue(
        phoneAttr.getValue().stream().anyMatch(v -> v.toString().contains("+41442814141")),
        "Updated phone number must be returned by GET");
  }

  @Test
  @Order(30)
  public void test030GetAllGroupsReturnsEmpty() {
    // When enableGroupsResource=false the invocator must return an empty set
    // without hitting the /Groups endpoint, so this should not throw
    ToListResultsHandler handler = new ToListResultsHandler();
    assertDoesNotThrow(() ->
        getConnectorFacade().search(
            new ObjectClass("Scim2Group"),
            null,
            handler,
            new OperationOptionsBuilder().build()));
    assertTrue(handler.getObjects().isEmpty(), "Groups search must return empty when Groups resource is disabled");
  }
}
