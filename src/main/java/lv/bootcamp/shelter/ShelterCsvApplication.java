package lv.bootcamp.shelter;

import lombok.extern.slf4j.Slf4j;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.service.CsvImportService;
import lv.bootcamp.shelter.service.data.ImportResult;
import lv.bootcamp.shelter.service.ReportExportService;
import lv.bootcamp.shelter.service.ShelterAnalyticsService;
import lv.bootcamp.shelter.service.data.ShelterReportData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Runs the program: imports CSV files and writes the reports.
 */
@Slf4j
public class ShelterCsvApplication {

    public static void main(String[] args) {
        Path dataDir = Path.of("src", "main", "resources", "data");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path outputPath = Path.of("output", "upload-report-" + timestamp + ".txt");
        Path registry = Path.of("output", "processed-files.txt");

        CsvImportService importService = new CsvImportService();
        ShelterAnalyticsService analyticsService = new ShelterAnalyticsService();
        ReportExportService reportExportService = new ReportExportService();

        List<Animal> allAnimals = new ArrayList<>();
        List<Integer> invalidRows = new ArrayList<>();
        int skippedRows = 0;

        Set<String> processedFiles = readRegistry(registry);

        try (Stream<Path> paths = Files.walk(dataDir)) {
            List<Path> csvFiles = paths.filter(path -> path.toString().endsWith(".csv")).toList();
            for (Path csvFile : csvFiles) {
                String fileName = csvFile.getFileName().toString();
                if (processedFiles.contains(fileName)) {
                    log.info("Skipping already processed file {}", fileName);
                    continue;
                }
                ImportResult result = importService.importAnimals(csvFile);
                allAnimals.addAll(result.allAnimals());
                skippedRows += result.skippedRows();
                invalidRows.addAll(result.invalidRows());
                saveToRegistry(registry, fileName);
            }
        } catch (IOException e) {
            log.error("Could not scan folder {}", dataDir, e);
        }

        ImportResult importResult = new ImportResult(allAnimals, skippedRows, invalidRows);
        ShelterReportData reportData = analyticsService.buildReportData(importResult);
        reportExportService.writeReport(outputPath, reportData);
        reportExportService.writeJsonSummary(Path.of("output", "summary.json"), reportData);
    }

    private static Set<String> readRegistry(Path registry) {
        Set<String> processedFiles = new HashSet<>();
        try {
            if (Files.exists(registry)) {
                processedFiles.addAll(Files.readAllLines(registry, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            log.error("Could not read registry {}", registry, e);
        }
        return processedFiles;
    }

    private static void saveToRegistry(Path registry, String fileName) throws IOException {
        Files.writeString(registry, fileName + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
