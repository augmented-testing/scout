![banner](icons/splash.png)

[![Plugins](https://github.com/augmented-testing/scout/actions/workflows/plugins.yml/badge.svg)](https://github.com/augmented-testing/scout/actions/workflows/plugins.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

# Augmented Testing with Scout

Visual GUI Testing made easy!

## Introduction

Scout is a tool using a novel approach we call Augmented Testing (AT) to simplify Visual GUI Testing (VGT).

[add more text]

[add ref to paper]

## Requirements

Scout is developed in Java and therefore requires the Java Runtime Environment (JRE).
JRE version 8 or later is suitable for running Scout.

## Run Scout

You can run Scout either by clicking on the `Scout.jar` or using the command `java -jar Scout.jar`.
Before you can run Scout for the first time you have to build all plugins with `make build`.

## Plugin Development

Scout is a highly customizable tool due its plugin architecture. If you want to extend Scout's functionalities just create a new plugin in the `plugin/` folder.

A plugin should be independent and therefore should not contain any external dependencies. If necessary, external libs must be copied to the `bin/` directory. A good idea for plugin development (and development in general) is to follow the Keep it Simple/Stupid (KISS) principle.

If you are interested in developing a single plugin without the whole Scout environment you should have a look at the [Single-Plugin-Template](https://github.com/augmented-testing/single-plugin-template) repository.

If you have to mange multiple Java versions you could use the tool [SDKMAN](https://sdkman.io) which helps to maintain multiple JDK versions.

### Build and Run

Build automation is accomplished using a Makefile. To get an overview of all provided make targets run `make help`.

- To **build** all plugins: `make build`.
- To **build** all plugins and **run** Scout: `make run`.

### VSCode

If you decide to use VSCode as IDE than you have to install the [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) to be able to develop a plugin.

You can find more information about how to manage Java projects in VSCode following this [link](https://code.visualstudio.com/docs/java/java-project).

### IntelliJ

If you use IntelliJ we recommend you to use the [Makefile Language](https://plugins.jetbrains.com/plugin/9333-makefile-language) extension to use the already defined build and run tasks. See the screenshot.

![IntelliJ Run Config](docs/intellij-run-config.png)

### Doc

The folder `docs/` contains additional documentation as well as the JavaDocs for Scout. JavaDocs are also accessible online via this [link](https://augmented-testing.github.io/scout/index.html).

## License

Copyright (c) 2021 Michel Nass, Andreas Bauer

This work (source code) is licensed under [MIT](./LICENSE/MIT.txt).

Files other than source code are licensed as follows:

- Documentation and screenshots are licensed under [CC BY-SA 4.0](./LICENSES/CC-BY-SA-4.0.txt).

See the [LICENSES](./LICENSES/) folder in the root of this project for license details.
