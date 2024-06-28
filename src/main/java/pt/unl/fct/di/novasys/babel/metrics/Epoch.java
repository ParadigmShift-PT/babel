package pt.unl.fct.di.novasys.babel.metrics;

public class Epoch {
    public enum EpochType {
        MS,
        LOGICAL_CLOCK
    }

    private long epoch;
    private final EpochType epochType;

    public Epoch(EpochType epochType) {
        this.epochType = epochType;
        this.reset();
    }

    public long getEpoch() {
        return epoch;
    }


    public EpochType getType() {
        return epochType;
    }

    public void tick(){
        switch (epochType){
            case MS:
                epoch = System.currentTimeMillis();
                break;
            case LOGICAL_CLOCK:
                epoch++;
                break;
        }
    }

    public void reset(){
        this.epoch = 0;
    }


}
