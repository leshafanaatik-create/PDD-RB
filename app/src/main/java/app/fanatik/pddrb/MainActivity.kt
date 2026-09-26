package app.fanatik.pddrb

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
 if(roll>42 && streak!=5 && streak!=10) return null
 return when{
  streak>=10 -> "А ЭТО ТОЧНО ДИНЬКА? 10 подряд. ГАИ напряглось."
  streak==5 -> "Денчик разогрелся. Пять подряд — подозрительно."
  correct -> listOf("Опа. Дёня обучаем.","Петрович, это было красиво.","Динька сегодня с мозгами.","ГАИ этот ответ не ожидало.","Денчик нажал не наугад. Уважаем.").random()
  else -> listOf("Динька… знак буквально перед тобой.","Петрович, автобус пока не отменяем.","Дёня, перечитай. Мы никому не скажем.","Вот и вернулся наш Денчик.","ГАИ облегчённо выдохнуло.").random()
 }
}

private val Good=Color(0xFF1B8F4D)
private val GoodBg=Color(0xFFE8F7EE)
private val Bad=Color(0xFFC63C3C)
private val BadBg=Color(0xFFFFECEC)

private val topics=listOf(
 Topic("Общие положения","Основные понятия и обязанности"),
 Topic("Дорожные знаки","Предупреждающие, приоритета, запрещающие и другие"),
 Topic("Дорожная разметка","Горизонтальная и вертикальная"),
 Topic("Светофор и регулировщик","Сигналы и порядок движения"),
 Topic("Маневрирование","Начало движения, перестроение и повороты"),
 Topic("Скорость движения","Ограничения и выбор безопасной скорости"),
 Topic("Обгон и встречный разъезд","Условия и запреты"),
 Topic("Остановка и стоянка","Где разрешены и запрещены"),
 Topic("Проезд перекрёстков","Регулируемые и нерегулируемые"),
 Topic("Пешеходные переходы","Пешеходы и маршрутные ТС"),
 Topic("Железнодорожные переезды","Правила и запреты"),
 Topic("Автомагистрали","Особые требования"),
 Topic("Внешние световые приборы","Когда и чем пользоваться"),
 Topic("Перевозка людей и грузов","Требования безопасности"),
 Topic("Техническое состояние","Условия допуска к движению"),
 Topic("Первая помощь","Действия при ДТП")
)

