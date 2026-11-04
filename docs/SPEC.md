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

### Week-end
- L'utilisateur saisit son heure de coucher.
- Réveil calculé pour un sommeil optimal (cycles), **au plus tard 10h30**.

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

## Questions ouvertes
- [ ] Partage « agenda » ADE : donne-t-il une URL ICS (synchro auto) ou seulement un fichier ?
- [ ] Filtrer les cours `IT4 FISA - IOT` (garder `IT4 FISA` + `IT4 FISA - Cyber`) ?
- [ ] Cases gris foncé du calendrier 4A (3-7 mai 2027, 1-3 sept. 2027) : école, entreprise, autre ?
- [ ] Notifs activités : fréquence max / jour, durée d'un créneau proposé ?
- [ ] Semaine = lundi → dimanche ?
