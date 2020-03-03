package naoserver;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.aldebaran.qi.Application;
import com.aldebaran.qi.CallError;
import com.aldebaran.qi.Tuple4;
import com.aldebaran.qi.helper.proxies.ALBattery;
import com.aldebaran.qi.helper.proxies.ALBehaviorManager;
import com.aldebaran.qi.helper.proxies.ALMemory;
import com.aldebaran.qi.helper.proxies.ALMotion;
import com.aldebaran.qi.helper.proxies.ALNavigation;
import com.aldebaran.qi.helper.proxies.ALRobotPosture;
import com.aldebaran.qi.helper.proxies.ALTextToSpeech;
import com.aldebaran.qi.helper.proxies.ALVideoDevice;
import com.aldebaran.qi.helper.proxies.PackageManager;
import com.aldebaran.qi.helper.proxies.ALSystem;


public class NAORobot {
    private Application app;

    // submodules
    private ALTextToSpeech ttsManager;
    private ALMemory memoryManager;
    private ALBattery batteryManager;
    private ALMotion movementManager;
    private ALNavigation navigationManager;
    private ALVideoDevice cameraManager;
    private ALBehaviorManager behaviorManager;
    private PackageManager packageManager;
    private ALSystem systemManager;

    public NAORobot(String url, String[] args) {

        try {
            // create a new connection to NAO
            app = new Application(args, url);
            app.start();

            System.out.println("Connection to NAO was successful");

            // Start submodules
            ttsManager = new ALTextToSpeech(app.session());
            memoryManager = new ALMemory(app.session());
            movementManager = new ALMotion(app.session());
            navigationManager = new ALNavigation(app.session());
            cameraManager = new ALVideoDevice(app.session());
            behaviorManager = new ALBehaviorManager(app.session());
            packageManager = new PackageManager(app.session());
            systemManager = new ALSystem(app.session());
            batteryManager = new ALBattery(app.session());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized List<String> getBehaviors() throws InterruptedException, CallError {
        return(behaviorManager.async().getInstalledBehaviors().get());
    }

    public synchronized List<Object> getTempData() throws InterruptedException, CallError {
        Object cpuTemp = memoryManager.async().getData("Device/SubDeviceList/Head/Temperature/Sensor/Value").get();
        Object batteryTemp = memoryManager.async().getData("Device/SubDeviceList/Battery/Temperature/Sensor/Value").get();
        
        List<Object> data = new ArrayList<Object>();

        data.add(cpuTemp);
        data.add(batteryTemp);

        return(data);
    }

    public synchronized List<Object> takeImage() throws InterruptedException, CallError {
        String device = cameraManager.async().subscribeCamera("test", 0, 1, 13, 10).get();
        Object imageObject = cameraManager.async().getDirectRawImageRemote(device).get();

        List<Object> data = (ArrayList<Object>) imageObject;

        System.out.println(data.get(6));
        
        ByteBuffer dataTest = ((ByteBuffer) data.get(6));
        ByteBuffer byteBuffer = ByteBuffer.allocate(dataTest.capacity());

        dataTest.flip();
        byteBuffer.put(dataTest);
        dataTest.compact();

        byte[] byteData;
      
        if(byteBuffer.hasArray()) {
            byteData = byteBuffer.array();
        }
        else {
            byteData = new byte[byteBuffer.capacity()];
            ((ByteBuffer) byteBuffer.duplicate().clear()).get(byteData);
        }
    
        data.set(6, byteData);
        

        cameraManager.async().releaseDirectRawImage(device);
        cameraManager.async().unsubscribe(device);

        System.out.println("Original byteBuffer: " + byteBuffer + "\n Original byteBuffer as byte[]: " + byteData);

        return(data);
    }


    public synchronized Integer getBatteryData() throws InterruptedException, CallError {
        return(batteryManager.async().getBatteryCharge().get());
    }

    public synchronized void installBehavior(String behavior) throws InterruptedException, CallError {
        if(!behaviorManager.async().isBehaviorInstalled(behavior).get()) {
            packageManager.async().install(behavior);
        }
        else {
            System.out.println("Package / Behavior already installed...");
        }
    }

    public synchronized void getSystemData() throws InterruptedException, CallError {
        List<Tuple4<String, String, Long, Long>> data = systemManager.async().diskFree(true).get();

        for(int i = 0; i < data.size(); i++) {
            System.out.println("var0: " + data.get(i).var0 + ", var1: " + data.get(i).var1 + ", var2: " + data.get(i).var2
                + ", var3: " + data.get(i).var3);
        }
    }

    public synchronized void stopBehavior(String behavior) throws InterruptedException, CallError {
        if(behaviorManager.async().isBehaviorRunning(behavior).get()) {
            behaviorManager.async().stopBehavior(behavior);
        }
        else {
            System.out.println("Couldn't find a running behavior by that name...");
        }
    }

    public synchronized void startBehavior(String behavior) throws InterruptedException, CallError {
        if(behaviorManager.async().isBehaviorInstalled(behavior).get()) {
            if(!behaviorManager.async().isBehaviorRunning(behavior).get()) {
                behaviorManager.async().runBehavior(behavior);
            }
            else {
                System.out.println("Behavior is already running");
            }
        }
        else {
            System.out.println("Behavior not found..");
        }
    }

    public synchronized String enableExternalCollisionProtection() throws InterruptedException, CallError {
        if(movementManager.async().getExternalCollisionProtectionEnabled("All").get()) {
            return(new String("ExternalCollisionProtection is already enabled"));
        }
        
        movementManager.async().setExternalCollisionProtectionEnabled("All", true);
        return(new String("ExternalCollisionProtection enabled!"));
    }

    public synchronized String setSecurityDistance(float distance) throws InterruptedException, CallError {
        if(movementManager.async().getExternalCollisionProtectionEnabled("All").get()) {
            movementManager.async().setOrthogonalSecurityDistance(distance);
            return(new String("Security distance set!"));
        }

        return(new String("Something went wrong, security distance couldn't be set"));
    }

    public synchronized void naoMove(float x, float y, float theta) throws InterruptedException, CallError {
        // wake up nao
        if(!movementManager.async().robotIsWakeUp().get()) {
            movementManager.async().wakeUp();
        }

        // make nao stand
        /*
        if(!postureManager.async().getPosture().get().equals("StandInit")) {
            postureManager.async().goToPosture("StandInit", 0.5f);
        }
        */

        // move nao
        navigationManager.async().moveTo(x, y, theta);
    }

    public synchronized List<Float> getPosition() throws InterruptedException, CallError {
        return(movementManager.async().getPosition("Gyrometer", 2, false).get());
    }


    public synchronized void naoSpeak(String message) throws InterruptedException, CallError {
        ttsManager.async().say(message);
    }

    public synchronized List<String> getCommands() throws InterruptedException {
        List<String> methods = new ArrayList<String>();
        
        for(Method m : this.getClass().getDeclaredMethods()) {
            methods.add(m.getName());    
        }

        return(methods);
    }  
}
