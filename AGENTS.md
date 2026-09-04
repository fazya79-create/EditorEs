# AGENTS.md

Android code-editor app (Kotlin 2.3.10, Jetpack Compose + Material 3, AGP 9.1.0). Requires **JDK 17** (CI uses Temurin 17).

## Build prerequisites (non-obvious)

- Init the submodule first — nothing compiles without it:
  `git submodule update --init --recursive`
- `sora-editor` is a pinned submodule (branch `0.24.6`) wired as a composite build via `includeBuild("sora-editor")` in `settings.gradle.kts`. The `io.github.rosemoe:editor`, `:language-textmate`, `:editor-lsp` deps in `app/build.gradle.kts` resolve from source, not Maven. Update the submodule pin, never those coordinates.
- `:terminal-emulator` needs **NDK `29.0.14206865` + CMake `3.22.1`** (see its `build.gradle.kts`; CI installs them via `sdkmanager`). Native code lives in `terminal-emulator/src/main/cpp`.
- Trust `app/build.gradle.kts` for SDK levels (`minSdk 28`, `targetSdk 28`, `compileSdk 36`) — `README.md` claims different values and is stale. Note the split: `:terminal-emulator` / `:terminal-view` use `minSdk 26`, only `:app` uses 28.

## Commands

- Do NOT build locally (`./gradlew assemble*`). Verify via GitHub Actions only: commit, push to `main` (no PRs), then watch with `gh`:
  `gh run list --workflow android-build.yml` / `gh run watch <run-id>`
- CI (`android-build.yml`, push to `main` + PRs + manual dispatch) builds release first, then debug, and uploads both APKs as artifacts. It runs no tests and no lint — a green build proves compilation and signing, nothing more.

## Module map

- `:app` (`com.editor.es`) — the whole app. Entrypoints: `MainActivity.kt` (storage-permission gate; projects dir is external-storage `/EditorEs` via `R.string.projects_folder_name`), `EditorEsApp.kt`, `ui/navigation/`. Feature packages: `editor/`, `lsp/`, `build/`, `proot/`, `patch/`, `agent/`, `data/`, `service/`, `storage/`, `net/`.
- `:terminal-emulator` (`com.termux.emulator`, Termux-derived + JNI) → `:terminal-view` (`com.termux.view`, `api` re-export) → consumed by `:app`.
- Manifest components are load-bearing: `TermuxService` (specialUse FGS), `EditorEsDocumentsProvider`, `FileProvider`. All three have matching `-keep` rules in `app/proguard-rules.pro` — keep them in sync.

## Gotchas

- Release is minified + resource-shrunk (R8, `localeFilters += "en"` only). New code using reflection/serialization (Gson, LSP4J, tm4e, smali/apksig/bouncycastle) almost certainly needs a `-keep` in `app/proguard-rules.pro` — check there first when release-only crashes appear. Debug builds skip R8, so always suspect shrinking when debug works and release crashes.
- `signing/release.keystore` + `signing/signing.properties` are **committed**; release auto-signs when present and silently builds unsigned when absent. Do not delete, move, or gitignore them.
- `packaging.jniLibs.useLegacyPackaging = true` + prebuilt `libproot.so`/`libloader.so` committed under `app/src/main/jniLibs/{arm64-v8a,armeabi-v7a}` — never rebuilt by normal builds. `proot-build.yml` is a manual-only workflow that rebuilds static proot from termux-packages.
- Editor is a sora `CodeEditor` (View system) inside Compose via `AndroidView` (`editor/SoraEditor.kt`): init TextMate exactly once via `TextMateSetup.ensureInitialized()`, always `editor.release()` in `onRelease`. TextMate grammars/themes live in `app/src/main/assets/textmate/` (`languages.json` registry, default theme `dark_plus`); language mapping in `EditorLanguageResolver.kt`, editor config in `EditorConfigurator.kt`. New languages need a grammar folder + a registry entry + a resolver mapping, all three.
- `dependencyResolutionManagement` uses `FAIL_ON_PROJECT_REPOS` — add repositories only in `settings.gradle.kts`.
- `androidResources.localeFilters` strips every locale except `en`; do not add user-facing strings in other languages without touching that filter.

## Style

- No comments in code, ever — no `//`, `/* */`, KDoc, or XML `<!-- -->`. Never add them.
- `kotlin.code.style=official`. No lint/detekt/ktlint config exists, so the compiler + CI build are the only checks.

## Writing rules (always on)

When you write prose (docs, commit messages, chat replies) — code comments are banned anyway, see above.
These rules are blocking: do not send the text until every one of them passes.

Voice and stance:
- Have a stake: for any opinion, take one defensible stance instead of both-sides mush.
- Prefer active voice and a named actor over agentless passive. Agentless passive
  ("no configuration is needed", "changes were made", "it is recommended that") is banned;
  name who acts or address the reader as "you".
- State facts, not their significance. Delete "this represents / underscores / highlights",
  "represents", "symbolizes", "speaks to", "embodies", "reflects broader" applied to mundane things.
- Calibrate certainty to real belief ("clearly" / "I think" / "I'm not sure"), never stack hedges
  ("could potentially possibly"). Delete leftover qualifiers ("to some extent", "arguably",
  "in some ways") sitting next to an already-confident claim.
