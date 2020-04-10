
## FAQ

**Ist NextBill wirklich kostenlos?**

Ja, NextBill ist kostenlos und vollständig OpenSource. Entsprechend den Grundsätzen werden auch zu keinem Zeitpunkt persönliche Daten erhoben oder weitergegeben. Alle Ihre eingegebenen Daten werden ausschließlich in Ihrer eigenen NextBill-Datenbank und allen mit Ihrer NextBill-Instanz verbundenen Android-Apps gespeichert.
Es wird aber eine kostenpflichtige Zusatzfunktion zur automatischen Erfassung von Rechnungen geben. Mehr Infos hier im FAQ unter "Gibt es auch eine Quittungs-Erfassung, die automatisch Summe, Geschäft usw. ermittelt?".

**Warum  kann sich die Android App nicht synchronisieren oder zeigt seltsame Fehler?**

Vermutlich sind Ihre App-Version und die Server-Version nicht (mehr) kompatibel. Auch wenn dies versucht wird zu verhindern - geplant ist zukünftig bei Inkompatibilität automatisiert darauf hinzuweisen - so kann dies in Ausnahmefällen nach einem Release passieren. Bitte aktualisieren Sie zur Problembehebung Ihre App über den PlayStore und Ihre NextBill Server Instanz, indem Sie einfach die alte Jar-Datei mit der neuesten Version ersetzen.

**Wieso unterscheidet sich der Funktionsumfang zwischen der Web-Applikation und der Android App?**

Generell werden Funktionen immer in die Web-Applikation eingebaut und erst dann wird entschieden, ob sie auch in der Android App verfügbar sein soll. Der Grund dafür ist schlichtweg, dass die native Android Entwicklung deutlich aufwendiger ist als die Web-Entwicklung und daher nicht alle Funktionen auf beiden Plattformen angeboten werden können.

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

**Gibt es auch eine Quittungs-Erfassung, die automatisch Summe, Geschäft usw. ermittelt?**

Jaein, aber es befindet sich eine in der Beta-Phase! Hierbei handelt es sich um eine Eigenentwicklung des initialen Entwicklers von NextBill mit den gleich hohen Anforderungen an den Datenschutz wie bei NextBill. Der externe Service heißt "Scansio" und wird kostenpflichtig sein. Näheres auf [scansio.de](https://scansio.de){:target="_blank"}. Die Veröffentlichung wird zeitnah hier und natürlich auf der Scansio-Homepage bekannt gegeben.
