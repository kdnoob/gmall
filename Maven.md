# 一、Maven 

Maven 是一个**项目管理工具**，可以对Java 项目进行 **构建、依赖管理** 。

> **Maven 的 Snapshot 版本与 Release 版本**
>
> 1、Snapshot 版本代表不稳定、尚处于开发中的版本。
>
> 2、Release 版本则代表稳定的版本。
>
> 3、什么情况下该用 SNAPSHOT？
>
> 协同开发时，如果 A 依赖构件 B，由于 B 会更新，B 应该使用 SNAPSHOT 来标识自己。
>
> 这种做法的必要性可以反证如下：
>
> ​	a. 如果 B 不用 SNAPSHOT，而是每次更新后都使用一个稳定的版本，那版本号就会升得太快，每天一升甚至每个小时一升，这就是对版本号的滥用。
>
> ​	b.如果B不用 SNAPSHOT，但一直使用一个单一的 Release 版本号，那当 B 更新后，A 可能并不会接受到更新。因为A所使用的 `respository` 一般不会频繁更新 release 版本的缓存（即本地repository），所以 B 以不换版本号的方式更新后，A 在拿B时发现本地已有这个版本，就不会去远程Repository 下载最新的 B。
>
> 4、 不用 Release 版本，在所有地方都用 SNAPSHOT 版本行不行？     
>
> ​	不行。正式环境中不得使用 snapshot 版本的库。 比如说，今天你依赖某个 snapshot 版本的第三方库成功构建了自己的应用，明天再构建时可能就会失败，因为今晚第三方可能已经更新了它的 snapshot 库。你再次构建时，Maven 会去远程 repository 下载 snapshot 的最新版本，你构建时用的库就是新的 jar 文件了，这时正确性就很难保证了。



# 二、POM

​	POM（Project Object Model， 项目对象模型）是 Maven 工程的基本工作单元，是一个 XML 文件，包含了项目的基本信息，用于描述项目如何构建，声明项目依赖，等等。

```xml
<project xmlns = "http://maven.apache.org/POM/4.0.0"
    xmlns:xsi = "http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation = "http://maven.apache.org/POM/4.0.0
    http://maven.apache.org/xsd/maven-4.0.0.xsd">
 
    <!-- 模型版本 -->
    <modelVersion>4.0.0</modelVersion>
    <!-- 公司或者组织的唯一标志，并且配置时生成的路径也是由此生成， 如com.companyname.project-group，maven会将该项目打成的jar包放本地路径：/com/companyname/project-group -->
    <groupId>com.companyname.project-group</groupId>
 
    <!-- 项目的唯一ID，一个groupId下面可能多个项目，就是靠artifactId来区分的 -->
    <artifactId>project</artifactId>
 
    <!-- 版本号 -->
    <version>1.0</version>
</project>
```



所有 POM 文件都需要 project 元素和三个必需字段：groupId，artifactId，version。



# 三、构建生命周期

| 阶段          | 处理   | 描述                           |
| ----------- | ---- | ---------------------------- |
| 验证 validate | 验证项目 | 验证项目是否正确且所有必须信息是可用的          |
| 编译 compile  | 执行编译 | 源代码编译在此阶段完成                  |
| 测试 Test     | 测试   | 使用适当的单元测试框架（例如JUnit）运行测试。    |
| 包装 package  | 打包   | 创建JAR/WAR包如在 pom.xml 中定义提及的包 |
| 检查 verify   | 检查   | 对集成测试的结果进行检查，以保证质量达标         |
| 安装 install  | 安装   | 安装打包的项目到本地仓库，以供其他项目使用        |
| 部署 deploy   | 部署   | 拷贝最终的工程包到远程仓库中，以共享给其他开发人员和工程 |

为了完成 default 生命周期，这些阶段（包括其他未在上面罗列的生命周期阶段）将被按顺序地执行。

Maven 有以下三个标准的生命周期：

- **clean**：项目清理的处理
- **default(或 build)**：项目部署的处理
- **site**：项目站点文档创建的处理



# 四、Maven仓库

Maven 仓库是项目中依赖的第三方库，这个库所在的位置叫做仓库。

Maven 仓库能帮助我们管理构件（主要是JAR），它就是放置所有JAR文件（WAR，ZIP，POM等等）的地方。

Maven 仓库有三种类型：

- 本地（local）
- 中央（central）
- 远程（remote）



## Maven 依赖搜索顺序

当我们执行 Maven 构建命令时，Maven 开始按照以下顺序查找依赖的库：

- **步骤 1** － 在本地仓库中搜索，如果找不到，执行步骤 2，如果找到了则执行其他操作。
- **步骤 2** － 在中央仓库中搜索，如果找不到，并且有一个或多个远程仓库已经设置，则执行步骤 4，如果找到了则下载到本地仓库中以备将来引用。
- **步骤 3** － 如果远程仓库没有被设置，Maven 将简单的停滞处理并抛出错误（无法找到依赖的文件）。
- **步骤 4** － 在一个或多个远程仓库中搜索依赖的文件，如果找到则下载到本地仓库以备将来引用，否则 Maven 将停止处理并抛出错误（无法找到依赖的文件）。



## Maven 阿里云(Aliyun)仓库

Maven 仓库默认在国外， 国内使用难免很慢，我们可以更换为阿里云的仓库。

第一步:修改 maven 根目录下的 conf 文件夹中的 setting.xml 文件，在 mirrors 节点上，添加内容如下：

```xml
<mirrors>
    <mirror>
      <id>alimaven</id>
      <name>aliyun maven</name>
      <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
      <mirrorOf>central</mirrorOf>        
    </mirror>
</mirrors>
```

第二步: pom.xml文件里添加：

```xml
<repositories>  
        <repository>  
            <id>alimaven</id>  
            <name>aliyun maven</name>  
            <url>http://maven.aliyun.com/nexus/content/groups/public/</url>  
            <releases>  
                <enabled>true</enabled>  
            </releases>  
            <snapshots>  
                <enabled>false</enabled>  
            </snapshots>  
        </repository>  
</repositories>
```

