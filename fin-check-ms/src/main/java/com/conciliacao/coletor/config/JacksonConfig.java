package com.conciliacao.coletor.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    /**
     * Configuração especial necessária para desserializar respostas da API Conciflex.
     * A API retorna campos numéricos ora como String, ora como Number:
     *   "VALOR_BRUTO": "32.17"   ← String
     *   "PERCENTUAL_TAXA": 3.61  ← Number
     *   "PERDA_RS": 0            ← Integer
     * ALLOW_COERCION_OF_SCALARS permite que Jackson converta entre esses tipos automaticamente.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
    }
}
