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
import androidx.compose.ui.graphics.Color
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

private val AppBg=Color(0xFF07111D)
private val AppSurface=Color(0xFF0D1927)
private val AppCard=Color(0xFF122132)
private val AppCard2=Color(0xFF17283B)
private val AppStroke=Color(0xFF24364A)
private val Accent=Color(0xFF7C4DFF)
private val Accent2=Color(0xFF4F6BFF)
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
 fun alternatingExam(count:Int=10):List<Question>{
  val visual=qs.filter{it.visual>0}.shuffled().toMutableList()
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
 fun next(){if(index+1<pool.size){index++;current=pool[index]}else screen="home"}
 Surface(Modifier.fillMaxSize(),color=AppBg){
  when(screen){
   "home"->HomeScreen(xp,streak,mistakes.size,{screen="topics"},{start(alternatingExam())},{screen="errors"},{start(qs.filter{it.visual>=100}.shuffled())},{screen="profile"})
   "topics"->TopicScreen(
    onHome={screen="home"},
    onTopic={start(qs.filter{q->q.topic==it.title})},
    onExam={start(alternatingExam())},
    onErrors={screen="errors"},
    onProfile={screen="profile"}
   )
   "profile"->ProfileScreen(
    xp=xp,streak=streak,mistakes=mistakes.size,
    onHome={screen="home"},onTopics={screen="topics"},onExam={start(alternatingExam())},onErrors={screen="errors"}
   )
   "question"->QuestionView(current,index,pool.size,correctCount,xp,streak,{screen="home"},{ok->
    if(ok){correctCount++;streak++;xp+=10+streak.coerceAtMost(10);mistakes=mistakes-current.text}else{streak=0;mistakes=mistakes+current.text}
    prefs.edit().putInt("xp",xp).putInt("streak",streak).putStringSet("mistakes",mistakes).apply()
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
 NavigationBar(containerColor=Color(0xFF091522),tonalElevation=0.dp,modifier=Modifier.height(72.dp)){
  NavigationBarItem(selected=selected=="home",onClick=onHome,icon={Icon(Icons.Rounded.Home,null)},label={Text("Главная")},colors=NavColors())
  NavigationBarItem(selected=selected=="topics",onClick=onTopics,icon={Icon(Icons.Rounded.List,null)},label={Text("Категории")},colors=NavColors())
  NavigationBarItem(selected=selected=="exam",onClick=onExam,icon={Icon(Icons.Rounded.School,null)},label={Text("Экзамен")},colors=NavColors())
  NavigationBarItem(selected=selected=="errors",onClick=onErrors,icon={Icon(Icons.Rounded.Error,null)},label={Text("Ошибки")},colors=NavColors())
  NavigationBarItem(selected=selected=="profile",onClick=onProfile,icon={Icon(Icons.Rounded.Person,null)},label={Text("Профиль")},colors=NavColors())
 }
}

@Composable
private fun StatTile(icon:androidx.compose.ui.graphics.vector.ImageVector,value:String,label:String,tint:Color,modifier:Modifier=Modifier){
 Surface(modifier=modifier,shape=RoundedCornerShape(16.dp),color=AppCard2,border=BorderStroke(1.dp,AppStroke)){
  Row(Modifier.padding(horizontal=12.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
   Icon(icon,null,tint=tint,modifier=Modifier.size(22.dp));Spacer(Modifier.width(9.dp))
   Column{Text(value,color=TextPrimary,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.titleMedium);Text(label,color=TextMuted,style=MaterialTheme.typography.labelSmall)}
  }
 }
}

@Composable
private fun MiniActionCard(title:String,subtitle:String,icon:androidx.compose.ui.graphics.vector.ImageVector,tint:Color,onClick:()->Unit,modifier:Modifier=Modifier){
 Card(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=AppCard),border=BorderStroke(1.dp,AppStroke)){
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   Surface(shape=RoundedCornerShape(12.dp),color=tint.copy(alpha=.16f)){Icon(icon,null,tint=tint,modifier=Modifier.padding(9.dp).size(23.dp))}
   Text(title,color=TextPrimary,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
   Text(subtitle,color=TextMuted,style=MaterialTheme.typography.bodySmall)
  }
 }
}

@Composable
fun HomeScreen(xp:Int,streak:Int,mistakes:Int,onTopics:()->Unit,onExam:()->Unit,onErrors:()->Unit,onImages:()->Unit,onProfile:()->Unit){
 val rank=rankFor(xp);val next=nextRank(xp)
 Scaffold(containerColor=AppBg,bottomBar={BottomNav("home",{},onTopics,onExam,onErrors,onProfile)}){inner->
  LazyColumn(
   Modifier.fillMaxSize().padding(inner).padding(horizontal=18.dp),
   contentPadding=PaddingValues(top=20.dp,bottom=24.dp),
   verticalArrangement=Arrangement.spacedBy(14.dp)
  ){
   item{
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
     Column(Modifier.weight(1f)){
      Text("ПДД РБ",color=TextPrimary,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black)
      Text("DENIS EDITION",color=Accent,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.ExtraBold)
     }
     IconButton(onClick=onProfile,modifier=Modifier.size(44.dp)){Icon(Icons.Rounded.Settings,"Настройки",tint=TextMuted)}
    }
   }
   item{
    Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=AppCard),border=BorderStroke(1.dp,AppStroke)){
     Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(15.dp)){
      Row(verticalAlignment=Alignment.CenterVertically){
       Surface(shape=CircleShape,color=Accent.copy(alpha=.20f),border=BorderStroke(1.dp,Accent.copy(alpha=.45f))){
        Icon(Icons.Rounded.DirectionsCar,null,tint=Color(0xFFC9B8FF),modifier=Modifier.padding(13.dp).size(30.dp))
       }
       Spacer(Modifier.width(14.dp))
       Column(Modifier.weight(1f)){
        Text(rank.title,color=TextPrimary,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
        Text("Уровень "+(ranks.indexOf(rank)+1),color=TextMuted,style=MaterialTheme.typography.bodyMedium)
       }
       Text(xp.toString()+" XP",color=Color(0xFFC9B8FF),fontWeight=FontWeight.ExtraBold)
      }
      if(next!=null){
       val progress=((xp-rank.minXp).toFloat()/(next.minXp-rank.minXp)).coerceIn(0f,1f)
       LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(7.dp),color=Accent,trackColor=Color(0xFF26374A))
       Text("Ещё "+(next.minXp-xp)+" XP до следующего уровня",color=TextMuted,style=MaterialTheme.typography.bodySmall)
      }
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
       StatTile(Icons.Rounded.LocalFireDepartment,streak.toString(),"серия",Orange,Modifier.weight(1f))
       StatTile(Icons.Rounded.CheckCircle,(xp/12).coerceAtLeast(0).toString(),"решено",Good,Modifier.weight(1f))
       StatTile(Icons.Rounded.Error,mistakes.toString(),"ошибки",Bad,Modifier.weight(1f))
      }
     }
    }
   }
   item{
    Card(onClick=onImages,shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Accent),modifier=Modifier.fillMaxWidth()){
     Row(Modifier.padding(19.dp),verticalAlignment=Alignment.CenterVertically){
      Surface(shape=RoundedCornerShape(14.dp),color=Color.White.copy(alpha=.14f)){Icon(Icons.Rounded.PlayArrow,null,tint=Color.White,modifier=Modifier.padding(10.dp).size(28.dp))}
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)){
       Text("Продолжить обучение",color=Color.White,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.titleLarge)
       Text("Ситуации с картинками • "+qs.count{it.visual>=100}+" новых",color=Color(0xFFE6DEFF),style=MaterialTheme.typography.bodyMedium)
      }
      Icon(Icons.Rounded.ArrowForward,null,tint=Color.White)
     }
    }
   }
   item{
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
     MiniActionCard("Экзамен","10 вопросов",Icons.Rounded.School,Color(0xFF56A7FF),onExam,Modifier.weight(1f))
     MiniActionCard("По темам","Все разделы",Icons.Rounded.MenuBook,Color(0xFFFFA452),onTopics,Modifier.weight(1f))
    }
   }
   item{
    Card(onClick=onImages,shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF0E342E)),border=BorderStroke(1.dp,Color(0xFF1D5B4F))){
     Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){
      Surface(shape=RoundedCornerShape(14.dp),color=Good.copy(alpha=.14f)){Icon(Icons.Rounded.Image,null,tint=Good,modifier=Modifier.padding(10.dp).size(26.dp))}
      Spacer(Modifier.width(13.dp))
      Column(Modifier.weight(1f)){
       Text("Ситуации с картинками",color=TextPrimary,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
       Text("Отдельный графический режим",color=Color(0xFFA9C9C0),style=MaterialTheme.typography.bodySmall)
      }
      Icon(Icons.Rounded.ArrowForward,null,tint=Good)
     }
    }
   }
   item{
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
     MiniActionCard("Ошибки","Повторить сложное",Icons.Rounded.Refresh,Bad,onErrors,Modifier.weight(1f))
     MiniActionCard("Статистика","Прогресс обучения",Icons.Rounded.BarChart,Color(0xFF33D2C1),onProfile,Modifier.weight(1f))
    }
   }
  }
 }
}

