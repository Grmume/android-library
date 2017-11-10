/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2016 ownCloud GmbH.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.common;


import java.io.IOException;

import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.NetworkUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.Uri;

/**
 * OwnCloud Account
 * 
 * @author David A. Velasco
 */
public class OwnCloudAccount {

    private Uri mBaseUri;

    private Uri mLocalBaseUri;

    private String mLocalWifiSsid;

    private Boolean mUseLocalUrl;
    
    private OwnCloudCredentials mCredentials;

    private String mDisplayName;
    
    private String mSavedAccountName;

    private Account mSavedAccount;


    /**
     * Constructor for already saved OC accounts.
     *
     * Do not use for anonymous credentials.
     */
    public OwnCloudAccount(Account savedAccount, Context context) throws AccountNotFoundException {
        if (savedAccount == null) {
            throw new IllegalArgumentException("Parameter 'savedAccount' cannot be null");
        }

        if (context == null) {
            throw new IllegalArgumentException("Parameter 'context' cannot be null");
        }

        mSavedAccount = savedAccount;
        mSavedAccountName = savedAccount.name;
        mCredentials = null;    // load of credentials is delayed

        AccountManager ama = AccountManager.get(context.getApplicationContext());
        String baseUrl = ama.getUserData(mSavedAccount, AccountUtils.Constants.KEY_OC_BASE_URL);
        if (baseUrl == null ) {
            throw new AccountNotFoundException(mSavedAccount, "Account not found", null);
        }
        mBaseUri = Uri.parse(AccountUtils.getBaseUrlForAccount(context, mSavedAccount));
        mLocalBaseUri = Uri.parse(AccountUtils.getLocalBaseUrlForAccount(context, mSavedAccount));
        mLocalWifiSsid = AccountUtils.getLocalWifiSsidForAccount(context, mSavedAccount);
        mUseLocalUrl = AccountUtils.useLocalUrlForAccount(context, mSavedAccount);

        mDisplayName = ama.getUserData(mSavedAccount, AccountUtils.Constants.KEY_DISPLAY_NAME);
    }

    // TODO CHeck if the following constructor is used in a place where the overloaded following methods are desired
    /**
     * Constructor for non yet saved OC accounts.
     *
     * @param baseUri           URI to the OC server to get access to.
     * @param credentials       Credentials to authenticate in the server. NULL is valid for anonymous credentials.
     */
    public OwnCloudAccount(Uri baseUri, OwnCloudCredentials credentials) {
        if (baseUri == null) {
            throw new IllegalArgumentException("Parameter 'baseUri' cannot be null");
        }
        mSavedAccount = null;
        mSavedAccountName = null;
        mBaseUri = baseUri;
        mCredentials = credentials != null ?
            credentials : OwnCloudCredentialsFactory.getAnonymousCredentials();
        String username = mCredentials.getUsername();
        if (username != null) {
            mSavedAccountName = AccountUtils.buildAccountName(mBaseUri, username);
        }
    }

    /**
     * Constructor for non yet saved OC accounts.
     *
     * @param baseUri           URI to the OC server to get access to.
     * @param credentials       Credentials to authenticate in the server. NULL is valid for anonymous credentials.
     */
    public OwnCloudAccount(Uri baseUri, Uri localBaseUri, String wifiSsid, OwnCloudCredentials credentials) {
        if (baseUri == null) {
            throw new IllegalArgumentException("Parameter 'baseUri' cannot be null");
        }
        mSavedAccount = null;
        mSavedAccountName = null;
        mBaseUri = baseUri;
        mLocalBaseUri = localBaseUri;
        mUseLocalUrl = true;
        mLocalWifiSsid = wifiSsid;
        mCredentials = credentials != null ?
                credentials : OwnCloudCredentialsFactory.getAnonymousCredentials();
        String username = mCredentials.getUsername();
        if (username != null) {
            mSavedAccountName = AccountUtils.buildAccountName(mBaseUri, username);
        }
    }


    /**
     * Method for deferred load of account attributes from AccountManager
     *
     * @param context
     * @throws AccountNotFoundException
     * @throws AuthenticatorException
     * @throws IOException
     * @throws OperationCanceledException
     */
    public void loadCredentials(Context context)
        throws AccountNotFoundException, AuthenticatorException,
                IOException, OperationCanceledException {

        if (context == null) {
            throw new IllegalArgumentException("Parameter 'context' cannot be null");
        }

        if (mSavedAccount != null) {
            mCredentials = AccountUtils.getCredentialsForAccount(context, mSavedAccount);
        }
    }

    public Uri getBaseUri() {
        return mBaseUri;
    }

    /**
     * Return the Base Uri of the server when connected to some random network but the
     * local base uri when connected to the local wifi.
     * @param context
     * @return
     */
    public Uri getAdjustedUri(Context context)
    {
        if(NetworkUtils.currentlyConnectedToSsid(mLocalWifiSsid,context)) {
            return mLocalBaseUri;
        } else {
            return mBaseUri;
        }
    }

    public Uri getLocalBaseUri() { return mLocalBaseUri; }

    public String getLocalWifiSsid() { return mLocalWifiSsid; }

    public Boolean useLocalUrl() { return mUseLocalUrl; }
            
    public OwnCloudCredentials getCredentials() {
        return mCredentials;
    }
    
    public String getName() {
    	return mSavedAccountName;
    }

    public Account getSavedAccount() {
        return mSavedAccount;
    }

    public String getDisplayName() {
        if (mDisplayName != null && mDisplayName.length() > 0) {
            return mDisplayName;
        } else if (mCredentials != null) {
            return mCredentials.getUsername();
        } else if (mSavedAccount != null) {
            return AccountUtils.getUsernameForAccount(mSavedAccount);
        } else {
            return null;
        }
    }

}