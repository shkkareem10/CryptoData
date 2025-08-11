package atg.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main Spring Boot application launcher.
 */
@SpringBootApplication
public class CryptoDataApplication {
    public static void main(String[] args) {
        SpringApplication.run(CryptoDataApplication.class, args);
    }
}

/**
 * REST Controller to handle requests related to cryptocurrency price data.
 */
@RestController
@RequestMapping("/cryptocurrency")
class CryptoDataController {
    // Map storing cryptocurrency symbol -> list of price entries sorted by timestamp
    private final Map<String, List<CryptoPriceEntry>> cryptoPriceMap = new HashMap<>();

    /**
     * Loads cryptocurrency price data from CSV files at application startup.
     * Expects files named like SYMBOL_values.csv under the data directory.
     */
    @PostConstruct
    public void initializeCryptoData() throws IOException {
        Path dataDirectory = Paths.get("src/main/java/atg/data/"); // directory containing CSV files

        // Check if the data folder exists
        if (!Files.exists(dataDirectory)) {
            throw new IOException("Data folder not found: " + dataDirectory.toAbsolutePath());
        }

        // Read each CSV file matching *_values.csv pattern
        try (DirectoryStream<Path> csvFiles = Files.newDirectoryStream(dataDirectory, "*_values.csv")) {
            for (Path csvFile : csvFiles) {
                // Extract cryptocurrency symbol from filename (e.g., BTC from BTC_values.csv)
                String cryptoSymbol = csvFile.getFileName().toString().split("_")[0].toUpperCase();

                // Read lines from CSV, skipping header, parsing timestamp and price
                List<CryptoPriceEntry> priceEntries = Files.lines(csvFile)
                        .skip(1) // skip header row
                        .map(line -> {
                            String[] fields = line.split(",");
                            long timestamp = Long.parseLong(fields[0]);
                            double price = Double.parseDouble(fields[2]);
                            return new CryptoPriceEntry(timestamp, cryptoSymbol, price);
                        })
                        .sorted(Comparator.comparingLong(CryptoPriceEntry::getTimestamp)) // sort by timestamp ascending
                        .collect(Collectors.toList());

                // Store the sorted list in the map
                cryptoPriceMap.put(cryptoSymbol, priceEntries);

                System.out.println("Loaded data for " + cryptoSymbol + ": " + priceEntries.size() + " entries");
            }
        }
    }

    /**
     * Retrieve statistics for a specific cryptocurrency symbol.
     *
     * @param symbol Cryptocurrency symbol, case-insensitive.
     * @return CryptoStatistics object with min, max, oldest, newest prices info.
     */
    @GetMapping("/statistics/{symbol}")
    public CryptoStatistics fetchCryptoStatistics(@PathVariable String symbol) {
        List<CryptoPriceEntry> prices = cryptoPriceMap.get(symbol.toUpperCase());
        if (prices == null) {
            throw new RuntimeException("Cryptocurrency not supported: " + symbol);
        }

        // Calculate min and max price from the price list
        double minPrice = prices.stream().mapToDouble(CryptoPriceEntry::getPrice).min().orElseThrow();
        double maxPrice = prices.stream().mapToDouble(CryptoPriceEntry::getPrice).max().orElseThrow();

        // Get oldest and newest price entries (first and last in sorted list)
        CryptoPriceEntry oldestEntry = prices.get(0);
        CryptoPriceEntry newestEntry = prices.get(prices.size() - 1);

        return new CryptoStatistics(symbol.toUpperCase(), oldestEntry, newestEntry, minPrice, maxPrice);
    }

    /**
     * Returns a list of cryptocurrencies sorted by their normalized price range in descending order.
     * Normalized range = (max price - min price) / min price
     */
    @GetMapping("/normalized-ranges")
    public List<NormalizedPriceRange> retrieveNormalizedRanges() {
        return cryptoPriceMap.entrySet().stream()
                .map(entry -> {
                    List<CryptoPriceEntry> prices = entry.getValue();
                    double minPrice = prices.stream().mapToDouble(CryptoPriceEntry::getPrice).min().orElseThrow();
                    double maxPrice = prices.stream().mapToDouble(CryptoPriceEntry::getPrice).max().orElseThrow();
                    double normalizedRange = (maxPrice - minPrice) / minPrice;
                    return new NormalizedPriceRange(entry.getKey(), normalizedRange);
                })
                .sorted(Comparator.comparingDouble(NormalizedPriceRange::getRange).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Returns the cryptocurrency with the highest normalized range on a given date.
     *
     * @param date Date in format yyyy-MM-dd (UTC timezone)
     * @return NormalizedPriceRange object for the crypto with highest normalized range on that date.
     */
    @GetMapping("/highest-normalized/{date}")
    public NormalizedPriceRange fetchHighestNormalizedRangeOnDate(@PathVariable String date) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));

        return cryptoPriceMap.entrySet().stream()
                .map(entry -> {
                    // Filter prices to only those on the requested date
                    List<CryptoPriceEntry> pricesOnDate = entry.getValue().stream()
                            .filter(p -> dateFormatter.format(Instant.ofEpochMilli(p.getTimestamp())).equals(date))
                            .collect(Collectors.toList());

                    // Skip if no data on this date
                    if (pricesOnDate.isEmpty()) return null;

                    double minPrice = pricesOnDate.stream().mapToDouble(CryptoPriceEntry::getPrice).min().orElseThrow();
                    double maxPrice = pricesOnDate.stream().mapToDouble(CryptoPriceEntry::getPrice).max().orElseThrow();
                    double normalizedRange = (maxPrice - minPrice) / minPrice;

                    return new NormalizedPriceRange(entry.getKey(), normalizedRange);
                })
                .filter(Objects::nonNull)
                .max(Comparator.comparingDouble(NormalizedPriceRange::getRange))
                .orElseThrow(() -> new RuntimeException("No data found for date: " + date));
    }
}

