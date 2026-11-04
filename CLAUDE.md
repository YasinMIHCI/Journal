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
- DataStore Preferences : réglages (`data/settings`, modèle `domain/settings/AppSettings`).
- WorkManager : synchro ICS quotidienne (`work/CalendarSyncWorker`).
- CameraX : aperçu caméra frontale pendant une session (`ui/session`).
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
data/repository/          Accès données pour l'UI (Journal, Habit, Session)
ui/theme/                 Thème Material 3 (couleurs dynamiques)
ui/format/                Formatage (durées…)
ui/today/                 Écran principal : activités + journal du jour
data/settings/            DataStore : tous les réglages (AppSettings)
domain/settings/          AppSettings (modèle + valeurs par défaut)
domain/day/               Type de journée (Periods) + calcul réveil/départ (WakePlanner)
domain/calendar/          Course + parser ICS (IcsParser), filtre par groupe
data/periods/             Lecture de assets/periods_*.json
data/calendar/            Téléchargement ICS, cours Room, état de synchro
work/                     CalendarSyncWorker (WorkManager, quotidien)
domain/day/AlarmPlanner   Prochain réveil / départ (pur, testé)
domain/activity/          Créneaux libres (FreeSlots), semaine lun→dim, choix d'activité
ui/session/               Caméra frontale (CameraX) + chrono d'une session
ui/habits/                Écran d'édition des activités
alarm/                    AlarmScheduler (setAlarmClock), receivers (alarme, boot/heure),
                          notifications, AlarmActivity (sonnerie), permissions
ui/settings/              Écran Réglages
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
  extraites du calendrier 4A (couleurs des cellules du .docx). Cases gris foncé = école.
- Fichier ICS réel : non commité (données perso). Fixture synthétique :
  `app/src/test/resources/sample_ade.ics`. Test sur le vrai export :
  `ADE_ICS=/chemin/ADECal.ics ./gradlew testDebugUnitTest` (`RealIcsTest`).
- Debug uniquement : HTTP autorisé vers `10.0.2.2` (hôte de l'émulateur) pour
  tester la synchro avec un serveur local (`src/debug/res/xml/network_security_config.xml`). Format ADE observé :
  `SUMMARY` = matière, `LOCATION` = salle, `DESCRIPTION` = groupes + enseignant
  (`IT4 FISA`, `IT4 FISA - Cyber`, `IT4 FISA - IOT`…). L'export contient des cours
  de plusieurs groupes qui se chevauchent → filtrage par groupe nécessaire (voir SPEC).
