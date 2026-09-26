package com.surexu.sesame.data;

import java.util.LinkedHashMap;

//@Data
public final class ModelFields extends LinkedHashMap<String, ModelField<?>> {

    public void addField(ModelField<?> modelField) {
        put(modelField.getCode(), modelField);
    }

}