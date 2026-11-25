package lv.bootcamp.shelter.service;

import lombok.extern.slf4j.Slf4j;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.service.data.ImportResult;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Slf4j
public class CsvImportService {

    public ImportResult importAnimals(Path inputPath) {
        log.info("Starting import from {}", inputPath);

        List<Animal> allAnimals = new ArrayList<>();
        int skippedRows = 0;
        List<Integer> invalidRows = new ArrayList<>();
        int rowNumber = 1;

        try (BufferedReader br = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                rowNumber++;
                String[] parts = line.split(",", -1);
                if (parts.length < 5) {
                    log.warn("Skipping malformed row: {}", line);
                    skippedRows++;
                    invalidRows.add(rowNumber);
                    continue;
                }
                String name = parts[0].trim();
                String species = parts[1].trim();
                if (name.isEmpty() || species.isEmpty()) {
                    log.warn("Skipping row with missing name or species: {}", line);
                    skippedRows++;
                    invalidRows.add(rowNumber);
                    continue;
                }
                String vaccinatedText = parts[3].trim();
                if (!vaccinatedText.equals("true") && !vaccinatedText.equals("false")) {
                    log.warn("Skipping row with invalid vaccinated value: {}", line);
                    skippedRows++;
                    invalidRows.add(rowNumber);
                    continue;
                }
                boolean vaccinated = Boolean.parseBoolean(vaccinatedText);
                LocalDate intakeDate;
                try {
                    intakeDate = LocalDate.parse(parts[4].trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                } catch (DateTimeParseException e) {
                    log.warn("Skipping row with invalid date: {}", line);
                    skippedRows++;
                    invalidRows.add(rowNumber);
                    continue;
                }
                Integer age = null;
                String ageText = parts[2].trim();
                if (!ageText.isEmpty()) {
                    try {
                        age = Integer.parseInt(ageText);
                        if (age <= 0) {
                            log.warn("Skipping row with non-positive age: {}", line);
                            skippedRows++;
                            invalidRows.add(rowNumber);
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Skipping row with invalid age: {}", line);
                        skippedRows++;
                        invalidRows.add(rowNumber);
                        continue;
                    }
                }
                allAnimals.add(new Animal(name, species, age, vaccinated, intakeDate));
            }
        } catch (IOException e) {
            log.error("Could not read file {}", inputPath, e);
        }

        log.info("Import finished: {} imported, {} skipped", allAnimals.size(), skippedRows);
        return new ImportResult(allAnimals, skippedRows, invalidRows);
    }
}
