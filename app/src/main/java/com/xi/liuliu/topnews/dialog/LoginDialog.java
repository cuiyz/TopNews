package com.xi.liuliu.topnews.dialog;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.xi.liuliu.topnews.R;
import com.xi.liuliu.topnews.activity.UserAgreementActivity;
import com.xi.liuliu.topnews.event.LoginEvent;
import com.xi.liuliu.topnews.event.WeiboLoginEvent;
import com.xi.liuliu.topnews.http.HttpClient;
import com.xi.liuliu.topnews.utils.CheckPhone;
import com.xi.liuliu.topnews.utils.SharedPrefUtil;
import com.xi.liuliu.topnews.utils.ToastUtil;

import java.io.IOException;

import de.greenrobot.event.EventBus;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by liuliu on 2017/6/27.
 */

public class LoginDialog implements View.OnClickListener {
    private static final String TAG = "LoginDialog";
    private Context mContext;
    private DialogView mDialogView;
    private static final int LOGIN_WITH_OUT_PASS = 1001;
    private static final int LOGIN_WITH_PASS = 1002;
    private int mCurrentLoginWay = LOGIN_WITH_OUT_PASS;
    private Button mClose;
    private TextView mLoginWay;
    private TextView mLoginHint;
    private EditText mInput;
    private TextView mFindPassword;
    private TextView mSendIdentifyCode;
    private TextView mLoginWayTitle;
    private TextView mEntry;
    private EditText mPhoneNumber;
    private SendingDialog mSendingDialog;
    private ImageView mWeiboBtn;
    private ImageView mWeichatBtn;
    private ImageView mQQBtn;
    private CheckBox mUserAgreementCb;
    private TextView mUserAgreementTv;

    public LoginDialog(Context context) {
        mContext = context;
        init();
    }

