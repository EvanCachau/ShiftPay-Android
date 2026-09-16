# Notes — Sécurité des données Google Play

État de l'application ShiftPay 1.2 :

- Les horaires, pauses, paramètres de salaire et historiques sont stockés localement dans la WebView sur l'appareil.
- ShiftPay n'a pas de compte utilisateur ni de serveur applicatif pour ces données dans cette version.
- Google Mobile Ads (AdMob) est intégré pour la publicité.
- Google UMP est intégré pour la gestion du consentement lorsque nécessaire.
- La déclaration Data Safety doit tenir compte des données traitées par le SDK Google Mobile Ads (par ex. identifiants/appareil, diagnostics, interactions, IP selon configuration et consentement).
- Vérifier les déclarations finales dans Play Console au moment de la soumission, car elles dépendent de la configuration AdMob/SDK effective.
