package pt.tecnico.distledger.userclient.domain;

import java.util.ArrayList;
import java.util.List;
import java.lang.Math;
import java.sql.Time;

public class Timestamp {

    private List<Integer> ts = new ArrayList<>();

    public Timestamp(int replicas) {
        for (int i = 0; i < replicas; i++) {
            this.addReplica();
        }
    }

    public void copyTS(Timestamp other) {
        ts = new ArrayList<>();
        int size = other.getSize();
        for (int i = 0; i < size; i++) {
            ts.add(other.getValue(i));
        }
    }

    public void setTS(List<Integer> ts) {
        int size = ts.size();
        this.ts = new ArrayList<>();
        for (int i=0; i<size; i++) {
            this.ts.add(ts.get(i));
        }
    }

    public List<Integer> getTS() {
        return this.ts;
    }

    public void addReplica() {
        this.ts.add(0);
    }

    public void updateValue(Integer id, Integer value) {
        ts.set(id, value);
    }

    public Integer getValue(Integer id) {
        return ts.get(id);
    }

    public Integer getSize() {
        return ts.size();
    }

    public void expand(int nReplicas) {
        int units = nReplicas - ts.size();
        for (int i = 0; i < units; i++) {
            this.addReplica();
        }
    }

    public void merge(Timestamp other) {
        this.assertSize(other);
        int size = this.getSize();
        for (int i = 0; i < size; i++) {
            if (other.getValue(i) > this.getValue(i)) {
                this.updateValue(i, other.getValue(i));
            }
        }
    }

    public boolean greaterThan(Timestamp other) {
        boolean greater = true;
        Timestamp cpy1 = new Timestamp(0);
        Timestamp cpy2 = new Timestamp(0);
        cpy1.copyTS(this);
        cpy2.copyTS(other);
        cpy1.assertSize(cpy2);
        int size = cpy1.getSize();

        for (int i = 0; i < size; i++) {
            if (cpy1.getValue(i) < cpy2.getValue(i)) {
                greater = false;
            }
        }

        return greater;
    }

    public void assertSize(Timestamp other) {
        int maxSize = Math.max(this.getSize(), other.getSize());
        Timestamp minSizeReplica = null;
        if (this.getSize() < maxSize) {
            minSizeReplica = this;
        }
        if (other.getSize() < maxSize) {
            minSizeReplica = other;
        }
        if (minSizeReplica != null) {
            minSizeReplica.expand(maxSize);
        }
    }

    @Override
    public String toString() {
        return ts.toString();
    }

}