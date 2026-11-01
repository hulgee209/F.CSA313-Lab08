# Лаборатори №8: Mock объектууд — Mockito

F.CSA313 — Программ хангамжийн чанарын баталгаа ба тест (2026)

- **Оюутан:** Э.Батхүлэг
- **Код:** B232270040

## Орчин

```
$ java -version
java version "23.0.2" 2025-01-21
Java(TM) SE Runtime Environment (build 23.0.2+7-58)
Java HotSpot(TM) 64-Bit Server VM (build 23.0.2+7-58, mixed mode, sharing)

$ mvn -version
Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
Maven home: D:\apache-maven-3.9.16-bin\apache-maven-3.9.16
Java version: 23.0.2, vendor: Oracle Corporation, runtime: D:\java
Default locale: en_US, platform encoding: UTF-8
OS name: "windows 11", version: "10.0", arch: "amd64", family: "windows"
```

JDK 23 дээр ажиллуулахад гарах `WARNING: A Java agent has been loaded dynamically (… byte-buddy-agent …)`
нь Mockito-гийн inline mock maker-ийн хэвийн анхааруулга.

## Бүтэц

```
├── pom.xml
├── src/main/java/mn/edu/must/sqat/      # starter код (өөрчлөгдөөгүй)
│   ├── Student.java
│   ├── StudentRepository.java
│   ├── EmailSender.java
│   └── CourseRegistrationService.java
├── src/test/java/mn/edu/must/sqat/
│   └── CourseRegistrationServiceTest.java
└── results/
    ├── mvn-test.txt                      # ногоон гаралт
    ├── mvn-test-mutant-1.txt
    ├── mvn-test-mutant-2.txt
    ├── mvn-test-mutant-3-before-fix.txt  # A6 засахаас өмнө — мутант амьд үлдсэн
    └── mvn-test-mutant-3.txt             # A6 зассаны дараа — мутант баригдсан
```

## Хэсэг A: Тестүүд

`results/mvn-test.txt`-ийн сүүлийн мөр: **Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS**
(7 энгийн тест + A8-ийн 3 мөр).

| № | Тестийн нэр | Хэрэгсэл |
|---|---|---|
| A1 | `register_activeStudentWithPrerequisites_returnsOkSavesAndSendsEmail` | `when` + `verify` |
| A2 | `register_unknownStudent_returnsErrorNoStudentAndDoesNotSaveOrEmail` | `verify(..., never())` |
| A3 | `register_inactiveStudent_returnsErrorInactiveAndNeverTouchesEmail` | `verifyNoInteractions` |
| A4 | `register_missingPrerequisite_returnsErrorPrerequisitesAndDoesNotSave` | `when` + `never` |
| A5 | `register_emailSenderThrows_stillReturnsOkAndRegistrationIsSaved` | `doThrow(...).when(...)` |
| A6 | `register_nullStudentIdOrCourseId_throwsIllegalArgumentAndNoInteractions` | `assertThrows` |
| A7 | `register_success_emailBodyContainsCourseCode` | `ArgumentCaptor` |
| A8 | `register_invalidCase_returnsExpectedErrorAndDoesNotSave` (3 мөр) | `@ParameterizedTest` + `@CsvSource` |

Matcher-ийн дүрэм: нэг дуудалтад matcher хэрэглэсэн бол бүх аргументад matcher ашигласан
(жишээ нь `verify(emailSender).send(eq("bat@must.edu.mn"), anyString(), bodyCaptor.capture())`).

### A8 — яагаад `lenient()` хэрэгтэй вэ

A8-ийн мөр бүр `register()`-ийн өөр алхам дээр зогсдог: `ERROR_NO_STUDENT` мөрөнд `courseExists` болон
`prerequisites` огт дуудагддаггүй, `ERROR_INACTIVE_STUDENT` мөрөнд мөн адил. MockitoExtension-ий strict stubs
горим ашиглагдаагүй stub-ыг `UnnecessaryStubbingException` гэж унагадаг тул бүх мөрөнд нийтлэг stub-уудыг
`lenient().when(...)` хэлбэрээр зарлаж, "энэ stub зарим мөрөнд ашиглагдахгүй байж болно" гэж Mockito-д мэдэгдсэн.

## Хэсэг B: Мини мутаци

