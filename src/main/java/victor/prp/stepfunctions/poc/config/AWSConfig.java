package victor.prp.stepfunctions.poc.config;

import java.util.concurrent.TimeUnit;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AWSConfig {
    private final AWSCredentials awsCredentials;
    private final Region region;

    public AWSConfig(SpringAWSCredentials awsCredentials, @Value("${aws.region}") String regionName) {
        this.awsCredentials = awsCredentials;
        this.region = RegionUtils.getRegion(regionName);
    }

    @Bean
    public AWSStepFunctions awsStepFunctions(){
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSocketTimeout((int) TimeUnit.SECONDS.toMillis(5000));

        final AWSStaticCredentialsProvider  awsCred = new AWSStaticCredentialsProvider(awsCredentials);

        return AWSStepFunctionsClientBuilder
            .standard()
            .withClientConfiguration(clientConfiguration)
            .withRegion(Regions.US_EAST_1)
            .withCredentials(awsCred)
            .build();
    }
}
