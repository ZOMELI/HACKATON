package com.peoplecapacity.mocksources.application.port.out;

import java.util.List;
import java.util.Map;

/** Puerto de salida: datos crudos de cada fuente simulada. */
public interface SourceDataPort {

    List<Map<String, Object>> jiraProjects();

    List<Map<String, Object>> jiraIssues();

    List<Map<String, Object>> sigaOrgUnits();

    List<Map<String, Object>> sigaEmployees();

    List<Map<String, Object>> preperBoxes();
}
