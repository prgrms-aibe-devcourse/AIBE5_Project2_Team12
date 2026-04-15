package com.generic4.itda.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HardFilterStatConverterTest {

    private final HardFilterStatConverter converter = new HardFilterStatConverter();

    @Test
    void convert() {
        HardFilterStat stat = new HardFilterStat(10, 8, 5, 3);

        String json = converter.convertToDatabaseColumn(stat);
        HardFilterStat restored = converter.convertToEntityAttribute(json);

        assertThat(restored).isEqualTo(stat);
        assertThat(restored.finalCount()).isEqualTo(3);
    }
}