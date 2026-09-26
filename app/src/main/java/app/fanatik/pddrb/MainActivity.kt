package app.fanatik.pddrb

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import app.fanatik.pddrb.data.topics
import app.fanatik.pddrb.data.qs

data class Topic(val title:String,val subtitle:String)
data class Question(val topic:String,val text:String,val answers:List<String>,val correct:Int,val explanation:String,val visual:Int=0)
data class Rank(val minXp:Int,val title:String)

private val ranks=listOf(
 Rank(0,"Динька пока пассажир"),
 Rank(60,"Дёня нашёл педали"),
 Rank(150,"Денчик выехал со двора"),
 Rank(300,"Петрович уже что-то подозревает"),
 Rank(500,"Динька приручает перекрёстки"),
 Rank(800,"Денчик опасно обучаем"),
 Rank(1200,"Петрович, документы можно не доставать"),
 Rank(1700,"Дёня пугающе близок к правам"),
 Rank(2300,"ГАИ проверяет ответы Денчика дважды"),
 Rank(3000,"Петрович, проезжайте"),
 Rank(4000,"ДИНЬКА ВЫЕХАЛ. ПРЯЧЬТЕСЬ.")
)
private fun rankFor(xp:Int)=ranks.last{xp>=it.minXp}
private fun nextRank(xp:Int)=ranks.firstOrNull{it.minXp>xp}
private fun reaction(correct:Boolean,streak:Int):String?{
 val roll=Random.nextInt(100)
 if(roll>58 && streak!=3 && streak!=5 && streak!=7 && streak!=10) return null
 return when{
  streak>=10 -> "ПЕТРОВИЧ В РЕЖИМЕ БОССА: 10 подряд. Инспектор листает методичку."
  streak==7 -> "Динька оформил 7 подряд. Где-то загрустил инструктор."
  streak==5 -> "Денчик поймал серию ×5. Петрович сегодня опасен."
  streak==3 -> "Дёня пошёл серией ×3. Не спугнуть."
  correct -> listOf("Динька сегодня подозрительно хорош.","Дёня включил режим «я это знал».","Петрович набирает обороты.","Денчик не тыкал. Денчик РЕШАЛ.","Динька уверенно движется к категории «страшно выпускать».","Петрович только что избежал маршрутки.","Дёня: +1 причина всё-таки дать ему права.").random()
  else -> listOf("Динька… инспектор всё видел.","Петрович, маршрутку пока не отменяем.","Дёня, это сейчас было творческое ПДД.","Денчик выбрал секретный пятый вариант.","ГАИ облегчённо выдохнуло.","Петрович создал новое правило. Жаль, оно не действует.","Динька, дорога запомнила этот момент.","Дёня уверенно поехал не туда.").random()
 }
}

private val AppBg=Color(0xFF050B13)
private val AppSurface=Color(0xFF09131F)
private val AppCard=Color(0xFF0E1A29)
private val AppCard2=Color(0xFF132235)
private val AppStroke=Color(0xFF1D3045)
private val Accent=Color(0xFF7857FF)
private val Accent2=Color(0xFF4169E8)
private val Good=Color(0xFF22C983)
private val GoodBg=Color(0xFF103A2D)
private val Bad=Color(0xFFFF5C68)
private val BadBg=Color(0xFF401F2A)
private val TextPrimary=Color(0xFFF6F8FC)
private val TextMuted=Color(0xFF9BAABD)
private val Orange=Color(0xFFFFB03A)

private val pddDarkScheme=darkColorScheme(
 primary=Accent,secondary=Accent2,background=AppBg,surface=AppSurface,surfaceVariant=AppCard,
 onPrimary=Color.White,onSecondary=Color.White,onBackground=TextPrimary,onSurface=TextPrimary,
 onSurfaceVariant=TextMuted,error=Bad
)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  window.statusBarColor=android.graphics.Color.rgb(7,17,29)
  window.navigationBarColor=android.graphics.Color.rgb(7,17,29)
  setContent{MaterialTheme(colorScheme=pddDarkScheme){App(this)}}
 }
}

