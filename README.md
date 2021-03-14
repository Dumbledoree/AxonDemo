

# 事件溯源
常规的java 后端业务代码的功能基本可以划分为增删改查四个功能。有时，会需要记录下，用户对某个具体的业务对象的操作进行过哪些操作，方便溯源。在这种场景下，解决的方案通常有三种。
### 在日志中记录
此种方式也有多个弊端
1.对代码有侵入
2.日志文件会变大，且检索统计不方便
3.容易有遗漏


### 后端服务入口处记录

适用于在业务代码已经完成，不适合大改的场景下，在每次前端访问或者别的微服务访问时，记录下访问接口和参数以及执行结果。
利用面向切面编程（设计模式中的装饰者模式）实现，技术选型上可以用java web的三大利器（Listener、Filter、Interceptor），如果采用的是spring框架，还可以采用AsepctJ库进行面向切面编程。如果是采用dubbo组件提供对外服务，可以使用dubbo的filter来实现。
这种实现方式常有多个弊端如下：
**1.1 接口不能准确的区分出增删改和查两个功能的区别。**
一个开发初期定义为查询的get接口，经过几个版本的变更开发，对应的service层的函数内可能也隐藏着对数据库的改动。
**1.2 记录无法适合适配业务逻辑的变更。**
业务代码会时常有变更需求。经历过多个版本的变更后，在接口处做的操作历史记录因为模型和业务逻辑的变更就失去了溯源价值。

### 利用数据库自带的操作日志功能，
但这也有两个弊端
**1.依赖于数据库，而不是所有的数据库都带操作日志记录功能**
**2.日志占用硬盘空间大，成本过高。**
**3.在微服务架构中，一次接口的提交可能会修改多个微服务的数据库，单个数据库的记录有时无法复原出用户的操作。**





那有什么比较好的方式实现事件溯源呢。笔者主要主要介绍下在微服务发展4，5年后，又出来的一个新的软件架构**CQRS/EventSourcing**。首先先介绍下现有的MVC架构。


# MVC架构

现在是2021年初，spring 基本一统java 后端业务代码的天下，大部分程序员开发都采用传统的MVC架构，但在实际开发场景中，传统的MVC暴露出了多个弊端，便逐渐有人针对MVC的弊端提出了新的架构提升。