private val qs=listOf(
 Question("Проезд перекрёстков","На нерегулируемом перекрёстке равнозначных дорог водитель должен уступить дорогу транспортным средствам, приближающимся…",listOf("Слева","Справа","Только прямо","С любой стороны"),1,"На равнозначном перекрёстке преимущество имеет транспортное средство, приближающееся справа."),
 Question("Остановка и стоянка","Разрешается ли остановка непосредственно на пешеходном переходе?",listOf("Да","Да, до 5 минут","Нет","Только для высадки пассажира"),2,"Остановка непосредственно на пешеходном переходе запрещена."),
 Question("Светофор и регулировщик","Что означает жёлтый сигнал светофора?",listOf("Разрешает движение","Запрещает движение, кроме предусмотренных ПДД случаев","Разрешает только поворот","Требует увеличить скорость"),1,"Жёлтый сигнал в общем случае запрещает движение и предупреждает о предстоящей смене сигналов."),
 Question("Маневрирование","Перед началом движения водитель обязан…",listOf("Подать звуковой сигнал","Убедиться, что манёвр безопасен и не создаст препятствий","Включить аварийную сигнализацию","Всегда уступить автомобилю позади"),1,"Перед выполнением манёвра необходимо убедиться в его безопасности и в том, что он не создаст препятствий другим участникам."),
 Question("Маневрирование","При перестроении водитель должен уступить дорогу…",listOf("Только встречным автомобилям","Транспортным средствам, движущимся попутно без изменения направления","Никому","Только маршрутным транспортным средствам"),1,"При перестроении уступают транспортным средствам, движущимся попутно без изменения направления движения."),
 Question("Дорожные знаки","Какую функцию выполняют знаки приоритета?",listOf("Устанавливают очередность проезда","Только ограничивают скорость","Указывают места стоянки","Предупреждают о погоде"),0,"Знаки приоритета устанавливают очередность проезда перекрёстков, пересечений проезжих частей и узких участков дороги."),
 Question("Дорожная разметка","Для чего применяется дорожная разметка?",listOf("Только для украшения дороги","Для установления порядка движения и информирования участников","Только на автомагистралях","Только возле перекрёстков"),1,"Разметка является средством организации дорожного движения и передаёт участникам установленные требования и информацию."),
 Question("Скорость движения","Выбирая скорость, водитель прежде всего должен учитывать…",listOf("Только мощность автомобиля","Дорожные, погодные условия и видимость","Только наличие камер","Только скорость потока"),1,"Скорость должна позволять контролировать движение с учётом дорожной обстановки, видимости и состояния дороги."),
 Question("Обгон и встречный разъезд","Перед началом обгона водитель обязан убедиться, что…",listOf("Полоса свободна на достаточном расстоянии и манёвр безопасен","Сзади обязательно нет автобуса","Впереди нет светофора","Автомобиль впереди снизил скорость"),0,"Обгон можно начинать только после оценки обстановки и при достаточном свободном пространстве для безопасного завершения манёвра."),
 Question("Пешеходные переходы","При приближении к пешеходному переходу водитель должен…",listOf("Всегда подать звуковой сигнал","Действовать так, чтобы обеспечить безопасность пешеходов","Увеличить скорость","Остановиться независимо от обстановки"),1,"Водитель обязан учитывать пешеходов и выбирать действия, обеспечивающие безопасность на переходе."),
 Question("Железнодорожные переезды","Разрешается ли самовольно открывать шлагбаум на железнодорожном переезде?",listOf("Да, если поезд не виден","Да, ночью","Нет","Да, если очень спешите"),2,"Самовольно открывать шлагбаум и объезжать закрытый или закрывающийся шлагбаум запрещено."),
 Question("Автомагистрали","Можно ли двигаться задним ходом на автомагистрали?",listOf("Да","Только по обочине","Нет","Только ночью"),2,"Движение задним ходом на автомагистрали запрещено."),
 Question("Внешние световые приборы","В тёмное время суток на движущемся автомобиле должны быть включены…",listOf("Предусмотренные ПДД внешние световые приборы","Только аварийная сигнализация","Только габаритные огни во всех случаях","Освещение салона"),0,"В тёмное время суток водитель обязан использовать соответствующие внешние световые приборы."),
 Question("Перевозка людей и грузов","Груз на автомобиле должен размещаться так, чтобы…",listOf("Не ограничивать обзор и не создавать опасности","Закрывать регистрационный знак","Выступать в любую сторону без обозначения","Мешать управлению, если поездка короткая"),0,"Размещение груза не должно ухудшать обзор, затруднять управление или создавать опасность участникам движения."),
 Question("Техническое состояние","Кто отвечает за контроль технического состояния автомобиля перед поездкой?",listOf("Только пассажир","Водитель в пределах установленных обязанностей","Любой пешеход","Только другой водитель"),1,"Перед участием в дорожном движении водитель должен убедиться, что транспортное средство соответствует требованиям безопасности."),
 Question("Первая помощь","При ДТП в первую очередь необходимо оценить…",listOf("Стоимость ремонта","Безопасность места происшествия и состояние пострадавших","Марку автомобилей","Кто снимал происшествие"),1,"Просто: сначала убедись, что место безопасно, затем проверь пострадавших и вызывай помощь. Ремонт и разбор виновных — потом."),
 Question("Проезд перекрёстков","Вы подъезжаете к равнозначному перекрёстку. Синий автомобиль находится справа. Кто должен проехать первым?",listOf("Ваш красный автомобиль","Синий автомобиль","Кто быстрее","Оба одновременно"),1,"Просто: на равнозначном перекрёстке смотри направо. Машина справа имеет преимущество — значит, красный автомобиль пропускает синюю.",1),
 Question("Светофор и регулировщик","Вам горит красный сигнал светофора. Можно ли продолжить движение прямо?",listOf("Да, если никого нет","Нет","Да, если быстро","Только ночью"),1,"Просто: красный — стой. То, что дорога пустая, ничего не меняет. Продолжать движение можно только когда разрешит сигнал или регулировщик.",2),
 Question("Дорожные знаки","Перед перекрёстком установлен знак «Уступить дорогу». Что от вас требуется?",listOf("Обязательно остановиться в любом случае","Уступить тем, кто имеет преимущество","Проехать первым","Только снизить скорость"),1,"Просто: этот знак не требует всегда останавливаться. Нужно пропустить транспорт, которому вы можете помешать. Если уступать некому — после оценки обстановки едете дальше.",3),
 Question("Маневрирование","Красный автомобиль перестраивается в соседнюю полосу, по которой уже едет синий. Кто имеет преимущество?",listOf("Красный","Синий","Кто включил поворотник первым","Преимущества нет"),1,"Просто: меняешь полосу — пропускаешь того, кто уже по ней едет. Поворотник показывает намерение, но преимущества не даёт.",4),
 Question("Пешеходные переходы","Перед вами нерегулируемый пешеходный переход. Пешеход уже вышел на проезжую часть. Как действовать?",listOf("Продолжить движение","Уступить дорогу пешеходу","Подать сигнал и проехать","Объехать по встречной"),1,"Пешеход на переходе имеет преимущество. Снижайте скорость и уступайте дорогу, не вынуждая его менять направление или скорость.",5),
 Question("Обгон и встречный разъезд","Впереди сплошная линия разметки. Можно ли пересечь её для обгона автомобиля?",listOf("Да, если встречных нет","Нет","Да, если автомобиль медленный","Только днём"),1,"Сплошную линию, разделяющую встречные потоки, для такого обгона пересекать нельзя.",6),
 Question("Остановка и стоянка","Красный автомобиль собирается остановиться прямо перед пешеходным переходом, закрывая обзор. Это безопасное и допустимое место?",listOf("Да","Нет","Только с аварийкой","Только на минуту"),1,"Остановка возле перехода регулируется ПДД так, чтобы сохранялась видимость пешеходов и транспорта. На схеме выбран опасный вариант непосредственно перед переходом.",7)
)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF6750A4))){App(this)}}}
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
 fun start(list:List<Question>){pool=if(list.isEmpty())qs else list.shuffled();index=0;correctCount=0;current=pool.first();screen="question"}
 fun next(){if(index+1<pool.size){index++;current=pool[index]}else screen="home"}
 Surface(Modifier.fillMaxSize(),color=Color(0xFFFCF8FF)){
  when(screen){
   "home"->HomeScreen(xp,streak,{screen="topics"},{start(qs.shuffled().take(10))},{screen="errors"})
   "topics"->TopicScreen({screen="home"}){start(qs.filter{q->q.topic==it.title})}
   "question"->QuestionView(current,index,pool.size,correctCount,xp,streak,{screen="home"},{ok->
    if(ok){correctCount++;streak++;xp+=10+(streak.coerceAtMost(10));mistakes=mistakes-current.text}else{streak=0;mistakes=mistakes+current.text}
    prefs.edit().putInt("xp",xp).putInt("streak",streak).putStringSet("mistakes",mistakes).apply()
   },{next()})
   else->Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
    TextButton({screen="home"}){Text("← На главную")}
    Text("Работа над ошибками",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
    val wrong=qs.filter{mistakes.contains(it.text)}
    if(wrong.isEmpty()) Card{Text("Ошибок пока нет. Подозрительно хорошо, Денчик.",Modifier.padding(18.dp))}
    else {Text("Накоплено: ${wrong.size}",color=Bad,fontWeight=FontWeight.Bold);Button({start(wrong)},Modifier.fillMaxWidth()){Text("Разобрать ошибки")}}
   }
  }
 }
}

