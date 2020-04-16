package ru.ifmo.rain.moshnikov.arrayset;

import java.util.*;

public class ArraySet<O> extends AbstractSet<O> implements NavigableSet<O> {

    private final List<? extends O> sourceElements;
    private final Comparator<? super O> comparator;


    public ArraySet(Collection<?extends O> sourceElements) {
        if (isCollectionSorted(sourceElements)) {
            this.sourceElements = List.copyOf(sourceElements);
        } else {
            this.sourceElements = List.copyOf(new TreeSet<>(sourceElements));
        }
        this.comparator = null;
    }

    public ArraySet(Comparator<? super O> comparator) {
        this(Collections.emptyList(), comparator);
    }

    public ArraySet(Collection<? extends O> sourceElements, Comparator<? super O> comparator) {
        this.comparator = comparator;
        if (!isCollectionSorted(sourceElements)) {
            SortedSet<O> sortedSet = new TreeSet<>(comparator);
            sortedSet.addAll(sourceElements);
            this.sourceElements = List.copyOf(sortedSet);
        } else {
            this.sourceElements = List.copyOf(sourceElements);
        }
    }

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    private ArraySet(List<? extends O> sourceElements, Comparator<? super O> comparator) {
        this.sourceElements = sourceElements;
        this.comparator = comparator;
    }

    private boolean isCollectionSorted(Collection<? extends O> collection) {
        Iterator<? extends O> pointer = collection.iterator();
        O next = null;
        O prev;
        while (pointer.hasNext()) {
            if (next != null) {
                prev = next;
            } else {
                prev = pointer.next();
            }
            if (pointer.hasNext()) {
                next = pointer.next();
                if (compare(prev, next) >= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private O elementOnPlace(int ind) {
        if (ind >= sourceElements.size()) {
            return null;
        } else if (ind < 0) {
            return null;
        }
        return sourceElements.get(ind);
    }

    private int getInd(O element) {
        return Collections.binarySearch(sourceElements, element, comparator);
    }

    //for find lower, floor  not in linear time
    private int getPrevInd(O element, boolean include) {
        if (include) {
            if (getInd(element) < 0) {
                return -(getInd(element) + 1) - 1;
            } else {
                return getInd(element);
            }
        } else {
            if (getInd(element) < 0) {
                return -(getInd(element) + 1) - 1;
            } else {
                return getInd(element) - 1;
            }
        }
    }

    //for find higher, ceiling not in linear time
    private int getNextIndex(O element, boolean include) {
        if (include) {
            if (getInd(element) < 0) {
                return -(getInd(element) + 1);
            } else {
                return getInd(element);
            }
        } else {
            if (getInd(element) < 0) {
                return -(getInd(element) + 1);
            } else {
                return getInd(element) + 1;
            }
        }
    }

    @Override
    public O lower(O element) {
        return elementOnPlace(getPrevInd(element, false));
    }

    //like <=
    @Override
    public O floor(O element) {
        return elementOnPlace(getPrevInd(element, true));
    }

    //like >=
    @Override
    public O ceiling(O element) {
        return elementOnPlace(getNextIndex(element, true));
    }

    @Override
    public O higher(O element) {
        return elementOnPlace(getNextIndex(element, false));
    }

    //return first element
    @Override
    public O pollFirst() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    //return last element
    @Override
    public O pollLast() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<O> iterator() {
        return (Iterator<O>) sourceElements.iterator();
    }

    @Override
    public boolean contains(Object el) throws NoSuchElementException {
        return Collections.binarySearch(sourceElements, el, (Comparator) comparator) >= 0;
    }

    //special set with reverse order of elements
    @Override
    public NavigableSet<O> descendingSet() {
        return new ArraySet<>(new ReversedList<>(sourceElements), Collections.reverseOrder(comparator));
    }

    //another descending iterator
    @Override
    public Iterator<O> descendingIterator() {
        return descendingSet().iterator();
    }

    //to stack piece of sequence
    @Override
    public NavigableSet<O> subSet(O from, boolean fromInclusive, O to, boolean toInclusive) throws IllegalArgumentException {
        if (compare(from, to) > 0) {
            throw new IllegalArgumentException();
        }
        int fromInt = getNextIndex(from, fromInclusive);
        int toInt = getPrevInd(to, toInclusive);
        if (fromInt <= toInt) {
            return new ArraySet<>(sourceElements.subList(fromInt, toInt + 1), comparator);
        }
        return new ArraySet<>(Collections.emptyList(), comparator);
    }

    //return the start piece of list to element "to"
    @Override
    public NavigableSet<O> headSet(O to, boolean include) {
        return new ArraySet<>(sourceElements.subList(0, getPrevInd(to, include) + 1), comparator);
    }

    //return the last piece of list from element "from"
    @Override
    public NavigableSet<O> tailSet(O from, boolean include) {
            return new ArraySet<>(sourceElements.subList(getNextIndex(from, include), sourceElements.size()), comparator);
    }

    @Override
    public Comparator<? super O> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<O> subSet(O fromElement, O toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<O> headSet(O toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<O> tailSet(O fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public O first() {
        if (sourceElements.isEmpty()) {
            throw new NoSuchElementException();
        }
        return elementOnPlace(0);
    }

    @Override
    public O last() throws NoSuchElementException {
        if (sourceElements.isEmpty()) {
            throw new NoSuchElementException();
        }
        return elementOnPlace(size() - 1);
    }

    @Override
    public int size() {
        return sourceElements.size();
    }

    private int compare(O elementFirst, O elementSecond) {
        if (comparator == null) {
            return ((Comparable<? super O>) elementFirst).compareTo(elementSecond);
        } else {
            return comparator.compare(elementFirst, elementSecond);
        }
    }

}