@Composable fun App(context:Context){
 var screen by remember{mutableStateOf("home")}
 var current by remember{mutableStateOf(qs.first())}
 var pool by remember{mutableStateOf(qs)}
 var index by remember{mutableIntStateOf(0)}
 var correctCount by remember{mutableIntStateOf(0)}
 val prefs=remember{context.getSharedPreferences("denis_progress",Context.MODE_PRIVATE)}
 var xp by remember{mutableIntStateOf(prefs.getInt("xp",0))}
 var streak by remember{mutableIntStateOf(prefs.getInt("streak",0))}
 var mistakes by remember{mutableStateOf(prefs.getStringSet("mistakes",emptySet())?.toSet()?:emptySet())}
 var solved by remember{mutableStateOf(prefs.getStringSet("solved",emptySet())?.toSet()?:emptySet())}
 var attempts by remember{mutableIntStateOf(prefs.getInt("attempts",0))}
 var rightTotal by remember{mutableIntStateOf(prefs.getInt("right_total",0))}
 val accuracy=if(attempts==0)0 else (rightTotal*100/attempts)

 fun alternatingExam(count:Int=10):List<Question>{
  val visual=qs.filter{it.visual>=100}.shuffled().toMutableList()
  val theory=qs.filter{it.visual==0}.shuffled().toMutableList()
  val result=mutableListOf<Question>()
  repeat(count){
   val preferred=if(it%2==0)visual else theory
   val fallback=if(it%2==0)theory else visual
   if(preferred.isNotEmpty())result+=preferred.removeAt(0) else if(fallback.isNotEmpty())result+=fallback.removeAt(0)
  }
  return result
 }
 fun start(list:List<Question>){if(list.isEmpty())return;pool=list;index=0;correctCount=0;current=pool.first();screen="question"}
 fun startTopic(title:String){start(qs.filter{it.topic==title})}
 fun next(){if(index+1<pool.size){index++;current=pool[index]}else screen="home"}

 Surface(Modifier.fillMaxSize(),color=AppBg){
  when(screen){
   "home"->HomeScreen(
    xp=xp,streak=streak,solved=solved.size,accuracy=accuracy,
    onTopics={screen="topics"},onExam={start(alternatingExam())},onErrors={screen="errors"},
    onImages={start(qs.filter{it.visual>=100}.shuffled())},onProfile={screen="profile"},
    onSigns={startTopic("Дорожные знаки")},onMarking={startTopic("Дорожная разметка")},
    onTraffic={startTopic("Светофор и регулировщик")}
   )
   "topics"->TopicScreen(
    solved=solved,onHome={screen="home"},onTopic={start(it)},onExam={start(alternatingExam())},
    onErrors={screen="errors"},onProfile={screen="profile"}
   )
   "profile"->ProfileScreen(
    xp=xp,streak=streak,mistakes=mistakes.size,
    onHome={screen="home"},onTopics={screen="topics"},onExam={start(alternatingExam())},onErrors={screen="errors"}
   )
   "question"->QuestionView(current,index,pool.size,correctCount,xp,streak,{screen="home"},{ok->
    attempts++
    solved=solved+current.text
    if(ok){correctCount++;rightTotal++;streak++;xp+=10+streak.coerceAtMost(10);mistakes=mistakes-current.text}else{streak=0;mistakes=mistakes+current.text}
    prefs.edit()
     .putInt("xp",xp).putInt("streak",streak)
     .putInt("attempts",attempts).putInt("right_total",rightTotal)
     .putStringSet("mistakes",mistakes).putStringSet("solved",solved).apply()
   },{next()})
   else->ErrorScreen(
    mistakes=mistakes,onStart={start(qs.filter{mistakes.contains(it.text)})},
    onHome={screen="home"},onTopics={screen="topics"},onExam={start(alternatingExam())},onProfile={screen="profile"}
   )
  }
 }
}

@Composable private fun NavColors()=NavigationBarItemDefaults.colors(
 selectedIconColor=Color.White,selectedTextColor=Color.White,indicatorColor=Accent.copy(alpha=.24f),
 unselectedIconColor=TextMuted,unselectedTextColor=TextMuted
)

@Composable
private fun BottomNav(selected:String,onHome:()->Unit,onTopics:()->Unit,onExam:()->Unit,onErrors:()->Unit,onProfile:()->Unit){
 Surface(color=Color(0xF20A1420),shadowElevation=16.dp){
  NavigationBar(
   containerColor=Color.Transparent,
   tonalElevation=0.dp,
   windowInsets=NavigationBarDefaults.windowInsets
  ){
   NavigationBarItem(selected=selected=="home",onClick=onHome,icon={Icon(Icons.Rounded.Home,null)},label={Text("Главная")},colors=NavColors())
   NavigationBarItem(selected=selected=="topics",onClick=onTopics,icon={Icon(Icons.Rounded.List,null)},label={Text("Категории")},colors=NavColors())
   NavigationBarItem(selected=selected=="exam",onClick=onExam,icon={Icon(Icons.Rounded.School,null)},label={Text("Экзамен")},colors=NavColors())
   NavigationBarItem(selected=selected=="errors",onClick=onErrors,icon={Icon(Icons.Rounded.Error,null)},label={Text("Ошибки")},colors=NavColors())
   NavigationBarItem(selected=selected=="profile",onClick=onProfile,icon={Icon(Icons.Rounded.Person,null)},label={Text("Профиль")},colors=NavColors())
  }
 }
}

