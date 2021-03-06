package games.strategy.triplea.ui;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;


/**
 * An extension of Properties which maintains properties file order.
 */
public class OrderedProperties extends Properties {

  private static final long serialVersionUID = 7458143419297149318L;

  private final Set<Object> keys = new LinkedHashSet<>();

  @Override
  public Enumeration<?> propertyNames() {
    return Collections.enumeration(keys);
  }

  @Override
  public synchronized Enumeration<Object> elements() {
    return Collections.enumeration(keys);
  }

  @Override
  public Enumeration<Object> keys() {
    return Collections.enumeration(keys);
  }

  @Override
  public Set<Object> keySet() {
    return keys;
  }

  @Override
  public synchronized Object put(final Object key, final Object value) {
    keys.add(key);
    return super.put(key, value);
  }

  @Override
  public synchronized Object remove(final Object key) {
    keys.remove(key);
    return super.remove(key);
  }

  @Override
  public synchronized void clear() {
    keys.clear();
    super.clear();
  }

}
