import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import tdd.cart.Product;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests Unitaires : Product (Logique Métier)")
class ProductTest {

    private Product product;
    private final String REF = "RefTest";

    @BeforeEach
    void setUp() {
        product = new Product(REF);
    }

    @Nested
    @DisplayName("Constructeur & Validation")
    class ConstructorTests {
        @Test
        @DisplayName("La référence doit être correctement assignée")
        void testReferenceAssignment() {
            assertEquals(REF, product.getReference());
        }

        @Test
        @DisplayName("Le stock initial doit être zéro")
        void testInitialStockIsZero() {
            assertEquals(0, product.getTotalQuantity());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Le constructeur doit rejeter une référence invalide")
        void testConstructorValidation(String invalidRef) {
            assertThrows(IllegalArgumentException.class, () -> new Product(invalidRef));
        }
    }

    @Nested
    @DisplayName("Gestion des Stocks (Ajout)")
    class StockAdditionTests {
        @Test
        @DisplayName("Ajouter du stock met à jour la quantité totale")
        void testAddStockUpdatesTotal() {
            product.addStock(BigDecimal.TEN, 5);
            assertEquals(5, product.getTotalQuantity());
        }

        @Test
        @DisplayName("Ajouter deux fois le même prix cumule la quantité")
        void testAddStockCumulatesSamePrice() {
            product.addStock(BigDecimal.TEN, 5);
            product.addStock(BigDecimal.TEN, 3);
            assertEquals(8, product.getQuantityAtPrice(BigDecimal.TEN));
        }

        @Test
        @DisplayName("Ajouter des prix différents sépare les lots")
        void testAddStockDifferentPrices() {
            product.addStock(BigDecimal.TEN, 1);
            product.addStock(BigDecimal.ONE, 1);
            assertEquals(2, product.getPrices().size());
        }

        @Test
        @DisplayName("Refuser un prix négatif")
        void testAddStockInvalidPrice() {
            assertThrows(IllegalArgumentException.class, () ->
                    product.addStock(new BigDecimal("-1"), 1));
        }

        @Test
        @DisplayName("Refuser une quantité zéro ou négative")
        void testAddStockInvalidQuantity() {
            assertThrows(IllegalArgumentException.class, () ->
                    product.addStock(BigDecimal.TEN, 0));
        }
    }

    @Nested
    @DisplayName("Logique de Retrait (Algorithme FIFO Prix)")
    class StockRemovalTests {

        // Setup : 10 chers (100€) et 10 pas chers (10€)
        @BeforeEach
        void setupMixedStock() {
            product.addStock(new BigDecimal("10.00"), 10);
            product.addStock(new BigDecimal("100.00"), 10);
        }

        @Test
        @DisplayName("Le retrait partiel entame d'abord le prix le plus élevé")
        void testRemoveTakesFromHighestPrice() {
            product.removeStock(2); // Retire 2 chers
            assertEquals(8, product.getQuantityAtPrice(new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("Le retrait partiel ne touche pas au prix le plus bas")
        void testRemoveDoesNotTouchLowPrice() {
            product.removeStock(2);
            assertEquals(10, product.getQuantityAtPrice(new BigDecimal("10.00")));
        }

        @Test
        @DisplayName("Épuiser le stock cher bascule sur le stock moins cher")
        void testRemoveCascadesToLowerPrice() {
            product.removeStock(15); // 10 chers + 5 pas chers
            assertEquals(5, product.getQuantityAtPrice(new BigDecimal("10.00")));
        }

        @Test
        @DisplayName("Le lot de prix disparait complètement si épuisé")
        void testPriceBatchDisappearsIfEmpty() {
            product.removeStock(10); // Tout le stock cher
            assertFalse(product.getPrices().contains(new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("Erreur si on tente de retirer plus que le total")
        void testRemoveMoreThanTotal() {
            assertThrows(IllegalArgumentException.class, () ->
                    product.removeStock(21));
        }
    }

    @Nested
    @DisplayName("Calculs Financiers")
    class CalculationTests {
        @Test
        @DisplayName("Calcul correct de la valeur totale (Mélange de prix)")
        void testTotalValue() {
            product.addStock(new BigDecimal("10"), 2); // 20
            product.addStock(new BigDecimal("5"), 4);  // 20
            assertEquals(new BigDecimal("40"), product.getTotalValue());
        }

        @Test
        @DisplayName("Valeur totale est zéro si stock vide")
        void testTotalValueEmpty() {
            assertEquals(BigDecimal.ZERO, product.getTotalValue());
        }

    }
}