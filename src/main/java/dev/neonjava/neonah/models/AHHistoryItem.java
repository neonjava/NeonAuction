package dev.neonjava.neonah.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AHHistoryItem {
    private final UUID seller;
    private final UUID buyer;
    private final double price;
    private final String itemName;
    private final long timestamp;
}
