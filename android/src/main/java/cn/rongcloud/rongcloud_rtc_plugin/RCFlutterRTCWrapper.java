package cn.rongcloud.rongcloud_rtc_plugin;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import cn.rongcloud.rtc.RTCErrorCode;
import cn.rongcloud.rtc.RongRTCEngine;
import cn.rongcloud.rtc.callback.JoinRoomUICallBack;
import cn.rongcloud.rtc.callback.RongRTCResultUICallBack;
import cn.rongcloud.rtc.engine.view.RongRTCVideoView;
import cn.rongcloud.rtc.events.RongRTCEventsListener;
import cn.rongcloud.rtc.room.RongRTCRoom;
import cn.rongcloud.rtc.stream.MediaType;
import cn.rongcloud.rtc.stream.local.RongRTCCapture;
import cn.rongcloud.rtc.stream.remote.RongRTCAVInputStream;
import cn.rongcloud.rtc.user.RongRTCRemoteUser;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;

public class RCFlutterRTCWrapper {

    private MethodChannel methodChannel;
    private Context context;
    private RongRTCRoom rtcRoom;
    private RongRTCCapture capture;

    private RCFlutterRTCWrapper() {

    }

    private static class SingleHolder {
        static RCFlutterRTCWrapper instance = new RCFlutterRTCWrapper();
    }

    public static RCFlutterRTCWrapper getInstance() {
        return SingleHolder.instance;
    }

    public void saveMethodChannel(MethodChannel methodChannel) {
        this.methodChannel = methodChannel;
    }

    public void saveContext(Context context) {
        this.context = context;
    }

    public void onRTCMethodCall(MethodCall call, MethodChannel.Result result) {
        if (call.method.equals(RCFlutterRTCMethodKey.Init)) {
            initWithAppkey(call.arguments);
        }else if(call.method.equals(RCFlutterRTCMethodKey.Connect)) {
            connect(call.arguments,result);
        }else if(call.method.equals(RCFlutterRTCMethodKey.JoinRTCRoom)) {
            joinRTCRoom(call.arguments,result);
        }else if(call.method.equals(RCFlutterRTCMethodKey.LeaveRTCRoom)) {
            leaveRTCRoom(call.arguments,result);
        }else if(call.method.equals(RCFlutterRTCMethodKey.PublishAVStream)) {
            publishAVStream(result);
        }else if(call.method.equals(RCFlutterRTCMethodKey.UnpublishAVStream)) {
            unpublishAVStream(result);
        }else if(call.method.equals(RCFlutterRTCMethodKey.RenderLocalVideo)) {
            renderLocalVideo(call.arguments);
        }else if(call.method.equals(RCFlutterRTCMethodKey.RenderRemoteVideo)) {
            renderRemoteVideo(call.arguments);
        }else if(call.method.equals(RCFlutterRTCMethodKey.RemoveNativeView)) {
            removeNativeView(call.arguments);
        }else if(call.method.equals(RCFlutterRTCMethodKey.SubscribeAVStream)) {
            subscribeAVStream(call.arguments);
        }else if(call.method.equals(RCFlutterRTCMethodKey.UnsubscribeAVStream)) {
            unsubscribeAVStream(call.arguments);
        }else if(call.method.equals(RCFlutterRTCMethodKey.GetRemoteUsers)) {
            getRemoteUsers(call.arguments,result);
        }else if(call.method.equals(RCFlutterRTCMethodKey.MuteLocalAudio)) {
            muteLocalAudio(call.arguments);
        }else if(call.method.equals(RCFlutterRTCMethodKey.MuteRemoteAudio)) {
            muteRemoteAudio(call.arguments);
        }else if(call.method.equals(RCFlutterRTCMethodKey.SwitchCamera)) {
            switchCamera(call.arguments);
        }
        else {
            result.notImplemented();
        }
    }

    private void initWithAppkey(Object arg) {
        RCLog.i("init");
        RCLog.i("param "+arg.toString());
        if(arg instanceof String) {
            String appkey = String.valueOf(arg);
            RongIMClient.init(this.context,appkey);

            setConnectStatusListener();
        }
    }

