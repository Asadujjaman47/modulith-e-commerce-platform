package com.company.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifies that the codebase respects Spring Modulith boundaries: no module reaches into another
 * module's internals; communication happens only via published events or public module APIs.
 */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(EcommerceApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void writesDocumentationSnippets() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}