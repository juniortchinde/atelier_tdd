package tdd.cart;

import java.math.BigDecimal;
import java.util.*;

/**
 * Classe représentant un panier d'achat.
 * Respecte les conventions Java (CamelCase, Getters, encapsulation).
 */
public class Cart {

    // Structure : Reference -> (Prix -> Quantité)
    // On utilise une NavigableMap avec un comparateur inversé pour trier les prix du plus cher au moins cher.
    private final Map<String, NavigableMap<BigDecimal, Integer>> items;

    /**
     * Constructeur : Initialisation d'un panier vide.
     */
    public Cart() {
        this.items = new HashMap<>();
    }

    /**
     * Ajoute une quantité d'une référence produit à un prix spécifié.
     *
     * @param reference La référence du produit (non vide).
     * @param price     Le prix unitaire (positif non nul).
     * @param quantity  La quantité à ajouter (entier positif non nul).
     * @throws IllegalArgumentException Si les entrées sont invalides.
     */
    public void addItem(String reference, BigDecimal price, int quantity) {
        validateReference(reference);
        validatePrice(price);
        validateQuantity(quantity);

        // Récupère ou crée la map des prix pour cette référence, triée par ordre décroissant
        items.computeIfAbsent(reference, k -> new TreeMap<>(Comparator.reverseOrder()))
                .merge(price, quantity, Integer::sum);
    }

    /**
     * Retire une quantité donnée d'une référence, en commençant par les articles les plus coûteux.
     *
     * @param reference La référence du produit.
     * @param quantityToRemove La quantité à retirer (entier positif non nul).
     * @throws IllegalArgumentException Si la référence n'existe pas, si la quantité est invalide
     * ou si l'on tente de retirer plus que la quantité totale disponible.
     */
    public void removeItem(String reference, int quantityToRemove) {
        validateReference(reference);
        validateQuantity(quantityToRemove);

        if (!items.containsKey(reference)) {
            throw new IllegalArgumentException("Erreur : La référence '" + reference + "' n'est pas dans le panier.");
        }

        NavigableMap<BigDecimal, Integer> priceMap = items.get(reference);
        int totalAvailable = priceMap.values().stream().mapToInt(Integer::intValue).sum();

        if (quantityToRemove > totalAvailable) {
            throw new IllegalArgumentException("Erreur : Tentative de retrait supérieure à la quantité disponible (" + totalAvailable + ").");
        }

        // Itération sur les prix (du plus cher au moins cher grâce au TreeMap inversé)
        Iterator<Map.Entry<BigDecimal, Integer>> iterator = priceMap.entrySet().iterator();

        while (quantityToRemove > 0 && iterator.hasNext()) {
            Map.Entry<BigDecimal, Integer> entry = iterator.next();
            int currentQty = entry.getValue();

            if (currentQty <= quantityToRemove) {
                // On retire toute la ligne pour ce prix
                quantityToRemove -= currentQty;
                iterator.remove();
            } else {
                // On réduit la quantité pour ce prix
                entry.setValue(currentQty - quantityToRemove);
                quantityToRemove = 0;
            }
        }

        // Nettoyage : si la référence n'a plus aucun article, on supprime la clé principale
        if (priceMap.isEmpty()) {
            items.remove(reference);
        }
    }

    /**
     * Retourne le montant total du panier.
     *
     * @return Le montant total (BigDecimal).
     */
    public BigDecimal getTotalAmount() {
        return items.values().stream()
                .flatMap(map -> map.entrySet().stream())
                .map(entry -> entry.getKey().multiply(BigDecimal.valueOf(entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Énumère les références présentes dans le panier sans doublon.
     *
     * @return Un ensemble (Set) des références.
     */
    public Set<String> getReferences() {
        return new HashSet<>(items.keySet());
    }

    /**
     * Énumère les prix unitaires pour une référence donnée.
     *
     * @param reference La référence du produit.
     * @return Un ensemble des prix existants pour cette référence.
     * @throws IllegalArgumentException Si la référence n'existe pas.
     */
    public Set<BigDecimal> getPricesForReference(String reference) {
        validateReference(reference);
        if (!items.containsKey(reference)) {
            throw new IllegalArgumentException("Erreur : Référence inconnue.");
        }
        return new HashSet<>(items.get(reference).keySet());
    }

    /**
     * Retourne la quantité totale d'une référence donnée (tous prix confondus).
     *
     * @param reference La référence du produit.
     * @return La quantité totale.
     * @throws IllegalArgumentException Si la référence n'existe pas.
     */
    public int getQuantity(String reference) {
        validateReference(reference);
        if (!items.containsKey(reference)) {
            throw new IllegalArgumentException("Erreur : Référence inconnue.");
        }
        return items.get(reference).values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Surcharge : Retourne la quantité pour une référence à un prix spécifique.
     *
     * @param reference La référence du produit.
     * @param price Le prix spécifique.
     * @return La quantité pour ce couple référence/prix.
     * @throws IllegalArgumentException Si la référence ou le couple référence/prix n'existe pas.
     */
    public int getQuantity(String reference, BigDecimal price) {
        validateReference(reference);
        validatePrice(price);

        if (!items.containsKey(reference)) {
            throw new IllegalArgumentException("Erreur : Référence inconnue.");
        }

        Integer qty = items.get(reference).get(price);
        if (qty == null) {
            throw new IllegalArgumentException("Erreur : Aucun article de cette référence à ce prix.");
        }

        return qty;
    }

    /**
     * Retourne le montant total pour un couple référence/prix existant.
     *
     * @param reference La référence du produit.
     * @param price Le prix unitaire.
     * @return Le sous-total (Prix * Quantité).
     * @throws IllegalArgumentException Si le couple n'existe pas.
     */
    public BigDecimal getSubTotal(String reference, BigDecimal price) {
        int qty = getQuantity(reference, price); // La validation se fait dans getQuantity
        return price.multiply(BigDecimal.valueOf(qty));
    }

    // --- Méthodes privées de validation ---

    private void validateReference(String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            throw new IllegalArgumentException("Erreur : La référence ne peut pas être vide.");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Erreur : Le prix doit être strictement positif.");
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Erreur : La quantité doit être un entier positif non nul.");
        }
    }
}