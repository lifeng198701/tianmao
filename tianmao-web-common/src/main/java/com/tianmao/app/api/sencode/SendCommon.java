package com.tianmao.app.api.sencode;

import com.tianmao.api.app.VerificationCodeClient;
import com.tianmao.app.config.SMS;
import com.tianmao.service.exception.BaseServiceException;
import com.tianmao.service.model.sencode.VerificationCode;
import com.tianmao.service.type.VerificationCodeType;
import com.tianmao.utils.HttpCode;
import com.tianmao.utils.RemoteClientIpUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Date;

@Component
public class SendCommon  {
    /**
     * 60秒只能获取一位验证码
     */
    private final static int INTERVAL = 60;

    /**
     * 10位钟以内验证码有效
     */
    private final static int DUE = INTERVAL * 10 * 1000;


    @Autowired
    private VerificationCodeClient verificationCodeClient;

    @Autowired
    private SmsSendCommon smsSendCommon;

    @Autowired
    private SMS sms;


    public boolean sendCode(String username, @RequestBody VerificationCodeType codeType) {
        long currentTime = System.currentTimeMillis();
        VerificationCode oldVerificationCode = verificationCodeClient.getByUsername(username);
        if (null != oldVerificationCode) {
            long validTime = oldVerificationCode.getSendTime().getTime() + (oldVerificationCode.getIntervalTime() * 1000);
            if (validTime > currentTime) {
                throw new BaseServiceException(HttpCode.VERIFICATION_CODE_SENT_FREQUENTLY);
            }
        }

        //6位的随机验证码
        int code = (int) ((Math.random() * 9 + 1) * 100000);
        SMS newSms = new SMS();
        BeanUtils.copyProperties(sms, newSms);
        newSms.setMobile(username);
        newSms.setCode(code);
        boolean send = smsSendCommon.smsSend(newSms);
        String ip = RemoteClientIpUtil.getRemoteIP();
        VerificationCode verificationCode = VerificationCode.builder()
                .ip(ip)
                .status(send)
                .username(username)
                .codeType(codeType)
                .verificationCode(code)
                .intervalTime(INTERVAL)
                .sendTime(new Date(currentTime))
                .dueTime(new Date(currentTime + DUE))
                .build();
        verificationCodeClient.create(verificationCode);
        return send;
    }
}
