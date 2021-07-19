# Rabbitmq_example

#### 介绍
Rabbitmq学习路线 课程时长总计：39小时

#### 学习路径

1.  入门：https://coding.imooc.com/class/chapter/461.html#Anchor (课程时长：21小时)
2.  进阶：https://coding.imooc.com/class/chapter/262.html#Anchor  (课程时长：14小时)
3.  精通：https://class.imooc.com/sale/javaarchitect (其中rabbit部分 时长：4小时)

#### Rabbit知识路线

1.  Rabbit基本使用
2.  Rabbiit消息可靠性
3.  Rabbit集群可靠性
4.	Rabbit最终一致性实现

### Rabbit基本使用

1.  语言：erlang 。 协议：AMQP 
2.  组件：
	1. Broker:接收和分发消息应用
	2. Virtual Host:虚拟Broker,将多个单元隔离开
	3. Connection :publisher/consumer 和broker之间的TCP连接
	4. channel:connection内部建立的逻辑连接。
	5. Routing key:路由键，用来指示信息的路由转发，相当于快递的地址
	6. Queue : 消息队列。
	7. Binding: exchange和 queue之间的虚拟连接
	8. Exchange:交换机，路由转发
		1. Direct Exchange :routing key 和 binding key 一致，发送到对应的队列
		2. Fanout Exchange : 分发到所有的queue中
		3. Topic Exchange : 
			1. 全匹配，与Direct类似
			2. Binding key 中的 # ：匹配任意个数的word
			3. Binding key 中的 ×： 匹配任意一个word


### Rabbiit消息可靠性

1.  发送端确认机制(rabbit是否真的收到消息)
	1. 单条同步确认
	2. 多条同步确认
	3. 异步确认
2.  发送端返回机制(消息是否被正确的路由)
	1. Return Listener 会被调用
	2. Mandatory 为true, 会处理无法路由的消息;为false,直接丢弃无法路由的消息
3.  消费端限流机制
	1. QoS，前提是不使用自动确认，保证一定数目的消息未确认前，不消费新的消息
	2. prefetchCount:针对一个消费端最多推送多少未确认消息
	3. global ：true ,针对整个消费端，false，针对当前channel
	4. prefetchsize:0 (单个消息大小限制，一般为0)
4.  消费端确认机制
	1. 自动ACK，消费端收到消息后，会自动签收消息
	2. 手动ACK，消费端收到消息后，需要显式签收消息
	3. 重回队列 ：一般不建议
5.  消费端过期机制
	1. 消息TTL:设置了单条消息的过期时间
	2. 队列TTL：设置了队列中所有消息的过期时间
	3. TTL应长于服务的平均重启时间
6.  死信队列
	1. 死信消息形成原因：
		1. 消息被拒绝 并且 requeue = false
		2. 消息过期
		3. 队列达到最大长度
	2. 设置方法：
		1. Exchange: dlx.exchange
		2. Queue：dlx.queue
		3. Routing key:#
		4. x-dead-letter-exchange =dlx.exchange
7. Spring AMQP的使用
	1. 异步消息监听容器
		1. 原始实现：自我实现线程池，回调方法，并注册回调方法
			1. SimpleMessageListenerContainer
				1. 设置同时监听多个队列，自动启动，自动配置RabbitMQ
				2. 设置消费者数量(最大数量，最小数量，批量消费)
				3. 设置消费确认模式，是否重回队列，异常捕获。
				4. 设置是否独占
				5. 设置具体的监听器和消息转换器
					1. MessageConverter:用来收发消息和自动转换消息
					2. Jackson2JsonMessageConverter
						1. 用来转换json格式消息
						2. 配合ClassMapper 可以直接转换为POJO对象
					3. 自定义MessageConverter
						1. 实现MessageConverter接口
						2. 重写toMessage,fromMessage方法
				6. 支持动态设置，运行中修改监听器设置
					1. MessageLisenerAdapter
						1. 普通模式：实现HandleMessage 方法
						2. 进阶模式：自定义“队列名-方法名”映射关系
		2. Springboot:自动实现可配置的线程池，并自动注册回调方法
	2. 原生提供RabbitTemplate,收发消息
		1. 相比于basicPublish,能实现自动消息转换
	3. 原生提供RabbitAdmin，队列，交换机声明
		1. 声明式提供队列，交换机，绑定关系的注册方法
			1. 将Exchang,Queue,Binding声明为bean
			2. 将RabbitAdmin 声明为Bean
			3. Exchang,Queue,Binding 即可自动创建
	
8. Springboot Rabbit 使用
	1. Spring boot Config原生支持RabbitMQ
		1. 隐式建立Connection,Channel
	2. 使用RabbitListener 使用注解声明，对业务代码无侵入
		1. @RabbitListener 注解
			1. @exchange:自动声明Exchange
			2. @Queue : 自动声明队列
			3. @QueueBinding: 自动声明绑定关系	
					
### Rabbiit集群可靠性
1. Rabbit容量不足 => 
	1. 通过Scale-out 扩展规模
2. Rabbit数据无副本 =>
	1. 通过镜像队列，将数据冗余至多个节点。
3. Rabbit可用性低 =>
	1. KeepAlived +HaProxy 作为热备解决方案

### Rabbiit最终一致性实现方案
1. 基本BASE，以及CAP的理论
2. 设计相关
	1. 发送失败重试
	2. 消费失败重试
	3. 死信告警
3. 失败消息数据表设计
	1. 消息ID
	2. 对应的服务名称
	3. 消息类型（接收，发送，死信）
	4. exchange （交换机）
	5. routing_key( 路由)
	6. queue（队列）
	7. sequence(序号)
	8. payload(消息内容) 
4. 整体流程
	1. 发送消息
		1. 消息持久化
			1. 发送成功 
				1. 确认回调
					1. 发送确认
						1. 删除消息
				2. 返回回调
					1. 重新持久化
			2. 发送失败
				1. 定时任务轮询查询未成功消息
					1. 超过次数限制
						1. 通过邮件短信的方式进行报警
					2. 未超过次数限制
						1. 记录重发次数
							1. 重新发送消息



####  消息中间件作用
1. 异步处理
2. 系统解耦
3. 流量削峰和流控
4. 流量控制

