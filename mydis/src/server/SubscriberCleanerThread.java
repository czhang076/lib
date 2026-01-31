package server;

public class SubscriberCleanerThread extends Thread {
    private final SubscriberManager subscriberManager;
    private final long intervalMillis;

    public SubscriberCleanerThread(SubscriberManager subscriberManager, long intervalMillis) {
        this.subscriberManager = subscriberManager;
        this.intervalMillis = intervalMillis;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                Thread.sleep(intervalMillis);
                subscriberManager.cleanupExpired();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
