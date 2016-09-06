package edu.feicui.smstest;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;

public class MainActivity extends AppCompatActivity {
    /**
     * Mob官网添加应用的App Key
     */
    private final String appKey = "16dc07c69ea66";
    /**
     * Mob官网添加应用的App Secret
     */
    private final String appSecret = "25f2b63f3b80601702f630d76ead52a9";
    /**
     * 验证码请求间隔时间(单位s)
     */
    private final int TIME_DELY = 60;
    private int time_dely;
    private final String TAG = getClass().getName();
    private SMSHandler mHandler;
    /**
     * 开关
     */
    private boolean flag;
    /**
     * 电话号码
     */
    private String phoneNumber;
    @BindView(R.id.eTxt_phone_number)
    EditText mETxtPhoneNumber;
    @BindView(R.id.btn_getCode)
    Button mBtnGetCode;
    @BindView(R.id.eTxt_enter_code)
    EditText mETxtEnterCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        //启动短信验证
        SMSSDK.initSDK(this, appKey, appSecret);
        //注册监听事件
        SMSSDK.registerEventHandler(mEventHandler);
        mHandler = new SMSHandler();
    }

    private EventHandler mEventHandler = new EventHandler() {//该部分在子线程中执行
        @Override
        public void afterEvent(int event, int result, Object data) {
            Message message = Message.obtain();
            if (result == SMSSDK.RESULT_COMPLETE) {//完成
                if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {//提交验证成功
                    message.what = 0;
                    message.obj = data;
                    Log.e(TAG, "提交验证码成功");
                } else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE) {//获取验证码成功
                    message.what = 1;
                    message.obj = "获取验证码成功";
                    Log.e(TAG, "获取验证码成功");
                }
            } else {
                message.what = 2;
                message.obj = "验证失败";
                Log.e(TAG, "验证失败");
                //打印错误信息
                ((Throwable) data).printStackTrace();
            }
            mHandler.sendMessage(message);
        }
    };

    @OnClick(R.id.btn_getCode)
    public void getCode(View view) {//获取验证码事件
        if (flag) {//如果开关打开则不执行点击事件
            return;
        }
        //获取输入的手机号码
        String str = mETxtPhoneNumber.getText().toString();
        if (str.equals("")) {
            Toast.makeText(this, "输入手机号码不能为空", Toast.LENGTH_SHORT).show();
        } else if (isPhoneNumber(str)) {//手机号码无误
            phoneNumber = mETxtPhoneNumber.getText().toString();
            SMSSDK.getVerificationCode("86", phoneNumber);
            changeBtnStatus();
            flag = true;
        } else {
            Toast.makeText(this, "输入的号码格式有误", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.btn_verify)
    public void verify() {
        String str = mETxtEnterCode.getText().toString();
        if (str.equals("")) {//验证码为空
            Toast.makeText(this, "验证码不能为空", Toast.LENGTH_SHORT).show();
        } else {
            if (null != phoneNumber) {
                SMSSDK.submitVerificationCode("86", phoneNumber, str);
            }
        }
    }

    private boolean isPhoneNumber(String phoneNumber) {
        // TODO: 2016/9/6  目前只支持第二位数是3、5、7、8的手机号码
        return phoneNumber.matches("1[3587]\\d{9}");
    }

    /**
     * 该方法控制
     */
    private void changeBtnStatus() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                time_dely = TIME_DELY;
                while (time_dely > 0) {
                    time_dely--;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBtnGetCode.setClickable(false);
                            mBtnGetCode.setText("获取验证码(" + time_dely + ")");
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtnGetCode.setClickable(true);
                        mBtnGetCode.setText("获取验证码");
                    }
                });
                flag = false;
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SMSSDK.unregisterAllEventHandler();
    }

    class SMSHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (null == msg) {
                return;
            }
            String str = "";
            switch (msg.what) {
                case 0://提交验证成功
                    str = "提交验证成功--" + msg.obj.toString();
                    break;
                case 1://获取验证成功
                    str = "获取验证码成功--" + msg.obj.toString();
                    break;
                case 2://验证失败
                    str = msg.obj.toString();
                    break;
            }
            Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
        }
    }
}
