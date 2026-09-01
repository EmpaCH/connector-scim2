package com.exclamationlabs.connid.base.scim2.model;

import com.exclamationlabs.connid.base.connector.model.IdentityModel;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import java.util.LinkedHashMap;

public class Scim2User implements IdentityModel {

  // Some servers (e.g. sGuard) return the enterprise extension as [] instead of {}
  // when there are no enterprise attributes. Return null so parsing can continue.
  static class EnterpriseUserAdapter implements JsonDeserializer<Scim2EnterpriseUser> {
    private static final Gson GSON = new Gson();
    @Override
    public Scim2EnterpriseUser deserialize(JsonElement json, java.lang.reflect.Type type,
                                           JsonDeserializationContext ctx) throws JsonParseException {
      if (json.isJsonArray()) return null;
      return GSON.fromJson(json, Scim2EnterpriseUser.class);
    }
  }

  private Boolean active;
  @SerializedName("addresses")
  private List<Scim2Address> addresses;
  private transient List<Map<String, String>> addressesAdded;
  private transient List<Map<String, String>> addressesRemoved;
  private String displayName;
  private List<Scim2ComplexType> emails;
  private transient List<Map<String, String>> emailsAdded;
  private transient List<Map<String, String>> emailsRemoved;
  @SerializedName("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User")
  @JsonAdapter(Scim2User.EnterpriseUserAdapter.class)
  private Scim2EnterpriseUser enterpriseInfo;
  private List<Scim2ComplexType> entitlements;
  private transient List<Map<String, String>> entitlementsAdded;
  private transient List<Map<String, String>> entitlementsRemoved;
  private String externalId;
  private List<Map<String, String>> groups;
  private transient List<Map<String, String>> groupsAdded;
  private transient List<Map<String, String>> groupsRemoved;
  private String id;
  private List<Scim2ComplexType> ims;
  private transient List<Map<String, String>> imsAdded;
  private transient List<Map<String, String>> imsRemoved;
  private String locale;
  private Scim2Meta meta;
  private Scim2Name name;
  private String nickName;
  private String password;
  private List<Scim2ComplexType> phoneNumbers;
  private transient List<Map<String, String>> phoneNumbersAdded;
  private transient List<Map<String, String>> phoneNumbersRemoved;
  private List<Scim2ComplexType> photos;
  private transient List<Map<String, String>> photosAdded;
  private transient List<Map<String, String>> photosRemoved;
  private String preferredLanguage;
  private String profileUrl;
  private List<Scim2ComplexType> roles;
  private transient List<Map<String, String>> rolesAdded;
  private transient List<Map<String, String>> rolesRemoved;
  private List<String> schemas;
  private String timezone;
  private String title;
  private String userName;
  private String userType;
  private List<Scim2ComplexType> x509Certificates;
  private transient List<Map<String, String>> x509CertificatesAdded;
  private transient List<Map<String, String>> x509CertificatesRemoved;

  /** Arbitrary extension schema attributes captured by {@link com.exclamationlabs.connid.base.scim2.adapter.dynamic.Scim2UserExtensionAdapterFactory}.
   *  Key: schema URN; Value: map of flattened attribute paths to values. */
  private transient Map<String, Map<String, Object>> extensions = new LinkedHashMap<>();

  /** Non-standard, non-extension core attributes from a dynamic schema (e.g. sGuard's {@code language}).
   *  These are serialized as flat root-level JSON keys by {@link com.exclamationlabs.connid.base.scim2.adapter.dynamic.Scim2UserExtensionAdapterFactory}. */
  private transient Map<String, Object> dynamicCoreAttributes = new LinkedHashMap<>();

  public Boolean getActive()
  {
    return active;
  }

  public List<Scim2Address> getAddresses()
  {
    return addresses;
  }

  public List<Map<String, String>> getAddressesAdded()
  {
    return addressesAdded;
  }

  public List<Map<String, String>> getAddressesRemoved()
  {
    return addressesRemoved;
  }

  public String getDisplayName()
  {
    return displayName;
  }

  public List<Scim2ComplexType> getEmails()
  {
    return emails;
  }

  public List<Map<String, String>> getEmailsAdded()
  {
    return emailsAdded;
  }

  public List<Map<String, String>> getEmailsRemoved()
  {
    return emailsRemoved;
  }

