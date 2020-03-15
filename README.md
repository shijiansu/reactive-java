![](https://img.shields.io/badge/language-java-blue)
![](https://img.shields.io/badge/technology-reactor3-blue)
![](https://img.shields.io/badge/development%20year-2020-orange)
![](https://img.shields.io/badge/license-MIT-lightgrey)

--------------------------------------------------------------------------------

- reactor3-by-simonbasle - examples of 4 testing scenario - words processing; hello world for subscription test; first emitting; alphabet with StepVerifier verification
- reactor3-by-yjmyzz - examples of comprehensive test cases for mono / flux apis

--------------------------------------------------------------------------------

# Execute all tests in project / sub-projects

`/bin/bash run-project-test.sh`

# Reactor性能

Reactor 性能相当高, 在最新的硬件平台上, 使用无堵塞分发器每秒钟可处理 1500 万事件.

提供了下列功能的替代函数

- 阻塞等待: 如 Future.get()
- 不安全的数据访问: 如 ReentrantLock.lock()
- 异常冒泡: 如 try…catch…finally
- 同步阻塞: 如 synchronized{ }
- Wrapper分配(GC 压力): 如 new Wrapper<T>(event)
