package victor.prp.stepfunctions.poc.redeem.standard;

public class RedeemPointsInput extends ExecInput{


    public RedeemPointsInput(Integer pointsAmount, Integer discount) {
        this.pointsAmount = pointsAmount;
        this.discount = discount;
    }

    private Integer pointsAmount;

    private Integer discount;

    public RedeemPointsInput() {
    }

    public Integer getPointsAmount() {
        return pointsAmount;
    }

    public void setPointsAmount(Integer pointsAmount) {
        this.pointsAmount = pointsAmount;
    }

    public Integer getDiscount() {
        return discount;
    }

    public void setDiscount(Integer discount) {
        this.discount = discount;
    }
}