@Composable
private fun StatTile(icon:androidx.compose.ui.graphics.vector.ImageVector,value:String,label:String,tint:Color,modifier:Modifier=Modifier){
 Surface(modifier=modifier,shape=RoundedCornerShape(14.dp),color=Color(0xAA142337),border=BorderStroke(1.dp,AppStroke)){
  Row(Modifier.padding(horizontal=10.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){
   Surface(shape=CircleShape,color=tint.copy(alpha=.13f)){Icon(icon,null,tint=tint,modifier=Modifier.padding(6.dp).size(16.dp))}
   Spacer(Modifier.width(7.dp))
   Column{Text(value,color=TextPrimary,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.bodyLarge);Text(label,color=TextMuted,style=MaterialTheme.typography.labelSmall)}
  }
 }
}

@Composable
private fun MiniActionCard(title:String,subtitle:String,icon:androidx.compose.ui.graphics.vector.ImageVector,tint:Color,onClick:()->Unit,modifier:Modifier=Modifier){
 Card(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=AppCard),border=BorderStroke(1.dp,AppStroke)){
  Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
   Surface(shape=RoundedCornerShape(12.dp),color=tint.copy(alpha=.16f)){Icon(icon,null,tint=tint,modifier=Modifier.padding(9.dp).size(22.dp))}
   Spacer(Modifier.width(11.dp))
   Column(Modifier.weight(1f)){
    Text(title,color=TextPrimary,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleSmall)
    Text(subtitle,color=TextMuted,style=MaterialTheme.typography.labelMedium)
   }
   Icon(Icons.Rounded.ChevronRight,null,tint=TextMuted,modifier=Modifier.size(20.dp))
  }
 }
}

@Composable
private fun PremiumProfileCard(xp:Int,streak:Int,mistakes:Int){
 val rank=rankFor(xp);val next=nextRank(xp)
 Surface(shape=RoundedCornerShape(24.dp),color=Color.Transparent,border=BorderStroke(1.dp,Color(0xFF263A52))){
  Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF17263B),Color(0xFF111C2C),Color(0xFF10182A))))){
   Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){
     Surface(shape=CircleShape,color=Color(0xFF5E46B8),border=BorderStroke(1.dp,Color(0xFF8D74FF))){
      Box(Modifier.size(52.dp),contentAlignment=Alignment.Center){Icon(Icons.Rounded.DirectionsCar,null,tint=Color.White,modifier=Modifier.size(26.dp))}
     }
     Spacer(Modifier.width(12.dp))
     Column(Modifier.weight(1f)){
      Text(rank.title,color=TextPrimary,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.titleMedium)
      Text("Уровень "+(ranks.indexOf(rank)+1),color=TextMuted,style=MaterialTheme.typography.labelLarge)
     }
     Surface(shape=RoundedCornerShape(12.dp),color=Accent.copy(alpha=.14f)){Text(xp.toString()+" XP",color=Color(0xFFD2C7FF),fontWeight=FontWeight.ExtraBold,modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp))}
    }
    if(next!=null){
     val progress=((xp-rank.minXp).toFloat()/(next.minXp-rank.minXp)).coerceIn(0f,1f)
     LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(6.dp),color=Accent,trackColor=Color(0xFF2A374C))
    }
    Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
     StatTile(Icons.Rounded.LocalFireDepartment,streak.toString(),"серия",Orange,Modifier.weight(1f))
     StatTile(Icons.Rounded.CheckCircle,(xp/12).coerceAtLeast(0).toString(),"решено",Good,Modifier.weight(1f))
     StatTile(Icons.Rounded.Error,mistakes.toString(),"ошибки",Bad,Modifier.weight(1f))
    }
   }
  }
 }
}

@Composable
private fun ContinueHero(onClick:()->Unit){
 Card(onClick=onClick,shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.Transparent),modifier=Modifier.fillMaxWidth()){
  Box(Modifier.fillMaxWidth().height(116.dp).background(Brush.horizontalGradient(listOf(Color(0xFF7047FF),Color(0xFF542CF1),Color(0xFF223EAA))))){
   Image(painter=painterResource(R.drawable.scene_intersection),contentDescription=null,modifier=Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.50f),contentScale=ContentScale.Crop,alpha=.48f)
   Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0xE86E45FF),Color(0xB85631F2),Color(0x11253CB1)))))
   Row(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){
    Surface(shape=RoundedCornerShape(14.dp),color=Color.White.copy(alpha=.15f)){Icon(Icons.Rounded.PlayArrow,null,tint=Color.White,modifier=Modifier.padding(10.dp).size(24.dp))}
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)){
     Text("Продолжить обучение",color=Color.White,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge)
     Text("Ситуации с картинками • "+qs.count{it.visual>=100}+" новых",color=Color(0xFFE5DEFF),style=MaterialTheme.typography.bodySmall)
    }
    Icon(Icons.Rounded.ArrowForward,null,tint=Color.White)
   }
  }
 }
}

@Composable
private fun VisualModeCard(onClick:()->Unit){
 Card(onClick=onClick,shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=AppCard),border=BorderStroke(1.dp,Color(0xFF1A4F4A))){
  Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
   Image(painter=painterResource(R.drawable.scene_crosswalk),contentDescription=null,modifier=Modifier.size(width=92.dp,height=66.dp).clip(RoundedCornerShape(14.dp)),contentScale=ContentScale.Crop)
   Spacer(Modifier.width(12.dp))
   Column(Modifier.weight(1f)){
    Text("Ситуации с картинками",color=TextPrimary,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(3.dp))
    Text("Отдельный графический режим",color=TextMuted,style=MaterialTheme.typography.bodySmall)
   }
   Icon(Icons.Rounded.ArrowForward,null,tint=Good)
  }
 }
}

