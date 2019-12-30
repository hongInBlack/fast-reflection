package com.hong.fastreflection.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @author huagnzhihong
 * @version 1.0
 * @description
 * @date 2019/12/30
 */
public class ListMap<K, E> {

    private final HashMap<K, List<E>> map;

    public ListMap() {
        map = new HashMap<>();
    }

    public void put(K key, E value) {
        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            ArrayList<E> es = new ArrayList<>();
            es.add(value);
            map.put(key, es);
        }
    }

    public void putElement(K key, E value) {
        put(key, value);
    }

    public List<E> get(Object key) {
        return map.get(key);
    }

    public List<E> remove(Object key) {
        return map.remove(key);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }
}
