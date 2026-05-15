# PDF 流式布局引擎：注解驱动的智能证书生成工具

## 📌 项目定位
一个基于**注解驱动**的 PDF 流式布局引擎，专为解决 **“固定框架 + 动态内容”** 的文档生成而生。尤其适用于证书、报告、合同等需要**自动换行、动态高度、多列表堆叠**的单页文档场景。

## ✨ 核心亮点
- **注解式布局**：用 `@Position`、`@Font`、`@ImageStyle` 等注解声明式定义样式和位置，业务代码与布局解耦。
- **自动换行**：逐字符精确计算宽度，完美支持中英文混排，不截断汉字。
- **动态页面高度**：自动测量可变内容，页面高度随数据伸缩，底部不留白。
- **多列表垂直堆叠**：支持一个模板内多个动态列表，按声明顺序自动排列。
- **可扩展设计**：提供前置/后置扩展点，背景、边框等特殊需求不侵入核心流程。

## 🚀 快速开始

### 1. 环境要求
- JDK 8
- Maven 3.6+
- PDFBox 2.0.x

### 2. 引入依赖
```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.27</version>
</dependency>
```

### 3. 唯一接口：`PdfUtils.generatePdf()`

你只需要调用这一个方法，传入**数据模型对象**和**可选的背景模板路径**即可。

```java
// 1. 构造数据模型，按照你的业务填充字段
CertiModel certData = new CertiModel();
certData.setCertiTitle("细胞储存证书");
// ... 设置其他字段，如细胞列表等

// 2. 生成 PDF（自动计算高度，支持换行、空值跳过）
String outputPath = "your_certificate.pdf";
PdfUtils.generateDocument(certData, null);  // 第二个参数为背景模板路径，不需要则传 null
```

**就是这么简单。** 引擎会自动：
- 读取注解中的坐标、字体、颜色等信息；
- 处理列表循环、换行、空字段跳过；
- 动态计算页面总高度并生成最终的 PDF 文件(文件输出路径暂时写死，会在target/output.pdf)。

## 📖 文档与源码
- **完整技术博客**：[(https://blog.csdn.net/qq_52392844/article/details/161058497?spm=1001.2014.3001.5501)]（万字详解、流程图、踩坑记录）
- **核心工具类**：[`PdfUtils.java`](src/main/java/pdf/utils/PdfUtils.java)

## 📂 项目结构
```
pdf-layout-engine
├── src/main/java/pdf
│   ├── anno            # 核心注解 (@Position, @Font, @PdfList...)
|   ├── cell            # 测试细胞类
│   ├── common           # 核心工具 (PdfUtils, 包含 generateDocument 方法)
│   ├── config          # 字体配置、Swagger配置管理
│   ├── enums           # 涉及的枚举。例如AlignEeum表示居中类型
│   ├── model           # 示例模型 (CertiModel)
│   ├── service           # 测试而建立的服务类
│   └── SpringBootDemo  # 简易 Spring Boot 测试入口
└── src/main/resources
    └── fonts           # 示例中文字体文件
```

## 🧩 能力边界

| ✅ 能做什么 | ❌ 不能做什么 |
| --- | --- |
| 固定框架 + 动态列表文档 | 行内混排（如图文同行） |
| 自动换行、动态高度、空值跳过 | 表格、多列布局 |
| 多列表垂直堆叠 | 复杂嵌套递归布局（可通过分层调用实现） |
| 绝对坐标固定模板 | — |
| 已有模版绘制新内容 | 该模版内容高度动态变化 | 

## 🔌 扩展点
- **前置处理**：先调用 `getTotalLength(模版对象)` 获取内容高度，自行创建带背景的页面后，再调用引擎绘制内容。
- **后置处理**：引擎返回生成的文件路径，你可以在其上自由添加水印、边框等。

## 🤝 关于项目
这个项目是我在**空窗期**为了复盘技术、保持开发手感而做的练手项目。它可能不够完美，但我如果发现不足之处会进行修改，逐渐完善。
