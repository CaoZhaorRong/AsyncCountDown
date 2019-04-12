package net.lzzy.asynccountdown;

import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Administrator
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int WHAT_COUNTING = 0;
    public static final int WHAT_EXCEPTION = 1;
    public static final int WHAT_COUNT_DONE = 2;
    private TextView textView;
    public int secounds = 20;
    boolean isCounting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.tv);
        findViewById(R.id.activity_main_btn1).setOnClickListener(this);
        findViewById(R.id.activity_main_btn2).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.activity_main_btn1) {
            countDown();
        }
        if (v.getId() == R.id.activity_main_btn2) {
            if (isCounting) {
                Toast.makeText(MainActivity.this, "计时中", Toast.LENGTH_SHORT).show();
                return;
            }
            asyncCountDown();
        }
    }

    //region 创建线程池执行

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAX_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECOUNDS = 30;
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "thread#" + count.getAndIncrement());
        }
    };
    private static final BlockingQueue<Runnable> POOL_QUEUE = new LinkedBlockingDeque<>(128);

    public static ThreadPoolExecutor getExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE,
                KEEP_ALIVE_SECOUNDS, TimeUnit.SECONDS, POOL_QUEUE, THREAD_FACTORY);
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    //endregion

    private void asyncCountDown() {
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                isCounting = true;
                while (secounds >= 0) {
                    secounds--;
                    Message msg = handler.obtainMessage();
                    msg.what = WHAT_COUNTING;
                    msg.arg1 = secounds;
                    handler.sendMessage(msg);
                    //发送任务
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        handler.sendMessage(handler.obtainMessage(WHAT_EXCEPTION, e.getMessage()));
                    }
                }
                handler.sendEmptyMessage(WHAT_COUNT_DONE);
            }
        });
    }

    private CountHandler handler = new CountHandler(this);

    private  static class CountHandler extends AbstractStaticHandler<MainActivity> {

        CountHandler(MainActivity context) {
            super(context);
        }

        @Override
        public void handleMessage(Message msg, MainActivity activity) {
            switch (msg.what) {
                case WHAT_COUNTING:
                    String text = "剩余时间" + msg.arg1 + "秒";
                    activity.textView.setText(text);
                    break;
                case WHAT_COUNT_DONE:
                    activity.textView.setText("计时完成");
                    break;
                case WHAT_EXCEPTION:
                    Toast.makeText(activity, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    activity.secounds = 20;
                    break;
                default:
                    break;
            }
        }
    }


    private void countDown() {
        while (secounds >= 0) {
            String text = "剩余时间" + secounds + "秒";
            textView.setText(text);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            secounds--;
        }
        secounds = 20;
        textView.setText("记时完成");
    }


}
