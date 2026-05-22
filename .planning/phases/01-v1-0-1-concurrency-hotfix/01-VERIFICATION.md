---
status: passed
phase: 01-v1-0-1-concurrency-hotfix
verified: 2026-05-23
requirements_addressed: [HOTFIX-01, HOTFIX-02, HOTFIX-03, HOTFIX-04]
---

# Верифікація фази 01: v1.0.1 Concurrency Hotfix

## Мета фази

> «Обмежити вплив гонки даних у production-версії v1.0.0, замінивши мутабельні HashMap на ConcurrentHashMap та огородивши читачів init-защіпкою, після чого відвантажити виправлення до Marketplace до початку рефакторингу.»

Верифікатор оцінює **досягнення мети**, а не лише формальне закриття задач.

---

## Перевірка критеріїв успіху по ROADMAP.md

### Критерій 1 — ConcurrentHashMap у `AppleScriptSystemDictionaryRegistryService` (14 полів) і `ApplicationDictionaryImpl` (9 карт)

**Статус: виконано з задокументованим відкладенням (addressed-with-deferral)**

Реальний стан коду підтверджений читанням файлу:

- Всі 14 outer-map-полів (рядки 67–80) та `dictionaryInfoMap` оголошені через `ConcurrentHashMap`.
- 4 bare-set сентинели (`notScriptableApplicationList`, `scriptingAdditions`, `notFoundApplicationList`, `discoveredApplicationNames`) ініціалізовані як `ConcurrentHashMap.newKeySet()`.
- Лічильники у файлі: `ConcurrentHashMap` — 23 входження; `newKeySet` — 6 входжень; `= HashMap()` — 0.
- Усі writer-сайти використовують `ConcurrentHashMap.compute(key) { _, existing -> (existing ?: ConcurrentHashMap.newKeySet()).also { it.add(...) } }` (атомарний get-or-put-and-mutate).

**ApplicationDictionaryImpl (9 maps):** навмисно не зачіпається в цій фазі. Рішення D-01 у 01-CONTEXT.md явно зафіксовує: «9 maps in ApplicationDictionaryImpl.kt:70-80 intentionally left as raw HashMap; addressed in v1.1 SDEF-05». Верифікатор трактує це як `addressed-with-deferral`, а не як прогалину, — відповідно до інструкцій верифікатора. Залишковий ризик прийнятний: `processInclude`-гонка зустрічається лише при `xi:include` SDEF, що є рідкісним сценарієм.

**Підтвердження того, що `ApplicationDictionaryImpl.kt` не зачіпався:**
```
git diff --stat 505f8e2..HEAD -- src/main/kotlin/com/intellij/plugin/applescript/psi/sdef/impl/ApplicationDictionaryImpl.kt
→ (порожній вивід — файл не змінено)
```

---

### Критерій 2 — CountDownLatch в `init {}` + латч-перевірки на reader-шляхах

**Статус: виконано повністю**

Перевірено безпосереднім читанням коду:

- `private val initLatch: CountDownLatch = CountDownLatch(1)` — оголошено (рядок 89, 1 входження).
- `init {}` містить `finally { initLatch.countDown() }` — навіть при виключенні в ланцюжку ініціалізації захіпка звільняється (D-05). Одне місце звільнення.
- **Parser hot path (22 Boolean predicates):** кожний метод розпочинається з `if (initLatch.count > 0L) return false` — не блокує; 22 входження підтверджено.
- **Resolver paths (findStdCommands, findApplicationCommands):** `if (!initLatch.await(2, TimeUnit.SECONDS)) return emptyList()` — 2 входження у тілах методів (третє входження — у KDoc, не у коді) підтверджено.
- Сигнатури `ParsableScriptHelper` і `ParsableScriptSuiteRegistryHelper` не змінені (D-12): `git diff --stat 505f8e2..HEAD -- src/main/kotlin/com/intellij/plugin/applescript/lang/parser/ParsableScriptHelper.kt` — порожній вивід.
- Writer-сайти всередині `init {}` (D-07) латч-перевірок не отримали — самодедлок виключено.

---

### Критерій 3 — `./gradlew test -PincludeHeavyTests=true` зелений; 6 ParserRegressionTest-фікстур; нові регресії відсутні

**Статус: виконано, з задокументованими уточненнями**

З 01-03-SUMMARY.md (Task 1, підтверджено):