![MVC架构](https://img-blog.csdnimg.cn/20210206115321421.png)

在spring的设计中，整个业务代码，主要分为Controller层、Service层、Repository层，此外还有config模块和Util模块等。与前端的交互为DTO模型。与数据库交互的为PO模型。为此spring特地设置了@RestController/Controller注解、@Service注解和@Repository注解。
在spring推广阶段，此种MVC架构设计具有优势，业务代码开发人员能快速上手发。但随着越来越多的场景使用MVC架构，弊端也暴露出来。

1. Service层随着业务的增加，会越来越囊肿，面向对象编程变成了面向过程编程。
2. 数据更改风险大。由于机器成本和维护成本问题，很多中小团队是没有完善的数据库备份机制的，在实际业务场景中，往往面临着产品需求变更，需要转换旧数据成新的数据的情况，在没有完善的数据备份情况下，刷旧数据成了一个非常大的风险点，一旦刷错，面临者无法回滚的问题。
3. 有时产品中会面临着数据溯源的需求，如某个对象曾经被哪些人做过哪些修改，历史数据状态如何查看等。现有的MVC架构对此种场景没有框架级支持。



# CQRS/EventSourcing架构设计


就是最近新出来的CQRS/EventSouring。
### 读写分离
CQRS的英文全程是Command Query Responsibility Segregation。中文意思是命令查询责任分离。顾名思义，就是在软件架构设计上，将命令和查询区分。command就是数据库的增删改，Query就是数据库的查询。
EventSourcing就是事件溯源，将每一次的数据库改动看成一次事件。整个数据的变化都是由事件驱动的，由国外的Martin Fowler提出来的。
CQRS和EventSourcing本来不是一起的，但在EventSourcing提出来后。大家迅速发现，跟CQRS正好结合。
![CQRS/EventSourcing架构图](https://img-blog.csdnimg.cn/20210206184619495.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dYWE0xMjM0,size_16,color_FFFFFF,t_70)
如上图，所有的职责按功能划分为增删改查Command。增删改依次对应一个或多个event事件，每一次的增删改都会生成一组event，event会保存到单独一个数据库，每个event都会对应一个handler，handler会将根据event内容修改数业务据库。查询直接从业务数据库中读取数据。整个架构中有两个数据库，一个专门保存event，一个存储专门的业务对象。在此种架构下，还实现了读写分离。
### 事件溯源
在CQRS/EventSourcing的软件架构下，每次业务对象的增删改，都会产生一个event事件保存到另外的数据库中，业务对象数据库只会是最新的业务对象状态。event的数据库可以起到事件溯源的作用，比如在设计到金钱交易的系统中，往往需要查询统计历史交易记录。此外，当用户误操作时，还可以起到重演的功能，比如某次用户某次提交失误，想要回滚到上一个时刻，只需要将提交失误前的event拿出来，依次给传给handler处理，即可回滚到上一个时刻。
类似于git操作。Event相当于每次的commit。repository相当于git上代码的最新状态，只要有commit在，git上管理的代码可以回滚到任一时刻。

### 领域模型
此时，还有个问题暴露出来了。一个微服务正常情况下不会只有一个业务对象啊，即不会只有一个表啊。比如小王开了一家专门卖机械键盘的网店， 需要一个键盘售卖管理系统，在系统中建立一个键盘库存表，表里面如价格，库存，品牌型号。除此外，网店还需要还需要一个资金对象表，如初始投入资金多少，剩余资金多少等。每次有用户购买一个键盘时，会同时修改键盘库存表和账单表。那比如有用户跟小王买了一个cherry的茶轴的键盘回家打游戏。此时对于键盘售卖管理系统而言，就生成了一个购买键盘的command，但这个command是该只生成一个修改键盘库存的event，在这个event handler里同时修改键盘库存表和资金对象表呢，这个就需要利用利用领域模型的聚合根对象概念来设计了，但此牵扯到聚合根模型，所需要的知识内容就很多了，本篇文章就不介绍了，后续文章里会有介绍。咱们就这边就简化成类对象就行了。


# AXON框架
在领域模型有很多演化模型，如清洁模型，六边形模型，其中大多尚没有框架级软件的支持，而CQRS/EventSourcing就有一点新颖的框架级支持了，就是Axon。官网链接如下
Axon [link](https://docs.axoniq.io/reference-guide/)
以下为Axon的详细且通俗易懂的介绍.
![Axon架构设计](https://img-blog.csdnimg.cn/20210208221020779.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dYWE0xMjM0,size_16,color_FFFFFF,t_70)



如上：在原先的CRQS/EventSourcing中，Axon框架主要多了几个概念对象 :Command Gateway、Query Gateway，Aggregate和EventBus。Axon主要采取观察者模式实现command各层之间的消息转发。

##  Spring 集成Axon
Axon 现在主要分为Axon FrameWork和Axon Server两个产品。Axon Server分为社区版和企业版。Axon Server的大部分功能也可以通过Axon 提供的Api实现。在Axon 4.0+版本里，Spring 集成Axon时，会默认连接AxonServer，为了集成的便捷性，咱们去除axon-server-connector。

```maven
<dependency>
    <groupId>org.axonframework</groupId>
     <artifactId>axon-spring-boot-starter</artifactId>
     <version>4.4.3</version>
     <exclusions>
         <exclusion>
             <artifactId>axon-server-connector</artifactId>
             <groupId>org.axonframework</groupId>
         </exclusion>
     </exclusions>
</dependency>
 ```



## Command Gateway
Axon提供了两个接口实现了消息的转发
1.Command Bus
2.Command Gateway
其中Command Gateway底层封装调用了Command Bus，使用更加简单。
主要提供两个api，send和sendAndWait。Command Bus的实现默认为DefaultCommandGateway。
sendAndWait可以获取Aggregate的返回结果。
 ```java
     Boolean result=commandGateway.sendAndWait(new SellCommand(identifierFactory.generateIdentifier(),name,number));
```

send为无须获知结果的情况下。
```java
    commandGateway.send(new RestockCommand(identifierFactory.generateIdentifier(),name,number));
```

## Command
在Axon定义的Command里面，必须有一个成员变量，加注解@TargetAggregateIdentifier，且不能为空。
```java
@Getter
public class BaseCommand {

    @TargetAggregateIdentifier
    private String targetAggregateIdentifier;
    
    public BaseCommand(String targetAggregateIdentifier){
        this.targetAggregateIdentifier=targetAggregateIdentifier;
    }
}
```
## Aggregate
所有的Command的处理都需要在Aggregate中进行。
一个Aggregate 聚合类的定义如
```java
@Aggregate
@Slf4j
public class KeyboardAggregate {

    @AggregateIdentifier
    private String id;
    
    ...
}
```
聚合根都必须在类上加注解@Aggregate，且必须拥有加注解AggregateIdentifier的成员变量id，id本身起任何名字都可以。程序运行，进入Aggregate时，必须给id赋值。
对Command的处理如下：
```java
@CommandHandler
@CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
public void on(RestockCommand command) {
    log.info("RestockCommand:{}", command);
    this.id = command.getTargetAggregateIdentifier();
    AggregateLifecycle.apply(new RestockEvent(command));
}
```
在Axon 4.3版本后，在CommandHandler函数上必须加注解@CreationPolicy，且指定Aggregate实例化方式，总共三种生成方式：ALWAYS、CREATE_IF_MISSING和NEVER。且在Command Handler函数里面，对event进行调用，调用函数为AggregateLifecycle.apply。采用的也是观察者模式。

## Event
在简单的应用场景中，Event成员变量跟对应的Command一致即可，但跟Command不同的是，无须属性加注解@TargetAggregateIdentifier。因此Event可以继承对应的Command。
```java
@Revision("1.0")
public class RestockEvent extends RestockCommand {

    public RestockEvent(RestockCommand command) {
        super(null, command.getName(), command.getNumber());
    }
}
```
注解@Revision("1.0")表示event版本，可不加，由于event可以保存到数据库中，且在业务迭代过程中，event可能发生变化，Revision表示对应的版本，并可借助Revision在重播过程中实现对event不同版本的兼容。

## EventHandler
在Axon框架设计中，需要定义函数监听对应的Event，代码如下：
```java
@Service
@ProcessingGroup("keyboardHandler")
@AllowReplay
@Slf4j
public class KeyboardHandler {


    @Autowired
    private KeyboardStockRepository keyboardStockRepository;


    @EventHandler
    public void on(RestockEvent event, ReplayStatus replayStatus) {
        KeyboardStock keyboardStock = keyboardStockRepository.findKeyboardStockByName(event.getName());
        if (Objects.isNull(keyboardStock)) {
            keyboardStockRepository.save(new KeyboardStock(null, event.getName(), event.getNumber()));
        } else {
            keyboardStock.setAccount(keyboardStock.getAccount() + event.getNumber());
            keyboardStockRepository.save(keyboardStock);
        }
    }
}
```
其中类上的注解@Service必须得加，是因为handle类必须扫描成bean，因为Axon框架是使用的Spring Aop对加了注解@EventHandler注解的函数且参数里类型有对应Event的函数进行增强的，Spring Aop只能拦截实例化为Bean的函数。
注解@ProcessingGroup表示成员函数的eventhandler函数都会对应一个EventProcessor事件处理器。其中value为自定义事件处理器名字。必须得加。
注解@@AllowReplay表示，允许事件重播回溯。可以不加。
@Slf4j为lombok提供的日志注解，可不加。

@EventHandler必须加到事件处理函数，函数参数中第一个参数RestockEvent 表示此函数监听事件RestockEvent ，第二个参数ReplayStatus 表示当前的事件调用是由聚合类Aggregate发起，还是处于重播过程中。

## Query
在Axon的设计中，查询数据库的过程也需要定义一个Query，并利用queryGateway实现查询转发，参考代码如下：
发送查询Query
```java
@GetMapping("queryKeyboard")
public Integer queryKeyboard(@RequestParam String name) throws ExecutionException, InterruptedException {
    return queryGateway.query(new KeyboardQuery(name), Integer.class).get();
}
```
处理查询
```java
 @QueryHandler
public Integer on(KeyboardQuery query) {
    KeyboardStock keyboardStock = keyboardStockRepository.findKeyboardStockByName(query.getName());
    return Objects.isNull(keyboardStock) ? 0 : keyboardStock.getAccount();
}
```
查询的过程无须Aggregate类的参与。

## EventStore
Axon可以将每次的Event默认保存下来，使用方需要根据使用的数据库类型自定义EventStore，如使用mysql保存event时，参考代码如下。
```java
@Configuration
public class AxonStoreConfig {
    @Bean
    @Primary
    public Serializer axonJsonSerializer() {
        JacksonSerializer jacksonSerializer = JacksonSerializer.builder().build();
        return jacksonSerializer;
    }
    @Bean
    public EmbeddedEventStore eventStore(EventStorageEngine storageEngine, AxonConfiguration configuration) {
        return EmbeddedEventStore.builder()
                .storageEngine(storageEngine)
                .messageMonitor(configuration.messageMonitor(EventStore.class, "eventStore"))
                .build();
    }

    @Bean
    public EventStorageEngine storageEngine(Serializer defaultSerializer,
                                            PersistenceExceptionResolver persistenceExceptionResolver,
                                            @Qualifier("eventSerializer") Serializer eventSerializer,
                                            AxonConfiguration configuration,
                                            EntityManagerProvider entityManagerProvider,
                                            TransactionManager transactionManager) {


        return JpaEventStorageEngine.builder()
                .snapshotSerializer(defaultSerializer)
                .upcasterChain(configuration.upcasterChain())
                .persistenceExceptionResolver(persistenceExceptionResolver)
                .eventSerializer(eventSerializer)
                .entityManagerProvider(entityManagerProvider)
                .transactionManager(transactionManager)
                .build();
    }



}
```
本例为了演示方便，使用h2,内存数据库做演示，使用h2时，以上3个bean都不需要定义
，定义application.properties文件，内容如下，
```
server.port=8099
#配置数据库h2的参数
spring.datasource.url=jdbc:h2:file:~/test
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=root
spring.datasource.password=123456
#在浏览器中开启控制台
spring.h2.console.enabled=true
```
启动spring后，在网页中输入h2数据库的登录链接:http://localhost:8099/h2-console。
窗口如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210314233510732.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dYWE0xMjM0,size_16,color_FFFFFF,t_70)
输入登录密码123456可以进入h2数据库，可以看到如下图
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210314233640859.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dYWE0xMjM0,size_16,color_FFFFFF,t_70)
除了KEYBOARD_STOCK为自定义的表外，Axon会自动生成ASSOCIATION_VALUE_ENTRY 、
DOMAIN_EVENT_ENTRY 、
SAGA_ENTRY 、
SNAPSHOT_EVENT_ENTRY 、
TOKEN_ENTRY 表，
其中，DOMAIN_EVENT_ENTRY 为保存event的表，SAGA_ENTRY 为保存sega事务的表，sega为Axon用了实现分布式事务性的，咱们先不用。SNAPSHOT_EVENT_ENTRY 为event过多时，压缩生成快照保存的表。

## 重播
Axon支持将每次变更数据库的event保存到数据库，再通过重播Replay提取event，再调用eventHandler函数对event进行处理，实现对业务数据库对象的重播回溯。参考代码如下：
```java
public void replay(){
        EventProcessingConfigurer configurer=context.getBean(EventProcessingConfigurer.class);
        configurer.registerTrackingEventProcessor("keyboardHandler");
        configurer.usingTrackingEventProcessors();
        Configuration configuration=context.getBean(Configuration.class);
        EventProcessingConfiguration eventProcessingConfiguration=configuration.eventProcessingConfiguration();
        Optional<TrackingEventProcessor> eventProcessorOptional=eventProcessingConfiguration.eventProcessorByProcessingGroup("keyboardHandler", TrackingEventProcessor.class);
        if(eventProcessorOptional.isPresent()){
            TrackingEventProcessor trackingEventProcessor=eventProcessorOptional.get();
            trackingEventProcessor.shutDown();
            trackingEventProcessor.resetTokens();
            trackingEventProcessor.start();
        }
        configurer.usingSubscribingEventProcessors();
}
```
首先得对Axon自定义的 EventProcessingConfigurer注册上 之前对EventProcessor keyboardHandler，然后调用usingTrackingEventProcessors表明，进入回溯Event模式中，然后获取trackingEventProcessor，进行shutDown、resetTokens和start三个步骤，重播完成后，再调用configurer.usingSubscribingEventProcessors()。表明再返回订阅模式，这步不能忘，否则EventHandler函数就不能监听从Aggregate类发出的event了。


## 完整参考例子
完整的参考例子我放到了github上[链接](https://github.com/Dumbledoree/AxonDemo)：https://github.com/Dumbledoree/AxonDemo。
有兴趣的小伙伴欢迎来指正啊。
可以通过swagger接口http://localhost:8099/swagger-ui.html。来观察整个代码逻辑。




 



