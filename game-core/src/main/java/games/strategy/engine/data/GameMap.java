package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import games.strategy.triplea.delegate.Matches;
import games.strategy.util.IntegerMap;

/**
 * Holds a collection of territories, and the links between them.
 * Utility methods for finding routes and distances between different territories.
 */
public class GameMap extends GameDataComponent implements Iterable<Territory> {
  private static final long serialVersionUID = -4606700588396439283L;
  private final List<Territory> m_territories = new ArrayList<>();
  // note that all entries are unmodifiable
  private final Map<Territory, Set<Territory>> m_connections = new HashMap<>();
  // for fast lookup based on the string name of the territory
  private final Map<String, Territory> m_territoryLookup = new HashMap<>();
  // nil if the map is not grid-based
  // otherwise, m_gridDimensions.length is the number of dimensions,
  // and each element is the size of a dimension
  private int[] m_gridDimensions = null;

  GameMap(final GameData data) {
    super(data);
  }

  public void setGridDimensions(final int... gridDimensions) {
    m_gridDimensions = gridDimensions;
  }

  public Territory getTerritoryFromCoordinates(final int... coordinate) {
    return getTerritoryFromCoordinates(true, coordinate);
  }

  private Territory getTerritoryFromCoordinates(final boolean allowNull, final int... coordinate) {
    if (m_gridDimensions == null) {
      if (allowNull) {
        return null;
      }
      throw new IllegalStateException("No Grid Dimensions");
    }
    if (!isCoordinateValid(coordinate)) {
      if (allowNull) {
        return null;
      }
      final String coordinates = Arrays.stream(coordinate)
          .mapToObj(String::valueOf)
          .collect(Collectors.joining(", "));
      throw new IllegalStateException("No Territory at coordinates: " + coordinates);
    }
    int listIndex = coordinate[0];
    int multiplier = 1;
    for (int i = 1; i < m_gridDimensions.length; i++) {
      multiplier *= m_gridDimensions[i - 1];
      // m_gridDimensions[i];
      listIndex += coordinate[i] * multiplier;
    }
    return m_territories.get(listIndex);
  }

  boolean isCoordinateValid(final int... coordinate) {
    return coordinate.length == m_gridDimensions.length && IntStream.range(0, coordinate.length)
        .noneMatch(i -> coordinate[i] >= m_gridDimensions[i] || coordinate[i] < 0);
  }

  protected void addTerritory(final Territory t1) {
    if (m_territories.contains(t1)) {
      throw new IllegalArgumentException("Map already contains " + t1.getName());
    }
    m_territories.add(t1);
    m_connections.put(t1, Collections.emptySet());
    m_territoryLookup.put(t1.getName(), t1);
  }

  /**
   * Bi-directional. T1 connects to T2, and T2 connects to T1.
   */
  protected void addConnection(final Territory t1, final Territory t2) {
    if (t1.equals(t2)) {
      throw new IllegalArgumentException("Cannot connect a territory to itself: " + t1);
    }
    if (!m_territories.contains(t1) || !m_territories.contains(t2)) {
      throw new IllegalArgumentException("Missing territory definition for either " + t1 + " or " + t2);
    }
    setConnection(t1, t2);
    setConnection(t2, t1);
  }

  private void setConnection(final Territory from, final Territory to) {
    // preserves the unmodifiable nature of the entries
    final Set<Territory> current = m_connections.get(from);
    final Set<Territory> modified = new HashSet<>(current);
    modified.add(to);
    m_connections.put(from, Collections.unmodifiableSet(modified));
  }

  /**
   * Returns the territory with the given name, or null if no territory can be found (case sensitive).
   *
   * @param s name of the searched territory (case sensitive)
   */
  public Territory getTerritory(final String s) {
    return m_territoryLookup.get(s);
  }

  /**
   * Returns all adjacent neighbors of the starting territory.
   * Does NOT include the original/starting territory in the returned Set.
   *
   * @param t referring territory
   */
  public Set<Territory> getNeighbors(final Territory t) {
    // ok since all entries in connections are already unmodifiable
    final Set<Territory> neighbors = m_connections.get(t);
    if (neighbors == null) {
      throw new IllegalArgumentException("No neighbors for:" + t);
    }
    return neighbors;
  }

  /**
   * Returns all adjacent neighbors of the starting territory that match the condition.
   * Does NOT include the original/starting territory in the returned Set.
   *
   * @param t referring territory
   * @param cond condition the neighboring territories have to match
   */
  public Set<Territory> getNeighbors(final Territory t, @Nullable final Predicate<Territory> cond) {
    if (cond == null) {
      return getNeighbors(t);
    }
    return m_connections.getOrDefault(t, Collections.emptySet()).stream()
        .filter(cond)
        .collect(Collectors.toSet());
  }