@Composable
private fun ErrorScreen(mistakes:Set<String>,onBack:()->Unit,onStart:()->Unit){
 Column(Modifier.fillMaxSize().background(AppBg).statusBarsPadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  TextButton(onBack){Icon(Icons.Rounded.ArrowBack,null);Spacer(Modifier.width(6.dp));Text("Главная")}
  Text("Работа над ошибками",color=TextPrimary,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold)
  Text("Слабые места собраны здесь — без лишнего шума.",color=TextMuted)
  Surface(shape=RoundedCornerShape(20.dp),color=AppCard,border=BorderStroke(1.dp,AppStroke)){
   Column(Modifier.padding(18.dp)){
    Text(mistakes.size.toString(),color=if(mistakes.isEmpty())Good else Bad,style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Black)
    Text(if(mistakes.isEmpty())"Ошибок пока нет" else "вопросов нужно повторить",color=TextMuted)
   }
  }
  if(mistakes.isNotEmpty())Button(onClick=onStart,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(16.dp)){Text("Разобрать ошибки")}
 }
}

@Composable
private fun ProfileScreen(xp:Int,streak:Int,mistakes:Int,onBack:()->Unit){
 val rank=rankFor(xp)
 LazyColumn(Modifier.fillMaxSize().background(AppBg).statusBarsPadding().padding(horizontal=18.dp),contentPadding=PaddingValues(bottom=30.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  item{Row(verticalAlignment=Alignment.CenterVertically){IconButton(onBack){Icon(Icons.Rounded.ArrowBack,null,tint=TextPrimary)};Text("Профиль",color=TextPrimary,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.headlineSmall)}}
  item{Card(colors=CardDefaults.cardColors(containerColor=AppCard),shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,AppStroke)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Surface(shape=CircleShape,color=Accent.copy(alpha=.2f)){Icon(Icons.Rounded.DirectionsCar,null,tint=Color(0xFFC9B8FF),modifier=Modifier.padding(16.dp).size(34.dp))};Spacer(Modifier.width(14.dp));Column{Text(rank.title,color=TextPrimary,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text(xp.toString()+" XP",color=TextMuted)}};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){StatTile(Icons.Rounded.LocalFireDepartment,streak.toString(),"серия",Orange,Modifier.weight(1f));StatTile(Icons.Rounded.Error,mistakes.toString(),"ошибки",Bad,Modifier.weight(1f))}}}}
  item{Text("PDD-RB 2.0",color=TextMuted,style=MaterialTheme.typography.labelLarge)}
 }
}

