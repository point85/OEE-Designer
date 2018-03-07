rem combined Maven and Ant build script.  Maven maintains the dependent jars.  
call mvn -f pom_full.xml jfx:jar
rem Ant builds the JavaFX jar.
call ant do-deploy