package com.ld.nfccardbledes;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import com.ld.blecardlibrarydes.ble.BLEUtil;
import com.ld.blecardlibrarydes.ble.BleDataListener;
import com.ld.blecardlibrarydes.ble.ScanListener;
import com.ld.blecardlibrarydes.utils.DataUtil;
import com.ld.blecardlibrarydes.utils.DesUtil;
import com.ld.blecardlibrarydes.utils.ToastUtil;
import butterknife.Bind;
import butterknife.ButterKnife;  // View 注入框架，依赖注入，需要module 的 build.gradle 文件中 有 compile 相应的 包
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.id_connect)
    Button connect;
    @Bind(R.id.id_send)
    Button send;
    @Bind(R.id.id_disconnect)
    Button disconnect;
    @Bind(R.id.id_clear)
    Button clear;
    //接收数据
    @Bind(R.id.id_tv)
    TextView tv;
    //功能数字
    @Bind(R.id.id_opcode)
    EditText ed_opcode;
    //数据值
    @Bind(R.id.id_opcode_data)
    EditText ed_opcode_data;
    // 滚动容器， framelayout
    @Bind(R.id.id_scrollview)
    ScrollView scrollView;

    private boolean isConnect = false;
    private Context ct;
    private ProgressDialog pd;
    private StringBuffer dataGet;
    private int opcode_num,loop;
    private BLEUtil bleUtil;
    private String[] errorCode;
    //显示收到的数据 函数定义
    public void dataChange(String data) {
        dataGet.append(data + "\n");
        tv.setText(dataGet);
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        ed_opcode.setText("1");
        loop = 0;
        dataGet = new StringBuffer();
        ct = this;
        errorCode = getResources().getStringArray(R.array.error_code);
        bleUtil = new BLEUtil(this, new BleDataListener() {
            @Override
            public void Disconnected() {
                if (pd != null) {
                    pd.dismiss();
                }
                isConnect = false;
                connect.setText(R.string.connect_again);
                ToastUtil.toastShort(ct, R.string.disconnected);
            }

            @Override
            public void Connected() {
                isConnect = true;
                connect.setText(R.string.connected);
                pd.dismiss();
            }

            @Override
            public void NormalData(byte[] data, int count) {
                dataChange("<-| " + "length:" + count + " - " + DataUtil.getStringByBytes(data) + "\n");
            }

            @Override
            public void ErrorData(byte errorData) {
//                Log.i("aaa","error:"+errorData);
                int num = errorData & 0x0f;
                dataChange("Error:" + errorCode[num - 1]);
            }

            @Override
            public void SendData(byte[] sendData) {
                dataChange("->| " + DataUtil.getStringByBytes(sendData) + "\n");
            }
        });
    }

    @OnClick({R.id.id_send, R.id.id_connect, R.id.id_disconnect, R.id.id_clear})
    void onClick(View view) {
        switch (view.getId()) {
            case R.id.id_send:
                if (isConnect) {
                    opcode_num = Integer.parseInt(ed_opcode.getText().toString());
                    if (opcode_num > 11 || opcode_num < 0) {
                        ToastUtil.toastShort(ct, "请输入0-11数字");
                        return;
                    }
                    //07不加密
                    if(opcode_num == 7){
                        bleUtil.ExchangeAPDU(BLEUtil.Generate_APDU(opcode_num,DataUtil.getBytesByString(ed_opcode_data.getText().toString().trim())));
                        String KeyNu = ed_opcode_data.getText().toString().trim().substring(0,2);
                        if(KeyNu.equals("01")) {
                            bleUtil.setMasterKey(ed_opcode_data.getText().toString().trim().substring(2));
                            dataGet.append("APP内部加解密密钥已修改" + "\n");
                            tv.setText(dataGet);
                            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                        return;
                    }
                    //加密
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            byte[] data = BLEUtil.Generate_APDU(opcode_num,DataUtil.getBytesByString(ed_opcode_data.getText().toString().trim()));//原始数据
                            bleUtil.ExchangeAPDU(DataUtil.getBytesByString(DesUtil.encryptDes(DataUtil.getStringByBytes(data))));//加密并发送
                        }
                    }).start();
                } else {
                    ToastUtil.toastShort(this, R.string.please_connect);
                    return;
                }
                break;
            case R.id.id_connect:
                if (isConnect) return;
                pd = ProgressDialog.show(this, "正在连接蓝牙", "请稍后....");
                bleUtil.ConnectDev(new ScanListener() {
                    @Override
                    public void Fail() {
                        pd.dismiss();
                    }
                });
                break;
            case R.id.id_disconnect:
                if (isConnect)
                    bleUtil.Disconnect();
                break;
            case R.id.id_clear:
                dataGet.setLength(0);
                tv.setText("");
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bleUtil.Destroy();
    }
}


