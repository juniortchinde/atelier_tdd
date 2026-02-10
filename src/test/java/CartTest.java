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

    @Test
    @DisplayName("L'activation d'un code enregistré retourne true")
    void testActivateRegisteredPromo() {
        cart.registerPromo("NOEL10", "Pomme", 10);
        assertTrue(cart.activatePromo("NOEL10"));
    }

    @Test
    @DisplayName("Erreur si pourcentage invalide")
    void testRegisterInvalidPercentage() {
        assertThrows(IllegalArgumentException.class, () ->
                cart.registerPromo("BAD", "Ref", 105));
    }
   // calcul de la réduction
    @Test
    @DisplayName("Le code promo réduit le montant total")
    void testPromoAppliedToTotal() {
        // Arrange
        cart.registerPromo("POMME10", "Pomme", 10); // 10% de réduction
        cart.addItem("Pomme", new BigDecimal("100.00"), 1);
        // Act
        cart.activatePromo("POMME10");
        // Assert : 100 - 10% = 90
        assertEquals(new BigDecimal("90.00"), cart.getTotalAmount());
    }

    @Test
    @DisplayName("Impossible d'activer deux codes pour la même référence")
    void testCannotActivateTwoCodesSameRef() {
        cart.registerPromo("CODE1", "Pomme", 10);
        cart.registerPromo("CODE2", "Pomme", 20);

        cart.activatePromo("CODE1");

        boolean result = cart.activatePromo("CODE2");

        assertFalse(result, "Le deuxième code pour la même ref doit être refusé");
    }

    @Test
    @DisplayName("Possible d'activer deux codes pour références différentes")
    void testActivateTwoCodesDiffRef() {
        cart.registerPromo("CODE_P", "Pomme", 10);
        cart.registerPromo("CODE_O", "Orange", 10);

        assertTrue(cart.activatePromo("CODE_P"));
        assertTrue(cart.activatePromo("CODE_O"));
    }

    @Test
    @DisplayName("La promo ne s'applique pas si le seuil minimum n'est pas atteint")
    void testPromoThresholdNotMet() {
        // Promo de 10% sur Pomme si montant > 100€
        cart.registerPromo("BIG10", "Pomme", 10, new BigDecimal("100.00"));
        cart.addItem("Pomme", new BigDecimal("50.00"), 1);

        cart.activatePromo("BIG10");

        // Total devrait rester 50 (pas de réduction)
        assertEquals(new BigDecimal("50.00"), cart.getTotalAmount());
    }

    @Test
    @DisplayName("La promo s'applique si le seuil est dépassé")
    void testPromoThresholdMet() {
        cart.registerPromo("BIG10", "Pomme", 10, new BigDecimal("100.00"));
        cart.addItem("Pomme", new BigDecimal("150.00"), 1); // > 100

        cart.activatePromo("BIG10");

        // 150 - 10% (15) = 135
        assertEquals(new BigDecimal("135.00"), cart.getTotalAmount());
    }

    @Test
    @DisplayName("Promo 'N achetés 1 offert' s'applique si quantité suffisante (N+1)")
    void testBuyNGet1Free_SufficientQty() {
        // "2 achetés 1 offert" => Il faut 3 articles pour déclencher.
        // Prix : 10, 10, 10. Total sans promo = 30. Total avec promo = 20.
        cart.registerBuyNGetOneFree("3POUR2", "Cahier", 2);
        cart.addItem("Cahier", new BigDecimal("10.00"), 3);

        cart.activatePromo("3POUR2");

        assertEquals(new BigDecimal("20.00"), cart.getTotalAmount());
    }

    @Test
    @DisplayName("Promo 'N achetés 1 offert' ne fait rien si quantité insuffisante")
    void testBuyNGet1Free_InsufficientQty() {
        // "2 achetés 1 offert". On en a que 2. On paie les 2.
        cart.registerBuyNGetOneFree("3POUR2", "Cahier", 2);
        cart.addItem("Cahier", new BigDecimal("10.00"), 2);

        cart.activatePromo("3POUR2");

        assertEquals(new BigDecimal("20.00"), cart.getTotalAmount());
    }

    @Test
    @DisplayName("L'article offert est strictement le moins cher du lot")
    void testBuyNGet1Free_CheapestIsFree() {
        // "2 achetés 1 offert"
        cart.registerBuyNGetOneFree("PROMO_MIX", "Mix", 2);

        // Ajout : 1 cher (100), 1 moyen (50), 1 pas cher (10)
        cart.addItem("Mix", new BigDecimal("100.00"), 1);
        cart.addItem("Mix", new BigDecimal("50.00"), 1);
        cart.addItem("Mix", new BigDecimal("10.00"), 1);

        cart.activatePromo("PROMO_MIX");

        assertEquals(new BigDecimal("150.00"), cart.getTotalAmount());
    }

    @Test
    @DisplayName("Gestion des grandes quantités (plusieurs offerts)")
    void testBuyNGet1Free_MultipleFreeItems() {
        // "1 acheté 1 offert" (N=1). Pack de 2.
        cart.registerBuyNGetOneFree("1POUR1", "Bonbon", 1);
        cart.addItem("Bonbon", new BigDecimal("10.00"), 2);
        cart.addItem("Bonbon", new BigDecimal("20.00"), 2);

        cart.activatePromo("1POUR1");

        assertEquals(new BigDecimal("40.00"), cart.getTotalAmount());
    }

    @Test
    @DisplayName("Cumul : BuyNGet1 s'applique d'abord, puis le pourcentage sur le reste")
    void testCumulativePromos() {
        // Promo 1 : 2 achetés 1 offert (N=2)
        cart.registerBuyNGetOneFree("B2G1", "Livre", 2);
        // Promo 2 : 10% de réduction
        cart.registerPromo("NOEL10", "Livre", 10);
        cart.addItem("Livre", new BigDecimal("100.00"), 3);

        assertTrue(cart.activatePromo("B2G1"));
        assertTrue(cart.activatePromo("NOEL10")); // Doit retourner true maintenant !
        assertEquals(new BigDecimal("180.00"), cart.getTotalAmount());
    }

}