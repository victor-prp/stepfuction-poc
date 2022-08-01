package victor.prp.stepfunctions.poc.redeem.express;


import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import com.amazonaws.services.stepfunctions.model.StartSyncExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartSyncExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Controller
public class RestController {
    private static final Logger log = LoggerFactory.getLogger(RestController.class);

    private final AWSStepFunctions client;

    private final Scheduler scheduler;

    public RestController(AWSStepFunctions client) {
        this.client = client;
        this.scheduler = Schedulers.boundedElastic();
    }

    @PostMapping(
        value = "/activity",
        produces = {"application/json"}
    )
    Mono<ResponseEntity<String>> redeemActivity(@RequestBody(required = false) String body) {
        log.info("activity executed with body: {}", body);
        return Mono.just(ResponseEntity
            .accepted()
            .body("{}"));
    }

    @PostMapping(
        value = "/redeem-points-sync",
        produces = {"application/json"}
    )
    Mono<ResponseEntity<String>> create(@RequestBody(required = false) String countStr) {

        int count = 1;
        if (StringUtils.hasText(countStr)) {
            count = Integer.parseInt(countStr);
        }


        List<Mono<String>> asyncResults = IntStream.rangeClosed(1, count)
            .boxed()
            .parallel()
            .map(i -> startWorkflowAsync())
            .collect(Collectors.toList());

        return Flux.fromIterable(asyncResults)
            .flatMap(x -> x)
            .collectList()
            .map(List::size)
            .map(result -> ResponseEntity
                .accepted()
                .body("" + result));
    }

    private Mono<String> startWorkflowAsync() {
        return Mono.fromSupplier(this::startWorkflow)
            .subscribeOn(scheduler);
    }

    private String startWorkflow() {
        String instanceId = UUID.randomUUID().toString();
        log.info("Workflow is starting, id: {}", instanceId);
        long started = System.currentTimeMillis();
        StartSyncExecutionRequest executionRequest = new StartSyncExecutionRequest()
            .withStateMachineArn("arn:aws:states:us-east-1:201136940110:stateMachine:MyStateMachine");
        StartSyncExecutionResult executionResult = client.startSyncExecution(executionRequest);
        String execUrn = executionResult.getExecutionArn();
        log.info("Execution completed within {} ms, result status: {},  execUrn: {} ",
            System.currentTimeMillis() - started,
            executionResult.getStatus(),
            execUrn);
        return execUrn;
    }

}
