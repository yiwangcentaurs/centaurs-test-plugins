import okhttp3.*;
import org.json.JSONObject;

import static java.lang.Math.toIntExact;

import java.io.IOException;

public class Plugin extends Thread {

    final private OkHttpClient client = new OkHttpClient();

    final static MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    // App information
    private static String appname;
    private static String address;
    private static int port;
    private int interval = 10;  // in second

    public Plugin(String appname, String address, int port) {
        this.address = address;
        this.port = port;
        this.appname = appname;
    }

    private void runTest() throws Exception {

    }

    private void sysCheck() {
        // Prepare verify
        String path = "/api/gm/server-info/";
        String url = String.format("%s:%d%s", address, port, path);

        int sysSum = toIntExact(Runtime.getRuntime().totalMemory() / 1024);
        int sysFree = toIntExact(Runtime.getRuntime().freeMemory() / 1024);
        int svrFree = sysFree;
        int svrAlc = sysSum - sysFree;

        JSONObject json = new JSONObject();

        json.put("sys_free", sysFree);
        json.put("sys_sum", sysSum);
        json.put("srv_alc", svrAlc);
        json.put("srv_free", svrFree);

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
            System.out.println("[PLUGIN]" + res);
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    public void showConfig() {
        System.out.println("appname: \t" + this.appname);
        System.out.println("address: \t" + this.address);
        System.out.println("port: \t\t" + this.port);
        System.out.println("interval: \t" + this.interval + " sec");
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    @Override
    public void run() {
        try{
            while(true) {
                this.sysCheck();
                Thread.sleep(interval * 1000);
            }
        } catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Plugin plugin = new Plugin("app-test","http://localhost", 10021);
        plugin.setInterval(12);
        plugin.showConfig();
        plugin.start();
    }
}