@Composable fun HomeScreen(xp:Int,streak:Int,onTopics:()->Unit,onExam:()->Unit,onErrors:()->Unit){
 val rank=rankFor(xp);val next=nextRank(xp)
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),contentPadding=PaddingValues(top=30.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column{Text("ПДД РБ",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black);Text("DENIS EDITION",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)};Spacer(Modifier.weight(1f));Surface(shape=RoundedCornerShape(16.dp),color=Color(0xFFEEE7FF)){Text("🔥 $streak",Modifier.padding(horizontal=14.dp,vertical=10.dp),fontWeight=FontWeight.Bold)}}}
  item{Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF292331)),shape=RoundedCornerShape(26.dp)){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(18.dp),color=Color(0xFF7558B5)){Text("Д",Modifier.padding(horizontal=18.dp,vertical=13.dp),color=Color.White,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineMedium)};Spacer(Modifier.width(14.dp));Column{Text(rank.title,color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text("$xp XP",color=Color(0xFFD8C9FF),fontWeight=FontWeight.Bold)}};if(next!=null){LinearProgressIndicator(progress={((xp-rank.minXp).toFloat()/(next.minXp-rank.minXp)).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(8.dp),color=Color(0xFFB89BFF),trackColor=Color(0xFF4A4254));Text("Ещё ${next.minXp-xp} XP → ${next.title}",color=Color(0xFFCFC7D6),style=MaterialTheme.typography.bodyMedium)}else Text("Максимальный уровень безобразия достигнут.",color=Color(0xFFCFC7D6))}}}
  item{Text("Продолжить подготовку",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=5.dp))}
  item{Card(onClick=onExam,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF7354B2))){Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Экзамен",color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text("10 случайных вопросов",color=Color(0xFFE8DEFF))};Text("→",color=Color.White,style=MaterialTheme.typography.headlineMedium)}}}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){Card(onClick=onTopics,modifier=Modifier.weight(1f),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(18.dp)){Text("📚",style=MaterialTheme.typography.headlineMedium);Spacer(Modifier.height(8.dp));Text("По темам",fontWeight=FontWeight.Bold);Text("Разобрать правила",style=MaterialTheme.typography.bodySmall,color=Color.Gray)}};Card(onClick=onErrors,modifier=Modifier.weight(1f),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(18.dp)){Text("🎯",style=MaterialTheme.typography.headlineMedium);Spacer(Modifier.height(8.dp));Text("Ошибки",fontWeight=FontWeight.Bold);Text("Добить слабые места",style=MaterialTheme.typography.bodySmall,color=Color.Gray)}}}}
  item{Text("Прогресс сохраняется на устройстве",style=MaterialTheme.typography.labelMedium,color=Color.Gray,modifier=Modifier.padding(top=8.dp))}
 }
}