    private void connect(Object arg, final MethodChannel.Result result) {
        RCLog.i("connect start");
        RCLog.i("param "+arg.toString());
        if(arg instanceof String) {
            String token = String.valueOf(arg);
            RongIMClient.connect(token, new RongIMClient.ConnectCallback() {
                @Override
                public void onTokenIncorrect() {
                    RCLog.e("connect end error 31004");
                    result.success(31004);
                }

                @Override
                public void onSuccess(String s) {
                    RCLog.i("connect success");
                    result.success(0);
                }

                @Override
                public void onError(RongIMClient.ErrorCode errorCode) {
                    RCLog.e(" connect end error " + errorCode.getValue());
                    result.success(errorCode.getValue());
                }
            });
        }
    }

    private void joinRTCRoom(Object arg, final MethodChannel.Result result) {
        RCLog.i("joinRTCRoom start");
        RCLog.i("param "+arg.toString());
        if(arg instanceof String) {
            String roomId = String.valueOf(arg);
            RongRTCEngine.getInstance().joinRoom(roomId, new JoinRoomUICallBack() {
                @Override
                protected void onUiSuccess(RongRTCRoom rongRTCRoom) {
                    rtcRoom = rongRTCRoom;
                    rtcRoom.registerEventsListener(new RTCEventsListener());
                    RCLog.i("joinRTCRoom success");
                    result.success(0);
                }

                @Override
                protected void onUiFailed(RTCErrorCode rtcErrorCode) {
                    RCLog.e("joinRTCRoom error "+ rtcErrorCode.getValue());
                    result.success(rtcErrorCode.getValue());
                }
            });
        }
    }

    private void leaveRTCRoom(Object arg, final MethodChannel.Result result) {
        RCLog.i("leaveRTCRoom start");
        RCLog.i("param "+arg.toString());
        if(arg instanceof String) {
            String roomId = String.valueOf(arg);
            RongRTCEngine.getInstance().quitRoom(roomId, new RongRTCResultUICallBack() {
                @Override
                public void onUiSuccess() {
                    RCLog.i("leaveRTCRoom success ");
                    result.success(0);
                }

                @Override
                public void onUiFailed(RTCErrorCode rtcErrorCode) {
                    RCLog.e("leaveRTCRoom error "+ rtcErrorCode.getValue());
                    result.success(rtcErrorCode.getValue());
                }
            });
        }
    }

    private void publishAVStream(final MethodChannel.Result result) {
        RCLog.i("publishAVStream start");
        if(this.rtcRoom == null || this.rtcRoom.getLocalUser() == null) {
            RCLog.e("publishAVStream error " + RTCErrorCode.RongRTCCodeNotInRoom.getValue());
            result.success(RTCErrorCode.RongRTCCodeNotInRoom.getValue());
            return;
        }
        this.rtcRoom.getLocalUser().publishDefaultAVStream(new RongRTCResultUICallBack() {
            @Override
            public void onUiSuccess() {
                RCLog.i("publishAVStream success");
                result.success(0);
            }

            @Override
            public void onUiFailed(RTCErrorCode rtcErrorCode) {
                RCLog.e("publishAVStream error "+ rtcErrorCode.getValue());
                result.success(rtcErrorCode.getValue());
            }
        });
    }

    private void unpublishAVStream(final MethodChannel.Result result) {
        RCLog.i("unpublishAVStream start");
        if(this.rtcRoom == null || this.rtcRoom.getLocalUser() == null) {
            RCLog.e("unpublishAVStream error " + RTCErrorCode.RongRTCCodeNotInRoom.getValue());
            result.success(RTCErrorCode.RongRTCCodeNotInRoom.getValue());
            return;
        }
        this.rtcRoom.getLocalUser().unPublishDefaultAVStream(new RongRTCResultUICallBack() {
            @Override
            public void onUiSuccess() {
                RCLog.i("unpublishAVStream success");
                result.success(0);
            }

            @Override
            public void onUiFailed(RTCErrorCode rtcErrorCode) {
                RCLog.e("unpublishAVStream error "+ rtcErrorCode.getValue());
                result.success(rtcErrorCode.getValue());
            }
        });
    }

    private void renderLocalVideo(Object arg) {
        String LOG_TAG = "renderLocalVideo " ;
        RCLog.i(LOG_TAG + "start");
        RCLog.i("param "+arg.toString());
        if(arg instanceof Map) {
            Map param = (Map)arg;
            int viewId = (Integer) param.get("viewId");
            RongRTCVideoView view = RCFlutterRTCViewFactory.getInstance().getRenderVideoView(viewId);
            //todo
            if(view != null) {
                getCapture().setRongRTCVideoView(view);
                getCapture().startCameraCapture();
            }
        }
    }

