package com.bwayo.superapp
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
 private var displayName="Member"; private var isAdmin=false; private lateinit var feed:LinearLayout; private var pickedVideo:Uri?=null; private var videoStatus:TextView?=null; private var pickedAvatar:Uri?=null; private var avatarUrl:String?=null
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  val p=getSharedPreferences("bwayo_session",MODE_PRIVATE); token=p.getString("token",null); userId=p.getString("user_id",null); userEmail=p.getString("email",null); displayName=p.getString("name","Member")?:"Member"
  if(token!=null&&userId!=null) loadHome() else showAuth()
 }
 private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
 private fun rounded(color:Int,radius:Int=18)=GradientDrawable().apply{setColor(color);cornerRadius=dp(radius).toFloat()}
 private fun base()=LinearLayout(this).apply{
  orientation=1
  background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(242,247,247),Color.rgb(250,252,252)))
  setPadding(dp(18),dp(18),dp(18),dp(22))
 }
 private fun heading(s:String)=TextView(this).apply{
  text=s;textSize=26f;setTextColor(Color.rgb(15,48,58));typeface=Typeface.create("sans-serif",Typeface.BOLD)
  gravity=Gravity.CENTER;setPadding(dp(8),dp(18),dp(8),dp(20))
 }
 private fun btn(s:String,a:()->Unit)=Button(this).apply{
  text=s;textSize=14f;isAllCaps=false;typeface=Typeface.create("sans-serif-medium",Typeface.BOLD)
  setTextColor(Color.WHITE);background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(0,137,123),Color.rgb(0,105,125))).apply{cornerRadius=dp(18).toFloat()}
  elevation=dp(4).toFloat();setPadding(dp(16),dp(9),dp(16),dp(9))
  val lp=LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(7),0,dp(7));layoutParams=lp
  setOnClickListener{a()}
 }
 private fun msg(s:String){Toast.makeText(this,s,Toast.LENGTH_LONG).show()}
 private fun req(path:String,method:String="GET",body:JSONObject?=null,bearer:String?=token,prefer:String?=null):String{
  val c=URL(baseUrl+path).openConnection() as HttpURLConnection
  c.requestMethod=method;c.connectTimeout=60000;c.readTimeout=60000
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
  async{try{val a=JSONArray(req("/rest/v1/profiles?select=role,name,avatar_url&id=eq."+id,bearer=t))
   isAdmin=a.length()>0&&a.getJSONObject(0).optString("role")=="admin"
   if(a.length()>0){a.getJSONObject(0).optString("name").takeIf{it.isNotBlank()}?.let{displayName=it};avatarUrl=a.getJSONObject(0).optString("avatar_url").takeIf{it.isNotBlank()}}
   runOnUiThread{showHome()}
  }catch(x:Exception){runOnUiThread{msg("Account setup needed: "+(x.message?:"unknown error"))}}}
 }
 private fun showHome(){
  val l=base();l.addView(heading("Hello, "+displayName));l.addView(TextView(this).apply{text="Your community feed";textSize=18f;setTextColor(Color.DKGRAY)})
  val input=EditText(this).apply{hint="Write a post..."};l.addView(input)
  l.addView(btn("Choose video to upload"){
   val pick=Intent(Intent.ACTION_GET_CONTENT).apply{type="video/*";addCategory(Intent.CATEGORY_OPENABLE)}
   startActivityForResult(Intent.createChooser(pick,"Select a video"),410)
  })
  videoStatus=TextView(this).apply{text="No video selected";textSize=14f;setTextColor(Color.DKGRAY);setPadding(8,6,8,10)}
  l.addView(videoStatus)
  lateinit var publishButton:Button
  publishButton=btn("Publish post"){
   val content=input.text.toString().trim();val video=pickedVideo
   if(content.isBlank()&&video==null){msg("Write a caption or choose a video first.");return@btn}
   publishButton.isEnabled=false
   publishButton.text="Publishing..."
   msg("Publishing started. Please wait...")
   async{try{
    val videoUrl=video?.let{uploadVideo(it)}
    val post=JSONObject().put("user_id",userId).put("content",content.ifBlank{"Video"})
    if(videoUrl!=null)post.put("media_url",videoUrl)
    req("/rest/v1/posts","POST",post,prefer="return=minimal")
    pickedVideo=null
    runOnUiThread{input.text.clear();videoStatus?.text="No video selected";publishButton.isEnabled=true;publishButton.text="Publish post";msg("Post published successfully!");refreshFeed()}
   }catch(x:Exception){runOnUiThread{publishButton.isEnabled=true;publishButton.text="Publish post";msg("Could not publish: "+(x.message?:"Unknown error"))}}
  }
  }
  l.addView(publishButton)
  l.addView(btn("MY PROFILE"){showProfile()});l.addView(btn("NOTIFICATIONS"){showNotifications()})
  if(isAdmin)l.addView(btn("ADMIN DASHBOARD"){showAdmin()})
  l.addView(btn("Refresh feed"){refreshFeed()});l.addView(btn("Sign out"){signOut()})
  feed=LinearLayout(this).apply{orientation=1};l.addView(ScrollView(this).apply{addView(feed)},LinearLayout.LayoutParams(-1,0,1f));setContentView(l);refreshFeed()
 }
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==410&&resultCode==RESULT_OK){pickedVideo=data?.data;if(pickedVideo!=null){videoStatus?.text="Video selected ✓  Add a caption, then tap Publish post.";msg("Video selected. Add a caption and tap Publish post.")}else{videoStatus?.text="No video selected"}};if(requestCode==411&&resultCode==RESULT_OK){pickedAvatar=data?.data;if(pickedAvatar!=null)msg("Profile photo selected. Tap Save photo.")}}
 private fun uploadVideo(uri:Uri):String{
  val bytes=contentResolver.openInputStream(uri)?.use{it.readBytes()}?:throw Exception("Cannot read selected video")
  if(bytes.isEmpty())throw Exception("Selected video is empty")
  if(bytes.size>100*1024*1024)throw Exception("Video is larger than 100 MB. Please choose a smaller video.")
  val ext=when(contentResolver.getType(uri)){"video/quicktime"->"mov";"video/webm"->"webm";else->"mp4"}
  val path=(userId?:throw Exception("Please sign in"))+"/"+java.util.UUID.randomUUID().toString()+"."+ext
  val c=URL(baseUrl+"/storage/v1/object/bwayo-videos/"+path).openConnection() as HttpURLConnection
  c.requestMethod="POST";c.connectTimeout=30000;c.readTimeout=60000;c.doOutput=true
  c.setRequestProperty("apikey",anonKey);c.setRequestProperty("Authorization","Bearer "+(token?:throw Exception("Please sign in")))
  c.setRequestProperty("Content-Type",contentResolver.getType(uri)?:"video/mp4");c.setRequestProperty("x-upsert","false")
  c.outputStream.use{it.write(bytes)}
  val code=c.responseCode;val response=(if(code in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty();c.disconnect()
  if(code !in 200..299)throw Exception("Video upload failed ($code): "+response)
  return baseUrl+"/storage/v1/object/public/bwayo-videos/"+path
 }
 private fun fetchPosts():JSONArray=JSONArray(req("/rest/v1/posts?select=id,user_id,content,media_url,created_at,profiles(name,avatar_url)&order=created_at.desc&limit=100"))
 private fun refreshFeed(){if(!::feed.isInitialized)return;async{try{val rows=fetchPosts();runOnUiThread{
  feed.removeAllViews();for(i in 0 until rows.length()){val r=rows.getJSONObject(i);val author=r.optJSONObject("profiles")?.optString("name")?.takeIf{it.isNotBlank()}?:"Member"
   feed.addView(TextView(this).apply{text="👤  "+author+"\n\n"+r.optString("content");textSize=16f;setTextColor(Color.rgb(40,48,68));typeface=Typeface.create("sans-serif",Typeface.NORMAL);background=rounded(Color.WHITE,18);elevation=dp(2).toFloat();setPadding(dp(18),dp(18),dp(18),dp(18))})
   val media=r.optString("media_url")
   if(media.startsWith("http")){
    val player=VideoView(this);player.setVideoURI(Uri.parse(media));player.setMediaController(MediaController(this).apply{setAnchorView(player)})
    feed.addView(player,LinearLayout.LayoutParams(-1,520))
   }
   val pid=r.getString("id");val uid=userId?: ""
   val actions=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL}
   val likeButton=btn("Like"){toggleLike(pid)}
   val commentButton=btn("Comments"){showComments(pid)}
   val shareButton=btn("Share"){sharePost(r.optString("content"),media)}
   actions.addView(likeButton,LinearLayout.LayoutParams(0,-2,1f))
   actions.addView(commentButton,LinearLayout.LayoutParams(0,-2,1f))
   actions.addView(shareButton,LinearLayout.LayoutParams(0,-2,1f))
   feed.addView(actions)
   async{try{
    val likes=JSONArray(req("/rest/v1/likes?select=id&post_id=eq."+pid))
    val mine=JSONArray(req("/rest/v1/likes?select=id&post_id=eq."+pid+"&user_id=eq."+uid))
    val comments=JSONArray(req("/rest/v1/comments?select=id&post_id=eq."+pid))
    runOnUiThread{
     likeButton.text=(if(mine.length()>0)"Unlike" else "Like")+" ("+likes.length()+")"
     commentButton.text="Comments ("+comments.length()+")"
    }
   }catch(_:Exception){}}
   feed.addView(Space(this).apply{minimumHeight=10})
  }
 }}catch(x:Exception){runOnUiThread{msg("Feed error: "+(x.message?:"error"))}}}}
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
 private fun sharePost(content:String,media:String){
  val shareText=content+(if(media.startsWith("http"))"\n"+media else "")
  val intent=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,shareText)}
  startActivity(Intent.createChooser(intent,"Share Bwayo post"))
 }
 private fun showProfile(){
  val l=base();l.addView(heading("My profile"))
  val photo=ImageView(this).apply{adjustViewBounds=true;layoutParams=LinearLayout.LayoutParams(180,180).apply{gravity=Gravity.CENTER};setBackgroundColor(Color.LTGRAY)}
  l.addView(photo);avatarUrl?.let{loadAvatar(it,photo)}
  l.addView(btn("Choose profile photo"){val pick=Intent(Intent.ACTION_GET_CONTENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE)};startActivityForResult(Intent.createChooser(pick,"Choose profile photo"),411)})
  l.addView(btn("Save photo"){val uri=pickedAvatar;if(uri==null){msg("Choose a photo first.");return@btn};async{try{val url=uploadAvatar(uri);req("/rest/v1/profiles?id=eq."+userId,"PATCH",JSONObject().put("avatar_url",url),prefer="return=minimal");avatarUrl=url;pickedAvatar=null;runOnUiThread{msg("Profile photo saved");showProfile()}}catch(x:Exception){runOnUiThread{msg("Photo save failed: "+(x.message?: "error"))}}}})
  l.addView(TextView(this).apply{text="Name: "+displayName;textSize=18f;setTextColor(Color.DKGRAY);setPadding(8,12,8,12)})
  l.addView(TextView(this).apply{text="Email: "+(userEmail?: "Not available");textSize=16f;setTextColor(Color.DKGRAY);setPadding(8,8,8,12)})
  l.addView(TextView(this).apply{text="Account type: "+if(isAdmin)"Administrator" else "Member";textSize=16f;setTextColor(Color.DKGRAY);setPadding(8,8,8,12)})
  l.addView(btn("Back to feed"){loadHome()});setContentView(l)
 }
 private fun showAdmin(){
  val l=base();l.addView(heading("Admin dashboard"))
  l.addView(TextView(this).apply{text="Administrator: "+userEmail;setTextColor(Color.DKGRAY)})
  val stats=TextView(this).apply{text="Loading totals...";textSize=17f;setTextColor(blue);setPadding(8,12,8,12)}
  l.addView(stats)
  val list=LinearLayout(this).apply{orientation=1}
  l.addView(ScrollView(this).apply{addView(list)},LinearLayout.LayoutParams(-1,0,1f))
  l.addView(btn("Refresh dashboard"){showAdmin()})
  l.addView(btn("Back to feed"){loadHome()})
  setContentView(l)
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

 private fun signOut(){async{try{req("/auth/v1/logout","POST",JSONObject(),token)}catch(_:Exception){};token=null;userId=null;userEmail=null;isAdmin=false;getSharedPreferences("bwayo_session",MODE_PRIVATE).edit().clear().apply();runOnUiThread{showAuth()}}}
}