@Composable
fun HomeScreen(xp:Int,streak:Int,mistakes:Int,onTopics:()->Unit,onExam:()->Unit,onErrors:()->Unit,onImages:()->Unit,onProfile:()->Unit){
 Scaffold(containerColor=AppBg,bottomBar={BottomNav("home",{},onTopics,onExam,onErrors,onProfile)}){inner->
  LazyColumn(
   Modifier.fillMaxSize().padding(inner).padding(horizontal=16.dp),
   contentPadding=PaddingValues(top=14.dp,bottom=16.dp),
   verticalArrangement=Arrangement.spacedBy(11.dp)
  ){
   item{
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
     Column(Modifier.weight(1f)){
      Text("ПДД РБ",color=TextPrimary,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
      Text("DENIS EDITION",color=Color(0xFF9B83FF),style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.ExtraBold)
     }
     Surface(onClick=onProfile,shape=CircleShape,color=AppCard,border=BorderStroke(1.dp,AppStroke)){Icon(Icons.Rounded.Settings,"Настройки",tint=TextMuted,modifier=Modifier.padding(10.dp).size(21.dp))}
    }
   }
   item{PremiumProfileCard(xp,streak,mistakes)}
   item{ContinueHero(onImages)}
   item{
    Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){
     MiniActionCard("Экзамен","10 вопросов",Icons.Rounded.School,Color(0xFF56A7FF),onExam,Modifier.weight(1f))
     MiniActionCard("По темам","17 разделов",Icons.Rounded.MenuBook,Color(0xFFFFA452),onTopics,Modifier.weight(1f))
    }
   }
   item{VisualModeCard(onImages)}
   item{
    Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){
     MiniActionCard("Ошибки","Повторить сложное",Icons.Rounded.Refresh,Bad,onErrors,Modifier.weight(1f))
     MiniActionCard("Статистика","Прогресс обучения",Icons.Rounded.BarChart,Color(0xFF33D2C1),onProfile,Modifier.weight(1f))
    }
   }
  }
 }
}

@Composable
private fun ErrorScreen(
 mistakes:Set<String>,onStart:()->Unit,onHome:()->Unit,onTopics:()->Unit,onExam:()->Unit,onProfile:()->Unit
){
 Scaffold(containerColor=AppBg,bottomBar={BottomNav("errors",onHome,onTopics,onExam,{},onProfile)}){inner->
  Column(Modifier.fillMaxSize().padding(inner).padding(horizontal=18.dp,vertical=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
   Text("Ошибки",color=TextPrimary,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold)
   Text("Повторяй только то, где Петрович дал слабину.",color=TextMuted)
   Surface(shape=RoundedCornerShape(20.dp),color=AppCard,border=BorderStroke(1.dp,AppStroke),modifier=Modifier.fillMaxWidth()){
    Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){
     Surface(shape=RoundedCornerShape(14.dp),color=Bad.copy(alpha=.14f)){Icon(Icons.Rounded.Error,null,tint=Bad,modifier=Modifier.padding(11.dp).size(28.dp))}
     Spacer(Modifier.width(14.dp))
     Column(Modifier.weight(1f)){
      Text(mistakes.size.toString(),color=if(mistakes.isEmpty())Good else Bad,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
      Text(if(mistakes.isEmpty())"Ошибок пока нет" else "вопросов нужно повторить",color=TextMuted)
     }
    }
   }
   if(mistakes.isNotEmpty())Button(onClick=onStart,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(16.dp)){Text("Разобрать ошибки")}
  }
 }
}

@Composable
private fun ProfileScreen(
 xp:Int,streak:Int,mistakes:Int,onHome:()->Unit,onTopics:()->Unit,onExam:()->Unit,onErrors:()->Unit
){
 val rank=rankFor(xp)
 Scaffold(containerColor=AppBg,bottomBar={BottomNav("profile",onHome,onTopics,onExam,onErrors,{})}){inner->
  LazyColumn(Modifier.fillMaxSize().padding(inner).padding(horizontal=18.dp),contentPadding=PaddingValues(top=18.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
   item{Text("Профиль",color=TextPrimary,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.headlineMedium)}
   item{
    Card(colors=CardDefaults.cardColors(containerColor=AppCard),shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,AppStroke)){
     Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
      Row(verticalAlignment=Alignment.CenterVertically){
       Surface(shape=CircleShape,color=Accent.copy(alpha=.2f)){Icon(Icons.Rounded.DirectionsCar,null,tint=Color(0xFFC9B8FF),modifier=Modifier.padding(14.dp).size(32.dp))}
       Spacer(Modifier.width(14.dp))
       Column(Modifier.weight(1f)){Text(rank.title,color=TextPrimary,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);Text(xp.toString()+" XP",color=TextMuted)}
      }
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
       StatTile(Icons.Rounded.LocalFireDepartment,streak.toString(),"серия",Orange,Modifier.weight(1f))
       StatTile(Icons.Rounded.Error,mistakes.toString(),"ошибки",Bad,Modifier.weight(1f))
      }
     }
    }
   }
   item{Text("PDD-RB 2.0 • Denis Edition",color=TextMuted,style=MaterialTheme.typography.labelLarge)}
  }
 }
}

