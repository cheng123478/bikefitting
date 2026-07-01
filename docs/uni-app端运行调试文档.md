# uni-app 端开发文档

本文档用于说明 `leo` 移动端项目的本地开发流程，主要分为两种调试方式：

1. H5 调试：在电脑浏览器中调试页面和普通接口流程。
2. 真机调试：在 Android 手机上调试 App 实际运行效果。

## 1. 通用准备

### 1.1 项目目录

uni-app 端项目目录：

```text
bikefitting/leo
```

后端项目目录：

```text
bikefitting/aries
```

移动端接口地址配置文件：

```text
bikefitting/leo/config/constants.uts
```

### 1.2 开发工具

需要准备：

1. HBuilderX。
2. Git，用于拉取项目代码。
3. Node.js，建议使用 LTS 版本。
4. Android 真机调试需要 Android 手机和 USB 数据线。

### 1.3 拉取代码

拉取项目代码后，用 HBuilderX 打开：

```text
bikefitting/leo
```

不要选择整个 `bikefitting` 根目录作为 HBuilderX 项目目录，应该选择 `bikefitting/leo`。

如果需要使用命令行安装依赖，可以进入 `leo` 目录执行：

```bash
npm install
```

## 2. H5 调试

H5 调试适合在电脑浏览器中查看页面、交互和普通接口请求。

### 2.1 H5 调试前检查

1. 后端 `aries` 服务已经启动。
2.HBuilderX 已经打开 `bikefitting/leo` 项目。

### 2.2 配置接口地址

H5 在电脑浏览器中运行，如果后端也运行在本机，可以使用：

```ts
export const BASE_URL = 'http://localhost:9200/aries'
```

也可以使用电脑的局域网 IP：

```ts
export const BASE_URL = 'http://192.168.1.10:9200/aries'
```

配置文件位置：

```text
bikefitting/leo/config/constants.uts
```

如果后端端口或基础路径调整了，需要同步修改 `9200` 和 `/aries`。

### 2.3 使用 HBuilderX 运行到浏览器

操作步骤：

1. 在 HBuilderX 中打开 `bikefitting/leo`。
2. 确认后端服务已经启动。
3. 确认 `config/constants.uts` 中的 `BASE_URL` 正确。
4. 点击菜单：`运行` -> `运行到浏览器`。
5. 选择要使用的浏览器。
6. 等待 HBuilderX 编译完成并自动打开页面。

### 2.4 使用命令行运行 H5

也可以在 `bikefitting/leo` 目录下执行：

```bash
npm run dev:h5
```

H5 调试主要用于页面和普通业务流程预览。涉及 App 原生能力、真机权限、相机、文件、蓝牙等能力时，应以真机调试结果为准。

## 3. 真机调试

真机调试适合验证 App 在手机上的真实运行效果，尤其是权限、网络、原生能力和设备相关功能。

### 3.1 真机调试前检查

1. 后端 `aries` 服务已经启动。
2. 手机和电脑连接同一个局域网，例如同一个 Wi-Fi。
3. 手机已经开启开发者选项和 USB 调试。
4. 手机通过 USB 连接电脑，并允许当前电脑进行 USB 调试。
5. HBuilderX 已经打开 `bikefitting/leo` 项目。

### 3.2 配置 HBuilderX Android 环境

在 HBuilderX 中检查：

```text
工具 -> 运行配置 -> Android 配置
```

需要确认：

1. adb 路径。
2. Gradle 路径。
3. JDK 路径。
4. Android SDK 路径。

如果 HBuilderX 无法识别手机，优先检查 USB 调试、数据线、USB 驱动和 adb 配置。

### 3.3 修改真机接口地址

真机调试时，手机不能使用 `localhost` 访问电脑后端，因为手机上的 `localhost` 指的是手机自己。

需要把接口地址改成运行后端服务那台电脑的局域网 IP。

打开文件：

```text
bikefitting/leo/config/constants.uts
```

修改：

```ts
export const BASE_URL = 'http://10.107.83.192:9200/aries'
```

把 `10.107.83.192` 改成自己电脑的局域网 IP，例如：

```ts
export const BASE_URL = 'http://192.168.1.10:9200/aries'
```

Windows 查看局域网 IP：

```bash
ipconfig
```

找到当前网络适配器下的 `IPv4 地址`，一般类似：

```text
192.168.x.x
10.x.x.x
```

注意事项：

1. IP 要填写运行后端服务那台电脑的局域网 IP。
2. 端口默认是 `9200`，如果后端端口改了，这里也要同步修改。
3. 基础路径默认是 `/aries`，如果后端路径改了，这里也要同步修改。
4. Windows 防火墙如果拦截了 `9200` 端口，手机会请求失败，需要放行后端服务或对应端口。

### 3.4 连接 Android 手机

