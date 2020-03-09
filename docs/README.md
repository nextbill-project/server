**Das OpenSource-Projekt NextBill hilft Ihnen bei der Kostenaufteilung mit dem Partner, WG-Mitbewohnern und Freunden. Unterstützt durch intelligente Funktionen, durchdachtem Workflow und einer Android-App, wird die Kostenaufstellung zum Kinderspiel. Und das alles kostenlos und auf Ihrem eigenen Server, NAS oder Desktop-System.**

![](https://raw.githubusercontent.com/nextbill-project/server/master/docs/assets/top_devices.png)

## Argumente gefällig? ##

**Sichere Datenspeicherung** Speichern Sie Ihre Rechnungen, Quittungsbilder und Kostenabrechnungen sicher auf Ihrem Server, NAS oder Desktop-Computer

**Jederzeit Datenzugriff** Zugriff von überall und jederzeit über Android App (auch offline) oder einen Browser

**Intelligente Eingabehilfen** Automatische Vervollständigung sowie E-Mail-Empfang, Bildupload und Spracheingabe unterstützen Sie bei der Erfassung der Daten

**Rechnungen aufteilen** Teilen Sie Rechnungen und erstellen Sie Abrechnungen für den Partner, Mitbewohner oder Freund

**Abrechnungs-Workflow** Behalten Sie immer den Überblick über noch nicht gezahlte Rechnungen und den Workflow der Kostenabrechnungen

**Kostengrenzen definieren** Erstellen Sie Budgets und werden Sie automatisch gewarnt, wenn diese überschritten werden

**Datenanalyse** Analysieren Sie Ihre Daten anhand von Diagrammen und Schlüsselwerten oder sehen Sie sich Prognosen an

**Benutzerverwaltung** Verwalten Sie die Benutzer und geben Sie ihnen ganz nach Bedarf unterschiedliche Rechte


## Installation
### Einrichtung der Umgebung
Die Java-Architektur erlaubt es, NextBill auf fast jedem System zu installieren. 
Hinsichtlich des Zugriffs lassen sich im wesentlichen zwei Szenarien unterscheiden:

1. Sie installieren NextBill auf Ihrem NAS- oder Desktop-Computer (Windows, Mac oder Linux) zu Hause. In diesem Fall haben Sie nur im lokalen Netzwerk Zugriff. NextBill ist dann über eine IP in jedem Browser und in der Android-App erreichbar. Sobald Sie das Haus verlassen, schaltet die Android-App in den Offline-Modus, synchronisiert sich aber bei der Rückkehr.

2. Sie installieren NextBill auf einem (Cloud-) Server. In diesem Fall können Sie von überall aus über einen Browser oder die Android-App zugreifen. Bedenken Sie aber, dass eine solche Konfiguration tendenziell höhere Sicherheitsrisiken birgt.

### Setup auf dem System
1. Version 1.8 der Java-Laufzeitumgebung installieren [Download](https://www.oracle.com/java/technologies/javase-jre8-downloads.html)
2. Laden Sie über die GitHub-Releases Seite die aktuelle Version der jar herunter und kopieren Sie sie in einen Ordner Ihrer Wahl.
3. **Unter Windows oder Mac** Doppelklicken Sie auf die Jar, warten Sie 10 Sekunden
**Unter Linux oder ohne Bildschirm** Öffnen Sie ein Terminal und geben Sie ein:
```
cd 'Pfad/zur/jar'
java -jar nextbill.jar
```
4. Öffnen Sie danach einen Browser mit der URL [http://localhost:8010](http://localhost:8010) und folgen Sie dem Setup.
5. Laden Sie bei Bedarf die Android-App aus dem Play Store und hinterlegen Sie dort die IP des Server-Systems.

Sie können NextBill auch bei jedem Systemstart ausführen. Weitere Details dazu finden Sie bald im Wiki.

## Entstehung

Gestartet 2016 als privates Projekt, begann NextBill schnell an Funktionsumfang weit über den privaten Bedarf hinaus zuzunehmen. Fokus war-  damals wie heute - neben der Grundfunktion des komfortablen Kosten aufteilens zwischen Partnern, WG-Mitgliedern oder Freunden, lästige Eingaben durch intelligente Helfer zu vereinfachen und dabei dennoch hohen Datenschutz-Ansprüchen gerecht zu werden. Insbesondere aufgrund des zweiten Aspekts stand früh fest, dass sich eine sichere Cloud-Lösung - bestehend aus Server-Anwendung, Web-App und Android-App - nur über eine Self-Hosted-Infrastruktur lösen lässt. Seit 2019 wird intensiv daran gearbeitet die Installation und Nutzung auch für technisch unerfahrene Nutzer zugänglicher zu machen. 

Im Frühling 2020 wurde NextBill auf GitHub unter der AGPL v3 Lizenz veröffentlicht und profitiert somit in Zukunft von einer aktiven Community, kurzfristigem Feedback und schneller Identifizierung von Sicherheitslücken.

## Support

Sie haben Fragen oder einen Fehler gefunden? Dann orientieren Sie sich bitte an folgendem Leitfaden:

1. Falls Sie es noch nicht getan haben: Versuchen Sie durch ausprobieren selbstständig auf eine Lösung zu kommen. :-P
2. Konsultieren Sie das FAQ auf dieser Seite. Viele Fragen können dort schon beantwortet werden, außerdem werden stetig Antworten ergänzt.
3. Durchstöbern Sie die Liste der bereits gefundenen Issues auf der Github-Seite. Eventuell wurde der Fehler ja bereits erfasst und befindet sich schon auf der Roadmap.
4. Schreiben Sie ein neues Issue, bitte orientieren Sie sich dabei am zur Verfügung gestellten Template. 
5. Falls Sie glauben, dass es sich um eine sehr spezielle Frage handelt, schreiben Sie bitte eine E-Mail an: scansio@gmx.de. Bedenken Sie, dass eine Antwort mehrere Wochen dauern kann.

## Spenden

In die Entwicklung von NextBill ist viel (Frei-) Zeit geflossen. Rechnerisch wurde sich schon jetzt über 1000 Stunden durchgehend mit dem Projekt beschäftigt.
Keiner sollte sich nun verpflichtet fühlen dafür eine Gegenleistung zu erbringen. NextBill ist und bleibt kostenlos und OpenSource. Das impliziert auch, dass die größte Würdigung der Leistung darin liegt, dass das Projekt (aktiv) genutzt wird und alle Wünsche zufriedenstellend erfüllt worden sind.
Dennoch ist jede Spende willkommen - nicht nur als Anerkennung des Gesamtwerks, sondern auch als Motivation NextBill weiterhin voran zu treiben und zu einem nachhaltigen Projekt auszubauen.

Spenden können gerne über die PayPal.me-Adresse des Gründers getätigt werden:
[paypal.me/MichasKonto](https://www.paypal.com/paypalme2/MichasKonto)
Schon im Voraus vielen herzlichen Dank für Ihre Unterstützung!

## FAQ

**Ist NextBill wirklich kostenlos?**

Ja, NextBill ist kostenlos und vollständig OpenSource. Entsprechend den Grundsätzen werden auch zu keinem Zeitpunkt persönliche Daten erhoben oder weitergegeben. Alle Ihre eingegebenen Daten werden ausschließlich in Ihrer eigenen NextBill-Datenbank und allen mit Ihrer NextBill-Instanz verbundenen Android-Apps gespeichert.
Es ist allerdings in Zukunft geplant eine kostenpflichte Zusatzfunktion anzubieten, die es erlaubt Quittungen automatisch analysieren bzw. auslesen zu lassen. Diese Funktion wird optional sein und Kosten werden ausschließlich, selbstverständlich vollständig transparent, bei aktiver Inanspruchnahme dieser Leistung entstehen. Nähere Infos auf https://scansio.de.

**Was ist ein Zahlungsempfänger, ein Geldgeber und was ein Kostenträger?**

Im Alltag wird Ihnen der Zahlungsempfänger am häufigsten begegnen. Dabei handelt es sich um das Geschäft, bei dem Sie einen Einkauf getätigt haben. Ein Geldgeber ist bspw. ihr Arbeitgeber. Eine Besonderheit stellt der Kostenträger dar, dieser ist im Falle einer Ausgabe (finanzsprachlich ausgedrückt) der eigentliche "Schuldner" und bei einer Einnahme der "Gläubiger". Am besten lassen sich die begrifflichen Zusammenhänge anhand eines Beispiels erklären: Sie (=implizit "Geldgeber") kaufen in einem Geschäft X (="Zahlungsempfänger") etwas ein, machen dies aber nicht nur für sich, sondern auch für einen Freund. Das heißt Sie wollen die Kosten mit ihm teilen. Sie tragen in diesem Fall sich und Ihren Freund als "Kostenträger" ein.

**Wie kann eine Einnahme aufgeteilt werden?**

Das in der vorherigen Frage erklärte Prinzip funktioniert auch, wenn Sie eine Einnahme, bspw. ihr Gehalt, mit Ihrem Freund teilen möchten. In diesem Fall ist Ihr Arbeitgeber der Geldgeber, Sie sind (nicht explizit sichtbar) der Zahlungsempfänger und Sie tragen sich und Ihren Freund als "Kostenträger" ein. Das Wort "Kostenträger" passt leider nicht optimal, allerdings ist es schwierig einen verständlichen Oberbegriff zu finden. Ideen sind willkommen!

**Was bedeutet im "Kostenverteilung ändern"-Dialog die Option "Anteil am Rest"?**

Dazu zwei Beispiele:
- Beispiel 1: Die Rechnung hat die Gesamtsumme von 10 Euro. Person A hat als Option "Prozent" mit 50% ausgewählt (=5 Euro). Person B und C wollen die Restkosten (von 5 Euro) gleichmäßig aufteilen. Hierzu wird für B und C die Option "Anteil am Rest" ausgewählt.
- Beispiel 2: Die Rechnung hat die Gesamtsumme von 10 Euro. Person A hat als Option "Prozent" mit 50% ausgewählt (=5 Euro) . Person B will 50% vom Anteil der Person C übernehmen. Es wird daher für B und C die Option "Anteil am Rest" ausgewählt und für Person B 1,5 eingetragen (=3,75 Euro) und für Person C 0,5 (=1,25 Euro).

**Was ist der Unterschied zwischen einer Rechnung und einer Abrechnung?**

Eine Rechnung ist der Nachweis einer Ausgabe, bspw. die Quittung von einem Einkauf.
Eine Abrechnung ist - zumindest in NextBill - etwas völlig anderes. Es bezeichnet den Vorgang, der stattfindet, wenn Sie Ihre und die Ausgaben/Einnahmen Ihres Freundes verrechnen wollen. In diesem Fall wird eine Abrechnung erstellt, die darstellt welche Ausgaben oder Einnahmen wer geleistet hat und wer zum Schluss wem was zahlen muss.

**Was bedeuten die Reiter "Offen" und "Überprüft"?**

Offene Rechnungen werden weder bei der Datenauswertung berücksichtigt, noch ist eine Abrechnung möglich. Erst wenn sowohl der Ersteller als auch der Kostenträger eine Rechnung "überprüft" hat, ist eine Abrechnung möglich.

**Es wird in meiner Liste eine Rechnung nicht angezeigt, obwohl ich ein Kostenträger bin.**

In diesem Fall ist die Rechnung beim Ersteller noch im Status "offen" oder sie wurde wieder in diesen Status versetzt.

**Der Wert "Rest am Monatsende" ist leer, obwohl schon Rechnungen eingegeben wurden.**

Die Berechnung dieses Wertes findet immer nur einmal nachts statt. Entweder lief NextBill zu diesem Zeitpunkt nicht oder die Rechnungen sind noch zu "frisch".

**Gibt es auch eine Quittungs-Analyse, die automatisch Summe, Geschäft usw. ermittelt?**

Jaein, aber es befindet sich eine in der Alpha-Phase! Auch hierbei handelt es sich um eine Eigenentwicklung mit den gleich hohen Anforderungen an den Datenschutz wie bei NextBill. Das Produkt wird den Namen "Scansio" tragen und kostenpflichtig sein, aber zu einem fairen Preis angeboten werden. Näheres wird verkündet, sobald die Alpha-Phase beendet ist.
