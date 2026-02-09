package tdd.cart;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public abstract class Product {
    private final Map<String, TreeMap<BigDecimal, Integer>> product ;

    protected Product(Map<String, TreeMap<BigDecimal, Integer>> items, Map<String, TreeMap<BigDecimal, Integer>> product) {
        this.product = product;
    }
}
