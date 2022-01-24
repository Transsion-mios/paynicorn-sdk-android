package com.transsion.pay.paysdk.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.transsion.pay.paysdk.manager.ApiConsts;
import com.transsion.pay.paysdk.manager.PaySDKManager;
import com.transsion.pay.paysdk.manager.entity.CountryCurrencyData;
import com.transsion.pay.paysdk.manager.entity.OrderEntity;
import com.transsion.pay.paysdk.manager.entity.OrderResultEntity;
import com.transsion.pay.paysdk.manager.entity.PriceEntity;
import com.transsion.pay.paysdk.manager.entity.StartPayEntity;
import com.transsion.pay.paysdk.manager.entity.SupportPayInfoEntity;
import com.transsion.pay.paysdk.manager.inter.InitResultCallBack;
import com.transsion.pay.paysdk.manager.inter.OrderQuery;
import com.transsion.pay.paysdk.manager.inter.StartPayCallBack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String API_KEY = "7790053";
    public static final String AP_ID = "1340001";
    public static final String CP_ID = "2280014";

    private Button btnInit, btnSmsPay, btnOnlinePay, btnSubPay, btnSubPayQuery;
    private List<PriceEntity> subPriceList = new ArrayList<>();
    private List<PriceEntity> smsPriceList = new ArrayList<>();
    private boolean mSupportOnlinePay;
    private CountryCurrencyData mCountryCurrencyData;
    private TextView tvPayRresult;
    private String subOrderNum ="898888";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnInit = findViewById(R.id.btn_init);
        btnSmsPay = findViewById(R.id.btn_sms_pay);
        btnOnlinePay = findViewById(R.id.btn_online_pay);
        btnSubPay = findViewById(R.id.btn_sub_pay);
        tvPayRresult = findViewById(R.id.tv_pay_result);
        btnSubPayQuery = findViewById(R.id.btn_sub_pay_query);
        btnInit.setOnClickListener(this);
        btnSmsPay.setOnClickListener(this);
        btnOnlinePay.setOnClickListener(this);
        btnSubPay.setOnClickListener(this);
        btnSubPayQuery.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_init:
                initPaySdk();
                break;
            case R.id.btn_sms_pay:
                startPay(StartPayEntity.PAY_MODE_SMS);
                break;
            case R.id.btn_online_pay:
                startPay(StartPayEntity.PAY_MODE_ONLINE);
                break;
            case R.id.btn_sub_pay:
                startOnLineSubPay();
                break;
            case R.id.btn_sub_pay_query:
                queryOnlineSubOrderStatus(subOrderNum);
                break;
        }
    }

    /**
     * 初始化支付sdk
     */
    private void initPaySdk() {
        PaySDKManager.getsInstance().initAriesPay(MainActivity.this, AP_ID, CP_ID, API_KEY,
                new InitResultCallBack() {
                    @Override
                    public void onSuccess(List<SupportPayInfoEntity> list, boolean supportOnlinePay, CountryCurrencyData countryCurrencyData) {
                        Toast.makeText(MainActivity.this, "初始化成功", Toast.LENGTH_SHORT).show();

                        //List<SupportPayInfoEntity> list 短代和线上订阅商品展示金额集合(返回什么展示什么、只需要关注金额price)
                        if (list != null && list.size() > 0) {
                            for (SupportPayInfoEntity supportPayInfoEntity : list) {
                                //在线订阅金额集合subPriceList
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    subPriceList = supportPayInfoEntity.priceEntities.stream()
                                            .filter((PriceEntity priceEntity) -> priceEntity.isSupportSub())
                                            .collect(Collectors.toList());
                                } else {

                                }
                                //短代金额集合smsPriceList
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    smsPriceList = supportPayInfoEntity.priceEntities.stream()
                                            .filter((PriceEntity priceEntity) -> priceEntity.isSupportNormal())
                                            .collect(Collectors.toList());
                                } else {

                                }
                            }
                        }

                        //-supportOnlinePay 为true 代表支持在线支付功能
                        //-countryCurrencyData 代表国家相关信息
                        // 只需要关注（支付要用到的参数）-国家code-countryCurrencyData.countryCode、支付币种countryCurrencyData.currency
                        // 线上支付最小金额countryCurrencyData.minAmount、线上支付最大金额countryCurrencyData.maxAmount
                        mSupportOnlinePay = supportOnlinePay;
                        mCountryCurrencyData = countryCurrencyData;

                        updateUI();

                    }

                    @Override
                    public void onFail(int code) {
                        btnInit.setText("初始化：失败");
                    }
                });
    }

    private void updateUI() {
        btnInit.setText("初始化：成功");

        if (subPriceList == null || subPriceList.size() == 0) {
            btnSubPay.setText("在线订阅支付：不支持");
        } else {
            btnSubPay.setText("在线订阅支付：支持");
        }
        if (smsPriceList == null || smsPriceList.size() == 0) {
            btnSmsPay.setText("短代支付：不支持");
        } else {
            btnSmsPay.setText("短代支付：支持");
        }
        if (!mSupportOnlinePay) {
            btnOnlinePay.setText("在线支付：不支持");
            btnOnlinePay.setClickable(false);
        } else {
            btnOnlinePay.setText("在线支付：[" + mCountryCurrencyData.minAmount + "," + mCountryCurrencyData.maxAmount + "]");
            btnOnlinePay.setClickable(true);
        }
    }
    private void updatePayResultUI(String result){
        tvPayRresult.setText(result);
    }

    /**
     * 短代支付、在线支付
     * @param payMode 支付mode
     */
    private void startPay(int payMode) {
        //非必填字段可以传空
        StartPayEntity startPayEntity = new StartPayEntity();
        //短代初始化返回金额、线上支付支持的最小最大金额之间任意值
        if (payMode == StartPayEntity.PAY_MODE_SMS) {
            if (smsPriceList != null && smsPriceList.size() > 0) {
                startPayEntity.amount = smsPriceList.get(0).price;
            }
        } else if (startPayEntity.payMode == StartPayEntity.PAY_MODE_ONLINE) {
            if (mCountryCurrencyData != null) {
                double minAmount = mCountryCurrencyData.minAmount;
                double maxAmount = mCountryCurrencyData.maxAmount;
                startPayEntity.amount = minAmount + (new Random().nextDouble() * (maxAmount - minAmount));
            }
        }
        if (mCountryCurrencyData != null) {
            startPayEntity.countryCode = mCountryCurrencyData.countryCode;//初始化返回countryCurrencyData对象
            startPayEntity.currency = mCountryCurrencyData.currency;
        }
        startPayEntity.matchDown = true;
        startPayEntity.orderNum = String.valueOf(System.currentTimeMillis());//商户订单号-自己维护
        //StartPayEntity.PAY_MODE_ONLINE: 在线支付，StartPayEntity.PAY_MODE_SMS：短代支付，StartPayEntity.PAY_MODE_ALL:全部支付方式
        startPayEntity.payMode = payMode;
        startPayEntity.type = 0;
        startPayEntity.adjustMode = BigDecimal.ROUND_UP;//在线支付自行设置
        if (startPayEntity.payMode == StartPayEntity.PAY_MODE_SMS) {
            startPayEntity.adjustMode = -1;//短代设置为-1
        }
        startPayEntity.serviceConfigPriority = true;
        try {
            //payOrderNum 支付订单号
            String payOrderNum = PaySDKManager.getsInstance().startPay(MainActivity.this, startPayEntity, new StartPayCallBack() {
                @Override
                public void onOrderCreated(OrderEntity orderEntity) {
                    //订单创建成功，进入开始支付，如果后续因为崩溃等原因没有收到结果，可以根据这 里的订单号查询订单结果
                    Toast.makeText(MainActivity.this, "订单已创建", Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onPaySuccess(OrderEntity orderEntity) {
                    //支付成功
                    Toast.makeText(MainActivity.this, "支付成功", Toast.LENGTH_SHORT).show();
                    updatePayResultUI("支付成功");
                }

                @Override
                public void onPaying(OrderEntity orderEntity) {
                    //本地查询超时，后续游戏去服务端确认
                    Toast.makeText(MainActivity.this, "支付中", Toast.LENGTH_SHORT).show();
                    updatePayResultUI("支付中:"+"订单号："+orderEntity.orderNum);

                }

                @Override
                public void onPayFail(int code, OrderEntity orderEntity) {
                    //支付失败
                    Toast.makeText(MainActivity.this, "支付失败 code:" + code, Toast.LENGTH_SHORT).show();
                    updatePayResultUI("支付失败 code:" + code + "  订单号："+orderEntity.orderNum);

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 在线订阅支付
     */
    private void startOnLineSubPay() {
        //非必填字段可以传空
        StartPayEntity startPayEntity = new StartPayEntity();
        startPayEntity.amount = 10;//初始化返回金额
        if (mCountryCurrencyData != null) {
            startPayEntity.countryCode = mCountryCurrencyData.countryCode;//初始化返回countryCurrencyData对象
            startPayEntity.currency = mCountryCurrencyData.currency;
        }
        startPayEntity.orderNum = String.valueOf(System.currentTimeMillis());//商户订单号-自己维护
        startPayEntity.cycle = "";//传空
        startPayEntity.email = "";//取不到-传空
        startPayEntity.phone = "";//取不到-传空
        startPayEntity.payMethod = "";//取不到-传空
        startPayEntity.orderDescription = "order description";//必传 不能为空 内容cp主自己定义
        startPayEntity.netPaySp = "";//传空
        try {
             subOrderNum = PaySDKManager.getsInstance().startOnLineSubPay(MainActivity.this, startPayEntity, new StartPayCallBack() {
                @Override
                public void onOrderCreated(OrderEntity orderEntity) {
                    //订单创建成功，进入开始支付，如果后续因为崩溃等原因没有收到结果，可以根据这 里的订单号查询订单结果
                }

                @Override
                public void onPaySuccess(OrderEntity orderEntity) {
                    //支付成功
                    updatePayResultUI("支付成功");

                }

                @Override
                public void onPaying(OrderEntity orderEntity) {
                    //本地查询超时，后续游戏去服务端确认
                    updatePayResultUI("支付中" +  "订单号："+orderEntity.orderNum);

                }

                @Override
                public void onPayFail(int code, OrderEntity orderEntity) {
                    //支付失败
                    updatePayResultUI("支付失败 code:" + code + "订单号："+orderEntity.orderNum);

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *在线订阅订单状态查询
     * @param orderNum
     */
    private void queryOnlineSubOrderStatus(String orderNum){
        if(TextUtils.isEmpty(orderNum)){
            Toast.makeText(MainActivity.this,"订单号不能为空",Toast.LENGTH_SHORT).show();
        }
        PaySDKManager.getsInstance().queryOnlineSubOrderStatus(orderNum, API_KEY, CP_ID, new OrderQuery() {
            @Override
            public void onSuccess(OrderResultEntity orderQueryEntity){
                //订阅成功
                updatePayResultUI("订阅成功");
            }

            @Override
            public void orderError(OrderResultEntity orderQueryEntity, String code, String errorMessage) {
                //网络错误导致-订阅失败
                updatePayResultUI("网络错误导致-订阅失败");
            }

            @Override
            public void onPaying(OrderResultEntity orderQueryEntity) {
                //订阅中-未确认结果
                updatePayResultUI("订阅中-未确认结果");

            }

            @Override
            public void orderFail(OrderResultEntity orderQueryEntity) {
                //订阅失败
                updatePayResultUI("订阅失败");

            }
        });
    }
}