@Composable fun TopicScreen(onBack:()->Unit,onTopic:(Topic)->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),contentPadding=PaddingValues(top=18.dp,bottom=30.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{TextButton(onBack){Text("← На главную")};Text("Темы",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp))}
  items(topics){t->val count=qs.count{it.topic==t.title};Card(onClick={onTopic(t)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(18.dp)){Text(t.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(3.dp));Text(t.subtitle,color=Color(0xFF6E6873));if(count>0){Spacer(Modifier.height(7.dp));Text("$count вопросов",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)}}}}
 }
}

@Composable fun QuestionView(q:Question,index:Int,total:Int,correctCount:Int,xp:Int,streak:Int,onBack:()->Unit,onAnswered:(Boolean)->Unit,onNext:()->Unit){
 var answer by remember(q){mutableStateOf<Int?>(null)}
 var quip by remember(q){mutableStateOf<String?>(null)}
 var showExplain by remember(q){mutableStateOf(false)}
 LaunchedEffect(quip){if(quip!=null){delay(2100);quip=null}}
 Box(Modifier.fillMaxSize()){
  LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),contentPadding=PaddingValues(top=8.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
   item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(onBack,contentPadding=PaddingValues(4.dp)){Text("← Выйти")};Spacer(Modifier.weight(1f));Text("${index+1} / $total",fontWeight=FontWeight.Bold)}}
   item{LinearProgressIndicator(progress={ (index+1).toFloat()/total.coerceAtLeast(1) },modifier=Modifier.fillMaxWidth().height(5.dp))}
   item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(q.topic,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.labelLarge);Spacer(Modifier.weight(1f));Text("✓ $correctCount",color=Good,fontWeight=FontWeight.Bold)}}
   item{Text(q.text,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
   if(q.visual>0) item{RoadSituation(q.visual)}
   items(q.answers.size){i->
    val a=q.answers[i];val selected=answer==i;val correct=answer!=null&&i==q.correct;val wrong=answer!=null&&selected&&i!=q.correct
    val container=when{correct->GoodBg;wrong->BadBg;else->Color.White};val border=when{correct->Good;wrong->Bad;else->Color(0xFFD3CDD7)}
    OutlinedButton(onClick={if(answer==null){answer=i;val ok=i==q.correct;quip=reaction(ok,if(ok)streak+1 else 0);onAnswered(ok);showExplain=true}},modifier=Modifier.fillMaxWidth().defaultMinSize(minHeight=50.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=container,contentColor=when{correct->Good;wrong->Bad;else->Color(0xFF352F39)}),border=BorderStroke(if(correct||wrong)2.dp else 1.dp,border),contentPadding=PaddingValues(horizontal=14.dp,vertical=10.dp)){Text((if(correct)"✓  " else if(wrong)"✕  " else "")+a,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.SemiBold)}
   }
  }
  AnimatedVisibility(visible=quip!=null,enter=fadeIn(),exit=fadeOut(),modifier=Modifier.align(Alignment.TopCenter).padding(top=72.dp,start=22.dp,end=22.dp)){
   Surface(shadowElevation=10.dp,tonalElevation=4.dp,shape=RoundedCornerShape(18.dp),color=Color(0xFF25212A)){Row(Modifier.padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Text("🔥",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.width(10.dp));Text(quip?:"",color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.bodyLarge)}}
  }
 }
 answer?.let{ans->if(showExplain) Dialog(onDismissRequest={}){
  Surface(shape=RoundedCornerShape(26.dp),color=Color.White,shadowElevation=18.dp){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   Text(if(ans==q.correct)"✓ Правильно" else "✕ Неверно",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=if(ans==q.correct)Good else Bad)
   Text("Разберём по-простому",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Text(q.explanation,style=MaterialTheme.typography.bodyLarge)
   Button(onClick={showExplain=false;onNext()},modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp)){Text(if(index+1<total)"Понятно, дальше →" else "Завершить")}
  }}
 }}
}

