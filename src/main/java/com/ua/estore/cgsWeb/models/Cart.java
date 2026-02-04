package com.ua.estore.cgsWeb.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "carts")
public class Cart {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field(targetType = FieldType.OBJECT_ID)
    private String userId;

    private List<Item> items = new ArrayList<>();

    public Cart(String userId) {
        this.userId = userId;
        this.items = new ArrayList<>();
    }

    /**
     * Add 1 of a product to the cart. If the item exists, increments quantity.
     */
    public void addProduct(String productId) {
        addProduct(productId, 1);
    }

    /**
     * Add N of a product to the cart. If the item exists, increments quantity.
     */
    public void addProduct(String productId, int quantityToAdd) {
        if (productId == null || productId.isBlank()) return;
        if (quantityToAdd <= 0) return;

        Item item = findItem(productId).orElseGet(() -> {
            Item created = new Item(productId, 0);
            items.add(created);
            return created;
        });

        item.increaseQuantity(quantityToAdd);
    }

    /**
     * Remove 1 of a product. If quantity reaches 0, removes the item.
     */
    public void removeOne(String productId) {
        if (productId == null || productId.isBlank()) return;

        findItem(productId).ifPresent(item -> {
            item.decreaseQuantity(1);
            if (item.getQuantity() <= 0) {
                items.remove(item);
            }
        });
    }

    /**
     * Remove the product entirely from the cart.
     */
    public void removeProduct(String productId) {
        if (productId == null || productId.isBlank()) return;
        items.removeIf(i -> Objects.equals(i.getProductId(), productId));
    }

    public int totalQuantity() {
        return items.stream()
                .filter(Objects::nonNull)
                .map(Item::getQuantity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public Optional<Item> findItem(String productId) {
        if (productId == null || productId.isBlank()) return Optional.empty();
        return items.stream()
                .filter(Objects::nonNull)
                .filter(i -> Objects.equals(i.getProductId(), productId))
                .findFirst();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @Field(targetType = FieldType.OBJECT_ID)
        private String productId;

        private Integer quantity = 1;

        public Item(String productId) {
            this.productId = productId;
            this.quantity = 1;
        }

        public void increaseQuantity() {
            increaseQuantity(1);
        }

        public void increaseQuantity(int delta) {
            if (delta <= 0) return;
            if (quantity == null) quantity = 0;
            quantity += delta;
        }

        public void decreaseQuantity() {
            decreaseQuantity(1);
        }

        public void decreaseQuantity(int delta) {
            if (delta <= 0) return;
            if (quantity == null) quantity = 0;
            quantity -= delta;
        }
    }
}
