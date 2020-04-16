package ru.ifmo.rain.moshnikov.arrayset;

import java.util.AbstractList;
import java.util.List;

public class ReversedList<O> extends AbstractList<O> {

    private final List<O> sourceElements;
    private final boolean used;

    public ReversedList(List<O> sourceElements) {
            this.sourceElements = sourceElements;
            this.used = true;
    }

    @Override
    public int size() {
        return sourceElements.size();
    }

    @Override
    public O get(int ind) {
        if (!used) {
            return sourceElements.get(ind);
        } else {
            return sourceElements.get(size() - ind - 1);
        }
    }

}
