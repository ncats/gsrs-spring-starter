package gsrs.util;

public class TaskListener {
    private Double p = null;
    private String msg = null;

    public Double getCompletePercentage() {
        return p;
    }

    public String getMessage() {
        return msg;
    }

    public TaskListener progress(double p) {
        this.p = p;
        return this;
    }

    public TaskListener message(String msg) {
        this.msg = msg;
        return this;
    }

    public TaskListener complete() {
        return progress(100);
    }

    public TaskListener start() {
        return progress(0);
    }


}
