package victor.prp.stepfunctions.poc.redeem.standard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"victor.prp.stepfunctions.poc.redeem.standard","victor.prp.stepfunctions.poc.config"})
public class RedeemPointsAsync {
    public static void main(String[] args) {
        SpringApplication.run(RedeemPointsAsync.class, args);
    }

}
