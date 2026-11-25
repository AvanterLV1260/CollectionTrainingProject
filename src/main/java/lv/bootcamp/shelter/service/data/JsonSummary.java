package lv.bootcamp.shelter.service.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record JsonSummary(
        int imported,
        int skipped,
        Set<String> species,
        Map<String, Long> totalPerSpecies,
        Map<String, Long> vaccinatedPerSpecies,
        List<String> needsVetInput
) {

}
