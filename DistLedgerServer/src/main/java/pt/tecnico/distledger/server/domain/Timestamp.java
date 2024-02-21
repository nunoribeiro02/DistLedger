package pt.tecnico.distledger.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.lang.Math;
import java.sql.Time;

public class Timestamp implements Comparable<Timestamp>{

    private List<Integer> ts = new ArrayList<>();

    public Timestamp(int replicas) {
        for (int i = 0; i < replicas; i++) {
            this.addReplica();
        }
    }

    /*copyTS: copy the values of given Timestamp object 
     * to this Timestamp object */
    public void copyTS(Timestamp other) {
        ts = new ArrayList<>();
        int size = other.getSize();
        for (int i = 0; i < size; i++) {
            ts.add(other.getValue(i));
        }
    }

    // Getters and Setters
    public void setTS(List<Integer> ts) {
        int size = ts.size();
        this.ts = new ArrayList<>();
        for (int i=0; i<size; i++) {
            this.ts.add(ts.get(i));
        }
    }

    public List<Integer> getVector() {
        return this.ts;
    }

    public List<Integer> getCopyVector() {
        List<Integer> copy = new ArrayList<>();
        int size = this.ts.size();
        for (int i=0; i<size; i++) {
            copy.add(this.ts.get(i));
        }
        return copy;
    }

    public Integer getValue(Integer id) {
        return ts.get(id);
    }

    public Integer getSize() {
        return ts.size();
    }

    /* addReplica: adds a new replica to the timestamp
     by appending a 0 at the end of timestamp list
     Function called by expand() */
    public void addReplica() {
        this.ts.add(0);
    }

    public void updateValue(Integer id, Integer value) {
        ts.set(id, value);
    }

    /*expand: expands the size of this Timestamp object
     * to be the size given in parameter 
     * Function called by assertSize() */
    public void expand(int nReplicas) {
        int units = nReplicas - ts.size();
        for (int i = 0; i < units; i++) {
            this.addReplica();
        }
    }

    /*merge: merges the values of every index of this object with given Timestamp object  */
    public void merge(Timestamp other) {
        this.assertSize(other);
        int size = this.getSize();
        for (int i = 0; i < size; i++) {
            // Only update the value if the other is greater
            if (other.getValue(i) > this.getValue(i)) {
                this.updateValue(i, other.getValue(i));
            }
        }
    }

    /*greaterOrEqual: compares this object with given Timestamp object,
     * returns false if any values of this object are less than the given one,
     * returns true otherwise (meaning this object is greater or equal than
     * the one given in the parameter)*/
    public boolean greaterOrEqual(Timestamp other) {
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

    /* equals: determines wheter this object is equal to given Timestam object
     * returns false if any value is diferent between them
     * returns true otherwise (meaning they are in fact equal) */
    public boolean equals(Timestamp other) {
        boolean equals = true;
        Timestamp cpy1 = new Timestamp(0);
        Timestamp cpy2 = new Timestamp(0);
        cpy1.copyTS(this);
        cpy2.copyTS(other);
        cpy1.assertSize(cpy2);
        int size = cpy1.getSize();

        // Check every index
        for (int i = 0; i < size; i++) {
            if (cpy1.getValue(i) != cpy2.getValue(i)) {
                equals = false;
            }
        }

        return equals;
    }   

    /*assertSize: checks if this Timestamp object has the same size as 
     * the one given in the parameter
     * If they have diferent sizes, expands the size of the smaller one 
     * to be the same size as the larger timestamp
     * Function called by merge() */
    public void assertSize(Timestamp other) {
        int maxSize = Math.max(this.getSize(), other.getSize());
        Timestamp minSizeReplica = null;
        // comparisons to see if either one has a smaller size
        if (this.getSize() < maxSize) {
            minSizeReplica = this;
        }
        if (other.getSize() < maxSize) {
            minSizeReplica = other;
        }
        // expand if one is smaller than the other
        if (minSizeReplica != null) {
            minSizeReplica.expand(maxSize);
        }
    }


    @Override
    public int compareTo(Timestamp other) {
        if (this.equals(other)) {
            return 0;
        }
        else if (this.greaterOrEqual(other)) {
            return 1;
        }
        else if (this.greaterOrEqual(other)) {
            return -1;
        }
        int minSize = Math.min(this.getSize(),other.getSize());
		for (int i=0; i<minSize; i++) {
			if (this.getValue(i)>other.getValue(i)) {
				return 1;
			}
			else if (other.getValue(i)>this.getValue(i)) {
				return -1;
			}
		}
		if (this.getSize()>minSize) {return 1;}
		if (other.getSize()>minSize) {return -1;}

		return 0;
    }

    @Override
    public String toString() {
        return ts.toString();
    }

}