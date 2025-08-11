package atg.rest;

public class NormalizedPriceRange {
    private String symbol;
    private double range;

    public NormalizedPriceRange(String symbol, double range) {
        this.symbol = symbol;
        this.range = range;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getRange() {
        return range;
    }

    @Override
    public String toString() {
        return "NormalizedRange{" +
                "symbol='" + symbol + '\'' +
                ", range=" + range +
                '}';
    }
}

