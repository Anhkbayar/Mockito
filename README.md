# Lab08 — Mockito

- **Оюутны нэр:** Z.Ankhbayar
- **Оюутны код:** B232270029

## Орчны хувилбар

```text
$ java -version
openjdk version "25.0.4.1" 2026-08-18
OpenJDK Runtime Environment (build 25.0.4.1+1-1-26.04.4-Ubuntu)
OpenJDK 64-Bit Server VM (build 25.0.4.1+1-1-26.04.4-Ubuntu, mixed mode, sharing)

$ mvn -version
Apache Maven 3.9.12
Maven home: /usr/share/maven
Java version: 25.0.4.1, vendor: Ubuntu, runtime: /usr/lib/jvm/java-25-openjdk-amd64
Default locale: en, platform encoding: UTF-8
OS name: "linux", version: "7.0.0-38-generic", arch: "amd64", family: "unix"
```

## Тестийн үр дүн

`results/mvn-test.txt` файлын эцсийн үр дүн: **Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS.** `CourseRegistrationServiceTest` нь найман тестийн аргатай; A8-ийн гурван параметр тус бүр тест болж тоологдоно.

A8-ийн мөр бүр зөвхөн тухайн сценарид хэрэгтэй stub-үүдийг тохируулдаг тул бусад мөрийн unused stub үүсэхгүй. Иймээс strict stubbing алдаа гарахгүй бөгөөд `lenient()` хэрэглэх шаардлагагүй.

## Хэсэг B — Мини мутацийн үр дүн

Мутацийн лог бүр `Tests run: 10` гэж тайлагнаж, `BUILD FAILURE`-ээр төгссөн. Mockito-ийн strict stubbing-оос үүдсэн `Errors` гараагүй; дараах тестүүд assertion эсвэл `verify` шалгалтаар унасан.

| Мутант | Түр өөрчилсөн код | Унасан тестүүд | Лог |
|---|---|---|---|
| 1 | Идэвхгүй оюутны шалгалтыг хассан | `register_inactiveStudent_returnsInactiveErrorWithoutEmailInteraction`; `register_errorScenario_returnsExpectedErrorWithoutSaving` `[2]` (A8-ийн INACTIVE_STUDENT мөр) | [mvn-test-mutant-1.txt](results/mvn-test-mutant-1.txt) |
| 2 | `saveRegistration(...)` дуудалтыг хассан | `register_activeStudent_returnsOkAndSavesAndSendsEmail`; `register_emailSenderThrows_keepsRegistrationSuccessful` | [mvn-test-mutant-2.txt](results/mvn-test-mutant-2.txt) |
| 3 | `courseId == null` шалгалтыг хассан | `register_nullInput_throwsIllegalArgumentExceptionWithoutDependencyInteractions` (A6) | [mvn-test-mutant-3.txt](results/mvn-test-mutant-3.txt) |

Гурван мутаци тус бүрийг тестүүд илрүүлсэн. A6 нь `studentId` болон `courseId`-ийн null утгыг тус тусад нь шалгадаг тул гурав дахь мутаци илэрсэн.

## Дүгнэлт

Mockito mock-ийг framework үүсгэж, stub болон `verify` тохиргоогоор хариу үйлдэл, харилцааг шалгадаг. Гар бичсэн fake нь интерфейсийг хэрэгжүүлсэн код бөгөөд өөрийн жижиг төлөв, дүрэм, эсвэл өгөгдөл хадгалах логиктой байж болно. Mock нь сан, SMTP зэрэг гадаад хамаарлыг бодит үйлчилгээ асаалгүй орлуулахад тохиромжтой. Тестэлж буй `CourseRegistrationService`-ийг өөрийг нь mock хийж болохгүй, учир нь тэгвэл шалгах бизнес логик ажиллахгүй. `Student` шиг өгөгдлийн объект болон энгийн, урьдчилан таамаглах боломжтой логикийг шаардлагагүй mock хийхгүй байх нь зүйтэй. `verify`-г дотоод дуудлага бүрт тавих нь тестийг хэрэгжүүлэлтийн нарийн бүтэцтэй хэт холбож, refactor хийхэд хэрэггүй эвдрэл үүсгэж болно. Тиймээс хэрэглэгчид харагдах үр дүн болон чухал side effect-үүдийг verify хийх нь тохиромжтой. Энэ мини мутацийн гурван өөр эвдрэл бүгд тестээр баригдсан нь эдгээр regression-ийг илрүүлэх хамгаалалт байгааг харууллаа.
