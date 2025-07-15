Fake replicas of classes, that are compileOnly scoped and relocated back to their real packages in runtime code.
This is required as we want to relocate 'org.slf4j' and other classes, while we need to access the unrelocated variants provided by platforms.

Could be fixed by https://github.com/johnrengelman/shadow/issues/727