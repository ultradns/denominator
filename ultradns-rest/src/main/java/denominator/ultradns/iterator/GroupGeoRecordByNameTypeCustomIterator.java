package denominator.ultradns.iterator;

import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.ArrayList;

import javax.inject.Inject;

import denominator.common.PeekingIterator;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.model.profile.Geo;
import denominator.ultradns.exception.UltraDNSRestException;
import denominator.ultradns.service.integration.UltraDNSRest;
import denominator.ultradns.model.DirectionalRecord;
import denominator.ultradns.model.DirectionalGroup;
import denominator.ultradns.model.Region;
import denominator.ultradns.util.RRSetUtil;

import static denominator.common.Util.peekingIterator;
import static denominator.common.Util.toMap;
import static denominator.ultradns.exception.UltraDNSRestException.processUltraDnsException;

/**
 * Generally, this iterator will produce ResourceRecordSet for only a single record type.
 * However, there are special cases where this can produce multiple.
 */
public final class GroupGeoRecordByNameTypeCustomIterator implements Iterator<ResourceRecordSet<?>> {

  private final Map<String, Geo> cache = new LinkedHashMap<String, Geo>();
  private final PeekingIterator<DirectionalRecord> peekingIterator;
  private final String zoneName;
  private final UltraDNSRest api;
  private final Map<Region, Collection<Region>> regions;

  /**
   * Creates a new GroupGeoRecordByNameTypeCustomIterator with specified directional record
   * and zone name.
   */
  private GroupGeoRecordByNameTypeCustomIterator(UltraDNSRest api,
                                                 Iterator<DirectionalRecord> sortedIterator,
                                                 String zoneName,
                                                 Map<Region, Collection<Region>> regions) {
    this.api = api;
    this.peekingIterator = peekingIterator(sortedIterator);
    this.zoneName = zoneName;
    this.regions = regions;
  }

  /**
   * Returns true if the owner name,geo group name and ttl of actual and expected records are matching.
   */
  static boolean typeTTLAndGeoGroupEquals(DirectionalRecord actual, DirectionalRecord expected) {
    return actual.getType().equals(expected.getType())
            && actual.getTtl() == expected.getTtl()
            && actual.getGeoGroupName().equals(expected.getGeoGroupName())
            && actual.getName().equals(expected.getName());
  }

  /**
   * Returns true if the iteration has more elements,
   * skips no response records as they aren't portable.
   */
  @Override
  public boolean hasNext() {
    if (!peekingIterator.hasNext()) {
      return false;
    }
    DirectionalRecord record;
    while (true) {
      if (peekingIterator.hasNext()) {
        record = peekingIterator.peek();
        if (record.isNoResponseRecord()) {
          peekingIterator.next();
        } else {
          return true;
        }
      } else {
        return false;
      }
    }
  }

  /**
   * Returns the next resource record set in the iteration having the unique combination
   * of owner name and type and geo group name.
   */
  @Override
  public ResourceRecordSet<?> next() {
    DirectionalRecord record = peekingIterator.next();

    Builder<Map<String, Object>>
        builder =
        ResourceRecordSet.builder().name(record.getName()).type(record.getType())
            .qualifier(record.getGeoGroupName()).ttl(record.getTtl());

    builder.add(toMap(record.getType(), record.getRdata()));

    final String key = record.getName() + "_" + record.getType() + "_" + record.getGeoGroupName();
    if (!cache.containsKey(key)) {
      Geo profile = Geo.create(getDirectionalDNSGroupByName(zoneName, record.getName(),
              RRSetUtil.directionalRecordType(record.getType()), record.getGeoGroupName()).getRegionToTerritories());
      cache.put(key, profile);
    }

    builder.geo(cache.get(key));
    while (hasNext()) {
      DirectionalRecord next = peekingIterator.peek();
      if (typeTTLAndGeoGroupEquals(next, record)) {
        peekingIterator.next();
        builder.add(toMap(record.getType(), next.getRdata()));
      } else {
        break;
      }
    }
    return builder.build();
  }

  /**
   * Returns UnsupportedOperationException.
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns DirectionalGroup which contains the group name and it's territories
   * for a particular Owner name, record type and geo group.
   * @param zone name of the zone
   * @param hostName owner name or directional pool name
   * @param rrType resource record record type
   * @param groupName name of the GEO group
   * @return DirectionalGroup
   */
  public DirectionalGroup getDirectionalDNSGroupByName(String zone, String hostName, int rrType, String groupName) {

    TreeSet<String> codes = new TreeSet<String>();
    try {
      codes = RRSetUtil.getDirectionalGroupDetails(
              api.getDirectionalDNSRecordsForHost(zone, hostName, rrType).rrSets(),
              groupName);
    } catch (UltraDNSRestException e) {
      processUltraDnsException(e, UltraDNSRestException.DATA_NOT_FOUND);
    }

    Map<String, Collection<String>> regionToTerritories = new TreeMap<String, Collection<String>>();

    if (codes != null && !codes.isEmpty()) {
      regionToTerritories = getRegionToTerritories(codes);
    }

    DirectionalGroup directionalGroup = new DirectionalGroup();
    directionalGroup.setName(groupName);
    directionalGroup.setRegionToTerritories(regionToTerritories);

    return  directionalGroup;
  }

  /**
   * Coverts set of GEO codes to a map contains key as the region name
   * & value as all it's child regions/territories names.
   * @param codes GEO codes
   * @return Map A map which contains key as the region name
   */
  private Map<String, Collection<String>> getRegionToTerritories(TreeSet<String> codes) {

    Map<String, Collection<String>> regionToTerritories = new TreeMap<String, Collection<String>>();

    Iterator<String> iterator = codes.iterator();
    while (iterator.hasNext()) {
      String code = iterator.next();
      Collection<String> list = new ArrayList<String>();
      boolean codeFound = false;

      if ("A1".equals(code) || "A2".equals(code) || "A3".equals(code)) {
        for (Region region : regions.keySet()) {
          if (code.equals(region.getCode())) {
            list.add(region.getName());
            if ("A1".equals(code) || "A2".equals(code)) {
              regionToTerritories.put(region.getName() + " (" + code + ")", list);
            } else {
              regionToTerritories.put(region.getName(), list);
            }
          }
        }
      } else {
        for (Map.Entry<Region, Collection<Region>> entry : regions.entrySet()) {
          for (Region region : entry.getValue()) {
            if (code.equals(region.getEffectiveCodeForGeo())) {
              if (regionToTerritories.keySet().contains(entry.getKey().getName())) {
                list = regionToTerritories.get(entry.getKey().getName());
              }
              list.add(region.getName());
              regionToTerritories.put(entry.getKey().getName(), list);
              codeFound = true;
              break;
            }
          }
          if (codeFound) {
            break;
          }
        }
      }
    }
    return regionToTerritories;
  }

  public static final class Factory {

    private final UltraDNSRest api;

    @Inject
    Factory(UltraDNSRest api) {
      this.api = api;
    }

    /**
     * Construct a custom iterator from directional record iterator and zone name.
     * @param sortedIterator only contains records with the same.
     */
    public Iterator<ResourceRecordSet<?>> create(Iterator<DirectionalRecord> sortedIterator, String name,
                                                 Map<Region, Collection<Region>> regions) {
      return new GroupGeoRecordByNameTypeCustomIterator(api, sortedIterator, name, regions);
    }
  }
}
