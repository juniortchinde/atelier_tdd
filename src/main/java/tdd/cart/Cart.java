package tdd.cart;
import java.math.BigDecimal;
import java.util.*;

public class Cart {

    private final Map<String, Product> products;
    private final Map<String, Promotion> availablePromos = new HashMap<>();
    private final Set<String> activePromoCodes = new HashSet<>();

    public Cart() {
        this.products = new HashMap<>();
    }

    /**
     * Ajout d'une quantité d'une référence produit au prix spécifié.
     */
    public void addItem(String reference, BigDecimal price, int quantity) {
        // On récupère le produit existant ou on en crée un nouveau (Injection/Composition)
        Product product = products.computeIfAbsent(reference, Product::new);
        product.addStock(price, quantity);
    }

    /**
     * Retrait du panier d'une quantité donnée d'une référence.
     */
    public void removeItem(String reference, int quantity) {
        Product product = getProductOrThrow(reference);
        product.removeStock(quantity);
        // Si le produit est vide après retrait, on le supprime du panier
        if (product.isEmpty()) {
            products.remove(reference);
        }
    }


    public BigDecimal getTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;

        for (Product product : products.values()) {
            BigDecimal productValue = product.getTotalValue();
            BigDecimal discount = calculateDiscountForReference(product.getReference(), productValue);
            total = total.add(productValue.subtract(discount));
        }
        return total;
    }

    private BigDecimal calculateDiscountForReference(String reference, BigDecimal amount) {
        // Cherche si une promo active concerne cette référence
        return activePromoCodes.stream()
                .map(availablePromos::get)
                .filter(p -> p.reference.equals(reference))
                .findFirst()
                .map(p -> amount.multiply(BigDecimal.valueOf(p.percentage))
                        .divide(BigDecimal.valueOf(100)))
                .orElse(BigDecimal.ZERO);
    }
    /**
     * Accesseur retournant la quantité totale d'une référence donnée.
     */
    public int getQuantity(String reference) {
        return getProductOrThrow(reference).getTotalQuantity();
    }
    /**
     * Accesseur retournant la quantité d'une référence pour un prix spécifique.
     */
    public int getQuantity(String reference, BigDecimal price) {
        return getProductOrThrow(reference).getQuantityAtPrice(price);
    }

    /**
     * Accesseur retournant le montant total pour un couple référence/prix.
     */
    public BigDecimal getSubTotal(String reference, BigDecimal price) {
        int qty = getQuantity(reference, price); // Délègue la validation au produit via getQuantity
        return price.multiply(BigDecimal.valueOf(qty));
    }

     // Accesseur énumérant les références présentes dans le panier.
    public Set<String> getReferences() {
        return Collections.unmodifiableSet(products.keySet());
    }

    /**
     * Accesseur énumérant les prix unitaires pour une référence donnée.
     */
    public Set<BigDecimal> getPricesForReference(String reference) {
        return getProductOrThrow(reference).getPrices();
    }

    private Product getProductOrThrow(String reference) {
        if (reference == null || !products.containsKey(reference)) {
            throw new IllegalArgumentException("Erreur : La référence '" + reference + "' n'est pas dans le panier.");
        }
        return products.get(reference);
    }

    public void registerPromo(String code, String reference, int percentage) {
        if (code == null || code.isEmpty()) throw new IllegalArgumentException("Code vide");
        if (percentage <= 0 || percentage >= 100) throw new IllegalArgumentException("Pourcentage invalide");
        // On stocke simplement les infos
        availablePromos.put(code, new Promotion(reference, percentage));
    }

    public boolean activatePromo(String code) {
        if (availablePromos.containsKey(code)) {
            activePromoCodes.add(code);
            return true;
        }
        return false;
    }


    private static class Promotion {
        String reference;
        int percentage;

        Promotion(String reference, int percentage) {
            this.reference = reference;
            this.percentage = percentage;
        }
    }
}