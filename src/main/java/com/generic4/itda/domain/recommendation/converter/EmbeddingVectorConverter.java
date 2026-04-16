package com.generic4.itda.domain.recommendation.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generic4.itda.domain.recommendation.vo.EmbeddingVector;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.List;

@Converter
public class EmbeddingVectorConverter implements AttributeConverter<EmbeddingVector, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Double>> DOUBLE_LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(EmbeddingVector attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(attribute.values());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("EmbeddingVector를 JSON으로 직렬화할 수 없습니다.", e);
        }
    }

    @Override
    public EmbeddingVector convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        try {
            List<Double> values = OBJECT_MAPPER.readValue(dbData, DOUBLE_LIST_TYPE);
            return new EmbeddingVector(values);
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON 값을 EmbeddingVector로 역직렬화할 수 없습니다.", e);
        }
    }
}
