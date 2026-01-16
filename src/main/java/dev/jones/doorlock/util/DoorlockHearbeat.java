package dev.jones.doorlock.util;

import dev.jones.doorlock.Doorlock;
import org.bukkit.Bukkit;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DoorlockHearbeat {
    private static int task=-1;
    private static final ConcurrentLinkedQueue<Runnable> queue=new ConcurrentLinkedQueue<>();
    public static void start(){
        task=Bukkit.getScheduler().scheduleSyncRepeatingTask(Doorlock.getInstance(), new Runnable() {
            @Override
            public void run() {
                while (true){
                    Runnable runnable = queue.poll();
                    if(runnable==null)break;
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        },0,1);
    }
    public static void stop(){
        Bukkit.getScheduler().cancelTask(task);
    }
    public static void queueRunnable(Runnable r){
        queue.add(r);
    }
}
