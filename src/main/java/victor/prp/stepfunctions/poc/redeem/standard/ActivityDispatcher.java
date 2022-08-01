package victor.prp.stepfunctions.poc.redeem.standard;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskRequest;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskResult;
import com.amazonaws.services.stepfunctions.model.SendTaskFailureRequest;
import com.amazonaws.services.stepfunctions.model.SendTaskSuccessRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ActivityDispatcher {
    private static final Logger log = LoggerFactory.getLogger(ActivityDispatcher.class);

    private final AmqpTemplate amqpTemplate;
    private final ObjectMapper jsonMapper;
    private final AWSStepFunctions client;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    private final Map<String, Activity> activities;

    public ActivityDispatcher(AmqpTemplate amqpTemplate, ObjectMapper jsonMapper, AWSStepFunctions client,
                              Collection<Activity> activitiesList) {
        this.amqpTemplate = amqpTemplate;
        this.jsonMapper = jsonMapper;
        this.client = client;
        this.activities = activitiesList.stream()
            .collect(Collectors.toMap(Activity::arn, Function.identity()));

        activities.values()
                .forEach(this::schedule);

    }

    void schedule(Activity activity){
        scheduler.schedule(()-> poll(activity), 100, TimeUnit.MILLISECONDS);
        log.info("Polling is scheduled for activity: {}", activity);
    }
    void poll(Activity activity){
        try {
            pollAndDispatch(activity);
        }catch (Throwable e){
            log.error("Failed to poll task for activity: "+ activity.arn(), e);
        }
    }
    void pollAndDispatch(Activity activity){
            while (true) {
                try {
                    pollTask(activity)
                        .ifPresentOrElse(
                            task -> dispatch(task, activity),
                            () -> busyWait(activity));
                }catch (Throwable e){
                    log.warn("Exception during polling for activity: "+activity.arn(), e);
                }
        }
    }
    private void busyWait(Activity activity){
        try {
            log.info("Going to cont. busy waiting! Activity tasks not found for: {}", activity.arn());
            Thread.sleep(10);
        } catch (InterruptedException e) {
            log.error("Sleep interrupted", e);
        }
    }

    private ExecInput deserialize(String input){
        try {
            return jsonMapper.reader().readValue(input, ExecInput.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize input to ExecInput.class: "+ input,e);
        }
    }

    private void dispatch(GetActivityTaskResult task, Activity activity) {

        try {
            activity.execute();
            log.info("Activity successfully executed. arn: {}, task input: {}", activity, task.getInput());
            if (activity instanceof FinalizeActivity) {
                sendSuccessEvent(deserialize(task.getInput()));
            }

            client.sendTaskSuccess(
                new SendTaskSuccessRequest()
                    .withOutput(task.getInput())
                    .withTaskToken(task.getTaskToken()));
        } catch (final Exception e) {
            log.warn("Activity executed with failure. arn: " + activity.arn() + "task input: " + task.getInput(), e);

            client.sendTaskFailure(new SendTaskFailureRequest()
                .withTaskToken(
                    task.getTaskToken()));
        }
    }

    private void sendSuccessEvent(ExecInput execInput) {
        amqpTemplate.convertAndSend("sf-exchange", execInput.getRoutingKey(), "success");
    }

    private Optional<GetActivityTaskResult> pollTask(Activity activity) {
        try {
            GetActivityTaskResult activityTask =
                client.getActivityTask(
                    new GetActivityTaskRequest().withActivityArn(activity.arn()));

            if (activityTask != null && StringUtils.hasText(activityTask.getTaskToken())) {
                return Optional.of(activityTask);
            }
        }catch (Throwable e){
            log.warn("client got error during polling for activity: "+activity.arn(), e);
        }

        return Optional.empty();
    }
}
