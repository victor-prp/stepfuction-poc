package victor.prp.stepfunctions.poc.redeem.standard;

public class ExecInput {

    private String routingKey;

    public ExecInput() {
    }

    public ExecInput(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public ExecInput with(String routingKey){
        this.setRoutingKey(routingKey);
        return this;
    }
}