| Мутант | Өөрчлөлт | Унасан тестүүд | Үр дүн (файл) |
|---|---|---|---|
| 1 | `if (!student.active()) return "ERROR_INACTIVE_STUDENT";`-ийг комментлосон | A3 (`expected ERROR_INACTIVE_STUDENT but was ERROR_NO_COURSE`), A8[2] (`expected ERROR_INACTIVE_STUDENT but was OK`) | Failures: 2, Errors: 0 — BUILD FAILURE (`mvn-test-mutant-1.txt`) |
| 2 | `repository.saveRegistration(studentId, courseId);`-г комментлосон | A1, A5 (`Wanted but not invoked: repository.saveRegistration`) | Failures: 2, Errors: 0 — BUILD FAILURE (`mvn-test-mutant-2.txt`) |
| 3 | эхний if-ээс `\|\| courseId == null`-ийг устгасан | **Засахаас өмнө: нэг ч тест унаагүй** (`mvn-test-mutant-3-before-fix.txt`, BUILD SUCCESS). Засварын дараа: A6 (`Expected IllegalArgumentException to be thrown, but nothing was thrown`) | Failures: 1, Errors: 0 — BUILD FAILURE (`mvn-test-mutant-3.txt`) |

**Мутант 3-ын засвар:** Анхны A6 зөвхөн `register(null, "CS201")`-г шалгадаг байсан тул `courseId == null`
шалгалтыг устгахад ямар ч тест анзаараагүй — энэ бол тестийн цоорхой. A6-д
`assertThrows(IllegalArgumentException.class, () -> service.register("B001", null))`-г нэмж, хоёр аргумент
тус бүрийн null-ийг шалгадаг болгосон (тестийн тоо 10 хэвээр). Үүний дараа мутант баригдсан.

**Дүгнэлт:** Гурван мутантын хоёрыг анхнаасаа барьсан, гуравдахь нь тестийн дутууг илрүүлсэн. Мутант 1-ийг
хоёр өөр тест (A3, A8) давхар барьсан нь идэвхгүй оюутны зам сайн хамгаалагдсаныг харуулна. Мутант 2-ыг зөвхөн
`verify(repository).saveRegistration(...)` барьсан — буцах утга "OK" хэвээр байсан тул `assertEquals`
ганцаараа хангалтгүй, харилцан үйлчлэлийн верификаци энд зайлшгүй. Аль ч мутантад
`UnnecessaryStubbingException` (Errors) гараагүй, бүгд Failures хэлбэрээр унасан. Мутаци бүрийн дараа
`src/main`-ийг эх хувилбарт нь сэргээж, `results/mvn-test.txt`-ийг дахин ногоон гаргасан.

## Дүгнэлт

Mock нь Mockito-гоор ажиллах үед автоматаар үүсдэг, stub-аар хариуг нь тохируулж, `verify`-аар хэрхэн
дуудагдсаныг шалгадаг объект; харин гараар бичсэн fake нь интерфейсийн жинхэнэ боловч хялбаршуулсан
хэрэгжүүлэлт (жишээ нь `HashMap` дээр суурилсан `InMemoryStudentRepository`) бөгөөд төлөв хадгалж, олон тестэд
дахин ашиглагдана. Mock нь "юу дуудагдсан бэ" гэсэн харилцан үйлчлэлийг шалгахад, fake нь "эцсийн төлөв ямар
байна" гэдгийг шалгахад илүү тохиромжтой. Mock хийх ёсгүй зүйлс: `Student` шиг энгийн өгөгдлийн объект (record),
тестлэгдэж буй класс өөрөө, мөн `List`, `String` зэрэг өөрийн эзэмшдэггүй стандарт сангийн төрлүүд — эдгээрийг
жинхэнээр нь үүсгэх нь илүү энгийн, найдвартай. Гадаад систем (өгөгдлийн сан, SMTP) л mock-ийн жинхэнэ
зорилтот хамаарал юм. `verify`-г хэтрүүлбэл тест хэрэгжүүлэлтийн дотоод нарийн ширийн (дуудлагын дараалал,
тоо, туслах аргууд)-тай хэт уягдаж, зан төлөв өөрчлөгдөөгүй байхад refactoring бүрд тест унадаг "хэврэг" болно.
Иймээс зөвхөн гадагш харагдах чухал үр дагаврыг (бүртгэл хадгалагдсан, и-мэйл явсан/яваагүй) verify хийж,
бусдыг буцах утгаар шалгах нь зөв. Хэсэг B-ийн мутаци ч харуулсанчлан тест нь кодыг эвдэхэд унадаг байж л
үнэ цэнэтэй.

## Ажиллуулах

```
mvn test
mkdir -p results && mvn test 2>&1 | tee results/mvn-test.txt
```
