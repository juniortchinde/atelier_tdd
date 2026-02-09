import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import tdd.cart.Cart;
import tdd.cart.Product;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CartTest {
    Cart cart = new Cart();
    @Test
    public void testAddProduct() {
        // Action
        cart.addProduct("Stylo", new BigDecimal("1.50"), 2);
        Cart cart = new Cart();
        cart.addProduct("Stylo", new BigDecimal("1.50"), 2);

        // Vérifie que la liste n'est pas vide et contient bien 1 élément
        assertEquals(1, cart.getItems().size(), "Le panier devrait contenir 1 produit");

        // Vérifie les détails du premier produit
        Product addedProduct = cart.getItems().get(0);
        assertEquals("Stylo", addedProduct.getName());
        assertEquals(2, addedProduct.getQuantity());

    }

    private void  addProducts () {
        Cart cart = new Cart();
        cart.addProduct("Stylo", new BigDecimal("1.50"), 2);
    }

    @Test
    public void testAddProductAndTotal() {
        // Initialisation
        Cart cart = new Cart();
        // Action
        cart.addProduct("Stylo", new BigDecimal("1.50"), 2);
        // Vérification (Assertion)
        // 1.50 * 2 = 3.00
        BigDecimal expected = new BigDecimal("3.00");
        assertEquals(expected, cart.getTotalAmount(), "Le total devrait être 3.00");
    }
}