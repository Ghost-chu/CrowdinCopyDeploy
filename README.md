# CrowdinCopyDeploy

该工具用于创建兼容 [Crowdin OTA](https://github.com/Ghost-chu/CrowdinOTA) 的 Crowdin Over-The-Air Distribution 服务器环境。  

## 为什么不直接使用 Crowdin 提供的 Over-The-Air 功能

请参见 [QuickShop-Hikari 3.6.0.3 更新日志](https://github.com/Ghost-chu/QuickShop-Hikari/releases/tag/3.6.0.3)

TL;DR 我们超出了免费配额并吃了一张 70 USD 的账单。

## 适配

目前只为 QuickShop-Hikari 和 QuickShop-Reremake 使用到的地方进行了兼容，其他项目特别是用到了 custom_language_mapping 功能的可能并不能直接使用。

## 环境变量

* CROWDIN_ACCESS_TOKEN - 可以在 [这里](https://crowdin.com/settings#api-key) 创建
* CROWDIN_PROJECT_BRANCH_ID - 通过手动请求 Crowdin API 获取
* CROWDIN_PROJECT_ID - 通过手动请求 Crowdin API 获取
* DEPLOY_PATH - 部署目录，默认在 `./deploy-target` 下
