package com.exclamationlabs.connid.base.scim2;

import static org.junit.jupiter.api.Assertions.*;

import com.exclamationlabs.connid.base.connector.configuration.ConfigurationReader;
import com.exclamationlabs.connid.base.connector.test.ApiIntegrationTest;
import com.exclamationlabs.connid.base.scim2.configuration.Scim2Configuration;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.test.common.ToListResultsHandler;
import org.junit.jupiter.api.*;

import java.util.List;

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
