# Changelog

## 0.1.29

- adaugă în MindExtra o creștere opțională a volumului la începutul sesiunii;
- adaugă o scădere opțională a volumului în ultimele minute ale sesiunii;
- memorează separat procentele și duratele celor două variații;
- interpolează lin volumul între cele două rampe când valorile lor intermediare diferă;
- aplică anvelopa numai generatorului binaural, fără a modifica semnalul suprapus;
- validează procentele, durata finită necesară rampei finale și suprapunerea rampelor;
- păstrează poziția anvelopei în pauză și o reia odată cu sesiunea.

## 0.1.28

- adaugă în Setări comanda „Distribuie AquaRitm”;
- deschide selectorul standard Android pentru trimiterea paginii publice prin aplicația aleasă de utilizator;
- adaugă pagina publică `aquaritm.php`, care citește dinamic ultima versiune din catalogul aplicației.

## 0.1.27

- condiționează afișarea ideogramelor SolaRitm de un acces master sau client validat;
- adaugă în Setări SolaRitm opțiunea „Folosește parola master” și memorarea validării;
- ascunde complet panoul ideogramelor și oprește agentul din bara de stare fără acces valid;
- păstrează active calculele astronomice, evenimentele solare și calculul ciclurilor;
- pregătește separat starea de acces pentru viitoarea parolă client;
- blochează și estompează toate controalele de acces după activare;
- injectează amprenta parolei master numai la compilare, din secretul mediului GitHub.

## 0.1.26

- sincronizează finalul fiecărei secvențe cu grila reală a bătăilor metronomului;
- înlocuiește permanent ultimul click al secvenței cu clopoțelul, fără suprapunere;
- elimină asincronia audibilă a clopoțelului la intervalele multiplicative fracționare.


## 0.1.25

- redefinește modul multiplicativ ca modificare a intervalului comun dintre bătăi, păstrând bazele etapelor neschimbate;
- afișează intervalul curent al bătăii și duratele efective rezultate;
- păstrează în modul aditiv bătaia fixă la o secundă și modifică uniform valorile etapelor;
- blochează modul, bazele și activarea etapelor după START, inclusiv în pauză;
- deblochează configurarea numai prin RESET VALORI sau încărcarea unui preset;
- salvează în preset modul de ritmare și intervalul multiplicativ.


## 0.1.24

- recapturează baza multiplicativă după editarea manuală a oricărei secvențe;
- resetează progresul multiplicativ când este definit un set nou de valori sau este încărcat un preset;
- împiedică actualizările automate ale câmpurilor să fie confundate cu editări făcute de utilizator.


## 0.1.23

- limitează reglajul aditiv la 1…10 secunde, în pași întregi;
- permite reglaj multiplicativ fracționar între 0,1× și 2,0×, în pași de 0,1×;
- păstrează intern duratele proporționale fără rotunjire la secunde întregi și le afișează la o zecimală;
- crește rezoluția tranzițiilor metronomului de la o secundă la aproximativ 8 ms.


## 0.1.22

- fixează ovalul indigo al ideogramelor SolaRitm în orientare verticală, inclusiv în notificarea agentului;
- adaugă documentația pentru utilizarea filei SolaRitm și a agentului din bara de notificări.


## 0.1.21

- redesenează pictograma MindExtra ca profil uman cu creier auriu și halo discret;
- mută comenzile Android pentru radio, rețea, Bluetooth, locație, ecran și baterie în fereastra „Setări sistem”;
- adaugă agentul SolaRitm opțional în bara de notificări, cu cele două ideograme actualizate în fundal;
- reconstruiește faza simbolurilor din ora sistemului după repaus sau relansarea serviciului;
- declară notificarea publică pentru ecranul blocat, fără sunet, vibrații ori insignă;
- oprește agentul împreună cu aplicația la comanda „Închide aplicația”.


## 0.1.20

- înlocuiește etichetele celor patru file cu butoane pătrate și pictograme dedicate;
- mută sursa locației și coordonatele manuale într-o fereastră separată de setări SolaRitm;
- adaugă selectorul memorat „Doar răsărit” / „Răsărit și apus”;
- introduce două cicluri simbolice 5→1, cu pași de 24 minute și 4 minute 48 secunde;
- resetează ambele cicluri exact la evenimentele solare selectate și păstrează faza după repornire;
- afișează ciclurile prin oval indigo, cerc verde, triunghi roșu, semilună argintie și pătrat galben.


## 0.1.19

- adaugă fila SolaRitm pentru calculul local al răsăritului și apusului;
- folosește locația telefonului prin GPS, rețea sau furnizorul disponibil, numai cât timp aplicația rulează;
- permite coordonate manuale memorate și păstrează sursa aleasă până la următoarea modificare;
- preia automat data, ora și fusul orar din Android și actualizează starea solară în timp real;
- afișează coordonatele active, precizia locației și timpul rămas până la următorul eveniment solar.


## 0.1.18

- reduce lățimea tuturor dialurilor de frecvență la 75% din spațiul disponibil;
- aliniază dialurile la stânga în BioStim, MindExtra și ferestrele frecvențelor auxiliare;
- lasă restul lățimii liber pentru derularea verticală a paginii.


## 0.1.17

- reduce fontul tuturor butoanelor la 75% din dimensiunea anterioară;
- micșorează și uniformizează înălțimea butoanelor în toate ecranele și meniurile;
- centrează butoanele singulare și păstrează spațieri egale în rândurile cu două sau trei butoane;
- aplică o spațiere verticală discretă între butoanele așezate unul sub altul.


## 0.1.16

- copiază automat vectorii CSV importați în spațiul persistent al aplicației;
- folosește numele fișierului drept nume al presetului local și îl selectează imediat;
- actualizează presetul existent când este reimportat un fișier cu aceeași denumire;
- păstrează vectorii importați disponibili după repornirea aplicației;
- micșorează fontul taburilor principale și păstrează etichetele pe un singur rând.


## 0.1.15

- separă generatorul monoaural și generatorul binaural în filele BioStim și MindExtra;
- elimină selectorul Monoaural/Binaural: START din instrumentul ales stabilește modul audio;
- păstrează sunetul curent neschimbat la simpla navigare între file;
- limitează componentele suplimentare și preseturile de frecvențe la BioStim;
- limitează vectorii CSV, graficul și stroboscopul la MindExtra;
- memorează separat frecvențele și durata sesiunii celor două instrumente;
- mută Catalog Online lângă sunetele locale, fiind comun vectorilor și pieselor audio.


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
