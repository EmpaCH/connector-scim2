package com.exclamationlabs.connid.base.scim2.model.response;

import com.exclamationlabs.connid.base.scim2.model.Scim2Schema;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

/** Maps the SCIM2 {@code /Schemas} ListResponse envelope. */
public class SchemasListResponse {
    @SerializedName("Resources")
    private List<Scim2Schema> resources;

    public List<Scim2Schema> getResources() {
        return resources != null ? resources : Collections.emptyList();
    }

    public void setResources(List<Scim2Schema> resources) {
        this.resources = resources;
    }
}
