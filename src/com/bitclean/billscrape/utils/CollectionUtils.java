package com.bitclean.billscrape.utils;

import java.util.Map;
import java.util.List;

import com.google.common.collect.Maps;
import com.google.common.collect.Iterables;

/**
 * Created by IntelliJ IDEA.
 * User: david
 * Date: 16-Jan-2010
 * Time: 17:42:37
 * To change this template use File | Settings | File Templates.
 */
public class CollectionUtils {
  /**
     * Creates a map by taking the items from "in", as the key and value alternatly.
   * @param in The data for the map.
   * @param <T> The type of the data, the resulting map will be <T,T>
   * @return the map with keys and values.
   */
  public static <T> Map<T, T> mapFromIterable(final Iterable<T> in) {
    Map<T, T> properties = Maps.newHashMap();
    for (List<T> pair : Iterables.partition(in, 2)) {
      properties.put(pair.get(0), pair.get(1));
    }
    return properties;
  }
}
