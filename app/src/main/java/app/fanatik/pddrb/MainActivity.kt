package app.fanatik.pddrb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Topic(val title:String,val subtitle:String)
data class Question(val topic:String,val text:String,val answers:List<String>,val correct:Int,val explanation:String)

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
 Question("Первая помощь","При ДТП в первую очередь необходимо оценить…",listOf("Стоимость ремонта","Безопасность места происшествия и состояние пострадавших","Марку автомобилей","Кто снимал происшествие"),1,"До оказания помощи важно исключить дополнительную опасность, оценить состояние пострадавших и организовать вызов экстренных служб.")
)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF6750A4))){App()}}}
}

@Composable fun App(){
 var screen by remember{mutableStateOf("home")}
 var current by remember{mutableStateOf(qs.first())}
 var pool by remember{mutableStateOf(qs)}
 var index by remember{mutableIntStateOf(0)}
 var correctCount by remember{mutableIntStateOf(0)}
 fun start(list:List<Question>){pool=if(list.isEmpty())qs else list.shuffled();index=0;correctCount=0;current=pool.first();screen="question"}
 fun next(){if(index+1<pool.size){index++;current=pool[index]}else screen="home"}
 Surface(Modifier.fillMaxSize(),color=Color(0xFFFCF8FF)){
  when(screen){
   "home"->HomeScreen({screen="topics"},{start(qs.shuffled().take(10))},{screen="errors"})
   "topics"->TopicScreen({screen="home"}){start(qs.filter{q->q.topic==it.title})}
   "question"->QuestionView(current,index,pool.size,correctCount,{screen="home"},{if(it)correctCount++},{next()})
   else->Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
    TextButton({screen="home"}){Text("← На главную")}
    Text("Работа над ошибками",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
    Card{Text("Здесь будут автоматически собираться вопросы, в которых допущены ошибки.",Modifier.padding(18.dp))}
   }
  }
 }
}

@Composable fun HomeScreen(onTopics:()->Unit,onExam:()->Unit,onErrors:()->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(top=42.dp,bottom=30.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  item{Text("ПДД РБ",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold);Spacer(Modifier.height(5.dp));Text("Подготовка к теоретическому экзамену",style=MaterialTheme.typography.titleMedium,color=Color(0xFF665F6D));Spacer(Modifier.height(22.dp));Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFEDE4FF)),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(22.dp)){Text("Готов к тренировке?",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp));Text("16 вопросов • 16 тем • объяснения после ответа")}}}
  item{Button(onTopics,Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(18.dp)){Text("Учить по темам",style=MaterialTheme.typography.titleMedium)}}
  item{Button(onExam,Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(18.dp)){Text("Экзамен • 10 вопросов",style=MaterialTheme.typography.titleMedium)}}
  item{OutlinedButton(onErrors,Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(18.dp)){Text("Работа над ошибками")}}
  item{Text("v0.2.0 • учебная версия",color=Color.Gray,modifier=Modifier.padding(top=8.dp))}
 }
}

@Composable fun TopicScreen(onBack:()->Unit,onTopic:(Topic)->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),contentPadding=PaddingValues(top=18.dp,bottom=30.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{TextButton(onBack){Text("← На главную")};Text("Темы",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp))}
  items(topics){t->val count=qs.count{it.topic==t.title};Card(onClick={onTopic(t)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(18.dp)){Text(t.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(3.dp));Text(t.subtitle,color=Color(0xFF6E6873));if(count>0){Spacer(Modifier.height(7.dp));Text("$count вопросов",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)}}}}
 }
}

@Composable fun QuestionView(q:Question,index:Int,total:Int,correctCount:Int,onBack:()->Unit,onAnswered:(Boolean)->Unit,onNext:()->Unit){
 var answer by remember(q){mutableStateOf<Int?>(null)}
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(top=14.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(onBack){Text("← Выйти")};Spacer(Modifier.weight(1f));Text("${index+1} / $total",fontWeight=FontWeight.SemiBold)}}
  item{LinearProgressIndicator(progress={ (index+1).toFloat()/total.coerceAtLeast(1) },modifier=Modifier.fillMaxWidth().height(7.dp))}
  item{Row(Modifier.fillMaxWidth()){Text(q.topic,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text("✓ $correctCount",color=Good,fontWeight=FontWeight.Bold)}}
  item{Text(q.text,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)}
  items(q.answers.size){i->
   val a=q.answers[i];val selected=answer==i;val correct=answer!=null&&i==q.correct;val wrong=answer!=null&&selected&&i!=q.correct
   val container=when{correct->GoodBg;wrong->BadBg;else->Color.Transparent};val border=when{correct->Good;wrong->Bad;else->Color(0xFF817984)}
   OutlinedButton(onClick={if(answer==null){answer=i;onAnswered(i==q.correct)}},modifier=Modifier.fillMaxWidth().defaultMinSize(minHeight=62.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=container,contentColor=when{correct->Good;wrong->Bad;else->MaterialTheme.colorScheme.primary}),border=BorderStroke(if(correct||wrong)2.dp else 1.dp,border),contentPadding=PaddingValues(horizontal=18.dp,vertical=14.dp)){Text((if(correct)"✓ " else if(wrong)"✕ " else "")+a,style=MaterialTheme.typography.titleMedium)}
  }
  answer?.let{ans->
   item{Card(colors=CardDefaults.cardColors(containerColor=if(ans==q.correct)GoodBg else BadBg),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Text(if(ans==q.correct)"Правильно" else "Неверно",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=if(ans==q.correct)Good else Bad);Spacer(Modifier.height(6.dp));Text(q.explanation,style=MaterialTheme.typography.bodyLarge)}}}
   item{Button(onNext,Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(18.dp)){Text(if(index+1<total)"Следующий вопрос →" else "Завершить")}}
  }
 }
}
