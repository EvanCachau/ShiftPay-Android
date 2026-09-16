# ShiftPay Android 1.2

Application Android de suivi des heures, pauses, heures supplémentaires et gains estimés.

## Configuration
- Package : `com.shiftpay.app`
- minSdk : 23
- targetSdk / compileSdk : 36
- Java : 17
- Google Mobile Ads SDK : 25.4.0
- Google UMP SDK : 4.0.0

## AdMob
Les builds sont séparés automatiquement :
- **debug** : IDs de démonstration Google AdMob uniquement ;
- **release** : IDs AdMob réels de ShiftPay.

Aucun changement manuel d'ID n'est nécessaire avant publication.

## Confidentialité / RGPD
- UMP est interrogé à chaque lancement.
- Le formulaire de consentement requis est affiché avant la demande d'annonces.
- Si Google indique qu'un point d'entrée de confidentialité est requis, un bouton **Gérer mes choix de confidentialité** apparaît dans Paramètres.
- La politique de confidentialité est accessible depuis l'app.

## Tests
Le workflow GitHub Actions :
1. construit l'APK debug ;
2. construit l'AAB release non signé ;
3. démarre un émulateur Android 16 ;
4. vérifie le lancement, le début/fin d'un shift et la persistance du stockage après redémarrage.

## Publication
Ne commitez jamais la clé de signature `.jks` ni ses mots de passe dans ce dépôt.
L'AAB produit par GitHub Actions reste non signé ; il doit être signé avec la clé d'upload avant envoi à Google Play.
