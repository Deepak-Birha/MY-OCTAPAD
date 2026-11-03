package com.deepakbirha.myoctapad;
import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.media.midi.*;
import android.webkit.*;
public class MainActivity extends Activity {
 WebView web; MidiManager manager; MidiDevice device;
 @Override public void onCreate(Bundle b){super.onCreate(b); web=new WebView(this); web.getSettings().setJavaScriptEnabled(true); web.addJavascriptInterface(new Bridge(),"AndroidMIDI"); setContentView(web); web.loadUrl("file:///android_asset/web/index.html"); manager=(MidiManager)getSystemService(Context.MIDI_SERVICE);}
 class Bridge { @JavascriptInterface public void connect(){runOnUiThread(()->{
  if(manager==null){status("Android MIDI service उपलब्ध नहीं");return;}
  MidiDeviceInfo[] ds=manager.getDevices(); if(ds.length==0){status("USB MIDI keyboard नहीं मिला। OTG जाँचें।");return;} open(ds,0);
 });}}
 void open(MidiDeviceInfo[] ds,int i){if(i>=ds.length){status("MIDI input port नहीं मिला");return;} MidiDeviceInfo info=ds[i]; int port=-1;
 for(MidiDeviceInfo.PortInfo p:info.getPorts())if(p.getType()==MidiDeviceInfo.PortInfo.TYPE_OUTPUT){port=p.getPortNumber();break;}
 if(port<0){open(ds,i+1);return;} final int pn=port;
 manager.openDevice(info,d->{if(d==null){open(ds,i+1);return;} try{device=d; MidiOutputPort out=d.openOutputPort(pn); if(out==null){open(ds,i+1);return;}
 out.connect(new MidiReceiver(){public void onSend(byte[] data,int offset,int count,long timestamp){for(int j=offset;j+2<offset+count;j+=3){int st=data[j]&255,n=data[j+1]&255,v=data[j+2]&255;if((st&240)==144&&v>0){final int nn=n,vv=v;runOnUiThread(()->web.evaluateJavascript("window.onNativeMIDIEvent("+nn+","+vv+")",null));}}}});
 status("Connected: "+info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME));
 }catch(Exception e){status("MIDI error: "+e.getMessage());}},null);
 }
 void status(String s){runOnUiThread(()->web.evaluateJavascript("window.onNativeMIDIStatus("+org.json.JSONObject.quote(s)+")",null));}
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
