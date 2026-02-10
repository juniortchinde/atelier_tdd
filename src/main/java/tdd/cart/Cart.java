package tdd.cart;
import java.math.BigDecimal;
import java.util.*;

public class Cart {
    private enum PromoType { PERCENTAGE, BUY_N_GET_1 }

    private final Map<String, Product> products;
    private final Map<String, Promotion> availablePromos = new HashMap<>();
    private final Set<String> activePromoCodes = new HashSet<>();

    public Cart() {
        this.products = new HashMap<>();
    }
    
     // Ajout d'une quantité d'une référence produit au prix spécifié.
    public void addItem(String reference, BigDecimal price, int quantity) {
        // On récupère le produit existant ou on en crée un nouveau (Injection/Composition)
        Product product = products.computeIfAbsent(reference, Product::new);
        product.addStock(price, quantity);
    }

     // Retrait du panier d'une quantité donnée d'une référence.
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
            total = total.add(calculateProductNetPrice(product));
        }
        return total;
    }

    private BigDecimal calculateProductNetPrice(Product product) {
        String ref = product.getReference();
        BigDecimal netAmount = product.getTotalValue();

        // Récupérer les promos actives pour ce produit
        List<Promotion> promos = activePromoCodes.stream()
                .map(availablePromos::get)
                .filter(p -> p.reference.equals(ref))
                .toList();

        // Étape 1 : Appliquer d'abord les gratuits (Buy N Get 1)
        // Cela réduit la base imposable
        for (Promotion p : promos) {
            if (p.type == PromoType.BUY_N_GET_1) {
                int totalQty = product.getTotalQuantity();
                int packSize = p.value + 1;
                int freeItemsCount = totalQty / packSize;
                BigDecimal discountBNG = product.getCheapestItemsValue(freeItemsCount);
                netAmount = netAmount.subtract(discountBNG);
            }
        }

        // Étape 2 : Appliquer les pourcentages sur le montant RESTANT
        for (Promotion p : promos) {
            if (p.type == PromoType.PERCENTAGE) {
                // Vérif seuil minPrice sur le montant net actuel (ou brut ? souvent brut, mais restons logique)
                // Le prompt disait "prix minimum auquel s'applique la réduction".
                // Si on a payé 200€ (après gratuité), c'est ce montant qu'on compare au seuil.
                if (netAmount.compareTo(p.minPrice) >= 0) {
                    BigDecimal discountPct = netAmount.multiply(BigDecimal.valueOf(p.value))
                            .divide(BigDecimal.valueOf(100));
                    netAmount = netAmount.subtract(discountPct);
                }
            }
        }

        return netAmount; // Ne peut pas être négatif
    }

    // Mise à jour du calcul
    private BigDecimal calculateDiscountForReference(String reference, BigDecimal productTotal) {
        Product product = products.get(reference);

        // On cherche la promo active
        return activePromoCodes.stream()
                .map(availablePromos::get)
                .filter(p -> p.reference.equals(reference))
                .findFirst()
                .map(p -> applyPromotion(p, product, productTotal))
                .orElse(BigDecimal.ZERO);
    }
    
     // Accesseur retournant la quantité totale d'une référence donnée.
    public int getQuantity(String reference) {
        return getProductOrThrow(reference).getTotalQuantity();
    }
    
     // Accesseur retournant la quantité d'une référence pour un prix spécifique.
    public int getQuantity(String reference, BigDecimal price) {
        return getProductOrThrow(reference).getQuantityAtPrice(price);
    }

     // Accesseur retournant le montant total pour un couple référence/prix.
     
    public BigDecimal getSubTotal(String reference, BigDecimal price) {
        int qty = getQuantity(reference, price); // Délègue la validation au produit via getQuantity
        return price.multiply(BigDecimal.valueOf(qty));
    }

     // Accesseur énumérant les références présentes dans le panier.
    public Set<String> getReferences() {
        return Collections.unmodifiableSet(products.keySet());
    }

     // Accesseur énumérant les prix unitaires pour une référence donnée.
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
        availablePromos.put(code, new Promotion(reference, percentage, BigDecimal.ZERO));
    }

    public void registerPromo(String code, String reference, int percentage, BigDecimal minPrice) {
        if (code == null || code.isEmpty()) throw new IllegalArgumentException("Code vide");
        // ... validations existantes ...
        availablePromos.put(code, new Promotion(reference, percentage, minPrice));
    }

    public boolean activatePromo(String code) {
        if (!availablePromos.containsKey(code)) return false;
        Promotion newPromo = availablePromos.get(code);
        // Vérification de compatibilité
        boolean incompatible = activePromoCodes.stream()
                .map(availablePromos::get)
                .filter(p -> p.reference.equals(newPromo.reference))
                // On rejette SI c'est le même type de promo
                // (On suppose qu'on ne peut pas cumuler deux pourcentages, ni deux BNG1)
                .anyMatch(p -> p.type == newPromo.type);

        if (incompatible) return false;

        activePromoCodes.add(code);
        return true;
    }

    public void registerBuyNGetOneFree(String code, String reference, int n) {
        if (code == null || code.isEmpty()) throw new IllegalArgumentException("Code vide");
        if (n <= 0) throw new IllegalArgumentException("N doit être positif");
        availablePromos.put(code, new Promotion(reference, n));
    }


    private BigDecimal applyPromotion(Promotion p, Product product, BigDecimal totalAmount) {
        if (p.type == PromoType.PERCENTAGE) {
            if (totalAmount.compareTo(p.minPrice) < 0) return BigDecimal.ZERO;
            return totalAmount.multiply(BigDecimal.valueOf(p.value))
                    .divide(BigDecimal.valueOf(100));
        }
        else if (p.type == PromoType.BUY_N_GET_1) {
            int totalQty = product.getTotalQuantity();
            // Formule : Pour chaque lot de (N+1), 1 est offert.
            // Ex: 2 achetés 1 offert (N=2). Pack de 3.
            int packSize = p.value + 1;
            int freeItemsCount = totalQty / packSize;

            return product.getCheapestItemsValue(freeItemsCount);
        }
        return BigDecimal.ZERO;
    }

    private static class Promotion {
        String reference;
        PromoType type;
        int value; // Sert pour % ou pour N
        BigDecimal minPrice; // Optionnel

        // Constructeur existant (% promo)
        Promotion(String reference, int percentage, BigDecimal minPrice) {
            this.reference = reference;
            this.type = PromoType.PERCENTAGE;
            this.value = percentage;
            this.minPrice = minPrice;
        }

        // Nouveau constructeur (BNG1 promo)
        Promotion(String reference, int buyN) {
            this.reference = reference;
            this.type = PromoType.BUY_N_GET_1;
            this.value = buyN;
            this.minPrice = BigDecimal.ZERO;
        }
    }
}