private fun topicIcon(title:String):androidx.compose.ui.graphics.vector.ImageVector=when(title){
 "Общие положения"->Icons.Rounded.Info
 "Дорожные знаки"->Icons.Rounded.Warning
 "Дорожная разметка"->Icons.Rounded.Straighten
 "Светофор и регулировщик"->Icons.Rounded.Traffic
 "Маневрирование"->Icons.Rounded.SwapHoriz
 "Расположение на дороге"->Icons.Rounded.ViewWeek
 "Скорость движения"->Icons.Rounded.Speed
 "Обгон и встречный разъезд"->Icons.Rounded.CompareArrows
 "Остановка и стоянка"->Icons.Rounded.LocalParking
 "Проезд перекрёстков"->Icons.Rounded.AltRoute
 "Пешеходы и переходы"->Icons.Rounded.DirectionsWalk
 "Железнодорожные переезды"->Icons.Rounded.Train
 "Автомагистрали"->Icons.Rounded.DirectionsCar
 "Световые приборы"->Icons.Rounded.LightMode
 "Перевозка людей и грузов"->Icons.Rounded.Luggage
 "Техническое состояние"->Icons.Rounded.Build
 else->Icons.Rounded.HealthAndSafety
}

private fun topicTint(index:Int):Color=listOf(
 Color(0xFFFF6677),Color(0xFFFFB44A),Color(0xFF7C89FF),Color(0xFF4DD7B3),
 Color(0xFF5CA8FF),Color(0xFFA882FF),Color(0xFFFF8C5A),Color(0xFF35C8E7)
)[index%8]

private fun topicPreviewRes(title:String):Int=when(title){
 "Дорожные знаки","Светофор и регулировщик","Проезд перекрёстков"->R.drawable.scene_intersection
 "Дорожная разметка","Маневрирование","Расположение на дороге","Обгон и встречный разъезд"->R.drawable.scene_lane
 "Пешеходы и переходы","Остановка и стоянка"->R.drawable.scene_crosswalk
 else->0
}

@Composable
private fun FilterPill(text:String,selected:Boolean,onClick:()->Unit){
 Surface(
  onClick=onClick,
  shape=RoundedCornerShape(12.dp),
  color=if(selected)Accent else AppCard,
  border=BorderStroke(1.dp,if(selected)Accent else AppStroke)
 ){
  Text(text,color=if(selected)Color.White else TextMuted,fontWeight=if(selected)FontWeight.Bold else FontWeight.SemiBold,modifier=Modifier.padding(horizontal=14.dp,vertical=9.dp),style=MaterialTheme.typography.labelLarge)
 }
}

@Composable
fun TopicScreen(
 onHome:()->Unit,onTopic:(List<Question>)->Unit,onExam:()->Unit,onErrors:()->Unit,onProfile:()->Unit
){
 var filter by remember{mutableStateOf("all")}
 val visibleTopics=topics.filter{t->
  val list=qs.filter{it.topic==t.title}
  when(filter){"images"->list.any{it.visual>0};"theory"->list.any{it.visual==0};else->list.isNotEmpty()}
 }
 Scaffold(containerColor=AppBg,bottomBar={BottomNav("topics",onHome,{},onExam,onErrors,onProfile)}){inner->
  LazyColumn(
   Modifier.fillMaxSize().padding(inner).padding(horizontal=16.dp),
   contentPadding=PaddingValues(top=14.dp,bottom=16.dp),
   verticalArrangement=Arrangement.spacedBy(8.dp)
  ){
   item{
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
     Column(Modifier.weight(1f)){
      Text("Категории",color=TextPrimary,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black)
      Text("Подготовка по разделам ПДД",color=TextMuted,style=MaterialTheme.typography.bodySmall)
     }
     Surface(shape=CircleShape,color=AppCard,border=BorderStroke(1.dp,AppStroke)){Icon(Icons.Rounded.Search,null,tint=TextMuted,modifier=Modifier.padding(10.dp).size(20.dp))}
    }
   }
   item{
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
     FilterPill("Все",filter=="all"){filter="all"}
     FilterPill("С картинками",filter=="images"){filter="images"}
     FilterPill("Теория",filter=="theory"){filter="theory"}
    }
   }
   items(items=visibleTopics){t:Topic->
    val all=qs.filter{it.topic==t.title}
    val filtered=when(filter){"images"->all.filter{it.visual>0};"theory"->all.filter{it.visual==0};else->all}
    val visualCount=all.count{it.visual>0}
    val idx=topics.indexOf(t)
    val tint=topicTint(idx)
    val preview=topicPreviewRes(t.title)
    Card(
     onClick={onTopic(filtered)},
     modifier=Modifier.fillMaxWidth(),
     shape=RoundedCornerShape(18.dp),
     colors=CardDefaults.cardColors(containerColor=AppCard),
     border=BorderStroke(1.dp,AppStroke)
    ){
     Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
      if(preview!=0){
       Box{
        Image(painter=painterResource(preview),contentDescription=null,modifier=Modifier.size(width=88.dp,height=66.dp).clip(RoundedCornerShape(13.dp)),contentScale=ContentScale.Crop)
        Surface(shape=RoundedCornerShape(8.dp),color=Color(0xCC06101B),modifier=Modifier.align(Alignment.BottomStart).padding(5.dp)){
         Icon(topicIcon(t.title),null,tint=tint,modifier=Modifier.padding(5.dp).size(14.dp))
        }
       }
      }else{
       Box(Modifier.size(width=88.dp,height=66.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(tint.copy(alpha=.22f),Color(0xFF101A2A)))),contentAlignment=Alignment.Center){
        Icon(topicIcon(t.title),null,tint=tint,modifier=Modifier.size(28.dp))
       }
      }
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)){
       Text(t.title,color=TextPrimary,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.titleSmall,maxLines=2)
       Spacer(Modifier.height(3.dp))
       Text(t.subtitle,color=TextMuted,style=MaterialTheme.typography.bodySmall,maxLines=1)
       Spacer(Modifier.height(6.dp))
       Row(verticalAlignment=Alignment.CenterVertically){
        Surface(shape=RoundedCornerShape(8.dp),color=tint.copy(alpha=.12f)){
         Text(filtered.size.toString()+" вопросов",color=tint,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(horizontal=7.dp,vertical=4.dp))
        }
        if(visualCount>0&&filter=="all"){
         Spacer(Modifier.width(6.dp))
         Text(visualCount.toString()+" с фото",color=TextMuted,style=MaterialTheme.typography.labelSmall)
        }
       }
      }
      Icon(Icons.Rounded.ChevronRight,null,tint=TextMuted,modifier=Modifier.size(22.dp))
     }
    }
   }
  }
 }
}

