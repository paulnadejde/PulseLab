# Changelog

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
