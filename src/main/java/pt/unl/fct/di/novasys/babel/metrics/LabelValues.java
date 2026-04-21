package pt.unl.fct.di.novasys.babel.metrics;

/**
 * An immutable key for a specific combination of label values used to identify a labeled metric instance.
 * Two {@code LabelValues} objects are equal when all their value strings match element-wise.
 */
public class LabelValues {
    private final String[] labelValues;

    /**
     * Creates a {@code LabelValues} instance with the given ordered label values.
     *
     * @param labelValues the label values, in the same order as the corresponding label names
     */
    public LabelValues(String... labelValues) {
        this.labelValues = labelValues;
    }

    /**
     * Returns the array of label values held by this instance.
     *
     * @return the label values array
     */
    public String[] getLabelValues() {
        return labelValues;
    }

    @Override
    public int hashCode() {
        StringBuilder sb = new StringBuilder();
        for (String s : labelValues) {
            sb.append(s);
        }
        return sb.toString().hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;

        if(!(obj instanceof LabelValues))
            return false;

        LabelValues l1 = (LabelValues) obj;

        for (int i = 0; i < this.labelValues.length; i++) {
            if(!l1.labelValues[i].equals(this.labelValues[i]))
                return false;
        }

        return true;
    }


}
