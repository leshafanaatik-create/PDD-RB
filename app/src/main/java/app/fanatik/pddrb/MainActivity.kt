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
  val visual=qs.filter{it.visual==101}.shuffled().toMutableList()
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
    onImages={start(qs.filter{it.visual==101}.shuffled())},onProfile={screen="profile"},
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

@Composable
private fun NavItem(
 selected:Boolean,label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit,modifier:Modifier=Modifier
){
 Surface(onClick=onClick,color=Color.Transparent,modifier=modifier){
  Column(
   Modifier.fillMaxHeight().padding(top=7.dp),
   horizontalAlignment=Alignment.CenterHorizontally,
   verticalArrangement=Arrangement.spacedBy(2.dp)
  ){
   Box(Modifier.height(3.dp).width(28.dp).clip(RoundedCornerShape(2.dp)).background(if(selected)Accent else Color.Transparent))
   Icon(icon,null,tint=if(selected)Color(0xFF9D86FF) else Color(0xFF65758A),modifier=Modifier.size(21.dp))
   Text(label,color=if(selected)Color(0xFFE5DFFF) else Color(0xFF65758A),fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium,style=MaterialTheme.typography.labelSmall,maxLines=1)
  }
 }
}

@Composable
private fun BottomNav(selected:String,onHome:()->Unit,onTopics:()->Unit,onExam:()->Unit,onErrors:()->Unit,onProfile:()->Unit){
 Surface(color=Color(0xFA07111C),shadowElevation=18.dp,border=BorderStroke(1.dp,Color(0xFF132235))){
  Row(
   Modifier.fillMaxWidth().navigationBarsPadding().height(58.dp),
   verticalAlignment=Alignment.CenterVertically
  ){
   NavItem(selected=="home","Главная",Icons.Rounded.Home,onHome,Modifier.weight(1f))
   NavItem(selected=="topics","Категории",Icons.Rounded.List,onTopics,Modifier.weight(1f))
   NavItem(selected=="exam","Экзамен",Icons.Rounded.School,onExam,Modifier.weight(1f))
   NavItem(selected=="errors","Ошибки",Icons.Rounded.Error,onErrors,Modifier.weight(1f))
   NavItem(selected=="profile","Профиль",Icons.Rounded.Person,onProfile,Modifier.weight(1f))
  }
 }
}

@Composable
private fun HomeStat(
 icon:androidx.compose.ui.graphics.vector.ImageVector,value:String,label:String,tint:Color,modifier:Modifier=Modifier
){
 Column(modifier=modifier,horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)){
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
   Icon(icon,null,tint=tint,modifier=Modifier.size(16.dp))
   Text(value,color=TextPrimary,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.bodyMedium)
  }
  Text(label,color=TextMuted,style=MaterialTheme.typography.labelSmall,maxLines=1)
 }
}

@Composable
private fun ProfileHero(xp:Int){
 val rank=rankFor(xp); val next=nextRank(xp)
 val maxXp=next?.minXp ?: xp.coerceAtLeast(1)
 val progress=if(next==null)1f else ((xp-rank.minXp).toFloat()/(maxXp-rank.minXp)).coerceIn(0f,1f)
 Surface(
  shape=RoundedCornerShape(22.dp),
  color=Color(0xFF0D1727),
  border=BorderStroke(1.dp,Color(0xFF1A2940))
 ){
  Row(Modifier.height(122.dp).padding(12.dp),verticalAlignment=Alignment.CenterVertically){
   Box(
    Modifier.width(112.dp).fillMaxHeight().clip(RoundedCornerShape(18.dp))
      .background(Brush.verticalGradient(listOf(Color(0xFF2D214C),Color(0xFF171529)))),
    contentAlignment=Alignment.BottomCenter
   ){
    Image(
     painter=painterResource(R.drawable.mascot_den),
     contentDescription="Динька",
     modifier=Modifier.fillMaxSize(),
     contentScale=ContentScale.Crop
    )
   }
   Spacer(Modifier.width(12.dp))
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){
    Text(rank.title,color=TextPrimary,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium,maxLines=2)
    Text("Уровень "+(ranks.indexOf(rank)+1),color=Color(0xFFB6C0D0),style=MaterialTheme.typography.bodySmall)
    LinearProgressIndicator(
     progress={progress},
     modifier=Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
     color=Color(0xFF8B5CFF),trackColor=Color(0xFF24314A)
    )
    Text(xp.toString()+" / "+maxXp+" XP",color=TextMuted,style=MaterialTheme.typography.labelSmall)
   }
  }
 }
}

