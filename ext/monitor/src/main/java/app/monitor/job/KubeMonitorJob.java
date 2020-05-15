package app.monitor.job;

import app.monitor.kube.KubeClient;
import app.monitor.kube.PodList;
import core.framework.internal.log.LogManager;
import core.framework.json.JSON;
import core.framework.kafka.MessagePublisher;
import core.framework.log.message.StatMessage;
import core.framework.scheduler.Job;
import core.framework.scheduler.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author neo
 */
public class KubeMonitorJob implements Job {
    public final MessagePublisher<StatMessage> publisher;
    public final KubeClient kubeClient;
    public final List<String> namespaces;
    private final Logger logger = LoggerFactory.getLogger(KubeMonitorJob.class);

    public KubeMonitorJob(List<String> namespaces, KubeClient kubeClient, MessagePublisher<StatMessage> publisher) {
        this.publisher = publisher;
        this.kubeClient = kubeClient;
        this.namespaces = namespaces;
    }

    @Override
    public void execute(JobContext context) {
        try {
            var now = ZonedDateTime.now();
            for (String namespace : namespaces) {
                PodList pods = kubeClient.listPods(namespace);
                for (PodList.Pod pod : pods.items) {
                    String errorMessage = check(pod, now);
                    if (errorMessage != null) {
                        logger.warn("detected failed pod, ns={}, name={}, pod={}", namespace, pod.metadata.name, JSON.toJSON(pod));
                        publishPodFailure(pod, errorMessage);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
            publishError(e);
        }
    }

    String check(PodList.Pod pod, ZonedDateTime now) {
        if (pod.metadata.deletionTimestamp != null) {
            Duration elapsed = Duration.between(pod.metadata.deletionTimestamp, now);
            if (elapsed.toSeconds() >= 300) {
                return "pod is still in deletion, elapsed=" + elapsed;
            }
            return null;
        }

        String phase = pod.status.phase;
        if ("Succeeded".equals(phase)) return null; // terminated
        if ("Failed".equals(phase) || "Unknown".equals(phase)) return "unexpected pod phase, phase=" + phase;
        if ("Pending".equals(phase)) {
            // newly created pod may not have container status yet, containerStatuses is initialized as empty
            for (PodList.ContainerStatus status : pod.status.containerStatuses) {
                if (status.state.waiting != null && "ImagePullBackOff".equals(status.state.waiting.reason)) {
                    return "ImagePullBackOff: " + status.state.waiting.message;
                }
            }
        }
        if ("Running".equals(phase)) {
            boolean ready = true;
            for (PodList.ContainerStatus status : pod.status.containerStatuses) {
                if (status.state.waiting != null && "CrashLoopBackOff".equals(status.state.waiting.reason)) {
                    return "CrashLoopBackOff: " + status.state.waiting.message;
                }
                if (status.restartCount >= 5) {
                    return "pod restarted too many times, restart=" + status.restartCount;
                }
                if (!Boolean.TRUE.equals(status.ready)) {
                    ready = false;
                }
            }
            if (ready) return null;  // all running, all ready
        }
        ZonedDateTime startTime = pod.status.startTime != null ? pod.status.startTime : pod.metadata.creationTimestamp;  // startTime may not be populated yet if pod is just created
        Duration elapsed = Duration.between(startTime, now);
        if (elapsed.toSeconds() >= 300) {
            return "pod is still not ready, elapsed=" + elapsed;
        }
        return null;
    }

    private void publishPodFailure(PodList.Pod pod, String errorMessage) {
        var message = new StatMessage();
        Instant now = Instant.now();
        message.id = LogManager.ID_GENERATOR.next(now);
        message.date = Instant.now();
        message.result = "ERROR";
        message.app = pod.metadata.labels.getOrDefault("app", pod.metadata.name);
        message.host = pod.metadata.name;
        message.errorCode = "POD_FAILURE";
        message.errorMessage = errorMessage;
        publisher.publish(message);
    }

    private void publishError(Throwable e) {
        var message = new StatMessage();
        Instant now = Instant.now();
        message.id = LogManager.ID_GENERATOR.next(now);
        message.date = now;
        message.result = "ERROR";
        message.app = "kubernetes";
        message.errorCode = "FAILED_TO_COLLECT";
        message.errorMessage = e.getMessage();
        publisher.publish(message);
    }
}
