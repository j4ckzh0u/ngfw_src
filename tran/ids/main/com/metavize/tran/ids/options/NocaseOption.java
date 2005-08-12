package com.metavize.tran.ids.options;

import com.metavize.tran.ids.IDSRuleSignature;

public class NocaseOption extends IDSOption {

	public NocaseOption(IDSRuleSignature signature, String params) {
		super(signature, params);
		IDSOption option = signature.getOption("ContentOption");
		if(option != null) {
			ContentOption content = (ContentOption) option;
			content.setNoCase();
		}
				
	}

	public boolean run() {
		return true;
	}
}
