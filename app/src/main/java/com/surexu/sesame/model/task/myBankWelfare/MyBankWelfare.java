package com.surexu.sesame.model.task.myBankWelfare;

import com.surexu.sesame.data.ModelFields;
import com.surexu.sesame.data.modelFieldExt.BooleanModelField;
import com.surexu.sesame.model.task.rewardSupport.IsolatedRewardTask;
import com.surexu.sesame.util.Log;

public final class MyBankWelfare extends IsolatedRewardTask {
    private BooleanModelField signIn;

    @Override
    public String getName() {
        return "网商银行福利签到";
    }

    @Override
    protected void addFields(ModelFields modelFields) {
        BooleanModelField booleanModelField = new BooleanModelField("signIn", "会员签到", false);
        this.signIn = booleanModelField;
        modelFields.addField(booleanModelField);
    }

    @Override
    protected void execute(IsolatedRewardTask.Run run) throws Exception {
        if (!this.signIn.getValue().booleanValue()) {
            Log.record(getName() + "：签到开关未开启");
            return;
        }
        if (run.onceToday("signinPlay", "com.alipay.loanpromoweb.member.play.signinPlay", "[{\"channel\":\"miniApp\",\"needMultiple\":false,\"operation\":\"signApply\",\"playId\":\"PLAY100177545\"}]", new IsolatedRewardTask.Allowed() {
            @Override
            public final boolean isAllowed() {
                return MyBankWelfare.this.signIn.getValue().booleanValue();
            }
        }) != null) {
            Log.record(getName() + "：签到接口返回成功");
        }
    }
}
