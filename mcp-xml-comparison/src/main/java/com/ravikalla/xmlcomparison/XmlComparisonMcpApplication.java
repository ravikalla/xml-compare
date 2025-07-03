package com.ravikalla.xmlcomparison;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ravikalla.xmlcomparison.service.XmlComparisonService;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class XmlComparisonMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(XmlComparisonMcpApplication.class, args);
    }

    @Bean
    public List<ToolCallback> xmlComparisonTools(XmlComparisonService xmlComparisonService) {
        return List.of(ToolCallbacks.from(xmlComparisonService));
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

}