| Клас | Результат |
|------|-----------|
| `ParserRegressionTest` (6 фікстур) | **PASS** — testDoShellScript, testTellEndtell, testMusicScript, testCountOfArgv, testTryMinimal, testTracksWhose |
| `StressTest` (новий) | **PASS** — testConcurrentReadersDoNotThrowOrDeadlock (2.04 с) |
| `ColdStartRegressionTest` (новий) | **PASS** — testReadersReturnDeterministicAnswerImmediatelyAfterInstantiation (2.27 с) |
| `AppleScriptLexerTest` | PASS |
| `ControlStmtParsingTestCase` | PASS |
| `DictionaryConstantParsingTestCase` | PASS |
| `StandardAdditionsParsingTestCase` | PASS |
| `HandlersParsingTestCase` | 2 FAIL — **попередньо існуючі** |
| `LiveSamplesParsingTestCase` | 2 FAIL — **попередньо існуючі** |
| `TellParsingTestCase` | 1 FAIL — **попередньо існуючий** |

**5 передіснуючих відмов НЕ є регресіями:** 01-03-SUMMARY.md фіксує порівняння з `origin/master` (коміт `505f8e2`) у окремому worktree — на `master` ті самі 5 тестів і додатково 6 інших відмовляють. Жоден тест, що раніше проходив, не став відмовляти на hotfix-гілці.

**`AppleScriptCodeInsightTest`:** не входить до фільтру `build.gradle.kts` ні за замовченням, ні при `-PincludeHeavyTests=true`. Відповідно до 01-CONTEXT.md §Specifics та інструкцій верифікатора — статус **N/A** для v1.0.1.

**Реєстрація concurrency-тестів у `build.gradle.kts`:** коміт `12681ab` (Plan 01-03) додав рядок `includeTestsMatching("com.intellij.plugin.applescript.test.concurrency.*")` та системний property-bridge. Це невелике авторизоване відхилення від оригінального diff-плану (Plan 01-02 SUMMARY явно делегував це Plan 01-03).

---

### Критерій 4 — Публікація v1.0.1 на JetBrains Marketplace; запис у CHANGELOG.md

**Статус: CHANGELOG виконано; publish/tag/push — навмисно відкладено**

- `CHANGELOG.md` містить `## [1.0.1] - YYYY-MM-DD` вище `## [1.0.0]`.
- Запис описує симптом (NullPointerException / зависання IDE при warm-up) без внутрішніх деталей реалізації (ConcurrentHashMap, CountDownLatch не згадуються). Відповідає правилу CLAUDE.md «changelogs are user-facing only».
- Дата-placeholder `YYYY-MM-DD` залишається навмисно: на момент виконання фази опублікування не відбулося.
- Публікація до Marketplace, `git tag v1.0.1`, `git push` **явно відкладені** відповідно до директиви користувача в 01-CONTEXT.md §Specifics: «спочатку хочаб баги виправимо, потім вже про маркет будемо думати». Plan 01-03 Task 3 («checkpoint:human-action») зафіксував це відкладення і попросив у користувача resume-signal.

Верифікатор трактує це як `addressed-with-deferral` (not a gap) — відповідно до інструкцій верифікатора.

---

## Трасованість вимог

| REQ-ID | Визначення | Plan | Виконано |
|--------|-----------|------|---------|
| **HOTFIX-01** | ConcurrentHashMap для 14 полів у `AppleScriptSystemDictionaryRegistryService`; 9 maps у `ApplicationDictionaryImpl` якщо є та ж уразливість | 01-01 | Частково: 14 полів registry-service виконані. 9 maps у `ApplicationDictionaryImpl` відкладено до v1.1 SDEF-05 за D-01 (задокументоване рішення про обсяг). |
| **HOTFIX-02** | CountDownLatch init-gating; parser non-blocking; resolver await(2s) | 01-01 | Повністю виконано. 22 Boolean predicates + 2 resolver awaits підтверджено grep-перевіркою. |
| **HOTFIX-03** | Heavy test зелений після concurrency-зміни; нові parser-регресії відсутні; Phase 8 фікстури зелені | 01-02 + 01-03 | Виконано. StressTest + ColdStartRegressionTest PASS; 6 ParserRegressionTest-фікстур PASS; 5 відмов — попередньо існуючі, не є регресіями. |
| **HOTFIX-04** | Marketplace publish v1.0.1 + CHANGELOG-запис про data-race fix | 01-01 + 01-03 | CHANGELOG виконано. Publish відкладено за явним рішенням користувача. |

---

## Аудит diff

Реальний diff `505f8e2..HEAD`:

