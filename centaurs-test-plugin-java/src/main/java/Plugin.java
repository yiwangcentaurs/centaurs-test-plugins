import com.sun.management.OperatingSystemMXBean;
import okhttp3.*;
import org.json.JSONObject;

import static java.lang.Math.toIntExact;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: Yuancheng Zhang & Feliciano Long
 * Please add following to gradle dependencies
 * compile 'com.squareup.okhttp3:okhttp:3.8.1'
 * compile group: 'org.json', name: 'json', version: '20160212'
 */
public class Plugin extends Thread {

    public interface ILogger { void log(String content); }

    public interface IEmailClient { boolean send(String title, String content); }

    final private OkHttpClient client = new OkHttpClient();

    final static MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    // App information
    private static String appname;
    private static String server;
    private static int port;
    private int interval = 10;  // in second

    private ILogger logger;

    private List<RunnableAction> tests;

    private Plugin(String appname, String server, int port) {
        this.server = server;
        this.port = port;
        this.appname = appname;

        // Synchronized test collection
        tests = Collections.synchronizedList(new ArrayList<RunnableAction>());

        // Default logger
        logger = content -> System.out.println(content);
    }

    private IEmailClient emailClient;

    public void setEmailClient(IEmailClient emailClient) {
        this.emailClient = emailClient;
    }

    public boolean sendEmail(String title, String content) { return emailClient.send(title, content); }

    private void addTest(RunnableAction runnableAction) {
        tests.add(runnableAction);
    }

    private void runTest() {
        AtomicInteger ordinal = new AtomicInteger();

        tests.forEach(runnableAction -> {
            logger.log("Running test: " + runnableAction.getActionName());
            boolean success = false;
            Exception exception = null;
            try {
                success = runnableAction.run();
            } catch (Exception e) {
                exception = e;
            } finally {
                if ( !success ) {
                    // Unsuccessful handler here
                    ordinal.getAndIncrement();
                    this.onFailedTest(
                            runnableAction.getActionName(),
                            (exception == null ? "Returned false" : exception.getMessage())
                    );
                }
            }
        });

        if ( ordinal.get() > 0 ) {
            logger.log("Tests failed: " + ordinal.get() );
        }
    }

    private void onFailedTest(String actionName, String message) {
        logger.log("Test failed: " + actionName + ". Message: " + message);
        // TODO: Handles failed test here
    }

    private void sysCheck() {
        // Prepare verify
        String path = "/api/gm/server-info/";
        String url = String.format("%s:%d%s", server, port, path);

        OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long sys_sum = operatingSystemMXBean.getTotalPhysicalMemorySize();
        long sys_free = operatingSystemMXBean.getFreePhysicalMemorySize();

        long svr_alc = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024;
        long svr_free = Runtime.getRuntime().freeMemory() / 1024;

        JSONObject json = new JSONObject();

        json.put("sys_free", sys_free);
        json.put("sys_sum", sys_sum);
        json.put("srv_alc", svr_alc);
        json.put("srv_free", svr_free);

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();

        try {
            Response response = client.newCall(request).execute();
            String res = response.body().string();
            logger.log("[PLUGIN]" + res);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showConfig() {
        logger.log("appname: \t" + this.appname);
        logger.log("server: \t" + this.server);
        logger.log("port: \t\t" + this.port);
        logger.log("interval: \t" + this.interval + " sec");
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    @Override
    public void run() {
        try{
            while(true) {
                this.sysCheck();
                this.runTest();
                Thread.sleep(interval * 1000);
            }
        } catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Plugin.newInstance("app-test","http://localhost", 10021);
        Plugin.getInstance().setInterval(12);
        Plugin.getInstance().showConfig();
        Plugin.getInstance().start();

        RunnableAction randomTest = new RunnableAction("CatInTheBox") {
            @Override
            boolean run() throws Exception {
                Random random = new Random();
                if (random.nextInt(2) == 1)
                    throw new IOException("The cat is dead :(");
                return true;
            }
        };

        Plugin.getInstance().addTest(randomTest);
        Plugin.getInstance().addTest(randomTest);
        Plugin.getInstance().addTest(randomTest);
        Plugin.getInstance().addTest(randomTest);
        Plugin.getInstance().addTest(randomTest);

        Plugin.getInstance().setEmailClient(new IEmailClient() {
            @Override
            public boolean send(String title, String content) {
                System.out.println("Title: " + title + "\nContent: " + content);
                return true;
            }
        });

        // Plugin.getInstance().setEmailClient(new EmailClient("", ""));
    }

    // Singleton
    private static Plugin instance;

    public static Plugin getInstance() {
        if (instance == null) {
            throw new NullPointerException("Couldn't find plugin instance, please init first.");
        }
        return instance;
    }

    public static void newInstance(String appname, String server, int port) {
        instance = new Plugin(appname, server, port);
    }

}