  /**
   * Returns all neighbors within a certain distance of the starting territory that match the condition.
   * Does NOT include the original/starting territory in the returned Set.
   *
   * @param territory referring territory
   * @param distance maximal distance of the neighboring territories
   */
  public Set<Territory> getNeighbors(final Territory territory, final int distance) {
    if (distance < 0) {
      throw new IllegalArgumentException("Distance must be positive not:" + distance);
    }
    if (distance == 0) {
      return Collections.emptySet();
    }
    final Set<Territory> start = getNeighbors(territory);
    if (distance == 1) {
      return start;
    }
    final Set<Territory> neighbors = getNeighbors(start, new HashSet<>(start), distance - 1);
    neighbors.remove(territory);
    return neighbors;
  }

  /**
   * Returns all neighbors within a certain distance of the starting territory that match the condition.
   * Does NOT include the original/starting territory in the returned Set.
   */
  public Set<Territory> getNeighbors(final Territory territory, final int distance, final Predicate<Territory> cond) {
    if (distance < 0) {
      throw new IllegalArgumentException("Distance must be positive not:" + distance);
    }
    if (distance == 0) {
      return Collections.emptySet();
    }
    final Set<Territory> start = getNeighbors(territory, cond);
    if (distance == 1) {
      return start;
    }
    final Set<Territory> neighbors = getNeighbors(start, new HashSet<>(start), distance - 1, cond);
    neighbors.remove(territory);
    return neighbors;
  }

  /**
   * Returns all neighbors within a certain distance of the starting territory set that match the condition.
   * Does NOT include the original/starting territories in the returned Set, even if they are neighbors of each
   * other.
   */
  public Set<Territory> getNeighbors(final Set<Territory> frontier, final int distance,
      final Predicate<Territory> cond) {
    final Set<Territory> neighbors = getNeighbors(frontier, new HashSet<>(frontier), distance, cond);
    neighbors.removeAll(frontier);
    return neighbors;
  }

  /**
   * Returns all neighbors within a certain distance of the starting territory set.
   * Does NOT include the original/starting territories in the returned Set, even if they are neighbors of each
   * other.
   */
  public Set<Territory> getNeighbors(final Set<Territory> frontier, final int distance) {
    final Set<Territory> neighbors = getNeighbors(frontier, new HashSet<>(frontier), distance);
    neighbors.removeAll(frontier);
    return neighbors;
  }

  private Set<Territory> getNeighbors(final Set<Territory> frontier, final Set<Territory> searched, final int distance,
      @Nullable final Predicate<Territory> cond) {
    if (distance == 0) {
      return searched;
    }
    final Set<Territory> newFrontier = frontier.stream()
        .map(t -> getNeighbors(t, cond))
        .flatMap(Collection::stream)
        .filter(t -> !searched.contains(t))
        .collect(Collectors.toSet());
    searched.addAll(newFrontier);
    return getNeighbors(newFrontier, searched, distance - 1, cond);
  }

  private Set<Territory> getNeighbors(final Set<Territory> frontier, final Set<Territory> searched,
      final int distance) {
    return getNeighbors(frontier, searched, distance, null);
  }

  /**
   * Returns the shortest route between two territories or null if no route exists.
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   */
  public Route getRoute(final Territory t1, final Territory t2) {
    return getRoute(t1, t2, Matches.territoryIsLandOrWater());
  }

  /**
   * Returns the shortest route between two territories so that covered territories match the condition
   * or null if no route exists.
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   * @param cond condition that covered territories of the route must match
   */
  public Route getRoute(final Territory t1, final Territory t2, final Predicate<Territory> cond) {
    if (t1 == t2) {
      return new Route(t1);
    }
    if (getNeighbors(t1, cond).contains(t2)) {
      return new Route(t1, t2);
    }
    return new RouteFinder(this, cond).findRoute(t1, t2);
  }

  /**
   * Returns the shortest land route between two territories or null if no route exists.
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   */
  public Route getLandRoute(final Territory t1, final Territory t2) {
    return getRoute(t1, t2, Matches.territoryIsLand());
  }

  /**
   * Returns the shortest water route between two territories or null if no route exists.
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   */
  public Route getWaterRoute(final Territory t1, final Territory t2) {
    return getRoute(t1, t2, Matches.territoryIsWater());
  }

  public Route getRoute_IgnoreEnd(final Territory t1, final Territory t2, final Predicate<Territory> match) {
    return getRoute(t1, t2, Matches.territoryIs(t2).or(match));
  }

