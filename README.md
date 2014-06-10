AOSUtilsAndroid
===============

Includes:

Facebook & Twitter SDKs  
YouTube SDK  
Easy HTTP calls, XML / JSON parsing


###### Twitter usage:

    // Where REQUEST_CODE_TWITTER_LOGIN is a unique ID that your calling activity will receive once the user authenticates
    int REQUEST_CODE_TWITTER_LOGIN = 1;
    SocialTwitter twitter = new SocialTwitter(String apiKey, String apiSecret, int REQUEST_CODE_TWITTER_LOGIN);
    
    // Where onSuccessfulTweetRunnable is a Runnable that will be called once the tweet has gone out (can be null if you'd like)
    twitter.tweet(String message, Activity activity, Runnable onSuccessfulTweetRunnable);
    
    // You need to call this from your activity's onActivityResult() if you want to send out the pending tweet once the user is logged in.
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (requestCode == REQUEST_CODE_TWITTER_LOGIN && resultCode == RESULT_OK) {
        twitter.onLoginSuccessful(data, activity, new OnSuccessfulPostRunnable());
      }
    }
    
    // And then if you want to log out
    SocialCommon.logout(Context context);


###### Facebook usage:

    // Where REQUEST_CODE_FACEBOOK_LOGIN is a unique ID that your calling activity will receive once the user authenticates
    int REQUEST_CODE_FACEBOOK_LOGIN = 2;
    SocialFacebook facebook = new SocialFacebook(String appId, String appSecret, int REQUEST_CODE_FACEBOOK_LOGIN);

    String action = "video.watches";
    HashMap<String, String> properties = new HashMap<String, String>();
    properties.put("video", "http://www.youtube.com/watch?v=" + youtubeVideoId);
    properties.put("fb:explicitly_shared", "false");
    // Where onSuccessfulPostRunnable is a Runnable that will be called once the status is posted (can be null if you'd like)
    facebook.post(String action, Map<String, String> properties, Activity activity, Runnable onSuccessfulPostRunnable)  

    // You need to call this from your activity's onActivityResult() if you want to send out the status update once the user is logged in.
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
      if (requestCode == REQUEST_CODE_FACEBOOK_LOGIN && resultCode == RESULT_OK) {  
        onLoginSuccessful(Intent data, Activity activity, Runnable onSuccessfulPostRunnable) {
      }  
    }
    
    // And then if you want to log out
    SocialCommon.logout(Context context);



#### This project depends on the [AOSUtilsBase](https://github.com/AndroidsOnSkateboards/AOSUtilsBase) project, though you can just copy the few classes from there and build it all together