@Composable
private fun PrimaryAction(title:String,subtitle:String,onClick:()->Unit){
 Surface(onClick=onClick,shape=RoundedCornerShape(18.dp),color=Color.Transparent){
  Box(
   Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(18.dp))
    .background(Brush.horizontalGradient(listOf(Color(0xFF2879FF),Color(0xFF6843FF),Color(0xFF9D38FF))))
  ){
   Row(Modifier.fillMaxSize().padding(horizontal=15.dp),verticalAlignment=Alignment.CenterVertically){
    Surface(shape=RoundedCornerShape(13.dp),color=Color.White.copy(alpha=.14f)){
     Icon(Icons.Rounded.MenuBook,null,tint=Color.White,modifier=Modifier.padding(11.dp).size(25.dp))
    }
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)){
     Text(title,color=Color.White,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium)
     Text(subtitle,color=Color(0xFFD9D2FF),style=MaterialTheme.typography.bodySmall)
    }
    Icon(Icons.Rounded.ChevronRight,null,tint=Color.White,modifier=Modifier.size(26.dp))
   }
  }
 }
}

@Composable
private fun FullWidthAction(
 title:String,subtitle:String,icon:androidx.compose.ui.graphics.vector.ImageVector,tint:Color,onClick:()->Unit
){
 Surface(onClick=onClick,shape=RoundedCornerShape(17.dp),color=Color(0xFF101A2C),border=BorderStroke(1.dp,Color(0xFF1D2A42))){
  Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically){
   Surface(shape=RoundedCornerShape(13.dp),color=tint.copy(alpha=.18f)){
    Icon(icon,null,tint=tint,modifier=Modifier.padding(10.dp).size(24.dp))
   }
   Spacer(Modifier.width(11.dp))
   Column(Modifier.weight(1f)){
    Text(title,color=TextPrimary,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.bodyLarge)
    Text(subtitle,color=TextMuted,style=MaterialTheme.typography.bodySmall)
   }
   Icon(Icons.Rounded.ChevronRight,null,tint=Color(0xFF7D8DA3))
  }
 }
}

@Composable
private fun SmallHomeTile(
 title:String,subtitle:String,icon:androidx.compose.ui.graphics.vector.ImageVector,tint:Color,onClick:()->Unit,modifier:Modifier=Modifier,badge:String?=null
){
 Surface(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(17.dp),color=Color(0xFF101A2C),border=BorderStroke(1.dp,Color(0xFF1D2A42))){
  Box(Modifier.fillMaxWidth().height(86.dp).padding(12.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){
    Surface(shape=RoundedCornerShape(12.dp),color=tint.copy(alpha=.18f)){
     Icon(icon,null,tint=tint,modifier=Modifier.padding(9.dp).size(21.dp))
    }
    Spacer(Modifier.width(9.dp))
    Column(Modifier.weight(1f)){
     Text(title,color=TextPrimary,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.bodyMedium,maxLines=1)
     Text(subtitle,color=TextMuted,style=MaterialTheme.typography.labelSmall,maxLines=2)
    }
   }
   if(badge!=null){
    Surface(shape=RoundedCornerShape(8.dp),color=Bad,modifier=Modifier.align(Alignment.TopEnd)){
     Text(badge,color=Color.White,fontWeight=FontWeight.Black,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(horizontal=6.dp,vertical=3.dp))
    }
   }
  }
 }
}

