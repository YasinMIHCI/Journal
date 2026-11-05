# Spécification

## 1. Type de journée
Trois types : **Entreprise**, **École**, **Week-end** (+ fériés).
Source : `assets/periods_4a_2026_2027.json` (modifiable dans l'app).

## 2. Réveil et départ (alarmes fiables, app fermée / téléphone en veille)
### Entreprise
| Jours | Réveil | Départ | Retour |
|---|---|---|---|
| Lun, Mer | 6h15 | 7h05 | ~18h00 |
| Mar, Jeu, Ven | 6h45 | 7h40 | ~17h30 |

### École
- Premier cours lu depuis l'ICS.
- Départ = début du premier cours − 30 min (trajet).
- Réveil = départ − 50 min (préparation).
- Ex. : cours 8h15 → départ 7h45 → réveil 6h55.
- Aucun cours connu ce jour-là (synchro ratée, export incomplet) : premier cours
  supposé à 8h00 (réglable) → réveil 6h40. Signalé sur l'écran du jour.

### Week-end (et jours libres : fériés, hors calendrier)
- L'utilisateur saisit son heure de coucher (écran du jour, pour la nuit précédente).
- Réveil = coucher + endormissement (15 min) + n cycles (1 h 30), n le plus grand
  possible sans dépasser **10h30** ni le max de cycles (6 = 9 h). Tout réglable.
- Coucher ≥ midi = veille au soir ; < midi = après minuit.
- Moins d'un cycle possible ou coucher non saisi → réveil à 10h30.

### Alarmes
- Réveil : `AlarmManager.setAlarmClock` (exact, traverse Doze, visible par le système).
  Sonnerie = notification canal « Réveil » (son d'alarme en boucle, vibration) +
  écran plein par-dessus le verrouillage ; « Arrêter » ; coupure auto après 10 min.
- Départ : notification à l'heure de départ (alarme exacte).
- Une alarme de chaque type programmée à la fois (la prochaine sur 8 jours),
  recalculée à chaque changement (réglages, coucher, synchro), après chaque
  sonnerie, au redémarrage du téléphone et au changement d'heure.
- Jours libres sans coucher saisi : réveil à l'heure « au plus tard » (10h30).
- Réveil et rappel de départ désactivables dans Réglages.

### Type de journée
Priorité : week-end > férié > école > entreprise ; hors périodes = libre.

## 3. Synchro emploi du temps
- Source ADE (plannings.ube.fr), export ICS.
- Synchro automatique quotidienne vers 6h00 (+ manuelle). Heure réglable.
- URL saisie dans les réglages (ne pas la committer).
- Tous les cours sont stockés ; filtre à la lecture : un cours est gardé si une ligne
  de sa DESCRIPTION est exactement un des groupes (défaut : `IT4 FISA`,
  `IT4 FISA - Cyber`, `4A`). Changer les groupes s'applique sans resynchroniser.
- « Travail personnel » compte comme un cours (premier cours → réveil).
- En cas d'échec, les cours précédents sont gardés.

## 4. Activités (piano, dessin…)
- Chaque activité a un objectif hebdo (ex. 1 h / semaine).
- L'app trouve des trous entre 8h et 20h (hors cours / travail / trajets) et envoie une notif.
- Clic sur la notif → caméra frontale (je me vois) + chrono.
- Fin de session → temps réel soustrait du restant de la semaine.

Règles retenues :
- Créneau = fenêtre 8h–20h (réglable), jamais avant le réveil, durée ≥ 30 min (réglable).
  - Entreprise : occupé du départ au retour.
  - École : occupé du départ à fin du dernier cours + trajet (pas de créneau entre deux cours).
    Sans cours connu : aucun créneau.
  - Jour libre : libre dès le réveil.
- Notification au début de chaque créneau (pas de limite par jour), pour l'activité
  à qui il reste le plus de temps cette semaine ; rien si tous les objectifs sont atteints.
- Session : chrono avec pause ; « Terminer » enregistre, retour = abandon (confirmation).
- Supprimer une activité = l'archiver (les sessions passées restent).

## 4 bis. Vue semaine et blocs perso
- Accueil = semaine lun → dim façon ENT : colonnes par jour, blocs dont la hauteur suit
  la durée. Couleurs : école (cours + trajets), entreprise (départ → retour), tags perso.
  Ligne du réveil, ligne « maintenant ». Toucher un jour → vue jour (réveil, activités, journal).
- Tags perso (ex. Volley) : nom, couleur, trajet. Un bloc a un trajet avant et après
  (même couleur, plus pâle).
- Bloc ponctuel (une date) ou chaque semaine (jours choisis, date de début, fin optionnelle).
- Effets : blocs + trajets occupés pour les créneaux libres. Le réveil laisse la
  préparation (réglage École > Préparation) avant le premier bloc/trajet du jour ;
  un jour libre, les cycles sont recalculés sous cette limite.
- Chaque activité peut avoir son son de notif de créneau (un canal Android par son).

## 5. Journal
- Un texte par jour, sous la journée. Sauvegarde auto.

## 6. Général
- Tous les paramètres modifiables dans l'app.
- UI simple, textes courts.

## Décisions (ex-questions ouvertes)
- [x] Partage « agenda » ADE : Partage → Agenda donne une URL ICS directe, saisie dans les réglages.
- [x] Groupes gardés : `IT4 FISA` (tronc commun) + `IT4 FISA - Cyber` + `4A` (événements promo) ; `IT4 FISA - IOT` exclu. Modifiable dans les réglages.
- [x] Cases gris foncé (3-7 mai 2027, 1-3 sept. 2027) : école.
- [x] Notifs activités : pas de limite par jour, à chaque créneau libre. Durée min. d'un créneau : réglage (défaut 30 min).
- [x] Semaine = lundi → dimanche.
