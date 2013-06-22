package com.geekyouup.android.ustopwatch;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.geekyouup.android.ustopwatch.fragments.StopwatchFragment;
import com.geekyouup.android.ustopwatch.stopwatchstateendpoint.Stopwatchstateendpoint;
import com.geekyouup.android.ustopwatch.stopwatchstateendpoint.model.StopWatchState;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;

import java.io.IOException;

/**
 * Created by appu on 5/12/13.
 */
public class StopwatchStateEndpointHelper {
    private static StopwatchStateEndpointHelper INSTANCE = null;
    private Stopwatchstateendpoint mStopwatchStateEndpoint = null;

    private StopwatchStateEndpointHelper() {
        mStopwatchStateEndpoint = CloudEndpointUtils.updateBuilder(new Stopwatchstateendpoint.Builder(
                AndroidHttp.newCompatibleTransport(),
                new JacksonFactory(),
                new HttpRequestInitializer() {
                    public void initialize(HttpRequest httpRequest) {
                    }
                })).build();
    }

    public static StopwatchStateEndpointHelper getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new StopwatchStateEndpointHelper();
        }
        return INSTANCE;
    }

    private Stopwatchstateendpoint getStopwatchStateEndpoint() {
        return mStopwatchStateEndpoint;
    }

    public void sendStateToEndpoint(Activity context, double time, boolean isRunning, String regId) {
        StopWatchState state = new StopWatchState();
        state.setTimestamp(time);
        state.setRunning(isRunning);
        state.setSourceDevice(regId);

        (new SendStateTask(context)).execute(state);
    }

    public void getStateFromEndpoint(Activity context, StopwatchFragment callbackFragment) {
        (new GetStateTask(context, callbackFragment)).execute();
    }

    public class SendStateTask extends AsyncTask<StopWatchState, Void, Boolean> {

        private final Activity mContext;

        public SendStateTask(Activity context) {
            super();
            mContext = context;
        }

        @Override
        protected Boolean doInBackground(StopWatchState... args) {
            try {
                StopwatchStateEndpointHelper.getInstance().getStopwatchStateEndpoint().insertStopWatchState(args[0]).execute();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(!result) {
                Toast.makeText(mContext, "Failed to sync time", Toast.LENGTH_SHORT).show();
            }
        }
    };

    public class GetStateTask extends AsyncTask<Void, Void, StopWatchState> {

        private final Activity mContext;
        private final StopwatchFragment mCallback;

        public GetStateTask(Activity context, StopwatchFragment callbackFragment) {
            mContext = context;
            mCallback = callbackFragment;
        }

        @Override
        protected StopWatchState doInBackground(Void... params) {
            try {
                return StopwatchStateEndpointHelper.getInstance().getStopwatchStateEndpoint().getStopWatchState(-1L).execute();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(StopWatchState result) {
            if(mCallback != null && mCallback.isAdded() && result != null) {
                mCallback.setState(result.getTimestamp(), result.getRunning());
            }
            else if(result == null){
                Toast.makeText(mContext, "Failed to sync time", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
