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

## État du prototype

- Les favoris Android Auto sont une liste de démonstration (flux de test Mux).
- Les écrans téléphone (comptes Xtream, chaînes, favoris) sont à brancher.