| Файл | Зміна |
|------|-------|
| `AppleScriptSystemDictionaryRegistryService.kt` | +200/-76 (основний production fix) |
| `CHANGELOG.md` | +6 (v1.0.1 entry) |
| `build.gradle.kts` | +4 (реєстрація concurrency-тестів + sysprop bridge) |
| `ColdStartRegressionTest.kt` | +72 (новий) |
| `StressTest.kt` | +88 (новий) |
| `01-01-SUMMARY.md` | +113 (planning artifact) |
| `01-02-SUMMARY.md` | +135 (planning artifact) |
| `01-03-SUMMARY.md` | +189 (planning artifact) |

**Заморожена поверхня** — не зачіпалась:
- `ApplicationDictionaryImpl.kt` — без змін (D-01)
- `ParsableScriptHelper.kt` — без змін (D-12)
- `ParsableScriptSuiteRegistryHelper.kt` — без змін (D-12)
- `AppleScriptGeneratedParserUtil.java` — без змін (D-12)
- `ParserRegressionTest.kt` — без змін (D-10)
- `PersistedState` + анотації `@CollectionBean`/`@AbstractCollection` — збережені (D-11)
- `COMPONENT_NAME` — незмінний (D-12)
- `APP_BUNDLE_DIRECTORIES` з `/System/Applications` — збережений в `ApplicationDictionary.kt` (D-10)

---

## Помітні архітектурні рішення

| ID | Рішення | Вплив |
|----|---------|-------|
| **D-01** | 9 HashMap у `ApplicationDictionaryImpl` відкладено до v1.1 SDEF-05 | Залишає latent race на `processInclude`, але низькочастотний (рідкісний шлях xi:include) |
| **D-04 + D-05** | Один CountDownLatch(1); звільнення в `finally` — навіть при failure | Читачі ніколи не deadlock при помилці ініціалізації |
| **D-06** | Асиметрична політика: parser non-blocking (`count > 0`), resolver bounded `await(2s)` | Parser hot path ніколи не блокує EDT; resolver може коротко чекати у фоновому треді |
| **D-07** | Writer-сайти всередині `init {}` без латч-перевірки | Запобігає self-deadlock всередині ланцюжка ініціалізації |
| **D-08** | Два нових heavy-test: StressTest (16 threads) + ColdStartRegressionTest | Обидва PASS, лочать latch + CHM контракт під навантаженням |
| **D-12** | `ParsableScriptHelper` / `getInstance()` / `ParsableScriptSuiteRegistryHelper` заморожені | Сумісність API з `AppleScriptGeneratedParserUtil.java` збережена |
| **Publish-deferral** | Tag, push, publish відкладено за директивою користувача | Фаза у ship-ready стані; конкретне відвантаження — окреме рішення користувача |

---

## Вердикт

**PASSED**

Мета фази досягнута: production data-race window у `AppleScriptSystemDictionaryRegistryService` закрито через `ConcurrentHashMap` (14 outer maps + 4 concurrent sets) та `CountDownLatch(1)`, reader hot paths огороджені асиметричною латч-перевіркою, новий heavy test suite PASS, Phase 8 parser-фікстури PASS, CHANGELOG-запис готовий до публікації.

Три зафіксованих відкладення (9 maps у ApplicationDictionaryImpl → v1.1 SDEF-05; AppleScriptCodeInsightTest → v1.1 SDEF-17; publish/tag/push → рішення користувача) задокументовані та обґрунтовані в 01-CONTEXT.md і 01-03-SUMMARY.md. Жодне з них не є прогалиною у реалізації.

---

## Елементи для опціональної перевірки людиною

Наступне не є блокерами для статусу `passed`, але потребує рішення користувача:

1. **Дата у CHANGELOG.md** — замінити `YYYY-MM-DD` на реальну дату публікації перед `./gradlew publishPlugin`.
2. **Git topology** — вибрати між merge до `master`, `release/v1.0.x`-гілкою або поточною `kotlin-rewrite-phase-7` (01-CONTEXT.md §Deferred).
3. **Marketplace channel** — default (stable) чи EAP (за умовчанням очікується `default`).
4. **`ApplicationDictionaryImpl` 9 maps** — підтвердити що v1.1 SDEF-05 охоплює цей пункт; до того часу залишається latent race при `xi:include` SDEF.
5. **5 передіснуючих тестових відмов** — закрити або задокументувати в окремому тікеті; не є блокерами для v1.0.1, але знижують загальний рівень сигналу в тестовому звіті.

---

*Верифіковано: 2026-05-23*
*Фаза: 01-v1-0-1-concurrency-hotfix*
*База порівняння: 505f8e2 (master)*
