package com.bwayo.superapp
import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.util.concurrent.Executors

class MainActivity : Activity() {
 private val blue=Color.rgb(13,71,161)
 private val baseUrl="https://pxjiadrraofmnreffvxx.supabase.co"
 private val anonKey="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB4amlhZHJyYW9mbW5yZWZmdnh4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQxNDc4MjcsImV4cCI6MjEwOTcyMzgyN30.wr1UqU5WAq3kp-sV0Hb0xWMgDzNq8ycwluCTe7ajInk"
 private val worker=Executors.newSingleThreadExecutor()
 private var token:String?=null; private var userId:String?=null; private var userEmail:String?=null
 private var displayName="Member"; private var isAdmin=false; private lateinit var feed:LinearLayout
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  val p=getSharedPreferences("bwayo_session",MODE_PRIVATE); token=p.getString("token",null); userId=p.getString("user_id",null); userEmail=p.getString("email",null); displayName=p.getString("name","Member")?:"Member"
  if(token!=null&&userId!=null) loadHome() else showAuth()
 }
 private fun base()=LinearLayout(this).apply{orientation=1;setBackgroundColor(Color.rgb(245,248,255));setPadding(24,20,24,24)}
 private fun heading(s:String)=TextView(this).apply{text=s;textSize=25f;setTextColor(blue);gravity=Gravity.CENTER;setPadding(0,12,0,18)}
 private fun btn(s:String,a:()->Unit)=Button(this).apply{text=s;setOnClickListener{a()}}
 private fun msg(s:String){Toast.makeText(this,s,Toast.LENGTH_LONG).show()}
 private fun req(path:String,method:String="GET",body:JSONObject?=null,bearer:String?=token,prefer:String?=null):String{
  val c=URL(baseUrl+path).openConnection() as HttpURLConnection
  c.requestMethod=method;c.connectTimeout=15000;c.readTimeout=15000
  c.setRequestProperty("apikey",anonKey);c.setRequestProperty("Content-Type","application/json")
  if(bearer!=null)c.setRequestProperty("Authorization","Bearer "+bearer)
  if(prefer!=null)c.setRequestProperty("Prefer",prefer)
  if(body!=null){c.doOutput=true;OutputStreamWriter(c.outputStream,Charsets.UTF_8).use{it.write(body.toString())}}
  val code=c.responseCode;val stream=if(code in 200..299)c.inputStream else c.errorStream
  val out=stream?.bufferedReader()?.use{it.readText()}?:"";c.disconnect()
  if(code !in 200..299)throw Exception("Server error "+code+": "+out)
  return out
 }
 private fun async(block:()->Unit){worker.execute(block)}
 private fun showAuth(){
  val l=base();l.addView(heading("BWAYO SUPER APP"));l.addView(TextView(this).apply{text="Sign in or create your account";textSize=17f;gravity=Gravity.CENTER;setTextColor(Color.DKGRAY)})
  val n=EditText(this).apply{hint="Your name (for registration)"}
  val e=EditText(this).apply{hint="Email address";inputType=33}
  val p=EditText(this).apply{hint="Password (at least 6 characters)";inputType=129}
  l.addView(n);l.addView(e);l.addView(p)
  l.addView(btn("Create account"){
   val name=n.text.toString().trim();val email=e.text.toString().trim();val pass=p.text.toString()
   if(name.isBlank()||email.isBlank()||pass.length<6){msg("Enter name, email and password of at least 6 characters.");return@btn}
   async{try{
    val payload=JSONObject().put("email",email).put("password",pass).put("data",JSONObject().put("name",name))
    val o=JSONObject(req("/auth/v1/signup","POST",payload,null))
    if(o.optString("access_token").isNotBlank()&&o.has("user")){save(o,name);runOnUiThread{loadHome()}}
    else runOnUiThread{msg("Account created. Check your email to confirm, then sign in.")}
   }catch(x:Exception){runOnUiThread{msg(x.message?:"Registration failed")}}}
  })
  l.addView(btn("Sign in"){
   val email=e.text.toString().trim();val pass=p.text.toString()
   if(email.isBlank()||pass.isBlank()){msg("Enter email and password.");return@btn}
   async{try{val o=JSONObject(req("/auth/v1/token?grant_type=password","POST",JSONObject().put("email",email).put("password",pass),null));save(o,n.text.toString().trim());runOnUiThread{loadHome()}}
   catch(x:Exception){runOnUiThread{msg(x.message?:"Sign in failed")}}}
  })
  setContentView(l)
 }
 private fun save(o:JSONObject,name:String){
  val u=o.getJSONObject("user");token=o.getString("access_token");userId=u.getString("id");userEmail=u.optString("email")
  displayName=name.ifBlank{u.optJSONObject("user_metadata")?.optString("name")?.takeIf{it.isNotBlank()}?:userEmail?:"Member"}
  getSharedPreferences("bwayo_session",MODE_PRIVATE).edit().putString("token",token).putString("user_id",userId).putString("email",userEmail).putString("name",displayName).apply()
 }
 private fun loadHome(){
  val t=token?:run{showAuth();return};val id=userId?:run{showAuth();return}
  async{try{val a=JSONArray(req("/rest/v1/profiles?select=role,name&id=eq."+id,bearer=t))
   isAdmin=a.length()>0&&a.getJSONObject(0).optString("role")=="admin"
   if(a.length()>0)a.getJSONObject(0).optString("name").takeIf{it.isNotBlank()}?.let{displayName=it}
   runOnUiThread{showHome()}
  }catch(x:Exception){runOnUiThread{msg("Account setup needed: "+(x.message?:"unknown error"))}}}
 }
 private fun showHome(){
  val l=base();l.addView(heading("Hello, "+displayName));l.addView(TextView(this).apply{text="Your community feed";textSize=18f;setTextColor(Color.DKGRAY)})
  val input=EditText(this).apply{hint="Write a post..."};l.addView(input)
  l.addView(btn("Publish post"){
   val content=input.text.toString().trim();if(content.isBlank()){msg("Write something first.");return@btn}
   async{try{req("/rest/v1/posts","POST",JSONObject().put("user_id",userId).put("content",content),prefer="return=minimal");runOnUiThread{input.text.clear();msg("Post published online!");refreshFeed()}}
   catch(x:Exception){runOnUiThread{msg("Could not publish: "+(x.message?:"error"))}}}
  })
  if(isAdmin)l.addView(btn("ADMIN DASHBOARD"){showAdmin()})
  l.addView(btn("Refresh feed"){refreshFeed()});l.addView(btn("Sign out"){signOut()})
  feed=LinearLayout(this).apply{orientation=1};l.addView(ScrollView(this).apply{addView(feed)},LinearLayout.LayoutParams(-1,0,1f));setContentView(l);refreshFeed()
 }
 private fun fetchPosts():JSONArray=JSONArray(req("/rest/v1/posts?select=id,content,created_at,profiles(name)&order=created_at.desc&limit=100"))
 private fun refreshFeed(){if(!::feed.isInitialized)return;async{try{val rows=fetchPosts();runOnUiThread{
  feed.removeAllViews();for(i in 0 until rows.length()){val r=rows.getJSONObject(i);val author=r.optJSONObject("profiles")?.optString("name")?.takeIf{it.isNotBlank()}?:"Member"
   feed.addView(TextView(this).apply{text=author+": "+r.optString("content");textSize=17f;setTextColor(Color.DKGRAY);setBackgroundColor(Color.WHITE);setPadding(16,18,16,18)})
   feed.addView(Space(this).apply{minimumHeight=10})
  }
 }}catch(x:Exception){runOnUiThread{msg("Feed error: "+(x.message?:"error"))}}}}
 private fun showAdmin(){
  val l=base();l.addView(heading("Admin dashboard"));l.addView(TextView(this).apply{text="Administrator: "+userEmail;setTextColor(Color.DKGRAY)})
  val list=LinearLayout(this).apply{orientation=1};l.addView(ScrollView(this).apply{addView(list)},LinearLayout.LayoutParams(-1,0,1f));l.addView(btn("Back to feed"){loadHome()});setContentView(l)
  async{try{val rows=fetchPosts();runOnUiThread{list.removeAllViews();for(i in 0 until rows.length()){val r=rows.getJSONObject(i);val id=r.getString("id");val author=r.optJSONObject("profiles")?.optString("name")?:"Member"
   list.addView(TextView(this).apply{text=author+": "+r.optString("content");textSize=16f;setTextColor(Color.DKGRAY);setPadding(8,12,8,8)})
   list.addView(btn("Delete post"){async{try{req("/rest/v1/posts?id=eq."+id,"DELETE",JSONObject(),prefer="return=minimal");runOnUiThread{msg("Post deleted");showAdmin()}}catch(x:Exception){runOnUiThread{msg("Delete failed: "+(x.message?:"error"))}}}})
  }}}catch(x:Exception){runOnUiThread{msg("Admin error: "+(x.message?:"error"))}}}
 }
 private fun signOut(){async{try{req("/auth/v1/logout","POST",JSONObject(),token)}catch(_:Exception){};token=null;userId=null;userEmail=null;isAdmin=false;getSharedPreferences("bwayo_session",MODE_PRIVATE).edit().clear().apply();runOnUiThread{showAuth()}}}
}