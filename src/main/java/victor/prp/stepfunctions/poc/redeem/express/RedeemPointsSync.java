package victor.prp.stepfunctions.poc.redeem.express;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"victor.prp.stepfunctions.poc.redeem.express","victor.prp.stepfunctions.poc.config"})
public class RedeemPointsSync {
    public static void main(String[] args) {
        SpringApplication.run(victor.prp.stepfunctions.poc.redeem.express.RedeemPointsSync.class, args);
    }
}
