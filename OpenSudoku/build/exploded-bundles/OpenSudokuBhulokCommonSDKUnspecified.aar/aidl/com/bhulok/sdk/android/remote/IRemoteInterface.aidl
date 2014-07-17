
package com.bhulok.sdk.android.remote;

import com.bhulok.sdk.android.AppContext;
import com.bhulok.sdk.android.remote.IRemoteInterfaceCallback;

interface IRemoteInterface {

	/******************
	 * Bootstrap methods
	 ******************/
	 
	void appAuth(String appKeyDigest, String payload, in AppContext appContext, IRemoteInterfaceCallback cb);
	
	/******************
	 * Product enabling methods
	 ******************/

	boolean isPurchased(String appKeyDigest, String productId);

	/******************
	 * Consumption methods
	 ******************/

	void canConsume(String appKeyDigest, String productId, int units,IRemoteInterfaceCallback cb);
	
	void consume(String appKeyDigest, String productId, int units,IRemoteInterfaceCallback cb);
	
	void procure(String appKeyDigest, String productId, int units,IRemoteInterfaceCallback cb);
	
	/******************
	 * Stats Widget methods
	 ******************/
	
	void showStats(String appKeyDigest, String productId);
}
