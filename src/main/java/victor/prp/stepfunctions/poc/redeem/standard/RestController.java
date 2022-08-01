package victor.prp.stepfunctions.poc.redeem.standard;


import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import victor.prp.stepfunctions.poc.config.RabbitMqConfig;

@Controller
public class RestController {
    private static final Logger log = LoggerFactory.getLogger(RestController.class);

    private final AWSStepFunctions client;

    private final Scheduler scheduler;

    private final AmqpTemplate amqpTemplate;

    private final AmqpAdmin amqpAdmin;

    private final ObjectMapper jsonMapper;

    public RestController(AWSStepFunctions client, AmqpTemplate amqpTemplate, AmqpAdmin amqpAdmin, ObjectMapper jsonMapper) {
        this.client = client;
        this.amqpTemplate = amqpTemplate;
        this.amqpAdmin = amqpAdmin;
        this.jsonMapper = jsonMapper;
        this.scheduler = Schedulers.boundedElastic();
    }


    @PostMapping(
        value = "/redeem-points",
        produces = {"application/json"}
    )
    Mono<ResponseEntity<String>> create(@RequestBody(required = false) String countStr) {
        amqpTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_NAME, null, "redeem");


        int count = 1;
        if (StringUtils.hasText(countStr)) {
            count = Integer.parseInt(countStr);
        }


        List<Mono<String>> asyncResults = IntStream.rangeClosed(1, count)
            .boxed()
            .parallel()
            .map(i -> executeWorkflowWithResult())
            .collect(Collectors.toList());

        return Flux.fromIterable(asyncResults)
            .flatMap(x -> x)
            .collectList()
            .map(List::size)
            .map(result -> ResponseEntity
                .accepted()
                .body("" + result));
    }


    private Mono<String> executeWorkflowWithResult(){
        return createTempQueueAsync()
            .flatMap(this::startWorkflowAsync)
            .flatMap(this::waitForResultAsync);
    }

    private Mono<String> waitForResultAsync(String routingKey) {
        return Mono.fromSupplier(() -> waitForResult(routingKey))
            .subscribeOn(scheduler);
    }

    private String waitForResult(String routingKey) {
        Message msg =  amqpTemplate.receive(routingKey, 5000);
        if (msg == null){
            log.error("Time out. routing key: "+routingKey);
            throw new RuntimeException("Timed out. routing key: "+routingKey);
        }
        String body = new String(msg.getBody());
        log.info("Walla, Got result for routing key: {}, result: {} "+routingKey, body);

        return body;
    }

    private Mono<String> startWorkflowAsync(String routingKey) {
        return Mono.fromSupplier(() -> startWorkflow(routingKey))
            .map( id -> routingKey)
            .subscribeOn(scheduler);
    }

    private Mono<String> createTempQueueAsync(){
        return Mono.fromSupplier(this::createTempQueue)
            .subscribeOn(scheduler);
    }

    private String createTempQueue(){

        String routingKey = UUID.randomUUID().toString();
        Queue queue = new Queue(routingKey, true, true, false);
        log.info("Going to create queue: {}",queue);
        String name = amqpAdmin.declareQueue(queue);

        Binding binding = new Binding(routingKey, Binding.DestinationType.QUEUE, RabbitMqConfig.EXCHANGE_NAME, routingKey, null);
        log.info("Going to create binding: {}",binding);
        amqpAdmin.declareBinding(binding);

        log.info("Queue with binding were successfully created for routingKey: {}",routingKey);

        return routingKey;
    }

    private String startWorkflow(String routingKey) {
        String instanceId = UUID.randomUUID().toString();
        log.info("Workflow is starting, id: {}", instanceId);
        ExecInput redeemPointsInput = new RedeemPointsInput(1,1)
            .with(routingKey);
        StartExecutionRequest executionRequest = new StartExecutionRequest()
            .withInput(serialize(redeemPointsInput))
            .withStateMachineArn("arn:aws:states:us-east-1:201136940110:stateMachine:saga-poc");
        StartExecutionResult executionResult = client.startExecution(executionRequest);
        String execUrn = executionResult.getExecutionArn();
        log.info("Execution started, execUrn: {} ", execUrn);
        return execUrn;
    }

    private String serialize(Object obj){
        try {
            return jsonMapper.writer().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to json: "+ obj, e);
        }
    }
}