@Composable
fun QuestionView(q:Question,index:Int,total:Int,correctCount:Int,xp:Int,streak:Int,onBack:()->Unit,onAnswered:(Boolean)->Unit,onNext:()->Unit){
 var answer by remember(q){mutableStateOf<Int?>(null)}
 var quip by remember(q){mutableStateOf<String?>(null)}
 var showExplain by remember(q){mutableStateOf(false)}
 LaunchedEffect(quip){if(quip!=null){delay(4000);quip=null}}
 Box(Modifier.fillMaxSize().background(AppBg)){
  LazyColumn(
   Modifier.fillMaxSize().padding(horizontal=16.dp),
   contentPadding=PaddingValues(top=14.dp,bottom=24.dp),
   verticalArrangement=Arrangement.spacedBy(10.dp)
  ){
   item{
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
     Surface(onClick=onBack,shape=CircleShape,color=AppCard,border=BorderStroke(1.dp,AppStroke)){Icon(Icons.Rounded.ArrowBack,null,tint=TextPrimary,modifier=Modifier.padding(9.dp).size(20.dp))}
     Spacer(Modifier.weight(1f))
     Column(horizontalAlignment=Alignment.End){
      Text("Вопрос "+(index+1)+" из "+total,color=TextPrimary,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge)
      Text(correctCount.toString()+" правильных",color=Good,style=MaterialTheme.typography.labelMedium)
     }
    }
   }
   item{LinearProgressIndicator(progress={(index+1).toFloat()/total.coerceAtLeast(1)},modifier=Modifier.fillMaxWidth().height(5.dp),color=Accent,trackColor=AppCard2)}
   item{
    Surface(shape=RoundedCornerShape(10.dp),color=Accent.copy(alpha=.13f),border=BorderStroke(1.dp,Accent.copy(alpha=.25f))){
     Text(q.topic,color=Color(0xFFC8BAFF),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge,modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp))
    }
   }
   if(q.visual>=100) item{PhotoSituation(q.visual)}
   item{Text(q.text,color=TextPrimary,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)}
   items(q.answers.size){i->
    val a=q.answers[i]
    val selected=answer==i
    val correct=answer!=null&&i==q.correct
    val wrong=answer!=null&&selected&&i!=q.correct
    val container=when{correct->GoodBg;wrong->BadBg;else->AppCard}
    val border=when{correct->Good;wrong->Bad;else->AppStroke}
    Surface(
     onClick={if(answer==null){answer=i;val ok=i==q.correct;quip=reaction(ok,if(ok)streak+1 else 0);onAnswered(ok);showExplain=!ok}},
     modifier=Modifier.fillMaxWidth(),
     shape=RoundedCornerShape(16.dp),
     color=container,
     border=BorderStroke(if(correct||wrong)2.dp else 1.dp,border)
    ){
     Row(Modifier.padding(horizontal=14.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){
      Surface(shape=CircleShape,color=when{correct->Good.copy(alpha=.18f);wrong->Bad.copy(alpha=.18f);else->Color(0xFF1A2A3E)}){
       Box(Modifier.size(30.dp),contentAlignment=Alignment.Center){
        Text(
         when{correct->"✓";wrong->"✕";else->(i+1).toString()},
         color=when{correct->Good;wrong->Bad;else->TextMuted},
         fontWeight=FontWeight.Black
        )
       }
      }
      Spacer(Modifier.width(11.dp))
      Text(a,color=TextPrimary,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
     }
    }
   }
   answer?.let{ans->
    if(ans==q.correct){
     item{
      Button(
       onClick=onNext,
       modifier=Modifier.fillMaxWidth().height(54.dp),
       shape=RoundedCornerShape(16.dp),
       colors=ButtonDefaults.buttonColors(containerColor=Accent)
      ){Text(if(index+1<total)"Дальше →" else "Завершить",fontWeight=FontWeight.Bold)}
     }
    }
   }
  }
  AnimatedVisibility(visible=quip!=null,enter=fadeIn(),exit=fadeOut(),modifier=Modifier.align(Alignment.TopCenter).padding(top=72.dp,start=18.dp,end=18.dp)){
   Surface(shadowElevation=14.dp,shape=RoundedCornerShape(18.dp),color=Color(0xF21A2635),border=BorderStroke(1.dp,AppStroke)){
    Row(Modifier.padding(horizontal=15.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
     Icon(Icons.Rounded.LocalFireDepartment,null,tint=Orange,modifier=Modifier.size(22.dp));Spacer(Modifier.width(9.dp))
     Text(quip?:"",color=TextPrimary,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.bodyMedium)
    }
   }
  }
 }
 if(showExplain)Dialog(onDismissRequest={}){
  Surface(shape=RoundedCornerShape(24.dp),color=Color(0xFF101C2A),border=BorderStroke(1.dp,Color(0xFF3B2732)),shadowElevation=20.dp){
   Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){
     Surface(shape=CircleShape,color=Bad.copy(alpha=.15f)){Icon(Icons.Rounded.Close,null,tint=Bad,modifier=Modifier.padding(8.dp).size(20.dp))}
     Spacer(Modifier.width(10.dp))
     Column{Text("Неверно",color=Bad,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge);Text("Разберём ситуацию",color=TextMuted,style=MaterialTheme.typography.bodySmall)}
    }
    Text(q.explanation,color=TextPrimary,style=MaterialTheme.typography.bodyLarge)
    Surface(shape=RoundedCornerShape(14.dp),color=Good.copy(alpha=.11f),border=BorderStroke(1.dp,Good.copy(alpha=.30f))){
     Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
      Icon(Icons.Rounded.CheckCircle,null,tint=Good);Spacer(Modifier.width(9.dp))
      Column{Text("Правильный ответ",color=Good,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge);Text(q.answers[q.correct],color=TextPrimary,fontWeight=FontWeight.Bold)}
     }
    }
    Button(onClick={showExplain=false;onNext()},modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=Accent)){Text(if(index+1<total)"Разобрался, дальше →" else "Завершить",fontWeight=FontWeight.Bold)}
   }
  }
 }
}

