import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import tdd.cart.Cart;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests Unitaires Cart")
class CartTest {

    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = new Cart();
    }

    @Nested
    @DisplayName("Initialisation")
    class InitializationTests {

        @Test
        @DisplayName("Le montant total doit être zéro à la création")
        void testNewCartTotalIsZero() {
            assertEquals(BigDecimal.ZERO, cart.getTotalAmount());
        }

        @Test
        @DisplayName("La liste des références doit être vide à la création")
        void testNewCartReferencesIsEmpty() {
            assertTrue(cart.getReferences().isEmpty());
        }
    }

    @Nested
    @DisplayName("Ajout d'articles (Happy Path)")
    class AddItemTests {

        @Test
        @DisplayName("La quantité totale d'une référence augmente après ajout")
        void testAddItemUpdatesQuantity() {
            cart.addItem("Pomme", BigDecimal.ONE, 5);
            assertEquals(5, cart.getQuantity("Pomme"));
        }

        @Test
        @DisplayName("Le montant total du panier augmente après ajout")
        void testAddItemUpdatesTotalAmount() {
            cart.addItem("Pomme", new BigDecimal("2.00"), 3);
            assertEquals(new BigDecimal("6.00"), cart.getTotalAmount());
        }

        @Test
        @DisplayName("Ajouter la même référence au même prix cumule la quantité")
        void testAddSameItemCumulatesQuantity() {
            cart.addItem("Pomme", BigDecimal.ONE, 5);
            cart.addItem("Pomme", BigDecimal.ONE, 3);
            assertEquals(8, cart.getQuantity("Pomme"));
        }

        @Test
        @DisplayName("Ajouter la même référence à un prix différent augmente la quantité globale")
        void testAddSameItemDiffPriceUpdatesTotalQuantity() {
            cart.addItem("Train", new BigDecimal("10"), 1);
            cart.addItem("Train", new BigDecimal("20"), 1);
            assertEquals(2, cart.getQuantity("Train"));
        }

        @Test
        @DisplayName("Ajouter la même référence à un prix différent conserve la distinction des prix")
        void testAddSameItemDiffPriceKeepsPriceStructure() {
            cart.addItem("Train", new BigDecimal("10"), 1);
            cart.addItem("Train", new BigDecimal("20"), 5);
            assertEquals(5, cart.getQuantity("Train", new BigDecimal("20")));
        }
    }

    @Nested
    @DisplayName("Validation des Entrées (Edge Cases)")
    class ValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        @DisplayName("Rejeter les références nulles ou vides")
        void testInvalidReference(String invalidRef) {
            assertThrows(IllegalArgumentException.class, () ->
                    cart.addItem(invalidRef, BigDecimal.ONE, 1));
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "-1", "-100"})
        @DisplayName("Rejeter les prix nuls ou négatifs")
        void testInvalidPrice(String priceStr) {
            BigDecimal invalidPrice = new BigDecimal(priceStr);
            assertThrows(IllegalArgumentException.class, () ->
                    cart.addItem("Ref", invalidPrice, 1));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -50})
        @DisplayName("Rejeter les quantités nulles ou négatives à l'ajout")
        void testInvalidQuantityAdd(int invalidQty) {
            assertThrows(IllegalArgumentException.class, () ->
                    cart.addItem("Ref", BigDecimal.ONE, invalidQty));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("Rejeter les quantités nulles ou négatives au retrait")
        void testInvalidQuantityRemove(int invalidQty) {
            cart.addItem("A", BigDecimal.ONE, 10);
            assertThrows(IllegalArgumentException.class, () ->
                    cart.removeItem("A", invalidQty));
        }
    }

    @Nested
    @DisplayName("Retrait d'articles simple")
    class RemoveItemSimpleTests {

        @BeforeEach
        void initCart() {
            cart.addItem("Livre", new BigDecimal("10.00"), 5);
        }

        @Test
        @DisplayName("Retirer une quantité partielle met à jour la quantité restante")
        void testRemovePartialUpdatesQuantity() {
            cart.removeItem("Livre", 2);
            assertEquals(3, cart.getQuantity("Livre"));
        }

        @Test
        @DisplayName("Retirer une quantité partielle met à jour le montant total")
        void testRemovePartialUpdatesAmount() {
            cart.removeItem("Livre", 2); // Retire 20.00
            assertEquals(new BigDecimal("30.00"), cart.getTotalAmount());
        }

        @Test
        @DisplayName("Retirer la totalité supprime la référence de la liste")
        void testRemoveAllRemovesReference() {
            cart.removeItem("Livre", 5);
            assertFalse(cart.getReferences().contains("Livre"));
        }

        @Test
        @DisplayName("Tenter de retirer une référence inexistante lève une erreur")
        void testRemoveUnknownReference() {
            assertThrows(IllegalArgumentException.class, () ->
                    cart.removeItem("Inconnu", 1));
        }

        @Test
        @DisplayName("Tenter de retirer plus que le stock lève une erreur")
        void testRemoveMoreThanStock() {
            assertThrows(IllegalArgumentException.class, () ->
                    cart.removeItem("Livre", 6));
        }
    }

    @Nested
    @DisplayName("Logique de retrait prioritaire (Le plus cher d'abord)")
    class RemoveMostExpensiveTests {

        // Setup spécifique : 2 articles chers (100€) et 5 articles pas chers (10€)
        @BeforeEach
        void initMixedCart() {
            cart.addItem("Mix", new BigDecimal("10.00"), 5);
            cart.addItem("Mix", new BigDecimal("100.00"), 2);
        }

        @Test
        @DisplayName("Le retrait attaque d'abord le stock le plus cher")
        void testRemoveImpactsHighPriceFirst() {
            cart.removeItem("Mix", 1); // Doit retirer 1 à 100€
            assertEquals(1, cart.getQuantity("Mix", new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("Le stock moins cher reste intact tant que le cher n'est pas épuisé")
        void testRemovePreservesLowPriceInitially() {
            cart.removeItem("Mix", 1); // Doit retirer 1 à 100€
            assertEquals(5, cart.getQuantity("Mix", new BigDecimal("10.00")));
        }

        @Test
        @DisplayName("Le retrait bascule sur le prix inférieur une fois le cher épuisé")
        void testRemoveCascadesToLowerPrice() {
            cart.removeItem("Mix", 3); // 2 chers + 1 pas cher
            assertEquals(4, cart.getQuantity("Mix", new BigDecimal("10.00")));
        }

        @Test
        @DisplayName("Le prix cher disparait complètement s'il est totalement consommé")
        void testHighPriceReferenceDisappears() {
            cart.removeItem("Mix", 2); // Retire tous les chers
            Set<BigDecimal> prices = cart.getPricesForReference("Mix");
            assertFalse(prices.contains(new BigDecimal("100.00")));
        }
    }

    @Nested
    @DisplayName("Calculs et Précision")
    class CalculationTests {

        @Test
        @DisplayName("Calcul précis des nombres flottants (pas d'erreur d'arrondi)")
        void testFloatPrecision() {
            cart.addItem("A", new BigDecimal("0.10"), 3); // 0.30
            cart.addItem("B", new BigDecimal("0.20"), 3); // 0.60
            assertEquals(new BigDecimal("0.90"), cart.getTotalAmount());
        }

        @Test
        @DisplayName("Calcul correct du sous-total pour un couple ref/prix")
        void testSubTotalCalculation() {
            cart.addItem("Chips", new BigDecimal("2.50"), 4);
            assertEquals(new BigDecimal("10.00"), cart.getSubTotal("Chips", new BigDecimal("2.50")));
        }
    }
    @Test
    @DisplayName("L'activation d'un code inconnu retourne false")
    void testActivateUnknownPromoReturnsFalse() {
        assertFalse(cart.activatePromo("UNKNOWN_CODE"));
    }
}