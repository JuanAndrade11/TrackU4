package com.uninorte.edu.co.tracku;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.annotations.SerializedName;

/**
 * author JuanPablo
 */
public class DefaultExclusionStrategy implements ExclusionStrategy {

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        return f.getAnnotation(SerializedName.class) == null;
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }
}