@Composable
private fun RecentTopicCard(title:String,progress:Int,onClick:()->Unit,modifier:Modifier=Modifier){
 Surface(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(16.dp),color=Color(0xFF101A2C),border=BorderStroke(1.dp,Color(0xFF1D2A42))){
  Row(Modifier.padding(9.dp),verticalAlignment=Alignment.CenterVertically){
   Image(
    painter=painterResource(R.drawable.scene_premium_intersection),
    contentDescription=null,
    modifier=Modifier.size(width=68.dp,height=52.dp).clip(RoundedCornerShape(10.dp)),
    contentScale=ContentScale.Crop
   )
   Spacer(Modifier.width(9.dp))
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
    Text(title,color=TextPrimary,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge,maxLines=1)
    Text(progress.toString()+"%",color=TextMuted,style=MaterialTheme.typography.labelSmall)
    LinearProgressIndicator(progress={progress/100f},modifier=Modifier.fillMaxWidth().height(4.dp),color=Good,trackColor=Color(0xFF24314A))
   }
  }
 }
}

@Composable
fun HomeScreen(
 xp:Int,streak:Int,solved:Int,accuracy:Int,
 onTopics:()->Unit,onExam:()->Unit,onErrors:()->Unit,onImages:()->Unit,onProfile:()->Unit,
 onSigns:()->Unit,onMarking:()->Unit,onTraffic:()->Unit
){
 Scaffold(containerColor=AppBg,bottomBar={BottomNav("home",{},onTopics,onExam,onErrors,onProfile)}){inner->
  LazyColumn(
   Modifier.fillMaxSize().padding(inner).padding(horizontal=14.dp),
   contentPadding=PaddingValues(top=12.dp,bottom=16.dp),
   verticalArrangement=Arrangement.spacedBy(10.dp)
  ){
   item{
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
     Column(Modifier.weight(1f)){
      Text("Привет, Денчик! 👋",color=TextPrimary,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall)
      Text("Готов к новым вопросам?",color=TextMuted,style=MaterialTheme.typography.bodySmall)
     }
     Surface(onClick=onProfile,shape=RoundedCornerShape(12.dp),color=Color(0xFF151A31)){
      Icon(Icons.Rounded.Settings,null,tint=Color(0xFFC4CCE0),modifier=Modifier.padding(10.dp).size(21.dp))
     }
    }
   }
   item{ProfileHero(xp)}
   item{
    Surface(shape=RoundedCornerShape(16.dp),color=Color(0xFF0B1523),border=BorderStroke(1.dp,Color(0xFF17263A))){
     Row(Modifier.fillMaxWidth().padding(vertical=10.dp,horizontal=6.dp)){
      HomeStat(Icons.Rounded.LocalFireDepartment,streak.toString(),"Серия",Orange,Modifier.weight(1f))
      HomeStat(Icons.Rounded.BarChart,solved.toString(),"Решено",Good,Modifier.weight(1f))
      HomeStat(Icons.Rounded.TrackChanges,accuracy.toString()+"%","Точность",Bad,Modifier.weight(1f))
     }
    }
   }
   item{PrimaryAction("Продолжить обучение","Случайные вопросы • теория + ситуации",onImages)}
   item{FullWidthAction("Экзамен","Как в ГАИ • 10 вопросов",Icons.Rounded.School,Color(0xFF6F8BFF),onExam)}
   item{
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
     SmallHomeTile("Темы","Выбор раздела",Icons.Rounded.GridView,Color(0xFF5B8CFF),onTopics,Modifier.weight(1f))
     SmallHomeTile("Картинки","Задачи с изображениями",Icons.Rounded.Image,Good,onImages,Modifier.weight(1f),"NEW")
    }
   }
   item{
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
     SmallHomeTile("Мои ошибки","Повторить слабое",Icons.Rounded.Close,Bad,onErrors,Modifier.weight(1f))
     SmallHomeTile("Статистика","Прогресс и достижения",Icons.Rounded.BarChart,Good,onProfile,Modifier.weight(1f))
    }
   }
   item{
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
     Text("Последние темы",color=TextPrimary,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.titleSmall,modifier=Modifier.weight(1f))
     TextButton(onClick=onTopics,contentPadding=PaddingValues(4.dp)){Text("Все  ›",color=Color(0xFF9B7DFF),style=MaterialTheme.typography.labelMedium)}
    }
   }
   item{
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
     RecentTopicCard("Перекрёстки",62,onTraffic,Modifier.weight(1f))
     RecentTopicCard("Маневрирование",38,onMarking,Modifier.weight(1f))
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
       Surface(shape=RoundedCornerShape(14.dp),color=Color(0xFF211B3C)){Image(painter=painterResource(R.drawable.ic_den_cat),contentDescription=null,modifier=Modifier.size(56.dp).padding(3.dp))}
       Spacer(Modifier.width(14.dp))
       Column(Modifier.weight(1f)){Text(rank.title,color=TextPrimary,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);Text(xp.toString()+" XP",color=TextMuted)}
      }
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
       HomeStat(Icons.Rounded.LocalFireDepartment,streak.toString(),"серия",Orange,Modifier.weight(1f))
       HomeStat(Icons.Rounded.Error,mistakes.toString(),"ошибки",Bad,Modifier.weight(1f))
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