1. 使用 USB 数据线连接手机和电脑。
2. 手机开启 USB 调试。
3. 手机弹出授权提示时，选择允许。
4. 在 HBuilderX 中查看是否能识别到手机。

如果识别不到手机，可以尝试：

1. 重新插拔 USB 数据线。
2. 更换 USB 接口。
3. 更换数据线。
4. 安装手机厂商 USB 驱动。
5. 重启 HBuilderX。

### 3.5 制作自定义调试基座

首次真机运行前，建议先制作自定义调试基座。

自定义基座的作用是把当前项目的 App 配置、原生能力、模块和插件打进调试 App 中。制作完成后，再运行项目到手机，调试结果会更接近真实 App 环境。

操作步骤：

1. 在 HBuilderX 中打开 `bikefitting/leo` 项目。
2. 打开 `manifest.json`，确认应用名称、AppID、Android 配置等信息。
3. 点击菜单：`发行` -> `App 云打包`。
4. 选择 Android 平台。
5. 打包类型选择自定义调试基座。
6. 按 HBuilderX 提示登录 DCloud 账号。
7. 按提示提交云端打包任务。
8. 等待自定义调试基座打包完成。
9. 打包完成后，将自定义基座安装到手机。

以下情况通常需要重新制作自定义基座：

1. 修改了 `manifest.json` 中的 App 原生配置。
2. 新增或删除了原生插件。
3. 修改了 App 权限、模块、图标、启动图等原生相关配置。
4. HBuilderX 提示当前基座不匹配。

普通页面、样式、接口请求、业务逻辑修改，一般不需要重新制作自定义基座，直接重新运行即可。

### 3.6 运行到手机

自定义基座安装完成后：

1. 确认后端服务已经启动。
2. 确认 `BASE_URL` 已经改成电脑局域网 IP。
3. 确认手机和电脑在同一个局域网。
4. 点击菜单：`运行` -> `运行到手机或模拟器`。
5. 选择已连接的 Android 设备。
6. 选择使用自定义调试基座运行。
7. 等待 HBuilderX 编译并安装到手机。

运行成功后，HBuilderX 控制台会输出编译日志，手机上会打开 App。

## 4. 常见问题

### 4.1 手机请求接口失败

优先检查：

1. 后端服务是否已启动。
2. `BASE_URL` 是否是电脑局域网 IP，而不是 `localhost` 或 `127.0.0.1`。
3. 手机和电脑是否在同一个 Wi-Fi 或局域网。
4. 电脑防火墙是否拦截了 `9200` 端口。
5. 后端接口基础路径是否仍然是 `/aries`。

可以在手机浏览器中直接访问：

```text
http://电脑局域网IP:9200/aries
```

如果手机浏览器也打不开，说明不是 App 代码问题，需要先检查网络、后端服务或防火墙。

### 4.2 HBuilderX 找不到手机

检查：

1. 手机是否开启 USB 调试。
2. 手机是否已经允许当前电脑调试。
3. USB 线是否支持数据传输。
4. 是否安装手机厂商 USB 驱动。
5. 是否被其他工具占用了设备连接。

### 4.3 修改代码后手机没有更新

可以尝试：

1. 停止当前运行任务后重新运行。
2. 清理 HBuilderX 编译缓存。
3. 卸载手机上的旧 App 后重新安装。
4. 如果改了原生配置，重新制作自定义基座。

### 4.4 自定义基座运行异常

可以尝试：

1. 确认 HBuilderX 版本和项目依赖版本兼容。
2. 确认 `manifest.json` 配置正确。
3. 删除手机上的旧自定义基座后重新安装。
4. 重新制作自定义调试基座。

## 5. 推荐流程

### 5.1 H5 调试推荐流程

1. 拉取代码。
2. 启动后端 `aries` 服务。
3. 用电脑浏览器确认 `http://localhost:9200/aries` 可访问。
4. HBuilderX 打开 `bikefitting/leo`。
5. 确认 `config/constants.uts` 中的 `BASE_URL` 正确。
6. 点击 `运行` -> `运行到浏览器`。
7. 在浏览器中测试页面和普通接口流程。

### 5.2 真机调试推荐流程

1. 拉取代码。
2. 启动后端 `aries` 服务。
3. 用电脑浏览器确认 `http://localhost:9200/aries` 可访问。
4. 查看电脑局域网 IP。
5. 修改 `bikefitting/leo/config/constants.uts` 中的 `BASE_URL`。
6. HBuilderX 打开 `bikefitting/leo`。
7. 配置 HBuilderX Android 运行环境。
8. 手机开启 USB 调试并连接电脑。
9. 通过 `发行` -> `App 云打包` 制作自定义调试基座。
10. 安装自定义调试基座到手机。
11. 通过 `运行` -> `运行到手机或模拟器` 运行项目。
12. 在手机上测试登录、接口请求和主要页面流程。
