package com.bwayo.superapp
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.app.DownloadManager
import android.os.Environment
import android.content.ContentValues
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import android.widget.MediaController
import android.widget.VideoView
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import android.webkit.WebView
import android.webkit.WebViewClient
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
 private val worker=Executors.newFixedThreadPool(4)
 private var token:String?=null; private var userId:String?=null; private var userEmail:String?=null
 private var displayName="Member"; private var isAdmin=false; private lateinit var feed:LinearLayout; private var pickedVideo:Uri?=null; private var videoStatus:TextView?=null; private var pickedAvatar:Uri?=null; private var avatarUrl:String?=null; private var converterVideo:Uri?=null; private var converterStatus:TextView?=null; private var converterBusy=false
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  displayName=getSharedPreferences("bwayo_offline",MODE_PRIVATE).getString("name","BWAYO PLAYER")?:"BWAYO PLAYER"
  showOfflineHome()
 }
 private fun offlinePrefs()=getSharedPreferences("bwayo_offline",MODE_PRIVATE)
 private fun hasInternet():Boolean{
  val manager=getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
  val network=manager.activeNetwork?:return false
  val capabilities=manager.getNetworkCapabilities(network)?:return false
  return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
 }
 private fun offlinePosts():JSONArray=try{JSONArray(offlinePrefs().getString("posts","[]"))}catch(_:Exception){JSONArray()}
 private fun showOfflineHome(){
  val l=base()
  l.addView(heading("BWAYO SUPER APP"))
  l.addView(TextView(this).apply{text="OFFLINE MODE • No account or internet required";textSize=13f;gravity=Gravity.CENTER;setTextColor(Color.rgb(0,105,75));setPadding(8,14,8,14)})
  l.addView(TextView(this).apply{text="Welcome, $displayName 👋";textSize=23f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(30,41,90));setPadding(4,12,4,4)})
  l.addView(TextView(this).apply{text="Your apps and saved content stay on this phone.";textSize=14f;setTextColor(Color.DKGRAY);setPadding(4,0,4,12)})
  l.addView(btn("🌐 SWITCH TO ONLINE MODE"){if(!hasInternet()){msg("No internet connection. Connect to Wi-Fi or mobile data, then try again.");return@btn};if(token!=null&&userId!=null)loadHome() else showAuth()})
  l.addView(btn("🎮 PLAY BWAYO SUPER GAME"){showSimulator()})
  l.addView(btn("♟️ PLAY OFFLINE CHESS"){showChess()})
  l.addView(btn("🎞️ OFFLINE VIDEO CONVERTER"){showOfflineVideoConverter()})
  l.addView(btn("📝 CREATE A LOCAL POST"){createOfflinePost()})
  l.addView(btn("🌍 MY LOCAL COMMUNITY"){showOfflineFeed()})
  l.addView(btn("👤 MY PROFILE"){showOfflineProfile()})
  l.addView(btn("⚙️ SETTINGS & STORAGE"){showOfflineSettings()})
  l.addView(btn("ℹ️ APP FEATURES"){showInfoPage("BWAYO SUPER APP • OFFLINE","Works without mobile data or Wi-Fi.",listOf("City life simulator" to "Play, work, earn money, buy homes and vehicles. Your game progress is saved on this phone.","Local community posts" to "Create and read text posts stored privately on this phone.","Local profile" to "Choose a display name saved on this phone.","Offline chess" to "Play standard two-player chess without internet. Your game runs on this phone.", "Offline storage" to "Your posts and game progress remain available without internet."))})
  l.addView(TextView(this).apply{text="BWAYO EMMANUEL • OWNER & FOUNDER";textSize=11f;gravity=Gravity.CENTER;setTextColor(Color.GRAY);setPadding(4,18,4,8)})
  setScrollableContent(l)
 }
 private fun createOfflinePost(){
  val input=EditText(this).apply{hint="Write something to save on this phone";minLines=3;gravity=Gravity.TOP}
  AlertDialog.Builder(this).setTitle("Create local post").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->
   val value=input.text.toString().trim()
   if(value.isBlank()){msg("Write something first.");return@setPositiveButton}
   val posts=offlinePosts()
   posts.put(JSONObject().put("text",value).put("author",displayName).put("time",System.currentTimeMillis()))
   offlinePrefs().edit().putString("posts",posts.toString()).apply()
   msg("Post saved on this phone.");showOfflineFeed()
  }.show()
 }
 private fun showOfflineFeed(){
  val l=base();l.addView(heading("MY LOCAL COMMUNITY"))
  val posts=offlinePosts()
  if(posts.length()==0)l.addView(TextView(this).apply{text="No saved posts yet. Create your first local post.";textSize=16f;setTextColor(Color.DKGRAY);setPadding(8,20,8,20)})
  for(i in posts.length()-1 downTo 0){
   val p=posts.getJSONObject(i)
   val card=LinearLayout(this).apply{orientation=1;background=rounded(Color.WHITE,14);setPadding(dp(14),dp(12),dp(14),dp(12))}
   card.addView(TextView(this).apply{text=p.optString("author","BWAYO PLAYER");textSize=14f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(49,46,129))})
   card.addView(TextView(this).apply{text=p.optString("text");textSize=16f;setTextColor(Color.DKGRAY);setPadding(0,dp(8),0,dp(8))})
   card.addView(TextView(this).apply{text="Saved on this device";textSize=11f;setTextColor(Color.GRAY)})
   l.addView(card,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(6),0,dp(6))})
  }
  l.addView(btn("＋ NEW POST"){createOfflinePost()})
  l.addView(btn("← HOME"){showOfflineHome()})
  setScrollableContent(l)
 }
 private fun showOfflineProfile(){
  val l=base();l.addView(heading("MY OFFLINE PROFILE"))
  val name=EditText(this).apply{setText(displayName);hint="Your display name"}
  l.addView(TextView(this).apply{text="Display name";textSize=14f;setTextColor(Color.DKGRAY)})
  l.addView(name)
  l.addView(btn("SAVE PROFILE"){val v=name.text.toString().trim();if(v.isBlank()){msg("Enter a name.");return@btn};displayName=v;offlinePrefs().edit().putString("name",v).apply();msg("Profile saved on this phone.");showOfflineHome()})
  l.addView(btn("← HOME"){showOfflineHome()});setScrollableContent(l)
 }
 private fun showOfflineSettings(){
  val l=base();l.addView(heading("OFFLINE SETTINGS"))
  l.addView(TextView(this).apply{text="No sign-in, cloud account, or internet connection is needed. Your local profile, posts and simulator progress are stored on this device.";textSize=15f;setTextColor(Color.DKGRAY);setPadding(8,18,8,18)})
  l.addView(btn("DELETE LOCAL POSTS"){AlertDialog.Builder(this).setTitle("Delete saved posts?").setMessage("This only deletes posts saved inside Bwayo on this phone.").setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->offlinePrefs().edit().remove("posts").apply();msg("Local posts deleted.");showOfflineSettings()}.show()})
  l.addView(btn("← HOME"){showOfflineHome()});setScrollableContent(l)
 }
 private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
 private fun rounded(color:Int,radius:Int=18)=GradientDrawable().apply{setColor(color);cornerRadius=dp(radius).toFloat()}
 private fun base()=LinearLayout(this).apply{
  orientation=1
  background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(239,244,255),Color.rgb(248,245,255),Color.rgb(255,255,255)))
  setPadding(dp(18),dp(18),dp(18),dp(24))
 }
 private fun heading(s:String)=TextView(this).apply{
  text=s;textSize=25f;setTextColor(Color.WHITE);typeface=Typeface.create("sans-serif",Typeface.BOLD)
  gravity=Gravity.CENTER;setPadding(dp(12),dp(22),dp(12),dp(22))
  background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(30,58,138),Color.rgb(79,70,229),Color.rgb(124,58,237))).apply{cornerRadius=dp(22).toFloat()}
  elevation=dp(2).toFloat()
 }
 private fun btn(s:String,a:()->Unit)=Button(this).apply{
  text=s;textSize=14f;isAllCaps=false;typeface=Typeface.create("sans-serif-medium",Typeface.BOLD)
  setTextColor(Color.WHITE);background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(79,70,229),Color.rgb(37,99,235))).apply{cornerRadius=dp(16).toFloat()}
  elevation=dp(3).toFloat();setPadding(dp(16),dp(10),dp(16),dp(10))
  val lp=LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,dp(8),0,dp(8));layoutParams=lp
  setOnClickListener{a()}
 }
 private fun msg(s:String){Toast.makeText(this,s,Toast.LENGTH_LONG).show()}
 private fun setScrollableContent(content:LinearLayout){
  val scroll=ScrollView(this).apply{isFillViewport=true;addView(content)}
  setContentView(scroll)
 }
 private fun req(path:String,method:String="GET",body:JSONObject?=null,bearer:String?=token,prefer:String?=null):String{
  val c=URL(baseUrl+path).openConnection() as HttpURLConnection
  c.requestMethod=method;c.connectTimeout=15000;c.readTimeout=25000
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
  val authStatus=TextView(this).apply{text="";textSize=14f;gravity=Gravity.CENTER;setTextColor(Color.rgb(0,105,95));setPadding(8,8,8,8)}
  l.addView(authStatus)
  lateinit var createButton:Button
  lateinit var signInButton:Button
  fun setAuthBusy(busy:Boolean,message:String){
   createButton.isEnabled=!busy;signInButton.isEnabled=!busy
   createButton.text=if(busy)"Creating account..." else "Create account"
   signInButton.text=if(busy)"Signing in..." else "Sign in"
   authStatus.text=message
  }
  createButton=btn("Create account"){
   val name=n.text.toString().trim();val email=e.text.toString().trim();val pass=p.text.toString()
   if(name.isBlank()||email.isBlank()||pass.length<6){msg("Enter name, email and password of at least 6 characters.");return@btn}
   setAuthBusy(true,"Creating your account securely...")
   async{try{
    val payload=JSONObject().put("email",email).put("password",pass).put("data",JSONObject().put("name",name))
    val o=JSONObject(req("/auth/v1/signup","POST",payload,null))
    if(o.optString("access_token").isNotBlank()&&o.has("user")){save(o,name);runOnUiThread{loadHome()}}
    else runOnUiThread{setAuthBusy(false,"Account created. Check your email, then sign in.");msg("Account created. Check your email to confirm, then sign in.")}
   }catch(x:Exception){runOnUiThread{setAuthBusy(false,"Please try again.");msg(x.message?:"Registration failed")}}
   }
  }
  l.addView(createButton)
  signInButton=btn("Sign in"){
   val email=e.text.toString().trim();val pass=p.text.toString()
   if(email.isBlank()||pass.isBlank()){msg("Enter email and password.");return@btn}
   setAuthBusy(true,"Signing you in...")
   async{try{
    val o=JSONObject(req("/auth/v1/token?grant_type=password","POST",JSONObject().put("email",email).put("password",pass),null))
    save(o,n.text.toString().trim());runOnUiThread{loadHome()}
   }catch(x:Exception){runOnUiThread{setAuthBusy(false,"Sign in failed. Check your details and try again.");msg(x.message?:"Sign in failed")}}
   }
  }
  l.addView(signInButton)
  setScrollableContent(l)
 }
 private fun save(o:JSONObject,name:String){
  val u=o.getJSONObject("user");token=o.getString("access_token");userId=u.getString("id");userEmail=u.optString("email")
  displayName=name.ifBlank{u.optJSONObject("user_metadata")?.optString("name")?.takeIf{it.isNotBlank()}?:userEmail?:"Member"}
  getSharedPreferences("bwayo_session",MODE_PRIVATE).edit().putString("token",token).putString("user_id",userId).putString("email",userEmail).putString("name",displayName).apply()
 }
 private fun loadHome(){
  val t=token?:run{showAuth();return};val id=userId?:run{showAuth();return}
  async{try{val a=JSONArray(req("/rest/v1/profiles?select=role,name,avatar_url&id=eq."+id,bearer=t))
   isAdmin=a.length()>0&&a.getJSONObject(0).optString("role")=="admin"
   if(a.length()>0){a.getJSONObject(0).optString("name").takeIf{it.isNotBlank()}?.let{displayName=it};avatarUrl=a.getJSONObject(0).optString("avatar_url").takeIf{it.isNotBlank()}}
   runOnUiThread{showHome()}
  }catch(x:Exception){runOnUiThread{msg("Account setup needed: "+(x.message?:"unknown error"))}}}
 }
 private fun showHome(){
  val l=base()
  // Phone-first app bar
  val top=LinearLayout(this).apply{
   orientation=0;gravity=Gravity.CENTER_VERTICAL
   background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(37,99,235),Color.rgb(79,70,229))).apply{cornerRadius=dp(18).toFloat()}
   setPadding(dp(16),dp(14),dp(16),dp(14));elevation=dp(3).toFloat()
  }
  top.addView(TextView(this).apply{text="BWAYO";textSize=21f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)})
  top.addView(TextView(this).apply{text="  SUPER APP";textSize=12f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(224,231,255))})
  top.addView(Space(this),LinearLayout.LayoutParams(0,1,1f))
  top.addView(TextView(this).apply{text=if(isAdmin)"ADMIN" else "MEMBER";textSize=10f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(49,46,129));background=rounded(Color.WHITE,14);setPadding(dp(10),dp(7),dp(10),dp(7))})
  l.addView(top)
  l.addView(TextView(this).apply{
   text="Hello, "+displayName+" 👋";textSize=24f;typeface=Typeface.create("sans-serif",Typeface.BOLD)
   setTextColor(Color.rgb(30,41,90));setPadding(dp(4),dp(22),dp(4),dp(3))
  })
  l.addView(TextView(this).apply{text="Your community, all in one place.";textSize=14f;setTextColor(Color.rgb(100,105,130));setPadding(dp(4),0,dp(4),dp(16))})
  l.addView(btn("📴 SWITCH TO OFFLINE MODE"){showOfflineHome()})
  // Main phone shortcuts
  l.addView(TextView(this).apply{text="GET STARTED";textSize=12f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.GRAY);setPadding(dp(4),dp(5),dp(4),dp(8))})
  val primary=listOf<Pair<String,()->Unit>>(
   "Explore community" to {showExplore()},
   "My profile" to {showProfile()},
   "My posts" to {showMyPosts()},
   "Notifications" to {showNotifications()},
   "Play simulator" to {showSimulator()},
   "Play Chess" to {showChess()},
   "Offline video converter" to {showOfflineVideoConverter()}
  )
  var index=0
  while(index<primary.size){
   val row=LinearLayout(this).apply{orientation=0}
   for(col in 0..1){
    if(index<primary.size){
     val item=primary[index]
     val symbol=when(item.first){"Explore community"->"🌍";"My profile"->"👤";"My posts"->"📝";"Play simulator"->"🎮";"Play Chess"->"♟️";else->"🔔"}
     val card=LinearLayout(this).apply{
      orientation=1;gravity=Gravity.CENTER;background=rounded(Color.WHITE,18)
      setPadding(dp(8),dp(13),dp(8),dp(13));elevation=dp(2).toFloat()
      addView(TextView(this@MainActivity).apply{text=symbol;textSize=24f;gravity=Gravity.CENTER})
      addView(TextView(this@MainActivity).apply{text=item.first;textSize=13f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(49,46,129));gravity=Gravity.CENTER;setPadding(0,dp(5),0,0)})
      setOnClickListener{item.second()}
     }
     row.addView(card,LinearLayout.LayoutParams(0,dp(88),1f).apply{setMargins(dp(4),dp(4),dp(4),dp(4))})
     index++
    }else row.addView(Space(this),LinearLayout.LayoutParams(0,dp(88),1f))
   }
   l.addView(row)
  }
  l.addView(TextView(this).apply{text="MORE FEATURES";textSize=12f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.GRAY);setPadding(dp(4),dp(14),dp(4),dp(4))})
  val more=listOf<Pair<String,()->Unit>>(
   "App features" to {showFeatures()},
   "App settings" to {showSettings()},
   "Help & support" to {showHelp()},
   "Invite friends" to {inviteFriends()}
  )
  more.forEach{item->
   val line=LinearLayout(this).apply{
    orientation=0;gravity=Gravity.CENTER_VERTICAL;background=rounded(Color.WHITE,14)
    setPadding(dp(14),dp(4),dp(12),dp(4));elevation=dp(1).toFloat()
    addView(TextView(this@MainActivity).apply{text=item.first;textSize=14f;typeface=Typeface.create("sans-serif-medium",Typeface.BOLD);setTextColor(Color.rgb(45,55,90))},LinearLayout.LayoutParams(0,dp(48),1f))
    addView(TextView(this@MainActivity).apply{text="›";textSize=25f;setTextColor(Color.rgb(79,70,229))})
    setOnClickListener{item.second()}
   }
   val params=LinearLayout.LayoutParams(-1,dp(52));params.setMargins(dp(4),dp(3),dp(4),dp(3));l.addView(line,params)
  }
  if(isAdmin)l.addView(btn("Admin dashboard"){showAdmin()})
  l.addView(TextView(this).apply{text="CREATE A POST";textSize=14f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.GRAY);setPadding(dp(4),dp(20),dp(4),dp(7))})
  val input=EditText(this).apply{hint="What's on your mind?";minLines=3;gravity=Gravity.TOP;background=rounded(Color.WHITE,14);setPadding(dp(12),dp(10),dp(12),dp(10))}
  l.addView(input)
  l.addView(btn("Choose video"){val pick=Intent(Intent.ACTION_GET_CONTENT).apply{type="video/*";addCategory(Intent.CATEGORY_OPENABLE)};startActivityForResult(Intent.createChooser(pick,"Select a video"),410)})
  videoStatus=TextView(this).apply{text="No video selected";textSize=13f;setTextColor(Color.DKGRAY);setPadding(dp(8),dp(4),dp(8),dp(8))}
  l.addView(videoStatus)
  lateinit var publishButton:Button
  publishButton=btn("Publish post"){
   val content=input.text.toString().trim();val video=pickedVideo
   if(content.isBlank()&&video==null){msg("Write a caption or choose a video first.");return@btn}
   publishButton.isEnabled=false;publishButton.text="Publishing..."
   async{try{
    val videoUrl=video?.let{uploadVideo(it)}
    val post=JSONObject().put("user_id",userId).put("content",content.ifBlank{"Video"})
    if(videoUrl!=null)post.put("media_url",videoUrl)
    req("/rest/v1/posts","POST",post,prefer="return=minimal");pickedVideo=null
    runOnUiThread{input.text.clear();videoStatus?.text="No video selected";publishButton.isEnabled=true;publishButton.text="Publish post";msg("Post published successfully!");refreshFeed()}
   }catch(x:Exception){runOnUiThread{publishButton.isEnabled=true;publishButton.text="Publish post";msg("Could not publish: "+(x.message?:"Unknown error"))}}
   }
  }
  l.addView(publishButton)
  l.addView(TextView(this).apply{text="COMMUNITY FEED";textSize=17f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(31,41,85));setPadding(dp(4),dp(22),dp(4),dp(8))})
  val searchInput=EditText(this).apply{hint="Search community posts";setSingleLine(true)}
  l.addView(searchInput)
  l.addView(btn("Search posts"){refreshFeed(searchInput.text.toString().trim())})
  l.addView(btn("Show all posts"){searchInput.text.clear();refreshFeed()})
  l.addView(btn("Refresh feed"){refreshFeed()})
  feed=LinearLayout(this).apply{orientation=1}
  l.addView(feed)
  l.addView(btn("Sign out"){signOut()})
  setScrollableContent(l)
  refreshFeed()
 }
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==410&&resultCode==RESULT_OK){pickedVideo=data?.data;if(pickedVideo!=null){videoStatus?.text="Video selected ✓  Add a caption, then tap Publish post.";msg("Video selected. Add a caption and tap Publish post.")}else{videoStatus?.text="No video selected"}};if(requestCode==411&&resultCode==RESULT_OK){pickedAvatar=data?.data;if(pickedAvatar!=null)msg("Profile photo selected. Tap Save photo.")};if(requestCode==412&&resultCode==RESULT_OK){converterVideo=data?.data;converterStatus?.text=if(converterVideo!=null)"MP4 video selected. Choose 3GP or MP3 conversion below." else "No video selected.";converterVideo?.let{try{contentResolver.takePersistableUriPermission(it,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){}}}}

 private fun showOfflineVideoConverter(){
  val l=base()
  l.addView(heading("OFFLINE VIDEO CONVERTER"))
  l.addView(TextView(this).apply{text="Choose an MP4 video from your phone, then convert it without internet. Converted files are saved on this device.";textSize=15f;setTextColor(Color.DKGRAY);setPadding(dp(8),dp(16),dp(8),dp(12))})
  converterStatus=TextView(this).apply{text=if(converterVideo==null)"No MP4 video selected." else "MP4 video selected.";textSize=14f;setTextColor(Color.rgb(30,90,70));setPadding(dp(8),dp(8),dp(8),dp(12))}
  l.addView(btn("📁 SELECT MP4 VIDEO"){val pick=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="video/mp4";addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)};startActivityForResult(pick,412)})
  l.addView(converterStatus)
  l.addView(btn("🎬 CONVERT TO 3GP"){startOfflineConversion("3gp")})
  l.addView(btn("🎵 EXTRACT AUDIO TO MP3"){startOfflineConversion("mp3")})
  l.addView(TextView(this).apply{text="• MP4 → 3GP video\n• MP4 → MP3 audio\n• Works offline after installation\n• Original video is not deleted\n• Saves files in Downloads/Bwayo Converted.";textSize=13f;setTextColor(Color.DKGRAY);setPadding(dp(10),dp(14),dp(10),dp(18))})
  l.addView(btn("← BACK HOME"){if(token!=null)showHome() else showOfflineHome()})
  setScrollableContent(l)
 }
 private fun startOfflineConversion(format:String){
  val uri=converterVideo
  if(uri==null){msg("Select an MP4 video first.");return}
  if(converterBusy){msg("A conversion is already running.");return}
  converterBusy=true
  converterStatus?.text="Preparing video..."
  val outputName="Bwayo_"+System.currentTimeMillis()+"."+format
  async{
   var inFile:File?=null
   var outFile:File?=null
   try{
    val sourceFile=File(cacheDir,"bwayo_input_"+System.currentTimeMillis()+".mp4")
    inFile=sourceFile
    contentResolver.openInputStream(uri)?.use{source->FileOutputStream(sourceFile).use{dest->source.copyTo(dest)}}?:throw Exception("Cannot open selected video")
    val resultFile=File(cacheDir,outputName)
    outFile=resultFile
    val command=if(format=="3gp") "-y -i \"${sourceFile.absolutePath}\" -map 0:v:0 -map 0:a? -c:v mpeg4 -q:v 5 -c:a aac -b:a 128k -f 3gp \"${resultFile.absolutePath}\"" else "-y -i \"${sourceFile.absolutePath}\" -vn -c:a libmp3lame -q:a 2 -f mp3 \"${resultFile.absolutePath}\""
    FFmpegKit.executeAsync(command){session->
     val success=ReturnCode.isSuccess(session.returnCode)
     try{
      if(success&&resultFile.exists()&&resultFile.length()>0){
       val saved=saveConvertedFile(resultFile,outputName,format)
       runOnUiThread{converterBusy=false;converterStatus?.text="Conversion complete ✓ Saved: $saved";msg("Your $format file is ready and saved on this phone.")}
      }else{
       runOnUiThread{converterBusy=false;converterStatus?.text="Conversion failed. Try another MP4 video.";msg("Conversion failed. This file may use an unsupported codec.")}
      }
     }catch(e:Exception){runOnUiThread{converterBusy=false;converterStatus?.text="Could not save converted file.";msg(e.message?:"Could not save file.")}}
     finally{sourceFile.delete();resultFile.delete()}
    }
   }catch(e:Exception){
    inFile?.delete();outFile?.delete()
    runOnUiThread{converterBusy=false;converterStatus?.text="Could not start conversion.";msg(e.message?:"Conversion error.")}
   }
  }
 }
 private fun saveConvertedFile(file:File,name:String,format:String):String{
  val mime=if(format=="3gp")"video/3gpp" else "audio/mpeg"
  if(android.os.Build.VERSION.SDK_INT>=29){
   val values=ContentValues().apply{put(MediaStore.MediaColumns.DISPLAY_NAME,name);put(MediaStore.MediaColumns.MIME_TYPE,mime);put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/Bwayo Converted");put(MediaStore.MediaColumns.IS_PENDING,1)}
   val collection=if(format=="3gp")MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) else MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
   val uri=contentResolver.insert(collection,values)?:throw Exception("Could not create a Downloads file")
   try{
    contentResolver.openOutputStream(uri)?.use{out->file.inputStream().use{it.copyTo(out)}}?:throw Exception("Could not write converted file")
    val done=ContentValues().apply{put(MediaStore.MediaColumns.IS_PENDING,0)}
    contentResolver.update(uri,done,null,null)
   }catch(e:Exception){contentResolver.delete(uri,null,null);throw e}
   return "Downloads/Bwayo Converted/$name"
  }
  val dir=File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"Bwayo Converted")
  if(!dir.exists()&&!dir.mkdirs())throw Exception("Cannot create output folder")
  val target=File(dir,name);file.copyTo(target,true)
  return target.absolutePath
 }
 private fun uploadVideo(uri:Uri):String{
  val size=contentResolver.openAssetFileDescriptor(uri,"r")?.use{it.length}?:-1L
  if(size==0L)throw Exception("Selected video is empty")
  if(size>100L*1024*1024)throw Exception("Video is larger than 100 MB. Please choose a smaller video.")
  val ext=when(contentResolver.getType(uri)){"video/quicktime"->"mov";"video/webm"->"webm";else->"mp4"}
  val path=(userId?:throw Exception("Please sign in"))+"/"+java.util.UUID.randomUUID().toString()+"."+ext
  val c=URL(baseUrl+"/storage/v1/object/bwayo-videos/"+path).openConnection() as HttpURLConnection
  c.requestMethod="POST";c.connectTimeout=30000;c.readTimeout=60000;c.doOutput=true
  c.setRequestProperty("apikey",anonKey);c.setRequestProperty("Authorization","Bearer "+(token?:throw Exception("Please sign in")))
  c.setRequestProperty("Content-Type",contentResolver.getType(uri)?:"video/mp4");c.setRequestProperty("x-upsert","false")
  val source=contentResolver.openInputStream(uri)?:throw Exception("Cannot read selected video")
  c.outputStream.use{output->source.use{input->val buffer=ByteArray(64*1024);while(true){val count=input.read(buffer);if(count<0)break;output.write(buffer,0,count)}}}
  val code=c.responseCode;val response=(if(code in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty();c.disconnect()
  if(code !in 200..299)throw Exception("Video upload failed ($code): "+response)
  return baseUrl+"/storage/v1/object/public/bwayo-videos/"+path
 }
 private fun fetchPosts(search:String=""):JSONArray{
  val filter=if(search.isBlank())"" else "&content=ilike."+java.net.URLEncoder.encode("*"+search+"*","UTF-8")
  return JSONArray(req("/rest/v1/posts?select=id,user_id,content,media_url,created_at,profiles(name,avatar_url)&order=created_at.desc&limit=1000"+filter))
 }
 private fun refreshFeed(search:String=""){if(!::feed.isInitialized)return;async{try{
  val rows=fetchPosts(search)
  val ids=(0 until rows.length()).map{rows.getJSONObject(it).getString("id")}
  val likes=if(ids.isEmpty()) JSONArray() else JSONArray(req("/rest/v1/likes?select=post_id,user_id&post_id=in.("+ids.joinToString(",")+")"))
  val comments=if(ids.isEmpty()) JSONArray() else JSONArray(req("/rest/v1/comments?select=post_id&post_id=in.("+ids.joinToString(",")+")"))
  val likeCounts=HashMap<String,Int>();val myLikes=HashSet<String>();val commentCounts=HashMap<String,Int>()
  for(i in 0 until likes.length()){val x=likes.getJSONObject(i);val pid=x.optString("post_id");likeCounts[pid]=(likeCounts[pid]?:0)+1;if(x.optString("user_id")==userId)myLikes.add(pid)}
  for(i in 0 until comments.length()){val pid=comments.getJSONObject(i).optString("post_id");commentCounts[pid]=(commentCounts[pid]?:0)+1}
  runOnUiThread{
   feed.removeAllViews()
   for(i in 0 until rows.length()){
    val r=rows.getJSONObject(i);val author=r.optJSONObject("profiles")?.optString("name")?.takeIf{it.isNotBlank()}?:"Member"
    feed.addView(TextView(this).apply{text="👤  "+author+"\n\n"+r.optString("content");textSize=16f;setTextColor(Color.rgb(40,48,68));typeface=Typeface.create("sans-serif",Typeface.NORMAL);background=rounded(Color.WHITE,18);elevation=dp(2).toFloat();setPadding(dp(18),dp(18),dp(18),dp(18))})
    val media=r.optString("media_url");val pid=r.getString("id")
    if(media.startsWith("http"))feed.addView(btn("▶  Play video"){showVideo(media)})
    val actions=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL}
    val likeButton=btn((if(myLikes.contains(pid))"Unlike" else "Like")+" ("+(likeCounts[pid]?:0)+")"){toggleLike(pid)}
    val commentButton=btn("Comments ("+(commentCounts[pid]?:0)+")"){showComments(pid)}
    val shareButton=btn("Share"){sharePost(r.optString("content"),media)}
    actions.addView(likeButton,LinearLayout.LayoutParams(0,-2,1f));actions.addView(commentButton,LinearLayout.LayoutParams(0,-2,1f));actions.addView(shareButton,LinearLayout.LayoutParams(0,-2,1f))
    feed.addView(actions)
    if(media.startsWith("http"))feed.addView(btn("Download video"){downloadPost(media,pid)})
    feed.addView(Space(this).apply{minimumHeight=10})
   }
  }
 }catch(x:Exception){runOnUiThread{msg("Feed error: "+(x.message?:"error"))}}}}
 private fun showVideo(media:String){
  val player=VideoView(this)
  val panel=LinearLayout(this).apply{orientation=1;setPadding(dp(8),dp(8),dp(8),dp(8));addView(player,LinearLayout.LayoutParams(-1,dp(240)))}
  val dialog=AlertDialog.Builder(this).setTitle("Bwayo video").setView(panel).setPositiveButton("Close",null).create()
  player.setMediaController(MediaController(this).apply{setAnchorView(player)})
  player.setVideoURI(Uri.parse(media))
  player.setOnPreparedListener{it.start()}
  dialog.setOnDismissListener{player.stopPlayback()}
  dialog.show()
 }
 private fun toggleLike(postId:String){
  val uid=userId?:return
  async{try{
   val mine=JSONArray(req("/rest/v1/likes?select=id&post_id=eq."+postId+"&user_id=eq."+uid))
   if(mine.length()>0)req("/rest/v1/likes?post_id=eq."+postId+"&user_id=eq."+uid,"DELETE",JSONObject(),prefer="return=minimal")
   else req("/rest/v1/likes","POST",JSONObject().put("post_id",postId).put("user_id",uid),prefer="return=minimal")
   runOnUiThread{refreshFeed()}
  }catch(x:Exception){runOnUiThread{msg("Like failed: "+(x.message?:"error"))}}
  }
 }
 private fun showComments(postId:String){
  async{try{
   val rows=JSONArray(req("/rest/v1/comments?select=content,created_at,profiles(name)&post_id=eq."+postId+"&order=created_at.asc"))
   val commentsText=StringBuilder()
   for(i in 0 until rows.length()){
    val row=rows.getJSONObject(i);val who=row.optJSONObject("profiles")?.optString("name")?:"Member"
    commentsText.append(who).append(": ").append(row.optString("content")).append("\n\n")
   }
   runOnUiThread{
    val input=EditText(this).apply{hint="Write a comment..."}
    val panel=LinearLayout(this).apply{orientation=1;setPadding(24,8,24,8)}
    panel.addView(TextView(this).apply{text=if(commentsText.isEmpty())"No comments yet" else commentsText.toString();textSize=15f;setTextColor(Color.DKGRAY)})
    panel.addView(input)
    AlertDialog.Builder(this).setTitle("Comments").setView(panel)
     .setPositiveButton("Send"){_,_->val content=input.text.toString().trim();if(content.isNotBlank())async{try{
      req("/rest/v1/comments","POST",JSONObject().put("post_id",postId).put("user_id",userId).put("content",content),prefer="return=minimal")
      runOnUiThread{msg("Comment added");refreshFeed();showComments(postId)}
     }catch(x:Exception){runOnUiThread{msg("Comment failed: "+(x.message?:"error"))}}}}
     .setNegativeButton("Close",null).show()
   }
  }catch(x:Exception){runOnUiThread{msg("Could not load comments: "+(x.message?:"error"))}}
  }
 }
 private fun downloadPost(media:String,postId:String){
  try{
   val uri=Uri.parse(media)
   if(uri.scheme!="https"&&uri.scheme!="http"){msg("This video link cannot be downloaded.");return}
   val extension=uri.lastPathSegment?.substringAfterLast('.', "mp4")?.takeIf{it.matches(Regex("[A-Za-z0-9]{2,5}"))}?: "mp4"
   val request=DownloadManager.Request(uri)
    .setTitle("Bwayo video")
    .setDescription("Downloading video from Bwayo Super App")
    .setMimeType(if(extension=="mov")"video/quicktime" else if(extension=="webm")"video/webm" else "video/mp4")
    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"Bwayo_"+postId+"."+extension)
    .setAllowedOverMetered(true)
    .setAllowedOverRoaming(false)
   val manager=getSystemService(DOWNLOAD_SERVICE) as DownloadManager
   manager.enqueue(request)
   msg("Download started. Check your phone's Downloads folder.")
  }catch(x:Exception){msg("Could not start download: "+(x.message?:"error"))}
 }
 private fun sharePost(content:String,media:String){
  val shareText=content+(if(media.startsWith("http"))"\n"+media else "")
  val intent=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,shareText)}
  startActivity(Intent.createChooser(intent,"Share Bwayo post"))
 }

 private fun showChess(){
  val page=LinearLayout(this).apply{orientation=1;setBackgroundColor(Color.rgb(7,21,34))}
  page.addView(btn("← Back to Bwayo Super App"){if(token!=null)showHome() else showOfflineHome()})
  val game=WebView(this).apply{
   setBackgroundColor(Color.rgb(7,21,34))
   settings.javaScriptEnabled=true
   settings.domStorageEnabled=true
   settings.allowFileAccess=true
   webViewClient=WebViewClient()
   loadUrl("file:///android_asset/chess.html")
  }
  page.addView(game,LinearLayout.LayoutParams(-1,0,1f))
  setContentView(page)
 }
 private fun showSimulator(){
  val page=LinearLayout(this).apply{orientation=1;setBackgroundColor(Color.rgb(8,24,18))}
  page.addView(btn("← Back to Bwayo Super App"){showOfflineHome()})
  val game=WebView(this).apply{
   setBackgroundColor(Color.rgb(8,24,18))
   settings.javaScriptEnabled=true
   settings.domStorageEnabled=true
   settings.allowFileAccess=true
   webViewClient=WebViewClient()
   loadUrl("file:///android_asset/index.html")
  }
  page.addView(game,LinearLayout.LayoutParams(-1,0,1f))
  setContentView(page)
 }
 private fun showExplore(){
  val l=base();l.addView(heading("EXPLORE COMMUNITY"))
  l.addView(TextView(this).apply{text="Discover the latest videos and posts shared by the Bwayo community.";textSize=15f;setTextColor(Color.DKGRAY);setPadding(8,8,8,14)})
  feed=LinearLayout(this).apply{orientation=1}
  l.addView(ScrollView(this).apply{addView(feed)},LinearLayout.LayoutParams(-1,0,1f))
  l.addView(btn("Refresh explore"){refreshFeed()});l.addView(btn("Back to home"){showOfflineHome()})
  setContentView(l);refreshFeed()
 }
 private fun showMyPosts(){
  val l=base();l.addView(heading("MY POSTS"))
  val list=LinearLayout(this).apply{orientation=1}
  l.addView(ScrollView(this).apply{addView(list)},LinearLayout.LayoutParams(-1,0,1f))
  l.addView(btn("Back to home"){loadHome()});setContentView(l)
  async{try{
   val rows=JSONArray(req("/rest/v1/posts?select=id,content,media_url,created_at&user_id=eq."+userId+"&order=created_at.desc&limit=100"))
   runOnUiThread{
    if(rows.length()==0)list.addView(TextView(this).apply{text="You have not published any posts yet.";textSize=16f;setTextColor(Color.DKGRAY);setPadding(12,16,12,16)})
    for(i in 0 until rows.length()){
     val p=rows.getJSONObject(i);val media=p.optString("media_url");val pid=p.getString("id")
     list.addView(TextView(this).apply{text=p.optString("content");textSize=16f;setTextColor(Color.DKGRAY);background=rounded(Color.WHITE,16);setPadding(16,16,16,16)})
     if(media.startsWith("http")){list.addView(btn("Play my video"){showVideo(media)});list.addView(btn("Download my video"){downloadPost(media,pid)})}
     list.addView(btn("Share my post"){sharePost(p.optString("content"),media)})
     list.addView(Space(this).apply{minimumHeight=dp(8)})
    }
   }
  }catch(x:Exception){runOnUiThread{msg("Could not load your posts: "+(x.message?:"error"))}}
  }
 }
 private fun showInfoPage(title:String,intro:String,sections:List<Pair<String,String>>){
  val l=base();l.addView(heading(title))
  l.addView(TextView(this).apply{text=intro;textSize=16f;setTextColor(Color.rgb(45,65,70));background=rounded(Color.WHITE,16);setPadding(dp(16),dp(16),dp(16),dp(16))})
  for(section in sections){
   l.addView(TextView(this).apply{text=section.first;textSize=19f;typeface=Typeface.create("sans-serif",Typeface.BOLD);setTextColor(Color.rgb(0,105,95));setPadding(dp(6),dp(18),dp(6),dp(6))})
   l.addView(TextView(this).apply{text=section.second;textSize=16f;setTextColor(Color.DKGRAY);background=rounded(Color.WHITE,14);setPadding(dp(16),dp(14),dp(16),dp(14))})
  }
  l.addView(btn("Back to home"){loadHome()})
  setScrollableContent(l)
 }
 private fun showGuidelines()=showInfoPage("COMMUNITY GUIDELINES","Help keep Bwayo welcoming and useful for everyone.",listOf(
  "Be respectful" to "Treat other members kindly. Do not bully, threaten, harass or target people.",
  "Share responsibly" to "Post content you own or have permission to share. Respect other people's privacy and creative work.",
  "Keep posts appropriate" to "Do not publish illegal, dangerous, hateful, sexually exploitative or misleading content.",
  "Protect personal information" to "Do not share passwords, private contact details, financial information or another person's private information.",
  "Report problems" to "If you see harmful or abusive content, contact the app support team and include details of the post."
 ))
 private fun showPrivacy()=showInfoPage("PRIVACY & SAFETY","Use Bwayo carefully and protect your account.",listOf(
  "Your account" to "Your email and account details are used to sign you in and associate your posts with your account.",
  "Posts and videos" to "Assume content you publish may be visible to other members and may be shared outside the app. Do not post anything you want to keep private.",
  "Profile photo" to "Only choose a profile image you are comfortable displaying to the community.",
  "Account security" to "Use a strong password, do not share it, and sign out on devices that other people use.",
  "Need help?" to "Use Help & Support or Send Feedback to contact the app team."
 ))
 private fun showTerms()=showInfoPage("TERMS OF USE","By using Bwayo Super App, you agree to use it responsibly.",listOf(
  "Your responsibility" to "You are responsible for the content you upload and for ensuring you have the right to share it.",
  "Acceptable use" to "Do not misuse the app, attempt unauthorized access, disrupt services or upload harmful content.",
  "Content visibility" to "Posts and videos may be viewed, downloaded or shared by other people. Publish only content you are comfortable sharing.",
  "Moderation" to "Administrators may remove content or restrict accounts that violate these rules.",
  "Updates" to "These in-app guidelines may be updated as the community and app develop."
 ))
 private fun showFeatures()=showInfoPage("BWAYO APP FEATURES","Explore the tools available in your community app.",listOf(
  "Community feed" to "Browse recent posts and videos, refresh the feed and search captions.",
  "Create posts" to "Write a caption, choose a video and publish it for the community.",
  "Interact" to "Like posts, add comments and share posts with other apps.",
  "Your profile" to "View your account details and add a profile photo.",
  "My Posts and Explore" to "Find your own posts or discover posts from the wider community.",
  "Downloads and invitations" to "Download available videos and invite friends using the app link.",
  "Notifications" to "View activity notifications and mark them as read."
 ))
 private fun showGettingStarted()=showInfoPage("GETTING STARTED","A quick guide to using Bwayo Super App.",listOf(
  "1. Sign in" to "Create an account with your name, email and password, or sign in to an existing account.",
  "2. Complete your profile" to "Open My Profile and choose a profile photo if you wish.",
  "3. Publish" to "On the home page, write a caption, choose a video if needed, then tap Publish post.",
  "4. Explore" to "Open Explore Community, search captions or refresh the feed to see recent posts.",
  "5. Connect" to "Like, comment on and share posts. Open Notifications to check activity.",
  "6. Invite friends" to "Tap Invite Friends and share the download link."
 ))
 private fun showSettings()=showInfoPage("APP SETTINGS","Manage your session and review your account details.",listOf(
  "Signed-in account" to ("Name: "+displayName+"\\nEmail: "+(userEmail?: "Not available")),
  "Account type" to (if(isAdmin)"Administrator account" else "Community member account"),
  "Session security" to "Sign out when you finish using Bwayo on a shared phone. You can sign in again with your email and password.",
  "Sign out" to "Use the Sign out button on the home page to end your current session."
 ))
 private fun sendFeedback(){
  try{startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:support@bwayo.app")).putExtra(Intent.EXTRA_SUBJECT,"Bwayo app feedback").putExtra(Intent.EXTRA_TEXT,"My feedback about Bwayo Super App:\\n"))}
  catch(_:Exception){msg("No email app found on this phone.")}
 }
 private fun showAbout(){
  val l=base();l.addView(heading("ABOUT BWAYO"))
  l.addView(TextView(this).apply{text="OWNER & FOUNDER: BWAYO EMMANUEL";textSize=20f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(Color.rgb(13,71,161));setPadding(dp(8),dp(18),dp(8),dp(18))})
  l.addView(TextView(this).apply{
   text="Bwayo Super App is a community space where members can share posts and videos, like and comment, follow community activity through notifications, and connect by sharing the app.\\n\\nBuilt to bring the Bwayo community together."
   textSize=17f;setTextColor(Color.DKGRAY);setPadding(dp(12),dp(12),dp(12),dp(20))
  })
  l.addView(btn("Back to home"){loadHome()});setScrollableContent(l)
 }
 private fun showHelp(){
  val l=base();l.addView(heading("HELP & SUPPORT"))
  val faqs=listOf(
   "How do I publish?\\nWrite a caption, choose a video if you want, then tap Publish post.",
   "How do I interact?\\nUse Like, Comments and Share under a post.",
   "How do I download a video?\\nTap Download video and check your phone's Downloads folder.",
   "How do I change my profile photo?\\nOpen My Profile, choose a photo and tap Save photo.",
   "Having trouble signing in?\\nCheck your email and password and make sure your internet connection is working."
  )
  for(item in faqs)l.addView(TextView(this).apply{text=item;textSize=16f;setTextColor(Color.DKGRAY);background=rounded(Color.WHITE,14);setPadding(dp(14),dp(14),dp(14),dp(14))})
  l.addView(btn("Contact support by email"){try{startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:support@bwayo.app")).putExtra(Intent.EXTRA_SUBJECT,"Bwayo Super App support"))}catch(_:Exception){msg("No email app found on this phone.")}})
  l.addView(btn("Back to home"){loadHome()});setContentView(ScrollView(this).apply{addView(l)})
 }
 private fun showProfile(){
  val l=base();l.addView(heading("My profile"))
  val photo=ImageView(this).apply{adjustViewBounds=true;layoutParams=LinearLayout.LayoutParams(dp(180),dp(180)).apply{gravity=Gravity.CENTER};background=rounded(Color.WHITE,90);clipToOutline=true;scaleType=ImageView.ScaleType.CENTER_CROP;setBackgroundColor(Color.rgb(225,231,255))}
  l.addView(photo);avatarUrl?.let{loadAvatar(it,photo)}
  l.addView(btn("Choose profile photo"){val pick=Intent(Intent.ACTION_GET_CONTENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE)};startActivityForResult(Intent.createChooser(pick,"Choose profile photo"),411)})
  l.addView(btn("Save photo"){val uri=pickedAvatar;if(uri==null){msg("Choose a photo first.");return@btn};async{try{val url=uploadAvatar(uri);req("/rest/v1/profiles?id=eq."+userId,"PATCH",JSONObject().put("avatar_url",url),prefer="return=minimal");avatarUrl=url;pickedAvatar=null;runOnUiThread{msg("Profile photo saved");showProfile()}}catch(x:Exception){runOnUiThread{msg("Photo save failed: "+(x.message?: "error"))}}}})
  l.addView(TextView(this).apply{text="Name: "+displayName;textSize=18f;setTextColor(Color.DKGRAY);setPadding(8,12,8,12)})
  l.addView(TextView(this).apply{text="Email: "+(userEmail?: "Not available");textSize=16f;setTextColor(Color.DKGRAY);setPadding(8,8,8,12)})
  l.addView(TextView(this).apply{text="Account type: "+if(isAdmin)"Administrator" else "Member";textSize=16f;setTextColor(Color.DKGRAY);setPadding(8,8,8,12)})
  l.addView(btn("Back to feed"){loadHome()});setScrollableContent(l)
 }
 private fun showAdmin(){
  val l=base()
  l.addView(heading("ADMIN CONTROL CENTER"))
  l.addView(TextView(this).apply{
   text="RESTRICTED ADMIN AREA  •  "+(userEmail?: "Administrator")
   textSize=14f;typeface=Typeface.create("sans-serif-medium",Typeface.BOLD)
   setTextColor(Color.rgb(0,83,75));background=rounded(Color.rgb(214,242,236),16)
   setPadding(dp(14),dp(14),dp(14),dp(14))
  })
  val stats=TextView(this).apply{text="Loading totals...";textSize=17f;setTextColor(blue);setPadding(8,12,8,12)}
  l.addView(stats)
  val list=LinearLayout(this).apply{orientation=1}
  l.addView(ScrollView(this).apply{addView(list)},LinearLayout.LayoutParams(-1,dp(480)))
  l.addView(btn("Refresh dashboard"){showAdmin()})
  l.addView(btn("Back to feed"){loadHome()})
  setScrollableContent(l)
  async{try{
   val users=JSONArray(req("/rest/v1/profiles?select=id,name,role&order=created_at.desc"))
   val posts=fetchPosts()
   runOnUiThread{
    stats.text="Total users: "+users.length()+"    Total posts: "+posts.length()
    list.addView(heading("USERS"))
    for(i in 0 until users.length()){
     val u=users.getJSONObject(i);val uid=u.getString("id");val uname=u.optString("name","Member");val role=u.optString("role","user")
     val row=LinearLayout(this).apply{orientation=1;setPadding(8,8,8,8);setBackgroundColor(Color.WHITE)}
     row.addView(TextView(this).apply{text=uname+"  •  "+role.uppercase();textSize=16f;setTextColor(Color.DKGRAY)})
     if(uid!=userId){
      row.addView(btn(if(role=="admin")"Remove admin access" else "Make admin"){
       val newRole=if(role=="admin")"user" else "admin"
       async{try{req("/rest/v1/rpc/bwayo_set_user_role","POST",JSONObject().put("target_user",uid).put("new_role",newRole));runOnUiThread{msg("User role updated");showAdmin()}}
       catch(x:Exception){runOnUiThread{msg("Role update failed: "+(x.message?:"error"))}}}
      })
     }
     list.addView(row)
    }
    list.addView(heading("POSTS"))
    for(i in 0 until posts.length()){
     val p=posts.getJSONObject(i);val pid=p.getString("id");val author=p.optJSONObject("profiles")?.optString("name")?:"Member"
     list.addView(TextView(this).apply{text=author+": "+p.optString("content");textSize=16f;setTextColor(Color.DKGRAY);setPadding(8,12,8,8)})
     list.addView(btn("Delete post"){async{try{req("/rest/v1/posts?id=eq."+pid,"DELETE",JSONObject(),prefer="return=minimal");runOnUiThread{msg("Post deleted");showAdmin()}}catch(x:Exception){runOnUiThread{msg("Delete failed: "+(x.message?:"error"))}}}})
    }
   }
  }catch(x:Exception){runOnUiThread{msg("Admin dashboard error: "+(x.message?:"error"))}}
  }
 }

 private fun uploadAvatar(uri:Uri):String{
  val bytes=contentResolver.openInputStream(uri)?.use{it.readBytes()}?:throw Exception("Cannot read photo")
  if(bytes.isEmpty()||bytes.size>8*1024*1024)throw Exception("Photo must be smaller than 8 MB")
  val mime=contentResolver.getType(uri)?: "image/jpeg"
  val ext=if(mime=="image/png")"png" else if(mime=="image/webp")"webp" else "jpg"
  val path=(userId?:throw Exception("Sign in first"))+"/"+java.util.UUID.randomUUID().toString()+"."+ext
  val c=URL(baseUrl+"/storage/v1/object/bwayo-avatars/"+path).openConnection() as HttpURLConnection
  c.requestMethod="POST";c.connectTimeout=30000;c.readTimeout=60000;c.doOutput=true
  c.setRequestProperty("apikey",anonKey);c.setRequestProperty("Authorization","Bearer "+(token?:throw Exception("Sign in first")))
  c.setRequestProperty("Content-Type",mime);c.setRequestProperty("x-upsert","false")
  c.outputStream.use{it.write(bytes)}
  val code=c.responseCode;val response=(if(code in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty();c.disconnect()
  if(code !in 200..299)throw Exception("Photo upload failed ($code): "+response)
  return baseUrl+"/storage/v1/object/public/bwayo-avatars/"+path
 }
 private fun loadAvatar(url:String,view:ImageView){
  async{try{val c=URL(url).openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=15000
   val bitmap=android.graphics.BitmapFactory.decodeStream(c.inputStream);c.disconnect()
   if(bitmap!=null)runOnUiThread{view.setImageBitmap(bitmap);view.scaleType=ImageView.ScaleType.CENTER_CROP}
  }catch(_:Exception){}}
 }
 private fun showNotifications(){
  val l=base();l.addView(heading("Notifications"))
  val list=LinearLayout(this).apply{orientation=1}
  l.addView(ScrollView(this).apply{addView(list)},LinearLayout.LayoutParams(-1,0,1f))
  l.addView(btn("Back to feed"){loadHome()});setContentView(l)
  async{try{
   val rows=JSONArray(req("/rest/v1/notifications?select=id,message,created_at,is_read&order=created_at.desc&limit=100"))
   runOnUiThread{
    if(rows.length()==0)list.addView(TextView(this).apply{text="You have no notifications yet.";textSize=16f;setTextColor(Color.DKGRAY)})
    for(i in 0 until rows.length()){
     val n=rows.getJSONObject(i);val nid=n.getString("id")
     list.addView(TextView(this).apply{text=(if(n.optBoolean("is_read"))"○ " else "● ")+n.optString("message");textSize=16f;setTextColor(Color.DKGRAY);setPadding(12,16,12,16);setBackgroundColor(Color.WHITE)})
     if(!n.optBoolean("is_read"))list.addView(btn("Mark as read"){async{try{req("/rest/v1/notifications?id=eq."+nid,"PATCH",JSONObject().put("is_read",true),prefer="return=minimal");runOnUiThread{showNotifications()}}catch(x:Exception){runOnUiThread{msg("Could not update notification")}}}})
    }
   }
  }catch(x:Exception){runOnUiThread{msg("Notifications error: "+(x.message?: "error"))}}
  }
 }


 private fun inviteFriends(){
  val downloadLink="https://github.com/nabonyajenipher-cell/Bwayo-supa-app/releases/latest/download/Bwayo-Super-App.apk"
  val message="Join me on Bwayo Super App! Download and install the app here: "+downloadLink
  val intent=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,message)}
  startActivity(Intent.createChooser(intent,"Invite friends to Bwayo Super App"))
 }

 private fun signOut(){async{try{req("/auth/v1/logout","POST",JSONObject(),token)}catch(_:Exception){};token=null;userId=null;userEmail=null;isAdmin=false;getSharedPreferences("bwayo_session",MODE_PRIVATE).edit().clear().apply();runOnUiThread{showAuth()}}}
}