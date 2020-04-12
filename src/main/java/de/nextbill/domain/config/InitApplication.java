/*
 * NextBill server application
 *
 * @author Michael Roedel
 * Copyright (c) 2020 Michael Roedel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.nextbill.domain.config;

import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.enums.InvoiceCategoryType;
import de.nextbill.domain.enums.Right;
import de.nextbill.domain.model.AppRight;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.model.InvoiceCategory;
import de.nextbill.domain.model.InvoiceCategoryKeyword;
import de.nextbill.domain.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class InitApplication {

    @Autowired
    private InvoiceCategoryRepository invoiceCategoryRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private AppRightRepository appRightRepository;

    @Autowired
    private InvoiceCategoryKeywordRepository invoiceCategoryKeywordRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @PostConstruct
    public void init(){
        initInvoiceCategories();
        initRights();
    }

    private void initRights(){

        for (Right value : Right.values()) {
            AppRight appRight = appRightRepository.findByCode(value);

            if (appRight == null) {
                appRight = new AppRight();
                appRight.setAppRightId(UUID.randomUUID());
            }
            appRight.setCode(value);

            appRightRepository.save(appRight);
        }
    }

    public void initInvoiceCategories(){

        InvoiceCategory parentInvoiceCategory = newICExpenseParentCategory("Wohnen", null);
        newICExpense("Miete & Hausgeld", parentInvoiceCategory, Arrays.asList("Miete"), Arrays.asList("Immobilien", "Hausgeld", "Hauskosten", "Nebenkosten", "Nebenkostenabrechnung", "Immobilienkonzern", "Wohnungen", "Vermietung", "Immobilienwirtschaft", "Wohnungsunternehmen", "Mietwohnungen"));
        newICExpense("Strom",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Strom", "Energieversorgung", "Energieversorgungskonzern", "Energieversorgern"));
        newICExpense("Heizung & Wasser",parentInvoiceCategory,Arrays.asList("Heizung", "Wasser"),  Arrays.asList("Fernwärme", "Heizung", "Wärme", "Ferngas", "Heizungsanlage", "Wasser", "Wasserversorgung"));
        newICExpense("Kommunikation & Internet",parentInvoiceCategory, Arrays.asList("Kommunikation"),  Arrays.asList("Kommunikation", "Mobilfunkkunden", "Internet", "Mobilfunk", "Handy", "Mobil", "Smartphone", "Mobilfunkverträge", "Festnetzanschlüsse", "Festnetz", "Mobilfunkanschlüsse ","Kabelnetzbetreiber", "Kabelfernsehen", "Telekommunikation", "Medien", "Telekommunikationsunternehmen"));
        newICExpense("Rundfunk & Streaming",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Rundfunk", "Streaming", "Filmwirtschaft", "Rundfunkanstalten", "Rundfunkbeitrag", "Rundfunkgebühr", "Fernsehempfang", "Fernsehen", "Serien", "Serienfolgen", "Filmen", "Video-on-Demand", "Online-Videothek"));
        newICExpense("Möbel & Einrichtung",parentInvoiceCategory, Arrays.asList("Möbel"), Arrays.asList("Einrichtung", "Einrichtungsgegenstände", "Möbelhäuser", "Baumarktkette", "Küche", "Badezimmer", "Wohnzimmer", "Kinderzimmer", "Arbeitszimmer", "Gäste-WC", "Baumarkt-Handelskette", "Bauhaus", "Baustoffhandel", "Baumarkt", "Baumärkte", "Raumausstattung", "Einrichtungshäuser", "Einrichtungsunternehmen", "Möbelhandel", "Möbel", "Einrichtung", "Wohneinrichtungs-Unternehmen"));
        newICExpense("Reparaturen",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Reparaturen", "Instandsetzung", "Wiederherstellung"));

        parentInvoiceCategory = newICExpenseParentCategory("Finanzen", "Geldanlagen");
        newICExpense("Bausparen & Hausbau",parentInvoiceCategory, Arrays.asList("Bausparen", "Darlehens-Tilgung", "Hypotheken-Zinsen"), Arrays.asList("Bausparen", "Bausparkasse", "Landesbausparkassen", "Bausparverträge", "Wohnungsbaufinanzierung"));
        newICExpense("Geldanlage",parentInvoiceCategory, Arrays.asList("Kontogebühren"), Arrays.asList("Kreditinsitut", "Bank", "Festgeld", "Anlagenberatung", "Girokonten", "Sparen", "Geldanlage","Kredit", "Kreditkarte", "Rahmenkredit", "Ratenkredit", "Sparbrief", "VL-Sparplan", "Tagesgeld", "Geldanlage", "Finanzdienstleistungen"));
        newICExpense("Börse",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("ETF", "Wertpapiere", "Fonds",  "Börse", "Aktienmärkte", "Aktiengesellschaften", "Wertpapierbörse", "Aktienmarkt", "Aktienindex", "Aktienindizes", "Dividenden", "Kursindex"));
        newICExpense("Versicherungen",parentInvoiceCategory, Arrays.asList("Versicherung"), Arrays.asList("Versicherungen", "Versicherung", "Versicherungsverein", "Versicherungsgesellschaft", "Versicherungswesen", "Versicherungskonzern", "Versicherungsunternehmen", "Versicherer", "Versicherung", "Krankenversicherung", "Haftpflichtversicherung", "Pflichtversicherungen", "Privat-Haftpflicht", "Kaskoversicherung", "Kfz-Haftpflichtversicherung"));

        parentInvoiceCategory = newICExpenseParentCategory("Bildung", "Ausbildung");
        newICExpense("Studium, Ausbildung & Weiterbildung",parentInvoiceCategory, Arrays.asList("Fahrtkosten","Fortbildungskosten"), Arrays.asList("Fortbildung", "Kindergarten", "Weiterbildung", "Schulung", "Seminar", "Fachhochschule", "Online-Seminare", "Unterricht", "Lernen", "Learning", "E-Learning", "Studium", "Hochschule", "Studiengänge", "Universität", "Ausbildung"));
        newICExpense("Lehrmittel",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Sachbücher", "Lehrmaterial", "Lehrmittel", "Tutorials", "Video-Tutorials", "Lernvideos", "Lehrbücher", "Fachliteratur"));


        List<String> getraenkeList = Arrays.asList("Getränke", "Trinkwasser", "Tafelwasser",  "Schorle", "Pils","Weizen","Ice Tea", "Kaffee", "Blatt-Tees", "Wasser", "Softgetränke", "Eistee", "Tee", "Wein", "Bier", "Kneipe", "Shots", "Cocktail", "Spirituosen", "Drink", "Alkohol");
        List<String> mahlzeitenList = Arrays.asList("Restaurants", "Catering", "Kaffeebohnen","Selbstbedienungs-Cafés","Kaffeeprodukte", "Früchten", "Toppings","Shake", "Geschmack", "kulinarisch", "Rezepte","Fischlokal", "Fischrestaurant", "Trinkkultur", "Tageskarte", "Speisen", "Buffet", "Fisch", "Gastronomiekonzepte", "Lemonade", "Freizeitgastronomie ", "Hamburger", "Ernährung", "Brasserien", "Gerichte", "Menü", "Lounge", "Dinner", "Nahrung", "Bewirtung", "Pasta", "Pizza", "Kaffee", "Cappuccino", "Burger", "Pommes", "Salat", "Espresso", "Ice Tea", "Bewirtungsaufwand","Bewirtungsaufwand-Angaben", "Pils", "Schorle", "Weizen", "Küche", "Hauptgericht", "Sushi", "Gastronomie", "Gaststätte", "Gaststättengewerbe", "Restaurantkette", "Quickservice-Systemgastronomie", "Fastfoodkette", "Fastfood-Bereich", "Kost", "Schnellrestaurants", "Pommesbude", "Imbiss", "Mahlzeit", "Lieferdienst", "Mittagsmenü", "Mittagsangebot", "Mittagessen", "Essensbestellung", "Fullservice-Systemgastronomie", "Mittagskarte", "Bäckereikette", "Backwaren", "Bäckerei", "Bäcker", "Flagship-Restaurant", "Snacks", "Essen", "Dessert", "Fastfood", "Schnellrestaurants", "Systemgastronomie", "Gaststätte", "Cafes");

        List<String> mahlzeitenResultList = new ArrayList<>();
        mahlzeitenResultList.addAll(mahlzeitenList);
        mahlzeitenResultList.addAll(getraenkeList);

        List<String> getraenkekaufList =Arrays.asList("Getränkedienst", "Teeladen", "Tee-Facheinzelhändler", "Brauerei", "Kiosk", "Getränkemarke", "Getränkelieferung", "Getränkelieferdienst");

        List<String> getraenkekaufResultList = new ArrayList<>();
        mahlzeitenResultList.addAll(getraenkekaufList);
        mahlzeitenResultList.addAll(getraenkeList);


        parentInvoiceCategory = newICExpenseParentCategory("Haushalt", null);
        newICExpense("Haushaltsprodukte",parentInvoiceCategory, Arrays.asList("Sonstige Haushaltskosten"), Arrays.asList("Lebensmitteleinzelhandel", "Fleischerei", "Metzgerei", "Discount", "Kaffee", "SB-Warenhäuser", "Konsumgüter", "Handelskette", "Einzelhandelsketten", "Markenartikel", "Supermarkt", "Markt", "Discount-Einzelhandelsketten", "Lebensmittelhändler", "Einzelhandelsunternehmen", "Lebensmittel", "Lebensmitteleinzelhändler", "Einkaufsverband", "Handel", "Warenhäuser", "Warenhauskette", "Kaufhof"));
        newICExpense("Haushaltsgeräte",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Haushaltsgeräte", "Hausgeräte", "Großgeräte", "Haushaltshelfer", "Großelektro", "Gewerbegeräte", "Elektrogeräte", "Elektronikkonzern"));
        newICExpense("Elektronik & Computer",parentInvoiceCategory, Arrays.asList("Computer & Laptop"), Arrays.asList("Elektrogeräte", "Computer", "Notebook", "Laptop", "Einzelplatzrechner", "Elektronik", "Elektronikkonzern", "Elektronikeinzelhandel", "Elektronik-Versandhandel", "IT/Multimedia", "Multimedia", "Unterhaltungselektronik", "Elektronik-Fachmarktkette", "Elektrohandelsketten"));
        newICExpense("Geschenke",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Geschenkartikeln", "Geschenke", "Mitbringsel", "Geschenkpapier"));
        newICExpense("Kleidung & Accessoires",parentInvoiceCategory, Arrays.asList("Bekleidung"), Arrays.asList("Mode", "Modemarke", "Einkaufszentrum", "Einkaufscenter", "Modedesigner", "Modeschöpfer", "Lederwaren", "Brautmode", "Brautkleid", "Anzug", "Umstandsmode", "Umstandskleidung", "Umstandskleid", "Textilwaren", "Modeanbieter", "Modeunternehmen", "Modehaus", "Versandhandel", "Textilindustrie", "Funktionsbekleidung", "Unterwäsche", "Oberteile", "Hosen", "Hose", "Jeans", "T-Shirt", "Pullover", "Textil-Einzelhandels-Kette", "Textilwirtschaft", "Fashion", "Damenmode", "Herrenmode", "Modeindustrie", "Outfit", "Style", "Kinderbekleidung", "Bekleidungsunternehmen", "Bekleidungshersteller ", "Bekleidungsindustrie", "Textil", "Textilien", "Bekleidung", "Kleidung", "Textilhandelsunternehmen", "Textilhandel", "Kleid"));
        newICExpense("Schuhe",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Schuhhandelsunternehmen", "Schuhhandel","Schuheinzelhandel", "Schuhproduktion", "Schuhcreme", "Schuhvertriebsunternehme", "Sportschuhe","Schuhe", "Schuhkauf", "Schuhen", "Schuhhändler", "Schuhmarken", "Markenschuhe", "Damenschuhe"));
        newICExpense("Pflege & Hygiene",parentInvoiceCategory, Arrays.asList("Körperpflege"), Arrays.asList("Drogerie", "Drogeriekonzern", "Nagelstudio", "Make-Up", "Kosmetik", "Kosmetikstudio", "Drogerieunternehmen", "Drogeriefachgeschäft", "Drogeriehandel", "Drogerieartikel", "Parfümerie", "Parfum", "Körperflege", "Hygiene", "Hygieneartikel", "Pflegeprodukte", "Pflege", "Drogeriemarktkette", "Drogeriemärkte", "Oberflächenreinigung", "Reinigungsmittel", "Reinigung"));
        newICExpense("Gesundheit", parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Gesundheit", "Brillen", "fachärztlich", "Hausarztpraxis", "Gesundheitsleistungen", "Hausarzt", "Facharzt", "Patienten", "Erkrankungen", "Erkrankung", "Patient", "Gemeinschaftspraxis", "Krankheit", "Krank", "Arztpraxis", "Allgemeinmedizin", "Apotheken", "Apotheke", "Gesundheitsartikel", "Versandapotheke", "Medikamente", "Medizin", "Arzneimittel", "Arzt", "Arznei", "Apotheker", "Pharmazie", "Pharmadienstleistungen", "Massage"));
        newICExpense("Mahlzeiten & Snacks",parentInvoiceCategory, Arrays.asList("Restaurantbesuche"), mahlzeitenResultList);
        newICExpense("Genussmittel",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Genussmittel", "Confiserie", "Schoko", "Kuchen", "Kekse", "Gebäck", "Trüffel", "Konditoreien", "Konditorei","Pralinés", "Feinbäcker", "Konditoreiwaren", "Pâtisserie", "Backstube",  "Torte", "Süßgebäck", "Schokolade", "Süßes", "Schokoladenhersteller", "Süßigkeiten", "Leckereien", "Bonbons", "Gummibärchen", "Süßwaren", "Pralinen", "Süßwarenindustrie"));
        newICExpense("Getränke",parentInvoiceCategory, new ArrayList<>(), getraenkekaufResultList);
        newICExpense("Garten & Pflanzen",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Garten", "Gartencenter-Kette", "Pflanzen", "Blumen", "Gartencenter", "Gartenzubehör", "Gartenwerkzeuge", "Blumenladen"));
        newICExpense("Haustiere",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Haustiere", "Heimtierprodukte", "Futter", "Tiere", "Hunde", "Katzen", "Kleintiere", "Tiernahrung", "Futtermittel", "Heimtierfutter", "Heimtiernahrung", " Tierbedarfhandel"));
        newICExpense("Schreibwaren",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Schreibwaren", "Schreibmaterial", "Schreibwarenladen", "Papier", "Stift", "PBS", "Papierwarenläden", "Bürobedarf", "Schreibzeug", "Büroartikel", "Radiergummi"));
        newICExpense("Uhren & Schmuck",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Schmuck", "Uhren", "Luxusgut", "Juwelen", "Juwelierunternehmen", "Juwelier", "Edelsteine", "Accessoires", "Ringe", "Armbanduhren", "Uhrmacherkette", "Ketten", "Ohrstecker", "Piercing", "Tattoos", "Luxusschmuck"));
        newICExpense("Dienstleistungen",parentInvoiceCategory, Arrays.asList("Nicht definiert"), Arrays.asList("Handwerk", "Umzugsunternehmen", "Umzüge", "Umzug", "Dienstleistungen", "Sanitär", "Dachdecker", "Installateur", "Tapezierer", "Hausmeister", "Handwerker"));
        newICExpense("Glücksspiele",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Lotto", "Totto", "Gewinnspiel", "Glücksspiel"));
        newICExpense("Software & Cloud-Dienste",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Software", "Web-Angebot", "Softwareentwicklung", "Anwendungen", "Anwendung", "Streaming Media", "Onlinedienst", "Projektmanagementsoftware", "Programme", "web-basiert", "Cloud", "Webanwendungen", "SaaS", "Software as a service", "Online-Anwendung"));
        newICExpense("Handel & Versand",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Onlineversandhändler", "Marketplace", "flohmarktähnliches", "Flohmarkt", "Consumer-to-Consumer-Marktplatz", "Gebrauchtwaren", "Rückgaberecht","Mindestbestellwert","Einkaufswagen", "Einkauf", "Versandhandel", "Versandhaus", "Onlinehandel", "Verkaufsplattform", "Onlinemarkt", "Online-Marktplätze", "Onlineshop", "Versandhandelsunternehmen", "Universalversender", "Tauschbörse", "Briefmarken", "Sendungen", "Paket", "Porto", "Paketdienst", "Päckchen", "Post", "Versand"));
        newICExpense("Friseur & Styling",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Friseur", "Haarverlängerungen", " Salon ", "Hair-Stylist", "Make-Up-Artist", "Make-Up","Coiffeur", "Haarschnitt", "Haarverlängerung", "Friseurhandwerk", "Haar", "Traumfrisur", "Frisur", "Frisuren", "Bartpflege"));
        newICExpense("Spenden",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Spenden", "Hilfsorganisation", "Spendengelder", "Hilfsorganisationen", "Förderer", "Nothilfe", "Katastrophenhilfe"));

        parentInvoiceCategory = newICExpenseParentCategory("Freizeit", null);
        newICExpense("Bücher & Zeitschriften",parentInvoiceCategory, Arrays.asList("Zeitschriften"), Arrays.asList("Buch", "Bücher", "Buchhändler", "Zeitschrift", "Nachrichtenmagazins", "Nachrichtenverlag", "Buchhandelsunternehmen", "Buchhandel", "Zeitschriften", "Chefredakteur", "Nachrichtenportale", "Magazine", "Verlagsgesellschaft", "Zeitungen", "Literatur", "Bestseller", "Tageszeitung", "Roman", "Belletristik", "Sachbücher", "Fachzeitschrift", "Zeitungsartikel", "Kochbücher", "Reiseführer", "Hörbücher", "Krimis"));
        newICExpense("Sport",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Sport", "Gymnastik", "sportlich", "Laufen", "Fitnessstudios", "Sportverein", "Sportarten", "Sportart", "Fitness", "Fitnessunternehmen", "Turnverein", "Fitnessverein", "Sportkurse", "Sportartikelhersteller", "Sportartikel", "Fitnesskurse", "Joggen", "Krafttraining", "Muskeltraining"));
        newICExpense("Kino & Home-Cinema",parentInvoiceCategory, Arrays.asList("Filmdatenträger", "Kino"), Arrays.asList("Kinounternehmen", "Kinobetreiber", "Kinosaal", "Lichtspielhäuser", "Filmverleih", "Kinokette", "Kino", "Filmspielhäuser", "Lichtspiele", "Filme", "Film", "Blockbuster", "Video", "Bluray", "Blu-ray"));
        newICExpense("Schwimmbäder",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Schwimmbäder", "Schwimmbad", "Whirlpool", "Stromschnelle", "Wasserrutsche", "Entspannung", "Wellness", "Wellnessoase", "Schwimmbecken", "schwimmen", "Badeanstalten", "Freibad", "Freibäder", "Hallenbäder", "Hallenbad", "Thermalquellen", "Strandbad", "Strandbäder", "Therme", "Naturbäder", "Allwetterbäder", "Sportbäder", "Stadtbäder", "Solebäder", "Solebad", "Erlebnisbad", "Erlebnisbäder", "Wasserpark", "Freizeitbad", "Freizeitbäder"));
        newICExpense("Spiele",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Spiele", "PC-Spiele", "Spielkonsole","Videospiel-Marke", "Videospiele","Videospielkonsolen", "Videospielmarke", "Games", "E-Game", "Konsolenspiel", "Gesellschaftsspiele", "Computerspiele", "Online-Spiele", "Multiplayer", "Gemeinschaftsspiele", "Brettspiele", "Kartenspiele", "Spielwarenhersteller", "Spielwarenunternehmen", "Spielwaren", "Spieleverlag", "Spielzeug"));
        newICExpense("Musik & Konzerte",parentInvoiceCategory, Arrays.asList("Musik"), Arrays.asList("Musikstreaming", "Musikplattform", "CDs", "Schallplatten", "Musikinstrumente", "Audioplayer-Software", "Portable Media Player", "Instrument", "Kassetten", "Mp3-Player", "Musikabspielgerät", "Musikstreamingdienst", "Musikstreaming-Dienste", "Hörbücher", "Musikkatalog", "Wiedergabeliste"));
        newICExpense("Erlebnisse & Parks",parentInvoiceCategory, Arrays.asList("Veranstaltungen"), Arrays.asList("Veranstaltung", "Veranstaltungen", "Schifffahrt", "Krimi-Dinner", "Krimi-Party", "Krimispiel", "Zoo", "Tierpark", "Streichelzoo", "Tiergarten", "Festen", "Kirmes", "Vergnügungspark", "Comedy", "Theater", "Musicals", "Shows", "Unterhaltungsbranche", "Entertainment", "Kabarett", "Kabarett-Ensemble", "Vorführung", "Aufführung", "Schauspiel", "Fahrgeschäfte", "Freizeitpark", "Jahrmarkt", "Comedy", "Theater", "Musicals", "Shows", "Unterhaltungsbranche", "Entertainment", "Künstler", "Kunst", "Kabarett", "Kabarett-Ensemble", "Vorführung", "Aufführung", "Schauspiel", "Schauspielhaus"));
        newICExpense("Sehenswürdigkeiten & Museen",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Museumsshop", "Sehenswürdigkeit", "Galerie", "Kunsthandwerk", "Museum", "Künstler", "Kunst", "Ausstellung", "Sonderausstellung", "Kulturgüter", "Museen", "Aussichtsturm", "Wahrzeichen", "Kultur", "Besuch", "Denkmal", "Denkmalschutz"));
        newICExpense("Vereine",parentInvoiceCategory, Arrays.asList("Vereinsbeiträge"), Arrays.asList("Vereine", "Verein", "Vereinsbedarf", "Vereins-Fachgeschäft"));
        newICExpense("Hobby & Leidenschaft",parentInvoiceCategory, Arrays.asList("Hobbies"), Arrays.asList("Hobby", "Bastelgeschäft", "Bastelladen", "Zeichnen", "Basteln","Bastelmaterial", "Bastelshop", "Bastler", "Bastelshop","Kreativ", "Kreativhaus", "Malen", "Bastelbedarf", "Bastelidee", "Hobbybedarf", "Modellbahn", "Miniatur", "Stricken", "Nähen", "Nähbedarf", "Künstlerbedarf", "Malbedarf"));

        parentInvoiceCategory = newICExpenseParentCategory("Verkehr & Reisen", "Verkehrsmittel");
        newICExpense("Kraftstoff",parentInvoiceCategory, Arrays.asList("Benzin"), Arrays.asList("Kraftstoffe", "Ottokraftstoff", "Motorenbenzin", "Petrochemiekonzern", "Kraftstoffmarkt", "Super", "Diesel-Kraftstoff", "Benzin", "Tankstellen", "Tankstelle", "Mineralöl", "Mineralölkonzern", "Mineralölunternehmen"));
        newICExpense("ÖPNV & Bahntickets",parentInvoiceCategory, Arrays.asList("Fahrkarten"), Arrays.asList("Deutsche Bahn", "Bahn", "SPNV", "Züge","Zug","Busse","Bus", "Train", "Eisenbahnverkehr", "Fernverkehr", "Nahverkehrsballungsraum", "Nahverkehr", "Verkehrsverbund", "Personennahverkehr"));
        newICExpense("Flugtickets",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Fluggesellschaft", "Flüge", "Flug", "Airline", "Flughafen", "Luftverkehrskonzern"));
        newICExpense("KFZ-Steuer",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Kraftfahrzeugsteuer", "KFZ-Steuer", "Kraftfahrtbundesamt", "KraftSt", "Zollverwaltung"));
        newICExpense("Miete & Leasing",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Mietwagenunternehmen", "Fahrradverleih", "Car rental", "Bikesharing", "Fahrradvermietungen", "Fahrradvermietung", "Fahrradverleihsystem", "Mietwagen", "Leasing", "Fahrdienstvermittlung", "Carsharing", "Autovermietung"));
        newICExpense("Inspektionen & Verbrauchsmaterial",parentInvoiceCategory, Arrays.asList("Reifen"), Arrays.asList("Ölwechsel", "Bremsflüssigkeit", "Kraftfahrzeugzubehör", "Inspektion", "Inspektionen", "Reifen", "Reifenwechsel", "Radwechsel"));
        newICExpense("Reparaturen",parentInvoiceCategory, Arrays.asList("KFZ-Reparaturen"), Arrays.asList("Werkstatt", "Reparatur",  "Fahrradreparatur", "Reparatur-Fachgeschäft", "Autoersatzteile", "Kraftfahrzeug-Werkstätten", "KFZ-Werkstätten", "KFZ-Werkstatt"));
        newICExpense("Parken",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Parkhaus", "Parken", "Parkplatz", "Parkautomat", "Parkticket", "Parkgebühr", "Abstellplatz", "Parkzeit", "Parkgaragengesellschaft", "Parkraumbewirtschaftung", "Parkraum", "Parking", "Parkraumbewirtschafter", "Stellplätze", "Garagen"));
        newICExpense("Reinigung & Pflege",parentInvoiceCategory, Arrays.asList("Autoreinigung"), Arrays.asList("Autoreinigung", "Autopflege", "Waschanlage", "Waschstraße", "Autowaschstraßen", "Auto-Wasch-Geräte"));
        newICExpense("Anschaffung",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Autokauf", "Fahrradkauf", "Nutzfahrzeuge", "Autohersteller", "Automobilindustrie", "Automobile", "Automobilhersteller", "Autohaus"));
        newICExpense("Übernachtungen & Pauschalreisen",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Hotels", "Hotel", "Reisebranche", "Übernachtung", "Fremdenverkehr", "Reisebüros", "Online-Reisebüro", "Übernachtungen", "Reiseveranstalter", "Kreuzfahrt", "Freizeitindustrie", "Touristik", "Tourismus","Tourismusbranche", "Touristikkonzern", "Pauschalreise", "flugreisen", "Ferienwohnungen", "Ferienwohnung", "Luxushotel", "Pension", "Jugendherberge", "Herberge", "Unterkunft", "Unterkünfte", "Hotelmarke", "Hotellerie", "Hotelunternehmen", "Hotelgesellschaft", "Beherbergung", "Design-Hotelgruppe", "Hotelgruppe", "Pensionen", "Motel", "Motels", "Bett", "Betten"));
        newICExpense("Zubehör",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Autoteile", "Autozubehör"));

        parentInvoiceCategory = newICIncomeParentCategory("Arbeit",null);
        newICIncome("Gehalt",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Gehalt", "Gehaltsnachweis", "Einkommen", "Gehaltsübersicht", "Lohn", "Lohnzettel"));
        newICIncome("Gehalt-Nebenjob",parentInvoiceCategory, new ArrayList<>(), new ArrayList<>());
        newICIncome("Weihnachtsgeld",parentInvoiceCategory, new ArrayList<>(), new ArrayList<>());
        newICIncome("Urlaubsgeld",parentInvoiceCategory, new ArrayList<>(), new ArrayList<>());
        newICIncome("Bonus",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Gewinnausschüttung"));

        parentInvoiceCategory = newICIncomeParentCategory("Sonstiges", "Sonstiges Ausgabe");
        newICIncome("Geldgeschenke",parentInvoiceCategory, new ArrayList<>(), new ArrayList<>());
        newICIncome("Wertgutscheine",parentInvoiceCategory, Arrays.asList("Nicht definiert"), new ArrayList<>());
        newICIncome("Zinsen",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Immobilienzinsen"));
        newICIncome("Immobilien",parentInvoiceCategory, new ArrayList<>(), new ArrayList<>());
        newICIncome("Aktien & Dividende",parentInvoiceCategory, new ArrayList<>(), Arrays.asList("Dividende"));
    }

    private InvoiceCategory newICExpenseParentCategory(String name, String oldName){

        InvoiceCategory foundInvoiceCategory = invoiceCategoryRepository.findOneByInvoiceCategoryNameAndInvoiceCategoryTypeAndParentInvoiceCategoryIsNullAndAppUserIsNull(name, InvoiceCategoryType.EXPENSE);
        if (foundInvoiceCategory == null && oldName != null){
            foundInvoiceCategory = invoiceCategoryRepository.findOneByInvoiceCategoryNameAndInvoiceCategoryTypeAndParentInvoiceCategoryIsNullAndAppUserIsNull(oldName, InvoiceCategoryType.EXPENSE);
        }

        InvoiceCategory newInvoiceCategory = newICExpenseBasis(foundInvoiceCategory, name, null, new ArrayList<>());

        if (oldName != null){
            reorganiseOldInvoiceCategories(Arrays.asList(oldName), newInvoiceCategory, InvoiceCategoryType.EXPENSE);
        }

        return newInvoiceCategory;
    }

    private InvoiceCategory newICExpense(String name, InvoiceCategory parentInvoiceCategory, List<String> oldNames, List<String> keywords){

        InvoiceCategory foundInvoiceCategory = invoiceCategoryRepository.findAllByInvoiceCategoryNameAndInvoiceCategoryTypeAndAppUserIsNull(name, InvoiceCategoryType.EXPENSE).stream().findAny().orElse(null);
        if (foundInvoiceCategory == null && oldNames != null && !oldNames.isEmpty()){
            foundInvoiceCategory = invoiceCategoryRepository.findAllByInvoiceCategoryNameInAndInvoiceCategoryTypeAndParentInvoiceCategoryIsNullAndAppUserIsNull(oldNames, InvoiceCategoryType.EXPENSE).stream().findAny().orElse(null);
        }

        InvoiceCategory newInvoiceCategory = newICExpenseBasis(foundInvoiceCategory, name, parentInvoiceCategory, keywords);

        reorganiseOldInvoiceCategories(oldNames, newInvoiceCategory, InvoiceCategoryType.EXPENSE);

        return newInvoiceCategory;
    }

    private InvoiceCategory newICExpenseBasis(InvoiceCategory newInvoiceCategory, String name, InvoiceCategory parentInvoiceCategory, List<String> keywords){

        if(newInvoiceCategory == null){
            newInvoiceCategory = new InvoiceCategory();
            newInvoiceCategory.setInvoiceCategoryId(UUID.randomUUID());
        }
        newInvoiceCategory.setAppUser(null);
        newInvoiceCategory.setParentInvoiceCategory(parentInvoiceCategory);
        newInvoiceCategory.setInvoiceCategoryName(name);
        newInvoiceCategory.setBasicStatusEnum(BasicStatusEnum.OK);
        newInvoiceCategory.setInvoiceCategoryType(InvoiceCategoryType.EXPENSE);
        newInvoiceCategory = invoiceCategoryRepository.save(newInvoiceCategory);

        for (String keyword : keywords) {
            newKeyword(newInvoiceCategory, keyword);
        }

        return newInvoiceCategory;
    }

    private InvoiceCategory newICIncomeParentCategory(String name, String oldName){

        InvoiceCategory foundInvoiceCategory = invoiceCategoryRepository.findOneByInvoiceCategoryNameAndInvoiceCategoryTypeAndParentInvoiceCategoryIsNullAndAppUserIsNull(name, InvoiceCategoryType.INCOME);
        if (foundInvoiceCategory == null && oldName != null){
            foundInvoiceCategory = invoiceCategoryRepository.findOneByInvoiceCategoryNameAndInvoiceCategoryTypeAndParentInvoiceCategoryIsNullAndAppUserIsNull(oldName, InvoiceCategoryType.INCOME);
        }

        InvoiceCategory newInvoiceCategory = newICIncomeBasis(foundInvoiceCategory, name, null, new ArrayList<>());

        if (oldName != null){
            reorganiseOldInvoiceCategories(Arrays.asList(oldName), newInvoiceCategory, InvoiceCategoryType.INCOME);
        }

        return newInvoiceCategory;
    }

    private InvoiceCategory newICIncome(String name, InvoiceCategory parentInvoiceCategory, List<String> oldNames, List<String> keywords){

        InvoiceCategory foundInvoiceCategory = invoiceCategoryRepository.findAllByInvoiceCategoryNameAndInvoiceCategoryTypeAndAppUserIsNull(name, InvoiceCategoryType.INCOME).stream().findAny().orElse(null);
        if (foundInvoiceCategory == null && oldNames != null && !oldNames.isEmpty()){
            foundInvoiceCategory = invoiceCategoryRepository.findAllByInvoiceCategoryNameInAndInvoiceCategoryTypeAndParentInvoiceCategoryIsNullAndAppUserIsNull(oldNames, InvoiceCategoryType.INCOME).stream().findAny().orElse(null);
        }

        InvoiceCategory newInvoiceCategory = newICIncomeBasis(foundInvoiceCategory, name, parentInvoiceCategory, keywords);

        reorganiseOldInvoiceCategories(oldNames, newInvoiceCategory, InvoiceCategoryType.INCOME);

        return newInvoiceCategory;
    }

    private InvoiceCategory newICIncomeBasis(InvoiceCategory newInvoiceCategory, String name, InvoiceCategory parentInvoiceCategory, List<String> keywords){

        if(newInvoiceCategory == null){
            newInvoiceCategory = new InvoiceCategory();
            newInvoiceCategory.setInvoiceCategoryId(UUID.randomUUID());
        }
        newInvoiceCategory.setAppUser(null);
        newInvoiceCategory.setParentInvoiceCategory(parentInvoiceCategory);
        newInvoiceCategory.setInvoiceCategoryName(name);
        newInvoiceCategory.setBasicStatusEnum(BasicStatusEnum.OK);
        newInvoiceCategory.setInvoiceCategoryType(InvoiceCategoryType.INCOME);
        newInvoiceCategory = invoiceCategoryRepository.save(newInvoiceCategory);

        for (String keyword : keywords) {
            newKeyword(newInvoiceCategory, keyword);
        }

        return newInvoiceCategory;
    }

    private InvoiceCategoryKeyword newKeyword(InvoiceCategory invoiceCategory, String keyword){

        InvoiceCategoryKeyword invoiceCategoryKeyword = invoiceCategoryKeywordRepository.findOneByInvoiceCategoryAndKeyword(invoiceCategory, keyword);
        if(invoiceCategoryKeyword == null){
            invoiceCategoryKeyword = new InvoiceCategoryKeyword();
            invoiceCategoryKeyword.setInvoiceCategoryKeywordId(UUID.randomUUID());
        }
        invoiceCategoryKeyword.setInvoiceCategory(invoiceCategory);
        invoiceCategoryKeyword.setKeyword(keyword);
        return invoiceCategoryKeywordRepository.save(invoiceCategoryKeyword);
    }

    private void reorganiseOldInvoiceCategories(List<String> categoryNames, InvoiceCategory invoiceCategoryToSet, InvoiceCategoryType invoiceCategoryType){

        for (String categoryName : categoryNames) {
            List<InvoiceCategory> invoiceCategories = invoiceCategoryRepository.findAllByInvoiceCategoryNameAndInvoiceCategoryTypeAndAppUserIsNull(categoryName, invoiceCategoryType);
            for (InvoiceCategory invoiceCategory : invoiceCategories) {
                List<Invoice> invoices = invoiceRepository.findByInvoiceCategory(invoiceCategory);

                for (Invoice invoice : invoices) {
                    invoice.setInvoiceCategory(invoiceCategoryToSet);
                    invoiceRepository.save(invoice);
                }

                invoiceCategoryRepository.delete(invoiceCategory);
            }

        }
    }
}
