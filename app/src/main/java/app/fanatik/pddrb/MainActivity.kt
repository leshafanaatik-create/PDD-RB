package app.fanatik.pddrb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Topic(val title:String,val subtitle:String)
data class Question(val topic:String,val text:String,val answers:List<String>,val correct:Int,val explanation:String)

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
 Topic("Первая помощь","Действия при ДТП"),
 Topic("Ответственность водителя","Безопасность и обязанности")
)
private val qs=listOf(
 Question("Проезд перекрёстков","На нерегулируемом перекрёстке равнозначных дорог водитель должен уступить дорогу транспортным средствам, приближающимся…",listOf("Слева","Справа","Только прямо","С любой стороны"),1,"На равнозначном перекрёстке действует правило преимущества транспортного средства, приближающегося справа."),
 Question("Остановка и стоянка","Разрешается ли остановка непосредственно на пешеходном переходе?",listOf("Да","Да, до 5 минут","Нет","Только для высадки пассажира"),2,"Остановка на пешеходном переходе создаёт опасность и запрещена."),
 Question("Светофор и регулировщик","Что означает жёлтый сигнал светофора?",listOf("Разрешает движение","Запрещает движение, кроме предусмотренных ПДД случаев","Разрешает только поворот","Требует увеличить скорость"),1,"Жёлтый сигнал в общем случае запрещает движение и предупреждает о предстоящей смене сигналов."),
 Question("Маневрирование","Перед началом движения водитель обязан…",listOf("Подать звуковой сигнал","Убедиться, что манёвр безопасен и не создаст препятствий","Включить аварийную сигнализацию","Всегда уступить автомобилю позади"),1,"Любой манёвр должен выполняться безопасно и не создавать препятствий другим участникам движения.")
)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme{App()}}}
}
@Composable fun App(){
 var screen by remember{mutableStateOf("home")}
 var selected by remember{mutableStateOf<Question?>(null)}
 Surface(Modifier.fillMaxSize()){
  when(screen){
   "home"->Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Text("ПДД РБ",style=MaterialTheme.typography.headlineLarge);Text("Тренажёр подготовки к теоретическому экзамену")
    Button({screen="topics"},Modifier.fillMaxWidth()){Text("Обучение по темам")}
    Button({selected=qs.random();screen="question"},Modifier.fillMaxWidth()){Text("Экзамен")}
    OutlinedButton({screen="errors"},Modifier.fillMaxWidth()){Text("Работа над ошибками")}
    Text("v0.1.0 • тестовая база вопросов")
   }
   "topics"->LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    item{TextButton({screen="home"}){Text("← Назад")};Text("Темы",style=MaterialTheme.typography.headlineMedium)}
    items(topics){t->Card(Modifier.fillMaxWidth(),onClick={selected=qs.firstOrNull{it.topic==t.title}?:qs.random();screen="question"}){Column(Modifier.padding(16.dp)){Text(t.title,style=MaterialTheme.typography.titleMedium);Text(t.subtitle)}}}
   }
   "question"->QuestionView(selected?:qs.first()){screen="home"}
   else->Column(Modifier.padding(20.dp)){TextButton({screen="home"}){Text("← Назад")};Text("Работа над ошибками",style=MaterialTheme.typography.headlineMedium);Text("Ошибки появятся здесь после прохождения вопросов.")}
  }
 }
}
@Composable fun QuestionView(q:Question,onBack:()->Unit){
 var answer by remember(q){mutableStateOf<Int?>(null)}
 Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  TextButton(onBack){Text("← На главную")};Text(q.topic,style=MaterialTheme.typography.labelLarge);Text(q.text,style=MaterialTheme.typography.headlineSmall)
  q.answers.forEachIndexed{i,a->OutlinedButton({if(answer==null)answer=i},Modifier.fillMaxWidth()){Text(a)}}
  answer?.let{Text(if(it==q.correct)"✓ Правильно" else "✕ Неверно",style=MaterialTheme.typography.titleLarge);Text(q.explanation);Button(onBack){Text("Продолжить")}}
 }
}