private fun previewAlignment(index:Int):Alignment=when(index%3){
 0->Alignment.CenterStart
 1->Alignment.Center
 else->Alignment.CenterEnd
}

@Composable
private fun FilterPill(text:String,selected:Boolean,onClick:()->Unit){
 Surface(
  onClick=onClick,shape=RoundedCornerShape(11.dp),
  color=if(selected)Accent else Color(0xFF0D1927),
  border=BorderStroke(1.dp,if(selected)Accent else Color(0xFF1B2B3F))
 ){
  Text(
   text,color=if(selected)Color.White else TextMuted,
   fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium,
   modifier=Modifier.padding(horizontal=13.dp,vertical=7.dp),
   style=MaterialTheme.typography.labelMedium
  )
 }
}

@Composable
fun TopicScreen(
 solved:Set<String>,onHome:()->Unit,onTopic:(List<Question>)->Unit,onExam:()->Unit,onErrors:()->Unit,onProfile:()->Unit
){
 var filter by remember{mutableStateOf("all")}
 val visibleTopics=topics.filter{t->
  val list=qs.filter{it.topic==t.title}
  when(filter){"images"->list.any{it.visual==101};"theory"->list.any{it.visual==0};else->list.any{it.visual==0||it.visual==101}}
 }
 Scaffold(containerColor=AppBg,bottomBar={BottomNav("topics",onHome,{},onExam,onErrors,onProfile)}){inner->
  LazyColumn(
   Modifier.fillMaxSize().padding(inner).padding(horizontal=14.dp),
   contentPadding=PaddingValues(top=11.dp,bottom=12.dp),
   verticalArrangement=Arrangement.spacedBy(7.dp)
  ){
   item{
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
     Text("Категории",color=TextPrimary,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black,modifier=Modifier.weight(1f))
     IconButton(onClick={},modifier=Modifier.size(36.dp)){Icon(Icons.Rounded.Search,null,tint=TextMuted,modifier=Modifier.size(21.dp))}
    }
   }
   item{
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
     FilterPill("Все",filter=="all"){filter="all"}
     FilterPill("С картинками",filter=="images"){filter="images"}
     FilterPill("Теория",filter=="theory"){filter="theory"}
    }
   }
   items(items=visibleTopics){t:Topic->
    val all=qs.filter{it.topic==t.title && (it.visual==0 || it.visual==101)}
    val filtered=when(filter){
     "images"->all.filter{it.visual==101}
     "theory"->all.filter{it.visual==0}
     else->all
    }
    val solvedCount=filtered.count{solved.contains(it.text)}
    val pct=if(filtered.isEmpty())0 else solvedCount*100/filtered.size
    val idx=topics.indexOf(t)
    val tint=topicTint(idx)
    Surface(
     onClick={onTopic(filtered)},
     modifier=Modifier.fillMaxWidth(),
     shape=RoundedCornerShape(14.dp),
     color=Color(0xFF0D1927),
     border=BorderStroke(1.dp,Color(0xFF182A3E))
    ){
     Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically){
      Box(Modifier.size(width=76.dp,height=54.dp).clip(RoundedCornerShape(11.dp))){
       Image(
        painter=painterResource(R.drawable.scene_premium_intersection),
        contentDescription=null,
        modifier=Modifier.matchParentSize(),
        contentScale=ContentScale.Crop,
        alignment=previewAlignment(idx)
       )
       Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xAA050B13)))))
       Surface(shape=RoundedCornerShape(7.dp),color=Color(0xCC0A1420),modifier=Modifier.align(Alignment.BottomStart).padding(4.dp)){
        Icon(topicIcon(t.title),null,tint=tint,modifier=Modifier.padding(4.dp).size(13.dp))
       }
      }
      Spacer(Modifier.width(10.dp))
      Column(Modifier.weight(1f)){
       Text(t.title,color=TextPrimary,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.bodyMedium,maxLines=1)
       Text(filtered.size.toString()+" вопросов",color=TextMuted,style=MaterialTheme.typography.labelSmall)
       Spacer(Modifier.height(6.dp))
       LinearProgressIndicator(
        progress={pct/100f},
        modifier=Modifier.fillMaxWidth().height(4.dp),
        color=tint,trackColor=Color(0xFF263448)
       )
      }
      Spacer(Modifier.width(9.dp))
      Text(pct.toString()+"%",color=if(pct>0)tint else TextMuted,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelMedium)
      Icon(Icons.Rounded.ChevronRight,null,tint=Color(0xFF607085),modifier=Modifier.size(20.dp))
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
 LaunchedEffect(quip){if(quip!=null){delay(4000);quip=null}}
 val isWrong=answer!=null && answer!=q.correct

 Box(Modifier.fillMaxSize().background(AppBg)){
  LazyColumn(
   Modifier.fillMaxSize().padding(horizontal=14.dp),
   contentPadding=PaddingValues(top=10.dp,bottom=18.dp),
   verticalArrangement=Arrangement.spacedBy(9.dp)
  ){
   item{
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
     IconButton(onClick=onBack,modifier=Modifier.size(36.dp)){Icon(Icons.Rounded.ArrowBack,null,tint=TextPrimary,modifier=Modifier.size(21.dp))}
     Spacer(Modifier.weight(1f))
     Text("Вопрос "+(index+1)+" из "+total,color=Color(0xFFC4CEDA),fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.labelMedium)
     Spacer(Modifier.weight(1f))
     IconButton(onClick={},modifier=Modifier.size(36.dp)){Icon(Icons.Rounded.FavoriteBorder,null,tint=TextMuted,modifier=Modifier.size(20.dp))}
    }
   }
   item{
    LinearProgressIndicator(
     progress={(index+1).toFloat()/total.coerceAtLeast(1)},
     modifier=Modifier.fillMaxWidth().height(4.dp),
     color=Accent,trackColor=Color(0xFF1C2A3D)
    )
   }
   if(q.visual==101)item{PhotoSituation(q.visual)}
   item{Text(q.text,color=TextPrimary,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold)}
   items(q.answers.size){i->
    val selected=answer==i
    val correct=answer!=null&&i==q.correct
    val wrong=answer!=null&&selected&&i!=q.correct
    val bg=when{correct->Color(0xFF0F472F);wrong->Color(0xFF49202A);else->Color(0xFF0D1927)}
    val stroke=when{correct->Good;wrong->Bad;else->Color(0xFF1C3045)}
    Surface(
     onClick={
      if(answer==null){
       answer=i
       val ok=i==q.correct
       quip=reaction(ok,if(ok)streak+1 else 0)
       onAnswered(ok)
      }
     },
     modifier=Modifier.fillMaxWidth(),
     shape=RoundedCornerShape(13.dp),
     color=bg,border=BorderStroke(if(correct||wrong)1.5.dp else 1.dp,stroke)
    ){
     Row(Modifier.padding(horizontal=12.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically){
      Box(
       Modifier.size(24.dp).clip(CircleShape).background(
        when{correct->Good;wrong->Bad;else->Color(0xFF16263A)}
       ),
       contentAlignment=Alignment.Center
      ){
       Text(
        when{correct->"✓";wrong->"×";else->(i+1).toString()},
        color=if(correct||wrong)Color.White else TextMuted,
        fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelMedium
       )
      }
      Spacer(Modifier.width(10.dp))
      Text(q.answers[i],color=TextPrimary,fontWeight=FontWeight.Medium,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.weight(1f))
     }
    }
   }
   if(isWrong){
    item{
     Surface(shape=RoundedCornerShape(15.dp),color=Color(0xFF321923),border=BorderStroke(1.dp,Color(0xFF6F2B3D))){
      Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
       Row(verticalAlignment=Alignment.CenterVertically){
        Surface(shape=CircleShape,color=Bad.copy(alpha=.18f)){Icon(Icons.Rounded.Close,null,tint=Bad,modifier=Modifier.padding(5.dp).size(16.dp))}
        Spacer(Modifier.width(8.dp))
        Text("Неверно",color=Bad,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.titleSmall)
       }
       Text(q.explanation,color=Color(0xFFD5DCE6),style=MaterialTheme.typography.bodySmall)
       Surface(shape=RoundedCornerShape(10.dp),color=Color(0xFF0B432D)){
        Column(Modifier.padding(horizontal=11.dp,vertical=8.dp)){
         Text("Правильный ответ",color=Good,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelSmall)
         Text(q.answers[q.correct],color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.bodyMedium)
        }
       }
      }
     }
    }
   }
   if(answer!=null){
    item{
     Button(
      onClick=onNext,
      modifier=Modifier.fillMaxWidth().height(48.dp),
      shape=RoundedCornerShape(12.dp),
      colors=ButtonDefaults.buttonColors(containerColor=Accent)
     ){Text(if(index+1<total)"Дальше →" else "Завершить",fontWeight=FontWeight.ExtraBold)}
    }
   }
  }

  AnimatedVisibility(
   visible=quip!=null,enter=fadeIn(),exit=fadeOut(),
   modifier=Modifier.align(Alignment.TopCenter).padding(top=62.dp,start=18.dp,end=18.dp)
  ){
   Surface(shape=RoundedCornerShape(14.dp),color=Color(0xF2182535),border=BorderStroke(1.dp,Color(0xFF263A52)),shadowElevation=12.dp){
    Row(Modifier.padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
     Icon(Icons.Rounded.LocalFireDepartment,null,tint=Orange,modifier=Modifier.size(19.dp))
     Spacer(Modifier.width(7.dp))
     Text(quip?:"",color=TextPrimary,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.bodySmall)
    }
   }
  }
 }
}

@Composable
fun PhotoSituation(type:Int){
 Surface(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),color=Color(0xFF0D1927),border=BorderStroke(1.dp,Color(0xFF1A2C40))){
  Image(
   painter=painterResource(R.drawable.scene_premium_intersection),
   contentDescription="Дорожная ситуация к вопросу",
   modifier=Modifier.fillMaxWidth().height(218.dp),
   contentScale=ContentScale.Crop
  )
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