@Composable fun RoadSituation(type:Int){
 Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFD9D4C6))){
  Canvas(Modifier.fillMaxWidth().height(205.dp)){
   val w=size.width;val h=size.height;val road=Color(0xFF474C52);val grass=Color(0xFFBFC9A9);val curb=Color(0xFFD8D5CB);val white=Color(0xFFF7F4E9)
   drawRect(grass)
   fun laneH(){drawRect(curb,Offset(0f,h*.20f),Size(w,h*.60f));drawRect(road,Offset(0f,h*.23f),Size(w,h*.54f))}
   fun laneV(){drawRect(curb,Offset(w*.31f,0f),Size(w*.38f,h));drawRect(road,Offset(w*.34f,0f),Size(w*.32f,h))}
   fun dashH(y:Float){for(i in 0..9 step 2)drawLine(white,Offset(w*i/10f,y),Offset(w*(i+1)/10f,y),3f)}
   fun dashV(x:Float){for(i in 0..9 step 2)drawLine(white,Offset(x,h*i/10f),Offset(x,h*(i+1)/10f),3f)}
   fun car(cx:Float,cy:Float,vertical:Boolean,color:Color){
    val cw=if(vertical)w*.115f else w*.22f;val ch=if(vertical)h*.32f else h*.17f
    drawRoundRect(Color(0x55000000),Offset(cx-cw/2+5,cy-ch/2+6),Size(cw,ch),CornerRadius(15f))
    drawRoundRect(color,Offset(cx-cw/2,cy-ch/2),Size(cw,ch),CornerRadius(15f))
    if(vertical){
     drawRoundRect(Color(0xFFB9D6E4),Offset(cx-cw*.34f,cy-ch*.28f),Size(cw*.68f,ch*.18f),CornerRadius(5f))
     drawRoundRect(Color(0xFF83A8BA),Offset(cx-cw*.34f,cy+ch*.08f),Size(cw*.68f,ch*.17f),CornerRadius(5f))
     drawCircle(Color(0xFFFFF1B0),3.5f,Offset(cx-cw*.27f,cy-ch*.43f));drawCircle(Color(0xFFFFF1B0),3.5f,Offset(cx+cw*.27f,cy-ch*.43f))
    }else{
     drawRoundRect(Color(0xFFB9D6E4),Offset(cx-cw*.28f,cy-ch*.34f),Size(cw*.18f,ch*.68f),CornerRadius(5f))
     drawRoundRect(Color(0xFF83A8BA),Offset(cx+cw*.08f,cy-ch*.34f),Size(cw*.17f,ch*.68f),CornerRadius(5f))
     drawCircle(Color(0xFFFFF1B0),3.5f,Offset(cx+cw*.43f,cy-ch*.27f));drawCircle(Color(0xFFFFF1B0),3.5f,Offset(cx+cw*.43f,cy+ch*.27f))
    }
   }
   fun zebra(x0:Float,x1:Float,y0:Float,y1:Float,vertical:Boolean){
    for(i in 0..7 step 2) if(vertical) drawRect(white,Offset(x0+(x1-x0)*i/8f,y0),Size((x1-x0)/8f,y1-y0))
    else drawRect(white,Offset(x0,y0+(y1-y0)*i/8f),Size(x1-x0,(y1-y0)/8f))
   }
   fun arrow(a:Offset,b:Offset,color:Color=Color(0xFFFFD54F)){drawLine(color,a,b,7f);val dx=b.x-a.x;val dy=b.y-a.y;val len=kotlin.math.sqrt(dx*dx+dy*dy);val ux=dx/len;val uy=dy/len;val px=-uy;val py=ux;drawLine(color,b,Offset(b.x-ux*18+px*10,b.y-uy*18+py*10),7f);drawLine(color,b,Offset(b.x-ux*18-px*10,b.y-uy*18-py*10),7f)}
   when(type){
    1,3->{laneH();laneV();dashH(h*.5f);dashV(w*.5f);car(w*.43f,h*.82f,true,Color(0xFFE44743));if(type==1){car(w*.82f,h*.43f,false,Color(0xFF397FD5));arrow(Offset(w*.43f,h*.69f),Offset(w*.43f,h*.54f));arrow(Offset(w*.70f,h*.43f),Offset(w*.54f,h*.43f))}else{val p=Path();p.moveTo(w*.77f,h*.63f);p.lineTo(w*.84f,h*.76f);p.lineTo(w*.70f,h*.76f);p.close();drawPath(p,Color.White);drawPath(p,Color(0xFFE14A45),style=androidx.compose.ui.graphics.drawscope.Stroke(5f));arrow(Offset(w*.43f,h*.68f),Offset(w*.43f,h*.53f))}}
    2->{laneH();dashH(h*.5f);car(w*.28f,h*.63f,false,Color(0xFFE44743));drawRoundRect(Color(0xFF202328),Offset(w*.78f,h*.02f),Size(w*.10f,h*.42f),CornerRadius(12f));drawCircle(Color(0xFFE53935),14f,Offset(w*.83f,h*.10f));drawCircle(Color(0xFF55585C),14f,Offset(w*.83f,h*.23f));drawCircle(Color(0xFF55585C),14f,Offset(w*.83f,h*.36f));arrow(Offset(w*.39f,h*.63f),Offset(w*.55f,h*.63f),Color(0x66FFD54F))}
    4->{laneH();drawLine(white,Offset(0f,h*.5f),Offset(w,h*.5f),3f);car(w*.31f,h*.64f,false,Color(0xFFE44743));car(w*.62f,h*.38f,false,Color(0xFF397FD5));arrow(Offset(w*.39f,h*.64f),Offset(w*.55f,h*.40f))}
    5->{laneH();dashH(h*.5f);zebra(w*.61f,w*.72f,h*.23f,h*.77f,true);car(w*.34f,h*.63f,false,Color(0xFFE44743));drawCircle(Color(0xFF2C2D30),7f,Offset(w*.66f,h*.42f));drawLine(Color(0xFF2C2D30),Offset(w*.66f,h*.45f),Offset(w*.66f,h*.57f),5f);drawLine(Color(0xFF2C2D30),Offset(w*.66f,h*.49f),Offset(w*.63f,h*.53f),4f);drawLine(Color(0xFF2C2D30),Offset(w*.66f,h*.49f),Offset(w*.69f,h*.53f),4f)}
    6->{laneH();drawLine(white,Offset(0f,h*.5f),Offset(w,h*.5f),6f);car(w*.30f,h*.63f,false,Color(0xFFE44743));car(w*.57f,h*.63f,false,Color(0xFF87919A));val p=Path();p.moveTo(w*.36f,h*.62f);p.cubicTo(w*.43f,h*.36f,w*.59f,h*.35f,w*.69f,h*.38f);drawPath(p,Color(0xFFFFD54F),style=androidx.compose.ui.graphics.drawscope.Stroke(7f))}
    7->{laneH();dashH(h*.5f);zebra(w*.62f,w*.73f,h*.23f,h*.77f,true);car(w*.48f,h*.63f,false,Color(0xFFE44743));drawLine(Color(0xFFFFC107),Offset(w*.59f,h*.24f),Offset(w*.59f,h*.76f),5f)}
    else->{laneH();dashH(h*.5f);car(w*.30f,h*.63f,false,Color(0xFFE44743));car(w*.67f,h*.37f,false,Color(0xFF397FD5))}
   }
  }
 }
}
