package org.hsw.application;

import org.hsw.contract.model.Topic;
import org.hsw.domain.question.*;

import java.util.*;

public class QuestionLoader {
    private static int DEFAULT_POINTS = 10;
    private static Map<Range, Double> DEFAULT_RANGES = loadRanges();
    private static final List<Question> questions = loadQuestions();
    private static final Map<Topic, List<Question>> questionsByTopic = indexQuestions();

    static List<Question> loadQuestions() {
        final List<Question> questions = List.of(
                new NumericQuestion("In welchem Jahr begann der Erste Weltkrieg?", DEFAULT_POINTS, Double.valueOf("1914"), 0, Topic.HISTORY),
                new NumericQuestion("In welchem Jahr entdeckte Christoph Kolumbus Amerika?", DEFAULT_POINTS, 1492.0, 0, Topic.HISTORY),
                new SingleChoiceQuestion("Wann fiel die Berliner Mauer?", DEFAULT_POINTS, List.of(
                        new Option('A', "09.November 1990"), new Option('B', "19.November 1989"),
                        new Option('C', "09.November 1989"), new Option('D', "19. November 1990")), new Option('C', "09.November 1989"), Topic.HISTORY),
                new SingleChoiceQuestion("Welche antike Stadt wurde im Jahr 79 n. Chr. beim Ausbruch des Vesuvs verschüttet?", DEFAULT_POINTS, List.of(
                        new Option('A', "Pompeji"), new Option('B', "Alexandria"),
                        new Option('C', "Karthago"), new Option('D', "Konstantinopel")), new Option('A', "Pompeji"), Topic.HISTORY),
                new NumericQuestion("In Welchem Jahr sank die Titanic", DEFAULT_POINTS, Double.valueOf("1912"), 0, Topic.HISTORY),
                new SingleChoiceQuestion("Welche antike Hochkultur errichtete Machu Picchu?", DEFAULT_POINTS, List.of(
                        new Option('A', "Azteken"), new Option('B', "Inka"),
                        new Option('C', "Maya"), new Option('D', "Olmeken")), new Option('B', "Inka"), Topic.HISTORY),
                new GuessingQuestion("wie viele verschiedene Reichskanzler gab es in der Weimarer Republik", DEFAULT_POINTS, 12.0, DEFAULT_RANGES, 0, Topic.HISTORY),
                new TrueOrFalseQuestion("Das Römische Reich wurde bereits vor der Geburt Christi in ein Ost- und Westreich geteilt", DEFAULT_POINTS, Option.getOptionsForTrueOrFalse().get(1), Topic.HISTORY),
                new SingleChoiceQuestion("Welches Säugetier legt Eier und besitz ein Schnabel?", DEFAULT_POINTS, List.of(
                        new Option('A', "Ameisenigel"), new Option('B', "Schnabeltier"),
                        new Option('C', "Kurzschnabeligel"), new Option('D', "Schuppentier")), new Option('B', "Schnabeltier"), Topic.ANIMALS),
                new SingleChoiceQuestion("Welches Tier besitzt drei Herzen und blaues Blut?", DEFAULT_POINTS, List.of(
                        new Option('A', "Nautilus"), new Option('B', "Viperfisch"),
                        new Option('C', "Stechrochen"), new Option('D', "Oktopus")), new Option('D', "Oktopus"), Topic.ANIMALS),
                new SingleChoiceQuestion("Welche Tierart gilt als das schnellste Tier der Welt, wenn man die Geschwindigkeit im Sturzflug betrachtet?", DEFAULT_POINTS, List.of(
                        new Option('A', "Steinadler"), new Option('B', "Bierschnegel"),
                        new Option('C', "Wanderfalke"), new Option('D', "Turmfalke")), new Option('C', "Wanderfalke"), Topic.ANIMALS),
                new SingleChoiceQuestion("Welche ist größte heute lebende Katzenart?", DEFAULT_POINTS, List.of(
                        new Option('A', "Sumatra Tiger"), new Option('B', "Sibirischer Tiger"),
                        new Option('C', "Bengalischer Tiger"), new Option('D', "Süd Chinesischer Tiger")), new Option('B', "Sibirischer Tiger"), Topic.ANIMALS),
                new SingleChoiceQuestion("Welcher Vogel besitzt die größte Flügelspannweite unter den heute lebenden Vogelarten?", DEFAULT_POINTS, List.of(
                        new Option('A', "Wanderalbatros"), new Option('B', "Afrikanischer Strauß"),
                        new Option('C', "Riesentrappe"), new Option('D', "Seeadler")), new Option('A', "Wanderalbatros"), Topic.ANIMALS),
                new SingleChoiceQuestion("Welches Tier besitzt sogenannte Nesselzellen, die zum Beutefang und zur Verteidigung eingesetzt werden?", DEFAULT_POINTS, List.of(
                        new Option('A', "Kegelschnecken"), new Option('B', "Salpen"),
                        new Option('C', "Kugelfisch"), new Option('D', "Quallen")), new Option('D', "Quallen"), Topic.ANIMALS),
                new TrueOrFalseQuestion("Haie gehören zu den Knochenfischen.", DEFAULT_POINTS, Option.getOptionsForTrueOrFalse().get(1), Topic.ANIMALS),
                new TrueOrFalseQuestion("Ein erwachsener Mensch besitzt normalerweise mehr Zähne als ein ausgewachsener Hund.", DEFAULT_POINTS, Option.getOptionsForTrueOrFalse().get(1), Topic.ANIMALS),
                new NumericQuestion("Welche Dezimalzahl ergibt sich aus 2^8", DEFAULT_POINTS, Double.valueOf("256"), 0, Topic.MATHEMATICS),
                new NumericQuestion("Wie viele Grad hat die Winkelsumme eines Dreiecks?", DEFAULT_POINTS, Double.valueOf("180"), 0, Topic.MATHEMATICS),
                new NumericQuestion("Löse die Gleichung nach x: 3x + 7 = 25", DEFAULT_POINTS, Double.valueOf("6"), 0, Topic.MATHEMATICS),
                new NumericQuestion("Ein Produkt kostet nach einer Preiserhöhung von 20 % nun 144 €. Wie hoch war der ursprüngliche Preis?", DEFAULT_POINTS, Double.valueOf("120"), 0, Topic.MATHEMATICS),
                new NumericQuestion("Ein Rechteck hat einen Umfang von 54 cm. Die Länge beträgt 17 cm. Wie breit ist es in cm?", DEFAULT_POINTS, Double.valueOf("10"), 0, Topic.MATHEMATICS),
                new NumericQuestion("Ein Kapital von 2.000 € wird ein Jahr lang mit 4 % verzinst. Wie hoch ist das Kapital danach?", DEFAULT_POINTS, Double.valueOf("2080"), 0, Topic.MATHEMATICS),
                new SingleChoiceQuestion("Welches ist das Flächengrößte Bundesland?", DEFAULT_POINTS, List.of(
                        new Option('A', "Niedersachsen"), new Option('B', "Brandenburg"),
                        new Option('C', "Nordrhein-Westfalen"), new Option('D', "Bayern")), new Option('D', "Bayern"), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Welcher ist der Größte Ozean?", DEFAULT_POINTS, List.of(
                        new Option('A', "Pazifischer Ozean"), new Option('B', "Atlantischer Ozean"),
                        new Option('C', "Indischer Ozean")), new Option('A', "Pazifischer Ozean"), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Welcher Fluss fließt durch Wien, Bratislava und Budapest?", DEFAULT_POINTS, List.of(
                        new Option('A', "Rhein"), new Option('B', "Donau"),
                        new Option('C', "Dnepr"), new Option('D', "Isar")), new Option('B', "Donau"), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Welches ist das flächenmäßig größte Binnenland?", DEFAULT_POINTS, List.of(
                        new Option('A', "Mongolei"), new Option('B', "Mali"),
                        new Option('C', "Demokratische Republik Kongo"), new Option('D', "Kasachstan")), new Option('D', "Kasachstan"), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Welcher See ist der tiefste See der Erde?", DEFAULT_POINTS, List.of(
                        new Option('A', "Kaspisches Meer"), new Option('B', "Lake Superior"),
                        new Option('C', "Lake Erie"), new Option('D', "Baikalsee")), new Option('D', "Baikalsee"), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Welcher Staat besitzt mit dem Wakhan-Korridor einen schmalen Landstreifen zwischen Tadschikistan und Pakistan?", DEFAULT_POINTS, List.of(
                        new Option('A', "Kirgisistan"), new Option('B', "Usbekistan"),
                        new Option('C', "Afghanistan "), new Option('D', "Turkmenistan")), new Option('C', "Afghanistan "), Topic.GEOGRAPHY),
                new NumericQuestion("Wie viele Länder grenzen an Deutschland?", DEFAULT_POINTS, Double.valueOf("9"), 0, Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Welche ist die Größte Wüste der Erde?", DEFAULT_POINTS, List.of(
                        new Option('A', "Sahara"), new Option('B', "Antarktis"),
                        new Option('C', "Gobi"), new Option('D', "Arktis")), new Option('B', "Antarktis"), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Wie heißt die Hauptstadt von Kanada?", DEFAULT_POINTS, List.of(
                        new Option('A', "Ottawa"), new Option('B', "Vancouver"),
                        new Option('C', "Toronto"), new Option('D', "Winnipeg")), new Option('A', "Ottawa"), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Wie heißt die Hauptstadt von Australien?", DEFAULT_POINTS, List.of(
                        new Option('A', "Canberra"), new Option('B', "Brisbane"),
                        new Option('C', "Sydney"), new Option('D', "Melbourne")), new Option('A', "Canberra"), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Wie heißt die Hauptstadt von Brasilien?", DEFAULT_POINTS, List.of(
                        new Option('A', "Rio de Janeiro"), new Option('B', "Natal"),
                        new Option('C', "São Paulo"), new Option('D', "Brasilia ")), new Option('D', "Brasilia "), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Wie heißt die Hauptstadt von Dänemark?", DEFAULT_POINTS, List.of(
                        new Option('A', "Odense"), new Option('B', "Esbjerg"),
                        new Option('C', "Kopenhagen "), new Option('D', "Aarhus")), new Option('C', "Kopenhagen "), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Wie heißt die Hauptstadt von den Vereinigten Arabischen Emirate?", DEFAULT_POINTS, List.of(
                        new Option('A', "al-Ain"), new Option('B', "Doha"),
                        new Option('C', "Dubai"), new Option('D', "Abu Dhabi")), new Option('D', "Abu Dhabi"), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Wie heißt die Hauptstadt von Burkina Faso?", DEFAULT_POINTS, List.of(
                        new Option('A', "Bobo-Dioulasso"), new Option('B', "Ouagadougou"),
                        new Option('C', "Accra"), new Option('D', "Lagos")), new Option('B', "Ouagadougou"), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Wie heißt die Hauptstadt von Mongolei?", DEFAULT_POINTS, List.of(
                        new Option('A', "Ulaanbaatar"), new Option('B', "Darchan"),
                        new Option('C', "Erdenet"), new Option('D', "Dschargaltchaan")), new Option('A', "Ulaanbaatar"), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Wie heißt die Hauptstadt von Ungarn?", DEFAULT_POINTS, List.of(
                        new Option('A', "Bratislava"), new Option('B', "Budapest"),
                        new Option('C', "Bukarest"), new Option('D', "Breslau")), new Option('B', "Budapest"), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Wie heißt die Hauptstadt von Argentinien?", DEFAULT_POINTS, List.of(
                        new Option('A', "Córdoba"), new Option('B', "Santa Cruz"),
                        new Option('C', "Buenos Aires "), new Option('D', "San Luis")), new Option('C', "Buenos Aires "), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Welche Städte sind die Hauptstädte von Südafrika?", DEFAULT_POINTS, List.of(
                        new Option('A', "Bloemfontein, Johannesburg, Durban"), new Option('B', "Kapstadt, Bloemfontein, Durban"),
                        new Option('C', "Pretoria, Kapstadt, Bloemfontein"), new Option('D', "Johannesburg, Kapstadt, Pretoria")), new Option('C', "Pretoria, Kapstadt, Bloemfontein"), Topic.GEOGRAPHY),
                new TrueOrFalseQuestion("Der Amazonas ist nach Wassermenge der größte Fluss der Erde.", DEFAULT_POINTS, Option.getOptionsForTrueOrFalse().get(0), Topic.GEOGRAPHY),
                new TrueOrFalseQuestion("Der Pazifische Ozean ist größer als alle Landflächen der Erde zusammen.", DEFAULT_POINTS, Option.getOptionsForTrueOrFalse().get(0), Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Welches Chemische Symbol hat Wasserstoff?", DEFAULT_POINTS, List.of(
                        new Option('A', "He"), new Option('B', "Hs"),
                        new Option('C', "H"), new Option('D', "W")), new Option('C', "H"), Topic.CHEMISTRY),
                new SingleChoiceQuestion("Welches Chemische Symbol hat Gold?", DEFAULT_POINTS, List.of(
                        new Option('A', "Ag"), new Option('B', "Au"),
                        new Option('C', "As"), new Option('D', "Ar")), new Option('B', "Au"), Topic.CHEMISTRY),
                new SingleChoiceQuestion("Woraus bestehen Diamanten?", DEFAULT_POINTS, List.of(
                        new Option('A', "Kristall"), new Option('B', "Silizium"),
                        new Option('C', "Kohlenstoff"), new Option('D', "Arsen")), new Option('C', "Kohlenstoff"), Topic.CHEMISTRY),
                new TrueOrFalseQuestion("Die Chemische Formel für Salzsäure ist HCl.", DEFAULT_POINTS, Option.getOptionsForTrueOrFalse().get(0), Topic.CHEMISTRY),
                new SingleChoiceQuestion("Wie viel °C hat der absolute Nullpunkt?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "215,37 °C"), new Option('B', "−273,15 °C"), new Option('C', "0 °C"), new Option('D', "153,42 °C")),
                        new Option('B', "−273,15 °C"),
                        Topic.PHYSICS),
                new SingleChoiceQuestion("Welches Gas macht den größten Anteil der Erdatmosphäre aus?", DEFAULT_POINTS, List.of(
                        new Option('A', "Stickstoff"), new Option('B', "Kohlenstoffdioxid"),
                        new Option('C', "Sauerstoff"), new Option('D', "Neon")), new Option('A', "Stickstoff"), Topic.CHEMISTRY),
                new SingleChoiceQuestion("Wofür steht VPN?", DEFAULT_POINTS, List.of(
                        new Option('A', "Virtual Passaged Network"), new Option('B', "Virtual Private Network"),
                        new Option('C', "Very Private Network")), new Option('B', "Virtual Private Network"), Topic.COMPUTER_SCIENCE),
                new SingleChoiceQuestion("Wofür steht die Abkürzung SQL?", DEFAULT_POINTS, List.of(
                        new Option('A', "Structured Query Language"), new Option('B', "Sequential Query Language"),
                        new Option('C', "Standard Query Language"), new Option('D', "System Query Logic")), new Option('A', "Structured Query Language"), Topic.COMPUTER_SCIENCE),
                new SingleChoiceQuestion("Wofür steht die Abkürzung „USB“?", DEFAULT_POINTS, List.of(
                        new Option('A', "Universal System Bus"), new Option('B', "Unified Serial Bus"),
                        new Option('C', "Universal Serial Bus"),
                        new Option('D', "Unified System Bus")), new Option('C', "Universal Serial Bus"), Topic.COMPUTER_SCIENCE),
                new NumericQuestion("Wie viele Planeten gehören zu unserem Sonnensystem?", DEFAULT_POINTS, Double.valueOf("8"), 0, Topic.ASTRONOMY),
                new SingleChoiceQuestion("Wie heißt die unserer Milchstraße nächstgelegene große Galaxie?", DEFAULT_POINTS, List.of(
                        new Option('A', "Virgo A"), new Option('B', "Andromedagalaxie "),
                        new Option('C', "Bodes Galaxie"), new Option('D', "Feuerrad-Galaxie")), new Option('B', "Andromedagalaxie "), Topic.ASTRONOMY),
                new SingleChoiceQuestion("Wie heißt die Grenze um ein Schwarzes Loch, jenseits derer selbst Licht nicht mehr entkommen kann?", DEFAULT_POINTS, List.of(
                        new Option('A', "Gravitationshorizont"), new Option('B', "Lichtgrenze"),
                        new Option('C', "Singularitätsgrenze"), new Option('D', "Ereignishorizont")), new Option('D', "Ereignishorizont"), Topic.ASTRONOMY),
                new SingleChoiceQuestion("Welcher Planet ist der Sonne am nächsten?", DEFAULT_POINTS, List.of(
                        new Option('A', "Merkur"), new Option('B', "Erde"),
                        new Option('C', "Venus"), new Option('D', "Jupiter")), new Option('A', "Merkur"), Topic.ASTRONOMY),
                new SingleChoiceQuestion("Welcher Planet im Sonnensystem hat die Höchste Oberflächentemperatur?", DEFAULT_POINTS, List.of(
                        new Option('A', "Merkur"), new Option('B', "Jupiter"),
                        new Option('C', "Venus"), new Option('D', "Neptun")), new Option('C', "Venus"), Topic.ASTRONOMY),
                new SingleChoiceQuestion("Welcher Planet hat die meisten Monde im Sonnensystem?", DEFAULT_POINTS, List.of(
                        new Option('A', "Neptun"), new Option('B', "Saturn"),
                        new Option('C', "Uranus"), new Option('D', "Jupiter")), new Option('B', "Saturn"), Topic.ASTRONOMY),
                new TrueOrFalseQuestion("Pluto ist größer als der Erdmond.", DEFAULT_POINTS, Option.getOptionsForTrueOrFalse().get(1), Topic.ASTRONOMY),
                new GuessingQuestion("Aus wie vielen Knochen besteht ein erwachsener Körper?", DEFAULT_POINTS, 206.0, DEFAULT_RANGES, 0, Topic.BIOLOGY),
                new SingleChoiceQuestion("Welches Protein bindet Sauerstoff in Roten Blutkörperchen?", DEFAULT_POINTS, List.of(
                        new Option('A', "Myoglobin"), new Option('B', "Hämoglobin"),
                        new Option('C', "Albumin"), new Option('D', "Fibrinogen")), new Option('B', "Hämoglobin"), Topic.BIOLOGY),
                new NumericQuestion("Wie viele Chromosomenpaare besitzt eine normale menschliche Körperzelle?", DEFAULT_POINTS, Double.valueOf("23"), 0, Topic.BIOLOGY),
                new SingleChoiceQuestion("Welches Molekül dient in Zellen hauptsächlich als kurzfristiger universeller Energieträger?", DEFAULT_POINTS, List.of(
                        new Option('A', "ADP – Adenosindiphosphat"), new Option('B', "GTP – Guanosintriphosphat"),
                        new Option('C', "ATP – Adenosintriphosphat"), new Option('D', "Glukose")), new Option('C', "ATP – Adenosintriphosphat"), Topic.BIOLOGY),
                new SingleChoiceQuestion("Welches Organ Produziert Insulin?", DEFAULT_POINTS, List.of(
                        new Option('A', "Pankreas "), new Option('B', "Schilddrüse"),
                        new Option('C', "Leber"), new Option('D', "Milz")), new Option('A', "Pankreas "), Topic.BIOLOGY),
                new NumericQuestion("Wie viele Kammern besitz das Menschliche Herz?", DEFAULT_POINTS, Double.valueOf("4"), 0, Topic.BIOLOGY),
                new TrueOrFalseQuestion("Die menschliche Leber kann einen erheblichen Teil ihres Gewebes regenerieren.", DEFAULT_POINTS, Option.getOptionsForTrueOrFalse().get(0), Topic.BIOLOGY),
                new SingleChoiceQuestion("Was ist eine Aktie?", DEFAULT_POINTS, List.of(
                        new Option('A', "Eine Unternehmensanleihe"), new Option('B', "Ein Unternehmensanteil"),
                        new Option('C', "ein staatliches Wertpapier"), new Option('D', "ein Bankkredit")), new Option('B', "Ein Unternehmensanteil"), Topic.ECONOMICS),
                new SingleChoiceQuestion("Wie lautet die wirtschaftswissenschaftliche Bezeichnung für den Gesamtwert aller innerhalb eines Landes in einem bestimmten Zeitraum produzierten Waren und Dienstleistungen?", DEFAULT_POINTS, List.of(
                        new Option('A', "Bruttoinlandsprodukt (BIP)"), new Option('B', "Nettoinlandsprodukt (NIP)"),
                        new Option('C', "Bruttonationaleinkommen (BNE)"), new Option('D', "Gesamtwirtschaftliche Wertschöpfung (GWW)")), new Option('A', "Bruttoinlandsprodukt (BIP)"), Topic.ECONOMICS),
                new SingleChoiceQuestion("Wie nennt man die allgemeine Erhöhung des Preisniveaus?", DEFAULT_POINTS, List.of(
                        new Option('A', "Deflation"), new Option('B', "Stagnation"),
                        new Option('C', "Inflation"), new Option('D', "Rezession")), new Option('C', "Inflation"), Topic.ECONOMICS),
                new SingleChoiceQuestion("Welche Institution ist im Euroraum hauptsächlich für die Geldpolitik verantwortlich?", DEFAULT_POINTS, List.of(
                        new Option('A', "Europäische Kommission"), new Option('B', "Europäischer Rechnungshof"),
                        new Option('C', "Europäische Investitionsbank (EIB)"), new Option('D', "Europäische Zentralbank (EZB)")), new Option('D', "Europäische Zentralbank (EZB)"), Topic.ECONOMICS),
                new SingleChoiceQuestion("Wie nennt man den Punkt, an dem die Erlöse eines Unternehmens genau seinen Kosten entsprechen?", DEFAULT_POINTS, List.of(
                        new Option('A', "Deckungsbeitrag"), new Option('B', "Rentabilitätspunkt"),
                        new Option('C', "Gewinnschwelle"), new Option('D', "Liquiditätsschwelle")), new Option('C', "Gewinnschwelle"), Topic.ECONOMICS),
                new SingleChoiceQuestion("Wie nennt man einen länger anhaltenden Rückgang des allgemeinen Preisniveaus?", DEFAULT_POINTS, List.of(
                        new Option('A', "Disinflation"), new Option('B', "Deflation"),
                        new Option('C', "Depression"), new Option('D', "Stagflation")), new Option('B', "Deflation"), Topic.ECONOMICS),
                new SingleChoiceQuestion("Was beschreibt der Begriff „Opportunitätskosten“?", DEFAULT_POINTS, List.of(
                        new Option('A', "Der entgangene Nutzen der besten nicht gewählten Alternative."), new Option('B', "Die Kosten, die durch eine falsche Entscheidung entstehen."),
                        new Option('C', "Die zusätzlichen Kosten einer Investition."), new Option('D', "Die Kosten für ungenutzte Produktionskapazitäten.")), new Option('A', "Der entgangene Nutzen der besten nicht gewählten Alternative."), Topic.ECONOMICS),
                new SingleChoiceQuestion("Wie nennt man die gezielte Streuung von Kapital auf verschiedene Anlageklassen zur Verringerung des Risikos?", DEFAULT_POINTS, List.of(
                        new Option('A', "Spekulation"), new Option('B', "Kapitalisierung"),
                        new Option('C', "Diversifikation"), new Option('D', "Hedging")), new Option('C', "Diversifikation"), Topic.ECONOMICS),
                new SingleChoiceQuestion("Wie heißt die theoretische Grenze für die Geschwindigkeit von Information im Vakuum?", DEFAULT_POINTS, List.of(
                        new Option('A', "Relativgeschwindigkeit"), new Option('B', "Grenzgeschwindigkeit"),
                        new Option('C', "Fluchtgeschwindigkeit"), new Option('D', "Lichtgeschwindigkeit")), new Option('D', "Lichtgeschwindigkeit"), Topic.PHYSICS),
                new SingleChoiceQuestion("In welcher Einheit wird elektrische Spannung gemessen?", DEFAULT_POINTS, List.of(
                        new Option('A', "Volt (V)"), new Option('B', "Ohm (Ω)"),
                        new Option('C', "Ampere (A)"), new Option('D', "Watt (W)")), new Option('A', "Volt (V)"), Topic.PHYSICS),
                new NumericQuestion("Mit wie vielen Figuren startet ein Schachspiel?", DEFAULT_POINTS, Double.valueOf("32"), 0, Topic.GENERAL_KNOWLEDGE),
                new SingleChoiceQuestion("Wer malte die Mona Lisa?", DEFAULT_POINTS, List.of(
                        new Option('A', "Michelangelo"), new Option('B', "Leonardo da Vinci"),
                        new Option('C', "Sandro Botticelli"), new Option('D', "Rembrandt")), new Option('B', "Leonardo da Vinci"), Topic.GENERAL_KNOWLEDGE),
                new SingleChoiceQuestion("Was bedeutet „Philosophie“ wörtlich?", DEFAULT_POINTS, List.of(
                        new Option('A', "Liebe zur Weisheit"), new Option('B', "Kunst des Denkens"),
                        new Option('C', "Suche nach Wahrheit"), new Option('D', "Streben nach Erkenntnis")), new Option('A', "Liebe zur Weisheit"), Topic.GENERAL_KNOWLEDGE),
                new SingleChoiceQuestion("Wer schrieb „1984“?", DEFAULT_POINTS, List.of(
                        new Option('A', "H. G. Wells"), new Option('B', "Aldous Huxley"),
                        new Option('C', "George Orwell"), new Option('D', "Ray Bradbury")), new Option('C', "George Orwell"), Topic.GENERAL_KNOWLEDGE),
                new SingleChoiceQuestion("Welcher Komponist wurde trotz zunehmender Taubheit zu einem der bedeutendsten Komponisten Europas?", DEFAULT_POINTS, List.of(
                        new Option('A', "Ludwig van Beethoven"), new Option('B', "Johann Sebastian Bach"),
                        new Option('C', "Wolfgang Amadeus Mozart"), new Option('D', "Franz Schubert")), new Option('A', "Ludwig van Beethoven"), Topic.GENERAL_KNOWLEDGE),
                new NumericQuestion("Wie viele Spieler stehen bei einer Fußballmannschaft regulär gleichzeitig auf dem Feld?", DEFAULT_POINTS, Double.valueOf("11"), 0, Topic.GENERAL_KNOWLEDGE),
                new TrueOrFalseQuestion("Die Große Chinesische Mauer ist mit bloßem Auge vom All aus sichtbar.",
                        DEFAULT_POINTS,
                        Option.getOptionsForTrueOrFalse().get(1),
                        Topic.GEOGRAPHY),
                new SingleChoiceQuestion("Der Blitz kennt kein Hausverbot: Wie oft kann er denselben Ort treffen?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Nur einmal"),
                                new Option('B', "B: Höchstens zweimal"),
                                new Option('C', "C: Auch mehrfach"),
                                new Option('D', "D: Nur einmal pro Jahrhundert")),
                        new Option('C', "C: Auch mehrfach"),
                        Topic.EVERYDAY_MYTHS),
                new TrueOrFalseQuestion("Fledermäuse sind grundsätzlich blind – Nachtflug also nur nach Gehör. (A=Richtig, B=Falsch)",
                        DEFAULT_POINTS,
                        Option.getOptionsForTrueOrFalse().get(1),
                        Topic.EVERYDAY_MYTHS),
                new TrueOrFalseQuestion(
                        "Strauße können bei Gefahr Kopf und Hals flach auf den Boden legen. Eingraben gehört nicht zum Plan. (A=Richtig, B=Falsch)",
                        DEFAULT_POINTS,
                        Option.getOptionsForTrueOrFalse().get(0),
                        Topic.EVERYDAY_MYTHS),
                new TrueOrFalseQuestion("Menschen nutzen nur 10 % ihres Gehirns. Die restlichen 90 % haben offenbar Semesterferien. (A=Richtig, B=Falsch)",
                        DEFAULT_POINTS,
                        Option.getOptionsForTrueOrFalse().get(1),
                        Topic.EVERYDAY_MYTHS),
                new TrueOrFalseQuestion(
                        "Die Jahreszeiten entstehen vor allem durch die Neigung der Erdachse. Die Erde hat also dauerhaft Schieflage. (A=Richtig, B=Falsch)",
                        DEFAULT_POINTS,
                        Option.getOptionsForTrueOrFalse().get(0),
                        Topic.EVERYDAY_MYTHS),

                new SingleChoiceQuestion("Selbstbewusstes Spielzeug: Im ersten „Toy Story“ hält sich Buzz für einen echten ...?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Space Ranger"),
                                new Option('B', "B: Rennfahrer"),
                                new Option('C', "C: Cowboy"),
                                new Option('D', "D: Astronauten der NASA")),
                        new Option('A', "A: Space Ranger"),
                        Topic.FILMS_AND_SERIES),
                new SingleChoiceQuestion("Kaffee statt Karriereplanung: Wie heißt das Stammcafé der Gruppe in „Friends“?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Café Nervosa"),
                                new Option('B', "B: Monk’s Café"),
                                new Option('C', "C: MacLaren’s Pub"),
                                new Option('D', "D: Central Perk")),
                        new Option('D', "D: Central Perk"),
                        Topic.FILMS_AND_SERIES),
                new TrueOrFalseQuestion(
                        "Zu Beginn von „Breaking Bad“ arbeitet Walter White als Physiklehrer. Die Berufswahl eskaliert erst später. (A=Richtig, B=Falsch)",
                        DEFAULT_POINTS,
                        Option.getOptionsForTrueOrFalse().get(1),
                        Topic.FILMS_AND_SERIES),
                new GuessingQuestion(
                        "Sitzfleisch gefragt: Wie viele Minuten dauert die ursprüngliche 2D-Kinofassung von „Titanic“ (1997)? Auf ganze Minuten runden. Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        195.0,
                        DEFAULT_RANGES,
                        0,
                        Topic.FILMS_AND_SERIES),
                new NumericQuestion(
                        "Zeitreise statt Strafzettel: Wie viele Meilen pro Stunde (mph) braucht der DeLorean in „Zurück in die Zukunft“ für eine Zeitreise? Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        88.0,
                        0,
                        Topic.FILMS_AND_SERIES),

                new SingleChoiceQuestion("Metall außen, Überraschung innen: Zu welcher Instrumentenfamilie gehört das Saxofon?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Blechblasinstrumente"),
                                new Option('B', "B: Holzblasinstrumente"),
                                new Option('C', "C: Streichinstrumente"),
                                new Option('D', "D: Schlaginstrumente")),
                        new Option('B', "B: Holzblasinstrumente"),
                        Topic.MUSIC),
                new SingleChoiceQuestion("Wofür steht der Bandname ABBA – außer für hartnäckige Ohrwürmer?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Abkürzung für einen Musikpreis"),
                                new Option('B', "B: Schwedisch für „Tanz“"),
                                new Option('C', "C: Name des ersten Proberaums"),
                                new Option('D', "D: Initialen der vier Vornamen")),
                        new Option('D', "D: Initialen der vier Vornamen"),
                        Topic.MUSIC),
                new TrueOrFalseQuestion(
                        "Queen-Gitarrist Brian May promovierte in Astrophysik. Er kennt sich also auch mit anderen Stars aus. (A=Richtig, B=Falsch)",
                        DEFAULT_POINTS,
                        Option.getOptionsForTrueOrFalse().get(0),
                        Topic.MUSIC),
                new GuessingQuestion(
                        "Playlist ohne WLAN: Wie viele Minuten Musik passen auf eine 650-MB-CD, wenn sie als Audio-CD gebrannt wird? Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        74.0,
                        DEFAULT_RANGES,
                        0,
                        Topic.MUSIC),
                new NumericQuestion("Wie viele Tasten hat ein Klavier mit heutiger Standardklaviatur – genug für sehr viele falsche Töne? Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        88.0,
                        0,
                        Topic.MUSIC),

                new SingleChoiceQuestion("Was meint „Swag“ (Jugendwort 2011)? Die Rechtschreibprüfung hilft heute nicht.",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Coolheit und Lässigkeit"),
                                new Option('B', "B: Hektik und Ungeduld"),
                                new Option('C', "C: Müdigkeit und Hunger"),
                                new Option('D', "D: Ordnung und Pünktlichkeit")),
                        new Option('A', "A: Coolheit und Lässigkeit"),
                        Topic.OLD_YOUTH_WORDS),
                new SingleChoiceQuestion("Wofür steht „YOLO“ (2012), der Klassiker vor einer fragwürdigen Entscheidung?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: You often lose online"),
                                new Option('B', "B: You only live once"),
                                new Option('C', "C: Your own little office"),
                                new Option('D', "D: You order lunch outside")),
                        new Option('B', "B: You only live once"),
                        Topic.OLD_YOUTH_WORDS),
                new SingleChoiceQuestion("Wer war im Jugendjargon 2013 der „Babo“ – auch ohne Chefsessel?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Ein Neuling"),
                                new Option('B', "B: Ein Spaßvogel"),
                                new Option('C', "C: Ein Mitläufer"),
                                new Option('D', "D: Der Chef")),
                        new Option('D', "D: Der Chef"),
                        Topic.OLD_YOUTH_WORDS),
                new TrueOrFalseQuestion(
                        "Handy-Zombie mit Datenvertrag: Ein „Smombie“ (2015) starrt beim Gehen aufs Handy und übersieht seine Umgebung. (A=Richtig, B=Falsch)",
                        DEFAULT_POINTS,
                        Option.getOptionsForTrueOrFalse().get(0),
                        Topic.OLD_YOUTH_WORDS),
                new TrueOrFalseQuestion("Rechtschreibung im Energiesparmodus: „I bims“ (Jugendwort 2017) bedeutet „Ich bin müde“. (A=Richtig, B=Falsch)",
                        DEFAULT_POINTS,
                        Option.getOptionsForTrueOrFalse().get(1),
                        Topic.OLD_YOUTH_WORDS),

                new SingleChoiceQuestion("Höflichkeit für Faule: Was sollte James Boyles patentierter Grußapparat von 1896 automatisch bewegen?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Den rechten Arm"),
                                new Option('B', "B: Den Hut"),
                                new Option('C', "C: Die Schuhe"),
                                new Option('D', "D: Den Spazierstock")),
                        new Option('B', "B: Den Hut"),
                        Topic.ABSURD_INVENTIONS),
                new SingleChoiceQuestion("Hunde mit Hängeohren: Wobei sollten die Ohrenschützer im Patent US4233942 helfen?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Beim Fressen sauber bleiben"),
                                new Option('B', "B: Beim Schwimmen besser lenken"),
                                new Option('C', "C: Beim Schlafen leiser schnarchen"),
                                new Option('D', "D: Beim Bellen höher klingen")),
                        new Option('A', "A: Beim Fressen sauber bleiben"),
                        Topic.ABSURD_INVENTIONS),
                new SingleChoiceQuestion("Selbstmotivation mit Nachdruck: Was sollte die Kurbelmaschine im Patent US6293874 dem Benutzer verpassen?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Eine Rückenmassage"),
                                new Option('B', "B: Eine neue Frisur"),
                                new Option('C', "C: Einen Handschlag"),
                                new Option('D', "D: Einen Tritt in den Po")),
                        new Option('D', "D: Einen Tritt in den Po"),
                        Topic.ABSURD_INVENTIONS),
                new GuessingQuestion("Haustier ohne Futterkosten: Wie viele US-Cent kostete Gary Dahls „Pet Rock“ 1975 im Einzelhandel? Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        395.0,
                        DEFAULT_RANGES,
                        0,
                        Topic.ABSURD_INVENTIONS),
                new GuessingQuestion(
                        "Eine Feder auf Tour: Wie viele Slinkys wurden bei der ersten Gimbels-Vorführung 1945 in 90 Minuten verkauft? Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        400.0,
                        DEFAULT_RANGES,
                        0,
                        Topic.ABSURD_INVENTIONS),

                new SingleChoiceQuestion("„Du glaubst nie, was dann passiert!“ soll vor allem Klicks erzeugen. Wie nennt man solche Köder-Überschriften?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Clicksave"),
                                new Option('B', "B: Crowdfunding"),
                                new Option('C', "C: Clickbait"),
                                new Option('D', "D: Cloudgaming")),
                        new Option('C', "C: Clickbait"),
                        Topic.INTERNET_CULTURE),
                new SingleChoiceQuestion("Der Link verspricht die Klausurlösung, liefert aber einen klassischen Rickroll. Welcher Sänger erscheint?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: David Hasselhoff"),
                                new Option('B', "B: Robbie Williams"),
                                new Option('C', "C: Rick Astley"),
                                new Option('D', "D: Phil Collins")),
                        new Option('C', "C: Rick Astley"),
                        Topic.INTERNET_CULTURE),
                new TrueOrFalseQuestion(
                        "Doomscrolling bedeutet, lange schlechte Nachrichten im Netz zu lesen. Die gute Laune scrollt dabei weg. (A=Richtig, B=Falsch)",
                        DEFAULT_POINTS,
                        Option.getOptionsForTrueOrFalse().get(0),
                        Topic.INTERNET_CULTURE),
                new GuessingQuestion(
                        "Twitter zwang selbst Plaudertaschen zur Kürze: Wie viele Zeichen durfte ein Tweet ursprünglich maximal haben? Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        140.0,
                        DEFAULT_RANGES,
                        0,
                        Topic.INTERNET_CULTURE),
                new GuessingQuestion("Pixelkrieg auf Reddit: Welche Kantenlänge hatte die quadratische r/place-Leinwand 2017, in Pixeln? Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        1000.0,
                        DEFAULT_RANGES,
                        0,
                        Topic.INTERNET_CULTURE),

                new SingleChoiceQuestion("Finger schneller als Gehirn: Welche Standard-Tastenkombination steht unter Windows für „Rückgängig“?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Strg + F"), new Option('B', "B: Strg + S"), new Option('C', "C: Strg + P"), new Option('D', "D: Strg + Z")),
                        new Option('D', "D: Strg + Z"),
                        Topic.TECH_PITFALLS),
                new SingleChoiceQuestion("Bluetooth klingt nach Zahnarzttermin. Worauf geht der Name tatsächlich zurück?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Auf eine blaue Kontrollleuchte"),
                                new Option('B', "B: Auf eine Zahnbürstenmarke"),
                                new Option('C', "C: Auf einen dänischen König"),
                                new Option('D', "D: Auf einen blauen Stecker")),
                        new Option('C', "C: Auf einen dänischen König"),
                        Topic.TECH_PITFALLS),
                new TrueOrFalseQuestion(
                        "Der private Modus von Firefox macht dich automatisch anonym im Internet. Digitaler Tarnumhang inklusive. (A=Richtig, B=Falsch)",
                        DEFAULT_POINTS,
                        Option.getOptionsForTrueOrFalse().get(1),
                        Topic.TECH_PITFALLS),
                new GuessingQuestion(
                        "Pixel-Konfetti: Wie viele Ziffern passen maximal in einen QR-Code Model 2 (Version 40, Fehlerkorrektur L)? Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        7089.0,
                        DEFAULT_RANGES,
                        0,
                        Topic.TECH_PITFALLS),
                new NumericQuestion("Ein Byte kommt selten allein: Aus wie vielen Bits besteht es nach heutiger Standarddefinition? Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        8.0,
                        0,
                        Topic.TECH_PITFALLS),

                new SingleChoiceQuestion("Was sind „Fisimatenten“ – abgesehen von einem Albtraum für die Rechtschreibprüfung?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Unnötige Umstände und Ausflüchte"),
                                new Option('B', "B: Besonders kleine Fußnoten"),
                                new Option('C', "C: Feierliche Begrüßungen"),
                                new Option('D', "D: Abkürzungen aus Behördenbriefen")),
                        new Option('A', "A: Unnötige Umstände und Ausflüchte"),
                        Topic.WORD_CURIOSITIES),
                new SingleChoiceQuestion("Du erhältst „a gift“ zum Geburtstag. Was bedeutet das englische Wort? Erst einmal ruhig bleiben.",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Giftstoff"), new Option('B', "B: Geschenk"), new Option('C', "C: Gebühr"), new Option('D', "D: Warnung")),
                        new Option('B', "B: Geschenk"),
                        Topic.WORD_CURIOSITIES),
                new SingleChoiceQuestion("„I become a sausage“, sagt jemand im Imbiss. Was behauptet er wörtlich? Guten Appetit!",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Ich bekomme eine Wurst"),
                                new Option('B', "B: Ich bestelle eine Wurst"),
                                new Option('C', "C: Ich werde eine Wurst"),
                                new Option('D', "D: Ich bezahle eine Wurst")),
                        new Option('C', "C: Ich werde eine Wurst"),
                        Topic.WORD_CURIOSITIES),
                new TrueOrFalseQuestion(
                        "Vogellogistik für Fortgeschrittene: „Eulen nach Athen tragen“ bedeutet, eine besonders nützliche Arbeit zu leisten. (A=Richtig, B=Falsch)",
                        DEFAULT_POINTS,
                        Option.getOptionsForTrueOrFalse().get(1),
                        Topic.WORD_CURIOSITIES),
                new TrueOrFalseQuestion(
                        "Wörter im Beziehungskrach: „Bittersüß“ ist ein Oxymoron, eine Verbindung gegensätzlicher Begriffe. (A=Richtig, B=Falsch)",
                        DEFAULT_POINTS,
                        Option.getOptionsForTrueOrFalse().get(0),
                        Topic.WORD_CURIOSITIES),

                new SingleChoiceQuestion("Die Chips werden weniger, der Preis bleibt gleich. Wie nennt man diese Diät für den Packungsinhalt?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Shrinkflation"),
                                new Option('B', "B: Deflation"),
                                new Option('C', "C: Diversifikation"),
                                new Option('D', "D: Liquidation")),
                        new Option('A', "A: Shrinkflation"),
                        Topic.ADVERTISING_TRICKS),
                new TrueOrFalseQuestion(
                        "Mit „Nur noch heute!“ können Shops Zeitdruck erzeugen, statt dir mehr Ruhe zum Vergleichen zu geben. (A=Richtig, B=Falsch)",
                        DEFAULT_POINTS,
                        Option.getOptionsForTrueOrFalse().get(0),
                        Topic.ADVERTISING_TRICKS),
                new GuessingQuestion(
                        "Ein Hoodie kostet 100 Euro: erst 20 % Rabatt, dann 20 % Aufschlag auf den neuen Preis. Schätze den Endpreis in ganzen Euro. Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        96.0,
                        DEFAULT_RANGES,
                        0,
                        Topic.ADVERTISING_TRICKS),
                new GuessingQuestion(
                        "Drei Packungen kosten sonst je 3 Euro. „Nimm drei, zahl zwei!“: Wie viel Prozent spart man? Auf ganze Prozent runden. Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        33.0,
                        DEFAULT_RANGES,
                        0,
                        Topic.ADVERTISING_TRICKS),
                new NumericQuestion("750 ml Saft kosten 3 Euro. Die Packung ruft „Schnäppchen!“. Wie hoch ist der Literpreis in Euro? Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        4.0,
                        0,
                        Topic.ADVERTISING_TRICKS),

                new SingleChoiceQuestion("Du liegst im Rennen auf Platz 3 und überholst die Person auf Platz 2. Welchen Platz hast du danach?",
                        DEFAULT_POINTS,
                        List.of(new Option('A', "A: Platz 1"), new Option('B', "B: Platz 2"), new Option('C', "C: Platz 3"), new Option('D', "D: Platz 4")),
                        new Option('B', "B: Platz 2"),
                        Topic.LOGIC_TRAPS),
                new GuessingQuestion("20 Leute begrüßen sich per Handschlag: Jedes Paar genau einmal. Wie viele Handschläge sind das? Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        190.0,
                        DEFAULT_RANGES,
                        0,
                        Topic.LOGIC_TRAPS),
                new GuessingQuestion("Eine magische Münze startet bei 1 Euro und verdoppelt ihren Wert zehnmal. Wie viele Euro sind es dann? Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        1024.0,
                        DEFAULT_RANGES,
                        0,
                        Topic.LOGIC_TRAPS),
                new NumericQuestion(
                        "Vier Bonbons, strenge Disziplin: Das erste isst du jetzt, dann alle 30 Minuten eins. Nach wie vielen Minuten ist das letzte weg? Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        90.0,
                        0,
                        Topic.LOGIC_TRAPS),
                new NumericQuestion(
                        "10 schwarze und 10 weiße Socken im Dunkeln: Wie viele musst du mindestens ziehen, um sicher zwei gleicher Farbe zu haben? Nur Zahl eingeben.",
                        DEFAULT_POINTS,
                        3.0,
                        0,
                        Topic.LOGIC_TRAPS)
        );
        return questions;
    }

    public static Map<Range, Double> loadRanges() {
        Map<Range, Double> ranges = new HashMap<>();
        ranges.put(new Range(0.0, 0.75), 0.0);
        ranges.put(new Range(0.75, 0.85), 0.6);
        ranges.put(new Range(0.85, 0.9), 0.8);
        ranges.put(new Range(0.9, 0.95), 0.9);
        ranges.put(new Range(0.95, 0.99), 0.95);
        ranges.put(new Range(0.99, 1), 1.0);
        return ranges;
    }

    private static Map<Topic, List<Question>> indexQuestions() {
        Map<Topic, List<Question>> index = new EnumMap<>(Topic.class);
        for (Topic topic : Topic.values()) index.put(topic, new ArrayList<>());
        for (Question question : questions) index.get(question.getTopic()).add(question);
        return index;
    }

    public static List<Question> getRandomQuestions(int n, Set<Topic> topics) {
        Objects.requireNonNull(topics, "topics");
        if (n <= 0 || topics.isEmpty()) return new ArrayList<>();
        Map<Topic, Set<Integer>> selectedIndices = new EnumMap<>(Topic.class);
        for (Topic topic : topics) {
            selectedIndices.put(Objects.requireNonNull(topic, "topic"), new HashSet<>());
        }
        List<Question> selectedQuestions = new ArrayList<>();
        Random random = new Random();
        while (selectedQuestions.size() < n) {
            boolean selectedInPass = false;
            for (Map.Entry<Topic, Set<Integer>> entry : selectedIndices.entrySet()) {
                if (selectedQuestions.size() >= n) break;
                List<Question> topicQuestions = questionsByTopic.get(entry.getKey());
                Set<Integer> usedIndices = entry.getValue();
                if (usedIndices.size() == topicQuestions.size()) continue;

                int selectedIdx;
                do {
                    selectedIdx = random.nextInt(topicQuestions.size());
                } while (!usedIndices.add(selectedIdx));
                selectedQuestions.add(topicQuestions.get(selectedIdx));
                selectedInPass = true;
            }
            if (!selectedInPass) break;
        }
        Collections.shuffle(selectedQuestions, random);
        return selectedQuestions;
    }
}
