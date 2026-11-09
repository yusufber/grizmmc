package com.grizmtech.oneblock.models;

public class WeightedItem<T> {
    private final T item;
    private final int weight;

    public WeightedItem(T item, int weight) {
        this.item = item;
        this.weight = weight;
    }

    public T getItem() {
        return item;
    }

    public int getWeight() {
        return weight;
    }
}
