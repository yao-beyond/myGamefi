package com.example.scaffold.conformance;

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
 * ADAF Conformance Suite — 把 ARCH_BLUEPRINT.md 第 7 章 checklist 變成可機器檢查的 fitness functions。
 *
 * 用法：
 *   1. 把本檔放進目標專案的 src/test/java（package 自行調整）。
 *   2. 設定 base package：-Dadaf.basePackage=com.yourcompany.yourapp（預設 com.example.app）。
 *   3. mvn test。任何架構漂移都會讓對應測試 fail，附上違規類別與位置。
 *
 * 約定的分層 package（對照 blueprint 第 2 章）：
 *   {base}.controller / {base}.bo / {base}.cache / {base}.dao / {base}.dto / {base}.code / {base}.provider
 *
 * 註：ArchUnit 無法偵測「switch(typeId) / if(typeId==..)」這類語句層級反模式，
 *     該規則改用 Checkstyle/PMD regex（見 README.md）。
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

    // ---- CPS-06：分層依賴單向往下 ----
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

    // ---- CPS-06：DAO 不得回呼上層 ----
    @Test
    public void dao_must_not_depend_on_upper_layers() {
        noClasses().that().resideInAPackage(BASE + ".dao..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(BASE + ".bo..", BASE + ".cache..", BASE + ".controller..")
                .check(classes);
    }

    // ---- CPS-06：DTO 是純資料，不得依賴任何上層 ----
    @Test
    public void dto_must_be_pure_data() {
        noClasses().that().resideInAPackage(BASE + ".dto..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(BASE + ".bo..", BASE + ".cache..", BASE + ".dao..", BASE + ".controller..")
                .check(classes);
    }

    // ---- CPS-06：命名 ↔ package 對齊 ----
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

    // ---- CPS-06：BO 為 static facade（建構子必須 private）----
    @Test
    public void bo_constructors_must_be_private() {
        constructors().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("BO")
                .should().bePrivate()
                .check(classes);
    }

    // ---- CPS-02：*Type 必須是 enum 且位於 code package ----
    @Test
    public void type_classes_must_be_enums_in_code_package() {
        classes().that().haveSimpleNameEndingWith("Type")
                .should().beAssignableTo(Enum.class)
                .andShould().resideInAPackage(BASE + ".code..")
                .check(classes);
    }

    // ---- CPS-01：Manager 欄位不得外露（必須 private）----
    @Test
    public void manager_fields_must_be_private() {
        fields().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")
                .should().bePrivate()
                .check(classes);
    }

    // ---- CPS-01：Provider 只能在註冊表(code)或 provider package 內被實例化（禁止 bo/dao/controller 裡 new）----
    // 註：enum 常數 body 會編譯成匿名子類（simpleName 為空），故用 package 判斷而非類名後綴，
    //     才不會把「註冊表 enum 內合法的 new」誤判成違規。
    @Test
    public void providers_only_instantiated_within_registry_or_provider_packages() {
        constructors().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Provider")
                .should(onlyBeCalledByClassesThat(
                        resideInAnyPackage(BASE + ".code..", BASE + ".provider..")))
                .check(classes);
    }

    // ---- The Nevers：禁止偷渡 Spring / DI container ----
    @Test
    public void must_not_introduce_spring_or_di_container() {
        noClasses().should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "javax.inject..", "com.google.inject..")
                .check(classes);
    }
}
