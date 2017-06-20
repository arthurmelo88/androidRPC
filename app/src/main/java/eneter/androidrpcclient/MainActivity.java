package eneter.androidrpcclient;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.rpc.IRpcClient;
import eneter.messaging.endpoints.rpc.RpcFactory;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class MainActivity extends Activity {
    // Eneter communication.
    IRpcClient<IMyService> myRpcClient;
    // UI controls
    private EditText myNumber1EditText;
    private EditText myNumber2EditText;
    private EditText myResultEditText;
    private Button myCalculateButton;
    private EditText myEchoEditText;
    private Button myGetEchoButton;
    private EditText myEchoedEditText;
    private OnClickListener myOnGetEchoButtonClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            onGetEchoButtonClick(v);
        }
    };
    private IDuplexOutputChannel anOutputChannel;
    public static volatile DispatchQueue connectionQueue = new DispatchQueue("connectionQueue");
    private OnClickListener myOnCalculateButtonClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            onCalculateButtonClick(v);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get UI widgets.
        myNumber1EditText = (EditText) findViewById(R.id.number1editText);
        myNumber2EditText = (EditText) findViewById(R.id.number2EditText);
        myResultEditText = (EditText) findViewById(R.id.resultEditText);
        myEchoEditText = (EditText) findViewById(R.id.echoTexteditText);
        myEchoedEditText = (EditText) findViewById(R.id.echoedEditText);

        myCalculateButton = (Button) findViewById(R.id.caculateBtn);
        myCalculateButton.setOnClickListener(myOnCalculateButtonClick);

        myGetEchoButton = (Button) findViewById(R.id.getEchoBtn);
        myGetEchoButton.setOnClickListener(myOnGetEchoButtonClick);

        // Open the connection in another thread.
        // Note: From Android 3.1 (Honeycomb) or higher
        // it is not possible to open TCP connection
        // from the main thread.
        connectionQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                openConnection();
            }
        });
    }

    @Override
    public void onDestroy() {
        closeConnection();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    private void openConnection() {
        try {
            // Instantiate RPC client.
            RpcFactory aFactory = new RpcFactory();
            myRpcClient = aFactory.createClient(IMyService.class);

            // Use TCP for the communication.
            IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();

            // Note: special IP address used by the Android simulator.
            // 10.0.2.2 is routed by the simulator to 127.0.0.1 of machine where
            // the simulator is running.
            // Use real IP address if you run it on a real Android device!
            anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://192.168.1.20:8032/");

            // Attach the output channel to the RPC client and be able to
            // communicate.
            myRpcClient.attachDuplexOutputChannel(anOutputChannel);
        } catch (Exception err) {
            EneterTrace.error("Failed to open connection with the service.", err);
        }
    }

    private void closeConnection() {
        // Stop listening to response messages.
        if (myRpcClient != null) {
            myRpcClient.detachDuplexOutputChannel();
        }
    }

    private void onCalculateButtonClick(View v) {
        final int aNumber1 = Integer.parseInt(myNumber1EditText.getText().toString());
        final int aNumber2 = Integer.parseInt(myNumber2EditText.getText().toString());

        try {
            if (!anOutputChannel.isConnected()) {
                closeConnection();
                connectionQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        openConnection();
                    }
                });
            }

            int aResult = myRpcClient.getProxy().Calculate(aNumber1, aNumber2);
            myResultEditText.setText(Integer.toString(aResult));

            // Invoke the remote method.
        } catch (Exception err) {
            EneterTrace.error("Failed to invoke the remote method.", err);
        }
    }

    private void onGetEchoButtonClick(View v) {
        try {
            String aText = myEchoEditText.getText().toString();
            String anEchoedText = myRpcClient.getProxy().GetEcho(aText);

            myEchoedEditText.setText(anEchoedText);
        } catch (Exception err) {
            EneterTrace.error("Failed to invoke the remote method.", err);
        }
    }

    // Interface exposed by the service.
    // Note: names of methods are case sensitive.
    // So keep capitals as declared in C#.
    public static interface IMyService {
        int Calculate(int a, int b);

        String GetEcho(String text);
    }
}
