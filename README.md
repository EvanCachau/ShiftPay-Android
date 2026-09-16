# ShiftPay Android

Application Android de suivi des heures, heures supplémentaires et gains estimés.

## Configuration
- Package: `com.shiftpay.app`
- minSdk: 23
- targetSdk: 36 (Android 16)
- compileSdk: 36
- Google Mobile Ads SDK: 25.4.0
- Google UMP SDK: 4.0.0

## Publicités
Le projet utilise uniquement les IDs de démonstration Google AdMob.
Avant publication, remplace :
- l'App ID dans `AndroidManifest.xml`
- l'ID banner dans `MainActivity.java`
- l'ID interstitiel dans `MainActivity.java`

Ne publie pas l'application avec les IDs de test.

## Confidentialité
UMP est initialisé au lancement. Il faut aussi créer le message de consentement dans
AdMob > Privacy & messaging avec le vrai App ID avant mise en production.

## Build
Ouvre le projet dans Android Studio, ou pousse-le sur GitHub : le workflow
`.github/workflows/android-build.yml` produit un APK debug et un AAB release non signé.
