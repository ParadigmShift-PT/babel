package pt.unl.fct.di.novasys.babel.metrics;

public class LabelValues {
    private final String[] labelValues;

    public LabelValues(String... labelValues) {
        this.labelValues = labelValues;
    }

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
