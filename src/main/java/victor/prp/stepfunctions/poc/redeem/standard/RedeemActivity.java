package victor.prp.stepfunctions.poc.redeem.standard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

@Controller
public class RedeemActivity implements Activity {
    private static final Logger log = LoggerFactory.getLogger(RedeemActivity.class);

    @Override
    public String arn() {
        return "arn:aws:states:us-east-1:201136940110:activity:redeem";
    }

    @Override
    public void execute() {
        log.info("Executing Activity: redeem");
    }
}
