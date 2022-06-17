/********************************************************************************
 * Copyright (c) 2022 ThingWave AB
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   ThingWave - design and implementation
 *   Arrowhead Consortia - conceptualization
 ********************************************************************************/

package eu.thingwave.arrowheadmqtt;

import java.util.List;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.io.OutputStream;
import java.util.Properties;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


//import java.io.FileWriter;
//import java.io.PrintWriter;


/**
 * Hello world!
 *
 */
public class App implements MqttCallback, Runnable
{
  MqttClient client;
  boolean subscribe_only = false, log_only=false;
  ArrayList<String> messages = null;
  Properties prop = null;

  public App() {
    System.setProperty("https.protocols", "TLSv1.3");
    messages = new ArrayList<String>();

    prop = new Properties();

    try {
      final InputStream input = new FileInputStream("app.properties");
      prop.load(input);

      /*System.out.println(prop.getProperty("cafile"));
      System.out.println(prop.getProperty("certfile"));
      System.out.println(prop.getProperty("keyfile"));

      System.out.println(prop.getProperty("broker-url"));
      System.out.println(prop.getProperty("topic"));*/

    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }


  public static void main(String[] args) {
    App a = new App();

    for (String s: args) {
      System.out.println("\t"+s);
      if (s.equals("--subscribe-only"))
	a.subscribe_only = true;
      if (s.equals("--log-only"))
	a.log_only = true;
    }

    Thread t = new Thread(a);
    t.start();
    if (a.subscribe_only) {
      a.doDemo("subscriber", a.subscribe_only);
    } else {
      a.doDemo("publisher", a.subscribe_only);
    }
  }


  public void run() {
    if (subscribe_only) {
      try {
	while (true) {
	  if (!messages.isEmpty() ) {
	    String msg = messages.get(0);
	    messages.remove(0);
	    //System.out.println(msg);
	  }
	  Thread.sleep(1000);
	}
      } catch(InterruptedException iex) {
	System.out.println("Interrupted!");
      }
    } 
  }



  /**
   * \fn doDemo()
   *
   */
  public void doDemo(String clientId, boolean subscribe_only){
    String topic        = prop.getProperty("topic");
    int qos             = 2;
    String broker       = prop.getProperty("broker-url");
    MemoryPersistence persistence = new MemoryPersistence();

    try {
      MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
      MqttConnectOptions connOpts = new MqttConnectOptions();
      connOpts.setCleanSession(true);

      if(prop.getProperty("password") != null ) {
	connOpts.setPassword(prop.getProperty("password").toCharArray());
      }
      if(prop.getProperty("username") != null) {
	connOpts.setUserName(prop.getProperty("username"));	   
      }

      try {
	    connOpts.setSocketFactory(SslUtil.getSocketFactory(prop.getProperty("cafile"), 
				    prop.getProperty("certfile"), 
				    prop.getProperty("keyfile"), 
				    prop.getProperty("username")));
      } catch(Exception err) {
	    System.out.println("Certificate exception(s): " + err);
      }
      System.out.println("Connecting to broker: "+broker + " with topic: " + topic);
      sampleClient.connect(connOpts);
      sampleClient.setCallback(this);
      sampleClient.subscribe(topic);
      sampleClient.subscribe(prop.getProperty("systemName") + "/replies");
      System.out.println("Connected");

      GsonBuilder builder = new GsonBuilder();
      Gson gson = builder.create();

      MqttRequestDTO req = new MqttRequestDTO();
      req.setMethod("GET");
      req.setReplyTo(prop.getProperty("systemName") + "/replies");
      String outMsg = gson.toJson(req);
      MqttMessage pingMessage = new MqttMessage(outMsg.getBytes());
      sampleClient.publish("ah/serviceregistry/echo", pingMessage);

      try {
	for(int i=0; i<5*100000; i++) {
	  long bt = System.currentTimeMillis();

	  //String content = new String("[{\"bn\": \"producer\"}]");
	  //System.out.println("Publishing message: " + content);

	  /* prepare message */
	  //MqttMessage message = new MqttMessage(content.getBytes());
	  //message.setQos(qos);
	  if (subscribe_only == false && log_only==false) {
	    //sampleClient.publish(topic, message);
	    //System.out.println("Message published");
	    sampleClient.publish("ah/serviceregistry/echo", pingMessage);
	  }
	  Thread.sleep(1000);
	}
      } catch(InterruptedException iex) {
	System.out.println("Interrupted!");
      }

      sampleClient.disconnect();
      System.out.println("Disconnected");
    } catch(MqttException me) {
      System.out.println("reason "+me.getReasonCode());
      System.out.println("msg "+me.getMessage());
      System.out.println("loc "+me.getLocalizedMessage());
      System.out.println("cause "+me.getCause());
      System.out.println("excep "+me);
      me.printStackTrace();
    } finally{
    }

    try {
      Thread.sleep(1000);
      System.exit(0);
    } catch(InterruptedException me) {
    }
  }


  @Override
    public void connectionLost(Throwable cause) {
      // TODO Auto-generated method stub
      System.out.println("connectionLost: "+ cause.toString());

    }


  @Override
    public void messageArrived(String topic, MqttMessage message) {
      //if(this.subscribe_only == false)
	//return;
      String jsonstr = message.toString();
      messages.add(jsonstr);
      System.out.println("Received message:\n" + message);
      /*try {
	JSONArray jsona = (JSONArray) JSONValue.parseWithException(jsonstr);
	if (jsona != null) {
	  //List<JSONObject> jobj = new ArrayList<JSONObject>();
	  Iterator it = jsona.iterator();
	  while (it.hasNext()) {
	    JSONObject json = (JSONObject) it.next();
	    //System.out.println("bn: " + json.get("bn"));
	    //System.out.println("bt: " + json.get("bt"));
	    //System.out.println("n: " + json.get("n"));
	    //System.out.println("v: " + json.get("v"));
	  }
	}
      } catch (Exception e) {
      }*/


    }

  @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

}
