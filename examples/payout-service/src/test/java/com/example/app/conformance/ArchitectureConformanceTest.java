package com.example.app.conformance;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeCalledByClassesThat;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * ADAF Conformance Suite（見框架 scaffold/conformance）。指向本範例的 com.example.app。
 * 把 ARCH_BLUEPRINT.md 第 7 章 checklist 變成可機器檢查的 fitness functions。
 */
public class ArchitectureConformanceTest {

    private static final String BASE = System.getProperty("adaf.basePackage", "com.example.app");

    private static JavaClasses classes;

    @BeforeClass
    public static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    public void layering_must_be_unidirectional() {
        layeredArchitecture().consideringOnlyDependenciesInLayers()
                .layer("Controller").definedBy(BASE + ".controller..")
                .layer("BO").definedBy(BASE + ".bo..")
                .layer("Cache").definedBy(BASE + ".cache..")
                .layer("DAO").definedBy(BASE + ".dao..")
                .layer("DTO").definedBy(BASE + ".dto..")
                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("BO").mayOnlyBeAccessedByLayers("Controller")
                .whereLayer("Cache").mayOnlyBeAccessedByLayers("Controller", "BO")
                .whereLayer("DAO").mayOnlyBeAccessedByLayers("BO", "Cache")
                .whereLayer("DTO").mayOnlyBeAccessedByLayers("Controller", "BO", "Cache", "DAO")
                .check(classes);
    }

    @Test
    public void dao_must_not_depend_on_upper_layers() {
        noClasses().that().resideInAPackage(BASE + ".dao..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(BASE + ".bo..", BASE + ".cache..", BASE + ".controller..")
                .check(classes);
    }

    @Test
    public void dto_must_be_pure_data() {
        noClasses().that().resideInAPackage(BASE + ".dto..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(BASE + ".bo..", BASE + ".cache..", BASE + ".dao..", BASE + ".controller..")
                .check(classes);
    }

    @Test
    public void controllers_reside_in_controller_package() {
        classes().that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage(BASE + ".controller..")
                .check(classes);
    }

    @Test
    public void dao_reside_in_dao_package() {
        classes().that().haveSimpleNameEndingWith("DAO")
                .should().resideInAPackage(BASE + ".dao..")
                .check(classes);
    }

    @Test
    public void cache_reside_in_cache_package() {
        classes().that().haveSimpleNameEndingWith("Cache")
                .should().resideInAPackage(BASE + ".cache..")
                .check(classes);
    }

    @Test
    public void bo_constructors_must_be_private() {
        constructors().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("BO")
                .should().bePrivate()
                .check(classes);
    }

    @Test
    public void type_classes_must_be_enums_in_code_package() {
        classes().that().haveSimpleNameEndingWith("Type")
                .should().beAssignableTo(Enum.class)
                .andShould().resideInAPackage(BASE + ".code..")
                .check(classes);
    }

    @Test
    public void manager_fields_must_be_private() {
        fields().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")
                .should().bePrivate()
                .check(classes);
    }

    @Test
    public void providers_only_instantiated_within_registry_or_provider_packages() {
        constructors().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Provider")
                .should(onlyBeCalledByClassesThat(
                        resideInAnyPackage(BASE + ".code..", BASE + ".provider..")))
                .check(classes);
    }

    @Test
    public void must_not_introduce_spring_or_di_container() {
        noClasses().should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "javax.inject..", "com.google.inject..")
                .check(classes);
    }
}