    private void renderRemoteVideo(Object arg) {
        String LOG_TAG = "renderRemoteVideo " ;
        RCLog.i(LOG_TAG + "start");
        RCLog.i(LOG_TAG +"param "+arg.toString());

        if(arg instanceof Map) {
            if(this.rtcRoom == null || this.rtcRoom.getRemoteUsers() == null) {
                RCLog.i(LOG_TAG +"error not in room or remote users don't exist");
                return;
            }
            Map param = (Map)arg;
            int viewId = (Integer) param.get("viewId");
            String userId = (String)param.get("userId");
            RongRTCVideoView view = RCFlutterRTCViewFactory.getInstance().getRenderVideoView(viewId);
            if(view != null) {

                for(String uId : this.rtcRoom.getRemoteUsers().keySet()) {
                    if(uId.equals(userId)) {
                        renderViewForRemoteUser(view,this.rtcRoom.getRemoteUser(uId));
                    }
                }
            }
        }

    }

    private void renderViewForRemoteUser(RongRTCVideoView view,RongRTCRemoteUser user) {
        String LOG_TAG = "renderViewForRemoteUser " ;
        RCLog.i(LOG_TAG + "start");
        if(user == null || user.getRemoteAVStreams() == null) {
            RCLog.e(LOG_TAG+"remote user is null or doesn't have remote streams");
            return;
        }
        for(RongRTCAVInputStream stream : user.getRemoteAVStreams()) {
            if(stream.getMediaType() == MediaType.VIDEO) {
                stream.setRongRTCVideoView(view);
                return;
            }
        }
    }

    private void removeNativeView(Object arg) {
        String LOG_TAG = "removeNativeView " ;
        RCLog.i(LOG_TAG + "start");
        if(arg instanceof Map) {
            Map param = (Map)arg;
            int viewId = (Integer) param.get("viewId");
            RCFlutterRTCViewFactory.getInstance().removeRenderVideoView(viewId);
        }
    }

    private void subscribeAVStream(Object arg) {
        String LOG_TAG = "subscribeAVStream " ;
        RCLog.i(LOG_TAG + "start");
        if(arg instanceof String) {
            String userId = String.valueOf(arg);
            if(this.rtcRoom == null || this.rtcRoom.getRemoteUser(userId) == null) {
                RCLog.e(LOG_TAG+"not in room or remote user doesn't exist :"+userId);
                return;
            }
            RongRTCRemoteUser user = this.rtcRoom.getRemoteUser(userId);
            this.rtcRoom.subscribeAvStream(user.getRemoteAVStreams(), new RongRTCResultUICallBack() {
                @Override
                public void onUiSuccess() {
                    RCLog.i("subscribeAVStream success ");
                }

                @Override
                public void onUiFailed(RTCErrorCode rtcErrorCode) {
                    RCLog.e("subscribeAVStream error "+ rtcErrorCode.getValue());
                }
            });
        }
    }

    private void unsubscribeAVStream(Object arg) {
        String LOG_TAG = "unsubscribeAVStream " ;
        RCLog.i(LOG_TAG + "start");
        if(arg instanceof String) {
            String userId = String.valueOf(arg);
            if (this.rtcRoom == null || this.rtcRoom.getRemoteUser(userId) == null) {
                RCLog.e(LOG_TAG + "not in room or remote user doesn't exist :" + userId);
                return;
            }
            RongRTCRemoteUser user = this.rtcRoom.getRemoteUser(userId);
            this.rtcRoom.unSubscribeAVStream(user.getRemoteAVStreams(), new RongRTCResultUICallBack() {
                @Override
                public void onUiSuccess() {
                    RCLog.i("unsubscribeAVStream success ");

                }

                @Override
                public void onUiFailed(RTCErrorCode rtcErrorCode) {
                    RCLog.e("unsubscribeAVStream error "+ rtcErrorCode.getValue());
                }
            });
        }
    }

    private void getRemoteUsers(Object arg, MethodChannel.Result result) {
        String LOG_TAG = "getRemoteUsers " ;
        RCLog.i(LOG_TAG + "start");
        if(arg instanceof String) {
            String roomId = String.valueOf(arg);
            if(this.rtcRoom == null || !this.rtcRoom.getRoomId().equals(roomId) || this.rtcRoom.getRemoteUsers() == null) {
                return;
            }
            List list = new ArrayList();
            for(String uid : this.rtcRoom.getRemoteUsers().keySet()) {
                list.add(uid);
            }
            result.success(list);

        }
    }