  public Scim2EnterpriseUser getEnterpriseInfo()
  {
    return enterpriseInfo;
  }

  public List<Scim2ComplexType> getEntitlements()
  {
    return entitlements;
  }

  public List<Map<String, String>> getEntitlementsAdded()
  {
    return entitlementsAdded;
  }

  public List<Map<String, String>> getEntitlementsRemoved()
  {
    return entitlementsRemoved;
  }

  public String getExternalId()
  {
    return externalId;
  }

  public List<Map<String, String>> getGroups()
  {
    return groups;
  }

  public List<Map<String, String>> getGroupsAdded()
  {
    return groupsAdded;
  }

  public List<Map<String, String>> getGroupsRemoved()
  {
    return groupsRemoved;
  }

  public String getId()
  {
    return id;
  }

  @Override
  public String getIdentityIdValue() {
    return id;
  }

  @Override
  public String getIdentityNameValue() {
    return userName;
  }

  public List<Scim2ComplexType> getIms()
  {
    return ims;
  }

  public List<Map<String, String>> getImsAdded()
  {
    return imsAdded;
  }

  public List<Map<String, String>> getImsRemoved()
  {
    return imsRemoved;
  }

  public String getLocale()
  {
    return locale;
  }

  public Scim2Meta getMeta()
  {
    return meta;
  }

  public Scim2Name getName()
  {
    return name;
  }

  public String getNickName()
  {
    return nickName;
  }

  public String getPassword()
  {
    return password;
  }

  public List<Scim2ComplexType> getPhoneNumbers()
  {
    return phoneNumbers;
  }

  public List<Map<String, String>> getPhoneNumbersAdded()
  {
    return phoneNumbersAdded;
  }

  public List<Map<String, String>> getPhoneNumbersRemoved()
  {
    return phoneNumbersRemoved;
  }

  public List<Scim2ComplexType> getPhotos()
  {
    return photos;
  }

  public List<Map<String, String>> getPhotosAdded()
  {
    return photosAdded;
  }

  public List<Map<String, String>> getPhotosRemoved()
  {
    return photosRemoved;
  }

  public String getPreferredLanguage()
  {
    return preferredLanguage;
  }

  public String getProfileUrl()
  {
    return profileUrl;
  }

  public List<Scim2ComplexType> getRoles()
  {
    return roles;
  }

  public List<Map<String, String>> getRolesAdded()
  {
    return rolesAdded;
  }

  public List<Map<String, String>> getRolesRemoved()
  {
    return rolesRemoved;
  }

  public List<String> getSchemas()
  {
    return schemas;
  }

  public String getTimezone()
  {
    return timezone;
  }

  public String getTitle()
  {
    return title;
  }

  public String getUserName()
  {
    return userName;
  }

  public String getUserType()
  {
    return userType;
  }

  public List<Scim2ComplexType> getX509Certificates()
  {
    return x509Certificates;
  }

  public List<Map<String, String>> getX509CertificatesAdded()
  {
    return x509CertificatesAdded;
  }

  public List<Map<String, String>> getX509CertificatesRemoved()
  {
    return x509CertificatesRemoved;
  }

  public void setActive(Boolean active)
  {
    this.active = active;
  }

  public void setAddresses(List<Scim2Address> addresses)
  {
    this.addresses = addresses;
  }

  public void setAddressesAdded(List<Map<String, String>> addressesAdded)
  {
    this.addressesAdded = addressesAdded;
  }

  public void setAddressesRemoved(List<Map<String, String>> addressesRemoved)
  {
    this.addressesRemoved = addressesRemoved;
  }

  public void setDisplayName(String displayName)
  {
    this.displayName = displayName;
  }

  public void setEmails(List<Scim2ComplexType> emails)
  {
    this.emails = emails;
  }

  public void setEmailsAdded(List<Map<String, String>> emailsAdded)
  {
    this.emailsAdded = emailsAdded;
  }

  public void setEmailsRemoved(List<Map<String, String>> emailsRemoved)
  {
    this.emailsRemoved = emailsRemoved;
  }

  public void setEnterpriseInfo(Scim2EnterpriseUser enterpriseInfo)
  {
    this.enterpriseInfo = enterpriseInfo;
  }

  public void setEntitlements(List<Scim2ComplexType> entitlements)
  {
    this.entitlements = entitlements;
  }

