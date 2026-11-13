package com.peoplecapacity.ingestion.infrastructure.adapter.out.source;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Lee los Excel y correos de la carpeta de fuentes. Lector mínimo de .xlsx (zip + XML), sin dependencias. */
@ApplicationScoped
public class SourceFiles {

    @ConfigProperty(name = "ingestion.sources.dir")
    String dir;

    /** Filas de una hoja como mapa encabezado → texto. La primera fila es el encabezado. */
    public List<Map<String, String>> sheet(String file, int sheetIndex) {
        Path path = Path.of(dir, "excel", file);
        try (ZipFile zip = new ZipFile(path.toFile())) {
            List<String> shared = new ArrayList<>();
            ZipEntry sharedEntry = zip.getEntry("xl/sharedStrings.xml");
            if (sharedEntry != null) {
                NodeList items = parse(zip.getInputStream(sharedEntry)).getElementsByTagName("si");
                for (int i = 0; i < items.getLength(); i++) {
                    shared.add(text((Element) items.item(i)));
                }
            }
            Document doc = parse(zip.getInputStream(zip.getEntry("xl/worksheets/sheet" + sheetIndex + ".xml")));
            NodeList rows = doc.getElementsByTagName("row");
            List<Map<String, String>> out = new ArrayList<>();
            Map<String, String> header = new LinkedHashMap<>();
            for (int r = 0; r < rows.getLength(); r++) {
                NodeList cells = ((Element) rows.item(r)).getElementsByTagName("c");
                Map<String, String> byColumn = new LinkedHashMap<>();
                for (int c = 0; c < cells.getLength(); c++) {
                    Element cell = (Element) cells.item(c);
                    String column = cell.getAttribute("r").replaceAll("[0-9]", "");
                    NodeList v = cell.getElementsByTagName("v");
                    String value;
                    if ("s".equals(cell.getAttribute("t")) && v.getLength() > 0) {
                        value = shared.get(Integer.parseInt(v.item(0).getTextContent()));
                    } else if (v.getLength() > 0) {
                        value = v.item(0).getTextContent();
                    } else {
                        value = text(cell);
                    }
                    byColumn.put(column, value == null || value.isBlank() ? null : value.trim());
                }
                if (r == 0) {
                    header = byColumn;
                    continue;
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (Map.Entry<String, String> h : header.entrySet()) {
                    row.put(h.getValue(), byColumn.get(h.getKey()));
                }
                out.add(row);
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer " + path + ": " + e.getMessage(), e);
        }
    }

    /** Correos en texto: nombre de archivo → contenido. */
    public Map<String, String> emails() {
        Map<String, String> out = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(Path.of(dir, "correos"))) {
            for (Path p : files.filter(f -> f.toString().endsWith(".txt")).sorted().toList()) {
                out.put(p.getFileName().toString(), Files.readString(p));
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer la carpeta de correos: " + e.getMessage(), e);
        }
        return out;
    }

    /** Fecha de Excel (número de serie) o ISO. */
    public static LocalDate date(String value) {
        if (value == null) {
            return null;
        }
        if (value.matches("\\d+(\\.0+)?")) {
            return LocalDate.of(1899, 12, 30).plusDays((long) Double.parseDouble(value));
        }
        return LocalDate.parse(value.substring(0, 10));
    }

    /** "a=1;b=2" omitiendo nulos. */
    public static String kv(String... pairs) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (pairs[i + 1] != null && !pairs[i + 1].isBlank()) {
                out.append(out.length() == 0 ? "" : ";").append(pairs[i]).append('=')
                        .append(pairs[i + 1].replace(';', ',').replace('\n', ' ').trim());
            }
        }
        return out.toString();
    }

    private static Document parse(InputStream in) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
    }

    private static String text(Element element) {
        NodeList texts = element.getElementsByTagName("t");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < texts.getLength(); i++) {
            out.append(texts.item(i).getTextContent());
        }
        return out.toString();
    }
}
