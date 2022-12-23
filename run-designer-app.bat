rem Launch the Designer application (args: JDBC connection string, user name, password and optional collector name)
start "" "%JAVA_HOME%\bin\javaw.exe" -cp ./oee-apps-3.9.1.jar;lib/*;lib/ext/* -p "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml,javafx.web -Dlog4j.configurationFile=config/logging/log4j2.xml org.point85.app.OeeApplication DESIGNER jdbc:hsqldb:hsql://localhost/OEE SA 
 