rem clean build folder
call ant -f build.xml clean
rem build the JavaFX applications
call mvn -f pom.xml jfx:jar
rem build the service wrapper collector
call ant -f build.xml build-collector
rem copy files to staging folder
call ant -f build.xml stage
rem archive files to dist folder
rem call ant -f build.xml zip