    private void muteLocalAudio(Object arg) {
        String LOG_TAG = "muteLocalAudio " ;
        RCLog.i(LOG_TAG + "start");
        if(arg instanceof Map) {
            Map map = (Map)arg;
            boolean muted = (boolean)map.get("muted");
            this.capture.muteMicrophone(muted);
        }
    }

    private void muteRemoteAudio(Object arg) {
        String LOG_TAG = "muteRemoteAudio " ;
        RCLog.i(LOG_TAG + "start");
        if(arg instanceof Map) {
            Map map = (Map)arg;
            boolean muted = (boolean)map.get("muted");
            String userId = (String)map.get("userId");
            if(this.rtcRoom == null) {
                return;
            }
            RongRTCRemoteUser user = this.rtcRoom.getRemoteUser(userId);
            if(user != null && user.getRemoteAVStreams() != null) {
                for(RongRTCAVInputStream stream : user.getRemoteAVStreams()) {
                    if(stream.getMediaType() == MediaType.AUDIO) {
//                        stream.
                        //todo
                    }
                }
            }
        }
    }

    private void switchCamera(Object arg) {
        String LOG_TAG = "switchCamera " ;
        RCLog.i(LOG_TAG + "start");
        if(this.capture == null) {
            return;
        }
        this.capture.switchCamera();
    }

    private void setConnectStatusListener() {
        RongIMClient.setConnectionStatusListener(new RongIMClient.ConnectionStatusListener() {
            @Override
            public void onChanged(ConnectionStatus connectionStatus) {
                final String LOG_TAG = "ConnectionStatusChanged";
                RCLog.i(LOG_TAG+" status:"+String.valueOf(connectionStatus.getValue()));
                Map map = new HashMap();
                map.put("status",connectionStatus.getValue());
                methodChannel.invokeMethod(RCFlutterRTCMethodKey.MethodCallBackKeyConnectionStatusChange,map);
            }
        });
    }

    private class RTCEventsListener implements RongRTCEventsListener {

        @Override
        public void onRemoteUserPublishResource(RongRTCRemoteUser rongRTCRemoteUser, List<RongRTCAVInputStream> list) {
            String userId = rongRTCRemoteUser.getUserId();
            if(userId != null) {
                Map map = new HashMap();
                map.put("userId",userId);
                methodChannel.invokeMethod(RCFlutterRTCMethodKey.OthersPublishStreamsCallBack,map);
            }
        }

        @Override
        public void onRemoteUserAudioStreamMute(RongRTCRemoteUser rongRTCRemoteUser, RongRTCAVInputStream rongRTCAVInputStream, boolean b) {

        }

        @Override
        public void onRemoteUserVideoStreamEnabled(RongRTCRemoteUser rongRTCRemoteUser, RongRTCAVInputStream rongRTCAVInputStream, boolean b) {

        }

        @Override
        public void onRemoteUserUnPublishResource(RongRTCRemoteUser rongRTCRemoteUser, List<RongRTCAVInputStream> list) {

        }

        @Override
        public void onUserJoined(RongRTCRemoteUser rongRTCRemoteUser) {
            String userId = rongRTCRemoteUser.getUserId();
            if(userId != null) {
                Map map = new HashMap();
                map.put("userId",userId);
                methodChannel.invokeMethod(RCFlutterRTCMethodKey.UserJoinedCallBack,map);
            }
        }

        @Override
        public void onUserLeft(RongRTCRemoteUser rongRTCRemoteUser) {
            String userId = rongRTCRemoteUser.getUserId();
            if(userId != null) {
                Map map = new HashMap();
                map.put("userId",userId);
                methodChannel.invokeMethod(RCFlutterRTCMethodKey.UserLeavedCallBack,map);
            }
        }

        @Override
        public void onUserOffline(RongRTCRemoteUser rongRTCRemoteUser) {

        }

        @Override
        public void onVideoTrackAdd(String s, String s1) {
            Log.i("checkonVideoTrackAdd",s+":"+s1);
        }

        @Override
        public void onFirstFrameDraw(String s, String s1) {
            Log.i("checkonFirstFrameDraw",s+":"+s1);
        }

        @Override
        public void onLeaveRoom() {

        }

        @Override
        public void onReceiveMessage(Message message) {

        }
    }

    private RongRTCCapture getCapture() {
        if(capture == null) {
            capture = RongRTCCapture.getInstance();
            capture.init(context);
        }
        return capture;
    }
}
