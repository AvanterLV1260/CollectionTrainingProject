package lv.bootcamp.shelter.service;

import lombok.extern.slf4j.Slf4j;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.service.data.ImportResult;
import lv.bootcamp.shelter.service.data.ShelterReportData;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ShelterAnalyticsService {

    public ShelterReportData buildReportData(ImportResult importResult) {
        List<Animal> allAnimals = importResult.allAnimals();

        Set<String> uniqueSpecies = new TreeSet<>();
        Map<String, List<Animal>> animalsBySpecies = new HashMap<>();
        List<String> animalsNeedingVetInput = new ArrayList<>();

        for (Animal animal : allAnimals) {
            log.debug("Processing animal {} ({})", animal.getName(), animal.getSpecies());
            uniqueSpecies.add(animal.getSpecies());

            if (animal.getAge() == null) {
                log.debug("{} has unknown age, adding to vet input list", animal.getName());
                animalsNeedingVetInput.add(animal.getName() + "(" + animal.getSpecies() + ")");
            }
            if (!animalsBySpecies.containsKey(animal.getSpecies())) {
                log.debug("New species found: {}, creating group", animal.getSpecies());
                animalsBySpecies.put(animal.getSpecies(), new ArrayList<>());
            }
            animalsBySpecies.get(animal.getSpecies()).add(animal);
        }

        Map<String, Long> totalPerSpecies = allAnimals.stream()
                .collect(Collectors.groupingBy(Animal::getSpecies, Collectors.counting()));

        Map<String, Long> vaccinatedPerSpecies = allAnimals.stream()
                .filter(Animal::isVaccinated)
                .collect(Collectors.groupingBy(Animal::getSpecies, Collectors.counting()));

        Map<String, Animal> oldestPerSpecies = new HashMap<>();
        for (String species : animalsBySpecies.keySet()) {
            animalsBySpecies.get(species).stream()
                    .filter(animal -> animal.getAge() != null)
                    .sorted(Comparator.comparing(Animal::getAge).reversed())
                    .findFirst()
                    .ifPresent(oldest -> oldestPerSpecies.put(species, oldest));
        }

        log.debug("Total per species: {}, vaccinated per species: {}", totalPerSpecies, vaccinatedPerSpecies);

        return new ShelterReportData(importResult, uniqueSpecies, totalPerSpecies,
                vaccinatedPerSpecies, oldestPerSpecies, animalsNeedingVetInput);
    }
}
