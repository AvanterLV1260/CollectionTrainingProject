package lv.bootcamp.shelter.service;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.service.data.JsonSummary;
import lv.bootcamp.shelter.service.data.ShelterReportData;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j

public class ReportExportService {

    public void writeReport(Path outputPath, ShelterReportData reportData) {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write("Shelter Intake Report");
            writer.newLine();
            writer.write("Date generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            writer.newLine();
            writer.newLine();

            writer.write("Total imported: " + reportData.importResult().allAnimals().size());
            writer.newLine();
            writer.write("Total skipped:  " + reportData.importResult().skippedRows());
            writer.newLine();
            writer.write("Invalid rows: " + reportData.importResult().invalidRows().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ")));
            writer.newLine();
            writer.newLine();

            writer.write("Unique species");
            writer.newLine();
            writer.write(reportData.uniqueSpecies().stream().collect(Collectors.joining(", ")));
            writer.newLine();
            writer.newLine();

            writer.write("Per species breakdown");
            writer.newLine();
            for (String species : reportData.uniqueSpecies()) {
                long total = reportData.totalPerSpecies().get(species);
                long vaccinated = reportData.vaccinatedPerSpecies().getOrDefault(species, 0L);
                writer.write(species + ": " + total + " total, " + vaccinated + " vaccinated");
                writer.newLine();
            }
            writer.newLine();

            writer.write("Oldest per species");
            writer.newLine();
            for (String species : reportData.uniqueSpecies()) {
                Animal oldest = reportData.oldestPerSpecies().get(species);
                if (oldest != null) {
                    writer.write(species + ": " + oldest.getName() + " (age " + oldest.getAge() + ")");
                    writer.newLine();
                }
            }
            writer.newLine();

            writer.write("Needs Vet input to determine age");
            writer.newLine();
            writer.write(reportData.animalsNeedingVetInput().stream().collect(Collectors.joining(", ")));
            writer.newLine();
        } catch (IOException e) {
            log.error("Could not write report to {}", outputPath, e);
        }
    }

    public void writeJsonSummary(Path jsonPath, ShelterReportData reportData) {
        JsonSummary summary = new JsonSummary(
                reportData.importResult().allAnimals().size(),
                reportData.importResult().skippedRows(),
                reportData.uniqueSpecies(),
                reportData.totalPerSpecies(),
                reportData.vaccinatedPerSpecies(),
                reportData.animalsNeedingVetInput());
        try {
            Files.writeString(jsonPath, new Gson().toJson(summary), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Could not write JSON summary to {}", jsonPath, e);
        }
    }
}
