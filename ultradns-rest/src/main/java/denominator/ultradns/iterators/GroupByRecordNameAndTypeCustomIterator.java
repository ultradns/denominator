package denominator.ultradns.iterators;

import java.util.Iterator;
import java.util.Map;

import denominator.ResourceTypeToValue;
import denominator.common.PeekingIterator;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.ultradns.model.Record;

import static denominator.common.Util.peekingIterator;
import static denominator.common.Util.toMap;

public class GroupByRecordNameAndTypeCustomIterator implements Iterator<ResourceRecordSet<?>> {

  private final PeekingIterator<Record> peekingIterator;

  public GroupByRecordNameAndTypeCustomIterator(Iterator<Record> sortedIterator) {
    this.peekingIterator = peekingIterator(sortedIterator);
  }

  static boolean fqdnAndTypeEquals(Record actual, Record expected) {
    return actual.getName().equals(expected.getName()) && actual.getTypeCode() == expected.getTypeCode();
  }

  @Override
  public boolean hasNext() {
    return peekingIterator.hasNext();
  }

  @Override
  public ResourceRecordSet<?> next() {
    Record record = peekingIterator.next();
    String type = ResourceTypeToValue.lookup(record.getTypeCode());
    Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
        .name(record.getName())
        .type(type)
        .ttl(record.getTtl());

    builder.add(toMap(type, record.getRdata()));

    while (hasNext()) {
      Record next = peekingIterator.peek();
      if (fqdnAndTypeEquals(next, record)) {
        peekingIterator.next();
        builder.add(toMap(type, next.getRdata()));
      } else {
        break;
      }
    }
    return builder.build();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
