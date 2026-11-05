# Sarcinile mele — Jetpack Compose + Room + Firebase

Aplicație de to-do construită după arhitectura din prezentare:

```
Compose (afișează starea)
   ↓ evenimente      ↑ stare
ViewModel (ține starea, fără Context)
   ↓
TodoRepository (alege sursa datelor)
   ↓                    ↓
Room (local)        Firebase (Auth: invitat + Google, Firestore)
```

- **Room** este sursa unică de adevăr: interfața citește doar din baza locală, deci aplicația pornește instant și merge offline.
- **Firebase Authentication**: la prima pornire aplicația creează un cont de invitat (anonim).
- **Login cu Google (OAuth 2.0 / OpenID Connect)**: din **Setări → Cont și sincronizare → Conectează-te cu Google**.
  Credential Manager deschide selectorul de conturi Google, utilizatorul se autentifică la Google, iar aplicația
  primește un ID token pe care Firebase îl schimbă într-o sesiune proprie (`auth/GoogleSignIn.kt`).
  - Contul de invitat este *legat* de contul Google (`linkWithCredential`): același uid, aceleași sarcini.
  - Dacă acel cont Google există deja (de pe alt telefon), aplicația intră în el și îi adaugă sarcinile de pe acest telefon.
  - **Deconectare**: sarcinile rămân în contul Google, telefonul continuă cu un invitat nou, cu listă goală.
- **Cloud Firestore** păstrează sarcinile în `users/{uid}/todos/{taskId}`. Fiecare modificare merge întâi în Firestore (care o pune în cache și o trimite când există internet), apoi în Room; un listener aduce înapoi în Room modificările făcute din consolă sau de pe alt dispozitiv.
- **Security Rules** (`firestore.rules`): fiecare utilizator își vede doar sarcinile lui, iar documentele trebuie să aibă exact forma scrisă de aplicație.

Fără `app/google-services.json`, aplicația se compilează și rulează doar cu Room; în Setări apare „Doar pe acest dispozitiv”.

## Conectare la Firebase

1. Creează un proiect în [Firebase Console](https://console.firebase.google.com/).
2. **Build → Authentication → Get started → Sign-in method**: activează **Anonymous** și **Google**
   (la Google alege un e-mail de suport și salvează).
3. **Build → Firestore Database → Create database**, pornește în **production mode**.
4. **Firestore → Rules**: copiază conținutul din `firestore.rules` și apasă **Publish**
   (sau, cu Firebase CLI: `firebase deploy --only firestore:rules`).
5. **Project settings → Add app → Android**, cu package name `com.example.demo1`.
6. Login-ul Google cere amprenta SHA-1 a cheii cu care e semnată aplicația. Rulează `./gradlew signingReport`
   și copiază `SHA1` de la varianta `debug` în **Project settings → Your apps → Add fingerprint**.
7. Descarcă **abia acum** `google-services.json` (după ce ai activat Google și ai pus SHA-1, ca să conțină
   Web client ID) și pune-l în folderul `app/`.
8. Rulează aplicația. În **Setări → Cont și sincronizare** apare „Cont de invitat” și butonul
   **Conectează-te cu Google**; sarcinile apar în consolă sub `users/<uid>/todos`.

Pe emulator, login-ul Google merge doar pe o imagine *Google Play* / *Google APIs* cu un cont Google adăugat în Setările telefonului.

## Build și teste

```bash
./gradlew testDebugUnitTest   # teste pentru Repository și ViewModel (fără emulator)
./gradlew assembleDebug
```
