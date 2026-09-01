package com.exclamationlabs.connid.base.scim2.adapter.dynamic;

import com.exclamationlabs.connid.base.scim2.model.Scim2User;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gson TypeAdapterFactory that captures arbitrary SCIM2 extension schema attributes
 * (any top-level JSON key starting with "urn:" other than the enterprise extension
 * which is already handled by a field-level adapter) into {@link Scim2User#getExtensions()}.
 *
 * <p>Attribute names in ConnId use the format {@code <schemaURN>::<fieldPath>}, where
 * {@code ::} is the separator between the schema URN and the attribute path within that
 * extension. Sub-attributes use dots: {@code <schemaURN>::<complexAttr>.<subAttr>}.
 */
public class Scim2UserExtensionAdapterFactory implements TypeAdapterFactory {

    private static final String ENTERPRISE_URN =
            "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!type.getRawType().equals(Scim2User.class)) return null;

        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        TypeAdapter<JsonObject> jsonObjectAdapter = gson.getAdapter(JsonObject.class);

        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                Scim2User user = (Scim2User) value;
                JsonElement tree = delegate.toJsonTree(value);
                if (tree.isJsonObject()) {
                    JsonObject obj = tree.getAsJsonObject();
                    Map<String, Map<String, Object>> extensions = user.getExtensions();
                    if (extensions != null) {
                        for (Map.Entry<String, Map<String, Object>> ext : extensions.entrySet()) {
                            if (ext.getValue() != null && !ext.getValue().isEmpty()) {
                                JsonObject extObj = new JsonObject();
                                for (Map.Entry<String, Object> field : ext.getValue().entrySet()) {
                                    extObj.add(field.getKey(), gson.toJsonTree(field.getValue()));
                                }
                                obj.add(ext.getKey(), extObj);
                            }
                        }
                    }
                    Map<String, Object> dynamicCore = user.getDynamicCoreAttributes();
                    if (dynamicCore != null) {
                        for (Map.Entry<String, Object> entry : dynamicCore.entrySet()) {
                            if (entry.getValue() != null) {
                                obj.add(entry.getKey(), gson.toJsonTree(entry.getValue()));
                            }
                        }
                    }
                    jsonObjectAdapter.write(out, obj);
                } else {
                    delegate.write(out, value);
                }
            }

            @Override
            public T read(JsonReader in) throws IOException {
                JsonObject jsonObject = jsonObjectAdapter.read(in);
                T result = delegate.fromJsonTree(jsonObject);
                Scim2User user = (Scim2User) result;
                Map<String, Map<String, Object>> extensions = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    String key = entry.getKey();
                    if (!key.startsWith("urn:") || !entry.getValue().isJsonObject()) continue;
                    if (key.equals(ENTERPRISE_URN)) {
                        // Enterprise extension is handled by the field-level adapter, but may
                        // contain nested extension URNs (e.g. Entra) as sibling keys.
                        extractNestedExtensions(entry.getValue().getAsJsonObject(), extensions);
                    } else {
                        Map<String, Object> extFields = new LinkedHashMap<>();
                        flattenJsonObject("", entry.getValue().getAsJsonObject(), extFields);
                        extensions.put(key, extFields);
                    }
                }
                user.setExtensions(extensions);
                return result;
            }

            private void extractNestedExtensions(JsonObject obj, Map<String, Map<String, Object>> out) {
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    if (e.getKey().startsWith("urn:") && e.getValue().isJsonObject()) {
                        Map<String, Object> fields = new LinkedHashMap<>();
                        flattenJsonObject("", e.getValue().getAsJsonObject(), fields);
                        out.put(e.getKey(), fields);
                    }
                }
            }

            private void flattenJsonObject(String prefix, JsonObject obj, Map<String, Object> out) {
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
                    JsonElement val = e.getValue();
                    if (val.isJsonObject()) {
                        flattenJsonObject(key, val.getAsJsonObject(), out);
                    } else {
                        out.put(key, toPrimitive(val));
                    }
                }
            }

            private Object toPrimitive(JsonElement el) {
                if (el.isJsonNull()) return null;
                if (el.isJsonPrimitive()) {
                    JsonPrimitive p = el.getAsJsonPrimitive();
                    if (p.isBoolean()) return p.getAsBoolean();
                    if (p.isNumber()) {
                        double d = p.getAsDouble();
                        return (d == Math.floor(d) && !Double.isInfinite(d))
                                ? (long) d
                                : d;
                    }
                    return p.getAsString();
                }
                return el.toString();
            }
        };
    }
}