@Composable fun TopicScreen(onBack:()->Unit,onTopic:(Topic)->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),contentPadding=PaddingValues(top=18.dp,bottom=30.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{TextButton(onBack){Text("← На главную")};Text("Темы",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp))}
  items(items=topics){t:Topic->val count=qs.count{it.topic==t.title};Card(onClick={onTopic(t)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(18.dp)){Text(t.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(3.dp));Text(t.subtitle,color=Color(0xFF6E6873));if(count>0){Spacer(Modifier.height(7.dp));Text("$count вопросов",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)}}}}
 }
}

@Composable fun QuestionView(q:Question,index:Int,total:Int,correctCount:Int,xp:Int,streak:Int,onBack:()->Unit,onAnswered:(Boolean)->Unit,onNext:()->Unit){
 var answer by remember(q){mutableStateOf<Int?>(null)}
 var quip by remember(q){mutableStateOf<String?>(null)}
 var showExplain by remember(q){mutableStateOf(false)}
 LaunchedEffect(quip){if(quip!=null){delay(4000);quip=null}}
 Box(Modifier.fillMaxSize()){
  LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),contentPadding=PaddingValues(top=8.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
   item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(onBack,contentPadding=PaddingValues(4.dp)){Text("← Выйти")};Spacer(Modifier.weight(1f));Text("${index+1} / $total",fontWeight=FontWeight.Bold)}}
   item{LinearProgressIndicator(progress={ (index+1).toFloat()/total.coerceAtLeast(1) },modifier=Modifier.fillMaxWidth().height(5.dp))}
   item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(q.topic,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.labelLarge);Spacer(Modifier.weight(1f));Text("✓ $correctCount",color=Good,fontWeight=FontWeight.Bold)}}
   item{Text(q.text,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
   if(q.visual>0) item{if(q.visual>=100) PhotoSituation(q.visual) else RoadSituation(q.visual)}
   items(q.answers.size){i->
    val a=q.answers[i];val selected=answer==i;val correct=answer!=null&&i==q.correct;val wrong=answer!=null&&selected&&i!=q.correct
    val container=when{correct->GoodBg;wrong->BadBg;else->Color.White};val border=when{correct->Good;wrong->Bad;else->Color(0xFFD3CDD7)}
    OutlinedButton(onClick={if(answer==null){answer=i;val ok=i==q.correct;quip=reaction(ok,if(ok)streak+1 else 0);onAnswered(ok);showExplain=!ok}},modifier=Modifier.fillMaxWidth().defaultMinSize(minHeight=50.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=container,contentColor=when{correct->Good;wrong->Bad;else->Color(0xFF352F39)}),border=BorderStroke(if(correct||wrong)2.dp else 1.dp,border),contentPadding=PaddingValues(horizontal=14.dp,vertical=10.dp)){Text((if(correct)"✓  " else if(wrong)"✕  " else "")+a,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.SemiBold)}
   }
   answer?.let{ans->if(ans==q.correct){item{Button(onClick=onNext,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp)){Text(if(index+1<total)"Дальше →" else "Завершить")}}}}
  }
  AnimatedVisibility(visible=quip!=null,enter=fadeIn(),exit=fadeOut(),modifier=Modifier.align(Alignment.TopCenter).padding(top=72.dp,start=22.dp,end=22.dp)){
   Surface(shadowElevation=10.dp,tonalElevation=4.dp,shape=RoundedCornerShape(18.dp),color=Color(0xFF25212A)){Row(Modifier.padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Text("🔥",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.width(10.dp));Text(quip?:"",color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.bodyLarge)}}
  }
 }
 answer?.let{ans->if(showExplain) Dialog(onDismissRequest={}){
  Surface(shape=RoundedCornerShape(26.dp),color=Color.White,shadowElevation=18.dp){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   Text("✕ Неверно",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=Bad)
   Text("Разберём по-простому",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Text(q.explanation,style=MaterialTheme.typography.bodyLarge)
   Button(onClick={showExplain=false;onNext()},modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp)){Text(if(index+1<total)"Разобрался, дальше →" else "Завершить")}
  }}
 }}
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
