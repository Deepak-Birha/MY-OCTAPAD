package com.deepakbirha.myoctapad;
import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.media.midi.*;
import android.webkit.*;

public class MainActivity extends Activity {
 private static final int FILE_CHOOSER_REQUEST=501;
 private WebView web;
 private ValueCallback<Uri[]> fileCallback;
 private MidiManager manager;
 private MidiDevice device;

 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  web=new WebView(this);
  WebSettings settings=web.getSettings();
  settings.setJavaScriptEnabled(true);
  settings.setDomStorageEnabled(true);
  settings.setAllowFileAccess(true);
  settings.setAllowContentAccess(true);
  settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
  web.clearCache(true);
  web.addJavascriptInterface(new Bridge(),"AndroidMIDI");
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params){
    if(fileCallback!=null)fileCallback.onReceiveValue(null);
    fileCallback=callback;
    Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("audio/*");
    intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"audio/wav","audio/x-wav","application/octet-stream"});
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    try{startActivityForResult(intent,FILE_CHOOSER_REQUEST);}
    catch(Exception e){fileCallback=null;callback.onReceiveValue(null);return false;}
    return true;
   }
  });
  setContentView(web);
  web.loadUrl("file:///android_asset/web/index.html?build=2");
  manager=(MidiManager)getSystemService(Context.MIDI_SERVICE);
 }

 @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
  super.onActivityResult(requestCode,resultCode,data);
  if(requestCode==FILE_CHOOSER_REQUEST&&fileCallback!=null){
   Uri[] result=null;
   if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
    Uri uri=data.getData(); result=new Uri[]{uri};
    try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
   }
   fileCallback.onReceiveValue(result); fileCallback=null;
  }
 }

 class Bridge{
  @JavascriptInterface public void connect(){runOnUiThread(()->{
   if(manager==null){status("Android MIDI service उपलब्ध नहीं");return;}
   MidiDeviceInfo[] ds=manager.getDevices();
   if(ds.length==0){status("USB MIDI keyboard नहीं मिला। OTG जाँचें।");return;}
   open(ds,0);
  });}
 }
 void open(MidiDeviceInfo[] ds,int i){
  if(i>=ds.length){status("MIDI input port नहीं मिला");return;}
  MidiDeviceInfo info=ds[i]; int port=-1;
  for(MidiDeviceInfo.PortInfo p:info.getPorts())if(p.getType()==MidiDeviceInfo.PortInfo.TYPE_OUTPUT){port=p.getPortNumber();break;}
  if(port<0){open(ds,i+1);return;} final int pn=port;
  manager.openDevice(info,d->{if(d==null){open(ds,i+1);return;}try{
   device=d; MidiOutputPort out=d.openOutputPort(pn); if(out==null){open(ds,i+1);return;}
   out.connect(new MidiReceiver(){@Override public void onSend(byte[] data,int offset,int count,long timestamp){
    for(int j=offset;j+2<offset+count;j+=3){int st=data[j]&255,n=data[j+1]&255,v=data[j+2]&255;
     if((st&240)==144&&v>0){final int nn=n,vv=v;runOnUiThread(()->web.evaluateJavascript("window.onNativeMIDIEvent("+nn+","+vv+")",null));}
    }
   }});
   status("Connected: "+info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME));
  }catch(Exception e){status("MIDI error: "+e.getMessage());}},null);
 }
 void status(String s){runOnUiThread(()->web.evaluateJavascript("window.onNativeMIDIStatus("+org.json.JSONObject.quote(s)+")",null));}
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
