# ADAF Conformance Suite（ArchUnit fitness functions）

把 [`../../ARCH_BLUEPRINT.md`](../../ARCH_BLUEPRINT.md) 第 7 章 checklist 變成 CI 可擋的測試。
AI 生成程式碼後跑這套，架構漂移就會 fail。

## 內容
- `ArchitectureConformanceTest.java` — 11 條規則，JUnit 4 + ArchUnit。

## 1. 加依賴（pom.xml）

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit4</artifactId>
    <version>1.4.1</version>   <!-- 需 Java 8+；在 JDK 21 上正常運作 -->
    <scope>test</scope>
</dependency>
```
> 已用 JUnit 4 + 純 `@Test` 寫法，不依賴 `archunit-junit4` 的 runner；只用 `archunit` 核心也可。
> Gradle：`testImplementation 'com.tngtech.archunit:archunit-junit4:1.3.0'`。

## 2. 指定 base package

測試讀 system property `adaf.basePackage`（預設 `com.example.app`）：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <systemPropertyVariables>
      <adaf.basePackage>com.yourcompany.yourapp</adaf.basePackage>
    </systemPropertyVariables>
  </configuration>
</plugin>
```
或 `mvn test -Dadaf.basePackage=com.yourcompany.yourapp`。

## 3. 約定的分層 package（對照 blueprint 第 2 章）
```
{base}.controller   {base}.bo     {base}.cache    {base}.dao
{base}.dto          {base}.code   {base}.provider
```

## 4. 11 條規則 ↔ CPS 對照

| 測試方法 | CPS | 檢查 |
|---|---|---|
| `layering_must_be_unidirectional` | 06 | Controller→BO→Cache→DAO→DTO 單向 |
| `dao_must_not_depend_on_upper_layers` | 06 | DAO 不回呼 bo/cache/controller |
| `dto_must_be_pure_data` | 06 | DTO 不依賴任何上層 |
| `controllers_reside_in_controller_package` | 06 | `*Controller` ↔ package |
| `dao_reside_in_dao_package` | 06 | `*DAO` ↔ package |
| `cache_reside_in_cache_package` | 06 | `*Cache` ↔ package |
| `bo_constructors_must_be_private` | 06 | `*BO` 全 private 建構子 |
| `type_classes_must_be_enums_in_code_package` | 02 | `*Type` 是 enum 且在 code |
| `manager_fields_must_be_private` | 01 | `*Manager` 欄位不外露 |
| `providers_only_instantiated_within_registry_or_provider_packages` | 01 | 禁止他處 `new XxxProvider()` |
| `must_not_introduce_spring_or_di_container` | Nevers | 禁偷渡 Spring/DI |

## 5. ArchUnit 測不到的規則 → 用 Checkstyle / PMD

語句層級反模式（如 `switch(typeId)` / `if (typeId == ...)`）ArchUnit 看不到，改用 regex。Checkstyle `RegexpSinglelineJava` 範例：

```xml
<module name="RegexpSinglelineJava">
  <property name="format" value="switch\s*\(\s*\w*([Tt]ype)?[Ii]d?\s*\)"/>
  <property name="message" value="禁止對 type id 做 switch；請用 *Type enum 多型（CPS-02）。"/>
</module>
```
其他建議 PMD/regex 守則：DAO 內禁止字串拼接 user input（SQL injection）、DAO 禁出現散落的 vendor 字串（方言只走 dialect util，CPS-07）、enum 須含 `getInstanceOf` 簽名。

## 6. 驗證紀錄

本套規則已用「合規 fixture」實跑通過（`OK (11 tests)`），並以注入違規確認會擋下
（`new XxxProvider()` 於 BO、Controller 直呼 DAO → 2 failures）。ArchUnit 1.4.1 / JUnit 4.12 / JDK 21（`--release 21`）。

> 已知陷阱：enum 常數帶 body 會編譯成匿名子類（simpleName 為空），故 provider 實例化規則
> 用 **package 判斷**而非類名後綴，避免把註冊表 enum 內的合法 `new` 誤判成違規。
