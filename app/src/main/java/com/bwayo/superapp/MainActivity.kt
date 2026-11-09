package com.bwayo.superapp
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.app.DownloadManager
import android.os.Environment
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
 private val worker=Executors.newFixedThreadPool(4)
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
 private fun setScrollableContent(content:LinearLayout){
  val scroll=ScrollView(this).apply{isFillViewport=true;addView(content,ScrollView.LayoutParams(-1,-2))}
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
  l.addView(heading(if(isAdmin)"BWAYO ADMIN" else "BWAYO COMMUNITY"))
  l.addView(TextView(this).apply{
   text=if(isAdmin)"ADMIN CONTROL CENTER  •  Welcome, "+displayName else "Welcome, "+displayName+"  •  Community member"
   textSize=15f;gravity=Gravity.CENTER;setTextColor(if(isAdmin)Color.rgb(0,83,75) else Color.DKGRAY)
   typeface=Typeface.create("sans-serif-medium",Typeface.BOLD)
   background=rounded(if(isAdmin)Color.rgb(214,242,236) else Color.WHITE,16)
   setPadding(dp(14),dp(14),dp(14),dp(14))
  })
  l.addView(TextView(this).apply{text=if(isAdmin)"Manage the community, members and posts below." else "Share videos, connect with people and explore posts.";textSize=16f;setTextColor(Color.DKGRAY);setPadding(4,dp(12),4,dp(8))})
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
  l.addView(btn("MY PROFILE"){showProfile()});l.addView(btn("MY POSTS"){showMyPosts()});l.addView(btn("EXPLORE COMMUNITY"){showExplore()});l.addView(btn("NOTIFICATIONS"){showNotifications()});l.addView(btn("HELP & SUPPORT"){showHelp()});l.addView(btn("ABOUT BWAYO"){showAbout()});l.addView(btn("INVITE FRIENDS"){inviteFriends()})
  if(isAdmin){
   l.addView(btn("OPEN ADMIN DASHBOARD  ⚙"){showAdmin()})
  }
  val searchInput=EditText(this).apply{hint="Search posts by caption...";setSingleLine(true)}
  l.addView(searchInput)
  l.addView(btn("Search posts"){refreshFeed(searchInput.text.toString().trim())})
  l.addView(btn("Show all posts"){searchInput.text.clear();refreshFeed()})
  l.addView(btn("Refresh feed"){refreshFeed()});l.addView(btn("Sign out"){signOut()})
  feed=LinearLayout(this).apply{orientation=1};l.addView(ScrollView(this).apply{isFillViewport=true;addView(feed)},LinearLayout.LayoutParams(-1,dp(420)));setScrollableContent(l);refreshFeed()
 }
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==410&&resultCode==RESULT_OK){pickedVideo=data?.data;if(pickedVideo!=null){videoStatus?.text="Video selected ✓  Add a caption, then tap Publish post.";msg("Video selected. Add a caption and tap Publish post.")}else{videoStatus?.text="No video selected"}};if(requestCode==411&&resultCode==RESULT_OK){pickedAvatar=data?.data;if(pickedAvatar!=null)msg("Profile photo selected. Tap Save photo.")}}
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
  return JSONArray(req("/rest/v1/posts?select=id,user_id,content,media_url,created_at,profiles(name,avatar_url)&order=created_at.desc&limit=20"+filter))
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

 private fun showExplore(){
  val l=base();l.addView(heading("EXPLORE COMMUNITY"))
  l.addView(TextView(this).apply{text="Discover the latest videos and posts shared by the Bwayo community.";textSize=15f;setTextColor(Color.DKGRAY);setPadding(8,8,8,14)})
  feed=LinearLayout(this).apply{orientation=1}
  l.addView(ScrollView(this).apply{addView(feed)},LinearLayout.LayoutParams(-1,0,1f))
  l.addView(btn("Refresh explore"){refreshFeed()});l.addView(btn("Back to home"){loadHome()})
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
 private fun showAbout(){
  val l=base();l.addView(heading("ABOUT BWAYO"))
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
  val photo=ImageView(this).apply{adjustViewBounds=true;layoutParams=LinearLayout.LayoutParams(180,180).apply{gravity=Gravity.CENTER};setBackgroundColor(Color.LTGRAY)}
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
  val downloadLink="https://github.com/nabonyajenipher-cell/Bwayo-supa-app/releases/latest/download/app-debug.apk"
  val message="Join me on Bwayo Super App! Download and install the app here: "+downloadLink
  val intent=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,message)}
  startActivity(Intent.createChooser(intent,"Invite friends to Bwayo Super App"))
 }

 private fun signOut(){async{try{req("/auth/v1/logout","POST",JSONObject(),token)}catch(_:Exception){};token=null;userId=null;userEmail=null;isAdmin=false;getSharedPreferences("bwayo_session",MODE_PRIVATE).edit().clear().apply();runOnUiThread{showAuth()}}}
}