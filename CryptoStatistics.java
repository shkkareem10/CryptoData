package atg.rest;

public class CryptoStatistics {
    private String symbol;
    private CryptoPriceEntry oldest;
    private CryptoPriceEntry newest;
    private double min;
    private double max;

    public CryptoStatistics(String symbol, CryptoPriceEntry oldest, CryptoPriceEntry newest, double min, double max) {
        this.symbol = symbol;
        this.oldest = oldest;
        this.newest = newest;
        this.min = min;
        this.max = max;
    }

    public String getSymbol() {
        return symbol;
    }

    public CryptoPriceEntry getOldest() {
        return oldest;
    }

    public CryptoPriceEntry getNewest() {
        return newest;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    @Override
    public String toString() {
        return "CryptoStats{" +
                "symbol='" + symbol + '\'' +
                ", oldest=" + oldest +
                ", newest=" + newest +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}