  /**
   * A composite route between two territories
   * Example set of matches: [Friendly Land, score: 1] [Enemy Land, score: 2] [Neutral Land, score = 4]
   * With this example set, an 8 length friendly route is considered equal in score to a 4 length enemy route and a 2
   * length neutral route.
   * This is because the friendly route score is 1/2 of the enemy route score and 1/4 of the neutral route score.
   * Note that you can choose whatever scores you want, and that the matches can mix and match with each other in any
   * way.
   * (Recommended that you use 2,3,4 as scores, unless you will allow routes to be much longer under certain conditions)
   * Returns null if there is no route that exists that matches any of the matches.
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   * @param matches HashMap of territory matches for covered territories
   * @return a composite route between two territories
   */
  public Route getCompositeRoute(final Territory t1, final Territory t2,
      final Map<Predicate<Territory>, Integer> matches) {
    if (t1 == t2) {
      return new Route(t1);
    }
    final Predicate<Territory> allCond = t -> matches.keySet().stream().anyMatch(p -> p.test(t));
    if (getNeighbors(t1, allCond).contains(t2)) {
      return new Route(t1, t2);
    }
    return new CompositeRouteFinder(this, matches).findRoute(t1, t2);
  }

  public Route getCompositeRoute_IgnoreEnd(final Territory t1, final Territory t2,
      final Map<Predicate<Territory>, Integer> matches) {
    matches.put(Matches.territoryIs(t2), 0);
    return getCompositeRoute(t1, t2, matches);
  }

  /**
   * Returns the distance between two territories or -1 if they are not connected.
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   */
  public int getDistance(final Territory t1, final Territory t2) {
    return getDistance(t1, t2, Matches.territoryIsLandOrWater());
  }

  /**
   * Returns the distance between two territories where the covered territories of the route satisfy the condition
   * or -1 if they are not connected.
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   * @param cond condition that covered territories of the route must match
   */
  public int getDistance(final Territory t1, final Territory t2, final Predicate<Territory> cond) {
    if (t1.equals(t2)) {
      return 0;
    }
    return getDistance(0, new HashSet<>(), Collections.singleton(t1), t2, cond);
  }

  /**
   * Guaranteed that frontier doesn't contain target.
   * Territories on the frontier are not target. They represent the extent of paths already searched.
   * Territories in searched have already been on the frontier.
   */
  private int getDistance(final int distance, final Set<Territory> searched, final Set<Territory> frontier,
      final Territory target, @Nullable final Predicate<Territory> cond) {
    // add the frontier to the searched
    searched.addAll(frontier);
    // find the new frontier

    final Set<Territory> newFrontier = frontier.stream()
        .map(m_connections::get)
        .flatMap(Collection::stream)
        .filter(f -> cond == null || cond.test(f))
        .collect(Collectors.toSet());
    if (newFrontier.contains(target)) {
      return distance + 1;
    }
    newFrontier.removeAll(searched);
    if (newFrontier.isEmpty()) {
      return -1;
    }
    return getDistance(distance + 1, searched, newFrontier, target, cond);
  }

  public IntegerMap<Territory> getDistance(final Territory target, final Collection<Territory> territories,
      final Predicate<Territory> condition) {
    final IntegerMap<Territory> distances = new IntegerMap<>();
    if (target == null || territories == null || territories.isEmpty()) {
      return distances;
    }
    for (final Territory t : territories) {
      distances.put(t, getDistance(target, t, condition));
    }
    return distances;
  }

  /**
   * Returns the land distance between two territories or -1 if they are not connected.
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   */
  public int getLandDistance(final Territory t1, final Territory t2) {
    return getDistance(t1, t2, Matches.territoryIsLand());
  }

  /**
   * Returns the water distance between two territories or -1 if they are not connected.
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   */
  public int getWaterDistance(final Territory t1, final Territory t2) {
    return getDistance(t1, t2, Matches.territoryIsWater());
  }

  /**
   * Returns the distance between two territories where the covered territories of the route (except the end) satisfy
   * the condition or -1 if they are not connected. (Distance includes to the end)
   *
   * @param t1 start territory of the route
   * @param t2 end territory of the route
   * @param cond condition that covered territories of the route must match EXCEPT FOR THE END
   */
  public int getDistance_IgnoreEndForCondition(final Territory t1, final Territory t2,
      final Predicate<Territory> cond) {
    return getDistance(t1, t2, Matches.territoryIs(t2).or(cond));
  }

  public List<Territory> getTerritories() {
    return Collections.unmodifiableList(m_territories);
  }

  @Override
  public Iterator<Territory> iterator() {
    return m_territories.iterator();
  }

  public List<Territory> getTerritoriesOwnedBy(final PlayerID player) {
    return m_territories.stream()
        .filter(t -> t.getOwner().equals(player))
        .collect(Collectors.toList());
  }

  /**
   * Indicates whether each territory is connected to the preceding territory.
   *
   * @param route route containing the territories in question
   */
  public boolean isValidRoute(final Route route) {
    Territory previous = null;
    for (final Territory t : route) {
      if (previous != null) {
        if (!getNeighbors(previous).contains(t)) {
          return false;
        }
      }
      previous = t;
    }
    return true;
  }
}
