package org.hsw.domain.question;

public class Range {

    private final double lowerBound;
    private final double upperBound;

    /**
     * @param lowerBound 0.0 - 1.0, inkludierend
     * @param upperBound 0.0 - 1.0, exkludierend
     */
    public Range(final double lowerBound, final double upperBound) {
        if (lowerBound > upperBound || lowerBound < 0 || upperBound < 0 || upperBound > 1) {
            throw new IllegalArgumentException("lowerBound or upperBound out of range");
        }
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    /**
     * prüft, ob der Wert innerhalb der Grenzen liegt <br>
     * {@code lowerBound} ist inkludierend, {@code upperBound} ist exkludierend
     *
     * @param n zu prüfender Wert
     * @return Ergebnis der Prüfung, {@code true} oder {@code false}
     */
    public boolean matches(double n) {
        return lowerBound <= n && n < upperBound;
    }
}
