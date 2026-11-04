# Journal — app Android perso (temps, habitudes, journal)

Utilisateur unique : étudiant en alternance (Polytech Dijon, 4A FISA Cyber 2026-2027), Android.
Spécification complète : `docs/SPEC.md`. Lire avant toute nouvelle fonctionnalité.

## Règles de travail (demandées par l'utilisateur)
- Réponses directes, sans bavardage. Code ou instructions.
- En cas de doute : **demander**, ne pas deviner.
- Si un changement risque de casser autre chose : **prévenir avant d'écrire**.
- Textes de l'app en français, très courts. UI simple pour l'instant.
- Tout paramètre (horaires, durées, objectifs, URL ICS…) doit être modifiable dans l'app.

## Stack
- Kotlin, Jetpack Compose (Material 3), un seul module `:app`.
- minSdk 26, targetSdk/compileSdk 35, JDK 17, AGP 8.7.3, Kotlin 2.1.0, Gradle 8.11.1.
- Room (KSP) pour les données. Versions dans `gradle/libs.versions.toml`.
- Prévu (déjà dans le catalogue, pas encore branché) : WorkManager, CameraX, DataStore.
- DI manuelle : `di/AppContainer.kt` (créé par `JournalApplication`). Pas de Hilt.

## Commandes
```
./gradlew assembleDebug        # APK debug
./gradlew testDebugUnitTest    # tests JVM
./gradlew installDebug         # installe sur le téléphone branché
```
Le conteneur cloud de Claude n'a **pas** accès à Google Maven ni au SDK Android :
impossible d'y compiler. L'utilisateur compile dans Android Studio et remonte les erreurs.
→ Écrire du code prudent, n'utiliser que des API connues pour les versions ci-dessus.

## Architecture (`app/src/main/java/com/yasinmihci/journal/`)
```
JournalApplication.kt     Application, crée AppContainer
MainActivity.kt           Point d'entrée Compose
di/AppContainer.kt        DB, repositories, appScope
data/local/               Room : AppDatabase, Converters, entity/, dao/
data/repository/          Accès données pour l'UI (JournalRepository, HabitRepository)
ui/theme/                 Thème Material 3 (couleurs dynamiques)
ui/format/                Formatage (durées…)
ui/today/                 Écran principal : activités + journal du jour
```
À créer plus tard :
```
data/calendar/            Téléchargement + parsing ICS (ADE), stockage des cours
data/settings/            DataStore : horaires, trajets, URL ICS, heures de coucher…
domain/                   Logique pure (testable JVM) : type de journée, calcul réveil,
                          recherche de créneaux libres, cycles de sommeil
alarm/                    AlarmManager.setAlarmClock, receivers (alarme, boot), canal notif
work/                     WorkManager : synchro ICS quotidienne 6h, planification des notifs
ui/session/               Caméra frontale (CameraX) + chrono d'une session d'activité
ui/settings/, ui/habits/  Écrans d'édition
```

## Conventions
- « Activité » (UI) = `Habit` dans le code (évite la confusion avec `android.app.Activity`).
- Logique métier dans `domain/` en Kotlin pur + tests unitaires JVM.
- Room : `exportSchema = true`, schémas dans `app/schemas/` (à committer).
  **Jamais** de `fallbackToDestructiveMigration` : le journal ne doit pas être perdu.
  Toute modif d'entité → incrémenter `version` + écrire une `Migration`.
- Dates : `java.time` (LocalDate/LocalTime/ZonedDateTime, zone `Europe/Paris`).
  Les heures ICS sont en UTC (`...Z`).
- Strings dans `res/values/strings.xml`, pas en dur.

## Données
- `app/src/main/assets/periods_4a_2026_2027.json` : périodes école/entreprise/fériés
  extraites du calendrier 4A (couleurs des cellules du .docx). Catégorie `other` à confirmer.
- Fichier ICS d'exemple : non commité (données perso). Format ADE observé :
  `SUMMARY` = matière, `LOCATION` = salle, `DESCRIPTION` = groupes + enseignant
  (`IT4 FISA`, `IT4 FISA - Cyber`, `IT4 FISA - IOT`…). L'export contient des cours
  de plusieurs groupes qui se chevauchent → filtrage par groupe nécessaire (voir SPEC).
