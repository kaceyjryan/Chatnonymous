/*
 * Copyright (c) 2011, AllSeen Alliance. All rights reserved.
 *
 *    Permission to use, copy, modify, and/or distribute this software for any
 *    purpose with or without fee is hereby granted, provided that the above
 *    copyright notice and this permission notice appear in all copies.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *    WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *    MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *    ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *    WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *    ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *    OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package org.alljoyn.bus.sample.chat;

import org.alljoyn.bus.sample.chat.ChatApplication;
import org.alljoyn.bus.sample.chat.Observable;
import org.alljoyn.bus.sample.chat.Observer;
import org.alljoyn.bus.sample.chat.DialogBuilder;

import android.os.Handler;
import android.os.Message;
import android.os.Bundle;

import android.app.Activity;
import android.app.Dialog;

import android.graphics.Color;
import android.graphics.PorterDuff;

import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import android.util.Log;

import java.util.List;

public class UseActivity extends Activity implements Observer {
    private static final String TAG = "chat.UseActivity";
    private static final Boolean DEBUG_MODE = false;
    
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.use);
                
        mHistoryList = new ArrayAdapter<String>(this, android.R.layout.test_list_item);
        ListView hlv = (ListView) findViewById(R.id.useHistoryList);
        hlv.setAdapter(mHistoryList);
        
        EditText messageBox = (EditText)findViewById(R.id.useMessage);
        messageBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                	String message = view.getText().toString();
                    Log.i(TAG, "useMessage.onEditorAction(): got message " + message + ")");
    	            mChatApplication.newLocalUserMessage(message);
    	            view.setText("");
                }
                return true;
            }
        });
                
        mJoinButton = (Button)findViewById(R.id.useJoin);
        mJoinButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_JOIN_ID);
        	}
        });

        mLeaveButton = (Button)findViewById(R.id.useLeave);
        mLeaveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_LEAVE_ID);
            }
        });
        
        mUseChannelName = (TextView)findViewById(R.id.useChannelName);
        mUseChannelStatus = (TextView)findViewById(R.id.useChannelStatus);
        
        mHostChannelName = (TextView)findViewById(R.id.hChannelName);
        mHostChannelStatus = (TextView)findViewById(R.id.hChannelStatus); 
        
        /*
         * Keep a pointer to the Android Appliation class around.  We use this
         * as the Model for our MVC-based application.    Whenever we are started
         * we need to "check in" with the application so it can ensure that our
         * required services are running.
         */
        mChatApplication = (ChatApplication)getApplication();
        mChatApplication.checkin();
        
        /*
         * Call down into the model to get its current state.  Since the model
         * outlives its Activities, this may actually be a lot of state and not
         * just empty.
         */
        updateChannelState();
        updateHistory();
        
        /*
         * Now that we're all ready to go, we are ready to accept notifications
         * from other components.
         */
        mChatApplication.addObserver(this);
        
        mQuitButton = (Button)findViewById(R.id.useQuit);
        mQuitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mChatApplication.quit();
            }
        });
        
        mRandomChatButton = (Button)findViewById(R.id.randomChat);
        setRandomChatButtonDefaults();
        
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        mProgressBar.setIndeterminate(true);
        
        if(DEBUG_MODE) {
        	TableLayout debugInfo = (TableLayout)findViewById(R.id.debugInfo);
        	LinearLayout debugButtons = (LinearLayout)findViewById(R.id.debugButtons);
        	
        	debugInfo.setVisibility(0);
        	debugButtons.setVisibility(0);
        }
        
    }
    
	public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        mChatApplication = (ChatApplication)getApplication();
        mChatApplication.deleteObserver(this);
    	super.onDestroy();
 	}
    
    public static final int DIALOG_JOIN_ID = 0;
    public static final int DIALOG_LEAVE_ID = 1;
    public static final int DIALOG_ALLJOYN_ERROR_ID = 2;

    protected Dialog onCreateDialog(int id) {
    	Log.i(TAG, "onCreateDialog()");
        Dialog result = null;
        switch(id) {
        case DIALOG_JOIN_ID:
	        { 
	        	DialogBuilder builder = new DialogBuilder();
	        	result = builder.createUseJoinDialog(this, mChatApplication);
	        }        	
        	break;
        case DIALOG_LEAVE_ID:
	        { 
	        	DialogBuilder builder = new DialogBuilder();
	        	result = builder.createUseLeaveDialog(this, mChatApplication);
	        }
	        break;
        case DIALOG_ALLJOYN_ERROR_ID:
	        { 
	        	DialogBuilder builder = new DialogBuilder();
	        	result = builder.createAllJoynErrorDialog(this, mChatApplication);
	        }
	        break;	        
        }
        return result;
    }
    
    public synchronized void update(Observable o, Object arg) {
        Log.i(TAG, "update(" + arg + ")");
        String qualifier = (String)arg;
        
        if (qualifier.equals(ChatApplication.APPLICATION_QUIT_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
            mHandler.sendMessage(message);
        }
        
        if (qualifier.equals(ChatApplication.HISTORY_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_HISTORY_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }
        
        if (qualifier.equals(ChatApplication.USE_CHANNEL_STATE_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_CHANNEL_STATE_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }
        
        if (qualifier.equals(ChatApplication.HOST_CHANNEL_STATE_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_CHANNEL_STATE_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }
        
        if (qualifier.equals(ChatApplication.ALLJOYN_ERROR_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_ALLJOYN_ERROR_EVENT);
            mHandler.sendMessage(message);
        }
        if (qualifier.equals(ChatApplication.CONNECTION_ATTEMPTED_EVENT)) {
        	Message message = mHandler.obtainMessage(HANDLE_CONNECTION_ATTEMPTED_EVENT);
        	mHandler.sendMessage(message);
        }
        if (qualifier.equals(ChatApplication.CONNECTION_ESTABLISHED_EVENT)) {
        	Message message = mHandler.obtainMessage(HANDLE_CONNECTION_ESTABLISHED_EVENT);
        	mHandler.sendMessage(message);
        }
        if (qualifier.equals(ChatApplication.SET_DEFAULTS_EVENT)) {
        	Message message = mHandler.obtainMessage(HANDLE_SET_DEFAULTS_EVENT);
        	mHandler.sendMessage(message);
        }
    }
    
    private void updateHistory() {
        Log.i(TAG, "updateHistory()");
	    mHistoryList.clear();
	    List<String> messages = mChatApplication.getHistory();
        for (String message : messages) {
            mHistoryList.add(message);
        }
	    mHistoryList.notifyDataSetChanged();
    }
    
    private void updateChannelState() {
        Log.i(TAG, "updateChannelState()");
    	AllJoynService.UseChannelState channelState = mChatApplication.useGetChannelState();
    	String name = mChatApplication.useGetChannelName();
    	if (name == null) {
    		name = "Not set";
    	}
        mUseChannelName.setText(name);
        
        switch (channelState) {
        case IDLE:
            mUseChannelStatus.setText("Idle");
            mJoinButton.setEnabled(true);
            mLeaveButton.setEnabled(false);
            break;
        case JOINED:
            mUseChannelStatus.setText("Joined");
            mJoinButton.setEnabled(false);
            mLeaveButton.setEnabled(true);
            break;	
        }
        
        AllJoynService.HostChannelState hostChannelState = mChatApplication.hostGetChannelState();
        String hostName = mChatApplication.hostGetChannelName();
        if(hostName == null) {
            hostName = "Not set";
        }
        mHostChannelName.setText(hostName);
        
        switch (hostChannelState) {
        case IDLE:
            mHostChannelStatus.setText("Idle");
            break;
        case NAMED:
            mHostChannelStatus.setText("Named");
            break;
        case BOUND:
            mHostChannelStatus.setText("Bound");
            break;
        case ADVERTISED:
            mHostChannelStatus.setText("Advertised");
            break;
        case CONNECTED:
            mHostChannelStatus.setText("Connected");
            break;
        }
        
    }
    
    private void setRandomChatButtonDefaults() {
    	//mRandomChatButton.setBackgroundResource(android.R.drawable.btn_default);
    	mRandomChatButton.setBackgroundColor(Color.LTGRAY);
        mRandomChatButton.setText("Random Chat");
        mRandomChatButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	mChatApplication.setAppStatus(ChatApplication.AppStatus.SEEKING_CONNECTION_ACTIVE);
            	mProgressBar.setVisibility(0);
                mRandomChatButton.setBackgroundColor(Color.parseColor("red"));
            	//mRandomChatButton.getBackground().setColorFilter(Color.parseColor("red"), PorterDuff.Mode.MULTIPLY);
                mRandomChatButton.setText("Stop");
                mRandomChatButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        mChatApplication.setAppStatus(ChatApplication.AppStatus.IDLE);
                        mProgressBar.setVisibility(4);
                        setRandomChatButtonDefaults();
                    }
                });
                mChatApplication.randomChat();
            }
        });
    }
    
    /**
     * An AllJoyn error has happened.  Since this activity pops up first we
     * handle the general errors.  We also handle our own errors.
     */
    private void alljoynError() {
    	if (mChatApplication.getErrorModule() == ChatApplication.Module.GENERAL ||
    		mChatApplication.getErrorModule() == ChatApplication.Module.USE) {
    		showDialog(DIALOG_ALLJOYN_ERROR_ID);
    	}
    }
    
    private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
    private static final int HANDLE_HISTORY_CHANGED_EVENT = 1;
    private static final int HANDLE_CHANNEL_STATE_CHANGED_EVENT = 2;
    private static final int HANDLE_ALLJOYN_ERROR_EVENT = 3;
    private static final int HANDLE_CONNECTION_ATTEMPTED_EVENT = 4;
    private static final int HANDLE_CONNECTION_ESTABLISHED_EVENT = 5;
    private static final int HANDLE_SET_DEFAULTS_EVENT = 6;
    
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case HANDLE_APPLICATION_QUIT_EVENT:
	            {
	                Log.i(TAG, "mHandler.handleMessage(): HANDLE_APPLICATION_QUIT_EVENT");
	                finish();
	            }
	            break; 
            case HANDLE_HISTORY_CHANGED_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_HISTORY_CHANGED_EVENT");
                    updateHistory();
                    break;
                }
            case HANDLE_CHANNEL_STATE_CHANGED_EVENT:
	            {
	                Log.i(TAG, "mHandler.handleMessage(): HANDLE_CHANNEL_STATE_CHANGED_EVENT");
	                updateChannelState();
	                break;
	            }
            case HANDLE_ALLJOYN_ERROR_EVENT:
	            {
	                Log.i(TAG, "mHandler.handleMessage(): HANDLE_ALLJOYN_ERROR_EVENT");
	                alljoynError();
	                break;
	            }
            case HANDLE_CONNECTION_ATTEMPTED_EVENT:
	            {
	                Log.i(TAG, "mHandler.handleMessage(): HANDLE_CONNECTION_ATTEMPTED_EVENT");
	                break;
	            }
            case HANDLE_CONNECTION_ESTABLISHED_EVENT:
	            {
	                Log.i(TAG, "mHandler.handleMessage(): HANDLE_CONNECTION_ESTABLISHED_EVENT");
	                mProgressBar.setVisibility(4);
	                // set the button back to default color
	                mRandomChatButton.setBackgroundResource(android.R.drawable.btn_default);
	                mRandomChatButton.setText("Disconnect From This Chat");
	                mRandomChatButton.setOnClickListener(new View.OnClickListener() {
	                    public void onClick(View v) {
	                    	mChatApplication.useLeaveChannel();
	                        mChatApplication.setAppStatus(ChatApplication.AppStatus.IDLE);
	                        setRandomChatButtonDefaults();
	                    }
	                });
	                break;
	            }
            case HANDLE_SET_DEFAULTS_EVENT:
            	{
            		Log.i(TAG, "mHandler.handleMessage(): HANDLE_SET_DEFAULTS_EVENT");
            		setRandomChatButtonDefaults();
            		break;
            	}
            default:
                break;
            }
        }
    };
    
    private ChatApplication mChatApplication = null;
    
    private ArrayAdapter<String> mHistoryList;
    
    private Button mJoinButton;
    private Button mLeaveButton;
    private Button mQuitButton;
    private Button mRandomChatButton;
    
    private ProgressBar mProgressBar;
    
    private TextView mUseChannelName;
      
    private TextView mUseChannelStatus;
    
    private TextView mHostChannelName;
    private TextView mHostChannelStatus;
}
