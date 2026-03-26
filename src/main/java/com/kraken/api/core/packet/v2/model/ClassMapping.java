package com.kraken.api.core.packet.v2.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ClassMapping {
    private String name;
    private String obfuscatedName;

    @SerializedName("fields")
    private List<FieldMapping> fields = new ArrayList<>();

    @SerializedName("methods")
    private List<MethodMapping> methods = new ArrayList<>();
}