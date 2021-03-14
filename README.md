


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




 