    private void init() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_login, null);
        mClose = (Button) view.findViewById(R.id.dialog_login_close);
        mClose.setOnClickListener(this);
        mLoginWay = (TextView) view.findViewById(R.id.dialog_login_way);
        mLoginWay.setOnClickListener(this);
        mLoginHint = (TextView) view.findViewById(R.id.dialog_login_hint);
        mInput = (EditText) view.findViewById(R.id.dialog_login_input);
        mFindPassword = (TextView) view.findViewById(R.id.dialog_login_find_password);
        mSendIdentifyCode = (TextView) view.findViewById(R.id.dialog_login_send_identify_code);
        mSendIdentifyCode.setOnClickListener(this);
        mLoginWayTitle = (TextView) view.findViewById(R.id.dialog_login_title);
        mEntry = (TextView) view.findViewById(R.id.dialog_login_entry);
        mEntry.setOnClickListener(this);
        mPhoneNumber = (EditText) view.findViewById(R.id.dialog_login_input_phone_number);
        mWeiboBtn = (ImageView) view.findViewById(R.id.wei_bo_login_dialog);
        mWeiboBtn.setOnClickListener(this);
        mWeichatBtn = (ImageView) view.findViewById(R.id.we_chat_login_dialog);
        mWeichatBtn.setOnClickListener(this);
        mQQBtn = (ImageView) view.findViewById(R.id.qq_login_dialog);
        mQQBtn.setOnClickListener(this);
        mUserAgreementCb = (CheckBox) view.findViewById(R.id.check_box_login_dialog_user_agreement);
        mUserAgreementCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mUserAgreementTv.setTextColor(mContext.getResources().getColor(R.color.login_dialog_user_agreement_checked));
                } else {
                    mUserAgreementTv.setTextColor(mContext.getResources().getColor(R.color.login_dialog_user_agreement_un_checked));
                }
            }
        });
        mUserAgreementTv = (TextView) view.findViewById(R.id.user_agreement_login_dialog);
        mUserAgreementTv.setOnClickListener(this);
        mDialogView = new DialogView(mContext, view, R.style.share_dialog_animation);
        mDialogView.setFullScreen(true);
        mDialogView.setCanceledOnTouchOutside(false);
        mDialogView.setCancelable(true);
        mDialogView.setGravity(Gravity.BOTTOM);
    }

    public void show() {
        if (mDialogView != null) {
            mDialogView.showDialog();
        }
    }

    public void dismiss() {
        if (mDialogView != null) {
            mDialogView.dismissDialog();
        }
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.dialog_login_close:
                dismiss();
                break;
            case R.id.dialog_login_way:
                changeLoginWay();
                break;
            case R.id.dialog_login_entry:
                login();
                break;
            case R.id.dialog_login_send_identify_code:
                sendSmsCode();
                break;
            case R.id.wei_bo_login_dialog:
                EventBus.getDefault().post(new WeiboLoginEvent());
                dismiss();
                break;
            case R.id.we_chat_login_dialog:

                break;
            case R.id.qq_login_dialog:

                break;
            case R.id.user_agreement_login_dialog:
                Intent intent = new Intent(mContext, UserAgreementActivity.class);
                mContext.startActivity(intent);
                break;
        }
    }

    private void sendSmsCode() {
        if (!isPhoneNumberLegal(mPhoneNumber.getText().toString().trim())) {
            ToastUtil.toastInCenter(mContext, R.string.login_dialog_toast_phone_number_illegal);
        } else {
            if (mSendingDialog == null) {
                mSendingDialog = new SendingDialog(mContext, false);
            }
            mSendingDialog.show();
            new HttpClient().setCallback(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (mSendingDialog != null) {
                                mSendingDialog.dissmiss();
                            }
                            ToastUtil.toastInCenter(mContext, R.string.login_dialog_toast_has_send_sms_code);
                        }
                    });

                }
            }).requestLoginSmsCode(mPhoneNumber.getText().toString().trim());
        }

    }

    private void login() {
        if (isSmsCodeLegal(mInput.getText().toString().trim())) {
            if (mSendingDialog == null) {
                mSendingDialog = new SendingDialog(mContext, false);
            }
            mSendingDialog.show();
            new HttpClient().setCallback(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (mSendingDialog != null) {
                                mSendingDialog.dissmiss();
                            }
                            ToastUtil.toastInCenter(mContext, R.string.login_dialog_toast_sms_code_error);
                        }
                    });

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            String phoneNumber = mPhoneNumber.getText().toString().trim();
                            SharedPrefUtil.getInstance(mContext).savaLoginStateWithPhone(phoneNumber);
                            if (mSendingDialog != null) {
                                mSendingDialog.dissmiss();
                            }
                            EventBus.getDefault().post(new LoginEvent(LoginEvent.LOGIN_PHONE, "手机用户" + phoneNumber, null));
                            dismiss();
                        }
                    });
                }
            }).verifySmsCode(mInput.getText().toString().trim(), mPhoneNumber.getText().toString().trim());
        } else {
            ToastUtil.toastInCenter(mContext, R.string.login_dialog_toast_sms_code_error);
        }
    }

    private void changeLoginWay() {
        if (mCurrentLoginWay == LOGIN_WITH_OUT_PASS) {
            mCurrentLoginWay = LOGIN_WITH_PASS;
            mLoginWay.setText(R.string.login_with_not_password);
            mLoginHint.setVisibility(View.INVISIBLE);
            mInput.setHint(R.string.logint_dialog_password);
            mInput.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
            mFindPassword.setVisibility(View.VISIBLE);
            mSendIdentifyCode.setVisibility(View.GONE);
            mLoginWayTitle.setText(R.string.login_with_count_password);
        } else {
            mCurrentLoginWay = LOGIN_WITH_OUT_PASS;
            mLoginWay.setText(R.string.login_with_count_password);
            mLoginHint.setVisibility(View.VISIBLE);
            mInput.setHint(R.string.login_input_identifying_code);
            mFindPassword.setVisibility(View.GONE);
            mSendIdentifyCode.setVisibility(View.VISIBLE);
            mLoginWayTitle.setText(R.string.login_dialog_with_identify_code_title);
        }
    }

    private boolean isPhoneNumberLegal(String phoneNumber) {
        return CheckPhone.isPhoneNum(phoneNumber);
    }

    /**
     * 判断验证码是否是6位数字
     *
     * @param smsCode 验证码
     * @return true代码验证码是6位数字
     */
    private boolean isSmsCodeLegal(String smsCode) {
        if (TextUtils.isEmpty(smsCode)) {
            return false;
        }
        if (smsCode.matches("\\d{6}")) {
            return true;
        }
        return false;
    }
}
