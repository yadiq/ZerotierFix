### Zerotier

Zerotier 内网穿透工具。通过虚拟局域网的组建，远程调试公网设备。
详细使用说明，见blog: https://yadiq.github.io/2024/12/26/OtherZeroTier/

## 软件下载

1. Android开源实现 https://github.com/kaaass/ZerotierFix
2. Android自己编译，见本项目源码。（需填入自己的"网络ID", NetworkListActivity.java 第64行）
3. PC端 官网下载 https://www.zerotier.com/

## 配置

1. 官网后台配置
注册账户
创建私有局域网，获得 Network ID

2. PC客户端配置
安装PC客户端，加入 Network ID

4. Android客户端配置
需填入自己的"网络ID", 位置 NetworkListActivity.java 第64行。

5. Android客户端使用
APP启动后，会自动添加一个网络ID，打开开关同意添加VPN链接。等待授权。
后台授权刚刚接入的设备，授权后可看到局域网ip。