- Never rebut an objection nobody raised ("While some might argue..."). State the position
  directly, or name a real objection that actually exists in the piece.
- Use plain copulas. "serves as", "stands as", "boasts", "features", "offers" are banned
  wherever is / are / has works.
- Have opinions, don't just report. "This design is frustrating" beats
  "This design has certain limitations."

Banned vocabulary, by tier:
- Tier 1, always cut: delve, tapestry, testament (figurative), multifaceted, realm, interplay,
  "in today's ... landscape".
- Tier 2, cut or replace with plain language: leverage, underscore (verb), "it's worth noting",
  "it's important to note", "due to the fact that", "in order to", "at this point in time",
  "when it comes to", "it should be noted that", "in the context of".
- Tier 3, cut only in clusters (2+ per paragraph): crucial, pivotal, vibrant, robust, seamless,
  foster, showcase, notably, moreover, furthermore, utilize, enhance, garner, bolster.
- Promotional adjectives are banned as substitutes for facts: cutting-edge, world-class,
  state-of-the-art, breathtaking, must-visit, nestled, "in the heart of", renowned, rich (figurative).
  Replace each with the specific fact that makes it notable.
- Aphorism formulas are banned: "X is the new Y", "the currency of", "where X meets Y",
  "not a X but a Y". Cut the aphorism; state the actual point.
- Never flag a lone ordinary word (key, important, significant, various, effective) or a single
  em dash, list, or "not only X but Y" in isolation. Flag clusters, not isolated tells.

Banned constructions:
- Negative parallelisms ("not only X but Y", "it's not just X, it's Y"): max once per text,
  never twice. State the point directly instead.
- False ranges ("from X to Y" where X and Y share no real spectrum): name the actual items.
- Hedged-enumeration openers ("There are several ways to...", "In general,",
  "Generally speaking,"): give the specific answer first, drop the throat-clearing.
- Question-format headings ("Why is Y important?"): use statement headings.
- Sycophancy, ever: no "Great question!", "Excellent point!", "You're absolutely right!".
  Answer without the flattery.
- Chatbot chatter: no "I hope this helps", "Of course!", "Certainly!", "Would you like me to",
  "Let me know if", "Here is a", "let me walk you through", "here's what you need to know".
- Knowledge-cutoff disclaimers ("As of [date]", "based on available information",
  "while specific details are limited"): state the fact or cut the hedge.

Structure:
- No em dashes. Use commas, colons, or hyphens. Hyphenate a compound modifier only before
  a noun ("high-quality report", never "the report is high-quality").
- Open with the content, never an overview ("this guide covers...", "let's dive into...").
  End on a specific fact or open question, never a generic positive ("exciting times ahead").
- No rule-of-three by reflex, no tidy summary sentence closing every paragraph,
  no "In conclusion" / "In summary" / "To sum up" wrap. Sometimes just stop.
- Cut the treadmill: one idea stated once. Delete "In other words," / "Put simply," /
  "Essentially," restatements.
- Each paragraph must earn its place and depend on the last; merge or cut blocks you could
  swap without breaking anything.
- Describe the thing as it is, not its edit history. "was added to", "now uses",
  "has been updated to", "previously" are banned from docs unless the history is the point.
- One emphasis device per section max: no bold on every other phrase, no emoji-bulleted
  headers, no skipped heading levels, no horizontal rule before every heading.
- Use sentence case in headings, never Title Case.

Rhythm:
- Vary sentence length. Mix short (3-8 words), medium (12-20), and long (25-40) in every
  paragraph. Never 3+ consecutive sentences of similar length. Fragments are fine.
- Vary paragraph length dramatically: four sentences, then one line. Occasionally break
  parallel structure on purpose.
- Prefer the second or third word that comes to mind over the most statistically likely one.
  "Anyway,", "So here's the thing:", "Look,", "Thing is," are allowed as informal transitions.

Texture (at least two per text, skip only on dry reference text):
- A concrete sensory or lived detail ("debugging this at 2am with cold coffee and a stack
  trace that makes no sense") instead of an abstraction ("the process is complex").
- An admitted uncertainty, bias, or unresolved feeling.
- A tangent, aside, callback to something earlier, or small self-correction.
- A mid-thought imperfect start ("So I was looking at the logs and...").

Honesty:
- Never invent facts, numbers, dates, names, or quotes. Replace abstractions with concrete
  specifics already in the source (numbers, file paths, real examples); if a concrete detail
  is missing, flag the gap or ask, don't fabricate.
- Never rewrite text inside quotes, blockquotes, or code blocks.
- Never present a vague attribution ("experts argue", "research suggests", "it is widely
  believed"): name the specific expert, paper, or report, or delete the claim.

Self-check before sending (all must pass):
- Read it aloud mentally. Press release or person talking?
- Zero em dashes, zero surviving blacklist words, zero banned constructions above the allowed max.
- No 3+ same-length sentences in a row; no interchangeable paragraphs.
- Opening hooks, ending is specific. No generic-positive closer anywhere.
- The "who wrote this?" test: if no specific person is imaginable behind it, it needs more voice.
- Final audit pass: ask "what still reads as machine-written?", answer in two or three bullets,
  fix exactly those, then re-check.
