package tdd.cart;

import java.math.BigDecimal;
import java.util.*;

public class Cart {

    // Structure : Map<Référence, TreeMap<Prix, Quantité>>
    // TreeMap est configuré en ordre inverse (reverseOrder) pour faciliter le retrait "des plus coûteux".
    private final Map<String, TreeMap<BigDecimal, Integer>> items;

    /**
     * Constructeur : Initialisation d'un panier vide.
     */
    public Cart() {
        this.items = new HashMap<>();
    }

    /**
     * Ajout d'une quantité d'une référence produit à un prix spécifié.
     * Gestion sans doublon pour le couple référence/prix (on cumule la quantité).
     */
    public void addProduct(String reference, BigDecimal price, int quantity) {
        if (reference == null || reference.isEmpty()) {
            throw new IllegalArgumentException("La référence ne peut pas être vide.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix doit être positif et non nul.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantité doit être un entier positif non nul.");
        }

        // Récupère ou crée la map des prix pour cette référence
        // On utilise reverseOrder pour trier les prix du plus haut au plus bas
        items.putIfAbsent(reference, new TreeMap<>(Collections.reverseOrder()));
        Map<BigDecimal, Integer> priceMap = items.get(reference);

        // Ajoute la quantité existante à la nouvelle quantité
        int currentQuantity = priceMap.getOrDefault(price, 0);
        priceMap.put(price, currentQuantity + quantity);
    }

    /**
     * Retrait d'une quantité donnée d'une référence, en partant des plus coûteux.
     */
    public void removeProduct(String reference, int quantityToRemove) {
        if (reference == null || !items.containsKey(reference)) return; // Rien à faire si la ref n'existe pas
        if (quantityToRemove <= 0) return;

        TreeMap<BigDecimal, Integer> priceMap = items.get(reference);

        // Itérateur sur les clés (Prix) - L'ordre est décroissant grâce au TreeMap inversé
        Iterator<Map.Entry<BigDecimal, Integer>> iterator = priceMap.entrySet().iterator();

        while (iterator.hasNext() && quantityToRemove > 0) {
            Map.Entry<BigDecimal, Integer> entry = iterator.next();
            int currentQty = entry.getValue();

            if (currentQty <= quantityToRemove) {
                // On retire toute la ligne de ce prix
                quantityToRemove -= currentQty;
                iterator.remove(); // Suppression sûre via l'itérateur
            } else {
                // On retire seulement une partie
                entry.setValue(currentQty - quantityToRemove);
                quantityToRemove = 0;
            }
        }

        // Si la référence n'a plus aucun produit (tous prix confondus), on nettoie la map principale
        if (priceMap.isEmpty()) {
            items.remove(reference);
        }
    }

    /**
     * Accesseur retournant le montant total du panier.
     */
    public BigDecimal getTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;

        for (Map<BigDecimal, Integer> priceMap : items.values()) {
            for (Map.Entry<BigDecimal, Integer> entry : priceMap.entrySet()) {
                BigDecimal price = entry.getKey();
                BigDecimal qty = BigDecimal.valueOf(entry.getValue());
                total = total.add(price.multiply(qty));
            }
        }
        return total;
    }

    /**
     * Accesseur énumérant les références présentes dans le panier sans doublon.
     */
    public Set<String> getReferences() {
        return new HashSet<>(items.keySet());
    }

    /**
     * Accesseur énumérant les prix unitaires pour une référence donnée.
     */
    public Set<BigDecimal> getPricesForReference(String reference) {
        if (!items.containsKey(reference)) {
            return Collections.emptySet();
        }
        return items.get(reference).keySet();
    }

    /**
     * Accesseur retournant la quantité totale d'une référence donnée.
     */
    public int getQuantity(String reference) {
        if (!items.containsKey(reference)) return 0;

        return items.get(reference).values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * Surcharge : Si le prix est spécifié, seule la quantité pour ce prix est retournée.
     */
    public int getQuantity(String reference, BigDecimal price) {
        if (!items.containsKey(reference)) return 0;
        return items.get(reference).getOrDefault(price, 0);
    }

    /**
     * Accesseur retournant le montant total pour un couple référence/prix existant.
     */
    public BigDecimal getSubTotal(String reference, BigDecimal price) {
        int qty = getQuantity(reference, price);
        if (qty == 0) return BigDecimal.ZERO;

        return price.multiply(BigDecimal.valueOf(qty));
    }

    public Map<String, TreeMap<BigDecimal, Integer>>getItems() {
        return items;
    }
}
