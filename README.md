# AquaRitm 0.1

Aplicație Android offline cu două motoare care pot funcționa simultan:

- metronom secvențial cu 1–4 perioade;
- generator sinusoidal monoaural/binaural, zgomot și program de frecvență din CSV.

## Cerințe

- Android 8.0 sau mai nou (`minSdk 26`);
- optimizat inițial pentru Android 14;
- căști stereo pentru modul binaural.

Aplicația nu cere acces la internet. Selectarea fișierelor se face prin selectorul
Android, iar aplicația primește acces numai la fișierele alese explicit.

## Funcții implementate în 0.1

### Metronom

- patru secvențe în secunde, activate individual;
- click la fiecare secundă și clopoțel sintetic scurt la capătul secvenței;
- volume separate;
- incrementare/decrementare manuală aditivă sau multiplicativă;
- baza multiplicativă este memorată la selectarea modului multiplicativ;
- Start/Pauză; reluarea pornește de la prima secvență activă;
- resetarea confirmată pune toate duratele la o secundă;
- timer cumulativ de sesiune, cu reset separat;
- zece preseturi locale;
- menținerea opțională a ecranului aprins.

### Generator

- cinci dialuri pentru purtătoare, rezoluție 0,1 Hz, interval 0–9999,9 Hz;
- cinci dialuri pentru diferență, rezoluție 0,01 Hz, interval 0–999,99 Hz;
- limitarea diferenței la jumătatea purtătoarei;
- binaural: `stânga = purtătoare − diferență/2`, `dreapta = purtătoare + diferență/2`;
- monoaural: purtătoarea este redată identic în ambele canale, fără aplicarea diferenței;
- volum propriu și limitator moale la ieșire;
- zgomot alb, roz sau brun cu volum separat;
- piesă audio aleasă de utilizator și reluată în buclă;
- sesiune constantă temporizată sau vector CSV;
- Start, Pauză/Reluare și Stop, cu oprirea semnalului suprapus;
- stroboscop sinusoidal alb, roșu, verde, albastru sau chihlimbar;
- avertisment la activarea stroboscopului;
- menținerea opțională a ecranului aprins.

## Format CSV

Separatorul poate fi virgulă sau punct și virgulă. Separatorul zecimal este
punctul. Antetul este opțional.

```csv
duration_seconds,frequency_hz,transition_seconds
300,10.00,30
600,7.83,60
300,4.00,0
```

Fiecare rând menține frecvența pentru `duration_seconds`, apoi face o tranziție
liniară către frecvența rândului următor în `transition_seconds`. Tranziția
ultimului rând este inclusă în durata totală și menține ultima frecvență.

## Construire

Deschide directorul proiectului în Android Studio, lasă sincronizarea Gradle să
se încheie, apoi folosește **Build → Build APK(s)**. Alternativ, cu Gradle 8.9
instalat:

```bash
gradle :app:assembleDebug
```

APK-ul rezultat se găsește în `app/build/outputs/apk/debug/`. Proiectul nu
include binarul `gradle-wrapper.jar`; acesta poate fi generat local cu
`gradle wrapper --gradle-version 8.9`.

Fișierul `.github/workflows/build-apk.yml` construiește automat același APK la
fiecare actualizare a ramurii `main` sau la pornirea manuală a workflow-ului.

Workflow-ul `build-release.yml` construiește separat un APK release semnat cu
cheia permanentă din GitHub Actions Secrets. Pornirea manuală produce un
artifact privat; publicarea unui tag `v*` creează și un GitHub Release.

## Observații 0.1

- Clickul și clopoțelul sunt sintetizate intern. Încărcarea unor WAV-uri
  personalizate este rezervată versiunii 0.2, pentru a le decoda și mixa în
  același buffer fără decalaje.
- Editorul vizual de vectori este rezervat versiunii 0.2; importul CSV este
  funcțional în această versiune.
- Precizia auditivă trebuie verificată pe dispozitiv fizic, deoarece emulatorul
  nu reproduce fidel DAC-ul și politicile audio ale telefonului.
