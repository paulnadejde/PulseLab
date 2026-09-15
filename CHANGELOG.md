# Changelog

## 0.1.14

- conectează butonul Help la catalogul online de documentație AquaRitm;
- afișează automat documentele disponibile, cu formatul și dimensiunea lor;
- deschide documentele prin browserul sau vizualizatorul instalat în Android;
- validează că documentele provin prin HTTPS de pe domeniul aquanano.eu.


## 0.1.13

- face obligatorii toate cele cinci valori numerice din presetul de frecvențe;
- folosește perechea 0 Hz / 0% pentru a marca o componentă auxiliară absentă;
- respinge explicit celulele goale și volumele nenule fără frecvență.


## 0.1.12

- adaugă volum independent 0–100% pentru fiecare componentă monoaurală, implicit 20%;
- normalizează suma ponderată și memorează volumele împreună cu frecvențele personalizate;
- adaugă presetul CSV cu cinci parametri pentru fundamentala și cele două componente opționale;
- încarcă preseturi de frecvențe din catalog, le memorează și actualizează toate dialurile și volumele;
- extinde catalogul bibliotecii cu directorul „frecvente” și SHA-256 pentru resurse.


## 0.1.11

- conectează secțiunea Update la catalogul din directorul „aplicatie”;
- compară versiunea instalată cu versiunea marcată automat drept „latest”;
- descarcă actualizarea prin managerul Android și păstrează progresul între deschideri;
- verifică SHA-256 înainte de a permite instalarea;
- deschide instalatorul Android și gestionează permisiunea pentru surse externe.


## 0.1.10

- adaugă pagina centrală „Setări AquaRitm”, accesibilă prin meniul ☰;
- oferă economii selective: stingerea ecranului, actualizare UI mai rară și dezactivarea stroboscopului;
- adaugă scurtături către setările Android pentru rețele, mod avion, Bluetooth, locație, ecran și baterie;
- pregătește secțiunile Update și Help și adaugă închiderea confirmată a aplicației și a serviciului audio.


## 0.1.9

- permite înlocuirea independentă a componentelor `2 × f0` și `3 × f0` cu frecvențe proprii;
- adaugă câte un dialog compact cu dial și bifa „Folosește această frecvență”;
- memorează activarea componentelor, frecvențele proprii și modul lor de utilizare;
- păstrează normalizarea comună și semnalul identic pe ambele canale monoaurale.


## 0.1.8

- adaugă opțional armonica a doua și armonica a treia în modul monoaural;
- normalizează suma componentelor pentru a păstra semnalul în domeniul PCM;
- păstrează semnalul rezultat identic pe canalele stâng și drept, fără folosirea lui `fm`;
- ignoră armonicile care ar depăși frecvența Nyquist la 48 kHz.


## 0.1.7

- folosește `catalog_aquaritm.php`, generat automat din directoarele Web Disk;
- separă complet biblioteca de vectori de biblioteca de sunete;
- salvează și selectează independent vectorii și piesele pentru utilizare offline.


## 0.1.6

- adaugă un catalog online pentru vectori și piese audio opționale;
- descarcă și verifică SHA-256 pentru fișierele catalogului;
- păstrează presetările descărcate local și le permite să fie încărcate offline;
- limitează descărcările la HTTPS de pe domeniul `aquanano.eu`.

## 0.1.5

- adaugă purtătoarea `f0` în formatul vectorial cu patru coloane;
- interpolează simultan `f0` și `fm` între etape;
- păstrează compatibilitatea cu CSV-urile vechi cu trei coloane, care folosesc purtătoarea din dial;
- actualizează ambele dialuri și diagnosticul vectorial în timpul rulării;
- corectează inițializarea și resetarea etichetei PAUZĂ/REIA.

## 0.1.4

- corectează păstrarea bifei vectoriale după import și activează automat vectorul încărcat;
- acceptă antetul CSV după linii goale sau comentarii și fișiere cu marcaj BOM;
- afișează în timp real frecvența vectorială, etapa și faza curentă;
- adaugă grafic vectorial full-screen în landscape, cu cursor și frecvență curentă;
- permite denumirea celor zece preseturi de metronom;
- evidențiază tabul activ și adaugă butoane colorate cu stare apăsată.

## 0.1.3

- numele afișat al aplicației devine AquaRitm;
- adaugă iconul propriu cu cele șapte sfere și puls violet;
- păstrează identificatorul Android și cheia release pentru actualizări.

## 0.1.2

- corectează definiția modului monoaural: ambele canale redau exclusiv
  purtătoarea, iar frecvența de diferență nu intră în semnalul audio;
- păstrează comutarea imediată Binaural/Monoaural din 0.1.1.

## 0.1.1

- selecția Binaural/Monoaural este aplicată imediat, inclusiv în timpul redării;
- modul monoaural continuă să trimită mixul identic către ambele canale.

## 0.1.0

- Primul prototip Android.
- Motor PCM stereo comun pentru generator și metronom.
- Metronom cu patru secvențe, reglaje manuale, timer și zece preseturi.
- Generator monoaural/binaural, trei culori de zgomot și muzică în buclă.
- Sesiuni constante și vectoriale CSV.
- Stroboscop sinusoidal cu avertisment.
- Rulare audio în serviciu foreground și opțiune de menținere a ecranului activ.