@Composable fun PhotoSituation(type:Int){
 Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp)){
  Image(painter=painterResource(id=when(type){101->R.drawable.scene_intersection;102->R.drawable.scene_lane;else->R.drawable.scene_crosswalk}),contentDescription="Дорожная ситуация к вопросу",modifier=Modifier.fillMaxWidth().height(245.dp),contentScale=ContentScale.Crop)
 }
}

@Composable fun RoadSituation(type:Int){
 Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFD9D3C5))){
  Canvas(Modifier.fillMaxWidth().height(205.dp)){
   val w=size.width;val h=size.height;val asphalt=Color(0xFF4B4F54);val verge=Color(0xFFD8D2C2);val white=Color(0xFFF7F4E9);val yellow=Color(0xFFF2C94C)
   drawRect(verge)
   fun car(cx:Float,cy:Float,vertical:Boolean,color:Color){
    val cw=if(vertical)w*.11f else w*.21f;val ch=if(vertical)h*.31f else h*.16f
    drawRoundRect(Color(0x44000000),Offset(cx-cw/2+5,cy-ch/2+6),Size(cw,ch),CornerRadius(14f))
    drawRoundRect(color,Offset(cx-cw/2,cy-ch/2),Size(cw,ch),CornerRadius(14f))
    if(vertical){drawRoundRect(Color(0xFFCDE4EE),Offset(cx-cw*.34f,cy-ch*.28f),Size(cw*.68f,ch*.18f),CornerRadius(5f));drawRoundRect(Color(0xFF9EB7C3),Offset(cx-cw*.34f,cy+ch*.08f),Size(cw*.68f,ch*.16f),CornerRadius(5f));drawCircle(Color(0xFFFFF2A8),4f,Offset(cx-cw*.29f,cy-ch*.45f));drawCircle(Color(0xFFFFF2A8),4f,Offset(cx+cw*.29f,cy-ch*.45f))}
    else{drawRoundRect(Color(0xFFCDE4EE),Offset(cx-cw*.28f,cy-ch*.34f),Size(cw*.18f,ch*.68f),CornerRadius(5f));drawRoundRect(Color(0xFF9EB7C3),Offset(cx+cw*.08f,cy-ch*.34f),Size(cw*.16f,ch*.68f),CornerRadius(5f))}
   }
   fun dash(a:Offset,b:Offset){for(i in 0..9 step 2){val t=i/10f;val u=(i+1)/10f;drawLine(white,Offset(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t),Offset(a.x+(b.x-a.x)*u,a.y+(b.y-a.y)*u),4f)}}
   fun zebra(y:Float){for(i in 0..7){val x=w*(.23f+i*.075f);drawRect(white,Offset(x,y),Size(w*.045f,h*.11f))}}
   when(type){
    1,3->{drawRect(asphalt,Offset(w*.32f,0f),Size(w*.36f,h));drawRect(asphalt,Offset(0f,h*.32f),Size(w,h*.36f));dash(Offset(w*.5f,0f),Offset(w*.5f,h*.29f));dash(Offset(w*.5f,h*.71f),Offset(w*.5f,h));dash(Offset(0f,h*.5f),Offset(w*.29f,h*.5f));dash(Offset(w*.71f,h*.5f),Offset(w,h*.5f));car(w*.43f,h*.82f,true,Color(0xFFE44B46));if(type==1)car(w*.82f,h*.43f,false,Color(0xFF397FD5))else{drawCircle(Color.White,25f,Offset(w*.77f,h*.78f));val p=Path();p.moveTo(w*.77f-19,h*.78f-14);p.lineTo(w*.77f+19,h*.78f-14);p.lineTo(w*.77f,h*.78f+20);p.close();drawPath(p,yellow);drawPath(p,Color(0xFF333333),style=androidx.compose.ui.graphics.drawscope.Stroke(3f))}}
    2->{drawRect(asphalt,Offset(0f,h*.18f),Size(w,h*.64f));dash(Offset(0f,h*.5f),Offset(w,h*.5f));car(w*.25f,h*.64f,false,Color(0xFFE44B46));drawRoundRect(Color(0xFF202226),Offset(w*.77f,h*.02f),Size(w*.095f,h*.40f),CornerRadius(10f));drawCircle(Color(0xFFE53935),14f,Offset(w*.817f,h*.09f));drawCircle(Color(0xFF55585C),14f,Offset(w*.817f,h*.22f));drawCircle(Color(0xFF55585C),14f,Offset(w*.817f,h*.35f))}
    4->{drawRect(asphalt,Offset(0f,h*.12f),Size(w,h*.76f));dash(Offset(0f,h*.5f),Offset(w,h*.5f));car(w*.40f,h*.66f,false,Color(0xFFE44B46));car(w*.67f,h*.35f,false,Color(0xFF397FD5));val p=Path();p.moveTo(w*.46f,h*.61f);p.cubicTo(w*.52f,h*.60f,w*.55f,h*.43f,w*.61f,h*.39f);drawPath(p,yellow,style=androidx.compose.ui.graphics.drawscope.Stroke(7f))}
    5->{drawRect(asphalt,Offset(0f,h*.13f),Size(w,h*.74f));dash(Offset(0f,h*.5f),Offset(w,h*.5f));zebra(h*.40f);car(w*.25f,h*.68f,false,Color(0xFFE44B46));drawCircle(Color(0xFF222222),9f,Offset(w*.59f,h*.31f));drawLine(Color(0xFF222222),Offset(w*.59f,h*.35f),Offset(w*.59f,h*.48f),7f);drawLine(Color(0xFF222222),Offset(w*.59f,h*.39f),Offset(w*.55f,h*.44f),6f);drawLine(Color(0xFF222222),Offset(w*.59f,h*.48f),Offset(w*.55f,h*.58f),6f);drawLine(Color(0xFF222222),Offset(w*.59f,h*.48f),Offset(w*.64f,h*.58f),6f)}
    6->{drawRect(asphalt,Offset(0f,h*.12f),Size(w,h*.76f));drawLine(white,Offset(0f,h*.5f),Offset(w,h*.5f),6f);car(w*.28f,h*.67f,false,Color(0xFFE44B46));car(w*.58f,h*.67f,false,Color(0xFF8C939B));drawLine(yellow,Offset(w*.32f,h*.59f),Offset(w*.53f,h*.38f),7f)}
    7->{drawRect(asphalt,Offset(0f,h*.13f),Size(w,h*.74f));dash(Offset(0f,h*.5f),Offset(w,h*.5f));zebra(h*.40f);car(w*.43f,h*.69f,false,Color(0xFFE44B46));drawCircle(Color(0xFF222222),8f,Offset(w*.68f,h*.32f));drawLine(Color(0xFF222222),Offset(w*.68f,h*.36f),Offset(w*.68f,h*.49f),7f)}
    else->{drawRect(asphalt,Offset(0f,h*.15f),Size(w,h*.70f));dash(Offset(0f,h*.5f),Offset(w,h*.5f));car(w*.30f,h*.65f,false,Color(0xFFE44B46));car(w*.67f,h*.35f,false,Color(0xFF397FD5))}
   }
  }
 }
}
