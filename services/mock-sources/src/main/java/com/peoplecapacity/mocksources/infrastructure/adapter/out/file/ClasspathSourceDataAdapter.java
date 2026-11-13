package com.peoplecapacity.mocksources.infrastructure.adapter.out.file;

import com.peoplecapacity.mocksources.application.port.out.SourceDataPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/** Adaptador de salida: carga los JSON simulados del classpath (src/main/resources/data). */
@ApplicationScoped
public class ClasspathSourceDataAdapter implements SourceDataPort {

    @Inject
    ObjectMapper mapper;

    private Map<String, List<Map<String, Object>>> jira;
    private Map<String, Object> siga;
    private Map<String, List<Map<String, Object>>> preper;

    @PostConstruct
    void load() {
        jira = read("data/jira.json", new TypeReference<Map<String, List<Map<String, Object>>>>() {
        });
        siga = read("data/siga.json", new TypeReference<Map<String, Object>>() {
        });
        preper = read("data/preper.json", new TypeReference<Map<String, List<Map<String, Object>>>>() {
        });
    }

    @Override
    public List<Map<String, Object>> jiraProjects() {
        return jira.get("projects");
    }

    @Override
    public List<Map<String, Object>> jiraIssues() {
        return jira.get("issues");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> sigaOrgUnits() {
        return (List<Map<String, Object>>) siga.get("orgUnits");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> sigaEmployees() {
        return (List<Map<String, Object>>) siga.get("employees");
    }

    @Override
    public List<Map<String, Object>> preperBoxes() {
        return preper.get("boxes");
    }

    private <T> T read(String resource, TypeReference<T> type) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró " + resource);
            }
            return mapper.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
