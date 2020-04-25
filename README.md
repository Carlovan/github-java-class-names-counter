# Github Java class names counter

> ‘There are only two hard things in Computer Science: cache invalidation and naming things.’  
> — Phil Karlton

The aim of this project is to try to verify the correctness of the second part of this statement.  
To do so it extracts the class names in Java based repositories hosted on GitHub.

## Project description
This is a [Gradle](https://gradle.org)-based project written in [Kotlin](https://kolinlang.org).  

### How it works
1. First we need to obtain the list of the repository and filter them to keep only Java-based ones.  
1. To determine if a repository is _"Java-based"_ we check if **Java** is one of the two most used languages inside that repository.  
1. We the obtain the list of all files for every  repository and keep only the ones with `.java` extension.  
1. Finally we can parse the source code to get the class names and count them.

All the information is requested from the [GitHub API](https://developer.github.com/v3/) and saved in a cache for faster subsequent usage.  
Cache is automatically saved when the program terminates correctly and every three new repositories are stored.

### Dependencies
Dependency libraries are used only to handle data or processes based on some kind of standard:

* [**Klaxon**](https://github.com/cbeust/klaxon) is used to serialize and deserialize JSON (to read from GitHub API or save to cache)
* [**Handy-URI-Templates**](https://github.com/damnhandy/Handy-URI-Templates) is used to expand [URI templates](https://en.wikipedia.org/wiki/URL_Template) from the GitHub API
* [**JavaParser**](https://javaparser.org/) is used to parse Java source code and extract class names

## Usage
Execute `./gradlew run` to run the project.  
It will create or update a cache file `cache.json` if necessary.  
While running it will print the repositories being analyzed,
and at the end it will write a file `output.csv` containing
the number of times each class name has been encountered.