  public void setEntitlementsAdded(List<Map<String, String>> entitlementsAdded)
  {
    this.entitlementsAdded = entitlementsAdded;
  }

  public void setEntitlementsRemoved(List<Map<String, String>> entitlementsRemoved)
  {
    this.entitlementsRemoved = entitlementsRemoved;
  }

  public void setExternalId(String externalId)
  {
    this.externalId = externalId;
  }

  public void setGroups(List<Map<String, String>> groups)
  {
    this.groups = groups;
  }

  public void setGroupsAdded(List<Map<String, String>> groupsAdded)
  {
    this.groupsAdded = groupsAdded;
  }

  public void setGroupsRemoved(List<Map<String, String>> groupsRemoved)
  {
    this.groupsRemoved = groupsRemoved;
  }

  public void setId(String id)
  {
    this.id = id;
  }

  public void setIms(List<Scim2ComplexType> ims)
  {
    this.ims = ims;
  }

  public void setImsAdded(List<Map<String, String>> imsAdded)
  {
    this.imsAdded = imsAdded;
  }

  public void setImsRemoved(List<Map<String, String>> imsRemoved)
  {
    this.imsRemoved = imsRemoved;
  }

  public void setLocale(String locale)
  {
    this.locale = locale;
  }

  public void setMeta(Scim2Meta meta)
  {
    this.meta = meta;
  }

  public void setName(Scim2Name name)
  {
    this.name = name;
  }

  public void setNickName(String nickName)
  {
    this.nickName = nickName;
  }

  public void setPassword(String password)
  {
    this.password = password;
  }

  public void setPhoneNumbers(List<Scim2ComplexType> phoneNumbers)
  {
    this.phoneNumbers = phoneNumbers;
  }

  public void setPhoneNumbersAdded(List<Map<String, String>> phoneNumbersAdded)
  {
    this.phoneNumbersAdded = phoneNumbersAdded;
  }

  public void setPhoneNumbersRemoved(List<Map<String, String>> phoneNumbersRemoved)
  {
    this.phoneNumbersRemoved = phoneNumbersRemoved;
  }

  public void setPhotos(List<Scim2ComplexType> photos)
  {
    this.photos = photos;
  }

  public void setPhotosAdded(List<Map<String, String>> photosAdded)
  {
    this.photosAdded = photosAdded;
  }

  public void setPhotosRemoved(List<Map<String, String>> photosRemoved)
  {
    this.photosRemoved = photosRemoved;
  }

  public void setPreferredLanguage(String preferredLanguage)
  {
    this.preferredLanguage = preferredLanguage;
  }

  public void setProfileUrl(String profileUrl)
  {
    this.profileUrl = profileUrl;
  }

  public void setRoles(List<Scim2ComplexType> roles)
  {
    this.roles = roles;
  }

  public void setRolesAdded(List<Map<String, String>> rolesAdded)
  {
    this.rolesAdded = rolesAdded;
  }

  public void setRolesRemoved(List<Map<String, String>> rolesRemoved)
  {
    this.rolesRemoved = rolesRemoved;
  }

  public void setSchemas(List<String> schemas)
  {
    this.schemas = schemas;
  }

  public void setTimezone(String timezone)
  {
    this.timezone = timezone;
  }

  public void setTitle(String title)
  {
    this.title = title;
  }

  public void setUserName(String userName)
  {
    this.userName = userName;
  }

  public void setUserType(String userType)
  {
    this.userType = userType;
  }

  public void setX509Certificates(List<Scim2ComplexType> x509Certificates)
  {
    this.x509Certificates = x509Certificates;
  }

  public void setX509CertificatesAdded(List<Map<String, String>> x509CertificatesAdded)
  {
    this.x509CertificatesAdded = x509CertificatesAdded;
  }

  public void setX509CertificatesRemoved(List<Map<String, String>> x509CertificatesRemoved)
  {
    this.x509CertificatesRemoved = x509CertificatesRemoved;
  }

  public Map<String, Map<String, Object>> getExtensions() {
    return extensions;
  }

  public void setExtensions(Map<String, Map<String, Object>> extensions) {
    this.extensions = extensions;
  }

  public Map<String, Object> getDynamicCoreAttributes() {
    return dynamicCoreAttributes;
  }

  public void setDynamicCoreAttributes(Map<String, Object> dynamicCoreAttributes) {
    this.dynamicCoreAttributes = dynamicCoreAttributes;
  }
}
