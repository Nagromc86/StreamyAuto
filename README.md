# StreamyAuto

Prototype IPTV personnel (Xtream Codes) pour Android Auto, en sideload.

## Télécharger l'APK

Chaque push sur `main` compile l'APK via GitHub Actions. Elle est publiée dans
l'onglet **Releases** (`StreamyAuto-<n>.apk`).

## Installation

1. Installe l'APK sur le téléphone (autorise l'installation d'apps inconnues).
2. Android Auto → Paramètres → appuie 10 fois sur « Version » → menu ⋮ →
   Paramètres développeur → coche « Sources inconnues ».
3. Connecte le téléphone à la voiture et lance StreamyAuto depuis le lanceur.
4. Accepte la demande d'accès à la vitesse du véhicule sur le téléphone.

## Comportement vidéo

- La vidéo s'affiche quand la voiture est à l'arrêt (vitesse nulle depuis 3 s).
- Dès que la voiture roule, la vidéo est coupée et le son continue.
- Si la voiture ne transmet pas sa vitesse, la lecture reste en audio seul.

## Utilisation

Une seule application, deux affichages :

- **Téléphone** : onglet *Comptes* (ajout d'un compte Xtream Codes, bouton *Tester*),
  onglet *Chaînes* (catégories, recherche, ☆ pour ajouter aux favoris, appui pour lire),
  onglet *Favoris* (ordre ▲▼, suppression ✕).
- **Android Auto** : les 12 premiers favoris, dans l'ordre choisi sur le téléphone.
  Lecteur avec précédent / lecture-pause / suivant / fermer.

Si une chaîne ne se lance pas, change le format du compte (HLS `.m3u8` ou MPEG-TS `.ts`).
Pour changer de format, supprime le compte et ajoute-le à nouveau.

Note : les identifiants Xtream sont stockés en clair dans la base locale de l'app.
