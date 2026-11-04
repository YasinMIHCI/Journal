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

### Week-end (et jours libres : fériés, école sans cours, hors calendrier)
- L'utilisateur saisit son heure de coucher (écran du jour, pour la nuit précédente).
- Réveil = coucher + endormissement (15 min) + n cycles (1 h 30), n le plus grand
  possible sans dépasser **10h30** ni le max de cycles (6 = 9 h). Tout réglable.
- Coucher ≥ midi = veille au soir ; < midi = après minuit.
- Moins d'un cycle possible ou coucher non saisi → réveil à 10h30.

### Type de journée
Priorité : week-end > férié > école > entreprise ; hors périodes = libre.

## 3. Synchro emploi du temps
- Source ADE (plannings.ube.fr), export ICS.
- Synchro automatique quotidienne vers 6h00 (+ manuelle).
- URL saisie dans les réglages (ne pas la committer).

## 4. Activités (piano, dessin…)
- Chaque activité a un objectif hebdo (ex. 1 h / semaine).
- L'app trouve des trous entre 8h et 20h (hors cours / travail / trajets) et envoie une notif.
- Clic sur la notif → caméra frontale (je me vois) + chrono.
- Fin de session → temps réel soustrait du restant de la semaine.

## 5. Journal
- Un texte par jour, sous la journée. Sauvegarde auto.

## 6. Général
- Tous les paramètres modifiables dans l'app.
- UI simple, textes courts.

## Décisions (ex-questions ouvertes)
- [x] Partage « agenda » ADE : Partage → Agenda donne une URL ICS directe, saisie dans les réglages.
- [x] Groupes gardés : IT4 Cyber → `IT4 FISA` (tronc commun) + `IT4 FISA - Cyber` ; `IT4 FISA - IOT` exclu. Filtre modifiable dans les réglages.
- [x] Cases gris foncé (3-7 mai 2027, 1-3 sept. 2027) : école.
- [x] Notifs activités : pas de limite par jour, à chaque créneau libre. Durée min. d'un créneau : réglage (défaut 30 min).
- [x] Semaine = lundi → dimanche.
