rem combined Maven and Ant build script.  Maven maintains the dependent jars.  
call mvn -f build_pom.xml jfx:jar
rem Ant builds the JavaFX jar.
rem call ant do-deploy
call mvn -f build_pom.xml install