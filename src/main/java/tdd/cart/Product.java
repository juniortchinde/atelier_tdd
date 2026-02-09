package tdd.cart;

import java.math.BigDecimal;
import java.util.*;

public class Product {

    private final String reference;
    // TreeMap inversé : Les clés (Prix) sont triées du plus grand au plus petit
    private final NavigableMap<BigDecimal, Integer> priceBatches;

    // Cache pour la quantité totale (optimisation)
    private int totalQuantity;

    public Product(String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            throw new IllegalArgumentException("Erreur : La référence ne peut pas être vide.");
        }
        this.reference = reference;
        this.priceBatches = new TreeMap<>(Comparator.reverseOrder());
        this.totalQuantity = 0;
    }

    public void addStock(BigDecimal price, int quantity) {
        validatePrice(price);
        validateQuantity(quantity);

        priceBatches.merge(price, quantity, Integer::sum);
        totalQuantity += quantity;
    }

    public void removeStock(int quantityToRemove) {
        validateQuantity(quantityToRemove);
        if (quantityToRemove > totalQuantity) {
            throw new IllegalArgumentException("Erreur : Stock insuffisant pour '" + reference + "'.");
        }

        totalQuantity -= quantityToRemove;

        // Itérateur sur les clés triées (du plus cher au moins cher)
        Iterator<Map.Entry<BigDecimal, Integer>> it = priceBatches.entrySet().iterator();

        while (quantityToRemove > 0 && it.hasNext()) {
            Map.Entry<BigDecimal, Integer> batch = it.next();
            int currentBatchQty = batch.getValue();

            if (currentBatchQty <= quantityToRemove) {
                // On consomme tout ce lot de prix
                quantityToRemove -= currentBatchQty;
                it.remove();
            } else {
                // On consomme une partie du lot
                batch.setValue(currentBatchQty - quantityToRemove);
                quantityToRemove = 0;
            }
        }
    }

    public BigDecimal getTotalValue() {
        return priceBatches.entrySet().stream()
                .map(e -> e.getKey().multiply(BigDecimal.valueOf(e.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getQuantityAtPrice(BigDecimal price) {
        validatePrice(price);
        Integer qty = priceBatches.get(price);
        if (qty == null) {
            throw new IllegalArgumentException("Erreur : Aucun stock au prix de " + price + " pour '" + reference + "'.");
        }
        return qty;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public Set<BigDecimal> getPrices() {
        return Collections.unmodifiableSet(priceBatches.keySet());
    }

    public String getReference() {
        return reference;
    }

    public boolean isEmpty() {
        return totalQuantity == 0;
    }

    // Validations internes
    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Erreur : Le prix doit être positif.");
        }
    }

    private void validateQuantity(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Erreur : La quantité doit être positive.");
        }
    }
}