package naoserver;

import com.aldebaran.qi.Application;
import com.aldebaran.qi.CallError;
import com.aldebaran.qi.helper.proxies.ALBehaviorManager;
import com.aldebaran.qi.helper.proxies.ALMemory;
import com.aldebaran.qi.helper.proxies.ALTextToSpeech;
import com.aldebaran.qi.helper.proxies.PackageManager;


public class NAORobot {
    private Application app;

    // submodules
    private ALTextToSpeech tts;
    private ALMemory memory;
    private ALBehaviorManager behaviorManager;
    private PackageManager packageManager;

    public NAORobot(String url, String[] args) {

        try {
            // create a new connection to NAO
            this.app = new Application(args, url);
            this.app.start();

            System.out.println("Connection to NAO was successful");

            // Start submodules
            this.tts = new ALTextToSpeech(this.app.session());
            this.memory = new ALMemory(this.app.session());
            this.behaviorManager = new ALBehaviorManager(this.app.session());
            this.packageManager = new PackageManager(this.app.session());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized Object[] getBehaviors() throws InterruptedException, CallError {
        return(new Object[]{this.behaviorManager.async().getInstalledBehaviors().get()});
    }

    public synchronized Object[] getTempData() throws InterruptedException, CallError {
        Object cpuTemp = this.memory.async().getData("Device/SubDeviceList/Head/Temperature/Sensor/Value").get();
        Object batteryTemp = this.memory.async().getData("Device/SubDeviceList/Battery/Temperature/Sensor/Value").get();

        return(new Object[]{cpuTemp, batteryTemp});
    }

    public synchronized void installBehavior(String behavior) throws InterruptedException, CallError {
        if(!this.behaviorManager.async().isBehaviorInstalled(behavior).get()) {
            this.packageManager.async().install(behavior);
        }
        else {
            System.out.println("Package / Behavior already installed...");
        }
    }

    public synchronized void stopBehavior(String behavior) throws InterruptedException, CallError {
        if(this.behaviorManager.async().isBehaviorRunning(behavior).get()) {
            this.behaviorManager.async().stopBehavior(behavior);
        }
        else {
            System.out.println("Couldn't find a running behavior by that name...");
        }
    }

    public synchronized void startBehavior(String behavior) throws InterruptedException, CallError {
        if(this.behaviorManager.async().isBehaviorInstalled(behavior).get()) {
            if(!this.behaviorManager.async().isBehaviorRunning(behavior).get()) {
                this.behaviorManager.async().runBehavior(behavior);
            }
            else {
                System.out.println("Behavior is already running");
            }
        }
        else {
            System.out.println("Behavior not found..");
        }
    }

    public synchronized void naoSpeak(String message) throws InterruptedException, CallError {
        this.tts.async().say(message);
    }
}
