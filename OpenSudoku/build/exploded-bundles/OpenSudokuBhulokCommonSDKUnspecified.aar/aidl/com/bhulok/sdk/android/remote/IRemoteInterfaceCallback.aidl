package com.bhulok.sdk.android.remote;

oneway interface IRemoteInterfaceCallback {
    /**
     * Called when the service has a new value for you.
     */
    void onComplete(int resultCode, String message, String data);
}
