package com.sobag.beaconplayground;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity
{

    // ------------------------------------------------------------------------
    // members
    // ------------------------------------------------------------------------

    private static final String LOG_TAG = "MainActivity";

    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private Handler scanHandler = new Handler();
    private int scan_interval_ms = 500000;
    private boolean isScanning = false;
    ListView item_list;
    private ArrayList<String> list = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    // ------------------------------------------------------------------------
    // default stuff...
    // ------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init BLE
        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        scanHandler.post(scanRunnable);

        item_list = (ListView) findViewById(R.id.item_list);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        item_list.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------------
    // public usage
    // ------------------------------------------------------------------------

    private Runnable scanRunnable = new Runnable()
    {
        @Override
        public void run() {

            if (isScanning)
            {
                //if (btAdapter != null)
                //{
                    btAdapter.stopLeScan(leScanCallback);
                //}
            }
            else
            {
               // if (btAdapter != null)
                //{
                    btAdapter.startLeScan(leScanCallback);
               // }
            }

            isScanning = !isScanning;

            scanHandler.postDelayed(this, scan_interval_ms);
        }
    };

    // ------------------------------------------------------------------------
    // Inner classes
    // ------------------------------------------------------------------------

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord)
        {
            int startByte = 2;
            boolean patternFound = false;

            while (startByte <= 5)
            {
                if (    ((int) scanRecord[startByte + 2] & 0xff) == 0x02 && //Identifies an iBeacon
                        ((int) scanRecord[startByte + 3] & 0xff) == 0x15)
                { //Identifies correct data length
                    patternFound = true;
                    break;
                }
                startByte++;
            }

            if (patternFound)
            {
                //Convert to hex String
                byte[] uuidBytes = new byte[16];
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);
                String hexString = bytesToHex(uuidBytes);

                //UUID detection
                String uuid =  hexString.substring(0,8) + "-" +
                        hexString.substring(8,12) + "-" +
                        hexString.substring(12,16) + "-" +
                        hexString.substring(16,20) + "-" +
                        hexString.substring(20,32);

                // major
                final int major = (scanRecord[startByte + 20] & 0xff) * 0x100 + (scanRecord[startByte + 21] & 0xff);

                // minor
                final int minor = (scanRecord[startByte + 22] & 0xff) * 0x100 + (scanRecord[startByte + 23] & 0xff);

                int txPower = (scanRecord[startByte + 24]);

                //Log.i(LOG_TAG,"UUID：" +uuid + "\nnmajor：" +major +"\nnminor：" +minor + "\nTxPower：" + txPower + "\nRSSI：" + rssi + "\ndistance："+calculateAccuracy(txPower,rssi));

                String range;
                double distance = calculateAccuracy(txPower,rssi);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setNegativeButton("YES",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub
                    }
                });

                if(distance<0.2)
                {
                    range = "Short Distance";
                    AlertDialog.Builder alertadd = new AlertDialog.Builder(
                            MainActivity.this);
                    alertadd.setTitle("義大利藝術家Aron Demetz");

                    LayoutInflater factory = LayoutInflater.from(MainActivity.this);
                    final View view = factory.inflate(R.layout.activity_imag, null);

                    ImageView image= (ImageView) view.findViewById(R.id.imageView);
                    image.setImageResource(R.mipmap.aron_image);

                    TextView text= (TextView) view.findViewById(R.id.textView);
                   text.setText("藝術帶來對生命的理解與謙和Art brings a sense of belonging!" +
                            " 義大利藝術家Aron Demetz 「Burning 燃燒」系列的作品闡述著生命力的本質。" +
                          "他將雕塑放在雪地裏，加上油用火燒碳化了表層，燒得烏黑透亮，撥開碳化的部分，仍看得出木頭的原色，" +
                            "他說不管你的生命遭受多大的傷害，內在還是充滿著生命力，不會真的死亡。" +
                            " 這系列的雕塑雖然黑漆漆的、甚而有些殘缺感，但確能讓人感受到生命的溫度與謙和。" +
                           "讓我們心有所住，不再惶恐不安！ Italian artist Aron Demetz's creation Burning Series expresses the essence of life. He burned sculptures with oil and carbonized their surface. The original color of wood is still underneath the surface, Aron said regardless how much pain and damage one suffers, the strength of life is still within. Although this series’ sculptures are all pitch black even with some deformation. They indeed bring a feeling of humbleness and the temperature of lives. Let us have a sense of belonging and no longer be fearful.");
                  // text.setText(" 義大利藝術家Aron Demetz 「Burning 燃燒」系列的作品闡述著生命力的本質");
                    alertadd.setView(view);
                    alertadd.setNeutralButton("ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dlg, int sumthin) {

                        }
                    });

                    alertadd.show();

//                    builder.setTitle("義大利藝術家Aron Demetz");
//
                  //  builder.setMessage("藝術帶來對生命的理解與謙和\n" +
                           // "Art brings a sense of belonging!\n" +
                         //   "義大利藝術家Aron Demetz 「Burning 燃燒」系列的作品闡述著生命力的本質。他將雕塑放在雪地裏，加上油用火燒碳化了表層，燒得烏黑透亮，撥開碳化的部分，仍看得出木頭的原色，他說不管你的生命遭受多大的傷害，內在還是充滿著生命力，不會真的死亡。\n" +
                         //   "這系列的雕塑雖然黑漆漆的、甚而有些殘缺感，但確能讓人感受到生命的溫度與謙和。讓我們心有所住，不再惶恐不安！\n" +
                         //   "Italian artist Aron Demetz's creation \"Burning Series\" expresses the essence of life. He burned sculptures with oil and carbonized their surface. The original color of wood is still underneath the surface, Aron said regardless how much pain and damage one suffers, the strength of life is still within.\n" +
                       //     "Although this series’ sculptures are all pitch black even with some deformation. They indeed bring a feeling of humbleness and the temperature of lives. Let us have a sense of belonging and no longer be fearful.");
//                    builder.show();

                }
                else if(distance>=1.0 && distance<=10.0)
                {
                    range = "Medium Distance";
                    //builder.setMessage(range);
                    //builder.show();
                }
                else if(distance>10.0)
                {
                    range = "Long Distance";
                   //builder.setMessage(range);
                    //builder.show();
                }
                else
                {
                    range = "NULL";
                }


                String newInput = "UUID：" +uuid +  "\nnmajor：" +major +"\nnminor：" +minor + "\nTxPower：" + txPower + "\nRSSI：" + rssi + "\nDistance："+ distance + "\nRange：" + range;

                adapter.add(newInput);
/*
                if(adapter.isEmpty())
                {
                    adapter.add(newInput);
                }
                else
                {
                    int count = adapter.getCount();

                    for(int i=0; i<count; i++)
                    {
                        String item = adapter.getItem(i).substring(5, 41);
                        if(!(newInput.substring(5, 41).equals(item)))
                        {
                            adapter.add(newInput);
                        }
                        else
                        {
                            adapter.clear();
                            adapter.add(newInput);
                        }
                    }
                }
*/
                adapter.add(newInput);
                adapter.notifyDataSetChanged();
            }

        }
    };

    /**
     * bytesToHex method
     */
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ )
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    protected static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            double accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            return accuracy;
        }
    }


}
