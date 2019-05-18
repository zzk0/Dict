# 选文查词

制作的缘由，最近下载了几个学英语用的app，遇到生词总是要复制，然后粘贴到某个地方去查询，步骤很麻烦。有时候，查完的单词，还想以后认得的话，那就得加到平时使用的背单词软件的生词本里面去。不如写个软件给自己用吧！于是，这个软件就在某天晚上开搞了。基本的思路是：选择一个单词，复制后，显示这个单词的音标，释义的信息。如果觉得有必要加入生词本的话，那就将他加入到生词本，作者常用的背单词软件是扇贝单词。嗯，思路就是这么的简单，实现的方法无非就是监听剪切板，网络请求提交到扇贝的生词本。下面记录一些实现过程中，遇到的问题及其处理方式，目前仍旧存在的问题。

# V0.1

完成基本的功能。可以选择复制单词之后，弹出单词的释义。按下Add，添加到扇贝生词本。

存在诸多限制：

1. 字典用户自行下载，放在一个指定的目录下才可以使用。
2. Add单词后，即刻网络请求。没有批量。扇贝未收录的单词，没做处理。请求失败，没做处理。
3. 缺少良好的用户权限管理，第一次运行会闪退，因为申请权限是异步的，而未申请到权限就尝试打开文件，故而闪退。
4. 剪切板监听有时候失效，在浏览器里面复制，一定不会弹出单词释义。
5. 生词本，是在创建视图的时候加载的。两个问题：在主线程进行数据库查询的耗时操作；用户新添加的单词，不会动态的添加进来。


# 目前仍旧存在的问题

在我的手机上，使用浏览器，当复制了文本之后，在返回到桌面的时候，或者切换应用的时候，才可以弹出单词释义。