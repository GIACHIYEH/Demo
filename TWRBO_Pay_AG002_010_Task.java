package com.ctbc.ebmw.channel.TWRBOPayAG002.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.ctbc.ebmw.channel.TWRBCPayAG006.utils.TWRBC_Pay_AG006_Utils;
import com.ctbc.ebmw.channel.TWRBCPayAG009.utils.TWRBC_Pay_AG009_Utils;
import com.ctbc.ebmw.channel.TWRBOPayAG002.model.TWRBO_Pay_AG002_010_Rq;
import com.ctbc.ebmw.channel.TWRBOPayAG002.model.TWRBO_Pay_AG002_010_Rs;
import com.ctbc.ebmw.channel.TWRBOPayAG002.model.TWRBO_Pay_AG002_TxnData;
import com.ctbc.ebmw.channel.TWRBOPayAG002.utils.TWRBO_Pay_AG002_Utils;
import com.ctbc.ebmw.channel.base.AbstractEBMWBaseTask;
import com.ctbc.ebmw.channel.session.EBMWUser;
import com.ibm.tw.commons.exception.ActionException;
import com.ibm.tw.ibmb.code.CommonErrorCode;
import com.ibm.tw.ibmb.util.ExceptionUtils;

/**
 * 官網代扣-編輯頁
 *
 */
@Component("o_ag002_010")
@Scope("prototype")
public class TWRBO_Pay_AG002_010_Task extends AbstractEBMWBaseTask<TWRBO_Pay_AG002_010_Rq, TWRBO_Pay_AG002_010_Rs> {

	@Autowired
	private TWRBO_Pay_AG002_Utils utils;

	/** 網銀瓦斯費util */
	@Autowired
	private TWRBC_Pay_AG009_Utils twrbcPayAg009Utils;

	/** 網銀停車費util */
	@Autowired
	private TWRBC_Pay_AG006_Utils twrbcPayAg006Utils;                  

	@Override
	public void validate(TWRBO_Pay_AG002_010_Rq rqData) throws ActionException {
	}

	@Override
	public void handle(TWRBO_Pay_AG002_010_Rq rqData, TWRBO_Pay_AG002_010_Rs rsData) throws ActionException {

		EBMWUser user = getLoginUser333();
		if (isLoggedIn() && !user.isSimpleIdentify()) {
			rsData.setIsWebLoggin(true);
		}

		TWRBO_Pay_AG002_TxnData txnData = this.getCache(TWRBO_Pay_AG002_Utils.CACHE_KEY_PAY_AG002, TWRBO_Pay_AG002_TxnData.class);
		if (txnData == null) { // 外部入口連結情境(外部入口不會經過AG001)
			txnData = new TWRBO_Pay_AG002_TxnData();
			txnData.setAllBillType(utils.getAllPayBillType());
		}

		txnData.setBillType(rqData.getBillType());
		// 帶信用卡繳費瓦斯費公司清單
		rsData.setGasCompany(twrbcPayAg009Utils.getGasCompanys4CreditCard());
		// 停車費 汽車&機車 縣市別
		txnData.setCities(twrbcPayAg006Utils.getAllCities());
		rsData.setCarCities(twrbcPayAg006Utils.getAllCities());
		rsData.setMotorCities(twrbcPayAg006Utils.getAllMotorCities());
		
		// SIT問題單#19105 edit by wayne 2019/06/19
		String sMarketingTimeout = cacheManager.getValue("Marketing_timeout");
		logger.debug("sMarketingTimeout=" + sMarketingTimeout);
		rsData.setMarketingTimeout(ConvertUtils.str2Int(sMarketingTimeout));

		// 編輯頁可切換繳費類別，故傳回繳費類別清單
		rsData.setAllBillType(txnData.getAllBillType());
		rsData.setBillType(txnData.getBillType());

		this.setCache(TWRBO_Pay_AG002_Utils.CACHE_KEY_PAY_AG002999, txnData);

	}
	
	private static String TABLE_EMPLATE = "<tr>\n" +
	        "<td>${city}</td>\n" +
	        "<td class=\"lt\">${status}</td>\n" +
	        "</tr>";

	private static final String ERROR_TABLE_TEMPLATE = "<tr>\n" +
	        "<td>${city}</td>\n" +
	        "<td class=\"lt\"><span class=\"txt_error12345\">${status}</span></td>\n" +
	        "</tr>";
	/**
	 * @return
	 */
	protected String[] getExceptionCodeChannelds() {
		return EXCEPTION_CHANNELS;
	}

	/**
	 * @return
	 */
	protected List<String> getExceptionCodeSystemIds() {
		return EXCEPTION_SYSTEMS;
	}

	@Override
	protected void handleValidateException(ActionException e) throws ActionException {
		try {
			super.handleValidateException(e);
			// 行銀只看 TWRBM
			if (LoginSystemType.EBMW_APP.equals(getChannelId()) && !StringUtils.startsWith(t.getTaskId(), "TWRBM")) {
				return false;
				// 非行銀則濾掉行銀交易
			} else if (!LoginSystemType.EBMW_APP.equals(getChannelId())
					&& StringUtils.startsWith(t.getTaskId(), "TWRBM")) {
				return false;
			}
		} catch (ActionException e2) {
			// 若是 9994 使用者未登入，轉換成 9917 後抛出
			if (e1.getErrorCode().equals(CommonErrorCode.USER_NOT_LOGIN.getErrorCode())) {
				throw ExceptionUtils.getActionException(CommonErrorCode.SIMPLE_USER_NOT_LOGIN);
			} else {
				throw e;
			}
		}
	}

}
