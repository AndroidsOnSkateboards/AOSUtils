AOSUtilsAndroid
===============

Includes:

Facebook & Twitter SDKs  
Easy HTTP calls, XML / JSON parsing


###### Twitter usage:

    int activityRequestCodeForTwitterLogin = 1;
    SocialTwitter twitter = new SocialTwitter(String apiKey, String apiSecret, int activityRequestCodeForTwitterLogin);
// where activityRequestCodeForLogin is a unique ID that your calling activity will receive once the user authenticates

    twitter.tweet(String message, Activity activity, Runnable onSuccessfulTweetRunnable);`  
// where onSuccessfulTweetRunnable is a Runnable that will happen once the tweet has gone out (can be null if you like)

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (requestCode == activityRequestCodeForTwitterLogin && resultCode == RESULT_OK) {
        twitter.onLoginSuccessful(data, activity, new OnSuccessfulPostRunnable());
      }
    }
// You need to call this from your activity's onActivityResult() if you want to send out the pending tweet once the user has logged in.`



###### Facebook usage:

    int activityRequestCodeForFacebookLogin = 2;
    SocialFacebook facebook = new SocialFacebook(String appId, String appSecret, int activityRequestCodeForFacebookLogin);
// where activityRequestCodeForLogin is a unique ID that your calling activity will receive once the user authenticates

    facebook.post(String action, Map<String, String> properties, Activity activity, Runnable onSuccessfulPost)  
// where onSuccessfulTweetRunnable is a Runnable that will happen once the status is posted (can be null if you like)

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
      if (requestCode == activityRequestCodeForFacebookLogin && resultCode == RESULT_OK) {  
        onLoginSuccessful(Intent data, Activity activity, Runnable onSuccessfulPostRunnable) {
      }  
    }
// You need to call this from your activity's onActivityResult() if you want to send out the status update once the user has logged in.



#### This project depends on the AOSUtilsBase project, though you can just copy the few classes from there into this project
